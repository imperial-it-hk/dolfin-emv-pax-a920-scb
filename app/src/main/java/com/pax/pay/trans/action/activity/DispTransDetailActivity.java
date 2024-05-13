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
package com.pax.pay.trans.action.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.pax.abl.core.ActionResult;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.BaseActivityWithTickForAction;
import com.pax.pay.ECR.EcrData;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.utils.ViewUtils;

import java.util.ArrayList;

import th.co.bkkps.utils.Log;

public class DispTransDetailActivity extends BaseActivityWithTickForAction {
    private Button btnConfirm;

    private String navTitle;
    private boolean navBack;
    private boolean isAutoDownloadMode;

    private ArrayList<String> leftColumns = new ArrayList<>();
    private ArrayList<String> rightColumns = new ArrayList<>();

    private boolean isBypassConfirm = false;

    @Override
    protected void loadParam() {
        Bundle bundle = getIntent().getExtras();
        navTitle = getIntent().getStringExtra(EUIParamKeys.NAV_TITLE.toString());
        navBack = getIntent().getBooleanExtra(EUIParamKeys.NAV_BACK.toString(), false);
        leftColumns = bundle.getStringArrayList(EUIParamKeys.ARRAY_LIST_1.toString());
        rightColumns = bundle.getStringArrayList(EUIParamKeys.ARRAY_LIST_2.toString());
        isBypassConfirm = bundle.getBoolean("BYPASS_CONFIRM", false);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.trans_detail_layout;
    }

    @Override
    protected String getTitleString() {
        return navTitle;
    }

    @Override
    protected void initViews() {
        Device.enableHomeRecentKey(false);
        LinearLayout llDetailContainer = (LinearLayout) findViewById(R.id.detail_layout);

        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.bottomMargin = 15;

        for (int i = 0; i < leftColumns.size(); i++) {
            LinearLayout layer = ViewUtils.genSingleLineLayout(DispTransDetailActivity.this, leftColumns.get(i),
                    rightColumns.get(i));
            llDetailContainer.addView(layer, params);
        }
        tickTimer.stop();

        btnConfirm = (Button) findViewById(R.id.confirm_btn);
        btnConfirm.setEnabled(true);

        if (isBypassConfirm && (EcrData.instance.isOnProcessing || EcrData.instance.isEcrProcess)) { onClickProtected(btnConfirm); }
    }

    @Override
    protected void setListeners() {
        enableBackAction(navBack);
        btnConfirm.setOnClickListener(this);
    }

    @Override
    public void onClickProtected(View v) {
        if (v.getId() == R.id.confirm_btn && btnConfirm.isEnabled())
            btnConfirm.setEnabled(false);
            Log.d("DispTransDetailActivity" ,"ACTIVITY--FINISH--SUCCESS");
            finish(new ActionResult(TransResult.SUCC, null));
    }

    @Override
    protected boolean onKeyBackDown() {
        Log.d("DispTransDetailActivity" ,"ACTIVITY--FINISH--ERROR");
        finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
        return true;
    }
}
