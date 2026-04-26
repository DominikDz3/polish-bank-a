package com.polishbank.bank_a.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pending_approvals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingApproval {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "junior_account_id", nullable = false)
    private Account juniorAccount;

    @ManyToOne
    @JoinColumn(name = "parent_user_id", nullable = false)
    private User parentUser;

    @ManyToOne
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    private BigDecimal amount;
    private String description;
    private String status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;
}