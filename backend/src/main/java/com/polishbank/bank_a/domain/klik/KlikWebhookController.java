package com.polishbank.bank_a.domain.klik;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.polishbank.bank_a.domain.klik.dto.KlikWebhookAuthorizeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks/klik")
@RequiredArgsConstructor
public class KlikWebhookController {

    private final KlikAuthorizationService authService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/authorize")
    public ResponseEntity<?> authorize(@RequestBody Map<String, Object> raw) throws Exception {
        System.out.println(">>> KLIK WEBHOOK RAW PAYLOAD: " + raw);
        
        KlikWebhookAuthorizeRequest request = objectMapper.convertValue(raw, KlikWebhookAuthorizeRequest.class);
        authService.receiveAuthorization(request);
        
        return ResponseEntity.ok(Map.of(
                "received", true,
                "will_prompt_user", true
        ));
    }

    @PostMapping("/ping")
    public ResponseEntity<?> ping(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(Map.of(
                "timestamp", body.get("timestamp"),
                "nonce", body.get("nonce"),
                "pong", true
        ));
    }
}