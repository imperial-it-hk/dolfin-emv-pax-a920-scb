package com.pax.pay.trans;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.action.ActionSettle;
import com.pax.pay.trans.model.ETransType;

import java.util.Arrays;
import java.util.List;

/**
 * Created by WITSUTA A on 12/4/2018.
 */

public class QRSettleTrans extends BaseTrans {

    public QRSettleTrans(Context context, TransEndListener transListener) {
        super(context, ETransType.SETTLE, transListener);
    }

    @Override
    protected void bindStateOnAction() {

        ActionSettle settleAction = new ActionSettle(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                List<String> selectAcqs = Arrays.asList(Constants.ACQ_QRC);
                ((ActionSettle) action).setParam(getCurrentContext(),
                        getString(R.string.menu_settle), selectAcqs);
            }

        });
        bind(PromptPaySettleTrans.State.SETTLE.toString(), settleAction);

        gotoState(PromptPaySettleTrans.State.SETTLE.toString());
    }

    enum State {
        SETTLE,
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {
        transEnd(result);
        return;
    }
}