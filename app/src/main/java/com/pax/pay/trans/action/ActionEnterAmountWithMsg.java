package com.pax.pay.trans.action;

import android.content.Context;
import android.content.Intent;

import com.pax.abl.core.AAction;
import com.pax.edc.R;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.action.activity.EnterAmountWithMsgActivity;
import com.pax.pay.utils.Utils;

/**
 * Created by WITSUTA A on 5/14/2018.
 */

public class ActionEnterAmountWithMsg  extends AAction {
    private Context context;
    private String title;
    private String MsgHeader;

    public ActionEnterAmountWithMsg(ActionStartListener listener) {
        super(listener);
    }


    public void setParam(Context context, String title, String MsgHeader) {
        this.context = context;
        this.title = title;
        this.MsgHeader = MsgHeader;
    }

    @Override
    protected void process() {
        Intent intent = new Intent(context, EnterAmountWithMsgActivity.class);
        intent.putExtra(EUIParamKeys.NAV_TITLE.toString(), title);
        intent.putExtra(Utils.getString(R.string.wallet_text_header), MsgHeader);
        context.startActivity(intent);
    }
}

