package com.polishbank.bank_a.domain.auth;

import com.polishbank.bank_a.domain.auth.dto.AuthResponse;
import com.polishbank.bank_a.domain.auth.dto.LoginRequest;
import com.polishbank.bank_a.domain.auth.dto.RegisterRequest;
import com.polishbank.bank_a.domain.user.User;
import com.polishbank.bank_a.domain.user.UserRepository;
import com.polishbank.bank_a.domain.user.UserRole;
import com.polishbank.bank_a.entity.Account;
import com.polishbank.bank_a.repository.AccountRepository;
import com.polishbank.bank_a.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String BANK_CODE = "88880000";
    private static final String COUNTRY_CODE_NUMERIC = "2521";

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
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

        Account account = Account.builder()
                .user(user)
                .accountNumber(generateAccountNumber())
                .balance(BigDecimal.ZERO)
                .blockedFunds(BigDecimal.ZERO)
                .currency("PLN")
                .type("STANDARD")
                .build();
        accountRepository.save(account);

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getCustomerNumber());
        return new AuthResponse(token, user.getEmail(), user.getRole().name(),
                user.getCustomerNumber(), user.getPinHash() != null);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByCustomerNumber(request.customerNumber())
                .orElseThrow(() -> new IllegalArgumentException("Nieprawidłowy numer klienta lub hasło"));
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getEmail(), request.password())
        );
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getCustomerNumber());
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

    private String generateAccountNumber() {
        Random random = new Random();
        String iban;
        do {
            StringBuilder bban = new StringBuilder(BANK_CODE); // 8 cyfr kodu banku
            for (int i = 0; i < 16; i++) {                     // 16 cyfr numeru konta
                bban.append(random.nextInt(10));
            }
            String checkDigits = computeIbanCheckDigits(bban.toString());
            iban = "PL" + checkDigits + bban;
        } while (accountRepository.findByAccountNumber(iban).isPresent());
        return iban;
    }

    private String computeIbanCheckDigits(String bban) {
        String rearranged = bban + COUNTRY_CODE_NUMERIC + "00";
        int mod = new BigInteger(rearranged).mod(BigInteger.valueOf(97)).intValue();
        int check = 98 - mod;
        return String.format("%02d", check);
    }
}