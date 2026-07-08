package java.util.zip;

import org.mini.zip.Zip;

/**
 * A minimal Inflater implementation for M3G decoding on miniJVM.
 * It uses the existing org.mini.zip.Zip.zlibExtract method.
 */
public class Inflater {
    private byte[] input;
    private byte[] uncompressed;
    private int uncompressedPos;
    private boolean finished;

    public Inflater() {
    }

    public void setInput(byte[] b) {
        setInput(b, 0, b.length);
    }

    public void setInput(byte[] b, int off, int len) {
        if (off == 0 && len == b.length) {
            this.input = b;
        } else {
            this.input = new byte[len];
            System.arraycopy(b, off, this.input, 0, len);
        }
        this.uncompressed = null;
        this.uncompressedPos = 0;
        this.finished = false;
    }

    public int inflate(byte[] b) throws DataFormatException {
        return inflate(b, 0, b.length);
    }

    public int inflate(byte[] b, int off, int len) throws DataFormatException {
        if (input == null) {
            return 0;
        }
        
        if (uncompressed == null) {
            // For M3G usage, the first inflate call usually passes the expected length.
            // We use this length to decompress the entire buffer at once using minijvm's native Zip tool.
            try {
                uncompressed = Zip.zlibExtract(input, len);
                if (uncompressed == null) {
                    throw new DataFormatException("zlibExtract returned null");
                }
            } catch (Exception e) {
                throw new DataFormatException(e.getMessage());
            }
        }
        
        int available = uncompressed.length - uncompressedPos;
        int count = Math.min(len, available);
        if (count <= 0) {
            finished = true;
            return 0;
        }
        
        System.arraycopy(uncompressed, uncompressedPos, b, off, count);
        uncompressedPos += count;
        
        if (uncompressedPos >= uncompressed.length) {
            finished = true;
        }
        
        return count;
    }

    public boolean finished() {
        return finished;
    }

    public void end() {
        input = null;
        uncompressed = null;
    }
}
