package com.polishbank.bank_a.domain.junior;

import com.polishbank.bank_a.domain.junior.dto.PendingApprovalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/junior/pending-approvals")
@RequiredArgsConstructor
public class PendingApprovalController {

    private final PendingApprovalService service;

    @GetMapping
    public ResponseEntity<List<PendingApprovalResponse>> list(Authentication auth) {
        return ResponseEntity.ok(service.listForParent(auth.getName()));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> count(Authentication auth) {
        return ResponseEntity.ok(Map.of("count", service.countPendingForParent(auth.getName())));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable UUID id, Authentication auth) {
        service.approve(id, auth.getName());
        return ResponseEntity.ok(Map.of("message", "Transakcja została zatwierdzona."));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable UUID id, Authentication auth) {
        service.reject(id, auth.getName());
        return ResponseEntity.ok(Map.of("message", "Transakcja została odrzucona."));
    }
}