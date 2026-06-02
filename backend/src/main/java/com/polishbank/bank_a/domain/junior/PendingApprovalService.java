package com.polishbank.bank_a.domain.junior;

import com.polishbank.bank_a.domain.junior.dto.PendingApprovalResponse;
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

@Service
@RequiredArgsConstructor
public class PendingApprovalService {

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");

    private final PendingApprovalRepository pendingApprovalRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public List<PendingApprovalResponse> listForParent(String parentEmail) {
        User parent = userRepository.findByEmail(parentEmail)
                .orElseThrow(() -> new IllegalArgumentException("Użytkownik nie znaleziony."));

        return pendingApprovalRepository
                .findByParentUser_IdAndStatusOrderByCreatedAtDesc(parent.getId(), "PENDING")
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public long countPendingForParent(String parentEmail) {
        User parent = userRepository.findByEmail(parentEmail)
                .orElseThrow(() -> new IllegalArgumentException("Użytkownik nie znaleziony."));
        return pendingApprovalRepository.countByParentUser_IdAndStatus(parent.getId(), "PENDING");
    }

    @Transactional
    public void approve(UUID approvalId, String parentEmail) {
        PendingApproval pa = loadOwnedByParent(approvalId, parentEmail);

        Transaction tx = pa.getTransaction();
        if (tx == null) {
            throw new IllegalStateException("Brak transakcji powiązanej z akceptacją.");
        }

        Account juniorAccount = pa.getJuniorAccount();
        if (juniorAccount.getBalance().compareTo(pa.getAmount()) < 0) {
            tx.setStatus("REJECTED");
            transactionRepository.save(tx);
            pa.setStatus("REJECTED");
            pa.setResolvedAt(LocalDateTime.now(ZONE));
            pendingApprovalRepository.save(pa);
            throw new IllegalStateException("Niewystarczające środki na koncie dziecka — transakcja została odrzucona.");
        }

        juniorAccount.setBalance(juniorAccount.getBalance().subtract(pa.getAmount()));
        accountRepository.save(juniorAccount);

        if (tx.getReceiverAccount() != null) {
            Account receiver = tx.getReceiverAccount();
            receiver.setBalance(receiver.getBalance().add(pa.getAmount()));
            accountRepository.save(receiver);
        }

        tx.setStatus("COMPLETED");
        tx.setExecutionDate(LocalDateTime.now(ZONE));
        transactionRepository.save(tx);

        pa.setStatus("APPROVED");
        pa.setResolvedAt(LocalDateTime.now(ZONE));
        pendingApprovalRepository.save(pa);
    }

    @Transactional
    public void reject(UUID approvalId, String parentEmail) {
        PendingApproval pa = loadOwnedByParent(approvalId, parentEmail);

        Transaction tx = pa.getTransaction();
        if (tx != null) {
            tx.setStatus("REJECTED");
            transactionRepository.save(tx);
        }

        pa.setStatus("REJECTED");
        pa.setResolvedAt(LocalDateTime.now(ZONE));
        pendingApprovalRepository.save(pa);
    }

    private PendingApproval loadOwnedByParent(UUID approvalId, String parentEmail) {
        User parent = userRepository.findByEmail(parentEmail)
                .orElseThrow(() -> new IllegalArgumentException("Użytkownik nie znaleziony."));

        PendingApproval pa = pendingApprovalRepository.findById(approvalId)
                .orElseThrow(() -> new IllegalArgumentException("Wniosek nie znaleziony."));

        if (!pa.getParentUser().getId().equals(parent.getId())) {
            throw new SecurityException("Brak uprawnień do tej akceptacji.");
        }
        if (!"PENDING".equals(pa.getStatus())) {
            throw new IllegalStateException("Ten wniosek został już rozpatrzony.");
        }
        return pa;
    }

    private PendingApprovalResponse toResponse(PendingApproval pa) {
        Account junior = pa.getJuniorAccount();
        Transaction tx = pa.getTransaction();
        return new PendingApprovalResponse(
                pa.getId(),
                junior.getId(),
                junior.getAccountNumber(),
                junior.getUser().getFirstName(),
                junior.getUser().getLastName(),
                tx != null ? tx.getId() : null,
                tx != null ? tx.getType() : null,
                tx != null ? tx.getReceiverName() : null,
                tx != null ? tx.getReceiverAccountNumber() : null,
                tx != null ? tx.getTitle() : null,
                pa.getAmount(),
                tx != null ? tx.getCurrency() : junior.getCurrency(),
                pa.getDescription(),
                pa.getStatus(),
                pa.getCreatedAt()
        );
    }
}