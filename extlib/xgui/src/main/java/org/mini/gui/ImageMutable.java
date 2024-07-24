package org.mini.gui;

import org.mini.nanovg.Nanovg;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.mini.gui.GToolkit.nvgRGBA;


/**
 * Generate an Image that mutable
 * 4 byte per pixle , ABGR format
 */

public class ImageMutable extends GImage {
    static final int BYTE_PER_PIXEL = 4;
    ByteBuffer data;
    IntBuffer shadow;
    int width, heigh;
    int image_init_flag;
    int gl_texture = -1; //source from gl texture id
    int nvg_texture = -1;
    GGraphics g;

    public ImageMutable(int w, int h) {
        this(w, h, 0);
    }

    public ImageMutable(int w, int h, int imageflag) {
        if (w <= 0 || h <= 0 || w > 4096 || h > 4096) {
            throw new RuntimeException("not support image size " + w + " x " + h);
        }
        init(w, h);
        image_init_flag = imageflag;
    }

    void init(int w, int h) {
        this.width = w;
        this.heigh = h;
        data = ByteBuffer.allocate(w * h * BYTE_PER_PIXEL);
        shadow = data.asIntBuffer();
        nvg_texture = -1;
    }

    public int getPix(int row, int col) {
        int pos = (row * width + col);
        return shadow.get(pos);
    }

    public void setPix(int row, int col, int ABGR) {
        //no check range to speedup
        int pos = (row * width + col);
        shadow.put(pos, ABGR);
    }

    public void setPix(int row, int col, int r, int g, int b, int a) {
        //no check range to speedup
        int pos = (row * width + col);
        if (pos + 3 > data.capacity()) {
            throw new IndexOutOfBoundsException("pos:" + pos);
        }
        shadow.put(pos, ((a & 0xff) << 24) | ((b & 0xff) << 16) | ((g & 0xff) << 8) | (r & 0xff));
    }

    public void setPix(byte[] rgbaData, int offset, int scanlength, int x, int y, int w, int h) {
        ByteBuffer buffer = ByteBuffer.wrap(rgbaData, offset, scanlength * h);
        IntBuffer intbuf = buffer.asIntBuffer();
        for (int j = 0; j < h; j++) {
            int imgFirst = ((y + j) * width) + x;
            shadow.position(imgFirst);
            intbuf.limit((w + imgFirst));
            shadow.put(intbuf);
            buffer.position(buffer.position() + scanlength);
        }
    }

    public void setPix(int[] rgbaData, int offset, int scanlength, int x, int y, int w, int h) {
        if (rgbaData == null) {
            throw new NullPointerException("RGB data is null");
        }
        if (offset < 0 || offset > rgbaData.length) {
            throw new IndexOutOfBoundsException("offset:" + offset);
        }

        int rgbX = offset % scanlength;
        int rgbY = offset / scanlength;
        int rgbMaxRow = rgbaData.length / scanlength;

        //trim out of area
        if (x < 0) {//填充区小于左边界
            rgbX += -x;
            x = 0;
        }
        if (x + w > width) {//填充区大于右边界
            w = width - x;
        }
        if (y < 0) {//填充区小于顶边界
            rgbY += -y;
            y = 0;
        }
        if (y + h > heigh) {//填充区大于底边界
            h = heigh - y;
        }
        if (w <= 0 || h <= 0) {//no area exist
            return;
        }
        //修正源图宽高
        if (rgbY + h > rgbMaxRow) {
            h = rgbMaxRow - rgbY;
        }

        if (rgbX + w > scanlength) {
            w = scanlength - rgbX;
        }

        //fill data
        for (int j = 0; j < h; j++) {
            int rgbFirst = (rgbY + j) * scanlength + rgbX;
            int imgFirst = ((y + j) * width) + x;
            shadow.position(imgFirst);
            try {
                shadow.put(rgbaData, rgbFirst, w);
            } catch (Exception e) {
                int debug = 1;
            }
        }
    }

    private void initimg() {
        long vg = GCallBack.getInstance().getNvContext();
        if (nvg_texture == -1) {
            nvg_texture = Nanovg.nvgCreateImageRGBA(vg, width, heigh, 0, data.array());
            gl_texture = Nanovg.nvglImageHandleGL3(vg, nvg_texture);
        }
    }

    public void updateImage() {
        getNvgTextureId();
        Nanovg.nvgUpdateImage(GCallBack.getInstance().getNvContext(), nvg_texture, data.array());
    }

    /**
     * MUST call by gl thread
     *
     * @return
     */
    public int getNvgTextureId() {
        if (nvg_texture == -1) {
            initimg();
        }
        return nvg_texture;
    }


    /**
     * MUST call by gl thread
     *
     * @return
     */

    public int getNvgTextureId(long vg) {
        if (nvg_texture == -1) {
            initimg();
        }
        return nvg_texture;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return heigh;
    }

    public ByteBuffer getData() {
        return data;
    }

    /**
     * MUST call by gl thread
     *
     * @return
     */

    public int getGLTextureId() {
        if (nvg_texture == -1) {
            initimg();
        }
        return gl_texture;
    }


    @Override
    protected void finalize() {
        try {
            GForm.deleteImage(nvg_texture);
        } catch (Throwable e) {
        }
    }
}
