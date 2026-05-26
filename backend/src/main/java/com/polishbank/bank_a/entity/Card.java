package com.polishbank.bank_a.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "cards")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    private String cardNumber;

    @Column(name = "transaction_limit")
    private BigDecimal transactionLimit;

    @Column(name = "daily_limit")
    private BigDecimal dailyLimit;

    private String currency;
    private LocalDate expiryDate;

    private String type;
    private boolean isBlocked;
}