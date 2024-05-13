package com.pax.pay.trans.receipt;

import com.pax.edc.R;
import com.pax.pay.utils.Utils;

public class ReceiptPrintEReceiptUploadStatus extends AReceiptPrint {
    public int print(PrintListener listener, long[] ermSuccessTotal, long[] ermUnsuccessfulTotal, long[] ermVoidSuccessTotal, long[] ermVoidUnsuccessfulTotal) {
        return print(listener, ermSuccessTotal, ermUnsuccessfulTotal, ermVoidSuccessTotal, ermVoidUnsuccessfulTotal, false);
    }

    public int print(PrintListener listener, long[] ermSuccessTotal, long[] ermUnsuccessfulTotal, long[] ermVoidSuccessTotal, long[] ermVoidUnsuccessfulTotal, boolean SkipPrintFlag) {
        if (listener != null) {
            listener.onShowMessage(null, Utils.getString(R.string.wait_print));
        }

        ReceiptGeneratorEReceiptUploadStatus uploadStatus = new ReceiptGeneratorEReceiptUploadStatus(ermSuccessTotal, ermUnsuccessfulTotal, ermVoidSuccessTotal, ermVoidUnsuccessfulTotal);
        int ret = printBitmap(uploadStatus.generateBitmap(), SkipPrintFlag);

        printStr("\n\n\n\n\n\n", SkipPrintFlag);

        if (listener != null) {
            listener.onEnd();
        }

        return ret;
    }

}
