package com.polishbank.bank_a.entity;

import jakarta.persistence.*;
import lombok.*;
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
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    private String reason;
    private String clientExplanation;
    private String status;
    private String createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime releasedAt;
}