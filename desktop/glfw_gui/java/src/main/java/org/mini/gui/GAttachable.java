package org.mini.gui;

public interface GAttachable {
    void setAttachment(Object attachment);

    <T extends Object> T getAttachment();
}
