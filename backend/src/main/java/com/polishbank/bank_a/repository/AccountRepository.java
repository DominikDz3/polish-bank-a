package com.polishbank.bank_a.repository;

import com.polishbank.bank_a.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID>{
    
}
