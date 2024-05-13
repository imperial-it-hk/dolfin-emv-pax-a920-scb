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
import com.pax.pay.trans.action.ActionInputTransData;
import com.pax.pay.trans.action.ActionQrSaleInquiry;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.AcqManager;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.task.PrintTask;
import com.pax.pay.utils.CurrencyConverter;

import java.util.Arrays;

/**
 * Created by WITSUTA A on 12/2/2018.
 */

public class QrInquiryTrans extends BaseTrans {

    public QrInquiryTrans(Context context, TransEndListener transListener) {
        super(context, ETransType.STATUS_INQUIRY_ALL_IN_ONE, transListener);
    }

    @Override
    protected void bindStateOnAction() {
        ActionInputTransData enterTransNoAction = new ActionInputTransData(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionInputTransData) action).setParam(getCurrentContext(), getString(R.string.menu_qr_sale_inquiry))
                        .setInputTransIDLine(getString(R.string.prompt_input_trans_id), ActionInputTransData.EInputType.TRANSID, 12, 0);
            }
        });
        bind(QrInquiryTrans.State.ENTER_TRANSID.toString(), enterTransNoAction, true);

        ActionEnterAmount amountAction = new ActionEnterAmount(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionEnterAmount) action).setParam(getCurrentContext(),
                        getString(R.string.menu_qr_sale_inquiry), false);
            }
        });
        bind(QrInquiryTrans.State.ENTER_AMOUNT.toString(), amountAction, true);

        ActionQrSaleInquiry qrSaleInquiry = new ActionQrSaleInquiry(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionQrSaleInquiry) action).setParam(getCurrentContext(),
                        getString(R.string.menu_qr_sale_inquiry), transData);
            }
        });
        bind(QrInquiryTrans.State.INQUIRY.toString(), qrSaleInquiry, true);

        //print preview action
        PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(QrInquiryTrans.this, QrInquiryTrans.State.PRINT.toString()));
        bind(QrInquiryTrans.State.PRINT.toString(), printTask);

        gotoState(QrInquiryTrans.State.ENTER_TRANSID.toString());
    }

    enum State {
        ENTER_TRANSID,
        ENTER_AMOUNT,
        INQUIRY,
        PRINT,
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {
        QrInquiryTrans.State state = QrInquiryTrans.State.valueOf(currentState);

        switch (state) {
            case ENTER_TRANSID:
                transData.setRefNo(result.getData().toString());
                initTransDataQr(this.transData);
                gotoState(QrInquiryTrans.State.ENTER_AMOUNT.toString());
                break;
            case ENTER_AMOUNT:
                transData.setAmount(result.getData().toString());
                gotoState(QrInquiryTrans.State.INQUIRY.toString());
                break;
            case INQUIRY:
                if (result.getRet() == TransResult.SUCC) {
                    transData = Component.initTextOnSlipQRVisa(transData);
                    FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                    PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(QrInquiryTrans.this, QrInquiryTrans.State.PRINT.toString()));
                    bind(QrInquiryTrans.State.PRINT.toString(), printTask);
                    gotoState(QrInquiryTrans.State.PRINT.toString());
                }else {
                    transEnd(result);
                }
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

    private TransData initTransDataQr(TransData transData) {
        AcqManager acqManager = FinancialApplication.getAcqManager();
        Acquirer acquirer = acqManager.findAcquirer(Constants.ACQ_QRC);
        Issuer issuer = acqManager.findIssuer(Constants.ISSUER_QRC);
        transData.setBatchNo(acquirer.getCurrBatchNo());
        // 冲正原因
        transData.setTransState(TransData.ETransStatus.NORMAL);
        transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
        transData.setAcquirer(acquirer);
        transData.setIssuer(issuer);
        transData.setCurrency(CurrencyConverter.getDefCurrency());
        transData.setTransInqID(true);
        transData.setTpdu("600" + acquirer.getNii() + "0000");
        return transData;
    }
}