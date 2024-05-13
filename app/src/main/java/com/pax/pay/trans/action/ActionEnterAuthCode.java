/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-1-10
 * Module Author: lixc
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.trans.action;

import android.content.Context;
import android.content.Intent;

import com.pax.abl.core.AAction;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.action.activity.EnterAuthCodeActivity;

public class ActionEnterAuthCode extends AAction {
    private Context context;
    private String title;
    private String header;
    private String amount;

    public ActionEnterAuthCode(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, String title, String header, String amount) {
        this.context = context;
        this.title = title;
        this.header = header;
        this.amount = amount;
    }

    @Override
    protected void process() {
        Intent intent = new Intent(context, EnterAuthCodeActivity.class);
        intent.putExtra(EUIParamKeys.NAV_TITLE.toString(), title);
        intent.putExtra(EUIParamKeys.PROMPT_1.toString(), header);
        intent.putExtra(EUIParamKeys.TRANS_AMOUNT.toString(), amount);
        context.startActivity(intent);
    }
}
