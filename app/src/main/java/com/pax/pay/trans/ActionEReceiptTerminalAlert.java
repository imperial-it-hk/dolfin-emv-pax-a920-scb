package com.pax.pay.trans;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.transmit.Online;
import com.pax.pay.trans.transmit.TransProcessListener;
import com.pax.pay.trans.transmit.TransProcessListenerImpl;

public class ActionEReceiptTerminalAlert extends AAction {

    private Context context;
    private TransData transData;

    /**
     * derived classes must call super(listener) to set
     *
     * @param listener {@link ActionStartListener}
     */
    public ActionEReceiptTerminalAlert(ActionStartListener listener) {
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
                TransProcessListener transProcessListenerImpl = new TransProcessListenerImpl(ActivityStack.getInstance().top());

                TransData tmAlertTrans = new TransData(transData);

                Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_ERCM_RECEIPT_MANAGEMENT_SERVICE);
                acquirer.setTerminalId(transData.getAcquirer().getTerminalId());
                acquirer.setMerchantId(transData.getAcquirer().getMerchantId());

                Component.transInit(tmAlertTrans, acquirer);

                tmAlertTrans.setStanNo(transData.getStanNo());
                tmAlertTrans.setTraceNo(transData.getTraceNo());
                tmAlertTrans.setBatchNo(transData.getBatchNo());
                tmAlertTrans.setTransType(ETransType.ERCEIPT_TERM_ALERT);

                transProcessListenerImpl.onUpdateProgressTitle(tmAlertTrans.getTransType().getTransName());

                new Online().online(tmAlertTrans, transProcessListenerImpl);

                transProcessListenerImpl.onHideProgress();
                setResult(new ActionResult(TransResult.SUCC, null));
            }
        });
    }
}
