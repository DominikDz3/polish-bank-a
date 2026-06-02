package com.polishbank.bank_a.domain.card;

import com.polishbank.bank_a.domain.card.dto.CardPaymentRequest;
import com.polishbank.bank_a.entity.Account;
import com.polishbank.bank_a.entity.Card;
import com.polishbank.bank_a.entity.PendingApproval;
import com.polishbank.bank_a.entity.Transaction;
import com.polishbank.bank_a.repository.AccountRepository;
import com.polishbank.bank_a.repository.CardRepository;
import com.polishbank.bank_a.repository.PendingApprovalRepository;
import com.polishbank.bank_a.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class CardPaymentService {

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");

    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final PendingApprovalRepository pendingApprovalRepository;
    private final CardLimitService cardLimitService;

    @Transactional
    public PaymentResult processPayment(CardPaymentRequest request, String userEmail) {
        Card card = cardRepository.findById(request.cardId())
                .orElseThrow(() -> new IllegalArgumentException("Karta nie znaleziona."));

        Account account = card.getAccount();
        if (!account.getUser().getEmail().equals(userEmail)) {
            throw new SecurityException("Brak uprawnień do tej karty.");
        }

        if (!card.getCurrency().equalsIgnoreCase(request.currency())) {
            throw new IllegalArgumentException(
                    "Transakcje w walucie innej niż waluta karty (" + card.getCurrency() + ") nie są obsługiwane.");
        }

        cardLimitService.validate(card, request.amount());

        if (account.getBalance().compareTo(request.amount()) < 0) {
            throw new IllegalStateException("Niewystarczające środki na koncie powiązanym z kartą.");
        }

        boolean juniorPayment = "JUNIOR".equals(account.getType());

        Transaction tx = Transaction.builder()
                .senderAccount(account)
                .senderAccountNumber(account.getAccountNumber())
                .receiverName(request.merchant())
                .title("Płatność kartą: " + request.merchant())
                .amount(request.amount())
                .currency(request.currency())
                .type("CARD_PAYMENT")
                .card(card)
                .build();

        if (juniorPayment) {
            Account parentAccount = account.getParentAccount();
            if (parentAccount == null) {
                throw new IllegalStateException("Konto Junior musi być podpięte do konta rodzica.");
            }

            tx.setStatus("PENDING_APPROVAL");
            transactionRepository.save(tx);

            PendingApproval pa = PendingApproval.builder()
                    .juniorAccount(account)
                    .parentUser(parentAccount.getUser())
                    .transaction(tx)
                    .amount(request.amount())
                    .description("Płatność kartą: " + request.merchant())
                    .status("PENDING")
                    .build();
            pendingApprovalRepository.save(pa);

            return new PaymentResult("PENDING_APPROVAL", tx);
        }

        account.setBalance(account.getBalance().subtract(request.amount()));
        accountRepository.save(account);

        tx.setStatus("COMPLETED");
        tx.setExecutionDate(LocalDateTime.now(ZONE));
        transactionRepository.save(tx);

        return new PaymentResult("COMPLETED", tx);
    }

    public record PaymentResult(String status, Transaction transaction) {}
}