package com.shortlink.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Base62UtilTest {

    @Test
    void encodesIdToFixedSixCharacterCode() {
        assertThat(Base62Util.encodeToFixedLength(1_000_000L, 6)).isEqualTo("004c92");
        assertThat(Base62Util.encodeToFixedLength(0L, 6)).isEqualTo("000000");
        assertThat(Base62Util.encodeToFixedLength(61L, 6)).isEqualTo("00000Z");
        assertThat(Base62Util.encodeToFixedLength(62L, 6)).isEqualTo("000010");
    }

    @Test
    void rejectsNegativeIdsAndImpossibleLengths() {
        assertThatThrownBy(() -> Base62Util.encodeToFixedLength(-1L, 6))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id");
        assertThatThrownBy(() -> Base62Util.encodeToFixedLength(1L, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("length");
    }
}
