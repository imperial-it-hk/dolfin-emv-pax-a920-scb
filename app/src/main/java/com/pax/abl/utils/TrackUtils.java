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
package com.pax.abl.utils;

import androidx.annotation.NonNull;
import com.pax.pay.utils.Utils;

/**
 * track utils
 */
public class TrackUtils {

    private TrackUtils() {

    }

    /**
     * @param tag57 emv tag57
     * @return track2
     */
    public static String getTrack2FromTag57(@NonNull byte[] tag57, boolean isSplitF) {
        String strTrack2 = Utils.bcd2Str(tag57);
        if (!isSplitF) {
            return strTrack2;
        }
        return strTrack2.split("F")[0];
    }

    /**
     * get account from track2
     *
     * @param track2 input track2 data
     * @return account no
     */
    public static String getPan(String track2) {
        if (track2 == null)
            return null;

        int len = track2.indexOf('=');
        if (len < 0) {
            len = track2.indexOf('D');
            if (len < 0)
                return null;
        }

        if ((len < 13) || (len > 19))
            return null;
        return track2.substring(0, len);
    }

    /**
     * get service code from track2
     *
     * @param track2 input track2 data
     * @return service code
     */
    public static String getServiceCode(String track2) {
        int idx = track2.indexOf('=');
        if (idx == -1) {
            return null;
        } else {
            return track2.substring(idx + 5, idx + 8);
        }
    }

    /**
     * check if it's IC card from track2
     *
     * @param track2 input track2 data
     * @return true/false
     */
    public static boolean isIcCard(String track2) {
        if (track2 == null)
            return false;

        int index = track2.indexOf('=');
        if (index < 0) {
            index = track2.indexOf('D');
            if (index < 0)
                return false;
        }

        if (index + 6 > track2.length())
            return false;

        return "2".equals(track2.substring(index + 5, index + 6)) || "6".equals(track2.substring(index + 5, index + 6));
    }

    /**
     * get expiry data from track2
     *
     * @param track2 input track2 data
     * @return expiry data
     */
    public static String getExpDate(String track2) {
        if (track2 == null)
            return null;

        int index = track2.indexOf('=');
        if (index < 0) {
            index = track2.indexOf('D');
            if (index < 0)
                return null;
        }

        if (index + 5 > track2.length())
            return null;
        return track2.substring(index + 1, index + 5);
    }

    /**
     * get card holder name from track1
     *
     * @param track1 input track1 data
     * @return card holder name
     */
    public static String getHolderName(String track1) {
        if (track1 == null) {
            return null;
        }

        int index1 = track1.indexOf('^');
        if (index1 < 0) {
            return null;
        }

        int index2 = track1.lastIndexOf('^');
        if (index2 < 0) {
            return null;
        }

        return track1.substring(index1 + 1, index2);
    }
}
