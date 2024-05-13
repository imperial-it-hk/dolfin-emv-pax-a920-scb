package com.pax.pay.trans.action.activity;

import android.os.Bundle;
import th.co.bkkps.utils.Log;
import android.view.MenuItem;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.BaseActivity;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.pack.qr.PackQr;
import com.pax.pay.utils.TickTimer;

/**
 * Created by NANNAPHAT S on 07-Mar-19.
 */

public abstract class BaseKplusShowQrActivity extends BaseActivity {
    protected TickTimer tickTimer;
    protected PackQr packQr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        tickTimer = new TickTimer(new TickTimer.OnTickTimerListener() {
            @Override
            public void onTick(long leftTime) {
                Log.i(TAG, "onTick:" + leftTime);
            }

            @Override
            public void onFinish() {
                onTimerFinish();
            }
        });
        Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_KPLUS);
        tickTimer.start(acquirer.getPromptQrTimeout());
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tickTimer.stop();
    }

    public void finish(ActionResult result) {
        tickTimer.stop();
        AAction action = TransContext.getInstance().getCurrentAction();
        if (action != null) {
            if (action.isFinished())
                return;
            action.setFinished(true);
            quickClickProtection.start(); // AET-93
            action.setResult(result);
        } else {
            finish();
        }
    }

    @Override
    protected boolean onKeyBackDown() {
        finish(new ActionResult(TransResult.ERR_ABORTED, null));
        return true;
    }

    @Override
    protected boolean onOptionsItemSelectedSub(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
            return true;
        }
        return super.onOptionsItemSelectedSub(item);
    }

    protected void onTimerFinish() {
        if (packQr!=null) {
            finish(new ActionResult(TransResult.ERR_TIMEOUT, packQr.getQrRef2()));
        }else {
            finish(new ActionResult(TransResult.ERR_TIMEOUT, null));
        }

    }
}
