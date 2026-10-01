/*
 * To change this license header, choose License Headers and Project Properties.
 * To change this template file, choose Tools | Templates.
 * and open the template in the editor.
 */
package org.mini.gui;

/**
 * 大图网格子图视图：与父 GImage 共享同一张纹理，只记录当前显示的子矩形。
 * picpara="cols,rows,index" 对应本类，index 从 0 开始，按从左到右、从上到下编号。
 * setIndex() 原地修改子矩形，不创建新对象、不上传纹理，可每帧调用做帧动画。
 *
 * @author gust
 */
public class GSubImage extends GImage {

    protected GImage parent;
    protected int cols;
    protected int rows;
    protected int index;
    // 当前子矩形（父图像素坐标）
    protected int sx, sy, sw, sh;

    /**
     * @param parent 底图，若本身是 GSubImage 则按其可见区域再分格
     * @param cols   列数
     * @param rows   行数
     * @param index  子图序号，0 起，从左到右从上到下
     */
    public GSubImage(GImage parent, int cols, int rows, int index) {
        if (parent == null) {
            throw new NullPointerException("parent image is null");
        }
        this.parent = parent;
        this.cols = cols < 1 ? 1 : cols;
        this.rows = rows < 1 ? 1 : rows;
        setIndex(index);
    }

    public GImage getParent() {
        return parent;
    }

    public int getIndex() {
        return index;
    }

    /**
     * 切换显示的子图。只改矩形字段，供脚本每帧调用。
     *
     * @param index 子图序号，越界会被收敛到 [0, cols*rows-1]
     */
    public void setIndex(int index) {
        int total = this.cols * this.rows;
        if (index < 0) {
            index = 0;
        } else if (index >= total) {
            index = total - 1;
        }
        this.index = index;
        int col = index % this.cols;
        int row = index / this.cols;
        // 分格用整除，不整除时余量归最右列/最下行
        int pw = parent.getWidth();
        int ph = parent.getHeight();
        int cw = pw / this.cols;
        int ch = ph / this.rows;
        this.sx = parent.getSx() + col * cw;
        this.sy = parent.getSy() + row * ch;
        this.sw = (col == this.cols - 1) ? (parent.getSx() + pw) - this.sx : cw;
        this.sh = (row == this.rows - 1) ? (parent.getSy() + ph) - this.sy : ch;
    }

    @Override
    public int getWidth() {
        return sw;
    }

    @Override
    public int getHeight() {
        return sh;
    }

    @Override
    public int getSx() {
        return sx;
    }

    @Override
    public int getSy() {
        return sy;
    }

    @Override
    public int getTexWidth() {
        return parent.getTexWidth();
    }

    @Override
    public int getTexHeight() {
        return parent.getTexHeight();
    }

    @Override
    public int getNvgTextureId() {
        return parent.getNvgTextureId();
    }

    @Override
    public int getNvgTextureId(long vg) {
        return parent.getNvgTextureId(vg);
    }

    @Override
    public int getGLTextureId() {
        return parent.getGLTextureId();
    }
}
