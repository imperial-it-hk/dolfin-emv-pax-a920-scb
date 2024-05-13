package com.pax.pay.trans.action;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.device.Device;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.service.AlipayWechatTransService;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.transmit.TransProcessListener;
import com.pax.pay.trans.transmit.TransProcessListenerImpl;
import com.pax.pay.trans.transmit.Transmit;

import static com.pax.pay.service.AlipayWechatTransService.WalletTransType.CANCEL_INQUIRY;

/**
 * Created by SORAYA S on 09-Mar-18.
 */

public class ActionQrSaleInquiry extends AAction {

    private Context context;
    private String title;
    private TransData transData;

    /**
     * derived classes must call super(listener) to set
     *
     * @param listener {@link ActionStartListener}
     */
    public ActionQrSaleInquiry(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, String title, TransData transData) {
        this.context = context;
        this.title = title;
        this.transData = transData;
    }

    @Override
    protected void process() {
        Device.enableBackKey(false);
        Device.enableHomeRecentKey(false);
        Device.enableStatusBar(false);
        FinancialApplication.getApp().runInBackground(new Runnable() {
            @Override
            public void run() {
                TransProcessListener transProcessListenerImpl = new TransProcessListenerImpl(ActivityStack.getInstance().top());

                if (isSettleFail()) {
                    setResult(new ActionResult(TransResult.ERR_SETTLE_NOT_COMPLETED, null));
                    return;
                }

                int ret;
                if (transData == null) {
                    Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_WALLET);
                    TransData adviceTrans = FinancialApplication.getTransDataDbHelper().findAdviceRecord(acquirer);
                    if (adviceTrans == null) {
                        ret = TransResult.ERR_ABORTED;
                    } else {
                        transProcessListenerImpl.onHideProgress();
                        ret = new Transmit().sendAdviceWallet(null, transProcessListenerImpl);
                    }
                } else if(transData.getAcquirer().getName().equals(Constants.ACQ_MY_PROMPT)) {
                    transProcessListenerImpl.onHideProgress();
                    ret = new Transmit().transmitMyPrompt(transData, transProcessListenerImpl);
                } else if (transData.getAcquirer().getName().equals(Constants.ACQ_QR_PROMPT)) {
                    transProcessListenerImpl.onHideProgress();
                    ret = new Transmit().transmitPromptPay(transData, transProcessListenerImpl);
                } else if (transData.getAcquirer().getName().equals(Constants.ACQ_QRC)) {
                    transProcessListenerImpl.onHideProgress();
                    ret = new Transmit().transmitQRSale(transData, transProcessListenerImpl);
                } else if (transData.getAcquirer().getName().equals(Constants.ACQ_WECHAT)
                        || transData.getAcquirer().getName().equals(Constants.ACQ_WECHAT_B_SCAN_C)
                        || transData.getAcquirer().getName().equals(Constants.ACQ_ALIPAY)
                        || transData.getAcquirer().getName().equals(Constants.ACQ_ALIPAY_B_SCAN_C)) {
                    transProcessListenerImpl.onHideProgress();
                    AlipayWechatTransService service = new AlipayWechatTransService(CANCEL_INQUIRY, transData, transProcessListenerImpl);
                    ret = service.process();
                } else if (transData.getAcquirer().getName().equals(Constants.ACQ_KPLUS)
                        || transData.getAcquirer().getName().equals(Constants.ACQ_QR_CREDIT)) {
                    transProcessListenerImpl.onHideProgress();
                    ret = new Transmit().transmitKbankWallet(transData, transProcessListenerImpl);
                } else if (transData.getAcquirer().getName().equals(Constants.ACQ_DOLFIN_INSTALMENT)) {
                    transProcessListenerImpl.onHideProgress();
                    ret = new Transmit().transmitKbankDolfin(transData, transProcessListenerImpl);
                } else {
                    transProcessListenerImpl.onHideProgress();
                    ret = new Transmit().transmitWallet(transData, transProcessListenerImpl);
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

    private boolean isSettleFail() {
        String acqName = null;
        if (transData == null) {
            acqName = Constants.ACQ_WALLET;
        } else {
            acqName = transData.getAcquirer().getName();
        }

        return Component.chkSettlementStatus(acqName);
    }
}
