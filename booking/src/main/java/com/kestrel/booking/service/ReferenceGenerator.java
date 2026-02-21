package com.kestrel.booking.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * Issues public booking references of the form {@code KB-XXXXXXXXXXXX}.
 *
 * <p>Per ADR-0006 a reference derived from a caller-supplied client reference is deterministic, so
 * retrying the same request yields the same booking instead of a duplicate.
 */
@Component
public class ReferenceGenerator {

    static final String PREFIX = "KB-";
    static final int BODY_LENGTH = 12;
    private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();

    private final SecureRandom random = new SecureRandom();

    public String generate(String clientReference) {
        if (clientReference == null || clientReference.isBlank()) {
            return PREFIX + randomBody();
        }
        return PREFIX + derivedBody(clientReference.trim());
    }

    private String randomBody() {
        StringBuilder body = new StringBuilder(BODY_LENGTH);
        for (int i = 0; i < BODY_LENGTH; i++) {
            body.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return body.toString();
    }

    private static String derivedBody(String clientReference) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(clientReference.getBytes(StandardCharsets.UTF_8));
            StringBuilder body = new StringBuilder(BODY_LENGTH);
            for (int i = 0; i < BODY_LENGTH; i++) {
                body.append(ALPHABET[(digest[i] & 0xFF) % ALPHABET.length]);
            }
            return body.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
