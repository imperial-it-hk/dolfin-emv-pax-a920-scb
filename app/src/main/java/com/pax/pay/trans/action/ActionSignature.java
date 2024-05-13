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
import com.pax.edc.R;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.action.activity.GetSignatureActivity;
import com.pax.pay.trans.action.activity.SignatureActivity;
import com.pax.pay.uart.SP200_serialAPI;
import com.pax.pay.utils.ToastUtils;

import static com.pax.pay.utils.Utils.getString;

public class ActionSignature extends AAction {
    private String amount;
    private boolean skipSign;
    private Context context;

    public ActionSignature(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, String amount) {
        this.context = context;
        this.amount = amount;
        this.skipSign = false;
    }

    public void setParam(Context context, String amount, boolean skipSign) {
        this.context = context;
        this.amount = amount;
        this.skipSign = skipSign;
    }

    @Override
    protected void process() {
        Intent intent = null;
        boolean isSP200Enable = SP200_serialAPI.getInstance().isSp200Enable();
        if (isSP200Enable)
        {
            int iRet = SP200_serialAPI.getInstance().checkStatusSP200();
            if(iRet == 0) {
                intent = new Intent(context, GetSignatureActivity.class);
            } else {
                ToastUtils.showMessage(getString(R.string.err_pinpad_not_response));
                intent = new Intent(context, SignatureActivity.class);
            }
        } else {
            intent = new Intent(context, SignatureActivity.class);
            intent.putExtra(EUIParamKeys.SUPPORTBYPASS.toString(), skipSign);
        }
        intent.putExtra(EUIParamKeys.TRANS_AMOUNT.toString(), amount);
        context.startActivity(intent);
    }
}
