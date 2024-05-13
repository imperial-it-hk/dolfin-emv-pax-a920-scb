/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-11-30
 * Module Author: Steven.W
 * Description:
 *
 * ============================================================================
 */
package com.pax.settings;

import android.text.InputFilter;
import th.co.bkkps.utils.Log;
import android.view.View;
import android.widget.EditText;

import com.pax.abl.utils.EncUtils;
import com.pax.edc.R;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.utils.ToastUtils;


public class PwdManageDetailFragment extends BaseFragment {
    private EditText mPasswordOld;
    private EditText mPassword1;
    private EditText mPassword2;
    private int len = 0;
    private SysParam.StringParam pwdName = null;
    private String pwdValue = null;

    @Override
    protected int getLayoutId() {
        return R.layout.setting_pwd_manage_detail;
    }

    @Override
    protected void initData() {
        getPasswordParam();
    }

    @Override
    protected void initView(View view) {
        view.findViewById(R.id.setting_old_pwd_layout).setVisibility(View.VISIBLE);

        mPasswordOld = (EditText) view.findViewById(R.id.setting_old_pwd);
        mPassword1 = (EditText) view.findViewById(R.id.setting_new_pwd);
        mPassword2 = (EditText) view.findViewById(R.id.setting_confirm_new_pwd);

        mPasswordOld.setFilters(new InputFilter[]{new InputFilter.LengthFilter(len)});
        mPassword1.setFilters(new InputFilter[]{new InputFilter.LengthFilter(len)});
        mPassword2.setFilters(new InputFilter[]{new InputFilter.LengthFilter(len)});
        mPasswordOld.requestFocus();
        view.findViewById(R.id.setting_confirm_pwd).setOnClickListener(this);
    }

    @Override
    public void onClickProtected(View v) {
        boolean flag = false;
        // 比较原密码，两次新密码，保存
        Log.d(TAG, "============================================");
        if (pwdValue == null || "".equals(pwdValue)) {
            ToastUtils.showMessage(R.string.input_old_password);
        } else if (!EncUtils.sha1(mPasswordOld.getText().toString()).equals(pwdValue)) {
            ToastUtils.showMessage(R.string.error_old_password);
        } else if (mPassword1 == null || "".equals(mPassword1.getText().toString())) {
            ToastUtils.showMessage(R.string.input_new_password);
        } else if (mPassword2 == null || "".equals(mPassword2.getText().toString())) {
            ToastUtils.showMessage(R.string.input_again_new_password);
        } else if ((mPassword1.length() != len)) {
            ToastUtils.showMessage(R.string.error_input_length);
        } else if (!mPassword1.getText().toString().equals(mPassword2.getText().toString())) {
            ToastUtils.showMessage(R.string.error_password_no_same);
        } else {
            flag = true;
        }

        if (flag) {
            // 保存密码
            if (SysParam.StringParam.SEC_SYS_PWD.equals(pwdName)) {
                String password = mPassword1.getText().toString();
                FinancialApplication.getSysParam().set(SysParam.StringParam.SEC_SYS_PWD, EncUtils.sha1(password));
            }
            savePwd();
            ToastUtils.showMessage(R.string.password_modify_success);
            // AET-80
            mPasswordOld.setText("");
            mPassword1.setText("");
            mPassword2.setText("");
        }
    }

    private void getPasswordParam() {
        len = 8;
        pwdName = SysParam.StringParam.SEC_SYS_PWD;
        pwdValue = FinancialApplication.getSysParam().get(pwdName, "");
    }

    private void savePwd() {
        FinancialApplication.getSysParam().set(pwdName, EncUtils.sha1(mPassword1.getText().toString()));
    }
}
