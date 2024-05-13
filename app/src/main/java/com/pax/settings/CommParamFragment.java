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

import android.app.Activity;
import android.app.ProgressDialog;
import android.preference.*;
import android.provider.Settings;
import th.co.bkkps.utils.Log;
import androidx.annotation.XmlRes;
import com.pax.dal.entity.ERoute;
import com.pax.dal.exceptions.PedDevException;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.glwrapper.convert.IConvert;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.app.MultiPathProgressiveListener;
import com.pax.pay.utils.ToastUtils;
import com.pax.pay.utils.Utils;

public class CommParamFragment extends BasePreferenceFragment {
    private ProgressDialog progressDialog;

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference.getTitleRes() == R.string.open_wifi) {
            Device.enableBackKey(true);
            Utils.callSystemSettings(getActivity(), Settings.ACTION_WIFI_SETTINGS);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    @XmlRes
    protected int getResourceId() {
        return R.xml.comm_para_pref;
    }

    @Override
    protected void initPreference() {
        bindPreference(SysParam.StringParam.COMM_TYPE);
        bindPreference(SysParam.NumberParam.COMM_TIMEOUT);
        bindPreference(SysParam.StringParam.MOBILE_TEL_NO);
        bindPreference(SysParam.StringParam.MOBILE_APN);
        bindPreference(SysParam.StringParam.MOBILE_APN_SYSTEM);

        bindPreference(SysParam.StringParam.MOBILE_USER);
        bindPreference(SysParam.StringParam.MOBILE_PWD);
    }

    @Override
    protected boolean onCheckBoxPreferenceChanged(CheckBoxPreference preference, Object value, boolean isInitLoading) {
        return true;
    }

    @Override
    protected boolean onEditTextPreferenceChanged(EditTextPreference preference, Object value, boolean isInitLoading) {
        String stringValue = value.toString();
        preference.setSummary(stringValue);
        return true;
    }

    @Override
    protected boolean onRingtonePreferenceChanged(RingtonePreference preference, Object value, boolean isInitLoading) {
        return true;
    }

    @Override
    protected boolean onListPreferenceChanged(ListPreference preference, Object value, boolean isInitLoading) {
        // For list preferences, look up the correct display value in
        // the preference's 'entries' list.
        Log.i("CommParamFragment", value.toString());
        String stringValue = value.toString();
        int index = preference.findIndexOfValue(stringValue);

        // Set the summary to reflect the new value.
        /*if (!isInitLoading && FinancialApplication.getTransDataDbHelper().countOf() > 0) {
            ToastUtils.showMessage(R.string.has_trans_for_settle);
            return false;
        } else*/ {
            final String hardcodeKey = "1234567890123456";
            boolean isFirstRun = FinancialApplication.getController().isFirstRun();
            if ("DEMO".equals(stringValue) && isFirstRun) {
                int keyIndex = FinancialApplication.getSysParam().get(SysParam.NumberParam.MK_INDEX);
                try {
                    Device.writeTMK((byte) keyIndex,
                            FinancialApplication.getConvert().strToBcd(hardcodeKey, IConvert.EPaddingPosition.PADDING_LEFT));
                    Device.writeTPK(FinancialApplication.getConvert().strToBcd(hardcodeKey,
                            IConvert.EPaddingPosition.PADDING_LEFT), null);
                    Device.writeTAK(FinancialApplication.getConvert().strToBcd(hardcodeKey,
                            IConvert.EPaddingPosition.PADDING_LEFT), null);
                } catch (PedDevException e) {
                    Log.e(TAG, "", e);
                }
            }

            if (preference.getTitleRes() == R.string.commParam_menu_mobile_apn_system) {
                switchAPN(preference, stringValue, new MultiPathProgressiveListener() {
                    @Override
                    public void onStart() {
                        final Activity activity = getActivity();
                        if (activity != null) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog = ProgressDialog.show(activity, "KBank", "Initializing...", true, false);
                                }
                            });
                        }
                    }

                    @Override
                    public void onError(final ERoute route, final String ipAddress) {
                        final Activity activity = getActivity();
                        if (activity != null) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (progressDialog != null) {
                                        progressDialog.setMessage(String.format("Unable to set '%1$s' to '%2$s' channel.", ipAddress, route.name()));
                                    }
                                }
                            });
                        }
                    }

                    @Override
                    public void onFinish(final boolean result) {
                        final Activity activity = getActivity();
                        if (activity != null) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.dismiss();
                                    if (!result) {
                                        activity.finishAndRemoveTask();
                                        System.exit(-1);
                                    }
                                }
                            });
                        }
                    }
                });
            }

            preference.setSummary(index >= 0 ? preference.getEntries()[index] : null);
        }
        return true;
    }

    @Override
    protected boolean onMultiSelectListPreferenceChanged(MultiSelectListPreference preference, Object value, boolean isInitLoading) {
        return false;
    }

    private void switchAPN(ListPreference preference, String newValue, MultiPathProgressiveListener listener) {
        int ret;

        if (Utils.getString(R.string.apn_none).equals(newValue)) {
            String sysValue = FinancialApplication.getSysParam().get(SysParam.StringParam.MOBILE_APN_SYSTEM);

            if (sysValue != null && sysValue.compareToIgnoreCase(Utils.getString(R.string.apn_none)) != 0) {
                ret = Utils.switchAPN(getActivity(), sysValue, 3000, 5000, listener);
            }
            else {
                ret = 1;
            }
        }
        else {
            String prefValue = preference.getEntry().toString();

            if (prefValue.compareToIgnoreCase(newValue) != 0) {
                ret = Utils.switchAPN(getActivity(), newValue, 3000, 5000, listener);
            }
            else { // Same value
                ret = 1;
            }
        }

        if (ret == 1) {//success
            FinancialApplication.getSysParam().set(SysParam.BooleanParam.NEED_SWITCH_APN, false);
        }
        else {//fail
            FinancialApplication.getSysParam().set(SysParam.BooleanParam.NEED_SWITCH_APN, true);
        }
    }

    @Override
    public void onResume() {
        Device.enableBackKey(false);
        super.onResume();
    }
}
