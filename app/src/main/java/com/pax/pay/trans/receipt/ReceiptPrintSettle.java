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

import com.pax.edc.R;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.model.TransTotal;
import com.pax.pay.utils.Utils;

/**
 * print settle
 *
 * @author Steven.W
 */
public class ReceiptPrintSettle extends AReceiptPrint {
    public int print(String title, String result, TransTotal transTotal, boolean settleFail, String errorCode, PrintListener listener) {
        return print(title, result, transTotal, settleFail, errorCode, listener, false);
    }

    public int print(String title, String result, TransTotal transTotal, boolean settleFail, String errorCode, PrintListener listener, boolean SkipPrintFlag) {
        this.listener = listener;
        if (listener != null) {
            listener.onShowMessage(null, Utils.getString(R.string.wait_print));
        }
        int ret;
        if (settleFail) {
            ReceiptGeneratorTotal receiptGenSettleFail = new ReceiptGeneratorTotal(title, transTotal, errorCode);
            ret = printBitmap(receiptGenSettleFail.generateBitmapSettleFail(), SkipPrintFlag);
        } else {
            String acquirerName = transTotal.getAcquirer() != null ? transTotal.getAcquirer().getName() : "";
            if (Constants.ACQ_WALLET.equals(acquirerName)) {
                ReceiptGeneratorTotalDetailWallet receiptGeneratorTotalDetailWallet = new ReceiptGeneratorTotalDetailWallet(transTotal.getAcquirer(), transTotal, false);
                ret = printBitmap(receiptGeneratorTotalDetailWallet.generateBitmap(), SkipPrintFlag);
            } else {
                // AET-108
                boolean isRedeem = Constants.ACQ_REDEEM.equals(acquirerName) || Constants.ACQ_REDEEM_BDMS.equals(acquirerName);
                if (Constants.isTOPS) {
                    ReceiptGeneratorTotalTOPS receiptGeneratorSettleTOPS = new ReceiptGeneratorTotalTOPS(title, result, transTotal, 0, false);
                    if (isRedeem) {
                        ret = printBitmap(receiptGeneratorSettleTOPS.generateBitmapRedeemKbank(), SkipPrintFlag);
                    } else {
                        ret = printBitmap(receiptGeneratorSettleTOPS.generateBitmap(), SkipPrintFlag);
                    }
                } else {
                    ReceiptGeneratorTotal receiptGeneratorSettle = new ReceiptGeneratorTotal(title, result, transTotal, 0, false);
                    if (isRedeem) {
                        ret = printBitmap(receiptGeneratorSettle.generateBitmapRedeemKbank(), SkipPrintFlag);
                    } else {
                        ret = printBitmap(receiptGeneratorSettle.generateBitmap(), SkipPrintFlag);
                    }
                }
            }
        }

        if (listener != null) {
            listener.onEnd();
        }

        return ret;
    }

    public void printGrandTotal(TransTotal transTotal, PrintListener listener) {
        printGrandTotal(transTotal, listener, false);
    }

    public void printGrandTotal(TransTotal transTotal, PrintListener listener, boolean SkipPrintFlag) {
        this.listener = listener;
        ReceiptGeneratorTotal receiptGeneratorTotal = new ReceiptGeneratorTotal(null, null, null, 0, false);
        Bitmap bitmap = receiptGeneratorTotal.generateGrandTotal(transTotal);
        if (bitmap != null) {
            printBitmap(bitmap, SkipPrintFlag);
        }
        if (listener != null) {
            listener.onEnd();
        }
    }

}
