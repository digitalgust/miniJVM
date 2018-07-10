package test;

import java.util.HashMap;
import java.util.Map;

class Hello {

    public static void main(String[] args) {
        Map<Long, String> map = new HashMap();
        map.put(5L, "a");
        String s=map.get(5L);
    }

}
