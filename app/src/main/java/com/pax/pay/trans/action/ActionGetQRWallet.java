package com.pax.pay.trans.action;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.utils.Tools;
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

import java.util.Arrays;

public class ActionGetQRWallet extends AAction {

    private Context context;
    private TransData transData;
    private TransData transGetInfo;

    public ActionGetQRWallet(ActionStartListener listener) {
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

                transGetInfo = initGetInfoTrans();
                ret = new Transmit().transmitWallet(transGetInfo,transProcessListenerImpl);
                if (ret != TransResult.SUCC){
                    setResult(new ActionResult(TransResult.ERR_DOWNLOAD_FAILED, null));
                }else {
                    initQRInfo(transGetInfo);
                    transData.setRefNo(transGetInfo.getRefNo());
                    transData.setOrigRefNo(transGetInfo.getRefNo());
                    setResult(new ActionResult(ret, transData));
                }

                transProcessListenerImpl.onHideProgress();
            }
        });
    }

    private TransData initGetInfoTrans () {
        TransData transGetInfo = Component.transInit();
        transGetInfo.setTransType(ETransType.GET_QR_WALLET);
        AcqManager acqManager = FinancialApplication.getAcqManager();
        Acquirer acquirer = acqManager.findAcquirer(Constants.ACQ_WALLET);
        Issuer issuer = acqManager.findIssuer(Constants.ISSUER_WALLET);
        transGetInfo.setReversalStatus(TransData.ReversalStatus.NORMAL);
        transGetInfo.setCurrency(CurrencyConverter.getDefCurrency());
        transGetInfo.setBatchNo(acquirer.getCurrBatchNo());
        transGetInfo.setAcquirer(acquirer);
        transGetInfo.setIssuer(issuer);
        transGetInfo.setAmount(transData.getAmount());
        transGetInfo.setTpdu("600" + acquirer.getNii() + "0000");
        return transGetInfo;
    }

    private void initQRInfo (TransData transGetInfo) {
        byte[] field63 = transGetInfo.getField63RecByte();
        byte[] temp;
        String qrCode;

        if (field63 != null) {
            temp = Arrays.copyOfRange(field63, 2, 4);
            String tableID = Tools.bytes2String(temp);

            if (tableID.equals("QR")) {
                temp = Arrays.copyOfRange(field63, 4, field63.length);
                qrCode = Tools.bytes2String(temp);
                transData.setQrCode(qrCode);
                transGetInfo.setQrCode(qrCode);
            }
        }

    }
}
