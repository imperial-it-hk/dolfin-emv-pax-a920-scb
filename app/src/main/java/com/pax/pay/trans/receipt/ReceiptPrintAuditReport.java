package com.pax.pay.trans.receipt;

import android.graphics.Bitmap;

import com.pax.edc.R;
import com.pax.pay.ECR.EcrData;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.record.Printer;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.model.TransTotal;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.util.List;

/**
 * Created by SORAYA S on 28-May-18.
 */

public class ReceiptPrintAuditReport extends AReceiptPrint {

    public int print(List<TransData> list, TransTotal total, Acquirer acquirer, PrintListener listener) {
        this.listener = listener;
        int count = 0;

        if (!FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_AUDITREPORT_RECEIPT_ENABLE) && EcrData.instance.isOnProcessing) {
            if (listener != null) {
                listener.onEnd();
            }
            return 0;
        }

        if (listener != null)
            listener.onShowMessage(null, Utils.getString(R.string.wait_print));
        int ret = 0;
        List<Bitmap> bitmaps;
        if (Constants.isTOPS) {
            ReceiptGeneratorAuditReportTOPS receiptGeneratorAuditReportTOPS = new ReceiptGeneratorAuditReportTOPS(list, total, acquirer);
            bitmaps = receiptGeneratorAuditReportTOPS.generateArrayOfBitmap();
        } else {
            ReceiptGeneratorAuditReport receiptGeneratorAuditReport = new ReceiptGeneratorAuditReport(list, total, acquirer);
            bitmaps = receiptGeneratorAuditReport.generateArrayOfBitmap();
        }

        ret = printBitmap(bitmaps);
        if (ret != 0) {
            if (listener != null) {
                listener.onEnd();
            }
            return ret;
        }
        Printer.printAppNameVersion(listener, false);
        printStr("\n\n\n\n\n\n");
        if (listener != null) {
            listener.onEnd();
        }

        return ret;
    }
}
