package com.kestrel.booking.lock;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

class AdvisoryLockWiringTest {

    @Nested
    @SpringBootTest
    @ActiveProfiles("test")
    class Enabled {

        @Autowired
        private AdvisoryLock lock;

        @Test
        void testProfileUsesInMemoryImplementation() {
            assertThat(lock).isInstanceOf(InMemoryAdvisoryLock.class);
        }
    }

    @Nested
    @SpringBootTest(properties = "kestrel.booking.lock.enabled=false")
    @ActiveProfiles("test")
    class Disabled {

        @Autowired
        private AdvisoryLock lock;

        @Test
        void switchingTheFlagOffInstallsTheNoopLock() {
            assertThat(lock).isInstanceOf(NoopAdvisoryLock.class);
        }
    }
}
