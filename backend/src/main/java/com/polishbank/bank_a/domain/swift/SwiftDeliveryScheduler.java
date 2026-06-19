package com.polishbank.bank_a.domain.swift;

import com.polishbank.bank_a.entity.SwiftTransfer;
import com.polishbank.bank_a.entity.Transaction;
import com.polishbank.bank_a.repository.SwiftTransferRepository;
import com.polishbank.bank_a.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
@Slf4j
public class SwiftDeliveryScheduler {

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");
    private static final BigDecimal DELIVERY_BUFFER_SECONDS = new BigDecimal("5");
    private static final BigDecimal DEFAULT_ETA_SECONDS = new BigDecimal("15");

    private final SwiftTransferRepository swiftTransferRepository;
    private final TransactionRepository transactionRepository;

    @Scheduled(fixedDelayString = "${integration.swift.delivery-scheduler-ms:5000}")
    @Transactional
    public void markDelivered() {
        LocalDateTime now = LocalDateTime.now(ZONE);
        var inTransit = swiftTransferRepository.findAllByStatusAndCreatedAtBefore("IN_TRANSIT", now);
        for (SwiftTransfer swift : inTransit) {
            if (swift.getCreatedAt() == null) {
                continue;
            }
            BigDecimal eta = swift.getEstimatedSeconds() == null
                    ? DEFAULT_ETA_SECONDS
                    : swift.getEstimatedSeconds();
            long deliverAfterSeconds = eta.add(DELIVERY_BUFFER_SECONDS).longValue();
            LocalDateTime deliverAt = swift.getCreatedAt().plusSeconds(deliverAfterSeconds);
            if (now.isBefore(deliverAt)) {
                continue;
            }
            swift.setStatus("DELIVERED");
            swift.setDeliveredAt(now);
            Transaction tx = swift.getTransaction();
            if (tx != null) {
                tx.setStatus("DELIVERED");
                tx.setExecutionDate(now);
                transactionRepository.save(tx);
            }
            swiftTransferRepository.save(swift);
            log.info("[SWIFT_DELIVERED] UETR={} po {}s", swift.getUetr(), deliverAfterSeconds);
        }
    }
}