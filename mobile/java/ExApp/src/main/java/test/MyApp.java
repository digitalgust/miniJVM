package test;

import org.mini.apploader.GApplication;
import org.mini.gui.*;
import org.mini.gui.callback.GCallBack;
import org.mini.layout.UITemplate;
import org.mini.layout.XContainer;
import org.mini.layout.XEventHandler;
import org.mini.layout.XmlExtAssist;
import org.mini.nanovg.Nanovg;
import test.ext.ExScriptLib;
import test.ext.GCustomList;

/**
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
        //set the default language
        GLanguage.setCurLang(GLanguage.ID_CHN);

        //load xml
        String xmlStr = GToolkit.readFileFromJarAsString("/res/MyForm.xml", "utf-8");

        UITemplate uit = new UITemplate(xmlStr);
        uit.getVarMap().put("Cancel", "CANCEL"); //replace keywork in xml
        uit.getVarMap().put("Change", "Change");
        uit.getVarMap().put("Test", "Test");
        uit.getVarMap().put("Exit", "QUIT");
        XContainer xc = (XContainer) XContainer.parseXml(uit.parse(), new XmlExtAssist(null));
        int screenW = GCallBack.getInstance().getDeviceWidth();
        int screenH = GCallBack.getInstance().getDeviceHeight();

        //build gui with event handler
        xc.build(screenW, screenH, new XEventHandler() {
            @Override
            public void action(GObject gobj) {
                String name = gobj.getName();
                if (name == null) return;
                switch (name) {
                    case "MI_OPENFRAME":
                        if (form.findByName("FRAME_TEST") == null) {
                            form.add(gframe);
                        }
                        break;
                    case "MI_OPENFRAME1":
                        XmlExtAssist assist = new XmlExtAssist(form);
                        assist.registerGUI("test.ext.XCustomList");
                        assist.addExtScriptLib(new ExScriptLib());
                        String xmlStr = GToolkit.readFileFromJarAsString("/res/Frame1.xml", "utf-8");
                        UITemplate uit = new UITemplate(xmlStr);
                        XContainer xc = (XContainer) XContainer.parseXml(uit.parse(), assist);
                        xc.build((int) form.getW(), (int) form.getH(), this);
                        GFrame f1 = xc.getGui();
                        GToolkit.closeFrame(form, f1.getName());
                        GToolkit.showFrame(f1);

                        GCustomList customList = GToolkit.getComponent(f1, "CUSTLIST");
                        customList.addItem(null, "CustomList Item1");
                        customList.addItem(null, "CustomList Item2");
                        break;
                    case "MI_EXIT":
                        closeApp();
                        break;
                    case "BT_CANCEL":
                        gframe.close();
                        break;
                }
            }

            public void onStateChange(GObject gobj, String cmd) {
            }
        });
        form = xc.getGui();
        gframe = form.findByName("FRAME_TEST");
        if (gframe != null) gframe.align(Nanovg.NVG_ALIGN_CENTER | Nanovg.NVG_ALIGN_MIDDLE);
        menu = (GMenu) form.findByName("MENU_MAIN");

        //process Hori screen or Vert screen
        //if screen size changed ,then ui will resized relative
        form.setSizeChangeListener((width, height) -> {
            if (gframe != null && gframe.getLayout() != null) {
                form.getLayout().reSize(width, height);
                gframe.align(Nanovg.NVG_ALIGN_CENTER | Nanovg.NVG_ALIGN_MIDDLE);
            }
        });
        return form;
    }
}
