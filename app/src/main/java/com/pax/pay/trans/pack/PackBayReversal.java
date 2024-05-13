package com.pax.pay.trans.pack;

import android.util.Log;
import androidx.annotation.NonNull;
import com.pax.abl.core.ipacker.PackListener;
import com.pax.device.UserParam;
import com.pax.pay.trans.model.TransData;

public class PackBayReversal extends PackReversal {
    public PackBayReversal(PackListener listener) {
        super(listener);
    }

    @NonNull
    @Override
    public byte[] pack(@NonNull TransData transData) {
        try {
            setCommonData(transData);

            if (IsTransTLE(transData)) {
                transData.setTpdu("600" + UserParam.TLENI02 + "0000");
                setBitHeader(transData);
            }
            return pack(false, transData);

        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
        return "".getBytes();
    }
}
