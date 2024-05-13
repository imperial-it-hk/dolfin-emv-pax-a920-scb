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
import com.pax.pay.trans.action.ActionEnterAmountWithMsg;
import com.pax.pay.trans.action.ActionInputTransData;
import com.pax.pay.trans.action.ActionQrSaleInquiry;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.AcqManager;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.task.PrintTask;

/**
 * Created by WITSUTA A on 5/11/2018.
 */

public class WalletRefundTrans extends BaseTrans{

    public WalletRefundTrans(Context context, TransEndListener transListener) {
        super(context, ETransType.REFUND_WALLET, Constants.ACQ_WALLET, transListener);
    }


    @Override
    protected void bindStateOnAction() {

        ActionEnterAmountWithMsg amountAction = new ActionEnterAmountWithMsg(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionEnterAmountWithMsg) action).setParam(getCurrentContext(),
                        getString(R.string.menu_qr_refund_wallet),  getString(R.string.input_refund_amount));
            }
        });
        bind(WalletRefundTrans.State.ENTER_AMOUNT.toString(), amountAction, true);

        ActionEnterAmountWithMsg origAmountAction = new ActionEnterAmountWithMsg(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionEnterAmountWithMsg) action).setParam(getCurrentContext(),
                        getString(R.string.menu_qr_refund_wallet), getString(R.string.input_original_refund));
            }
        });
        bind(WalletRefundTrans.State.ENTER_ORIG_AMOUNT.toString(), origAmountAction, true);

        ActionInputTransData enterTransNoAction = new ActionInputTransData(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionInputTransData) action).setParam(getCurrentContext(), getString(R.string.menu_qr_refund_wallet))
                        .setInputTransIDLine(getString(R.string.wallet_input_partner_id), ActionInputTransData.EInputType.TRANSID, 12,0);
            }
        });
        bind(WalletRefundTrans.State.ENTER_TRANSID.toString(), enterTransNoAction, true);

        ActionQrSaleInquiry qrSaleInquiry = new ActionQrSaleInquiry(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionQrSaleInquiry) action).setParam(getCurrentContext(),
                        getString(R.string.menu_qr_refund_wallet), transData);
            }
        });
        bind(WalletRefundTrans.State.INQUIRY.toString(), qrSaleInquiry, false);

        //print preview action
        PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(WalletRefundTrans.this, WalletRefundTrans.State.PRINT.toString()));
        bind(WalletRefundTrans.State.PRINT.toString(), printTask);

        gotoState(WalletRefundTrans.State.ENTER_AMOUNT.toString());
    }

    enum State {
        ENTER_AMOUNT,
        ENTER_ORIG_AMOUNT,
        ENTER_TRANSID,
        INQUIRY,
        PRINT,
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {
        WalletRefundTrans.State state = WalletRefundTrans.State.valueOf(currentState);

        switch (state) {
            case ENTER_AMOUNT:
                transData.setAmount(result.getData().toString());
                gotoState(WalletRefundTrans.State.ENTER_ORIG_AMOUNT.toString());
                break;
            case ENTER_ORIG_AMOUNT:
                transData.setOrigAmount(result.getData().toString());
                gotoState(WalletRefundTrans.State.ENTER_TRANSID.toString());
                break;
            case ENTER_TRANSID:
                initTransDataQr(result);
                break;
            case INQUIRY:
                toPrint(result);
                break;
            case PRINT:
                if (result.getRet() == TransResult.SUCC) {
                    transEnd(result);
                } else {
                    dispResult(transType.getTransName(), result, null);
                    gotoState(WalletRefundTrans.State.PRINT.toString());
                }
                break;
            default:
                transEnd(result);
                break;
        }

    }

    private void initTransDataQr(ActionResult result){
        transData.setRefNo(result.getData().toString());
        transData.setOrigRefNo(result.getData().toString());
        transData.setWalletPartnerID(result.getData().toString());

        AcqManager acqManager = FinancialApplication.getAcqManager();
        Acquirer acquirer = acqManager.findAcquirer(Constants.ACQ_WALLET);
        Issuer issuer = acqManager.findIssuer(Constants.ISSUER_WALLET);
        transData.setBatchNo(acquirer.getCurrBatchNo());
        // 冲正原因
        transData.setTransState(TransData.ETransStatus.NORMAL);
        transData.setAcquirer(acquirer);
        transData.setIssuer(issuer);
        transData.setTpdu("600" + acquirer.getNii() + "0000");
        transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
        transData.setAdviceStatus(TransData.AdviceStatus.NORMAL);
        transData.setWalletRetryStatus(TransData.WalletRetryStatus.NORMAL);
        gotoState(WalletRefundTrans.State.INQUIRY.toString());
    }

    private void toPrint(ActionResult result){
//        transData = (TransData) result.getData();
        if (result.getRet() == TransResult.SUCC || result.getRet() == TransResult.ERR_ADVICE) {
            Component.initField63Wallet(transData);
            transData.setSignFree(true);
            FinancialApplication.getTransDataDbHelper().updateTransData(transData);
            PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(WalletRefundTrans.this, WalletQrSaleTrans.State.PRINT.toString()));
            bind(WalletRefundTrans.State.PRINT.toString(), printTask);
            gotoState(WalletRefundTrans.State.PRINT.toString());
        } else {
            transEnd(result);
            return;
        }
    }

}
