/* SPDX-License-Identifier: MIT */
package com.sun.cldc.util.j2me;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Compact proleptic-Gregorian calendar for the CLDC Calendar subset.
 *
 * <p>This implementation was written for miniJVM. It deliberately supports
 * fixed-offset zones only; that is the level required by CLDC 1.1.</p>
 */
public final class CalendarImpl extends Calendar {
    private static final long MILLIS_PER_DAY = 86400000L;
    private static final String[] MONTHS = {
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };
    private static final String[] DAYS = {
            "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
    };

    public CalendarImpl() {
        super();
    }

    protected void computeFields() {
        TimeZone zone = getTimeZone();
        long local = time + zone.getRawOffset();
        long epochDay = floorDiv(local, MILLIS_PER_DAY);
        int millisInDay = (int) floorMod(local, MILLIS_PER_DAY);
        int[] date = civilFromDays(epochDay);

        fields[YEAR] = date[0];
        fields[MONTH] = date[1] - 1;
        fields[DATE] = date[2];
        fields[DAY_OF_WEEK] = (int) floorMod(epochDay + 4, 7) + SUNDAY;
        fields[HOUR_OF_DAY] = millisInDay / 3600000;
        millisInDay %= 3600000;
        fields[MINUTE] = millisInDay / 60000;
        millisInDay %= 60000;
        fields[SECOND] = millisInDay / 1000;
        fields[MILLISECOND] = millisInDay % 1000;
        fields[AM_PM] = fields[HOUR_OF_DAY] < 12 ? AM : PM;
        fields[HOUR] = fields[HOUR_OF_DAY] % 12;
        for (int i = 0; i < isSet.length; i++) {
            isSet[i] = false;
        }
    }

    protected void computeTime() {
        int year = fields[YEAR] == 0 ? 1970 : fields[YEAR];
        int month = fields[MONTH];
        int day = fields[DATE] == 0 ? 1 : fields[DATE];
        int hour;
        if (isSet[HOUR_OF_DAY]) {
            hour = (int) floorMod(fields[HOUR_OF_DAY], 24L);
        } else {
            hour = (int) floorMod(fields[HOUR], 12L);
            if (fields[AM_PM] == PM) {
                hour += 12;
            }
        }

        long normalizedMonth = (long) year * 12 + month;
        int normalizedYear = (int) floorDiv(normalizedMonth, 12L);
        int normalizedMonthOneBased = (int) floorMod(normalizedMonth, 12L) + 1;
        long local = daysFromCivil(normalizedYear, normalizedMonthOneBased, 1)
                * MILLIS_PER_DAY;
        local += (long) (day - 1) * MILLIS_PER_DAY;
        local += (long) hour * 3600000L;
        local += (long) fields[MINUTE] * 60000L;
        local += (long) fields[SECOND] * 1000L;
        local += fields[MILLISECOND];

        int offset = getTimeZone().getRawOffset();
        time = local - offset;
        computeFields();
    }

    /** Returns the legacy Date string representation used by CLDC. */
    public static String toString(Calendar calendar) {
        if (calendar == null) {
            return "Thu Jan 01 00:00:00 UTC 1970";
        }
        StringBuffer out = new StringBuffer(32);
        out.append(DAYS[calendar.get(DAY_OF_WEEK) - 1]).append(' ');
        out.append(MONTHS[calendar.get(MONTH)]).append(' ');
        appendTwo(out, calendar.get(DATE)).append(' ');
        appendTwo(out, calendar.get(HOUR_OF_DAY)).append(':');
        appendTwo(out, calendar.get(MINUTE)).append(':');
        appendTwo(out, calendar.get(SECOND)).append(' ');
        String id = calendar.getTimeZone().getID();
        if (id != null && id.length() != 0) {
            out.append(id).append(' ');
        }
        appendFour(out, calendar.get(YEAR));
        return out.toString();
    }

    /** Returns the CLDC ISO-like form: yyyy MM dd hh mm ss +zzzz. */
    public static String toISO8601String(Calendar calendar) {
        if (calendar == null) {
            return "0000 00 00 00 00 00 +0000";
        }
        StringBuffer out = new StringBuffer(28);
        appendFour(out, calendar.get(YEAR)).append(' ');
        appendTwo(out, calendar.get(MONTH) + 1).append(' ');
        appendTwo(out, calendar.get(DATE)).append(' ');
        appendTwo(out, calendar.get(HOUR_OF_DAY)).append(' ');
        appendTwo(out, calendar.get(MINUTE)).append(' ');
        appendTwo(out, calendar.get(SECOND)).append(' ');
        int minutes = calendar.getTimeZone().getRawOffset() / 60000;
        out.append(minutes < 0 ? '-' : '+');
        if (minutes < 0) {
            minutes = -minutes;
        }
        appendTwo(out, minutes / 60);
        appendTwo(out, minutes % 60);
        return out.toString();
    }

    private static StringBuffer appendTwo(StringBuffer out, int value) {
        if (value < 10 && value >= 0) {
            out.append('0');
        }
        return out.append(value);
    }

    private static StringBuffer appendFour(StringBuffer out, int value) {
        if (value >= 0 && value < 1000) out.append('0');
        if (value >= 0 && value < 100) out.append('0');
        if (value >= 0 && value < 10) out.append('0');
        return out.append(value);
    }

    /* Howard Hinnant's civil-date algorithm, independently expressed here. */
    private static long daysFromCivil(int year, int month, int day) {
        year -= month <= 2 ? 1 : 0;
        long era = floorDiv(year, 400);
        int yearOfEra = (int) (year - era * 400);
        int shiftedMonth = month + (month > 2 ? -3 : 9);
        int dayOfYear = (153 * shiftedMonth + 2) / 5 + day - 1;
        int dayOfEra = yearOfEra * 365 + yearOfEra / 4
                - yearOfEra / 100 + dayOfYear;
        return era * 146097L + dayOfEra - 719468L;
    }

    private static int[] civilFromDays(long days) {
        days += 719468L;
        long era = floorDiv(days, 146097L);
        int dayOfEra = (int) (days - era * 146097L);
        int yearOfEra = (dayOfEra - dayOfEra / 1460 + dayOfEra / 36524
                - dayOfEra / 146096) / 365;
        int year = (int) (yearOfEra + era * 400L);
        int dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4
                - yearOfEra / 100);
        int mp = (5 * dayOfYear + 2) / 153;
        int day = dayOfYear - (153 * mp + 2) / 5 + 1;
        int month = mp + (mp < 10 ? 3 : -9);
        year += month <= 2 ? 1 : 0;
        return new int[] {year, month, day};
    }

    private static long floorDiv(long value, long divisor) {
        long result = value / divisor;
        if ((value ^ divisor) < 0 && result * divisor != value) {
            result--;
        }
        return result;
    }

    private static long floorMod(long value, long divisor) {
        return value - floorDiv(value, divisor) * divisor;
    }
}
