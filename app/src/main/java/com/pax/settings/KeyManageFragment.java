/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-11-30
 * Module Author: Steven.W
 * Description:
 *
 * ============================================================================
 */
package com.pax.settings;

import android.preference.*;
import th.co.bkkps.utils.Log;
import androidx.annotation.XmlRes;
import com.pax.dal.exceptions.PedDevException;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.glwrapper.convert.IConvert.EPaddingPosition;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.utils.ToastUtils;

public class KeyManageFragment extends BasePreferenceFragment {
    @Override
    @XmlRes
    protected int getResourceId() {
        return R.xml.key_manage_pref;
    }

    @Override
    protected void initPreference() {
        bindPreference(SysParam.NumberParam.MK_INDEX);
        bindPreference(SysParam.StringParam.KEY_ALGORITHM);
        bindPreference(SysParam.NumberParam.MK_INDEX_MANUAL);
        bindPreference(SysParam.StringParam.MK_VALUE);
        bindPreference(SysParam.StringParam.PK_VALUE);
        bindPreference(SysParam.StringParam.AK_VALUE);
    }

    @Override
    protected boolean onCheckBoxPreferenceChanged(CheckBoxPreference preference, Object value, boolean isInitLoading) {
        return true;
    }

    @Override
    protected boolean onRingtonePreferenceChanged(RingtonePreference preference, Object value, boolean isInitLoading) {
        return true;
    }

    private void writeMK(EditTextPreference preference, String stringValue) {
        int index = FinancialApplication.getSysParam().get(SysParam.NumberParam.MK_INDEX_MANUAL, -1);
        if ((index < 0) || (index >= 100)) {
            ToastUtils.showMessage(R.string.keyManage_menu_tmk_index_no);
        } else {
            if (stringValue.length() != 16) {
                ToastUtils.showMessage(R.string.input_len_err);
            } else {
                // 写主密钥
                try {
                    Device.writeTMK((byte) index,
                            FinancialApplication.getConvert().strToBcd(stringValue, EPaddingPosition.PADDING_LEFT));
                    Device.beepOk();
                    ToastUtils.showMessage(R.string.set_key_success);
                    //Don't return true cuz we don't want to write it to file
                } catch (PedDevException e) {
                    Log.e(TAG, "", e);
                    Device.beepErr();
                    ToastUtils.showMessage(R.string.set_key_fail);
                }
            }
        }
    }

    private void writePK(EditTextPreference preference, String stringValue) {
        if (stringValue.length() != 16) {
            ToastUtils.showMessage(R.string.input_len_err);
        } else {
            try {
                Device.writeTPK(FinancialApplication.getConvert().strToBcd(stringValue,
                        EPaddingPosition.PADDING_LEFT), null);
                Device.beepOk();
                ToastUtils.showMessage(R.string.set_key_success);
            } catch (PedDevException e) {
                Log.e(TAG, "", e);
                Device.beepErr();
                ToastUtils.showMessage(R.string.set_key_fail);
            }
        }
    }

    private void writeAK(EditTextPreference preference, String stringValue) {
        if (stringValue.length() != 16) {
            ToastUtils.showMessage(R.string.input_len_err);
        } else {
            try {
                Device.writeTAK(FinancialApplication.getConvert().strToBcd(stringValue,
                        EPaddingPosition.PADDING_LEFT), null);
                Device.beepOk();
                ToastUtils.showMessage(R.string.set_key_success);
            } catch (PedDevException e) {
                Log.e(TAG, "", e);
                Device.beepErr();
                ToastUtils.showMessage(R.string.set_key_fail);
            }
        }
    }

    private boolean writeMKIndex(EditTextPreference preference, String stringValue) {
        boolean failed = false;
        int intValue = Integer.parseInt(stringValue);
        if (intValue < 0 || intValue >= 100) {
            failed = true;
        } else {
            preference.setSummary(stringValue);
        }

        if (failed) {
            ToastUtils.showMessage(R.string.input_err);
        }

        return !failed;
    }

    @Override
    protected boolean onEditTextPreferenceChanged(EditTextPreference preference, Object value, boolean isInitLoading) {
        String key = preference.getKey();
        String stringValue = value.toString();

        if (SysParam.StringParam.MK_VALUE.toString().equals(key)) {
            if (!isInitLoading) {
                writeMK(preference, stringValue);
            }
            return false;
        } else if (SysParam.StringParam.PK_VALUE.toString().equals(key)) {
            if (!isInitLoading) {
                writePK(preference, stringValue);
            }
            return false;
        } else if (SysParam.StringParam.AK_VALUE.toString().equals(key)) {
            if (!isInitLoading) {
                writeAK(preference, stringValue);
            }
            return false;
        } else if (SysParam.NumberParam.MK_INDEX.toString().equals(key)
                || SysParam.NumberParam.MK_INDEX_MANUAL.toString().equals(key)) {
            return writeMKIndex(preference, stringValue);
        } else {
            preference.setSummary(value.toString());
        }
        return true;
    }

    @Override
    protected boolean onListPreferenceChanged(ListPreference preference, Object value,
                                              boolean isInitLoading) {
        String stringValue = value.toString();
        int index = preference.findIndexOfValue(stringValue);
        preference.setSummary(index >= 0 ? preference.getEntries()[index] : null);
        return true;
    }

    @Override
    protected boolean onMultiSelectListPreferenceChanged(MultiSelectListPreference preference, Object value, boolean isInitLoading) {
        return false;
    }
}
