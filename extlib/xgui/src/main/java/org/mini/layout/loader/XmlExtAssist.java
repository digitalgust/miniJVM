package org.mini.layout.loader;

import org.mini.apploader.GApplication;
import org.mini.gui.GForm;
import org.mini.gui.GImage;
import org.mini.gui.GToolkit;
import org.mini.gui.gscript.EnvVarProvider;
import org.mini.gui.gscript.Interpreter;
import org.mini.layout.XEventHandler;
import org.mini.layout.guilib.GuiScriptLib;
import org.mini.gui.gscript.Lib;
import org.mini.layout.xwebview.BrowserHolder;
import org.mini.layout.xwebview.XuiBrowser;
import org.mini.layout.xwebview.XuiScriptLib;

import java.util.Vector;

/**
 * XMLUI parse assist
 */
public class XmlExtAssist implements BrowserHolder {


    public interface XLoader {
        GImage loadImage(String path);

        String loadXml(String path, String post);
    }

    protected Vector<String> extGuiClassName = new Vector();
    protected Vector<Lib> extScriptLibs = new Vector();
    XuiAppHolder xuiAppHolder;
    protected XLoader loader;
    XEventHandler eventHandler;

    EnvVarProvider envVarProvider;

    XuiBrowser browser;

    public XmlExtAssist(XuiAppHolder xuiAppHolder) {
        if (xuiAppHolder == null || xuiAppHolder.getApp() == null) {
            throw new RuntimeException("[ERRO]app can not be null");
        }
        //if (form == null) throw new RuntimeException("Form can not be null");
        this.xuiAppHolder = xuiAppHolder;
        // the default image loader is jar image loader
        loader = new XLoader() {
            public GImage loadImage(String path) {
                return GToolkit.getCachedImageFromJar(path);
            }

            public String loadXml(String path, String post) {
                return GToolkit.readFileFromJarAsString(path, "utf-8");
            }
        };

        addExtScriptLib(new GuiScriptLib(xuiAppHolder));

        addExtScriptLib(new XuiScriptLib(this, xuiAppHolder));
    }

    public XuiAppHolder getXuiBrowserHolder() {
        return xuiAppHolder;
    }

    @Override
    public XuiBrowser getBrowser() {
        if (browser == null) {
            browser = new XuiBrowser(eventHandler, this);
        }
        return browser;
    }

    public GApplication getApp() {
        return xuiAppHolder.getApp();
    }

    public GForm getForm() {
        return xuiAppHolder.getForm();
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
        xuiAppHolder = assist.xuiAppHolder;
        envVarProvider = assist.envVarProvider;
        browser = assist.browser;
    }

    public XEventHandler getEventHandler() {
        return eventHandler;
    }

    public void setEventHandler(XEventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    public EnvVarProvider getEnvVarProvider() {
        return envVarProvider;
    }

    public void setEnvVarProvider(EnvVarProvider envVarProvider) {
        this.envVarProvider = envVarProvider;
    }

    public void interpreterSetup(Interpreter inp) {
        inp.setEnvVarProvider(envVarProvider);
        for (Lib lib : getExtScriptLibs()) {
            inp.reglib(lib);
        }
    }
}
