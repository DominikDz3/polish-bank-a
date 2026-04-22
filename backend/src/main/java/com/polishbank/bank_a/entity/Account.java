package com.polishbank.bank_a.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String accountNumber;
    private BigDecimal balance;
    private String currency;
    private String type;

    @ManyToOne
    @JoinColumn(name = "parent_account_id")
    private Account parentAccount;
}