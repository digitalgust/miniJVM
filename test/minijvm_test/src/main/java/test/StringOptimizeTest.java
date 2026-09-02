package test;

import java.io.UnsupportedEncodingException;

/** Regression coverage for miniJVM's allocation-reduced String paths. */
public class StringOptimizeTest {

    private static int checks;

    private static void check(String label, boolean condition) {
        checks++;
        if (!condition) {
            throw new RuntimeException("FAILED: " + label);
        }
    }

    private static void checkInt(int value) {
        String expected = Integer.toString(value);
        String actual = new StringBuilder("prefix:").append(value).toString();
        check("append int " + value, actual.equals("prefix:" + expected));
    }

    private static void checkLong(long value) {
        String expected = Long.toString(value);
        String actual = new StringBuilder("prefix:").append(value).toString();
        check("append long " + value, actual.equals("prefix:" + expected));
    }

    private static void testNumbers() {
        checkInt(0);
        checkInt(1);
        checkInt(-1);
        checkInt(9);
        checkInt(10);
        checkInt(65535);
        checkInt(65536);
        checkInt(Integer.MAX_VALUE);
        checkInt(Integer.MIN_VALUE);

        checkLong(0L);
        checkLong(1L);
        checkLong(-1L);
        checkLong(2147483648L);
        checkLong(-2147483649L);
        checkLong(Long.MAX_VALUE);
        checkLong(Long.MIN_VALUE);
    }

    private static void testSharedToString() {
        StringBuilder builder = new StringBuilder("abc");
        String first = builder.toString();
        builder.append("def");
        check("append after shared toString", first.equals("abc"));
        check("appended result", builder.toString().equals("abcdef"));

        String second = builder.toString();
        builder.setCharAt(0, 'X');
        check("setCharAt copy-on-write", second.equals("abcdef"));
        check("setCharAt builder result", builder.toString().equals("Xbcdef"));

        String third = builder.toString();
        builder.delete(1, 3);
        check("delete copy-on-write", third.equals("Xbcdef"));
        check("delete builder result", builder.toString().equals("Xdef"));

        StringBuilder oversized = new StringBuilder(1024);
        oversized.append("small");
        String compact = oversized.toString();
        oversized.setCharAt(0, 'S');
        check("oversized builder compact result", compact.equals("small"));
        check("empty builder", new StringBuilder().toString().equals(""));
    }

    private static void testIndexOfAndContentEquals() {
        StringBuilder builder = new StringBuilder("ababa");
        check("builder indexOf first", builder.indexOf("aba") == 0);
        check("builder indexOf from", builder.indexOf("aba", 1) == 2);
        check("builder indexOf missing", builder.indexOf("aba", 3) == -1);
        check("builder indexOf negative from", builder.indexOf("ab", -10) == 0);
        check("builder indexOf empty past end", builder.indexOf("", 99) == 5);
        check("String indexOf empty past end", "abc".indexOf("", 99) == 3);

        boolean nullRejected = false;
        try {
            builder.indexOf(null);
        } catch (NullPointerException expected) {
            nullRejected = true;
        }
        check("builder indexOf null", nullRejected);

        String expected = "content";
        check("contentEquals String", expected.contentEquals("content"));
        check("contentEquals builder",
                expected.contentEquals(new StringBuilder("content")));
        check("contentEquals buffer",
                expected.contentEquals(new StringBuffer("content")));
        check("contentEquals generic",
                expected.contentEquals(new SimpleSequence("content")));
        check("contentEquals mismatch",
                !expected.contentEquals(new StringBuilder("contents")));
    }

    private static void testCharSequenceAppend() {
        CharSequence source = new StringBuilder("abcdef");
        check("append CharSequence",
                new StringBuilder("x").append(source).toString().equals("xabcdef"));
        check("append CharSequence range",
                new StringBuilder("x").append(source, 1, 4).toString().equals("xbcd"));
        check("append generic CharSequence",
                new StringBuilder().append(new SimpleSequence("generic"))
                        .toString().equals("generic"));

        StringBuilder self = new StringBuilder("abc");
        self.append(self);
        check("append self", self.toString().equals("abcabc"));

        self = new StringBuilder("abcd");
        self.append(self, 1, 3);
        check("append self range", self.toString().equals("abcdbc"));

        check("append null CharSequence",
                new StringBuilder().append((CharSequence) null)
                        .toString().equals("null"));
        check("append null CharSequence range",
                new StringBuilder().append((CharSequence) null, 1, 3)
                        .toString().equals("ul"));

        StringBuilder shared = new StringBuilder("old");
        String snapshot = shared.toString();
        shared.append(source, 0, 2);
        check("append CharSequence keeps shared String", snapshot.equals("old"));
        check("append CharSequence shared result", shared.toString().equals("oldab"));

        boolean rangeRejected = false;
        try {
            new StringBuilder().append(source, 3, 2);
        } catch (IndexOutOfBoundsException expected) {
            rangeRejected = true;
        }
        check("append CharSequence invalid range", rangeRejected);
    }

    private static void checkInsertInt(int value) {
        String actual = new StringBuilder("ab").insert(1, value).toString();
        check("insert int " + value,
                actual.equals("a" + Integer.toString(value) + "b"));
    }

    private static void checkInsertLong(long value) {
        String actual = new StringBuilder("ab").insert(1, value).toString();
        check("insert long " + value,
                actual.equals("a" + Long.toString(value) + "b"));
    }

    private static void testNumberInsert() {
        checkInsertInt(0);
        checkInsertInt(-1);
        checkInsertInt(Integer.MAX_VALUE);
        checkInsertInt(Integer.MIN_VALUE);
        checkInsertLong(0L);
        checkInsertLong(-1L);
        checkInsertLong(Long.MAX_VALUE);
        checkInsertLong(Long.MIN_VALUE);

        StringBuilder shared = new StringBuilder("tail");
        String snapshot = shared.toString();
        shared.insert(0, 123);
        check("insert number copy-on-write", snapshot.equals("tail"));
        check("insert number result", shared.toString().equals("123tail"));

        boolean offsetRejected = false;
        try {
            new StringBuilder("x").insert(2, 1);
        } catch (StringIndexOutOfBoundsException expected) {
            offsetRejected = true;
        }
        check("insert int invalid offset", offsetRejected);
    }

    private static final class SimpleSequence implements CharSequence {
        private final String value;

        SimpleSequence(String value) {
            this.value = value;
        }

        public int length() {
            return value.length();
        }

        public char charAt(int index) {
            return value.charAt(index);
        }

        public CharSequence subSequence(int start, int end) {
            return value.substring(start, end);
        }

        public String toString() {
            return value;
        }
    }

    private static void testUtf8() throws UnsupportedEncodingException {
        String text = "ASCII-中文-\ud83d\ude03";
        byte[] bytes = text.getBytes("utf-8");
        check("UTF-8 exact decode", new String(bytes, "utf-8").equals(text));

        byte[] wrapped = new byte[bytes.length + 4];
        System.arraycopy(bytes, 0, wrapped, 2, bytes.length);
        check("UTF-8 slice decode",
                new String(wrapped, 2, bytes.length, "utf-8").equals(text));
        check("UTF-8 empty decode", new String(new byte[0], "utf-8").equals(""));

        boolean invalidRangeRejected = false;
        try {
            new String(bytes, -1, 1, "utf-8");
        } catch (StringIndexOutOfBoundsException expected) {
            invalidRangeRejected = true;
        }
        check("UTF-8 negative offset", invalidRangeRejected);

        invalidRangeRejected = false;
        try {
            new String(bytes, 0, bytes.length + 1, "utf-8");
        } catch (StringIndexOutOfBoundsException expected) {
            invalidRangeRejected = true;
        }
        check("UTF-8 oversized length", invalidRangeRejected);

        String osName = System.getProperty("os.name");
        check("native-created String", osName != null && osName.length() > 0);
    }

    public static void main(String[] args) throws Exception {
        testNumbers();
        testSharedToString();
        testIndexOfAndContentEquals();
        testCharSequenceAppend();
        testNumberInsert();
        testUtf8();
        System.out.println("StringOptimizeTest OK checks=" + checks);
    }
}
