package com.polishbank.bank_a.repository;

import com.polishbank.bank_a.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>{
    
}
