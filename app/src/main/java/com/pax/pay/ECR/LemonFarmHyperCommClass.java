package com.pax.pay.ECR;

import androidx.annotation.NonNull;

import com.pax.edc.opensdk.TransResult;
import com.pax.pay.uart.CommManageClass;
import com.pax.pay.uart.ProtoFilterClass;
import com.pax.pay.utils.Convert;
import com.pax.pay.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import th.co.bkkps.utils.Log;

public class LemonFarmHyperCommClass extends HyperCommClass {

    private static final String TAG = "LemonFarmHyperCommClass";

    public static final byte[] TRANSACTION_CODE_SALE_ALL_CREDIT = new byte[]{'2', '0'};
    public static final byte[] TRANSACTION_CODE_VOID_TYPE = new byte[]{'2', '6'};
    public static final byte[] TRANSACTION_CODE_SALE_ALL_QR = new byte[]{'7', '0'};

    public static final byte[] FIELD_QR_TYPE = new byte[]{'A', '1'};        // Extra for LemonFarm & Lawson
    public static final byte[] FIELD_REFERENCE_SALE_ID_R1 = new byte[]{'R', '1'};        // Extra for LemonFarm & Lawson

    public LemonFarmHyperCommClass(CommManageClass CommManage, HyperCommInterface HyperCommCbk) {
        super(CommManage, HyperCommCbk);
    }

    public LemonFarmHyperCommClass(CommManageClass CommManage, ProtoFilterClass ProtoFilter, HyperCommInterface HyperCommCbk) {
        super(CommManage, ProtoFilter, HyperCommCbk);
    }

    public LemonFarmHyperCommClass() {
        super();
    }


    public static byte[] validateCommand() {
        byte[] respData = null;
        try {
            if (Arrays.equals(HyperComMsg.instance.transactionCode, LemonFarmHyperCommClass.TRANSACTION_CODE_SALE_ALL_CREDIT)) {
                if (HyperComMsg.instance.data_field_40_amount == null
                        || HyperComMsg.instance.data_field_R1_ref_saleID == null) {
                    respData = new byte[]{NACK};
                }
            } else if (Arrays.equals(HyperComMsg.instance.transactionCode, LemonFarmHyperCommClass.TRANSACTION_CODE_SALE_ALL_QR)) {
                if (HyperComMsg.instance.data_field_40_amount == null
                        || HyperComMsg.instance.data_field_R1_ref_saleID == null
                        || HyperComMsg.instance.data_field_A1_qr_type == null) {
                    respData = new byte[]{NACK};
                }
            } else if (Arrays.equals(HyperComMsg.instance.transactionCode, LemonFarmHyperCommClass.TRANSACTION_CODE_VOID_TYPE)) {
                if (HyperComMsg.instance.data_field_65_invoiceNo == null) {
                    respData = new byte[]{NACK};
                }
            } else {
                respData = new byte[]{NACK};
            }
        } catch (Exception ex) {
            Log.d(TAG, "Error during validateCommand in LemonFarmHyperCommClass");
            respData = new byte[]{NACK};
        }

        return respData;
    }

    @Override
    public int saleTransactionPack(@NonNull ByteArrayOutputStream pduBuffer, byte[] transType) {
        if (HyperComMsg.instance != null) {
            if (Arrays.equals(HyperComMsg.instance.transactionCode, LemonFarmHyperCommClass.TRANSACTION_CODE_SALE_ALL_CREDIT)) {
                return saleCreditLemonFarmTransactionPack(pduBuffer, transType);
            } else if (Arrays.equals(HyperComMsg.instance.transactionCode, LemonFarmHyperCommClass.TRANSACTION_CODE_SALE_ALL_QR)) {
                return saleQRLemonFarmTransactionPack(pduBuffer, transType);
            } else {
                return TransResult.ERR_ABORTED;
            }
        } else {
            return TransResult.ERR_ABORTED;
        }
    }

    private int saleCreditLemonFarmTransactionPack(@NonNull ByteArrayOutputStream pduBuffer, byte[] transType) {
        ByteArrayOutputStream tmpStream = new ByteArrayOutputStream();
        ByteArrayOutputStream msgBuffer = new ByteArrayOutputStream();
        //Pack Message Data
        TransportHeaderPack(msgBuffer, new byte[]{'6', '0'}, String.format("%04d", 0).getBytes(), String.format("%04d", 0).getBytes());
        Log.d("TRANHDR_GEN :", Convert.getInstance().bcdToStr(msgBuffer.toByteArray()));
        PresentationHeaderPack(msgBuffer, new byte[]{'1'}, new byte[]{'1'}, transType, EcrData.instance.RespCode, new byte[]{'0'});
        Log.d("PREHDR_GEN  :", Convert.getInstance().bcdToStr(msgBuffer.toByteArray()));

        //FieldType = "02", Response Text from host or EDC
        fieldDataElementPack(msgBuffer, FIELD_RESPONSE_TEXT_TYPE, EcrData.instance.HyperComRespText);
        //FieldType = "D0", Merchant Name and Address
        tmpStream.reset();
        tmpStream.write(EcrData.instance.MerName, 0, EcrData.instance.MerName.length);
        tmpStream.write(EcrData.instance.MerAddress, 0, EcrData.instance.MerAddress.length);
        tmpStream.write(EcrData.instance.MerAddress1, 0, EcrData.instance.MerAddress1.length);
        fieldDataElementPack(msgBuffer, FIELD_MERCHNT_NAME_AND_ADDR_TYPE, tmpStream.toByteArray());
        //FieldType = "03", Transaction Date
        fieldDataElementPack(msgBuffer, FIELD_TRANSACTION_DATE_TYPE, EcrData.instance.DateByte);
        //FieldType = "04", Transaction Time
        fieldDataElementPack(msgBuffer, FIELD_TRANSACTION_TIME_TYPE, EcrData.instance.TimeByte);
        //FieldType = "01", Approval Code
        fieldDataElementPack(msgBuffer, FIELD_APPROVAL_CODE_TYPE, EcrData.instance.ApprovalCode);
        //FieldType = "65", Invoice Number
        fieldDataElementPack(msgBuffer, FIELD_TRACE_INVOICE_NUMBER_TYPE, EcrData.instance.TraceNo);
        //FieldType = "16", Terminal Identification Number (TID)
        fieldDataElementPack(msgBuffer, FIELD_TERMINAL_ID_TYPE, EcrData.instance.TermID);
        //FieldType = "D1", Merchant Number (MID)
        fieldDataElementPack(msgBuffer, FIELD_MERCHNT_ID_TYPE, EcrData.instance.MerID);
        //FieldType = "D2", Card Issuer Name
        fieldDataElementPack(msgBuffer, FIELD_CARD_ISSUER_NAME_TYPE, EcrData.instance.CardIssuerName);
        //FieldType = "30", Card Number
        fieldDataElementPack(msgBuffer, FIELD_CARD_NUMBER_TYPE, EcrData.instance.HyperComCardNo);
        //FieldType = "31", Expiry Date
        fieldDataElementPack(msgBuffer, FIELD_EXPIRED_DATE_TYPE, EcrData.instance.ExpDate);
        //FieldType = "50", Batch Number
        fieldDataElementPack(msgBuffer, FIELD_BATCH_NUMBER_TYPE, EcrData.instance.BatchNo);
        //FieldType = "D3", Retrieval Reference
        fieldDataElementPack(msgBuffer, FIELD_REF_NUMBER_TYPE, EcrData.instance.RefNo);
        //FieldType = "D4", Card Issuer ID
        fieldDataElementPack(msgBuffer, FIELD_CARD_ISSUER_ID_TYPE, new byte[]{'1', '2'});
        //FieldType = "D5", Cardholder Name
        fieldDataElementPack(msgBuffer, FIELD_CARDHOLDER_NAME, Utils.getStringPadding("", 26, "X", Convert.EPaddingPosition.PADDING_LEFT).getBytes());
        //FieldType = "40", Transaction Amount
        fieldDataElementPack(msgBuffer, FIELD_AMOUNT_TRANSACTION_TYPE, EcrData.instance.transAmount);

        ProtocolPack(pduBuffer, msgBuffer);

        return 0;
    }

    private int saleQRLemonFarmTransactionPack(@NonNull ByteArrayOutputStream pduBuffer, byte[] transType) {
        ByteArrayOutputStream tmpStream = new ByteArrayOutputStream();
        ByteArrayOutputStream msgBuffer = new ByteArrayOutputStream();
        //Pack Message Data
        TransportHeaderPack(msgBuffer, new byte[]{'6', '0'}, String.format("%04d", 0).getBytes(), String.format("%04d", 0).getBytes());
        Log.d("TRANHDR_GEN :", Convert.getInstance().bcdToStr(msgBuffer.toByteArray()));
        PresentationHeaderPack(msgBuffer, new byte[]{'1'}, new byte[]{'1'}, transType, EcrData.instance.RespCode, new byte[]{'0'});
        Log.d("PREHDR_GEN  :", Convert.getInstance().bcdToStr(msgBuffer.toByteArray()));
        //FieldType = "02", Response Text from host or EDC
        fieldDataElementPack(msgBuffer, FIELD_RESPONSE_TEXT_TYPE, EcrData.instance.HyperComRespText);
        //FieldType = "40", Transaction Amount
        fieldDataElementPack(msgBuffer, FIELD_AMOUNT_TRANSACTION_TYPE, EcrData.instance.transAmount);
        //FieldType = "D0", Merchant Name and Address
        tmpStream.reset();
        tmpStream.write(EcrData.instance.MerName, 0, EcrData.instance.MerName.length);
        tmpStream.write(EcrData.instance.MerAddress, 0, EcrData.instance.MerAddress.length);
        tmpStream.write(EcrData.instance.MerAddress1, 0, EcrData.instance.MerAddress1.length);
        fieldDataElementPack(msgBuffer, FIELD_MERCHNT_NAME_AND_ADDR_TYPE, tmpStream.toByteArray());
        //FieldType = "03", Transaction Date
        fieldDataElementPack(msgBuffer, FIELD_TRANSACTION_DATE_TYPE, EcrData.instance.DateByte);
        //FieldType = "04", Transaction Time
        fieldDataElementPack(msgBuffer, FIELD_TRANSACTION_TIME_TYPE, EcrData.instance.TimeByte);
        //FieldType = "01", Approval Code
        fieldDataElementPack(msgBuffer, FIELD_APPROVAL_CODE_TYPE, EcrData.instance.ApprovalCode);
        //FieldType = "65", Invoice Number
        fieldDataElementPack(msgBuffer, FIELD_TRACE_INVOICE_NUMBER_TYPE, EcrData.instance.TraceNo);
        //FieldType = "16", Terminal Identification Number (TID)
        fieldDataElementPack(msgBuffer, FIELD_TERMINAL_ID_TYPE, EcrData.instance.TermID);
        //FieldType = "D1", Merchant Number (MID)
        fieldDataElementPack(msgBuffer, FIELD_MERCHNT_ID_TYPE, EcrData.instance.MerID);
        //FieldType = "D2", Card Issuer Name
        fieldDataElementPack(msgBuffer, FIELD_CARD_ISSUER_NAME_TYPE, EcrData.instance.CardIssuerName);
        //FieldType = "30", Card Number
        fieldDataElementPack(msgBuffer, FIELD_CARD_NUMBER_TYPE, EcrData.instance.HyperComCardNo);
        //FieldType = "31", Expiry Date
        fieldDataElementPack(msgBuffer, FIELD_EXPIRED_DATE_TYPE, EcrData.instance.ExpDate);
        //FieldType = "50", Batch Number
        fieldDataElementPack(msgBuffer, FIELD_BATCH_NUMBER_TYPE, EcrData.instance.BatchNo);
        //FieldType = "D3", Retrieval Reference
        fieldDataElementPack(msgBuffer, FIELD_REF_NUMBER_TYPE, EcrData.instance.RefNo);
        //FieldType = "D4", Card Issuer ID
        fieldDataElementPack(msgBuffer, FIELD_CARD_ISSUER_ID_TYPE, new byte[]{'1', '2'});
        //FieldType = "D5", Cardholder Name
        fieldDataElementPack(msgBuffer, FIELD_CARDHOLDER_NAME, Utils.getStringPadding("", 26, "X", Convert.EPaddingPosition.PADDING_LEFT).getBytes());

        ProtocolPack(pduBuffer, msgBuffer);

        return 0;
    }

    @Override
    public int voidTransactionPack(@NonNull ByteArrayOutputStream pduBuffer) {
        ByteArrayOutputStream tmpStream = new ByteArrayOutputStream();
        ByteArrayOutputStream msgBuffer = new ByteArrayOutputStream();
        //Pack Message Data
        TransportHeaderPack(msgBuffer, new byte[]{'6', '0'}, String.format("%04d", 0).getBytes(), String.format("%04d", 0).getBytes());
        Log.d("TRANHDR_GEN :", Convert.getInstance().bcdToStr(msgBuffer.toByteArray()));
        PresentationHeaderPack(msgBuffer, new byte[]{'1'}, new byte[]{'1'}, TRANSACTION_CODE_VOID_TYPE, EcrData.instance.RespCode, new byte[]{'0'});
        Log.d("PREHDR_GEN  :", Convert.getInstance().bcdToStr(msgBuffer.toByteArray()));
        //FieldType = "02", Response Text from host or EDC
        fieldDataElementPack(msgBuffer, FIELD_RESPONSE_TEXT_TYPE, EcrData.instance.HyperComRespText);
        //FieldType = "40", Transaction Amount
        fieldDataElementPack(msgBuffer, FIELD_AMOUNT_TRANSACTION_TYPE, EcrData.instance.transAmount);
        //FieldType = "D0", Merchant Name and Address
        tmpStream.reset();
        tmpStream.write(EcrData.instance.MerName, 0, EcrData.instance.MerName.length);
        tmpStream.write(EcrData.instance.MerAddress, 0, EcrData.instance.MerAddress.length);
        tmpStream.write(EcrData.instance.MerAddress1, 0, EcrData.instance.MerAddress1.length);
        fieldDataElementPack(msgBuffer, FIELD_MERCHNT_NAME_AND_ADDR_TYPE, tmpStream.toByteArray());
        //FieldType = "03", Transaction Date
        fieldDataElementPack(msgBuffer, FIELD_TRANSACTION_DATE_TYPE, EcrData.instance.DateByte);
        //FieldType = "04", Transaction Time
        fieldDataElementPack(msgBuffer, FIELD_TRANSACTION_TIME_TYPE, EcrData.instance.TimeByte);
        //FieldType = "01", Approval Code
        fieldDataElementPack(msgBuffer, FIELD_APPROVAL_CODE_TYPE, EcrData.instance.ApprovalCode);
        //FieldType = "65", Invoice Number
        fieldDataElementPack(msgBuffer, FIELD_TRACE_INVOICE_NUMBER_TYPE, EcrData.instance.TraceNo);
        //FieldType = "16", Terminal Identification Number (TID)
        fieldDataElementPack(msgBuffer, FIELD_TERMINAL_ID_TYPE, EcrData.instance.TermID);
        //FieldType = "D1", Merchant Number (MID)
        fieldDataElementPack(msgBuffer, FIELD_MERCHNT_ID_TYPE, EcrData.instance.MerID);
        //FieldType = "D2", Card Issuer Name
        fieldDataElementPack(msgBuffer, FIELD_CARD_ISSUER_NAME_TYPE, EcrData.instance.CardIssuerName);
        //FieldType = "30", Card Number
        fieldDataElementPack(msgBuffer, FIELD_CARD_NUMBER_TYPE, EcrData.instance.HyperComCardNo);
        //FieldType = "31", Expiry Date
        fieldDataElementPack(msgBuffer, FIELD_EXPIRED_DATE_TYPE, EcrData.instance.ExpDate);
        //FieldType = "50", Batch Number
        fieldDataElementPack(msgBuffer, FIELD_BATCH_NUMBER_TYPE, EcrData.instance.BatchNo);
        //FieldType = "D3", Retrieval Reference
        fieldDataElementPack(msgBuffer, FIELD_REF_NUMBER_TYPE, EcrData.instance.RefNo);
        //FieldType = "D4", Card Issuer ID
        fieldDataElementPack(msgBuffer, FIELD_CARD_ISSUER_ID_TYPE, new byte[]{'1', '2'});
        //FieldType = "D5", CARD HOLDERNAME
        fieldDataElementPack(msgBuffer, FIELD_CARDHOLDER_NAME, Utils.getStringPadding("", 26, "X", Convert.EPaddingPosition.PADDING_LEFT).getBytes());

        ProtocolPack(pduBuffer, msgBuffer);

        return 0;
    }
}
