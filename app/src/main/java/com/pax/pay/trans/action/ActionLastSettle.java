package com.pax.pay.trans.action;

import android.app.Activity;
import com.pax.abl.core.AAction;
import com.pax.edc.R;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.record.Printer;
import com.pax.view.dialog.DialogUtils;

import static com.pax.edc.opensdk.TransResult.*;
import static com.pax.pay.utils.Utils.getString;

/**
 * Created by WITSUTA A on 4/26/2018.
 */

public class ActionLastSettle extends AAction {

    private Activity activity;
    private Acquirer acquirer;


    public ActionLastSettle(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Activity activity, Acquirer acquirer) {
        this.activity = activity;
        this.acquirer = acquirer;
    }

    @Override
    protected void process() {
        FinancialApplication.getApp().runInBackground(new Runnable() {
            @Override
            public void run() {
                int result = Printer.printLastSettlement(activity,acquirer);
                if (result != SUCC) {
                    DialogUtils.showErrMessage(activity,
                            getString(R.string.dialog_print), getString(R.string.err_no_trans),
                            null, Constants.FAILED_DIALOG_SHOW_TIME);
                }
            }
        });
    }
}
