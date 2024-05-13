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

import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.BaseActivityWithTickForAction;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.action.ActionEnterRedeemKbank;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.utils.EnterAmountTextWatcher;
import com.pax.pay.utils.TickTimer;
import com.pax.pay.utils.Utils;
import com.pax.view.dialog.DialogUtils;

import java.math.BigDecimal;
import java.text.NumberFormat;

public class EnterRedeemKbankActivity extends BaseActivityWithTickForAction {

    private String title;
    private EnterAmountTextWatcher amountWatcher = null;

    //////
    private ETransType transType;
    private String cardType;
    private String cardNum;
    private String cardExp;

    private EditText editCardType;
    private EditText editCardNumber;
    private EditText editCardExp;
    private EditText editProduct;
    private EditText editQty;
    private EditText editPoints;
    private EditText editAmt;
    private Spinner spnDiscount;

    private LinearLayout prodCdLL;
    private LinearLayout qtyLL;
    private LinearLayout pointsLL;
    private LinearLayout amtLL;
    private LinearLayout discountLL;

    private Button confirmBtn;

    private String product;
    private String amt;
    private int qty;
    private int points;

    private TextView txv_card_type;
    private TextView txv_card_number;
    private TextView txv_card_expiry;
    private TextView txv_discount_type;
    private TextView txv_product;
    private TextView txv_quantity;
    private TextView txv_redeem_point;
    private TextView txv_redeem_amount;


    private int currentKey = 0;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    protected void loadParam() {
        title = getIntent().getStringExtra(EUIParamKeys.NAV_TITLE.toString());
        transType = (ETransType) getIntent().getExtras().get(EUIParamKeys.TRANS_TYPE.toString());

        cardType = getIntent().getStringExtra(getString(R.string.param_card_type));
        cardNum = getIntent().getStringExtra(getString(R.string.param_card_num));
        cardExp = getIntent().getStringExtra(getString(R.string.param_card_exp));
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_enter_redeem_kbank;
    }

    @Override
    protected String getTitleString() {
        return title;
    }

    @Override
    protected void initViews() {
        tickTimer.start(TickTimer.DEFAULT_TIMEOUT * 5);
        /////
        prodCdLL = (LinearLayout) findViewById(R.id.product_LL);
        qtyLL = (LinearLayout) findViewById(R.id.quantity_LL);
        pointsLL = (LinearLayout) findViewById(R.id.points_LL);
        amtLL = (LinearLayout) findViewById(R.id.amount_LL);
        discountLL  = (LinearLayout) findViewById(R.id.discount_type_LL);

        editCardType = (EditText) findViewById(R.id.redeem_card_type);
        editCardNumber = (EditText) findViewById(R.id.redeem_card_num);
        editCardExp = (EditText) findViewById(R.id.redeem_card_exp);
        editProduct = (EditText) findViewById(R.id.redeem_product);
        editQty = (EditText) findViewById(R.id.redeem_qty);
        editPoints = (EditText) findViewById(R.id.redeem_points);
        editAmt = (EditText) findViewById(R.id.redeem_amount);
        spnDiscount = (Spinner) findViewById(R.id.redeem_discount_type_spinner);

        txv_card_type = (TextView) findViewById(R.id.redeem_card_type_title);
        txv_card_number = (TextView) findViewById(R.id.redeem_card_num_title);
        txv_card_expiry = (TextView) findViewById(R.id.redeem_card_exp_title);
        txv_discount_type = (TextView) findViewById(R.id.redeem_discount_type_title);
        txv_product = (TextView) findViewById(R.id.redeem_product_title);
        txv_quantity = (TextView) findViewById(R.id.redeem_qty_title);
        txv_redeem_point = (TextView) findViewById(R.id.redeem_points_title);
        txv_redeem_amount = (TextView) findViewById(R.id.redeem_amount_title);


        confirmBtn = (Button) findViewById(R.id.redeem_confirm);
        confirmBtn.setEnabled(true);
        confirmBtn.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && confirmBtn.isEnabled()) {
                    onConfirmResult();
                }
            }

        });

        switch (transType) {
            case KBANK_REDEEM_PRODUCT:
                pointsLL.setVisibility(View.GONE);
                amtLL.setVisibility(View.GONE);
                discountLL.setVisibility(View.GONE);
                break;
            case KBANK_REDEEM_VOUCHER:
                prodCdLL.setVisibility(View.GONE);
                qtyLL.setVisibility(View.GONE);
                amtLL.setVisibility(View.GONE);
                discountLL.setVisibility(View.GONE);
                break;
            case KBANK_REDEEM_VOUCHER_CREDIT:
                prodCdLL.setVisibility(View.GONE);
                qtyLL.setVisibility(View.GONE);
                discountLL.setVisibility(View.GONE);
                break;
            case KBANK_REDEEM_DISCOUNT:
                qtyLL.setVisibility(View.GONE);
                pointsLL.setVisibility(View.GONE);

                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.redeem_discount_type_kbank, android.R.layout.simple_spinner_item);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spnDiscount.setAdapter(adapter);

                spnDiscount.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                        if(pos == 0){//Fix Pnt
                            prodCdLL.setVisibility(View.VISIBLE);
                            editProduct.requestFocus();
                        }else if(pos == 1){//Var Pnt
                            editProduct.setText("");
                            prodCdLL.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        // Another interface callback
                    }
                });

                break;
        }

        editCardType.setText(cardType);
        editCardNumber.setText(cardNum);
        editCardExp.setText(cardExp);

        editAmt.addTextChangedListener(new textWatcherAmount());
        editAmt.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                currentKey = setCurrentKey(keyCode);
                return false;
            }
        });

        String redStar = "<font color='red'> *</font>";
        txv_card_type.append(Html.fromHtml(redStar));
        txv_card_number.append(Html.fromHtml(redStar));
        txv_card_expiry.append(Html.fromHtml(redStar));
        txv_discount_type.append(Html.fromHtml(redStar));
        txv_product.append(Html.fromHtml(redStar));
        txv_quantity.append(Html.fromHtml(redStar));
        txv_redeem_point.append(Html.fromHtml(redStar));
        txv_redeem_amount.append(Html.fromHtml(redStar));

    }

        @Override
    protected void setListeners() {
        confirmBtn.setOnClickListener(this);
    }

    @Override
    public void onClickProtected(View v) {
        if (v.getId() == R.id.redeem_confirm && confirmBtn.isEnabled()) {
            onConfirmResult();
        }
    }

    private void onConfirmResult() {

        product = editProduct.getText() != null && !editProduct.getText().toString().isEmpty() ? editProduct.getText().toString() : null;
        qty = editQty.getText() != null && !("").equals(editQty.getText().toString()) ? Integer.parseInt(editQty.getText().toString()) : 0;
        points = editPoints.getText() != null && !("").equals(editPoints.getText().toString()) ? Integer.parseInt(editPoints.getText().toString()) : 0;
        amt = editAmt.getText() != null && !editAmt.getText().toString().isEmpty() ? editAmt.getText().toString().replaceAll("[,.]","") : null;

        if(transType ==  ETransType.KBANK_REDEEM_DISCOUNT && !("0").equals(getDiscountTypeVal())){
            product = getDiscountTypeVal();
        }


        if (editProduct.isShown() && product == null ){
            DialogUtils.showErrMessage(
                    this,
                    title,
                    Utils.getString(R.string.err_redeem_product),
                    null,
                    Constants.FAILED_DIALOG_SHOW_TIME
            );
            editProduct.requestFocus();
        }else if(editQty.isShown() && qty == 0 ){
            DialogUtils.showErrMessage(
                    this,
                    title,
                    Utils.getString(R.string.err_redeem_quantity),
                    null,
                    Constants.FAILED_DIALOG_SHOW_TIME
            );
            editQty.requestFocus();
        }else if(editPoints.isShown() && points == 0 ){
            DialogUtils.showErrMessage(
                    this,
                    title,
                    Utils.getString(R.string.err_redeem_points),
                    null,
                    Constants.FAILED_DIALOG_SHOW_TIME
            );
            editPoints.requestFocus();
        }
        else if(editAmt.isShown() && (amt == null || ("000").equals(amt))){
            DialogUtils.showErrMessage(
                    this,
                    title,
                    Utils.getString(R.string.err_redeem_amount),
                    null,
                    Constants.FAILED_DIALOG_SHOW_TIME
            );
            editAmt.requestFocus();
        }
        else if (editAmt.isShown() && Utils.parseLongSafe(amt, 0) > FinancialApplication.getSysParam().getEDCMaxAmt()) {
            DialogUtils.showErrMessage(
                    this,
                    transType.getTransName()+ " " + title,
                    getString(R.string.err_amount_exceed_max_limit),
                    null,
                    Constants.FAILED_DIALOG_SHOW_TIME
            );
            editAmt.requestFocus();
        }
        else if (editAmt.isShown() && Utils.parseLongSafe(amt, 0) < FinancialApplication.getSysParam().getEDCMinAmt()) {
            DialogUtils.showErrMessage(
                    this,
                    transType.getTransName()+ " " + title,
                    getString(R.string.err_amount_below_min_limit),
                    null,
                    Constants.FAILED_DIALOG_SHOW_TIME
            );
            editAmt.requestFocus();
        }
        else
        {
            confirmBtn.setEnabled(false);
            //Log.e("menu","product = " + product + ", qty = " + qty + ", points = " + points + ", amt = " + amt);
            finish(new ActionResult(TransResult.SUCC,  new ActionEnterRedeemKbank.RedeemKbankInfo
                    (product,qty,points,amt, transType == ETransType.KBANK_REDEEM_DISCOUNT ? getDiscountTypeVal() : null)));
        }
    }


    @Override
    protected boolean onKeyBackDown() {
        finish(new ActionResult(TransResult.ERR_USER_CANCEL,null));
        return true;
    }

    private String getDiscountTypeVal(){
        if(spnDiscount.isShown()){
            final String[] discountTypeValue = getResources().getStringArray(R.array.redeem_discount_type_kbank_value);
            return discountTypeValue[spnDiscount.getSelectedItemPosition()];
        }else{
            return null;
        }

    }

    private class textWatcherAmount implements TextWatcher {
        private String valueAsString = "0.00";

        textWatcherAmount(){

        }

        @Override
        public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {
            valueAsString = s.toString();
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            calDecimal(editAmt,valueAsString,String.valueOf(currentKey), 10,this,s,count);
        }

        @Override
        public void afterTextChanged(Editable editable) {}
    }

    private void calDecimal(EditText editText,String currentValue, String currentKey, int maxSize, TextWatcher textWatcher, CharSequence s, int count){
        NumberFormat formatterNum =  NumberFormat.getInstance();
        String valueAsString = "";
        formatterNum.setMinimumFractionDigits(2);
        formatterNum.setMaximumFractionDigits(2);

        if(!currentValue.isEmpty()){
            String currentVal = currentValue.replace(",","");
            BigDecimal bd = new BigDecimal("100");
            BigDecimal valBd = bd.multiply(new BigDecimal(currentVal));
            long valLong = valBd.longValue();
            valueAsString = String.valueOf(valLong);
        }

        if (s != null && s.length() > 0 && count > 0) {
            String displayValue = calculateValue(valueAsString, currentKey, maxSize);
            if(displayValue != null && !displayValue.isEmpty()){
                editText.removeTextChangedListener(textWatcher);
                editText.setText(formatterNum.format(Double.parseDouble(displayValue) / 100));
                editText.setSelection(editText.getText().length());
                editText.addTextChangedListener(textWatcher);
            }
        }else if(s != null && s.length() > 0 && count == 0){
            String  displayValue = deleteValue(valueAsString);
            if(displayValue.isEmpty()){
                displayValue = "0.00";
            }

            editText.removeTextChangedListener(textWatcher);
            editText.setText(formatterNum.format(Double.parseDouble(displayValue) / 100));
            editText.setSelection(editText.getText().length());
            editText.addTextChangedListener(textWatcher);
        }
    }

    private int setCurrentKey(int keyCode){
        int currentKey = 0;
        switch(keyCode){
            case KeyEvent.KEYCODE_0:
                 currentKey = 0;
                 break;
            case KeyEvent.KEYCODE_1:
                currentKey = 1;
                break;
            case KeyEvent.KEYCODE_2:
                currentKey = 2;
                break;
            case KeyEvent.KEYCODE_3:
                currentKey = 3;
                break;
            case KeyEvent.KEYCODE_4:
                currentKey = 4;
                break;
            case KeyEvent.KEYCODE_5:
                currentKey = 5;
                break;
            case KeyEvent.KEYCODE_6:
                currentKey = 6;
                break;
            case KeyEvent.KEYCODE_7:
                currentKey = 7;
                break;
            case KeyEvent.KEYCODE_8:
                currentKey = 8;
                break;
            case KeyEvent.KEYCODE_9:
                currentKey = 9;
                break;
        }
        return currentKey;
    }

    public final String calculateValue( String currentValue,  String number, int maximumSizeOfAmount) {
        String newValue = null;
        if (currentValue.length() == maximumSizeOfAmount) {
            newValue = currentValue;
        } else {
            if (currentValue == null || currentValue.isEmpty()) {
                newValue = number;
            } else {
                if (currentValue.length() + number.length() <= maximumSizeOfAmount) {
                    newValue = currentValue + number;
                } else {
                    int remaining = maximumSizeOfAmount - currentValue.length();
                    String trimmedNumber = number.substring(0, remaining - 1);
                    newValue = currentValue + trimmedNumber;
                }
            }
        }
        return newValue;
    }

    public final String deleteValue( String currentValue) {
        String newValue = "";

        if (currentValue != null && currentValue.length() > 1) {
            newValue = currentValue.substring(0,currentValue.length()-1);
        }

        return newValue;
    }

}
