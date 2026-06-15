package com.polishbank.bank_a.integration.cards;

import com.polishbank.bank_a.entity.Card;
import com.polishbank.bank_a.entity.Transaction;
import com.polishbank.bank_a.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class CardTransactionAuditService {

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");

    private final TransactionRepository transactionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveRejectedCardPayment(Card card, BigDecimal amount, String externalTxId,
                                        String merchant, String reason) {
        String cardLabel = (card.getMaskedPan() != null && card.getMaskedPan().length() >= 4)
                ? " (•••• " + card.getMaskedPan().substring(card.getMaskedPan().length() - 4) + ")"
                : "";
        Transaction rejected = Transaction.builder()
                .senderAccount(card.getAccount())
                .senderAccountNumber(card.getAccount().getAccountNumber())
                .card(card)
                .receiverName(merchant)
                .title("Próba płatności kartą" + cardLabel + " odrzucona: " + reason)
                .amount(amount)
                .currency(card.getCurrency())
                .type("CARD_PAYMENT_REJECTED")
                .status("REJECTED")
                .externalPaymentId(externalTxId)
                .executionDate(LocalDateTime.now(ZONE))
                .build();
        transactionRepository.save(rejected);
    }
}