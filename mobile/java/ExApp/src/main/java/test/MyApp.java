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
                    case "MI_COLORPICKER":
                        openColorPickerFrame();
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

    /**
     * 打开颜色选择器测试 frame: 演示 GColorSelector 的拖动选色、编程预设色与实时取色。
     * 拖动色轮/三角/Alpha 条时, 下方的预览块和十六进制文本会实时刷新。
     */
    private void openColorPickerFrame() {
        final String frameName = "FRAME_COLORPICKER";
        GFrame exist = (GFrame) form.findByName(frameName);
        if (exist != null) {
            //已打开则前置并返回, 避免重复弹出
            GToolkit.showFrame(exist);
            return;
        }

        int screenW = GCallBack.getInstance().getDeviceWidth();
        int screenH = GCallBack.getInstance().getDeviceHeight();
        float fw = Math.min(360, screenW - 20);
        float fh = Math.min(460, screenH - 20);

        final GFrame cpf = new GFrame(form, "Color Picker", (screenW - fw) / 2f, (screenH - fh) / 2f, fw, fh);
        cpf.setName(frameName);

        //frame.add 会把子组件放进 frame 的 view(GViewPort), view 自身已带标题栏偏移(TITLE_HEIGHT=30),
        //所以下面坐标都相对 view 内容区左上角, y 从 5 开始留一点边距
        float pad = 5f;
        float innerW = fw - pad * 2;          //view 内可用宽度 (忽略 PAD=2 的微小误差)
        float bottomArea = 64f;               //预览块 + hex 文本 + 间距的预留高度
        float pickerH = fh - GFrame.TITLE_HEIGHT - bottomArea - pad;
        if (pickerH < 80f) pickerH = 80f;

        //颜色选择器: 上方占大部分高度
        final GColorSelector picker = new GColorSelector(form, 0f, pad, pad, innerW, pickerH);
        picker.setName("COLOR_PICKER");

        //预览块: 显示当前选中色 (含 alpha)
        float previewY = pad + pickerH + pad;
        final GPanel preview = new GPanel(form, pad, previewY, innerW, 28);
        preview.setName("COLOR_PREVIEW");
        preview.setBgColor(GColorSelector.RED);

        //十六进制文本
        final GLabel hexLabel = new GLabel(form, "", pad, previewY + 30, innerW, 24);
        hexLabel.setName("COLOR_HEX");
        hexLabel.setAlign(Nanovg.NVG_ALIGN_CENTER | Nanovg.NVG_ALIGN_MIDDLE);

        //实时取色回调: 拖动过程中持续触发
        picker.setStateChangeListener(gobj -> {
            float[] c = ((GColorSelector) gobj).getColor();
            preview.setBgColor(c);
            hexLabel.setText("ARGB: " + Integer.toHexString(GColorSelector.getHexColorARGB(c)).toUpperCase());
            GForm.flush();
        });

        //初始触发一次, 让预览与初始色 (红) 一致
        float[] initC = picker.getColor();
        preview.setBgColor(initC);
        hexLabel.setText("ARGB: " + Integer.toHexString(GColorSelector.getHexColorARGB(initC)).toUpperCase());

        cpf.add(picker);
        cpf.add(preview);
        cpf.add(hexLabel);
        GToolkit.showFrame(cpf);
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
