package com.polishbank.bank_a.repository;

import com.polishbank.bank_a.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    @Query("SELECT t FROM Transaction t WHERE t.senderAccount.id = :accountId OR t.receiverAccount.id = :accountId ORDER BY t.createdAt DESC")
    List<Transaction> findAllByAccountId(@Param("accountId") UUID accountId);

    @Query("""
       SELECT COALESCE(SUM(t.amount), 0)
       FROM Transaction t
       WHERE t.card.id = :cardId
         AND t.status = 'COMPLETED'
         AND t.executionDate >= :startOfDay
         AND t.executionDate < :startOfNextDay
       """)
    BigDecimal sumCardSpentInRange(
        @Param("cardId") UUID cardId,
        @Param("startOfDay") LocalDateTime startOfDay,
        @Param("startOfNextDay") LocalDateTime startOfNextDay);
}