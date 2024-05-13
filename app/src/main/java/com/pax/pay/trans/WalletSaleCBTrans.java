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
import com.pax.pay.trans.action.ActionGetQRWallet;
import com.pax.pay.trans.action.ActionQrSaleInquiry;
import com.pax.pay.trans.action.ActionShowQRCode;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.AcqManager;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.task.PrintTask;
import com.pax.pay.trans.transmit.Transmit;
import com.pax.settings.SysParam;

public class WalletSaleCBTrans extends BaseTrans {
    String mAmount = null;

    public WalletSaleCBTrans(Context context, TransEndListener transListener) {
        super(context, ETransType.SALE_WALLET, Constants.ACQ_WALLET, transListener);
        setBackToMain(true);
    }

    public WalletSaleCBTrans(Context context, String amount, TransEndListener transListener) {
        super(context, ETransType.SALE_WALLET, Constants.ACQ_WALLET, transListener);
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
        bind(WalletSaleCBTrans.State.ENTER_AMOUNT.toString(), amountAction, true);

        ActionGetQRWallet getQrInfo = new ActionGetQRWallet(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionGetQRWallet) action).setParam(getCurrentContext(), transData);
            }
        });
        bind(WalletSaleCBTrans.State.GET_QR_INFO.toString(),getQrInfo);

        ActionShowQRCode showQRCode = new ActionShowQRCode(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionShowQRCode) action).setParam(getCurrentContext(),
                        getString(R.string.menu_qr_sale), R.string.trans_dynamic_qr, transData, true);
            }
        });
        bind(WalletSaleCBTrans.State.DISPLAY_QR.toString(), showQRCode);


        ActionQrSaleInquiry qrSaleInquiry = new ActionQrSaleInquiry(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionQrSaleInquiry) action).setParam(getCurrentContext(),
                        getString(R.string.menu_wallet_sale), transData);
            }
        });
        bind(WalletSaleCBTrans.State.INQUIRY.toString(), qrSaleInquiry, false);

        ActionCheckQR checkQR = new ActionCheckQR(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionCheckQR) action).setParam(getCurrentContext(),
                        getString(R.string.menu_wallet_sale), transData);
            }
        });
        bind(WalletSaleCBTrans.State.CHECKQR.toString(), checkQR, true);

        //print preview action
        PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(WalletSaleCBTrans.this, WalletSaleCBTrans.State.PRINT.toString()));
        bind(WalletSaleCBTrans.State.PRINT.toString(), printTask);

        if (mAmount == null) {
            gotoState(WalletSaleCBTrans.State.ENTER_AMOUNT.toString());
        } else {
            transData.setAmount(mAmount);
            initTransDataQr();
            gotoState(WalletSaleCBTrans.State.GET_QR_INFO.toString());
        }

    }

    enum State {
        ENTER_AMOUNT,
        GET_QR_INFO,
        DISPLAY_QR,
        INQUIRY,
        CHECKQR,
        PRINT,
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {
        int ret = result.getRet();
        WalletSaleCBTrans.State state = WalletSaleCBTrans.State.valueOf(currentState);
        if (ret != TransResult.SUCC && ret != TransResult.ERR_WALLET_RESP_UK ) {
            if(ret == TransResult.ERR_TIMEOUT && state == WalletSaleCBTrans.State.DISPLAY_QR){
                updateTransNo();
            }else{
                transEnd(result);
            }
            return;
        }
        switch (state) {
            case ENTER_AMOUNT:
                transData.setAmount(result.getData().toString());
                initTransDataQr();
                gotoState(WalletSaleCBTrans.State.GET_QR_INFO.toString());
                break;
            case GET_QR_INFO:
                gotoState(WalletSaleCBTrans.State.DISPLAY_QR.toString());
                break;
            case DISPLAY_QR:
                updateTransNo();
                break;
            case INQUIRY:
                toCheckQrOrPrint(result);
                break;
            case CHECKQR:
                if (result.getRet() == TransResult.SUCC) {
                    gotoState(WalletSaleCBTrans.State.INQUIRY.toString());
                } else {
                    ECRProcReturn(null, new ActionResult(result.getRet(), null));
                    gotoState(WalletSaleCBTrans.State.PRINT.toString());
                }
                break;
            case PRINT:
                if (result.getRet() == TransResult.SUCC) {
                    transEnd(result);
                } else {
                    dispResult(transType.getTransName(), result, null);
                    gotoState(WalletSaleCBTrans.State.PRINT.toString());
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
        transData.setTransState(TransData.ETransStatus.NORMAL);
        transData.setAcquirer(acquirer);
        transData.setIssuer(issuer);
        transData.setTpdu("600" + acquirer.getNii() + "0000");
        transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
        transData.setWalletRetryStatus(TransData.WalletRetryStatus.NORMAL);
    }

    private void updateTransNo() {
        long transNo = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO);
        transData.setTraceNo(transNo);
        gotoState(WalletSaleCBTrans.State.INQUIRY.toString());
    }

    private void toCheckQrOrPrint(ActionResult result){
        if (result.getRet() == TransResult.SUCC) {
            Component.initField63Wallet(transData);
            transData.setSignFree(true);
            FinancialApplication.getTransDataDbHelper().updateTransData(transData);
            ECRProcReturn(null, new ActionResult(result.getRet(), null));
            gotoState(WalletSaleCBTrans.State.PRINT.toString());
        } else {
            if (result.getRet() == TransResult.ERR_WALLET_RESP_UK) {
                gotoState(WalletSaleCBTrans.State.CHECKQR.toString());
            } else {
                transEnd(result);
            }
        }
    }
}

