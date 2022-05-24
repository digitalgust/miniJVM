package org.mini.gui;

abstract public class GOpenGLPanel extends GPanel {
    protected GImage glRendereredImg;

    protected boolean inited = false;

    GCmd cmd = new GCmd(() -> {
        if (!inited) {
            try {
                gl_panel_init();
            } catch (Exception e) {
                e.printStackTrace();
            }
            inited = true;
        }
        try {
            gl_paint();
        } catch (Exception e) {
            e.printStackTrace();
        }
    });

    public GOpenGLPanel() {
        this(0f, 0f, 1f, 1f);
    }

    public GOpenGLPanel(int left, int top, int width, int height) {
        this((float) left, top, width, height);
    }

    public GOpenGLPanel(float left, float top, float width, float height) {
        setLocation(left, top);
        setSize(width, height);
    }

    abstract public void gl_paint();

    abstract public void gl_init();

    abstract public void gl_destroy();

    private void gl_panel_init() {
        gl_init();
    }

    protected void finalize() {
        GForm.addCmd(new GCmd(() -> {
            try {
                gl_destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("GOpenGLPanel clean success");
        }));
    }

    public void setGlRendereredImg(GImage img) {
        glRendereredImg = img;
    }

    public GImage getGlRendereredImg() {
        return glRendereredImg;
    }

    public boolean isGLInited() {
        return inited;
    }

    public boolean paint(long vg) {
        GForm.addCmd(cmd);
        if (glRendereredImg != null) {
            GToolkit.drawImage(vg, glRendereredImg, getX(), getY(), getW(), getH(), false, 1.f);
        }
        GObject.flush();

        return super.paint(vg);
    }
}
