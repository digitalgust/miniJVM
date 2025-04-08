package org.mini.layout;

import org.mini.gui.GObject;
import org.mini.gui.GTextObject;
import org.mini.gui.event.*;


/**
 * according to GUI system :
 * GKeyboardShowListener, GStateChangeListener, GActionListener
 */
public class XEventHandler implements
        GActionListener,
        GStateChangeListener,
        GFlyListener,
        GHrefListener,
        GChildrenListener,
        GLocationChangeListener,
        GSizeChangeListener,
        GCaretListener {

    @Override
    public void action(GObject gobj) {

    }

    @Override
    public void onStateChange(GObject gobj) {

    }

    @Override
    public void flyBegin(GObject gObject, float x, float y) {
    }

    @Override
    public void flying(GObject gObject, float x, float y) {
    }

    @Override
    public void flyEnd(GObject gObject, float x, float y) {
    }

    @Override
    public void gotoHref(GObject gobj, String href) {

    }

    @Override
    public void onChildAdd(GObject child) {

    }

    @Override
    public void onChildRemove(GObject child) {

    }

    @Override
    public void onLocationChange(float oldLeft, float oldTop, float newLeft, float newTop) {

    }

    @Override
    public void onSizeChange(int width, int height) {

    }

    @Override
    public void caretChanged(GTextObject obj, int caretIndex) {

    }
}
