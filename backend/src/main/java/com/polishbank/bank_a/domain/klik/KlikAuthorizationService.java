package com.polishbank.bank_a.domain.klik;

import com.polishbank.bank_a.domain.auth.PinService;
import com.polishbank.bank_a.domain.klik.dto.KlikPendingAuthorizationResponse;
import com.polishbank.bank_a.domain.klik.dto.KlikWebhookAuthorizeRequest;
import com.polishbank.bank_a.domain.user.User;
import com.polishbank.bank_a.domain.user.UserRepository;
import com.polishbank.bank_a.entity.Account;
import com.polishbank.bank_a.entity.KlikAuthorization;
import com.polishbank.bank_a.entity.KlikCode;
import com.polishbank.bank_a.entity.Transaction;
import com.polishbank.bank_a.integration.klik.KlikC2BClient;
import com.polishbank.bank_a.repository.AccountRepository;
import com.polishbank.bank_a.repository.KlikAuthorizationRepository;
import com.polishbank.bank_a.repository.KlikCodeRepository;
import com.polishbank.bank_a.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KlikAuthorizationService {

    private final KlikAuthorizationRepository authRepository;
    private final KlikCodeRepository codeRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final PinService pinService;
    private final KlikC2BClient klikClient;

    @Transactional
    public void receiveAuthorization(KlikWebhookAuthorizeRequest req) {
        if (authRepository.findByKlikTransactionId(req.transactionId()).isPresent()) {
            return;
        }

        KlikCode activeCode = null;
        User user = null;

        // Najpierw spróbuj po user_id (jeśli KLIK go wysłał)
        if (req.userId() != null && !req.userId().isBlank()) {
            try {
                UUID userId = UUID.fromString(req.userId());
                user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    activeCode = codeRepository.findByUser_IdAndStatus(userId, "ACTIVE")
                            .stream()
                            .filter(c -> c.getExpiresAt().isAfter(LocalDateTime.now()))
                            .findFirst()
                            .orElse(null);
                }
            } catch (IllegalArgumentException ignored) {
                // user_id nie jest UUID - fallback poniżej
            }
        }

        // Fallback - znajdź najnowszy aktywny kod (kompatybilność ze starszą wersją KLIK)
        if (activeCode == null) {
            List<KlikCode> activeCodes = codeRepository.findByStatusOrderByCreatedAtDesc("ACTIVE")
                    .stream()
                    .filter(c -> c.getExpiresAt().isAfter(LocalDateTime.now()))
                    .toList();

            if (activeCodes.isEmpty()) {
                throw new IllegalStateException("Brak aktywnego kodu BLIK - nie wiadomo do kogo przypisać autoryzację.");
            }

            activeCode = activeCodes.get(0);
            user = activeCode.getUser();
        }

        LocalDateTime expiry = OffsetDateTime.parse(req.expiryTime()).toLocalDateTime();

        KlikAuthorization auth = KlikAuthorization.builder()
                .klikTransactionId(req.transactionId())
                .user(user)
                .account(activeCode.getAccount())
                .klikCode(activeCode)
                .amount(req.amount())
                .currency(req.currency())
                .merchantName(req.merchantName())
                .isOnUs(req.isOnUs())
                .status("PENDING_AUTH")
                .expiryTime(expiry)
                .build();

        authRepository.save(auth);

        activeCode.setStatus("USED");
        activeCode.setUsedAt(LocalDateTime.now());
        codeRepository.save(activeCode);
    }

    public List<KlikPendingAuthorizationResponse> getPendingForUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        return authRepository.findByUser_IdAndStatus(user.getId(), "PENDING_AUTH")
                .stream()
                .filter(a -> a.getExpiryTime().isAfter(LocalDateTime.now()))
                .map(a -> new KlikPendingAuthorizationResponse(
                        a.getId(),
                        a.getKlikTransactionId(),
                        a.getAmount(),
                        a.getCurrency(),
                        a.getMerchantName(),
                        a.getAccount().getAccountNumber(),
                        a.getExpiryTime()
                ))
                .toList();
    }

    @Transactional
    public void confirm(UUID authId, String userEmail, String decision, String reason) {
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        KlikAuthorization auth = authRepository.findById(authId)
                .orElseThrow(() -> new IllegalArgumentException("Autoryzacja nie istnieje."));

        if (!auth.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Brak dostępu do tej autoryzacji.");
        }
        if (!"PENDING_AUTH".equals(auth.getStatus())) {
            throw new IllegalStateException("Autoryzacja nie jest w stanie oczekującym.");
        }
        if (auth.getExpiryTime().isBefore(LocalDateTime.now())) {
            auth.setStatus("EXPIRED");
            authRepository.save(auth);
            throw new IllegalStateException("Czas autoryzacji minął.");
        }

        if ("REJECT".equals(decision)) {
            String rejectReason = (reason == null || reason.isBlank()) ? "USER_DECLINED" : reason;

            try {
                klikClient.rejectPayment(auth.getKlikTransactionId(), rejectReason);
            } catch (Exception e) {
                System.err.println("KLIK reject failed: " + e.getMessage());
            }

            auth.setStatus("REJECTED");
            auth.setRejectReason(rejectReason);
            auth.setResolvedAt(LocalDateTime.now());
            authRepository.save(auth);
            return;
        }

        throw new IllegalArgumentException(
                "Endpoint /confirm bez PIN-u służy tylko do REJECT. Do ACCEPT użyj wariantu z PIN-em.");
    }

    @Transactional
    public void confirmWithPin(UUID authId, String userEmail, String pin) {
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        KlikAuthorization auth = authRepository.findById(authId)
                .orElseThrow(() -> new IllegalArgumentException("Autoryzacja nie istnieje."));

        if (!auth.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Brak dostępu do tej autoryzacji.");
        }
        if (!"PENDING_AUTH".equals(auth.getStatus())) {
            throw new IllegalStateException("Autoryzacja nie jest w stanie oczekującym.");
        }
        if (auth.getExpiryTime().isBefore(LocalDateTime.now())) {
            auth.setStatus("EXPIRED");
            authRepository.save(auth);
            throw new IllegalStateException("Czas autoryzacji minął.");
        }

        pinService.verifyPin(user, pin);

        Account account = auth.getAccount();
        if (account.getBalance().compareTo(auth.getAmount()) < 0) {
            try {
                klikClient.rejectPayment(auth.getKlikTransactionId(), "INSUFFICIENT_FUNDS");
            } catch (Exception e) {
                System.err.println("KLIK reject (insufficient funds) failed: " + e.getMessage());
            }
            auth.setStatus("REJECTED");
            auth.setRejectReason("INSUFFICIENT_FUNDS");
            auth.setResolvedAt(LocalDateTime.now());
            authRepository.save(auth);
            throw new IllegalStateException("Niewystarczające środki na koncie.");
        }

        account.setBalance(account.getBalance().subtract(auth.getAmount()));
        accountRepository.save(account);

        Transaction tx = Transaction.builder()
                .senderAccount(account)
                .senderAccountNumber(account.getAccountNumber())
                .receiverAccountNumber("KLIK:" + auth.getMerchantName())
                .receiverName(auth.getMerchantName())
                .amount(auth.getAmount())
                .currency(auth.getCurrency())
                .title("BLIK: " + auth.getMerchantName())
                .status("COMPLETED")
                .type("KLIK_C2B")
                .executionDate(LocalDateTime.now())
                .build();
        transactionRepository.save(tx);

        try {
            klikClient.confirmPayment(auth.getKlikTransactionId());
        } catch (Exception e) {
            System.err.println("KLIK confirm failed: " + e.getMessage());
        }

        auth.setStatus("COMPLETED");
        auth.setResolvedAt(LocalDateTime.now());
        authRepository.save(auth);
    }
}