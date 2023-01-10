/*
 * Copyright (c) 2003, 2006, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */


package java.util;

/**
 * The class Date represents a specific instant in time, with millisecond
 * precision.
 * <p>
 * This class has been subset for the J2ME based on the JDK 1.3 Date class.
 * Many methods and variables have been pruned, and other methods
 * simplified, in an effort to reduce the size of this class.
 * <p>
 * Although the Date class is intended to reflect coordinated universal
 * time (UTC), it may not do so exactly, depending on the host environment
 * of the Java Virtual Machine. Nearly all modern operating systems assume
 * that 1 day = 24x60x60 = 86400 seconds in all cases. In UTC, however,
 * about once every year or two there is an extra second, called a "leap
 * second." The leap second is always added as the last second of the
 * day, and always on December 31 or June 30. For example, the last minute
 * of the year 1995 was 61 seconds long, thanks to an added leap second.
 * Most computer clocks are not accurate enough to be able to reflect the
 * leap-second distinction.
 *
 * @author James Gosling, Roger Riggs, Brian Modra
 * Kinsley Wong, Antero Taivalsaari
 * @version CLDC 1.1 03/13/2002 (Based on JDK 1.3)
 * @see java.util.TimeZone
 * @see java.util.Calendar
 */

public class Date {

    /* If calendar is null, then fastTime indicates the time in millis.
     * Otherwise, fastTime is ignored, and calendar indicates the time.
     */
    private Calendar calendar;
    private long fastTime;

    /**
     * Allocates a <code>Date</code> object and initializes it to
     * represent the current time specified number of milliseconds since the
     * standard base time known as "the epoch", namely January 1,
     * 1970, 00:00:00 GMT.
     *
     * @see java.lang.System#currentTimeMillis()
     */
    public Date() {
        this(System.currentTimeMillis());
    }

    /**
     * Allocates a <code>Date</code> object and initializes it to
     * represent the specified number of milliseconds since the
     * standard base time known as "the epoch", namely January 1,
     * 1970, 00:00:00 GMT.
     *
     * @param date the milliseconds since January 1, 1970, 00:00:00 GMT.
     * @see java.lang.System#currentTimeMillis()
     */
    public Date(long date) {
        calendar = Calendar.getInstance();
        if (calendar != null) {
            calendar.setTimeInMillis(date);
        }
        fastTime = date;
    }

    /**
     * Returns the number of milliseconds since January 1, 1970, 00:00:00 GMT
     * represented by this <tt>Date</tt> object.
     *
     * @return the number of milliseconds since January 1, 1970, 00:00:00 GMT
     * represented by this date.
     * @see #setTime
     */
    public long getTime() {
        if (calendar != null) {
            return calendar.getTimeInMillis();
        } else {
            return fastTime;
        }
    }

    /**
     * Sets this <tt>Date</tt> object to represent a point in time that is
     * <tt>time</tt> milliseconds after January 1, 1970 00:00:00 GMT.
     *
     * @param time the number of milliseconds.
     * @see #getTime
     */
    public void setTime(long time) {
        if (calendar != null) {
            calendar.setTimeInMillis(time);
        }
        fastTime = time;
    }

    /**
     * Compares two dates for equality.
     * The result is <code>true</code> if and only if the argument is
     * not <code>null</code> and is a <code>Date</code> object that
     * represents the same point in time, to the millisecond, as this object.
     * <p>
     * Thus, two <code>Date</code> objects are equal if and only if the
     * <code>getTime</code> method returns the same <code>long</code>
     * value for both.
     *
     * @param obj the object to compare with.
     * @return <code>true</code> if the objects are the same;
     * <code>false</code> otherwise.
     * @see java.util.Date#getTime()
     */
    public boolean equals(Object obj) {
        return obj != null && obj instanceof Date && getTime() == ((Date) obj).getTime();
    }

    /**
     * Returns a hash code value for this object. The result is the
     * exclusive OR of the two halves of the primitive <tt>long</tt>
     * value returned by the {@link Date#getTime}
     * method. That is, the hash code is the value of the expression:
     * <blockquote><pre>
     * (int)(this.getTime()^(this.getTime() >>> 32))</pre></blockquote>
     *
     * @return a hash code value for this object.
     */
    public int hashCode() {
        long ht = getTime();
        return (int) ht ^ (int) (ht >> 32);
    }

    /**
     * Converts this <code>Date</code> object to a <code>String</code>
     * of the form:
     * <blockquote><pre>
     * dow mon dd hh:mm:ss zzz yyyy</pre></blockquote>
     * where:<ul>
     * <li><tt>dow</tt> is the day of the week (<tt>Sun, Mon, Tue, Wed,
     * Thu, Fri, Sat</tt>).
     * <li><tt>mon</tt> is the month (<tt>Jan, Feb, Mar, Apr, May, Jun,
     * Jul, Aug, Sep, Oct, Nov, Dec</tt>).
     * <li><tt>dd</tt> is the day of the month (<tt>01</tt> through
     * <tt>31</tt>), as two decimal digits.
     * <li><tt>hh</tt> is the hour of the day (<tt>00</tt> through
     * <tt>23</tt>), as two decimal digits.
     * <li><tt>mm</tt> is the minute within the hour (<tt>00</tt> through
     * <tt>59</tt>), as two decimal digits.
     * <li><tt>ss</tt> is the second within the minute (<tt>00</tt> through
     * <tt>61</tt>, as two decimal digits.
     * <li><tt>zzz</tt> is the time zone (and may reflect daylight savings
     * time). If time zone information is not available,
     * then <tt>zzz</tt> is empty - that is, it consists
     * of no characters at all.
     * <li><tt>yyyy</tt> is the year, as four decimal digits.
     * </ul>
     *
     * @return a string representation of this date.
     * @since CLDC 1.1
     */
    public String toString() {
        return com.sun.microedition.util.mini.CalendarImpl.toString(calendar);
    }


    public static long parse(String s) {
        if (staticCal == null)
            makeStaticCalendars(); // Called only for side-effect of setting defaultCenturyStart

        int year = Integer.MIN_VALUE;
        int mon = -1;
        int mday = -1;
        int hour = -1;
        int min = -1;
        int sec = -1;
        int millis = -1;
        int c = -1;
        int i = 0;
        int n = -1;
        int wst = -1;
        int tzoffset = -1;
        int prevc = 0;
        syntax:
        {
            if (s == null)
                break syntax;
            int limit = s.length();
            while (i < limit) {
                c = s.charAt(i);
                i++;
                if (c <= ' ' || c == ',')
                    continue;
                if (c == '(') { // skip comments
                    int depth = 1;
                    while (i < limit) {
                        c = s.charAt(i);
                        i++;
                        if (c == '(') depth++;
                        else if (c == ')')
                            if (--depth <= 0)
                                break;
                    }
                    continue;
                }
                if ('0' <= c && c <= '9') {
                    n = c - '0';
                    while (i < limit && '0' <= (c = s.charAt(i)) && c <= '9') {
                        n = n * 10 + c - '0';
                        i++;
                    }
                    if (prevc == '+' || prevc == '-' && year != Integer.MIN_VALUE) {
                        // timezone offset
                        if (n < 24)
                            n = n * 60; // EG. "GMT-3"
                        else
                            n = n % 100 + n / 100 * 60; // eg "GMT-0430"
                        if (prevc == '+')   // plus means east of GMT
                            n = -n;
                        if (tzoffset != 0 && tzoffset != -1)
                            break syntax;
                        tzoffset = n;
                    } else if (n >= 70)
                        if (year != Integer.MIN_VALUE)
                            break syntax;
                        else if (c <= ' ' || c == ',' || c == '/' || i >= limit)
                            // year = n < 1900 ? n : n - 1900;
                            year = n;
                        else
                            break syntax;
                    else if (c == ':')
                        if (hour < 0)
                            hour = (byte) n;
                        else if (min < 0)
                            min = (byte) n;
                        else
                            break syntax;
                    else if (c == '/')
                        if (mon < 0)
                            mon = (byte) (n - 1);
                        else if (mday < 0)
                            mday = (byte) n;
                        else
                            break syntax;
                    else if (i < limit && c != ',' && c > ' ' && c != '-')
                        break syntax;
                    else if (hour >= 0 && min < 0)
                        min = (byte) n;
                    else if (min >= 0 && sec < 0)
                        sec = (byte) n;
                    else if (mday < 0)
                        mday = (byte) n;
                        // Handle two-digit years < 70 (70-99 handled above).
                    else if (year == Integer.MIN_VALUE && mon >= 0 && mday >= 0)
                        year = n;
                    else
                        break syntax;
                    prevc = 0;
                } else if (c == '/' || c == ':' || c == '+' || c == '-')
                    prevc = c;
                else {
                    int st = i - 1;
                    while (i < limit) {
                        c = s.charAt(i);
                        if (!('A' <= c && c <= 'Z' || 'a' <= c && c <= 'z'))
                            break;
                        i++;
                    }
                    if (i <= st + 1)
                        break syntax;
                    int k;
                    for (k = wtb.length; --k >= 0; )
                        if (wtb[k].regionMatches(true, 0, s, st, i - st)) {
                            int action = ttb[k];
                            if (action != 0) {
                                if (action == 1) {  // pm
                                    if (hour > 12 || hour < 1)
                                        break syntax;
                                    else if (hour < 12)
                                        hour += 12;
                                } else if (action == 14) {  // am
                                    if (hour > 12 || hour < 1)
                                        break syntax;
                                    else if (hour == 12)
                                        hour = 0;
                                } else if (action <= 13) {  // month!
                                    if (mon < 0)
                                        mon = (byte) (action - 2);
                                    else
                                        break syntax;
                                } else {
                                    tzoffset = action - 10000;
                                }
                            }
                            break;
                        }
                    if (k < 0)
                        break syntax;
                    prevc = 0;
                }
            }
            if (year == Integer.MIN_VALUE || mon < 0 || mday < 0)
                break syntax;
            // Parse 2-digit years within the correct default century.
            if (year < 100) {
                year += (defaultCenturyStart / 100) * 100;
                if (year < defaultCenturyStart) year += 100;
            }
            year -= 1900;
            if (sec < 0)
                sec = 0;
            if (min < 0)
                min = 0;
            if (hour < 0)
                hour = 0;
            if (tzoffset == -1) // no time zone specified, have to use local
                return UTC(year, mon, mday, hour, min, sec);
            return UTC(year, mon, mday, hour, min, sec) + tzoffset * (60 * 1000);
        }
        // syntax error
        throw new IllegalArgumentException();
    }

    private final static String wtb[] = {
            "am", "pm",
            "monday", "tuesday", "wednesday", "thursday", "friday",
            "saturday", "sunday",
            "january", "february", "march", "april", "may", "june",
            "july", "august", "september", "october", "november", "december",
            "gmt", "ut", "utc", "est", "edt", "cst", "cdt",
            "mst", "mdt", "pst", "pdt"
            // this time zone table needs to be expanded
    };
    private final static int ttb[] = {
            14, 1, 0, 0, 0, 0, 0, 0, 0,
            2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
            10000 + 0, 10000 + 0, 10000 + 0,    // GMT/UT/UTC
            10000 + 5 * 60, 10000 + 4 * 60, // EST/EDT
            10000 + 6 * 60, 10000 + 5 * 60,
            10000 + 7 * 60, 10000 + 6 * 60,
            10000 + 8 * 60, 10000 + 7 * 60
    };
    private static Calendar staticCal = null;
    private static Calendar utcCal = null;
    private static int defaultCenturyStart = 0;

    public static long UTC(int year, int month, int date,
                           int hrs, int min, int sec) {
        if (utcCal == null)
            makeStaticCalendars();
        synchronized (utcCal) {
            utcCal.set(Calendar.YEAR, year + 1900);
            utcCal.set(Calendar.MONTH, month);
            utcCal.set(Calendar.DAY_OF_MONTH, date);
            utcCal.set(Calendar.HOUR_OF_DAY, hrs);
            utcCal.set(Calendar.MINUTE, min);
            utcCal.set(Calendar.SECOND, sec);

            //utcCal.set(year + 1900, month, date, hrs, min, sec);
            return utcCal.getTimeInMillis();
        }
    }

    private synchronized static void makeStaticCalendars() {
        if (staticCal == null) {
            Calendar calendar = Calendar.getInstance();
            utcCal = Calendar.getInstance();
            utcCal.setTimeZone(TimeZone.getTimeZone("GMT"));
            defaultCenturyStart = calendar.get(Calendar.YEAR) - 80;
            staticCal = calendar;
        }
    }
}

