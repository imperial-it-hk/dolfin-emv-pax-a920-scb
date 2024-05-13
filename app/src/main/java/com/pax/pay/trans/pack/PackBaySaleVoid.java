package com.pax.pay.trans.pack;

import android.util.Log;
import androidx.annotation.NonNull;
import com.pax.abl.core.ipacker.PackListener;
import com.pax.device.UserParam;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.trans.model.TransData;

public class PackBaySaleVoid extends PackSaleVoid {
    public PackBaySaleVoid(PackListener listener) {
        super(listener);
    }

    @NonNull
    @Override
    public byte[] pack(@NonNull TransData transData) {
        try {
            setMandatoryData(transData);
            setBitData63(transData);

            if (IsTransTLE(transData)) {
                transData.setTpdu("600" + UserParam.TLENI02 + "0000");
                setBitHeader(transData);
                return packWithTLE(transData);
            }
            else
                return pack(false,transData);

        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
        return "".getBytes();
    }

    @Override
    protected void setBitData63(@NonNull TransData transData) throws Iso8583Exception {
        //override from PackSaleVoid
        setBitData("63", transData.getField63Byte());
    }
}
