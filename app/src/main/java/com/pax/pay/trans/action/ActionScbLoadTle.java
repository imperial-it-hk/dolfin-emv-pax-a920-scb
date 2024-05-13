package com.pax.pay.trans.action;

import android.content.Context;
import android.content.Intent;

import com.pax.abl.core.AAction;
import com.pax.device.TerminalEncryptionParam;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Bank;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.action.activity.ScbLoadTleActivity;

import java.util.ArrayList;

public class ActionScbLoadTle extends AAction {
    /**
     * derived classes must call super(listener) to set
     *
     * @param listener {@link ActionStartListener}
     */
    public ActionScbLoadTle(ActionStartListener listener) {
        super(listener);
    }

    private Context context = null;
    private String strJsonTE =null;
    private ArrayList<String> scbSelectAcquirer;

    public void setParam(Context context, String strJsonTE, ArrayList<String> scbSelectAcquirer) {
        this.context = context;
        this.strJsonTE = strJsonTE;
        this.scbSelectAcquirer = scbSelectAcquirer;
    }

    @Override
    protected void process() {
        TransContext.getInstance().setCurrentAction(this);
        Intent intent = new Intent(context, ScbLoadTleActivity.class);
        if (strJsonTE == null) {
            TerminalEncryptionParam param = FinancialApplication.getUserParam().getTEParam(Bank.SCB);
            if (param != null) {
                strJsonTE = "{\"TLE\" : [{\"BANK_NAME\": \"SCB\",\"TE_ID\": \"" + param.getId() + "\",\"TE_PIN\": \"" + param.getPin() + "\"}]}";
            }
        }
        intent.putExtra("SCB_JSON_TEID", strJsonTE);
        intent.putStringArrayListExtra("SCB_SELECT_ACQ", scbSelectAcquirer);
        context.startActivity(intent);
    }
}
