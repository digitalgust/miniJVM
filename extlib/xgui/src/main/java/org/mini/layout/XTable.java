/* SPDX-License-Identifier: MIT */

package org.mini.layout;

import org.mini.gui.GContainer;
import org.mini.gui.GObject;
import org.mini.gui.GPanel;
import static org.mini.nanovg.Nanovg.*;

public class XTable
        extends XPanel {
    static public final String XML_NAME = "table";
    private boolean border;

    public XTable(XContainer xc) {
        super(xc);
    }

    public String getXmlTag() {
        return XML_NAME;
    }

    @Override
    protected void parseMoreAttribute(String name, String value) {
        super.parseMoreAttribute(name, value);
        if ("border".equals(name)) border = !"0".equals(value) && !"false".equalsIgnoreCase(value);
    }

    @Override
    protected <T extends GObject> T createGuiImpl() {
        return (T) new GPanel(getAssist().getForm(), x, y, width, height) {
            @Override
            public boolean paint(long vg) {
                boolean result = super.paint(vg);
                if (!border || getW() <= 0 || getH() <= 0) return result;
                // Paint after cell backgrounds. Shared edges are drawn only once,
                // using the final GUI bounds so resize and multiline rows agree.
                nvgSave(vg);
                try {
                    float left = getX(), top = getY(), right = left + getW(), bottom = top + getH();
                    nvgIntersectScissor(vg, left, top, getW(), getH());
                    nvgBeginPath(vg);
                    nvgRect(vg, left + .5f, top + .5f, Math.max(0, getW() - 1), Math.max(0, getH() - 1));
                    for (XObject row : children) {
                        GContainer rowGui = row.getGui();
                        if (rowGui == null || rowGui.getH() <= 0) continue;
                        float rowTop = Math.max(top + .5f, rowGui.getY());
                        float rowBottom = Math.min(bottom - .5f, rowGui.getY() + rowGui.getH());
                        if (rowBottom <= rowTop) continue;
                        if (rowGui.getY() > top + .5f && rowGui.getY() < bottom - .5f) {
                            nvgMoveTo(vg, left + .5f, rowGui.getY() + .5f);
                            nvgLineTo(vg, right - .5f, rowGui.getY() + .5f);
                        }
                        XContainer cells = (XContainer) row;
                        for (int i = 1; i < cells.children.size(); i++) {
                            GObject cell = cells.children.get(i).getGui();
                            if (cell == null || cell.getW() <= 0) continue;
                            float edge = cell.getX() + .5f;
                            if (edge > left + .5f && edge < right - .5f) {
                                nvgMoveTo(vg, edge, rowTop);
                                nvgLineTo(vg, edge, rowBottom);
                            }
                        }
                    }
                    nvgStrokeWidth(vg, 1f);
                    nvgStrokeColor(vg, getColor());
                    nvgStroke(vg);
                } finally {
                    nvgRestore(vg);
                }
                return result;
            }
        };
    }

    boolean parseNoTagText() {
        return false;
    }


    protected void preAlignHorizontal() {
        super.preAlignHorizontal();
        if (isBlank() && raw_width == XDef.NODEF && raw_widthPercent == XDef.NODEF) {
            viewW = width = 0;
        }
    }

    protected void preAlignVertical() {
        int size = children.size();
        if (size == 1) {
            XObject xo = children.get(0);
            if (!((XContainer) xo).isBlank() && xo.raw_heightPercent == XDef.NODEF && xo.raw_height == XDef.NODEF) {
                xo.raw_heightPercent = 100;
            }
        }
        if (isBlank() && raw_height == XDef.NODEF && raw_heightPercent == XDef.NODEF) {
            viewH = height = 0;
        }
        super.preAlignVertical();

        int floatCount = 0;
        int nonFloatPix = 0;
        for (int i = 0; i < size; i++) {
            XObject xo = children.get(i);
            if (xo.vfloat) {
                floatCount++;
            } else {
                nonFloatPix += xo.height;
            }
        }

        if (viewH - nonFloatPix > 0) {
            int floatAvgH = floatCount == 0 ? 0 : (viewH - nonFloatPix) / floatCount;
            int yOffset = 0;
            for (int i = 0; i < size; i++) {
                XObject xo = children.get(i);
                xo.y += yOffset;
                if (xo.getGui() != null) xo.getGui().setLocation(xo.x, xo.y);
                if (xo.vfloat) {
                    yOffset += floatAvgH - xo.height;
//                    xo.viewH = xo.height = floatAvgH;
                    //key
                    xo.raw_heightPercent = Math.round((float) floatAvgH * 100 / viewH);
//                    xo.getGui().setSize(xo.width, xo.height);
                    int tx = xo.x;
                    int ty = xo.y;
                    //不在这里align: 外层build/reSize的align会递归整棵树,
                    //此处再align会使align=right/center的子组件偏移叠加两次
                    ((XTr) xo).reSize(this.viewW, this.viewH, false);
                    xo.x = tx;
                    xo.y = ty;
                    if (xo.getGui() != null) xo.getGui().setLocation(xo.x, xo.y);
                }
            }
        }

    }

}
