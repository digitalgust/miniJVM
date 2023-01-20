package test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Foo4 {
    public static void main(String[] args) {
        Pear[] a = {new Pear("A"), new Pear("B")};
        Arrays.sort(a, Comparator.comparing(Pear::getName));

        try {
            Method m = ZipFile.class.getMethod("getInputStream", ZipEntry.class);
            if (m != null) {
                Class[] types = m.getExceptionTypes();
                for (Class c : types) {
                    System.out.println(c.getName());
                }
            }

            Class[] types = ArrayList.class.getInterfaces();
            for (Class c : types) {
                System.out.println(c.getName());
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }
}

class Pear {
    String name;

    Pear(String n) {
        this.name = n;
    }

    String getName() {
        return name;
    }
}
