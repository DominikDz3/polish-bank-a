package com.polishbank.bank_a.repository;

import com.polishbank.bank_a.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID> {
    Optional<Card> findByProviderToken(String providerToken);
    List<Card> findByAccount_Id(UUID accountId);
}