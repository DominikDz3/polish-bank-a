package com.polishbank.bank_a.repository;

import com.polishbank.bank_a.entity.ExternalTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExternalTransferRepository extends JpaRepository<ExternalTransfer, UUID> {
    Optional<ExternalTransfer> findByExternalPaymentId(String externalPaymentId);
    List<ExternalTransfer> findByStatusIn(List<String> statuses);
    List<ExternalTransfer> findBySenderAccount_IdOrderByCreatedAtDesc(UUID senderAccountId);
}