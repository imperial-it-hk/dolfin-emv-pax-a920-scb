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
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import th.co.bkkps.utils.Log;

/**
 * print receipt
 *
 * @author Steven.W
 */
public class ReceiptPrintTrans extends AReceiptPrint {

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
        if (!Constants.isTOPS) {
            ReceiptGeneratorTrans receiptGeneratorTrans = new ReceiptGeneratorTrans(transData, currentReceiptNo, receiptNum, isReprint);
            if(Constants.ACQ_MY_PROMPT.equals(transData.getAcquirer().getName())){
                return receiptGeneratorTrans.generateKbankMyPromptReceiptBitmap();
            } else if (transData.getTransType() == ETransType.BPS_QR_SALE_INQUIRY || transData.getTransType() == ETransType.BPS_QR_INQUIRY_ID
                    || transData.getTransType() == ETransType.PROMPTPAY_VOID) {
                return receiptGeneratorTrans.generatePromptPayReceiptBitmap();
            } else if (transData.getTransType() == ETransType.QR_SALE_WALLET ||
                    transData.getTransType() == ETransType.REFUND_WALLET ||
                    transData.getTransType() == ETransType.SALE_WALLET) {
                return receiptGeneratorTrans.generateWalletReceiptBitmap();
            } else if (Constants.ACQ_QRC.equals(acquirerName)) {
                return receiptGeneratorTrans.generatePromptPayAllinOneReceiptBitmap();
            } else if (Constants.ACQ_KPLUS.equals(acquirerName)
                    || Constants.ACQ_ALIPAY.equals(acquirerName)
                    || Constants.ACQ_ALIPAY_B_SCAN_C.equals(acquirerName)
                    || Constants.ACQ_WECHAT.equals(acquirerName)
                    || Constants.ACQ_WECHAT_B_SCAN_C.equals(acquirerName)
                    || Constants.ACQ_QR_CREDIT.equals(acquirerName)) {
                return receiptGeneratorTrans.generateKbankWalletReceiptBitmap();
            } else {
                return receiptGeneratorTrans.generateBitmap();
            }
        } else {
            return generateBitmapForTOPS(transData, currentReceiptNo, receiptNum, acquirerName);
        }
    }

    private Bitmap generateBitmapForTOPS(TransData transData, int currentReceiptNo, int receiptNum, String acquirerName) {
        ReceiptGeneratorTransTOPS receiptGeneratorTransTOPS = new ReceiptGeneratorTransTOPS(transData, currentReceiptNo, receiptNum, isReprint);
        if (transData.getTransType() == ETransType.BPS_QR_SALE_INQUIRY || transData.getTransType() == ETransType.BPS_QR_INQUIRY_ID
                || transData.getTransType() == ETransType.PROMPTPAY_VOID) {
            return receiptGeneratorTransTOPS.generatePromptPayReceiptBitmap();
        } else if (transData.getTransType() == ETransType.QR_SALE_WALLET ||
                transData.getTransType() == ETransType.REFUND_WALLET ||
                transData.getTransType() == ETransType.SALE_WALLET) {
            return receiptGeneratorTransTOPS.generateWalletReceiptBitmap();
        } else if (Constants.ACQ_QRC.equals(acquirerName)) {
            return receiptGeneratorTransTOPS.generatePromptPayAllinOneReceiptBitmap();
        } else if (Constants.ACQ_MY_PROMPT.equals(transData.getAcquirer().getName())) {
            return receiptGeneratorTransTOPS.generateKbankMyPromptReceiptBitmap();
        } else if (Constants.ACQ_KPLUS.equals(acquirerName)
                || Constants.ACQ_ALIPAY.equals(acquirerName)
                || Constants.ACQ_ALIPAY_B_SCAN_C.equals(acquirerName)
                || Constants.ACQ_WECHAT.equals(acquirerName)
                || Constants.ACQ_WECHAT_B_SCAN_C.equals(acquirerName)
                || Constants.ACQ_QR_CREDIT.equals(acquirerName)) {
            if (Constants.ACQ_KPLUS.equals(acquirerName) && transData.getTransType().equals(ETransType.QR_VERIFY_PAY_SLIP)) {
                return receiptGeneratorTransTOPS.generateWalletVerifyPaySlipReceiptBitmap();
            } else {
                return receiptGeneratorTransTOPS.generateKbankWalletReceiptBitmap();
            }
        } else {
            return receiptGeneratorTransTOPS.generateBitmap();
        }
    }

    protected int getVoucherNum(String acquirerName, TransData transData) {
        int receiptNum = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_RECEIPT_NUM);
        if (receiptNum < 1 || receiptNum > 3) // receipt copy number is 1-3
            receiptNum = 2;

        if (isReprint) {
            receiptNum = 2;
        }

        if (Constants.ACQ_KPLUS.equals(acquirerName)
                || Constants.ACQ_WECHAT.equals(acquirerName)
                || Constants.ACQ_WECHAT_B_SCAN_C.equals(acquirerName)
                || Constants.ACQ_ALIPAY.equals(acquirerName)
                || Constants.ACQ_ALIPAY_B_SCAN_C.equals(acquirerName)
                || Constants.ACQ_QR_CREDIT.equals(acquirerName)) {
            receiptNum = 1;
        } else if (!Constants.ACQ_QR_PROMPT.equals(acquirerName) && !Constants.ACQ_WALLET.equals(acquirerName) && transData.isTxnSmallAmt()) {
            //EDCBBLAND-426 Support small amount
//            receiptNum = isReprint && transData.getNumSlipSmallAmt() == 0 ? 2 : transData.getNumSlipSmallAmt();
            receiptNum = isReprint ? 2 : transData.getNumSlipSmallAmt();
        }

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

