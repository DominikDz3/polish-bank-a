package com.polishbank.bank_a.entity;

import com.polishbank.bank_a.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "aml_holds")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmlHold {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @ManyToOne
    @JoinColumn(name = "external_transfer_id")
    private ExternalTransfer externalTransfer;

    @ManyToOne
    @JoinColumn(name = "swift_transfer_id")
    private SwiftTransfer swiftTransfer;

    @Column(name = "hold_type")
    private String holdType;

    private String reason;

    @Column(name = "triggered_rule")
    private String triggeredRule;

    @Column(name = "client_explanation", columnDefinition = "TEXT")
    private String clientExplanation;

    private String status;

    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 3)
    private String currency;

    @Column(name = "receiver_info")
    private String receiverInfo;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "decision_at")
    private LocalDateTime decisionAt;

    @ManyToOne
    @JoinColumn(name = "decision_by")
    private User decisionBy;

    @Column(name = "decision_note", length = 500)
    private String decisionNote;
}