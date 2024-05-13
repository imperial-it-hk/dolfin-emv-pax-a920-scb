package com.pax.pay.trans.action;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.pax.abl.core.AAction;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.action.activity.VerifyTransActivity;

public class ActionVerifyTrans extends AAction {
    private Context context;
    private String title;
    private Boolean isVerifyState;
    private String amount;
    private String transID;

    public ActionVerifyTrans(ActionStartListener listener) {
        super(listener);
    }


    public void setParam(Context context, String title, Boolean isVerifyState, String amount, String transID) {
        this.context = context;
        this.title = title;
        this.isVerifyState = isVerifyState;
        this.amount = amount;
        this.transID = transID;
    }

    @Override
    protected void process() {
        FinancialApplication.getApp().runOnUiThread(new ProcessRunnable());
    }

    private class ProcessRunnable implements Runnable {
        @Override
        public void run() {
            Bundle bundle = new Bundle();
            bundle.putString(EUIParamKeys.NAV_TITLE.toString(), title);
            bundle.putBoolean(EUIParamKeys.NAV_BACK.toString(), true);
            bundle.putBoolean(EUIParamKeys.VERIFY_STATE.toString(), isVerifyState);
            bundle.putString(EUIParamKeys.TRANS_AMOUNT.toString(), amount);
            bundle.putString(EUIParamKeys.TRANS_ID.toString(), transID);

            Intent intent = new Intent(context, VerifyTransActivity.class);
            intent.putExtras(bundle);
            context.startActivity(intent);
        }
    }
}
