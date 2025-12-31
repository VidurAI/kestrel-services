package com.kestrel.booking.lock;

import java.util.concurrent.locks.ReentrantLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Single-JVM {@link AdvisoryLock} for the {@code test} profile. Keys hash onto a fixed set of
 * stripes; each stripe is released when the owning transaction completes.
 */
@Component
@Profile("test")
@ConditionalOnProperty(prefix = "kestrel.booking.lock", name = "enabled", havingValue = "true",
        matchIfMissing = true)
public class InMemoryAdvisoryLock implements AdvisoryLock {

    static final int STRIPES = 64;

    private final ReentrantLock[] stripes = new ReentrantLock[STRIPES];

    public InMemoryAdvisoryLock() {
        for (int i = 0; i < STRIPES; i++) {
            stripes[i] = new ReentrantLock();
        }
    }

    @Override
    public void acquire(String key) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("advisory lock requires an active transaction");
        }
        ReentrantLock lock = stripes[stripeFor(key)];
        lock.lock();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                lock.unlock();
            }
        });
    }

    /** Visible for tests: whether the calling thread currently holds the stripe for {@code key}. */
    boolean isHeldByCurrentThread(String key) {
        return stripes[stripeFor(key)].isHeldByCurrentThread();
    }

    static int stripeFor(String key) {
        return Math.floorMod(Long.hashCode(AdvisoryKey.of(key)), STRIPES);
    }
}
