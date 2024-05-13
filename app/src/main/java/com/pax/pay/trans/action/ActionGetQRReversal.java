package com.pax.pay.trans.action;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.trans.action.activity.CheckQRAlipayActivity;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.transmit.TransProcessListener;
import com.pax.pay.trans.transmit.TransProcessListenerImpl;
import com.pax.pay.trans.transmit.Transmit;

/**
 * Created by NANNAPHAT S on 28-NOv-18.
 */

public class ActionGetQRReversal extends AAction {

    private Context context;
    private String title;
    private TransData transData;
    private int lastresult;

    /**
     * derived classes must call super(listener) to set
     *
     * @param listener {@link ActionStartListener}
     */
    public ActionGetQRReversal(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, String title, TransData transData) {
        this.context = context;
        this.title = title;
        this.transData = transData;
    }

    @Override
    protected void process() {

        FinancialApplication.getApp().runInBackground(new Runnable() {
            @Override
            public void run() {
                TransProcessListener transProcessListenerImpl = new TransProcessListenerImpl(context);
                int ret = new Transmit().sendReversalQR(transProcessListenerImpl, transData.getAcquirer());
                transProcessListenerImpl.onHideProgress();
                if(ret == TransResult.SUCC) {
                    setResult(new ActionResult(TransResult.ERR_USER_CANCEL, null));
                } else {
                    setResult(new ActionResult(ret, null));
                }
            }
        });
    }

//    private void initInquiryQr (TransData transData) {
//        //TODO:need to add transData from getQR
//        transData.setTransType(ETransType.QR_INQUIRY);
//        AcqManager acqManager = FinancialApplication.getAcqManager();
//        Acquirer acquirer = acqManager.findAcquirer(Constants.ACQ_WALLET);
//        Issuer issuer = acqManager.findIssuer(Constants.ISSUER_WALLET);
//        transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
//        transData.setBatchNo(acquirer.getCurrBatchNo());
//        transData.setAcquirer(acquirer);
//        transData.setIssuer(issuer);
//        transData.setTpdu("600" + acquirer.getNii() + "0000");
//
//    }
}
