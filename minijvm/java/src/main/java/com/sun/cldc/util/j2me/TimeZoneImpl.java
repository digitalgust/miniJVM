/* SPDX-License-Identifier: MIT */
package com.sun.cldc.util.j2me;

import java.util.TimeZone;

/** Fixed-offset time zones for the CLDC 1.1 TimeZone subset. */
public final class TimeZoneImpl extends TimeZone {
    private static final int HOUR = 60 * 60 * 1000;
    private static final String PROPERTY =
            "com.sun.cldc.util.j2me.TimeZoneImpl.timezone";
    private static final String[] IDS = {
            "GMT", "UTC", "UT", "CTT", "EST", "CST", "MST", "PST"
    };

    private final String id;
    private final int rawOffset;

    public TimeZoneImpl() {
        this("GMT", 0);
    }

    private TimeZoneImpl(String id, int rawOffset) {
        this.id = id;
        this.rawOffset = rawOffset;
    }

    public int getOffset(int era, int year, int month, int day,
                         int dayOfWeek, int millis) {
        if ((era != 0 && era != 1) || month < 0 || month > 11
                || day < 1 || day > 31 || dayOfWeek < 1 || dayOfWeek > 7
                || millis < 0 || millis >= 24 * HOUR) {
            throw new IllegalArgumentException("Invalid date fields");
        }
        return rawOffset;
    }

    public int getRawOffset() {
        return rawOffset;
    }

    public boolean useDaylightTime() {
        return false;
    }

    public String getID() {
        return id;
    }

    public synchronized TimeZone getInstance(String requestedId) {
        if (requestedId == null) {
            requestedId = System.getProperty(PROPERTY);
            if (requestedId == null || requestedId.length() == 0) {
                requestedId = "UTC";
            }
        }
        if ("GMT".equals(requestedId) || "UTC".equals(requestedId)
                || "UT".equals(requestedId)) {
            return new TimeZoneImpl(requestedId, 0);
        }
        if ("CTT".equals(requestedId)) return new TimeZoneImpl("CTT", 8 * HOUR);
        if ("EST".equals(requestedId)) return new TimeZoneImpl("EST", -5 * HOUR);
        if ("CST".equals(requestedId)) return new TimeZoneImpl("CST", -6 * HOUR);
        if ("MST".equals(requestedId)) return new TimeZoneImpl("MST", -7 * HOUR);
        if ("PST".equals(requestedId)) return new TimeZoneImpl("PST", -8 * HOUR);
        return parseCustom(requestedId);
    }

    public synchronized String[] getIDs() {
        String[] copy = new String[IDS.length];
        System.arraycopy(IDS, 0, copy, 0, IDS.length);
        return copy;
    }

    private static TimeZone parseCustom(String value) {
        if (!(value.startsWith("GMT+") || value.startsWith("GMT-")
                || value.startsWith("UTC+") || value.startsWith("UTC-"))) {
            return null;
        }
        int sign = value.charAt(3) == '-' ? -1 : 1;
        String number = value.substring(4);
        int colon = number.indexOf(':');
        int hours;
        int minutes;
        try {
            if (colon >= 0) {
                hours = Integer.parseInt(number.substring(0, colon));
                minutes = Integer.parseInt(number.substring(colon + 1));
            } else if (number.length() > 2) {
                hours = Integer.parseInt(number.substring(0, number.length() - 2));
                minutes = Integer.parseInt(number.substring(number.length() - 2));
            } else {
                hours = Integer.parseInt(number);
                minutes = 0;
            }
        } catch (RuntimeException badOffset) {
            return null;
        }
        if (hours > 23 || minutes > 59 || hours < 0 || minutes < 0) {
            return null;
        }
        return new TimeZoneImpl(value, sign * (hours * HOUR + minutes * 60000));
    }
}
