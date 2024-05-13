package com.pax.pay.trans;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.trans.action.ActionDispSingleLineMsg;
import com.pax.pay.trans.action.ActionEnterAmount;
import com.pax.pay.trans.action.ActionScanCode;
import com.pax.pay.trans.action.ActionSignature;
import com.pax.pay.trans.action.ActionTransOnline;
import com.pax.pay.trans.action.ActionUserAgreement;
import com.pax.pay.trans.action.activity.UserAgreementActivity;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.task.PrintTask;
import com.pax.pay.utils.ToastUtils;

/**
 * Created by JAICHANOK N on 1/30/2018.
 */

public class BpsQrSaleTrans extends BaseTrans {

    private String amount;
    private boolean isNeedInputAmount = true; // is need input amount
    private byte searchCardMode = -1; // search card mode
    private boolean isFreePin  = true;
    private String qrCodeData;

    public BpsQrSaleTrans(Context context, TransEndListener transListener) {

        super(context, ETransType.BPS_QR_READ, transListener);
        setParam(amount, "0", searchCardMode, isFreePin, false);
    }
    private void setParam(String amount, String tipAmount, byte mode, boolean isFreePin, boolean hasTip) {
        this.searchCardMode = mode;
        this.amount = amount;
        this.isFreePin = isFreePin;

        /*if (searchCardMode == -1) { // 待机银行卡消费入口
            searchCardMode = Component.getCardReadMode(ETransType.BPS_TRANS);
            this.transType = ETransType.BPS_TRANS;
        }*/
        this.transType = ETransType.BPS_QR_READ;
    }


    enum State {
        ENTER_AMOUNT,
        SCAN_CODE,
        MAG_ONLINE,
        EMV_PROC,
        CLSS_PREPROC,
        CLSS_PROC,
        SIGNATURE,
        USER_AGREEMENT,
        SHOW_CODE,
        OFFLINE_SEND,
        PRINT,
    }

    @Override
    protected void bindStateOnAction() {
    // enter amount action
        ActionEnterAmount amountAction = new ActionEnterAmount(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionEnterAmount) action).setParam(getCurrentContext(),
                        getString(R.string.trans_sale), false);
            }
        });
        bind(State.ENTER_AMOUNT.toString(), amountAction, true);

        ActionScanCode scanCodeAction = new ActionScanCode(null);
        bind(State.SCAN_CODE.toString(), scanCodeAction, true);

        ActionDispSingleLineMsg displayInfoAction = new ActionDispSingleLineMsg(new com.pax.abl.core.AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionDispSingleLineMsg) action).setParam(getCurrentContext(),
                        "BPS QR", "QR Data", qrCodeData, 60);
            }
        });
        bind(State.SHOW_CODE.toString(), displayInfoAction, true);

        // online action
        ActionTransOnline transOnlineAction = new ActionTransOnline(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionTransOnline) action).setParam(getCurrentContext(), transData);
            }
        });

        bind(State.MAG_ONLINE.toString(), transOnlineAction, true);

        // signature action
        ActionSignature signatureAction = new ActionSignature(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionSignature) action).setParam(getCurrentContext(), transData.getAmount());
            }
        });
        bind(State.SIGNATURE.toString(), signatureAction);

        // Agreement action
        ActionUserAgreement userAgreementAction = new ActionUserAgreement(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionUserAgreement) action).setParam(getCurrentContext());
            }
        });
        bind(State.USER_AGREEMENT.toString(), userAgreementAction, true);

        //print preview action
        PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(BpsQrSaleTrans.this, State.PRINT.toString()));
        bind(State.PRINT.toString(), printTask);

        // execute the first action
        if (isNeedInputAmount) {
            gotoState(State.ENTER_AMOUNT.toString());
        } else {
            transData.setAmount(amount);
            gotoState(State.SCAN_CODE.toString());
        }
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {
       State state = State.valueOf(currentState);
        if (result.getRet() != TransResult.SUCC) {
            ToastUtils.showMessage("Fail");
            transEnd(result);
            return;
        }

        switch (state){
            case ENTER_AMOUNT:
                // save trans amount
                transData.setAmount(result.getData().toString());
                gotoState(State.SCAN_CODE.toString());
                break;
            case SCAN_CODE:
                afterScanCode(result);
                break;
            case SHOW_CODE:
                if (result.getRet() == TransResult.SUCC) {
                    // end trans
                   // gotoState(State.MAG_ONLINE.toString());
                    transEnd(result);
                }
                break;
            case MAG_ONLINE: // subsequent processing of online
                // determine whether need electronic signature or print
                toSignOrPrint();
                break;
            case SIGNATURE:
                // save signature data
                byte[] signData = (byte[]) result.getData();
                byte[] signPath = (byte[]) result.getData1();

                if (signData != null && signData.length > 0 &&
                        signPath != null && signPath.length > 0) {
                    transData.setSignData(signData);
                    transData.setSignPath(signPath);
                    // update trans data，save signature
                    FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                }

                //check offline trans
                gotoState(State.PRINT.toString());
                break;
            case USER_AGREEMENT:
                String agreement = (String) result.getData();
                if (agreement != null && agreement.equals(UserAgreementActivity.ENTER_BUTTON)) {
                    gotoState(State.SCAN_CODE.toString());
                } else {
                    transEnd(result);
                }
                break;
            case PRINT:
                transEnd(result);
            default:
                transEnd(result);
        }
    }

    private void afterScanCode(ActionResult result) {
        // 扫码
        qrCodeData = (String) result.getData();
       /* if (qrCode == null || qrCode.length() == 0) {
            transEnd(new ActionResult(TransResult.ERR_INVALID_EMV_QR, null));
            return;
        }
        EmvQr emvQr = EmvQr.decodeEmvQr(transData, qrCode);
        if (emvQr == null) {
            transEnd(new ActionResult(TransResult.ERR_INVALID_EMV_QR, null));
            return;
        }
        if (!emvQr.isUpiAid()) {
            transEnd(new ActionResult(TransResult.ERR_NOT_SUPPORT_TRANS, null));
            return;
        }*/

        transData.setQrCode(qrCodeData);
        gotoState(State.SHOW_CODE.toString());
    }

    // need electronic signature or send
    private void toSignOrPrint() {
        transData.setSignFree(false);
        gotoState(State.PRINT.toString());
        FinancialApplication.getTransDataDbHelper().updateTransData(transData);
    }

}
