package com.polishbank.bank_a.domain.card;

import com.polishbank.bank_a.domain.card.dto.CardPaymentRequest;
import com.polishbank.bank_a.domain.card.dto.CardResponse;
import com.polishbank.bank_a.domain.card.dto.OrderCardRequest;
import com.polishbank.bank_a.domain.card.dto.OrderCardResponse;
import com.polishbank.bank_a.domain.card.dto.TopupCardRequest;
import com.polishbank.bank_a.domain.card.dto.UpdateCardLimitsRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;
    private final CardPaymentService cardPaymentService;
    private final CardOrderService cardOrderService;

    @GetMapping
    public ResponseEntity<List<CardResponse>> listMyCards(Authentication authentication) {
        return ResponseEntity.ok(cardService.listMyCards(authentication.getName()));
    }

    @GetMapping("/junior/{juniorAccountId}")
    public ResponseEntity<List<CardResponse>> listJuniorCards(@PathVariable UUID juniorAccountId,
                                                              Authentication authentication) {
        return ResponseEntity.ok(cardService.listForJuniorAccount(juniorAccountId, authentication.getName()));
    }

    @PostMapping("/payment")
    public ResponseEntity<?> pay(@Valid @RequestBody CardPaymentRequest request,
                                 Authentication authentication) {
        CardPaymentService.PaymentResult result = cardPaymentService.processPayment(request, authentication.getName());
        String message = "PENDING_APPROVAL".equals(result.status())
                ? "Płatność została wysłana do zatwierdzenia przez rodzica."
                : "Płatność kartą została zrealizowana.";
        return ResponseEntity.ok(Map.of("message", message, "status", result.status()));
    }

    @PatchMapping("/{cardId}/limits")
    public ResponseEntity<?> updateLimits(@PathVariable UUID cardId,
                                          @Valid @RequestBody UpdateCardLimitsRequest request,
            Authentication authentication) {
        cardService.updateJuniorCardLimits(cardId, request, authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Limity karty zostały zaktualizowane."));
    }
    
    @PostMapping("/{cardId}/dev/force-activate")
    public ResponseEntity<?> devForceActivate(@PathVariable UUID cardId, Authentication authentication) {
        cardOrderService.devForceActivate(cardId, authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Karta przepchnięta do statusu ACTIVE (DEV)."));
    }

    @PostMapping("/order")
    public ResponseEntity<OrderCardResponse> orderCard(@Valid @RequestBody OrderCardRequest request,
                                                       Authentication authentication) {
        return ResponseEntity.ok(cardOrderService.orderForUser(authentication.getName(), request.cardType()));
    }

    @PostMapping("/junior/{juniorAccountId}/order")
    public ResponseEntity<OrderCardResponse> orderJuniorCard(@PathVariable UUID juniorAccountId,
                                                             @Valid @RequestBody OrderCardRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(cardOrderService.orderForJunior(juniorAccountId,
                authentication.getName(), request.cardType()));
    }

    @PostMapping("/{cardId}/block")
    public ResponseEntity<?> block(@PathVariable UUID cardId, Authentication authentication) {
        cardOrderService.blockCard(cardId, authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Karta została zablokowana."));
    }

    @PostMapping("/{cardId}/unblock")
    public ResponseEntity<?> unblock(@PathVariable UUID cardId, Authentication authentication) {
        cardOrderService.unblockCard(cardId, authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Karta została odblokowana."));
    }

    @PostMapping("/{cardId}/activate")
    public ResponseEntity<?> activate(@PathVariable UUID cardId, Authentication authentication) {
        cardOrderService.activateCard(cardId, authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Karta została aktywowana."));
    }

    @PostMapping("/{cardId}/topup")
    public ResponseEntity<?> topup(@PathVariable UUID cardId,
                                   @Valid @RequestBody TopupCardRequest request,
                                   Authentication authentication) {
        BigDecimal newBalance = cardOrderService.topupCard(cardId, authentication.getName(), request.amount());
        return ResponseEntity.ok(Map.of(
                "message", "Karta została doładowana.",
                "newCardBalance", newBalance != null ? newBalance : ""
        ));
    }
}