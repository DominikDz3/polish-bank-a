package com.polishbank.bank_a.domain.klik;

import com.polishbank.bank_a.domain.aml.*;
import com.polishbank.bank_a.domain.auth.PinService;
import com.polishbank.bank_a.domain.klik.dto.KlikP2PRequest;
import com.polishbank.bank_a.domain.klik.dto.KlikP2PResponse;
import com.polishbank.bank_a.domain.transaction.TransactionService;
import com.polishbank.bank_a.domain.transaction.dto.InternalTransferRequest;
import com.polishbank.bank_a.domain.transfer.ExternalTransferService;
import com.polishbank.bank_a.domain.transfer.dto.ExternalTransferRequest;
import com.polishbank.bank_a.domain.transfer.dto.ExternalTransferResponse;
import com.polishbank.bank_a.domain.user.User;
import com.polishbank.bank_a.domain.user.UserRepository;
import com.polishbank.bank_a.entity.Account;
import com.polishbank.bank_a.entity.ExternalTransfer;
import com.polishbank.bank_a.entity.Transaction;
import com.polishbank.bank_a.integration.klik.KlikP2PClient;
import com.polishbank.bank_a.repository.AccountRepository;
import com.polishbank.bank_a.repository.ExternalTransferRepository;
import com.polishbank.bank_a.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KlikP2PService {

    private final KlikP2PClient klikClient;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionService transactionService;
    private final ExternalTransferService externalTransferService;
    private final PinService pinService;
    private final AmlEvaluator amlEvaluator;
    private final AmlHoldCreator amlHoldCreator;
    private final TransactionRepository transactionRepository;
    private final ExternalTransferRepository externalTransferRepository;

    @Value("${app.bank.bicfi}")
    private String ourBicfi;

    @Transactional
    public KlikP2PResponse send(KlikP2PRequest req, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        Account senderAccount = accountRepository.findById(req.senderAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Konto nadawcy nie istnieje."));
        if (!senderAccount.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Brak dostępu do tego konta.");
        }
        pinService.verifyPin(user, req.pin());

        if (senderAccount.getBalance().compareTo(req.amount()) < 0) {
            throw new IllegalStateException("Niewystarczające środki na koncie.");
        }

        Map<String, Object> lookup;
        try {
            lookup = klikClient.lookupAlias(req.phone());
        } catch (HttpClientErrorException.NotFound e) {
            throw new IllegalStateException("Numer telefonu nie jest zarejestrowany w KLIK.");
        } catch (Exception e) {
            throw new IllegalStateException("Nie udało się sprawdzić numeru w KLIK: " + e.getMessage());
        }

        String receiverBankCode = (String) lookup.get("bank_code");
        @SuppressWarnings("unchecked")
        Map<String, Object> accIdent = (Map<String, Object>) lookup.get("account_identifier");
        String receiverIban = (String) accIdent.get("value");
        boolean isOnUs = ourBicfi.equals(receiverBankCode);

        AmlResult eval = amlEvaluator.evaluate(new AmlContext(
                user.getId(),
                senderAccount.getId(),
                req.amount(),
                senderAccount.getCurrency(),
                AmlTransactionType.KLIK_P2P,
                req.title(),
                receiverIban,
                null
        ));
        if (eval.hold()) {
            return holdAndStage(req, user, senderAccount, receiverBankCode, receiverIban, isOnUs, eval);
        }

        String prefixedTitle = "[KLIK " + req.phone() + "] " + req.title();
        if (isOnUs) {
            InternalTransferRequest internalReq = new InternalTransferRequest(
                    req.senderAccountId(), receiverIban, req.amount(), prefixedTitle, req.pin());
            String result = transactionService.processInternalTransfer(internalReq, user.getCustomerNumber());
            return new KlikP2PResponse("INTERNAL", result, receiverBankCode, receiverIban, result);
        } else {
            ExternalTransferRequest extReq = new ExternalTransferRequest(
                    req.senderAccountId(), receiverIban, req.receiverName(), receiverBankCode,
                    req.amount(), prefixedTitle, "EXPRESS", req.pin());
            ExternalTransferResponse r = externalTransferService.createTransfer(extReq, userEmail);
            return new KlikP2PResponse("EXTERNAL", r.status(), receiverBankCode, receiverIban, r.externalPaymentId());
        }
    }

    private KlikP2PResponse holdAndStage(KlikP2PRequest req, User user, Account senderAccount,
                                         String receiverBankCode, String receiverIban,
                                         boolean isOnUs, AmlResult eval) {
        senderAccount.setBalance(senderAccount.getBalance().subtract(req.amount()));
        senderAccount.setBlockedFunds(senderAccount.getBlockedFunds().add(req.amount()));
        accountRepository.save(senderAccount);

        String prefixedTitle = "[KLIK " + req.phone() + "] " + req.title();
        Transaction stagedTx = null;
        ExternalTransfer stagedExt = null;

        if (isOnUs) {
            Account receiverAccount = accountRepository.findByAccountNumber(receiverIban).orElse(null);
            stagedTx = Transaction.builder()
                    .senderAccount(senderAccount)
                    .senderAccountNumber(senderAccount.getAccountNumber())
                    .receiverAccount(receiverAccount)
                    .receiverAccountNumber(receiverIban)
                    .amount(req.amount())
                    .currency(senderAccount.getCurrency())
                    .title(prefixedTitle)
                    .type("INTERNAL")
                    .status("HELD_FOR_AML")
                    .build();
            transactionRepository.save(stagedTx);
        } else {
            stagedExt = ExternalTransfer.builder()
                    .externalPaymentId("KLIK-" + System.currentTimeMillis() + "-" +
                            UUID.randomUUID().toString().substring(0, 8))
                    .senderAccount(senderAccount)
                    .senderAccountNumber(senderAccount.getAccountNumber())
                    .senderName(user.getFirstName() + " " + user.getLastName())
                    .receiverAccountNumber(receiverIban)
                    .receiverName(req.receiverName())
                    .receiverBankBicfi(receiverBankCode)
                    .amount(req.amount())
                    .currency(senderAccount.getCurrency())
                    .title(prefixedTitle)
                    .routingSystem("EXPRESS")
                    .status("HELD_FOR_AML")
                    .build();
            externalTransferRepository.save(stagedExt);
        }

        amlHoldCreator.create(user, senderAccount, AmlTransactionType.KLIK_P2P,
                stagedTx, stagedExt, null, eval, req.amount(), senderAccount.getCurrency(),
                "KLIK " + req.phone() + " → " + receiverIban + " (" + receiverBankCode + ")");

        return new KlikP2PResponse(
                isOnUs ? "INTERNAL" : "EXTERNAL",
                "HELD_FOR_AML",
                receiverBankCode,
                receiverIban,
                "Transakcja wstrzymana przez AML. Złóż wyjaśnienia w sekcji 'Wstrzymane transakcje'."
        );
    }
}