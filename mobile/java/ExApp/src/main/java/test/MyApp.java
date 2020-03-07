package test;

import org.mini.apploader.AppManager;
import org.mini.gui.*;
import org.mini.gui.event.*;

/**
 *
 * @author gust
 */
public class MyApp extends GApplication {

    GForm form;
    GMenu menu;
    GFrame gframe;

    @Override
    public GForm getForm() {
        if (form != null) {
            return form;
        }
        GLanguage.setCurLang(GLanguage.ID_CHN);
        form = new GForm();

        form.setFps(30f);
        long vg = form.getNvContext();

        int menuH = 80;
        GImage img = GImage.createImageFromJar("/res/hello.png");
        menu = new GMenu(0, form.getDeviceHeight() - menuH, form.getDeviceWidth(), menuH);
        menu.setFixed(true);
        GMenuItem item = menu.addItem("Hello World", img);
        item.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                if (gframe != null) {
                    gframe.close();
                }
                gframe = getFrame1();
                form.add(gframe);
                gframe.align(GGraphics.HCENTER | GGraphics.VCENTER);
            }
        });

        img = GImage.createImageFromJar("/res/appmgr.png");
        item = menu.addItem("Exit to AppManager", img);
        item.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                AppManager.getInstance().active();
            }
        });

        form.add(menu);
        return form;
    }

    public GFrame getFrame1() {

        GFrame gframe = new GFrame("Hello World", 50, 50, form.getDeviceWidth() * .8f, (form.getDeviceHeight() - menu.getH()) * .7f);
        GViewPort parent = gframe.getView();
        float pad = 8;
        float x = pad, y = 10;
        float btnH = 28;

        String conttxt = "  This app is an example of mini_jvm, Threre are a menu and a frame .\n"
                + "  Touch the 'Exit to AppManager' , you will enter the AppManager, AppManager manage all app, it can upload ,download , delete app.\n"
                + "  1. DOWNLOAD : Put your jar in a website , then input the url of jar in AppManager, Touch 'Download' ,it would download the jar ,then update the app list.\n"
                + "  2. UPLOAD : The first you touch the 'Start' to open the inapp webserver, then open browser in your Desktop Computer, open 'http://phone_ip_addr:8088' , and pickup a jar in the page, upload it.  NOTE: That computer and the phone must be same LAN.\n"
                + "  3. RUN : Touch the App name in the list, Touch 'Run' can start the app.\n "
                + "  4. SET AS BOOT APP : The boot app will startup when MiniPack opend. \n"
                + "  5. UPGRADE : AppManager will download the new jar ,url that get from config.txt in jar.\n"
                + "  6. DELETE : The app would be deleteted.\n";
        GTextBox cont = new GTextBox(conttxt, "Contents", x, y, parent.getW() - x * 2, parent.getH() - pad * 2 - btnH - y);
        cont.setEditable(false);
        parent.add(cont);
        y += cont.getH() + pad;

        GButton bt2 = new GButton("Cancel", x + 170, y, 110, btnH);
        bt2.setBgColor(0, 0, 0, 0);
        bt2.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                gobj.getForm().remove(gframe);
            }
        });
        parent.add(bt2);

        return gframe;
    }

}
