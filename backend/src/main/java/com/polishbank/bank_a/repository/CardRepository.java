package com.polishbank.bank_a.repository;

import com.polishbank.bank_a.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID> {
}