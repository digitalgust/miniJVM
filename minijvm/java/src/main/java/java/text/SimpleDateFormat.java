/* Copyright (c) 2008-2015, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */

package java.text;

import com.sun.cldc.util.j2me.TimeZoneImpl;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleDateFormat {
    static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss";

    static final String[] MONTHS = {
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };
    static final String[] DAYS = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
    static Pattern yyyyP = Pattern.compile("yyyy+");
    static Pattern yyyP = Pattern.compile("yyy+");
    static Pattern yP = Pattern.compile("y+");
    static Pattern MMMP = Pattern.compile("MMM+");
    static Pattern MP = Pattern.compile("M+");
    static Pattern dP = Pattern.compile("d+");
    static Pattern HP = Pattern.compile("H+");
    static Pattern mP = Pattern.compile("m+");
    static Pattern sP = Pattern.compile("s+");
    static Pattern SP = Pattern.compile("S+");
    static Pattern kP = Pattern.compile("k+");
    static Pattern KP = Pattern.compile("K+");
    static Pattern hP = Pattern.compile("h+");
    static Pattern aP = Pattern.compile("a+");
    static Pattern zzzzP = Pattern.compile("zzzz+");
    static Pattern zP = Pattern.compile("z+");
    static Pattern ZP = Pattern.compile("Z+");
    static Pattern GP = Pattern.compile("G+");
    static Pattern FP = Pattern.compile("F+");
    static Pattern DP = Pattern.compile("D+");
    static Pattern EEEP = Pattern.compile("EEE+");
    static Pattern EP = Pattern.compile("E+");
    static final String regex = "G+|y+|M+|w+|W+|D+|d+|F+|E+|a+|H+|k+|K+|h+|m+|s+|S+|z+|Z+|''|'(([^']*(?:'')[^']*)*[^']*)'|(?![GyMwWDdFEaHkKhmsSzZ']+)(.+?)";

    Pattern pattern;
    Matcher matcher;

    List<String> fields = new ArrayList<String>();

    public SimpleDateFormat() {
        this(DEFAULT_PATTERN);
    }

    public SimpleDateFormat(String input) {
        pattern = Pattern.compile(regex);
        matcher = pattern.matcher(input);

        int index = 0;
        String tc = matcher.target();
        while (matcher.find()) {
            int start = matcher.start();
            index = matcher.end();
            String s = tc.substring(start, index);
            fields.add(s);
        }
    }

    public void setTimeZone(TimeZone tz) {
        if (!tz.getDisplayName().equals("GMT")) {
            throw new UnsupportedOperationException();
        }
    }

    public final String format(Date date) {
        return format(date, new StringBuffer(),
                null).toString();
    }

    public StringBuffer format(Date date, StringBuffer buffer, FieldPosition position) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int millisecond = calendar.get(Calendar.MILLISECOND);

        // int weekOfMonth = calendar.get(Calendar.WEEK_OF_YEAR);//F
        // int weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR);//w
        // int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);//D
        int dayOfWeekOfDay = calendar.get(Calendar.DAY_OF_WEEK);// E

        int offsetHour = calendar.getTimeZone().getRawOffset() / 3600 / 1000;

        for (int i = 0; i < fields.size(); i++) {
            String field = fields.get(i);
            if (yyyyP.matches(field)) {
                buffer.append(String.format("%04d", year % 10000));
            } else if (yyyP.matches(field)) {
                buffer.append(String.format("%03d", year % 1000));
            } else if (yP.matches(field)) {
                buffer.append(String.format("%02d", year % 100));
            } else if (MMMP.matches(field)) {
                buffer.append(MONTHS[month]);
            } else if (MP.matches(field)) {
                buffer.append(String.format("%02d", month + 1));
            } else if (dP.matches(field)) {
                buffer.append(String.format("%02d", day));
            } else if (HP.matches(field)) {
                buffer.append(String.format("%02d", hour));
            } else if (mP.matches(field)) {
                buffer.append(String.format("%02d", minute));
            } else if (sP.matches(field)) {
                buffer.append(String.format("%02d", second));
            } else if (SP.matches(field)) {
                buffer.append(String.format("%03d", millisecond));
            } else if (kP.matches(field)) {
                buffer.append(String.format("%02d", hour + 1));
            } else if (KP.matches(field)) {
                buffer.append(String.format("%02d", (hour % 12)));
            } else if (hP.matches(field)) {
                buffer.append(String.format("%02d", (hour % 12) + 1));
            } else if (aP.matches(field)) {
                buffer.append(String.format("%s", (hour < 12) ? "AM" : "PM"));
            } else if (zzzzP.matches(field) || zP.matches(field)) {
                buffer.append(String.format("%s", calendar.getTimeZone().getDisplayName()));
            } else if (ZP.matches(field)) {
                buffer.append(String.format("%+03d00", offsetHour));
            } else if (GP.matches(field)) {
                buffer.append(String.format("%s", "AD"));
            } else if (FP.matches(field)) {
                buffer.append(String.format("%s", " "));
            } else if (DP.matches(field)) {
                buffer.append(String.format("%s", " "));
            } else if (EEEP.matches(field)) {
                buffer.append(DAYS[dayOfWeekOfDay - 1]);
            } else if (EP.matches(field)) {
                buffer.append(String.format("%d", dayOfWeekOfDay));
            } else if (field.equals("''")) {
                buffer.append("'");
            } else {
                String f = field;
                if (f.startsWith("'")) {
                    f = field.substring(1, field.length() - 1);
                }
                f = f.replace("''", "'");
                buffer.append(f);
            }
        }
        return buffer;
    }

    public Date parse(String text) {
        return parse(text, new ParsePosition(0));
    }

    public Date parse(String text, ParsePosition position) {
        int index = position.getIndex();
        try {

            Calendar calendar = Calendar.getInstance();
            for (String field : fields) {
                if (yyyyP.matches(field)) {
                    index = parseField(text, index, 4, calendar, Calendar.YEAR, 0);
                } else if (yyyP.matches(field)) {
                    index = parseField(text, index, 3, calendar, Calendar.YEAR, 0);
                } else if (yP.matches(field)) {
                    index = parseField(text, index, 2, calendar, Calendar.YEAR, 0);
                } else if (MMMP.matches(field)) {
                    index = parseMonthAbbreviation(text, index, calendar);
                } else if (MP.matches(field)) {
                    index = parseField(text, index, field.length(), calendar, Calendar.MONTH, -1);
                } else if (dP.matches(field)) {
                    index = parseField(text, index, field.length(), calendar, Calendar.DAY_OF_MONTH, 0);
                } else if (HP.matches(field)) {
                    index = parseField(text, index, field.length(), calendar, Calendar.HOUR_OF_DAY, 0);
                } else if (mP.matches(field)) {
                    index = parseField(text, index, field.length(), calendar, Calendar.MINUTE, 0);
                } else if (sP.matches(field)) {
                    index = parseField(text, index, field.length(), calendar, Calendar.SECOND, 0);
                } else if (SP.matches(field)) {
                    index = parseField(text, index, field.length(), calendar, Calendar.MILLISECOND, 0);
                } else if (kP.matches(field)) {
                    index = parseField(text, index, field.length(), calendar, Calendar.HOUR_OF_DAY, -1);
                } else if (KP.matches(field)) {
                    index = parseField(text, index, field.length(), calendar, Calendar.HOUR, 0);
                } else if (hP.matches(field)) {
                    index = parseField(text, index, field.length(), calendar, Calendar.HOUR, -1);
                } else if (aP.matches(field)) {
                    index = parseAmPm(text, index, calendar);
                } else if (zzzzP.matches(field)) {
                    index = parseTimeZone(text, index, calendar);
                } else if (zP.matches(field)) {
                    index = parseTimeZone(text, index, calendar);
                } else if (ZP.matches(field)) {
                    index = parseTimeZoneOffset(text, index, calendar);
                } else if (GP.matches(field)) {
                    // 暂时忽略纪元部分，后续可根据需求完善
                    index += field.length();
                } else if (FP.matches(field)) {
                    // 暂时忽略周几在月中的位置部分，后续可根据需求完善
                    index += field.length();
                } else if (DP.matches(field)) {
                    // 暂时忽略一年中的第几天部分，后续可根据需求完善
                    index += field.length();
                } else if (EEEP.matches(field)) {
                    // 暂时忽略星期缩写部分，后续可根据需求完善
                    index += field.length();
                } else if (EP.matches(field)) {
                    // 暂时忽略星期部分，后续可根据需求完善
                    index += field.length();
                } else {
                    index = expectPrefix(text, index, field);
                }
            }
            position.setIndex(index);
            return calendar.getTime();
        } catch (ParseException e) {
            position.setErrorIndex(index);
            return null;
        }
    }

    private int parseMonthAbbreviation(String text, int offset, Calendar calendar) throws ParseException {
        for (int i = 0; i < MONTHS.length; i++) {
            if (text.startsWith(MONTHS[i], offset)) {
                calendar.set(Calendar.MONTH, i);
                return offset + MONTHS[i].length();
            }
        }
        throw new ParseException("Invalid month abbreviation: " + text.substring(offset), offset);
    }

    private int parseAmPm(String text, int offset, Calendar calendar) throws ParseException {
        if (text.startsWith("AM", offset)) {
            calendar.set(Calendar.AM_PM, Calendar.AM);
            return offset + 2;
        } else if (text.startsWith("PM", offset)) {
            calendar.set(Calendar.AM_PM, Calendar.PM);
            return offset + 2;
        }
        throw new ParseException("Invalid AM/PM indicator: " + text.substring(offset), offset);
    }

    private int parseTimeZone(String text, int offset, Calendar calendar) throws ParseException {
        String tz = text.substring(offset).toLowerCase();

        String[] ids = TimeZone.getAvailableIDs();
        for (String id : ids) {
            String lowid = id.toLowerCase();
            if (tz.startsWith(lowid)) {
                calendar.setTimeZone(TimeZone.getTimeZone(id));
                return offset + lowid.length();
            }
        }
        throw new ParseException("Invalid time zone: " + text.substring(offset), offset);
    }

    private int parseTimeZoneOffset(String text, int offset, Calendar calendar) throws ParseException {
        // 计算时区偏移量 格式为 “+0800”
        int rawOffset = 0;
        if (text.charAt(offset) == '+') {
            rawOffset = Integer.parseInt(text.substring(offset + 1, offset + 3)) * 3600000;
            rawOffset += Integer.parseInt(text.substring(offset + 3, offset + 5)) * 60000;
        } else if (text.charAt(offset) == '-') {
            rawOffset = Integer.parseInt(text.substring(offset + 1, offset + 3)) * 3600000;
            rawOffset -= Integer.parseInt(text.substring(offset + 3, offset + 5)) * 60000;
        } else {
            throw new ParseException("Invalid time zone offset: " + text.substring(offset), offset);
        }
        TimeZone timeZone = calendar.getTimeZone();
        String[] ids = TimeZone.getAvailableIDs();
        for (String id : ids) {
            TimeZone zone = TimeZone.getTimeZone(id);
            if (zone.getRawOffset() == rawOffset) {
                timeZone = zone;
                break;
            }
        }
        calendar.setTimeZone(timeZone);
        return offset + 5;
    }

    private static void pad(StringBuffer buffer, int value, int digits) {
        int i = value == 0 ? 1 : value;
        while (i > 0) {
            i /= 10;
            --digits;
        }
        while (digits-- > 0) {
            buffer.append('0');
        }
        buffer.append(value);
    }

    private static int parseField(String text, int offset, int length, Calendar calendar, int field, int adjustment)
            throws ParseException {
        if (text.length() < offset + length)
            throw new ParseException("Short date: " + text, offset);
        try {
            int value = Integer.parseInt(text.substring(offset, offset + length), 10);
            calendar.set(field, value + adjustment);
        } catch (NumberFormatException e) {
            throw new ParseException("Not a number: " + text, offset);
        }
        return offset + length;
    }

    private static int expectPrefix(String text, int offset, String prefix) throws ParseException {
        if (text.length() <= offset)
            throw new ParseException("Short date: " + text, offset);
        if (!text.substring(offset).startsWith(prefix))
            throw new ParseException("Parse error: " + text, offset);
        return offset + prefix.length();
    }
}
