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
import th.co.bkkps.bpsapi.SaleIPPMsg;
import th.co.bkkps.bpsapi.TransAPIFactory;
import th.co.bkkps.bpsapi.TransResult;

public class ScbIppSaleActivity extends AppCompatActivity {

    ITransAPI transAPI;
    SaleIPPMsg.Request saleReq;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        long type = getIntent().getLongExtra(EUIParamKeys.SCB_IPP_TYPE.toString(), 0);
        saleReq = new SaleIPPMsg.Request();
        saleReq.setIppType(type);
        transAPI = TransAPIFactory.createTransAPI();
        transAPI.startTrans(this, saleReq);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d("ScbIppSaleActivity", "BpsApi onActivityResult invoked");
        SaleIPPMsg.Response saleIPPResp = (SaleIPPMsg.Response) transAPI.onResult(requestCode, resultCode, data);
        if (saleIPPResp != null) {
            Log.d("BpsApi", "getAuthCode="+saleIPPResp.getAuthCode());
            Log.d("BpsApi", "getRefNo="+saleIPPResp.getRefNo());
            Log.d("BpsApi", "getStanNo="+saleIPPResp.getStanNo());
            Log.d("BpsApi", "getVoucherNo="+saleIPPResp.getVoucherNo());
            finish(new ActionResult(saleIPPResp.getRspCode(), saleIPPResp));
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
