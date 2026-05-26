package com.polishbank.bank_a.domain.auth;

import com.polishbank.bank_a.domain.auth.dto.AuthResponse;
import com.polishbank.bank_a.domain.auth.dto.LoginRequest;
import com.polishbank.bank_a.domain.auth.dto.RegisterRequest;
import com.polishbank.bank_a.domain.auth.dto.SetPinRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PinService pinService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/pin")
    public ResponseEntity<?> setPin(@Valid @RequestBody SetPinRequest request,
                                    Authentication authentication) {
        pinService.setPin(authentication.getName(), request.pin(), request.confirmPin());
        return ResponseEntity.ok(Map.of("message", "PIN został ustawiony."));
    }
}