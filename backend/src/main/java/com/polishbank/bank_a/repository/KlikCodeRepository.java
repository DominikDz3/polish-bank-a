package com.polishbank.bank_a.repository;

import com.polishbank.bank_a.entity.KlikCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface KlikCodeRepository extends JpaRepository<KlikCode, UUID> {
}