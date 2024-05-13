/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-11-25
 * Module Author: Steven.W
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.trans.action;

import android.content.Context;
import android.content.Intent;

import com.pax.abl.core.AAction;
import com.pax.pay.InitializeActivity;
import com.pax.pay.InputPwdActivity;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.action.activity.SignatureActivity;

public class ActionLockTerminal extends AAction {
    private Context context;

    public ActionLockTerminal(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context) {
        this.context = context;

    }

    @Override
    protected void process() {
        Intent intent = new Intent(context, InputPwdActivity.class);
        context.startActivity(intent);
    }
}
