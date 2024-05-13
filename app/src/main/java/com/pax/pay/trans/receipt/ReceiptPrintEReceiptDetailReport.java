package com.pax.pay.trans.receipt;

import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class ReceiptPrintEReceiptDetailReport extends AReceiptPrint {

    public int print(PrintListener listener, List<TransData> transDataList) {
        if (transDataList == null || transDataList.isEmpty()) {
            return TransResult.ERR_NO_TRANS;
        }

        int count = 0;

        if (listener != null)
            listener.onShowMessage(null, Utils.getString(R.string.wait_print));

        ReceiptGeneratorEReceiptDetailReport report = new ReceiptGeneratorEReceiptDetailReport();
        int ret = printBitmap(report.generateHeaderBitmap());
        if (ret != 0) {
            if (listener != null) {
                listener.onEnd();
            }
            return ret;
        }

        List<TransData> details = new ArrayList<>();
        for (TransData data : transDataList) {
            details.add(data);
            count++;
            if (count == transDataList.size() || count % 20 == 0) {
                report = new ReceiptGeneratorEReceiptDetailReport(details);
                ret = printBitmap(report.generateBitmap());
                if (ret != 0) {
                    if (listener != null) {
                        listener.onEnd();
                    }
                    return ret;
                }
                details.clear();
            }
        }

        printStr("\n\n\n\n\n\n");
        if (listener != null) {
            listener.onEnd();
        }

        return ret;
    }
}
