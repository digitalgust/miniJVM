package test;

import org.mini.apploader.AppManager;
import org.mini.gui.*;
import org.mini.gui.event.GSizeChangeListener;
import org.mini.layout.*;

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
        GLanguage.setCurLang(GLanguage.ID_CHN);

        String xmlStr = GToolkit.readFileFromJarAsString("/res/MyForm.xml", "utf-8");

        UITemplate uit = new UITemplate(xmlStr);
        for (String key : uit.getVariable()) {
            uit.setVar(key, GLanguage.getString(key));
        }
        XContainer xc = (XContainer) XContainer.parseXml(uit.parse());
        xc.build(GCallBack.getInstance().getDeviceWidth(), GCallBack.getInstance().getDeviceHeight(), new XEventHandler() {
            public void action(GObject gobj, String cmd) {
                String name = gobj.getName();
                if ("MI_OPENFRAME".equals(name)) {
                    if (form.findByName("FRAME_TEST") == null) {
                        form.add(gframe);
                    }
                } else if ("MI_EXIT".equals(name)) {
                    AppManager.getInstance().active();
                } else if ("BT_CANCEL".equals(name)) {
                    gframe.close();
                }
            }

            public void onStateChange(GObject gobj, String cmd) {
            }
        });
        form = (GForm) xc.getGui();
        gframe = (GFrame) form.findByName("FRAME_TEST");
        if (gframe != null) gframe.align(GGraphics.HCENTER | GGraphics.VCENTER);
        menu = (GMenu) form.findByName("MENU_MAIN");
        form.setSizeChangeListener(new GSizeChangeListener() {
            @Override
            public void onSizeChange(int width, int height) {
                if (gframe != null && gframe.getAttachment() != null
                        && (gframe.getAttachment() instanceof XFrame)) {
                    ((XContainer) form.getAttachment()).reSize(width, height);
                    if (gframe != null) gframe.align(GGraphics.HCENTER | GGraphics.VCENTER);
                }
            }
        });
        return form;
    }
}
