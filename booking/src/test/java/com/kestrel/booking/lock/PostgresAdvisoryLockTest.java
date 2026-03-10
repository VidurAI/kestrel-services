package com.kestrel.booking.lock;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class PostgresAdvisoryLockTest {

    private EntityManager entityManager;
    private Session session;
    private Connection connection;
    private PreparedStatement statement;
    private PostgresAdvisoryLock lock;

    @BeforeEach
    void setUp() throws Exception {
        entityManager = mock(EntityManager.class);
        session = mock(Session.class);
        connection = mock(Connection.class);
        statement = mock(PreparedStatement.class);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(connection.prepareStatement(PostgresAdvisoryLock.LOCK_SQL)).thenReturn(statement);
        doAnswer(invocation -> {
            invocation.<Work>getArgument(0).execute(connection);
            return null;
        }).when(session).doWork(any(Work.class));
        lock = new PostgresAdvisoryLock(entityManager);
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void runsNativeAdvisoryLockStatementWithHashedKey() throws Exception {
        TransactionSynchronizationManager.setActualTransactionActive(true);

        lock.acquire("KB-ABC123");

        InOrder order = inOrder(connection, statement);
        order.verify(connection).prepareStatement("select pg_advisory_xact_lock(?)");
        order.verify(statement).setLong(1, AdvisoryKey.of("KB-ABC123"));
        order.verify(statement).execute();
        order.verify(statement).close();
    }

    @Test
    void refusesToRunOutsideATransaction() throws Exception {
        assertThatThrownBy(() -> lock.acquire("KB-ABC123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("active transaction");

        verify(connection, never()).prepareStatement(any());
    }
}
