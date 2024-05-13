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
import com.pax.pay.trans.action.ActionQrSaleInquiry;
import com.pax.pay.trans.action.ActionShowQRRef;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.AcqManager;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.task.PrintTask;
import com.pax.pay.utils.CurrencyConverter;

/**
 * Created by WITSUTA A on 4/27/2018.
 */

public class PromptPayOnlineLastTrans extends BaseTrans{

    public PromptPayOnlineLastTrans(Context context, TransEndListener transListener) {
        super(context, ETransType.BPS_QR_SALE_INQUIRY, transListener);
    }

    @Override
    protected void bindStateOnAction() {

        ActionShowQRRef getLastTransAction = new ActionShowQRRef(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionShowQRRef) action).setParam(getCurrentContext(),
                        getString(R.string.menu_qr_last_online), null, Constants.ACQ_QR_PROMPT);
            }
        });
        bind(PromptPayOnlineLastTrans.State.GET_LAST_TRANS.toString(), getLastTransAction, true);

        gotoState(PromptPayOnlineLastTrans.State.GET_LAST_TRANS.toString());

        ActionQrSaleInquiry qrSaleInquiry = new ActionQrSaleInquiry(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionQrSaleInquiry) action).setParam(getCurrentContext(),
                        getString(R.string.menu_qr_sale_inquiry), transData);
            }
        });
        bind(PromptPayOnlineLastTrans.State.INQUIRY.toString(), qrSaleInquiry, true);

        //print preview action
        PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(PromptPayOnlineLastTrans.this, PromptPayOnlineLastTrans.State.PRINT.toString()));
        bind(PromptPayOnlineLastTrans.State.PRINT.toString(), printTask);

        gotoState(PromptPayOnlineLastTrans.State.GET_LAST_TRANS.toString());
    }

    enum State {
        GET_LAST_TRANS,
        INQUIRY,
        PRINT,
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {
        PromptPayOnlineLastTrans.State state = PromptPayOnlineLastTrans.State.valueOf(currentState);

        switch (state) {
            case GET_LAST_TRANS:
                transData = (TransData) result.getData();
                if(transData.getAuthCode() == null) {
                    setTransData(transData);
                    gotoState(PromptPayOnlineLastTrans.State.INQUIRY.toString());
                }
                else {
                    setTransDataToPrintSlip(transData);
                    gotoState(PromptPayOnlineLastTrans.State.PRINT.toString());
                }
                break;
            case INQUIRY:
                Component.incTraceNo(null);//If send transaction, trace no will be increased.
                toPrintPreview(result);
                setTransDataToPrintSlip(transData);
                gotoState(PromptPayOnlineLastTrans.State.PRINT.toString());
                break;
            case PRINT:
                if (result.getRet() == TransResult.SUCC) {
                    FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                    transEnd(result);
                } else {
                    dispResult(transType.getTransName(), result, null);
                    gotoState(PromptPayOnlineLastTrans.State.PRINT.toString());
                }
                break;
            default:
                transEnd(result);
                break;
        }

    }

    private void setTransData(TransData transResult){
        AcqManager acqManager = FinancialApplication.getAcqManager();
        Acquirer acquirer = acqManager.findAcquirer(Constants.ACQ_QR_PROMPT);

        transData.setTpdu("600" + acquirer.getNii() + "0000");
        transData.setHeader("");
        transData.setField63(transResult.getQrRef2());
        transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
        transData.setQrSaleStatus(TransData.QrSaleStatus.GENERATE_QR.toString());
        transData.setCurrency(CurrencyConverter.getDefCurrency());
    }

    private void toPrintPreview(ActionResult result){
//        transData = (TransData) result.getData();
        transData.setSignFree(true);
        transData.setQrSaleStatus(transData.getResponseCode().getCode());
        return;
    }

    private void setTransDataToPrintSlip(TransData transResult){
        PrintTask printTask = new PrintTask(getCurrentContext(), transResult, PrintTask.genTransEndListener(PromptPayOnlineLastTrans.this, PromptPayOnlineLastTrans.State.PRINT.toString()));
        bind(PromptPayOnlineLastTrans.State.PRINT.toString(), printTask);
    }
}
