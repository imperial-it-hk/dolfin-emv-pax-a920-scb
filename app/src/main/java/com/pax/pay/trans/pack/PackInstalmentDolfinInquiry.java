package com.pax.pay.trans.pack;

import androidx.annotation.NonNull;

import com.pax.abl.core.ipacker.PackListener;
import com.pax.device.Device;
import com.pax.device.UserParam;
import com.pax.eemv.utils.Tools;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.TimeConverter;

import java.io.IOException;
import java.util.Currency;

import th.co.bkkps.utils.Log;

public class PackInstalmentDolfinInquiry extends PackIso8583 {

    public PackInstalmentDolfinInquiry(PackListener listener) {
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
        setBitData4(transData);
        setBitData11(transData);
        setBitData12(transData);
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


    @Override
    public int unpack(@NonNull TransData transData, byte[] rsp) {
        int i = super.unpack(transData, rsp);
        if (!transData.getResponseCode().getCode().equals("00")) {
            try {
                transData.getResponseCode().setMessage(new String(transData.getField60RecByte()));
            } catch (Exception e) {
                Log.e(TAG, "", e);
            }
        }
        return i;
    }
}
