package com.polishbank.bank_a.domain.card;

import com.polishbank.bank_a.domain.card.dto.CardPaymentRequest;
import com.polishbank.bank_a.entity.Account;
import com.polishbank.bank_a.entity.Card;
import com.polishbank.bank_a.entity.Transaction;
import com.polishbank.bank_a.repository.AccountRepository;
import com.polishbank.bank_a.repository.CardRepository;
import com.polishbank.bank_a.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CardPaymentService {

    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CardLimitService cardLimitService;

    @Transactional
    public Transaction processPayment(CardPaymentRequest request, String userEmail) {
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

        account.setBalance(account.getBalance().subtract(request.amount()));
        accountRepository.save(account);

        Transaction tx = Transaction.builder()
                .senderAccount(account)
                .senderAccountNumber(account.getAccountNumber())
                .receiverName(request.merchant())
                .title("Płatność kartą: " + request.merchant())
                .amount(request.amount())
                .currency(request.currency())
                .status("COMPLETED")
                .type("CARD_PAYMENT")
                .card(card)
                .executionDate(LocalDateTime.now(java.time.ZoneId.of("Europe/Warsaw")))
                .build();

        return transactionRepository.save(tx);
    }
}