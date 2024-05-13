package com.pax.pay.trans.action;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Parcelable;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.MainActivity;
import com.pax.pay.SplashActivity;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.splash.SplashListener;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.action.activity.ReInitialEReceiptActivity;
import com.pax.pay.trans.action.activity.Sp200UpdateActivity;
import com.pax.pay.utils.TransResultUtils;
import com.pax.pay.utils.Utils;
import com.pax.view.dialog.DialogUtils;

public class ActionReIntialEReceipt extends AAction {
    private Context context;
    private boolean bForceDownloadCd;
    private boolean bAuth;


    public ActionReIntialEReceipt(AAction.ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context) {
        this.context = context;
    }

    @Override
    protected void process() {
        Intent intent = new Intent(context, ReInitialEReceiptActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }


}
