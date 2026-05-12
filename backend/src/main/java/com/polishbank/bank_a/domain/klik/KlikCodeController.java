package com.polishbank.bank_a.domain.klik;

import com.polishbank.bank_a.domain.klik.dto.KlikCodeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/klik/codes")
@RequiredArgsConstructor
public class KlikCodeController {

    private final KlikCodeService klikCodeService;

    @PostMapping("/generate")
    public ResponseEntity<KlikCodeResponse> generate(
            @RequestParam UUID accountId,
            Authentication authentication) {
        return ResponseEntity.ok(klikCodeService.generateCode(accountId, authentication.getName()));
    }
}