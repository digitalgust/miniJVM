package org.mini.layout;

import org.mini.gui.GObject;
import org.mini.gui.event.GActionListener;
import org.mini.gui.event.GFlyListener;
import org.mini.gui.event.GHrefListener;
import org.mini.gui.event.GStateChangeListener;


/**
 * according to GUI system :
 * GKeyboardShowListener, GStateChangeListener, GActionListener
 */
public class XEventHandler implements GActionListener, GStateChangeListener, GFlyListener, GHrefListener {

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
}
