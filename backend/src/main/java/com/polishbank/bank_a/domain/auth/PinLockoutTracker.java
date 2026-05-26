package com.polishbank.bank_a.domain.auth;

import com.polishbank.bank_a.domain.user.User;
import com.polishbank.bank_a.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PinLockoutTracker {

    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LocalDateTime recordFailure(UUID userId, int maxAttempts, int lockoutMinutes) {
        User user = userRepository.findById(userId).orElseThrow();
        int attempts = user.getPinFailedAttempts() + 1;
        if (attempts >= maxAttempts) {
            LocalDateTime lockedUntil = LocalDateTime.now().plusMinutes(lockoutMinutes);
            user.setPinLockedUntil(lockedUntil);
            user.setPinFailedAttempts(0);
            userRepository.save(user);
            return lockedUntil;
        }
        user.setPinFailedAttempts(attempts);
        userRepository.save(user);
        return null;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();
        if (user.getPinFailedAttempts() != 0 || user.getPinLockedUntil() != null) {
            user.setPinFailedAttempts(0);
            user.setPinLockedUntil(null);
            userRepository.save(user);
        }
    }
}