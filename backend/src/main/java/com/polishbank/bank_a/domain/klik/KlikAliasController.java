package com.polishbank.bank_a.domain.klik;

import com.polishbank.bank_a.domain.klik.dto.KlikAliasView;
import com.polishbank.bank_a.domain.klik.dto.KlikRegisterAliasRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/klik/aliases")
@RequiredArgsConstructor
public class KlikAliasController {

    private final KlikAliasService aliasService;

    @GetMapping
    public ResponseEntity<List<KlikAliasView>> myAliases(Authentication auth) {
        return ResponseEntity.ok(aliasService.getMyAliases(auth.getName()));
    }

    @PostMapping
    public ResponseEntity<KlikAliasView> register(
            @Valid @RequestBody KlikRegisterAliasRequest req,
            Authentication auth) {
        return ResponseEntity.ok(aliasService.registerAlias(req, auth.getName()));
    }

    @DeleteMapping("/{aliasId}")
    public ResponseEntity<Void> delete(@PathVariable UUID aliasId, Authentication auth) {
        aliasService.deleteAlias(aliasId, auth.getName());
        return ResponseEntity.noContent().build();
    }
}