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
import th.co.bkkps.utils.Log;
import androidx.annotation.XmlRes;
import com.pax.dal.entity.ERoute;
import com.pax.edc.R;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.app.MultiPathProgressiveListener;

public class LinkPOSManageFragment extends BasePreferenceFragment {
    private ProgressDialog progressDialog;

    @Override
    @XmlRes
    protected int getResourceId() {
        return R.xml.linkpos_manage_pref;
    }

    @Override
    protected void initPreference() {
        bindPreference(SysParam.StringParam.LINKPOS_PROTOCOL);
        bindPreference(SysParam.StringParam.LINKPOS_COMM_TYPE);
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
        Log.i("LinkPOSManageFragment", value.toString());

        String stringValue = value.toString();
        int index = preference.findIndexOfValue(stringValue);
        int resId = preference.getTitleRes();
        if(resId == R.string.linkpos_protocol){
            if(FinancialApplication.getEcrProcess() != null && FinancialApplication.getEcrProcess().mProtoFilter != null){
                FinancialApplication.getEcrProcess().mProtoFilter.setProtoSelect(stringValue);
            }
        }
        preference.setSummary(index >= 0 ? preference.getEntries()[index] : null);

        return true;
    }

    @Override
    protected boolean onMultiSelectListPreferenceChanged(MultiSelectListPreference preference, Object value, boolean isInitLoading) {
        return false;
    }

}
