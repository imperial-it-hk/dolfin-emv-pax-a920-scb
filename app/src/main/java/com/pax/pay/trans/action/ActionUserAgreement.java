/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-03-10
 * Module Author: huangwp
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.trans.action;

import android.content.Context;
import android.content.Intent;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.trans.action.activity.UserAgreementActivity;
import com.pax.settings.SysParam;

public class ActionUserAgreement extends AAction {
    private Context context;

    public ActionUserAgreement(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context) {
        this.context = context;
    }

    @Override
    protected void process() {
        if (!FinancialApplication.getSysParam().get(SysParam.BooleanParam.SUPPORT_USER_AGREEMENT)) {
            setResult(new ActionResult(TransResult.SUCC, null));
            return;
        }

        Intent intent = new Intent(context, UserAgreementActivity.class);
        context.startActivity(intent);
    }
}
