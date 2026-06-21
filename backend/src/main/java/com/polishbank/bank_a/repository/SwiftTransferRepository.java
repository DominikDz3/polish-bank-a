package com.polishbank.bank_a.repository;

import com.polishbank.bank_a.entity.SwiftTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SwiftTransferRepository extends JpaRepository<SwiftTransfer, UUID> {

    Optional<SwiftTransfer> findByUetr(String uetr);

    List<SwiftTransfer> findAllByStatusAndCreatedAtBefore(String status, LocalDateTime cutoff);
}