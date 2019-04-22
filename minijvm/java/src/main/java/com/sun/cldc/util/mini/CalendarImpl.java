/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.cldc.util.mini;

import java.util.*;
import java.io.*;
import java.lang.*;
/**
 * This class is an implementation of the subsetted
 * CLDC 1.1 Calendar class.
 *
 * @author Kinsley Wong
 * @see java.util.Calendar
 * @see java.util.TimeZone
 */
public class CalendarImpl extends Calendar {

    /* ERA */
    private static final int BC = 0;
    private static final int AD = 1;

    /* January 1, year 1 (Gregorian) */
    private static final int JAN_1_1_JULIAN_DAY = 1721426;

    /* January 1, 1970 (Gregorian) */
    private static final int EPOCH_JULIAN_DAY   = 2440588;

    /* 0-based, for day-in-year */
    private static final int NUM_DAYS[]
    = {0,31,59,90,120,151,181,212,243,273,304,334};

    /* 0-based, for day-in-year */
    private static final int LEAP_NUM_DAYS[]
    = {0,31,60,91,121,152,182,213,244,274,305,335};

    /**
     * Useful millisecond constants.  Although ONE_DAY and ONE_WEEK can fit
     * into ints, they must be longs in order to prevent arithmetic overflow
     * when performing (bug 4173516).
     */
    private static final int  ONE_SECOND = 1000;
    private static final int  ONE_MINUTE = 60*ONE_SECOND;
    private static final int  ONE_HOUR   = 60*ONE_MINUTE;
    private static final long ONE_DAY    = 24*ONE_HOUR;
    private static final long ONE_WEEK   = 7*ONE_DAY;

    /**
     * The point at which the Gregorian calendar rules are used, measured in
     * milliseconds from the standard epoch.  Default is October 15, 1582
     * (Gregorian) 00:00:00 UTC or -12219292800000L.  For this value, October 4,
     * 1582 (Julian) is followed by October 15, 1582 (Gregorian).  This
     * corresponds to Julian day number 2299161.
     */
    private static final long gregorianCutover = -12219292800000L;

    /**
     * The year of the gregorianCutover, with 0 representing
     * 1 BC, -1 representing 2 BC, etc.
     */
    private static final int gregorianCutoverYear = 1582;

    public CalendarImpl() {
        super();
    }

    /**
     * Converts UTC as milliseconds to time field values.
     */
    protected void computeFields() {
        int rawOffset = getTimeZone().getRawOffset();
        long localMillis = time + rawOffset;

        // Check for very extreme values -- millis near Long.MIN_VALUE or
        // Long.MAX_VALUE.  For these values, adding the zone offset can push
        // the millis past MAX_VALUE to MIN_VALUE, or vice versa.  This produces
        // the undesirable effect that the time can wrap around at the ends,
        // yielding, for example, a Date(Long.MAX_VALUE) with a big BC year
        // (should be AD).  Handle this by pinning such values to Long.MIN_VALUE
        // or Long.MAX_VALUE. - liu 8/11/98 bug 4149677
        if (time > 0 && localMillis < 0 && rawOffset > 0) {
            localMillis = Long.MAX_VALUE;
        } else if (time < 0 && localMillis > 0 && rawOffset < 0) {
            localMillis = Long.MIN_VALUE;
        }

        // Time to fields takes the wall millis (Standard or DST).
        timeToFields(localMillis);

        long days = (long)(localMillis / ONE_DAY);
        int millisInDay = (int)(localMillis - (days * ONE_DAY));

        if (millisInDay < 0) millisInDay += ONE_DAY;

        // Call getOffset() to get the TimeZone offset.
        // The millisInDay value must be standard local millis.
        int dstOffset = getTimeZone().getOffset(AD,
                                 this.fields[YEAR],
                                 this.fields[MONTH],
                                 this.fields[DATE],
                                 this.fields[DAY_OF_WEEK],
                                 millisInDay) - rawOffset;

        // Adjust our millisInDay for DST, if necessary.
        millisInDay += dstOffset;

        // If DST has pushed us into the next day,
        // we must call timeToFields() again.
        // This happens in DST between 12:00 am and 1:00 am every day.
        // The call to timeToFields() will give the wrong day,
        // since the Standard time is in the previous day
        if (millisInDay >= ONE_DAY) {
            long dstMillis = localMillis + dstOffset;
            millisInDay -= ONE_DAY;
            // As above, check for and pin extreme values
            if (localMillis > 0 && dstMillis < 0 && dstOffset > 0) {
                dstMillis = Long.MAX_VALUE;
            } else if (localMillis < 0 && dstMillis > 0 && dstOffset < 0) {
                dstMillis = Long.MIN_VALUE;
            }
            timeToFields(dstMillis);
        }

        // Fill in all time-related fields based on millisInDay.
        // so as not to perturb flags.
        this.fields[MILLISECOND] = millisInDay % 1000;
        millisInDay /= 1000;

        this.fields[SECOND] = millisInDay % 60;
        millisInDay /= 60;

        this.fields[MINUTE] = millisInDay % 60;
        millisInDay /= 60;

        this.fields[HOUR_OF_DAY] = millisInDay;
        this.fields[AM_PM] = millisInDay / 12;
        this.fields[HOUR] = millisInDay % 12;
    }

    /**
     * Convert the time as milliseconds to the date fields.  Millis must be
     * given as local wall millis to get the correct local day.  For example,
     * if it is 11:30 pm Standard, and DST is in effect, the correct DST millis
     * must be passed in to get the right date.
     * <p>
     * Fields that are completed by this method: YEAR, MONTH, DATE, DAY_OF_WEEK.
     * @param theTime the time in wall millis (either Standard or DST),
     * whichever is in effect
     */
    private final void timeToFields(long theTime) {
        int dayOfYear, weekCount, rawYear;
        boolean isLeap;

        // Compute the year, month, and day of month from the given millis
        if (theTime >= gregorianCutover) {

            // The Gregorian epoch day is zero for Monday January 1, year 1.
            long gregorianEpochDay =
                millisToJulianDay(theTime) - JAN_1_1_JULIAN_DAY;

            // Here we convert from the day number to the multiple radix
            // representation.  We use 400-year, 100-year, and 4-year cycles.
            // For example, the 4-year cycle has 4 years + 1 leap day; giving
            // 1461 == 365*4 + 1 days.
            int[] rem = new int[1];

            // 400-year cycle length
            int n400 = floorDivide(gregorianEpochDay, 146097, rem);

            // 100-year cycle length
            int n100 = floorDivide(rem[0], 36524, rem);

            // 4-year cycle length
            int n4 = floorDivide(rem[0], 1461, rem);

            int n1 = floorDivide(rem[0], 365, rem);
            rawYear = 400*n400 + 100*n100 + 4*n4 + n1;

            // zero-based day of year
            dayOfYear = rem[0];

            // Dec 31 at end of 4- or 400-yr cycle
            if (n100 == 4 || n1 == 4) {
                dayOfYear = 365;
            } else {
                ++rawYear;
            }

            // equiv. to (rawYear%4 == 0)
            isLeap =
                ((rawYear&0x3) == 0) && (rawYear%100 != 0 || rawYear%400 == 0);

            // Gregorian day zero is a Monday
            this.fields[DAY_OF_WEEK] = (int)((gregorianEpochDay+1) % 7);
        } else {
            // The Julian epoch day (not the same as Julian Day)
            // is zero on Saturday December 30, 0 (Gregorian).
            long julianEpochDay =
                millisToJulianDay(theTime) - (JAN_1_1_JULIAN_DAY - 2);

            rawYear = (int) floorDivide(4*julianEpochDay + 1464, 1461);

            // Compute the Julian calendar day number for January 1, year
            long january1 = 365*(rawYear-1) + floorDivide(rawYear-1, 4);
            dayOfYear = (int)(julianEpochDay - january1); // 0-based

            // Julian leap years occurred historically every 4 years starting
            // with 8 AD.  Before 8 AD the spacing is irregular; every 3 years
            // from 45 BC to 9 BC, and then none until 8 AD.  However, we don't
            // implement this historical detail; instead, we implement the
            // computationally cleaner proleptic calendar, which assumes
            // consistent 4-year cycles throughout time.

            // equiv. to (rawYear%4 == 0)
            isLeap = ((rawYear&0x3) == 0);

            // Julian calendar day zero is a Saturday
            this.fields[DAY_OF_WEEK] = (int)((julianEpochDay-1) % 7);
        }

        // Common Julian/Gregorian calculation
        int correction = 0;

        // zero-based DOY for March 1
        int march1 = isLeap ? 60 : 59;

        if (dayOfYear >= march1) correction = isLeap ? 1 : 2;

        // zero-based month
        int month_field = (12 * (dayOfYear + correction) + 6) / 367;

        // one-based DOM
        int date_field = dayOfYear -
            (isLeap ? LEAP_NUM_DAYS[month_field] : NUM_DAYS[month_field]) + 1;

        // Normalize day of week
        this.fields[DAY_OF_WEEK] += (this.fields[DAY_OF_WEEK] < 0) ? (SUNDAY+7) : SUNDAY;

        this.fields[YEAR] = rawYear;

        // If year is < 1 we are in BC
        if (this.fields[YEAR] < 1) {
            this.fields[YEAR] = 1 - this.fields[YEAR];
        }

        // 0-based
        this.fields[MONTH] = month_field + JANUARY;
        this.fields[DATE] = date_field;
    }

    /*
     * The following two static arrays are used privately by the
     * <code>toString(Calendar calendar)</code> function below.
     */
    static String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                              "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    static String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

    /**
     * Converts this <code>Date</code> object to a <code>String</code>
     * of the form:
     * <blockquote><pre>
     * dow mon dd hh:mm:ss zzz yyyy</pre></blockquote>
     * where:<ul>
     * <li><tt>dow</tt> is the day of the week (<tt>Sun, Mon, Tue, Wed,
     *     Thu, Fri, Sat</tt>).
     * <li><tt>mon</tt> is the month (<tt>Jan, Feb, Mar, Apr, May, Jun,
     *     Jul, Aug, Sep, Oct, Nov, Dec</tt>).
     * <li><tt>dd</tt> is the day of the month (<tt>01</tt> through
     *     <tt>31</tt>), as two decimal digits.
     * <li><tt>hh</tt> is the hour of the day (<tt>00</tt> through
     *     <tt>23</tt>), as two decimal digits.
     * <li><tt>mm</tt> is the minute within the hour (<tt>00</tt> through
     *     <tt>59</tt>), as two decimal digits.
     * <li><tt>ss</tt> is the second within the minute (<tt>00</tt> through
     *     <tt>61</tt>, as two decimal digits.
     * <li><tt>zzz</tt> is the time zone (and may reflect daylight savings
     *     time). If time zone information is not available,
     *     then <tt>zzz</tt> is empty - that is, it consists
     *     of no characters at all.
     * <li><tt>yyyy</tt> is the year, as four decimal digits.
     * </ul>
     *
     * @return  a string representation of this date.
     */
    public static String toString(Calendar calendar) {
        // Printing in the absence of a Calendar
        // implementation class is not supported
        if (calendar == null) {
            return "Thu Jan 01 00:00:00 UTC 1970";
        }

        int dow = calendar.get(Calendar.DAY_OF_WEEK);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour_of_day = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int seconds = calendar.get(Calendar.SECOND);
        int year = calendar.get(Calendar.YEAR);

        String yr = Integer.toString(year);

        TimeZone zone = calendar.getTimeZone();
        String zoneID = zone.getID();
        if (zoneID == null) zoneID = "";

        // The total size of the string buffer
        // 3+1+3+1+2+1+2+1+2+1+2+1+zoneID.length+1+yr.length
        //  = 21 + zoneID.length + yr.length
        StringBuffer sb = new StringBuffer(25 + zoneID.length() + yr.length());

        sb.append(days[dow-1]).append(' ');
        sb.append(months[month]).append(' ');
        appendTwoDigits(sb, day).append(' ');
        appendTwoDigits(sb, hour_of_day).append(':');
        appendTwoDigits(sb, minute).append(':');
        appendTwoDigits(sb, seconds).append(' ');
        if (zoneID.length() > 0) sb.append(zoneID).append(' ');
        appendFourDigits(sb, year);

        return sb.toString();
    }

    /**
     * Converts this <code>Date</code> object to a <code>String</code>.
     * The output format is as follows:
     * <blockquote><pre>yyyy MM dd hh mm ss +zzzz</pre></blockquote>
     * where:<ul>
     * <li><dd>yyyy</dd> is the year, as four decimal digits.
     *    Year values larger than <dd>9999</dd> will be truncated
     *    to <dd>9999</dd>.
     * <li><dd>MM</dd> is the month (<dd>01</dd> through <dd>12</dd>),
     *    as two decimal digits.
     * <li><dd>dd</dd> is the day of the month (<dd>01</dd> through
     *    <dd>31</dd>), as two decimal digits.
     * <li><dd>hh</dd> is the hour of the day (<dd>00</dd> through
     *    <dd>23</dd>), as two decimal digits.
     * <li><dd>mm</dd> is the minute within the hour (<dd>00</dd>
     *     through <dd>59</dd>), as two decimal digits.
     * <li><dd>ss</dd> is the second within the minute (<dd>00</dd>
     *    through <dd>59</dd>), as two decimal digits.
     * <li><dd>zzzz</dd> is the time zone offset in hours and minutes
     *      (four decimal digits <dd>"hhmm"</dd>) relative to GMT,
     *      preceded by a "+" or "-" character (<dd>-1200</dd>
     *      through <dd>+1200</dd>).
     *      For instance, Pacific Standard Time zone is printed
     *      as <dd>-0800</dd>.  GMT is printed as <dd>+0000</dd>.
     * </ul>
     *
     * @return  a string representation of this date.
     */
    public static String toISO8601String(Calendar calendar) {
        // Printing in the absence of a Calendar
        // implementation class is not supported
        if (calendar == null) {
            return "0000 00 00 00 00 00 +0000";
        }

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour_of_day = calendar.get(Calendar.HOUR_OF_DAY);
        int hour = calendar.get(Calendar.HOUR);
        int minute = calendar.get(Calendar.MINUTE);
        int seconds = calendar.get(Calendar.SECOND);

        String yr = Integer.toString(year);

        // The total size of the string buffer
        // yr.length+1+2+1+2+1+2+1+2+1+2+1+5 = 25 + yr.length
        StringBuffer sb = new StringBuffer(25 + yr.length());

        appendFourDigits(sb, year).append(' ');
        appendTwoDigits(sb, month).append(' ');
        appendTwoDigits(sb, day).append(' ');
        appendTwoDigits(sb, hour_of_day).append(' ');
        appendTwoDigits(sb, minute).append(' ');
        appendTwoDigits(sb, seconds).append(' ');

        // TimeZone offset is represented in milliseconds.
        // Convert the offset to minutes:
        TimeZone t = calendar.getTimeZone();
        int zoneOffsetInMinutes = t.getRawOffset() / 1000 / 60;

        if (zoneOffsetInMinutes < 0) {
            zoneOffsetInMinutes = Math.abs(zoneOffsetInMinutes);
            sb.append('-');
        } else {
            sb.append('+');
        }

        int zoneHours = zoneOffsetInMinutes / 60;
        int zoneMinutes = zoneOffsetInMinutes % 60;

        appendTwoDigits(sb, zoneHours);
        appendTwoDigits(sb, zoneMinutes);

        return sb.toString();
    }

    private static final StringBuffer appendFourDigits(StringBuffer sb, int number) {
        if (number >= 0 && number < 1000) {
            sb.append('0');
            if (number < 100) {
                sb.append('0');
            }
            if (number < 10) {
                sb.append('0');
            }
        }
        return sb.append(number);
    }

    private static final StringBuffer appendTwoDigits(StringBuffer sb, int number) {
        if (number < 10) {
            sb.append('0');
        }
        return sb.append(number);
    }

    /////////////////////////////
    // Fields => Time computation
    /////////////////////////////

    /**
     * Converts time field values to UTC as milliseconds.
     * @exception IllegalArgumentException if any fields are invalid.
     */
    protected void computeTime() {

        correctTime();

        // This function takes advantage of the fact that unset fields in
        // the time field list have a value of zero.

        // First, use the year to determine whether to use the Gregorian or the
        // Julian calendar. If the year is not the year of the cutover, this
        // computation will be correct. But if the year is the cutover year,
        // this may be incorrect. In that case, assume the Gregorian calendar,
        // make the computation, and then recompute if the resultant millis
        // indicate the wrong calendar has been assumed.

        // A date such as Oct. 10, 1582 does not exist in a Gregorian calendar
        // with the default changeover of Oct. 15, 1582, since in such a
        // calendar Oct. 4 (Julian) is followed by Oct. 15 (Gregorian).  This
        // algorithm will interpret such a date using the Julian calendar,
        // yielding Oct. 20, 1582 (Gregorian).
        int year = this.fields[YEAR];
        boolean isGregorian = year >= gregorianCutoverYear;
        long julianDay = calculateJulianDay(isGregorian, year);
        long millis = julianDayToMillis(julianDay);

        // The following check handles portions of the cutover year BEFORE the
        // cutover itself happens. The check for the julianDate number is for a
        // rare case; it's a hardcoded number, but it's efficient.  The given
        // Julian day number corresponds to Dec 3, 292269055 BC, which
        // corresponds to millis near Long.MIN_VALUE.  The need for the check
        // arises because for extremely negative Julian day numbers, the millis
        // actually overflow to be positive values. Without the check, the
        // initial date is interpreted with the Gregorian calendar, even when
        // the cutover doesn't warrant it.
        if (isGregorian != (millis >= gregorianCutover) &&
            julianDay != -106749550580L) { // See above

            julianDay = calculateJulianDay(!isGregorian, year);
            millis = julianDayToMillis(julianDay);
        }

        // Do the time portion of the conversion.

        int millisInDay = 0;

        // Hours
        // Don't normalize here; let overflow bump into the next period.
        // This is consistent with how we handle other fields.
        millisInDay += this.fields[HOUR_OF_DAY];
        millisInDay *= 60;

        // now get minutes
        millisInDay += this.fields[MINUTE];
        millisInDay *= 60;

        // now get seconds
        millisInDay += this.fields[SECOND];
        millisInDay *= 1000;

        // now get millis
        millisInDay += this.fields[MILLISECOND];

        // Compute the time zone offset and DST offset.  There are two potential
        // ambiguities here.  We'll assume a 2:00 am (wall time) switchover time
        // for discussion purposes here.
        // 1. The transition into DST.  Here, a designated time of 2:00 am - 2:59 am
        //    can be in standard or in DST depending.  However, 2:00 am is an invalid
        //    representation (the representation jumps from 1:59:59 am Std to 3:00:00 am DST).
        //    We assume standard time.
        // 2. The transition out of DST.  Here, a designated time of 1:00 am - 1:59 am
        //    can be in standard or DST.  Both are valid representations (the rep
        //    jumps from 1:59:59 DST to 1:00:00 Std).
        //    Again, we assume standard time.
        // We use the TimeZone object to get the zone offset
        int zoneOffset = getTimeZone().getRawOffset();

        // Now add date and millisInDay together, to make millis contain local wall
        // millis, with no zone or DST adjustments
        millis += millisInDay;

        // Normalize the millisInDay to 0..ONE_DAY-1.  If the millis is out
        // of range, then we must call timeToFields() to recompute our
        // fields.
        int[] normalizedMillisInDay = new int[1];
        floorDivide(millis, (int)ONE_DAY, normalizedMillisInDay);

        // We need to have the month, the day, and the day of the week.
        // Calling timeToFields will compute the MONTH and DATE fields.
        //
        // It's tempting to try to use DAY_OF_WEEK here, if it
        // is set, but we CAN'T.  Even if it's set, it might have
        // been set wrong by the user.  We should rely only on
        // the Julian day number, which has been computed correctly
        // using the disambiguation algorithm above. [LIU]
        int dow = julianDayToDayOfWeek(julianDay);

        // It's tempting to try to use DAY_OF_WEEK here, if it
        // is set, but we CAN'T.  Even if it's set, it might have
        // been set wrong by the user.  We should rely only on
        // the Julian day number, which has been computed correctly
        // using the disambiguation algorithm above. [LIU]
        int dstOffset = getTimeZone().getOffset(AD,
                                 this.fields[YEAR],
                                 this.fields[MONTH],
                                 this.fields[DATE],
                                 dow,
                                 normalizedMillisInDay[0]) -
                                 zoneOffset;
        // Note: Because we pass in wall millisInDay, rather than
        // standard millisInDay, we interpret "1:00 am" on the day
        // of cessation of DST as "1:00 am Std" (assuming the time
        // of cessation is 2:00 am).

        // Store our final computed GMT time, with timezone adjustments.
        time = millis - zoneOffset - dstOffset;
    }

    /**
     * Compute the Julian day number under either the Gregorian or the
     * Julian calendar, using the given year and the remaining fields.
     * @param isGregorian if true, use the Gregorian calendar
     * @param year the adjusted year number, with 0 indicating the
     * year 1 BC, -1 indicating 2 BC, etc.
     * @return the Julian day number
     */
    private final long calculateJulianDay(boolean isGregorian, int year) {
        int month = 0;
        long millis = 0;

        month = this.fields[MONTH] - JANUARY;

        // If the month is out of range, adjust it into range
        if (month < 0 || month > 11) {
            int[] rem = new int[1];
            year += floorDivide(month, 12, rem);
            month = rem[0];
        }

        boolean isLeap = year%4 == 0;

        long julianDay =
            365L*(year - 1) + floorDivide((year - 1), 4) + (JAN_1_1_JULIAN_DAY - 3);

        if (isGregorian) {
            isLeap = isLeap && ((year%100 != 0) || (year%400 == 0));
            // Add 2 because Gregorian calendar starts 2 days after Julian calendar
            julianDay +=
                floorDivide((year - 1), 400) - floorDivide((year - 1), 100) + 2;
        }

        // At this point julianDay is the 0-based day BEFORE the first day of
        // January 1, year 1 of the given calendar.  If julianDay == 0, it
        // specifies (Jan. 1, 1) - 1, in whatever calendar we are using (Julian
        // or Gregorian).
        julianDay += isLeap ? LEAP_NUM_DAYS[month] : NUM_DAYS[month];
        julianDay += this.fields[DATE];
        return julianDay;
    }

    /**
     * Validates the field values for HOUR_OF_DAY, AM_PM and HOUR
     * The calendar will give preference in the following order
     * HOUR_OF_DAY, AM_PM, HOUR
     */
    private void correctTime() {
        int value;

        if (isSet[HOUR_OF_DAY]) {
            value = this.fields[HOUR_OF_DAY] % 24;
            this.fields[HOUR_OF_DAY] = value;
            this.fields[AM_PM] = (value < 12) ? AM : PM;
            this.isSet[HOUR_OF_DAY] = false;
            return;
        }

        if(isSet[AM_PM]) {
            // Determines AM PM with the 24 hour clock
            // This prevents the user from inputing an invalid one.
            if (this.fields[AM_PM] != AM && this.fields[AM_PM] != PM) {
                value = this.fields[HOUR_OF_DAY];
                this.fields[AM_PM] = (value < 12) ? AM : PM;
            }
            this.isSet[AM_PM] = false;
        }

        if (isSet[HOUR]) {
            value = this.fields[HOUR];
            if (value > 12) {
                this.fields[HOUR_OF_DAY] = (value % 12) + 12;
                this.fields[HOUR] = value % 12;
                this.fields[AM_PM] = PM;
            } else {
                if (this.fields[AM_PM] == PM) {
                    this.fields[HOUR_OF_DAY] = value + 12;
                } else {
                    this.fields[HOUR_OF_DAY] = value;
                }
            }
            this.isSet[HOUR] = false;
        }
    }

    /////////////////
    // Implementation
    /////////////////

    /**
     * Converts time as milliseconds to Julian day.
     * @param millis the given milliseconds.
     * @return the Julian day number.
     */
    private static final long millisToJulianDay(long millis) {
        return EPOCH_JULIAN_DAY + floorDivide(millis, ONE_DAY);
    }

    /**
     * Converts Julian day to time as milliseconds.
     * @param julian the given Julian day number.
     * @return time as milliseconds.
     */
    private static final long julianDayToMillis(long julian) {
        return (julian - EPOCH_JULIAN_DAY) * ONE_DAY;
    }

    private static final int julianDayToDayOfWeek(long julian) {
        // If julian is negative, then julian%7 will be negative, so we adjust
        // accordingly.  We add 1 because Julian day 0 is Monday.
        int dayOfWeek = (int)((julian + 1) % 7);
        return dayOfWeek + ((dayOfWeek < 0) ? (7 + SUNDAY) : SUNDAY);
    }

    /**
     * Divide two long integers, returning the floor of the quotient.
     * <p>
     * Unlike the built-in division, this is mathematically well-behaved.
     * E.g., <code>-1/4</code> => 0
     * but <code>floorDivide(-1,4)</code> => -1.
     * @param numerator the numerator
     * @param denominator a divisor which must be > 0
     * @return the floor of the quotient.
     */
    private static final long floorDivide(long numerator, long denominator) {
        // We do this computation in order to handle
        // a numerator of Long.MIN_VALUE correctly
        return (numerator >= 0) ?
            numerator / denominator :
            ((numerator + 1) / denominator) - 1;
    }

    /**
     * Divide two integers, returning the floor of the quotient.
     * <p>
     * Unlike the built-in division, this is mathematically well-behaved.
     * E.g., <code>-1/4</code> => 0
     * but <code>floorDivide(-1,4)</code> => -1.
     * @param numerator the numerator
     * @param denominator a divisor which must be > 0
     * @return the floor of the quotient.
     */
    private static final int floorDivide(int numerator, int denominator) {
        // We do this computation in order to handle
        // a numerator of Integer.MIN_VALUE correctly
        return (numerator >= 0) ?
            numerator / denominator :
            ((numerator + 1) / denominator) - 1;
    }

    /**
     * Divide two integers, returning the floor of the quotient, and
     * the modulus remainder.
     * <p>
     * Unlike the built-in division, this is mathematically well-behaved.
     * E.g., <code>-1/4</code> => 0 and <code>-1%4</code> => -1,
     * but <code>floorDivide(-1,4)</code> => -1 with <code>remainder[0]</code> => 3.
     * @param numerator the numerator
     * @param denominator a divisor which must be > 0
     * @param remainder an array of at least one element in which the value
     * <code>numerator mod denominator</code> is returned. Unlike <code>numerator
     * % denominator</code>, this will always be non-negative.
     * @return the floor of the quotient.
     */
    private static final int
        floorDivide(int numerator, int denominator, int[] remainder) {

        if (numerator >= 0) {
            remainder[0] = numerator % denominator;
            return numerator / denominator;
        }
        int quotient = ((numerator + 1) / denominator) - 1;
        remainder[0] = numerator - (quotient * denominator);
        return quotient;
    }

    /**
     * Divide two integers, returning the floor of the quotient, and
     * the modulus remainder.
     * <p>
     * Unlike the built-in division, this is mathematically well-behaved.
     * E.g., <code>-1/4</code> => 0 and <code>-1%4</code> => -1,
     * but <code>floorDivide(-1,4)</code> => -1 with <code>remainder[0]</code> => 3.
     * @param numerator the numerator
     * @param denominator a divisor which must be > 0
     * @param remainder an array of at least one element in which the value
     * <code>numerator mod denominator</code> is returned. Unlike <code>numerator
     * % denominator</code>, this will always be non-negative.
     * @return the floor of the quotient.
     */
    private static final int
        floorDivide(long numerator, int denominator, int[] remainder) {

        if (numerator >= 0) {
            remainder[0] = (int)(numerator % denominator);
            return (int)(numerator / denominator);
        }
        int quotient = (int)(((numerator + 1) / denominator) - 1);
        remainder[0] = (int)(numerator - (quotient * denominator));
        return quotient;
    }
}

