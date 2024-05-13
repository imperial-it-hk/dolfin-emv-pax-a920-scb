package com.pax.pay.trans.pack;

import th.co.bkkps.utils.Log;
import androidx.annotation.NonNull;
import com.pax.abl.core.ipacker.PackListener;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.TransData;

/**
 * Created by SORAYA S on 17-May-18.
 */

public class PackWalletSettle extends PackIso8583 {

    public PackWalletSettle(PackListener listener) {
        super(listener);
    }

    @NonNull
    @Override
    public byte[] pack(@NonNull TransData transData) {
        try {
            setMandatoryData(transData);

            // field 11
            setBitData11(transData);

            transData.setNii(transData.getAcquirer().getNii());
            setBitData24(transData);

            setBitData60(transData);

            setBitData61(transData);

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
        if (transData.getField63() == null) {
            setBitData("63", "");
        } else {
            super.setBitData63(transData);
        }
    }
}
