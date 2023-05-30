package org.mini.layout;

import org.mini.gui.GObject;
import org.mini.gui.event.GActionListener;
import org.mini.gui.event.GFlyListener;
import org.mini.gui.event.GStateChangeListener;


/**
 * according to GUI system :
 * GKeyboardShowListener, GStateChangeListener, GActionListener
 */
public class XEventHandler implements GActionListener, GStateChangeListener, GFlyListener {

    public void action(GObject gobj) {

    }

    public void onStateChange(GObject gobj) {

    }

    public void flyBegin(GObject gObject, float x, float y) {
    }

    public void flying(GObject gObject, float x, float y) {
    }

    public void flyEnd(GObject gObject, float x, float y) {
    }
}
