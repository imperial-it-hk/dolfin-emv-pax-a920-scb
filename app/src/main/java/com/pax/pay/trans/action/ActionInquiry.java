package com.pax.pay.trans.action;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.Issuer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.model.AcqManager;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.transmit.TransProcessListener;
import com.pax.pay.trans.transmit.TransProcessListenerImpl;
import com.pax.pay.trans.transmit.Transmit;

/**
 * Created by NANNAPHAT S on 13-NOv-18.
 */

public class ActionInquiry extends AAction {

    private Context context;
    private String title;
    private TransData transData;

    /**
     * derived classes must call super(listener) to set
     *
     * @param listener {@link ActionStartListener}
     */
    public ActionInquiry(ActionStartListener listener) {
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
                TransProcessListener transProcessListenerImpl = new TransProcessListenerImpl(ActivityStack.getInstance().top());

                int ret;
//                initInquiryQr();
                //FinancialApplication.getTransDataDbHelper().insertTransData(transData);
                ret = new Transmit().transmitKbankWallet(transData,transProcessListenerImpl);
                transProcessListenerImpl.onHideProgress();
                setResult(new ActionResult(ret, null)); //TODO: Need to divide bit63 message and update into database.
            }
        });
    }

    private void initInquiryQr () {
        //TODO:need to add transData from getQR
        transData.setTransType(ETransType.QR_INQUIRY);
        AcqManager acqManager = FinancialApplication.getAcqManager();
        Acquirer acquirer = acqManager.findAcquirer(Constants.ACQ_KPLUS);
        Issuer issuer = acqManager.findIssuer(Constants.ISSUER_KPLUS);
        transData.setReversalStatus(TransData.ReversalStatus.NOREVERSE);

        transData.setBatchNo(acquirer.getCurrBatchNo());
        transData.setAcquirer(acquirer);
        transData.setIssuer(issuer);
        transData.setTpdu("600" + acquirer.getNii() + "0000");
    }

    @Override
    public void setResult(ActionResult result) {
        if (TransContext.getInstance().getCurrentAction() == null
                || isFinished()) {
            return;
        }
        TransContext.getInstance().getCurrentAction().setFinished(true); //AET-229
        TransContext.getInstance().setCurrentAction(null); //fix leaks
        super.setResult(result);
    }
}
