package com.polishbank.bank_a.repository;

import com.polishbank.bank_a.entity.KlikCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KlikCodeRepository extends JpaRepository<KlikCode, UUID> {
    List<KlikCode> findByUser_IdAndStatus(UUID userId, String status);
    Optional<KlikCode> findByCode(String code);
}