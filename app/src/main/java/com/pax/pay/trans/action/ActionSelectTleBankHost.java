package com.pax.pay.trans.action;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.pay.trans.action.activity.TleBankHostActivity;

import th.co.bkkps.bpsapi.TransResult;

public class ActionSelectTleBankHost extends AAction {
    /**
     * derived classes must call super(listener) to set
     *
     * @param listener {@link ActionStartListener}
     */
    public ActionSelectTleBankHost(ActionStartListener listener) {
        super(listener);
    }

    private Context context = null;
    private String disp_title = null;
    private String disp_text = null;
    public void setParam (Context context, String disp_title, String disp_text) {
        this.context = context;
        this.disp_title = disp_title;
        this.disp_text = disp_text;
    }

    @Override
    protected void process() {
        if (context !=null) {
            Intent intent = new Intent(context, TleBankHostActivity.class) ;
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("DISP_TITLE", disp_title);
            intent.putExtra("DISP_TEXT", disp_text);
            context.startActivity(intent);
        } else {
            this.setFinished(true);
            this.setResult(new ActionResult(TransResult.ERR_HOST_NOT_FOUND, null));
        }
    }
}
