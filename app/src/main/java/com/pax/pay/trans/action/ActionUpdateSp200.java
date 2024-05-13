package com.pax.pay.trans.action;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.pay.MainActivity;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.action.activity.Sp200UpdateActivity;
import com.pax.pay.utils.TransResultUtils;
import com.pax.view.dialog.DialogUtils;

public class ActionUpdateSp200 extends AAction {
    private Context context;
    private boolean bForceDownloadCd;
    private boolean bAuth;
    private ActionEndListener endListener;


    public ActionUpdateSp200(ActionStartListener listener) {
        super(listener);
    }

    @Override
    public void setEndListener(ActionEndListener endListener) {
        this.endListener = endListener;
    }

    public void setParam(Context context) {
        this.context = context;
    }

    @Override
    protected void process() {
        FinancialApplication.getApp().setUpdateSp200Action(this);
        Intent intent = new Intent(context, Sp200UpdateActivity.class);
        //intent.setFlags(Intent.);
        context.startActivity(intent);
    }

    @Override
    public void setResult(ActionResult result) {
        super.setResult(result);
        if (endListener!=null) {
            endListener.onEnd(this, result);
        }
    }

    //    public void setLocalEndListener(DialogInterface.OnDismissListener  onDismissListener) {
//        ActionEndListener actionEndListener = new ActionEndListener() {
//            @Override
//            public void onEnd(AAction action, ActionResult result) {
//                if (result.getRet() != 0) {
//                    DialogUtils.showErrMessage(context, null, TransResultUtils.getMessage(result.getRet()), onDismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
//                } else {
//                    DialogUtils.showSuccMessage(context, "", onDismissListener, Constants.SUCCESS_DIALOG_SHOW_TIME);
//                }
//            }
//        };
//        super.setEndListener(actionEndListener);
//    }

}
