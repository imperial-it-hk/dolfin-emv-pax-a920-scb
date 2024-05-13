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
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.record.Printer;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * print detail
 *
 * @author Steven.W
 */
public class ReceiptPrintTransDetail extends AReceiptPrint {

    public int print(String title, List<TransData> list, Acquirer acquirer, PrintListener listener) {
        this.listener = listener;
        int count = 0;

        if (listener != null)
            listener.onShowMessage(null, Utils.getString(R.string.wait_print));
        // print detail main information
        ReceiptGeneratorTransDetail receiptGeneratorTransDetail = new ReceiptGeneratorTransDetail();
        int ret = printBitmap(receiptGeneratorTransDetail.generateMainInfo(title, acquirer));
        if (ret != 0) {
            if (listener != null) {
                listener.onEnd();
            }
            return ret;
        }
        List<TransData> details = new ArrayList<>();
        for (TransData data : list) {
            details.add(data);
            count++;
            if (count == list.size() || count % 20 == 0) {
                receiptGeneratorTransDetail = new ReceiptGeneratorTransDetail(details);
                if (Utils.getString(R.string.acq_all_acquirer).equals(acquirer.getName())) {
                    ret = printBitmap(receiptGeneratorTransDetail.generateBitmapAllAcquirer());
                } else if (Constants.ACQ_QR_PROMPT.equals(acquirer.getName())) {
                    ret = printBitmap(receiptGeneratorTransDetail.generateBitmapPromptPay());
                } else if (Constants.ACQ_WALLET.equals(acquirer.getName())) {
                    ret = printBitmap(receiptGeneratorTransDetail.generateBitmapWallet());
                } else if (Constants.ACQ_QRC.equals(acquirer.getName())) {
                    ret = printBitmap(receiptGeneratorTransDetail.generateBitmapQRSale());
                } else if (Constants.ACQ_KPLUS.equals(acquirer.getName())
                        || Constants.ACQ_ALIPAY.equals(acquirer.getName())
                        || Constants.ACQ_WECHAT.equals(acquirer.getName())
                        || Constants.ACQ_QR_CREDIT.equals(acquirer.getName())) {
                    ret = printBitmap(receiptGeneratorTransDetail.generateBitmapKbank());
                } else if (Constants.ACQ_REDEEM.equals(acquirer.getName()) || Constants.ACQ_REDEEM_BDMS.equals(acquirer.getName())) {
                    ret = printBitmap(receiptGeneratorTransDetail.generateBitmapKbankRedeem());
                } else if (Constants.ACQ_SMRTPAY.equals(acquirer.getName())
                        || Constants.ACQ_SMRTPAY_BDMS.equals(acquirer.getName())
                        || Constants.ACQ_DOLFIN_INSTALMENT.equals(acquirer.getName())) {
                    ret = printBitmap(receiptGeneratorTransDetail.generateBitmapInstalmentKbank());
                } else if (Constants.ACQ_DCC.equals(acquirer.getName())) {
                    ret = printBitmap(receiptGeneratorTransDetail.generateBitmapDccKbank());
                } else if (Constants.ACQ_AMEX_EPP.equals(acquirer.getName())) {
                    ret = printBitmap(receiptGeneratorTransDetail.generateBitmapInstalmentAmex());
                } else if (Constants.ACQ_BAY_INSTALLMENT.equals(acquirer.getName())) {
                    ret = printBitmap(receiptGeneratorTransDetail.generateBitmapInstalmentBay());
                } else {
                    ret = printBitmap(receiptGeneratorTransDetail.generateBitmap());
                }
                if (ret != 0) {
                    if (listener != null) {
                        listener.onEnd();
                    }
                    return ret;
                }
                details.clear();
            }
        }
        Printer.printAppNameVersion(listener, false);
        printStr("\n\n\n\n\n\n");
        if (listener != null) {
            listener.onEnd();
        }

        return ret;
    }
}
