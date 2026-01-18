package com.kestrel.booking.ratelimit;

/** Minimal thread-safe token bucket driven by caller-supplied timestamps. */
final class TokenBucket {

    private final double capacity;
    private final double refillPerMilli;
    private double tokens;
    private long lastRefillMillis;

    TokenBucket(double capacity, double refillPerSecond, long nowMillis) {
        this.capacity = capacity;
        this.refillPerMilli = refillPerSecond / 1000.0;
        this.tokens = capacity;
        this.lastRefillMillis = nowMillis;
    }

    synchronized boolean tryAcquire(long nowMillis) {
        refill(nowMillis);
        if (tokens >= 1.0) {
            tokens -= 1.0;
            return true;
        }
        return false;
    }

    /** Milliseconds until one whole token is available; zero if one already is. */
    synchronized long millisUntilNextToken(long nowMillis) {
        refill(nowMillis);
        if (tokens >= 1.0) {
            return 0L;
        }
        return (long) Math.ceil((1.0 - tokens) / refillPerMilli);
    }

    synchronized double availableTokens(long nowMillis) {
        refill(nowMillis);
        return tokens;
    }

    private void refill(long nowMillis) {
        long elapsed = nowMillis - lastRefillMillis;
        if (elapsed <= 0) {
            return;
        }
        tokens = Math.min(capacity, tokens + elapsed * refillPerMilli);
        lastRefillMillis = nowMillis;
    }
}
