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

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * PAN calc utils
 */
public class PanUtils {
    /**
     * Pan mode algorithm
     */
    @IntDef({X9_8_WITH_PAN, X9_8_NO_PAN})
    @Retention(RetentionPolicy.SOURCE)
    public @interface EPanMode {
    }

    public static final int X9_8_WITH_PAN = 0;
    public static final int X9_8_NO_PAN = 1;

    /**
     * shift pan value
     *
     * @param pan  input pan data
     * @param mode {@link EPanMode}
     * @return shifted pan value
     */
    public static String getPanBlock(String pan, @EPanMode int mode) {
        String panBlock = null;
        if (pan == null || pan.length() < 13 || pan.length() > 19) {
            return null;
        }
        switch (mode) {
            case X9_8_WITH_PAN:
                panBlock = "0000" + pan.substring(pan.length() - 13, pan.length() - 1);
                break;
            case X9_8_NO_PAN:
                panBlock = "0000000000000000";
                break;

            default:
                break;
        }

        return panBlock;
    }

    /**
     * format card no with spaces
     *
     * @param cardNo the original card no
     * @return spaced card no
     */
    public static String separateWithSpace(String cardNo) {
        if (cardNo == null)
            return "";

        StringBuilder temp = new StringBuilder();
        int total = cardNo.length() / 4;
        for (int i = 0; i < total; i++) {
            temp.append(cardNo.substring(i * 4, i * 4 + 4));
            if (i != (total - 1)) {
                temp.append(" ");
            }
        }
        if (total * 4 < cardNo.length()) {
            temp.append(" ");
            temp.append(cardNo.substring(total * 4, cardNo.length()));
        }

        return temp.toString();
    }

    /**
     * mask card no using specific pattern
     *
     * @param cardNo  the original card no
     * @param pattern it's a regular expression, def:{@link com.pax.pay.constant.Constants#DEF_PAN_MASK_PATTERN}
     * @return masked card no
     */
    public static String maskCardNo(String cardNo, String pattern) {
        if (cardNo == null || cardNo.isEmpty() || pattern == null || pattern.isEmpty())
            return cardNo;

        return cardNo.replaceAll(pattern, "*");
    }

    public static String maskCardNo(String cardNo, String pattern, String strMask) {
        if (cardNo == null || cardNo.isEmpty() || pattern == null || pattern.isEmpty())
            return cardNo;

        return cardNo.replaceAll(pattern, strMask);
    }
}
