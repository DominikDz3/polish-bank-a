package com.polishbank.bank_a.repository;

import com.polishbank.bank_a.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
}