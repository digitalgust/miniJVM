package org.minijvm.activity;

import android.app.NativeActivity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

/**
 * Created by gust on 2018/4/19.
 */

public class JvmNativeActivity extends NativeActivity {
    static {
        System.loadLibrary("minijvm");
    }
    ClipboardManager mClipboardManager ;
    InputMethodManager inputMethodManager;
    private final static String TAG = "JvmNativeActivity";

    // android:name="android.app.NativeActivity"
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        View v = getWindow().getDecorView();
        super.onCreate(savedInstanceState);
        mClipboardManager = mClipboardManager =
                (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        String str = event.getCharacters();
        if (str != null) {
            if (onStringInput(str)) return true;
        }
        return super.dispatchKeyEvent(event);
    }


    // Need this for screen rotation to send configuration changed callbacks to native
    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public void showKeyboard() {
        inputMethodManager.showSoftInput(this.getWindow().getDecorView(), InputMethodManager.SHOW_FORCED);
    }


    public void hideKeyboard() {
        inputMethodManager.hideSoftInputFromWindow(this.getWindow().getDecorView().getWindowToken(), 0);
    }

    public void setClipBoardContent(String str) {
        ClipData mClipData;
        mClipData = ClipData.newPlainText("", str);
        mClipboardManager.setPrimaryClip(mClipData);
    }

    public String getClipBoardContent() {
        ClipData clipData = mClipboardManager.getPrimaryClip();
        ClipData.Item item = clipData.getItemAt(0);
        String text = item.getText().toString();
        return text;
    }

    native boolean onStringInput(String str);
}
