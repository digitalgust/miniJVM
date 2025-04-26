package org.mini.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class SysLog {
    static Level level = Level.ALL;
    static Logger logger = Logger.getLogger(SysLog.class.getName());

    static {
        try {
            logger.setLevel(level);
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(level);
            logger.addHandler(consoleHandler);
            consoleHandler.setFormatter(new MyFormatter()); // 默认格式
//
//            // 自定义格式
//            consoleHandler.setFormatter(new Formatter() {
//                @Override
//                public String format(LogRecord record) {
//                    return record.getLevel() + ": " + record.getMessage() + "\n";
//                }
//            });
//
//            FileHandler fileHandler = new FileHandler("app.log", true);
//            fileHandler.setLevel(Level.WARNING);
//            logger.addHandler(fileHandler);
//
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error configuring handlers", e);
        }
    }


    static class MyFormatter extends Formatter {

        Date dat = new Date();
        private final static String format = "{0} {1}";
        SimpleDateFormat dateFormat = new SimpleDateFormat("yy-MM-dd");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        private MessageFormat formatter;

        private Object args[] = new Object[2];


        private String lineSeparator = System.getProperty("line.separator");
        private String fieldSeparator = "|";


        public synchronized String format(LogRecord record) {
            StringBuilder sb = new StringBuilder();

            dat.setTime(record.getMillis());
            args[0] = dateFormat.format(dat);
            args[1] = timeFormat.format(dat);
            StringBuffer text = new StringBuffer();
            if (formatter == null) {
                formatter = new MessageFormat(format);
            }
            formatter.format(args, text, null);
            sb.append(text);
            sb.append(fieldSeparator);
//            if (record.getSourceClassName() != null) {
//                sb.append(record.getSourceClassName());
//            } else {
//                sb.append(record.getLoggerName());
//            }
//            if (record.getSourceMethodName() != null) {
//                sb.append(" ");
//                sb.append(record.getSourceMethodName());
//            }
//            sb.append(fieldSeparator);
            String message = formatMessage(record);
            sb.append(record.getLevel().getLocalizedName());
            sb.append(fieldSeparator);
            sb.append(message);
            if (record.getThrown() != null) {
                sb.append(lineSeparator);
                try {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    record.getThrown().printStackTrace(pw);
                    pw.close();
                    sb.append(sw.toString());
                } catch (Exception ex) {
                }
            }
            sb.append(lineSeparator);
            return sb.toString();
        }
    }


    public static Logger getLogger() {
        return logger;
    }

    public static void setLevel(Level level) {
        SysLog.level = level;
        logger.setLevel(level);
    }

    public static void log(Level level, String msg) {
        logger.log(level, msg);
    }

    public static void log(Level level, String msg, Throwable ex) {
        logger.log(level, msg, ex);
    }

    public static void log(Level level, String msg, Object... params) {
        logger.log(level, msg, params);
    }

    public static void fine(String msg, Object... params) {
        logger.log(Level.FINE, msg, params);
    }

    public static void info(String msg, Object... params) {
        logger.log(Level.INFO, msg, params);
    }

    public static void warn(String msg, Object... params) {
        logger.log(Level.WARNING, msg, params);
    }

    public static void error(String msg, Throwable thr) {
        logger.log(Level.SEVERE, msg, thr);
    }

    public static void error(String msg) {
        logger.log(Level.SEVERE, msg);
    }

}
