package com.polishbank.bank_a.domain.account;

import com.polishbank.bank_a.domain.account.dto.AccountSummaryResponse;
import com.polishbank.bank_a.domain.user.User;
import com.polishbank.bank_a.domain.user.UserRepository; // Dodajemy to repozytorium
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<AccountSummaryResponse>> getMyAccounts(Authentication authentication) {
        
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Użytkownik nie znaleziony w bazie danych!"));

        String customerNumber = user.getCustomerNumber();

        List<AccountSummaryResponse> accounts = accountService.getUserAccountSummary(customerNumber);
        
        return ResponseEntity.ok(accounts);
    }
}