package com.polishbank.bank_a.domain.swift;

import com.polishbank.bank_a.domain.swift.dto.SwiftTransferRequest;
import com.polishbank.bank_a.domain.swift.dto.SwiftTransferResponse;
import com.polishbank.bank_a.integration.swift.SwiftExchangeRateProvider;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions/swift")
@RequiredArgsConstructor
public class SwiftController {

    private final SwiftService swiftService;
    private final SwiftExchangeRateProvider exchangeRateProvider;

    @PostMapping
    public ResponseEntity<SwiftTransferResponse> send(
            @Valid @RequestBody SwiftTransferRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(swiftService.send(request, authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<SwiftTransferResponse>> list(Authentication authentication) {
        return ResponseEntity.ok(swiftService.listForUser(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SwiftTransferResponse> get(
            @PathVariable UUID id,
            Authentication authentication) {
        return ResponseEntity.ok(swiftService.getById(id, authentication.getName()));
    }

        @GetMapping("/quote")
    public ResponseEntity<java.util.Map<String, Object>> quote(
            @org.springframework.web.bind.annotation.RequestParam java.math.BigDecimal amount,
            @org.springframework.web.bind.annotation.RequestParam String from,
            @org.springframework.web.bind.annotation.RequestParam String to) {
        java.math.BigDecimal converted = exchangeRateProvider.convert(amount, from, to);
        return ResponseEntity.ok(java.util.Map.of(
                "fromCurrency", from.toUpperCase(),
                "toCurrency", to.toUpperCase(),
                "amount", amount,
                "converted", converted
        ));
    }
}