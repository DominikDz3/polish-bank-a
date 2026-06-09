package com.polishbank.bank_a.domain.klik;

import com.polishbank.bank_a.domain.klik.dto.KlikAuthorizationConfirmRequest;
import com.polishbank.bank_a.domain.klik.dto.KlikPendingAuthorizationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/klik/authorizations")
@RequiredArgsConstructor
public class KlikAuthorizationController {

    private final KlikAuthorizationService authorizationService;

    @GetMapping("/pending")
    public ResponseEntity<List<KlikPendingAuthorizationResponse>> getPending(Authentication authentication) {
        return ResponseEntity.ok(authorizationService.getPendingForUser(authentication.getName()));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<?> confirm(
            @PathVariable UUID id,
            @Valid @RequestBody KlikAuthorizationConfirmRequest request,
            Authentication authentication) {

        if ("ACCEPT".equals(request.status())) {
            if (request.pin() == null || request.pin().isBlank()) {
                throw new IllegalArgumentException("PIN jest wymagany przy akceptacji.");
            }
            authorizationService.confirmWithPin(id, authentication.getName(), request.pin());
        } else {
            authorizationService.confirm(id, authentication.getName(), request.status(), request.reason());
        }

        return ResponseEntity.ok(Map.of("status", "OK"));
    }
}