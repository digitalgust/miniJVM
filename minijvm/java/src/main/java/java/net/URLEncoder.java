/* SPDX-License-Identifier: MIT */
package java.net;

import java.io.UnsupportedEncodingException;

/** application/x-www-form-urlencoded encoder. */
public final class URLEncoder {
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();
    static final String dfltEncName = "UTF-8";

    private URLEncoder() {
    }

    public static String encode(String value) {
        try {
            return encode(value, dfltEncName);
        } catch (UnsupportedEncodingException impossible) {
            throw new RuntimeException(impossible.toString());
        }
    }

    public static String encode(String value, String encoding)
            throws UnsupportedEncodingException {
        if (value == null || encoding == null) {
            throw new NullPointerException();
        }
        StringBuffer out = new StringBuffer(value.length() + 16);
        int index = 0;
        while (index < value.length()) {
            char ch = value.charAt(index);
            if (isSafe(ch)) {
                out.append(ch == ' ' ? '+' : ch);
                index++;
                continue;
            }
            int start = index;
            do {
                index++;
            } while (index < value.length() && !isSafe(value.charAt(index)));
            byte[] bytes = value.substring(start, index).getBytes(encoding);
            for (int i = 0; i < bytes.length; i++) {
                int octet = bytes[i] & 0xff;
                out.append('%');
                out.append(HEX[octet >>> 4]);
                out.append(HEX[octet & 0x0f]);
            }
        }
        return out.toString();
    }

    private static boolean isSafe(char ch) {
        return ch >= 'a' && ch <= 'z'
                || ch >= 'A' && ch <= 'Z'
                || ch >= '0' && ch <= '9'
                || ch == ' ' || ch == '-' || ch == '_' || ch == '.' || ch == '*';
    }
}
