package com.polishbank.bank_a.domain.auth;

import com.polishbank.bank_a.domain.auth.dto.AuthResponse;
import com.polishbank.bank_a.domain.auth.dto.LoginRequest;
import com.polishbank.bank_a.domain.auth.dto.RegisterRequest;
import com.polishbank.bank_a.domain.user.User;
import com.polishbank.bank_a.domain.user.UserRepository;
import com.polishbank.bank_a.domain.user.UserRole;
import com.polishbank.bank_a.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email już jest w użyciu");
        }
        User user = User.builder()
                .customerNumber(generateCustomerNumber())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.CUSTOMER)
                .build();
        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getEmail(), user.getRole().name(),
                user.getCustomerNumber(), user.getPinHash() != null);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByCustomerNumber(request.customerNumber())
                .orElseThrow(() -> new IllegalArgumentException("Nieprawidłowy numer klienta lub hasło"));
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getEmail(), request.password())
        );
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getEmail(), user.getRole().name(),
                user.getCustomerNumber(), user.getPinHash() != null);
    }

    private String generateCustomerNumber() {
        String number;
        Random random = new Random();
        do {
            number = String.format("%08d", random.nextInt(100_000_000));
        } while (userRepository.existsByCustomerNumber(number));
        return number;
    }
}