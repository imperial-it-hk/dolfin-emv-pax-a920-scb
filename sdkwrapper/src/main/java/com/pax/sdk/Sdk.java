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

import android.content.Context;
import android.util.Log;

import com.pax.dal.IDAL;
import com.pax.linhb.nativetouchevent.NativeTouchEvent;
import com.pax.neptunelite.api.NeptuneLiteUser;

public class Sdk {
    private static final String TAG = "SDK";
    private static Sdk instance = null;
    private IDAL dal;

    public static class TouchEvent extends NativeTouchEvent{

    }

    private TouchEvent touchEvent = new TouchEvent();

    // TODO: KiTty we can improve this
    private Sdk() {
    }

    public static Sdk getInstance() {
        if (instance == null) {
            instance = new Sdk();
        }
        return instance;
    }

    public IDAL getDal(Context context) {
        if (Utils.isPaxDevice()) {
            Log.i(TAG, "before NeptuneUser");
            try {
                dal = NeptuneLiteUser.getInstance().getDal(context);
            } catch (Exception e) {
                Log.w(TAG, e);
            }
            Log.i(TAG, "after NeptuneUser");
        }

        return dal;
    }

    public TouchEvent getTouchEvent(){
        return touchEvent;
    }

    public static boolean isPaxDevice() {
        return Utils.isPaxDevice();
    }
}
