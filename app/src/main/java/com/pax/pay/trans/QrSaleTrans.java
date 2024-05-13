package com.pax.pay.trans;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.utils.Tools;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.Issuer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.action.ActionEnterAmount;
import com.pax.pay.trans.action.ActionGetQrInfo;
import com.pax.pay.trans.action.ActionQrSaleInquiry;
import com.pax.pay.trans.action.ActionShowQRCode;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.AcqManager;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.task.PrintTask;
import com.pax.pay.utils.CurrencyConverter;

import java.util.Arrays;

/**
 * Created by WITSUTA A on 11/23/2018.
 */

public class QrSaleTrans extends BaseTrans {
    String mAmount = null;
    String qrType;

    public QrSaleTrans(Context context, TransEndListener transListener, String qrType) {
        super(context, ETransType.QR_SALE_ALL_IN_ONE, transListener);
        this.qrType = qrType;
    }

    public QrSaleTrans(Context context, String amount, String qrType, TransEndListener transListener) {
        super(context, ETransType.QR_SALE_ALL_IN_ONE, transListener);
        this.qrType = qrType;
        this.mAmount = amount;
    }

    @Override
    protected void bindStateOnAction() {

        // enter amount action
        ActionEnterAmount amountAction = new ActionEnterAmount(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionEnterAmount) action).setParam(getCurrentContext(),
                        getString(R.string.menu_qr_sale), false);
            }
        });
        bind(QrSaleTrans.State.ENTER_AMOUNT.toString(), amountAction, true);

        ActionGetQrInfo getQrInfo = new ActionGetQrInfo(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionGetQrInfo) action).setParam(getCurrentContext(), transData);
            }
        });
        bind(QrSaleTrans.State.GET_QR_INFO.toString(),getQrInfo);

        ActionShowQRCode showQRCode = new ActionShowQRCode(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionShowQRCode) action).setParam(getCurrentContext(),
                        getString(R.string.menu_qr_sale), R.string.trans_dynamic_qr, transData, true);
            }
        });
        bind(QrSaleTrans.State.GEN_QR_CODE.toString(), showQRCode);

        ActionQrSaleInquiry qrSaleInquiry = new ActionQrSaleInquiry(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionQrSaleInquiry) action).setParam(getCurrentContext(),
                        getString(R.string.menu_qr_sale_inquiry), transData);
            }
        });
        bind(QrSaleTrans.State.INQUIRY.toString(), qrSaleInquiry, false);

        //print preview action
        PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(QrSaleTrans.this, QrSaleTrans.State.PRINT.toString()));
        bind(QrSaleTrans.State.PRINT.toString(), printTask);

        if (mAmount == null) {
            gotoState(QrSaleTrans.State.ENTER_AMOUNT.toString());
        }
        else{
            initTransDataQr(this.transData);
            transData.setAmount(mAmount.replace(".", ""));
            gotoState(QrSaleTrans.State.GET_QR_INFO.toString());
        }
    }

    enum State{
        ENTER_AMOUNT,
        GET_QR_INFO,
        GEN_QR_CODE,
        INQUIRY,
        PRINT,
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {
        int ret = result.getRet();
        QrSaleTrans.State state = QrSaleTrans.State.valueOf(currentState);
        if (ret != TransResult.SUCC) {
            if(ret == TransResult.ERR_TIMEOUT && state == QrSaleTrans.State.GEN_QR_CODE){
                gotoState(QrSaleTrans.State.INQUIRY.toString());
            }else {
                transEnd(result);
            }
            return;
        }
        switch (state) {
            case ENTER_AMOUNT:
                initTransDataQr(this.transData);
                this.transData.setAmount(result.getData().toString());
                gotoState(QrSaleTrans.State.GET_QR_INFO.toString());
                break;
            case GET_QR_INFO:
                transData = (TransData) result.getData();
                gotoState(QrSaleTrans.State.GEN_QR_CODE.toString());
                break;
            case GEN_QR_CODE:
                gotoState(QrSaleTrans.State.INQUIRY.toString());
                break;
            case INQUIRY:
                if (result.getRet() == TransResult.SUCC) {
                    toPrintPreview();
                    ECRProcReturn(null, new ActionResult(TransResult.SUCC, null));
                }
                break;
            case PRINT:
                if (result.getRet() == TransResult.SUCC) {
                    transEnd(result);
                } else {
                    dispResult(transType.getTransName(), result, null);
                    gotoState(QrSaleTrans.State.PRINT.toString());
                }
                break;
            default:
                transEnd(result);
                break;
        }
    }

    private TransData initTransDataQr(TransData transData){
        AcqManager acqManager = FinancialApplication.getAcqManager();
        Acquirer acquirer = acqManager.findAcquirer(Constants.ACQ_QRC);
        Issuer issuer = acqManager.findIssuer(Constants.ISSUER_QRC);
        transData.setBatchNo(acquirer.getCurrBatchNo());
        transData.setTpdu("600" + acquirer.getNii() + "0000");
        // 冲正原因
        transData.setDupReason("06");
        transData.setTransState(TransData.ETransStatus.NORMAL);
        transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
        transData.setAcquirer(acquirer);
        transData.setIssuer(issuer);
        transData.setCurrency(CurrencyConverter.getDefCurrency());
        transData.setPromptPayRetry(true);
        transData.setQrType(qrType);
        return transData;
    }

    private void toPrintPreview(){
        transData.setSignFree(true);
        transData.setQrSaleStatus(transData.getResponseCode().getCode());
        transData = Component.initTextOnSlipQRVisa(transData);
        FinancialApplication.getTransDataDbHelper().updateTransData(transData);
        PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(QrSaleTrans.this, QrSaleTrans.State.PRINT.toString()));
        bind(QrSaleTrans.State.PRINT.toString(), printTask);
        gotoState(QrSaleTrans.State.PRINT.toString());
    }
}
