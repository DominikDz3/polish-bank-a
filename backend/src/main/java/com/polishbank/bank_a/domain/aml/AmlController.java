package com.polishbank.bank_a.domain.aml;

import com.polishbank.bank_a.domain.aml.dto.AmlExplanationRequest;
import com.polishbank.bank_a.domain.aml.dto.AmlHoldView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/aml")
@RequiredArgsConstructor
public class AmlController {

    private final AmlService amlService;

    @GetMapping("/holds")
    public ResponseEntity<List<AmlHoldView>> myHolds(Authentication auth) {
        return ResponseEntity.ok(amlService.listMyHolds(auth.getName()));
    }

    @GetMapping("/holds/count")
    public ResponseEntity<Map<String, Long>> myHoldsCount(Authentication auth) {
        return ResponseEntity.ok(Map.of("count", amlService.countMyPending(auth.getName())));
    }

    @PostMapping("/holds/{id}/explanation")
    public ResponseEntity<AmlHoldView> explain(
            @PathVariable UUID id,
            @Valid @RequestBody AmlExplanationRequest req,
            Authentication auth) {
        return ResponseEntity.ok(amlService.submitExplanation(id, req.text(), auth.getName()));
    }
}