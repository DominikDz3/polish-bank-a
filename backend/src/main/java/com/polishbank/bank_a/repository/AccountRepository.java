package com.polishbank.bank_a.repository;

import com.polishbank.bank_a.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID>{
    List<Account> findByUser_CustomerNumber(String customerNumber);
}
