package com.kestrel.booking.lock;

/**
 * Transaction-scoped mutual exclusion keyed by an arbitrary string.
 *
 * <p>{@link #acquire(String)} blocks until the key is free and holds it until the surrounding
 * transaction commits or rolls back. Callers must already be inside a transaction.
 */
public interface AdvisoryLock {

    /**
     * Blocks until the lock for {@code key} is held by the current transaction.
     *
     * @throws IllegalStateException if no transaction is active
     */
    void acquire(String key);
}
