package com.pax.pay.trans.pack;

import th.co.bkkps.utils.Log;
import androidx.annotation.NonNull;
import com.pax.abl.core.ipacker.PackListener;
import com.pax.dal.entity.ETermInfoKey;
import com.pax.eemv.utils.Tools;
import com.pax.gl.pack.IIso8583;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

public class PackWalletGetQR extends PackIso8583 {

    public PackWalletGetQR(PackListener listener) {
        super(listener);
    }

    @Override
    @NonNull
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
        }else {
            entity.setFieldValue("m", transType.getMsgType());
        }

        setBitData3(transData);

        setBitData4(transData);

        setBitData11(transData);

        setBitData22(transData);

        // field 24 Nii
        transData.setNii(transData.getAcquirer().getNii());
        setBitData24(transData);

        setBitData41(transData);

        setBitData42(transData);

        setBitData60(transData);

        setBitData62(transData);

        setBitData63(transData);
    }

    @Override
    protected void setBitData22(@NonNull TransData transData) throws Iso8583Exception {
        IIso8583.IIso8583Entity.IFieldAttrs iFieldAttrs22 = entity.createFieldAttrs().setPaddingPosition(IIso8583.IIso8583Entity.IFieldAttrs.EPaddingPosition.PADDING_LEFT);
        setBitData("22", "000", iFieldAttrs22);
    }

    @Override
    protected void setBitData63(@NonNull TransData transData) throws Iso8583Exception {
        try {
            byte[] byteQR = initGetQR(transData);
            byte[] byteTM = initTerminalInformation(transData);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(byteQR);
            outputStream.write(byteTM);
            //entity.setFieldValue("63", outputStream.toByteArray());
            setBitData("63", outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private byte[] initGetQR(TransData transData) throws IOException {
        String tableID = "QR";
        byte[] byteTableID = Tools.string2Bytes(tableID);

        byte[] byteTableVersion = new byte[]{(byte)0x01};

        String amount= Component.getPaddedString(transData.getAmount(), 12,'0');
        byte[] byteAmount = Tools.str2Bcd(amount);

        byte[] byteCurrentDateTime = Tools.str2Bcd(transData.getDateTime());

        ByteArrayOutputStream outputMessage = new ByteArrayOutputStream( );
        outputMessage.write(byteTableID);
        outputMessage.write(byteTableVersion);
        outputMessage.write(byteAmount);
        outputMessage.write(byteCurrentDateTime);
        byte byteMSG[] = outputMessage.toByteArray( );

        String lengthMSG = String.valueOf(Component.getPaddedNumber(outputMessage.size(),4));
        byte[] bytesLen = Tools.str2Bcd(lengthMSG);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(bytesLen);
        outputStream.write(byteMSG);

        return outputStream.toByteArray( );
    }

    private byte[] initTerminalInformation(TransData transData) throws IOException {
        String tableID = "TM";
        byte[] byteTableID = Tools.string2Bytes(tableID);
        byte[] byteTableVersion = new byte[]{(byte)0x01};
        byte[] byteSeparator = new byte[] {(byte)0x1f};
        byte[] byteCurrentDateTime = Tools.str2Bcd(transData.getDateTime());

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
        outputMessage.write(byteSeparator);
        byte byteMSG[] = outputMessage.toByteArray( );

        String lengthMSG = String.valueOf(Component.getPaddedNumber(outputMessage.size(),4));
        byte[] bytesLen = Tools.str2Bcd(lengthMSG);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(bytesLen);
        outputStream.write(byteMSG);

        return outputStream.toByteArray( );
    }

}
