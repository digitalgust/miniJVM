/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.apploader;

import org.mini.gui.*;

/**
 * @author Gust
 */
public abstract class GApplication {
    public enum AppState {
        STATE_INITED, STATE_STARTED, STATE_PAUSEED, STATE_CLOSED,
    }

    private AppState state = AppState.STATE_INITED;

    String saveRootPath;
    GStyle oldStyle;
    GStyle myStyle;
    int myLang;

    void setOldStyle(GStyle style) {
        oldStyle = style;
    }

    public final void setSaveRoot(String path) {
        saveRootPath = path;
    }

    public final String getSaveRoot() {
        return saveRootPath;
    }

    public AppState getState() {
        return state;
    }

    public void setState(AppState state) {
        this.state = state;
    }

    public final void startApp() {
        setState(AppState.STATE_STARTED);
        try {
            onStart();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public final void closeApp() {
        if (getState() == AppState.STATE_CLOSED) return;
        System.out.println("Closed app : " + this);
        try {
            onClose();
        } catch (Exception e) {
            e.printStackTrace();
        }
        GLanguage.clear();
        GToolkit.setStyle(oldStyle);
        AppManager.getInstance().active();
        GForm.addCmd(new GCmd(() -> {
            Thread.currentThread().setContextClassLoader(null);
        }));
        setState(AppState.STATE_CLOSED);
    }

    public final void pauseApp() {
        if (getState() == AppState.STATE_PAUSEED || getState() == AppState.STATE_CLOSED) return;
        setState(AppState.STATE_PAUSEED);
        myStyle = GToolkit.getStyle();
        GToolkit.setStyle(oldStyle);
        myLang = AppLoader.getDefaultLang();
        try {
            onPause();
        } catch (Exception e) {
            e.printStackTrace();
        }
        AppManager.getInstance().active();
    }

    public final void resumeApp() {
        if (getState() != AppState.STATE_PAUSEED) return;
        setState(AppState.STATE_STARTED);
        oldStyle = GToolkit.getStyle();
        GToolkit.setStyle(myStyle);
        GLanguage.setCurLang(myLang);
        try {
            onResume();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
