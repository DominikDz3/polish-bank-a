package com.polishbank.bank_a.domain.card;

import com.polishbank.bank_a.domain.card.dto.CardResponse;
import com.polishbank.bank_a.domain.card.dto.UpdateCardLimitsRequest;
import com.polishbank.bank_a.domain.user.User;
import com.polishbank.bank_a.domain.user.UserRepository;
import com.polishbank.bank_a.entity.Account;
import com.polishbank.bank_a.entity.Card;
import com.polishbank.bank_a.repository.AccountRepository;
import com.polishbank.bank_a.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class CardService {

    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;
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

    public List<CardResponse> listForJuniorAccount(UUID juniorAccountId, String parentEmail) {
        User parent = userRepository.findByEmail(parentEmail)
                .orElseThrow(() -> new IllegalArgumentException("Użytkownik nie znaleziony."));

        Account juniorAccount = accountRepository.findById(juniorAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Konto Junior nie znalezione."));

        if (!"JUNIOR".equals(juniorAccount.getType())) {
            throw new IllegalArgumentException("To nie jest konto Junior.");
        }
        if (juniorAccount.getParentAccount() == null
                || !juniorAccount.getParentAccount().getUser().getId().equals(parent.getId())) {
            throw new SecurityException("Brak dostępu do tego konta Junior.");
        }

        return cardRepository.findAll().stream()
                .filter(c -> c.getAccount().getId().equals(juniorAccountId))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void updateJuniorCardLimits(UUID cardId, UpdateCardLimitsRequest request, String parentEmail) {
        User parent = userRepository.findByEmail(parentEmail)
                .orElseThrow(() -> new IllegalArgumentException("Użytkownik nie znaleziony."));

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Karta nie znaleziona."));

        Account account = card.getAccount();
        if (!"JUNIOR".equals(account.getType())) {
            throw new IllegalArgumentException("Limity można zmieniać tylko dla kart konta Junior.");
        }
        if (!"PREPAID".equals(card.getType())) {
            throw new IllegalArgumentException("Limity można zmieniać tylko dla kart prepaid.");
        }
        if (account.getParentAccount() == null
                || !account.getParentAccount().getUser().getId().equals(parent.getId())) {
            throw new SecurityException("Brak uprawnień do tej karty.");
        }

        if (request.transactionLimit() != null) {
            card.setTransactionLimit(request.transactionLimit());
        }
        if (request.dailyLimit() != null) {
            card.setDailyLimit(request.dailyLimit());
        }
        cardRepository.save(card);
    }

        private CardResponse toResponse(Card card) {
        String masked = card.getMaskedPan() != null
                ? card.getMaskedPan()
                : (card.getCardNumber() == null
                    ? null
                    : "**** **** **** " + card.getCardNumber().substring(Math.max(0, card.getCardNumber().length() - 4)));
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
                card.isBlocked(),
                card.getProviderToken(),
                card.getProviderStatus(),
                card.getMaskedPan()
        );
    }
}