package org.minijvm.activity.wxapi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import org.minijvm.activity.JvmNativeActivity;

import java.util.HashMap;
import java.util.Map;

public class WXEntryActivity extends Activity implements IWXAPIEventHandler {

    private IWXAPI api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handle(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handle(intent);
    }

    private void handle(Intent intent) {
        String appId = JvmNativeActivity.sWxAppId;
        if (appId == null || appId.length() == 0) {
            finish();
            return;
        }
        api = WXAPIFactory.createWXAPI(this, appId, true);
        api.registerApp(appId);
        if (!api.handleIntent(intent, this)) {
            finish();
        }
    }

    @Override
    public void onReq(BaseReq baseReq) {
    }

    @Override
    public void onResp(BaseResp baseResp) {
        Map<String, String> result = new HashMap<>();
        result.put("errCode", String.valueOf(baseResp.errCode));
        result.put("errStr", baseResp.errStr == null ? "" : baseResp.errStr);
        if (baseResp instanceof SendAuth.Resp) {
            SendAuth.Resp auth = (SendAuth.Resp) baseResp;
            result.put("code", auth.code == null ? "" : auth.code);
            result.put("state", auth.state == null ? "" : auth.state);
        }
        JvmNativeActivity.completeWechatAuth(result);
        finish();
    }
}
