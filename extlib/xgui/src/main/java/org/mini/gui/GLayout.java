package org.mini.gui;

import org.mini.layout.XContainer;

public interface GLayout {
    /**
     * @param parent
     */
    void setParent(GLayout parent);

    <T extends GLayout> T getParent();

    /**
     * 重新布局
     *
     * @param parentW
     * @param parentH
     */
    void reSize(int parentW, int parentH);
}
