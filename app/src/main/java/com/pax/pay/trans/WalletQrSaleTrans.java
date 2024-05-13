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
import com.pax.pay.trans.action.ActionCheckQR;
import com.pax.pay.trans.action.ActionEnterAmount;
import com.pax.pay.trans.action.ActionQrSaleInquiry;
import com.pax.pay.trans.action.ActionScanCode;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.AcqManager;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.task.PrintTask;

/**
 * Created by WITSUTA A on 4/5/2018.
 */
public class WalletQrSaleTrans extends BaseTrans{
    String mAmount = null;

    public WalletQrSaleTrans(Context context, TransEndListener transListener) {
        super(context, ETransType.QR_SALE_WALLET, Constants.ACQ_WALLET, transListener);
        setBackToMain(true);
    }

    public WalletQrSaleTrans(Context context, String amount, TransEndListener transListener) {
        super(context, ETransType.QR_SALE_WALLET, Constants.ACQ_WALLET, transListener);
        setBackToMain(true);
        this.mAmount = amount;
    }

    @Override
    protected void bindStateOnAction() {

        ActionEnterAmount amountAction = new ActionEnterAmount(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionEnterAmount) action).setParam(getCurrentContext(),
                        getString(R.string.menu_wallet_sale), false);
            }
        });
        bind(WalletQrSaleTrans.State.ENTER_AMOUNT.toString(), amountAction, true);

        ActionScanCode scanCodeAction = new ActionScanCode(null);
        bind(WalletQrSaleTrans.State.SCAN_CODE.toString(), scanCodeAction, true);

        ActionQrSaleInquiry qrSaleInquiry = new ActionQrSaleInquiry(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionQrSaleInquiry) action).setParam(getCurrentContext(),
                        getString(R.string.menu_wallet_sale), transData);
            }
        });
        bind(WalletQrSaleTrans.State.INQUIRY.toString(), qrSaleInquiry);

        ActionCheckQR checkQR = new ActionCheckQR(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionCheckQR) action).setParam(getCurrentContext(),
                        getString(R.string.menu_wallet_sale), transData);
            }
        });
        bind(WalletQrSaleTrans.State.CHECKQR.toString(), checkQR, true);

        //print preview action
        PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(WalletQrSaleTrans.this, WalletQrSaleTrans.State.PRINT.toString()));
        bind(WalletQrSaleTrans.State.PRINT.toString(), printTask);

        if (mAmount == null) {
            gotoState(WalletQrSaleTrans.State.ENTER_AMOUNT.toString());
        } else {
            initTransDataQr();
            transData.setAmount(mAmount);
        }

    }

    enum State {
        ENTER_AMOUNT,
        SCAN_CODE,
        INQUIRY,
        CHECKQR,
        PRINT, GEN_QR_CODE,
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {

        WalletQrSaleTrans.State state = WalletQrSaleTrans.State.valueOf(currentState);

        switch (state) {
            case ENTER_AMOUNT:
                initTransDataQr();
                transData.setAmount(result.getData().toString());
                break;
            case SCAN_CODE:
                transData.setQrBuyerCode(result.getData().toString());
                transData.setPan(result.getData().toString());
                gotoState(WalletQrSaleTrans.State.INQUIRY.toString());
                break;
            case INQUIRY:
                toCheckQrOrPrint(result);
                break;
            case CHECKQR:
                if (result.getRet() == TransResult.SUCC) {
                    //transData = (TransData) result.getData();
                    gotoState(WalletQrSaleTrans.State.INQUIRY.toString());
                } else {
                    ECRProcReturn(null, new ActionResult(result.getRet(), null));
                    gotoState(WalletQrSaleTrans.State.PRINT.toString());
                }
                break;
            case PRINT:
                if (result.getRet() == TransResult.SUCC) {
                    transEnd(result);
                } else {
                    dispResult(transType.getTransName(), result, null);
                    gotoState(WalletQrSaleTrans.State.PRINT.toString());
                }
                break;
            default:
                transEnd(result);
                break;
        }

    }

    private void initTransDataQr(){

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
        transData.setWalletRetryStatus(TransData.WalletRetryStatus.NORMAL);
        gotoState(WalletQrSaleTrans.State.SCAN_CODE.toString());
    }

    private void toCheckQrOrPrint(ActionResult result){
        if (result.getRet() == TransResult.SUCC) {
            Component.initField63Wallet(transData);
            transData.setSignFree(true);
            FinancialApplication.getTransDataDbHelper().updateTransData(transData);
            ECRProcReturn(null, new ActionResult(result.getRet(), null));
            gotoState(WalletQrSaleTrans.State.PRINT.toString());
        } else {
            if (result.getRet() == TransResult.ERR_WALLET_RESP_UK) {
                gotoState(WalletQrSaleTrans.State.CHECKQR.toString());
            } else {
                transEnd(result);
            }
        }
    }
}
