package org.mini.gui.event;

import org.mini.gui.GObject;

/**
 * 对拖动的响应
 */
public interface GFlyListener {

    public void flyBegin(GObject gObject, float x, float y);

    public void flying(GObject gObject, float x, float y);

    public void flyEnd(GObject gObject, float x, float y);
}
