package com.polishbank.bank_a.domain.card;

import com.polishbank.bank_a.domain.card.dto.CardPaymentRequest;
import com.polishbank.bank_a.domain.card.dto.CardResponse;
import com.polishbank.bank_a.domain.card.dto.UpdateCardLimitsRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;
    private final CardPaymentService cardPaymentService;

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
}