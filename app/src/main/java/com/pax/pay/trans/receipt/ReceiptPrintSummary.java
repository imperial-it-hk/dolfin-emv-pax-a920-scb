package com.pax.pay.trans.receipt;

import android.graphics.Bitmap;

import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.record.Printer;
import com.pax.pay.trans.model.TransTotal;
import com.pax.pay.utils.Utils;

import java.util.List;

import th.co.bkkps.utils.Log;

public class ReceiptPrintSummary extends AReceiptPrint {

    private static final String TITLE = Utils.getString(R.string.print_history_summary).toUpperCase();
    private final boolean isAllAcquirers;
    private Acquirer acquirer;

    public ReceiptPrintSummary(Acquirer acquirer, PrintListener listener) {
        this.acquirer = acquirer;
        this.isAllAcquirers = acquirer.getId() == 0;
        this.listener = listener;
    }

    public int print() {
        if (acquirer == null) {
            return TransResult.ERR_NO_TRANS;
        }

        if (listener != null) {
            listener.onShowMessage(null, Utils.getString(R.string.wait_print));
        }

        int ret;
        if (isAllAcquirers) {
            ret = printAllAcquirers();
        } else {
            ret = printByAcquirer();
        }

        if (ret == TransResult.SUCC) {
            Printer.printAppNameVersion(listener, false);
            printStr("\n\n\n\n\n\n");
        }

        if (ret == -1) {//Out of paper, Printer over heats, Printer voltage is too low
            return TransResult.SUCC;//no need to show error
        }

//        if (listener != null) {
//            listener.onEnd();
//        }

        return ret;
    }

    private int printAllAcquirers() {
        List<Acquirer> acquirers = FinancialApplication.getAcqManager().findEnableAcquirers();
        if (acquirers == null) {
            return TransResult.ERR_NO_TRANS;
        }

        int lastResultSuccess = -1, idx = 1;
        for (Acquirer objAcquirer : acquirers) {
            acquirer = objAcquirer;
            int ret = printByAcquirer();
            if (ret == -1 || ERR_PRINT_MAP.get(ret, -1) != -1) {
                //-1 : Out of paper, Printer over heats, Printer voltage is too low
                //Otherwise: Other printer errors
                return ret;
            } else if (ret != TransResult.SUCC && lastResultSuccess != TransResult.SUCC && (idx == acquirers.size())) {
                return ret;
            } else {
                if (lastResultSuccess != TransResult.SUCC) {
                    lastResultSuccess = ret;
                }
            }
            idx++;
        }
        return lastResultSuccess;
    }

    private int printByAcquirer() {
        TransTotal total = FinancialApplication.getTransTotalDbHelper().calcTotal(acquirer, false);

        if (total == null || total.isZero()) {
            return TransResult.ERR_NO_TRANS;
        }

        total.setAcquirer(acquirer);
        total.setMerchantID(acquirer.getMerchantId());
        total.setTerminalID(acquirer.getTerminalId());
        total.setDateTime(Device.getTime(Constants.TIME_PATTERN_TRANS));

        try {
            List<Bitmap> bitmaps = null;
            switch (acquirer.getName()) {
                case Constants.ACQ_KBANK:
                case Constants.ACQ_KBANK_BDMS:
                case Constants.ACQ_UP:
                case Constants.ACQ_AMEX_EPP:
                case Constants.ACQ_SMRTPAY:
                case Constants.ACQ_SMRTPAY_BDMS:
                case Constants.ACQ_BAY_INSTALLMENT:
                case Constants.ACQ_DOLFIN_INSTALMENT:
                    bitmaps = new ReceiptGeneratorSummary(total, TITLE).generateBitmaps();
                    break;
                case Constants.ACQ_DCC:
                    bitmaps = new ReceiptGeneratorSummaryDcc(total, TITLE).generateBitmaps();
                    break;
                case Constants.ACQ_REDEEM:
                case Constants.ACQ_REDEEM_BDMS:
                    ReceiptGeneratorTotalTOPS receiptGeneratorTotalTOPS = new ReceiptGeneratorTotalTOPS(TITLE, null, total, 999, false);
                    return printBitmap(receiptGeneratorTotalTOPS.generateBitmapRedeemKbank());
                case Constants.ACQ_KPLUS:
                case Constants.ACQ_WECHAT:
                case Constants.ACQ_WECHAT_B_SCAN_C:
                case Constants.ACQ_ALIPAY:
                case Constants.ACQ_ALIPAY_B_SCAN_C:
                case Constants.ACQ_QR_CREDIT:
                case Constants.ACQ_MY_PROMPT:
                    bitmaps = new ReceiptGeneratorSummaryKBankWallet(total, TITLE).generateBitmaps();
                    break;
                case Constants.ACQ_DOLFIN:
                case Constants.ACQ_SCB_IPP:
                case Constants.ACQ_SCB_REDEEM:
                case Constants.ACQ_AMEX:
                case Constants.ACQ_KCHECKID:
                    return TransResult.SUCC;
            }

            if (bitmaps == null || bitmaps.isEmpty()) {
                return TransResult.ERR_GEN_BITMAP_FAIL;
            }

            return printBitmap(bitmaps);
        } catch (Exception e) {
            Log.e(TAG, "", e);
            return TransResult.ERR_GEN_BITMAP_FAIL;
        }
    }
}
