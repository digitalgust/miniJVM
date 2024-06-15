package org.mini.gui;

abstract public class GOpenGLPanel extends GPanel {
    protected GImage glRendereredImg;

    protected boolean glinited = false;

    GCmd cmd = new GCmd(() -> {
        if (!glinited) {
            try {
                gl_panel_init();
            } catch (Exception e) {
                e.printStackTrace();
            }
            glinited = true;
        }
        try {
            gl_paint();
        } catch (Exception e) {
            e.printStackTrace();
        }
    });

    public GOpenGLPanel(GForm form) {
        this(form, 0f, 0f, 1f, 1f);
    }

    public GOpenGLPanel(GForm form, float left, float top, float width, float height) {
        super(form);
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
        return glinited;
    }

    public boolean paint(long vg) {
        super.paint(vg);
        GForm.addCmd(cmd);
        if (glRendereredImg != null) {
            GToolkit.drawImage(vg, glRendereredImg, getX(), getY(), getW(), getH(), false, 1.f);
        }
        GForm.flush();

        return true;
    }
}
