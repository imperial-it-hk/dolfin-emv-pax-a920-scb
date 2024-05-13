package com.pax.pay.trans.action;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.Issuer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.AcqManager;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.transmit.TransProcessListener;
import com.pax.pay.trans.transmit.TransProcessListenerImpl;
import com.pax.pay.trans.transmit.Transmit;
import com.pax.pay.utils.CurrencyConverter;

public class ActionGetQrCreditFromServer extends AAction {

    private Context context;
    private TransData transData;

    /**
     * derived classes must call super(listener) to set
     *
     * @param listener {@link ActionStartListener}
     */
    public ActionGetQrCreditFromServer(ActionStartListener listener) {
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
                TransProcessListener transProcessListenerImpl = new TransProcessListenerImpl(context);

                int ret;

                initGetInfoTrans();
                ret = new Transmit().transmitKbankWallet(transData, transProcessListenerImpl);
                if (ret != TransResult.SUCC){
                    setResult(new ActionResult(TransResult.ERR_FAIL_GET_QR, null));
                } else {
                    //  split field 63 to trans data
                    splitField63();
                    transData.setReversalStatus(TransData.ReversalStatus.PENDING);
                    transData.setTransType(ETransType.QR_INQUIRY_CREDIT);
                    FinancialApplication.getTransDataDbHelper().insertTransData(transData); //test
                    setResult(new ActionResult(ret, null));
                }

                transProcessListenerImpl.onHideProgress();
            }
        });
    }

    private void initGetInfoTrans () {
        transData.setTransType(ETransType.GET_QR_CREDIT);
        AcqManager acqManager = FinancialApplication.getAcqManager();
        Acquirer acquirer = acqManager.findAcquirer(Constants.ACQ_QR_CREDIT);
        transData.setReversalStatus(TransData.ReversalStatus.NOREVERSE);
        transData.setWalletRetryStatus(TransData.WalletRetryStatus.NORMAL);
        transData.setTransState(TransData.ETransStatus.NORMAL);
        transData.setCurrency(CurrencyConverter.getDefCurrency());
        transData.setBatchNo(acquirer.getCurrBatchNo());
        transData.setAcquirer(acquirer);
        transData.setDupReason("06");
        transData.setTpdu("600" + acquirer.getNii() + "0000");
    }

    private void splitField63 (){
        byte[] field63RecByte = transData.getField63RecByte();
        if (field63RecByte != null) {
            Component.splitField63Wallet(transData, field63RecByte);
        }
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
