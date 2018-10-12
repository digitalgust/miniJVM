/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import java.util.Timer;
import java.util.TimerTask;
import org.mini.glfm.Glfm;
import static org.mini.gui.GObject.HEIGHT;
import static org.mini.gui.GObject.LEFT;
import static org.mini.gui.GObject.TOP;
import static org.mini.gui.GObject.WIDTH;
import static org.mini.gui.GObject.flush;
import static org.mini.gui.GObject.flush;
import static org.mini.gui.GObject.flush;
import static org.mini.gui.GObject.flush;
import static org.mini.gui.GObject.flush;
import static org.mini.gui.GObject.flush;
import static org.mini.gui.GObject.flush;
import static org.mini.gui.GObject.flush;

/**
 *
 * @author Gust
 */
public class GViewPort extends GContainer {

    protected float[] viewBoundle = new float[4];//可视窗口边界, 
    float minX, maxX, minY, maxY;
    float scrollx;
    float scrolly;

    @Override
    public int getType() {
        return TYPE_VIEWPORT;
    }

    @Override
    public void setViewLocation(float x, float y) {
        viewBoundle[LEFT] = x;
        viewBoundle[TOP] = y;
        if (parent != null) {
            parent.reBoundle();
        }
    }

    @Override
    public void setViewSize(float w, float h) {
        viewBoundle[WIDTH] = w;
        viewBoundle[HEIGHT] = h;
        if (parent != null) {
            parent.reBoundle();
        }
    }

    @Override
    public float getViewX() {
        if (parent != null) {
            return parent.getX() + viewBoundle[LEFT];
        }
        return viewBoundle[LEFT];
    }

    @Override
    public float getViewY() {
        if (parent != null) {
            return parent.getY() + viewBoundle[TOP];
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

    public float[] getViewBoundle() {
        return viewBoundle;
    }

    @Override
    public void move(float dx, float dy) {
        boundle[LEFT] += dx;
        boundle[TOP] += dy;
        viewBoundle[LEFT] += dx;
        viewBoundle[TOP] += dy;
        if (parent != null) {
            parent.reBoundle();
        }
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
        float posY = scrolly * (maxY - minY);
        float posX = scrollx * (maxX - minX);

        minX = 0;
        minY = 0;
        maxX = minX + viewBoundle[WIDTH];
        maxY = minY + viewBoundle[HEIGHT];
        for (GObject nko : elements) {
            float[] bond = null;
            if (nko instanceof GContainer) {
                GContainer con = (GContainer) nko;
                bond = con.getViewBoundle();

            } else {
                bond = nko.getBoundle();
            }
            if (bond[LEFT] < minX) {
                minX = bond[LEFT];
            }
            if (bond[LEFT] + bond[WIDTH] > maxX) {
                maxX = bond[LEFT] + bond[WIDTH];
            }
            if (bond[TOP] < minY) {
                minY = bond[TOP];
            }
            if (bond[TOP] + bond[HEIGHT] > maxY) {
                maxY = bond[TOP] + bond[HEIGHT];
            }
        }
        this.boundle[WIDTH] = maxX - minX;
        this.boundle[HEIGHT] = maxY - minY;
        if (boundle[WIDTH] <= viewBoundle[WIDTH]) {
            boundle[LEFT] = viewBoundle[LEFT];
        }
        if (boundle[HEIGHT] <= viewBoundle[HEIGHT]) {
            boundle[TOP] = viewBoundle[TOP];
        }

        setScrollY(posY / (maxY - minY));
        setScrollX(posX / (maxX - minX));
    }

    boolean touched;
    static final byte DIR_NODEF = 0, DIR_X = 1, DIR_Y = 2;
    byte dragDirection = DIR_NODEF;

    @Override
    public void touchEvent(int phase, int x, int y) {
        switch (phase) {
            case Glfm.GLFMTouchPhaseBegan: {
                if (task != null) {
                    task.cancel();
                    task = null;
                }
                touched = true;
                break;
            }
            case Glfm.GLFMTouchPhaseEnded: {
                touched = false;
                dragDirection = DIR_NODEF;
                break;
            }
        }
        super.touchEvent(phase, x, y);
    }

    //每多长时间进行一次惯性动作
    long inertiaPeriod = 16;
    //总共做多少次操作
    long maxMoveCount = 120;
    //初速度加成
    float addOn = 1.2f;
    //惯性任务
    TimerTask task;

    @Override
    public boolean inertiaEvent(float x1, float y1, float x2, float y2, final long moveTime) {
        GObject go = findByXY(x1, y1);
        if (go != null) {
            if (go.inertiaEvent(x1, y1, x2, y2, moveTime)) {
                return true;
            }
        }
        //
        //System.out.println("inertia: x1,y1,x2,y2 = " + x1 + "," + y1 + "," + x2 + "," + y2);
        final double dx = x2 - x1;
        final double dy = y2 - y1;
        if (Math.abs(dy) > Math.abs(dx)) {
            if (getH() <= getViewH()) {
                return false;
            }
            task = new TimerTask() {
                //惯性速度
                double speedY = dy * addOn / (moveTime / inertiaPeriod);
                //阻力
                double resistance = speedY / maxMoveCount;
                //lo
                float count = 0;

                @Override
                public void run() {
//                System.out.println("inertia " + speed);
                    speedY -= resistance;//速度和阻力抵消为0时,退出滑动

                    float tmpScrollY = scrolly;
                    float dh = getH();
                    if (dh > 0) {
                        float vec = (float) speedY / dh;
                        moveY(vec);
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
            if (getW() <= getViewW()) {
                return false;
            }
            task = new TimerTask() {
                //惯性速度
                double speedX = dx * addOn / (moveTime / inertiaPeriod);
                //阴力
                double resistance = speedX / maxMoveCount;
                //
                float count = 0;

                @Override
                public void run() {
//                System.out.println("inertia " + speed);
                    speedX -= resistance;//速度和阴力抵消为0时,退出滑动

                    float dw = getOutOfViewWidth();
                    float tmpScrollX = scrollx;
                    if (dw > 0) {
                        float vec = (float) speedX / dw;
                        moveX(vec);
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
        Timer timer = getTimer();
        if (timer != null) {
            timer.schedule(task, 0, inertiaPeriod);
        }
        return true;
    }

    @Override
    public boolean scrollEvent(float dx, float dy, float x, float y) {
        return dragEvent(dx, dy, x, y);
    }

    @Override
    public boolean dragEvent(float dx, float dy, float x, float y) {
        if (focus == null) {
            setFocus(findByXY(x, y));
        }
        if (focus != null && focus.dragEvent(dx, dy, x, y)) {
            return true;
        }
        reBoundle();
        float dw = getOutOfViewWidth();
        float dh = getOutOfViewHeight();
        if (dw == 0 && dh == 0) {
            return false;
        }
        if (dragDirection == DIR_NODEF) {
            if (Math.abs(dx) > Math.abs(dy)) {
                dragDirection = DIR_X;
            } else {
                dragDirection = DIR_Y;
            }
        }
        float odx = (dw == 0) ? 0.f : (float) dx / dw;
        float ody = (dh == 0) ? 0.f : (float) dy / dh;
        if (dragDirection == DIR_X) {
            return moveX(odx);
        } else if (dragDirection == DIR_Y) {
            return moveY(ody);
        } else {
            return false;
        }
    }

    boolean moveY(float dy) {
        if (getOutOfViewHeight() <= 0) {
            return false;
        }
        float tmpy = scrolly;
        tmpy -= dy;
        if (tmpy < 0) {
            tmpy = 0;
        }
        if (tmpy > 1) {
            tmpy = 1;
        }
        boundle[TOP] = viewBoundle[TOP] + (-minY) - tmpy * getOutOfViewHeight();
        if (scrolly != tmpy) {
            scrolly = tmpy;
            return true;
        }
        return false;
    }

    boolean moveX(float dx) {
        if (getOutOfViewWidth() <= 0) {
            return false;
        }
        float tmpx = scrollx;
        tmpx -= dx;
        if (tmpx < 0) {
            tmpx = 0;
        }
        if (tmpx > 1) {
            tmpx = 1;
        }
        boundle[LEFT] = viewBoundle[LEFT] + (-minX) - tmpx * getOutOfViewWidth();
        if (scrollx != tmpx) {
            scrollx = tmpx;
            return true;
        }
        return false;
    }

    public void setScrollX(float sx) {
        if (sx < 0 || sx > 1 || boundle[WIDTH] <= viewBoundle[WIDTH]) {
            return;
        }
        scrollx = sx;
        boundle[LEFT] = viewBoundle[LEFT] + (-minX) - scrollx * getOutOfViewWidth();
    }

    public void setScrollY(float sy) {
        if (sy < 0 || sy > 1 || boundle[HEIGHT] <= viewBoundle[HEIGHT]) {
            return;
        }
        scrolly = sy;
        boundle[TOP] = viewBoundle[TOP] + (-minY) - scrolly * getOutOfViewHeight();
    }

    public float getScrollY() {
        return scrolly;
    }

    public float getScrollX() {
        return scrollx;
    }

    float getOutOfViewHeight() {
        return boundle[HEIGHT] - viewBoundle[HEIGHT];
    }

    float getOutOfViewWidth() {
        return boundle[WIDTH] - viewBoundle[WIDTH];
    }

}
