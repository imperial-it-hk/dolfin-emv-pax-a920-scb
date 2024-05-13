/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-11-25
 * Module Author: Steven.W
 * Description:
 *
 * ============================================================================
 */
package com.pax.device;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.pax.pay.app.FinancialApplication;

/**
 * general param, share preferences
 *
 * @author Steven.W
 */
public class GeneralParam {
    /**
     * PIN key
     */
    public static final String TPK = "TPK";
    /**
     * MAC key
     */
    public static final String TAK = "TAK";
    /**
     * DES key
     */
    public static final String TDK = "TDK";

    private static final String CONFIG_FILE_NAME = "generalParam";

    public GeneralParam() {
        //do nothing
    }

    public String get(String key) {
        String value;
        SharedPreferences sharedPreferences = FinancialApplication.getApp().getSharedPreferences(CONFIG_FILE_NAME, Context.MODE_PRIVATE);
        value = sharedPreferences.getString(key, null);
        return value;
    }

    public void set(String key, String value) {
        SharedPreferences sharedPreferences = FinancialApplication.getApp().getSharedPreferences(CONFIG_FILE_NAME, Context.MODE_PRIVATE);
        Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

}
