package com.polishbank.bank_a.domain.aml;

import com.polishbank.bank_a.domain.aml.dto.AmlHoldView;
import com.polishbank.bank_a.domain.user.User;
import com.polishbank.bank_a.domain.user.UserRepository;
import com.polishbank.bank_a.entity.*;
import com.polishbank.bank_a.repository.AmlHoldRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AmlService {

    private final AmlHoldRepository holdRepository;
    private final UserRepository userRepository;

    @Transactional
    public AmlHold createHold(
            User user,
            Account account,
            AmlTransactionType type,
            Transaction transaction,
            ExternalTransfer externalTransfer,
            SwiftTransfer swiftTransfer,
            AmlResult evaluation,
            BigDecimal amount,
            String currency,
            String receiverInfo
    ) {
        AmlHold hold = AmlHold.builder()
                .user(user)
                .account(account)
                .holdType(type.name())
                .transaction(transaction)
                .externalTransfer(externalTransfer)
                .swiftTransfer(swiftTransfer)
                .reason(evaluation.reason())
                .triggeredRule(evaluation.ruleCode())
                .amount(amount)
                .currency(currency)
                .receiverInfo(receiverInfo)
                .status(AmlHoldStatus.AWAITING_EXPLANATION)
                .createdBy("AML_ENGINE")
                .build();
        return holdRepository.save(hold);
    }

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