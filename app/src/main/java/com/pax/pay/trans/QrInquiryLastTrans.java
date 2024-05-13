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

public class QrInquiryLastTrans extends BaseTrans {

    public QrInquiryLastTrans(Context context, TransEndListener transListener) {
        super(context, ETransType.QR_SALE_ALL_IN_ONE, transListener);
    }

    @Override
    protected void bindStateOnAction() {

        ActionShowQRRef getLastTransAction = new ActionShowQRRef(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionShowQRRef) action).setParam(getCurrentContext(),
                        getString(R.string.menu_qr_last_online),null, Constants.ACQ_QRC);
            }
        });
        bind(QrInquiryLastTrans.State.GET_LAST_TRANS.toString(), getLastTransAction, true);

        ActionQrSaleInquiry qrSaleInquiry = new ActionQrSaleInquiry(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionQrSaleInquiry) action).setParam(getCurrentContext(),
                        getString(R.string.menu_qr_sale_inquiry), transData);
            }
        });
        bind(QrInquiryLastTrans.State.INQUIRY.toString(), qrSaleInquiry, true);

        //print preview action
        PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(QrInquiryLastTrans.this, QrInquiryLastTrans.State.PRINT.toString()));
        bind(QrInquiryLastTrans.State.PRINT.toString(), printTask);

        gotoState(QrInquiryLastTrans.State.GET_LAST_TRANS.toString());
    }

    enum State {
        GET_LAST_TRANS,
        INQUIRY,
        PRINT,
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {
        QrInquiryLastTrans.State state = QrInquiryLastTrans.State.valueOf(currentState);

        switch (state) {
            case GET_LAST_TRANS:
                if (result.getRet() == TransResult.ERR_NO_TRANS) {
                    transEnd(result);
                } else {
                    TransData transQr = (TransData) result.getData();
                    if (!transQr.getQrSaleStatus().equals(TransData.QrSaleStatus.SUCCESS.toString())) {
                        setTransData(transQr);
                        gotoState(QrInquiryLastTrans.State.INQUIRY.toString());
                    } else {
                        setTransDataToPrintSlip(transQr);
                        gotoState(QrInquiryLastTrans.State.PRINT.toString());
                    }
                }
                break;
            case INQUIRY:
                if (result.getRet() == TransResult.SUCC) {
                    Component.incTraceNo(null);//If send transaction, trace no will be increased.
                    toPrintPreview();
                    setTransDataToPrintSlip(transData);
                    gotoState(QrInquiryLastTrans.State.PRINT.toString());
                } else {
                    transEnd(result);
                }
                break;
            case PRINT:
                if (result.getRet() == TransResult.SUCC) {
                    FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                    transEnd(result);
                } else {
                    dispResult(transType.getTransName(), result, null);
                    gotoState(QrInquiryLastTrans.State.PRINT.toString());
                }
                break;
            default:
                transEnd(result);
                break;
        }
    }

    private void setTransData(TransData transResult) {
        AcqManager acqManager = FinancialApplication.getAcqManager();
        Acquirer acquirer = acqManager.findAcquirer(Constants.ACQ_QRC);
        Issuer issuer = acqManager.findIssuer(Constants.ISSUER_QRC);
        transData.setAcquirer(acquirer);
        transData.setIssuer(issuer);
        transData.setTpdu("600" + acquirer.getNii() + "0000");
        transData.setHeader("");
        transData.setField63(transResult.getQrRef2());
        transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
        transData.setQrSaleStatus(TransData.QrSaleStatus.GENERATE_QR.toString());
        transData.setCurrency(CurrencyConverter.getDefCurrency());
        transData.setQrType(transResult.getQrType());
        transData.setQrRef2(transResult.getQrRef2());
        transData.setAmount(transResult.getAmount());
        transData.setQrID(transResult.getQrID());
        transData.setLastTrans(true);
    }

    private void toPrintPreview() {
        transData.setSignFree(true);
        transData = Component.initTextOnSlipQRVisa(transData);
        transData.setQrSaleStatus(TransData.QrSaleStatus.SUCCESS.toString());
        FinancialApplication.getTransDataDbHelper().updateTransData(transData);
        return;
    }

    private void setTransDataToPrintSlip(TransData transResult) {
        PrintTask printTask = new PrintTask(getCurrentContext(), transResult, PrintTask.genTransEndListener(QrInquiryLastTrans.this, QrInquiryLastTrans.State.PRINT.toString()));
        bind(QrInquiryLastTrans.State.PRINT.toString(), printTask);
    }
}
