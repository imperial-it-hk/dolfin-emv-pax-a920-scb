package com.pax.pay.trans.action;
import android.content.Context;
import android.content.Intent;
import com.pax.abl.core.AAction;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.action.activity.CheckQRActivity;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.TransData;

/**
 * Created by WITSUTA A on 5/9/2018.
 */

public class ActionCheckQR extends AAction {

    private Context context;
    private String title;
    private TransData transData;

    /**
     * derived classes must call super(listener) to set
     *
     * @param listener {@link ActionStartListener}
     */
    public ActionCheckQR(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, String title, TransData transData) {
        this.context = context;
        this.title = title;
        Component.setTransDataInstance(transData);
    }

    @Override
    protected void process() {
        Intent intent = new Intent(context, CheckQRActivity.class);
        intent.putExtra(EUIParamKeys.NAV_TITLE.toString(), title);
        context.startActivity(intent);
    }
}
