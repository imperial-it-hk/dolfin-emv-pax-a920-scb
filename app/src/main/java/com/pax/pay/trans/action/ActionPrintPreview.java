/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-1-6
 * Module Author: lixc
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.trans.action;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import com.pax.pay.trans.model.ETransType;
import com.pax.abl.core.AAction;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.action.activity.PrintPreviewActivity;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.receipt.PrintListenerImpl;
import com.pax.pay.trans.receipt.ReceiptPreviewTrans;
import com.pax.pay.trans.receipt.ReceiptPrintInstalmentKbankTrans;

import java.io.ByteArrayOutputStream;

public class ActionPrintPreview extends AAction {

    private Context context;
    private TransData transData;

    private Bitmap loadedBitmap = null;

    public ActionPrintPreview(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, TransData transData) {
        this.context = context;
        this.transData = transData;
    }

    @Override
    protected void process() {
        FinancialApplication.getApp().runInBackground(new PrintPreviewRunnable());
    }

    private class PrintPreviewRunnable implements Runnable {
        @Override
        public void run() {
            genReceipt();
            //bitmap to byte data, then transfer data to PrintPreviewActivity
            Intent intent = new Intent(context, PrintPreviewActivity.class);
            ByteArrayOutputStream bitmapData = new ByteArrayOutputStream();
            loadedBitmap.compress(Bitmap.CompressFormat.PNG, 100, bitmapData);
            byte[] bitmapByte = bitmapData.toByteArray();
            intent.putExtra(EUIParamKeys.BITMAP.toString(), bitmapByte);
            context.startActivity(intent);
        }

        private void genReceipt() {
            if (loadedBitmap == null) {
                //generate bitmap image of send preview
                ReceiptPreviewTrans receiptPreviewTrans = new ReceiptPreviewTrans();
                PrintListenerImpl listener = new PrintListenerImpl(context);
                switch (transData.getAcquirer().getName()) {
                    case Constants.ACQ_REDEEM:
                    case Constants.ACQ_REDEEM_BDMS:
                        loadedBitmap = receiptPreviewTrans.previewRedeem(transData, listener);
                        break;
                    case Constants.ACQ_SMRTPAY:
                    case Constants.ACQ_SMRTPAY_BDMS:
                    case Constants.ACQ_DOLFIN_INSTALMENT:
                        loadedBitmap = receiptPreviewTrans.previewInstalment(transData, listener);
                        break;
                    case Constants.ACQ_AMEX_EPP:
                        loadedBitmap = receiptPreviewTrans.previewInstalmentAmex(transData, listener);
                        break;
                    case Constants.ACQ_ALIPAY:
                    case Constants.ACQ_ALIPAY_B_SCAN_C:
                    case Constants.ACQ_WECHAT:
                    case Constants.ACQ_WECHAT_B_SCAN_C:
                        if (transData.getTransType() == ETransType.QR_WECHAT_SCAN || transData.getTransType() == ETransType.QR_ALIPAY_SCAN || transData.getTransType() == ETransType.QR_VOID_ALIPAY || transData.getTransType() == ETransType.QR_VOID_WECHAT) {
                            loadedBitmap = receiptPreviewTrans.previewScanQRReceipt(transData, listener);
                        } else {
                            loadedBitmap = receiptPreviewTrans.preview(transData, listener);
                        }
                        break;
                    default:
                        loadedBitmap = receiptPreviewTrans.preview(transData, listener);
                        break;
                }
            }
        }
    }
}
