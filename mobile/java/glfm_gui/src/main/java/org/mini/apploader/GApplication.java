/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.apploader;

import org.mini.gui.GCallBack;
import org.mini.gui.GForm;

/**
 * @author Gust
 */
public abstract class GApplication {

    String saveRootPath;

    public final void setSaveRoot(String path) {
        saveRootPath = path;
    }

    public final String getSaveRoot() {
        return saveRootPath;
    }


    public final void close() {
        System.out.println("Closed app : " + this);
        onClose();
        AppManager.getInstance().active();
        new RuntimeException().printStackTrace();
    }

    public final void notifyCurrentFormChanged() {
        GCallBack.getInstance().notifyCurrentFormChanged(this);
    }

    /**
     * return current form
     *
     * @return
     */
    public abstract GForm getForm();

    /**
     * AppManager notify this application will start
     */
    public void onStart() {

    }

    /**
     * AppManager notify this application will close
     */
    public void onClose() {

    }

    /**
     * AppManager notify this application pause ,eg call , app enter background
     */
    public void onPause() {

    }

    /**
     * AppManager notify this application resume from pause ,eg call end , app reactived
     */
    public void onResume() {

    }
}
