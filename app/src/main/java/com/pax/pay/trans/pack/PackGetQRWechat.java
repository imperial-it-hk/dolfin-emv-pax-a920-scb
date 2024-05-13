package com.pax.pay.trans.pack;

import th.co.bkkps.utils.Log;
import androidx.annotation.NonNull;
import com.pax.abl.core.ipacker.PackListener;
import com.pax.appstore.DownloadManager;
import com.pax.eemv.utils.Tools;
import com.pax.gl.pack.IIso8583;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.TimeConverter;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by NANNAPHAT S on 27/12/2018.
 */

public class PackGetQRWechat extends PackIso8583 {

    private static final String TABLE_ID_BC = "BC";
    private static final String TABLE_VERSION_BC = "\01";//hex-01(Start of Heading:SOH)
    //private static final String TABLE_ID_QR = "QR";
    //private static final String TABLE_VERSION_QR = "\01";//hex-01(Start of Heading:SOH)

    public PackGetQRWechat(PackListener listener) {
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

        if(transData.getTransType() == ETransType.QR_INQUIRY_WECHAT){
            setBitData35(transData);
        }

        // field 41 TID
        setBitData41(transData);

        // field 42 MID
        setBitData42(transData);

        //setBitData60(transData);

        setBitData61(transData);

        setBitData62(transData);
        setBitData63(transData);
    }

    @Override
    protected void setBitData22(@NonNull TransData transData) throws Iso8583Exception {
        IIso8583.IIso8583Entity.IFieldAttrs iFieldAttrs22 = entity.createFieldAttrs().setPaddingPosition(IIso8583.IIso8583Entity.IFieldAttrs.EPaddingPosition.PADDING_LEFT);
        //entity.setFieldValue("22", "030").setFieldAttrs("22", iFieldAttrs22);
        setBitData("22","000",iFieldAttrs22);
    }

    @Override
    protected void setBitData35(@NonNull TransData transData) throws Iso8583Exception {
        IIso8583.IIso8583Entity.IFieldAttrs iFieldAttrs35 = entity.createFieldAttrs().setPaddingPosition(IIso8583.IIso8583Entity.IFieldAttrs.EPaddingPosition.PADDING_LEFT);
        setBitData("35","00",iFieldAttrs35);
    }

    @Override
    protected void setBitData61(@NonNull TransData transData) throws Iso8583Exception {
        int iMode = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_SUPPORT_REF1_2_MODE);
        DownloadManager.EdcRef1Ref2Mode mode = DownloadManager.EdcRef1Ref2Mode.getByMode(iMode);
        if (mode != DownloadManager.EdcRef1Ref2Mode.DISABLE) {
            byte[] f61 = PackGetQR.buildReqMsgRef1Ref2QrPayment(transData);
            setBitData("61", f61);
        } else {
            super.setBitData61(transData);
        }
    }

    @Override
    protected void setBitData63(@NonNull TransData transData) throws Iso8583Exception {
        //entity.setFieldValue("63", initBuyerCode(transData) + Component.initPrintText() + Component.initTerminalInformation());
        if(transData.getTransType() == ETransType.GET_QR_WECHAT){
            //setBitData("63", /*initBuyerCode(transData) + Component.initPrintText()+*/initGetQR(transData));
            //setBitData("63", initGetQR(transData));
            setBitData("63", Utils.safeConvertByteArrayToString(initGetQR(transData)));
        } else if (transData.getTransType() == ETransType.QR_INQUIRY_WECHAT){
            //setBitData("63", /*initBuyerCode(transData) + Component.initPrintText()+*/initGetQR(transData));
            //setBitData("63", initGetQR(transData));
            setBitData("63", Utils.safeConvertByteArrayToString(initGetQR(transData)));
        }
    }

    private String initBuyerCode(TransData transData){
        // buyer code.
        String qrBuyerCode = transData.getQrBuyerCode();
        String bcData = TABLE_ID_BC + TABLE_VERSION_BC + qrBuyerCode;
        String lengthbcDataAscii = Tools.hexToAscii(String.valueOf(Component.getPaddedNumber(Tools.string2Bytes(bcData).length, 4)));
        return lengthbcDataAscii + bcData;
    }

    //this function is for KBANK wallet Transaction.
    private byte[] initGetQR (TransData transData){
        //TODO: This is hardcode for test with KBANK server. Need to improve later.
        //Pack Bit 63

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        byte[] bytes;
// Currency (3 bytes)
        bytes = new byte[3];
        Arrays.fill( bytes, (byte) 0x20 );
        if (transData.getTransType() == ETransType.GET_QR_WECHAT){
            String Currency = java.util.Currency.getInstance(transData.getCurrency()).getCurrencyCode();
            outputStream.write(Currency.getBytes(), 0, bytes.length);
        } else if(transData.getTransType() == ETransType.QR_INQUIRY_WECHAT){
            String Currency = transData.getQRCurrency();
            outputStream.write(Currency.getBytes(),0,Currency.getBytes().length);
        }


// Partner Transaction ID (32 bytes)
        bytes = new byte[32];
        if(transData.getTransType() == ETransType.GET_QR_WECHAT){
            String DateTime = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS, Constants.TIME_PATTERN_TRANS2);
            String Random = randomNumber(6);
            String PartnerTxnID = DateTime + Component.getPaddedNumber(transData.getTraceNo(), 6) + Random + transData.getAcquirer().getTerminalId();
            transData.setWalletPartnerID(PartnerTxnID);
            //Arrays.fill( bytes, PartnerTxnID );
            outputStream.write(PartnerTxnID.getBytes(), 0,    PartnerTxnID.getBytes().length);
        } else if (transData.getTransType() == ETransType.QR_INQUIRY_WECHAT){
            String PartnerTxnID = transData.getWalletPartnerID();
            outputStream.write(PartnerTxnID.getBytes(), 0,    PartnerTxnID.getBytes().length);
        }

//  Transaction ID (64 bytes)
        bytes = new byte[64];
        if(transData.getTransType() == ETransType.GET_QR_WECHAT){
            Arrays.fill( bytes, (byte) 0x20 );
            outputStream.write(bytes, 0,    bytes.length);
        } else if(transData.getTransType() == ETransType.QR_INQUIRY_WECHAT){
            String TxnID = transData.getTxnID();
           // TxnID = padRight(TxnID,64);
            outputStream.write(TxnID.getBytes(), 0, TxnID.getBytes().length);
        }


//  Pay Time (16 bytes)
        bytes = new byte[16];
        if(transData.getTransType() == ETransType.GET_QR_WECHAT){
            Arrays.fill( bytes, (byte) 0x20 );
            outputStream.write(bytes, 0,    bytes.length);
        } else if (transData.getTransType() == ETransType.QR_INQUIRY_WECHAT){
            String PayTime = transData.getPayTime();
            outputStream.write(PayTime.getBytes(),0,PayTime.getBytes().length);
        }

// Exchange Rate (12 bytes)
        bytes = new byte[12];
        if(transData.getTransType() == ETransType.GET_QR_WECHAT){
            Arrays.fill( bytes, (byte) 0x20 );
            outputStream.write(bytes, 0,    bytes.length);
        } else if (transData.getTransType() == ETransType.QR_INQUIRY_WECHAT){
            String ExchRate = transData.getExchangeRate();
            outputStream.write(ExchRate.getBytes(),0,ExchRate.getBytes().length);
        }


// Transaction Amount CNY (12 bytes)
        bytes = new byte[12];
        if(transData.getTransType() == ETransType.GET_QR_WECHAT){
            Arrays.fill( bytes, (byte) 0x20 );
            outputStream.write(bytes, 0,    bytes.length);
        } else if (transData.getTransType() == ETransType.QR_INQUIRY_WECHAT){
            String TxnAmtCNY = transData.getAmountCNY();
            outputStream.write(TxnAmtCNY.getBytes(),0,TxnAmtCNY.getBytes().length);
        }

// Transaction Amount (12 bytes)
        bytes = new byte[12];
        if(transData.getTransType() == ETransType.GET_QR_WECHAT){
            Arrays.fill( bytes, (byte) 0x20 );
            outputStream.write(bytes, 0,    bytes.length);
        } else if (transData.getTransType() == ETransType.QR_INQUIRY_WECHAT){
//            String Amount = transData.getAmount();
//            Amount = Amount.replaceAll("0","");
//            Amount = Amount.trim();
//            String AddAmount = padRight(Amount,12);
            String Amount = transData.getTxnAmount();
            outputStream.write(Amount.getBytes(), 0, Amount.getBytes().length);
        }


// Buyer User ID (16 bytes)
        bytes = new byte[16];
        if(transData.getTransType() == ETransType.GET_QR_WECHAT){
            Arrays.fill( bytes, (byte) 0x20 );
            outputStream.write(bytes, 0,    bytes.length);
        } else if (transData.getTransType() == ETransType.QR_INQUIRY_WECHAT){
            String BuyerUserID = transData.getBuyerUserID();
            outputStream.write(BuyerUserID.getBytes(),0,BuyerUserID.getBytes().length);
        }


// Buyer Login ID (20 bytes)
        bytes = new byte[20];
        if(transData.getTransType() == ETransType.GET_QR_WECHAT){
            Arrays.fill( bytes, (byte) 0x20 );
            outputStream.write(bytes, 0,    bytes.length);
        } else if (transData.getTransType() == ETransType.QR_INQUIRY_WECHAT){
            String BuyerLoginID = transData.getBuyerLoginID();
            outputStream.write(BuyerLoginID.getBytes(),0,BuyerLoginID.getBytes().length);
        }


// Merchant Info (128 bytes)
        bytes = new byte[128];
        if(transData.getTransType() == ETransType.GET_QR_WECHAT){
            Arrays.fill( bytes, (byte) 0x20 );
            outputStream.write(bytes, 0,    bytes.length);
        } else if (transData.getTransType() == ETransType.QR_INQUIRY_WECHAT){
            String MerInfo = transData.getMerchantInfo();
            outputStream.write(MerInfo.getBytes(),0,MerInfo.getBytes().length);
        }


// Application Code (2 bytes)
        if(transData.getTransType() == ETransType.GET_QR_WECHAT){
            bytes = new byte[]{0x30, 0x32};
            //Arrays.fill( bytes, (byte) 0x20 );
            outputStream.write(bytes, 0,    bytes.length);
        } else if (transData.getTransType() == ETransType.QR_INQUIRY_WECHAT){
            String AppCode = transData.getAppCode();
            outputStream.write(AppCode.getBytes(),0,AppCode.getBytes().length);
        }


//  Promocode (24 bytes)
        bytes = new byte[24];
        if(transData.getTransType() == ETransType.GET_QR_WECHAT){
            Arrays.fill( bytes, (byte) 0x20 );
            outputStream.write(bytes, 0,    bytes.length);
        } else if(transData.getTransType() == ETransType.QR_INQUIRY_WECHAT){
            String Promocode = transData.getPromocode();
            outputStream.write(Promocode.getBytes(),0,Promocode.getBytes().length);
        }

// Transaction No (24 bytes)
        bytes = new byte[24];
        if(transData.getTransType() == ETransType.GET_QR_WECHAT){
            Arrays.fill( bytes, (byte) 0x20 );
            outputStream.write(bytes, 0,    bytes.length);
        } else if (transData.getTransType() == ETransType.QR_INQUIRY_WECHAT){
            String TxnNo = transData.getTxnNo();
            outputStream.write(TxnNo.getBytes(),0,TxnNo.getBytes().length);
        }


//  Fee (24 bytes)
        bytes = new byte[24];
        if(transData.getTransType() == ETransType.GET_QR_WECHAT){
            Arrays.fill( bytes, (byte) 0x20 );
            outputStream.write(bytes, 0,    bytes.length);
        } else if (transData.getTransType() == ETransType.QR_INQUIRY_WECHAT){
            String Fee = transData.getFee();
            outputStream.write(Fee.getBytes(),0,Fee.getBytes().length);
        }

// QR Code (400 bytes)
        bytes = new byte[400];
        Arrays.fill( bytes, (byte) 0x20 );
        outputStream.write(bytes, 0,    bytes.length);
        return outputStream.toByteArray();
    }

    private String randomNumber(int len ){
        final String AB = "0123456789"; //"0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder( len );
        for( int i = 0; i < len; i++ )
            sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );
        return sb.toString();
    }

    @Override
        protected int checkRecvData(@NonNull HashMap<String, byte[]> map, @NonNull TransData transData, boolean isCheckAmt) {

        return super.checkRecvData(map, transData, false);

        //return TransResult.SUCC;
    }
}
