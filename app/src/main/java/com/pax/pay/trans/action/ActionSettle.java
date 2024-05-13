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
import android.os.Bundle;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.action.activity.SettleActivity;

import java.util.ArrayList;
import java.util.List;

public class ActionSettle extends AAction {
    private Context context;
    private String title;
    private List<String> list;
    private boolean isEcrProcess;
    private boolean isSupportEReceipt;
    private boolean isBypassConfirmSettle;
    private boolean isSettleAllMerchants;

    public ActionSettle(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, String title, List<String> list) {
        this.context = context;
        this.title = title;
        this.list = list;
        this.isEcrProcess = false;
        this.isSupportEReceipt = false;
    }

    public void setParam(Context context, String title, List<String> list, boolean isEcrProcess, boolean isBypassConfirmSettle, boolean isSettleAllMerchants, boolean isSupportEReceipt) {
        this.context = context;
        this.title = title;
        this.list = list;
        this.isEcrProcess = isEcrProcess;
        this.isSupportEReceipt = isSupportEReceipt;
        this.isBypassConfirmSettle = isBypassConfirmSettle;
        this.isSettleAllMerchants = isSettleAllMerchants;
    }

    @Override
    protected void process() {
        if (list.size() == 0) {
            setResult(new ActionResult(TransResult.ERR_HOST_NOT_FOUND, null));
            return;
        }
        FinancialApplication.getApp().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Intent intent = new Intent(context, SettleActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString(EUIParamKeys.NAV_TITLE.toString(), title);
                bundle.putBoolean(EUIParamKeys.NAV_BACK.toString(), true);
                bundle.putBoolean(EUIParamKeys.ECR_PROCESS.toString(), isEcrProcess);
                bundle.putBoolean(EUIParamKeys.SUPPORT_E_RECEIPT.toString(), isSupportEReceipt);
                bundle.putBoolean(EUIParamKeys.BYPASS_CONFIRM_SETTLE.toString(), isBypassConfirmSettle);
                bundle.putBoolean(EUIParamKeys.SETTLE_ALL_MERCHANTS.toString(), isSettleAllMerchants);
                bundle.putStringArrayList(EUIParamKeys.ARRAY_LIST_2.toString(), new ArrayList<>(list));

                intent.putExtras(bundle);
                context.startActivity(intent);
            }
        });

    }
}
