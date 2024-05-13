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
import com.pax.pay.record.TransQueryActivity;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.action.activity.SettleActivity;

import th.co.bkkps.bpsapi.BaseRequest;
import th.co.bkkps.bpsapi.BaseResponse;
import th.co.bkkps.bpsapi.ITransAPI;
import th.co.bkkps.bpsapi.ReprintTotalMsg;
import th.co.bkkps.bpsapi.SettingMsg;
import th.co.bkkps.bpsapi.SettleMsg;
import th.co.bkkps.bpsapi.TransAPIFactory;
import th.co.bkkps.bpsapi.TransResult;
import th.co.bkkps.scbapi.ScbIppService;
import th.co.bkkps.scbapi.trans.action.ActionScbIppLink;

public class ScbIppLinkActivity  extends AppCompatActivity  {

    private ITransAPI transAPI;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!ScbIppService.isSCBInstalled(this)) {
            finish(new ActionResult(TransResult.ERR_ABORTED, null));
            return;
        }

        String acquirerName = getIntent().getStringExtra(EUIParamKeys.SCB_ACQ_NAME.toString());

        SettleMsg.Request settleReq = new SettleMsg.Request();
        settleReq.setAcquirerName(acquirerName);
        transAPI = TransAPIFactory.createTransAPI();
        transAPI.startTrans(this, settleReq);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d("ScbIppLinkActivity", "BpsApi onActivityResult invoked");
        SettleMsg.Response response = (SettleMsg.Response) transAPI.onResult(requestCode, resultCode, data);

        if (response != null) {
            Log.d("BpsApi", "getRspCode=" + response.getRspCode());
            finish(new ActionResult(response.getRspCode(), null));
            return;
        }
        Log.d("BpsApi", "response is null");

        finish(new ActionResult(TransResult.ERR_PARAM, null));
    }

    public void finish(ActionResult result) {
        AAction action = TransContext.getInstance().getCurrentAction();
        if (action != null) {
            if (action.isFinished())
                return;
            action.setFinished(true);
            action.setResult(result);
        }
        finish();
    }
}
