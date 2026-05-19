package com.polishbank.bank_a.domain.card;

import com.polishbank.bank_a.domain.card.dto.CardResponse;
import com.polishbank.bank_a.domain.user.User;
import com.polishbank.bank_a.domain.user.UserRepository;
import com.polishbank.bank_a.entity.Card;
import com.polishbank.bank_a.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardLimitService cardLimitService;

    public List<CardResponse> listMyCards(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Użytkownik nie znaleziony."));

        return cardRepository.findAll().stream()
                .filter(c -> c.getAccount().getUser().getId().equals(user.getId()))
                .map(this::toResponse)
                .toList();
    }

    private CardResponse toResponse(Card card) {
        String masked = card.getCardNumber() == null
                ? null
                : "**** **** **** " + card.getCardNumber().substring(Math.max(0, card.getCardNumber().length() - 4));
        return new CardResponse(
                card.getId(),
                card.getAccount().getId(),
                card.getAccount().getAccountNumber(),
                masked,
                card.getTransactionLimit(),
                card.getDailyLimit(),
                cardLimitService.sumSpentToday(card.getId()),
                card.getCurrency(),
                card.getExpiryDate(),
                card.getType(),
                card.isBlocked()
        );
    }
}