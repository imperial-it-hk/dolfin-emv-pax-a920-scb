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
import android.widget.ImageView;

import com.pax.edc.R;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.Utils;

import th.co.bkkps.utils.Log;

/**
 * generate bitmap image of print preview
 */

public class ReceiptPreviewTrans {

    public Bitmap preview(TransData transData, PrintListener listener) {

        if (listener != null)
            listener.onShowMessage(null, Utils.getString(R.string.wait_receipt_generate));

        ReceiptGeneratorTrans receiptGeneratorTrans = new ReceiptGeneratorTrans(transData, 0, 1, false, true);
        Bitmap bitmap;
        if(Constants.ACQ_MY_PROMPT.equals(transData.getAcquirer().getName())){
            bitmap = receiptGeneratorTrans.generateKbankMyPromptReceiptBitmap();
        } else if (transData.getTransType() == ETransType.BPS_QR_SALE_INQUIRY
                || transData.getTransType() == ETransType.BPS_QR_INQUIRY_ID
                || transData.getTransType() == ETransType.PROMPTPAY_VOID) {//Modified by Cz to check transType and generate bitmap for PromptPay.
            bitmap = receiptGeneratorTrans.generatePromptPayReceiptBitmap();
        } else if (transData.getTransType() == ETransType.QR_SALE_WALLET ||
                transData.getTransType() == ETransType.REFUND_WALLET ||
                transData.getTransType() == ETransType.SALE_WALLET) {
            bitmap = receiptGeneratorTrans.generateWalletReceiptBitmap();
        } else if (Constants.ACQ_QRC.equals(transData.getAcquirer().getName())) {
            bitmap = receiptGeneratorTrans.generatePromptPayAllinOneReceiptBitmap();
        } else if (Constants.ACQ_KPLUS.equals(transData.getAcquirer().getName())
                || Constants.ACQ_WECHAT.equals(transData.getAcquirer().getName())
                || Constants.ACQ_WECHAT_B_SCAN_C.equals(transData.getAcquirer().getName())
                || Constants.ACQ_ALIPAY.equals(transData.getAcquirer().getName())
                || Constants.ACQ_ALIPAY_B_SCAN_C.equals(transData.getAcquirer().getName())
                || Constants.ACQ_QR_CREDIT.equals(transData.getAcquirer().getName())) {
            bitmap = receiptGeneratorTrans.generateKbankWalletReceiptBitmap();
        } else {
            bitmap = receiptGeneratorTrans.generateBitmap();
        }

        if (listener != null) {
            listener.onEnd();
        }

        return bitmap;
    }

    public Bitmap previewRedeem(TransData transData, PrintListener listener) {
        if (listener != null)
            listener.onShowMessage(null, Utils.getString(R.string.wait_receipt_generate));

        ReceiptGeneratorRedeemedTrans preview = new ReceiptGeneratorRedeemedTrans(transData, 0, 1, false, true);
        Bitmap bitmap = preview.generateBitmap();

        if (listener != null) {
            listener.onEnd();
        }

        return bitmap;
    }

    public Bitmap previewInstalment(TransData transData, PrintListener listener) {
        if (listener != null)
            listener.onShowMessage(null, Utils.getString(R.string.wait_receipt_generate));

        ReceiptGeneratorInstalmentKbankTrans preview = new ReceiptGeneratorInstalmentKbankTrans(transData, 0, 1, false, true);
        Bitmap bitmap = preview.generateBitmap();

        if (listener != null) {
            listener.onEnd();
        }

        return bitmap;
    }

    public Bitmap previewParam(PrintListener listener) {

        Bitmap bitmap;

        if (listener != null)
            listener.onShowMessage(null, Utils.getString(R.string.wait_receipt_generate));

        ReceiptGeneratorParam receiptGeneratorParam = new ReceiptGeneratorSystemParam();
        bitmap = receiptGeneratorParam.generateBitmap();

        Log.d("BITMAP", "WxH = " + bitmap.getWidth() + "x" + bitmap.getHeight());
        if(bitmap.getHeight() > 4096) {
            Bitmap tempBitmap  = bitmap;
            Float fRatio_high = Math.abs(1f - (bitmap.getHeight()/4096f));
            int newH = (int) (4096);
            int newW = (int) (bitmap.getWidth() *(1f - fRatio_high));
            bitmap = Bitmap.createScaledBitmap(tempBitmap, newW, newH, true);
        }
        Log.d("BITMAP", "WxH = " + bitmap.getWidth() + "x" + bitmap.getHeight());

        if (listener != null) {
            listener.onEnd();
        }

        return bitmap;
    }

    public Bitmap previewInstalmentAmex(TransData transData, PrintListener listener) {
        if (listener != null)
            listener.onShowMessage(null, Utils.getString(R.string.wait_receipt_generate));

        ReceiptGeneratorInstalmentAmexTrans preview = new ReceiptGeneratorInstalmentAmexTrans(transData, 0, 1, false, true);
        Bitmap bitmap = preview.generateBitmap();

        if (listener != null) {
            listener.onEnd();
        }

        return bitmap;
    }

    public Bitmap previewScanQRReceipt(TransData transData, PrintListenerImpl listener) {
        if (listener != null)
            listener.onShowMessage(null, Utils.getString(R.string.wait_receipt_generate));

        ReceiptGeneratorScanqrTrans preview = new ReceiptGeneratorScanqrTrans(transData, 0, 1, false, true);
        Bitmap bitmap = preview.generateBitmap();

        if (listener != null) {
            listener.onEnd();
        }

        return bitmap;
    }
}
