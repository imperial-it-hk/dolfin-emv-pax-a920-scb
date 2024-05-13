/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-9-18
 * Module Author: Kim.L
 * Description:
 *
 * ============================================================================
 */
package com.pax.edc.opensdk;

import android.os.Bundle;
import android.util.Log;

/**
 * Package-private, hide from Javadoc
 */
class IntentUtil {

    private static final String TAG = "IntentUtil";

    private IntentUtil(){
        //do nothing
    }

    static String getStringExtra(Bundle bundle, String key){
        String str = null;
        if(bundle != null) {
            try {
                str = bundle.getString(key);
            } catch (Exception e) {
                Log.e(TAG, "getStringExtra exception:" + e.getMessage());
            }
        }
        return str;
    }

    static int getIntExtra(Bundle bundle, String key){
        int value = -1;
        if(bundle != null) {
            try {
                value = bundle.getInt(key, -1);
            } catch (Exception e) {
                Log.e(TAG, "getIntExtra exception:" + e.getMessage());
            }
        }
        return value;
    }

    static long getLongExtra(Bundle bundle, String key){
        long value = -1L;
        if(bundle != null) {
            try {
                value = bundle.getLong(key, -1L);
            } catch (Exception e) {
                Log.e(TAG, "getLongExtra exception:" + e.getMessage());
            }
        }
        return value;
    }

    static byte[] getByteArrayExtra(Bundle bundle, String key){
        byte[] value = null;
        if(bundle != null) {
            try {
                value = bundle.getByteArray(key);
            } catch (Exception e) {
                Log.e(TAG, "getByteArrayExtra exception:" + e.getMessage());
            }
        }
        return value;
    }
}
