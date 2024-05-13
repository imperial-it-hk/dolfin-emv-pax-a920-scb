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

/*
Called from DbUpgrader by reflection
 */
public class Upgrade1To2 extends SpUpgrader {

    @Override
    public void upgrade(SharedPreferences.Editor editor) {
        editor.putString("TEST", "sd"); // FIXME should be SysParam Tag
    }
}
