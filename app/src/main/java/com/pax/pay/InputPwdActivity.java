/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-12-27
 * Module Author: lixc
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.style.AbsoluteSizeSpan;
import android.view.Gravity;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.pax.abl.utils.EncUtils;
import com.pax.edc.R;
import com.pax.pay.ECR.EcrData;
import com.pax.pay.ECR.EcrProcessClass;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.utils.CommunicationUtils;
import com.pax.pay.utils.EditorActionListener;
import com.pax.pay.utils.ToastUtils;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;
import com.pax.view.dialog.DialogUtils;

import th.co.bkkps.linkposapi.LinkPOSApi;
import th.co.bkkps.linkposapi.action.activity.LinkPosAppInitialActivity;

public class InputPwdActivity extends BaseActivity {

    private EditText edtPwd;
    private TextView carrierName;
    private TextView appVersion;
    private Context mContext;

    public static void setHideActivity() {

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);
        mContext = getApplicationContext();

        Intent intent = new Intent(InputPwdActivity.this, LinkPosAppInitialActivity.class);
        startActivity(intent);
    }

    @Override
    protected void loadParam() {
        // do nothing
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (EcrProcessClass.useLinkPos) {
            LinkPOSApi.INSTANCE.bindService(mContext);
            EcrData.instance.isOnHomeScreen = true;
        }
        //todo linkpos_cz
//        FinancialApplication.getEcrProcess().mCommManage.StartReceive();
//        FinancialApplication.getEcrProcess().setEcrDismissListener(new EcrDismissListener() {
//            @Override
//            public void onDismiss(int result) {
//                onDismiss(result, true);
//            }
//
//            @Override
//            public void onDismiss(int result, boolean showDialog) {
//                if (result != TransResult.SUCC) {
//                    if (showDialog || result != TransResult.ERR_ABORTED) {
//                        DialogUtils.showErrMessage(InputPwdActivity.this, "Ecr process error", EcrProcessResult.getMessage(result), null, Constants.FAILED_DIALOG_SHOW_TIME);
//                    }
//                }
//            }
//        });

        String carrier = CommunicationUtils.getCarrierName();
        if (carrier != null && !carrier.isEmpty()) {
            carrierName.setText(String.format("%s %s", getString(R.string.default_carrier_name), carrier));
        } else {
            carrierName.setText("");
        }
        appVersion.setText(String.format("%s %s", getString(R.string.default_app_version), FinancialApplication.getVersion()));
    }

    @Override
    protected void onPause() {
        super.onPause();
        EcrData.instance.isOnHomeScreen = false;
//        FinancialApplication.getEcrProcess().mCommManage.StopReceive();//todo linkpos_cz
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_initialize_pwd_layout;
    }

    @Override
    protected void initViews() {
        enableActionBar(false);
        edtPwd = (EditText) findViewById(R.id.operator_pwd_edt);
        SpannableString ss = new SpannableString(Utils.getString(R.string.plz_enter_pwd));
        AbsoluteSizeSpan ass = new AbsoluteSizeSpan(32, true);
        ss.setSpan(ass, 0, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        edtPwd.setHint(new SpannedString(ss));
        edtPwd.requestFocus();
        carrierName = findViewById(R.id.carrier_name);
        appVersion = findViewById(R.id.app_version);
    }

    @Override
    protected void setListeners() {
        edtPwd.setOnEditorActionListener(new PwdActionListener());
    }

    private class PwdActionListener extends EditorActionListener {
        @Override
        public void onKeyOk() {
            process();
        }

        @Override
        public void onKeyCancel() {
            exit();
        }

        /**
         * check password
         */
        private void process() {
            String password = edtPwd.getText().toString().trim();
            if (password.isEmpty()) {
                edtPwd.setFocusable(true);
                edtPwd.requestFocus();
                return;
            }
            if (!EncUtils.sha1(password).equals(FinancialApplication.getSysParam().get(SysParam.StringParam.SEC_TERMINAL_PWD))) {
                //ToastUtils.showMessage(R.string.error_password);
                Toast toast = new Toast(InputPwdActivity.this);
                toast.setText(R.string.err_password);
                toast.setDuration(Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0,0);
                toast.show();

                edtPwd.setText("");
                edtPwd.setFocusable(true);
                edtPwd.requestFocus();
                return;
            }

            // back to main
            TerminalLockCheck.getInstance().init();
            InputMethodManager imm = (InputMethodManager) getSystemService(InputPwdActivity.this.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(edtPwd.getWindowToken(), 0);

            finish();
        }
    }

    @Override
    protected boolean onKeyBackDown() {
        // exit app
        exit();
        return true;
    }

    private void exit() {
        DialogUtils.showExitAppDialog(InputPwdActivity.this);
        setResult(RESULT_CANCELED);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Intent intent = getIntent();
        setResult(RESULT_OK, intent);
        finish();
    }
}
