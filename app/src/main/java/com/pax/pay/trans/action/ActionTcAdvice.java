package com.pax.pay.trans.action;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.transmit.TransProcessListener;
import com.pax.pay.trans.transmit.TransProcessListenerImpl;
import com.pax.pay.trans.transmit.Transmit;

/**
 * Created by SORAYA S on 25-Feb-19.
 */

public class ActionTcAdvice extends AAction {
    private TransProcessListener transProcessListenerImpl;
    private Context context;
    private TransData transData;

    /**
     * derived classes must call super(listener) to set
     *
     * @param listener {@link ActionStartListener}
     */
    public ActionTcAdvice(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, TransData transData) {
        this.context = context;
        this.transData = transData;
    }

    @Override
    protected void process() {
        FinancialApplication.getApp().runInBackground(new Runnable() {
            @Override
            public void run() {
                int ret;
                transProcessListenerImpl = new TransProcessListenerImpl(context);
                Acquirer acquirer = transData.getAcquirer();
                TransData originalTransData = FinancialApplication.getTransDataDbHelper().findAdviceRecord(acquirer);
                if (Constants.ACQ_SMRTPAY.equals(acquirer.getName()) || Constants.ACQ_SMRTPAY_BDMS.equals(acquirer.getName())) {
                    ret = new Transmit().sendTcAdviceInstalmentKbank(originalTransData, acquirer, transProcessListenerImpl);
                } else {
                    ret = new Transmit().sendTcAdvice(originalTransData, acquirer, transProcessListenerImpl);
                }
                updateAdviceStatus(ret, originalTransData);
                transProcessListenerImpl.onHideProgress();
                setResult(new ActionResult(ret, null));
            }
        });
    }

    private void updateAdviceStatus(int ret, TransData adviceTrans) {
        if (ret == TransResult.SUCC && adviceTrans != null) {
            if (adviceTrans.getTraceNo() == transData.getTraceNo()) {//if current transaction
                transData.setAdviceStatus(TransData.AdviceStatus.NORMAL);
                FinancialApplication.getTransDataDbHelper().updateTransData(transData);
            } else {//another transaction
                adviceTrans.setAdviceStatus(TransData.AdviceStatus.NORMAL);
                FinancialApplication.getTransDataDbHelper().updateTransData(adviceTrans);
            }
        }
    }
}
