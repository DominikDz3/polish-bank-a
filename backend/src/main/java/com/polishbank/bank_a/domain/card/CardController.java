package com.polishbank.bank_a.domain.card;

import com.polishbank.bank_a.domain.card.dto.CardPaymentRequest;
import com.polishbank.bank_a.domain.card.dto.CardResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    @PostMapping("/payment")
    public ResponseEntity<?> pay(@Valid @RequestBody CardPaymentRequest request,
                                 Authentication authentication) {
        cardPaymentService.processPayment(request, authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Płatność kartą została zrealizowana."));
    }
}