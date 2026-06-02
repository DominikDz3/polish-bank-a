package com.polishbank.bank_a.repository;

import com.polishbank.bank_a.entity.PendingApproval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PendingApprovalRepository extends JpaRepository<PendingApproval, UUID> {

    List<PendingApproval> findByParentUser_IdAndStatusOrderByCreatedAtDesc(UUID parentUserId, String status);

    long countByParentUser_IdAndStatus(UUID parentUserId, String status);
}