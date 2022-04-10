package org.mini.layout;

import org.mini.gui.gscript.Lib;

import java.util.Vector;

public class XmlExtAssist {

    protected Vector<String> extGuiClassName = new Vector();
    protected Vector<Lib> extScriptLibs = new Vector();


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


}
