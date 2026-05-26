package com.polishbank.bank_a.domain.junior;

import com.polishbank.bank_a.domain.junior.dto.CreateJuniorRequest;
import com.polishbank.bank_a.domain.junior.dto.JuniorResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/junior")
@RequiredArgsConstructor
public class JuniorAccountController {

    private final JuniorAccountService juniorAccountService;

    @PostMapping
    public ResponseEntity<JuniorResponse> create(
            @Valid @RequestBody CreateJuniorRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(juniorAccountService.createJunior(request, authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<JuniorResponse>> listMyJuniors(Authentication authentication) {
        return ResponseEntity.ok(juniorAccountService.listJuniors(authentication.getName()));
    }
}