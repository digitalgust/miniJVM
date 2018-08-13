/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import java.util.TimerTask;
import org.mini.glfm.Glfm;
import static org.mini.gui.GObject.flush;

/**
 *
 * @author gust
 */
public class GPanel extends GContainer {

    protected float[] viewBoundle = new float[4];//可视窗口边界, 
    float minX, maxX, minY, maxY;
    float scrollx;
    float scrolly;

    @Override
    public void setLocation(float x, float y) {
        viewBoundle[LEFT] = x;
        viewBoundle[TOP] = y;
        super.setLocation(x, y);
    }

    @Override
    public void setSize(float w, float h) {
        viewBoundle[WIDTH] = w;
        viewBoundle[HEIGHT] = h;
        super.setSize(w, h);
    }

    @Override
    public float getViewX() {
        if (parent != null) {
            return parent.getViewX() + viewBoundle[LEFT];
        }
        return viewBoundle[LEFT];
    }

    @Override
    public float getViewY() {
        if (parent != null) {
            return parent.getViewY() + viewBoundle[TOP];
        }
        return viewBoundle[TOP];
    }

    @Override
    public float getViewW() {
        return viewBoundle[WIDTH];
    }

    @Override
    public float getViewH() {
        return viewBoundle[HEIGHT];
    }

    @Override
    public void move(float dx, float dy) {
        boundle[LEFT] += dx;
        boundle[TOP] += dy;
        viewBoundle[LEFT] += dx;
        viewBoundle[TOP] += dy;
    }

    @Override
    public void onAdd(GObject obj) {
        reBoundle();
    }

    @Override
    public void onRemove(GObject obj) {
        reBoundle();
    }

    @Override
    public void reBoundle() {
        float oldMinX = minX, oldMaxX = maxX, oldMinY = minY, oldMaxY = maxY;
        minX = 0;
        minY = 0;
        maxX = minX + viewBoundle[WIDTH];
        maxY = minY + viewBoundle[HEIGHT];
        for (GObject nko : elements) {
            if (nko.boundle[LEFT] < minX) {
                minX = nko.boundle[LEFT];
            }
            if (nko.boundle[LEFT] + nko.boundle[WIDTH] > maxX) {
                maxX = nko.boundle[LEFT] + nko.boundle[WIDTH];
            }
            if (nko.boundle[TOP] < minY) {
                minY = nko.boundle[TOP];
            }
            if (nko.boundle[TOP] + nko.boundle[HEIGHT] > maxY) {
                maxY = nko.boundle[TOP] + nko.boundle[HEIGHT];
            }
        }
        if (maxX - minX > viewBoundle[WIDTH]) {
            this.boundle[WIDTH] = maxX - minX;
        }
        if (maxY - minY > viewBoundle[HEIGHT]) {
            this.boundle[HEIGHT] = maxY - minY;
        }
    }

    @Override
    public void touchEvent(int phase, int x, int y) {
        switch (phase) {
            case Glfm.GLFMTouchPhaseBegan: {
                if (task != null) {
                    task.cancel();
                    task = null;
                }
            }
        }
        super.touchEvent(phase, x, y);
    }

    //每多长时间进行一次惯性动作
    long inertiaPeriod = 16;
    //总共做多少次操作
    long maxMoveCount = 120;
    //惯性任务
    TimerTask task;

    @Override
    public void inertiaEvent(float x1, float y1, float x2, float y2, final long moveTime) {
        GObject go = findFocus(x1, y1);
        if (go != null) {
            go.inertiaEvent(x1, y1, x2, y2, moveTime);
            return;
        }
        //
        //System.out.println("inertia: x1,y1,x2,y2 = " + x1 + "," + y1 + "," + x2 + "," + y2);
        final double dx = x2 - x1;
        final double dy = y2 - y1;
        if (Math.abs(dy) > Math.abs(dx)) {
            task = new TimerTask() {
                //惯性速度
                double speedY = dy / (moveTime / inertiaPeriod);
                //阻力
                double resistance = speedY / maxMoveCount;
                //
                float count = 0;

                @Override
                public void run() {
//                System.out.println("inertia " + speed);
                    speedY += resistance;//速度和阻力抵消为0时,退出滑动

                    float tmpScrollY = scrolly;
                    float dh = getOutOfViewHeight();
                    if (dh > 0) {
                        float vec = (float) speedY / dh;
                        setScrollY(vec);
                        tmpScrollY -= vec;
                        //System.out.println("dy:" + ((float) speedY / dh));
                    }
                    flush();
                    if (count++ > maxMoveCount || tmpScrollY < 0 || tmpScrollY > 1) {
                        try {
                            this.cancel();
                        } catch (Exception e) {
                        }
                    }
                }
            };
        } else {
            task = new TimerTask() {
                //惯性速度
                double speedX = dx / (moveTime / inertiaPeriod);
                //阴力
                double resistance = speedX / maxMoveCount;
                //
                float count = 0;

                @Override
                public void run() {
//                System.out.println("inertia " + speed);
                    speedX += resistance;//速度和阴力抵消为0时,退出滑动

                    float dw = getOutOfViewWidth();
                    float tmpScrollX = scrollx;
                    if (dw > 0) {
                        float vec = (float) speedX / dw;
                        setScrollX(vec);
                        tmpScrollX -= vec;
                        //System.out.println("dx:" + ((float) speedX / dw));
                    }
                    flush();
                    if (count++ > maxMoveCount || tmpScrollX < 0 || tmpScrollX > 1) {
                        try {
                            this.cancel();
                        } catch (Exception e) {
                        }
                    }
                }
            };
        }
        getTimer().schedule(task, 0, inertiaPeriod);
    }

    @Override
    public void dragEvent(float dx, float dy, float x, float y) {
        if (focus == null) {
            setFocus(findFocus(x, y));
        }
        if (focus != null) {
            focus.dragEvent(dx, dy, x, y);
        } else {
            reBoundle();
            float dw = getOutOfViewWidth();
            float dh = getOutOfViewHeight();
            float odx = (dw == 0) ? 0.f : (float) dx / dw;
            float ody = (dh == 0) ? 0.f : (float) dy / dh;
            if (Math.abs(odx) > Math.abs(ody)) {
                setScrollX(odx);
            } else {
                setScrollY(ody);
            }

        }
    }

    void setScrollY(float dy) {
        this.scrolly -= dy;
        if (scrolly < 0) {
            scrolly = 0;
        }
        if (scrolly > 1) {
            scrolly = 1;
        }
        boundle[TOP] = viewBoundle[TOP] + (-minY) - scrolly * getOutOfViewHeight();
    }

    void setScrollX(float dx) {
        this.scrollx -= dx;
        if (scrollx < 0) {
            scrollx = 0;
        }
        if (scrollx > 1) {
            scrollx = 1;
        }
        boundle[LEFT] = viewBoundle[LEFT] + (-minX) - scrollx * getOutOfViewWidth();
    }

    float getOutOfViewHeight() {
        return boundle[HEIGHT] - viewBoundle[HEIGHT];
    }

    float getOutOfViewWidth() {
        return boundle[WIDTH] - viewBoundle[WIDTH];
    }

}
