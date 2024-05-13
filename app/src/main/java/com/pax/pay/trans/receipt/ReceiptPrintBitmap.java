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
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.glwrapper.imgprocessing.IImgProcessing;
import com.pax.glwrapper.page.IPage;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.trans.component.Component;
import com.pax.pay.utils.Utils;

/**
 * print bitmap
 */
public class ReceiptPrintBitmap extends AReceiptPrint {

    public int print(String bitmapStr, PrintListener listener) {
        this.listener = listener;

        if (listener != null) {
            listener.onShowMessage(null, Utils.getString(R.string.wait_process));
        }

        // 将json传入的String转换成Bitmap
        byte[] bitmapArray;
        bitmapArray = Base64.decode(bitmapStr, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapArray, 0, bitmapArray.length);

        if (listener != null) {
            listener.onShowMessage(null, Utils.getString(R.string.wait_print));
        }

        printBitmap(bitmap);
        if (listener != null) {
            listener.onEnd();
        }
        return 0;
    }

    public int printBitmap(Bitmap bitmap, PrintListener listener) {
        this.listener = listener;

        if (listener != null) {
            listener.onShowMessage(null, Utils.getString(R.string.wait_process));
        }

        int ret = printBitmap(bitmap);
        printStr("\n\n\n\n\n\n");
        if (listener != null) {
            listener.onEnd();
        }
        return ret;
    }

    public void printAppNameVersion(PrintListener listener, boolean forceEndListener) {
        printAppNameVersion(listener, forceEndListener, false);
    }

    public void printAppNameVersion(PrintListener listener, boolean forceEndListener, boolean SkipPrintFlag) {
        this.listener = listener;

        IPage page = Device.generatePage(true);
        Component.genAppVersiononSlip(page);

        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        printBitmap(imgProcessing.pageToBitmap(page, 384), SkipPrintFlag);

        if (forceEndListener && listener != null) {
            listener.onEnd();
        }
    }

}
