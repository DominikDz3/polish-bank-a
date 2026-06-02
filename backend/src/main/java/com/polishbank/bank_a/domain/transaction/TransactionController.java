package com.polishbank.bank_a.domain.transaction;

import com.polishbank.bank_a.domain.transaction.dto.InternalTransferRequest;
import com.polishbank.bank_a.domain.transaction.dto.TransactionResponse;
import com.polishbank.bank_a.domain.user.User;
import com.polishbank.bank_a.domain.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final UserRepository userRepository;

    @PostMapping("/internal")
    public ResponseEntity<?> sendInternalTransfer(
        @Valid @RequestBody InternalTransferRequest request,
        Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new IllegalStateException("Użytkownik nie znaleziony."));
        String status = transactionService.processInternalTransfer(request, user.getCustomerNumber());
        String message = "PENDING_APPROVAL".equals(status)
            ? "Przelew został wysłany do zatwierdzenia przez rodzica."
            : "Przelew został zrealizowany pomyślnie.";
        return ResponseEntity.ok(Map.of("message", message, "status", status));
    }

    @GetMapping("/history/{accountId}")
    public ResponseEntity<List<TransactionResponse>> getHistory(
            @PathVariable UUID accountId,
            Authentication authentication) {
        return ResponseEntity.ok(transactionService.getHistory(accountId, authentication.getName()));
    }
}