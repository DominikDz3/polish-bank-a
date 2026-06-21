package com.polishbank.bank_a.domain.aml;

import com.polishbank.bank_a.domain.aml.dto.AmlDecisionRequest;
import com.polishbank.bank_a.domain.aml.dto.AmlHoldView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/aml/admin")
@RequiredArgsConstructor
public class AmlAdminController {

    private final AmlService amlService;

    @GetMapping("/holds/pending")
    public ResponseEntity<List<AmlHoldView>> listPending() {
        return ResponseEntity.ok(amlService.listPendingForAdmin());
    }

    @GetMapping("/holds")
    public ResponseEntity<List<AmlHoldView>> listAll() {
        return ResponseEntity.ok(amlService.listAllForAdmin());
    }

    @PostMapping("/holds/{id}/approve")
    public ResponseEntity<AmlHoldView> approve(
            @PathVariable UUID id,
            @Valid @RequestBody AmlDecisionRequest req,
            Authentication auth) {
        return ResponseEntity.ok(amlService.approve(id, auth.getName(), req.note()));
    }

    @PostMapping("/holds/{id}/reject")
    public ResponseEntity<AmlHoldView> reject(
            @PathVariable UUID id,
            @Valid @RequestBody AmlDecisionRequest req,
            Authentication auth) {
        return ResponseEntity.ok(amlService.reject(id, auth.getName(), req.note()));
    }
}