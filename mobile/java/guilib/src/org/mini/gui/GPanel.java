/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

/**
 *
 * @author gust
 */
public class GPanel extends GContainer {

    float scroll_h = 0;//0-1 区间,描述窗口滚动条件位置, 滚动符0-1分别对应文本顶部超出显示区域的高度百分比
    float scroll_v = 0;//0-1 区间,描述窗口滚动条件位置, 滚动符0-1分别对应文本顶部超出显示区域的高度百分比

    @Override
    public void scrollEvent(double scrollX, double scrollY, int x, int y) {
        if (focus != null) {
            focus.scrollEvent(scrollX, scrollY, x, y);
        } else if (parent != null) {
            if (boundle[WIDTH] > parent.getW() || boundle[HEIGHT] > parent.getH()) {

            }
        }
    }
}
