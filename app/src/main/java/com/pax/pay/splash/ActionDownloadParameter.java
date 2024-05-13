package com.pax.pay.splash;

import android.content.Context;
import android.content.Intent;

import com.pax.abl.core.AAction;
import com.pax.pay.trans.action.activity.AutoInitialActivity;

public class ActionDownloadParameter extends AAction {
    /**
     * derived classes must call super(listener) to set
     *
     * @param listener {@link ActionStartListener}
     */
    private Context context;
    public ActionDownloadParameter(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context) {
        this.context = context;
    }
    @Override
    protected void process() {
        Intent downloadParamIntent = new Intent(context, DownloadParameterActivity.class);
        context.startActivity(downloadParamIntent);
    }
}


