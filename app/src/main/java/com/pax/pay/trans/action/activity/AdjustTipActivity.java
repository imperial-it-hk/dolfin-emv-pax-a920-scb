/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-1-13
 * Module Author: lixc
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.trans.action.activity;

import android.content.Intent;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pax.abl.core.ActionResult;
import com.pax.dal.entity.EReaderType;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.eventbus.EmvCallbackEvent;
import com.pax.pay.BaseActivityWithTickForAction;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.EditorActionListener;
import com.pax.pay.utils.EnterAmountTextWatcher;
import com.pax.pay.utils.Utils;
import com.pax.view.keyboard.CustomKeyboardEditText;

import java.math.BigDecimal;

public class AdjustTipActivity extends BaseActivityWithTickForAction {

    private TextView textTipAmount;//tip amount text
    private CustomKeyboardEditText editAmount;//total amount edit text

    private String title;
    private String amount;
    private float percent;
    private String cardMode;

    private boolean isFirstStart = true;// whether the first time or not

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && isFirstStart) {
            editAmount.requestFocus();
            isFirstStart = false;
        }
    }

    @Override
    protected void loadParam() {
        title = getIntent().getStringExtra(EUIParamKeys.NAV_TITLE.toString());
        amount = getIntent().getStringExtra(EUIParamKeys.TRANS_AMOUNT.toString());
        percent = getIntent().getFloatExtra(EUIParamKeys.TIP_PERCENT.toString(), 0.0f);
        cardMode = getIntent().getStringExtra(EUIParamKeys.CARD_MODE.toString());
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_enter_amount;
    }

    @Override
    protected String getTitleString() {
        return title;
    }

    @Override
    protected void initViews() {

        TextView textBaseAmount = (TextView) findViewById(R.id.base_amount_input_text);
        textBaseAmount.setVisibility(View.VISIBLE);
        textBaseAmount.setText(CurrencyConverter.convert(Utils.parseLongSafe(amount, 0)));

        TextView promptTip = (TextView) findViewById(R.id.prompt_tip);
        promptTip.setText(getString(R.string.prompt_tip) + "(max:" + percent + "%)");

        LinearLayout tipAmountLL = (LinearLayout) findViewById(R.id.tip_amount_ll);
        tipAmountLL.setVisibility(View.VISIBLE);
        textTipAmount = (TextView) findViewById(R.id.tip_amount_input_text);
        textTipAmount.setText(CurrencyConverter.convert(0L));

        editAmount = (CustomKeyboardEditText) findViewById(R.id.amount_edit);
        editAmount.setText(CurrencyConverter.convert(Utils.parseLongSafe(amount, 0)));
        editAmount.requestFocus();

    }

    @Override
    protected void setListeners() {
        EnterAmountTextWatcher amountWatcher = new EnterAmountTextWatcher();
        amountWatcher.setAmount(Utils.parseLongSafe(amount, 0), 0L);


        amountWatcher.setOnTipListener(new EnterAmountTextWatcher.OnTipListener() {
            BigDecimal hundredBd = new BigDecimal(100);
            BigDecimal percentBd = BigDecimal.valueOf(percent);

            @Override
            public void onUpdateTipListener(long baseAmount, long tipAmount) {
                textTipAmount.setText(CurrencyConverter.convert(tipAmount));
            }

            @Override
            public boolean onVerifyTipListener(long baseAmount, long tipAmount) {
                // AET-313
                BigDecimal baseAmountBd = new BigDecimal(baseAmount);
                BigDecimal maxTipsBd = baseAmountBd.divide(hundredBd).multiply(percentBd).divide(hundredBd);
                BigDecimal tipAmountBd = (new BigDecimal(tipAmount)).divide(hundredBd);

                return maxTipsBd.doubleValue() >= tipAmountBd.doubleValue(); //AET-33
            }
        });
        editAmount.addTextChangedListener(amountWatcher);
        editAmount.setOnEditorActionListener(new AdjustTipEditorActionListener());
    }

    //AET-281
    @Override
    protected boolean onOptionsItemSelectedSub(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            FinancialApplication.getApp().doEvent(new EmvCallbackEvent(EmvCallbackEvent.Status.CARD_NUM_CONFIRM_ERROR));
            return true;
        }
        return super.onOptionsItemSelectedSub(item);
    }

    private class AdjustTipEditorActionListener extends EditorActionListener {
        @Override
        public void onKeyCancel() {
            textTipAmount.setText("");
            editAmount.setText("");
            if ((cardMode != null) && (cardMode.equals(EReaderType.ICC.toString()))) {
                FinancialApplication.getApp().doEvent(new EmvCallbackEvent(EmvCallbackEvent.Status.CARD_NUM_CONFIRM_ERROR));
            } else {
                finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
            }
        }

        @Override
        public void onKeyOk() {
            if ((cardMode != null) && (cardMode.equals(EReaderType.ICC.toString()))) {
                Intent intent = getIntent();
                intent.putExtra(EUIParamKeys.TRANS_AMOUNT.toString(), editAmount.getText().toString().trim());
                intent.putExtra(EUIParamKeys.TIP_AMOUNT.toString(), textTipAmount.getText().toString().trim());
                setResult(SearchCardActivity.REQ_ADJUST_TIP, intent);
                finish();
            } else {
                finish(new ActionResult(TransResult.SUCC, editAmount.getText().toString().trim(), textTipAmount.getText().toString().trim()));
            }
        }
    }
}
