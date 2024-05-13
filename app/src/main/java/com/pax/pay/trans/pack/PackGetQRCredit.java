package com.pax.pay.trans.pack;

import androidx.annotation.NonNull;

import com.pax.abl.core.ipacker.PackListener;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.TimeConverter;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import th.co.bkkps.utils.Log;

public class PackGetQRCredit extends PackGetQR {
    public PackGetQRCredit(PackListener listener) {
        super(listener);
    }

    @NonNull
    @Override
    public byte[] pack(@NonNull TransData transData) {
        try {
            setMandatoryData(transData);
            if(transData.getTransType() == ETransType.QR_INQUIRY_CREDIT){
                setBitData35(transData);
            }

            return pack(false,transData);

        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
        return "".getBytes();
    }

    @Override
    protected void setBitData63(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("63", initGetQR(transData));
    }

    //this function is for KBANK wallet Transaction.
    private byte[] initGetQR (TransData transData){
        //TODO: This is hardcode for test with KBANK server. Need to improve later.
        //Pack Bit 63

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        byte[] bytes;
// Currency (3 bytes)
        bytes = new byte[3];
        if (transData.getTransType() == ETransType.GET_QR_CREDIT){
            Arrays.fill( bytes, (byte) 0x20 );
            outputStream.write(bytes, 0,    bytes.length);
        } else if(transData.getTransType() == ETransType.QR_INQUIRY_CREDIT){
            String Currency = transData.getQRCurrency();
            outputStream.write(Currency.getBytes(),0,Currency.getBytes().length);
        }


// Partner Transaction ID (32 bytes)
        bytes = new byte[32];
        if(transData.getTransType() == ETransType.GET_QR_CREDIT){
            String DateTime = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS, Constants.TIME_PATTERN_TRANS2);
            String Random = randomNumber(12);
            String PartnerTxnID = DateTime+Random+transData.getAcquirer().getTerminalId();
            transData.setWalletPartnerID(PartnerTxnID);
            //Arrays.fill( bytes, PartnerTxnID );
            outputStream.write(PartnerTxnID.getBytes(), 0,    PartnerTxnID.getBytes().length);
        } else if (transData.getTransType() == ETransType.QR_INQUIRY_CREDIT){
            String PartnerTxnID = transData.getWalletPartnerID();
            outputStream.write(PartnerTxnID.getBytes(), 0,    PartnerTxnID.getBytes().length);
        }

//  Transaction ID (64 bytes)
        bytes = new byte[64];
        if(transData.getTransType() == ETransType.GET_QR_CREDIT){
            Arrays.fill( bytes, (byte) 0x20 );
            outputStream.write(bytes, 0,    bytes.length);
        } else if(transData.getTransType() == ETransType.QR_INQUIRY_CREDIT){
            String TxnID = transData.getTxnID();
            // TxnID = padRight(TxnID,64);
            outputStream.write(TxnID.getBytes(), 0, TxnID.getBytes().length);
        }


//  Pay Time (16 bytes)
        bytes = new byte[16];
        if(transData.getTransType() == ETransType.GET_QR_CREDIT){
            Arrays.fill( bytes, (byte) 0x20 );
            outputStream.write(bytes, 0,    bytes.length);
        } else if (transData.getTransType() == ETransType.QR_INQUIRY_CREDIT){
            String PayTime = transData.getPayTime();
            outputStream.write(PayTime.getBytes(),0,PayTime.getBytes().length);
        }

// Exchange Rate (12 bytes)
        bytes = new byte[12];
        if(transData.getTransType() == ETransType.GET_QR_CREDIT){
            Arrays.fill( bytes, (byte) 0x20 );
            outputStream.write(bytes, 0,    bytes.length);
        } else if (transData.getTransType() == ETransType.QR_INQUIRY_CREDIT){
            String ExchRate = transData.getExchangeRate();
            outputStream.write(ExchRate.getBytes(),0,ExchRate.getBytes().length);
        }


// Transaction Amount CNY (12 bytes)
        bytes = new byte[12];
        if(transData.getTransType() == ETransType.GET_QR_CREDIT){
            Arrays.fill( bytes, (byte) 0x20 );
            outputStream.write(bytes, 0,    bytes.length);
        } else if (transData.getTransType() == ETransType.QR_INQUIRY_CREDIT){
            String TxnAmtCNY = transData.getAmountCNY();
            outputStream.write(TxnAmtCNY.getBytes(),0,TxnAmtCNY.getBytes().length);
        }

// Transaction Amount (12 bytes)
        bytes = new byte[12];
        if(transData.getTransType() == ETransType.GET_QR_CREDIT){
            Arrays.fill( bytes, (byte) 0x20 );
            outputStream.write(bytes, 0,    bytes.length);
        } else if (transData.getTransType() == ETransType.QR_INQUIRY_CREDIT){
//            String Amount = transData.getAmount();
//            Amount = Amount.replaceAll("0","");
//            Amount = Amount.trim();
//            String AddAmount = padRight(Amount,12);
            String Amount = transData.getTxnAmount();
            outputStream.write(Amount.getBytes(), 0, Amount.getBytes().length);
        }


// Buyer User ID (16 bytes)
        bytes = new byte[16];
        if(transData.getTransType() == ETransType.GET_QR_CREDIT){
            Arrays.fill( bytes, (byte) 0x20 );
            outputStream.write(bytes, 0,    bytes.length);
        } else if (transData.getTransType() == ETransType.QR_INQUIRY_CREDIT){
            String BuyerUserID = transData.getBuyerUserID();
            outputStream.write(BuyerUserID.getBytes(),0,BuyerUserID.getBytes().length);
        }


// Buyer Login ID (20 bytes)
        bytes = new byte[20];
        if(transData.getTransType() == ETransType.GET_QR_CREDIT){
            Arrays.fill( bytes, (byte) 0x20 );
            outputStream.write(bytes, 0,    bytes.length);
        } else if (transData.getTransType() == ETransType.QR_INQUIRY_CREDIT){
            String BuyerLoginID = transData.getBuyerLoginID();
            outputStream.write(BuyerLoginID.getBytes(),0,BuyerLoginID.getBytes().length);
        }


// Merchant Info (128 bytes)
        bytes = new byte[128];
        if(transData.getTransType() == ETransType.GET_QR_CREDIT){
            Arrays.fill( bytes, (byte) 0x20 );
            outputStream.write(bytes, 0,    bytes.length);
        } else if (transData.getTransType() == ETransType.QR_INQUIRY_CREDIT){
            String MerInfo = transData.getMerchantInfo();
            outputStream.write(MerInfo.getBytes(),0,MerInfo.getBytes().length);
        }


// Application Code (2 bytes)
        if(transData.getTransType() == ETransType.GET_QR_CREDIT){
            bytes = new byte[]{0x30, 0x34};
            //Arrays.fill( bytes, (byte) 0x20 );
            outputStream.write(bytes, 0,    bytes.length);
        } else if (transData.getTransType() == ETransType.QR_INQUIRY_CREDIT){
            String AppCode = transData.getAppCode();
            outputStream.write(AppCode.getBytes(),0,AppCode.getBytes().length);
        }


//  Promocode (24 bytes)
        bytes = new byte[24];
        if(transData.getTransType() == ETransType.GET_QR_CREDIT){
            Arrays.fill( bytes, (byte) 0x20 );
            outputStream.write(bytes, 0,    bytes.length);
        } else if(transData.getTransType() == ETransType.QR_INQUIRY_CREDIT){
            String Promocode = transData.getPromocode();
            outputStream.write(Promocode.getBytes(),0,Promocode.getBytes().length);
        }

// Transaction No (24 bytes)
        bytes = new byte[24];
        if(transData.getTransType() == ETransType.GET_QR_CREDIT){
            Arrays.fill( bytes, (byte) 0x20 );
            outputStream.write(bytes, 0,    bytes.length);
        } else if (transData.getTransType() == ETransType.QR_INQUIRY_CREDIT){
            String TxnNo = transData.getTxnNo();
            outputStream.write(TxnNo.getBytes(),0,TxnNo.getBytes().length);
        }


//  Fee (24 bytes)
        bytes = new byte[24];
        if(transData.getTransType() == ETransType.GET_QR_CREDIT){
            Arrays.fill( bytes, (byte) 0x20 );
            outputStream.write(bytes, 0,    bytes.length);
        } else if (transData.getTransType() == ETransType.QR_INQUIRY_CREDIT){
            String Fee = transData.getFee();
            outputStream.write(Fee.getBytes(),0,Fee.getBytes().length);
        }

// QR Code (400 bytes)
        bytes = new byte[400];
        Arrays.fill( bytes, (byte) 0x20 );
        outputStream.write(bytes, 0,    bytes.length);

        return outputStream.toByteArray();
    }
}
