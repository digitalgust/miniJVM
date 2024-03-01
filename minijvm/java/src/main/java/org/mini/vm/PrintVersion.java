package org.mini.vm;

public class PrintVersion {
    public static void main(String[] args) {

        System.out.println("minijvm version \"" + System.getProperty("java.version") + "\"");
        System.out.println("minijvm SE Runtime Environment (" + System.getProperty("java.version") + "-b01)");
        String arch = RefNative.refIdSize() == 8 ? "64" : "32";
        System.out.println("minijvm " + arch + "-Bit Server VM (build 1.0-b01, mixed mode)");
    }
}
