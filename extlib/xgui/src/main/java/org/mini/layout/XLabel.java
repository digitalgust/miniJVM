/* SPDX-License-Identifier: MIT */

package org.mini.layout;

import org.mini.gui.GLabel;
import org.mini.gui.GObject;
import org.mini.layout.loader.XmlExtAssist;
import org.xmlpull.v1.KXmlParser;
import org.mini.nanovg.Nanovg;
import org.mini.gui.GToolkit;
import java.util.List;
import static org.mini.nanovg.Nanovg.*;

/**
 *
 */
public class XLabel
        extends XObject {

    static public final String XML_NAME = "label";
    // 当前绘制颜色
    protected int align = Nanovg.NVG_ALIGN_LEFT | Nanovg.NVG_ALIGN_TOP;
    protected int addon = XDef.SPACING_LABEL_ADD;
    protected boolean multiLine = false;
    protected float lineSpacing;

    GLabel label;
    List<XFlow.Part> parts;
    private boolean initialized;

    boolean flows() {
        return multiLine && raw_width == XDef.NODEF && raw_widthPercent == XDef.NODEF
                && raw_height == XDef.NODEF && raw_heightPercent == XDef.NODEF
                && (align & NVG_ALIGN_LEFT) != 0 && (align & NVG_ALIGN_TOP) != 0;
    }

    @Override
    public int getFontSize() {
        return label == null ? super.getFontSize() : (int) Math.ceil(label.getFontSize());
    }

    @Override
    protected void resetBoundle() {
        super.resetBoundle();
        parts = null;
    }

    public XLabel(XContainer xc) {
        super((xc));
    }

    public String getXmlTag() {
        return XML_NAME;
    }


    protected void parseMoreAttribute(String attName, String attValue) {
        super.parseMoreAttribute(attName, attValue);
        if (attName.equals("align")) {
            align = 0;
            for (String s : attValue.split(",")) {
                align |= XUtil.parseAlign(s);
            }
        } else if (attName.equals("addon")) {
            addon = Integer.parseInt(attValue);
        } else if (attName.equals("multiline")) {
            multiLine = "0".equals(attValue) ? false : true;
        } else if (attName.equals("linespace")) {
            lineSpacing = Float.parseFloat(attValue);
        }
    }

    @Override
    public void parse(KXmlParser parser, XmlExtAssist assist) throws Exception {
        super.parse(parser, assist);
        String tmps;
        tmps = parser.nextText(); //得到文本
        setText(tmps);
        toEndTag(parser, getXmlTag());
    }

//----------------------------------------------------------------------------
//                    内部方法
//----------------------------------------------------------------------------


    protected int getDefaultWidth(int parentViewW) {
        int w = XUtil.measureWidth(Math.max(1, parentViewW - GLabel.TEXT_BOUND_DEC), text, getFontSize(), multiLine)
                + (multiLine ? GLabel.TEXT_BOUND_DEC : addon);
        if (w > parentViewW) w = parentViewW;
        return w;
    }

    protected int getDefaultHeight(int parentViewH) {
        return XUtil.measureHeight(viewW - GLabel.TEXT_BOUND_DEC, text, getFontSize(), multiLine, lineSpacing);
    }

    protected <T extends GObject> T createGuiImpl() {
        return (T) new GLabel(getAssist().getForm(), text, x, y, width, height) {
            private void reflow() {
                XContainer root = XLabel.this.getRoot();
                if (root != null && root.getGui() != null) {
                    GObject rootGui = root.getGui();
                    float left = rootGui.getLocationLeft();
                    float top = rootGui.getLocationTop();
                    root.reflow();
                    // Reflow updates the contents, not the runtime position of
                    // a frame that has been centered or dragged after build.
                    rootGui.setLocation(left, top);
                }
            }

            @Override
            public void setText(String value) {
                String previous = getText();
                super.setText(value);
                boolean autoWidth = raw_width == XDef.NODEF && raw_widthPercent == XDef.NODEF;
                boolean autoHeight = raw_height == XDef.NODEF && raw_heightPercent == XDef.NODEF;
                if (initialized && !getText().equals(previous)) {
                    XLabel.this.text = getText();
                    if (autoWidth || autoHeight) reflow();
                }
            }

            @Override
            public void setFontSize(float value) {
                float previous = getFontSize();
                super.setFontSize(value);
                boolean autoWidth = raw_width == XDef.NODEF && raw_widthPercent == XDef.NODEF;
                boolean autoHeight = raw_height == XDef.NODEF && raw_heightPercent == XDef.NODEF;
                if (initialized && getFontSize() != previous && (autoWidth || autoHeight)) reflow();
            }

            @Override
            public void setLineSpacing(float value) {
                float previous = getLineSpacing();
                super.setLineSpacing(value);
                XLabel.this.lineSpacing = value;
                boolean autoWidth = raw_width == XDef.NODEF && raw_widthPercent == XDef.NODEF;
                boolean autoHeight = raw_height == XDef.NODEF && raw_heightPercent == XDef.NODEF;
                if (initialized && value != previous && (autoWidth || autoHeight)) reflow();
            }

            @Override
            public boolean paint(long vg) {
                if (parts == null) return super.paint(vg);
                nvgSave(vg);
                try {
                    nvgFontFace(vg, GToolkit.getFontWord());
                    nvgFontSize(vg, getFontSize());
                    nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);
                    nvgTextLineHeight(vg, 1f);
                    for (XFlow.Part p : parts) {
                        float px = getX() + p.drawX - XLabel.this.x;
                        float py = getY() + p.drawY - XLabel.this.y;
                        if (getBgImg() != null) GToolkit.drawImageStretch(vg, getBgImg(), px, py, p.w, p.h, false, getBgImgAlpha(), getCornerRadius());
                        // getBgColor() falls back to the theme even when this
                        // label has no background. Only paint an explicit color.
                        if (bgColor != null) GToolkit.drawRect(vg, px, py, p.w, p.h, bgColor);
                        nvgFillColor(vg, getColor());
                        nvgTextJni(vg, px, py - p.top, p.bytes, 0, p.bytes.length - 1);
                    }
                } finally { nvgRestore(vg); }
                return true;
            }

            @Override
            public boolean isInArea(float px, float py) {
                if (parts == null) return super.isInArea(px, py);
                for (XFlow.Part p : parts) {
                    float left = getX() + p.drawX - XLabel.this.x;
                    float top = getY() + p.drawY - XLabel.this.y;
                    if (px >= left && px < left + p.w && py >= top && py < top + p.h) return true;
                }
                return false;
            }
        };
    }

    protected void createAndSetGui() {
        if (label == null) {
            label = createGuiImpl();
            initGuiMore();
            label.setAlign(align);
            label.setShowMode(multiLine ? GLabel.MODE_MULTI_SHOW : GLabel.MODE_SINGLE_SHOW);
            label.setLineSpacing(lineSpacing);
            initialized = true;

        } else {
            label.setLocation(x, y);
            label.setSize(width, height);
        }
    }


    public GObject getGui() {
        return label;
    }
}
