package org.mini.layout.loader;

import org.mini.apploader.GApplication;
import org.mini.gui.GContainer;
import org.mini.layout.guilib.FormHolder;

public interface XuiAppHolder extends FormHolder {

    GApplication getApp();


    GContainer getWebView();
}
