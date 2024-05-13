package com.pax.pay.splash;

import android.content.Context;
import android.content.Intent;

import com.pax.abl.core.AAction;
import com.pax.pay.SelfTestActivity;

public class ActionSelfTest extends AAction {

    public ActionSelfTest (AAction.ActionStartListener listener) {
        super(listener);
    }

    @Override
    public void setEndListener(ActionEndListener listener) {
        super.setEndListener(listener);
    }

    private Context context = null;
    public void setParam(Context context) {
        this.context = context;
    }

    @Override
    protected void process() {
        Intent intent = new Intent(context, SelfTestActivity.class);
        context.startActivity(intent);
    }


}
