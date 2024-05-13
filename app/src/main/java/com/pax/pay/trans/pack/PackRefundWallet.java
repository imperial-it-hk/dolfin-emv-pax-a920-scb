package com.pax.pay.trans.pack;

import th.co.bkkps.utils.Log;
import androidx.annotation.NonNull;
import com.pax.abl.core.ipacker.PackListener;
import com.pax.dal.entity.ETermInfoKey;
import com.pax.device.Device;
import com.pax.eemv.utils.Tools;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Created by WITSUTA A on 5/11/2018.
 */

public class PackRefundWallet extends PackIso8583  {

    private static final String TABLE_ID_RF = "RF";
    private static final String TABLE_VERSION_RF = "\01";

    public PackRefundWallet(PackListener listener) {
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
        if (transData.getReversalStatus() == TransData.ReversalStatus.REVERSAL) {
            entity.setFieldValue("m", transType.getDupMsgType());
        } else if (transData.getWalletRetryStatus() == TransData.WalletRetryStatus.RETRY_CHECK) {
            entity.setFieldValue("m", transType.getRetryChkMsgType());
        } else if (transData.getAdviceStatus() == TransData.AdviceStatus.ADVICE) {
            entity.setFieldValue("m", transType.getRetryChkMsgType());
        } else{
            entity.setFieldValue("m", transType.getMsgType());
        }

        // field 3
        setBitData3(transData);
        setBitData4(transData);
        //field 11
        setBitData11(transData);

        if (transData.getAdviceStatus() == TransData.AdviceStatus.ADVICE) {
            setBitData37(transData);
        }

        // field 24 NII
        transData.setNii(transData.getAcquirer().getNii());
        setBitData24(transData);

        setBitData41(transData);
        setBitData42(transData);

        if (transData.getAdviceStatus() != TransData.AdviceStatus.ADVICE) {
            setBitData60(transData);
            setBitData62(transData);
            setBitData63(transData);
        }

    }

    @Override
    protected void setBitData3(@NonNull TransData transData) throws Iso8583Exception {
        if (transData.getAdviceStatus() == TransData.AdviceStatus.ADVICE) {
            setBitData("3", "220000");
        } else {
            setBitData("3", transData.getTransType().getProcCode());
        }
    }

    @Override
    protected void setBitData63(@NonNull TransData transData) throws Iso8583Exception {
        try {
            byte[] byteRF = initRefundByte(transData);
            byte[] byteTX = initPrintTextByte();
            byte[] byteTM = initTerminalInformationByte();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(byteRF);
            outputStream.write(byteTX);
            outputStream.write(byteTM);
            //entity.setFieldValue("63", outputStream.toByteArray());
            setBitData("63", outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private byte[] initRefundByte (TransData transData) throws IOException {
        byte[] byteTableID = Tools.string2Bytes(TABLE_ID_RF);
        byte[] byteTableVer = Tools.string2Bytes(TABLE_VERSION_RF);
        byte[] byteDateTime = hexStringToByteArray(Device.getTime(Constants.TIME_PATTERN_TRANS));
        String origAmount = String.valueOf(Component.getPaddedNumber(Long.parseLong(transData.getOrigAmount()), 12));
        byte[] byteOrigAmount = hexStringToByteArray(origAmount);
        byte[] bytePartnerID = Tools.string2Bytes(transData.getRefNo());


        ByteArrayOutputStream outputMessage = new ByteArrayOutputStream( );
        outputMessage.write(byteTableID);
        outputMessage.write(byteTableVer);
        outputMessage.write(byteDateTime);
        outputMessage.write(byteOrigAmount);
        outputMessage.write(bytePartnerID);

        byte byteMSG[] = outputMessage.toByteArray( );

        String lengthMSG = String.valueOf(Component.getPaddedNumber(outputMessage.size(),4));
        byte[] bytesLen = hexStringToByteArray(lengthMSG);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(bytesLen);
        outputStream.write(byteMSG);
        return outputStream.toByteArray( );
    }

    public byte[] initPrintTextByte() throws IOException {
        byte[] byteTableID = Tools.string2Bytes("TX");
        byte[] byteTableVer = new byte[]{(byte)0x02};
        byte[] byteCharPerLine = new byte[]{(byte)0x34};
        byte[] byteMaxSize = hexStringToByteArray("0500");

        ByteArrayOutputStream outputMessage = new ByteArrayOutputStream( );
        outputMessage.write(byteTableID);
        outputMessage.write(byteTableVer);
        outputMessage.write(byteCharPerLine);
        outputMessage.write(byteMaxSize);

        byte byteMSG[] = outputMessage.toByteArray( );

        String lengthMSG = String.valueOf(Component.getPaddedNumber(outputMessage.size(),4));
        byte[] bytesLen = hexStringToByteArray(lengthMSG);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(bytesLen);
        outputStream.write(byteMSG);
        return outputStream.toByteArray( );
    }

    private byte[] initTerminalInformationByte() throws IOException {

        String tableID = "TM";
        byte[] byteTableID = Tools.string2Bytes(tableID);

        byte[] byteTableVersion = new byte[]{(byte)0x01};

        byte[] byteSeparator = new byte[] {(byte)0x1f};

        // terminal information
        String currentDateTime = Tools.hexToAscii(Device.getTime(Constants.TIME_PATTERN_TRANS));
        byte[] byteCurrentDateTime = Tools.string2Bytes(currentDateTime);

        // get serial number EDC.
        Map<ETermInfoKey, String> termInfo = FinancialApplication.getDal().getSys().getTermInfo();
        String termsn = termInfo.get(ETermInfoKey.SN);
        byte[] byteTermsn = Tools.string2Bytes(termsn);

        // get app version.
        String termsw = FinancialApplication.getApp().getVersion();
        byte[] byteTermsw = Tools.string2Bytes(termsw);

        ByteArrayOutputStream outputMessage = new ByteArrayOutputStream( );

        outputMessage.write(byteTableID);
        outputMessage.write(byteTableVersion);

        outputMessage.write(byteCurrentDateTime);
        outputMessage.write(byteTermsn);
        outputMessage.write(byteSeparator);
        outputMessage.write(byteTermsw);
        byte byteMSG[] = outputMessage.toByteArray( );

        String lengthMSG = String.valueOf(Component.getPaddedNumber(outputMessage.size(),4));
        byte[] bytesLen = hexStringToByteArray(lengthMSG);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(bytesLen);
        outputStream.write(byteMSG);

        return outputStream.toByteArray( );
    }


    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}

