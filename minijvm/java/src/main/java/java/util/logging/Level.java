/*
 * Copyright (c) 2003, 2006, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.util.logging;


public class Level implements java.io.Serializable {
    private static java.util.ArrayList known = new java.util.ArrayList();
    private static String defaultBundle = "sun.util.logging.resources.logging";


    private final String name;


    private final int value;

    public static final Level OFF = new Level("OFF", Integer.MAX_VALUE, defaultBundle);


    public static final Level SEVERE = new Level("SEVERE", 1000, defaultBundle);


    public static final Level WARNING = new Level("WARNING", 900, defaultBundle);


    public static final Level INFO = new Level("INFO", 800, defaultBundle);


    public static final Level CONFIG = new Level("CONFIG", 700, defaultBundle);


    public static final Level FINE = new Level("FINE", 500, defaultBundle);


    public static final Level FINER = new Level("FINER", 400, defaultBundle);


    public static final Level FINEST = new Level("FINEST", 300, defaultBundle);


    public static final Level ALL = new Level("ALL", Integer.MIN_VALUE, defaultBundle);


    protected Level(String name, int value) {
        this(name, value, null);
    }


    protected Level(String name, int value, String resourceBundleName) {
        this.name = name;
        this.value = value;
        synchronized (Level.class) {
            known.add(this);
        }
    }


    public String getName() {
        return name;
    }


    public String getLocalizedName() {
        return name;
    }


    public final String toString() {
        return name;
    }


    public final int intValue() {
        return value;
    }

    public static synchronized Level parse(String name) throws IllegalArgumentException {

        name.length();


        for (int i = 0; i < known.size(); i++) {
            Level l = (Level) known.get(i);
            if (name.equals(l.name)) {
                return l;
            }
        }


        try {
            int x = Integer.parseInt(name);
            for (int i = 0; i < known.size(); i++) {
                Level l = (Level) known.get(i);
                if (l.value == x) {
                    return l;
                }
            }

            return new Level(name, x);
        } catch (NumberFormatException ex) {


        }


        for (int i = 0; i < known.size(); i++) {
            Level l = (Level) known.get(i);
            if (name.equals(l.getLocalizedName())) {
                return l;
            }
        }


        throw new IllegalArgumentException("Bad level \"" + name + "\"");
    }


    public boolean equals(Object ox) {
        try {
            Level lx = (Level) ox;
            return (lx.value == this.value);
        } catch (Exception ex) {
            return false;
        }
    }


    public int hashCode() {
        return this.value;
    }
}
