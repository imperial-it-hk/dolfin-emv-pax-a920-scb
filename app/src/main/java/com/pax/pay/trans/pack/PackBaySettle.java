package com.pax.pay.trans.pack;

import android.util.Log;
import androidx.annotation.NonNull;
import com.pax.abl.core.ipacker.PackListener;
import com.pax.device.UserParam;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.model.TransData;

public class PackBaySettle extends PackSettle {
    public PackBaySettle(PackListener listener) {
        super(listener);
    }

    @NonNull
    @Override
    public byte[] pack(@NonNull TransData transData) {
        try {
            boolean isAmex = Constants.ACQ_AMEX.equals(transData.getAcquirer().getName());

            setMandatoryData(transData);

            // field 11
            setBitData11(transData);

            setBitData60(transData);

            if (isAmex) {
                setBitData62(transData);
            }

            // field 63
            setBitData63(transData);

            if (IsTransTLE(transData)) {
                transData.setTpdu("600" + UserParam.TLENI02 + "0000");
                setBitHeader(transData);
            }
            return pack(false,transData);
        } catch (Iso8583Exception e) {
            Log.e(TAG, "", e);
        }
        return "".getBytes();
    }
}
