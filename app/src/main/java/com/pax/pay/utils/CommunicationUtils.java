/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-11-27
 * Module Author: Steven.W
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.utils;

import android.content.Context;
import android.telephony.TelephonyManager;

import com.pax.pay.app.FinancialApplication;

import java.util.HashMap;
import java.util.Map;

import th.co.bkkps.utils.Log;

public class CommunicationUtils {

    private static Map<String, String> carrierDisplayName = new HashMap<>();

    static {
        carrierDisplayName.put("52003", "AIS");
        carrierDisplayName.put("52015", "AIS-T");
        carrierDisplayName.put("52018", "dtac");
        carrierDisplayName.put("52005", "dtac");
        carrierDisplayName.put("52047", "dtac-T");
    }

    private CommunicationUtils() {
        //do nothing
    }

    public static int getSimState(){
        TelephonyManager telMgr = (TelephonyManager) FinancialApplication.getApp().getSystemService(Context.TELEPHONY_SERVICE);
        return telMgr.getSimState();
    }

    public static String getCarrierName() {
        TelephonyManager manager = (TelephonyManager) FinancialApplication.getApp().getSystemService(Context.TELEPHONY_SERVICE);
        if (manager.getSimState() == TelephonyManager.SIM_STATE_READY) {
            return mappingCarrierName(manager.getNetworkOperatorName());
        }
        return "";
    }

    private static String mappingCarrierName(String carrier) {
        try {
            String display = carrierDisplayName.get(carrier);
            if (display == null) {
                throw new Exception("");
            }
            return display;
        } catch (Exception e) {
            Log.w("CommunicationUtils", "No matching carrier display name [getNetworkOperatorName=" + (carrier != null ? carrier : "null") + "]");
        }
        return carrier;
    }

}
