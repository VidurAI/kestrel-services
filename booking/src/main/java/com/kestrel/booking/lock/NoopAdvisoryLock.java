package com.kestrel.booking.lock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Selected when {@code kestrel.booking.lock.enabled=false}; takes no lock at all. */
@Component
@ConditionalOnProperty(prefix = "kestrel.booking.lock", name = "enabled", havingValue = "false")
public class NoopAdvisoryLock implements AdvisoryLock {

    @Override
    public void acquire(String key) {
        // intentionally empty
    }
}
