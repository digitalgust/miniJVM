/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.cldc.util.mini;

import java.util.*;

/** 
 * This class provides the time zone implementations
 * for J2ME CLDC/MIDP.  By default, the only supported
 * time zone is UTC/GMT.  Vendor-specific implementations 
 * may provide additional time zones.
 *
 * @see java.util.TimeZone
 */
 
public class TimeZoneImpl extends TimeZone {

    static String HOME_ID = null;

    public TimeZoneImpl() {}

    /**
     * Constructs a TimeZone with the given base time zone offset from GMT
     * and time zone ID. Timezone IDs can be obtained from
     * TimeZone.getAvailableIDs. Normally you should use TimeZone.getDefault to
     * construct a TimeZone.
     *
     * @param rawOffset  The given base time zone offset to GMT.
     * @param ID         The time zone ID which is obtained from
     *                   TimeZone.getAvailableIDs.
     */
    private TimeZoneImpl(int rawOffset, String ID) {
        this.rawOffset = rawOffset;
        this.ID = ID;
        dstSavings = millisPerHour; // In case user sets rules later
    }

    /**
     * Constructor.  This constructor is identical to the 10-argument
     * constructor, but also takes a dstSavings parameter.
     * @param dstSavings   The amount of time in ms saved during DST.
     * @exception IllegalArgumentException the month, day, dayOfWeek, or time
     * parameters are out of range for the start or end rule
     */
    private TimeZoneImpl(int rawOffset, String ID,
                         int startMonth, int startDay, int startDayOfWeek,
                         int startTime, int endMonth, int endDay,
                         int endDayOfWeek, int endTime, int dstSavings) {
        this.ID             = ID;
        this.rawOffset      = rawOffset;
        this.startMonth     = startMonth;
        this.startDay       = startDay;
        this.startDayOfWeek = startDayOfWeek;
        this.startTime      = startTime;
        this.endMonth       = endMonth;
        this.endDay         = endDay;
        this.endDayOfWeek   = endDayOfWeek;
        this.endTime        = endTime;
        this.dstSavings     = dstSavings;
        decodeRules();
        if (dstSavings <= 0) {
           throw new IllegalArgumentException("Illegal DST savings");
        }
    }

    /* Constants used internally; unit is milliseconds */
    private static final int ONE_MINUTE = 60*1000;
    private static final int ONE_HOUR   = 60*ONE_MINUTE;
    private static final int ONE_DAY    = 24*ONE_HOUR;

    /**
     * Gets offset, for current date, modified in case of
     * daylight savings. This is the offset to add *to* GMT to get local time.
     * Gets the time zone offset, for current date, modified in case of daylight
     * savings. This is the offset to add *to* GMT to get local time. Assume
     * that the start and end month are distinct. This method may return incorrect
     * results for rules that start at the end of February (e.g., last Sunday in
     * February) or the beginning of March (e.g., March 1).
     *
     * @param era           The era of the given date (0 = BC, 1 = AD).
     * @param year          The year in the given date.
     * @param month         The month in the given date. Month is 0-based. e.g.,
     *                      0 for January.
     * @param day           The day-in-month of the given date.
     * @param dayOfWeek     The day-of-week of the given date.
     * @param millis        The milliseconds in day in <em>standard</em> local time.
     * @return              The offset to add *to* GMT to get local time.
     * @exception IllegalArgumentException the era, month, day,
     * dayOfWeek, or millis parameters are out of range
     */
    public int getOffset(int era, int year, int month, int day,
                         int dayOfWeek, int millis) {
        if (month < Calendar.JANUARY
            || month > Calendar.DECEMBER) {

            throw new IllegalArgumentException("Illegal month " + month);
        }
        return getOffset(era, year, month, day, dayOfWeek, millis,
                         staticMonthLength[month]);
    }

    /**
     * Gets offset, for current date, modified in case of
     * daylight savings. This is the offset to add <em>to</em> GMT to get local time.
     * Gets the time zone offset, for current date, modified in case of daylight
     * savings. This is the offset to add *to* GMT to get local time. Assume
     * that the start and end month are distinct.
     * @param era           The era of the given date (0 = BC, 1 = AD).
     * @param year          The year in the given date.
     * @param month         The month in the given date. Month is 0-based. e.g.,
     *                      0 for January.
     * @param day           The day-in-month of the given date.
     * @param dayOfWeek     The day-of-week of the given date.
     * @param millis        The milliseconds in day in <em>standard</em> local time.
     * @param monthLength   The length of the given month in days.
     * @return              The offset to add *to* GMT to get local time.
     * @exception IllegalArgumentException the era, month, day,
     * dayOfWeek, millis, or monthLength parameters are out of range
     */
    int getOffset(int era, int year, int month, int day, int dayOfWeek,
                  int millis, int monthLength) {
        if (true) {
            // Use this parameter checking code for normal operation.  Only one
            // of these two blocks should actually get compiled into the class
            // file.
            if ((era != 0 && era != 1)
                || month < Calendar.JANUARY
                || month > Calendar.DECEMBER
                || day < 1
                || day > monthLength
                || dayOfWeek < Calendar.SUNDAY
                || dayOfWeek > Calendar.SATURDAY
                || millis < 0
                || millis >= millisPerDay
                || monthLength < 28
                || monthLength > 31) {

                throw new IllegalArgumentException();
            }
        } else {
            // This parameter checking code is better for debugging, but
            // overkill for normal operation.  Only one of these two blocks
            // should actually get compiled into the class file.
            if (era != 0 && era != 1) {
                throw new IllegalArgumentException("Illegal era " + era);
            }
            if (month < Calendar.JANUARY || month > Calendar.DECEMBER) {
                throw new IllegalArgumentException("Illegal month " + month);
            }
            if (day < 1 || day > monthLength) {
                throw new IllegalArgumentException("Illegal day " + day);
            }
            if (dayOfWeek < Calendar.SUNDAY || dayOfWeek > Calendar.SATURDAY) {
                throw new IllegalArgumentException("Illegal day of week " + dayOfWeek);
            }
            if (millis < 0 || millis >= millisPerDay) {
                throw new IllegalArgumentException("Illegal millis " + millis);
            }
            if (monthLength < 28 || monthLength > 31) {
                throw new IllegalArgumentException("Illegal month length " + monthLength);
            }
        }

        int result = rawOffset;

        // Bail out if we are before the onset of daylight savings time
        if (!useDaylight || year < startYear || era != 1) return result;

        // Check for southern hemisphere.  We assume that the start and end
        // month are different.
        boolean southern = (startMonth > endMonth);

        // Compare the date to the starting and ending rules.+1 = date>rule, -1
        // = date<rule, 0 = date==rule.
        int startCompare = compareToRule(month, monthLength, day, dayOfWeek, millis,
                                         startMode, startMonth, startDayOfWeek,
                                         startDay, startTime);
        int endCompare = 0;

        // We don't always have to compute endCompare.  For many instances,
        // startCompare is enough to determine if we are in DST or not.  In the
        // northern hemisphere, if we are before the start rule, we can't have
        // DST.  In the southern hemisphere, if we are after the start rule, we
        // must have DST.  This is reflected in the way the next if statement
        // (not the one immediately following) short circuits.
        if (southern != (startCompare >= 0)) {
            // For the ending rule comparison, we add the dstSavings to the millis
            // passed in to convert them from standard to wall time.  We then must
            // normalize the millis to the range 0..millisPerDay-1.
            millis += dstSavings; // Assume dstSavings > 0
            while (millis >= millisPerDay) {
                millis -= millisPerDay;
                ++day;
                dayOfWeek = 1 + (dayOfWeek % 7); // Assume dayOfWeek is one-based
                if (day > monthLength) {
                    day = 1;
                    // When incrementing the month, it is desirable to overflow
                    // from DECEMBER to DECEMBER+1, since we use the result to
                    // compare against a real month. Wraparound of the value
                    // leads to bug 4173604.
                    ++month;
                }
            }
            endCompare = compareToRule(month, monthLength, day, dayOfWeek, millis,
                                       endMode, endMonth, endDayOfWeek,
                                       endDay, endTime);
        }

        // Check for both the northern and southern hemisphere cases.  We
        // assume that in the northern hemisphere, the start rule is before the
        // end rule within the calendar year, and vice versa for the southern
        // hemisphere.
        if ((!southern && (startCompare >= 0 && endCompare < 0)) ||
             (southern && (startCompare >= 0 || endCompare < 0))) {

            result += dstSavings;
        }

        return result;
    }

    /**
     * Compare a given date in the year to a rule. Return 1, 0, or -1, depending
     * on whether the date is after, equal to, or before the rule date. The
     * millis are compared directly against the ruleMillis, so any
     * standard-daylight adjustments must be handled by the caller.
     *
     * @return  1 if the date is after the rule date, -1 if the date is before
     *          the rule date, or 0 if the date is equal to the rule date.
     */
    private static int compareToRule(int month, int monthLen, int dayOfMonth,
                                     int dayOfWeek, int millis,
                                     int ruleMode, int ruleMonth, int ruleDayOfWeek,
                                     int ruleDay, int ruleMillis) {
        if (month < ruleMonth) return -1;
        else if (month > ruleMonth) return 1;

        int ruleDayOfMonth = 0;
        switch (ruleMode) {
        case DOM_MODE:
            ruleDayOfMonth = ruleDay;
            break;
        case DOW_IN_MONTH_MODE:
            // In this case ruleDay is the day-of-week-in-month
            if (ruleDay > 0) {
                ruleDayOfMonth = 1 + (ruleDay - 1) * 7 +
                (7 + ruleDayOfWeek - (dayOfWeek - dayOfMonth + 1)) % 7;
            } else {
                // Assume ruleDay < 0 here
                ruleDayOfMonth = monthLen + (ruleDay + 1) * 7 -
                    (7 + (dayOfWeek + monthLen - dayOfMonth) - ruleDayOfWeek) % 7;
            }
            break;
        case DOW_GE_DOM_MODE:
            ruleDayOfMonth = ruleDay +
                (49 + ruleDayOfWeek - ruleDay - dayOfWeek + dayOfMonth) % 7;
            break;
        case DOW_LE_DOM_MODE:
            ruleDayOfMonth = ruleDay -
                (49 - ruleDayOfWeek + ruleDay + dayOfWeek - dayOfMonth) % 7;
            // Note at this point ruleDayOfMonth may be <1, although it will
            // be >=1 for well-formed rules.
            break;
        }

        if (dayOfMonth < ruleDayOfMonth) return -1;
        else if (dayOfMonth > ruleDayOfMonth) return 1;

        if (millis < ruleMillis) return -1;
        else if (millis > ruleMillis) return 1;
        else return 0;
    }

    /**
     * Gets the GMT offset for this time zone.
     */
    public int getRawOffset() {
        // The given date will be taken into account while
        // we have the historical time zone data in place.
        return rawOffset;
    }

    /**
     * Queries if this time zone uses Daylight Savings Time.
     */
    public boolean useDaylightTime() {
        return useDaylight;
    }

    /**
     * Gets the ID of this time zone.
     * @return the ID of this time zone.
     */
    public String getID() {
        return ID;
    }

    /**
     * Gets the <code>TimeZone</code> for the given ID.
     * @param ID the ID for a <code>TimeZone</code>, either an abbreviation such as
     * "GMT", or a full name such as "America/Los_Angeles".
     * <p> The only time zone ID that is required to be supported is "GMT",
     * though typically, the timezones for the regions where the device is
     * sold should be supported.
     * @return the specified <code>TimeZone</code>, or null if the given ID
     * cannot be understood.
     */
    public synchronized TimeZone getInstance(String ID) {
        if (ID == null) {
            if (HOME_ID == null) {
                HOME_ID = System.getProperty("com.sun.cldc.util.mini.TimeZoneImpl.timezone");
                if (HOME_ID == null)
                    HOME_ID = "UTC";
            }
            ID = HOME_ID;
        }
        for (int i = 0; i < zones.length; i++) {
            if (zones[i].getID().equals(ID))
                return zones[i];
            }
        return null;
    }

    /** Gets all the available IDs supported.
     * @return  an array of IDs.
     */
    public synchronized String[] getIDs() {
        if (ids == null) {
            ids = new String[zones.length];
            for (int i = 0; i < zones.length; i++)
                ids[i] = zones[i].getID();
        }
        return ids;
    }

    // =======================privates===============================

    /**
     * The string identifier of this <code>TimeZone</code>.  This is a
     * programmatic identifier used internally to look up <code>TimeZone</code>
     * objects from the system table and also to map them to their localized
     * display names.  <code>ID</code> values are unique in the system
     * table but may not be for dynamically created zones.
     * @serial
     */
    private String ID;

    static String[] ids = null;

    /**
     * The month in which daylight savings time starts.  This value must be
     * between <code>Calendar.JANUARY</code> and
     * <code>Calendar.DECEMBER</code> inclusive.  This value must not equal
     * <code>endMonth</code>.
     * <p>If <code>useDaylight</code> is false, this value is ignored.
     * @serial
     */
    private int startMonth;

    /**
     * This field has two possible interpretations:
     * <dl>
     * <dt><code>startMode == DOW_IN_MONTH</code></dt>
     * <dd>
     * <code>startDay</code> indicates the day of the month of
     * <code>startMonth</code> on which daylight
     * savings time starts, from 1 to 28, 30, or 31, depending on the
     * <code>startMonth</code>.
     * </dd>
     * <dt><code>startMode != DOW_IN_MONTH</code></dt>
     * <dd>
     * <code>startDay</code> indicates which <code>startDayOfWeek</code> in th
     * month <code>startMonth</code> daylight
     * savings time starts on.  For example, a value of +1 and a
     * <code>startDayOfWeek</code> of <code>Calendar.SUNDAY</code> indicates the
     * first Sunday of <code>startMonth</code>.  Likewise, +2 would indicate the
     * second Sunday, and -1 the last Sunday.  A value of 0 is illegal.
     * </dd>
     * </ul>
     * <p>If <code>useDaylight</code> is false, this value is ignored.
     * @serial
     */
    private int startDay;

    /**
     * The day of the week on which daylight savings time starts.  This value
     * must be between <code>Calendar.SUNDAY</code> and
     * <code>Calendar.SATURDAY</code> inclusive.
     * <p>If <code>useDaylight</code> is false or
     * <code>startMode == DAY_OF_MONTH</code>, this value is ignored.
     * @serial
     */
    private int startDayOfWeek;

    /**
     * The time in milliseconds after midnight at which daylight savings
     * time starts.  This value is expressed as <em>wall time</em>, which means
     * it is compared to <em>standard</em> time for the daylight savings start.
     * <p>If <code>useDaylight</code> is false, this value is ignored.
     * @serial
     */
    private int startTime;

    /**
     * The month in which daylight savings time ends.  This value must be
     * between <code>Calendar.JANUARY</code> and
     * <code>Calendar.UNDECIMBER</code>.  This value must not equal
     * <code>startMonth</code>.
     * <p>If <code>useDaylight</code> is false, this value is ignored.
     * @serial
     */
    private int endMonth;

    /**
     * This field has two possible interpretations:
     * <dl>
     * <dt><code>endMode == DOW_IN_MONTH</code></dt>
     * <dd>
     * <code>endDay</code> indicates the day of the month of
     * <code>endMonth</code> on which daylight
     * savings time ends, from 1 to 28, 30, or 31, depending on the
     * <code>endMonth</code>.
     * </dd>
     * <dt><code>endMode != DOW_IN_MONTH</code></dt>
     * <dd>
     * <code>endDay</code> indicates which <code>endDayOfWeek</code> in th
     * month <code>endMonth</code> daylight
     * savings time ends on.  For example, a value of +1 and a
     * <code>endDayOfWeek</code> of <code>Calendar.SUNDAY</code> indicates the
     * first Sunday of <code>endMonth</code>.  Likewise, +2 would indicate the
     * second Sunday, and -1 the last Sunday.  A value of 0 is illegal.
     * </dd>
     * </ul>
     * <p>If <code>useDaylight</code> is false, this value is ignored.
     * @serial
     */
    private int endDay;

    /**
     * The day of the week on which daylight savings time ends.  This value
     * must be between <code>Calendar.SUNDAY</code> and
     * <code>Calendar.SATURDAY</code> inclusive.
     * <p>If <code>useDaylight</code> is false or
     * <code>endMode == DAY_OF_MONTH</code>, this value is ignored.
     * @serial
     */
    private int endDayOfWeek;

    /**
     * The time in milliseconds after midnight at which daylight savings
     * time ends.  This value is expressed as <em>wall time</em>, which means
     * it is compared to <em>daylight</em> time for the daylight savings end.
     * <p>If <code>useDaylight</code> is false, this value is ignored.
     * @serial
     */
    private int endTime;

    /**
     * The year in which daylight savings time is first observed.  This is an AD
     * value.  If this value is less than 1 then daylight savings is observed
     * for all AD years.
     * <p>If <code>useDaylight</code> is false, this value is ignored.
     * @serial
     */
    private int startYear;

    /**
     * The offset in milliseconds between this zone and GMT.  Negative offsets
     * are to the west of Greenwich.  To obtain local <em>standard</em> time,
     * add the offset to GMT time.  To obtain local wall time it may also be
     * necessary to add <code>dstSavings</code>.
     * @serial
     */
    private int rawOffset;

    /**
     * A boolean value which is true if and only if this zone uses daylight
     * savings time.  If this value is false, several other fields are ignored.
     * @serial
     */
    private boolean useDaylight=false; // indicate if this time zone uses DST

    private static final int millisPerHour = 60*60*1000;
    private static final int millisPerDay  = 24*millisPerHour;

    /**
     * This field was serialized in JDK 1.1, so we have to keep it that way
     * to maintain serialization compatibility. However, there's no need to
     * recreate the array each time we create a new time zone.
     * @serial An array of bytes containing the values {31, 28, 31, 30, 31, 30,
     * 31, 31, 30, 31, 30, 31}.  This is ignored as of JDK 1.2, however, it must
     * be streamed out for compatibility with JDK 1.1.
     */
    private final byte monthLength[] = staticMonthLength;
    private final static byte staticMonthLength[] = {31,29,31,30,31,30,31,31,30,31,30,31}; //**NS**

    /**
     * Variables specifying the mode of the start rule.  Takes the following
     * values:
     * <dl>
     * <dt><code>DOM_MODE</code></dt>
     * <dd>
     * Exact day of week; e.g., March 1.
     * </dd>
     * <dt><code>DOW_IN_MONTH_MODE</code></dt>
     * <dd>
     * Day of week in month; e.g., last Sunday in March.
     * </dd>
     * <dt><code>DOW_GE_DOM_MODE</code></dt>
     * <dd>
     * Day of week after day of month; e.g., Sunday on or after March 15.
     * </dd>
     * <dt><code>DOW_LE_DOM_MODE</code></dt>
     * <dd>
     * Day of week before day of month; e.g., Sunday on or before March 15.
     * </dd>
     * </dl>
     * The setting of this field affects the interpretation of the
     * <code>startDay</code> field.
     * <p>If <code>useDaylight</code> is false, this value is ignored.
     * @serial
     * @since JDK1.1.4
     */
    private int startMode;

    /**
     * Variables specifying the mode of the end rule.  Takes the following
     * values:
     * <dl>
     * <dt><code>DOM_MODE</code></dt>
     * <dd>
     * Exact day of week; e.g., March 1.
     * </dd>
     * <dt><code>DOW_IN_MONTH_MODE</code></dt>
     * <dd>
     * Day of week in month; e.g., last Sunday in March.
     * </dd>
     * <dt><code>DOW_GE_DOM_MODE</code></dt>
     * <dd>
     * Day of week after day of month; e.g., Sunday on or after March 15.
     * </dd>
     * <dt><code>DOW_LE_DOM_MODE</code></dt>
     * <dd>
     * Day of week before day of month; e.g., Sunday on or before March 15.
     * </dd>
     * </dl>
     * The setting of this field affects the interpretation of the
     * <code>endDay</code> field.
     * <p>If <code>useDaylight</code> is false, this value is ignored.
     * @serial
     * @since JDK1.1.4
     */
    private int endMode;

    /**
     * A positive value indicating the amount of time saved during DST in
     * milliseconds.
     * Typically one hour (3600000); sometimes 30 minutes (1800000).
     * <p>If <code>useDaylight</code> is false, this value is ignored.
     * @serial
     * @since JDK1.1.4
     */
    private int dstSavings;

    /**
     * Constants specifying values of startMode and endMode.
     */
    private static final int DOM_MODE          = 1; // Exact day of month, "Mar 1"
    private static final int DOW_IN_MONTH_MODE = 2; // Day of week in month, "lastSun"
    private static final int DOW_GE_DOM_MODE   = 3; // Day of week after day of month, "Sun>=15"
    private static final int DOW_LE_DOM_MODE   = 4; // Day of week before day of month, "Sun<=21"

    //----------------------------------------------------------------------
    // Rule representation
    //
    // We represent the following flavors of rules:
    //       5        the fifth of the month
    //       lastSun  the last Sunday in the month
    //       lastMon  the last Monday in the month
    //       Sun>=8   first Sunday on or after the eighth
    //       Sun<=25  last Sunday on or before the 25th
    // This is further complicated by the fact that we need to remain
    // backward compatible with the 1.1 FCS.  Finally, we need to minimize
    // API changes.  In order to satisfy these requirements, we support
    // three representation systems, and we translate between them.
    //
    // INTERNAL REPRESENTATION
    // This is the format TimeZone objects take after construction or
    // streaming in is complete.  Rules are represented directly, using an
    // unencoded format.  We will discuss the start rule only below; the end
    // rule is analogous.
    //   startMode      Takes on enumerated values DAY_OF_MONTH,
    //                  DOW_IN_MONTH, DOW_AFTER_DOM, or DOW_BEFORE_DOM.
    //   startDay       The day of the month, or for DOW_IN_MONTH mode, a
    //                  value indicating which DOW, such as +1 for first,
    //                  +2 for second, -1 for last, etc.
    //   startDayOfWeek The day of the week.  Ignored for DAY_OF_MONTH.
    //
    // ENCODED REPRESENTATION
    // This is the format accepted by the constructor and by setStartRule()
    // and setEndRule().  It uses various combinations of positive, negative,
    // and zero values to encode the different rules.  This representation
    // allows us to specify all the different rule flavors without altering
    // the API.
    //   MODE              startMonth    startDay    startDayOfWeek
    //   DOW_IN_MONTH_MODE >=0           !=0         >0
    //   DOM_MODE          >=0           >0          ==0
    //   DOW_GE_DOM_MODE   >=0           >0          <0
    //   DOW_LE_DOM_MODE   >=0           <0          <0
    //   (no DST)          don't care    ==0         don't care
    //
    // STREAMED REPRESENTATION
    // We must retain binary compatibility with the 1.1 FCS.  The 1.1 code only
    // handles DOW_IN_MONTH_MODE and non-DST mode, the latter indicated by the
    // flag useDaylight.  When we stream an object out, we translate into an
    // approximate DOW_IN_MONTH_MODE representation so the object can be parsed
    // and used by 1.1 code.  Following that, we write out the full
    // representation separately so that contemporary code can recognize and
    // parse it.  The full representation is written in a "packed" format,
    // consisting of a version number, a length, and an array of bytes.  Future
    // versions of this class may specify different versions.  If they wish to
    // include additional data, they should do so by storing them after the
    // packed representation below.
    //----------------------------------------------------------------------

    /**
     * Given a set of encoded rules in startDay and startDayOfMonth, decode
     * them and set the startMode appropriately.  Do the same for endDay and
     * endDayOfMonth.  Upon entry, the day of week variables may be zero or
     * negative, in order to indicate special modes.  The day of month
     * variables may also be negative.  Upon exit, the mode variables will be
     * set, and the day of week and day of month variables will be positive.
     * This method also recognizes a startDay or endDay of zero as indicating
     * no DST.
     */
    private void decodeRules() {
        decodeStartRule();
        decodeEndRule();
    }

    /**
     * Decode the start rule and validate the parameters.  The parameters are
     * expected to be in encoded form, which represents the various rule modes
     * by negating or zeroing certain values.  Representation formats are:
     * <p>
     * <pre>
     *            DOW_IN_MONTH  DOM    DOW>=DOM  DOW<=DOM  no DST
     *            ------------  -----  --------  --------  ----------
     * month       0..11        same    same      same     don't care
     * day        -5..5         1..31   1..31    -1..-31   0
     * dayOfWeek   1..7         0      -1..-7    -1..-7    don't care
     * time        0..ONEDAY    same    same      same     don't care
     * </pre>
     * The range for month does not include UNDECIMBER since this class is
     * really specific to Calendar, which does not use that month.
     * The range for time includes ONEDAY (vs. ending at ONEDAY-1) because the
     * end rule is an exclusive limit point.  That is, the range of times that
     * are in DST include those >= the start and < the end.  For this reason,
     * it should be possible to specify an end of ONEDAY in order to include the
     * entire day.  Although this is equivalent to time 0 of the following day,
     * it's not always possible to specify that, for example, on December 31.
     * While arguably the start range should still be 0..ONEDAY-1, we keep
     * the start and end ranges the same for consistency.
     */
    private void decodeStartRule() {
        useDaylight = (startDay != 0) && (endDay != 0);
        if (startDay != 0) {
            if (startMonth < Calendar.JANUARY || startMonth > Calendar.DECEMBER) {
                throw new IllegalArgumentException(
                              "Illegal start month " + startMonth);
            }
            if (startTime < 0 || startTime > millisPerDay) {
                throw new IllegalArgumentException(
                              "Illegal start time " + startTime);
            }
            if (startDayOfWeek == 0) {
                startMode = DOM_MODE;
            } else {
                if (startDayOfWeek > 0) {
                    startMode = DOW_IN_MONTH_MODE;
                } else {
                    startDayOfWeek = -startDayOfWeek;
                    if (startDay > 0) {
                        startMode = DOW_GE_DOM_MODE;
                    } else {
                        startDay = -startDay;
                        startMode = DOW_LE_DOM_MODE;
                    }
                }
                if (startDayOfWeek > Calendar.SATURDAY) {
                    throw new IllegalArgumentException(
                                  "Illegal start day of week " + startDayOfWeek);
                }
            }
            if (startMode == DOW_IN_MONTH_MODE) {
                if (startDay < -5 || startDay > 5) {
                    throw new IllegalArgumentException(
                                  "Illegal start day of week in month " + startDay);
                }
            } else if (startDay > staticMonthLength[startMonth]) {
                throw new IllegalArgumentException(
                              "Illegal start day " + startDay);
            }
        }
    }

    /**
     * Decode the end rule and validate the parameters.  This method is exactly
     * analogous to decodeStartRule().
     * @see decodeStartRule
     */
    private void decodeEndRule() {
        useDaylight = (startDay != 0) && (endDay != 0);
        if (endDay != 0) {
            if (endMonth < Calendar.JANUARY || endMonth > Calendar.DECEMBER) {
                throw new IllegalArgumentException(
                              "Illegal end month " + endMonth);
            }
            if (endTime < 0 || endTime > millisPerDay) {
                throw new IllegalArgumentException(
                              "Illegal end time " + endTime);
            }
            if (endDayOfWeek == 0) {
                endMode = DOM_MODE;
            } else {
                if (endDayOfWeek > 0) {
                    endMode = DOW_IN_MONTH_MODE;
                } else {
                    endDayOfWeek = -endDayOfWeek;
                    if (endDay > 0) {
                        endMode = DOW_GE_DOM_MODE;
                    } else {
                        endDay = -endDay;
                        endMode = DOW_LE_DOM_MODE;
                    }
                }
                if (endDayOfWeek > Calendar.SATURDAY) {
                    throw new IllegalArgumentException(
                                  "Illegal end day of week " + endDayOfWeek);
                }
            }
            if (endMode == DOW_IN_MONTH_MODE) {
                if (endDay < -5 || endDay > 5) {
                    throw new IllegalArgumentException(
                                  "Illegal end day of week in month " + endDay);
                }
            } else if (endDay > staticMonthLength[endMonth]) {
                throw new IllegalArgumentException(
                              "Illegal end day " + endDay);
            }
        }
    }

    static TimeZone zones[] = {

    //----------------------------------------------------------
    new TimeZoneImpl(0*ONE_HOUR, "GMT"),
    // GMT  -(-)    0:00    -   GMT
    new TimeZoneImpl(0*ONE_HOUR, "UTC"),
    /**
     * NOTE: as in this example, most implementations will only include
     * a handful of timezones
     * look for the closing comment which has a string of '*' asterisks
     *************////////////////////////////////////////////////////////////
    // America
    //----------------------------------------------------------
    new TimeZoneImpl(-10*ONE_HOUR, "America/Adak",
                 Calendar.APRIL, 1, -Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule US  1967    max -   Oct lastSun 2:00    0   S
    // Rule US  1987    max -   Apr Sun>=1  2:00    1:00    D
    // America/Adak Alaska(US)  -10:00  US  HA%sT
    //----------------------------------------------------------
    new TimeZoneImpl(-9*ONE_HOUR, "America/Anchorage",
                 Calendar.APRIL, 1, -Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule US  1967    max -   Oct lastSun 2:00    0   S
    // Rule US  1987    max -   Apr Sun>=1  2:00    1:00    D
    // America/Anchorage    Alaska(US)  -9:00   US  AK%sT
    new TimeZoneImpl(-9*ONE_HOUR, "AST",
                 Calendar.APRIL, 1, -Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    //----------------------------------------------------------
    new TimeZoneImpl(-8*ONE_HOUR, "America/Vancouver",
                 Calendar.APRIL, 1, -Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule Vanc    1962    max -   Oct lastSun 2:00    0   S
    // Rule Vanc    1987    max -   Apr Sun>=1  2:00    1:00    D
    // America/Vancouver    British Columbia(CA)    -8:00   Vanc    P%sT
    //----------------------------------------------------------
    new TimeZoneImpl(-8*ONE_HOUR, "America/Tijuana",
                 Calendar.APRIL, 1, -Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule Mexico  1996    max -   Apr Sun>=1  2:00    1:00    D
    // Rule Mexico  1996    max -   Oct lastSun 2:00    0   S
    // America/Tijuana  Mexico(MX)  -8:00   Mexico  P%sT
    //----------------------------------------------------------
    new TimeZoneImpl(-8*ONE_HOUR, "America/Los_Angeles",
                 Calendar.APRIL, 1, -Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule US  1967    max -   Oct lastSun 2:00    0   S
    // Rule US  1987    max -   Apr Sun>=1  2:00    1:00    D
    // America/Los_Angeles  US Pacific time, represented by Los Angeles(US) -8:00   US  P%sT
    new TimeZoneImpl(-8*ONE_HOUR, "PST",
                 Calendar.APRIL, 1, -Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    //----------------------------------------------------------
    new TimeZoneImpl(-7*ONE_HOUR, "America/Dawson_Creek"),
    // America/Dawson_Creek British Columbia(CA)    -7:00   -   MST
    //----------------------------------------------------------
    new TimeZoneImpl(-7*ONE_HOUR, "America/Phoenix"),
    // America/Phoenix  ?(US)   -7:00   -   MST
    new TimeZoneImpl(-7*ONE_HOUR, "PNT"),
    //----------------------------------------------------------
    new TimeZoneImpl(-7*ONE_HOUR, "America/Edmonton",
                 Calendar.APRIL, 1, -Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule Edm 1972    max -   Oct lastSun 2:00    0   S
    // Rule Edm 1987    max -   Apr Sun>=1  2:00    1:00    D
    // America/Edmonton Alberta(CA) -7:00   Edm M%sT
    //----------------------------------------------------------
    new TimeZoneImpl(-7*ONE_HOUR, "America/Mazatlan",
                 Calendar.APRIL, 1, -Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule Mexico  1996    max -   Apr Sun>=1  2:00    1:00    D
    // Rule Mexico  1996    max -   Oct lastSun 2:00    0   S
    // America/Mazatlan Mexico(MX)  -7:00   Mexico  M%sT
    //----------------------------------------------------------
    new TimeZoneImpl(-7*ONE_HOUR, "America/Denver",
                 Calendar.APRIL, 1, -Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule US  1967    max -   Oct lastSun 2:00    0   S
    // Rule US  1987    max -   Apr Sun>=1  2:00    1:00    D
    // America/Denver   US Mountain time, represented by Denver(US) -7:00   US  M%sT
    new TimeZoneImpl(-7*ONE_HOUR, "MST",
                 Calendar.APRIL, 1, -Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    //----------------------------------------------------------
    new TimeZoneImpl(-6*ONE_HOUR, "America/Belize"),
    // America/Belize   Belize(BZ)  -6:00   -   C%sT
    //----------------------------------------------------------
    new TimeZoneImpl(-6*ONE_HOUR, "America/Regina"),
    // America/Regina   Saskatchewan(CA)    -6:00   -   CST
    //----------------------------------------------------------
    new TimeZoneImpl(-6*ONE_HOUR, "America/Guatemala"),
    // America/Guatemala    Guatemala(GT)   -6:00   -   C%sT
    //----------------------------------------------------------
    new TimeZoneImpl(-6*ONE_HOUR, "America/Tegucigalpa"),
    // America/Tegucigalpa  Honduras(HN)    -6:00   -   C%sT
    //----------------------------------------------------------
    new TimeZoneImpl(-6*ONE_HOUR, "America/El_Salvador"),
    // America/El_Salvador  El Salvador(SV) -6:00   -   C%sT
    //----------------------------------------------------------
    new TimeZoneImpl(-6*ONE_HOUR, "America/Costa_Rica"),
    // America/Costa_Rica   Costa Rica(CR)  -6:00   -   C%sT
    //----------------------------------------------------------
    new TimeZoneImpl(-6*ONE_HOUR, "America/Winnipeg",
                 Calendar.APRIL, 1, -Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule Winn    1966    max -   Oct lastSun 2:00    0   S
    // Rule Winn    1987    max -   Apr Sun>=1  2:00    1:00    D
    // America/Winnipeg Manitoba(CA)    -6:00   Winn    C%sT
    //----------------------------------------------------------
    new TimeZoneImpl(-6*ONE_HOUR, "America/Mexico_City",
                 Calendar.APRIL, 1, -Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule Mexico  1996    max -   Apr Sun>=1  2:00    1:00    D
    // Rule Mexico  1996    max -   Oct lastSun 2:00    0   S
    // America/Mexico_City  Mexico(MX)  -6:00   Mexico  C%sT
    //----------------------------------------------------------
    new TimeZoneImpl(-6*ONE_HOUR, "America/Chicago",
                 Calendar.APRIL, 1, -Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule US  1967    max -   Oct lastSun 2:00    0   S
    // Rule US  1987    max -   Apr Sun>=1  2:00    1:00    D
    // America/Chicago  US Central time, represented by Chicago(US) -6:00   US  C%sT
    new TimeZoneImpl(-6*ONE_HOUR, "CST",
                 Calendar.APRIL, 1, -Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    //----------------------------------------------------------
    new TimeZoneImpl(-5*ONE_HOUR, "America/Porto_Acre"),
    // America/Porto_Acre   Brazil(BR)  -5:00   -   AST
    //----------------------------------------------------------
    new TimeZoneImpl(-5*ONE_HOUR, "America/Bogota"),
    // America/Bogota   Colombia(CO)    -5:00   -   CO%sT   # Colombia Time
    //----------------------------------------------------------
    new TimeZoneImpl(-5*ONE_HOUR, "America/Guayaquil"),
    // America/Guayaquil    Ecuador(EC) -5:00   -   ECT # Ecuador Time
    //----------------------------------------------------------
    new TimeZoneImpl(-5*ONE_HOUR, "America/Jamaica"),
    // America/Jamaica  Jamaica(JM) -5:00   -   EST
    //----------------------------------------------------------
    new TimeZoneImpl(-5*ONE_HOUR, "America/Cayman"),
    // America/Cayman   Cayman Is(KY)   -5:00   -   EST
    //----------------------------------------------------------
    new TimeZoneImpl(-5*ONE_HOUR, "America/Managua"),
    // America/Managua  Nicaragua(NI)   -5:00   -   EST
    //----------------------------------------------------------
    new TimeZoneImpl(-5*ONE_HOUR, "America/Panama"),
    // America/Panama   Panama(PA)  -5:00   -   EST
    //----------------------------------------------------------
    new TimeZoneImpl(-5*ONE_HOUR, "America/Lima"),
    // America/Lima Peru(PE)    -5:00   -   PE%sT   # Peru Time
    //----------------------------------------------------------
    new TimeZoneImpl(-5*ONE_HOUR, "America/Indianapolis"),
    // America/Indianapolis Indiana(US) -5:00   -   EST
    new TimeZoneImpl(-5*ONE_HOUR, "IET"),
    //----------------------------------------------------------
    new TimeZoneImpl(-5*ONE_HOUR, "America/Nassau",
                 Calendar.APRIL, 1, -Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule Bahamas 1964    max -   Oct lastSun 2:00    0   S
    // Rule Bahamas 1987    max -   Apr Sun>=1  2:00    1:00    D
    // America/Nassau   Bahamas(BS) -5:00   Bahamas E%sT
    //----------------------------------------------------------
    new TimeZoneImpl(-5*ONE_HOUR, "America/Montreal",
                 Calendar.APRIL, 1, -Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule Mont    1957    max -   Oct lastSun 2:00    0   S
    // Rule Mont    1987    max -   Apr Sun>=1  2:00    1:00    D
    // America/Montreal Ontario, Quebec(CA) -5:00   Mont    E%sT
    //----------------------------------------------------------
    new TimeZoneImpl(-5*ONE_HOUR, "America/Havana",
                 Calendar.APRIL, 1, -Calendar.SUNDAY, 0*ONE_HOUR,
                 Calendar.OCTOBER, 8, -Calendar.SUNDAY, 1*ONE_HOUR, 1*ONE_HOUR),
    // Rule Cuba    1990    max -   Apr Sun>=1  0:00    1:00    D
    // Rule Cuba    1997    max -   Oct Sun>=8  0:00s   0   S
    // America/Havana   Cuba(CU)    -5:00   Cuba    C%sT
    //----------------------------------------------------------
    new TimeZoneImpl(-5*ONE_HOUR, "America/Port-au-Prince",
                 Calendar.APRIL, 1, -Calendar.SUNDAY, 1*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule Haiti   1988    max -   Apr Sun>=1  1:00s   1:00    D
    // Rule Haiti   1988    max -   Oct lastSun 1:00s   0   S
    // America/Port-au-Prince   Haiti(HT)   -5:00   Haiti   E%sT
    //----------------------------------------------------------
    new TimeZoneImpl(-5*ONE_HOUR, "America/Grand_Turk",
                 Calendar.APRIL, 1, -Calendar.SUNDAY, 0*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 0*ONE_HOUR, 1*ONE_HOUR),
    // Rule TC  1979    max -   Oct lastSun 0:00    0   S
    // Rule TC  1987    max -   Apr Sun>=1  0:00    1:00    D
    // America/Grand_Turk   Turks and Caicos(TC)    -5:00   TC  E%sT
    //----------------------------------------------------------
    new TimeZoneImpl(-5*ONE_HOUR, "America/New_York",
                 Calendar.APRIL, 1, -Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule US  1967    max -   Oct lastSun 2:00    0   S
    // Rule US  1987    max -   Apr Sun>=1  2:00    1:00    D
    // America/New_York US Eastern time, represented by New York(US)    -5:00   US  E%sT
    new TimeZoneImpl(-5*ONE_HOUR, "EST",
                 Calendar.APRIL, 1, -Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    //----------------------------------------------------------
    new TimeZoneImpl(-4*ONE_HOUR, "America/Antigua"),
    // America/Antigua  Antigua and Barbuda(AG) -4:00   -   AST
    //----------------------------------------------------------
    new TimeZoneImpl(-4*ONE_HOUR, "America/Anguilla"),
    // America/Anguilla Anguilla(AI)    -4:00   -   AST
    //----------------------------------------------------------
    new TimeZoneImpl(-4*ONE_HOUR, "America/Curacao"),
    // America/Curacao  Curacao(AN) -4:00   -   AST
    //----------------------------------------------------------
    new TimeZoneImpl(-4*ONE_HOUR, "America/Aruba"),
    // America/Aruba    Aruba(AW)   -4:00   -   AST
    //----------------------------------------------------------
    new TimeZoneImpl(-4*ONE_HOUR, "America/Barbados"),
    // America/Barbados Barbados(BB)    -4:00   -   A%sT
    //----------------------------------------------------------
    new TimeZoneImpl(-4*ONE_HOUR, "America/La_Paz"),
    // America/La_Paz   Bolivia(BO) -4:00   -   BOT # Bolivia Time
    //----------------------------------------------------------
    new TimeZoneImpl(-4*ONE_HOUR, "America/Manaus"),
    // America/Manaus   Brazil(BR)  -4:00   -   WST
    //----------------------------------------------------------
    new TimeZoneImpl(-4*ONE_HOUR, "America/Dominica"),
    // America/Dominica Dominica(DM)    -4:00   -   AST
    //----------------------------------------------------------
    new TimeZoneImpl(-4*ONE_HOUR, "America/Santo_Domingo"),
    // America/Santo_Domingo    Dominican Republic(DO)  -4:00   -   AST
    //----------------------------------------------------------
    new TimeZoneImpl(-4*ONE_HOUR, "America/Grenada"),
    // America/Grenada  Grenada(GD) -4:00   -   AST
    //----------------------------------------------------------
    new TimeZoneImpl(-4*ONE_HOUR, "America/Guadeloupe"),
    // America/Guadeloupe   Guadeloupe(GP)  -4:00   -   AST
    //----------------------------------------------------------
    new TimeZoneImpl(-4*ONE_HOUR, "America/Guyana"),
    // America/Guyana   Guyana(GY)  -4:00   -   GYT
    //----------------------------------------------------------
    new TimeZoneImpl(-4*ONE_HOUR, "America/St_Kitts"),
    // America/St_Kitts St Kitts-Nevis(KN)  -4:00   -   AST
    //----------------------------------------------------------
    new TimeZoneImpl(-4*ONE_HOUR, "America/St_Lucia"),
    // America/St_Lucia St Lucia(LC)    -4:00   -   AST
    //----------------------------------------------------------
    new TimeZoneImpl(-4*ONE_HOUR, "America/Martinique"),
    // America/Martinique   Martinique(MQ)  -4:00   -   AST
    //----------------------------------------------------------
    new TimeZoneImpl(-4*ONE_HOUR, "America/Montserrat"),
    // America/Montserrat   Montserrat(MS)  -4:00   -   AST
    //----------------------------------------------------------
    new TimeZoneImpl(-4*ONE_HOUR, "America/Puerto_Rico"),
    // America/Puerto_Rico  Puerto Rico(PR) -4:00   -   AST
    new TimeZoneImpl(-4*ONE_HOUR, "PRT"),
    //----------------------------------------------------------
    new TimeZoneImpl(-4*ONE_HOUR, "America/Port_of_Spain"),
    // America/Port_of_Spain    Trinidad and Tobago(TT) -4:00   -   AST
    //----------------------------------------------------------
    new TimeZoneImpl(-4*ONE_HOUR, "America/St_Vincent"),
    // America/St_Vincent   St Vincent and the Grenadines(VC)   -4:00   -   AST
    //----------------------------------------------------------
    new TimeZoneImpl(-4*ONE_HOUR, "America/Tortola"),
    // America/Tortola  British Virgin Is(VG)   -4:00   -   AST
    //----------------------------------------------------------
    new TimeZoneImpl(-4*ONE_HOUR, "America/St_Thomas"),
    // America/St_Thomas    Virgin Is(VI)   -4:00   -   AST
    //----------------------------------------------------------
    new TimeZoneImpl(-4*ONE_HOUR, "America/Caracas"),
    // America/Caracas  Venezuela(VE)   -4:00   -   VET
    //----------------------------------------------------------
    new TimeZoneImpl(-4*ONE_HOUR, "America/Cuiaba",
                 Calendar.OCTOBER, 1, -Calendar.SUNDAY, 0*ONE_HOUR,
                 Calendar.FEBRUARY, 11, -Calendar.SUNDAY, 0*ONE_HOUR, 1*ONE_HOUR),
    // Rule Brazil  1998    max -   Oct Sun>=1  0:00    1:00    D
    // Rule Brazil  1999    max -   Feb Sun>=11 0:00    0   S
    // America/Cuiaba   Brazil(BR)  -4:00   Brazil  W%sT
    //----------------------------------------------------------
    new TimeZoneImpl(-4*ONE_HOUR, "America/Halifax",
                 Calendar.APRIL, 1, -Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule Halifax 1962    max -   Oct lastSun 2:00    0   S
    // Rule Halifax 1987    max -   Apr Sun>=1  2:00    1:00    D
    // America/Halifax  ?(CA)   -4:00   Halifax A%sT
    //----------------------------------------------------------
    new TimeZoneImpl(-4*ONE_HOUR, "America/Thule",
                 Calendar.APRIL, 1, -Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule Thule   1993    max -   Apr Sun>=1  2:00    1:00    D
    // Rule Thule   1993    max -   Oct lastSun 2:00    0   S
    // America/Thule    ?(GL)   -4:00   Thule   A%sT
    //----------------------------------------------------------
    new TimeZoneImpl(-4*ONE_HOUR, "America/Asuncion",
                 Calendar.OCTOBER, 1, 0, 0*ONE_HOUR,
                 Calendar.MARCH, 1, 0, 0*ONE_HOUR, 1*ONE_HOUR),
    // Rule Para    1996    max -   Mar 1   0:00    0   -
    // Rule Para    1997    max -   Oct 1   0:00    1:00    S
    // America/Asuncion Paraguay(PY)    -4:00   Para    PY%sT
    //----------------------------------------------------------
    new TimeZoneImpl(-4*ONE_HOUR, "America/Santiago",
                 Calendar.OCTOBER, 9, -Calendar.SUNDAY, 0*ONE_HOUR,
                 Calendar.MARCH, 9, -Calendar.SUNDAY, 0*ONE_HOUR, 1*ONE_HOUR),
    // Rule Chile   1969    max -   Oct Sun>=9  0:00    1:00    S
    // Rule Chile   1970    max -   Mar Sun>=9  0:00    0   -
    // America/Santiago Chile(CL)   -4:00   Chile   CL%sT
    //----------------------------------------------------------
    new TimeZoneImpl((int)(-3.5*ONE_HOUR), "America/St_Johns",
                 Calendar.APRIL, 1, -Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule StJohns 1960    max -   Oct lastSun 2:00    0   S
    // Rule StJohns 1989    max -   Apr Sun>=1  2:00    1:00    D
    // America/St_Johns Canada(CA)  -3:30   StJohns N%sT
    new TimeZoneImpl((int)(-3.5*ONE_HOUR), "CNT",
                 Calendar.APRIL, 1, -Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    //----------------------------------------------------------
    new TimeZoneImpl(-3*ONE_HOUR, "America/Fortaleza"),
    // America/Fortaleza    Brazil(BR)  -3:00   -   EST
    //----------------------------------------------------------
    new TimeZoneImpl(-3*ONE_HOUR, "America/Cayenne"),
    // America/Cayenne  French Guiana(GF)   -3:00   -   GFT
    //----------------------------------------------------------
    new TimeZoneImpl(-3*ONE_HOUR, "America/Paramaribo"),
    // America/Paramaribo   Suriname(SR)    -3:00   -   SRT
    //----------------------------------------------------------
    new TimeZoneImpl(-3*ONE_HOUR, "America/Montevideo"),
    // America/Montevideo   Uruguay(UY) -3:00   -   UY%sT
    //----------------------------------------------------------
    new TimeZoneImpl(-3*ONE_HOUR, "America/Buenos_Aires"),
    // America/Buenos_Aires Argentina(AR)   -3:00   -   AR%sT
    new TimeZoneImpl(-3*ONE_HOUR, "AGT"),
    //----------------------------------------------------------
    new TimeZoneImpl(-3*ONE_HOUR, "America/Godthab",
                 Calendar.MARCH, -1, Calendar.SATURDAY, 22*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SATURDAY, 22*ONE_HOUR, 1*ONE_HOUR),
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // America/Godthab  ?(GL)   -3:00   EU  WG%sT
    //----------------------------------------------------------
    new TimeZoneImpl(-3*ONE_HOUR, "America/Miquelon",
                 Calendar.APRIL, 1, -Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule Mont    1957    max -   Oct lastSun 2:00    0   S
    // Rule Mont    1987    max -   Apr Sun>=1  2:00    1:00    D
    // America/Miquelon St Pierre and Miquelon(PM)  -3:00   Mont    PM%sT   # Pierre & Miquelon Time
    //----------------------------------------------------------
    new TimeZoneImpl(-3*ONE_HOUR, "America/Sao_Paulo",
                 Calendar.OCTOBER, 1, -Calendar.SUNDAY, 0*ONE_HOUR,
                 Calendar.FEBRUARY, 11, -Calendar.SUNDAY, 0*ONE_HOUR, 1*ONE_HOUR),
    // Rule Brazil  1998    max -   Oct Sun>=1  0:00    1:00    D
    // Rule Brazil  1999    max -   Feb Sun>=11 0:00    0   S
    // America/Sao_Paulo    Brazil(BR)  -3:00   Brazil  E%sT
    new TimeZoneImpl(-3*ONE_HOUR, "BET",
                 Calendar.OCTOBER, 1, -Calendar.SUNDAY, 0*ONE_HOUR,
                 Calendar.FEBRUARY, 11, -Calendar.SUNDAY, 0*ONE_HOUR, 1*ONE_HOUR),
    //----------------------------------------------------------
    new TimeZoneImpl(-2*ONE_HOUR, "America/Noronha"),
    // America/Noronha  Brazil(BR)  -2:00   -   FST
    //----------------------------------------------------------
    new TimeZoneImpl(-1*ONE_HOUR, "America/Scoresbysund",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 0*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 0*ONE_HOUR, 1*ONE_HOUR),
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // America/Scoresbysund ?(GL)   -1:00   EU  EG%sT

    ////////////////////////////////////////////////////////////
    // Antarctica
    //----------------------------------------------------------
    new TimeZoneImpl(-4*ONE_HOUR, "Antarctica/Palmer",
                 Calendar.OCTOBER, 9, -Calendar.SUNDAY, 0*ONE_HOUR,
                 Calendar.MARCH, 9, -Calendar.SUNDAY, 0*ONE_HOUR, 1*ONE_HOUR),
    // Rule ChileAQ 1969    max -   Oct Sun>=9  0:00    1:00    S
    // Rule ChileAQ 1970    max -   Mar Sun>=9  0:00    0   -
    // Antarctica/Palmer    USA - year-round bases(AQ)  -4:00   ChileAQ CL%sT
    //----------------------------------------------------------
    new TimeZoneImpl(6*ONE_HOUR, "Antarctica/Mawson"),
    // Antarctica/Mawson    Australia - territories(AQ) 6:00    -   MAWT    # Mawson Time
    //----------------------------------------------------------
    new TimeZoneImpl(8*ONE_HOUR, "Antarctica/Casey"),
    // Antarctica/Casey Australia - territories(AQ) 8:00    -   WST # Western (Aus) Standard Time
    //----------------------------------------------------------
    new TimeZoneImpl(10*ONE_HOUR, "Antarctica/DumontDUrville"),
    // Antarctica/DumontDUrville    France - year-round bases(AQ)   10:00   -   DDUT    # Dumont-d'Urville Time
    //----------------------------------------------------------
    new TimeZoneImpl(12*ONE_HOUR, "Antarctica/McMurdo",
                 Calendar.OCTOBER, 1, -Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.MARCH, 15, -Calendar.SUNDAY, 3*ONE_HOUR, 1*ONE_HOUR),
    // Rule NZAQ    1990    max -   Oct Sun>=1  2:00s   1:00    D
    // Rule NZAQ    1990    max -   Mar Sun>=15 2:00s   0   S
    // Antarctica/McMurdo   USA - year-round bases(AQ)  12:00   NZAQ    NZ%sT

    ////////////////////////////////////////////////////////////
    // Australia
    //----------------------------------------------------------
    new TimeZoneImpl(8*ONE_HOUR, "Australia/Perth"),
    // Australia/Perth  Australia(AU)   8:00    -   WST
    //----------------------------------------------------------
    new TimeZoneImpl((int)(9.5*ONE_HOUR), "Australia/Darwin"),
    // Australia/Darwin Australia(AU)   9:30    -   CST
    new TimeZoneImpl((int)(9.5*ONE_HOUR), "ACT"),
    //----------------------------------------------------------
    new TimeZoneImpl((int)(9.5*ONE_HOUR), "Australia/Adelaide",
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.MARCH, -1, Calendar.SUNDAY, 3*ONE_HOUR, 1*ONE_HOUR),
    // Rule AS  1987    max -   Oct lastSun 2:00s   1:00    -
    // Rule AS  1995    max -   Mar lastSun 2:00s   0   -
    // Australia/Adelaide   South Australia(AU) 9:30    AS  CST
    //----------------------------------------------------------
    new TimeZoneImpl(10*ONE_HOUR, "Australia/Brisbane"),
    // Australia/Brisbane   Australia(AU)   10:00   -   EST
    //----------------------------------------------------------
    new TimeZoneImpl(10*ONE_HOUR, "Australia/Sydney",
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.MARCH, -1, Calendar.SUNDAY, 3*ONE_HOUR, 1*ONE_HOUR),
    // Rule AN  1987    max -   Oct lastSun 2:00s   1:00    -
    // Rule AN  1996    max -   Mar lastSun 2:00s   0   -
    // Australia/Sydney New South Wales(AU) 10:00   AN  EST
    new TimeZoneImpl(10*ONE_HOUR, "AET",
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.MARCH, -1, Calendar.SUNDAY, 3*ONE_HOUR, 1*ONE_HOUR),
    //----------------------------------------------------------
    new TimeZoneImpl((int)(10.5*ONE_HOUR), "Australia/Lord_Howe",
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.MARCH, -1, Calendar.SUNDAY, 3*ONE_HOUR, (int)(0.5*ONE_HOUR)),
    // Rule LH  1987    max -   Oct lastSun 2:00s   0:30    -
    // Rule LH  1996    max -   Mar lastSun 2:00s   0   -
    // Australia/Lord_Howe  Lord Howe Island(AU)    10:30   LH  LHST

    ////////////////////////////////////////////////////////////
    // Atlantic
    //----------------------------------------------------------
    new TimeZoneImpl(-4*ONE_HOUR, "Atlantic/Bermuda",
                 Calendar.APRIL, 1, -Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule Bahamas 1964    max -   Oct lastSun 2:00    0   S
    // Rule Bahamas 1987    max -   Apr Sun>=1  2:00    1:00    D
    // Atlantic/Bermuda Bermuda(BM) -4:00   Bahamas A%sT
    //----------------------------------------------------------
    new TimeZoneImpl(-4*ONE_HOUR, "Atlantic/Stanley",
                 Calendar.SEPTEMBER, 8, -Calendar.SUNDAY, 0*ONE_HOUR,
                 Calendar.APRIL, 16, -Calendar.SUNDAY, 0*ONE_HOUR, 1*ONE_HOUR),
    // Rule Falk    1986    max -   Apr Sun>=16 0:00    0   -
    // Rule Falk    1996    max -   Sep Sun>=8  0:00    1:00    S
    // Atlantic/Stanley Falklands(FK)   -4:00   Falk    FK%sT
    //----------------------------------------------------------
    new TimeZoneImpl(-2*ONE_HOUR, "Atlantic/South_Georgia"),
    // Atlantic/South_Georgia   South Georgia(GS)   -2:00   -   GST # South Georgia Time
    //----------------------------------------------------------
    new TimeZoneImpl(-1*ONE_HOUR, "Atlantic/Jan_Mayen"),
    // Atlantic/Jan_Mayen   ?(NO)   -1:00   -   EGT
    //----------------------------------------------------------
    new TimeZoneImpl(-1*ONE_HOUR, "Atlantic/Cape_Verde"),
    // Atlantic/Cape_Verde  Cape Verde(CV)  -1:00   -   CVT
    //----------------------------------------------------------
    new TimeZoneImpl(-1*ONE_HOUR, "Atlantic/Azores",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 0*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 0*ONE_HOUR, 1*ONE_HOUR),
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Atlantic/Azores  Portugal(PT)    -1:00   EU  AZO%sT
    //----------------------------------------------------------
    new TimeZoneImpl(0*ONE_HOUR, "Atlantic/Reykjavik"),
    // Atlantic/Reykjavik   Iceland(IS) 0:00    -   GMT
    //----------------------------------------------------------
    new TimeZoneImpl(0*ONE_HOUR, "Atlantic/Faeroe",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 1*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 1*ONE_HOUR, 1*ONE_HOUR),
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Atlantic/Faeroe  Denmark, Faeroe Islands, and Greenland(DK)  0:00    EU  WE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(0*ONE_HOUR, "Atlantic/Canary",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 1*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 1*ONE_HOUR, 1*ONE_HOUR),
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Atlantic/Canary  Spain(ES)   0:00    EU  WE%sT

    ////////////////////////////////////////////////////////////
    // Africa
    //----------------------------------------------------------
    new TimeZoneImpl(0*ONE_HOUR, "Africa/Ouagadougou"),
    // Africa/Ouagadougou   Burkina Faso(BF)    0:00    -   GMT
    //----------------------------------------------------------
    new TimeZoneImpl(0*ONE_HOUR, "Africa/Abidjan"),
    // Africa/Abidjan   Cote D'Ivoire(CI)   0:00    -   GMT
    //----------------------------------------------------------
    new TimeZoneImpl(0*ONE_HOUR, "Africa/Accra"),
    // Africa/Accra Ghana(GH)   0:00    -   %s
    //----------------------------------------------------------
    new TimeZoneImpl(0*ONE_HOUR, "Africa/Banjul"),
    // Africa/Banjul    Gambia(GM)  0:00    -   GMT
    //----------------------------------------------------------
    new TimeZoneImpl(0*ONE_HOUR, "Africa/Conakry"),
    // Africa/Conakry   Guinea(GN)  0:00    -   GMT
    //----------------------------------------------------------
    new TimeZoneImpl(0*ONE_HOUR, "Africa/Bissau"),
    // Africa/Bissau    Guinea-Bissau(GW)   0:00    -   GMT
    //----------------------------------------------------------
    new TimeZoneImpl(0*ONE_HOUR, "Africa/Monrovia"),
    // Africa/Monrovia  Liberia(LR) 0:00    -   GMT
    //----------------------------------------------------------
    new TimeZoneImpl(0*ONE_HOUR, "Africa/Casablanca"),
    // Africa/Casablanca    Morocco(MA) 0:00    -   WET
    //----------------------------------------------------------
    new TimeZoneImpl(0*ONE_HOUR, "Africa/Timbuktu"),
    // Africa/Timbuktu  Mali(ML)    0:00    -   GMT
    //----------------------------------------------------------
    new TimeZoneImpl(0*ONE_HOUR, "Africa/Nouakchott"),
    // Africa/Nouakchott    Mauritania(MR)  0:00    -   GMT
    //----------------------------------------------------------
    new TimeZoneImpl(0*ONE_HOUR, "Atlantic/St_Helena"),
    // Atlantic/St_Helena   St Helena(SH)   0:00    -   GMT
    //----------------------------------------------------------
    new TimeZoneImpl(0*ONE_HOUR, "Africa/Freetown"),
    // Africa/Freetown  Sierra Leone(SL)    0:00    -   %s
    //----------------------------------------------------------
    new TimeZoneImpl(0*ONE_HOUR, "Africa/Dakar"),
    // Africa/Dakar Senegal(SN) 0:00    -   GMT
    //----------------------------------------------------------
    new TimeZoneImpl(0*ONE_HOUR, "Africa/Sao_Tome"),
    // Africa/Sao_Tome  Sao Tome and Principe(ST)   0:00    -   GMT
    //----------------------------------------------------------
    new TimeZoneImpl(0*ONE_HOUR, "Africa/Lome"),
    // Africa/Lome  Togo(TG)    0:00    -   GMT
    //----------------------------------------------------------
    new TimeZoneImpl(1*ONE_HOUR, "Africa/Luanda"),
    // Africa/Luanda    Angola(AO)  1:00    -   WAT
    //----------------------------------------------------------
    new TimeZoneImpl(1*ONE_HOUR, "Africa/Porto-Novo"),
    // Africa/Porto-Novo    Benin(BJ)   1:00    -   WAT
    //----------------------------------------------------------
    new TimeZoneImpl(1*ONE_HOUR, "Africa/Bangui"),
    // Africa/Bangui    Central African Republic(CF)    1:00    -   WAT
    //----------------------------------------------------------
    new TimeZoneImpl(1*ONE_HOUR, "Africa/Kinshasa"),
    // Africa/Kinshasa  Democratic Republic of Congo(CG)    1:00    -   WAT
    //----------------------------------------------------------
    new TimeZoneImpl(1*ONE_HOUR, "Africa/Douala"),
    // Africa/Douala    Cameroon(CM)    1:00    -   WAT
    //----------------------------------------------------------
    new TimeZoneImpl(1*ONE_HOUR, "Africa/Libreville"),
    // Africa/Libreville    Gabon(GA)   1:00    -   WAT
    //----------------------------------------------------------
    new TimeZoneImpl(1*ONE_HOUR, "Africa/Malabo"),
    // Africa/Malabo    Equatorial Guinea(GQ)   1:00    -   WAT
    //----------------------------------------------------------
    new TimeZoneImpl(1*ONE_HOUR, "Africa/Niamey"),
    // Africa/Niamey    Niger(NE)   1:00    -   WAT
    //----------------------------------------------------------
    new TimeZoneImpl(1*ONE_HOUR, "Africa/Lagos"),
    // Africa/Lagos Nigeria(NG) 1:00    -   WAT
    //----------------------------------------------------------
    new TimeZoneImpl(1*ONE_HOUR, "Africa/Ndjamena"),
    // Africa/Ndjamena  Chad(TD)    1:00    -   WAT
    //----------------------------------------------------------
    new TimeZoneImpl(1*ONE_HOUR, "Africa/Tunis"),
    // Africa/Tunis Tunisia(TN) 1:00    -   CE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(1*ONE_HOUR, "Africa/Algiers"),
    // Africa/Algiers   Algeria(DZ) 1:00    -   CET
    //----------------------------------------------------------
    new TimeZoneImpl(1*ONE_HOUR, "Africa/Tripoli",
                 Calendar.MARCH, -1, Calendar.THURSDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, 1, -Calendar.THURSDAY, 3*ONE_HOUR, 1*ONE_HOUR),
    // Rule Libya   1997    max -   Mar lastThu 2:00s   1:00    S
    // Rule Libya   1997    max -   Oct Thu>=1  2:00s   0   -
    // Africa/Tripoli   Libya(LY)   1:00    Libya   CE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(1*ONE_HOUR, "Africa/Windhoek",
                 Calendar.SEPTEMBER, 1, -Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.APRIL, 1, -Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule Namibia 1994    max -   Sep Sun>=1  2:00    1:00    S
    // Rule Namibia 1995    max -   Apr Sun>=1  2:00    0   -
    // Africa/Windhoek  Namibia(NA) 1:00    Namibia WA%sT
    //----------------------------------------------------------
    new TimeZoneImpl(2*ONE_HOUR, "Africa/Bujumbura"),
    // Africa/Bujumbura Burundi(BI) 2:00    -   CAT
    //----------------------------------------------------------
    new TimeZoneImpl(2*ONE_HOUR, "Africa/Gaborone"),
    // Africa/Gaborone  Botswana(BW)    2:00    -   CAT
    //----------------------------------------------------------
    new TimeZoneImpl(2*ONE_HOUR, "Africa/Lubumbashi"),
    // Africa/Lubumbashi    Democratic Republic of Congo(CG)    2:00    -   CAT
    //----------------------------------------------------------
    new TimeZoneImpl(2*ONE_HOUR, "Africa/Maseru"),
    // Africa/Maseru    Lesotho(LS) 2:00    -   SAST
    //----------------------------------------------------------
    new TimeZoneImpl(2*ONE_HOUR, "Africa/Blantyre"),
    // Africa/Blantyre  Malawi(ML)  2:00    -   CAT
    //----------------------------------------------------------
    new TimeZoneImpl(2*ONE_HOUR, "Africa/Maputo"),
    // Africa/Maputo    Mozambique(MZ)  2:00    -   CAT
    //----------------------------------------------------------
    new TimeZoneImpl(2*ONE_HOUR, "Africa/Kigali"),
    // Africa/Kigali    Rwanda(RW)  2:00    -   CAT
    //----------------------------------------------------------
    new TimeZoneImpl(2*ONE_HOUR, "Africa/Khartoum"),
    // Africa/Khartoum  Sudan(SD)   2:00    -   CA%sT
    //----------------------------------------------------------
    new TimeZoneImpl(2*ONE_HOUR, "Africa/Mbabane"),
    // Africa/Mbabane   Swaziland(SZ)   2:00    -   SAST
    //----------------------------------------------------------
    new TimeZoneImpl(2*ONE_HOUR, "Africa/Lusaka"),
    // Africa/Lusaka    Zambia(ZM)  2:00    -   CAT
    //----------------------------------------------------------
    new TimeZoneImpl(2*ONE_HOUR, "Africa/Harare"),
    // Africa/Harare    Zimbabwe(ZW)    2:00    -   CAT
    new TimeZoneImpl(2*ONE_HOUR, "CAT"),
    //----------------------------------------------------------
    new TimeZoneImpl(2*ONE_HOUR, "Africa/Johannesburg"),
    // Africa/Johannesburg  South Africa(ZA)    2:00    -   SAST
    //----------------------------------------------------------
    new TimeZoneImpl(2*ONE_HOUR, "Africa/Cairo",
                 Calendar.APRIL, -1, Calendar.FRIDAY, 1*ONE_HOUR,
                 Calendar.SEPTEMBER, -1, Calendar.FRIDAY, 3*ONE_HOUR, 1*ONE_HOUR),
    // Rule Egypt   1995    max -   Apr lastFri 1:00    1:00    S
    // Rule Egypt   1995    max -   Sep lastFri 3:00    0   -
    // Africa/Cairo Egypt(EG)   2:00    Egypt   EE%sT
    new TimeZoneImpl(2*ONE_HOUR, "ART",
                 Calendar.APRIL, -1, Calendar.FRIDAY, 1*ONE_HOUR,
                 Calendar.SEPTEMBER, -1, Calendar.FRIDAY, 3*ONE_HOUR, 1*ONE_HOUR),
    //----------------------------------------------------------
    new TimeZoneImpl(3*ONE_HOUR, "Africa/Djibouti"),
    // Africa/Djibouti  Djibouti(DJ)    3:00    -   EAT
    //----------------------------------------------------------
    new TimeZoneImpl(3*ONE_HOUR, "Africa/Asmera"),
    // Africa/Asmera    Eritrea(ER) 3:00    -   EAT
    //----------------------------------------------------------
    new TimeZoneImpl(3*ONE_HOUR, "Africa/Addis_Ababa"),
    // Africa/Addis_Ababa   Ethiopia(ET)    3:00    -   EAT
    new TimeZoneImpl(3*ONE_HOUR, "EAT"),
    //----------------------------------------------------------
    new TimeZoneImpl(3*ONE_HOUR, "Africa/Nairobi"),
    // Africa/Nairobi   Kenya(KE)   3:00    -   EAT
    //----------------------------------------------------------
    new TimeZoneImpl(3*ONE_HOUR, "Africa/Mogadishu"),
    // Africa/Mogadishu Somalia(SO) 3:00    -   EAT
    //----------------------------------------------------------
    new TimeZoneImpl(3*ONE_HOUR, "Africa/Dar_es_Salaam"),
    // Africa/Dar_es_Salaam Tanzania(TZ)    3:00    -   EAT
    //----------------------------------------------------------
    new TimeZoneImpl(3*ONE_HOUR, "Africa/Kampala"),
    // Africa/Kampala   Uganda(UG)  3:00    -   EAT

    ////////////////////////////////////////////////////////////
    // Europe
    //----------------------------------------------------------
    new TimeZoneImpl(0*ONE_HOUR, "Europe/Dublin",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 1*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 1*ONE_HOUR, 1*ONE_HOUR),
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Dublin    ---(IE) 0:00    EU  GMT/IST
    //----------------------------------------------------------
    new TimeZoneImpl(0*ONE_HOUR, "Europe/Lisbon",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 1*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 1*ONE_HOUR, 1*ONE_HOUR),
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Lisbon    Portugal(PT)    0:00    EU  WE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(0*ONE_HOUR, "Europe/London",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 1*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 1*ONE_HOUR, 1*ONE_HOUR),
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/London    ---(GB) 0:00    EU  GMT/BST
    //----------------------------------------------------------
    new TimeZoneImpl(1*ONE_HOUR, "Europe/Andorra",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Andorra   Andorra(AD) 1:00    EU  CE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(1*ONE_HOUR, "Europe/Tirane",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Tirane    Albania(AL) 1:00    EU  CE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(1*ONE_HOUR, "Europe/Vienna",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Vienna    Austria(AT) 1:00    EU  CE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(1*ONE_HOUR, "Europe/Brussels",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Brussels  Belgium(BE) 1:00    EU  CE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(1*ONE_HOUR, "Europe/Zurich",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Zurich    Switzerland(CH) 1:00    EU  CE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(1*ONE_HOUR, "Europe/Prague",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Prague    Czech Republic(CZ)  1:00    EU  CE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(1*ONE_HOUR, "Europe/Berlin",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Berlin    Germany(DE) 1:00    EU  CE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(1*ONE_HOUR, "Europe/Copenhagen",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Copenhagen    Denmark, Faeroe Islands, and Greenland(DK)  1:00    EU  CE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(1*ONE_HOUR, "Europe/Madrid",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Madrid    Spain(ES)   1:00    EU  CE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(1*ONE_HOUR, "Europe/Gibraltar",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Gibraltar Gibraltar(GI)   1:00    EU  CE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(1*ONE_HOUR, "Europe/Budapest",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Budapest  Hungary(HU) 1:00    EU  CE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(1*ONE_HOUR, "Europe/Rome",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Rome  Italy(IT)   1:00    EU  CE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(1*ONE_HOUR, "Europe/Vaduz",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Vaduz Liechtenstein(LI)   1:00    EU  CE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(1*ONE_HOUR, "Europe/Luxembourg",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Luxembourg    Luxembourg(LU)  1:00    EU  CE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(1*ONE_HOUR, "Europe/Monaco",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Monaco    Monaco(MC)  1:00    EU  CE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(1*ONE_HOUR, "Europe/Malta",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Malta Malta(MT)   1:00    EU  CE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(1*ONE_HOUR, "Europe/Amsterdam",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Amsterdam Netherlands(NL) 1:00    EU  CE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(1*ONE_HOUR, "Europe/Oslo",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Oslo  Norway(NO)  1:00    EU  CE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(1*ONE_HOUR, "Europe/Warsaw",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 1*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule W-Eur   1981    max -   Mar lastSun 1:00s   1:00    S
    // Rule W-Eur   1996    max -   Oct lastSun 1:00s   0   -
    // Europe/Warsaw    Poland(PL)  1:00    W-Eur   CE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(1*ONE_HOUR, "Europe/Stockholm",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Stockholm Sweden(SE)  1:00    EU  CE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(1*ONE_HOUR, "Europe/Belgrade",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Belgrade  Yugoslavia(YU)  1:00    EU  CE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(1*ONE_HOUR, "Europe/Paris",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Paris France(FR)  1:00    EU  CE%sT
    new TimeZoneImpl(1*ONE_HOUR, "ECT",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*ONE_HOUR, 1*ONE_HOUR),
    new TimeZoneImpl(2*ONE_HOUR, "Europe/Sofia",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 0*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 0*ONE_HOUR, 1*ONE_HOUR),
    // Rule E-Eur   1981    max -   Mar lastSun 0:00    1:00    S
    // Rule E-Eur   1996    max -   Oct lastSun 0:00    0   -
    // Europe/Sofia Bulgaria(BG)    2:00    E-Eur   EE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(2*ONE_HOUR, "Europe/Minsk",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 3*ONE_HOUR, 1*ONE_HOUR),
    // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
    // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
    // Europe/Minsk Belarus(BY) 2:00    Russia  EE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(2*ONE_HOUR, "Europe/Tallinn",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 3*ONE_HOUR, 1*ONE_HOUR),
    // Rule C-Eur   1981    max -   Mar lastSun 2:00s   1:00    S
    // Rule C-Eur   1996    max -   Oct lastSun 2:00s   0   -
    // Europe/Tallinn   Estonia(EE) 2:00    C-Eur   EE%sT
    //----------------------------------------------------------

    new TimeZoneImpl(2*ONE_HOUR, "Europe/Helsinki",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 3*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 3*ONE_HOUR, 1*ONE_HOUR),
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Helsinki  Finland(FI) 2:00    EU  EE%sT
    //----------------------------------------------------------

    new TimeZoneImpl(2*ONE_HOUR, "Europe/Athens",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 3*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 3*ONE_HOUR, 1*ONE_HOUR),
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Athens    Greece(GR)  2:00    EU  EE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(2*ONE_HOUR, "Europe/Vilnius",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 3*ONE_HOUR, 1*ONE_HOUR),
    // Rule C-Eur   1981    max -   Mar lastSun 2:00s   1:00    S
    // Rule C-Eur   1996    max -   Oct lastSun 2:00s   0   -
    // Europe/Vilnius   Lithuania(LT)   2:00    C-Eur   EE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(2*ONE_HOUR, "Europe/Riga",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.SEPTEMBER, -1, Calendar.SUNDAY, 3*ONE_HOUR, 1*ONE_HOUR),
    // Rule Latvia  1992    max -   Mar lastSun 2:00s   1:00    S
    // Rule Latvia  1992    max -   Sep lastSun 2:00s   0   -
    // Europe/Riga  Latvia(LV)  2:00    Latvia  EE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(2*ONE_HOUR, "Europe/Chisinau",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 0*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 0*ONE_HOUR, 1*ONE_HOUR),
    // Rule E-Eur   1981    max -   Mar lastSun 0:00    1:00    S
    // Rule E-Eur   1996    max -   Oct lastSun 0:00    0   -
    // Europe/Chisinau  Moldova(MD) 2:00    E-Eur   EE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(2*ONE_HOUR, "Europe/Bucharest",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 0*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 0*ONE_HOUR, 1*ONE_HOUR),
    // Rule E-Eur   1981    max -   Mar lastSun 0:00    1:00    S
    // Rule E-Eur   1996    max -   Oct lastSun 0:00    0   -
    // Europe/Bucharest Romania(RO) 2:00    E-Eur   EE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(2*ONE_HOUR, "Europe/Kaliningrad",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 3*ONE_HOUR, 1*ONE_HOUR),
    // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
    // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
    // Europe/Kaliningrad   Russia(RU)  2:00    Russia  EE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(2*ONE_HOUR, "Europe/Kiev",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 3*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 3*ONE_HOUR, 1*ONE_HOUR),
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Kiev  Ukraine(UA) 2:00    EU  EE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(2*ONE_HOUR, "Europe/Istanbul",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 3*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 3*ONE_HOUR, 1*ONE_HOUR),
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Istanbul  Turkey(TR)  2:00    EU  EE%sT
    new TimeZoneImpl(2*ONE_HOUR, "EET",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 3*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 3*ONE_HOUR, 1*ONE_HOUR),
    //----------------------------------------------------------
    new TimeZoneImpl(3*ONE_HOUR, "Europe/Simferopol",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 3*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 3*ONE_HOUR, 1*ONE_HOUR),
    // Rule Crimea  1996    max -   Mar lastSun 0:00u   1:00    -
    // Rule Crimea  1996    max -   Oct lastSun 0:00u   0   -
    // Europe/Simferopol    Ukraine(UA) 3:00    Crimea  MSK/MSD
    //----------------------------------------------------------
    new TimeZoneImpl(3*ONE_HOUR, "Europe/Moscow",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 3*ONE_HOUR, 1*ONE_HOUR),
    // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
    // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
    // Europe/Moscow    Russia(RU)  3:00    Russia  MSK/MSD
    //----------------------------------------------------------
    new TimeZoneImpl(4*ONE_HOUR, "Europe/Samara",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 3*ONE_HOUR, 1*ONE_HOUR),
    // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
    // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
    // Europe/Samara    Russia(RU)  4:00    Russia  SAM%sT

    ////////////////////////////////////////////////////////////
    // Asia
    //----------------------------------------------------------
    new TimeZoneImpl(2*ONE_HOUR, "Asia/Nicosia",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 0*ONE_HOUR,
                 Calendar.SEPTEMBER, -1, Calendar.SUNDAY, 0*ONE_HOUR, 1*ONE_HOUR),
    // Rule Cyprus  1979    max -   Sep lastSun 0:00    0   -
    // Rule Cyprus  1981    max -   Mar lastSun 0:00    1:00    S
    // Asia/Nicosia Cyprus(CY)  2:00    Cyprus  EE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(2*ONE_HOUR, "Asia/Jerusalem",
                 Calendar.MARCH, 15, -Calendar.FRIDAY, 0*ONE_HOUR,
                 Calendar.SEPTEMBER, 1, -Calendar.SUNDAY, 0*ONE_HOUR, 1*ONE_HOUR),
    // Rule Zion    1999    max -   Mar Fri>=15 0:00    1:00    D
    // Rule Zion    1999    max -   Sep Sun>=1  0:00    0   S
    // Asia/Jerusalem   Israel(IL)  2:00    Zion    I%sT
    //----------------------------------------------------------
    new TimeZoneImpl(2*ONE_HOUR, "Asia/Amman",
                 Calendar.APRIL, 1, -Calendar.FRIDAY, 0*ONE_HOUR,
                 Calendar.SEPTEMBER, 15, -Calendar.FRIDAY, 1*ONE_HOUR, 1*ONE_HOUR),
    // Rule    Jordan   1993    max -   Apr Fri>=1  0:00    1:00    S
    // Rule    Jordan   1995    max -   Sep Fri>=15 0:00s   0   -
    // Asia/Amman   Jordan(JO)  2:00    Jordan  EE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(2*ONE_HOUR, "Asia/Beirut",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 0*ONE_HOUR,
                 Calendar.SEPTEMBER, -1, Calendar.SUNDAY, 0*ONE_HOUR, 1*ONE_HOUR),
    // Rule Lebanon 1993    max -   Mar lastSun 0:00    1:00    S
    // Rule Lebanon 1993    max -   Sep lastSun 0:00    0   -
    // Asia/Beirut  Lebanon(LB) 2:00    Lebanon EE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(2*ONE_HOUR, "Asia/Damascus",
                 Calendar.APRIL, 1, 0, 0*ONE_HOUR,
                 Calendar.OCTOBER, 1, 0, 0*ONE_HOUR, 1*ONE_HOUR),
    // Rule Syria   1994    max -   Apr 1   0:00    1:00    S
    // Rule Syria   1994    max -   Oct 1   0:00    0   -
    // Asia/Damascus    Syria(SY)   2:00    Syria   EE%sT
    //----------------------------------------------------------
    new TimeZoneImpl(3*ONE_HOUR, "Asia/Bahrain"),
    // Asia/Bahrain Bahrain(BH) 3:00    -   AST
    //----------------------------------------------------------
    new TimeZoneImpl(3*ONE_HOUR, "Asia/Kuwait"),
    // Asia/Kuwait  Kuwait(KW)  3:00    -   AST
    //----------------------------------------------------------
    new TimeZoneImpl(3*ONE_HOUR, "Asia/Qatar"),
    // Asia/Qatar   Qatar(QA)   3:00    -   AST
    //----------------------------------------------------------
    new TimeZoneImpl(3*ONE_HOUR, "Asia/Aden"),
    // Asia/Aden    Yemen(YE)   3:00    -   AST
    //----------------------------------------------------------
    new TimeZoneImpl(3*ONE_HOUR, "Asia/Riyadh"),
    // Asia/Riyadh  Saudi Arabia(SA)    3:00    -   AST
    //----------------------------------------------------------
    new TimeZoneImpl(3*ONE_HOUR, "Asia/Baghdad",
                 Calendar.APRIL, 1, 0, 3*ONE_HOUR,
                 Calendar.OCTOBER, 1, 0, 4*ONE_HOUR, 1*ONE_HOUR),
    // Rule Iraq    1991    max -   Apr 1   3:00s   1:00    D
    // Rule Iraq    1991    max -   Oct 1   3:00s   0   D
    // Asia/Baghdad Iraq(IQ)    3:00    Iraq    A%sT
    //----------------------------------------------------------
    new TimeZoneImpl((int)(3.5*ONE_HOUR), "Asia/Tehran",
                 Calendar.MARCH, 21, 0, 0*ONE_HOUR,
                 Calendar.SEPTEMBER, 23, 0, 0*ONE_HOUR, 1*ONE_HOUR),
    // Rule Iran    1997    1999    -   Mar 21  0:00    1:00    S
    // Rule Iran    1997    1999    -   Sep 23  0:00    0   -
    // Asia/Tehran  Iran(IR)    3:30    Iran    IR%sT
    new TimeZoneImpl((int)(3.5*ONE_HOUR), "MET",
                 Calendar.MARCH, 21, 0, 0*ONE_HOUR,
                 Calendar.SEPTEMBER, 23, 0, 0*ONE_HOUR, 1*ONE_HOUR),
    //----------------------------------------------------------
    new TimeZoneImpl(4*ONE_HOUR, "Asia/Dubai"),
    // Asia/Dubai   United Arab Emirates(AE)    4:00    -   GST
    //----------------------------------------------------------
    new TimeZoneImpl(4*ONE_HOUR, "Asia/Muscat"),
    // Asia/Muscat  Oman(OM)    4:00    -   GST
    //----------------------------------------------------------
    new TimeZoneImpl(4*ONE_HOUR, "Asia/Yerevan"),
    // Asia/Yerevan Armenia(AM) 4:00    -   AM%sT
    new TimeZoneImpl(4*ONE_HOUR, "NET"),
    //----------------------------------------------------------
    new TimeZoneImpl(4*ONE_HOUR, "Asia/Baku",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 5*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 5*ONE_HOUR, 1*ONE_HOUR),
    // Rule EUAsia  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EUAsia  1996    max -   Oct lastSun 1:00u   0   -
    // Asia/Baku    Azerbaijan(AZ)  4:00    EUAsia  AZ%sT
    //----------------------------------------------------------
    new TimeZoneImpl(4*ONE_HOUR, "Asia/Aqtau",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 0*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 0*ONE_HOUR, 1*ONE_HOUR),
    // Rule E-EurAsia   1981    max -   Mar lastSun 0:00    1:00    S
    // Rule E-EurAsia   1996    max -   Oct lastSun 0:00    0   -
    // Asia/Aqtau   Kazakhstan(KZ)  4:00    E-EurAsia   AQT%sT
    //----------------------------------------------------------
    new TimeZoneImpl((int)(4.5*ONE_HOUR), "Asia/Kabul"),
    // Asia/Kabul   Afghanistan(AF) 4:30    -   AFT
    //----------------------------------------------------------
    new TimeZoneImpl(5*ONE_HOUR, "Asia/Tbilisi"),
    // Asia/Tbilisi Georgia(GE) 5:00    -   GET
    //----------------------------------------------------------
    new TimeZoneImpl(5*ONE_HOUR, "Asia/Dushanbe"),
    // Asia/Dushanbe    Tajikistan(TJ)  5:00    -   TJT # Tajikistan Time
    //----------------------------------------------------------
    new TimeZoneImpl(5*ONE_HOUR, "Asia/Ashkhabad"),
    // Asia/Ashkhabad   Turkmenistan(TM)    5:00    -   TMT # Turkmenistan Time
    //----------------------------------------------------------
    new TimeZoneImpl(5*ONE_HOUR, "Asia/Tashkent"),
    // Asia/Tashkent    Uzbekistan(UZ)  5:00    -   UZT # Uzbekistan Time
    //----------------------------------------------------------
    new TimeZoneImpl(5*ONE_HOUR, "Asia/Karachi"),
    // Asia/Karachi Pakistan(PK)    5:00    -   PKT # Pakistan Time
    new TimeZoneImpl(5*ONE_HOUR, "PLT"),
    //----------------------------------------------------------
    new TimeZoneImpl(5*ONE_HOUR, "Asia/Bishkek",
                 Calendar.APRIL, 7, -Calendar.SUNDAY, 0*ONE_HOUR,
                 Calendar.SEPTEMBER, -1, Calendar.SUNDAY, 0*ONE_HOUR, 1*ONE_HOUR),
    // Rule Kirgiz  1992    max -   Apr Sun>=7  0:00    1:00    S
    // Rule Kirgiz  1991    max -   Sep lastSun 0:00    0   -
    // Asia/Bishkek Kirgizstan(KG)  5:00    Kirgiz  KG%sT   # Kirgizstan Time
    //----------------------------------------------------------
    new TimeZoneImpl(5*ONE_HOUR, "Asia/Aqtobe",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 0*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 0*ONE_HOUR, 1*ONE_HOUR),
    // Rule E-EurAsia   1981    max -   Mar lastSun 0:00    1:00    S
    // Rule E-EurAsia   1996    max -   Oct lastSun 0:00    0   -
    // Asia/Aqtobe  Kazakhstan(KZ)  5:00    E-EurAsia   AQT%sT
    //----------------------------------------------------------
    new TimeZoneImpl(5*ONE_HOUR, "Asia/Yekaterinburg",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 3*ONE_HOUR, 1*ONE_HOUR),
    // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
    // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
    // Asia/Yekaterinburg   Russia(RU)  5:00    Russia  YEK%sT  # Yekaterinburg Time
    //----------------------------------------------------------
    new TimeZoneImpl((int)(5.5*ONE_HOUR), "Asia/Calcutta"),
    // Asia/Calcutta    India(IN)   5:30    -   IST
    new TimeZoneImpl((int)(5.5*ONE_HOUR), "IST"),
    //----------------------------------------------------------
    new TimeZoneImpl((int)(5.75*ONE_HOUR), "Asia/Katmandu"),
    // Asia/Katmandu    Nepal(NP)   5:45    -   NPT # Nepal Time
    //----------------------------------------------------------
    new TimeZoneImpl(6*ONE_HOUR, "Asia/Thimbu"),
    // Asia/Thimbu  Bhutan(BT)  6:00    -   BTT # Bhutan Time
    //----------------------------------------------------------
    new TimeZoneImpl(6*ONE_HOUR, "Asia/Colombo"),
    // Asia/Colombo Sri Lanka(LK)   6:00    -   LKT
    //----------------------------------------------------------
    new TimeZoneImpl(6*ONE_HOUR, "Asia/Dacca"),
    // Asia/Dacca   Bangladesh(BD)  6:00    -   BDT # Bangladesh Time
    new TimeZoneImpl(6*ONE_HOUR, "BST"),
    //----------------------------------------------------------
    new TimeZoneImpl(6*ONE_HOUR, "Asia/Alma-Ata",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 0*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 0*ONE_HOUR, 1*ONE_HOUR),
    // Rule E-EurAsia   1981    max -   Mar lastSun 0:00    1:00    S
    // Rule E-EurAsia   1996    max -   Oct lastSun 0:00    0   -
    // Asia/Alma-Ata    Kazakhstan(KZ)  6:00    E-EurAsia   ALM%sT
    //----------------------------------------------------------
    new TimeZoneImpl(6*ONE_HOUR, "Asia/Novosibirsk",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 3*ONE_HOUR, 1*ONE_HOUR),
    // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
    // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
    // Asia/Novosibirsk Russia(RU)  6:00    Russia  NOV%sT
    //----------------------------------------------------------
    new TimeZoneImpl((int)(6.5*ONE_HOUR), "Asia/Rangoon"),
    // Asia/Rangoon Burma / Myanmar(MM) 6:30    -   MMT # Myanmar Time
    //----------------------------------------------------------
    new TimeZoneImpl(7*ONE_HOUR, "Asia/Jakarta"),
    // Asia/Jakarta Indonesia(ID)   7:00    -   JAVT
    //----------------------------------------------------------
    new TimeZoneImpl(7*ONE_HOUR, "Asia/Phnom_Penh"),
    // Asia/Phnom_Penh  Cambodia(KH)    7:00    -   ICT
    //----------------------------------------------------------
    new TimeZoneImpl(7*ONE_HOUR, "Asia/Vientiane"),
    // Asia/Vientiane   Laos(LA)    7:00    -   ICT
    //----------------------------------------------------------
    new TimeZoneImpl(7*ONE_HOUR, "Asia/Saigon"),
    // Asia/Saigon  Vietnam(VN) 7:00    -   ICT
    new TimeZoneImpl(7*ONE_HOUR, "VST"),
    //----------------------------------------------------------
    new TimeZoneImpl(7*ONE_HOUR, "Asia/Bangkok"),
    // Asia/Bangkok Thailand(TH)    7:00    -   ICT
    //----------------------------------------------------------
    new TimeZoneImpl(7*ONE_HOUR, "Asia/Krasnoyarsk",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 3*ONE_HOUR, 1*ONE_HOUR),
    // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
    // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
    // Asia/Krasnoyarsk Russia(RU)  7:00    Russia  KRA%sT
    //----------------------------------------------------------
    new TimeZoneImpl(8*ONE_HOUR, "Asia/Brunei"),
    // Asia/Brunei  Brunei(BN)  8:00    -   BNT
    //----------------------------------------------------------
    new TimeZoneImpl(8*ONE_HOUR, "Asia/Hong_Kong"),
    // Asia/Hong_Kong   China(HK)   8:00    -   C%sT
    //----------------------------------------------------------
    new TimeZoneImpl(8*ONE_HOUR, "Asia/Ujung_Pandang"),
    // Asia/Ujung_Pandang   Indonesia(ID)   8:00    -   BORT
    //----------------------------------------------------------
    new TimeZoneImpl(8*ONE_HOUR, "Asia/Ishigaki"),
    // Asia/Ishigaki    Japan(JP)   8:00    -   CST
    //----------------------------------------------------------
    new TimeZoneImpl(8*ONE_HOUR, "Asia/Macao"),
    // Asia/Macao   Macao(MO)   8:00    -   C%sT
    //----------------------------------------------------------
    new TimeZoneImpl(8*ONE_HOUR, "Asia/Kuala_Lumpur"),
    // Asia/Kuala_Lumpur    Malaysia(MY)    8:00    -   MYT # Malaysia Time
    //----------------------------------------------------------
    new TimeZoneImpl(8*ONE_HOUR, "Asia/Manila"),
    // Asia/Manila  Philippines(PH) 8:00    -   PH%sT
    //----------------------------------------------------------
    new TimeZoneImpl(8*ONE_HOUR, "Asia/Singapore"),
    // Asia/Singapore   Singapore(SG)   8:00    -   SGT
    //----------------------------------------------------------
    new TimeZoneImpl(8*ONE_HOUR, "Asia/Taipei"),
    // Asia/Taipei  Taiwan(TW)  8:00    -   C%sT
    //----------------------------------------------------------
    new TimeZoneImpl(8*ONE_HOUR, "Asia/Shanghai"),
    // Asia/Shanghai    China(CN)   8:00    -   C%sT
    new TimeZoneImpl(8*ONE_HOUR, "CTT"),
    //----------------------------------------------------------
    new TimeZoneImpl(8*ONE_HOUR, "Asia/Ulan_Bator",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 0*ONE_HOUR,
                 Calendar.SEPTEMBER, -1, Calendar.SUNDAY, 0*ONE_HOUR, 1*ONE_HOUR),
    // Rule Mongol  1991    max -   Mar lastSun 0:00    1:00    S
    // Rule Mongol  1997    max -   Sep lastSun 0:00    0   -
    // Asia/Ulan_Bator  Mongolia(MN)    8:00    Mongol  ULA%sT
    //----------------------------------------------------------
    new TimeZoneImpl(8*ONE_HOUR, "Asia/Irkutsk",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 3*ONE_HOUR, 1*ONE_HOUR),
    // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
    // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
    // Asia/Irkutsk Russia(RU)  8:00    Russia  IRK%sT
    //----------------------------------------------------------
    new TimeZoneImpl(9*ONE_HOUR, "Asia/Jayapura"),
    // Asia/Jayapura    Indonesia(ID)   9:00    -   JAYT
    //----------------------------------------------------------
    new TimeZoneImpl(9*ONE_HOUR, "Asia/Pyongyang"),
    // Asia/Pyongyang   ?(KP)   9:00    -   KST
    //----------------------------------------------------------
    new TimeZoneImpl(9*ONE_HOUR, "Asia/Seoul"),
    // Asia/Seoul   ?(KR)   9:00    -   K%sT
    //----------------------------------------------------------
    new TimeZoneImpl(9*ONE_HOUR, "Asia/Tokyo"),
    // Asia/Tokyo   Japan(JP)   9:00    -   JST
    new TimeZoneImpl(9*ONE_HOUR, "JST"),
    //----------------------------------------------------------
    new TimeZoneImpl(9*ONE_HOUR, "Asia/Yakutsk",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 3*ONE_HOUR, 1*ONE_HOUR),
    // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
    // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
    // Asia/Yakutsk Russia(RU)  9:00    Russia  YAK%sT
    //----------------------------------------------------------
    new TimeZoneImpl(10*ONE_HOUR, "Asia/Vladivostok",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 3*ONE_HOUR, 1*ONE_HOUR),
    // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
    // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
    // Asia/Vladivostok Russia(RU)  10:00   Russia  VLA%sT
    //----------------------------------------------------------
    new TimeZoneImpl(11*ONE_HOUR, "Asia/Magadan",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 3*ONE_HOUR, 1*ONE_HOUR),
    // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
    // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
    // Asia/Magadan Russia(RU)  11:00   Russia  MAG%sT
    //----------------------------------------------------------
    new TimeZoneImpl(12*ONE_HOUR, "Asia/Kamchatka",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 3*ONE_HOUR, 1*ONE_HOUR),
    // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
    // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
    // Asia/Kamchatka   Russia(RU)  12:00   Russia  PET%sT
    //----------------------------------------------------------
    new TimeZoneImpl(13*ONE_HOUR, "Asia/Anadyr",
                 Calendar.MARCH, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 3*ONE_HOUR, 1*ONE_HOUR),
    // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
    // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
    // Asia/Anadyr  Russia(RU)  13:00   Russia  ANA%sT

    ////////////////////////////////////////////////////////////
    // India
    //----------------------------------------------------------
    new TimeZoneImpl(3*ONE_HOUR, "Indian/Comoro"),
    // Indian/Comoro    Comoros(KM) 3:00    -   EAT
    //----------------------------------------------------------
    new TimeZoneImpl(3*ONE_HOUR, "Indian/Antananarivo"),
    // Indian/Antananarivo  Madagascar(MK)  3:00    -   EAT
    //----------------------------------------------------------
    new TimeZoneImpl(3*ONE_HOUR, "Indian/Mayotte"),
    // Indian/Mayotte   Mayotte(YT) 3:00    -   EAT
    //----------------------------------------------------------
    new TimeZoneImpl(4*ONE_HOUR, "Indian/Mauritius"),
    // Indian/Mauritius Mauritius(MU)   4:00    -   MUT # Mauritius Time
    //----------------------------------------------------------
    new TimeZoneImpl(4*ONE_HOUR, "Indian/Reunion"),
    // Indian/Reunion   Reunion(RE) 4:00    -   RET # Reunion Time
    //----------------------------------------------------------
    new TimeZoneImpl(4*ONE_HOUR, "Indian/Mahe"),
    // Indian/Mahe  Seychelles(SC)  4:00    -   SCT # Seychelles Time
    //----------------------------------------------------------
    new TimeZoneImpl(5*ONE_HOUR, "Indian/Kerguelen"),
    // Indian/Kerguelen France - year-round bases(FR)   5:00    -   TFT # ISO code TF Time
    //----------------------------------------------------------
    new TimeZoneImpl(5*ONE_HOUR, "Indian/Chagos"),
    // Indian/Chagos    British Indian Ocean Territory(IO)  5:00    -   IOT # BIOT Time
    //----------------------------------------------------------
    new TimeZoneImpl(5*ONE_HOUR, "Indian/Maldives"),
    // Indian/Maldives  Maldives(MV)    5:00    -   MVT # Maldives Time
    //----------------------------------------------------------
    new TimeZoneImpl((int)(6.5*ONE_HOUR), "Indian/Cocos"),
    // Indian/Cocos Cocos(CC)   6:30    -   CCT # Cocos Islands Time
    //----------------------------------------------------------
    new TimeZoneImpl(7*ONE_HOUR, "Indian/Christmas"),
    // Indian/Christmas Australian miscellany(AU)   7:00    -   CXT # Christmas Island Time

    ////////////////////////////////////////////////////////////
    // Pacific
    //----------------------------------------------------------
    new TimeZoneImpl(9*ONE_HOUR, "Pacific/Palau"),
    // Pacific/Palau    Palau(PW)   9:00    -   PWT # Palau Time
    //----------------------------------------------------------
    new TimeZoneImpl(10*ONE_HOUR, "Pacific/Truk"),
    // Pacific/Truk Micronesia(FM)  10:00   -   TRUT    # Truk Time
    //----------------------------------------------------------
    new TimeZoneImpl(10*ONE_HOUR, "Pacific/Guam"),
    // Pacific/Guam Guam(GU)    10:00   -   GST
    //----------------------------------------------------------
    new TimeZoneImpl(10*ONE_HOUR, "Pacific/Saipan"),
    // Pacific/Saipan   N Mariana Is(MP)    10:00   -   MPT
    //----------------------------------------------------------
    new TimeZoneImpl(10*ONE_HOUR, "Pacific/Port_Moresby"),
    // Pacific/Port_Moresby Papua New Guinea(PG)    10:00   -   PGT # Papua New Guinea Time
    //----------------------------------------------------------
    new TimeZoneImpl(11*ONE_HOUR, "Pacific/Ponape"),
    // Pacific/Ponape   Micronesia(FM)  11:00   -   PONT    # Ponape Time
    //----------------------------------------------------------
    new TimeZoneImpl(11*ONE_HOUR, "Pacific/Efate"),
    // Pacific/Efate    Vanuatu(VU) 11:00   -   VU%sT   # Vanuatu Time
    //----------------------------------------------------------
    new TimeZoneImpl(11*ONE_HOUR, "Pacific/Guadalcanal"),
    // Pacific/Guadalcanal  Solomon Is(SB)  11:00   -   SBT # Solomon Is Time
    new TimeZoneImpl(11*ONE_HOUR, "SST"),
    //----------------------------------------------------------
    new TimeZoneImpl(11*ONE_HOUR, "Pacific/Noumea",
                 Calendar.NOVEMBER, -1, Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.MARCH, 1, -Calendar.SUNDAY, 3*ONE_HOUR, 1*ONE_HOUR),
    // Rule NC  1997    max -   Mar Sun>=1  2:00s   0   -
    // Rule NC  1997    max -   Nov lastSun 2:00s   1:00    S
    // Pacific/Noumea   New Caledonia(NC)   11:00   NC  NC%sT
    //----------------------------------------------------------
    new TimeZoneImpl(-11*ONE_HOUR, "Pacific/Niue"),
    // Pacific/Niue Niue(NU)    -11:00  -   NUT
    //----------------------------------------------------------
    new TimeZoneImpl(-11*ONE_HOUR, "Pacific/Apia"),
    // Pacific/Apia W Samoa(WS) -11:00  -   WST # W Samoa Time
    new TimeZoneImpl(-11*ONE_HOUR, "MIT"),
    //----------------------------------------------------------
    new TimeZoneImpl(-11*ONE_HOUR, "Pacific/Pago_Pago"),
    // Pacific/Pago_Pago    American Samoa(US)  -11:00  -   SST # S=Samoa
    //----------------------------------------------------------
    new TimeZoneImpl(-10*ONE_HOUR, "Pacific/Tahiti"),
    // Pacific/Tahiti   French Polynesia(PF)    -10:00  -   TAHT    # Tahiti Time
    //----------------------------------------------------------
    new TimeZoneImpl(-10*ONE_HOUR, "Pacific/Fakaofo"),
    // Pacific/Fakaofo  Tokelau Is(TK)  -10:00  -   TKT # Tokelau Time
    //----------------------------------------------------------
    new TimeZoneImpl(-10*ONE_HOUR, "Pacific/Honolulu"),
    // Pacific/Honolulu Hawaii(US)  -10:00  -   HST
    new TimeZoneImpl(-10*ONE_HOUR, "HST"),
    //----------------------------------------------------------
    new TimeZoneImpl(-10*ONE_HOUR, "Pacific/Rarotonga",
                 Calendar.OCTOBER, -1, Calendar.SUNDAY, 0*ONE_HOUR,
                 Calendar.MARCH, 1, -Calendar.SUNDAY, 0*ONE_HOUR, (int)(0.5*ONE_HOUR)),
    // Rule Cook    1979    max -   Mar Sun>=1  0:00    0   -
    // Rule Cook    1979    max -   Oct lastSun 0:00    0:30    HS
    // Pacific/Rarotonga    Cook Is(CK) -10:00  Cook    CK%sT
    //----------------------------------------------------------
    new TimeZoneImpl((int)(-9.5*ONE_HOUR), "Pacific/Marquesas"),
    // Pacific/Marquesas    French Polynesia(PF)    -9:30   -   MART    # Marquesas Time
    //----------------------------------------------------------
    new TimeZoneImpl(-9*ONE_HOUR, "Pacific/Gambier"),
    // Pacific/Gambier  French Polynesia(PF)    -9:00   -   GAMT    # Gambier Time
    //----------------------------------------------------------
    new TimeZoneImpl((int)(-8.5*ONE_HOUR), "Pacific/Pitcairn"),
    // Pacific/Pitcairn Pitcairn(PN)    -8:30   -   PNT # Pitcairn Time
    //----------------------------------------------------------
    new TimeZoneImpl(-6*ONE_HOUR, "Pacific/Galapagos"),
    // Pacific/Galapagos    Ecuador(EC) -6:00   -   GALT    # Galapagos Time
    //----------------------------------------------------------
    new TimeZoneImpl(-6*ONE_HOUR, "Pacific/Easter",
                 Calendar.OCTOBER, 9, -Calendar.SUNDAY, 0*ONE_HOUR,
                 Calendar.MARCH, 9, -Calendar.SUNDAY, 0*ONE_HOUR, 1*ONE_HOUR),
    // Rule Chile   1969    max -   Oct Sun>=9  0:00    1:00    S
    // Rule Chile   1970    max -   Mar Sun>=9  0:00    0   -
    // Pacific/Easter   Chile(CL)   -6:00   Chile   EAS%sT
    //----------------------------------------------------------
    new TimeZoneImpl((int)(11.5*ONE_HOUR), "Pacific/Norfolk"),
    // Pacific/Norfolk  Norfolk(NF) 11:30   -   NFT # Norfolk Time
    //----------------------------------------------------------
    new TimeZoneImpl(12*ONE_HOUR, "Pacific/Kosrae"),
    // Pacific/Kosrae   Micronesia(FM)  12:00   -   KOST    # Kosrae Time
    //----------------------------------------------------------
    new TimeZoneImpl(12*ONE_HOUR, "Pacific/Tarawa"),
    // Pacific/Tarawa   Kiribati(KI)    12:00   -   GILT    # Gilbert Is Time
    //----------------------------------------------------------
    new TimeZoneImpl(12*ONE_HOUR, "Pacific/Majuro"),
    // Pacific/Majuro   Marshall Is(MH) 12:00   -   MHT
    //----------------------------------------------------------
    new TimeZoneImpl(12*ONE_HOUR, "Pacific/Nauru"),
    // Pacific/Nauru    Nauru(NR)   12:00   -   NRT
    //----------------------------------------------------------
    new TimeZoneImpl(12*ONE_HOUR, "Pacific/Funafuti"),
    // Pacific/Funafuti Tuvalu(TV)  12:00   -   TVT # Tuvalu Time
    //----------------------------------------------------------
    new TimeZoneImpl(12*ONE_HOUR, "Pacific/Wake"),
    // Pacific/Wake Wake(US)    12:00   -   WAKT    # Wake Time
    //----------------------------------------------------------
    new TimeZoneImpl(12*ONE_HOUR, "Pacific/Wallis"),
    // Pacific/Wallis   Wallis and Futuna(WF)   12:00   -   WFT # Wallis & Futuna Time
    //----------------------------------------------------------
    new TimeZoneImpl(12*ONE_HOUR, "Pacific/Fiji"),
    // Pacific/Fiji Fiji(FJ)    12:00   -   FJT # Fiji Time
    //----------------------------------------------------------
    new TimeZoneImpl(12*ONE_HOUR, "Pacific/Auckland",
                 Calendar.OCTOBER, 1, -Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.MARCH, 15, -Calendar.SUNDAY, 3*ONE_HOUR, 1*ONE_HOUR),
    // Rule NZ  1990    max -   Oct Sun>=1  2:00s   1:00    D
    // Rule NZ  1990    max -   Mar Sun>=15 2:00s   0   S
    // Pacific/Auckland New Zealand(NZ) 12:00   NZ  NZ%sT
    new TimeZoneImpl(12*ONE_HOUR, "NST",
                 Calendar.OCTOBER, 1, -Calendar.SUNDAY, 2*ONE_HOUR,
                 Calendar.MARCH, 15, -Calendar.SUNDAY, 3*ONE_HOUR, 1*ONE_HOUR),
    //----------------------------------------------------------
    new TimeZoneImpl((int)(12.75*ONE_HOUR), "Pacific/Chatham",
                 Calendar.OCTOBER, 1, -Calendar.SUNDAY, (int)(2.75*ONE_HOUR),
                 Calendar.MARCH, 15, -Calendar.SUNDAY, (int)(3.75*ONE_HOUR), 1*ONE_HOUR),
    // Rule Chatham 1990    max -   Oct Sun>=1  2:45s   1:00    D
    // Rule Chatham 1991    max -   Mar Sun>=15 2:45s   0   S
    // Pacific/Chatham  New Zealand(NZ) 12:45   Chatham CHA%sT
    //----------------------------------------------------------
    new TimeZoneImpl(13*ONE_HOUR, "Pacific/Enderbury"),
    // Pacific/Enderbury    Kiribati(KI)    13:00   -   PHOT
    //----------------------------------------------------------
    new TimeZoneImpl(13*ONE_HOUR, "Pacific/Tongatapu"),
    // Pacific/Tongatapu    Tonga(TO)   13:00   -   TOT
    //----------------------------------------------------------
    new TimeZoneImpl(14*ONE_HOUR, "Pacific/Kiritimati"),
    // Pacific/Kiritimati   Kiribati(KI)    14:00   -   LINT
    /************/
    };
}

