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
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.glwrapper.convert.IConvert;
import com.pax.pay.BaseActivity;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.TransContext;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.ToastUtils;
import com.pax.pay.utils.Utils;
import com.pax.view.ElectronicSignatureView;

import java.io.ByteArrayOutputStream;
import java.util.List;

import th.co.bkkps.utils.Log;

public class SignatureActivity extends BaseActivity {

    public static final String SIGNATURE_FILE_NAME = "customSignature.png";
    public static final String PARAM_TITLE = "title";
    public static final String PARAM_AMOUNT = "amount";

    private final int timeout = 10 * 60; //10 min

    private ElectronicSignatureView mSignatureView;

    private RelativeLayout writeUserName = null;

    private Button clearBtn;
    private Button confirmBtn;

    private String amount;
    private boolean skipSign;

    private boolean processing = false;

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (!quickClickProtection.isStarted()) {
            quickClickProtection.start();
        }

        if(event.getAction() == KeyEvent.ACTION_UP){
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_BACK:
                    if (isProcessing()) {
                        return false;
                    }
                    setProcessFlag();
                    mSignatureView.clear();
                    clearProcessFlag();
                    break;
                case KeyEvent.KEYCODE_ENTER:
                    Log.i(TAG, "sign confirm_btn");
                    if (isProcessing()) {
                        return false;
                    }
                    setProcessFlag();
                    if (!mSignatureView.getTouched()) {
                        Log.i("touch", "no touch");
                        clearProcessFlag();
                        return false;
                    }

                    Bitmap bitmap = mSignatureView.save(true, 0);
                    // 保存签名图片
                    byte[] data = FinancialApplication.getGl().getImgProcessing().bitmapToJbig(bitmap, Constants.rgb2MonoAlgo);

                    Log.i(TAG, "电子签名数据长度为:" + data.length);

                    if (data.length > 999) {
                        ToastUtils.showMessage(R.string.signature_redo);
                        setProcessFlag();
                        mSignatureView.clear();
                        clearProcessFlag();
                        return false;
                    }
                    //clearProcessFlag();
                    finish(new ActionResult(TransResult.SUCC, data, genSignPos()));
                    break;
                default:
                    break;
            }
            return false;
        }else{
            return false;
        }
    }


    @Override
    protected int getLayoutId() {
        return R.layout.activity_authgraph_layout;
    }

    @Override
    protected void loadParam() {
        Bundle bundle = getIntent().getExtras();
        amount = bundle.getString(EUIParamKeys.TRANS_AMOUNT.toString());
        skipSign = bundle.getBoolean(EUIParamKeys.SUPPORTBYPASS.toString());
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected String getTitleString() {
        return getString(R.string.trans_signature);
    }

    @Override
    protected void initViews() {

        enableBackAction(false);

        TextView amountText = (TextView) findViewById(R.id.trans_amount_tv);
        amount = CurrencyConverter.convert(Utils.parseLongSafe(amount, 0));
        amountText.setText(amount);

        writeUserName = (RelativeLayout) findViewById(R.id.writeUserNameSpace);
        mSignatureView = new ElectronicSignatureView(SignatureActivity.this);
        mSignatureView.setSampleRate(5);
        mSignatureView.setBitmap(new Rect(0, 0, 474, 158), 10, Color.WHITE);
        writeUserName.addView(mSignatureView);

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.CENTER_IN_PARENT);

        clearBtn = (Button) findViewById(R.id.clear_btn);
        confirmBtn = (Button) findViewById(R.id.confirm_btn);
        clearProcessFlag();

    }

    @Override
    protected void setListeners() {
        clearBtn.setOnClickListener(this);
        confirmBtn.setOnClickListener(this);
    }

    @Override
    public void onClickProtected(View v) {

        switch (v.getId()) {
            case R.id.clear_btn:

                if (isProcessing()) {
                    return;
                }
                setProcessFlag();
                mSignatureView.clear();
                clearProcessFlag();
                break;
            case R.id.confirm_btn:
                Log.i(TAG, "sign confirm_btn");
                if (skipSign && !mSignatureView.getTouched()) {
                    clearProcessFlag();
                    finish(new ActionResult(TransResult.SUCC, null, null));
                    return;
                }

                if (isProcessing()) {
                    return;
                }
                setProcessFlag();
                if (!mSignatureView.getTouched()) {
                    Log.i("touch", "no touch");
                    clearProcessFlag();
                    return;
                }

                Bitmap bitmap = mSignatureView.save(true, 0);
                // 保存签名图片
                byte[] data = FinancialApplication.getGl().getImgProcessing().bitmapToJbig(bitmap, Constants.rgb2MonoAlgo);

                Log.i(TAG, "电子签名数据长度为:" + data.length);

                if (data.length > 999) {
                    ToastUtils.showMessage(R.string.signature_redo);
                    setProcessFlag();
                    mSignatureView.clear();
                    clearProcessFlag();
                    return;
                }
                clearProcessFlag();
                finish(new ActionResult(TransResult.SUCC, data, genSignPos()));

                break;
            default:
                break;
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return item.getItemId() == android.R.id.home || super.onOptionsItemSelected(item);
    }

    private byte[] genSignPos() {
        List<float[]> signPos = mSignatureView.getPathPos();
        ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
        for (float[] i : signPos) {
            if (i[0] < 0 && i[1] < 0) {
                swapStream.write(FinancialApplication.getConvert().intToByteArray(0xFFFFFFFF, IConvert.EEndian.LITTLE_ENDIAN), 0, 4);
            } else {
                byte[] bytes = FinancialApplication.getConvert().shortToByteArray((short) i[0], IConvert.EEndian.LITTLE_ENDIAN);
                swapStream.write(bytes, 0, 2);
                bytes = FinancialApplication.getConvert().shortToByteArray((short) i[1], IConvert.EEndian.LITTLE_ENDIAN);
                swapStream.write(bytes, 0, 2);
            }
        }
        return swapStream.toByteArray();
    }

    protected void setProcessFlag() {
        processing = true;
    }

    protected void clearProcessFlag() {
        processing = false;
    }

    protected boolean isProcessing() {
        return processing;
    }

    public void finish(ActionResult result) {
        AAction action = TransContext.getInstance().getCurrentAction();
        if (action != null) {
            if (action.isFinished())
                return;
            action.setFinished(true);
            if (!quickClickProtection.isStarted()) {
                quickClickProtection.start();
            }
            action.setResult(result);
        } else {
            super.finish();
        }
    }

}