package th.co.bkkps.scbapi.trans.action.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.pay.MainActivity;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.TransContext;

import th.co.bkkps.bpsapi.ITransAPI;
import th.co.bkkps.bpsapi.TransAPIFactory;
import th.co.bkkps.bpsapi.TransResult;
import th.co.bkkps.bpsapi.VoidMsg;

public class ScbIppVoidActivity extends AppCompatActivity {

    ITransAPI transAPI;
    VoidMsg.Request voidReq;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        long traceNo = getIntent().getLongExtra(EUIParamKeys.SCB_TRACE_NO.toString(), 0);
        voidReq = new VoidMsg.Request();
        voidReq.setVoucherNo((int) traceNo);
        transAPI = TransAPIFactory.createTransAPI();
        transAPI.startTrans(this, voidReq);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d("ScbIppVoidActivity", "BpsApi onActivityResult invoked");
        VoidMsg.Response voidRsp = (VoidMsg.Response) transAPI.onResult(requestCode, resultCode, data);
        if (voidRsp != null) {
            Log.d("BpsApi", "getAuthCode="+voidRsp.getAuthCode());
            Log.d("BpsApi", "getRefNo="+voidRsp.getRefNo());
            Log.d("BpsApi", "getStanNo="+voidRsp.getStanNo());
            Log.d("BpsApi", "getVoucherNo="+voidRsp.getVoucherNo());
            finish(new ActionResult(voidRsp.getRspCode(), voidRsp));
            return;
        }
        Log.d("BpsApi", "response is null");
        finish(new ActionResult(TransResult.ERR_ABORTED, null));
    }

    public void finish(ActionResult result) {
        AAction action = TransContext.getInstance().getCurrentAction();
        if (action != null) {
            if (action.isFinished())
                return;
            action.setFinished(true);
            action.setResult(result);
        }
        ActivityStack.getInstance().popTo(MainActivity.class);
        finish();
    }
}
