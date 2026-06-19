package com.polishbank.bank_a.integration.cards;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CardsProviderHmacSignerTest {

    private static final String SECRET = "secret-pl-a-hmac";
    private static final String TIMESTAMP = "1700000000";

    private final CardsProviderHmacSigner signer = new CardsProviderHmacSigner();

    @Test
    void produces_python_compatible_signature_for_issue_card_body() {
        // body canonical (sort_keys=True, separators=(',', ':')) — to obliczone w Pythonie:
        // import hmac, hashlib
        // body_json = '{"account_id":"a1","card_type":"VIRTUAL","initial_balance":0.0,"user_id":"u1"}'
        // hmac.new(b"secret-pl-a-hmac", ("1700000000" + body_json).encode(), hashlib.sha256).hexdigest()
        String canonicalBody = "{\"account_id\":\"a1\",\"card_type\":\"VIRTUAL\",\"initial_balance\":0.0,\"user_id\":\"u1\"}";
        String expectedSignature = "21f17359554ec511694386754312e1ba002fc41c5f1b6df10a91e2f0198e2a29";

        CardsProviderHmacSigner.SignedRequest signed =
                signer.signWithTimestamp(SECRET, canonicalBody, TIMESTAMP);

        assertEquals(expectedSignature, signed.signature());
        assertEquals(TIMESTAMP, signed.timestamp());
        assertEquals(canonicalBody, signed.bodyJson());
    }

    @Test
    void produces_python_compatible_signature_for_change_status_body() {
        String canonicalBody = "{\"reason\":\"test\",\"status\":\"BLOCKED\"}";
        String expectedSignature = "5815e72df893de06e1c0e022e4b573a09cf4e436df3150a426337c859d3fac74";

        CardsProviderHmacSigner.SignedRequest signed =
                signer.signWithTimestamp(SECRET, canonicalBody, TIMESTAMP);

        assertEquals(expectedSignature, signed.signature());
    }
}