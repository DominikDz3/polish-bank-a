package com.polishbank.bank_a.integration.cards;

import com.polishbank.bank_a.integration.cards.dto.AuthorizeWebhookRequest;
import com.polishbank.bank_a.integration.cards.dto.AuthorizeWebhookResponse;
import com.polishbank.bank_a.integration.cards.dto.CaptureWebhookRequest;
import com.polishbank.bank_a.integration.cards.dto.CaptureWebhookResponse;
import com.polishbank.bank_a.integration.cards.dto.RefundWebhookRequest;
import com.polishbank.bank_a.integration.cards.dto.RefundWebhookResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks/cards")
@RequiredArgsConstructor
public class CardsCallbackController {

    private final CardsCallbackService cardsCallbackService;

    @PostMapping({"/authorize", "/api/webhooks/cards/authorize"})
    public ResponseEntity<AuthorizeWebhookResponse> authorize(@RequestBody AuthorizeWebhookRequest request) {
        return ResponseEntity.ok(cardsCallbackService.authorize(request));
    }

    @PostMapping({"/capture", "/api/webhooks/cards/capture"})
    public ResponseEntity<CaptureWebhookResponse> capture(@RequestBody CaptureWebhookRequest request) {
        return ResponseEntity.ok(cardsCallbackService.capture(request));
    }

    @PostMapping({"/refund", "/api/webhooks/cards/refund"})
    public ResponseEntity<RefundWebhookResponse> refund(@RequestBody RefundWebhookRequest request) {
        return ResponseEntity.ok(cardsCallbackService.refund(request));
    }
}