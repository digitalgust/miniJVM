/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.util.Random;
import org.mini.nanovg.*;
import org.mini.gui.*;
import org.mini.gui.event.*;

/**
 *
 * @author gust
 */
public class App1 implements GApplication {

    private static App1 app;

    GForm form;

    static public App1 getInstance() {
        if (app == null) {
            app = new App1();
        }
        return app;
    }

    @Override
    public GForm createdForm(GuiCallBack ccb) {
        if (form != null) {
            return form;
        }
        GLanguage.setCurLang(GLanguage.ID_CHN);
        form = new GForm(/*"GuiTest"*/"登录 窗口", 800, 600, ccb);

        form.setFps(30f);
        long vg = form.getNvContext();

        GMenu menu;
        int menuH = 80;
        GImage img = new GImage("./image4.png");
        menu = new GMenu(0, form.getDeviceHeight() - menuH, form.getDeviceWidth(), menuH);
        menu.setFixed(true);
        GMenuItem item = menu.addItem("Home", img);
        item.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                GFrame gframe = new GFrame("demo", 50, 50, 300, 500);
                init(gframe.getPanel(), vg, ccb);
                form.add(gframe);
                gframe.align(GGraphics.HCENTER | GGraphics.VCENTER);
            }
        });
        menu.addItem("Search", img);
        menu.addItem("New", img);
        menu.addItem("My", img);

        form.add(menu);
        return form;
    }

    public void init(GPanel parent, final long vg, final GuiCallBack ccb) {
//        light = new Light();

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
//        String conttxt = "  \n  \n ";
        String conttxt = "子窗口This is longer chunk of text.\n  \n  Would have used lorem ipsum but she    was busy jumping over the lazy dog with the fox and all the men who came to the aid of the party.";
        conttxt += "I test the program ,there are two window , one window left a button that open the other window, the other left a button for close self.\n"
                + "\n"
                + "the issue maybe related with font , if i use nuklear defult font , the bug nerver show , but i am using chinese font (google android system default font), the bug frequently occure. the app memory using about 180M with default font in macos, use chinese font it would be 460M, is that nuklear load all glyph? but it's not the cause of bug .\n"
                + "\n"
                + "i have a reference that using stb_truetype, follow code is a stbtt test case , the code using chinese font ,that var byteOffset is -64 , out of the allocated bitmap memory . but i 'm not sure there is a same issue, only a note.";
        GTextBox cont = new GTextBox(conttxt, "Contents", x, y, 280, 188);
        parent.add(cont);
        y += 195;

        GCheckBox cbox = new GCheckBox("Remember me", true, x, y, 140, 28);
        parent.add(cbox);
        GButton sig = new GButton("Sign in", x + 138, y, 140, 28);
        sig.setBgColor(0, 96, 128, 255);
        sig.setIcon(GObject.ICON_LOGIN);
        parent.add(sig);
        sig.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                Random ran = new Random();
                GFrame sub1 = new GFrame(/*"子窗口"*/"颜色选择", 40 + ran.nextInt(100), 50 + ran.nextInt(100), 300, 600);
                GPanel panel = sub1.getPanel();
                init1(panel, vg);
                sub1.setClosable(true);
                form.add(sub1);
            }
        });
        y += 35;
        GLabel lb2 = new GLabel("Diameter", x, y, 280, 20);
        parent.add(lb2);
        y += 25;
        //drawEditBoxNum(vg, "123.00", "px", x + 180, y, 100, 28);
        GScrollBar sli = new GScrollBar(0.4f, GScrollBar.HORIZONTAL, x, y, 170, 28);
        parent.add(sli);
        y += 35;
        GButton bt1 = new GButton("Delete删除", x, y, 160, 28);
        bt1.setBgColor(128, 16, 8, 255);
        bt1.setIcon(GObject.ICON_TRASH);
        parent.add(bt1);
        GButton bt2 = new GButton("Cancel", x + 170, y, 110, 28);
        bt2.setBgColor(0, 0, 0, 0);
        parent.add(bt2);

        bt1.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                System.out.println("delete something");
            }
        });
        bt2.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                System.out.println("switch app");
                ccb.setApplication(App2.getInstance());
            }
        });
    }

    public void init1(GPanel parent, long vg) {
        GImage img = new GImage("./image4.png");

        int x = 10, y = 10;
        GList list = new GList(x, y, 280, 30);
        parent.add(list);
        if (list.getImages() == null) {
            int i = Nanovg.nvgCreateImage(vg, Gutil.toUtf8("./image4.png"), 0);
            list.setItems(new int[]{i, i, i},
                    new String[]{"One", "Two", "Three",});

        }
        y += 50;
        parent.add(new TestCanvas(x, y, 280, 150));
        y += 160;
        list = new GList(x, y, 280, 140);
        list.setMode(GList.MODE_MULTI_LINE);
        parent.add(list);
        if (list.getImages() == null) {
            int i = Nanovg.nvgCreateImage(vg, Gutil.toUtf8("./image4.png"), 0);
            list.setItems(new int[]{i, i, i, i, i, i, i, i, i, i},
                    new String[]{"One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",});
        }
        y += 150;
        GColorSelector cs = new GColorSelector(0, x, y, 130, 130);
        parent.add(cs);

    }

    class TestCanvas extends GCanvas {

//        GLFrameBuffer glfb;
//        GLFrameBufferPainter glfbRender;
        GImage img3D;

        public TestCanvas(int x, int y, int w, int h) {
            super(x, y, w, h);
//            glfb = new GLFrameBuffer(300, 300);
//            glfbRender = new GLFrameBufferPainter() {
//                @Override
//                public void paint() {
//                    light.setCamera();
//                    light.draw();
//                }
//            };
//            img3D = new GImage(glfb.getTexture(), glfb.getWidth(), glfb.getHeight());
        }

        int pos = 0, delta = 1;

        public void paint(GGraphics g) {
            g.setColor(0xff000000);
            g.fillRect(0, 0, (int) getW(), (int) getH());
            g.setColor(0xff0000ff);
            g.drawLine(0, 100, 100, 100);
            pos += delta;
            if (pos > 50) {
                delta = -1;
            }
            if (pos < 0) {
                delta = 1;
            }
            g.drawString("this is a canvas", pos, 50, GGraphics.TOP | GGraphics.LEFT);
//            glfb.render(glfbRender);
//            g.drawImage(img3D, 0, 0, 100, 100, GGraphics.TOP | GGraphics.LEFT);
        }
    }
}
