package org.mini.gui.event;

import org.mini.gui.GTextObject;

public interface GCaretListener {
    public void caretChanged(GTextObject obj, int caretIndex);
}
