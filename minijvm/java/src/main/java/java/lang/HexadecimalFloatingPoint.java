package java.lang;

/** Exact hexadecimal significand rounding, shared by Float and Double. */
final class HexadecimalFloatingPoint {
    private HexadecimalFloatingPoint() {}

    static String input(String text) {
        String value = text.trim();
        int start = value.startsWith("+") || value.startsWith("-") ? 1 : 0;
        return value.length() >= start + 2 && value.charAt(start) == '0'
            && (value.charAt(start + 1) == 'x' || value.charAt(start + 1) == 'X') ? value : null;
    }

    static long bits(String text, int precision, int exponentWidth) {
        int fractionWidth = precision - 1;
        long sign = text.charAt(0) == '-' ? 1L << (fractionWidth + exponentWidth) : 0;
        int start = text.charAt(0) == '+' || text.charAt(0) == '-' ? 3 : 2;
        int end = text.length();
        char last = text.charAt(end - 1);
        if (last == 'f' || last == 'F' || last == 'd' || last == 'D') end--;
        int digits = 0, fractionDigits = 0, leading = 0, first = 0;
        boolean dot = false;
        int cursor = start;
        while (cursor < end && text.charAt(cursor) != 'p' && text.charAt(cursor) != 'P') {
            char ch = text.charAt(cursor++);
            if (ch == '.') {
                if (dot) throw new NumberFormatException(text);
                dot = true;
                continue;
            }
            int digit = digit(ch);
            if (digit < 0) throw new NumberFormatException(text);
            digits++;
            if (dot) fractionDigits++;
            if (first == 0) { if (digit == 0) leading++; else first = digit; }
        }
        if (digits == 0 || cursor == end) throw new NumberFormatException(text);
        int significandEnd = cursor++;
        boolean negativeExponent = false;
        if (cursor < end && (text.charAt(cursor) == '+' || text.charAt(cursor) == '-'))
            negativeExponent = text.charAt(cursor++) == '-';
        int exponentStart = cursor;
        long exponent = 0;
        while (cursor < end) {
            char ch = text.charAt(cursor++);
            if (ch < '0' || ch > '9') throw new NumberFormatException(text);
            // Larger magnitudes cannot cancel a significand that fits in a Java String.
            if (exponent < 1000000000000L) exponent = Math.min(1000000000000L, exponent * 10 + ch - '0');
        }
        if (cursor == exponentStart) throw new NumberFormatException(text);
        if (first == 0) return sign;
        if (negativeExponent) exponent = -exponent;
        int firstWidth = first >= 8 ? 4 : first >= 4 ? 3 : first >= 2 ? 2 : 1;
        long length = 4L * (digits - leading - 1) + firstWidth;
        long binaryExponent = exponent - 4L * fractionDigits + length - 1;
        int bias = (1 << (exponentWidth - 1)) - 1;
        int minimumNormal = 1 - bias;
        int minimumSubnormal = minimumNormal - fractionWidth;
        long infinity = ((1L << exponentWidth) - 1) << fractionWidth;
        if (binaryExponent > bias) return sign | infinity;
        if (binaryExponent < minimumSubnormal - 1) return sign;
        int retained = (int) Math.min(precision, Math.max(0L, binaryExponent - minimumSubnormal + 1));
        long significand = 0;
        int collected = 0;
        boolean started = false, hasGuard = false, guard = false, sticky = false;
        for (int i = start; i < significandEnd; i++) {
            if (text.charAt(i) == '.') continue;
            int digit = digit(text.charAt(i));
            for (int bit = 3; bit >= 0; bit--) {
                boolean one = ((digit >>> bit) & 1) != 0;
                if (!started) { if (!one) continue; started = true; }
                if (collected < retained) { significand = (significand << 1) | (one ? 1 : 0); collected++; }
                else if (!hasGuard) { hasGuard = true; guard = one; }
                else sticky |= one;
            }
        }
        significand <<= retained - collected;
        if (guard && (sticky || (significand & 1) != 0)) significand++;
        if (binaryExponent < minimumNormal) return sign | significand;
        if (significand == (1L << precision)) { significand >>>= 1; binaryExponent++; }
        if (binaryExponent > bias) return sign | infinity;
        return sign | ((binaryExponent + bias) << fractionWidth) | (significand & ((1L << fractionWidth) - 1));
    }

    private static int digit(char ch) {
        return ch >= '0' && ch <= '9' ? ch - '0' : ch >= 'a' && ch <= 'f' ? ch - 'a' + 10
            : ch >= 'A' && ch <= 'F' ? ch - 'A' + 10 : -1;
    }
}
