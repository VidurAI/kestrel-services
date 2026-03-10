package com.kestrel.booking.lock;

import java.util.List;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Simulates a transaction boundary for lock tests without a database. */
final class TxHelper {

    private TxHelper() {
    }

    static void inTransaction(Runnable body) {
        TransactionSynchronizationManager.initSynchronization();
        try {
            body.run();
        } finally {
            List<TransactionSynchronization> syncs = TransactionSynchronizationManager.getSynchronizations();
            try {
                syncs.forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }
    }
}
