package com.polishbank.bank_a.entity;

import com.polishbank.bank_a.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "klik_authorizations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KlikAuthorization {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "klik_transaction_id", nullable = false, unique = true)
    private UUID klikTransactionId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne
    @JoinColumn(name = "klik_code_id")
    private KlikCode klikCode;

    private BigDecimal amount;
    private String currency;

    @Column(name = "merchant_name")
    private String merchantName;

    @Column(name = "is_on_us", nullable = false)
    @Builder.Default
    private boolean isOnUs = false;

    @Column(nullable = false)
    private String status;

    @Column(name = "reject_reason")
    private String rejectReason;

    @Column(name = "expiry_time", nullable = false)
    private LocalDateTime expiryTime;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}