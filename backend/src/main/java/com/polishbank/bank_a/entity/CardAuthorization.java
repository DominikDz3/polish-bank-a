package com.polishbank.bank_a.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "card_authorizations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardAuthorization {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "authorization_code", nullable = false, unique = true)
    private String authorizationCode;

    @Column(name = "external_transaction_id", nullable = false, unique = true)
    private String externalTransactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id")
    private Card card;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "merchant_name")
    private String merchantName;

    @Column(nullable = false, length = 20)
    private String status; // HELD / SETTLED / REFUNDED / EXPIRED

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = "HELD";
    }
}