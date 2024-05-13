package com.pax.pay.trans.pack;

import androidx.annotation.NonNull;

import com.pax.abl.core.ipacker.PackListener;
import com.pax.eemv.utils.Tools;
import com.pax.gl.pack.IIso8583;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.Convert;
import com.pax.pay.utils.TimeConverter;
import com.pax.pay.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;

import th.co.bkkps.utils.Log;

/**
 * Created by NANNAPHAT S on 8/11/2018.
 */

public class PackThaiQrVerifyPaySlip extends PackIso8583 {

    private static final String TABLE_ID_BC = "BC";
    private static final String TABLE_VERSION_BC = "\01";//hex-01(Start of Heading:SOH)
    //private static final String TABLE_ID_QR = "QR";
    //private static final String TABLE_VERSION_QR = "\01";//hex-01(Start of Heading:SOH)

    public PackThaiQrVerifyPaySlip(PackListener listener) {
        super(listener);
    }

    @NonNull
    @Override
    public byte[] pack(@NonNull TransData transData) {
        try {
            setMandatoryData(transData);

            return pack(false,transData);

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

        // field 3 Processing Code
        setBitData3(transData);

        // field4 Txn Amt
        setBitData4(transData);

        // field 24 NII
        transData.setNii(transData.getAcquirer().getNii());
        setBitData24(transData);

        // field 41 TID
        setBitData41(transData);

        // field 42 MID
        setBitData42(transData);

        // field 63 Other request data
        setBitData63(transData);
    }




    @Override
    protected void setBitData63(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("63", getField63Data(transData));
    }

    private byte[] getField63Data(@NonNull TransData transData)  {
        ByteArrayOutputStream byteArrOutputStream = new ByteArrayOutputStream();
        try {
            if (transData.getWalletPartnerID()!=null
                    && transData.getTxnID()!=null
                    && transData.getWalletVerifyPaySlipQRCode()!=null) {

                String paddedStr = null;
                paddedStr = Utils.getStringPadding(transData.getWalletPartnerID(), 32, " ", Convert.EPaddingPosition.PADDING_RIGHT);
                byteArrOutputStream.write(paddedStr.getBytes());

                paddedStr = Utils.getStringPadding(transData.getTxnID(), 64, " ", Convert.EPaddingPosition.PADDING_RIGHT);
                byteArrOutputStream.write(paddedStr.getBytes());

                paddedStr = Utils.getStringPadding("", 24, " ", Convert.EPaddingPosition.PADDING_RIGHT);
                byteArrOutputStream.write(paddedStr.getBytes());

                paddedStr = Utils.getStringPadding("", 16, " ", Convert.EPaddingPosition.PADDING_RIGHT);
                byteArrOutputStream.write(paddedStr.getBytes());

                paddedStr = Utils.getStringPadding("", 3, " ", Convert.EPaddingPosition.PADDING_RIGHT);
                byteArrOutputStream.write(paddedStr.getBytes());

                paddedStr = Utils.getStringPadding(transData.getWalletVerifyPaySlipQRCode(), 400, " ", Convert.EPaddingPosition.PADDING_RIGHT);
                byteArrOutputStream.write(paddedStr.getBytes());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return byteArrOutputStream.toByteArray();
    }

}
