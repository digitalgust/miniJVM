package java.awt.image;

public class BufferedImage {
    private int width;
    private int height;
    private int[] rgb;

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public native int[] getRgb(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize);

    public BufferedImage(int w, int h, int[] rgb) {
        this.width = w;
        this.height = h;
        this.rgb = rgb;
    }
}
