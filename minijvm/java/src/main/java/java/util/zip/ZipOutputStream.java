/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools and Templates, and edit the template in the editor.
 */
package java.util.zip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import org.mini.zip.Zip;

/**
 * ZIP archive writer, JDK-compatible subset. Entry data is buffered, so each
 * local file header carries the real sizes (no data descriptors), and the
 * central directory plus EOCD are written by {@link #finish()}.
 *
 * <p>
 * Deflate is delegated to the native zlib through {@link Zip#zlibCompress0(byte[])} (raw
 * deflate = zlib output minus the 2-byte header and the 4-byte adler32 trailer). Entry
 * names and comments are always encoded as UTF-8; the EFS flag bit is set.
 * </p>
 */
public class ZipOutputStream extends OutputStream {

    public static final int STORED  = ZipEntry.STORED;
    public static final int DEFLATED = ZipEntry.DEFLATED;

    private static final int LOCAL_FILE_HEADER_SIG  = 0x04034b50;
    private static final int CENTRAL_HEADER_SIG     = 0x02014b50;
    private static final int EOCD_SIG               = 0x06054b50;

    private final OutputStream out;
    private int  method = DEFLATED;
    private int  level  = -1;                       // accepted, deflate is one-shot per entry

    // { snapshot, name, crc, size, compressedSize, method, flags, offset, extra, comment }
    private final List<Object[]> entries = new ArrayList<Object[]>();
    private final Set<String> names = new HashSet<String>();
    private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private CRC32                 crc    = new CRC32();
    private ZipEntry              current;
    private ZipEntry              original;
    private long                  written = 0;
    private boolean               finished, closed;

    public ZipOutputStream(OutputStream out) {
        if (out == null) throw new NullPointerException();
        this.out = out;
    }

    public void setLevel(int level) {
        if (level < -1 || level > 9) throw new IllegalArgumentException("invalid compression level: " + level);
        this.level = level;  // -1 = DEFAULT_COMPRESSION; zlib uses its own default
    }

    public void setMethod(int method) {
        if (method != STORED && method != DEFLATED) throw new IllegalArgumentException("invalid method: " + method);
        this.method = method;
    }

    public void putNextEntry(ZipEntry e) throws IOException {
        ensureOpen();
        if (current != null) closeEntry();
        if (e == null) throw new NullPointerException();
        if (e.getName().getBytes("UTF-8").length > 65535) throw new ZipException("entry name too long");
        if (names.contains(e.getName())) throw new ZipException("duplicate entry: " + e.getName());
        if (entries.size() >= 65534) throw new ZipException("ZIP64 entry count not supported");
        int m = e.getMethod() >= 0 ? e.getMethod() : this.method;
        if (m == STORED) {
            if (e.getSize() < 0 && e.getCompressedSize() >= 0) e.setSize(e.getCompressedSize());
            if (e.getCompressedSize() < 0) e.setCompressedSize(e.getSize());
            if (e.getSize() < 0 || e.getCrc() < 0) throw new ZipException("STORED entry missing size/crc: " + e.getName());
            if (e.getSize() != e.getCompressedSize()) throw new ZipException("STORED entry sizes differ");
        }
        if (e.getTime() == -1) e.setTime(System.currentTimeMillis());
        e.setMethod(m);
        current = new ZipEntry(e);
        original = e;
        names.add(e.getName());
        buffer  = new ByteArrayOutputStream();
        crc.reset();
    }

    public void closeEntry() throws IOException {
        ensureOpen();
        if (current == null) return;

        byte[] data    = buffer.toByteArray();
        int    m       = current.getMethod();
        long   crcVal  = crc.getValue();
        long   size    = data.length;
        byte[] cdata;
        if (m == DEFLATED) {
            cdata = rawDeflate(data);
        } else {
            if (current.getSize() >= 0 && current.getSize() != size) {
                throw new ZipException("STORED entry size mismatch for " + current.getName());
            }
            cdata = data;
        }
        int csize = cdata.length;
        if (current.getCrc() >= 0 && current.getCrc() != crcVal) throw new ZipException("entry CRC mismatch: " + current.getName());
        if (current.getSize() >= 0 && current.getSize() != size) throw new ZipException("entry size mismatch: " + current.getName());
        if (current.getCompressedSize() >= 0 && current.getCompressedSize() != csize) throw new ZipException("compressed size mismatch: " + current.getName());
        byte[] extra = current.getExtra() == null ? new byte[0] : current.getExtra();
        byte[] comment = current.getComment() == null ? new byte[0] : current.getComment().getBytes("UTF-8");
        if (comment.length > 65535) throw new ZipException("entry comment too long");

        byte[] name;
        try {
            name = current.getName().getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException uee) {
            throw new IOException("UTF-8 unavailable");
        }
        int flags = 0x0800;
        if (written + 30L + name.length + extra.length + csize >= 0xffffffffL) throw new ZipException("ZIP64 offsets not supported");

        // Local file header (sizes known - no data descriptor).
        writeInt(LOCAL_FILE_HEADER_SIG);
        writeShort(20);                       // version needed
        writeShort(flags);
        writeShort(m);
        writeInt(dosTime(current.getTime()));
        writeInt((int) crcVal);
        writeInt(csize);
        writeInt((int) size);
        writeShort(name.length);
        writeShort(extra.length);
        out.write(name);
        out.write(extra);

        out.write(cdata);

        current.setSize(size);
        current.setCompressedSize(csize);
        current.setCrc(crcVal);
        original.setSize(size);
        original.setCompressedSize(csize);
        original.setCrc(crcVal);
        entries.add(new Object[] { current, name, crcVal, size, csize, m, flags, written, extra, comment });
        written += 30L + name.length + extra.length + csize;

        current = null;
        original = null;
        buffer  = null;
    }

    @Override
    public void write(int b) throws IOException {
        ensureOpen();
        if (current == null) throw new ZipException("no current ZIP entry");
        buffer.write(b);
        crc.update(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        ensureOpen();
        if (current == null) throw new ZipException("no current ZIP entry");
        buffer.write(b, off, len);
        crc.update(b, off, len);
    }

    public void finish() throws IOException {
        ensureOpen();
        if (finished) return;
        if (current != null) closeEntry();

        long cdStart = written;
        int  n = entries.size();
        long directorySize = 0;
        for (Object[] e : entries) directorySize += 46L + ((byte[]) e[1]).length + ((byte[]) e[8]).length + ((byte[]) e[9]).length;
        if (directorySize >= 0xffffffffL || cdStart + directorySize >= 0xffffffffL) throw new ZipException("ZIP64 directory not supported");
        for (Object[] e : entries) {
            ZipEntry ze  = (ZipEntry) e[0];
            byte[]  name = (byte[]) e[1];
            byte[] extra = (byte[]) e[8], comment = (byte[]) e[9];

            writeInt(CENTRAL_HEADER_SIG);
            writeShort(20);                   // version made by
            writeShort(20);                   // version needed
            writeShort(((Integer) e[6]).intValue()); // flags
            writeShort(((Integer) e[5]).intValue()); // method
            writeInt(dosTime(ze.getTime()));
            writeInt((int) ((Long) e[2]).longValue());           // crc
            writeInt(((Integer) e[4]).intValue());               // csize
            writeInt((int) ((Long) e[3]).longValue());           // size
            writeShort(name.length);
            writeShort(extra.length);
            writeShort(comment.length);
            writeShort(0);                    // disk number
            writeShort(0);                    // internal attrs
            writeInt(ze.isDirectory() ? 0x10 : 0); // external attrs
            writeInt((int) ((Long) e[7]).longValue());           // local header offset
            out.write(name);
            out.write(extra);
            out.write(comment);
            written += 46L + name.length + extra.length + comment.length;
        }
        long cdSize = written - cdStart;

        // EOCD.
        writeInt(EOCD_SIG);
        writeShort(0);                        // disk number
        writeShort(0);                        // disk with cd
        writeShort(n);
        writeShort(n);
        writeInt((int) cdSize);
        writeInt((int) cdStart);
        writeShort(0);                        // comment length

        finished = true;
    }

    @Override
    public void close() throws IOException {
        if (closed) return;
        try {
            finish();
        } finally {
            closed = true;
            out.close();
        }
    }

    @Override
    public void flush() throws IOException {
        ensureOpen();
        out.flush();
    }

    private void ensureOpen() throws IOException {
        if (closed) throw new IOException("stream closed");
    }

    /** zlib container -> raw deflate stream. Empty input has a fixed 2-byte final-block encoding. */
    private static byte[] rawDeflate(byte[] data) throws IOException {
        if (data.length == 0) return new byte[] { 0x03, 0x00 };
        byte[] z = Zip.zlibCompress0(data);
        if (z == null || z.length < 6) throw new IOException("zlib compression failed");
        byte[] raw = new byte[z.length - 6];
        System.arraycopy(z, 2, raw, 0, raw.length);
        return raw;
    }

    /** DOS date/time (2 words packed into one int), clamped to the DOS date range. */
    private static int
    dosTime(long millis) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(millis);
        if (c.get(java.util.Calendar.YEAR) < 1980) return (1 << 21) | (1 << 16);
        int year  = Math.min(2107, c.get(java.util.Calendar.YEAR));
        int date  = ((year - 1980) << 9) | ((c.get(java.util.Calendar.MONTH) + 1) << 5) | c.get(java.util.Calendar.DAY_OF_MONTH);
        int time  = (c.get(java.util.Calendar.HOUR_OF_DAY) << 11) | (c.get(java.util.Calendar.MINUTE) << 5) | (c.get(java.util.Calendar.SECOND) >> 1);
        return (date << 16) | time;
    }

    private void
    writeShort(int v) throws IOException {
        out.write(v & 0xFF);
        out.write((v >>> 8) & 0xFF);
    }

    private void
    writeInt(int v) throws IOException {
        out.write(v & 0xFF);
        out.write((v >>> 8) & 0xFF);
        out.write((v >>> 16) & 0xFF);
        out.write((v >>> 24) & 0xFF);
    }
}
