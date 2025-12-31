package com.kestrel.booking.lock;

import jakarta.persistence.EntityManager;
import java.sql.PreparedStatement;
import org.hibernate.Session;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** {@link AdvisoryLock} backed by {@code pg_advisory_xact_lock}. */
@Component
@Profile("!test")
@ConditionalOnProperty(prefix = "kestrel.booking.lock", name = "enabled", havingValue = "true",
        matchIfMissing = true)
public class PostgresAdvisoryLock implements AdvisoryLock {

    static final String LOCK_SQL = "select pg_advisory_xact_lock(?)";

    private final EntityManager entityManager;

    public PostgresAdvisoryLock(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void acquire(String key) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("advisory lock requires an active transaction");
        }
        long lockId = AdvisoryKey.of(key);
        entityManager.unwrap(Session.class).doWork(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(LOCK_SQL)) {
                statement.setLong(1, lockId);
                statement.execute();
            }
        });
    }
}
