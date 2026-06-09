package com.polishbank.bank_a.repository;

import com.polishbank.bank_a.entity.KlikAuthorization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KlikAuthorizationRepository extends JpaRepository<KlikAuthorization, UUID> {
    Optional<KlikAuthorization> findByKlikTransactionId(UUID klikTransactionId);
    List<KlikAuthorization> findByUser_IdAndStatus(UUID userId, String status);
}