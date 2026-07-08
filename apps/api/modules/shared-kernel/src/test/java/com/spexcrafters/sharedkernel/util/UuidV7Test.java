package com.spexcrafters.sharedkernel.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidV7Test {

    @Test
    void hasVersion7AndRfcVariant() {
        UUID uuid = UuidV7.generate();

        assertThat(uuid.version()).isEqualTo(7);
        assertThat(uuid.variant()).isEqualTo(2); // IETF variant 0b10
    }

    @Test
    void embedsTheMillisecondTimestamp() {
        long timestamp = 1_752_000_000_000L;

        UUID uuid = UuidV7.generate(timestamp);

        assertThat(UuidV7.timestampMillis(uuid)).isEqualTo(timestamp);
    }

    @Test
    void sortsMonotonicallyByGenerationTimeAcrossMillis() {
        // UUIDv7 guarantees ordering at millisecond granularity; verify that ids generated
        // for strictly increasing timestamps sort lexicographically in generation order.
        List<String> generated = new ArrayList<>();
        long base = 1_752_000_000_000L;
        for (int i = 0; i < 1_000; i++) {
            generated.add(UuidV7.generate(base + i).toString());
        }

        List<String> sorted = new ArrayList<>(generated);
        sorted.sort(String::compareTo);

        assertThat(generated).isEqualTo(sorted);
    }

    @Test
    void generatesUniqueValues() {
        List<UUID> uuids = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) {
            uuids.add(UuidV7.generate());
        }

        assertThat(uuids).doesNotHaveDuplicates();
    }
}
