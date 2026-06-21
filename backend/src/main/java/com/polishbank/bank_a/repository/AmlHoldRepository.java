package com.polishbank.bank_a.repository;

import com.polishbank.bank_a.entity.AmlHold;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AmlHoldRepository extends JpaRepository<AmlHold, UUID> {
    List<AmlHold> findByUser_IdOrderByCreatedAtDesc(UUID userId);
    List<AmlHold> findByStatusInOrderByCreatedAtDesc(List<String> statuses);
    long countByUser_IdAndStatusIn(UUID userId, List<String> statuses);
}