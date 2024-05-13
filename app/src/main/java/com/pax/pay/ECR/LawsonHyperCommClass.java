package com.pax.pay.ECR;

import androidx.annotation.NonNull;

import com.pax.device.Device;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.utils.Tools;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.uart.CommManageClass;
import com.pax.pay.uart.ProtoFilterClass;
import com.pax.pay.utils.Convert;
import com.pax.pay.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashMap;

import th.co.bkkps.utils.Log;

import static com.pax.pay.trans.component.Component.transInit;

public class LawsonHyperCommClass extends HyperCommClass {

    private static final String TAG = "LawsonHyperCommClass";

    public static final byte[] TRANSACTION_CODE_SALE_ALL_CREDIT = new byte[]{'2', '0'};
    public static final byte[] TRANSACTION_CODE_VOID_TYPE = new byte[]{'2', '6'};
    public static final byte[] TRANSACTION_CODE_SALE_ALL_QR = new byte[]{'7', '0'};        // Extra for Lawson LinkPoS
    public static final byte[] TRANSACTION_CODE_POS_QR_INQUIRY = new byte[]{'7', '1'};        // Extra for Lawson LinkPoS
    public static final byte[] TRANSACTION_CODE_POS_QR_CANCEL = new byte[]{'7', '2'};        // Extra for Lawson LinkPoS
    public static final byte[] TRANSACTION_CODE_GET_PHONE_NUMBER = new byte[]{'9', '8'};        // Extra for Lawson LinkPoS

    public static final byte[] FIELD_MERCHANT_NUMBER = new byte[]{'4', '5'};        // Extra for Lawson LinkPoS
    public static final byte[] FIELD_QR_TYPE = new byte[]{'A', '1'};        // Extra for Lawson LinkPoS
    public static final byte[] FIELD_SIGNATURE_IMAGE_DATA = new byte[]{'F', 'G'};        // Extra for Lawson LinkPoS
    public static final byte[] FIELD_KIOSK_POS_AID = new byte[]{'F', 'H'};        // Extra for Lawson LinkPoS
    public static final byte[] FIELD_KIOSK_POS_TVR = new byte[]{'F', 'I'};        // Extra for Lawson LinkPoS
    public static final byte[] FIELD_KIOSK_POS_TSI = new byte[]{'F', 'J'};        // Extra for Lawson LinkPoS
    public static final byte[] FIELD_PRINT_SIGNATURE_BOX = new byte[]{'F', 'K'};        // Extra for Lawson LinkPoS
    public static final byte[] FIELD_HOST_INDEX = new byte[]{'H', 'I'};        // Extra for Lawson LinkPoS
    public static final byte[] FIELD_REFERENCE_SALE_ID_R0 = new byte[]{'R', '0'};        // Extra for Lawson LinkPoS
    public static final byte[] FIELD_PHONE_NUMBER = new byte[]{'R', '3'};        // Extra for Lawson LinkPoS

    // Extra field for Settlement Transaction
    public static final byte[] FIELD_SETTLE_ALL_HOST_BATCH_TOTAL = new byte[]{'Z', 'Y'};        // Extra for Lawson LinkPoS
    public static final byte[] FIELD_SETTLE_ALL_HOST_NII = new byte[]{'Z', 'Z'};        // Extra for Lawson LinkPoS

    // Extra field for AuditReport Transaction
    public static final byte[] FIELD_SINGLE_BATCH_TOTAL = new byte[]{'H', 'O'};        // Extra for Lawson LinkPoS

    public LawsonHyperCommClass(CommManageClass CommManage, HyperCommInterface HyperCommCbk) {
        super(CommManage, HyperCommCbk);
    }

    public LawsonHyperCommClass(CommManageClass CommManage, ProtoFilterClass ProtoFilter, HyperCommInterface HyperCommCbk) {
        super(CommManage, ProtoFilter, HyperCommCbk);
    }

    public LawsonHyperCommClass() {
        super();
    }


//    public static byte[] validateCommand() {
//        byte[] respData = null;
//        try {
//            // command '20'
//            if (Arrays.equals(HyperComMsg.instance.transactionCode, LawsonHyperCommClass.TRANSACTION_CODE_SALE_ALL_CREDIT)) {
//                if (HyperComMsg.instance.data_field_40_amount == null
//                        || HyperComMsg.instance.data_field_R0_ref_saleID == null) {
//                    respData = new byte[]{NACK};
//                }
//            }
//            // command '26'
//            else if (Arrays.equals(HyperComMsg.instance.transactionCode, LawsonHyperCommClass.TRANSACTION_CODE_VOID_TYPE)) {
//                if (HyperComMsg.instance.data_field_45_merchant_number == null
//                        || HyperComMsg.instance.data_field_65_invoiceNo == null) {
//                    respData = new byte[]{NACK};
//                }
//            }
//            // command '50'
//            else if (Arrays.equals(HyperComMsg.instance.transactionCode, LawsonHyperCommClass.TRANSACTION_CODE_SETTLEMENT_TYPE)) {
//                if (HyperComMsg.instance.data_field_45_merchant_number == null
//                        || HyperComMsg.instance.data_field_HN_nii == null) {
//                    respData = new byte[]{NACK};
//                }
//            }
//            // command '70'
//            else if (Arrays.equals(HyperComMsg.instance.transactionCode, LawsonHyperCommClass.TRANSACTION_CODE_SALE_ALL_QR)) {
//                if (HyperComMsg.instance.data_field_40_amount == null
//                        || HyperComMsg.instance.data_field_R0_ref_saleID == null) {
//                    respData = new byte[]{NACK};
//                }
//            }
//            // command '71'
//            else if (Arrays.equals(HyperComMsg.instance.transactionCode, LawsonHyperCommClass.TRANSACTION_CODE_POS_QR_INQUIRY)) {
//                // do nothing -- No validation required
//            }
//            // command '72'
//            else if (Arrays.equals(HyperComMsg.instance.transactionCode, LawsonHyperCommClass.TRANSACTION_CODE_POS_QR_CANCEL)) {
//                // do nothing -- No validation required
//            }
//            // command '91'
////            else if (Arrays.equals(HyperComMsg.instance.transactionCode, LawsonHyperCommClass.TRANSACTION_CODE_AUDIT_REPORT_TYPE)) {
////                if (HyperComMsg.instance.data_field_45_merchant_number == null
////                        || HyperComMsg.instance.data_field_HN_nii == null) {
////                    respData = new byte[]{NACK};
////                }
////            }
//            // command '98'
////            else if (Arrays.equals(HyperComMsg.instance.transactionCode, LawsonHyperCommClass.TRANSACTION_CODE_GET_PHONE_NUMBER)) {
////                if (HyperComMsg.instance.data_field_R3_phone_number == null) {
////                    respData = new byte[]{NACK};
////                }
////            }
//            else {
//                respData = new byte[]{NACK};
//            }
//        } catch (Exception ex) {
//            Log.d(TAG, "Error during validateCommand in LawsonFarmHyperCommClass");
//            respData = new byte[]{NACK};
//        }
//
//        return respData;
//    }

    @Override
    public int saleTransactionPack(@NonNull ByteArrayOutputStream pduBuffer, byte[] transType) {
        if (HyperComMsg.instance != null) {
            if (Arrays.equals(HyperComMsg.instance.transactionCode, LawsonHyperCommClass.TRANSACTION_CODE_SALE_ALL_CREDIT)) {
                return saleCreditTransactionPack(pduBuffer, transType);
            } else if (Arrays.equals(HyperComMsg.instance.transactionCode, LawsonHyperCommClass.TRANSACTION_CODE_SALE_ALL_QR)) {
                return saleQRTransactionPack(pduBuffer, transType);
            } else if (Arrays.equals(HyperComMsg.instance.transactionCode, LawsonHyperCommClass.TRANSACTION_CODE_POS_QR_INQUIRY)) {
                return saleQRInquiryTransactionPack(pduBuffer, transType);
            } else if (Arrays.equals(HyperComMsg.instance.transactionCode, LawsonHyperCommClass.TRANSACTION_CODE_POS_QR_CANCEL)) {
                return saleQRInquiryTransactionPack(pduBuffer, transType);
            } else {
                return TransResult.ERR_ABORTED;
            }
        } else {
            return TransResult.ERR_ABORTED;
        }
    }


    @Override
    public int rejectPack(@NonNull ByteArrayOutputStream pduBuffer, byte[] transType, byte[] respCode, byte[] respText) {
        if (Arrays.equals(HyperComMsg.instance.transactionCode, LawsonHyperCommClass.TRANSACTION_CODE_POS_QR_CANCEL)) {
            return QrPosManualPack(pduBuffer, transType, HyperCommClass.ECR_RESP_NO_DATA);
        } else {
            return this.rejectPackEx(pduBuffer, transType, respCode, respText);
        }
    }


    public int rejectPackEx(@NonNull ByteArrayOutputStream pduBuffer, byte[] transType, byte[] respCode, byte[] respText) {
        ByteArrayOutputStream tmpStream = new ByteArrayOutputStream();
        ByteArrayOutputStream msgBuffer = new ByteArrayOutputStream();

        //Pack Message Data
        TransportHeaderPack(msgBuffer, new byte[]{'6', '0'}, String.format("%04d", 0).getBytes(), String.format("%04d", 0).getBytes());
        Log.d("REJECT:", Convert.getInstance().bcdToStr(msgBuffer.toByteArray()));

        PresentationHeaderPack(msgBuffer, new byte[]{'1'}, new byte[]{'1'}, transType, respCode, new byte[]{'0'});
        Log.d("REJECT:", Convert.getInstance().bcdToStr(msgBuffer.toByteArray()));

        //FieldType = "02", Response Text from host or EDC
        //fieldDataElementPack(msgBuffer, FIELD_RESPONSE_TEXT_TYPE, EcrData.instance.HyperComRespText);

        //FieldType = "D0", Merchant Name and Address
        //tmpStream.reset();
        //tmpStream.write(EcrData.instance.MerName, 0 , EcrData.instance.MerName.length);
        //tmpStream.write(EcrData.instance.MerAddress, 0 , EcrData.instance.MerAddress.length);
        //tmpStream.write(EcrData.instance.MerAddress1, 0 , EcrData.instance.MerAddress1.length);
        //fieldDataElementPack(msgBuffer, FIELD_MERCHNT_NAME_AND_ADDR_TYPE, tmpStream.toByteArray());

        //FieldType = "03", Transaction Date
        //fieldDataElementPack(msgBuffer,  FIELD_TRANSACTION_DATE_TYPE, EcrData.instance.DateByte);

        //FieldType = "04", Transaction Time
        //fieldDataElementPack(msgBuffer,  FIELD_TRANSACTION_TIME_TYPE,  EcrData.instance.TimeByte);

        ProtocolPack(pduBuffer, msgBuffer);
        return 0;
    }

    @Override
    public int settlementTransactionPack(@NonNull ByteArrayOutputStream pduBuffer, String HostID) {
        if (HyperComMsg.instance.data_field_HN_nii.equals("999")) {
            return settlementPack(pduBuffer);
        } else {
            return 0;
        }
    }

    private int saleCreditTransactionPack(@NonNull ByteArrayOutputStream pduBuffer, byte[] transType) {
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
        //FieldType = "D5", Cardholder Name                                                                             **
        fieldDataElementPack(msgBuffer, FIELD_CARDHOLDER_NAME, Utils.getStringPadding("", 26, "X", Convert.EPaddingPosition.PADDING_LEFT));
        //FieldType = "FG", SIGNATURE  IMAGE                                                                            **
        fieldDataElementPack(msgBuffer, FIELD_SIGNATURE_IMAGE_DATA, EcrData.instance.signatureImgData, true);
        //FieldType = "FH", Kiosk/PoS print AID                                                                         **
        fieldDataElementPack(msgBuffer, FIELD_KIOSK_POS_AID, EcrData.instance.kioskPos_AID, true);
        //FieldType = "FI", Kiosk/PoS print TVR                                                                         **
        fieldDataElementPack(msgBuffer, FIELD_KIOSK_POS_TVR, EcrData.instance.kioskPos_TVR, true);
        //FieldType = "FJ", Kiosk/PoS print TSI                                                                         **
        fieldDataElementPack(msgBuffer, FIELD_KIOSK_POS_TSI, EcrData.instance.kioskPos_TSI, true);
        //FieldType = "FK", Print Signature Box                                                                         **
        fieldDataElementPack(msgBuffer, FIELD_PRINT_SIGNATURE_BOX, EcrData.instance.kioskPos_PrintSignatureBox, true);
        //FieldType = "R0", Host index                                                                                  **
        fieldDataElementPack(msgBuffer, FIELD_REFERENCE_SALE_ID_R0, EcrData.instance.saleReferenceIDR0);
        //FieldType = "40", Transaction Amount
        fieldDataElementPack(msgBuffer, FIELD_AMOUNT_TRANSACTION_TYPE, EcrData.instance.transAmount);
        //FieldType = "HI", Host index                                                                                  **
        fieldDataElementPack(msgBuffer, FIELD_HOST_INDEX, EcrData.instance.hostIndex);

        ProtocolPack(pduBuffer, msgBuffer);

        return 0;
    }

    private int saleQRTransactionPack(@NonNull ByteArrayOutputStream pduBuffer, byte[] transType) {
        ByteArrayOutputStream tmpStream = new ByteArrayOutputStream();
        ByteArrayOutputStream msgBuffer = new ByteArrayOutputStream();
        //Pack Message Data
        TransportHeaderPack(msgBuffer, new byte[]{'6', '0'}, String.format("%04d", 0).getBytes(), String.format("%04d", 0).getBytes());
        Log.d("TRANHDR_GEN :", Convert.getInstance().bcdToStr(msgBuffer.toByteArray()));
        PresentationHeaderPack(msgBuffer, new byte[]{'1'}, new byte[]{'1'}, transType, EcrData.instance.RespCode, new byte[]{'0'});
        Log.d("PREHDR_GEN  :", Convert.getInstance().bcdToStr(msgBuffer.toByteArray()));
        //FieldType = "02", Response Text from host or EDC
        String hyperComRespText = null;
        try{
            hyperComRespText = "APPROVAL       " + new String(EcrData.instance.ApprovalCode,"UTF-8") + "                   ";
            fieldDataElementPack(msgBuffer, FIELD_RESPONSE_TEXT_TYPE, hyperComRespText.getBytes());
        } catch (Exception ex) {
            fieldDataElementPack(msgBuffer, FIELD_RESPONSE_TEXT_TYPE, EcrData.instance.HyperComRespText);
        }

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
        fieldDataElementPack(msgBuffer, FIELD_CARD_ISSUER_NAME_TYPE, EcrData.instance.qr_IssuerName);
         //FieldType = "30", Card Number
        //fieldDataElementPack(msgBuffer,  FIELD_CARD_NUMBER_TYPE, EcrData.instance.HyperComCardNo);
        //FieldType = "31", Expiry Date
        fieldDataElementPack(msgBuffer, FIELD_EXPIRED_DATE_TYPE, "0000".getBytes());
        //FieldType = "50", Batch Number
        fieldDataElementPack(msgBuffer, FIELD_BATCH_NUMBER_TYPE, EcrData.instance.BatchNo);
        //FieldType = "D3", Retrieval Reference
        fieldDataElementPack(msgBuffer, FIELD_REF_NUMBER_TYPE, EcrData.instance.RefNo);
        //FieldType = "D4", Card Issuer ID
        fieldDataElementPack(msgBuffer, FIELD_CARD_ISSUER_ID_TYPE, new byte[]{'1', '2'});
        //FieldType = "D5", Cardholder Name                                                                             **
        fieldDataElementPack(msgBuffer, FIELD_CARDHOLDER_NAME, Utils.getStringPadding("", 26, "X", Convert.EPaddingPosition.PADDING_LEFT));
        //FieldType = "FG", SIGNATURE  IMAGE                                                                            **
        //fieldDataElementPack(msgBuffer, FIELD_SIGNATURE_IMAGE_DATA, EcrData.instance.SignatureImgData);
        //FieldType = "FH", Kiosk/PoS print AID                                                                         **
        //fieldDataElementPack(msgBuffer, FIELD_KIOSK_POS_AID, EcrData.instance.kioskPos_AID);
        //FieldType = "FI", Kiosk/PoS print TVR                                                                         **
        //fieldDataElementPack(msgBuffer, FIELD_KIOSK_POS_TVR, EcrData.instance.kioskPos_TVR);
        //FieldType = "FJ", Kiosk/PoS print TSI                                                                         **
        //fieldDataElementPack(msgBuffer, FIELD_KIOSK_POS_TSI, EcrData.instance.kioskPos_TSI);
        //FieldType = "FK", Print Signature Box                                                                         **
        //fieldDataElementPack(msgBuffer, FIELD_PRINT_SIGNATURE_BOX, EcrData.instance.kioskPos_PrintSignatureBox);
        //FieldType = "R0", Host index                                                                                  **
        fieldDataElementPack(msgBuffer, FIELD_REFERENCE_SALE_ID_R0, EcrData.instance.saleReferenceIDR0);
        //FieldType = "40", Transaction Amount
        fieldDataElementPack(msgBuffer, FIELD_AMOUNT_TRANSACTION_TYPE, EcrData.instance.transAmount);
        //FieldType = "A1", Wallet Type                                                                                 **
        fieldDataElementPack(msgBuffer, FIELD_QR_TYPE, EcrData.instance.walletType);
        //FieldType = "HI", Host index                                                                                  **
        fieldDataElementPack(msgBuffer, FIELD_HOST_INDEX, EcrData.instance.qr_hostIndex);

        Log.d(TAG, Tools.bcd2Str(msgBuffer.toByteArray()));

        ProtocolPack(pduBuffer, msgBuffer);

        return 0;
    }

    private int saleQRInquiryTransactionPack(@NonNull ByteArrayOutputStream pduBuffer, byte[] transType) {
        ByteArrayOutputStream tmpStream = new ByteArrayOutputStream();
        ByteArrayOutputStream msgBuffer = new ByteArrayOutputStream();
        //Pack Message Data
        TransportHeaderPack(msgBuffer, new byte[]{'6', '0'}, String.format("%04d", 0).getBytes(), String.format("%04d", 0).getBytes());
        Log.d("TRANHDR_GEN :", Convert.getInstance().bcdToStr(msgBuffer.toByteArray()));
        PresentationHeaderPack(msgBuffer, new byte[]{'1'}, new byte[]{'1'}, transType, EcrData.instance.RespCode, new byte[]{'0'});
        Log.d("PREHDR_GEN  :", Convert.getInstance().bcdToStr(msgBuffer.toByteArray()));
        //FieldType = "02", Response Text from host or EDC
        String hyperComRespText = null;
        try{
            hyperComRespText = "APPROVAL       " + new String(EcrData.instance.ApprovalCode,"UTF-8") + "                   ";
            fieldDataElementPack(msgBuffer, FIELD_RESPONSE_TEXT_TYPE, hyperComRespText.getBytes());
        } catch (Exception ex) {
            fieldDataElementPack(msgBuffer, FIELD_RESPONSE_TEXT_TYPE, EcrData.instance.HyperComRespText);
        }
        //fieldDataElementPack(msgBuffer, FIELD_RESPONSE_TEXT_TYPE, EcrData.instance.HyperComRespText);
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
        fieldDataElementPack(msgBuffer, FIELD_CARD_ISSUER_NAME_TYPE, EcrData.instance.qr_IssuerName);
        //FieldType = "30", Card Number
        //fieldDataElementPack(msgBuffer, FIELD_CARD_NUMBER_TYPE, Utils.getStringPadding("", 19, "0", Convert.EPaddingPosition.PADDING_LEFT).getBytes());
        fieldDataElementPack(msgBuffer, FIELD_CARD_NUMBER_TYPE, EcrData.instance.qr_TransID);
        //FieldType = "31", Expiry Date
        fieldDataElementPack(msgBuffer, FIELD_EXPIRED_DATE_TYPE, "0000".getBytes());
        //FieldType = "50", Batch Number
        fieldDataElementPack(msgBuffer, FIELD_BATCH_NUMBER_TYPE, EcrData.instance.BatchNo);
        //FieldType = "D3", Retrieval ReferenceTEST10000010
        fieldDataElementPack(msgBuffer, FIELD_REF_NUMBER_TYPE, EcrData.instance.RefNo);
        //FieldType = "D4", Card Issuer ID
        fieldDataElementPack(msgBuffer, FIELD_CARD_ISSUER_ID_TYPE, new byte[]{'1', '2'});
        //FieldType = "D5", Cardholder Name                                                                             **
        fieldDataElementPack(msgBuffer, FIELD_CARDHOLDER_NAME, Utils.getStringPadding("", 26, "X", Convert.EPaddingPosition.PADDING_LEFT).getBytes());
        //FieldType = "FG", SIGNATURE  IMAGE                                                                            **
        fieldDataElementPack(msgBuffer, FIELD_SIGNATURE_IMAGE_DATA, new byte[0], true);
        //FieldType = "FH", Kiosk/PoS print AID                                                                         **
        fieldDataElementPack(msgBuffer, FIELD_KIOSK_POS_AID, new byte[0], true);
        //FieldType = "FI", Kiosk/PoS print TVR                                                                         **
        fieldDataElementPack(msgBuffer, FIELD_KIOSK_POS_TVR, new byte[0], true);
        //FieldType = "FJ", Kiosk/PoS print TSI                                                                         **
        fieldDataElementPack(msgBuffer, FIELD_KIOSK_POS_TSI, new byte[0], true);
        //FieldType = "FK", Print Signature Box                                                                         **
        fieldDataElementPack(msgBuffer, FIELD_PRINT_SIGNATURE_BOX, "1".getBytes(), true);
        //FieldType = "R0", Host index                                                                                  **
        fieldDataElementPack(msgBuffer, FIELD_REFERENCE_SALE_ID_R0, EcrData.instance.saleReferenceIDR0);
        //FieldType = "40", Transaction Amount
        fieldDataElementPack(msgBuffer, FIELD_AMOUNT_TRANSACTION_TYPE, EcrData.instance.transAmount);
        //FieldType = "A1", Wallet Type                                                                                 **
        //fieldDataElementPack(msgBuffer, FIELD_QR_TYPE, EcrData.instance.WalletType);
        //FieldType = "HI", Host index                                                                                  **
        fieldDataElementPack(msgBuffer, FIELD_HOST_INDEX, EcrData.instance.qr_hostIndex);

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
        //FieldType = "FG", SIGNATURE  IMAGE                                                                            **
        fieldDataElementPack(msgBuffer, FIELD_SIGNATURE_IMAGE_DATA, EcrData.instance.signatureImgData, true);
        //FieldType = "FH", Kiosk/PoS print AID                                                                         **
        fieldDataElementPack(msgBuffer, FIELD_KIOSK_POS_AID, EcrData.instance.kioskPos_AID, true);
        //FieldType = "FI", Kiosk/PoS print TVR                                                                         **
        fieldDataElementPack(msgBuffer, FIELD_KIOSK_POS_TVR, EcrData.instance.kioskPos_TVR, true);
        //FieldType = "FJ", Kiosk/PoS print TSI                                                                         **
        fieldDataElementPack(msgBuffer, FIELD_KIOSK_POS_TSI, EcrData.instance.kioskPos_TSI, true);
        //FieldType = "FK", Print Signature Box                                                                         **
        fieldDataElementPack(msgBuffer, FIELD_PRINT_SIGNATURE_BOX, EcrData.instance.kioskPos_PrintSignatureBox, true);
        //FieldType = "R0", Host index                                                                                  **
        fieldDataElementPack(msgBuffer, FIELD_REFERENCE_SALE_ID_R0, EcrData.instance.saleReferenceIDR0);
        //FieldType = "40", Transaction Amount
        fieldDataElementPack(msgBuffer, FIELD_AMOUNT_TRANSACTION_TYPE, EcrData.instance.transAmount);
        //FieldType = "HI", Host index                                                                                  **
        fieldDataElementPack(msgBuffer, FIELD_HOST_INDEX, EcrData.instance.hostIndex);

        ProtocolPack(pduBuffer, msgBuffer);

        return 0;
    }


    public int QrPosManualPack(@NonNull ByteArrayOutputStream pduBuffer, byte[] transType, byte[] respCode) {
        ByteArrayOutputStream msgBuffer = new ByteArrayOutputStream();

        //Pack Message Data
        TransportHeaderPack(msgBuffer, new byte[]{'6', '0'}, String.format("%04d", 0).getBytes(), String.format("%04d", 0).getBytes());
        Log.d("REJECT:", Convert.getInstance().bcdToStr(msgBuffer.toByteArray()));

        PresentationHeaderPack(msgBuffer, new byte[]{'1'}, new byte[]{'1'}, transType, respCode, new byte[]{'0'});
        Log.d("REJECT:", Convert.getInstance().bcdToStr(msgBuffer.toByteArray()));

        ProtocolPack(pduBuffer, msgBuffer);
        return 0;
    }


    public int settlementPack(@NonNull ByteArrayOutputStream pduBuffer) {
        ByteArrayOutputStream msgBuffer = new ByteArrayOutputStream();

        //Pack Message Data
        TransportHeaderPack(msgBuffer, new byte[]{'6', '0'}, String.format("%04d", 0).getBytes(), String.format("%04d", 0).getBytes());
        Log.d("TRANHDR_GEN :", Convert.getInstance().bcdToStr(msgBuffer.toByteArray()));

        PresentationHeaderPack(msgBuffer, new byte[]{'1'}, new byte[]{'1'}, TRANSACTION_CODE_SETTLEMENT_TYPE, EcrData.instance.RespCode, new byte[]{'0'});
        Log.d("PREHDR_GEN  :", Convert.getInstance().bcdToStr(msgBuffer.toByteArray()));

        //FieldType = "ZY", Batch Total
        fieldDataElementPack(msgBuffer, FIELD_SETTLE_ALL_HOST_BATCH_TOTAL, EcrData.instance.settleBatchTotal);
        //FieldType = "ZZ",Settlement NII
        fieldDataElementPack(msgBuffer, FIELD_SETTLE_ALL_HOST_NII, EcrData.instance.settleNII);

        ProtocolPack(pduBuffer, msgBuffer);

        return 0;
    }

    @Override
    public int auditReportTransactionPack(@NonNull ByteArrayOutputStream pduBuffer, String HostID) {
        ByteArrayOutputStream tmpStream = new ByteArrayOutputStream();
        ByteArrayOutputStream msgBuffer = new ByteArrayOutputStream();

        //Pack Message Data
        TransportHeaderPack(msgBuffer, new byte[]{'6', '0'}, String.format("%04d", 0).getBytes(), String.format("%04d", 0).getBytes());
        Log.d("TRANHDR_GEN :", Convert.getInstance().bcdToStr(msgBuffer.toByteArray()));

        byte[] tmpRespCode = null;
        if (EcrData.instance.singleBatchTotal != null) {
            if (EcrData.instance.singleBatchTotal.length > 0) {
                tmpRespCode = "00".getBytes();
            }
        }
        if (tmpRespCode == null) {
            tmpRespCode = "ND".getBytes();
        }

        PresentationHeaderPack(msgBuffer, new byte[]{'1'}, new byte[]{'1'}, TRANSACTION_CODE_AUDIT_REPORT_TYPE, tmpRespCode, new byte[]{'0'});
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
        fieldDataElementPack(msgBuffer, FIELD_TRANSACTION_DATE_TYPE, Device.getTime("yyMMdd"));
        //FieldType = "04", Transaction Time
        fieldDataElementPack(msgBuffer, FIELD_TRANSACTION_TIME_TYPE, Device.getTime("HHmmss"));
        //FieldType = "50", Batch Number
        fieldDataElementPack(msgBuffer, FIELD_BATCH_NUMBER_TYPE, "000000".getBytes());
        //FieldType = "16", Terminal Identification Number (TID)
        fieldDataElementPack(msgBuffer, FIELD_TERMINAL_ID_TYPE, "00000000".getBytes());
        //FieldType = "D1", Merchant Number (MID)
        fieldDataElementPack(msgBuffer, FIELD_MERCHNT_ID_TYPE, "000000000000000".getBytes());
        //FieldType = "HO", single Batch Total
        fieldDataElementPack(msgBuffer, FIELD_SINGLE_BATCH_TOTAL, EcrData.instance.singleBatchTotal);
        //FieldType = "HN", single Host NII
        fieldDataElementPack(msgBuffer, FIELD_TRANSACTION_DATE_TYPE, EcrData.instance.singleHostNII);

        ProtocolPack(pduBuffer, msgBuffer);

        return 0;
    }

    public static String getLawsonHostIndex(Acquirer acquirer) {
        String hostindex = null;
        switch (acquirer.getName()) {
            case Constants.ACQ_KBANK:
                hostindex = "001";
                break;
            case Constants.ACQ_DCC:
                hostindex = "002";
                break;
            case Constants.ACQ_SMRTPAY:
                hostindex = "003";
                break;
            case Constants.ACQ_REDEEM:
                hostindex = "004";
                break;
            case Constants.ACQ_UP:
                hostindex = "005";
                break;
            case Constants.ACQ_AMEX:
                hostindex = "006";
                break;
            case "DINER":
                hostindex = "007";
                break;
            case "NERA":
                hostindex = "008";
                break;
            case "BBL":
                hostindex = "009";
                break;
            case Constants.ACQ_ALIPAY:
                hostindex = "010";
                break;
            case Constants.ACQ_WECHAT:
                hostindex = "011";
                break;
            case Constants.ACQ_KPLUS:
                hostindex = "012";
                break;
            case Constants.ACQ_QR_CREDIT:
                hostindex = "013";
                break;
            default:
                break;
        }

        return hostindex;
    }

    public static String getLawsonHostName(String index) {
        if (index == null) {
            return null;
        }
        String hostName = null;
        switch (index) {
            case "001":
                hostName = Constants.ACQ_KBANK;
                break;
            case "002":
                hostName = Constants.ACQ_DCC;
                break;
            case "003":
                hostName = Constants.ACQ_SMRTPAY;
                break;
            case "004":
                hostName = Constants.ACQ_REDEEM;
                break;
            case "005":
                hostName = Constants.ACQ_UP;
                break;
            case "006":
                hostName = Constants.ACQ_AMEX;
                break;
            case "007":
                hostName = "DINER";
                break;
            case "008":
                hostName = "NERA";
                break;
            case "009":
                hostName = "BBL";
                break;
            case "010":
                hostName = Constants.ACQ_ALIPAY;
                break;
            case "011":
                hostName = Constants.ACQ_WECHAT;
                break;
            case "012":
                hostName = Constants.ACQ_KPLUS;
                break;
            case "013":
                hostName = Constants.ACQ_QR_CREDIT;
                break;
            default:
                break;
        }
        return hostName;
    }
}
