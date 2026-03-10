package com.kestrel.booking.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ReferenceGeneratorTest {

    private final ReferenceGenerator generator = new ReferenceGenerator();

    @Test
    void clientReferenceYieldsDeterministicReference() {
        assertThat(generator.generate("order-9")).isEqualTo(generator.generate("order-9"));
        assertThat(generator.generate("order-9")).isEqualTo(generator.generate("  order-9 "));
    }

    @Test
    void differentClientReferencesDiffer() {
        assertThat(generator.generate("order-9")).isNotEqualTo(generator.generate("order-10"));
    }

    @Test
    void referencesHaveStableShape() {
        assertThat(generator.generate("order-9")).matches("KB-[0-9A-HJKMNP-TV-Z]{12}");
        assertThat(generator.generate(null)).matches("KB-[0-9A-HJKMNP-TV-Z]{12}");
    }

    @Test
    void blankClientReferenceFallsBackToRandom() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            seen.add(generator.generate("  "));
        }

        assertThat(seen).hasSize(200);
    }
}
