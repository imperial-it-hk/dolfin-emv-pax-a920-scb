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
package com.pax.pay.password;


import th.co.bkkps.utils.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.pax.edc.R;
import com.pax.pay.BaseActivity;
import com.pax.pay.BaseActivityWithTickForAction;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.utils.ToastUtils;
import com.pax.view.keyboard.KeyboardUtils;

/**
 * Chang password
 *
 * @author Steven.W
 */
public abstract class BaseChangePwdActivity extends BaseActivityWithTickForAction {
    protected EditText edtNewPwd;
    protected EditText edtNewPwdConfirm;

    protected Button btnConfirm;
    protected String navTitle;
    protected String pwd;

    @Override
    protected void loadParam() {
        navTitle = getIntent().getStringExtra(EUIParamKeys.NAV_TITLE.toString());
    }

    @Override
    protected void onPause() {
        KeyboardUtils.hideSystemKeyboard(this, getWindow().getDecorView()); // AET-121 AET-270
        super.onPause();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_chg_pwd_layout;
    }

    @Override
    protected String getTitleString() {
        return navTitle;
    }

    @Override
    protected void initViews() {
        edtNewPwd = (EditText) findViewById(R.id.setting_new_pwd);
        edtNewPwd.requestFocus();
        edtNewPwdConfirm = (EditText) findViewById(R.id.setting_confirm_new_pwd);

        btnConfirm = (Button) findViewById(R.id.setting_confirm_pwd);

    }

    @Override
    protected void setListeners() {
        btnConfirm.setOnClickListener(this);
        TextView.OnEditorActionListener actionListener = new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
                    if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
                        Log.d("silly muhua", "物理按键的Enter, 等同于点击页面上的OK按钮");
                        onOkClicked();
                        return true;
                    }
                }
                return false;
            }
        };
        edtNewPwd.setOnEditorActionListener(actionListener);
        edtNewPwdConfirm.setOnEditorActionListener(actionListener);
    }

    @Override
    public void onClickProtected(View v) {
        if (v.getId() == R.id.setting_confirm_pwd) {
            onOkClicked();
        }
    }

    private void onOkClicked() {
        if (updatePwd()) {
            ToastUtils.showMessage(R.string.pwd_succ);
            finish();
        }
    }

    @Override
    protected boolean onOptionsItemSelectedSub(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelectedSub(item);
    }

    //save to SP
    protected abstract void savePwd();

    protected boolean updatePwd() {

        String newPWD = edtNewPwd.getText().toString();
        if ("".equals(newPWD)) {
            edtNewPwd.requestFocus();
            return false;
        }

        if (newPWD.length() != 6) {
            ToastUtils.showMessage(R.string.pwd_incorrect_length);
            return false;
        }

        String newAgainPWD = edtNewPwdConfirm.getText().toString();

        if ("".equals(newAgainPWD)) {
            edtNewPwdConfirm.requestFocus();
            return false;
        }

        if (!newAgainPWD.equals(newPWD)) {
            ToastUtils.showMessage(R.string.pwd_not_equal);
            return false;
        }
        pwd = edtNewPwd.getText().toString().trim();
        savePwd();
        return true;
    }

}
