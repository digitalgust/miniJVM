/* SPDX-License-Identifier: MIT */
package javax.microedition.io;

import java.util.Date;

/** Compatibility wrapper for HTTP-date parsing. */
public final class DateParser {
    private DateParser() {
    }

    public static long parse(String value) {
        return Date.parse(value);
    }

    static void setTimeZone(String ignored) {
        // Date.parse handles explicit zones and the default zone itself.
    }
}
