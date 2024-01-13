package org.lwjgl.input;

public class Mouse {
    void create();
    void destroy();
    int getDX();
    int getDY();
    int getEventButton();
    boolean getEventButtonState();
    boolean next();
    void setGrabbed(boolean);
}
