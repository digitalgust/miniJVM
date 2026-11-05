/* SPDX-License-Identifier: MIT */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.glfm.Glfm;
import org.mini.glfw.Glfw;
import org.mini.gui.callback.GCmd;
import org.mini.gui.event.GActionListener;
import org.mini.gui.callback.GCallBack;
import org.mini.gui.event.GStateChangeListener;
import org.mini.util.SysLog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.mini.gui.GToolkit.FEEL_DIMENSION;
import static org.mini.gui.GToolkit.nvgRGBA;
import static org.mini.glwrap.GLUtil.toCstyleBytes;
import static org.mini.nanovg.Nanovg.*;

/**
 * GList 有两种外观，
 * 第一种是单行模式 MODE_SINGLE_SHOW，未点击时会显示 normalPanel,点击时会弹出一个下拉菜单popWin供选取
 * 第二种是多行模式，多行模式直接显示popWin
 * <p>
 * popWin包含一个ViewPort(popView)和一个滚动条 scrollBar
 *
 * @author gust
 */
public class GList extends GContainer {
    public static final int MODE_MULTI_SHOW = 1, MODE_SINGLE_SHOW = 2;
    public static final int MODE_MULTI_SELECT = 4, MODE_SINGLE_SELECT = 8;
    public static final float POPUP_WIDTH_AUTO = -1f;
    public static final float ITEM_HEIGH_DEFAULT = 36f;
    public static final float ITEM_IMG_H_DEFAULT = 28f;
    static float[] GRADIENT_COLOR0 = {1.0f, 1.0f, 1.0f, 0.125f};
    static float[] GRADIENT_COLOR1 = {0.0f, 0.0f, 0.0f, 0.125f};

    protected String preicon;
    protected List<Integer> selected = new ArrayList();
    protected boolean pulldown;
    private float popupWidth;
    private float popupMaxWidth;
    private float popupContentWidth;
    //
    protected int showMode = MODE_SINGLE_SHOW;
    //
    protected int selectMode = MODE_SINGLE_SELECT;

    protected GScrollBar scrollBar;
    protected GViewPort popView;
    protected GListPopWindow popWin;
    protected float[] lineh = {0f};
    protected float list_image_size = ITEM_IMG_H_DEFAULT;
    protected float list_item_heigh = ITEM_HEIGH_DEFAULT;
    protected float list_rows_max = 7;
    protected float list_rows_min = 1;
    protected float pad = 2;
    protected int scrollbarWidth = 20;

    protected boolean showScrollbar = false;

    protected float left, top, width, height;
    //
    protected final List<Integer> outOffilterList = new ArrayList();//if a item in this list , it would show dark text color


    /**
     *
     */
    public GList(GForm form) {
        this(form, 0f, 0f, 1f, 1f);
    }

    public GList(GForm form, float left, float top, float width, float height) {
        super(form);
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;

        //
        scrollBar = new GScrollBar(form, 0, GScrollBar.VERTICAL, 0, 0, scrollbarWidth, 100);
        scrollBar.setActionListener(new ScrollBarActionListener());
        scrollBar.setStateChangeListener(new ScrollBarStateChangeListener());
        popView = new GListPopView(form);
        popWin = new GListPopWindow(form);
        popWin.addImpl(scrollBar);
        popWin.addImpl(popView);
        setScrollBar(false);
        changeCurPanel();
        setCornerRadius(4.f);

        setLocation(left, top);
        setSize(width, height);

        setShowMode(MODE_SINGLE_SHOW);

    }

    @Override
    public void setSize(float w, float h) {
        width = w;
        height = h;
        super.setSize(w, h);
        sizeAdjust();
    }

    @Override
    public void setInnerSize(float w, float h) {
        setSize(w, h);
    }


    public void setScrollBar(boolean show) {
        this.showScrollbar = show;
        scrollbarWidth = show ? 20 : 0;
        scrollBar.setVisible(show);
        sizeAdjust();
    }

    public void setItemHeight(float h) {
        list_item_heigh = h;
        list_image_size = h - 12;
        for (int i = 0; i < getElementSize(); i++) {
            GListItem item = getItem(i);
            item.setSize(item.getW(), h);
        }
        sizeAdjust();
    }

    /**
     * lock the list when modify it
     *
     * @return
     */
    public List<GObject> getItemList() {
        return popView.getElementsImpl();
    }


    @Override
    public float getInnerX() {
        return getX();
    }

    @Override
    public float getInnerY() {
        return getY();
    }

    @Override
    public float getInnerW() {
        return getW();
    }

    @Override
    public float getInnerH() {
        return getH();
    }

    @Override
    public void setInnerLocation(float x, float y) {
        setLocation(x, y);
    }

    @Override
    public float[] getInnerBoundle() {
        return getBoundle();
    }

    public GListItem addItem(GImage img, String lab) {
        GListItem gli = new GListItem(form, img, lab);
        add(gli);
        return gli;
    }

    public void add(GObject gli) {
        if (gli != null && gli instanceof GListItem) {
            popView.addImpl(gli);
            ((GListItem) gli).list = this;
            sizeAdjust();
        }
    }

    public void add(int index, GObject gli) {
        if (gli != null && gli instanceof GListItem) {
            popView.addImpl(index, gli);
            ((GListItem) gli).list = this;
            sizeAdjust();
        }
    }

    public void remove(int index) {
        popView.removeImpl(index);
        sizeAdjust();
    }

    public void remove(GObject go) {
        if (!(go instanceof GListItem)) {
            throw new IllegalArgumentException("need GListItem");
        }
        popView.removeImpl(go);
        sizeAdjust();
    }

    public GListItem[] getItems() {
        GListItem[] items = new GListItem[popView.getElementSizeImpl()];
        int i = 0;
        for (GObject go : popView.elements) {
            items[i] = (GListItem) go;
            i++;
        }
        return items;
    }

    /**
     * lock the list when modify it
     *
     * @return
     */
    public List<GObject> getElements() {
        return popView.getElementsImpl();
    }

    public int getElementSize() {
        return popView.elements.size();
    }

    public boolean contains(GObject son) {
        return popView.containsImpl(son);
    }

    public void clear() {
        popView.clearImpl();
        sizeAdjust();
    }


    /**
     * Sets the dropdown width in layout units. Use 0 for the header width,
     * or POPUP_WIDTH_AUTO to fit the widest item.
     */
    public void setPopupWidth(float width) {
        if (Float.isNaN(width) || Float.isInfinite(width) || (width < 0 && width != POPUP_WIDTH_AUTO)) {
            throw new IllegalArgumentException("Invalid popup width: " + width);
        }
        popupWidth = width;
        sizeAdjust();
    }

    public float getPopupWidth() {
        return popupWidth;
    }

    /** Sets the dropdown width limit. Use 0 to limit it only to the form. */
    public void setPopupMaxWidth(float width) {
        if (Float.isNaN(width) || Float.isInfinite(width) || width < 0) {
            throw new IllegalArgumentException("Invalid maximum popup width: " + width);
        }
        popupMaxWidth = width;
        sizeAdjust();
    }

    public float getPopupMaxWidth() {
        return popupMaxWidth;
    }

    void sizeAdjust() {
        if (showMode == MODE_MULTI_SHOW) {
            popWin.setLocation(0, 0);
            resizeListPanel(width, height);
        } else {
            normalPanel.setLocation(0, 0);
            normalPanel.setSize(width, height);
            if (pulldown) {
                measurePopupContent();
                if (!layoutPopup()) closePopup();
            } else {
                resizeListPanel(width, getPopWinH());
            }
        }
        GForm.flush();
    }

    private void resizeListPanel(float w, float h) {
        popWin.setSize(w, h);
        reAlignItems();
        scrollBar.setPos(popView.scrolly);
    }

    public void reAlignItems() {
        List<GObject> items = popView.getElementsImpl();
        for (int i = 0; i < items.size(); i++) {
            GObject go = items.get(i);
            go.setLocation(pad, i * list_item_heigh);
            go.setSize(Math.max(0, popView.getW() - pad * 2), list_item_heigh);
        }
        popView.reAlign();
    }

    void changeCurPanel() {
        if (showMode == MODE_SINGLE_SHOW) {
            if (popWin.parent == this) super.removeImpl(popWin);
            super.addImpl(normalPanel);
        } else {
            if (normalPanel.parent == this) super.removeImpl(normalPanel);
            super.addImpl(popWin);
        }
    }

    private float getPopWinH() {
        return Math.max(list_rows_min, Math.min(list_rows_max, popView.elements.size())) * list_item_heigh;
    }

    private void measurePopupContent() {
        popupContentWidth = 0;
        if (popupWidth != POPUP_WIDTH_AUTO || popView.elements.isEmpty()) return;
        GCallBack callback = GCallBack.getInstance();
        long vg = callback == null ? 0 : callback.getNvContext();
        if (vg == 0 || GToolkit.FontHolder.word == null) return;

        nvgSave(vg);
        try {
            nvgFontSize(vg, getFontSize());
            nvgFontFace(vg, GToolkit.getFontWord());
            nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
            float[] bounds = new float[4];
            List<GObject> items = popView.getElementsImpl();
            for (int i = 0; i < items.size(); i++) {
                GListItem item = (GListItem) items.get(i);
                popupContentWidth = Math.max(popupContentWidth, item.measurePopupWidth(vg, bounds));
            }
        } finally {
            nvgRestore(vg);
        }
        popupContentWidth = (float) Math.ceil(popupContentWidth + 2 * pad + scrollbarWidth);
    }

    boolean layoutPopup() {
        float formW = form.getInnerW();
        float formH = form.getInnerH();
        if (formW <= 0 || formH <= 0) return false;

        float preferredW = width;
        if (popupWidth == POPUP_WIDTH_AUTO) preferredW = Math.max(width, popupContentWidth);
        else if (popupWidth > 0) preferredW = popupWidth;
        float maxW = popupMaxWidth > 0 ? Math.min(formW, popupMaxWidth) : formW;
        float w = Math.min(maxW, Math.max(1, preferredW));

        float anchorX = normalPanel.getX() - form.getInnerX();
        float anchorY = normalPanel.getY() - form.getInnerY();
        float above = Math.max(0, Math.min(formH, anchorY));
        float belowY = Math.max(0, Math.min(formH, anchorY + normalPanel.getH()));
        float below = formH - belowY;
        float preferredH = getPopWinH();
        boolean openBelow = below >= preferredH || below >= above;
        float h = Math.min(preferredH, openBelow ? below : above);
        if (h <= 0) return false;

        float x = Math.max(0, Math.min(anchorX, formW - w));
        float y = openBelow ? belowY : above - h;
        if (popWin.getLocationLeft() != x || popWin.getLocationTop() != y) {
            popWin.setLocation(x, y);
        }
        if (popWin.getW() != w || popWin.getH() != h || popView.getW() != Math.max(1, w - scrollbarWidth)) {
            resizeListPanel(w, h);
        }
        return true;
    }

    private void openPopup() {
        if (pulldown || showMode != MODE_SINGLE_SHOW || !enable || !isSuperParent(form)) return;
        measurePopupContent();
        if (layoutPopup()) form.showListPopup(this);
    }

    void closePopup() {
        form.hideListPopup(this);
    }

    public void setItems(GImage[] imgs, String[] labs) {
        if ((imgs == null && labs == null) || (imgs != null && labs != null && imgs.length != labs.length)) {
            throw new IllegalArgumentException("need images and labels count equals.");
        }
        int len = imgs == null ? labs.length : imgs.length;
        for (int i = 0; i < len; i++) {
            addItem(imgs == null ? null : imgs[i], labs == null ? null : labs[i]);
        }
        sizeAdjust();
    }

    public void setShowMode(int m) {
        if (showMode != m) closePopup();
        this.showMode = m;
        if (m == MODE_MULTI_SHOW) {
            popWin.layer = LAYER_NORMAL;
        } else {
            popWin.layer = LAYER_MENU_OR_POPUP;
        }

        sizeAdjust();
        changeCurPanel();
    }

    public int getShowMode() {
        return this.showMode;
    }

    @Override
    public void setEnable(boolean enable) {
        super.setEnable(enable);
        if (!enable) closePopup();
    }

    public void setSelectMode(int m) {
        selectMode = m;
    }

    public int getSelectMode() {
        return selectMode;
    }

    public int getSelectedIndex() {
        if (selected.size() > 0) {
            return selected.get(0);
        }
        return -1;
    }


    public GListItem getSelectedItem() {
        if (selected.size() > 0) {
            return (GListItem) getElements().get(selected.get(0));
        }
        return null;
    }


    public void setSelectedIndex(int i) {
        this.selected.clear();
        if (i >= 0 && i < this.popView.getElementsImpl().size()) {
            this.selected.add(i);
            float viewHeight = this.popView.getH();
            float itemHeight = this.list_item_heigh;
            float currentScrollY = this.popView.getScrollY();
            float totalContentHeight = (float) this.getElementSize() * itemHeight;
            float visibleTopY = currentScrollY * (totalContentHeight - viewHeight);
            float visibleBottomY = visibleTopY + viewHeight;
            float itemTopY = (float) i * itemHeight;
            float itemBottomY = itemTopY + itemHeight;
            boolean itemVisible = itemTopY >= visibleTopY && itemBottomY <= visibleBottomY;
            if (!itemVisible) {
                float targetScrollY;
                if (itemTopY < visibleTopY) {
                    targetScrollY = itemTopY / (totalContentHeight - viewHeight);
                } else {
                    targetScrollY = (itemBottomY - viewHeight) / (totalContentHeight - viewHeight);
                }

                if (targetScrollY < 0.0F) {
                    targetScrollY = 0.0F;
                }

                if (targetScrollY > 1.0F) {
                    targetScrollY = 1.0F;
                }

                this.popView.setScrollY(targetScrollY);
            }
        }

    }


    public int[] getSelectedIndices() {
        int size = selected.size();
        int[] r = new int[size];
        for (int i = 0; i < size; i++) {
            r[i] = selected.get(i);
        }
        return r;
    }

    public void setSelectedIndices(int[] s) {
        selected.clear();
        for (int i = 0; i < s.length; i++) {
            selected.add(s[i]);
        }
    }

    boolean isSelected(int index) {
        return selected.contains(index);
    }

    public int getItemIndex(GListItem item) {
        return popView.getElementsImpl().indexOf(item);
    }

    public void selectAll() {
        if (selectMode == MODE_MULTI_SELECT) {
            int size = popView.getElementSizeImpl();
            for (int i = 0; i < size; i++) {
                selected.add(i);
            }
        }
    }

    public void deSelectAll() {
        selected.clear();
    }

    void select(int index) {
        if (selectMode == MODE_SINGLE_SELECT) {
            selected.clear();
            selected.add(Integer.valueOf(index));
        } else if (selected.contains(index)) {
            selected.remove(Integer.valueOf(index));
        } else {
            selected.add(Integer.valueOf(index));
        }
    }

    void unSelect(int index) {
        selected.remove(new Integer(index));
    }

    public GListItem getItem(int index) {
        if (index < 0 || index >= popView.elements.size()) {
            return null;
        }
        return (GListItem) popView.elements.get(index);
    }

    public void addOutOfFilter(int index) {
        outOffilterList.add(index);
    }

    public void removeOutOfFilter(int index) {
        outOffilterList.remove(index);
    }

    public void clearOutOfFilter() {
        outOffilterList.clear();
    }

    public boolean isOutOfFilter(int index) {
        return outOffilterList.contains(index);
    }

    public void filterLabelWithKey(String key) {
        if (key == null) {
            return;
        }
        clearOutOfFilter();
        List<GObject> list = getItemList();
        for (GObject go : list) {
            GListItem gli = (GListItem) go;
            if (gli.getLabel() != null && gli.getLabel().toLowerCase().contains(key.toLowerCase())) {

            } else {
                addOutOfFilter(getItemIndex(gli));
                //System.out.println("except item:" + getItemIndex(gli));
            }
        }

    }

    public void sort(Comparator<? super GObject> c) {
        popView.sort(c);
        sizeAdjust();
    }


    @Override
    public void setFlyable(boolean flyable) {
        if (flyable) SysLog.info("" + this.getClass() + " " + getName() + ", can't dragfly, setting ignored ");
    }

    /**
     * @param vg
     * @return
     */
    @Override
    public boolean paint(long vg) {

        //int itemcount = popView.elements.size();
        nvgFontSize(vg, getFontSize());
        nvgFontFace(vg, GToolkit.getFontWord());
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);

        nvgTextMetrics(vg, null, null, lineh);
        super.paint(vg);

        return true;
    }

    @Override
    public void mouseButtonEvent(int button, boolean pressed, int x, int y) {
        super.mouseButtonEvent(button, pressed, x, y);
    }

    /**
     *
     */
    NormalPanel normalPanel = new NormalPanel(form);

    class NormalPanel extends GContainer {
        public NormalPanel(GForm form) {
            super(form);
            setCornerRadius(4.f);
        }

        private boolean pressed;
        private int activeTouch = -1;

        @Override
        public void touchEvent(int touchid, int phase, int x, int y) {
            if (phase == Glfm.GLFMTouchPhaseBegan && activeTouch == -1) {
                activeTouch = touchid;
            } else if (touchid == activeTouch && phase == Glfm.GLFMTouchPhaseEnded) {
                activeTouch = -1;
                if (isInArea(x, y)) openPopup();
            } else if (touchid == activeTouch && phase == Glfm.GLFMTouchPhaseCancelled) {
                activeTouch = -1;
            }
            super.touchEvent(touchid, phase, x, y);
        }

        @Override
        public void mouseButtonEvent(int button, boolean down, int x, int y) {
            if (button == Glfw.GLFW_MOUSE_BUTTON_1) {
                if (down) {
                    pressed = true;
                } else {
                    boolean clicked = pressed && isInArea(x, y);
                    pressed = false;
                    if (clicked) openPopup();
                }
            }
            super.mouseButtonEvent(button, down, x, y);
        }

        @Override
        void doFocusLost(GObject newFocus) {
            pressed = false;
            activeTouch = -1;
            super.doFocusLost(newFocus);
        }

        @Override
        public boolean paint(long vg) {
            drawNormal(vg, normalPanel.getX(), normalPanel.getY(), normalPanel.getW(), normalPanel.getH());
            return true;
        }

        void drawNormal(long vg, float x, float y, float w, float h) {
            byte[] bg;

            if (GToolkit.getFeel() == FEEL_DIMENSION) {
                bg = nvgLinearGradient(vg, x, y, x, y + h, GRADIENT_COLOR0, GRADIENT_COLOR1);
                nvgBeginPath(vg);
                nvgRoundedRect(vg, x + 1, y + 1, w - 2, h - 2, getCornerRadius() - 1);
                nvgFillPaint(vg, bg);
                nvgFill(vg);
            } else {

                nvgBeginPath(vg);
                nvgRoundedRect(vg, x + 1, y + 1, w - 2, h - 2, getCornerRadius() - 0.5f);
                nvgFillColor(vg, GRADIENT_COLOR0);
                nvgFill(vg);
            }

            nvgBeginPath(vg);
            nvgRoundedRect(vg, x + 0.5f, y + 0.5f, w - 1, h - 1, getCornerRadius() - 0.5f);
            nvgStrokeColor(vg, nvgRGBA(0, 0, 0, 48));
            nvgStroke(vg);

            nvgFontSize(vg, GToolkit.getStyle().getIconFontSize());
            nvgFontFace(vg, GToolkit.getFontIcon());
            nvgFillColor(vg, GToolkit.getStyle().getTextFontColor());
            nvgTextAlign(vg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
            byte[] curIcon = pulldown ? ICON_CHEVRON_DOWN_BYTE : ICON_CHEVRON_RIGHT_BYTE;
            nvgTextJni(vg, x + w - 15, y + h * 0.5f, curIcon, 0, curIcon.length);

            float thumb = h - pad;
            if (popView.elements.size() > 0) {
                int selectIndex = getSelectedIndex();
                if (selectIndex >= 0) {
                    GListItem gli = (GListItem) getItem(selectIndex);
                    GToolkit.drawImage(vg, gli.img, x + pad, y + h * 0.5f - thumb / 2, thumb, thumb, false, 1.0f);

                    nvgScissor(vg, x, y, w - 20, h);
                    float dx = x + (gli.img == null ? 0 : thumb) + pad + pad;
                    GToolkit.drawTextLine(vg, dx, y + h / 2, gli.getText(), GList.this.getFontSize(), GList.this.getColor(), NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
                }
            }
        }
    }

    ;

    /**
     *
     */


    class GListPopView extends GViewPort {
        public GListPopView(GForm form) {
            super(form);
        }

        @Override
        public void setScrollY(float sy) {
            super.setScrollY(sy);
            scrollBar.setPos(sy);
        }

        @Override
        boolean movePercentY(float dy) {
            boolean ret = super.movePercentY(dy);
            scrollBar.setPos(scrolly);
            return ret;
        }
    }

    ;


    class GListPopWindow extends GContainer {
        public GListPopWindow(GForm form) {
            super(form);
            setCornerRadius(4.f);
        }

        @Override
        public boolean paint(long vg) {

            float x = getX();
            float y = getY();
            float w = getW();
            float h = getH();

            float[] bg;
            if (showMode == MODE_MULTI_SHOW) {
                bg = GToolkit.getStyle().getListBackgroundColor();
            } else {
                bg = GToolkit.getStyle().getPopBackgroundColor();
            }
            if (GList.this.bgColor != null) {
                bg = GList.this.bgColor;
            }
            GToolkit.drawRoundedRect(vg, x, y, w, h, getCornerRadius(), nvgRGBA(0, 0, 0, 48), bg);

            super.paint(vg);

            return true;
        }

        @Override
        public void setSize(float width, float height) {
            super.setSize(width, height);

            popView.setLocation(0, 0);
            float w = width - scrollbarWidth;
            if (w < 1) w = 1;
            popView.setSize(w, height);

            scrollBar.setLocation(width - scrollbarWidth, 0);
            scrollBar.setSize(20, height);
        }

        @Override
        void doFocusLost(GObject newFocus) {
            if (pulldown) closePopup();
            super.doFocusLost(newFocus);
        }

        @Override
        public void keyEventGlfw(int key, int scanCode, int action, int mods) {
            if (parent == null || parent.getCurrent() != this) return;
            if (action == Glfw.GLFW_PRESS) {
                if (key == Glfw.GLFW_KEY_ESCAPE) {
                    closePopup();
                } else if (key == Glfw.GLFW_KEY_UP) {
                    if (popView.elements.isEmpty()) return;
                    int selectIndex = getSelectedIndex();
                    selectIndex -= 1;
                    if (selectIndex < 0) selectIndex = popView.getElementSize() - 1;
                    select(selectIndex);
                    setCurrent(getSelectedItem());
                } else if (key == Glfw.GLFW_KEY_DOWN) {
                    if (popView.elements.isEmpty()) return;
                    int selectIndex = getSelectedIndex();
                    selectIndex += 1;
                    if (selectIndex >= popView.getElementSize()) selectIndex = 0;
                    select(selectIndex);
                    setCurrent(getSelectedItem());
                } else if (action == Glfw.GLFW_PRESS) {
                    closePopup();

                    if (key == Glfw.GLFW_KEY_ENTER) {
                        GListItem li = getSelectedItem();
                        if (li != null) {
                            li.select();
                            doAction();
                        }
                    }
                }
            }
            super.keyEventGlfw(key, scanCode, action, mods);
        }

    }

    ;

    class ScrollBarActionListener implements GActionListener {

        @Override
        public void action(GObject gobj) {
            popView.setScrollY(((GScrollBar) gobj).getPos());
            sizeAdjust();
            GForm.flush();
        }

    }

    class ScrollBarStateChangeListener implements GStateChangeListener {

        @Override
        public void onStateChange(GObject gobj) {
            popView.setScrollY(((GScrollBar) gobj).getPos());
            GForm.flush();
        }

    }
}
