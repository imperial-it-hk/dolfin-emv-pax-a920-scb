package com.pax.pay.trans.pack;

import th.co.bkkps.utils.Log;
import androidx.annotation.NonNull;
import com.pax.abl.core.ipacker.PackListener;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.trans.model.TransData;

/**
 * Created by huangmuhua on 2018/1/30.
 */

public class PackBps extends PackIso8583 {
    public PackBps(PackListener listener) {
        super(listener);
    }

    @NonNull
    @Override
    public byte[] pack(@NonNull TransData transData) {

        try {
            setMandatoryData(transData);
            setBitData2(transData);

            return pack(false,transData);

        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
        return "".getBytes();
    }

    @Override
    protected void setBitData2(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("2", transData.getQrCode());
    }
}
