/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-1-11
 * Module Author: lixc
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.trans.action;

import android.content.Context;
import android.content.Intent;

import com.pax.abl.core.AAction;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.record.TransQueryActivity;
import com.pax.pay.trans.action.activity.EnterAmountActivity;

public class ActionAcquirerHistory extends AAction {
    private Context context;
    private String acqName;

    public ActionAcquirerHistory(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, String acqName) {
        this.context = context;
        this.acqName = acqName;
    }


    @Override
    protected void process() {
        Intent intent = new Intent(context, TransQueryActivity.class);
        intent.putExtra(EUIParamKeys.ACQUIRER_NAME.toString(), acqName);
        context.startActivity(intent);
    }
}
