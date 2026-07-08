package com.spexcrafters.sharedkernel.util;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * UUID version 7 generator per RFC 9562 (time-ordered, index-friendly).
 *
 * <p>Layout: 48-bit Unix epoch milliseconds, 4-bit version ({@code 0b0111}), 12 random bits,
 * 2-bit variant ({@code 0b10}), 62 random bits. Random bits come from {@link SecureRandom};
 * no external dependency is used.
 */
public final class UuidV7 {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7() {
    }

    /** Generates a UUIDv7 for the current instant. */
    public static UUID generate() {
        return generate(System.currentTimeMillis());
    }

    /**
     * Generates a UUIDv7 for an explicit timestamp. Visible for tests; production code
     * should call {@link #generate()}.
     */
    static UUID generate(long epochMillis) {
        byte[] random = new byte[10];
        RANDOM.nextBytes(random);

        long msb = (epochMillis & 0xFFFF_FFFF_FFFFL) << 16;
        msb |= 0x7000L; // version 7
        msb |= ((random[0] & 0x0FL) << 8) | (random[1] & 0xFFL); // rand_a (12 bits)

        long randB = 0L;
        for (int i = 2; i < 10; i++) {
            randB = (randB << 8) | (random[i] & 0xFFL);
        }
        long lsb = 0x8000_0000_0000_0000L; // variant 0b10
        lsb |= randB & 0x3FFF_FFFF_FFFF_FFFFL; // rand_b (62 bits)

        return new UUID(msb, lsb);
    }

    /** Extracts the millisecond timestamp embedded in a UUIDv7. */
    public static long timestampMillis(UUID uuid) {
        return uuid.getMostSignificantBits() >>> 16;
    }
}
