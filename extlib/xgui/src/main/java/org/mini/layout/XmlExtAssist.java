package org.mini.layout;

import org.mini.gui.GForm;
import org.mini.gui.GImage;
import org.mini.gui.GToolkit;
import org.mini.gui.GuiScriptLib;
import org.mini.gui.gscript.Lib;

import java.util.Vector;

/**
 * XMLUI parse assist
 */
public class XmlExtAssist {


    public interface XLoader {
        GImage loadImage(String path);

        String loadXml(String path);
    }

    protected Vector<String> extGuiClassName = new Vector();
    protected Vector<Lib> extScriptLibs = new Vector();
    GForm form;
    protected XLoader loader;
    XEventHandler eventHandler;

    public XmlExtAssist(GForm form) {
        //if (form == null) throw new RuntimeException("Form can not be null");
        this.form = form;
        // the default image loader is jar image loader
        loader = new XLoader() {
            public GImage loadImage(String path) {
                return GToolkit.getCachedImageFromJar(path);
            }

            public String loadXml(String path) {
                return GToolkit.readFileFromJarAsString(path, "utf-8");
            }
        };

        addExtScriptLib(new GuiScriptLib(form));
    }

    public GForm getForm() {
        return form;
    }

    public void setForm(GForm form) {
        this.form = form;
    }

    public void registerGUI(String guiClassName) {
        if (!extGuiClassName.contains(guiClassName)) {
            extGuiClassName.addElement(guiClassName);
        }
    }

    public void unregisterGUI(String guiClassName) {
        extGuiClassName.removeElement(guiClassName);
    }


    public Vector<String> getExtGuiClassName() {
        return extGuiClassName;
    }

    public Vector<Lib> getExtScriptLibs() {
        return extScriptLibs;
    }


    public void removeExtScriptLibs(Lib lib) {
        extScriptLibs.remove(lib);
    }

    public void addExtScriptLib(Lib lib) {
        if (!extScriptLibs.contains(lib)) extScriptLibs.add(lib);
    }

    public XLoader getLoader() {
        return loader;
    }

    public void setLoader(XLoader loader) {
        this.loader = loader;
    }

    public GImage loadImage(String path) {
        if (path == null) return null;
        return loader.loadImage(path);
    }


    public void copyFrom(XmlExtAssist assist) {
        extGuiClassName.addAll(assist.extGuiClassName);
        extScriptLibs.addAll(assist.extScriptLibs);
        loader = assist.loader;
        form = assist.form;
    }

    XEventHandler getEventHandler() {
        return eventHandler;
    }

    void setEventHandler(XEventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }
}
