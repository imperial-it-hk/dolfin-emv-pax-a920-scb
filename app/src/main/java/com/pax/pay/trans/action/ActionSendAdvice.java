package com.pax.pay.trans.action;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.transmit.TransProcessListener;
import com.pax.pay.trans.transmit.TransProcessListenerImpl;
import com.pax.pay.trans.transmit.Transmit;

/**
 * Created by NANNAPHAT S on 13-Dec-18.
 */

public class ActionSendAdvice extends AAction {

    private Context context;
    private String title;
    private TransData transData;
    private Acquirer acquirer;

    /**
     * derived classes must call super(listener) to set
     *
     * @param listener {@link ActionStartListener}
     */
    public ActionSendAdvice(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, String title, TransData transData, Acquirer acquirer) {
        this.context = context;
        this.title = title;
        this.transData = transData;
        this.acquirer = acquirer;
    }

    @Override
    protected void process() {

        FinancialApplication.getApp().runInBackground(new Runnable() {
            @Override
            public void run() {
                TransProcessListener transProcessListenerImpl = new TransProcessListenerImpl(ActivityStack.getInstance().top());

                int ret = TransResult.ERR_ABORTED;
                TransData adviceTrans = FinancialApplication.getTransDataDbHelper().findAdviceRecord(acquirer);
                if (adviceTrans == null) {
                    ret = TransResult.ERR_ABORTED;
                } else {
                    if (acquirer.getName().equals(Constants.ACQ_QR_PROMPT)) {
                        transProcessListenerImpl.onHideProgress();
                        ret = new Transmit().sendAdvicePromptpay(null, transProcessListenerImpl);
                    }
                }
                transProcessListenerImpl.onHideProgress();
                setResult(new ActionResult(ret, null));
            }
        });
    }

    @Override
    public void setResult(ActionResult result) {
        if (isFinished()) {
            return;
        }
        setFinished(true);
        super.setResult(result);
    }
}
