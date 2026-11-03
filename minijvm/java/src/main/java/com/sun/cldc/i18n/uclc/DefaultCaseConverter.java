/* SPDX-License-Identifier: MIT */
package com.sun.cldc.i18n.uclc;

/** CLDC Latin-1 character case and digit operations. */
public final class DefaultCaseConverter {
    private DefaultCaseConverter() {
    }

    public static boolean isLowerCase(char value) {
        return (value >= 'a' && value <= 'z') ||
                (value >= 0x00df && value <= 0x00f6) ||
                (value >= 0x00f8 && value <= 0x00ff);
    }

    public static boolean isUpperCase(char value) {
        return (value >= 'A' && value <= 'Z') ||
                (value >= 0x00c0 && value <= 0x00d6) ||
                (value >= 0x00d8 && value <= 0x00de);
    }

    public static char toLowerCase(char value) {
        if (value >= 'A' && value <= 'Z') {
            return (char) (value + 0x20);
        }
        if ((value >= 0x00c0 && value <= 0x00d6) ||
                (value >= 0x00d8 && value <= 0x00de)) {
            return (char) (value + 0x20);
        }
        return value;
    }

    public static char toUpperCase(char value) {
        if (value >= 'a' && value <= 'z') {
            return (char) (value - 0x20);
        }
        if ((value >= 0x00e0 && value <= 0x00f6) ||
                (value >= 0x00f8 && value <= 0x00fe)) {
            return (char) (value - 0x20);
        }
        return value;
    }

    public static boolean isDigit(char value) {
        return value >= '0' && value <= '9';
    }

    public static int digit(char value, int radix) {
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
            return -1;
        }
        int digit;
        if (isDigit(value)) {
            digit = value - '0';
        } else if (value >= 'A' && value <= 'Z') {
            digit = value - 'A' + 10;
        } else if (value >= 'a' && value <= 'z') {
            digit = value - 'a' + 10;
        } else {
            return -1;
        }
        return digit < radix ? digit : -1;
    }
}
