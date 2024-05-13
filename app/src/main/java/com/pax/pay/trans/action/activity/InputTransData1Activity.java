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

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Editable;
import android.text.InputFilter;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.pax.abl.core.ActionResult;
import com.pax.dal.IScanner;
import com.pax.dal.IScanner.IScanListener;
import com.pax.dal.entity.EScannerType;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.BaseActivityWithTickForAction;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.action.ActionInputTransData.EInputType;
import com.pax.pay.trans.component.Component;
import com.pax.pay.uart.SP200_serialAPI;
import com.pax.pay.utils.EditorActionListener;
import com.pax.pay.utils.EnterAmountTextWatcher;
import com.pax.pay.utils.TickTimer;
import com.pax.pay.utils.ToastUtils;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;
import com.pax.view.keyboard.CustomKeyboardEditText;

import th.co.bkkps.utils.Log;

public class InputTransData1Activity extends BaseActivityWithTickForAction {

    private Button confirmBtn;

    private String prompt;
    private String navTitle;
    private String remark;

    private EInputType inputType;

    private boolean isGetLastTrans;
    private boolean isPaddingZero;

    private int maxLen;
    private int minLen;

    private CustomKeyboardEditText mEditText = null;

    private boolean isScanner = false;
    private String qrCode;
    private ImageButton mBtnScanner;
    private IScanner scanner;
    private Thread threadSp200ReceiveQR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEditText();
        if (isScanner) {
            if (SP200_serialAPI.getInstance().isSp200Enable())
                setSP200();
            else
                setBtnScanner();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mEditText.setText("");
        if (qrCode != null)
            mEditText.setText(qrCode);
        qrCode = null;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_input_trans_data1;
    }

    @Override
    protected void loadParam() {
        prompt = getIntent().getStringExtra(EUIParamKeys.PROMPT_1.toString());
        remark = getIntent().getStringExtra(EUIParamKeys.PROMPT_REMARK.toString());
        inputType = (EInputType) getIntent().getSerializableExtra(EUIParamKeys.INPUT_TYPE.toString());
        maxLen = getIntent().getIntExtra(EUIParamKeys.INPUT_MAX_LEN.toString(), 6);
        minLen = getIntent().getIntExtra(EUIParamKeys.INPUT_MIN_LEN.toString(), 0);
        navTitle = getIntent().getStringExtra(EUIParamKeys.NAV_TITLE.toString());
        isGetLastTrans = getIntent().getBooleanExtra(EUIParamKeys.GET_LAST_TRANS_UI.toString(), false);
        isPaddingZero = getIntent().getBooleanExtra(EUIParamKeys.INPUT_PADDING_ZERO.toString(), true);
        isScanner = getIntent().getBooleanExtra(EUIParamKeys.USE_SCANNER.toString(), false);
    }

    @Override
    protected String getTitleString() {
        return navTitle;
    }

    @Override
    protected void initViews() {
        TextView promptText = (TextView) findViewById(R.id.prompt_amount);
        promptText.setText(prompt);

        confirmBtn = (Button) findViewById(R.id.info_confirm);
        TextView promptDoLast = (TextView) findViewById(R.id.prompt_do_last);
        if (remark != null)
            promptDoLast.setText(remark);
        else if (!isGetLastTrans) {
            promptDoLast.setVisibility(View.INVISIBLE);
        }
    }

    private void setEditText() {
        if (EInputType.NUM == inputType) {
            setEditTextNum();
        }
        if (mEditText != null) {
            mEditText.setOnEditorActionListener(new EditorActionListener() {
                @Override
                protected void onKeyOk() {
                    quickClickProtection.stop();
                    onClick(confirmBtn);
                }

                @Override
                protected void onKeyCancel() {
                    finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
                }
            });
        }
    }

    // 数字
    private void setEditTextNum() {
        mEditText = (CustomKeyboardEditText) findViewById(R.id.input_data_1);
        mEditText.requestFocus();
        mEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLen)});
        if (minLen == 0) {
            confirmBtn.setEnabled(true);
            confirmBtn.setBackgroundResource(R.drawable.btn_bg_light);
        } else {
            mEditText.addTextChangedListener(new EnterAmountTextWatcher() {

                @Override
                public void afterTextChanged(Editable s) {
                    confirmBtnChange();
                }
            });
        }

    }

    @Override
    protected void setListeners() {
        confirmBtn.setOnClickListener(this);
    }

    @Override
    public void onClickProtected(View v) {
        if (v.getId() == R.id.info_confirm) {
            String content = process();
            if (onConfirmResult(content))
                finish(new ActionResult(TransResult.SUCC, content));
        }
    }

    private boolean onConfirmResult(String content) {
        if (EInputType.NUM == inputType && minLen > 0 && (content == null || content.isEmpty())) {
            ToastUtils.showMessage(R.string.please_input_again);
            return false;
        }
        return true;
    }

    /**
     * 输入数值检查
     */
    private String process() {
        String content = mEditText.getText().toString().trim();

        if (content.isEmpty()) {
            return null;
        }

        if (EInputType.NUM == inputType) {
            if (content.length() >= minLen && content.length() <= maxLen) {
                if (isPaddingZero) {
                    content = Component.getPaddedString(content, maxLen, '0');
                }
            } else {
                return null;
            }
        }
        return content;
    }

    private void confirmBtnChange() {
        String content = mEditText.getText().toString();
        confirmBtn.setEnabled(!content.isEmpty());
    }

    @Override
    protected boolean onKeyBackDown() {
        finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
        return true;
    }

    @Override
    public void finish(ActionResult result) {
        cancelSP200();
        super.finish(result);
    }

    private void setBtnScanner() {
        mBtnScanner = findViewById(R.id.imageButtonScanner);
        mBtnScanner.setVisibility(View.VISIBLE);

        IScanListener iScanListener = new IScanListener() {
            @Override
            public void onCancel() {
                // DO NOT call setResult here since it will be can in onFinish
            }
            @Override
            public void onFinish() {
                if (scanner != null) scanner.close();
            }
            @Override
            public void onRead(String content) { qrCode = content; }
        };

        mBtnScanner.setOnClickListener(
                view -> FinancialApplication.getApp().runOnUiThread(() -> {
                    if (Utils.getString(R.string.back_camera).equals(FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_DEFAULT_CAMERA))) {
                        scanner = Device.getScanner(EScannerType.REAR);
                    } else {
                        scanner = Device.getScanner(EScannerType.FRONT);
                    }
                    //scanner.close(); // 系统扫码崩溃之后，再调用掉不起来
                    scanner.open();
                    scanner.setTimeOut(TickTimer.DEFAULT_TIMEOUT * 1000);
                    scanner.setContinuousTimes(1);
                    scanner.setContinuousInterval(1000);
                    scanner.start(iScanListener);
                })
        );
    }

    private SP200_serialAPI.SP200ReturnListener sp200ReturnListener = result -> {
        if (result.getRet() == TransResult.SUCC) {
            String qrcode = result.getData().toString();
            Log.d("SP200 result" + qrcode);
            FinancialApplication.getApp().runOnUiThread(() -> mEditText.setText(qrcode));
            SystemClock.sleep(1000);
            sp200ScanQR();
        }
    };

    private void sp200ScanQR() {
        SP200_serialAPI.getInstance().ScanQRForPan(TickTimer.DEFAULT_TIMEOUT - 2, sp200ReturnListener);
    }

    private void setSP200() {
        if (SP200_serialAPI.getInstance().isSp200Enable()) {
            if (threadSp200ReceiveQR == null) {
                try {
                    threadSp200ReceiveQR = new Thread(() -> sp200ScanQR());
                    threadSp200ReceiveQR.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void cancelSP200() {
        if (threadSp200ReceiveQR == null)
            return;
        threadSp200ReceiveQR.interrupt();
        SP200_serialAPI.getInstance().BreakReceiveThread();
        SP200_serialAPI.getInstance().setSp200Cancel(true);
        FinancialApplication.getApp().runInBackground(()->SP200_serialAPI.getInstance().cancelSP200());
    }

}