package com.pax.pay.trans.action;

import android.content.Context;
import android.content.Intent;

import com.pax.abl.core.AAction;
import com.pax.edc.R;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.action.activity.ShowQRRefActivity;
import com.pax.pay.utils.Utils;

/**
 * Created by SORAYA S on 26-Apr-18.
 */

public class ActionShowQRRef extends AAction {

    private Context context;
    private String title;
    private String msgRef;
    private String acqName;

    /**
     * derived classes must call super(listener) to set
     *
     * @param listener {@link ActionStartListener}
     */
    public ActionShowQRRef(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, String title, String msgRef, String acqName) {
        this.context = context;
        this.title = title;
        this.msgRef = msgRef;
        this.acqName = acqName;
    }

    @Override
    protected void process() {
        Intent intent = new Intent(context, ShowQRRefActivity.class);
        intent.putExtra(EUIParamKeys.NAV_TITLE.toString(), title);
        intent.putExtra(Utils.getString(R.string.qr_trans_ref), msgRef);
        intent.putExtra(Utils.getString(R.string.acquirer), acqName);
        context.startActivity(intent);
    }
}
