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
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.BaseActivityWithTickForAction;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.utils.TickTimer;

public class DispSingleLineMsgActivity extends BaseActivityWithTickForAction {

    private Button btnConfirm;

    private String navTitle;
    private String prompt;
    private String content;

    private int tickTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tickTimer.start(tickTime);
    }

    @Override
    protected void loadParam() {
        Bundle bundle = getIntent().getExtras();
        navTitle = getIntent().getStringExtra(EUIParamKeys.NAV_TITLE.toString());
        prompt = bundle.getString(EUIParamKeys.PROMPT_1.toString());
        content = bundle.getString(EUIParamKeys.CONTENT.toString());
        tickTime = bundle.getInt(EUIParamKeys.TIKE_TIME.toString(), TickTimer.DEFAULT_TIMEOUT);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_disp_single_line_msg_layout;
    }

    @Override
    protected String getTitleString() {
        return navTitle;
    }

    @Override
    protected void initViews() {
        enableBackAction(true);
        TextView tvPrompt = (TextView) findViewById(R.id.version_prompt);
        tvPrompt.setText(prompt);

        TextView tvContent = (TextView) findViewById(R.id.version_tv);
        tvContent.setText(content);
        btnConfirm = (Button) findViewById(R.id.confirm_btn);
    }

    @Override
    protected void setListeners() {
        btnConfirm.setOnClickListener(this);

    }

    @Override
    public void onClickProtected(View v) {
        if (v.getId() == R.id.confirm_btn)
            finish(new ActionResult(TransResult.SUCC, null));
    }

    @Override
    protected boolean onOptionsItemSelectedSub(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(new ActionResult(TransResult.ERR_ABORTED, null));
            return true;
        }
        return super.onOptionsItemSelectedSub(item);
    }
}
