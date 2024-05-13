package com.pax.pay.trans.action;

import android.content.Intent;

import com.pax.abl.core.AAction;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.action.activity.SearchCardActivity;
import com.pax.pay.trans.action.activity.SearchCardForPanActivity;

public class ActionSearchCardForPan extends ActionSearchCard{
    public ActionSearchCardForPan(ActionStartListener listener) {
        super(listener);
    }

    @Override
    protected void process() {
        Intent intent = new Intent(context, SearchCardForPanActivity.class);
        intent.putExtra(EUIParamKeys.NAV_TITLE.toString(), title);
        intent.putExtra(EUIParamKeys.NAV_BACK.toString(), true);
        intent.putExtra(EUIParamKeys.TRANS_AMOUNT.toString(), amount);
        intent.putExtra(EUIParamKeys.CARD_SEARCH_MODE.toString(), mode);
        intent.putExtra(EUIParamKeys.TRANS_DATE.toString(), date);
        intent.putExtra(EUIParamKeys.SEARCH_CARD_PROMPT.toString(), searchCardPrompt);
        intent.putExtra(EUIParamKeys.SUPP_DUAL_CARD.toString(), supportDualCard);
        intent.putExtra("activity_timeout", this.timeOut);
        context.startActivity(intent);
    }
}
