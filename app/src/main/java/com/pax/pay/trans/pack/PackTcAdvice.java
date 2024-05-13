package com.pax.pay.trans.pack;

import th.co.bkkps.utils.Log;
import androidx.annotation.NonNull;
import com.pax.abl.core.ipacker.PackListener;
import com.pax.device.UserParam;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.trans.model.TransData;

/**
 * Created by SORAYA S on 08-Aug-18.
 */

public class PackTcAdvice extends PackIso8583 {

    public PackTcAdvice(PackListener listener) {
        super(listener);
    }

    @NonNull
    @Override
    public byte[] pack(@NonNull TransData transData) {
        try {
            setMandatoryData(transData);//3, 25, 41, 42

            setBitData2(transData);
            setBitData4(transData);
            setBitData11(transData);
            setBitData12(transData);//12, 13
            setBitData14(transData);
            setBitData22(transData);
            setBitData23(transData);

            transData.setNii(transData.getAcquirer().getNii());
            setBitData24(transData);

            setBitData37(transData);
            setBitData38(transData);
            setBitData39(transData);
            setBitData55(transData);
            setBitData60(transData);
            setBitData62(transData);

            if (IsTransTLE(transData)) {
                transData.setTpdu("600" + UserParam.TLENI01 + "8000");
                setBitHeader(transData);
                return packWithTLE(transData);
            }
            else
                return pack(false,transData);
        } catch (Iso8583Exception e) {
            Log.e(TAG, "", e);
        }
        return "".getBytes();
    }

    @Override
    protected void setBitData37(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("37", transData.getOrigRefNo());
    }

    @Override
    protected void setBitData39(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("39", "00");
    }
}
