/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package java.nio.charset;

/**
 *
 * @author Gust
 */
public abstract class Charset
        implements Comparable<Charset> {

    private final String name;          // tickles a bug in oldjavac
    private final String[] aliases;     // tickles a bug in oldjavac

    protected Charset(String canonicalName, String[] aliases) {
        checkName(canonicalName);
        String[] as = (aliases == null) ? new String[0] : aliases;
        for (int i = 0; i < as.length; i++) {
            checkName(as[i]);
        }
        this.name = canonicalName;
        this.aliases = as;
    }

    private static void checkName(String s) {
        int n = s.length();
        if (n == 0) {
            throw new IllegalCharsetNameException(s);
        }
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                continue;
            }
            if (c >= 'a' && c <= 'z') {
                continue;
            }
            if (c >= '0' && c <= '9') {
                continue;
            }
            if (c == '-' && i != 0) {
                continue;
            }
            if (c == '+' && i != 0) {
                continue;
            }
            if (c == ':' && i != 0) {
                continue;
            }
            if (c == '_' && i != 0) {
                continue;
            }
            if (c == '.' && i != 0) {
                continue;
            }
            throw new IllegalCharsetNameException(s);
        }
    }
    private static volatile Charset defaultCharset;

    /**
     * Returns the default charset of this Java virtual machine.
     *
     * <p>
     * The default charset is determined during virtual-machine startup and
     * typically depends upon the locale and charset of the underlying operating
     * system.
     *
     * @return A charset object for the default charset
     *
     * @since 1.5
     */
    public static Charset defaultCharset() {
        if (defaultCharset == null) {
            synchronized (Charset.class) {
                String csn = System.getProperty("file.encoding");

                if (csn != null) {
                    defaultCharset = forName(csn);
                } else {
                    defaultCharset = forName("UTF-8");
                }
            }
        }
        return defaultCharset;
    }

    static Charset forName(String cname) {
        Charset c = null;
        if (cname != null) {

            String n = cname.toLowerCase();
            switch (n) {
                case "utf-8":
                default: {
                    c = new CharsetUTF8(n, null);
                    break;
                }

            }
        }
        return c;
    }

    public final String name() {
        return name;
    }
    
    public String toString(){
        return name;
    }

    public static class CharsetUTF8 extends Charset {

        CharsetUTF8(String canonicalName, String[] aliases) {
            super(canonicalName, aliases);
        }

        @Override
        public int compareTo(Charset o) {
            return CharsetUTF8.this.name().compareTo(o.name());
        }

    }
}
