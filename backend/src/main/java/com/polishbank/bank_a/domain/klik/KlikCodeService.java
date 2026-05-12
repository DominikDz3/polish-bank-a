package com.polishbank.bank_a.domain.klik;

import com.polishbank.bank_a.domain.klik.dto.KlikCodeResponse;
import com.polishbank.bank_a.domain.user.User;
import com.polishbank.bank_a.domain.user.UserRepository;
import com.polishbank.bank_a.entity.Account;
import com.polishbank.bank_a.entity.KlikCode;
import com.polishbank.bank_a.integration.klik.KlikC2BClient;
import com.polishbank.bank_a.integration.klik.dto.KlikGenerateCodeResponse;
import com.polishbank.bank_a.repository.AccountRepository;
import com.polishbank.bank_a.repository.KlikCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KlikCodeService {

    private final KlikC2BClient klikClient;
    private final KlikCodeRepository klikCodeRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Transactional
    public KlikCodeResponse generateCode(UUID accountId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Użytkownik nie znaleziony."));

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Konto nie znalezione."));

        if (!account.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Brak dostępu do konta.");
        }

        // Wygaszamy poprzednie aktywne kody dla tego usera (jeden aktywny kod naraz)
        klikCodeRepository.findByUser_IdAndStatus(user.getId(), "ACTIVE")
                .forEach(c -> {
                    c.setStatus("EXPIRED");
                    klikCodeRepository.save(c);
                });

        KlikGenerateCodeResponse klikResponse = klikClient.generateCode(user.getId().toString());

        LocalDateTime expiresAt = java.time.OffsetDateTime
                .parse(klikResponse.expiresAt())
                .toLocalDateTime();

        KlikCode code = KlikCode.builder()
                .user(user)
                .account(account)
                .code(klikResponse.code())
                .status("ACTIVE")
                .expiresAt(expiresAt)
                .build();
        klikCodeRepository.save(code);

        return new KlikCodeResponse(
                code.getId(),
                code.getCode(),
                code.getExpiresAt(),
                code.getStatus(),
                account.getAccountNumber()
        );
    }
}