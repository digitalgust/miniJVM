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

    private static void testUtf8() throws UnsupportedEncodingException {
        String text = "ASCII-中文-\ud83d\ude03";
        byte[] bytes = text.getBytes("utf-8");
        check("UTF-8 exact decode", new String(bytes, "utf-8").equals(text));

        byte[] wrapped = new byte[bytes.length + 4];
        System.arraycopy(bytes, 0, wrapped, 2, bytes.length);
        check("UTF-8 slice decode",
                new String(wrapped, 2, bytes.length, "utf-8").equals(text));
        check("UTF-8 empty decode", new String(new byte[0], "utf-8").equals(""));

        String osName = System.getProperty("os.name");
        check("native-created String", osName != null && osName.length() > 0);
    }

    public static void main(String[] args) throws Exception {
        testNumbers();
        testSharedToString();
        testUtf8();
        System.out.println("StringOptimizeTest OK checks=" + checks);
    }
}
