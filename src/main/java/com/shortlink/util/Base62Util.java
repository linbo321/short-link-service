package com.shortlink.util;

public final class Base62Util {
    private static final char[] ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    private Base62Util() {
    }

    public static String encodeToFixedLength(long id, int length) {
        if (id < 0) {
            throw new IllegalArgumentException("id must be non-negative");
        }
        if (length <= 0) {
            throw new IllegalArgumentException("length must be positive");
        }

        StringBuilder encoded = new StringBuilder();
        long value = id;
        do {
            int remainder = (int) (value % ALPHABET.length);
            encoded.append(ALPHABET[remainder]);
            value = value / ALPHABET.length;
        } while (value > 0);

        encoded.reverse();
        if (encoded.length() > length) {
            return encoded.toString();
        }
        while (encoded.length() < length) {
            encoded.insert(0, '0');
        }
        return encoded.toString();
    }
}
