package com.pax.pay.trans;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.trans.action.ActionInputPassword;
import com.pax.pay.trans.model.ETransType;

import th.co.bkkps.utils.DynamicOffline;

public class DynamicOfflineTrans  extends BaseTrans {

    public DynamicOfflineTrans(Context context, ETransType transType, TransEndListener transListener) {
        super(context, transType, transListener);
    }

    @Override
    protected void bindStateOnAction() {
        ActionInputPassword inputPasswordAction = new ActionInputPassword(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionInputPassword) action).setParam(getCurrentContext(), 6, getString(R.string.prompt_dynamic_offline_pwd), null);
            }
        });
        bind(State.INPUT_PASSWORD.toString(), inputPasswordAction, false);

        gotoState(State.INPUT_PASSWORD.toString());
    }

    enum State {
        INPUT_PASSWORD,
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {
        DynamicOfflineTrans.State state = DynamicOfflineTrans.State.valueOf(currentState);

        switch (state) {
            case INPUT_PASSWORD:
                boolean verifyPwd = DynamicOffline.getInstance().VerifyInputPassword((String)result.getData());
                if (verifyPwd==true) {
                    transEnd(new ActionResult(TransResult.SUCC, null));
                } else {
                    transEnd(new ActionResult(TransResult.ERR_PASSWORD, null));
                }
                break;
        }
    }



}
