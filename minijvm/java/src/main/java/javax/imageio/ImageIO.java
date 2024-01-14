package javax.imageio;

import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;

public class ImageIO {
    private static byte[] readAllBytes(InputStream inputStream) throws IOException {
        final int bufLen = 4 * 0x400; // 4KB
        byte[] buf = new byte[bufLen];
        int readLen;
        IOException exception = null;

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            while ((readLen = inputStream.read(buf, 0, bufLen)) != -1)
                outputStream.write(buf, 0, readLen);

            return outputStream.toByteArray();
        } catch (IOException e) {
            exception = e;
            throw e;
        } finally {
            outputStream.close();
            if (exception == null) inputStream.close();
            else try {
                inputStream.close();
            } catch (IOException e) {
                exception.addSuppressed(e);
            }
        }
    }

    private static native int[] readInternal(byte[] in, int[] size);

    public static BufferedImage read(InputStream is) throws IOException {
        byte[] bytes = readAllBytes(is);
        int[] size = { 0, 0 };
        int[] rgb = readInternal(bytes, size);

        return new BufferedImage(size[0], size[1], rgb);
    }
}
