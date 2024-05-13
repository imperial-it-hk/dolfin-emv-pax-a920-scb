package com.pax.pay.trans.action;

import android.content.Context;
import android.content.Intent;

import com.pax.abl.core.AAction;
import com.pax.pay.trans.action.activity.AutoInitialActivity;

public class ActionAutoInitial extends AAction {
    /**
     * derived classes must call super(listener) to set
     *
     * @param listener {@link ActionStartListener}
     */
    private Context context;
    public ActionAutoInitial(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context) {
        this.context = context;
    }

    @Override
    protected void process() {
        Intent autoInitialIntent = new Intent(context, AutoInitialActivity.class);
        context.startActivity(autoInitialIntent);
    }
}
