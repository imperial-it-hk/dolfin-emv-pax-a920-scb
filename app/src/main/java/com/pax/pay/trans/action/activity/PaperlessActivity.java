/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-5-27
 * Module Author: lixc
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.trans.action.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.BaseActivityWithTickForAction;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.action.ActionInputTransData.EInputType;
import com.pax.pay.utils.EditorActionListener;
import com.pax.pay.utils.EnterAmountTextWatcher;
import com.pax.pay.utils.ToastUtils;

public class PaperlessActivity extends BaseActivityWithTickForAction {
    private Button confirmBtn;

    private String prompt;
    private String navTitle;

    private EInputType inputType;

    private EditText editText = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEditText();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        editText.requestFocus();
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        editText.setText("");
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_paperless;
    }

    @Override
    protected void loadParam() {
        prompt = getIntent().getStringExtra(EUIParamKeys.PROMPT_1.toString());
        inputType = (EInputType) getIntent().getSerializableExtra(EUIParamKeys.INPUT_TYPE.toString());
        navTitle = getIntent().getStringExtra(EUIParamKeys.NAV_TITLE.toString());
    }

    @Override
    protected String getTitleString() {
        return navTitle;
    }

    @Override
    protected void initViews() {
        TextView promptText = (TextView) findViewById(R.id.prompt_amount);
        promptText.setText(prompt);

        editText = (EditText) findViewById(R.id.input_data_sp);

        confirmBtn = (Button) findViewById(R.id.info_confirm);
    }

    private void setEditText() {
        if (inputType == EInputType.PHONE) {
            editText.setInputType(InputType.TYPE_CLASS_PHONE);
        }

        editText.addTextChangedListener(new EnterAmountTextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
                confirmBtnChange();
            }
        });

        editText.setOnEditorActionListener(new EditorActionListener() {

            @Override
            protected void onKeyOk() {
                quickClickProtection.stop();
                onClick(confirmBtn);
            }

            @Override
            protected void onKeyCancel() {
                //do nothing
            }
        });
    }

    @Override
    protected void setListeners() {
        confirmBtn.setOnClickListener(this);
    }

    @Override
    public void onClickProtected(View v) {
        if (v.getId() == R.id.info_confirm)
            onConfirmResult(process());
    }

    private void onConfirmResult(String content) {
        if (content == null || content.isEmpty()) {
            ToastUtils.showMessage(R.string.please_input_again);
            editText.setText("");
            return;
        }
        finish(new ActionResult(TransResult.SUCC, content));
    }

    /**
     * 输入数值检查
     */
    private String process() {
        String content = editText.getText().toString().trim();

        if (content.isEmpty()) {
            return null;
        }

        if (inputType == EInputType.EMAIL && !isEmailValid(content)) {
            return null;
        }

        return content;
    }

    //Check the email number
    private boolean isEmailValid(String email) {
        String regex = "\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*";
        return email.matches(regex);
    }

    private void confirmBtnChange() {
        String content = editText.getText().toString();
        confirmBtn.setEnabled(!content.isEmpty());
    }

    @Override
    protected boolean onKeyBackDown() {
        finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
        return true;
    }
}