package com.polishbank.bank_a.domain.junior;

import com.polishbank.bank_a.domain.junior.dto.CreateJuniorRequest;
import com.polishbank.bank_a.domain.junior.dto.JuniorResponse;
import com.polishbank.bank_a.domain.user.User;
import com.polishbank.bank_a.domain.user.UserRepository;
import com.polishbank.bank_a.domain.user.UserRole;
import com.polishbank.bank_a.entity.Account;
import com.polishbank.bank_a.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class JuniorAccountService {

    private static final int MIN_AGE = 7;
    private static final int MAX_AGE = 13;
    private static final String BANK_PREFIX = "PL45888800";

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public JuniorResponse createJunior(CreateJuniorRequest request, String parentEmail) {
        User parent = userRepository.findByEmail(parentEmail)
                .orElseThrow(() -> new IllegalArgumentException("Rodzic nie znaleziony."));

        if (parent.getRole() == UserRole.JUNIOR) {
            throw new SecurityException("Konto Junior nie może zakładać własnych subkont.");
        }

        int age = Period.between(request.dateOfBirth(), LocalDate.now()).getYears();
        if (age < MIN_AGE || age > MAX_AGE) {
            throw new IllegalArgumentException(
                    "Konto Junior jest przeznaczone dla dzieci w wieku " + MIN_AGE + "-" + MAX_AGE + " lat.");
        }

        Account parentAccount = accountRepository.findById(request.parentAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Wskazane konto rodzica nie istnieje."));

        if (!parentAccount.getUser().getId().equals(parent.getId())) {
            throw new SecurityException("Wskazane konto nie należy do Ciebie.");
        }
        if ("JUNIOR".equals(parentAccount.getType())) {
            throw new IllegalArgumentException("Konto Junior nie może być rodzicem innego konta Junior.");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email jest już w użyciu.");
        }

        User junior = User.builder()
                .customerNumber(generateCustomerNumber())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .dateOfBirth(request.dateOfBirth())
                .role(UserRole.JUNIOR)
                .build();
        userRepository.save(junior);

        Account juniorAccount = Account.builder()
                .user(junior)
                .accountNumber(generateAccountNumber())
                .balance(BigDecimal.ZERO)
                .blockedFunds(BigDecimal.ZERO)
                .currency(parentAccount.getCurrency())
                .type("JUNIOR")
                .parentAccount(parentAccount)
                .build();
        accountRepository.save(juniorAccount);

        if (parent.getRole() == UserRole.CUSTOMER) {
            parent.setRole(UserRole.PARENT);
            userRepository.save(parent);
        }

        return toResponse(junior, juniorAccount, parentAccount);
    }

    public List<JuniorResponse> listJuniors(String parentEmail) {
        User parent = userRepository.findByEmail(parentEmail)
                .orElseThrow(() -> new IllegalArgumentException("Rodzic nie znaleziony."));

        return accountRepository.findByParentAccount_User_Id(parent.getId()).stream()
                .filter(a -> "JUNIOR".equals(a.getType()))
                .map(a -> toResponse(a.getUser(), a, a.getParentAccount()))
                .toList();
    }

    private JuniorResponse toResponse(User junior, Account juniorAccount, Account parentAccount) {
        return new JuniorResponse(
                junior.getId(),
                junior.getCustomerNumber(),
                junior.getEmail(),
                junior.getFirstName(),
                junior.getLastName(),
                junior.getDateOfBirth(),
                juniorAccount.getId(),
                juniorAccount.getAccountNumber(),
                juniorAccount.getBalance(),
                juniorAccount.getCurrency(),
                parentAccount.getAccountNumber()
        );
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
        String acc;
        do {
            StringBuilder digits = new StringBuilder();
            for (int i = 0; i < 16; i++) digits.append(random.nextInt(10));
            acc = BANK_PREFIX + digits;
        } while (accountRepository.findByAccountNumber(acc).isPresent());
        return acc;
    }
}