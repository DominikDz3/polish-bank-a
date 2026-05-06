package com.polishbank.bank_a.domain.transaction;

import com.polishbank.bank_a.domain.transaction.dto.InternalTransferRequest;
import com.polishbank.bank_a.entity.Account;
import com.polishbank.bank_a.entity.Transaction;
import com.polishbank.bank_a.repository.AccountRepository;
import com.polishbank.bank_a.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public void processInternalTransfer(InternalTransferRequest request, String customerNumber) {
        
        Account senderAccount = accountRepository.findById(request.senderAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono konta nadawcy."));

        if (!senderAccount.getUser().getCustomerNumber().equals(customerNumber)) {
            throw new IllegalStateException("Brak uprawnień do tego konta.");
        }

        Account receiverAccount = accountRepository.findByAccountNumber(request.receiverAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono konta odbiorcy w naszym banku."));

        if (senderAccount.getId().equals(receiverAccount.getId())) {
            throw new IllegalArgumentException("Konto nadawcy i odbiorcy nie może być takie samo.");
        }

        if (senderAccount.getBalance().compareTo(request.amount()) < 0) {
            throw new IllegalStateException("Niewystarczające środki na koncie.");
        }

        senderAccount.setBalance(senderAccount.getBalance().subtract(request.amount()));
        receiverAccount.setBalance(receiverAccount.getBalance().add(request.amount()));

        Transaction transaction = Transaction.builder()
                .senderAccount(senderAccount)
                .senderAccountNumber(senderAccount.getAccountNumber())
                .receiverAccount(receiverAccount)
                .receiverAccountNumber(receiverAccount.getAccountNumber())
                .amount(request.amount())
                .currency(senderAccount.getCurrency())
                .title(request.title())
                .status("COMPLETED")
                .type("INTERNAL")
                .createdAt(LocalDateTime.now())
                .executionDate(LocalDateTime.now())
                .build();

        transactionRepository.save(transaction);
    }
}