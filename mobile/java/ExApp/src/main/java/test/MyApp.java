package test;

import org.mini.apploader.GApplication;
import org.mini.gui.*;
import org.mini.gui.callback.GCallBack;
import org.mini.layout.XContainer;
import org.mini.layout.XEventHandler;
import org.mini.layout.loader.UITemplate;
import org.mini.layout.loader.XmlExtAssist;
import org.mini.layout.loader.XuiAppHolder;
import org.mini.nanovg.Nanovg;
import test.ext.ExScriptLib;
import test.ext.GCustomList;

import java.util.ArrayList;
import java.util.List;

/**
 * @author gust
 */
public class MyApp extends GApplication implements XuiAppHolder {

    GForm form;
    GMenu menu;
    GFrame gframe;

    @Override
    public void onInit() {
        //set the default language
        GLanguage.setCurLang(GLanguage.ID_CHN);

        //load xml
        String xmlStr = GToolkit.readFileFromJarAsString("/res/MyForm.xml", "utf-8");

        UITemplate uit = new UITemplate(xmlStr);
        uit.getVarMap().put("Cancel", "CANCEL"); //replace keywork in xml
        uit.getVarMap().put("Change", "Change");
        uit.getVarMap().put("Test", "Test");
        uit.getVarMap().put("Exit", "QUIT");
        XContainer xc = (XContainer) XContainer.parseXml(uit.parse(), new XmlExtAssist(this));
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
                        XmlExtAssist assist = new XmlExtAssist(MyApp.this);
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
                    case "BT_SET_BLUE": {
                        GTextBox tb = GToolkit.getComponent(form, "INPUT_AREA");
                        if (tb != null && tb.isSelected()) {
                            int start = tb.getSelectBegin();
                            int end = tb.getSelectEnd();
                            float[] blue = Nanovg.nvgRGBAf(0.3f, 0.5f, 1.f, 1.f);
                            tb.addStyle(start, end - start, blue);
                            System.out.println("--------------------\n" + tb.getStyleJson());
                            GToolkit.saveDataToFile(getSaveRoot() + "/style.json", tb.getStyleJson().getBytes());
                        }
                        break;
                    }
                    case "BT_CLEAR_STYLE": {
                        GTextBox tb = GToolkit.getComponent(form, "INPUT_AREA");
                        if (tb != null && tb.isSelected()) {
                            int selStart = tb.getSelectBegin();
                            int selEnd = tb.getSelectEnd();

                            List<GTextBox.StyleRun> oldStyles = tb.getStyles();
                            List<GTextBox.StyleRun> newStyles = new ArrayList<>();

                            for (GTextBox.StyleRun run : oldStyles) {
                                int runStart = run.getStart();
                                int runEnd = run.getStart() + run.getLength();

                                // No overlap
                                if (runEnd <= selStart || runStart >= selEnd) {
                                    newStyles.add(run);
                                    continue;
                                }

                                // Overlap exists. We need to calculate what parts of the run remain.

                                // Part of the run before the selection
                                if (runStart < selStart) {
                                    newStyles.add(new GTextBox.StyleRun(runStart, selStart - runStart, run.getColor()));
                                }

                                // Part of the run after the selection
                                if (runEnd > selEnd) {
                                    newStyles.add(new GTextBox.StyleRun(selEnd, runEnd - selEnd, run.getColor()));
                                }
                            }
                            tb.setStyles(newStyles);
                        }
                        break;
                    }
                }
            }

            public void onStateChange(GObject gobj, String cmd) {
            }
        });
        form = xc.getGui();
        setForm(form);
        gframe = form.findByName("FRAME_TEST");
        if (gframe != null) gframe.align(Nanovg.NVG_ALIGN_CENTER | Nanovg.NVG_ALIGN_MIDDLE);
        menu = (GMenu) form.findByName("MENU_MAIN");

        GTextBox tb = GToolkit.getComponent(form, "INPUT_AREA");
        String s = GToolkit.readFileFromFileAsString(getSaveRoot() + "/style.json", "utf-8");
        tb.setStyleJson(s);

        //process Hori screen or Vert screen
        //if screen size changed ,then ui will resized relative
        form.setSizeChangeListener((width, height) -> {
            if (gframe != null && gframe.getLayout() != null) {
                form.getLayout().reSize(width, height);
                gframe.align(Nanovg.NVG_ALIGN_CENTER | Nanovg.NVG_ALIGN_MIDDLE);
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
