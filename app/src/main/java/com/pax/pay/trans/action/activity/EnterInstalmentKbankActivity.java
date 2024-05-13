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
import com.pax.pay.trans.action.ActionEnterInstalmentKbank;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.utils.EnterAmountTextWatcher;
import com.pax.pay.utils.TickTimer;
import com.pax.pay.utils.Utils;
import com.pax.view.dialog.DialogUtils;

import java.math.BigDecimal;
import java.text.NumberFormat;

public class EnterInstalmentKbankActivity extends BaseActivityWithTickForAction {

    private String title;
    private EnterAmountTextWatcher amountWatcher = null;

    //////
    private ETransType transType;


    private EditText editSupplier;
    private EditText editProduct;
    private EditText editSerialNum;
    private EditText editTerms;
    private EditText editAmt;
    private Spinner spnPromoType;

    private LinearLayout productLL;
    private LinearLayout promoTypeLL;
    private LinearLayout supplierLL;
    private LinearLayout serialNumLL;
    private LinearLayout termsLL;
    private LinearLayout amountLL;

    private Button confirmBtn;

    private String iPlanMode;
    private String product;
    private String supplier;
    private String serialNum;
    private int terms;
    private String amt;
    private String promotionKey = null;
    private boolean isPromoProduct = false;

    private int currentKey = 0;

    private TextView titleType;
    private TextView titleSupplier;
    private TextView titleProduct;
    private TextView titleTerms;
    private TextView titleAmt;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    protected void loadParam() {
        title = getIntent().getStringExtra(EUIParamKeys.NAV_TITLE.toString());
        transType = (ETransType) getIntent().getExtras().get(EUIParamKeys.TRANS_TYPE.toString());
        iPlanMode = getIntent().getStringExtra(getString(R.string.param_iplan_mode));
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_enter_instalment_kbank;
    }

    @Override
    protected String getTitleString() {
        return title;
    }

    @Override
    protected void initViews() {
        tickTimer.start(TickTimer.DEFAULT_TIMEOUT * 5);
        /////
        promoTypeLL = (LinearLayout) findViewById(R.id.promo_type_LL);
        supplierLL = (LinearLayout) findViewById(R.id.supplier_LL);
        productLL = (LinearLayout) findViewById(R.id.product_LL);
        serialNumLL = (LinearLayout) findViewById(R.id.serial_num_LL);
        termsLL  = (LinearLayout) findViewById(R.id.terms_LL);
        amountLL  = (LinearLayout) findViewById(R.id.amount_LL);


        editSupplier = (EditText) findViewById(R.id.instalment_supplier);
        editProduct = (EditText) findViewById(R.id.instalment_product);
        editSerialNum = (EditText) findViewById(R.id.instalment_serial_num);
        editTerms = (EditText) findViewById(R.id.instalment_terms);
        editAmt = (EditText) findViewById(R.id.instalment_amount);
        spnPromoType = (Spinner) findViewById(R.id.instalment_promo_type_spinner);

        titleType =  (TextView) findViewById(R.id.instalment_promo_type_title);
        titleSupplier =  (TextView) findViewById(R.id.instalment_supplier_title);
        titleProduct =  (TextView) findViewById(R.id.instalment_product_title);
        titleTerms =  (TextView) findViewById(R.id.instalment_terms_title);
        titleAmt =  (TextView) findViewById(R.id.instalment_amount_title);

        confirmBtn = (Button) findViewById(R.id.instalment_confirm);
        confirmBtn.setEnabled(true);
        confirmBtn.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && confirmBtn.isEnabled()) {
                    onConfirmResult();
                }
            }

        });

        switch (iPlanMode) {
            case "01": //Merchant Pay Interest
            case "02": //Customer Pay Interest
                promoTypeLL.setVisibility(View.VISIBLE);

                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.instalment_promo_type_kbank, android.R.layout.simple_spinner_item);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spnPromoType.setAdapter(adapter);

                spnPromoType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                        if(pos == 0){//ALL Product
                            isPromoProduct = false;
                            termsLL.setVisibility(View.VISIBLE);
                            supplierLL.setVisibility(View.GONE);
                            productLL.setVisibility(View.GONE);
                            serialNumLL.setVisibility(View.GONE);
                            editSupplier.setText("");
                            editProduct.setText("");
                            editSerialNum.setText("");
                            termsLL.requestFocus();
                        }else if(pos == 1){//PROMO Product
                            isPromoProduct = true;
                            supplierLL.setVisibility(View.VISIBLE);
                            productLL.setVisibility(View.VISIBLE);
                            serialNumLL.setVisibility(View.VISIBLE);
                            supplierLL.requestFocus();
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        // Another interface callback
                    }
                });

                break;
            case "03": //Supplier Pay Interest
            case "04":// Special Interest
                promoTypeLL.setVisibility(View.GONE);
                break;
        }

        editAmt.addTextChangedListener(new textWatcherAmount());
        editAmt.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                currentKey = setCurrentKey(keyCode);
                return false;
            }
        });

        String redStar = "<font color='red'> *</font>";
        titleType.append(Html.fromHtml(redStar));
        titleSupplier.append(Html.fromHtml(redStar));
        titleProduct.append(Html.fromHtml(redStar));
        titleTerms.append(Html.fromHtml(redStar));
        titleAmt.append(Html.fromHtml(redStar));
    }

        @Override
    protected void setListeners() {
        confirmBtn.setOnClickListener(this);
    }

    @Override
    public void onClickProtected(View v) {
        if (v.getId() == R.id.instalment_confirm && confirmBtn.isEnabled()) {
            onConfirmResult();
        }
    }

    private void onConfirmResult() {

        supplier = editSupplier.getText() != null && !editSupplier.getText().toString().isEmpty() ? editSupplier.getText().toString() : null;
        product = editProduct.getText() != null && !editProduct.getText().toString().isEmpty() ? editProduct.getText().toString() : null;
        serialNum = editSerialNum.getText() != null && !editSerialNum.getText().toString().isEmpty() ? editSerialNum.getText().toString() : "";
        terms = editTerms.getText() != null && !("").equals(editTerms.getText().toString()) ? Integer.parseInt(editTerms.getText().toString()) : 0;
        amt = editAmt.getText() != null && !editAmt.getText().toString().isEmpty() && !("0.00").equals(editAmt.getText().toString()) ? editAmt.getText().toString().replaceAll("[,.]","") : null;

        if(("01").equals(iPlanMode) || ("02").equals(iPlanMode)){
            promotionKey = getPromoTypeVal();
        }

        if(editSupplier.isShown() && (supplier == null || supplier.length() < 5)){
            DialogUtils.showErrMessage(
                    this,
                    transType.getTransName()+ " " + title,
                    "Please Insert Supplier 5 Digits",
                    null,
                    Constants.FAILED_DIALOG_SHOW_TIME
            );
            editSupplier.requestFocus();
        }else if (editProduct.isShown() && (product == null || product.length() < 9)){
            DialogUtils.showErrMessage(
                    this,
                    transType.getTransName()+ " " + title,
                    "Please Insert Product 9 Digits",
                    null,
                    Constants.FAILED_DIALOG_SHOW_TIME
            );
            editProduct.requestFocus();
        }else if(editTerms.isShown() && terms == 0 ){
            DialogUtils.showErrMessage(
                    this,
                    transType.getTransName()+ " " + title,
                    "Please Insert Terms",
                    null,
                    Constants.FAILED_DIALOG_SHOW_TIME
            );
            editTerms.requestFocus();
        }
        else if(editAmt.isShown() && (amt == null || ("000").equals(amt)) ){
            DialogUtils.showErrMessage(
                    this,
                    transType.getTransName()+ " " + title,
                    "Please Insert Amount",
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
//            promotionKey = promotionKey != null && !promotionKey.equals("0") ? promotionKey : supplier+product ;
            if (promotionKey == null || promotionKey.equals("0")) {
                if (supplier == null || supplier.length() < 5) {
                    DialogUtils.showErrMessage(
                            this,
                            transType.getTransName()+ " " + title,
                            "Please Insert Supplier 5 Digits",
                            null,
                            Constants.FAILED_DIALOG_SHOW_TIME
                    );
                    editSupplier.requestFocus();
                    return;
                }
                if (product == null || product.length() < 9) {
                    DialogUtils.showErrMessage(
                            this,
                            transType.getTransName()+ " " + title,
                            "Please Insert Product 9 Digits",
                            null,
                            Constants.FAILED_DIALOG_SHOW_TIME
                    );
                    editProduct.requestFocus();
                    return;
                }
                promotionKey = supplier+product;
            }

            if (promotionKey == null) {
                DialogUtils.showErrMessage(
                        this,
                        transType.getTransName()+ " " + title,
                        "Invalid Promotion Key, Please retry.",
                        null,
                        Constants.FAILED_DIALOG_SHOW_TIME
                );
                return;
            }
            confirmBtn.setEnabled(false);
            //Log.e("menu","promotionKey = " + promotionKey + ", serialNum = " + serialNum + ", terms = " + terms + ", amt = " + amt + ", isPromoProduct = " + isPromoProduct);
            finish(new ActionResult(TransResult.SUCC,  new ActionEnterInstalmentKbank.InstalmentKbankInfo
                    (promotionKey,serialNum,terms,amt,isPromoProduct)));
        }
    }


    @Override
    protected boolean onKeyBackDown() {
        finish(new ActionResult(TransResult.ERR_USER_CANCEL,null));
        return true;
    }

    private String getPromoTypeVal(){
        if(spnPromoType.isShown()){
            final String[] discountTypeValue = getResources().getStringArray(R.array.instalment_promo_type_kbank_value);
            return discountTypeValue[spnPromoType.getSelectedItemPosition()];
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
