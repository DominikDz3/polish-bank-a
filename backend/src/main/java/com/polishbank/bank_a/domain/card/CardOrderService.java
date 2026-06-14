package com.polishbank.bank_a.domain.card;

import com.polishbank.bank_a.domain.card.dto.OrderCardResponse;
import com.polishbank.bank_a.domain.user.User;
import com.polishbank.bank_a.domain.user.UserRepository;
import com.polishbank.bank_a.entity.Account;
import com.polishbank.bank_a.entity.Card;
import com.polishbank.bank_a.integration.cards.CardsProviderClient;
import com.polishbank.bank_a.integration.cards.CardsProviderProperties;
import com.polishbank.bank_a.integration.cards.dto.ActivateCardRequest;
import com.polishbank.bank_a.integration.cards.dto.ChangeStatusRequest;
import com.polishbank.bank_a.integration.cards.dto.IssueCardRequest;
import com.polishbank.bank_a.integration.cards.dto.IssueCardResponse;
import com.polishbank.bank_a.integration.cards.dto.TopupCardRequest;
import com.polishbank.bank_a.integration.cards.dto.TopupCardResponse;
import com.polishbank.bank_a.repository.AccountRepository;
import com.polishbank.bank_a.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CardOrderService {

    private static final Set<String> ALLOWED_USER_TYPES = Set.of("VIRTUAL", "PHYSICAL");
    private static final Set<String> ALLOWED_JUNIOR_TYPES = Set.of("PREPAID");

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;
    private final CardsProviderClient cardsProviderClient;
    private final CardsProviderProperties cardsProviderProperties;

    @Transactional
    public OrderCardResponse orderForUser(String userEmail, String cardType) {
        if (!ALLOWED_USER_TYPES.contains(cardType)) {
            throw new IllegalArgumentException("Niedozwolony typ karty dla klienta: " + cardType);
        }
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Użytkownik nie znaleziony."));
        Account account = findPrimaryAccount(user);

        IssueCardResponse issued = cardsProviderClient.issueCard(new IssueCardRequest(
                user.getId().toString(),
                account.getId().toString(),
                cardType,
                0.0
        ));

        Card card = persistIssuedCard(account, issued, cardType, null);
        return toResponse(card, issued);
    }

    @Transactional
    public OrderCardResponse orderForJunior(UUID juniorAccountId, String parentEmail, String cardType) {
        if (!ALLOWED_JUNIOR_TYPES.contains(cardType)) {
            throw new IllegalArgumentException("Dla konta Junior dopuszczalne są tylko karty PREPAID.");
        }
        User parent = userRepository.findByEmail(parentEmail)
                .orElseThrow(() -> new IllegalArgumentException("Użytkownik nie znaleziony."));
        Account juniorAccount = accountRepository.findById(juniorAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Konto Junior nie znalezione."));
        if (!"JUNIOR".equals(juniorAccount.getType())) {
            throw new IllegalArgumentException("To nie jest konto Junior.");
        }
        if (juniorAccount.getParentAccount() == null
                || !juniorAccount.getParentAccount().getUser().getId().equals(parent.getId())) {
            throw new SecurityException("Brak uprawnień do tego konta Junior.");
        }

        User juniorUser = juniorAccount.getUser();
        IssueCardResponse issued = cardsProviderClient.issueCard(new IssueCardRequest(
                juniorUser.getId().toString(),
                juniorAccount.getId().toString(),
                cardType,
                0.0
        ));

        Card card = persistIssuedCard(juniorAccount, issued, cardType, BigDecimal.valueOf(200));
        return toResponse(card, issued);
    }

    @Transactional
    public void blockCard(UUID cardId, String userEmail) {
        Card card = loadCardOwnedBy(cardId, userEmail);
        cardsProviderClient.changeStatus(card.getProviderToken(),
                new ChangeStatusRequest("BLOCKED", "Zablokowana przez klienta"));
        card.setBlocked(true);
        card.setProviderStatus("BLOCKED");
        cardRepository.save(card);
    }

    @Transactional
    public void unblockCard(UUID cardId, String userEmail) {
        Card card = loadCardOwnedBy(cardId, userEmail);
        cardsProviderClient.changeStatus(card.getProviderToken(),
                new ChangeStatusRequest("ACTIVE", "Odblokowana przez klienta"));
        card.setBlocked(false);
        card.setProviderStatus("ACTIVE");
        cardRepository.save(card);
    }

    @Transactional
    public void activateCard(UUID cardId, String userEmail) {
        Card card = loadCardOwnedBy(cardId, userEmail);
        cardsProviderClient.activateCard(card.getProviderToken(), new ActivateCardRequest("customer"));
        card.setProviderStatus("ACTIVE");
        cardRepository.save(card);
    }

    @Transactional
    public BigDecimal topupCard(UUID cardId, String userEmail, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Kwota doładowania musi być dodatnia.");
        }
        Card card = loadCardOwnedOrManagedBy(cardId, userEmail);
        if (!"PREPAID".equals(card.getType())) {
            throw new IllegalArgumentException("Doładowanie dostępne tylko dla kart PREPAID.");
        }

        Account fundingAccount = findFundingAccountForTopup(card, userEmail);
        if (fundingAccount.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Brak wystarczających środków na koncie do doładowania.");
        }

        TopupCardResponse resp = cardsProviderClient.topupCard(
                card.getProviderToken(),
                new TopupCardRequest(amount.doubleValue(), card.getCurrency()));

        fundingAccount.setBalance(fundingAccount.getBalance().subtract(amount));
        accountRepository.save(fundingAccount);

        return resp != null && resp.newBalance() != null
                ? BigDecimal.valueOf(resp.newBalance())
                : null;
    }
    
    @Transactional
    public void devForceActivate(UUID cardId, String userEmail) {
        Card card = loadCardOwnedBy(cardId, userEmail);

        var remote = cardsProviderClient.getStatus(card.getProviderToken());
        String status = remote.status();

        if ("ACTIVE".equals(status)) {
            card.setProviderStatus("ACTIVE");
            card.setBlocked(false);
            cardRepository.save(card);
            return;
        }
        if ("BLOCKED".equals(status)) {
            cardsProviderClient.changeStatus(card.getProviderToken(),
                    new com.polishbank.bank_a.integration.cards.dto.ChangeStatusRequest("ACTIVE", "dev"));
            card.setProviderStatus("ACTIVE");
            card.setBlocked(false);
            cardRepository.save(card);
            return;
        }
        if ("REQUESTED".equals(status)) {
            cardsProviderClient.updateLifecycle(card.getProviderToken(),
                    new com.polishbank.bank_a.integration.cards.dto.LifecycleRequest("PRODUCING", "dev"));
            status = "PRODUCING";
        }
        if ("PRODUCING".equals(status)) {
            cardsProviderClient.updateLifecycle(card.getProviderToken(),
                    new com.polishbank.bank_a.integration.cards.dto.LifecycleRequest("SHIPPED", "dev"));
            status = "SHIPPED";
        }
        if ("SHIPPED".equals(status)) {
            cardsProviderClient.activateCard(card.getProviderToken(),
                    new com.polishbank.bank_a.integration.cards.dto.ActivateCardRequest("dev"));
        }
        card.setProviderStatus("ACTIVE");
        card.setBlocked(false);
        cardRepository.save(card);
    }

    private Card persistIssuedCard(Account account, IssueCardResponse issued, String cardType,
                                   BigDecimal defaultLimit) {
        LocalDate expiry = null;
        if (issued.expiryMonth() != null && issued.expiryYear() != null) {
            int year = issued.expiryYear() < 100 ? 2000 + issued.expiryYear() : issued.expiryYear();
            expiry = LocalDate.of(year, issued.expiryMonth(), 1).withDayOfMonth(
                    LocalDate.of(year, issued.expiryMonth(), 1).lengthOfMonth());
        }
        Card card = Card.builder()
                .account(account)
                .cardNumber(issued.maskedPan())
                .maskedPan(issued.maskedPan())
                .providerToken(issued.cardToken())
                .providerStatus(issued.status())
                .binPrefix(cardsProviderProperties.binPrefix())
                .currency(account.getCurrency() != null ? account.getCurrency() : "PLN")
                .type(cardType)
                .expiryDate(expiry)
                .transactionLimit(defaultLimit)
                .isBlocked(false)
                .build();
        return cardRepository.save(card);
    }

    private OrderCardResponse toResponse(Card card, IssueCardResponse issued) {
        return new OrderCardResponse(
                card.getId(),
                card.getProviderToken(),
                card.getMaskedPan(),
                issued.fullPan(),
                issued.cvv(),
                issued.expiryMonth(),
                issued.expiryYear(),
                card.getProviderStatus(),
                card.getType()
        );
    }

    private Card loadCardOwnedBy(UUID cardId, String userEmail) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Karta nie znaleziona."));
        if (card.getProviderToken() == null) {
            throw new IllegalStateException("Karta nie jest zarejestrowana u providera kart.");
        }
        if (!card.getAccount().getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new SecurityException("Brak uprawnień do tej karty.");
        }
        return card;
    }

    private Card loadCardOwnedOrManagedBy(UUID cardId, String userEmail) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Karta nie znaleziona."));
        if (card.getProviderToken() == null) {
            throw new IllegalStateException("Karta nie jest zarejestrowana u providera kart.");
        }
        Account account = card.getAccount();
        boolean isOwner = account.getUser().getEmail().equalsIgnoreCase(userEmail);
        boolean isParent = "JUNIOR".equals(account.getType())
                && account.getParentAccount() != null
                && account.getParentAccount().getUser().getEmail().equalsIgnoreCase(userEmail);
        if (!isOwner && !isParent) {
            throw new SecurityException("Brak uprawnień do tej karty.");
        }
        return card;
    }

    private Account findFundingAccountForTopup(Card card, String userEmail) {
        Account cardAccount = card.getAccount();
        if ("JUNIOR".equals(cardAccount.getType()) && cardAccount.getParentAccount() != null
                && cardAccount.getParentAccount().getUser().getEmail().equalsIgnoreCase(userEmail)) {
            return cardAccount.getParentAccount();
        }
        return cardAccount;
    }

    private Account findPrimaryAccount(User user) {
        List<Account> accounts = accountRepository.findByUser_CustomerNumber(user.getCustomerNumber());
        return accounts.stream()
                .filter(a -> !"JUNIOR".equals(a.getType()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Użytkownik nie ma głównego konta."));
    }
}