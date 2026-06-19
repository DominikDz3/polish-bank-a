package com.polishbank.bank_a.domain.klik;

import com.polishbank.bank_a.domain.klik.dto.KlikAliasView;
import com.polishbank.bank_a.domain.klik.dto.KlikRegisterAliasRequest;
import com.polishbank.bank_a.domain.user.User;
import com.polishbank.bank_a.domain.user.UserRepository;
import com.polishbank.bank_a.entity.Account;
import com.polishbank.bank_a.entity.KlikAlias;
import com.polishbank.bank_a.integration.klik.KlikP2PClient;
import com.polishbank.bank_a.repository.AccountRepository;
import com.polishbank.bank_a.repository.KlikAliasRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KlikAliasService {

    private final KlikAliasRepository aliasRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final KlikP2PClient klikClient;

    @Transactional
    public KlikAliasView registerAlias(KlikRegisterAliasRequest req, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        Account account = accountRepository.findById(req.accountId())
                .orElseThrow(() -> new IllegalArgumentException("Konto nie istnieje."));
        if (!account.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Brak dostępu do tego konta.");
        }

        aliasRepository.findByAliasAndActiveTrue(req.phone()).ifPresent(a -> {
            throw new IllegalStateException("Ten numer jest już zarejestrowany.");
        });

        Map<String, Object> resp;
        try {
            resp = klikClient.registerAlias(req.phone(), account.getAccountNumber());
        } catch (Exception e) {
            throw new IllegalStateException("KLIK odrzucił rejestrację: " + e.getMessage());
        }

        UUID klikAliasId = UUID.fromString((String) resp.get("alias_id"));

        KlikAlias alias = KlikAlias.builder()
                .user(user)
                .account(account)
                .alias(req.phone())
                .klikAliasId(klikAliasId)
                .active(true)
                .build();
        aliasRepository.save(alias);

        return toView(alias);
    }

    public List<KlikAliasView> getMyAliases(String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        return aliasRepository.findByUser_IdAndActiveTrueOrderByCreatedAtDesc(user.getId())
                .stream().map(this::toView).toList();
    }

    @Transactional
    public void deleteAlias(UUID aliasId, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        KlikAlias alias = aliasRepository.findById(aliasId)
                .orElseThrow(() -> new IllegalArgumentException("Alias nie istnieje."));
        if (!alias.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Brak dostępu do tego aliasu.");
        }
        if (!alias.isActive()) return;

        try {
            klikClient.deleteAlias(alias.getAlias());
        } catch (Exception e) {
            throw new IllegalStateException("KLIK odrzucił usunięcie: " + e.getMessage());
        }

        alias.setActive(false);
        alias.setDeactivatedAt(LocalDateTime.now());
        aliasRepository.save(alias);
    }

    private KlikAliasView toView(KlikAlias a) {
        return new KlikAliasView(
                a.getId(),
                a.getAlias(),
                a.getAccount().getAccountNumber(),
                a.getCreatedAt()
        );
    }
}