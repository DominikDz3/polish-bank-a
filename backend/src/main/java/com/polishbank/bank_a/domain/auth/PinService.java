package com.polishbank.bank_a.domain.auth;

import com.polishbank.bank_a.domain.user.User;
import com.polishbank.bank_a.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PinService {

    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final int LOCKOUT_MINUTES = 15;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PinLockoutTracker lockoutTracker;

    @Transactional
    public void setPin(String email, String pin, String confirmPin) {
        if (!pin.equals(confirmPin)) {
            throw new IllegalArgumentException("Wpisane kody PIN nie są identyczne.");
        }
        if (!pin.matches("\\d{4}")) {
            throw new IllegalArgumentException("PIN musi składać się z dokładnie 4 cyfr.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Użytkownik nie znaleziony."));

        if (user.getPinHash() != null) {
            throw new IllegalStateException("PIN został już ustawiony. Zmiana PIN-u nie jest jeszcze obsługiwana.");
        }

        user.setPinHash(passwordEncoder.encode(pin));
        user.setPinFailedAttempts(0);
        user.setPinLockedUntil(null);
        userRepository.save(user);
    }

    public void verifyPin(User user, String pin) {
        if (user.getPinHash() == null) {
            throw new IllegalStateException("Najpierw ustaw kod PIN, aby zatwierdzać operacje finansowe.");
        }
        if (pin == null || !pin.matches("\\d{4}")) {
            throw new IllegalArgumentException("PIN musi składać się z dokładnie 4 cyfr.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (user.getPinLockedUntil() != null && user.getPinLockedUntil().isAfter(now)) {
            long minutesLeft = java.time.Duration.between(now, user.getPinLockedUntil()).toMinutes() + 1;
            throw new IllegalStateException(
                    "PIN został zablokowany po zbyt wielu nieudanych próbach. Spróbuj ponownie za "
                            + minutesLeft + " min.");
        }

        if (!passwordEncoder.matches(pin, user.getPinHash())) {
            lockoutTracker.recordFailure(user.getId(), MAX_FAILED_ATTEMPTS, LOCKOUT_MINUTES);
            int attemptsAfter = user.getPinFailedAttempts() + 1;
            if (attemptsAfter >= MAX_FAILED_ATTEMPTS) {
                throw new IllegalStateException(
                        "Wprowadzono nieprawidłowy PIN " + MAX_FAILED_ATTEMPTS
                                + " razy z rzędu. Konto zostało zablokowane na " + LOCKOUT_MINUTES + " minut.");
            }
            int left = MAX_FAILED_ATTEMPTS - attemptsAfter;
            throw new IllegalArgumentException("Nieprawidłowy PIN. Pozostało prób: " + left + ".");
        }

        lockoutTracker.recordSuccess(user.getId());
    }
}