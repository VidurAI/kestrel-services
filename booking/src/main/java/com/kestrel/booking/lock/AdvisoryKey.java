package com.kestrel.booking.lock;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Maps string keys onto the 64-bit key space used by Postgres advisory locks. */
public final class AdvisoryKey {

    private static final String NAMESPACE = "kestrel.booking.shipment:";

    private AdvisoryKey() {
    }

    public static long of(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("advisory lock key must not be blank");
        }
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] digest = sha.digest((NAMESPACE + key).getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.wrap(digest, 0, Long.BYTES).getLong();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
