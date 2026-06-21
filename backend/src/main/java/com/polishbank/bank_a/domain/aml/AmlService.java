package com.polishbank.bank_a.domain.aml;

import com.polishbank.bank_a.domain.aml.dto.AmlHoldView;
import com.polishbank.bank_a.domain.user.User;
import com.polishbank.bank_a.domain.user.UserRepository;
import com.polishbank.bank_a.entity.AmlHold;
import com.polishbank.bank_a.repository.AmlHoldRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AmlService {

    private final AmlHoldRepository holdRepository;
    private final UserRepository userRepository;
    private final AmlDecisionExecutor executor;

    public List<AmlHoldView> listMyHolds(String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        return holdRepository.findByUser_IdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::toView).toList();
    }

    public long countMyPending(String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        return holdRepository.countByUser_IdAndStatusIn(user.getId(),
                List.of(AmlHoldStatus.AWAITING_EXPLANATION, AmlHoldStatus.AWAITING_DECISION));
    }

    @Transactional
    public AmlHoldView submitExplanation(UUID holdId, String text, String userEmail) {
        AmlHold hold = holdRepository.findById(holdId)
                .orElseThrow(() -> new IllegalArgumentException("Wstrzymanie nie istnieje."));
        if (!hold.getUser().getEmail().equals(userEmail)) {
            throw new SecurityException("Brak dostępu do tego wstrzymania.");
        }
        if (!AmlHoldStatus.AWAITING_EXPLANATION.equals(hold.getStatus())
                && !AmlHoldStatus.AWAITING_DECISION.equals(hold.getStatus())) {
            throw new IllegalStateException("To wstrzymanie zostało już rozpatrzone.");
        }
        hold.setClientExplanation(text);
        hold.setStatus(AmlHoldStatus.AWAITING_DECISION);
        return toView(holdRepository.save(hold));
    }

    public List<AmlHoldView> listPendingForAdmin() {
        return holdRepository.findByStatusInOrderByCreatedAtDesc(
                List.of(AmlHoldStatus.AWAITING_EXPLANATION, AmlHoldStatus.AWAITING_DECISION))
                .stream().map(this::toView).toList();
    }

    public List<AmlHoldView> listAllForAdmin() {
        return holdRepository.findByStatusInOrderByCreatedAtDesc(
                List.of(AmlHoldStatus.AWAITING_EXPLANATION,
                        AmlHoldStatus.AWAITING_DECISION,
                        AmlHoldStatus.APPROVED,
                        AmlHoldStatus.REJECTED))
                .stream().map(this::toView).toList();
    }

    @Transactional
    public AmlHoldView approve(UUID holdId, String adminEmail, String note) {
        AmlHold hold = loadActive(holdId);
        User admin = userRepository.findByEmail(adminEmail).orElseThrow();

        executor.executeApprove(hold);

        hold.setStatus(AmlHoldStatus.APPROVED);
        hold.setDecisionAt(LocalDateTime.now());
        hold.setDecisionBy(admin);
        hold.setDecisionNote(note);
        hold.setReleasedAt(LocalDateTime.now());
        return toView(holdRepository.save(hold));
    }

    @Transactional
    public AmlHoldView reject(UUID holdId, String adminEmail, String note) {
        AmlHold hold = loadActive(holdId);
        User admin = userRepository.findByEmail(adminEmail).orElseThrow();

        executor.executeReject(hold);

        hold.setStatus(AmlHoldStatus.REJECTED);
        hold.setDecisionAt(LocalDateTime.now());
        hold.setDecisionBy(admin);
        hold.setDecisionNote(note);
        hold.setReleasedAt(LocalDateTime.now());
        return toView(holdRepository.save(hold));
    }

    private AmlHold loadActive(UUID holdId) {
        AmlHold hold = holdRepository.findById(holdId)
                .orElseThrow(() -> new IllegalArgumentException("Wstrzymanie nie istnieje."));
        if (AmlHoldStatus.APPROVED.equals(hold.getStatus())
                || AmlHoldStatus.REJECTED.equals(hold.getStatus())) {
            throw new IllegalStateException("Decyzja została już podjęta.");
        }
        return hold;
    }

    private AmlHoldView toView(AmlHold h) {
        return new AmlHoldView(
                h.getId(),
                h.getHoldType(),
                h.getStatus(),
                h.getAmount(),
                h.getCurrency(),
                h.getReceiverInfo(),
                h.getReason(),
                h.getTriggeredRule(),
                h.getClientExplanation(),
                h.getDecisionNote(),
                h.getUser() != null ? h.getUser().getEmail() : null,
                h.getCreatedAt(),
                h.getDecisionAt()
        );
    }
}