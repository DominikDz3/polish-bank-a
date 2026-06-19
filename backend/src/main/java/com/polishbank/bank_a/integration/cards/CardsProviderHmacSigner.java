package com.polishbank.bank_a.integration.cards;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;

@Component
public class CardsProviderHmacSigner {

    private static final String ALGORITHM = "HmacSHA256";

    private final Clock clock;

    public CardsProviderHmacSigner() {
        this(Clock.systemUTC());
    }

    CardsProviderHmacSigner(Clock clock) {
        this.clock = clock;
    }

    public SignedRequest sign(String hmacSecret, String canonicalBodyJson) {
        String timestamp = String.valueOf(clock.instant().getEpochSecond());
        return signWithTimestamp(hmacSecret, canonicalBodyJson, timestamp);
    }

    SignedRequest signWithTimestamp(String hmacSecret, String canonicalBodyJson, String timestamp) {
        String payload = timestamp + canonicalBodyJson;
        String signature = hmacSha256Hex(hmacSecret, payload);
        return new SignedRequest(signature, timestamp, canonicalBodyJson);
    }

    private String hmacSha256Hex(String secret, String message) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Nie udało się obliczyć HMAC-SHA256", e);
        }
    }

    public record SignedRequest(String signature, String timestamp, String bodyJson) {
    }
}