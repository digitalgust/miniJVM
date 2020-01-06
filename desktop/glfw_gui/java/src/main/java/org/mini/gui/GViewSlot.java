package org.mini.gui;

import org.mini.glfm.Glfm;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

/**
 * Privide multi slots of view, can swap it by auto or manul drag
 * <p>
 * scrollMode decide to these views scroll direction
 * move decide to the view can move to direction
 */
public class GViewSlot extends GViewPort {
    class SlotProp {
        int move = MOVE_FIXED;

        boolean canMoveToLeft() {
            return (move & MOVE_LEFT) != 0;
        }

        boolean canMoveToRight() {
            return (move & MOVE_RIGHT) != 0;
        }

        boolean canMoveToUp() {
            return (move & MOVE_UP) != 0;
        }

        boolean canMoveToDown() {
            return (move & MOVE_DOWN) != 0;
        }
    }

    List<SlotProp> props = new ArrayList<>();
    public static final int SCROLL_MODE_HORIZONTAL = 0, SCROLL_MODE_VERTICAL = 1;
    public static final int MOVE_FIXED = 0, MOVE_LEFT = 1, MOVE_RIGHT = 2, MOVE_UP = 4, MOVE_DOWN = 8;
    int scrollMode;
    int current = 0;

    float dragBeginX, dragBeginY;

    public GViewSlot(float w, float h, int scrollMod) {
        setSize(w, h);
        scrollMode = scrollMod;
    }

    public void add(int index, GObject go, int moveMode) {
        super.add(index, go);
        if (scrollMode == SCROLL_MODE_HORIZONTAL) {
            go.setLocation(index * getW(), 0);
        } else {
            go.setLocation(0, index * getH());
        }
        go.setSize(getW(), getH());
        SlotProp prop = new SlotProp();
        prop.move = scrollMode == SCROLL_MODE_HORIZONTAL ?
                moveMode & (MOVE_LEFT | MOVE_RIGHT) :
                moveMode & (MOVE_UP | MOVE_DOWN);
        props.add(index, prop);
        reBoundle();
    }

    public void remove(int index) {
        super.remove(index);
    }

    private SlotProp getProp(GObject go) {
        int slot = getElements().indexOf(go);
        return props.get(slot);
    }

    private SlotProp getProp(int slot) {
        return props.get(slot);
    }

    public void setSlotMoveMode(GObject go, int moveMode) {
        int i = getElements().indexOf(go);
        props.get(i).move = moveMode;
    }

    public void showSlot(int slot) {
        GObject go = getElements().get(slot);
        if (go != null) {
            float x = scrollMode == SCROLL_MODE_HORIZONTAL ? -slot * getW() : 0;
            float y = scrollMode == SCROLL_MODE_VERTICAL ? -slot * getH() : 0;
            setInnerLocation(x, y);
        }
    }

    public void moveTo(int slot, long timeInMils) {
        GObject go = getElements().get(slot);
        if (go != null) {
            moveTo(go, timeInMils);
        }
    }


    /**
     * @param go
     */
    public void moveTo(GObject go, long timeInMils) {
        GObject curGo = getElements().get(current);
        if (curGo != null && go != null) {
            SlotSwaper swaper = new SlotSwaper(this, curGo, go, timeInMils);
            getForm().getTimer().schedule(swaper, 0, (long) 16);
            this.current = getElements().indexOf(go);
        }
    }

    class SlotSwaper extends TimerTask {

        long curTime;
        long startAt;
        long timeInMils;

        float distX, distY;
        float slotOrignalX, slotOrignalY;
        GObject from, to;
        GViewSlot slots;


        public SlotSwaper(GViewSlot slots, GObject from, GObject to, long timeInMils) {
            this.slots = slots;
            this.from = from;
            this.to = to;
            slotOrignalX = slots.getInnerBoundle()[LEFT];
            slotOrignalY = slots.getInnerBoundle()[TOP];
            if (from != null && to != null) {
                distX = to.getBoundle()[LEFT] + slotOrignalX;
                distY = to.getBoundle()[TOP] + slotOrignalY;
            }
            if (timeInMils == 0) {
                timeInMils = 1;
            }
            this.timeInMils = timeInMils;
            startAt = System.currentTimeMillis();

        }

        public void run() {
            try {
                curTime = System.currentTimeMillis();
                long goTime = curTime - startAt;
                if ((distX == 0 && scrollMode == SCROLL_MODE_HORIZONTAL)
                        || (distY == 0 && scrollMode == SCROLL_MODE_VERTICAL)) {
                    cancel();
                    return;
                }

                float curX, curY;
                if (goTime < timeInMils) {
                    curX = slotOrignalX - distX * goTime / timeInMils;
                    curY = slotOrignalY - distY * goTime / timeInMils;
                } else {
                    curX = slotOrignalX - distX;
                    curY = slotOrignalY - distY;
                    cancel();
                }
                slots.setInnerLocation(curX, curY);
                //System.out.println("==slot(" + slots.getInnerX() + "," + slots.getInnerY() + "), from:" + from + "to:" + to + ")");
                from.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void touchEvent(int phase, int x, int y) {
        switch (phase) {
            case Glfm.GLFMTouchPhaseBegan: {
                touched = true;
                dragBeginX = x;
                dragBeginY = y;
                break;
            }
            case Glfm.GLFMTouchPhaseEnded: {
                if (dragDirection != DIR_NODEF) {
                    float dx = x - dragBeginX;
                    float dy = y - dragBeginY;
                    SlotProp p = getProp(current);
                    if (p != null) {
                        if (scrollMode == SCROLL_MODE_HORIZONTAL) {
                            if (dx > getW() / 5 && p.canMoveToLeft()) {
                                moveTo(current - 1, 200);
                            } else if (dx < getW() / 5 && p.canMoveToRight()) {
                                moveTo(current + 1, 200);
                            } else {
                                moveTo(current, 200);
                            }
                        } else {
                            if (dy > getH() / 5 && p.canMoveToUp()) {
                                moveTo(current - 1, 200);
                            } else if (dy < getH() / 5 && p.canMoveToDown()) {
                                moveTo(current + 1, 200);
                            } else {
                                moveTo(current, 200);
                            }
                        }
                    } else {
                        moveTo(current, 200);
                    }
                }
                dragDirection = DIR_NODEF;
                touched = false;
                break;
            }
        }
        super.touchEvent(phase, x, y);
    }

    public boolean dragEvent(float dx, float dy, float x, float y) {

        GObject found = findByXY(x, y);
        if (found instanceof GMenu) {
            return found.dragEvent(dx, dy, x, y);
        }

        if (focus == null) {
            setFocus(found);
        }
        if (focus != null && focus.dragEvent(dx, dy, x, y)) {
            return true;
        }
        //System.out.println("drag " + x + "," + y + "," + dx + "," + dy);
        reBoundle();
        float dw = getOutOfViewWidth();
        float dh = getOutOfViewHeight();
        if (dw == 0 && dh == 0) {
            return false;
        }
        if (dragDirection == DIR_NODEF) {
            if (SCROLL_MODE_HORIZONTAL == scrollMode) {
                dragDirection = DIR_X;
            } else {
                dragDirection = DIR_Y;
            }
        }
        float ddx = x - dragBeginX;
        float ddy = y - dragBeginY;
        SlotProp p = getProp(current);
        float odx = (dw == 0) ? 0.f : (float) dx / dw;
        float ody = (dh == 0) ? 0.f : (float) dy / dh;
        if (dragDirection == DIR_X) {
            if (ddx > 0 && p != null && p.canMoveToLeft()) {
                return movePercentX(odx);
            } else if (ddx < 0 && p != null && p.canMoveToRight()) {
                return movePercentX(odx);
            } else {
                return false;
            }
        } else if (dragDirection == DIR_Y) {
            if (ddy > 0 && p != null && p.canMoveToUp()) {
                return movePercentY(ody);
            } else if (ddy < 0 && p != null && p.canMoveToDown()) {
                return movePercentY(ody);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean inertiaEvent(float x1, float y1, float x2, float y2, final long moveTime) {

        GObject go = findByXY(x1, y1);
        if (go != null) {
            if (go.inertiaEvent(x1, y1, x2, y2, moveTime)) {
                return true;
            }
        }
        return false;
    }

}