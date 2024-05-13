/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-12-27
 * Module Author: xiawh
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import com.pax.edc.R;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.trans.model.Controller;

public class WizardActivity extends BaseActivityWithTickForAction implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadParam();
        initViews();
        setListeners();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_settings;
    }

    @Override
    protected String getTitleString() {
        return getString(R.string.settings_title);
    }

    @Override
    protected void initViews() {
        if (FinancialApplication.getController().isFirstRun()) {
            doWizardActivity();
            finish();
        }
    }

    @Override
    protected void setListeners() {
        //do nothing
    }

    @Override
    protected void loadParam() {
        // do nothing
    }

   @Override
    protected boolean onOptionsItemSelectedSub(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            doWizardActivity();
            finish();
            return true;
        }
        return super.onOptionsItemSelectedSub(item);
    }

    private void doWizardActivity(){
        FinancialApplication.getController().set(Controller.NEED_SET_WIZARD, Controller.Constant.NO);
        Intent intent = getIntent();
        setResult(InitializeActivity.REQ_WIZARD, intent);
    }
}
