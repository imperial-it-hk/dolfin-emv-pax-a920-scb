/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-1-10
 * Module Author: xiawh
 * Description:
 *
 * ============================================================================
 */
package com.pax.settings.host;

import android.preference.*;
import android.text.InputFilter;
import android.text.InputType;
import th.co.bkkps.utils.Log;
import androidx.annotation.XmlRes;

import com.pax.appstore.DownloadManager;
import com.pax.edc.R;
import com.pax.pay.WizardActivity;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.InputFilterMinMax;
import com.pax.pay.utils.ToastUtils;
import com.pax.pay.utils.Utils;
import com.pax.settings.BasePreferenceFragment;
import com.pax.settings.SysParam;
import com.pax.view.EditTextPreferenceFix;
import com.pax.view.dialog.DialogUtils;
import com.pax.view.keyboard.KeyboardUtils;
import com.pax.view.widget.BaseWidget;

import java.util.*;


public class EDCFragment extends BasePreferenceFragment {

    private static CharSequence[] entries;
    private static CharSequence[] entryValues;


    // AET-116
    private static void updateEntries() {
        Map<String, String> allEntries = new TreeMap<>();
        List<Locale> locales = CurrencyConverter.getSupportedLocale();
        for (Locale i : locales) {
            try {
                Currency currency = Currency.getInstance(i);
                //Log.i(TAG, i.getISO3Country() + "  " + i.getDisplayName(Locale.US) + " " + currency.getDisplayName(Locale.US));
                allEntries.put(i.getDisplayName(Locale.US) + " " + currency.getDisplayName(), i.getDisplayName(Locale.US));
            } catch (IllegalArgumentException e) {
                //Log.d(TAG, "", e);
            }
        }
        entries = allEntries.keySet().toArray(new CharSequence[allEntries.size()]);
        entryValues = allEntries.values().toArray(new CharSequence[allEntries.size()]);
    }

    @Override
    @XmlRes
    protected int getResourceId() {
        return R.xml.edc_para_pref;
    }

    @Override
    protected void initPreference() {
        updateEntries();

        bindListPreference(SysParam.StringParam.EDC_CURRENCY_LIST, entries, entryValues);

        bindPreference(SysParam.StringParam.EDC_MERCHANT_NAME_EN);
        bindPreference(SysParam.StringParam.EDC_MERCHANT_ADDRESS);
        bindPreference(SysParam.StringParam.EDC_MERCHANT_ADDRESS1);
        bindPreference(SysParam.StringParam.EDC_CURRENCY_LIST);
        bindPreference(SysParam.StringParam.EDC_PED_MODE);
        bindPreference(SysParam.NumberParam.EDC_RECEIPT_NUM);
        bindPreference(SysParam.NumberParam.EDC_STAN_NO);
//        bindPreference(SysParam.BooleanParam.EDC_SUPPORT_TIP);
        bindPreference(SysParam.BooleanParam.EDC_CONFIRM_PROCESS);
        bindPreference(SysParam.BooleanParam.EDC_SUPPORT_REFUND);
        bindPreference(SysParam.BooleanParam.EDC_ENABLE_OFFLINE);
        bindPreference(SysParam.BooleanParam.EDC_ENABLE_TIP_ADJUST);
        bindPreference(SysParam.NumberParam.EDC_MAX_PERCENTAGE_TIP_ADJUST);

        bindPreference(SysParam.BooleanParam.SUPPORT_USER_AGREEMENT);
        bindPreference(SysParam.BooleanParam.EDC_ENABLE_PAPERLESS);
        bindPreference(SysParam.StringParam.EDC_SMTP_HOST);
        bindPreference(SysParam.NumberParam.EDC_SMTP_PORT);
        bindPreference(SysParam.StringParam.EDC_SMTP_USERNAME);
        bindPreference(SysParam.StringParam.EDC_SMTP_PASSWORD);
        bindPreference(SysParam.BooleanParam.EDC_SMTP_ENABLE_SSL);
        bindPreference(SysParam.NumberParam.EDC_SMTP_SSL_PORT);
        bindPreference(SysParam.StringParam.EDC_SMTP_FROM);

        bindPreference(SysParam.NumberParam.EDC_TRACE_NO);
        bindPreference(SysParam.NumberParam.EDC_NUM_OF_SLIP_LINEPAY);
        bindPreference(SysParam.BooleanParam.EDC_ENABLE_E_SIGNATURE);
        bindPreference(SysParam.BooleanParam.EDC_ENABLE_GRAND_TOTAL);

        bindPreference(SysParam.StringParam.EDC_DEFAULT_CAMERA);
        bindPreference(SysParam.BooleanParam.EDC_ENABLE_QR_BARCODE);
        bindPreference(SysParam.BooleanParam.EDC_KIOSK_MODE);
        bindPreference(SysParam.NumberParam.EDC_KIOSK_TIMEOUT);
        bindPreference(SysParam.BooleanParam.EDC_ENABLE_IMG_ON_END_RECEIPT);

        //contactless
        bindPreference(SysParam.BooleanParam.EDC_ENABLE_CONTACTLESS);
        bindPreference(SysParam.BooleanParam.EDC_ENABLE_CONTACTLESS_VISA);
        bindPreference(SysParam.BooleanParam.EDC_ENABLE_CONTACTLESS_MASTER);
        bindPreference(SysParam.BooleanParam.EDC_ENABLE_CONTACTLESS_JCB);
        bindPreference(SysParam.BooleanParam.EDC_ENABLE_CONTACTLESS_UP);
        bindPreference(SysParam.BooleanParam.EDC_ENABLE_CONTACTLESS_TPN);
        bindPreference(SysParam.BooleanParam.EDC_ENABLE_CONTACTLESS_AMEX);

        bindPreference(SysParam.BooleanParam.EDC_ENABLE_KEYIN);
        bindPreference(SysParam.BooleanParam.EDC_ENABLE_QR_BARCODE_ALIPAY_WECHAT);

        bindPreference(SysParam.BooleanParam.EDC_ENABLE_LOGGLY);
        bindPreference(SysParam.BooleanParam.EDC_ENABLE_VOID_WITH_STAND);
        bindPreference(SysParam.BooleanParam.EDC_SUPPORT_SP200);

        bindPreference(SysParam.BooleanParam.KBANK_DYNAMIC_OFFLINE_SHOW_MENU);
        bindPreference(SysParam.StringParam.EDC_CTLS_TRANS_LIMIT);
        bindPreference(SysParam.StringParam.EDC_MAXIMUM_AMOUNT);
        bindPreference(SysParam.StringParam.EDC_MINIMUM_AMOUNT);
        bindPreference(SysParam.BooleanParam.EDC_ENABLE_FORCE_SEL_APP_FOR_DUO_BRAND_CARD);
        bindPreference(SysParam.BooleanParam.EDC_ENABLE_PREAUTH);
    }

    @Override
    protected void bindPreference(SysParam.NumberParam key) {
        if (key == SysParam.NumberParam.EDC_MAX_PERCENTAGE_TIP_ADJUST) {
            EditTextPreference preference = (EditTextPreference) super.findPreference(key.toString());
            preference.getEditText().setFilters(new InputFilter[]{new InputFilterMinMax(1, 100)});
            super.bindPreference(preference);
        } else {
            super.bindPreference(key);
        }
    }

    @Override
    protected boolean onListPreferenceChanged(ListPreference preference, Object value, boolean isInitLoading) {
        String stringValue = value.toString();
        int index = preference.findIndexOfValue(stringValue);

        if (SysParam.StringParam.EDC_CURRENCY_LIST.toString().equals(preference.getKey()) && index >= 0) {
            if (!isInitLoading && FinancialApplication.getTransDataDbHelper().countOf() > 0) {
                ToastUtils.showMessage(R.string.has_trans_for_settle);
                return false;
            } else {
                preference.setSummary(index >= 0 ? preference.getEntries()[index] : null);
                if (!isInitLoading && !CurrencyConverter.getDefCurrency().getCountry().equals(preference.getEntryValues()[index].toString())) {
                    BaseWidget.updateWidget(FinancialApplication.getApp());
                    //Utils.changeAppLanguage(FinancialApplication.getApp(), CurrencyConverter.setDefCurrency(preference.getEntryValues()[index].toString()));

                    Utils.restart();
                }
            }
            return true;
        }
        preference.setSummary(index >= 0 ? preference.getEntries()[index] : null);
        return true;
    }

    @Override
    protected boolean onCheckBoxPreferenceChanged(CheckBoxPreference preference, Object value, boolean isInitLoading) {

        if (SysParam.BooleanParam.EDC_ENABLE_KEYIN.toString().equals(preference.getKey())
            || SysParam.BooleanParam.EDC_SUPPORT_SP200.toString().equals(preference.getKey())) {
            if (!isInitLoading){
               Utils.restart();
            }
        }

        if (SysParam.BooleanParam.EDC_ENABLE_LOGGLY.toString().equals(preference.getKey())) {
            if ((boolean)value){
                Log.AddLoggly();
            } else {
                Log.RemoveLoggly();
            }
        }

        if (SysParam.BooleanParam.KBANK_DYNAMIC_OFFLINE_SHOW_MENU.toString().equals(preference.getKey())) {
            if ( ! (boolean)value) {
                if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.DYNAMIC_OFFLINE_MODE_ENABLED)) {
                    ToastUtils.showMessage(R.string.unable_to_disable_dynamic_offline);
                    return false;
                }
            }
        }

        if (SysParam.BooleanParam.EDC_ENABLE_FORCE_SEL_APP_FOR_DUO_BRAND_CARD.toString().equals(preference.getKey())) {
            Log.d("SYSPARAM", "Force Application selection for duo-brand card status = " + (boolean)value);
        }

        if (SysParam.BooleanParam.EDC_ENABLE_OFFLINE.toString().equals(preference.getKey())) {
            DownloadManager.getInstance().updateOfflineTipAdjustEnable((boolean) value, true);
        }

        if (SysParam.BooleanParam.EDC_ENABLE_TIP_ADJUST.toString().equals(preference.getKey())) {
            //DownloadManager.getInstance().updateOfflineTipAdjustEnable((boolean) value, false);
        }

        return true;
    }

    @Override
    protected boolean onRingtonePreferenceChanged(RingtonePreference preference, Object value, boolean isInitLoading) {
        return true;
    }

    @Override
    protected boolean onEditTextPreferenceChanged(EditTextPreference preference, Object value, boolean isInitLoading) {
        String stringValue = value.toString();
        if ((preference.getEditText().getInputType() & InputType.TYPE_TEXT_VARIATION_PASSWORD) == InputType.TYPE_TEXT_VARIATION_PASSWORD) {
            String temp = !stringValue.isEmpty() ? "******" : stringValue;
            preference.setSummary(temp);
        }else if(!isInitLoading && preference.getKey().equalsIgnoreCase(SysParam.NumberParam.EDC_KIOSK_TIMEOUT.name())){
            int intValue = stringValue != null ? Integer.parseInt(stringValue) : 0;
            if(intValue <= 0){
                ToastUtils.showMessage(R.string.err_kiosk_timeout_val);
                return false;
            }
        } else if (preference.getKey().equalsIgnoreCase(SysParam.StringParam.EDC_MAXIMUM_AMOUNT.name())
                || preference.getKey().equalsIgnoreCase(SysParam.StringParam.EDC_MINIMUM_AMOUNT.name())
                || preference.getKey().equalsIgnoreCase(SysParam.StringParam.EDC_CTLS_TRANS_LIMIT.name())) {
            stringValue = stringValue.replaceAll("\\D+", "");
            preference.setText(stringValue);
            preference.setSummary(CurrencyConverter.convert(Utils.parseLongSafe(stringValue, 0)));
            if (preference.getKey().equalsIgnoreCase(SysParam.StringParam.EDC_CTLS_TRANS_LIMIT.name())) {
                DownloadManager.getInstance().updateCtlsTransLimit(stringValue);
            }
        } else if (preference.getKey().equalsIgnoreCase(SysParam.NumberParam.EDC_MAX_PERCENTAGE_TIP_ADJUST.name())) {
            preference.setSummary(stringValue);
            //DownloadManager.getInstance().updateTipAdjustPercent(stringValue);
        }
        else {
            preference.setSummary(stringValue);
        }

        KeyboardUtils.hideSystemKeyboard(getActivity(), preference.getEditText()); //AET-206
        return true;
    }

    @Override
    protected boolean onMultiSelectListPreferenceChanged(MultiSelectListPreference preference, Object value, boolean isInitLoading) {
        return false;
    }

}
