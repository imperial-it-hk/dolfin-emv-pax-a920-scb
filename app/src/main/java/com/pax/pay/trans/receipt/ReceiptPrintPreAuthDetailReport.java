package com.pax.pay.trans.receipt;

import android.graphics.Bitmap;

import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.model.MerchantProfileManager;
import com.pax.pay.trans.model.TransTotal;
import com.pax.pay.utils.Utils;

import java.util.Arrays;
import java.util.List;

import th.co.bkkps.utils.Log;

public class ReceiptPrintPreAuthDetailReport extends ReceiptPrintDetailReport {
    private static final String TITLE = Utils.getString(R.string.print_detail_report).toUpperCase();

    public ReceiptPrintPreAuthDetailReport(Acquirer acquirer, PrintListener listener) {
        super(acquirer, listener);
    }

    @Override
    protected int printAllAcquirers() {
        List<Acquirer> acquirers = FinancialApplication.getAcqManager().findAcquirer(Arrays.asList(Constants.ACQ_KBANK, Constants.ACQ_KBANK_BDMS, Constants.ACQ_UP));
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

    @Override
    protected int printByAcquirer() {
        long[] preAuthTotal = FinancialApplication.getTransTotalDbHelper().calPreAuthTotal(acquirer);
        if (preAuthTotal[0] == 0 && preAuthTotal[2] == 0) {
            return TransResult.ERR_NO_TRANS;
        }

        TransTotal total = new TransTotal();
        total.setMerchantName(MerchantProfileManager.INSTANCE.getCurrentMerchant());
        total.setAcquirer(acquirer);
        total.setMerchantID(acquirer.getMerchantId());
        total.setTerminalID(acquirer.getTerminalId());
        total.setDateTime(Device.getTime(Constants.TIME_PATTERN_TRANS));

        try {
            List<Bitmap> bitmaps = new ReceiptGeneratorPreAuthDetail(total, TITLE).generateBitmaps();

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
