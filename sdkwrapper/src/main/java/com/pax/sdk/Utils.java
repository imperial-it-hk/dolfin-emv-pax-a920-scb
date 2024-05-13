/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-7-4
 * Module Author: Kim.L
 * Description:
 *
 * ============================================================================
 */
package com.pax.sdk;

import android.os.Build;

import java.util.ArrayList;
import java.util.List;

class Utils {

    private static final List<String> NO_PAX_DEVICE = new ArrayList<>();
    static {
        NO_PAX_DEVICE.add("N6F27I"); //Nexus6
    }

    private Utils() {
        //do nothing
    }

    static boolean isPaxDevice() {
        return NO_PAX_DEVICE.indexOf(Build.DISPLAY) == -1;
    }
}
