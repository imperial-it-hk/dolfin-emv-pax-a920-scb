package com.pax.pay.trans;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.entity.Amounts;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.Issuer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.action.ActionEnterAmount;
import com.pax.pay.trans.action.ActionQrSaleInquiry;
import com.pax.pay.trans.action.ActionShowQRCode;
import com.pax.pay.trans.model.AcqManager;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.task.PrintTask;
import com.pax.pay.utils.CurrencyConverter;

/**
 * Created by SORAYA S on 07-Mar-18.
 */

public class BPSQrCodeSaleTrans extends BaseTrans {
    String mAmount = null;

    public BPSQrCodeSaleTrans(Context context, TransEndListener transListener) {
        super(context, ETransType.BPS_QR_SALE_INQUIRY, transListener);
    }

    public BPSQrCodeSaleTrans(Context context, String amount, TransEndListener transListener) {
        super(context, ETransType.BPS_QR_SALE_INQUIRY, transListener);

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
        bind(BPSQrCodeSaleTrans.State.ENTER_AMOUNT.toString(), amountAction, true);

        ActionShowQRCode showQRCode = new ActionShowQRCode(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionShowQRCode) action).setParam(getCurrentContext(),
                        getString(R.string.menu_qr_sale), R.string.trans_dynamic_qr, transData,false);
            }
        });
        bind(BPSQrCodeSaleTrans.State.GEN_QR_CODE.toString(), showQRCode);

        ActionQrSaleInquiry qrSaleInquiry = new ActionQrSaleInquiry(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionQrSaleInquiry) action).setParam(getCurrentContext(),
                        getString(R.string.menu_qr_sale_inquiry), transData);
            }
        });
        bind(BPSQrCodeSaleTrans.State.INQUIRY.toString(), qrSaleInquiry, true);

        //print preview action
        PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(BPSQrCodeSaleTrans.this, State.PRINT.toString()));
        bind(State.PRINT.toString(), printTask);

        if (mAmount == null) {
            gotoState(BPSQrCodeSaleTrans.State.ENTER_AMOUNT.toString());
        }
        else{
            initTransDataQr(this.transData);
            this.transData.setAmount(mAmount);
            gotoState(BPSQrCodeSaleTrans.State.GEN_QR_CODE.toString());
        }
    }

    enum State{
        ENTER_AMOUNT,
        GEN_QR_CODE,
        INQUIRY,
        PRINT,
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {
        int ret = result.getRet();
        BPSQrCodeSaleTrans.State state = BPSQrCodeSaleTrans.State.valueOf(currentState);
        if(state == BPSQrCodeSaleTrans.State.GEN_QR_CODE){
            setTransDataQr(result);
        }
        if (ret != TransResult.SUCC) {
            if(ret == TransResult.ERR_TIMEOUT && state == BPSQrCodeSaleTrans.State.GEN_QR_CODE){
                gotoState(BPSQrCodeSaleTrans.State.INQUIRY.toString());
            }else {
                transEnd(result);
            }
            return;
        }
        switch (state) {
            case ENTER_AMOUNT:
                initTransDataQr(this.transData);
                this.transData.setAmount(result.getData().toString());
                gotoState(BPSQrCodeSaleTrans.State.GEN_QR_CODE.toString());
                break;
            case GEN_QR_CODE:
                gotoState(BPSQrCodeSaleTrans.State.INQUIRY.toString());
                break;
            case INQUIRY:
                if (result.getRet() == TransResult.SUCC) {
                    ECRProcReturn(null, new ActionResult(TransResult.SUCC, null));
                }
                toPrintPreview();
                PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(BPSQrCodeSaleTrans.this, State.PRINT.toString()));
                bind(State.PRINT.toString(), printTask);
                gotoState(State.PRINT.toString());
                break;
            case PRINT:
                if (result.getRet() == TransResult.SUCC) {
                    transEnd(result);
                } else {
                    dispResult(transType.getTransName(), result, null);
                    gotoState(State.PRINT.toString());
                }
                break;
            default:
                transEnd(result);
                break;
        }
    }

    private TransData initTransDataQr(TransData transData){
        AcqManager acqManager = FinancialApplication.getAcqManager();
        Acquirer acquirer = acqManager.findAcquirer(Constants.ACQ_QR_PROMPT);
        Issuer issuer = acqManager.findIssuer(Constants.ISSUER_PROMTPAY);

//        transData.setStanNo(getTransNo());
        transData.setBatchNo(acquirer.getCurrBatchNo());

//        transData.setDateTime(Device.getTime(Constants.TIME_PATTERN_TRANS));
//        transData.setHeader("");
        transData.setTpdu("600" + acquirer.getNii() + "0000");
        // 冲正原因
        transData.setDupReason("06");
        transData.setTransState(TransData.ETransStatus.NORMAL);
        transData.setAcquirer(acquirer);
        transData.setIssuer(issuer);
        transData.setCurrency(CurrencyConverter.getDefCurrency());
        transData.setPromptPayRetry(true);
        return transData;
    }

    private void setTransDataQr(ActionResult result){
        if(result.getData() !=  null) {
            String qrRef2 = result.getData().toString();
            transData.setField63(qrRef2);
            transData.setQrRef2(qrRef2);
            transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
            transData.setQrSaleState(TransData.QrSaleState.QR_SEND_ONLINE);
            transData.setQrSaleStatus(TransData.QrSaleStatus.GENERATE_QR.toString());
            transData.setPan(qrRef2);
            // save trans data
            FinancialApplication.getTransDataDbHelper().insertTransData(transData);
        }
        return;
    }

    private void toPrintPreview(){
        //transData = (TransData) result.getData();
        transData.setSignFree(true);
        transData.setQrSaleStatus(transData.getResponseCode().getCode());
        FinancialApplication.getTransDataDbHelper().updateTransData(transData);
        return;
    }
}
