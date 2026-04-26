package com.polishbank.bank_a.repository;

import com.polishbank.bank_a.entity.AmlHold;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AmlHoldRepository extends JpaRepository<AmlHold, UUID> {
}