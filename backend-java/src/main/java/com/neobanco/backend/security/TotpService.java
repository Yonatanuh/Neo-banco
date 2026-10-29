package com.neobanco.backend.security;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;

/** Standard six-digit, 30-second TOTP with a one-step clock skew allowance. */
@Service
public class TotpService {
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private final SecureRandom random = new SecureRandom();

    public String generateSecret() {
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        StringBuilder encoded = new StringBuilder();
        int buffer = 0, bits = 0;
        for (byte value : bytes) {
            buffer = (buffer << 8) | (value & 0xff);
            bits += 8;
            while (bits >= 5) {
                encoded.append(ALPHABET.charAt((buffer >>> (bits - 5)) & 31));
                bits -= 5;
            }
        }
        if (bits > 0) encoded.append(ALPHABET.charAt((buffer << (5 - bits)) & 31));
        return encoded.toString();
    }

    public String provisioningUri(String email, String secret) {
        String label = URLEncoder.encode("NeoBanco:" + email, StandardCharsets.UTF_8).replace("+", "%20");
        return "otpauth://totp/" + label + "?secret=" + secret + "&issuer=NeoBanco&algorithm=SHA1&digits=6&period=30";
    }

    public boolean verify(String secret, String code) {
        return verifyAt(secret, code, Instant.now());
    }

    public boolean isLegacySecret(String secret) {
        return secret != null && secret.matches("[A-F0-9]{16}");
    }

    public boolean verifyAt(String secret, String code, Instant instant) {
        if (secret == null || code == null || !code.matches("[0-9]{6}")) return false;
        byte[] key;
        try { key = decodeSecret(secret); }
        catch (IllegalArgumentException ex) { return false; }
        if (key.length < 20) return false;
        long counter = instant.getEpochSecond() / 30;
        for (long step = counter - 1; step <= counter + 1; step++) {
            if (step < 0) continue;
            try {
                Mac hmac = Mac.getInstance("HmacSHA1");
                hmac.init(new SecretKeySpec(key, "HmacSHA1"));
                byte[] hash = hmac.doFinal(ByteBuffer.allocate(8).putLong(step).array());
                int offset = hash[hash.length - 1] & 15;
                int binary = ((hash[offset] & 0x7f) << 24) | ((hash[offset + 1] & 0xff) << 16)
                        | ((hash[offset + 2] & 0xff) << 8) | (hash[offset + 3] & 0xff);
                byte[] expected = String.format(Locale.ROOT, "%06d", binary % 1_000_000).getBytes(StandardCharsets.US_ASCII);
                if (MessageDigest.isEqual(expected, code.getBytes(StandardCharsets.US_ASCII))) return true;
            } catch (GeneralSecurityException ex) { throw new IllegalStateException("TOTP no disponible", ex); }
        }
        return false;
    }

    private byte[] decodeSecret(String input) {
        String value = input.toUpperCase(Locale.ROOT);
        if (!value.matches("[A-Z2-7]{32}")) throw new IllegalArgumentException("Secreto TOTP inválido");
        byte[] result = new byte[value.length() * 5 / 8];
        int buffer = 0, bits = 0, index = 0;
        for (char ch : value.toCharArray()) {
            buffer = (buffer << 5) | ALPHABET.indexOf(ch);
            bits += 5;
            if (bits >= 8) {
                bits -= 8;
                result[index++] = (byte) (buffer >>> bits);
            }
        }
        return result;
    }
}
