/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
 * @see     java.util.TimeZone
 * @see     java.util.Calendar
 * @version CLDC 1.1 03/13/2002 (Based on JDK 1.3)
 * @author  James Gosling, Roger Riggs, Brian Modra
 *          Kinsley Wong, Antero Taivalsaari
 */

public class Date {

    /* If calendar is null, then fastTime indicates the time in millis.
     * Otherwise, fastTime is ignored, and calendar indicates the time.
     */
    private Calendar calendar;
    private long     fastTime;

    /**
     * Allocates a <code>Date</code> object and initializes it to
     * represent the current time specified number of milliseconds since the
     * standard base time known as "the epoch", namely January 1,
     * 1970, 00:00:00 GMT.
     * @see     java.lang.System#currentTimeMillis()
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
     * @param   date   the milliseconds since January 1, 1970, 00:00:00 GMT.
     * @see     java.lang.System#currentTimeMillis()
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
     * @return  the number of milliseconds since January 1, 1970, 00:00:00 GMT
     *          represented by this date.
     *
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
     * @param   time   the number of milliseconds.
     *
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
     * @param   obj   the object to compare with.
     * @return  <code>true</code> if the objects are the same;
     *          <code>false</code> otherwise.
     * @see     java.util.Date#getTime()
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
     * @return  a hash code value for this object.
     */
    public int hashCode() {
        long ht = getTime();
        return (int)ht ^ (int)(ht >> 32);
    }

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
     * @since CLDC 1.1
     */
     public String toString() {
         return com.sun.cldc.util.mini.CalendarImpl.toString(calendar);
     }
}

