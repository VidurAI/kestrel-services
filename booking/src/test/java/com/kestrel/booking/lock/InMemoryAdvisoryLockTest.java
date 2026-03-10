package com.kestrel.booking.lock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class InMemoryAdvisoryLockTest {

    private final InMemoryAdvisoryLock lock = new InMemoryAdvisoryLock();

    @Test
    void requiresAnActiveTransaction() {
        assertThatThrownBy(() -> lock.acquire("KB-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("active transaction");
    }

    @Test
    void isHeldUntilTransactionCompletes() {
        TxHelper.inTransaction(() -> {
            lock.acquire("KB-1");
            assertThat(lock.isHeldByCurrentThread("KB-1")).isTrue();
        });

        assertThat(lock.isHeldByCurrentThread("KB-1")).isFalse();
    }

    @Test
    void isReentrantWithinOneTransaction() {
        TxHelper.inTransaction(() -> {
            lock.acquire("KB-1");
            lock.acquire("KB-1");
        });

        assertThat(lock.isHeldByCurrentThread("KB-1")).isFalse();
    }

    @Test
    void serialisesCriticalSectionsForTheSameKey() throws Exception {
        int workers = 8;
        AtomicInteger inside = new AtomicInteger();
        AtomicInteger maxInside = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < workers; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    TxHelper.inTransaction(() -> {
                        lock.acquire("KB-SAME");
                        int now = inside.incrementAndGet();
                        maxInside.accumulateAndGet(now, Math::max);
                        sleepQuietly(15);
                        inside.decrementAndGet();
                    });
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(maxInside.get()).isEqualTo(1);
    }

    @Test
    void keysOnDifferentStripesDoNotBlockEachOther() throws Exception {
        String first = "KB-A";
        String second = findKeyOnDifferentStripe(first);
        CountDownLatch holding = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<?> holder = pool.submit(() -> TxHelper.inTransaction(() -> {
                lock.acquire(first);
                holding.countDown();
                awaitQuietly(release);
            }));
            assertThat(holding.await(5, TimeUnit.SECONDS)).isTrue();

            TxHelper.inTransaction(() -> {
                lock.acquire(second);
                assertThat(lock.isHeldByCurrentThread(second)).isTrue();
            });

            release.countDown();
            holder.get(5, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void stripeMappingIsStable() {
        assertThat(InMemoryAdvisoryLock.stripeFor("KB-XYZ")).isEqualTo(InMemoryAdvisoryLock.stripeFor("KB-XYZ"));
        assertThat(InMemoryAdvisoryLock.stripeFor("KB-XYZ")).isBetween(0, InMemoryAdvisoryLock.STRIPES - 1);
    }

    private static String findKeyOnDifferentStripe(String key) {
        int stripe = InMemoryAdvisoryLock.stripeFor(key);
        for (int i = 0; i < 1_000; i++) {
            String candidate = "KB-B" + i;
            if (InMemoryAdvisoryLock.stripeFor(candidate) != stripe) {
                return candidate;
            }
        }
        throw new AssertionError("no distinct stripe found");
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
