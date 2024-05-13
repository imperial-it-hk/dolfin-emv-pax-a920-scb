package com.pax.pay.trans.pack;

import androidx.annotation.NonNull;

import com.pax.abl.core.ipacker.PackListener;
import com.pax.device.UserParam;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.utils.Tools;
import com.pax.gl.pack.IIso8583;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;

import th.co.bkkps.utils.Log;

public class PackGetT1CMemberID extends PackIso8583 {
    public PackGetT1CMemberID(PackListener listener) { super(listener);}

    @Override
    @NonNull
    public byte[] pack(@NonNull TransData transData) {
        try {
            // set mandatory field
            setMandatoryData(transData);

            if (IsTransTLE(transData)) {
                // for Enabled TLE mode
                transData.setTpdu("600" + UserParam.TLENI02 + "832F");
                setBitHeader(transData);
                return packWithTLE(transData);
            } else {
                // for Non-TLE mode
                return pack(false, transData);
            }
        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
        return "".getBytes();
    }

    @Override
    protected void setMandatoryData(@NonNull TransData transData) throws Iso8583Exception {
        // h
        String pHeader = transData.getTpdu() + transData.getHeader();
        entity.setFieldValue("h", pHeader);
        // m
        ETransType transType = transData.getTransType();
        entity.setFieldValue("m", transType.getMsgType());

//        if (transData.getEnterMode() == TransData.EnterMode.MANUAL) {
//            setBitData2(transData);
//        }

        setBitData3(transData);

        setBitData11(transData);

//        if (transData.getEnterMode() == TransData.EnterMode.MANUAL) {
//            setBitData14(transData);
//        }

        setBitData22(transData);

//        if (transData.getEnterMode() != TransData.EnterMode.MANUAL) {
//            setBitData23(transData);
//        }

        setBitData24(transData);

        setBitData25(transData);

//        if (transData.getEnterMode() != TransData.EnterMode.MANUAL) {
//            setBitData35(transData);
//        }

        TransData.EnterMode enterMode = transData.getEnterMode();
        switch (enterMode) {
            case MANUAL:
                setBitData2(transData);
                setBitData14(transData);
                break;
            case FALLBACK:
            case SWIPE:
                setBitData35(transData);
                break;
            case SP200:
            case CLSS:
            case INSERT:
                setBitData23(transData);
                setBitData35(transData);
                setBitData55(transData);
                break;
        }

        setBitData41(transData);

        setBitData42(transData);

        // Override
        transData.setField63Byte(createInquiryT1CTableField63());
        setBitData63Byte(transData);
    }

    @Override
    protected void setBitData23(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("23", "001");
    }

    @Override
    protected int checkRecvData(@NonNull HashMap<String, byte[]> map, @NonNull TransData transData, boolean isCheckAmt) {
        return TransResult.SUCC;
    }

    private byte[] createInquiryT1CTableField63 () {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            outputStream.write(new byte[]{0x00, 0x35});                                                                                         // LEN=02 TABLE LEN
            outputStream.write(new byte[]{0x39, 0x31});                                                                                         // LEN=04 Table ID = '91'
            outputStream.write(new byte[]{0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20,});                            // LEN=12 Non Value fix HEX value '20'
            outputStream.write(new byte[]{0x39, 0x31, 0x32, 0x33});                                                                             // LEN=04 Extended Transaction Type = '9123'
            outputStream.write(new byte[]{0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20});     // LEN=16 Card No.
            outputStream.write(new byte[]{0x20});                                                                                               // LEN=01 Card Reference Flag

//            outputStream.write(new byte[]{0x00, 0x48});
//            outputStream.write(new byte[]{0x38, 0x30});
//            outputStream.write(new byte[]{0x39, 0x31, 0x30, 0x30});
//            outputStream.write(new byte[]{0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20,   0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20,
//                                          0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20,   0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20,
//                                          0x20, 0x20});
            byte[] field63SendRaw =  outputStream.toByteArray();
            Log.d("Online","GT1C-F63 Data Raw : " + Tools.bcd2Str(field63SendRaw));
            return field63SendRaw;
        } catch (IOException e) {
            Log.e(TAG, "", e);
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "", e);
            }
        }
        return new byte[0];
    }
}
