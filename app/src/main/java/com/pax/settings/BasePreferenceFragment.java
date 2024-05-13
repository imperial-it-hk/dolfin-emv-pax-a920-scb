/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-5-22
 * Module Author: Kim.L
 * Description:
 *
 * ============================================================================
 */
package com.pax.settings;

import android.os.Bundle;
import android.preference.*;
import androidx.annotation.XmlRes;

public abstract class BasePreferenceFragment extends PreferenceFragment {
    protected static final String TAG = "   PreferenceFragment";

    private boolean isFirst = true;

    private Preference.OnPreferenceChangeListener listener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            if (preference instanceof ListPreference) {
                return onListPreferenceChanged((ListPreference) preference, value, isFirst);
            } else if (preference instanceof CheckBoxPreference) {
                return onCheckBoxPreferenceChanged((CheckBoxPreference) preference, value, isFirst);
            } else if (preference instanceof EditTextPreference) {
                return onEditTextPreferenceChanged((EditTextPreference) preference, value, isFirst);
            } else if (preference instanceof RingtonePreference) {
                return onRingtonePreferenceChanged((RingtonePreference) preference, value, isFirst);
            } else if (preference instanceof MultiSelectListPreference) {
                return onMultiSelectListPreferenceChanged((MultiSelectListPreference) preference, value, isFirst);
            } else {
                String stringValue = value.toString();
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(getResourceId());
        isFirst = true;
        initPreference();
        isFirst = false;
    }

    protected void bindPreference(SysParam.NumberParam key) {
        bindPreference(key.toString());
    }

    protected void bindPreference(SysParam.StringParam key) {
        bindPreference(key.toString());
    }

    protected void bindPreference(SysParam.BooleanParam key) {
        bindPreference(key.toString());
    }

    protected void bindListPreference(SysParam.StringParam key, final CharSequence[] entries,
                                      final CharSequence[] entryValues) {
        if (entries == null || entryValues == null) {
            bindPreference(key.toString());
        } else {
            bindPreference(key.toString(), entries, entryValues);
        }
    }

    protected void bindPreference(Preference preference) {
        preference.setOnPreferenceChangeListener(listener);
        if (preference instanceof CheckBoxPreference) {
            listener.onPreferenceChange(preference, PreferenceManager
                    .getDefaultSharedPreferences(preference.getContext()).getBoolean(preference.getKey(), false));
        } else if (!(preference instanceof MultiSelectListPreference)) {
            listener.onPreferenceChange(preference, PreferenceManager
                    .getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
        }
    }

    private void bindPreference(String key) {
        Preference preference = super.findPreference(key);
        preference.setOnPreferenceChangeListener(listener);
        if (preference instanceof CheckBoxPreference) {
            listener.onPreferenceChange(preference, PreferenceManager
                    .getDefaultSharedPreferences(preference.getContext()).getBoolean(preference.getKey(), false));
        } else if (!(preference instanceof MultiSelectListPreference)) {
            listener.onPreferenceChange(preference, PreferenceManager
                    .getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
        }
    }

    private void bindPreference(String key, final CharSequence[] entries,
                                final CharSequence[] entryValues) {
        Preference preference = super.findPreference(key);
        preference.setOnPreferenceChangeListener(listener);
        if (preference instanceof ListPreference) {
            ((ListPreference) preference).setEntries(entries);
            ((ListPreference) preference).setEntryValues(entryValues);
            listener.onPreferenceChange(preference, PreferenceManager
                    .getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
        }
    }

    @XmlRes
    protected abstract int getResourceId();

    protected abstract void initPreference();

    protected abstract boolean onListPreferenceChanged(ListPreference preference, Object value, boolean isInitLoading);

    protected abstract boolean onCheckBoxPreferenceChanged(CheckBoxPreference preference, Object value, boolean isInitLoading);

    protected abstract boolean onRingtonePreferenceChanged(RingtonePreference preference, Object value, boolean isInitLoading);

    protected abstract boolean onEditTextPreferenceChanged(EditTextPreference preference, Object value, boolean isInitLoading);

    protected abstract boolean onMultiSelectListPreferenceChanged(MultiSelectListPreference preference, Object value, boolean isInitLoading);
}
