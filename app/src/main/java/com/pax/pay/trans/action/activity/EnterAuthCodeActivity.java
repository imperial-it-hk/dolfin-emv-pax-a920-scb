/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-1-10
 * Module Author: lixc
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.trans.action.activity;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.BaseActivityWithTickForAction;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.component.Component;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.EditorActionListener;
import com.pax.pay.utils.Utils;

public class EnterAuthCodeActivity extends BaseActivityWithTickForAction {

    private EditText authCodeTv;

    private String title;
    private String prompt1;
    private String amount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authCodeTv.requestFocus();
        authCodeTv.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.showSoftInput(authCodeTv, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 200);
    }

    @Override
    protected void loadParam() {
        title = getIntent().getStringExtra(EUIParamKeys.NAV_TITLE.toString());
        prompt1 = getIntent().getStringExtra(EUIParamKeys.PROMPT_1.toString());
        amount = getIntent().getStringExtra(EUIParamKeys.TRANS_AMOUNT.toString());
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_enter_auth_code;
    }

    @Override
    protected String getTitleString() {
        return title;
    }

    @Override
    protected void initViews() {
        TextView amountTv = (TextView) findViewById(R.id.amount_txt);
        LinearLayout amountLayout = (LinearLayout) findViewById(R.id.trans_amount_layout);
        if (amount != null && !amount.isEmpty()) {
            amount = CurrencyConverter.convert(Utils.parseLongSafe(amount, 0));
            amountTv.setText(amount);
        } else {
            amountLayout.setVisibility(View.INVISIBLE);
        }

        TextView promptTv1 = (TextView) findViewById(R.id.prompt_title);
        promptTv1.setText(prompt1);

        authCodeTv = (EditText) findViewById(R.id.auth_code_input_text);
    }

    @Override
    protected void setListeners() {
        authCodeTv.setOnEditorActionListener(new EditorActionListener() {
            @Override
            protected void onKeyOk() {
                String authCode = authCodeTv.getText().toString();
                if (!authCode.isEmpty()) {
                    String paddedAuthCode = Component.getPaddedString(authCode,6, '0');
                    finish(new ActionResult(TransResult.SUCC, paddedAuthCode));
                }
            }

            @Override
            protected void onKeyCancel() {
                authCodeTv.setText("");
                finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
            }
        });
    }

    @Override
    protected boolean onKeyBackDown() {
        finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
        return true;
    }
}
