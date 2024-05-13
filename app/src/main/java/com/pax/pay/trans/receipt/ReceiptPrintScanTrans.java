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
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import th.co.bkkps.utils.Log;

/**
 * @author minson
 * @date 2022.04.12
 */
public class ReceiptPrintScanTrans extends AReceiptPrint {

    protected boolean isReprint;
    protected int receiptNo;

    public int print(TransData transData, boolean isRePrint, PrintListener listener) {
        if (!transData.getIssuer().isAllowPrint())
            return 0;
        this.listener = listener;
        this.isReprint = isRePrint;
        int ret = 0;
        String acquirerName = transData.getAcquirer() != null ? transData.getAcquirer().getName() : "";
        int receiptNum = getVoucherNum(acquirerName, transData);
        if (receiptNum > 0) {
            if (listener != null)
                listener.onShowMessage(null, Utils.getString(R.string.wait_print));

            receiptNo = 0;
            receiptNum = handleVoucherNum(transData, receiptNum);

            for (; receiptNo < receiptNum; receiptNo++) {
                ret = printBitmap(generateBitmap(transData, receiptNo, receiptNum, acquirerName));
                if (ret == -1) {
                    break;
                }
                if (receiptNum > 1 && receiptNum - 1 != receiptNo) {
                    PrintListener.Status result = null;
                    if (listener != null) {
                        result = listener.onPrintNext(Utils.getString(R.string.receipt_dlg_title), Utils.getString(R.string.receipt_dlg_body));
                    }
                    if (result == PrintListener.Status.CANCEL) {
                        break;
                    }
                }
            }
        }
        if (listener != null) {
            listener.onEnd();
        }

        return ret;
    }

    protected Bitmap generateBitmap(TransData transData, int currentReceiptNo, int receiptNum, String acquirerName) {
        ReceiptGeneratorScanqrTrans preview = new ReceiptGeneratorScanqrTrans(transData, currentReceiptNo, receiptNum, isReprint, false);
        return preview.generateBitmap();
    }


    protected int getVoucherNum(String acquirerName, TransData transData) {
        int receiptNum = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_RECEIPT_NUM);
        if (receiptNum < 1 || receiptNum > 3) // receipt copy number is 1-3
            receiptNum = 2;

        if (isReprint) {
            receiptNum = 2;
        }

//        if (Constants.ACQ_KPLUS.equals(acquirerName) || Constants.ACQ_WECHAT.equals(acquirerName) || Constants.ACQ_ALIPAY.equals(acquirerName) || Constants.ACQ_QR_CREDIT.equals(acquirerName)) {
//            receiptNum = 1;
//        } else if (!Constants.ACQ_QR_PROMPT.equals(acquirerName) && !Constants.ACQ_WALLET.equals(acquirerName) && transData.isTxnSmallAmt()) {
//            //EDCBBLAND-426 Support small amount
////            receiptNum = isReprint && transData.getNumSlipSmallAmt() == 0 ? 2 : transData.getNumSlipSmallAmt();
//            receiptNum = isReprint ? 2 : transData.getNumSlipSmallAmt();
//        }

        Log.d(TAG, "ReceiptPrintInstalmentKbankTrans ---- NumbOfReceipt = " + receiptNum);
        return receiptNum;
    }

    protected int handleVoucherNum(TransData transData, int receiptNum) {
        // for EDC config receipt num == 3 (print only customer copy)
        if (receiptNum == 3) {
            receiptNo = 1; //print only customer copy
            return 2; //return receiptNum = 2
        }
        return receiptNum;
    }

}

