
package test;

import java.io.File;
import org.mini.glfm.Glfm;
import org.mini.gui.*;
import org.mini.gui.event.*;
import org.mini.gui.impl.GuiCallBack;

/**
 *
 * @author gust
 */
public class MyApp implements GApplication {

    private static MyApp app;

    GForm form;
    GMenu menu;

    static public MyApp getInstance() {
        if (app == null) {
            app = new MyApp();
        }
        return app;
    }

    @Override
    public GForm createdForm(GuiCallBack ccb) {
        if (form != null) {
            return form;
        }
        GLanguage.setCurLang(GLanguage.ID_CHN);
        form = new GForm(ccb);

        form.setFps(30f);
        long vg = form.getNvContext();

        int menuH = 80;
        GImage img = GImage.createImageFromJar(form.getNvContext(), "/res/mini_jvm_64.png");
        menu = new GMenu(0, form.getDeviceHeight() - menuH, form.getDeviceWidth(), menuH);
        menu.setFixed(true);
        GMenuItem item = menu.addItem("Login", img);
        item.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                GFrame gframe = getFrame1();
                form.add(gframe);
                gframe.align(GGraphics.HCENTER | GGraphics.VCENTER);
            }
        });
        item = menu.addItem("Select", img);
        item.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                GFrame gframe = getFrame2();
                form.add(gframe);
                gframe.align(GGraphics.VCENTER | GGraphics.HCENTER);
            }
        });
        item = menu.addItem("File", img);
        item.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                GFrame gframe = getFrame3();
                form.add(gframe);
                gframe.align(GGraphics.VCENTER | GGraphics.HCENTER);
            }
        });

        form.add(menu);
        return form;
    }

    public GFrame getFrame1() {
        GFrame gframe = new GFrame("Login", 50, 50, 300, 500);
        GContainer parent = gframe.getView();
        int x = 8, y = 10;
        GTextField gif = new GTextField("", "search", x, y, 280, 25);
        gif.setBoxStyle(GTextField.BOX_STYLE_SEARCH);
        parent.add(gif);
        y += 30;
        GLabel lb1 = new GLabel("Login", x, y, 280, 20);
        parent.add(lb1);
        y += 25;
        GTextField mail = new GTextField("", "Email", x, y, 280, 28);
        parent.add(mail);
        y += 35;
        GTextField pwd = new GTextField("", "Password", x, y, 280, 28);
        parent.add(pwd);
        y += 35;

        String conttxt = "Features:\n"
                + "Jvm Build pass: iOS / Android / mingww64 32 64bit / cygwin / MSVC 32 64bit / MacOS / Linux .\n"
                + "No dependence Library .\n"
                + "Low memory footprint .\n"
                + "Minimal runtime classlib .\n"
                + "Support java5/6/7/8 class file version .\n"
                + "Support embedded java source compiler(janino compiler) .\n"
                + "Thread supported .\n"
                + "Network supported .\n"
                + "File io supported .\n"
                + "Java native method supported .\n"
                + "Java garbage collection supported .\n"
                + "Java remote debug supported, JDWP Spec .";
        GTextBox cont = new GTextBox(conttxt, "Contents", x, y, 280, 188);
        parent.add(cont);
        y += 195;

        GCheckBox cbox = new GCheckBox("Remember me", true, x, y, 140, 28);
        parent.add(cbox);
        GButton sig = new GButton("Sign in", x + 138, y, 140, 28);
        sig.setBgColor(0, 96, 128, 255);
        sig.setIcon(GObject.ICON_LOGIN);
        parent.add(sig);

        y += 35;
        GLabel lb2 = new GLabel("Diameter", x, y, 280, 20);
        parent.add(lb2);
        y += 25;
        GScrollBar sli = new GScrollBar(0.4f, GScrollBar.HORIZONTAL, x, y, 170, 28);
        parent.add(sli);
        y += 35;
        GButton bt1 = new GButton("Delete删除", x, y, 160, 28);
        bt1.setBgColor(128, 16, 8, 255);
        bt1.setIcon(GObject.ICON_TRASH);
        parent.add(bt1);
        GButton bt2 = new GButton("Cancel", x + 170, y, 110, 28);
        bt2.setBgColor(0, 0, 0, 0);
        bt2.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                gobj.getForm().remove(gframe);
            }
        });
        parent.add(bt2);

        bt1.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                System.out.println("delete something");
            }
        });
        return gframe;
    }

    public GFrame getFrame2() {
        GFrame gframe = new GFrame("Select", 0, 0, 300, 550);
        GContainer parent = gframe.getView();
        GImage img = GImage.createImageFromJar(form.getNvContext(), "/res/logo128.png");

        int x = 10, y = 10;
        GList list = new GList(x, y, 280, 30);
        parent.add(list);
        list.setItems(new GImage[]{img, img, img, img, img, img, img, img, img, img},
                new String[]{"One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",});

        y += 50;
        parent.add(new TestCanvas(x, y, 280, 150));
        y += 160;
        list = new GList(x, y, 280, 140);
        list.setShowMode(GList.MODE_MULTI_SHOW);
        parent.add(list);
        list.setItems(new GImage[]{img, img, img, img, img, img, img, img, img, img},
                new String[]{"One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",});

        y += 150;
        GColorSelector cs = new GColorSelector(0, x, y, 130, 130);
        parent.add(cs);
        return gframe;
    }

    class TestCanvas extends GCanvas {

        GImage img;

        public TestCanvas(int x, int y, int w, int h) {
            super(x, y, w, h);
        }

        int pos = 0, delta = 1;

        public void paint(GGraphics g) {
            g.setColor(0xff000000);
            g.fillRect(0, 0, (int) getW(), (int) getH());
            g.setColor(0xff0000ff);
            g.drawLine(20, 100, 100, 100);
            pos += delta;
            if (pos > 50) {
                delta = -1;
            }
            if (pos < 0) {
                delta = 1;
            }

            g.setColor(0xffff00ff);
            g.drawString("this is a canvas", pos, 50, GGraphics.TOP | GGraphics.LEFT);

            g.setColor(0xff00ff00);
            g.drawLine(20, 50, 100, 50);

            if (img == null) {
                img = GImage.createImageFromJar(g.getNvContext(), "/res/logo128.png");
            }
            g.drawImage(img, 130, 30, 100, 100, GGraphics.TOP | GGraphics.LEFT);
            form.flush();
        }
    }

    public GFrame getFrame3() {
        GFrame gframe = new GFrame("File", 0, 0, form.getDeviceWidth() - 40, (form.getDeviceHeight() - menu.getH() - 150));

        GList list = new GList(0, 0, (int) gframe.getView().getW(), (int) (gframe.getView().getH()));
        list.setShowMode(GList.MODE_MULTI_SHOW);
        list.setSelectMode(GList.MODE_MULTI_SHOW);
        gframe.getView().add(list);

        String resRoot = Glfm.glfmGetResRoot();
        File f = new File(resRoot);
        if (f.exists()) {
            String[] files = f.list();
            GImage[] imgs = new GImage[files.length];
            list.setItems(imgs, files);
        }
        list.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                GList glist = (GList) gobj;
                System.out.println(glist.getSelectedIndex());
            }
        });
        return gframe;
    }
}
