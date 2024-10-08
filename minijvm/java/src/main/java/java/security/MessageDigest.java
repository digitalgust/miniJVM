package java.security;


import org.mini.crypt.MD5;
import org.mini.crypt.SHA1;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;

public abstract class MessageDigest {


    protected String algorithm;

    // The state of this digest
    protected static final int INITIAL = 0;
    protected static final int IN_PROGRESS = 1;
    protected int state = INITIAL;

    protected Provider provider;


    protected MessageDigest(String algorithm) {
        this.algorithm = algorithm;
    }

    public static MessageDigest getInstance(String algorithm)
            throws NoSuchAlgorithmException {
        try {
            MessageDigest md = null;
            switch (algorithm) {
                case "MD5":
                    md = new MD5();
                    break;
                case "SHA-1":
                    md = new SHA1();
                    break;
                default:
                    throw new NoSuchAlgorithmException(algorithm + " not found");
            }

            return md;

        } catch (Exception e) {
            throw new NoSuchAlgorithmException(algorithm + " not found");
        }
    }

    public static MessageDigest getInstance(String algorithm, String provider)
            throws NoSuchAlgorithmException, NoSuchProviderException {
        return getInstance(algorithm);
    }

    public static MessageDigest getInstance(String algorithm,
                                            Provider provider)
            throws NoSuchAlgorithmException {
        return null;
    }

    public final Provider getProvider() {
        return this.provider;
    }

    abstract public void update(byte input);

    abstract public void update(byte[] input, int offset, int len);

    abstract public void update(byte[] input);

    public final void update(ByteBuffer input) {
        if (input == null) {
            throw new NullPointerException();
        }
        state = IN_PROGRESS;
        byte[] b = new byte[input.remaining()];
        input.get(b);
        update(b);
    }

    abstract public byte[] digest();

    abstract public int digest(byte[] buf, int offset, int len) throws DigestException;

    abstract public byte[] digest(byte[] input);

    private String getProviderName() {
        return (provider == null) ? "(no provider)" : provider.getName();
    }

    public String toString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream p = new PrintStream(baos);
        p.print(algorithm + " Message Digest from " + getProviderName() + ", ");
        switch (state) {
            case INITIAL:
                p.print("<initialized>");
                break;
            case IN_PROGRESS:
                p.print("<in progress>");
                break;
        }
        p.println();
        return (baos.toString());
    }

    /**
     * Compares two digests for equality. Does a simple byte compare.
     *
     * @param digesta one of the digests to compare.
     * @param digestb the other digest to compare.
     * @return true if the digests are equal, false otherwise.
     */
    public static boolean isEqual(byte[] digesta, byte[] digestb) {
        /* All bytes in digesta are examined to determine equality.
         * The calculation time depends only on the length of digesta
         * It does not depend on the length of digestb or the contents
         * of digesta and digestb.
         */
        if (digesta == digestb) return true;
        if (digesta == null || digestb == null) {
            return false;
        }

        int lenA = digesta.length;
        int lenB = digestb.length;

        if (lenB == 0) {
            return lenA == 0;
        }

        int result = 0;
        result |= lenA - lenB;

        // time-constant comparison
        for (int i = 0; i < lenA; i++) {
            // If i >= lenB, indexB is 0; otherwise, i.
            int indexB = ((i - lenB) >>> 31) * i;
            result |= digesta[i] ^ digestb[indexB];
        }
        return result == 0;
    }

    /**
     * Resets the digest for further use.
     */
    abstract public void reset();

    public final String getAlgorithm() {
        return this.algorithm;
    }

    public final int getDigestLength() {
        return -1;
    }

    public Object clone() throws CloneNotSupportedException {
        if (this instanceof Cloneable) {
            return super.clone();
        } else {
            throw new CloneNotSupportedException();
        }
    }


}

