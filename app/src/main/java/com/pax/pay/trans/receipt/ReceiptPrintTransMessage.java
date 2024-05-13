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
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

/**
 * print receipt
 *
 * @author Steven.W
 */
public class ReceiptPrintTransMessage extends AReceiptPrint {

    private int fontSize ;
    private boolean useCustomFontSize = false;
    public int print(String[] reqMsg, String[] respMsg, PrintListener listener, int fontSize) {
        this.fontSize = fontSize;
        this.useCustomFontSize = true;
        return print(reqMsg, respMsg, listener);
    }

    public int print(String[] reqMsg, String[] respMsg, PrintListener listener) {
        this.listener = listener;
        int ret = 0;
        if (listener != null)
            listener.onShowMessage(null, Utils.getString(R.string.wait_print));


        ReceiptGeneratorTransMessage receiptGeneratorTransMessage = null ;
        if (useCustomFontSize) {
            receiptGeneratorTransMessage = new ReceiptGeneratorTransMessage(reqMsg, respMsg, fontSize);
        } else {
            receiptGeneratorTransMessage = new ReceiptGeneratorTransMessage(reqMsg, respMsg);
        }
        ret = printBitmap(receiptGeneratorTransMessage.generateBitmap());

        if (listener != null) {
            listener.onEnd();
        }

        return ret;
    }
}
