package com.pax.pay.trans.pack;

import th.co.bkkps.utils.Log;
import androidx.annotation.NonNull;
import com.pax.abl.core.ipacker.PackListener;
import com.pax.device.UserParam;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.trans.model.TransData;

public class PackInstalmentSettleKbank extends PackIso8583 {
    public PackInstalmentSettleKbank(PackListener listener) {
        super(listener);
    }

    @NonNull
    @Override
    public byte[] pack(@NonNull TransData transData) {
        try {
            setFinancialData(transData);

            return pack(false, transData);

        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
        return "".getBytes();
    }

    @Override
    protected void setFinancialData(@NonNull TransData transData) throws Iso8583Exception {
        if (IsTransTLE(transData)) {
            transData.setTpdu("600" + UserParam.TLENI01 + "8000");
        }
        // h
        String pHeader = transData.getTpdu() + transData.getHeader();
        entity.setFieldValue("h", pHeader);
        // m
        entity.setFieldValue("m", transData.getTransType().getMsgType());

        setBitData3(transData);
        setBitData11(transData);
        transData.setNii(FinancialApplication.getAcqManager().getCurAcq().getNii());
        setBitData24(transData);
        setBitData41(transData);
        setBitData42(transData);
        setBitData60(transData);
        setBitData62(transData);
        setBitData63(transData);
    }

    @Override
    protected void setBitData60(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("60", transData.getField60());
    }
}
