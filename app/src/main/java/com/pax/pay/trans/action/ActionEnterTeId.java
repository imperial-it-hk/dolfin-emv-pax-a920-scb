package com.pax.pay.trans.action;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.pax.abl.core.AAction;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.action.activity.EnterTeIdActivity;

public class ActionEnterTeId extends AAction {
    private Context context;
    private String title;
    private boolean scbTle;
    private boolean isAutoDownloadMode;

    public ActionEnterTeId(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, String title, boolean scbTle) {
        this.context = context;
        this.title = title;
        this.scbTle = scbTle;
        this.isAutoDownloadMode=false;
    }

    public void setParam(Context context, String title, boolean scbTle, boolean isAutoDownloadMode) {
        this.context = context;
        this.title = title;
        this.scbTle = scbTle;
        this.isAutoDownloadMode = isAutoDownloadMode;
    }

    @Override
    protected void process() {
        Intent intent = new Intent(context, EnterTeIdActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(EUIParamKeys.NAV_TITLE.toString(), title);
        bundle.putBoolean(EUIParamKeys.NAV_BACK.toString(), true);
        bundle.putBoolean("SCB_TLE", scbTle);
        bundle.putBoolean("BYPASS_MODE", isAutoDownloadMode);
        intent.putExtras(bundle);
        context.startActivity(intent);
    }
}