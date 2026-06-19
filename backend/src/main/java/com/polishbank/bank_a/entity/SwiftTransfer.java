package com.polishbank.bank_a.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "swift_transfers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SwiftTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false, unique = true)
    private Transaction transaction;

    @Column(unique = true)
    private String uetr;

    private String messageId;
    private String instructionId;

    private String senderBic;
    private String receiverBic;
    private String receiverCountry;
    private String receiverIban;

    private String chargeBearer;
    private String chargeBearerInput;

    @Column(columnDefinition = "TEXT")
    private String route;

    private BigDecimal feeTotal;
    private BigDecimal feeSender;
    private BigDecimal feeReceiver;
    private BigDecimal feeIntermediary;

    private BigDecimal estimatedSeconds;

    private String status;
    private String returnReason;

    private LocalDateTime deliveredAt;
    private LocalDateTime returnedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}