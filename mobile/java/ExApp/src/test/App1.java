/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.util.Random;
import org.mini.gl.warp.GLFrameBuffer;
import org.mini.gl.warp.GLFrameBufferPainter;
import org.mini.gui.GButton;
import org.mini.gui.GCanvas;
import org.mini.gui.GCheckBox;
import org.mini.gui.GColorSelector;
import org.mini.gui.GForm;
import org.mini.gui.GFrame;
import org.mini.gui.GGraphics;
import org.mini.gui.GImage;
import org.mini.gui.GTextField;
import org.mini.gui.GLabel;
import org.mini.gui.GList;
import org.mini.gui.GMenu;
import org.mini.gui.GObject;
import org.mini.gui.GPanel;
import org.mini.gui.GScrollBar;
import org.mini.gui.GTextBox;
import org.mini.gui.GuiCallBack;
import org.mini.gui.event.GActionListener;
import static org.mini.nanovg.Gutil.toUtf8;
import org.mini.nanovg.Nanovg;
import org.mini.gui.GApplication;
import org.mini.gui.GLanguage;
import org.mini.gui.GMenuItem;

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

        y += 35;
        GButton bt5 = new GButton("long button", x, y, 160, 28);
        bt5.setBgColor(128, 16, 8, 255);
        bt5.setIcon(GObject.ICON_TRASH);
        parent.add(bt5);

        bt1.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                System.out.println("delete something");
                //menu.setPos(menu.getX(), menu.getY() - 20);
//                if (menu.getY() < 0) {
//                    menu.setPos(menu.getX(), form.getDeviceHeight() - menu.getH());
//                }
                String gf3_tb_txt = "剧情场景切换时偶尔蹦出的模型场景残留，和不正常UI，特别特别影响气氛\n"
                        + "开场和大姐npc距离太近了，导致后面的剧情切换显得比较不自然，如果离得远一些还会好一些\n"
                        + "剧情字幕和角色介绍文字纯白色看起来有点费劲，建议增加一些阴影描边\n"
                        + "剧情里部分角色没有影子，显得比较飘\n"
                        + "第二波山贼是等着玩家过去打的，没有主动进攻性，建议主动跑过来攻击玩家，增加玩家大开杀戒的合理性\n"
                        + "引导的黑屏有点过于黑，太暴力，不过这是个人看法\n"
                        + "失魂洞刚出生的地方周围障碍就非常容易阻碍视线或者造成大范围视角变化\n"
                        + "第一次打大粽子的时候丢贴图和动作，第二次打的时候，动作间隙里大粽子会卡住定帧，还会丢失战斗bgm和音效\n"
                        + "地上的草远看其实效果还行，但是部分剧情里面有特写镜头，拉近就很露怯了\n"
                        + "和山贼的第一场战斗后没有引导方向和行动，可能会有点懵逼（失魂洞里也卡住过）\n"
                        + "开局第一波虫子生命略多 \n"
                        + "玩了几个小时，感觉游戏整体性、打击效果非常好、场景的昼夜切换都是非常不错的，在副本中尝试了一下3D视角和2.5D视角，3D效果在打怪的时候晃的比较厉害导致不好对怪进行瞄准，以及上来打1-2关的大粽子boss的时候，由于场地空间狭小，视野非常受限。2.5D体验的还好只是部分地形会挡住视野。体验的是冲锋枪，希望后期可以有更多种武器选择，使用不同的武器有不同的技能效果，以及增加一些体术技能，冲锋枪枪的技能效果大多都是击退效果，可以加一些击倒效果。游戏闪退的次数较多，以及游戏帧数较低，总体体验还是很好的，非常期待更多优化和更新！ "//
                        + "玩了几个小时，感觉游戏整体性、打击效果非常好、场景的昼夜切换都是非常不错的，在副本中尝试了一下3D视角和2.5D视角，3D效果在打怪的时候晃的比较厉害导致不好对怪进行瞄准，以及上来打1-2关的大粽子boss的时候，由于场地空间狭小，视野非常受限。2.5D体验的还好只是部分地形会挡住视野。体验的是冲锋枪，希望后期可以有更多种武器选择，使用不同的武器有不同的技能效果，以及增加一些体术技能，冲锋枪枪的技能效果大多都是击退效果，可以加一些击倒效果。游戏闪退的次数较多，以及游戏帧数较低，总体体验还是很好的，非常期待更多优化和更新！ "//
                        + "玩了几个小时，感觉游戏整体性、打击效果非常好、场景的昼夜切换都是非常不错的，在副本中尝试了一下3D视角和2.5D视角，3D效果在打怪的时候晃的比较厉害导致不好对怪进行瞄准，以及上来打1-2关的大粽子boss的时候，由于场地空间狭小，视野非常受限。2.5D体验的还好只是部分地形会挡住视野。体验的是冲锋枪，希望后期可以有更多种武器选择，使用不同的武器有不同的技能效果，以及增加一些体术技能，冲锋枪枪的技能效果大多都是击退效果，可以加一些击倒效果。游戏闪退的次数较多，以及游戏帧数较低，总体体验还是很好的，非常期待更多优化和更新！ "//
                        + "玩了几个小时，感觉游戏整体性、打击效果非常好、场景的昼夜切换都是非常不错的，在副本中尝试了一下3D视角和2.5D视角，3D效果在打怪的时候晃的比较厉害导致不好对怪进行瞄准，以及上来打1-2关的大粽子boss的时候，由于场地空间狭小，视野非常受限。2.5D体验的还好只是部分地形会挡住视野。体验的是冲锋枪，希望后期可以有更多种武器选择，使用不同的武器有不同的技能效果，以及增加一些体术技能，冲锋枪枪的技能效果大多都是击退效果，可以加一些击倒效果。游戏闪退的次数较多，以及游戏帧数较低，总体体验还是很好的，非常期待更多优化和更新！ "//
                        ;
                GFrame gf3 = new GFrame("文字", 0, 0, form.getDeviceWidth(), form.getDeviceHeight());
                GTextBox gf3_tb = new GTextBox(gf3_tb_txt, "", 0, 0, (int) gf3.getW(), (int) gf3.getH() - 80);
                gf3.add(gf3_tb);
                form.add(gf3);
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
            int i = Nanovg.nvgCreateImage(vg, toUtf8("./image4.png"), 0);
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
            int i = Nanovg.nvgCreateImage(vg, toUtf8("./image4.png"), 0);
            list.setItems(new int[]{i, i, i, i, i, i, i, i, i, i},
                    new String[]{"One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",});
        }
        y += 150;
        GColorSelector cs = new GColorSelector(0, x, y, 130, 130);
        parent.add(cs);

    }

    class TestCanvas extends GCanvas {

        GLFrameBuffer glfb;
        GLFrameBufferPainter glfbRender;
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
