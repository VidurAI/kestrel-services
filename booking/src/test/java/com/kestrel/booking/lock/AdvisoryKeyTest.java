package com.kestrel.booking.lock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AdvisoryKeyTest {

    @Test
    void sameInputGivesSameKey() {
        assertThat(AdvisoryKey.of("KB-1")).isEqualTo(AdvisoryKey.of("KB-1"));
    }

    @Test
    void differentInputsGiveDifferentKeys() {
        assertThat(AdvisoryKey.of("KB-1")).isNotEqualTo(AdvisoryKey.of("KB-2"));
    }

    @Test
    void rejectsBlankKeys() {
        assertThatThrownBy(() -> AdvisoryKey.of(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AdvisoryKey.of(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
