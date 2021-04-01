package com.ebsee.j2c;

/**
 * Java Field
 */
public class JField {

    String className;
    String name;
    String javaSignature;

    public JField(String name, String javaSignature) {
        this(null, name, javaSignature);
    }

    public JField(String className, String name, String javaSignature) {
        this.className = className;
        this.name = name;
        this.javaSignature = javaSignature;
    }

    public int hashCode() {
        String s = className + name + javaSignature;
        return s.hashCode();
    }

    @Override
    public boolean equals(Object o) {

        if (o instanceof JField) {
            JField f = (JField) o;
            if (className != null && className.equals(f.className) || className == f.className) {
                if (name != null && name.equals(f.name) || name == f.name) {
                    if (javaSignature != null && javaSignature.equals(f.javaSignature) || javaSignature == f.javaSignature) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
