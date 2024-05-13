package com.pax.pay.trans;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.trans.action.ActionEnterAmount;
import com.pax.pay.trans.action.ActionShowQRCode;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.utils.ToastUtils;

/**
 * Created by SORAYA S on 30-Jan-18.
 */

public class BPSQrDynamicTrans extends BaseTrans {
    private String amount;

    public BPSQrDynamicTrans(Context context, TransEndListener transListener) {
        super(context, ETransType.BPS_QR_SALE_INQUIRY, transListener);
    }

    public BPSQrDynamicTrans(Context context, String amount, TransEndListener transListener) {
        super(context, ETransType.BPS_QR_SALE_INQUIRY, transListener);
        this.amount = amount;
    }

    @Override
    protected void bindStateOnAction() {
        // enter amount action
        ActionEnterAmount amountAction = new ActionEnterAmount(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionEnterAmount) action).setParam(getCurrentContext(),
                        getString(R.string.trans_display_qr), false);
            }
        });
        bind(BPSQrDynamicTrans.State.ENTER_AMOUNT.toString(), amountAction, true);

        ActionShowQRCode showQRCode = new ActionShowQRCode(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionShowQRCode) action).setParam(getCurrentContext(),
                        getString(R.string.trans_display_qr), R.string.trans_dynamic_qr, transData, false);
            }
        });
        bind(BPSQrDynamicTrans.State.GEN_QR_CODE.toString(), showQRCode);

        // online action
        /*ActionTransOnline transOnlineAction = new ActionTransOnline(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionTransOnline) action).setParam(getCurrentContext(), transData);
            }
        });
        bind(BPSQrDynamicTrans.State.TRANS_ONLINE.toString(), transOnlineAction);*/

        gotoState(BPSQrDynamicTrans.State.ENTER_AMOUNT.toString());
    }

    enum State{
        ENTER_AMOUNT,
        GEN_QR_CODE,
        TRANS_ONLINE,
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {
        int ret = result.getRet();
        BPSQrDynamicTrans.State state = BPSQrDynamicTrans.State.valueOf(currentState);
        if (ret != TransResult.SUCC) {
            ToastUtils.showMessage("Fail");
            transEnd(result);
            return;
        }
        switch (state) {
            case ENTER_AMOUNT:
                transData.setAmount(result.getData().toString());
                gotoState(BPSQrDynamicTrans.State.GEN_QR_CODE.toString());
                break;
            case GEN_QR_CODE:
//                gotoState(BPSQrDynamicTrans.State.TRANS_ONLINE.toString());
                if (result.getRet() == TransResult.SUCC) {
                    // end trans
                    transEnd(result);
                }
                break;
            /*case TRANS_ONLINE:
                if (result.getRet() == TransResult.SUCC) {
                    // end trans
                    transEnd(result);
                }
                break;*/
            default:
                transEnd(result);
        }
    }
}
