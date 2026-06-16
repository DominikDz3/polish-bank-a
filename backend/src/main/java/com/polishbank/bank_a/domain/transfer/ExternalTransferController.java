package com.polishbank.bank_a.domain.transfer;

import com.polishbank.bank_a.domain.transfer.dto.ExternalTransferRequest;
import com.polishbank.bank_a.domain.transfer.dto.ExternalTransferResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transfers/external")
@RequiredArgsConstructor
public class ExternalTransferController {

    private final ExternalTransferService externalTransferService;

    @PostMapping
    public ResponseEntity<ExternalTransferResponse> create(
            @Valid @RequestBody ExternalTransferRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(externalTransferService.createTransfer(request, authentication.getName()));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<ExternalTransferResponse>> history(
            @PathVariable UUID accountId,
            Authentication authentication) {
        return ResponseEntity.ok(externalTransferService.getHistory(accountId, authentication.getName()));
    }
}