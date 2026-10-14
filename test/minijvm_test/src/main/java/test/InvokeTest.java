package test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InvokeTest {

    interface SweetTast {
        public int getSweet();
    }

    interface Botany {
        public int getPrice();
    }

    static abstract class Fruit implements Botany, SweetTast {
        public String name;
        public int price;

        public Fruit(String name, int price) {
            this.name = name;
            this.price = price;
        }

        public int getPrice() {
            return price;
        }

        public String getName() {
            return name;
        }
    }

    static class Apple extends Fruit {
        public Apple(String name, int price) {
            super(name, price);
        }

        public int getSweet() {
            return 15;
        }
    }

    static class Orange extends Fruit {
        public Orange(String name, int price) {
            super(name, price);
        }

        public int getSweet() {
            return 10;
        }
    }

    static class Banana extends Fruit {
        public Banana(String name, int price) {
            super(name, price);
        }

        public int getSweet() {
            return 5;
        }
    }

    public static void main(String[] args) {
        Map<String, String> map = new HashMap<>();   //enabled
        String s = map.get("1"); //neednot  (String) myMap.get(key)
        Runnable r = () -> {
            System.out.println(s);
        };  //enabled and executed.
        Runnable r1 = r::run;  //enabled
        r1.run();

        List<Fruit> list = new ArrayList<>();
        list.add(new Apple("apple", 10));
        list.add(new Orange("orange", 20));
        list.add(new Banana("banana", 30));

        long start = System.currentTimeMillis();
        long totalPrice = 0;
        for (int i = 0; i < 10000000; i++) {
            for (int j = 0; j < list.size(); j++) {
                Fruit fruit = list.get(j);
                if (fruit instanceof SweetTast) {
                    totalPrice += fruit.getPrice() * ((SweetTast) fruit).getSweet();
                } else {
                    totalPrice += fruit.getPrice();
                }
            }
        }

        System.out.println("cost:" + (System.currentTimeMillis() - start));
    }
}
