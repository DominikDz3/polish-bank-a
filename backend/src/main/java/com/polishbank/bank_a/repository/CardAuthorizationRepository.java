package com.polishbank.bank_a.repository;

import com.polishbank.bank_a.entity.CardAuthorization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CardAuthorizationRepository extends JpaRepository<CardAuthorization, UUID> {
    Optional<CardAuthorization> findByExternalTransactionId(String externalTransactionId);
    Optional<CardAuthorization> findByAuthorizationCode(String authorizationCode);
}