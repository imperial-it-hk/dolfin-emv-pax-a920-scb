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
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pax.abl.core.ActionResult;
import com.pax.abl.utils.EncUtils;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.BaseActivityWithTickForAction;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.EditorActionListener;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

public class EnterPinTestActivity extends BaseActivityWithTickForAction {

    private String title;
    private String prompt2;
    private String prompt1;
    private String totalAmount;
    private String tipAmount;

    private EditText pinEdit;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_input_pin_test;
    }

    @Override
    protected void loadParam() {
        title = getIntent().getStringExtra(EUIParamKeys.NAV_TITLE.toString());
        prompt1 = getIntent().getStringExtra(EUIParamKeys.PROMPT_1.toString());
        prompt2 = getIntent().getStringExtra(EUIParamKeys.PROMPT_2.toString());
        totalAmount = getIntent().getStringExtra(EUIParamKeys.TRANS_AMOUNT.toString());
        tipAmount = getIntent().getStringExtra(EUIParamKeys.TIP_AMOUNT.toString());
    }


    @Override
    protected String getTitleString() {
        return title;
    }

    @Override
    protected void initViews() {
        enableActionBar(false);

        TextView totalAmountTv = (TextView) findViewById(R.id.total_amount_txt);
        LinearLayout totalAmountLayout = (LinearLayout) findViewById(R.id.trans_total_amount_layout);
        if (totalAmount != null && !totalAmount.isEmpty()) {
            totalAmount = CurrencyConverter.convert(Utils.parseLongSafe(totalAmount, 0));
            totalAmountTv.setText(totalAmount);
        } else {
            totalAmountLayout.setVisibility(View.INVISIBLE);
        }


        TextView tipAmountTv = (TextView) findViewById(R.id.tip_amount_txt);
        LinearLayout tipAmountLayout = (LinearLayout) findViewById(R.id.trans_tip_amount_layout);
        if (tipAmount != null && !tipAmount.isEmpty()) {
            tipAmount = CurrencyConverter.convert(Utils.parseLongSafe(tipAmount, 0));
            tipAmountTv.setText(tipAmount);
        } else {
            tipAmountLayout.setVisibility(View.INVISIBLE);
        }

        // hide trip when trip is not enable.
        if (!FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_SUPPORT_TIP)) {
            tipAmountLayout.setVisibility(View.INVISIBLE);
        }

        TextView promptTv1 = (TextView) findViewById(R.id.prompt_title);
        promptTv1.setText(prompt1);

        TextView promptTv2 = (TextView) findViewById(R.id.prompt_no_pin);
        if (prompt2 != null) {
            promptTv2.setText(prompt2);
        } else {
            promptTv2.setVisibility(View.INVISIBLE);
        }

        pinEdit = (EditText) findViewById(R.id.pin_input_text);
        pinEdit.setOnEditorActionListener(new EditorActionListener() {
            @Override
            protected void onKeyOk() {
                String pin = pinEdit.getText().toString();
                if (pin.length() == 0) {
                    finish(new ActionResult(TransResult.SUCC, null));
                } else {
                    finish(new ActionResult(TransResult.SUCC, EncUtils.sha1(pin).substring(0, 16)));
                }
            }

            @Override
            protected void onKeyCancel() {
                finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
            }
        });
        pinEdit.requestFocus();
    }

    @Override
    protected void setListeners() {
        //do nothing
    }
}