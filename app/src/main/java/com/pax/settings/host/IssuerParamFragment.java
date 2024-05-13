/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-12-29
 * Module Author: xiawh
 * Description:
 *
 * ============================================================================
 */
package com.pax.settings.host;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.pax.edc.R;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Issuer;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.emv.EmvAid;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.EnterAmountTextWatcher;
import com.pax.pay.utils.TextValueWatcher;
import com.pax.settings.BaseFragment;
import com.pax.settings.NewSpinnerAdapter;
import com.pax.settings.SysParam;
import com.pax.view.keyboard.CustomKeyboardEditText;

import java.util.ArrayList;
import java.util.List;

import th.co.bkkps.utils.Log;

public class IssuerParamFragment extends BaseFragment implements CompoundButton.OnCheckedChangeListener {

    private Issuer issuer;
    private NewSpinnerAdapter<Issuer> adapter;

    private CustomKeyboardEditText floorLimit;
    private EditText adjustPercent;
    private CheckBox pinRequired;
    private CheckBox enablePrint;
    private CheckBox checkPan;
    private CheckBox checkExpiry;
    private CheckBox enableManualPan;
    private CheckBox enableOffline;
    private CheckBox enableRefund;
    private CheckBox enableAdjust;
    private CheckBox enableExpiry;
    private CheckBox enableReferral;
    private CheckBox enableAutoReversal;
    private CheckBox enableSmallAmt;
    private EditText smallAmt;
    private EditText numOfReceiptSmallAmt;
    private CheckBox enablePreAuth;
    private EditText saleCompletionPercent;

    private boolean isNewIssuer = false;

    private EnterAmountTextWatcher watcherFloorLimit;
    private EnterAmountTextWatcher watcherSmallAmt;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_issuer_details;
    }

    @Override
    protected void initData() {
        Bundle bundle = getArguments();
        if (bundle != null) {
            String issuerName = bundle.getString(EUIParamKeys.ISSUER_NAME.toString());
            issuer = FinancialApplication.getAcqManager().findIssuer(issuerName);
        }

        List<Issuer> listIssuers = FinancialApplication.getAcqManager().findAllIssuers();
        if (issuer == null && !listIssuers.isEmpty())
            issuer = listIssuers.get(0);

        adapter = new NewSpinnerAdapter<>(this.context);
        adapter.setListInfo(listIssuers);
        adapter.setOnTextUpdateListener(new NewSpinnerAdapter.OnTextUpdateListener() {
            @Override
            public String onTextUpdate(final List<?> list, int position) {
                return ((Issuer) list.get(position)).getName();
            }
        });
    }

    @Override
    protected void initView(View view) {
        Spinner spinner = (Spinner) view.findViewById(R.id.issuer_list);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int pos, long id) {
                Issuer newIssuer = adapter.getListInfo().get(pos);
                if (newIssuer.getId() != issuer.getId()) {
                    List<Issuer> listUpdate = updateItemList();
                    updateAllIssuerName(listUpdate);
                    if (newIssuer.getIssuerName().equals(issuer.getIssuerName())) {
                        cloneUpdatedValue(newIssuer, issuer);
                    }
                    issuer = newIssuer;
                    isNewIssuer = true;
                    updateItemsValue();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        });

        if (!adapter.getListInfo().isEmpty()) {
            updateItems(view);
            updateItemsValue();
        } else {
            view.findViewById(R.id.issuer_details).setVisibility(View.GONE);
        }
    }

    private void updateItems(View view) {
        // AET-49
        floorLimit = (CustomKeyboardEditText) view.findViewById(R.id.issuer_floor_limit);
        //AET-151
        floorLimit.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                floorLimit.setCursorVisible(true);
                adjustPercent.setCursorVisible(false);
                smallAmt.setCursorVisible(false);
                numOfReceiptSmallAmt.setCursorVisible(false);
                return false;
            }
        });

        floorLimit.removeTextChangedListener(watcherFloorLimit);
        watcherFloorLimit = new EnterAmountTextWatcher();
        watcherFloorLimit.setMaxValue(99999999L);// AET-238
        watcherFloorLimit.setOnTipListener(new EnterAmountTextWatcher.OnTipListener() {
            @Override
            public void onUpdateTipListener(long baseAmount, long tipAmount) {
                if (!isNewIssuer) {
                    issuer.setFloorLimit(tipAmount);
                }
            }

            @Override
            public boolean onVerifyTipListener(long baseAmount, long tipAmount) {
                return true;
            }
        });
        floorLimit.addTextChangedListener(watcherFloorLimit);

        enableAdjust = (CheckBox) view.findViewById(R.id.issuer_enable_adjust);
        enableAdjust.setOnCheckedChangeListener(this);

        adjustPercent = (EditText) view.findViewById(R.id.issuer_adjust_percent);
        //AET-151
        adjustPercent.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                floorLimit.setCursorVisible(false);
                adjustPercent.setCursorVisible(true);
                smallAmt.setCursorVisible(false);
                numOfReceiptSmallAmt.setCursorVisible(false);
                return false;
            }
        });
        TextValueWatcher<Float> textValueWatcher = new TextValueWatcher<>(0.0f, 100.0f);
        textValueWatcher.setOnCompareListener(new TextValueWatcher.OnCompareListener() {
            @Override
            public boolean onCompare(String value, Object min, Object max) {
                float temp = Float.parseFloat(value);
                return temp >= (float) min && temp <= (float) max;
            }
        });
        textValueWatcher.setOnTextChangedListener(new TextValueWatcher.OnTextChangedListener() {
            @Override
            public void afterTextChanged(String value) {
                issuer.setAdjustPercent(Float.parseFloat(value));
            }
        });

        adjustPercent.addTextChangedListener(textValueWatcher);

        enableOffline = (CheckBox) view.findViewById(R.id.issuer_enable_offline);
        enableOffline.setOnCheckedChangeListener(this);

        enableRefund = (CheckBox) view.findViewById(R.id.issuer_allow_refund);
        enableRefund.setOnCheckedChangeListener(this);

        enableExpiry = (CheckBox) view.findViewById(R.id.issuer_enable_expiry);
        enableExpiry.setOnCheckedChangeListener(this);

        enableManualPan = (CheckBox) view.findViewById(R.id.issuer_enable_manualPan);
        enableManualPan.setOnCheckedChangeListener(this);

        checkExpiry = (CheckBox) view.findViewById(R.id.issuer_check_expiry);
        checkExpiry.setOnCheckedChangeListener(this);

        checkPan = (CheckBox) view.findViewById(R.id.issuer_check_pan);
        checkPan.setOnCheckedChangeListener(this);

        enablePrint = (CheckBox) view.findViewById(R.id.issuer_enable_print);
        enablePrint.setOnCheckedChangeListener(this);

        pinRequired = (CheckBox) view.findViewById(R.id.issuer_pin_required);
        pinRequired.setOnCheckedChangeListener(this);

        enableReferral = (CheckBox) view.findViewById(R.id.issuer_referral);
        enableReferral.setOnCheckedChangeListener(this);

        enableAutoReversal = (CheckBox) view.findViewById(R.id.issuer_auto_reversal);
        enableAutoReversal.setOnCheckedChangeListener(this);

        enableSmallAmt = (CheckBox) view.findViewById(R.id.issuer_enable_small_amt);
        enableSmallAmt.setOnCheckedChangeListener(this);

        smallAmt = (EditText) view.findViewById(R.id.issuer_small_amt);

        smallAmt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                floorLimit.setCursorVisible(false);
                adjustPercent.setCursorVisible(false);
                smallAmt.setCursorVisible(true);
                numOfReceiptSmallAmt.setCursorVisible(false);
                return false;
            }
        });

        smallAmt.removeTextChangedListener(watcherSmallAmt);
        watcherSmallAmt = new EnterAmountTextWatcher(smallAmt);
        watcherSmallAmt.setMaxValue(999999999999L);
        watcherSmallAmt.setOnTipListener(new EnterAmountTextWatcher.OnTipListener() {
            @Override
            public void onUpdateTipListener(long baseAmount, long tipAmount) {
                if (!isNewIssuer) {
                    issuer.setSmallAmount(tipAmount);
                }
            }

            @Override
            public boolean onVerifyTipListener(long baseAmount, long tipAmount) {
                return true;
            }
        });
        smallAmt.addTextChangedListener(watcherSmallAmt);

        enableAdjust = (CheckBox) view.findViewById(R.id.issuer_enable_adjust);
        enableAdjust.setOnCheckedChangeListener(this);

        numOfReceiptSmallAmt = (EditText) view.findViewById(R.id.issuer_num_of_receipt);

        numOfReceiptSmallAmt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                floorLimit.setCursorVisible(false);
                adjustPercent.setCursorVisible(false);
                smallAmt.setCursorVisible(false);
                numOfReceiptSmallAmt.setCursorVisible(true);
                return false;
            }
        });
        TextValueWatcher<Integer> numOfSlipWatcher = new TextValueWatcher<>(0, 2);
        numOfSlipWatcher.setOnCompareListener(new TextValueWatcher.OnCompareListener() {
            @Override
            public boolean onCompare(String value, Object min, Object max) {
                int temp = Integer.parseInt(value);
                return temp >= (int) min && temp <= (int) max;
            }
        });
        numOfSlipWatcher.setOnTextChangedListener(new TextValueWatcher.OnTextChangedListener() {
            @Override
            public void afterTextChanged(String value) {
                issuer.setNumberOfReceipt(Integer.parseInt(value));
            }
        });
        numOfReceiptSmallAmt.addTextChangedListener(numOfSlipWatcher);


        enablePreAuth = (CheckBox) view.findViewById(R.id.issuer_enable_preauth);
        enablePreAuth.setOnCheckedChangeListener(this);

        saleCompletionPercent = (EditText) view.findViewById(R.id.issuer_salecompletion_percent);
        //AET-151
        saleCompletionPercent.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                adjustPercent.setCursorVisible(true);
                return false;
            }
        });
        TextValueWatcher<Float> seleCompValueValidator = new TextValueWatcher<>(0.0f, 100.0f);
        seleCompValueValidator.setOnCompareListener(new TextValueWatcher.OnCompareListener() {
            @Override
            public boolean onCompare(String value, Object min, Object max) {
                float temp = Float.parseFloat(value);
                return temp >= (float) min && temp <= (float) max;
            }
        });
        seleCompValueValidator.setOnTextChangedListener(new TextValueWatcher.OnTextChangedListener() {
            @Override
            public void afterTextChanged(String value) {
                issuer.setSaleCompPercent(Float.parseFloat(value));
            }
        });
        saleCompletionPercent.addTextChangedListener(seleCompValueValidator);

    }

    private void updateItemsValue() {
        if (isNewIssuer) {
            floorLimit.setText("");
            smallAmt.setText("");
            isNewIssuer = false;
        }
        if (watcherFloorLimit != null)
            watcherFloorLimit.setAmount(0, issuer.getFloorLimit());
        floorLimit.setText(CurrencyConverter.convert(issuer.getFloorLimit()));

        enableAdjust.setChecked(issuer.isEnableAdjust());

        adjustPercent.setText(String.valueOf(issuer.getAdjustPercent()));
        adjustPercent.setEnabled(enableAdjust.isChecked());

        enableOffline.setChecked(issuer.isEnableOffline());

        enableRefund.setChecked(issuer.isAllowRefund());

        enableExpiry.setChecked(issuer.isAllowExpiry());

        enableManualPan.setChecked(issuer.isAllowManualPan());

        checkExpiry.setChecked(issuer.isAllowCheckExpiry());

        checkPan.setChecked(issuer.isAllowCheckPanMod10());

        enablePrint.setChecked(issuer.isAllowPrint());

        pinRequired.setChecked(issuer.isRequirePIN());

        enableReferral.setChecked(issuer.isReferral());
        enableAutoReversal.setChecked(issuer.isAutoReversal());

        enableSmallAmt.setChecked(issuer.isEnableSmallAmt());

        if (watcherSmallAmt != null)
            watcherSmallAmt.setAmount(0, issuer.getSmallAmount());
        smallAmt.setText(CurrencyConverter.convert(issuer.getSmallAmount()));

        numOfReceiptSmallAmt.setText(String.valueOf(issuer.getNumberOfReceipt()));


        // PreAuth & Sale Completion configuration
        // ======================================================================== <<
        boolean edcAllowPreAuth = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_PREAUTH, false);
        enablePreAuth.setChecked(issuer.isAllowPreAuth());

        saleCompletionPercent.setText(String.valueOf(issuer.getSaleCompPercent()));
        saleCompletionPercent.setEnabled(edcAllowPreAuth);
        // ======================================================================== <<

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.issuer_enable_adjust:
                issuer.setEnableAdjust(isChecked);
                break;
            case R.id.issuer_enable_offline:
                issuer.setEnableOffline(isChecked);
                break;
            case R.id.issuer_allow_refund:
                issuer.setAllowRefund(isChecked);
                break;
            case R.id.issuer_enable_expiry:
                issuer.setAllowExpiry(isChecked);
                break;
            case R.id.issuer_enable_manualPan:
                issuer.setAllowManualPan(isChecked);
                break;
            case R.id.issuer_check_expiry:
                issuer.setAllowCheckExpiry(isChecked);
                break;
            case R.id.issuer_check_pan:
                issuer.setAllowCheckPanMod10(isChecked);
                break;
            case R.id.issuer_enable_print:
                issuer.setAllowPrint(isChecked);
                break;
            case R.id.issuer_pin_required:
                issuer.setRequirePIN(isChecked);
                break;
            case R.id.issuer_referral:
                issuer.setReferral(isChecked);
                break;
            case R.id.issuer_auto_reversal:
                issuer.setAutoReversal(isChecked);
                break;
            case R.id.issuer_enable_small_amt:
                issuer.setEnableSmallAmt(isChecked);
                break;
            case R.id.issuer_enable_preauth:
                issuer.setAllowPreAuth(isChecked);
                break;
            default:
                break;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (issuer != null) {
            updateAllIssuerName(null); //updateAllIssuerBrand(null);
            List<EmvAid> emvAids = FinancialApplication.getEmvDbHelper().findAIDByIssuerBrand(issuer.getIssuerBrand());
            for (EmvAid aid : emvAids) {
                if (issuer.getFloorLimit() != aid.getFloorLimit()) {
                    aid.setFloorLimit(issuer.getFloorLimit());
                    FinancialApplication.getEmvDbHelper().updateAID(aid);
                }
            }
        }
    }

    private void updateAllIssuerBrand(List<Issuer> issuers) {
        // update all issuers matched with issuer brand
        issuers = issuers == null ? FinancialApplication.getAcqManager().findIssuerByBrand(issuer.getIssuerBrand()) : issuers;
        if (issuers != null) {
            for (Issuer tIssuer : issuers) {
                cloneUpdatedValue(tIssuer, issuer);
                FinancialApplication.getAcqManager().updateIssuer(tIssuer);
            }
        }
    }

    private void updateAllIssuerName(List<Issuer> issuers) {
        // update all issuers matched with issuer name
        issuers = issuers == null ? FinancialApplication.getAcqManager().findIssuerByName(issuer.getIssuerName()) : issuers;
        if (issuers != null) {
            for (Issuer tIssuer : issuers) {
                cloneUpdatedValue(tIssuer, issuer);
                FinancialApplication.getAcqManager().updateIssuer(tIssuer);
            }
        }
    }

    private Issuer cloneUpdatedValue(Issuer target, Issuer source) {
        target.setFloorLimit(source.getFloorLimit());
        target.setAdjustPercent(source.getAdjustPercent());
        target.setEnableAdjust(source.isEnableAdjust());
        target.setEnableOffline(source.isEnableOffline());
        target.setAllowRefund(source.isAllowRefund());
        target.setAllowExpiry(source.isAllowExpiry());
        target.setAllowManualPan(source.isAllowManualPan());
        target.setAllowCheckExpiry(source.isAllowCheckExpiry());
        target.setAllowPrint(source.isAllowPrint());
        target.setAllowCheckPanMod10(source.isAllowCheckPanMod10());
        target.setRequirePIN(source.isRequirePIN());
        target.setReferral(source.isReferral());
        target.setAutoReversal(source.isAutoReversal());
        target.setEnableSmallAmt(source.isEnableSmallAmt());
        target.setSmallAmount(source.getSmallAmount());
        target.setNumberOfReceipt(source.getNumberOfReceipt());
        target.setAllowPreAuth(source.isAllowPreAuth());
        target.setSaleCompPercent(source.getSaleCompPercent());
        return target;
    }

    private List<Issuer> updateItemList() {
        List<Issuer> results = new ArrayList<>();
        List<Issuer> items = adapter.getListInfo();
        for (int i=0 ; i < items.size() ; i++) {
            Issuer lIssuer = items.get(i);
            if (lIssuer.getIssuerName().equals(issuer.getIssuerName())) {
                cloneUpdatedValue(lIssuer, issuer);
                adapter.updateItem(i, lIssuer);
                results.add(lIssuer);
            }
        }
        return results;
    }
}
