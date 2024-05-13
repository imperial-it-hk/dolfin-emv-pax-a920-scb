package com.pax.pay.trans.pack;

import androidx.annotation.NonNull;

import com.pax.abl.core.ipacker.PackListener;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.EReceiptUtils;
import com.pax.pay.utils.Utils;

import th.co.bkkps.utils.Log;

public class PackEReceiptTerminalAlert extends PackIso8583 {
    public PackEReceiptTerminalAlert(PackListener listener) {
        super(listener);
    }

    @NonNull
    @Override
    public byte[] pack(@NonNull TransData transData) {
        try {

            setMandatoryData(transData);

            return pack(false, transData);
        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
        return "".getBytes();
    }

    @Override
    protected void setMandatoryData(@NonNull TransData transData) throws Iso8583Exception {
        // h
        String pHeader = transData.getTpdu() + transData.getHeader();
        entity.setFieldValue("h", pHeader);
        // m
        entity.setFieldValue("m", transData.getTransType().getMsgType());

        setBitData3(transData);
        setBitData46(transData);

    }

    @Override
    protected void setBitData3(@NonNull TransData transData) throws Iso8583Exception {
        long lProcCode = Utils.parseLongSafe(transData.getTransType().getProcCode(), 0);
        if (EReceiptUtils.getInstance().num_term_alert_txn > 0) {
            lProcCode += 1;
        }
        EReceiptUtils.getInstance().num_term_alert_txn += 1;
        setBitData("3", String.valueOf(lProcCode));
    }

    @Override
    protected void setBitData46(@NonNull TransData transData) throws Iso8583Exception {
        String TSN = EReceiptUtils.getInstance().GetSerialNumber(FinancialApplication.getDownloadManager().getSn());
        setBitData("46", EReceiptUtils.getInstance().getSize(TSN.getBytes()));
    }
}
