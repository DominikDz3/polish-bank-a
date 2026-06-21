package com.polishbank.bank_a.repository;

import com.polishbank.bank_a.entity.KlikAlias;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KlikAliasRepository extends JpaRepository<KlikAlias, UUID> {
    List<KlikAlias> findByUser_IdAndActiveTrueOrderByCreatedAtDesc(UUID userId);
    Optional<KlikAlias> findByAliasAndActiveTrue(String alias);
}