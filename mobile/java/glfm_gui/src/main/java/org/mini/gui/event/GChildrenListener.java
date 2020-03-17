package org.mini.gui.event;

import org.mini.gui.GObject;

public interface GChildrenListener {

    public void onChildAdd(GObject child);

    public void onChildRemove(GObject child);
}
