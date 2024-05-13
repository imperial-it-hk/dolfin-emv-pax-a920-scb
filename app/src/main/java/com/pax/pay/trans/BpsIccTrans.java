package com.pax.pay.trans;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.trans.action.ActionDispSingleLineMsg;
import com.pax.pay.trans.action.ActionEnterAmount;
import com.pax.pay.trans.action.ActionIccReader;
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

public class BpsIccTrans extends BaseTrans {

    private String amount;
    private boolean isNeedInputAmount = true; // is need input amount
    private byte searchCardMode = -1; // search card mode
    private boolean isFreePin  = true;
    private String qrCodeData;

    public BpsIccTrans(Context context, TransEndListener transListener) {

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
        READ_CARD
    }

    @Override
    protected void bindStateOnAction() {

        ActionIccReader iccReaderAction = new ActionIccReader(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionIccReader) action).setParam(getCurrentContext(),
                        null);
            }
        });
        bind(State.READ_CARD.toString(), iccReaderAction, true);


        // execute the first action
        gotoState(State.READ_CARD.toString());
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
            case READ_CARD:
                if (result.getRet() == TransResult.SUCC) {
                    // end trans
                    transEnd(result);
                }
                break;
            default:
                transEnd(result);
        }
    }


}
