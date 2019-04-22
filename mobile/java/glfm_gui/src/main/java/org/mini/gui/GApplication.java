/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.guijni.GuiCallBack;

/**
 *
 * @author Gust
 */
public abstract class GApplication {

    String saveRootPath;

    public void setSaveRoot(String path) {
        saveRootPath = path;
    }

    public String getSaveRoot() {
        return saveRootPath;
    }

    public void close() {

    }

    public void notifyCurrentFormChanged() {
        GuiCallBack.getInstance().notifyCurrentFormChanged(this);
    }

    /**
     * return current form
     *
     * @param appins
     * @return
     */
    public abstract GForm getForm(GApplication appins);
}
