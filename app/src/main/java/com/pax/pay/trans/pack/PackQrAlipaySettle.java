package com.pax.pay.trans.pack;

import th.co.bkkps.utils.Log;
import androidx.annotation.NonNull;
import com.pax.abl.core.ipacker.PackListener;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.TransData;

import java.io.ByteArrayOutputStream;

/**
 * Created by NANNAPHAT S on 04-Feb-19.
 */

public class PackQrAlipaySettle extends PackIso8583 {

    public PackQrAlipaySettle(PackListener listener) {
        super(listener);
    }

    @NonNull
    @Override
    public byte[] pack(@NonNull TransData transData) {
        try {
            setMandatoryData(transData);

            // field 11 STAN
            setBitData11(transData);

            // field 12 / 13 date time
            setBitData12(transData);

            // field 24 NII
            transData.setNii(transData.getAcquirer().getNii());
            setBitData24(transData);

            // field 41 TID
            setBitData41(transData);

            // field 42 MID
            setBitData42(transData);

            // field 60 Batch number
            setBitData60(transData);

            //setBitData61(transData);

            // field 63
            setBitData63(transData);

            return pack(false, transData);
        } catch (Iso8583Exception e) {
            Log.e(TAG, "", e);
        }
        return "".getBytes();
    }

    @Override
    protected void setBitData60(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("60", Component.getPaddedNumber(transData.getBatchNo(), 6));
    }

    @Override
    protected void setBitData61(@NonNull TransData transData) throws Iso8583Exception {
        //entity.setFieldValue("61", Component.initPrintText() + Component.initTerminalInformation());
        setBitData("61", Component.initPrintText() + Component.initTerminalInformation());
    }

    @Override
    protected void setBitData63(@NonNull TransData transData) throws Iso8583Exception {
        if (transData.getField63Byte() == null) {
            setBitData("63", "");
        } else {
            super.setBitData63Byte(transData);
        }
        //setBitData("63", initBit63(transData));
    }

    private byte[] initBit63 (TransData transData){
        //TODO: Need to improve for KBANK QR settlement

        String saleNum;
        String saleAmt;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

// Payment Transaction No. count

// Summary total payment amount

// Application Code
        String AppCode = transData.getAppCode();
        outputStream.write(AppCode.getBytes(),0,AppCode.getBytes().length);

        return outputStream.toByteArray();
    }
}
