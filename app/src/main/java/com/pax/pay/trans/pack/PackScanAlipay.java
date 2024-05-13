package com.pax.pay.trans.pack;

import androidx.annotation.NonNull;

import com.pax.abl.core.ipacker.PackListener;
import com.pax.eemv.utils.Tools;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.TimeConverter;
import com.pax.device.UserParam;
import com.pax.pay.utils.ControlLimitUtils;
import com.pax.pay.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Currency;

import th.co.bkkps.utils.Log;

/**
 * Created by NANNAPHAT S on 04-Feb-19.
 */

public class PackScanAlipay extends PackIso8583 {

    public PackScanAlipay(PackListener listener) {
        super(listener);
    }

    @NonNull
    @Override
    public byte[] pack(@NonNull TransData transData) {
        try {
            setMandatoryData(transData);

            return pack(false, transData);
        } catch (Iso8583Exception e) {
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
        if (transData.getReversalStatus() == TransData.ReversalStatus.REVERSAL) {
            entity.setFieldValue("m", transType.getDupMsgType());
        } else {
            entity.setFieldValue("m", transType.getMsgType());
        }

        // field 3 Processing Code
        if (transData.getReversalStatus() == TransData.ReversalStatus.REVERSAL) {
            setBitData("3", "000000");
        } else {
            setBitData3(transData);
        }

        // field4 Txn Amt
        setBitData4(transData);

        //field 11 STAN
        setBitData11(transData);

        // field 12 / 13 date time
        setBitData12(transData);


        // field 22 POS Entry mode
        setBitData22(transData);

        // field 24 NII
        transData.setNii(transData.getAcquirer().getNii());
        setBitData24(transData);

        // field 25 POS Condition code
        setBitData25(transData);

        setBitData35(transData);

        // field 41 TID
        setBitData41(transData);

        // field 42 MID
        setBitData42(transData);

        setBitData62(transData);
        setBitData63(transData);
    }

    @Override
    protected void setBitData22(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("22", "030");  //30 means QR code.
    }

    @Override
    protected void setBitData24(@NonNull TransData transData) throws Iso8583Exception {
        android.util.Log.i(TAG, "allen setBitData24: " + transData.getNii());
        setBitData("24", transData.getNii());
    }

    @Override
    protected void setBitData25(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("25", "00");
    }

    @Override
    protected void setBitData35(@NonNull TransData transData) throws Iso8583Exception {
        android.util.Log.i(TAG, "allen setBitData35: " + transData.getQrResult());
        setBitData("35", transData.getQrResult());
    }

    @Override
    protected void setBitData60(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("60", Component.getPaddedNumber(transData.getBatchNo(), 6));
    }

    @Override
    protected void setBitData61(@NonNull TransData transData) throws Iso8583Exception {
        //entity.setFieldValue("61", Component.initPrintText() + Component.initTerminalInformation());
        setBitData("61", Component.initPrintText() + Component.initTerminalInformation());
    }

    @Override
    protected void setBitData63(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("63", Utils.safeConvertByteArrayToString(initGetQR(transData)));
        //setBitData("63", initBit63(transData));
    }

    //this function is for KBANK wallet Transaction.
    private byte[] initGetQR(TransData transData) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        byte[] bytes;
// Currency (3 bytes)
        bytes = new byte[3];
        Arrays.fill( bytes, (byte) 0x20 );
        String Currency = java.util.Currency.getInstance(transData.getCurrency()).getCurrencyCode();
        outputStream.write(Currency.getBytes(), 0, Currency.getBytes().length);

// Partner Transaction ID (32 bytes)
        bytes = new byte[32];
        String DateTime = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS, Constants.TIME_PATTERN_TRANS2);
        String Random = randomNumber(12);
        String PartnerTxnID = DateTime + Random + transData.getAcquirer().getTerminalId();
        transData.setWalletPartnerID(PartnerTxnID);
        //Arrays.fill( bytes, PartnerTxnID );
        outputStream.write(PartnerTxnID.getBytes(), 0, PartnerTxnID.getBytes().length);


//  Transaction ID (64 bytes)
        bytes = new byte[64];
        Arrays.fill(bytes, (byte) 0x20);
        outputStream.write(bytes, 0, bytes.length);

//  Pay Time (16 bytes)
        bytes = new byte[16];
        Arrays.fill(bytes, (byte) 0x20);
        outputStream.write(bytes, 0, bytes.length);

// Exchange Rate (12 bytes)
        bytes = new byte[12];
        Arrays.fill(bytes, (byte) 0x20);
        outputStream.write(bytes, 0, bytes.length);

// Transaction Amount CNY (12 bytes)
        bytes = new byte[12];
        Arrays.fill(bytes, (byte) 0x20);
        outputStream.write(bytes, 0, bytes.length);

// Transaction Amount (12 bytes)
        bytes = new byte[12];
        Arrays.fill(bytes, (byte) 0x20);
        outputStream.write(bytes, 0, bytes.length);


// Buyer User ID (16 bytes)
        bytes = new byte[16];
        Arrays.fill(bytes, (byte) 0x20);
        outputStream.write(bytes, 0, bytes.length);


// Buyer Login ID (20 bytes)
        bytes = new byte[20];
        Arrays.fill(bytes, (byte) 0x20);
        outputStream.write(bytes, 0, bytes.length);

// Merchant Info (128 bytes)
        bytes = new byte[128];
        Arrays.fill(bytes, (byte) 0x20);
        outputStream.write(bytes, 0, bytes.length);


// Application Code (2 bytes)
        bytes = new byte[]{0x30, 0x31};
        outputStream.write(bytes, 0, bytes.length);

// Promocode (24 bytes)
        bytes = new byte[24];
        Arrays.fill(bytes, (byte) 0x20);
        outputStream.write(bytes, 0, bytes.length);

// REF1 (24 bytes)
        if (transData.getSaleReference1() == null) {
            transData.setSaleReference1("");
        }
        String ref1 = Component.getPaddedStringRight(transData.getSaleReference1(), 24, ' ');
        outputStream.write(Tools.string2Bytes(ref1),0, ref1.length());

// REF2 (24 bytes)
        if (transData.getSaleReference2() == null) {
            transData.setSaleReference2("");
        }
        String ref2 = Component.getPaddedStringRight(transData.getSaleReference2(), 24, ' ');
        outputStream.write(Tools.string2Bytes(ref2),0, ref2.length());

        return outputStream.toByteArray();
    }

    protected String randomNumber(int len) {
        final String AB = "0123456789"; //"0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++)
            sb.append(AB.charAt(rnd.nextInt(AB.length())));
        return sb.toString();
    }

    private byte[] initBit63(TransData transData) {
        //TODO: Need to improve for KBANK QR settlement

        String saleNum;
        String saleAmt;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

// Payment Transaction No. count

// Summary total payment amount

// Application Code
        String AppCode = transData.getAppCode();
        outputStream.write(AppCode.getBytes(), 0, AppCode.getBytes().length);

        return outputStream.toByteArray();
    }
}
