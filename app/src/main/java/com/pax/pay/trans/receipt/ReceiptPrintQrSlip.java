package com.pax.pay.trans.receipt;

import android.graphics.Bitmap;

import com.pax.edc.R;
import com.pax.pay.constant.Constants;
import com.pax.pay.utils.Utils;

/**
 * Created by SORAYA S on 18-Apr-18.
 */

public class ReceiptPrintQrSlip extends AReceiptPrint {

    public int print(String termId, String merId, String amt, String transDateTime, String qrRef2, Bitmap bitmapQr, String billerServiceCode, String acqName, PrintListener listener) {
        this.listener = listener;
        if (listener != null) {
            listener.onShowMessage(null, Utils.getString(R.string.wait_print));
        }

        ReceiptGenerateQrSlip receiptGenerateQrSlip = new ReceiptGenerateQrSlip(termId, merId, amt, transDateTime, qrRef2, bitmapQr, billerServiceCode);
        int ret;

        if (Constants.ACQ_QRC.equals(acqName)) {
            ret = printBitmap(receiptGenerateQrSlip.generateVisaQRBitmap());
        }else if (Constants.ACQ_WALLET.equals(acqName)) {
            ret = printBitmap(receiptGenerateQrSlip.generateWalletBitmap());
        } else {
            ret = printBitmap(receiptGenerateQrSlip.generateBitmap());
        }

        if (listener != null) {
            listener.onEnd();
        }

        return ret;
    }

}
