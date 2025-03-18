package test;

import org.mini.apploader.GApplication;
import org.mini.gui.*;
import org.mini.gui.callback.GCallBack;
import org.mini.layout.loader.UITemplate;
import org.mini.layout.XContainer;
import org.mini.layout.XEventHandler;
import org.mini.layout.loader.XmlExtAssist;
import org.mini.layout.loader.XuiAppHolder;
import test.ext.SimplePanel;

/**
 * @author gust
 */
public class MyGame extends GApplication implements XuiAppHolder {

    GForm form;
    GMenu menu;

    @Override
    public void onInit() {
        //set the default language
        GLanguage.setCurLang(GLanguage.ID_CHN);

        //load xml
        String xmlStr = GToolkit.readFileFromJarAsString("/res/GameForm.xml", "utf-8");

        UITemplate uit = new UITemplate(xmlStr);
        XmlExtAssist assist = new XmlExtAssist(this);
        assist.registerGUI("test.ext.XSimplePanel");
        XContainer xc = (XContainer) XContainer.parseXml(uit.parse(), assist);
        int screenW = GCallBack.getInstance().getDeviceWidth();
        int screenH = GCallBack.getInstance().getDeviceHeight();

        //build gui with event handler
        xc.build(screenW, screenH, new XEventHandler() {
            @Override
            public void action(GObject gobj) {
                String name = gobj.getName();
                if (name == null) return;
                switch (name) {
                    case "MI_EXIT":
                        closeApp();
                        break;
                }
            }

            public void onStateChange(GObject gobj, String cmd) {
            }
        });
        form = xc.getGui();
        setForm(form);
        menu = (GMenu) form.findByName("MENU_MAIN");

        //process Hori screen or Vert screen
        //if screen size changed ,then ui will resized relative
        form.setSizeChangeListener((width, height) -> {
            SimplePanel sp = form.findByName("GLP_SIMPLE");
            if (sp != null) {
                sp.reSize();
            }
        });
    }

    @Override
    public GApplication getApp() {
        return this;
    }

    @Override
    public GContainer getWebView() {
        return null;
    }
}
