/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-4-19
 * Module Author: Kim.L
 * Description:
 *
 * ============================================================================
 */
package com.pax.settings.spupgrader;

import android.content.SharedPreferences;
import th.co.bkkps.utils.Log;

public abstract class SpUpgrader {

    private static final String TAG = "SP Upgrader";

    public static void upgrade(SharedPreferences.Editor editor, int oldVersion, int newVersion, String packagePath) {
        try {
            Class<?> c1 = Class.forName(packagePath + ".Upgrade" + oldVersion + "To" + newVersion);
            SpUpgrader upgrader = (SpUpgrader) c1.newInstance();
            Log.i("SpUpgrader", "upgrading from version(" + oldVersion + ") to version(" + newVersion + ")");
            upgrader.upgrade(editor);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            Log.w(TAG, "", e);
            throw new IllegalArgumentException("No Upgrader for SP" +
                    " from version(" + oldVersion +
                    ") to version(" + newVersion + ")");
        }
    }

    public abstract void upgrade(SharedPreferences.Editor editor);
}
