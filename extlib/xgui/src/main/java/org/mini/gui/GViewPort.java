/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.glfm.Glfm;
import org.mini.glfw.Glfw;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Gust
 */
public class GViewPort extends GContainer {

    protected float[] viewBoundle = new float[4];//可视窗口边界, 
    protected float minX, maxX, minY, maxY;
    protected float scrollx;
    protected float scrolly;

    protected boolean slideDirectionLimit = false;

    public GViewPort(GForm form) {
        super(form);
    }


    @Override
    public void setLocation(float x, float y) {
        viewBoundle[LEFT] = x;
        viewBoundle[TOP] = y;
        reAlign();
    }

    @Override
    public void setSize(float w, float h) {
        viewBoundle[WIDTH] = w;
        viewBoundle[HEIGHT] = h;
        reAlign();
    }

    @Override
    public float getX() {
        if (parent != null) {
            return parent.getInnerX() + viewBoundle[LEFT];
        }
        return viewBoundle[LEFT];
    }

    @Override
    public float getY() {
        if (parent != null) {
            return parent.getInnerY() + viewBoundle[TOP];
        }
        return viewBoundle[TOP];
    }

    @Override
    public float getW() {
        return viewBoundle[WIDTH];
    }

    @Override
    public float getH() {
        return viewBoundle[HEIGHT];
    }

    @Override
    public float[] getBoundle() {
        return viewBoundle;
    }

    public float getLocationLeft() {
        return viewBoundle[LEFT];
    }

    public float getLocationTop() {
        return viewBoundle[TOP];
    }

    @Override
    public float getInnerX() {
        return super.getX();
    }

    @Override
    public float getInnerY() {
        return super.getY();
    }

    @Override
    public float getInnerW() {
        return super.getW();
    }

    @Override
    public float getInnerH() {
        return super.getH();
    }

    @Override
    public void setInnerLocation(float x, float y) {
        super.setLocation(x, y);
        if (getOutOfViewWidth() > 0) setScrollX(-x / getOutOfViewWidth());
        if (getOutOfViewHeight() > 0) setScrollY(-y / getOutOfViewHeight());
    }

    @Override
    public void setInnerSize(float x, float y) {
        super.setSize(x, y);
    }

    @Override
    public float[] getInnerBoundle() {
        return super.getBoundle();
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
        super.onAdd(obj);
        reAlign();
    }

    @Override
    public void onRemove(GObject obj) {
        super.onRemove(obj);
        reAlign();
    }

    @Override
    public void reAlign() {
        float posY = scrolly * (maxY - minY);
        float posX = scrollx * (maxX - minX);

        minX = 0;
        minY = 0;
        maxX = minX + viewBoundle[WIDTH];
        maxY = minY + viewBoundle[HEIGHT];
        synchronized (elements) {
            for (GObject nko : elements) {
                float[] bond = null;
                if (nko instanceof GContainer) {
                    GContainer con = (GContainer) nko;
                    bond = con.getBoundle();

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
        }
        this.boundle[WIDTH] = maxX - minX;
        this.boundle[HEIGHT] = maxY - minY;

        if (boundle[WIDTH] <= viewBoundle[WIDTH]) {
            boundle[LEFT] = viewBoundle[LEFT];
        }
        if (boundle[HEIGHT] <= viewBoundle[HEIGHT]) {
            boundle[TOP] = viewBoundle[TOP];
        }
//        if ((maxY - minY) == 0) {
//            int debug = 1;
//        }
        if (maxY - minY != 0) setScrollY(posY / (maxY - minY));
        if (maxX - minX != 0) setScrollX(posX / (maxX - minX));
    }

    boolean touched;
    static final byte DIR_NODEF = 0, DIR_X = 1, DIR_Y = 2;
    byte dragDirection = DIR_NODEF;

    @Override
    public void touchEvent(int touchid, int phase, int x, int y) {
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
        super.touchEvent(touchid, phase, x, y);
    }

    //每多长时间进行一次惯性动作
    float inertiaPeriod = 16;
    //总共做多少次操作
    long maxMoveCount = 120;
    //初速度加成
    float addOn = 1.5f;
    //惯性任务
    TimerTask task;

    @Override
    public boolean inertiaEvent(float x1, float y1, float x2, float y2, final long moveTime) {
        GObject go = findSonByXY(x1, y1);
        if (go != null) {
            if (go.inertiaEvent(x1, y1, x2, y2, moveTime)) {
                return true;
            }
        }
        //
        final double dx = x2 - x1;
        final double dy = y2 - y1;
        //System.out.println("inertia time: " + moveTime + " , count: " + maxMoveCount + " pos: x1,y1,x2,y2 = " + x1 + "," + y1 + "," + x2 + "," + y2);
        if (Math.abs(dy) > Math.abs(dx)) {
            if (getInnerH() <= getH()) {
                return false;
            }
            task = new TimerTask() {
                //惯性速度
                double speedY = dy * addOn / (moveTime / inertiaPeriod);
                //阻力
                double resistance = speedY / maxMoveCount;
                //lo
                int count = 0;

                @Override
                public void run() {
                    try {
                        //System.out.println(this + " inertia Y " + speedY + " , " + resistance + " , " + count);
                        speedY -= resistance;//速度和阻力抵消为0时,退出滑动

                        float tmpScrollY = scrolly;
                        float inh = getInnerH();
                        if (inh > 0) {
                            float vec = (float) speedY / inh;
                            synchronized (elements) {
                                movePercentY(vec);
                            }
                            tmpScrollY -= vec;
                            //System.out.println("dy:" + ((float) speedY / dh));
                        }
                        GForm.flush();
                        if (count++ > maxMoveCount || tmpScrollY < 0 || tmpScrollY > 1) {
                            try {
                                this.cancel();
                            } catch (Exception e) {
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
        } else {
            if (getInnerW() <= getW()) {
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
                    try {
                        //System.out.println(this + " inertia X " + speedX + " , " + resistance + " , " + count);
                        speedX -= resistance;//速度和阴力抵消为0时,退出滑动

                        float inw = getInnerW();
                        float tmpScrollX = scrollx;
                        if (inw > 0) {
                            float vec = (float) speedX / inw;
                            synchronized (elements) {
                                movePercentX(vec);
                            }
                            tmpScrollX -= vec;
                            //System.out.println("dx:" + ((float) speedX / dw));
                        }
                        GForm.flush();
                        if (count++ > maxMoveCount || tmpScrollX < 0 || tmpScrollX > 1) {
                            try {
                                this.cancel();
                            } catch (Exception e) {
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
        }
        Timer timer = GForm.timer;
        if (timer != null) {
            timer.schedule(task, 0, (long) inertiaPeriod);
        }
        return true;
    }

    @Override
    public boolean scrollEvent(float dx, float dy, float x, float y) {
        boolean b = super.scrollEvent(dx, dy, x, y);
        if (b) return b;

        return dragEvent(Glfw.GLFW_MOUSE_BUTTON_1, dx, dy, x, y);
    }

    @Override
    public void setFlyable(boolean flyable) {
        if (flyable) System.out.println(this.getClass() + " " + getName() + ", can't dragfly, setting ignored ");
    }

    @Override
    public void mouseButtonEvent(int button, boolean pressed, int x, int y) {
        super.mouseButtonEvent(button, pressed, x, y);
        if (!pressed) {
            dragDirection = DIR_NODEF;
        }
    }

    @Override
    public boolean dragEvent(int button, float dx, float dy, float x, float y) {
        GObject found = findSonByXY(x, y);
        if (found instanceof GMenu) {
            return found.dragEvent(button, dx, dy, x, y);
        }

        if (focus == null) {
            setFocus(found);
        }
        if (focus != null && focus.dragEvent(button, dx, dy, x, y)) {
            return true;
        }
        //reSize();
        float dw = getOutOfViewWidth();
        float dh = getOutOfViewHeight();
        if (dw == 0 && dh == 0) {
            return false;
        }
        if (dragDirection == DIR_NODEF) {
            if (Math.abs(dx) > Math.abs(dy) && dw > 0.f) {
                dragDirection = DIR_X;
            } else {
                dragDirection = DIR_Y;
            }
        }
        float odx = (dw == 0) ? 0.f : (float) dx / dw;
        float ody = (dh == 0) ? 0.f : (float) dy / dh;
        if (isSlideDirectionLimit()) {
            if (dragDirection == DIR_X) {
                return movePercentX(odx);
            } else if (dragDirection == DIR_Y) {
                return movePercentY(ody);
            } else {
                return false;
            }
        } else {
            boolean rx = movePercentX(odx);
            boolean ry = movePercentY(ody);
            return rx || ry;
        }
    }

    boolean movePercentY(float dy) {
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

    boolean movePercentX(float dx) {
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

    public boolean isSlideDirectionLimit() {
        return slideDirectionLimit;
    }

    public void setSlideDirectionLimit(boolean slideDirectionLimit) {
        this.slideDirectionLimit = slideDirectionLimit;
    }

}
