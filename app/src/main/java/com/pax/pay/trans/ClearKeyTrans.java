package com.pax.pay.trans;

import android.content.Context;
import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.trans.action.ActionClearKey;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.ToastUtils;

public class ClearKeyTrans extends BaseTrans {

    public ClearKeyTrans(Context context, TransEndListener transListener) {
        super(context, ETransType.CLEAR_KEY, transListener);
    }

    @Override
    protected void bindStateOnAction() {

        ActionClearKey clearKeyAction = new ActionClearKey(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionClearKey) action).setParam(getCurrentContext(), getString(R.string.trans_clear_key));
            }
        });
        bind(ClearKeyTrans.State.CLEAR_KEY.toString(), clearKeyAction, true);

        gotoState(ClearKeyTrans.State.CLEAR_KEY.toString());
    }

    enum State {
        CLEAR_KEY,
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {
        ClearKeyTrans.State state = ClearKeyTrans.State.valueOf(currentState);

        if (result.getRet() != TransResult.SUCC) {
            ToastUtils.showMessage("Fail");
            transEnd(result);
            return;
        }

        switch (state){
            case CLEAR_KEY:
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
