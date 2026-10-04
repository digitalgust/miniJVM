/* Copyright (c) 2008-2015, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */

package java.util.jar;

import java.util.HashMap;

public class Attributes {
    private final HashMap<String, String> values = new HashMap<String, String>();

    public String getValue(String name) {
        if (name == null) return null;
        return values.get(normalize(name));
    }

    public String getValue(Name name) {
        return name == null ? null : getValue(name.toString());
    }

    public String putValue(String name, String value) {
        if (name == null || value == null) throw new NullPointerException();
        return values.put(normalize(name), value);
    }

    public int size() {
        return values.size();
    }

    public void clear() {
        values.clear();
    }

    private static String normalize(String name) {
        return name.toLowerCase();
    }

    public static class Name {
        private final String name;

        private static final int MAX_NAME_LENGTH = 70;

        public Name(String s) {
            int len = s.length();
            if (len == 0 || len > MAX_NAME_LENGTH)
                throw new IllegalArgumentException();

            name = s;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Name && name.equalsIgnoreCase(((Name) other).name);
        }

        @Override
        public int hashCode() {
            return name.toLowerCase().hashCode();
        }

        @Override
        public String toString() {
            return name;
        }
    }
}