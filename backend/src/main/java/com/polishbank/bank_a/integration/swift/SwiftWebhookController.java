package com.polishbank.bank_a.integration.swift;

import com.polishbank.bank_a.domain.swift.SwiftInboundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks/swift")
@RequiredArgsConstructor
@Slf4j
public class SwiftWebhookController {

    private final SwiftInboundService inboundService;

    @PostMapping(value = "/receive",
            consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE, MediaType.ALL_VALUE})
    public ResponseEntity<Map<String, Object>> receive(
            @RequestBody String xml,
            @RequestHeader(value = "X-SWIFT-Message-Type", required = false) String messageType,
            @RequestHeader(value = "X-SWIFT-UETR", required = false) String uetrHeader,
            @RequestHeader(value = "X-SWIFT-Fee-For", required = false) String feeFor) {

        if (feeFor != null && !feeFor.isBlank()) {
            log.info("[SWIFT_FEE_NOTIFY] uetr={} feeFor={}", uetrHeader, feeFor);
            return ResponseEntity.ok(Map.of("status", "fee_acknowledged"));
        }

        if ("RETURN".equalsIgnoreCase(messageType)) {
            SwiftInboundService.ReturnResult res = inboundService.handleReturn(xml, uetrHeader);
            return ResponseEntity.ok(Map.of(
                    "status", res.matched() ? "return_received" : "return_unmatched",
                    "uetr", res.uetr() == null ? "" : res.uetr()
            ));
        }

        SwiftInboundService.IncomingResult res = inboundService.handleIncoming(xml, uetrHeader);
        if (!res.accepted()) {
            return ResponseEntity.status(res.httpStatus())
                    .body(Map.of("status", res.statusLabel(), "reason", res.reason()));
        }
        return ResponseEntity.ok(Map.of(
                "status", "accepted",
                "uetr", res.uetr() == null ? "" : res.uetr(),
                "credited_account", res.creditedAccount() == null ? "" : res.creditedAccount()
        ));
    }
}