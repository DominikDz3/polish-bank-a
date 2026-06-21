package com.polishbank.bank_a.domain.klik;

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
import com.polishbank.bank_a.integration.klik.KlikP2PClient;
import com.polishbank.bank_a.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class KlikP2PService {

    private final KlikP2PClient klikClient;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionService transactionService;
    private final ExternalTransferService externalTransferService;

    @Value("${app.bank.bicfi}")
    private String ourBicfi;

    public KlikP2PResponse send(KlikP2PRequest req, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        Account senderAccount = accountRepository.findById(req.senderAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Konto nadawcy nie istnieje."));
        if (!senderAccount.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Brak dostępu do tego konta.");
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

        if (ourBicfi.equals(receiverBankCode)) {
            InternalTransferRequest internalReq = new InternalTransferRequest(
                    req.senderAccountId(),
                    receiverIban,
                    req.amount(),
                    "[KLIK " + req.phone() + "] " + req.title(),
                    req.pin()
            );
            String result = transactionService.processInternalTransfer(internalReq, user.getCustomerNumber());
            return new KlikP2PResponse("INTERNAL", "COMPLETED", receiverBankCode, receiverIban, result);
        } else {
            ExternalTransferRequest extReq = new ExternalTransferRequest(
                    req.senderAccountId(),
                    receiverIban,
                    req.receiverName(),
                    receiverBankCode,
                    req.amount(),
                    "[KLIK " + req.phone() + "] " + req.title(),
                    "EXPRESS",
                    req.pin()
            );
            ExternalTransferResponse r = externalTransferService.createTransfer(extReq, userEmail);
            return new KlikP2PResponse("EXTERNAL", r.status(), receiverBankCode, receiverIban, r.externalPaymentId());
        }
    }
}