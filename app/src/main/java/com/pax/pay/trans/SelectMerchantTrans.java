package com.pax.pay.trans;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.pay.trans.action.ActionSelectMerchant;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.MerchantProfileManager;

public class SelectMerchantTrans extends BaseTrans {

    private Context context = null;

    public SelectMerchantTrans (Context context) {
        super(context, ETransType.DUMMY, null, true);
        this.context = context;
    }

    @Override
    protected void bindStateOnAction() {
        ActionSelectMerchant selMercAction = new ActionSelectMerchant(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionSelectMerchant) action).setParam(getCurrentContext());
            }
        });
        bind(State.SELECT_MERCHANT.toString(), selMercAction);

        gotoState(State.SELECT_MERCHANT.toString());
    }

    enum State {
        SELECT_MERCHANT
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {
        if (result.getData() instanceof String) {
            MerchantProfileManager.INSTANCE.applyProfileAndSave((String)result.getData());
        }
        
        transEnd(result);
    }
}
