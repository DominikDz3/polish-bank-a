package com.polishbank.bank_a.domain.card;

import com.polishbank.bank_a.entity.Card;
import com.polishbank.bank_a.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CardLimitService {

    private final TransactionRepository transactionRepository;

    public BigDecimal sumSpentToday(UUID cardId) {
        ZoneId zone = ZoneId.of("Europe/Warsaw");
        LocalDateTime startOfDay = LocalDate.now(zone).atStartOfDay();
        LocalDateTime startOfNext = LocalDate.now(zone).plusDays(1).atStartOfDay();
        BigDecimal sum = transactionRepository.sumCardSpentInRange(cardId, startOfDay, startOfNext);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    public void validate(Card card, BigDecimal amount) {
        if (card.isBlocked()) {
            throw new IllegalStateException("Karta jest zablokowana.");
        }
        if (card.getExpiryDate() != null && card.getExpiryDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException("Karta wygasła.");
        }
        if (card.getTransactionLimit() != null
                && amount.compareTo(card.getTransactionLimit()) > 0) {
            throw new IllegalStateException(
                    "Kwota " + amount + " " + card.getCurrency()
                            + " przekracza limit pojedynczej transakcji ("
                            + card.getTransactionLimit() + " " + card.getCurrency() + ").");
        }
        if (card.getDailyLimit() != null) {
            BigDecimal spent = sumSpentToday(card.getId());
            BigDecimal afterTx = spent.add(amount);
            if (afterTx.compareTo(card.getDailyLimit()) > 0) {
                BigDecimal remaining = card.getDailyLimit().subtract(spent).max(BigDecimal.ZERO);
                throw new IllegalStateException(
                        "Kwota przekracza dzienny limit karty. Wydano dziś: "
                                + spent + " " + card.getCurrency()
                                + ", pozostały limit: " + remaining + " " + card.getCurrency() + ".");
            }
        }
    }
}