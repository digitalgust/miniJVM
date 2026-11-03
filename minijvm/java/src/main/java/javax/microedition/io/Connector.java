/* SPDX-License-Identifier: MIT */
package javax.microedition.io;

import com.sun.cldc.io.ConnectionBaseInterface;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Generic Connection Framework entry point. */
public final class Connector {
    public static final int READ = 1;
    public static final int WRITE = 2;
    public static final int READ_WRITE = READ | WRITE;

    private static final String protocolRoot = initProtocolRoot();

    private Connector() {
    }

    private static String initProtocolRoot() {
        String value = System.getProperty("javax.microedition.io.Connector.protocolpath");
        return value == null || value.length() == 0 ? "org.mini.net" : value;
    }

    public static Connection open(String name) throws IOException {
        return open(name, READ_WRITE, false);
    }

    public static Connection open(String name, int mode) throws IOException {
        return open(name, mode, false);
    }

    public static Connection open(String name, int mode, boolean timeouts)
            throws IOException {
        if (name == null) {
            throw new IllegalArgumentException("Null URL");
        }
        if (mode != READ && mode != WRITE && mode != READ_WRITE) {
            throw new IllegalArgumentException("Illegal mode: " + mode);
        }
        int colon = name.indexOf(':');
        if (colon <= 0) {
            throw new IllegalArgumentException("URL has no protocol: " + name);
        }
        String protocol = name.substring(0, colon).replace('-', '_').toLowerCase();
        String className = protocolRoot + "." + protocol + ".Protocol";
        try {
            Class type = Class.forName(className);
            Object instance = type.newInstance();
            if (!(instance instanceof ConnectionBaseInterface)) {
                throw new IOException(className + " is not a connection protocol");
            }
            return ((ConnectionBaseInterface) instance).openPrim(
                    name.substring(colon + 1), mode, timeouts);
        } catch (ClassNotFoundException missing) {
            throw new ConnectionNotFoundException(
                    "The requested protocol does not exist: " + protocol);
        } catch (InstantiationException failure) {
            throw new IOException(failure.toString());
        } catch (IllegalAccessException failure) {
            throw new IOException(failure.toString());
        }
    }

    public static DataInputStream openDataInputStream(String name)
            throws IOException {
        InputConnection connection = (InputConnection) open(name, READ);
        try {
            return connection.openDataInputStream();
        } finally {
            connection.close();
        }
    }

    public static DataOutputStream openDataOutputStream(String name)
            throws IOException {
        OutputConnection connection = (OutputConnection) open(name, WRITE);
        try {
            return connection.openDataOutputStream();
        } finally {
            connection.close();
        }
    }

    public static InputStream openInputStream(String name) throws IOException {
        return openDataInputStream(name);
    }

    public static OutputStream openOutputStream(String name) throws IOException {
        return openDataOutputStream(name);
    }
}
