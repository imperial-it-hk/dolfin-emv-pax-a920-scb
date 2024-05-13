/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-1-11
 * Module Author: lixc
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.trans.action.activity;

import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.BaseActivityWithTickForAction;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.action.ActionEnterAmount;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.EnterAmountTextWatcher;
import com.pax.pay.utils.ToastUtils;
import com.pax.pay.utils.Utils;
import com.pax.view.keyboard.CustomKeyboardEditText;

public class EnterAmountActivity extends BaseActivityWithTickForAction {

    private LinearLayout tipAmountLL;
    private TextView promptBaseAmount;
    private TextView textBaseAmount;//base amount text
    private TextView promptTip;
    private TextView textTipAmount;//tip amount text
    private CustomKeyboardEditText editAmount;//total amount edit text

    private String title;
    private boolean hasTip;
    private float percent;
    private String baseAmount;
    private boolean isDccRequired;
    private String dccAmount;
    private String dccConversionRate;
    private String currencyNumeric;
    private long dccNewBaseAmount;
    private long dccTipAmount;

    private boolean isTipMode = false;
    private boolean isAlreadyPressed = false;

    private EnterAmountTextWatcher amountWatcher = null;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        editAmount.requestFocus();
    }

    @Override
    protected void loadParam() {
        title = getIntent().getStringExtra(EUIParamKeys.NAV_TITLE.toString());
        hasTip = getIntent().getBooleanExtra(EUIParamKeys.HAS_TIP.toString(), false);
        if (hasTip) {
            percent = getIntent().getFloatExtra(EUIParamKeys.TIP_PERCENT.toString(), 0.0f);
            baseAmount = getIntent().getStringExtra(EUIParamKeys.BASE_AMOUNT.toString());
            isDccRequired = getIntent().getBooleanExtra(EUIParamKeys.DCC_REQUIRED.toString(), false);
            if (isDccRequired) {
                dccAmount = getIntent().getStringExtra(EUIParamKeys.DCC_AMOUNT.toString());
                dccConversionRate = getIntent().getStringExtra(EUIParamKeys.DCC_CONVERSION_RATE.toString());
                currencyNumeric = getIntent().getStringExtra(EUIParamKeys.CURRENCY_NUMERIC.toString());
            }
        }
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
        textBaseAmount = (TextView) findViewById(R.id.base_amount_input_text);
        promptBaseAmount =  findViewById(R.id.prompt_base_amount);
        tipAmountLL = (LinearLayout) findViewById(R.id.tip_amount_ll);
        promptTip = (TextView) findViewById(R.id.prompt_tip);
        textTipAmount = (TextView) findViewById(R.id.tip_amount_input_text);

        editAmount = (CustomKeyboardEditText) findViewById(R.id.amount_edit);
        editAmount.setText(convertAmountWithCurrency(0L, true)); //AET-64
        editAmount.requestFocus();

        if (hasTip) {
            isTipMode = true;
            textBaseAmount.setText(convertAmountWithCurrency(Utils.parseLongSafe(baseAmount, 0), true));
            textBaseAmount.setVisibility(View.VISIBLE);
            tipAmountLL.setVisibility(View.VISIBLE);

            String strPromptTip;
            String strPromptBaseAmount = "";
            String tipPercent = percent + "%";
            if (isDccRequired) {
                strPromptTip = getString(R.string.prompt_tip) + "(max:" + tipPercent + ")" +
                        " " + "(" + convertAmountWithCurrency(0L, false) + ")";
                strPromptBaseAmount = getString(R.string.prompt_base_amount) +
                        "(" + convertAmountWithCurrency(Utils.parseLongSafe(dccAmount, 0), false) + ")";
            } else {
                strPromptTip = getString(R.string.prompt_tip) + "(max:" + tipPercent + ")";
                strPromptBaseAmount = getString(R.string.prompt_base_amount);
            }
            promptTip.setText(strPromptTip);
            promptBaseAmount.setText(strPromptBaseAmount);
            ToastUtils.makeText(this, getString(R.string.prompt_input_tip_amount, tipPercent), 2000);
        }
    }

    @Override
    protected void setListeners() {

        if (hasTip) {
            amountWatcher = new EnterAmountTextWatcher(Utils.parseLongSafe(baseAmount, 0), 0L);
        } else {
            amountWatcher = new EnterAmountTextWatcher();
        }

        amountWatcher.setOnTipListener(new EnterAmountTextWatcher.OnTipListener() {
            @Override
            public void onUpdateTipListener(long baseAmount, long tipAmount) {
                textTipAmount.setText(convertAmountWithCurrency(tipAmount, true));
                if (isDccRequired) {
                    double exRate = Double.parseDouble(dccConversionRate) / 10000;
                    double dDccNewBaseAmt = baseAmount * exRate;
                    double dDccTipAmt = tipAmount * exRate;
                    dccNewBaseAmount = CurrencyConverter.parse(dDccNewBaseAmt);
                    dccTipAmount = CurrencyConverter.parse(dDccTipAmt);
                    String strPromptTip = getString(R.string.prompt_tip) + "(max:" + percent + "%)" +
                            " " + "(" + convertAmountWithCurrency(dccTipAmount, false) + ")";
                    promptTip.setText(strPromptTip);
                }
            }

            @Override
            public boolean onVerifyTipListener(long baseAmount, long tipAmount) {
                return !isTipMode || (baseAmount * percent / 100 >= tipAmount); //AET-33
            }
        });

        editAmount.addTextChangedListener(amountWatcher);

        editAmount.setOnEditorActionListener(new CustomEnterAmountEditorActionListener());
    }


    @Override
    protected boolean onKeyBackDown() {
        finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
        return true;
    }

    private String convertAmountWithCurrency(long amount, boolean localCurrency) {
        String result;
        if (!localCurrency && isDccRequired && currencyNumeric != null) {
            result = CurrencyConverter.convert(amount, currencyNumeric);
        } else {
            result = CurrencyConverter.convert(amount);
        }
        return result;
    }

    private class CustomEnterAmountEditorActionListener implements TextView.OnEditorActionListener {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (!isAlreadyPressed) {
                isAlreadyPressed = true;
                switch (actionId) {
                    case EditorInfo.IME_ACTION_UNSPECIFIED:
                        if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
                            onKeyOk();
                            return true;
                        }
                        break;
                    case EditorInfo.IME_ACTION_DONE:
                        onKeyOk();
                        return true;
                    case EditorInfo.IME_ACTION_NONE:
                        isAlreadyPressed = false;
                        onKeyCancel();
                        return true;
                }
                return false;
            }
            return false;
        }
    }

    private void onKeyCancel() {
        finish(new ActionResult(TransResult.ERR_USER_CANCEL, null)); // AET-64
    }

    private void onKeyOk() {
        if (!isTipMode) {
            if ("0".equals(CurrencyConverter.parse(editAmount.getText().toString().trim()).toString())) {
                return;
            }
            if (CurrencyConverter.parse(editAmount.getText().toString().trim()) > FinancialApplication.getSysParam().getEDCMaxAmt()) {
                ToastUtils.showMessage(getString(R.string.err_amount_exceed_max_limit));
                return;
            }
            if (CurrencyConverter.parse(editAmount.getText().toString().trim()) < FinancialApplication.getSysParam().getEDCMinAmt()) {
                ToastUtils.showMessage(getString(R.string.err_amount_below_min_limit));
                return;
            }
//                if (hasTip) {
//                    updateAmount(true);
//                    return;
//                }
        }
        if (hasTip) {
            if (CurrencyConverter.parse(textTipAmount.getText().toString().trim()) == 0) {
                return;
            }

            ActionEnterAmount.TipInformation tipInformation;
            if (isDccRequired) {
                tipInformation = new ActionEnterAmount.TipInformation(
                        CurrencyConverter.parse(editAmount.getText().toString().trim()),
                        dccNewBaseAmount,
                        CurrencyConverter.parse(textTipAmount.getText().toString().trim()),
                        dccTipAmount);
            } else {
                tipInformation = new ActionEnterAmount.TipInformation(
                        CurrencyConverter.parse(editAmount.getText().toString().trim()),
                        0L,
                        CurrencyConverter.parse(textTipAmount.getText().toString().trim()),
                        0L);
            }
            finish(new ActionResult(TransResult.SUCC, tipInformation));
        } else {
            finish(new ActionResult(TransResult.SUCC,
                    CurrencyConverter.parse(editAmount.getText().toString().trim()),
                    CurrencyConverter.parse(textTipAmount.getText().toString().trim()))
            );
        }
    }

    /*private class EnterAmountEditorActionListener extends EditorActionListener {
        @Override
        public void onKeyCancel() {
//            updateAmount(false);
            finish(new ActionResult(TransResult.ERR_USER_CANCEL, null)); // AET-64
        }

        @Override
        public void onKeyOk() {
            if (!isTipMode) {
                if ("0".equals(CurrencyConverter.parse(editAmount.getText().toString().trim()).toString())) {
                    return;
                }
                if (CurrencyConverter.parse(editAmount.getText().toString().trim()) > FinancialApplication.getSysParam().getEDCMaxAmt()) {
                    ToastUtils.showMessage(getString(R.string.err_amount_exceed_max_limit));
                    return;
                }
                if (CurrencyConverter.parse(editAmount.getText().toString().trim()) < FinancialApplication.getSysParam().getEDCMinAmt()) {
                    ToastUtils.showMessage(getString(R.string.err_amount_below_min_limit));
                    return;
                }
//                if (hasTip) {
//                    updateAmount(true);
//                    return;
//                }
            }
            if (hasTip) {
                if (CurrencyConverter.parse(textTipAmount.getText().toString().trim()) == 0) {
                    return;
                }

                ActionEnterAmount.TipInformation tipInformation;
                if (isDccRequired) {
                    tipInformation = new ActionEnterAmount.TipInformation(
                            CurrencyConverter.parse(editAmount.getText().toString().trim()),
                            dccNewBaseAmount,
                            CurrencyConverter.parse(textTipAmount.getText().toString().trim()),
                            dccTipAmount);
                } else {
                    tipInformation = new ActionEnterAmount.TipInformation(
                            CurrencyConverter.parse(editAmount.getText().toString().trim()),
                            0L,
                            CurrencyConverter.parse(textTipAmount.getText().toString().trim()),
                            0L);
                }
                finish(new ActionResult(TransResult.SUCC, tipInformation));
            } else {
                finish(new ActionResult(TransResult.SUCC,
                        CurrencyConverter.parse(editAmount.getText().toString().trim()),
                        CurrencyConverter.parse(textTipAmount.getText().toString().trim()))
                );
            }
        }

        //update total amount
        private synchronized void updateAmount(boolean isTipMode) {
            EnterAmountActivity.this.isTipMode = isTipMode;
            editAmount.requestFocus();
            if (isTipMode) {
                textBaseAmount.setVisibility(View.VISIBLE);
                tipAmountLL.setVisibility(View.VISIBLE);
                textBaseAmount.setText(editAmount.getText());
                promptTip.setText(getString(R.string.prompt_tip) + "(max:" + percent + "%)");
                textTipAmount.setText("");
                if (amountWatcher != null)
                    amountWatcher.setAmount(CurrencyConverter.parse(editAmount.getText().toString().trim()), 0L);
            } else {
                textBaseAmount.setVisibility(View.INVISIBLE);
                tipAmountLL.setVisibility(View.INVISIBLE);
                textBaseAmount.setText("");
                textTipAmount.setText("");
                if (amountWatcher != null)
                    amountWatcher.setAmount(0L, 0L);
                editAmount.setText("");
            }
        }
    }*/
}
