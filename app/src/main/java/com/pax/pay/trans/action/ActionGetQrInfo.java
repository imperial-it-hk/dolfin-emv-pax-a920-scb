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

/**
 * Created by WITSUTA A on 11/22/2018.
 */

public class ActionGetQrInfo extends AAction{

    private Context context;
    private TransData transData;
    private TransData transGetInfo;

    // field of field QR Code
    private String infoField0;
    private String infoField1;
    private String infoField2;
    private String infoField3;
    private String infoField4;
    private String infoField5;
    private String infoField7;
    private String infoField8;
    private String infoField12;

    // QR
    private String tableLen;
    private String tableID;
    private String qrID;
    private String qrRef;
    private String showThaiLogo;
    private String cardPrompt;
    private String cardCount;
    private String cardList;
    private String qrCode;

    // field QR Code
    private String qrField0;
    private String qrField1;
    private String qrField2;
    private String qrField3;
    private String qrField4;
    private String qrField5;
    private String qrField30;
    private String qrField51;
    private String qrField52;
    private String qrField53;
    private String qrField54;
    private String qrField58;
    private String qrField59;
    private String qrField60;
    private String qrField62;
    private String qrField63;



    public ActionGetQrInfo(ActionStartListener listener) {
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
                ret = new Transmit().transmitQRSale(transGetInfo,transProcessListenerImpl);
                if (ret != TransResult.SUCC){
                    setResult(new ActionResult(TransResult.ERR_DOWNLOAD_FAILED, null));
                }else {
                    //  split field 63 to trans data
                    splitField63(transGetInfo);
                    transGetInfo.setQrSaleStatus(TransData.QrSaleStatus.NOT_SUCCESS.toString());
                    FinancialApplication.getTransDataDbHelper().insertTransData(transGetInfo);
                    setResult(new ActionResult(ret, transData));
                }

                transProcessListenerImpl.onHideProgress();
            }
        });
    }

    private TransData initGetInfoTrans () {
        TransData transGetInfo = Component.transInit();
        transGetInfo.setTransType(ETransType.GET_QR_INFO);
        AcqManager acqManager = FinancialApplication.getAcqManager();
        Acquirer acquirer = acqManager.findAcquirer(Constants.ACQ_QRC);
        Issuer issuer = acqManager.findIssuer(Constants.ISSUER_QRC);
        transGetInfo.setReversalStatus(TransData.ReversalStatus.NORMAL);
        transGetInfo.setCurrency(CurrencyConverter.getDefCurrency());
        transGetInfo.setBatchNo(acquirer.getCurrBatchNo());
        transGetInfo.setAcquirer(acquirer);
        transGetInfo.setIssuer(issuer);
        transGetInfo.setAmount(transData.getAmount());
        transGetInfo.setTpdu("600" + acquirer.getNii() + "0000");
        transGetInfo.setQrType(transData.getQrType());
        return transGetInfo;
    }

    private void splitField63 (TransData transGetInfo){
        byte[] field63RecByte = transGetInfo.getField63RecByte();
        if (field63RecByte != null) {
            splitMainField(field63RecByte);
            splitQrCode(qrCode);
            splitInfoField(qrField30);
            transData.setQrRef2(infoField3);
            transData.setQrID(qrID);
            transGetInfo.setQrRef2(infoField3);
            transGetInfo.setQrID(qrID);
        }
    }

    private void splitMainField (byte[] field63RecByte) {
        byte[] temp;
        temp = Arrays.copyOfRange(field63RecByte, 0, 2);
        tableLen = Tools.bcd2Str(temp);

        temp = Arrays.copyOfRange(field63RecByte, 2, 4);
        tableID = Tools.bcd2Str(temp);

        temp = Arrays.copyOfRange(field63RecByte, 4, 24);
        qrID = Tools.bytes2String(temp);

        temp = Arrays.copyOfRange(field63RecByte, 24, 44);
        qrRef = Tools.bytes2String(temp);

        temp = Arrays.copyOfRange(field63RecByte, 44, 45);
        showThaiLogo = Tools.bcd2Str(temp);

        temp = Arrays.copyOfRange(field63RecByte, 45, 46);
        cardPrompt = Tools.bcd2Str(temp);

        temp = Arrays.copyOfRange(field63RecByte, 46, 47);
        cardCount = Tools.bcd2Str(temp);

        int intCardCount = Integer.parseInt(cardCount);
        if (intCardCount != 0) {
            temp = Arrays.copyOfRange(field63RecByte, 47, 47 + intCardCount);
            cardList = Tools.bcd2Str(temp);
        }

        temp = Arrays.copyOfRange(field63RecByte, 47+ intCardCount, field63RecByte.length);
        qrCode = Tools.bytes2String(temp);
        qrCode = qrCode!=null ? qrCode.trim() : qrCode;
        transData.setQrCode(qrCode);
        transGetInfo.setQrCode(qrCode);
    }

    private void splitQrCode(String strQrCode) {
        String qrCode = strQrCode;
        if (qrCode != null) {
            for (;;) {
                int num = Integer.parseInt(qrCode.substring(0,2));
                int len = Integer.parseInt(qrCode.substring(2,4));
                String info = qrCode.substring(4,4+len);
                mappingQrField(num,info);
                if (qrCode.length() != 4+len) {
                    qrCode = qrCode.substring(4+len, qrCode.length());
                }else {
                    return;
                }
            }
        }
    }

    private void mappingQrField (int numOfField, String info) {
        switch (numOfField){
            case 0:
                qrField0 = info;
                break;
            case 1:
                qrField1 = info;
                break;
            case 2:
                qrField2 = info;
                break;
            case 3:
                qrField3 = info;
                break;
            case 4:
                qrField4 = info;
                break;
            case 5:
                qrField5 = info;
                break;
            case 30:
                qrField30 = info;
                break;
            case 51:
                qrField51 = info;
                break;
            case 52:
                qrField52 = info;
                break;
            case 53:
                qrField53 = info;
                break;
            case 54:
                qrField54 = info;
                break;
            case 58:
                qrField58 = info;
                break;
            case 59:
                qrField59 = info;
                break;
            case 60:
                qrField60 = info;
                break;
            case 62:
                qrField62 = info;
                break;
            case 63:
                qrField63 = info;
                break;
            default:
                return;
        }
    }

    private boolean splitInfoField(String temp) {
        boolean hasData = false;
        String infoField = temp;
        if (infoField != null) {
            hasData = true;
            for (;;) {
                int num = Integer.parseInt(infoField.substring(0,2));
                int len = Integer.parseInt(infoField.substring(2,4));
                String info = infoField.substring(4,4+len);
                mappingInfo(num,info);
                if (infoField.length() != 4+len) {
                    infoField = infoField.substring(4+len, infoField.length());
                }else {
                    break;
                }
            }
        }
        return hasData;
    }

    private void mappingInfo (int numOfField, String info) {
        switch (numOfField){
            case 0:
                infoField0 = info;
                break;
            case 1:
                infoField1 = info;
                break;
            case 2:
                infoField2 = info;
                break;
            case 3:
                infoField3 = info;
                break;
            case 4:
                infoField4 = info;
                break;
            case 5:
                infoField5 = info;
                break;
            case 7:
                infoField7 = info;
                break;
            case 8:
                infoField8 = info;
                break;
            case 12:
                infoField12 = info;
                break;
            default:
                return;
        }
    }

}
