package com.pax.pay.trans;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.trans.action.ActionShowQRCode;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.utils.ToastUtils;

/**
 * Created by SORAYA S on 06-Feb-18.
 */

public class BPSQrStaticTrans extends BaseTrans {

    private static final String amount = "0";

    public BPSQrStaticTrans(Context context, TransEndListener transListener) {
        super(context, ETransType.BPS_QR_SALE_INQUIRY, transListener);
    }

    @Override
    protected void bindStateOnAction() {

        ActionShowQRCode showQRCode = new ActionShowQRCode(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                transData.setAmount(amount);
                ((ActionShowQRCode) action).setParam(getCurrentContext(),
                        getString(R.string.trans_display_qr), R.string.trans_static_qr, transData, false);
            }
        });
        bind(BPSQrStaticTrans.State.GEN_QR_CODE.toString(), showQRCode);

        gotoState(BPSQrStaticTrans.State.GEN_QR_CODE.toString());
    }

    enum State{
        GEN_QR_CODE,
        TRANS_ONLINE,
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {
        int ret = result.getRet();
        BPSQrStaticTrans.State state = BPSQrStaticTrans.State.valueOf(currentState);
        if (ret != TransResult.SUCC) {
            ToastUtils.showMessage("Fail");
            transEnd(result);
            return;
        }
        switch (state) {
            case GEN_QR_CODE:
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
