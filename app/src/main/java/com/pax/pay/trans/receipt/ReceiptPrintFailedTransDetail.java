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

import com.pax.edc.R;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.Utils;

import java.util.List;

/**
 * print failed transaction detail
 *
 * @author Steven.W
 */
public class ReceiptPrintFailedTransDetail extends AReceiptPrint {

    public int print(String title, List<TransData> failedTransList, PrintListener listener) {
        this.listener = listener;

        if (listener != null)
            listener.onShowMessage(null, Utils.getString(R.string.wait_print));
        ReceiptGeneratorFailedTransDetail receiptGeneratorFailedTransDetail = new ReceiptGeneratorFailedTransDetail(
                title, failedTransList);
        int ret = printBitmap(receiptGeneratorFailedTransDetail.generateBitmap());
        if (listener != null) {
            listener.onEnd();
        }

        //AET-71
        if (ret == 0) {
            printStr("\n\n\n\n\n\n\n\n\n\n");
        }
        return ret;
    }
}
