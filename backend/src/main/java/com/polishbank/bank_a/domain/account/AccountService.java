package com.polishbank.bank_a.domain.account;

import com.polishbank.bank_a.domain.account.dto.AccountSummaryResponse;
import com.polishbank.bank_a.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    public List<AccountSummaryResponse> getUserAccountSummary(String customerNumber) {
        return accountRepository.findByUser_CustomerNumber(customerNumber)
                .stream()
                .map(account -> new AccountSummaryResponse(
                        account.getId(),
                        account.getAccountNumber(),
                        account.getBalance(),
                        account.getBlockedFunds(),
                        account.getCurrency(),
                        account.getType()
                ))
                .collect(Collectors.toList());
    }
}
