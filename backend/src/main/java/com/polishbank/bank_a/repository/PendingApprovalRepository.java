package com.polishbank.bank_a.repository;

import com.polishbank.bank_a.entity.PendingApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface PendingApprovalRepository extends JpaRepository<PendingApproval, UUID> {
}