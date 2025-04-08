import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test {


    static char[] chars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    static public String append(long i) {
        if (i == 0) return "0";
        boolean ne = i < 0;
        if (ne) i = -i;
        String s = "";
        while (i != 0) {
            int v = (int) (i % 10);
            s = chars[v] + s;
            i /= 10;
        }
        if (ne) {
            s = '-' + s;
        }
        return s;
    }

    static public void main(String[] args) {
        System.out.println(append(0L));

        System.out.println(String.format("%2d", 1989 % 100));
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MM M G m SS SSS s  ''ZZZZ''  ''Z'' ''zzz''  ''z''");
        System.out.println(sdf.format(System.currentTimeMillis()));

        t1();
        t2();
        t3();
    }


    public static void t1() {
//        String input = "yyyy mm GG 'exam''ple' '' 'another example' y m G";
//        String regex = "'y+'|m+|G+|''|'([^']*)'";
//        String input = "yyyymm GG 'example' '' 'another '' example' y m G";
        String input = "hh 'o''clock' a, ''zzzz''  ''z''";
        String regex = "G+|y+|M+|w+|W+|D+|d+|F+|E+|a+|H+|k+|K+|h+|m+|s+|S+|z+|Z+|''|'(([^']*(?:'')[^']*)*[^']*)'";


        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            System.out.println("Found: " + matcher.group());
        }

        System.out.println(String.format("%+03d", 8));
    }

    static class TestInner {
    }

    public static void t2() {
        Logger logger = Logger.getLogger(Test.class.getName());
        try {
            logger.setLevel(Level.ALL);
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.ALL);
            logger.addHandler(consoleHandler);
            consoleHandler.setFormatter(new SimpleFormatter()); // 默认格式

            // 自定义格式
            consoleHandler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    return record.getLevel() + ": " + record.getMessage() + "\n";
                }
            });

            FileHandler fileHandler = new FileHandler("app.log", true);
            fileHandler.setLevel(Level.WARNING);
            logger.addHandler(fileHandler);

            Logger.getLogger(Test.class.getName()).severe("This is a severe message1");
            Logger.getLogger(TestInner.class.getName()).severe("This is a severe message2");
            logger.severe("This is a severe message3");
            logger.warning("This is a warning message");
            logger.info("This is an info message");
            logger.config("This is a config message");
            logger.fine("This is a fine message");
            logger.finer("This is a finer message");
            logger.finest("This is a finest message");

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSzzz");
            Date d = dateFormat.parse("2023-09-01 12:34:56.389CST");
            logger.log(Level.INFO, "Date: " + dateFormat.format(d));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error configuring handlers", e);
        }
    }

    static void t3() {

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 2; i++) {
                    System.out.println("Hello World");
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }
}
