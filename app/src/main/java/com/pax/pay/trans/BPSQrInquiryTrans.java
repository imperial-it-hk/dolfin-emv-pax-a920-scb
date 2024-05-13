package com.pax.pay.trans;


import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.Issuer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.action.ActionEnterAmount;
import com.pax.pay.trans.action.ActionInputTransData;
import com.pax.pay.trans.action.ActionQrSaleInquiry;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.AcqManager;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.task.PrintTask;

/**
 * Created by WITSUTA A on 4/20/2018.
 */

public class BPSQrInquiryTrans extends BaseTrans{

    public BPSQrInquiryTrans(Context context, TransEndListener transListener) {
        super(context, ETransType.BPS_QR_INQUIRY_ID, transListener);
    }



    @Override
    protected void bindStateOnAction() {


        ActionInputTransData enterTransNoAction = new ActionInputTransData(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionInputTransData) action).setParam(getCurrentContext(), getString(R.string.menu_qr_sale_inquiry))
                        .setInputTransIDLine(getString(R.string.prompt_input_trans_id), ActionInputTransData.EInputType.TRANSID, 12,0);
            }
        });
        bind(BPSQrInquiryTrans.State.ENTER_TRANSID.toString(), enterTransNoAction, true);

        ActionEnterAmount amountAction = new ActionEnterAmount(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionEnterAmount) action).setParam(getCurrentContext(),
                        getString(R.string.menu_qr_sale_inquiry), false);
            }
        });
        bind(BPSQrInquiryTrans.State.ENTER_AMOUNT.toString(), amountAction, true);

        ActionQrSaleInquiry qrSaleInquiry = new ActionQrSaleInquiry(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionQrSaleInquiry) action).setParam(getCurrentContext(),
                        getString(R.string.menu_qr_sale_inquiry), transData);
            }
        });
        bind(BPSQrInquiryTrans.State.INQUIRY.toString(), qrSaleInquiry, true);

        //print preview action
        PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(BPSQrInquiryTrans.this, BPSQrInquiryTrans.State.PRINT.toString()));
        bind(BPSQrInquiryTrans.State.PRINT.toString(), printTask);

        gotoState(BPSQrInquiryTrans.State.ENTER_TRANSID.toString());
    }

    enum State {
        ENTER_TRANSID,
        ENTER_AMOUNT,
        INQUIRY,
        PRINT,
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {
        BPSQrInquiryTrans.State state = BPSQrInquiryTrans.State.valueOf(currentState);

        switch (state) {
            case ENTER_TRANSID:
                transData.setRefNo(result.getData().toString());
                //getTransfromRefNo(result.getData().toString());
                initTransDataQr(this.transData);
                gotoState(BPSQrInquiryTrans.State.ENTER_AMOUNT.toString());
                break;
            case ENTER_AMOUNT:
                transData.setAmount(result.getData().toString());
                gotoState(BPSQrInquiryTrans.State.INQUIRY.toString());
                break;
            case INQUIRY:
//                transData = (TransData) result.getData();
                setTransRefData(transData);
                checkTransdata(transData); //save trans
                PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(BPSQrInquiryTrans.this, BPSQrInquiryTrans.State.PRINT.toString()));
                bind(BPSQrInquiryTrans.State.PRINT.toString(), printTask);
                gotoState(BPSQrInquiryTrans.State.PRINT.toString());
                break;
            case PRINT:
                if (result.getRet() == TransResult.SUCC) {
                    transEnd(result);
                } else {
                    dispResult(transType.getTransName(), result, null);
                    gotoState(BPSQrCodeSaleTrans.State.PRINT.toString());
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
        transData.setBatchNo(acquirer.getCurrBatchNo());
        // 冲正原因
        transData.setTransState(TransData.ETransStatus.NORMAL);
        transData.setAcquirer(acquirer);
        transData.setIssuer(issuer);
        transData.setTransInqID(true);
        transData.setTpdu("600" + acquirer.getNii() + "0000");
        return transData;
    }

    private void getTransfromRefNo(String RefNo) {
        final TransData transDataRefNo = FinancialApplication.getTransDataDbHelper().findTransDataByRefNo(RefNo);

        if (transDataRefNo == null) {
            transEnd(new ActionResult(TransResult.ERR_NOT_SUPPORT_TRANS, null));
            return;
        }
        transData.setDateTime(transDataRefNo.getDateTime());
    }

    private void setTransRefData(TransData transData) {
        String ref = transData.getField63();
        if(ref != null){
            int start = ref.lastIndexOf("RF");
            ref = ref.substring(start+2);
        }
        transData.setQrRef2(ref);
    }

    private void checkTransdata(TransData transData){
        final TransData transDataRefNo = FinancialApplication.getTransDataDbHelper().findTransDataByRefNo(transData.getRefNo());
        transData.setQrSaleStatus(transData.getResponseCode().getCode());
        transData.setQrSaleState(TransData.QrSaleState.QR_SEND_ONLINE);
        FinancialApplication.getTransDataDbHelper().insertTransData(transData);
        if (transDataRefNo == null) {
            //increase trans no.
            Component.incStanNo(transData);
            Component.incTraceNo(transData);
        }
    }


}
