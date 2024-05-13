package com.pax.pay.trans.action;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.Issuer;
import com.pax.pay.constant.Constants;
import com.pax.pay.service.AlipayWechatTransService;
import com.pax.pay.trans.model.AcqManager;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.transmit.TransProcessListener;
import com.pax.pay.trans.transmit.TransProcessListenerImpl;
import com.pax.pay.trans.transmit.Transmit;

import static com.pax.pay.service.AlipayWechatTransService.WalletTransType.SALE_INQUIRY;

/**
 * Created by NANNAPHAT S on 04-Feb-19.
 */

public class ActionInquiryWechat extends AAction {

    private Context context;
    private String title;
    private TransData transData;

    /**
     * derived classes must call super(listener) to set
     *
     * @param listener {@link ActionStartListener}
     */
    public ActionInquiryWechat(ActionStartListener listener) {
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
                /*
                TransProcessListener transProcessListenerImpl = new TransProcessListenerImpl(ActivityStack.getInstance().top());

                int ret;
                initInquiryQr(transData);
                //FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                ret = new Transmit().transmitKbankWallet(transData,transProcessListenerImpl);
                transProcessListenerImpl.onHideProgress();
                setResult(new ActionResult(ret, null)); //TODO: Need to divide bit63 message and update into database.
                */
                TransProcessListener listener = new TransProcessListenerImpl(context);
                initInquiryQr(transData);
                AlipayWechatTransService service = new AlipayWechatTransService(SALE_INQUIRY, transData, listener);
                int ret = service.process();
                setResult(new ActionResult(ret, null));
            }
        });
    }

    private void initInquiryQr (TransData transData) {
        //TODO:need to add transData from getQR
        transData.setTransType(ETransType.QR_INQUIRY_WECHAT);
        AcqManager acqManager = FinancialApplication.getAcqManager();
        Acquirer acquirer = acqManager.findAcquirer(Constants.ACQ_WECHAT);
        Issuer issuer = acqManager.findIssuer(Constants.ISSUER_WECHAT);
        transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
        transData.setBatchNo(acquirer.getCurrBatchNo());
        transData.setAcquirer(acquirer);
        transData.setIssuer(issuer);
        transData.setTpdu("600" + acquirer.getNii() + "0000");

    }
}
