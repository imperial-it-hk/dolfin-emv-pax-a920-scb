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
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

/**
 * print receipt show DCC rate
 *
 */
public class ReceiptPrintDccRate extends AReceiptPrint {

    public int print(String localAmount,String rate, String dccAmount,String localCurrency,TransData transData, PrintListener listener,String markUp) {

        this.listener = listener;
        int ret = 0;
        if (listener != null)
            listener.onShowMessage(null, Utils.getString(R.string.wait_print));


        if(Constants.isTOPS){
            ReceiptGeneratorDccRateTOPS receiptGeneratorDccRateTops = new ReceiptGeneratorDccRateTOPS(transData,localAmount,rate,dccAmount,localCurrency,markUp);
            ret = printBitmap(receiptGeneratorDccRateTops.generateBitmap());
        }else{
            ReceiptGeneratorDccRate receiptGeneratorDccRate = new ReceiptGeneratorDccRate(transData,localAmount,rate,dccAmount,localCurrency);
            ret = printBitmap(receiptGeneratorDccRate.generateBitmap());
        }


        if (listener != null) {
            listener.onEnd();
        }

        return ret;
    }

}
