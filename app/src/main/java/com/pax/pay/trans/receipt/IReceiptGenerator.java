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
package com.pax.pay.trans.receipt;

import android.graphics.Bitmap;

/**
 * receipt generator
 *
 * @author Steven.W
 */
public interface IReceiptGenerator {

    String TAG = "ReceiptGenerator";

    int FONT_BIG = 30;
    int FONT_BIG_28 = 28;
    int FONT_NORMAL_26 = 26;
    int FONT_NORMAL = 24;
    int FONT_NORMAL_22 = 22;
    int FONT_SMALL = 20;
    int FONT_SMALL_18 = 18;
    int FONT_SMALL_19 = 19;
    int FONT_SMALL_16 = 16;

    /**
     * generate receipt
     *
     * @return
     */
    Bitmap generateBitmap();

    /**
     * generate simplified receipt string
     */
    String generateString();
}
