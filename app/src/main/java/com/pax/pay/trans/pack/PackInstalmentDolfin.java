package com.pax.pay.trans.pack;

import androidx.annotation.NonNull;

import com.pax.abl.core.ipacker.PackListener;
import com.pax.device.Device;
import com.pax.device.UserParam;
import com.pax.eemv.utils.Tools;
import com.pax.gl.pack.IIso8583;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.TimeConverter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Currency;

import th.co.bkkps.utils.Log;

public class PackInstalmentDolfin extends PackIso8583 {

    public PackInstalmentDolfin(PackListener listener) {
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
        setBitData61(transData);
        setBitData63(transData);
    }


    @Override
    protected void setBitData25(@NonNull TransData transData) throws Iso8583Exception {
        //do nothing
    }

    @Override
    protected void setBitData61(@NonNull TransData transData) throws Iso8583Exception {
        transData.setField61Byte(Tools.string2Bytes(transData.getQrCode()));
        setBitData("61", transData.getField61Byte());
    }

    @Override
    protected void setBitData63(@NonNull TransData transData) throws Iso8583Exception {
        if (transData.getReversalStatus() == TransData.ReversalStatus.REVERSAL) {
            setBitData("63", transData.getField63Byte());
        } else {
            byte[] f63 = new byte[0];
            try {
                f63 = packBit63(transData);
            } catch (IOException e) {
                Log.e(TAG, "", e);
            }

            transData.setField63Byte(f63);
            setBitData("63", f63);
        }
    }

    private byte[] packBit63(TransData transData) throws IOException {
        String serialNo = transData.isInstalmentPromoProduct() || ("03".equals(transData.getInstalmentIPlanMode()) || "04".equals(transData.getInstalmentIPlanMode()))
                ? Component.getPaddedStringRight(transData.getInstalmentSerialNo(), 18, ' ')
                : Component.getPaddedString("", 18, '9');

        String transId = Component.getPaddedStringRight(TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS, "yyyyMMdd") + "_" +
                transData.getAcquirer().getTerminalId() + Device.getTime(Constants.TIME_PATTERN_TRANS3), 50, ' ');

        String f63 = Currency.getInstance(transData.getCurrency()).getCurrencyCode() +
                transId +
                Component.getPaddedStringRight("999", 11, ' ') +
                Component.getPaddedStringRight(transData.getInstalmentIPlanMode(), 10, ' ') +
                Component.getPaddedStringRight(transData.getSkuCode(), 10, transData.getSkuCode().isEmpty() ? '9' : ' ') +
                Component.getPaddedStringRight(String.valueOf(transData.getInstalmentPaymentTerm()) , 11, ' ') +
                Component.getPaddedStringRight(transData.getProductCode(), 9, transData.getProductCode().isEmpty() ? '9' : ' ') +
                serialNo;
        f63 = Component.getPaddedStringRight(f63, 267, '9');
        return Tools.string2Bytes(f63);
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
