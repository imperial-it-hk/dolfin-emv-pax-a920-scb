package com.pax.pay.trans.pack;

import androidx.annotation.NonNull;

import com.pax.abl.core.ipacker.PackListener;
import com.pax.device.UserParam;
import com.pax.gl.pack.IIso8583;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.model.TransData;

import th.co.bkkps.utils.Log;

public class PackInstalmentDolfinVoid extends PackIso8583 {

    public PackInstalmentDolfinVoid(PackListener listener) {
        super(listener);
    }

    @NonNull
    @Override
    public byte[] pack(@NonNull TransData transData) {
        try {
            setMandatoryData(transData);
            setFinancialData(transData);

            if (IsTransTLE(transData)) {
                transData.setTpdu("600" + UserParam.TLENI01 + "8000");
                setBitHeader(transData);
                return packWithTLE(transData);
            } else {
                return pack(false, transData);
            }

        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
        return "".getBytes();
    }

    @Override
    protected void setFinancialData(@NonNull TransData transData) throws Iso8583Exception {
        // Header, MsgType, field 3, 24, 25, 41, 42 are set in Mandatory data.
        setBitData4(transData);
        setBitData11(transData);
        setBitData12(transData);// field 12, 13
        //setBitData37(transData);
        //setBitData38(transData);
        setBitData63(transData);
    }

    @Override
    protected void setBitData25(@NonNull TransData transData) throws Iso8583Exception {
        //do nothing
    }

    @Override
    protected void setBitData63(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("63", transData.getField63Byte());
    }
}
