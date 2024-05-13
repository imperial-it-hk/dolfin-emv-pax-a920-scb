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
import android.os.SystemClock;
import th.co.bkkps.utils.Log;
import android.util.SparseIntArray;

import com.pax.dal.IPrinter;
import com.pax.dal.exceptions.PrinterDevException;
import com.pax.edc.R;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.util.List;

abstract class AReceiptPrint {

    protected static final String TAG = "ReceiptPrint";

    protected PrintListener listener;

    private static final SparseIntArray RESULT_MAP = new SparseIntArray();
    protected static final SparseIntArray ERR_PRINT_MAP = new SparseIntArray();

    static {
        RESULT_MAP.put(2, R.string.err_print_paper);
        RESULT_MAP.put(8, R.string.err_print_hot);
        RESULT_MAP.put(9, R.string.err_print_voltage);

        ERR_PRINT_MAP.put(1, R.string.err_print_busy);
        ERR_PRINT_MAP.put(3, R.string.err_print_data_packet);
        ERR_PRINT_MAP.put(4, R.string.err_print_malfunctions);
        ERR_PRINT_MAP.put(-16, R.string.err_print_unfinished);
        ERR_PRINT_MAP.put(-4, R.string.err_print_no_font_lib);
        ERR_PRINT_MAP.put(-2, R.string.err_print_data_packet_long);
    }

    /**
     * return -1 stop print
     *
     * @param bitmap
     * @return
     */
    protected synchronized int printBitmap(Bitmap bitmap) {
        return printBitmap(bitmap, false);
    }
    protected synchronized int printBitmap(Bitmap bitmap, boolean skipPrint) {
        if (skipPrint) {return -1;}
        IPrinter printer = FinancialApplication.getDal().getPrinter();
        try {
            printer.init();
            printer.printBitmap(bitmap);

            return start(printer);

        } catch (PrinterDevException e) {
            Log.e(TAG, "", e);
        }

        return -1;
    }
    protected synchronized int printBitmap(List<Bitmap> bitmap) {
        return printBitmap(bitmap, false);
    }
    protected synchronized int printBitmap(List<Bitmap> bitmap, boolean skipPrint) {
        if (skipPrint) {return -1;}
        IPrinter printer = FinancialApplication.getDal().getPrinter();
        int ret = 0;
        try {
            for (Bitmap i : bitmap) {
                printer.init();
                printer.printBitmap(i);

                ret = start(printer);
                if (ret != 0)
                    return ret;
            }
            return ret;
        } catch (PrinterDevException e) {
            Log.e(TAG, "", e);
        }

        return -1;
    }
    protected synchronized int printStr(String str) {
        return printStr(str, false);
    }
    protected synchronized int printStr(String str, boolean skipPrint) {
        if (skipPrint) { return 0; }
        IPrinter printer = FinancialApplication.getDal().getPrinter();
        try {
            printer.init();
            printer.printStr(str, null);
            return start(printer);

        } catch (PrinterDevException e) {
            Log.e(TAG, "", e);
        }

        return -1;
    }

    private int start(IPrinter printer) throws PrinterDevException {
        boolean isContinue = true;
        while (isContinue) {
            if (listener != null)
                listener.onShowMessage(null, Utils.getString(R.string.wait_print));
            printer.setGray(250);
            int ret = printer.start();

            // AET-263
            if (ret == 0 || ret == -4) // workaround: since now it just prints logo, and some new version of framework which may not have font on SP, so ignore -4
                return 0;
            else if (ret == 1) { // printer busy please wait
                SystemClock.sleep(100);
            } else {
                int resId = RESULT_MAP.get(ret, -1);
                if (resId == -1) {
                    return ret;
                }
                isContinue = isContinueWhenFailed(resId);
            }
        }
        return -1;
    }

    private boolean isContinueWhenFailed(int ret) {
        boolean KioskModeEnable = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_KIOSK_MODE);
        int timeout;
        if(!KioskModeEnable) {
            timeout = (ret == 2) ? 0 : 30;
        } else {
            timeout = 30;
        }
        if (ret != -1 && listener != null) {
            PrintListener.Status result = listener.onConfirm(null, Utils.getString(ret),timeout);
            if (result == PrintListener.Status.CONTINUE) {
                return true;
            }
        }
        return false;
    }

}
