package org.minijvm.activity.wxapi;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import org.minijvm.activity.JvmNativeActivity;

public class WXPayEntryActivity extends Activity implements IWXAPIEventHandler {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (JvmNativeActivity.sWxApi == null && JvmNativeActivity.sWxAppId != null) {
            JvmNativeActivity.sWxApi = WXAPIFactory.createWXAPI(this, JvmNativeActivity.sWxAppId, true);
            JvmNativeActivity.sWxApi.registerApp(JvmNativeActivity.sWxAppId);
        }
        if (JvmNativeActivity.sWxApi != null) {
            JvmNativeActivity.sWxApi.handleIntent(getIntent(), this);
        } else {
            finish();
        }
    }

    @Override
    public void onReq(BaseReq baseReq) {
    }

    @Override
    public void onResp(BaseResp baseResp) {
        if (baseResp.getType() == ConstantsAPI.COMMAND_PAY_BY_WX) {
            int code = baseResp.errCode;
            Log.i("wxpay", "code=" + code + ", msg=" + baseResp.errStr);
            Toast.makeText(this, code == 0 ? "Pay success" : "Pay failed: " + code, Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}

