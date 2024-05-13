/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-1-13
 * Module Author: qixw
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.trans.action.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.BaseActivityWithTickForAction;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.EditorActionListener;
import com.pax.pay.utils.EnterAmountTextWatcher;
import com.pax.pay.utils.ToastUtils;
import com.pax.pay.utils.Utils;
import com.pax.view.keyboard.CustomKeyboardEditText;

public class InputPanActivity extends BaseActivityWithTickForAction {


    private CustomKeyboardEditText mEditNewTips;

    private Button confirmBtn;

    private String navTitle;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEditText();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_input_new_pan;
    }

    @Override
    protected void loadParam() {
        navTitle = getIntent().getStringExtra(EUIParamKeys.NAV_TITLE.toString());

    }

    @Override
    protected String getTitleString() {
        return navTitle;
    }

    @Override
    protected void initViews() {
        TextView mBaseAmount = (TextView) findViewById(R.id.value_base_amount);

        mEditNewTips = (CustomKeyboardEditText) findViewById(R.id.prompt_edit_pan);
        //mEditNewTips.setText(CurrencyConverter.convert(tipAmountLong));
        mEditNewTips.setFocusable(true);
        mEditNewTips.requestFocus();

        confirmBtn = (Button) findViewById(R.id.info_confirm);
    }

    private void setEditText() {
        mEditNewTips.setHint(getString(R.string.amount_default));
        mEditNewTips.requestFocus();
    }

    @Override
    protected void setListeners() {
        confirmBtn.setOnClickListener(this);
        mEditNewTips.setOnEditorActionListener(new TipEditorActionListener());
    }

    private class TipEditorActionListener extends EditorActionListener {
        @Override
        public void onKeyOk() {
            //do nothing
            quickClickProtection.stop();
            onClick(confirmBtn);
        }

        @Override
        public void onKeyCancel() {
            finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
        }
    }

    @Override
    public void onClickProtected(View v) {

        if (v.getId() == R.id.info_confirm) {
            String content = process();
            if (content == null || content.isEmpty()) {
                ToastUtils.showMessage(R.string.please_input_again);
                return;
            }
            finish(new ActionResult(TransResult.SUCC, null, null));
        }

    }

    /**
     * 输入数值检查
     */
    private String process() {
        String content = mEditNewTips.getText().toString().trim();

        if (content.isEmpty()) {
            return null;
        }

        //tip can be 0, so don't need to check here
        return CurrencyConverter.parse(mEditNewTips.getText().toString().trim()).toString();
    }

    @Override
    protected boolean onKeyBackDown() {
        finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
        return true;
    }
}
