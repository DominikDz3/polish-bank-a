package com.polishbank.bank_a.domain.transaction;

import com.polishbank.bank_a.domain.auth.PinService;
import com.polishbank.bank_a.domain.transaction.dto.InternalTransferRequest;
import com.polishbank.bank_a.domain.transaction.dto.TransactionResponse;
import com.polishbank.bank_a.domain.user.User;
import com.polishbank.bank_a.domain.user.UserRepository;
import com.polishbank.bank_a.entity.Account;
import com.polishbank.bank_a.entity.PendingApproval;
import com.polishbank.bank_a.entity.Transaction;
import com.polishbank.bank_a.repository.AccountRepository;
import com.polishbank.bank_a.repository.PendingApprovalRepository;
import com.polishbank.bank_a.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final PendingApprovalRepository pendingApprovalRepository;
    private final PinService pinService;

    @Transactional
    public String processInternalTransfer(InternalTransferRequest request, String customerNumber) {
        Account senderAccount = accountRepository.findById(request.senderAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono konta nadawcy."));

        if (!senderAccount.getUser().getCustomerNumber().equals(customerNumber)) {
            throw new IllegalStateException("Brak uprawnień do tego konta.");
        }

        pinService.verifyPin(senderAccount.getUser(), request.pin());

        Account receiverAccount = accountRepository.findByAccountNumber(request.receiverAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono konta odbiorcy w naszym banku."));

        if (senderAccount.getId().equals(receiverAccount.getId())) {
            throw new IllegalArgumentException("Konto nadawcy i odbiorcy nie może być takie samo.");
        }

        if (senderAccount.getBalance().compareTo(request.amount()) < 0) {
            throw new IllegalStateException("Niewystarczające środki na koncie.");
        }

        boolean juniorTransfer = "JUNIOR".equals(senderAccount.getType());

        Transaction transaction = Transaction.builder()
                .senderAccount(senderAccount)
                .senderAccountNumber(senderAccount.getAccountNumber())
                .receiverAccount(receiverAccount)
                .receiverAccountNumber(receiverAccount.getAccountNumber())
                .amount(request.amount())
                .currency(senderAccount.getCurrency())
                .title(request.title())
                .type("INTERNAL")
                .build();

        if (juniorTransfer) {
            Account parentAccount = senderAccount.getParentAccount();
            if (parentAccount == null) {
                throw new IllegalStateException("Konto Junior musi być podpięte do konta rodzica.");
            }

            transaction.setStatus("PENDING_APPROVAL");
            transactionRepository.save(transaction);

            PendingApproval pa = PendingApproval.builder()
                    .juniorAccount(senderAccount)
                    .parentUser(parentAccount.getUser())
                    .transaction(transaction)
                    .amount(request.amount())
                    .description("Przelew: " + request.title())
                    .status("PENDING")
                    .build();
            pendingApprovalRepository.save(pa);

            return "PENDING_APPROVAL";
        }

        senderAccount.setBalance(senderAccount.getBalance().subtract(request.amount()));
        receiverAccount.setBalance(receiverAccount.getBalance().add(request.amount()));

        transaction.setStatus("COMPLETED");
        transaction.setExecutionDate(LocalDateTime.now(ZONE));
        transactionRepository.save(transaction);

        return "COMPLETED";
    }

    public List<TransactionResponse> getHistory(UUID accountId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Użytkownik nie znaleziony."));

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Konto nie znalezione."));

        boolean isOwner = account.getUser().getId().equals(user.getId());
        boolean isParentOfJunior = "JUNIOR".equals(account.getType())
                && account.getParentAccount() != null
                && account.getParentAccount().getUser().getId().equals(user.getId());

        if (!isOwner && !isParentOfJunior) {
            throw new SecurityException("Brak dostępu do historii tego konta.");
        }

        return transactionRepository.findAllByAccountId(accountId)
                .stream()
                .map(t -> {
                    String direction = t.getSenderAccount() != null
                            && accountId.equals(t.getSenderAccount().getId())
                            ? "OUTGOING" : "INCOMING";
                    return new TransactionResponse(
                            t.getId(),
                            t.getSenderAccountNumber(),
                            t.getReceiverAccountNumber(),
                            t.getReceiverName(),
                            t.getTitle(),
                            t.getAmount(),
                            t.getCurrency(),
                            t.getStatus(),
                            t.getType(),
                            t.getCreatedAt(),
                            t.getExecutionDate(),
                            direction
                    );
                })
                .collect(Collectors.toList());
    }
}