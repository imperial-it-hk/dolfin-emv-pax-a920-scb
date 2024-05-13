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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.BaseActivityWithTickForAction;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.record.Printer;
import com.pax.pay.utils.ViewUtils;

import java.util.ArrayList;

public class DispParamActivity extends BaseActivityWithTickForAction {
    private Button btnConfirm;
    private String navTitle;
    private boolean navBack;
    private Bitmap bitmap;
    private ImageView imageView;
    final ConditionVariable cv = new ConditionVariable();

    @Override
    protected void loadParam() {
        Bundle bundle = getIntent().getExtras();
        navTitle = getIntent().getStringExtra(EUIParamKeys.NAV_TITLE.toString());
        navBack = getIntent().getBooleanExtra(EUIParamKeys.NAV_BACK.toString(), false);
        byte[] bis = bundle.getByteArray(EUIParamKeys.BITMAP.toString());
        bitmap = BitmapFactory.decodeByteArray(bis, 0, bis.length);
        cv.open();
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
        LinearLayout llDetailContainer = (LinearLayout) findViewById(R.id.detail_layout);

        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.bottomMargin = 15;

        imageView =  new ImageView(DispParamActivity.this);
        imageView.setImageBitmap(bitmap);
        llDetailContainer.addView(imageView, params);

        tickTimer.stop();
        btnConfirm = (Button) findViewById(R.id.confirm_btn);
    }

    @Override
    protected void setListeners() {
        enableBackAction(navBack);
        btnConfirm.setOnClickListener(this);
    }

    @Override
    public void onClickProtected(View v) {
        if (v.getId() == R.id.confirm_btn){
            finish(new ActionResult(TransResult.SUCC, null));
        }
    }

    @Override
    protected boolean onKeyBackDown() {
        finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
        return true;
    }

}
