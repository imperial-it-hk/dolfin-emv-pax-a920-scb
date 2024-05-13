package com.pax.pay.trans.receipt;

import com.pax.edc.R;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import th.co.bkkps.utils.Log;

public class ReceiptPrintInstalmentKbankTrans extends ReceiptPrintTrans {

    private boolean isReprint;

    public int print(TransData transData, boolean isRePrint, PrintListener listener) {
        if (!transData.getIssuer().isAllowPrint())
            return 0;

        this.listener = listener;
        this.isReprint = isRePrint;
        int ret = 0;
        int receiptNum = getVoucherNum();
        if (receiptNum > 0) {
            if (listener != null)
                listener.onShowMessage(null, Utils.getString(R.string.wait_print));

            receiptNo = 0;
            receiptNum = handleVoucherNum(transData, receiptNum);

            for (; receiptNo < receiptNum; receiptNo++) {
                ReceiptGeneratorInstalmentKbankTrans genReceipt = new ReceiptGeneratorInstalmentKbankTrans(transData, receiptNo, receiptNum, isReprint);
                ret = printBitmap(genReceipt.generateBitmap());
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

    private int getVoucherNum() {
        int receiptNum = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_RECEIPT_NUM);
        if (receiptNum < 1 || receiptNum > 3) // receipt copy number is 1-3
            receiptNum = 2;

        if (isReprint) {
            receiptNum = 2;
        }

        Log.d(TAG, "ReceiptPrintInstalmentKbankTrans ---- NumbOfReceipt = " + receiptNum);
        return receiptNum;
    }
}
