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
import com.pax.pay.service.AlipayWechatTransService;
import com.pax.pay.trans.WechatQrSaleTrans;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.AcqManager;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.transmit.TransProcessListener;
import com.pax.pay.trans.transmit.TransProcessListenerImpl;
import com.pax.pay.trans.transmit.Transmit;
import com.pax.pay.utils.CurrencyConverter;

import java.util.Arrays;

import static com.pax.pay.service.AlipayWechatTransService.WalletTransType.QR_INQUIRY;

/**
 * Created by Nannaphat S on 4-Feb-19.
 */

public class ActionGetQrWechatFromServer extends AAction {

    private Context context;
    //private String title;
    private TransData transData;

    // Field 63 definition
    private String TableLen;
    private String Currency;
    private String PartnerTxnID;
    private String TxnID;
    private String PayTime;
    private String ExchRate;
    private String TxnAmtCNY;
    private String TxnAmt;
    private String BuyerUserID;
    private String BuyerLoginID;
    private String MerInfo;
    private String AppCode;
    private String Promocode;
    private String TxnNo;
    private String Fee;
    private String QRCode;

    /**
     * derived classes must call super(listener) to set
     *
     * @param listener {@link ActionStartListener}
     */
    public ActionGetQrWechatFromServer(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, TransData transData) {
        this.context = context;
       // this.title = title;
        this.transData = transData;
    }

    @Override
    protected void process() {
//        initTransDataQr();

        FinancialApplication.getApp().runInBackground(new Runnable() {
            @Override
            public void run() {
                /*
                TransProcessListener transProcessListenerImpl = new TransProcessListenerImpl(context);
                //TransProcessListener transProcessListenerImpl = new TransProcessListenerImpl(ActivityStack.getInstance().top());

                int ret;

                //transProcessListenerImpl.onHideProgress();
                initGetInfoTrans(); //test
                ret = new Transmit().transmitKbankWallet(transData, transProcessListenerImpl);
                if (ret != TransResult.SUCC){
                    setResult(new ActionResult(TransResult.ERR_DOWNLOAD_FAILED, null));
                }else {
                    //  split field 63 to trans data
                    splitField63(transData);
                    transData.setReversalStatus(TransData.ReversalStatus.PENDING);
                    transData.setTransType(ETransType.QR_INQUIRY_WECHAT);
                    FinancialApplication.getTransDataDbHelper().insertTransData(transData); //test
                    setResult(new ActionResult(ret, transData));
                }

                transProcessListenerImpl.onHideProgress();
//                ActionResult result = new ActionResult(ret, transData.getField63());
//                setResult(result);
                */

                TransProcessListener listener = new TransProcessListenerImpl(context);
                initGetInfoTrans();
                AlipayWechatTransService service = new AlipayWechatTransService(QR_INQUIRY, transData, listener);
                int ret = service.process();
                setResult(new ActionResult(ret, null));
            }
        });
    }

    private void initTransDataQr(){

        AcqManager acqManager = FinancialApplication.getAcqManager();
        Acquirer acquirer = acqManager.findAcquirer(Constants.ACQ_WECHAT);
        Issuer issuer = acqManager.findIssuer(Constants.ISSUER_WECHAT);
        transData.setBatchNo(acquirer.getCurrBatchNo());
        // 冲正原因
        transData.setTransState(TransData.ETransStatus.NORMAL);
        transData.setAcquirer(acquirer);
        transData.setIssuer(issuer);
        transData.setTpdu("600" + acquirer.getNii() + "0000");
        transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
        transData.setWalletRetryStatus(TransData.WalletRetryStatus.NORMAL);
    }

    private void initGetInfoTrans () {
        transData.setTransType(ETransType.GET_QR_WECHAT);
        AcqManager acqManager = FinancialApplication.getAcqManager();
        Acquirer acquirer = acqManager.findAcquirer(Constants.ACQ_WECHAT);
        Issuer issuer = acqManager.findIssuer(Constants.ISSUER_WECHAT);
        transData.setTransState(TransData.ETransStatus.NORMAL);
        transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
        transData.setCurrency(CurrencyConverter.getDefCurrency());
        transData.setBatchNo(acquirer.getCurrBatchNo());
        transData.setAcquirer(acquirer);
        transData.setIssuer(issuer);
        transData.setTpdu("600" + acquirer.getNii() + "0000");
    }

    private void splitField63 (TransData transGetInfo){
        byte[] field63RecByte = transGetInfo.getField63RecByte();
        if (field63RecByte != null) {
            Component.splitField63Wallet(transData, field63RecByte);
//            splitMainField(field63RecByte);
//            splitQrCode(qrCode);
//            splitInfoField(qrField30);
//            transData.setQrRef2(infoField3);
//            transData.setQrID(qrID);


        }
    }

    private void splitMainField (byte[] field63RecByte) {
        byte[] temp;
//        temp = Arrays.copyOfRange(field63RecByte, 0, 3);
//        TableLen = Tools.bcd2Str(temp);

        temp = Arrays.copyOfRange(field63RecByte, 0, 3);
        Currency = Tools.bytes2String(temp);

        temp = Arrays.copyOfRange(field63RecByte, 3, 35);
        PartnerTxnID = Tools.bytes2String(temp);

        temp = Arrays.copyOfRange(field63RecByte, 35, 99);
        TxnID = Tools.bytes2String(temp);

        temp = Arrays.copyOfRange(field63RecByte, 99, 115);
        PayTime = Tools.bytes2String(temp);

        temp = Arrays.copyOfRange(field63RecByte, 115, 127);
        ExchRate = Tools.bytes2String(temp);

        temp = Arrays.copyOfRange(field63RecByte, 127, 139);
        TxnAmtCNY = Tools.bytes2String(temp);

        temp = Arrays.copyOfRange(field63RecByte, 139, 151);
        TxnAmt = Tools.bytes2String(temp);

        temp = Arrays.copyOfRange(field63RecByte, 151, 167);
        BuyerUserID = Tools.bytes2String(temp);

        temp = Arrays.copyOfRange(field63RecByte, 167, 187);
        BuyerLoginID = Tools.bytes2String(temp);

        temp = Arrays.copyOfRange(field63RecByte, 187, 315);
        MerInfo = Tools.bytes2String(temp);

        temp = Arrays.copyOfRange(field63RecByte, 315, 317);
        AppCode = Tools.bytes2String(temp);

        temp = Arrays.copyOfRange(field63RecByte, 317, 341);
        Promocode = Tools.bytes2String(temp);

        temp = Arrays.copyOfRange(field63RecByte, 341, 365);
        TxnNo = Tools.bytes2String(temp);

        temp = Arrays.copyOfRange(field63RecByte, 365, 389);
        Fee = Tools.bytes2String(temp);

        temp = Arrays.copyOfRange(field63RecByte, 389, 789);
        QRCode = Tools.bytes2String(temp);

        transData.setQRCurrency(Currency);
        transData.setWalletPartnerID(PartnerTxnID);
        transData.setTxnID(TxnID);
        transData.setPayTime(PayTime);
        transData.setExchangeRate(ExchRate);
        transData.setAmountCNY(TxnAmtCNY);
        transData.setTxnAmount(TxnAmt);
        transData.setBuyerUserID(BuyerUserID);
        transData.setBuyerLoginID(BuyerLoginID);
        transData.setMerchantInfo(MerInfo);
        transData.setAppCode(AppCode);
        transData.setPromocode(Promocode);
        transData.setTxnNo(TxnNo);
        transData.setFee(Fee);
        transData.setQrCode(QRCode.trim());

        //FinancialApplication.getTransDataDbHelper().updateTransData(transData);

        //FinancialApplication.getTransDataDbHelper().insertTransData(transData);
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
