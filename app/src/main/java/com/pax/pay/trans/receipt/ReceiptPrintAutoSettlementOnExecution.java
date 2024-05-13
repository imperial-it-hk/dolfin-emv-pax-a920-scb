package com.pax.pay.trans.receipt;

import com.pax.edc.opensdk.TransResult;
import com.pax.pay.base.Acquirer;

import java.util.List;

public class ReceiptPrintAutoSettlementOnExecution extends AReceiptPrint {

    public int print(PrintListener listener, List<String> hostList, String title) {
        ReceiptGeneratorAutoSettlementOnExecution report = new ReceiptGeneratorAutoSettlementOnExecution(title);
        int ret = printBitmap(report.generateBitmapAutoSettlementWakeupHost(hostList));
        if (listener!=null) { listener.onEnd();}
        return ret;
    }

    public int printEReceiptOnFailedUpload (PrintListener listener) {
        ReceiptGeneratorAutoSettlementOnExecution report = new ReceiptGeneratorAutoSettlementOnExecution();
        int ret = printBitmap(report.generateBitmapAutoSettleERMUploadFailed());
        if (listener!=null) { listener.onEnd();}
        return ret;
    }


    public int printHostNotFound (PrintListener listener) {
        ReceiptGeneratorAutoSettlementOnExecution report = new ReceiptGeneratorAutoSettlementOnExecution();
        int ret = printBitmap(report.generateBitmapAutoSettleHostNotFound());
        if (listener!=null) { listener.onEnd();}
        return ret;
    }

}
