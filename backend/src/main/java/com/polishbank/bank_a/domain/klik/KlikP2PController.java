package com.polishbank.bank_a.domain.klik;

import com.polishbank.bank_a.domain.klik.dto.KlikP2PRequest;
import com.polishbank.bank_a.domain.klik.dto.KlikP2PResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/klik/p2p")
@RequiredArgsConstructor
public class KlikP2PController {

    private final KlikP2PService klikP2PService;

    @PostMapping
    public ResponseEntity<KlikP2PResponse> send(
            @Valid @RequestBody KlikP2PRequest req,
            Authentication auth) {
        return ResponseEntity.ok(klikP2PService.send(req, auth.getName()));
    }
}