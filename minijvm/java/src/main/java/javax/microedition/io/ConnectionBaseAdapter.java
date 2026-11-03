/* SPDX-License-Identifier: MIT */
package javax.microedition.io;

import com.sun.cldc.io.ConnectionBaseInterface;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;

/** Shared lifecycle and stream handling for GCF protocol implementations. */
public abstract class ConnectionBaseAdapter
        implements ConnectionBaseInterface, StreamConnection {
    protected boolean connectionOpen;
    protected int iStreams;
    protected int maxIStreams = 1;
    protected int oStreams;
    protected int maxOStreams = 1;
    protected int requiredPermission = -1;
    protected String protocol;

    private boolean permissionChecked;
    private boolean disconnected;

    public final Connection openPrim(String name, int mode, boolean timeouts)
            throws IOException {
        if (mode != Connector.READ && mode != Connector.WRITE &&
                mode != Connector.READ_WRITE) {
            throw new IllegalArgumentException("Illegal mode: " + mode);
        }
        permissionChecked = false;
        checkForPermission(name);
        verifyPermissionCheck();
        connect(name, mode, timeouts);
        disconnected = false;
        connectionOpen = true;
        return this;
    }

    public final Connection openPrim(String name) throws IOException {
        return openPrim(name, Connector.READ_WRITE, false);
    }

    public void checkForPermission(String name) throws InterruptedIOException {
        permissionChecked = true;
    }

    protected final void verifyPermissionCheck() {
        if (!permissionChecked) {
            throw new SecurityException("The permission check was bypassed");
        }
    }

    public InputStream openInputStream() throws IOException {
        ensureOpen();
        if (iStreams >= maxIStreams) {
            throw new IOException("No more input streams available");
        }
        iStreams++;
        return new AdapterInputStream(this);
    }

    public final DataInputStream openDataInputStream() throws IOException {
        return new DataInputStream(openInputStream());
    }

    public OutputStream openOutputStream() throws IOException {
        ensureOpen();
        if (oStreams >= maxOStreams) {
            throw new IOException("No more output streams available");
        }
        oStreams++;
        return new AdapterOutputStream(this);
    }

    public final DataOutputStream openDataOutputStream() throws IOException {
        return new DataOutputStream(openOutputStream());
    }

    public final void close() throws IOException {
        if (connectionOpen) {
            connectionOpen = false;
            disconnectWhenUnused();
        }
    }

    protected void closeInputStream() throws IOException {
        if (iStreams > 0) iStreams--;
        disconnectWhenUnused();
    }

    protected void closeOutputStream() throws IOException {
        if (oStreams > 0) oStreams--;
        disconnectWhenUnused();
    }

    private void disconnectWhenUnused() throws IOException {
        if (!connectionOpen && iStreams == 0 && oStreams == 0 && !disconnected) {
            disconnected = true;
            disconnect();
        }
    }

    protected final void ensureOpen() throws IOException {
        if (!connectionOpen) {
            throw new IOException("Connection closed");
        }
    }

    protected abstract void connect(String name, int mode, boolean timeouts)
            throws IOException;
    protected abstract void disconnect() throws IOException;
    protected abstract int readBytes(byte[] data, int offset, int length)
            throws IOException;
    protected abstract int writeBytes(byte[] data, int offset, int length)
            throws IOException;

    public int available() throws IOException {
        return 0;
    }

    protected void flush() throws IOException {
    }

    private static final class AdapterInputStream extends InputStream {
        private ConnectionBaseAdapter owner;
        private final byte[] one = new byte[1];

        AdapterInputStream(ConnectionBaseAdapter owner) {
            this.owner = owner;
        }

        public int read() throws IOException {
            int count = read(one, 0, 1);
            return count < 0 ? -1 : one[0] & 0xff;
        }

        public int read(byte[] data, int offset, int length) throws IOException {
            requireOwner();
            checkBounds(data, offset, length);
            if (length == 0) return 0;
            return owner.readBytes(data, offset, length);
        }

        public int available() throws IOException {
            requireOwner();
            return owner.available();
        }

        public void close() throws IOException {
            if (owner != null) {
                ConnectionBaseAdapter current = owner;
                owner = null;
                current.closeInputStream();
            }
        }

        private void requireOwner() throws InterruptedIOException {
            if (owner == null) throw new InterruptedIOException("Stream closed");
        }
    }

    private static final class AdapterOutputStream extends OutputStream {
        private ConnectionBaseAdapter owner;
        private final byte[] one = new byte[1];

        AdapterOutputStream(ConnectionBaseAdapter owner) {
            this.owner = owner;
        }

        public void write(int value) throws IOException {
            one[0] = (byte) value;
            write(one, 0, 1);
        }

        public void write(byte[] data, int offset, int length) throws IOException {
            requireOwner();
            checkBounds(data, offset, length);
            int written = 0;
            while (written < length) {
                int count = owner.writeBytes(data, offset + written, length - written);
                if (count <= 0) {
                    throw new IOException("Protocol made no write progress");
                }
                written += count;
            }
        }

        public void flush() throws IOException {
            requireOwner();
            owner.flush();
        }

        public void close() throws IOException {
            if (owner != null) {
                ConnectionBaseAdapter current = owner;
                owner = null;
                current.closeOutputStream();
            }
        }

        private void requireOwner() throws InterruptedIOException {
            if (owner == null) throw new InterruptedIOException("Stream closed");
        }
    }

    private static void checkBounds(byte[] data, int offset, int length) {
        if (data == null) throw new NullPointerException();
        if (offset < 0 || length < 0 || offset > data.length - length) {
            throw new IndexOutOfBoundsException();
        }
    }
}
