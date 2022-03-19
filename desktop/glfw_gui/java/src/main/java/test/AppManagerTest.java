package test;

import org.mini.apploader.AppManager;
import org.mini.gui.GCallBack;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author gust
 */
public class AppManagerTest  {

    public static void main(String[] args) {
        GCallBack ccb = GCallBack.getInstance();
        ccb.init(700, 320);//window size  568 320
        //ccb.init(812, 375);//ip 12 pro max

        AppManager.getInstance().active();

        ccb.mainLoop();
        ccb.destroy();
    }
}
