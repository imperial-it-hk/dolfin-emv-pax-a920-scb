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

import androidx.annotation.IntDef;
import com.pax.edc.R;
import com.pax.pay.utils.Utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * print receipt
 *
 * @author Steven.W
 */
public class ReceiptPrintParam extends AReceiptPrint {

    @IntDef({AID, CAPK, CARD_RANGE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {
    }

    public static final int AID = 1;
    public static final int CAPK = 2;
    public static final int CARD_RANGE = 3;

    public int print(@Type int type, PrintListener listener) {
        this.listener = listener;
        if (listener != null)
            listener.onShowMessage(null, Utils.getString(R.string.wait_print));

        ReceiptGeneratorParam receiptGeneratorParam;
        switch (type) {
            case AID:
                receiptGeneratorParam = new ReceiptGeneratorAidParam();
                break;
            case CAPK:
                receiptGeneratorParam = new ReceiptGeneratorCapkParam();
                break;
            case CARD_RANGE:
                receiptGeneratorParam = new ReceiptGeneratorCardRangeList();
                break;
            default:
                return -1;
        }

        int ret = printBitmap(receiptGeneratorParam.generateBitmaps());
        if (listener != null) {
            listener.onEnd();
        }
        return ret;
    }

}
