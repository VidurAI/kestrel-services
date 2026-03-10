package com.kestrel.booking.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenBucketTest {

    @Test
    void startsFullAndDrains() {
        TokenBucket bucket = new TokenBucket(3, 1, 0);

        assertThat(bucket.tryAcquire(0)).isTrue();
        assertThat(bucket.tryAcquire(0)).isTrue();
        assertThat(bucket.tryAcquire(0)).isTrue();
        assertThat(bucket.tryAcquire(0)).isFalse();
    }

    @Test
    void waitTimeReflectsRefillRate() {
        TokenBucket bucket = new TokenBucket(1, 4, 0);
        bucket.tryAcquire(0);

        assertThat(bucket.millisUntilNextToken(0)).isEqualTo(250);
        assertThat(bucket.millisUntilNextToken(100)).isEqualTo(150);
        assertThat(bucket.millisUntilNextToken(250)).isZero();
    }

    @Test
    void ignoresClockGoingBackwards() {
        TokenBucket bucket = new TokenBucket(1, 1, 1_000);
        bucket.tryAcquire(1_000);

        assertThat(bucket.tryAcquire(500)).isFalse();
    }
}
