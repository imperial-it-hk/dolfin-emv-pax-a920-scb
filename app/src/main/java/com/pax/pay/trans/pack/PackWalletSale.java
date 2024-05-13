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

public class PackWalletSale extends PackIso8583  {

    public PackWalletSale(PackListener listener) {
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
        } else {
            entity.setFieldValue("m", transType.getMsgType());
        }

        setBitData3(transData);
        setBitData4(transData);
        setBitData11(transData);

        setBitData22(transData);

        // field 24 NII
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
        setBitData("22","000",iFieldAttrs22);
    }

    @Override
    protected void setBitData63(@NonNull TransData transData) throws Iso8583Exception {
        try {
            byte[] byteQR = initQRTransInq(transData);
            byte[] byteTM = initTerminalInformation(transData);
            byte[] byteTX = initPrintText();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(byteQR);
            outputStream.write(byteTM);
            outputStream.write(byteTX);
            setBitData("63", outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private byte[] initQRTransInq(TransData transData) throws IOException {
        String tableID = "QI";
        byte[] byteTableID = Tools.string2Bytes(tableID);
        byte[] byteTableVersion = new byte[]{(byte)0x01};
        byte[] byteTransID = Tools.str2Bcd(transData.getRefNo());

        ByteArrayOutputStream outputMessage = new ByteArrayOutputStream( );
        outputMessage.write(byteTableID);
        outputMessage.write(byteTableVersion);
        outputMessage.write(byteTransID);
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

    private byte[] initPrintText() throws IOException {
        byte[] bTableID = Tools.string2Bytes("TX");
        byte[] bTableVersion = Tools.str2Bcd("02");
        byte[] bCharPerLine = Tools.str2Bcd("34");
        byte[] bMaxPrint = Tools.str2Bcd("0500");

        ByteArrayOutputStream outputMessage = new ByteArrayOutputStream( );
        outputMessage.write(bTableID);
        outputMessage.write(bTableVersion);
        outputMessage.write(bCharPerLine);
        outputMessage.write(bMaxPrint);

        byte byteMSG[] = outputMessage.toByteArray( );

        String lengthMSG = String.valueOf(Component.getPaddedNumber(outputMessage.size(),4));
        byte[] bytesLen = Tools.str2Bcd(lengthMSG);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(bytesLen);
        outputStream.write(byteMSG);

        return outputStream.toByteArray( );
    }

}

