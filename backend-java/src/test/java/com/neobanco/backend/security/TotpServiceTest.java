package com.neobanco.backend.security;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class TotpServiceTest {
    private final TotpService totp = new TotpService();
    // RFC 6238 SHA-1 test secret: ASCII 12345678901234567890, encoded as Base32.
    private static final String RFC_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    @Test void validatesKnownRfc6238VectorsAndClockSkew() {
        assertTrue(totp.verifyAt(RFC_SECRET, "287082", Instant.ofEpochSecond(59)));
        assertTrue(totp.verifyAt(RFC_SECRET, "287082", Instant.ofEpochSecond(60)));
        assertTrue(totp.verifyAt(RFC_SECRET, "081804", Instant.ofEpochSecond(1111111109)));
        assertFalse(totp.verifyAt(RFC_SECRET, "287082", Instant.ofEpochSecond(120)));
    }

    @Test void rejectsMalformedCodesAndLegacyMockSecrets() {
        Instant instant = Instant.ofEpochSecond(59);
        assertFalse(totp.verifyAt(RFC_SECRET, "any-code", instant));
        assertFalse(totp.verifyAt(RFC_SECRET, "28708", instant));
        assertFalse(totp.verifyAt("1234", "287082", instant));
        assertFalse(totp.verifyAt(null, "287082", instant));
    }

    @Test void createsUniqueBase32SecretsAndEncodedProvisioningUri() {
        String first = totp.generateSecret(), second = totp.generateSecret();
        assertTrue(first.matches("[A-Z2-7]{32}"));
        assertNotEquals(first, second);
        String uri = totp.provisioningUri("cliente+test@example.com", first);
        assertTrue(uri.startsWith("otpauth://totp/NeoBanco%3Acliente%2Btest%40example.com?"));
        assertTrue(uri.contains("secret=" + first));
    }
}
