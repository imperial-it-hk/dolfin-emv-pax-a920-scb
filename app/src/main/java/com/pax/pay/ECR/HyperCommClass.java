package com.pax.pay.ECR;

import android.os.CountDownTimer;

import androidx.annotation.NonNull;

import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.trans.EReceiptStatusTrans;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.uart.CommManageClass;
import com.pax.pay.uart.CommManageInterface;
import com.pax.pay.uart.ProtoFilterClass;
import com.pax.pay.utils.Checksum;
import com.pax.pay.utils.Convert;
import com.pax.pay.utils.EReceiptUtils;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashMap;

import th.co.bkkps.utils.Log;


public class HyperCommClass {

    protected ProtoFilterClass mProtoFilter;

    public enum PAXCommState {
        SOF, LEN, MSG, EOF, LRC, WAIT_ACK
    }

    public enum fieldDataUnpackState {
        TYPE, LEN, DATA, SEP
    }

    protected PAXCommState mPAXCommState;
    protected fieldDataUnpackState mfieldDataUnpackState;

    public volatile int runFlag = 0;
    public volatile int exitFlag = 0;
    public int sndTry = 0;

    protected int i_tmp;
    protected byte[] tmpBuffer = new byte[1024];
    int tmpLen;

    protected byte[] msgBuffer = new byte[1024];
    int msgLen;
    byte msgLRC;

    protected TransportClass mTransport;
    protected PresentationClass mPresentation;

    protected HyperCommInterface mHyperCommCbk;
    protected CommManageClass mCommManage = null;

//    private CountDownTimer rcvTimeout = null;
//    private CountDownTimer sndTimeout = null;

    protected static final byte ACK = 0x06;
    protected static final byte NACK = 0x15;
    protected final byte STX = 0x02;
    protected final byte ETX = 0x03;
    protected final byte FIELD_SEPARATOR = 0x1C;
    protected final byte[] TRAN_HDR = new byte[]{0x36, 0x30};

    protected boolean isSendPDUPendingFlag = false;
    protected boolean isBaseReadyFlag = true;
    protected boolean isRcvTimeoutON = false;
    protected boolean isSndTimeoutON = false;

    protected ByteArrayOutputStream pduBufferBackup = null;


    public class TransportClass {
        public int destAddr;
        public int srcAddr;

        public TransportClass() {
            this.destAddr = 0;
            this.srcAddr = 0;
        }
    }

    public class PresentationClass {
        public byte formatVer;
        public byte reqResIndicator;
        public byte[] transactionCode = new byte[2];
        public byte[] responseCode = new byte[2];
        public byte moreIndicator;

        public PresentationClass() {
            this.formatVer = 0;
            this.reqResIndicator = 0;

            this.transactionCode[0] = 0;
            this.transactionCode[1] = 0;

            this.responseCode[0] = 0;
            this.responseCode[1] = 0;
            this.moreIndicator = 0;
        }
    }

    public static final byte[] TRANSACTION_CODE_SALE_ALL_TYPE = new byte[]{'2', '0'};
    public static final byte[] TRANSACTION_CODE_VOID_TYPE = new byte[]{'2', '6'};
    public static final byte[] TRANSACTION_CODE_SETTLEMENT_TYPE = new byte[]{'5', '0'};
    public static final byte[] TRANSACTION_CODE_SALE_CREDIT_TYPE = new byte[]{'5', '6'};
    public static final byte[] TRANSACTION_CODE_SALE_RABBIT_TYPE = new byte[]{'5', '7'};
    public static final byte[] TRANSACTION_CODE_SALE_QR_TYPE = new byte[]{'6', '2'};
    public static final byte[] TRANSACTION_CODE_SALE_WALLET_TYPE = new byte[]{'6', '3'};
    public static final byte[] TRANSACTION_CODE_QR_VISA_TYPE = new byte[]{'6', '4'};
    public static final byte[] TRANSACTION_CODE_WECHAT_CSB_TYPE = new byte[]{'6', '6'};
    public static final byte[] TRANSACTION_CODE_ALIPAY_CSB_TYPE = new byte[]{'6', '7'};
    public static final byte[] TRANSACTION_CODE_AUDIT_REPORT_TYPE = new byte[]{'9', '1'};
    public static final byte[] TRANSACTION_CODE_TEST_COMMUNICATION_TYPE = new byte[]{'D', '0'};
    public static final byte[] TRANSACTION_CODE_SALE_DOLFIN_TYPE = new byte[]{'2', '0'};
    public static final byte[] TRANSACTION_CODE_DOLFIN_TYPE = new byte[]{'Q', '1'};

    public static final byte[] FIELD_NULL_TYPE = new byte[]{'0', '0'};
    public static final byte[] FIELD_APPROVAL_CODE_TYPE = new byte[]{'0', '1'};
    public static final byte[] FIELD_RESPONSE_TEXT_TYPE = new byte[]{'0', '2'};
    public static final byte[] FIELD_TRANSACTION_DATE_TYPE = new byte[]{'0', '3'};
    public static final byte[] FIELD_TRANSACTION_TIME_TYPE = new byte[]{'0', '4'};
    public static final byte[] FIELD_MERCHANT_NUMBER_TYPE = new byte[]{'0', '6'};
    public static final byte[] FIELD_TERMINAL_ID_TYPE = new byte[]{'1', '6'};
    public static final byte[] FIELD_CARD_NUMBER_TYPE = new byte[]{'3', '0'};
    public static final byte[] FIELD_EXPIRED_DATE_TYPE = new byte[]{'3', '1'};
    public static final byte[] FIELD_AMOUNT_TRANSACTION_TYPE = new byte[]{'4', '0'};
    public static final byte[] FIELD_AMOUNT_TIP_TYPE = new byte[]{'4', '1'};
    public static final byte[] FIELD_AMOUNT_CASH_BACK_TYPE = new byte[]{'4', '2'};
    public static final byte[] FIELD_AMOUNT_TAX_TYPE = new byte[]{'4', '3'};
    public static final byte[] FIELD_AMOUNT_BALANCE_TYPE = new byte[]{'4', '4'};
    public static final byte[] FIELD_AMOUNT_BALN_POS_NEG_TYPE = new byte[]{'4', '5'};
    public static final byte[] FIELD_BATCH_NUMBER_TYPE = new byte[]{'5', '0'};
    public static final byte[] FIELD_TRACE_INVOICE_NUMBER_TYPE = new byte[]{'6', '5'};
    public static final byte[] FIELD_MERCHNT_NAME_AND_ADDR_TYPE = new byte[]{'D', '0'};
    public static final byte[] FIELD_MERCHNT_ID_TYPE = new byte[]{'D', '1'};
    public static final byte[] FIELD_CARD_ISSUER_NAME_TYPE = new byte[]{'D', '2'};
    public static final byte[] FIELD_REF_NUMBER_TYPE = new byte[]{'D', '3'};
    public static final byte[] FIELD_CARD_ISSUER_ID_TYPE = new byte[]{'D', '4'};
    public static final byte[] FIELD_CARDHOLDER_NAME = new byte[]{'D', '5'};
    public static final byte[] FIELD_ADDITIONAL_1 = new byte[]{'D', '6'};
    public static final byte[] FIELD_ADDITIONAL_2 = new byte[]{'D', '7'};
    public static final byte[] FIELD_ADDITIONAL_3 = new byte[]{'D', '8'};
    public static final byte[] FIELD_BATCH_TOTAL_SALES_COUNT_TYPE = new byte[]{'H', '1'};
    public static final byte[] FIELD_BATCH_TOTAL_SALES_AMOUNT_TYPE = new byte[]{'H', '2'};
    public static final byte[] FIELD_NII_TYPE = new byte[]{'H', 'N'};


    public static final byte[] ECR_RESP_USER_CANCEL = new byte[]{0x00, 0x00};        // NULL NULL
    public static final byte[] ECR_RESP_NO_DATA = new byte[]{0x4E, 0x44};        // ND
    public static final byte[] ECR_RESP_EDC_REJECT = new byte[]{0x52, 0x42};        // RB
    public static final byte[] ECR_RESP_SUCCESS = new byte[]{0x30, 0x30};        // 00


    protected CountDownTimer rcvTimeout = new CountDownTimer(2000, 100) {
        public void onTick(long millisUntilFinished) {
            isRcvTimeoutON = true;
        }

        public void onFinish() {
            if (mCommManage.MainIO != null) mCommManage.MainIO.Write(new byte[]{NACK});
            isRcvTimeoutON = false;
            mPAXCommState = PAXCommState.SOF;
            Log.d("HyperComm:", "RX TIMEOUT OCCUR.....");
        }
    };

    protected CountDownTimer sndTimeout = new CountDownTimer(2000, 100) {
        public void onTick(long millisUntilFinished) {
            isSndTimeoutON = true;
        }

        public void onFinish() {
            mPAXCommState = PAXCommState.SOF;
            isSndTimeoutON = false;
            Log.d("HyperComm:", "TX TIMEOUT OCCUR....." + sndTry);
        }
    };

    public HyperCommClass(CommManageClass CommManage, HyperCommInterface HyperCommCbk) {
        this.mTransport = new TransportClass();
        this.mPresentation = new PresentationClass();

        this.mPAXCommState = PAXCommState.SOF;
        this.mfieldDataUnpackState = fieldDataUnpackState.TYPE;

        this.mHyperCommCbk = HyperCommCbk;

        this.mCommManage = CommManage;

        AddReceiveListener(CommManage);

        this.i_tmp = 0;
        this.exitFlag = 0;

        rcvTimeout.cancel();
        sndTimeout.cancel();
    }

    public HyperCommClass(CommManageClass CommManage, ProtoFilterClass ProtoFilter, HyperCommInterface HyperCommCbk) {
        this.mTransport = new TransportClass();
        this.mPresentation = new PresentationClass();

        this.mPAXCommState = PAXCommState.SOF;
        this.mfieldDataUnpackState = fieldDataUnpackState.TYPE;

        this.mHyperCommCbk = HyperCommCbk;

        this.mCommManage = CommManage;

        AddReceiveListener(ProtoFilter);

        this.i_tmp = 0;
        this.exitFlag = 0;
        rcvTimeout.cancel();
        sndTimeout.cancel();
    }

    public HyperCommClass() {

    }

    public void initial(CommManageClass CommManage, ProtoFilterClass ProtoFilter, HyperCommInterface HyperCommCbk) {
        this.mTransport = new TransportClass();
        this.mPresentation = new PresentationClass();

        this.mPAXCommState = PAXCommState.SOF;
        this.mfieldDataUnpackState = fieldDataUnpackState.TYPE;

        this.mHyperCommCbk = HyperCommCbk;

        this.mCommManage = CommManage;

        AddReceiveListener(ProtoFilter);

        this.i_tmp = 0;
        this.exitFlag = 0;
        rcvTimeout.cancel();
        sndTimeout.cancel();
    }

    public void AddReceiveListener(CommManageClass CommManage) {
        if (CommManage == null) return;

        mCommManage = CommManage;
        mCommManage.AddReceiveListener(new CommManageInterface() {
            public int onReceive(byte[] data, int len) {
                if (mCommManage != null) mCommManage.StopReceive();
                Log.d("onReceive:", "HyperComm Data = " + Convert.getInstance().bcdToStr(data));
                Log.d("onReceive:", "HyperComm Begin State = " + mPAXCommState.toString());
                return ProtocolUnpack(data, len);
            }
        });

    }

    public void AddReceiveListener(ProtoFilterClass ProtoFilter) {
        if (ProtoFilter == null) return;

        mProtoFilter = ProtoFilter;

        mProtoFilter.AddHyperComListener(new CommManageInterface() {
            public int onReceive(byte[] data, int len) {
                if (mCommManage != null) mCommManage.StopReceive();
                //Log.d("HyperComm:", "mPAXCommState = " + PAXCommState.values().toString());

                return ProtocolUnpack(data, len);
            }
        });

    }

    public void onBaseConnect() {
        isBaseReadyFlag = true;
        if (isSendPDUPendingFlag) {
            isSendPDUPendingFlag = false;
            sendPduPending();
        }
        mPAXCommState = PAXCommState.SOF;
    }

    public void onBaseDisconnect() {
        isBaseReadyFlag = false;
    }


    public int ProtocolUnpack(byte[] data, int len) {

        // Enhanced internal ECR_BSS_LOGGING :
        //bss_ecr_logging(pSTATE_RECV_MSG_POS_TO_EDC, pProtocol_ECR_HYP, data);

        if (mPAXCommState != PAXCommState.WAIT_ACK
                && data[0] == ACK) {
            return -1;
        }

        for (int i_cnt = 0; i_cnt < len; i_cnt++) {
            switch (mPAXCommState) {
                case SOF:
                    if (data[i_cnt] == STX) {
                        i_tmp = 0;
                        msgLRC = 0;
                        mPAXCommState = PAXCommState.LEN;
                        //  Log.d("HyperComm:", "SOF_PASS");
                        rcvTimeout.start();
                        isRcvTimeoutON = true;
                    } else {
                        // Log.d("HyperComm:", "SOF................: " +  String.format("%02X", data[i_cnt]));
                    }
                    break;

                case LEN:

                    if (!isRcvTimeoutON) rcvTimeout.start();

                    msgBuffer[i_tmp++] = data[i_cnt];
                    msgLRC ^= data[i_cnt];
                    if (i_tmp == 2) {
                        i_tmp = 0;
                        msgLen = (((msgBuffer[0] & 0xFF) >> 4) * 1000);//
                        msgLen += ((msgBuffer[0] & 0xF) * 100);//
                        msgLen += (((msgBuffer[1] & 0xFF) >> 4) * 10);//
                        msgLen += ((msgBuffer[1] & 0xF));//

                        if ((msgLen >= 1024) || (msgLen < 15)) {
                            if (mCommManage != null) mCommManage.ClearBuffer();
                            rcvTimeout.cancel();
                            mPAXCommState = PAXCommState.SOF;
                            // if (mCommManage.MainIO != null) mCommManage.MainIO.Write(new byte[]{NACK});
                            //      Log.d("HyperComm:", "LEN_FAIL = " + msgLen);
                        } else {

                            //      Log.d("HyperComm:", "LEN_PASS = " + msgLen);
                            mPAXCommState = PAXCommState.MSG;
                        }
                    } else {
                        //Log.d("HyperComm:", "LEN..............");
                    }

                    if (!isRcvTimeoutON) rcvTimeout.start();

                    break;

                case MSG:

                    if (!isRcvTimeoutON) rcvTimeout.start();

                    msgBuffer[i_tmp++] = data[i_cnt];
                    msgLRC ^= data[i_cnt];

                    if (i_tmp == msgLen) {
                        //     Log.d("HyperComm:", "MSG_PASS");
                        mPAXCommState = PAXCommState.EOF;
                    } else {
                        //Log.d("HyperComm:", "MSG............");
                    }

                    break;

                case EOF:
                    msgLRC ^= data[i_cnt];

                    if (data[i_cnt] == ETX) {
                        //   Log.d("HyperComm:", "EOF_PASS");
                        mPAXCommState = PAXCommState.LRC;
                    } else {
                        if (mCommManage != null) mCommManage.ClearBuffer();
                        rcvTimeout.cancel();
                        mPAXCommState = PAXCommState.SOF;
                        //if (mCommManage.MainIO != null) mCommManage.MainIO.Write(new byte[]{NACK});
                        //Log.d("HyperComm:", "EOF.............");
                    }

                    if (!isRcvTimeoutON) rcvTimeout.start();

                    break;

                case LRC:

                    if (!isRcvTimeoutON) rcvTimeout.start();

                    if (mCommManage != null) mCommManage.ClearBuffer();
                    rcvTimeout.cancel();
                    mPAXCommState = PAXCommState.SOF;

                    if (msgLRC == data[i_cnt]) {
                        // Log.d("HyperComm:", "LRC_PASS");
                        if (!EcrData.instance.isOnProcessing) {
                            if (mCommManage.MainIO != null) {
                                byte[] respData = new byte[]{ACK};
                                ;
//                                try {
//                                    if (FinancialApplication.getEcrProcess().mHyperComm instanceof LemonFarmHyperCommClass) {
//                                        respData = LemonFarmHyperCommClass.validateCommand();
//                                    } else if (FinancialApplication.getEcrProcess().mHyperComm instanceof LawsonHyperCommClass) {
//                                        respData = LawsonHyperCommClass.validateCommand();
//                                    } else {
//                                        respData = new byte[]{ACK};
//                                    }
//                                } catch (Exception ex) {
//                                    // do nothing
//                                }

                                mCommManage.MainIO.Write(respData);


                                // Extend ECR-EDC-RDR
                                //bss_ecr_logging(pSTATE_SEND_MSG_EDC_TO_POS,pProtocol_ECR_HYP,new byte[]{ACK});
                            }


                            if (mCommManage != null) mCommManage.StopReceive();
                            transportUnpack(Arrays.copyOfRange(msgBuffer, 0, 10));
                            presentationUnpack(Arrays.copyOfRange(msgBuffer, 10, 18));
                            fieldDataUnpack(Arrays.copyOfRange(msgBuffer, 18, msgLen));
                            // Log.d("onReceive:", "End State = " + mPAXCommState.toString());
                        }

                        return 1;

                    } else {
                        // if (mCommManage.MainIO != null) mCommManage.MainIO.Write(new byte[]{NACK});
                        //  Log.d("HyperComm:", "LRC_FAIL, calLRC = " + msgLRC + " rcvLRC = " + data[i_cnt]);
                    }

                case WAIT_ACK:

                    if (!isSndTimeoutON) sndTimeout.start();

                    if (data[i_cnt] == ACK) {
                        mPAXCommState = PAXCommState.SOF;
                        sndTry = 0;
                        sndTimeout.cancel();
                        //mCommManage.StartReceive();
                        //  Log.d("HyperComm:", "WAIT_ACK_PASS");

                        // return 1;
                    } else ///if (data[i_cnt] == NACK)
                    {
                        sndTry++;
                        if (sndTry >= 3) {
                            sndTry = 0;
                            sndTimeout.cancel();
                            mPAXCommState = PAXCommState.SOF;
                            mCommManage.StartReceive();
                        } else {
                            sndTimeout.cancel();
                            sndTimeout.start();
                        }
                        //    Log.d("HyperComm:", "WAIT_ACK.............");
                    }
                    break;
                default:
                    mPAXCommState = PAXCommState.SOF;
                    break;
            }

        }
        //Log.d("onReceive:", "End State = " + mPAXCommState.toString());
        return 0;
    }

    protected int transportUnpack(byte[] data) {
        // Log.d("HyperCommClass : ", Convert.getInstance().bcdToStr(data));
        if ((data[0] == TRAN_HDR[0]) && (data[1] == TRAN_HDR[1])) {
            mTransport.destAddr = (((int) data[2] - 0x30) * 1000) +
                    ((data[3] - 0x30) * 100) +
                    ((data[4] - 0x30) * 10) +
                    ((data[5] - 0x30));

            mTransport.srcAddr = (((int) data[6] - 0x30) * 1000) +
                    ((data[7] - 0x30) * 100) +
                    ((data[8] - 0x30) * 10) +
                    ((data[9] - 0x30));
            //           Log.d("HyperCommClass : ", "SRC_ADDR = " + mTransport.srcAddr + ", DEST_ADDR = " + mTransport.destAddr);
            mHyperCommCbk.onTransportHeaderRcv(Arrays.copyOfRange(data, 0, 2), mTransport.destAddr, mTransport.srcAddr);
            return 0;
        }

        return -1;
    }

    protected int presentationUnpack(byte[] data) {
        //Log.d("HyperCommClass : ", Convert.getInstance().bcdToStr(data));
        mPresentation.formatVer = (byte) (data[0] - 0x30);
        mPresentation.reqResIndicator = (byte) (data[1] - 0x30);

        mPresentation.transactionCode[0] = (byte) data[2];
        mPresentation.transactionCode[1] = (byte) data[3];

        mPresentation.responseCode[0] = (byte) (data[4] - 0x30);
        mPresentation.responseCode[1] = (byte) (data[5] - 0x30);

        mPresentation.moreIndicator = (byte) (data[6] - 0x30);

        mHyperCommCbk.onPresentationHeaderRcv(mPresentation.formatVer, mPresentation.reqResIndicator, mPresentation.transactionCode, mPresentation.responseCode, mPresentation.moreIndicator);

//        Log.d("HyperCommClass : ", "formatVer = " + mPresentation.formatVer +
//                ", reqResIndicator = " + mPresentation.reqResIndicator +
//                ", transactionCode = " + mPresentation.transactionCode +
//                ", responseCode[0] = " + mPresentation.responseCode[0] +
//                ", responseCode[1] = " + mPresentation.responseCode[1] +
//                ", moreIndicator = " + mPresentation.moreIndicator
//        );
        //data[7] == 0x1C;
        return 1;
    }

    protected int fieldDataUnpack(byte[] data) {
        Log.d("HyperCommClass : ", Convert.getInstance().bcdToStr(data));
        Log.d("HyperComm:", "ALL_LEN = " + data.length);
        byte[] fieldType = new byte[2];
        int dataLength = 0;
        byte[] data_tmp = null;

        mfieldDataUnpackState = fieldDataUnpackState.TYPE;
        i_tmp = 0;

        for (int i_cnt = 0; i_cnt < data.length; i_cnt++) {
            switch (mfieldDataUnpackState) {
                case TYPE:

                    fieldType[i_tmp++] = data[i_cnt];
                    if (i_tmp == 2) {
                        i_tmp = 0;
                        mfieldDataUnpackState = fieldDataUnpackState.LEN;
                        Log.d("HyperComm:", "FD_TYPE_PASS");
                    }
                    break;

                case LEN:
                    tmpBuffer[i_tmp++] = data[i_cnt];

                    if (i_tmp == 2) {

                        //data length ASCII Decode
                        if (((tmpBuffer[0] & 0x30) == 0x30) && ((tmpBuffer[1] & 0x30) == 0x30)) {
                            dataLength = (tmpBuffer[0] - 0x30) * 10;
                            dataLength += tmpBuffer[1] - 0x30;
                        } else {//data length BCD Decode
                            dataLength = (((tmpBuffer[0] & 0xFF) >> 4) * 1000);//
                            dataLength += ((tmpBuffer[0] & 0xF) * 100);//
                            dataLength += (((tmpBuffer[1] & 0xFF) >> 4) * 10);//
                            dataLength += ((tmpBuffer[1] & 0xF));//
                            //i_tmp = 0
                        }

                        if (dataLength >= 1024) {
                            return -1;
                        }
                        data_tmp = new byte[dataLength];

                        i_tmp = 0;

                        Log.d("HyperComm:", "FD_LEN_PASS = " + dataLength);
                        mfieldDataUnpackState = fieldDataUnpackState.DATA;
                    }
                    break;

                case DATA:

                    data_tmp[i_tmp++] = data[i_cnt];
                    if (i_tmp == dataLength) {
                        Log.d("HyperComm:", "FD_DATA_PASS");
                        mfieldDataUnpackState = fieldDataUnpackState.SEP;
                    }
                    if (i_tmp > dataLength) {
                        return -1;
                    }
                    break;

                case SEP:
                    if (data[i_cnt] == FIELD_SEPARATOR) {
                        Log.d("HyperCommClass :", "FD_DATA ");
                        mHyperCommCbk.onFieldDataRcv(fieldType, data_tmp);

                    }
                    i_tmp = 0;
                    mfieldDataUnpackState = fieldDataUnpackState.TYPE;
                    break;

                default:
                    mfieldDataUnpackState = fieldDataUnpackState.TYPE;
                    break;
            }

        }

        mHyperCommCbk.onRcvCmpt();

        return 0;
    }

    public int saleTransactionPack(@NonNull ByteArrayOutputStream pduBuffer, byte[] transType) {
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
        fieldDataElementPack(msgBuffer, FIELD_CARD_ISSUER_ID_TYPE, EcrData.instance.CardIssuerID);

//
//        //FieldType ="R1", Rabbit Reader Id
//        fieldDataElementPack(msgBuffer,  FIELD_RABBIT_READER_ID_TYPE, EcrData.instance.RabbitID);
//
//        //FieldType = "R2", Rabbit Trace
//        fieldDataElementPack(msgBuffer,  FIELD_RABBIT_TRACE_ID_TYPE, EcrData.instance.RabbitTrace);
//
//        //FieldType = "44", Balance Amount
//        fieldDataElementPack(msgBuffer,  FIELD_AMOUNT_BALANCE_TYPE, EcrData.instance.RabbitBalanceAmount);
//

        ProtocolPack(pduBuffer, msgBuffer);

        return 0;
    }

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
        fieldDataElementPack(msgBuffer, FIELD_CARD_ISSUER_ID_TYPE, EcrData.instance.CardIssuerID);


        ProtocolPack(pduBuffer, msgBuffer);

        return 0;
    }


    public int settlementTransactionPack(@NonNull ByteArrayOutputStream pduBuffer, String HostID) {
        ByteArrayOutputStream tmpStream = new ByteArrayOutputStream();
        ByteArrayOutputStream msgBuffer = new ByteArrayOutputStream();

        //Pack Message Data
        TransportHeaderPack(msgBuffer, new byte[]{'6', '0'}, String.format("%04d", 0).getBytes(), String.format("%04d", 0).getBytes());
        Log.d("TRANHDR_GEN :", Convert.getInstance().bcdToStr(msgBuffer.toByteArray()));

        PresentationHeaderPack(msgBuffer, new byte[]{'1'}, new byte[]{'1'}, TRANSACTION_CODE_SETTLEMENT_TYPE, EcrData.instance.RespCode, new byte[]{'0'});
        Log.d("PREHDR_GEN  :", Convert.getInstance().bcdToStr(msgBuffer.toByteArray()));

        //FieldType = "02", Response Text from host or EDC
        fieldDataElementPack(msgBuffer, FIELD_RESPONSE_TEXT_TYPE, EcrData.instance.HyperComRespText);

        //FieldType = "D0", Merchant Name and Address
        tmpStream.reset();
        tmpStream.write(EcrData.instance.MerName, 0, EcrData.instance.MerName.length);
        tmpStream.write(EcrData.instance.MerAddress, 0, EcrData.instance.MerAddress.length);
        tmpStream.write(EcrData.instance.MerAddress1, 0, EcrData.instance.MerAddress1.length);
        fieldDataElementPack(msgBuffer, FIELD_MERCHNT_NAME_AND_ADDR_TYPE, tmpStream.toByteArray());

        //FieldType = "16", Terminal Identification Number (TID)
        fieldDataElementPack(msgBuffer, FIELD_TERMINAL_ID_TYPE, EcrData.instance.TermID);

        //FieldType = "D1", Merchant Number (MID)
        fieldDataElementPack(msgBuffer, FIELD_MERCHNT_ID_TYPE, EcrData.instance.MerID);

        //FieldType = "50", Batch Number
        fieldDataElementPack(msgBuffer, FIELD_BATCH_NUMBER_TYPE, EcrData.instance.BatchNo);

        //FieldType = "03", Transaction Date
        fieldDataElementPack(msgBuffer, FIELD_TRANSACTION_DATE_TYPE, EcrData.instance.DateByte);

        //FieldType = "04", Transaction Time
        fieldDataElementPack(msgBuffer, FIELD_TRANSACTION_TIME_TYPE, EcrData.instance.TimeByte);

        //FieldType = 0x48 0x4E, HN-NII
        fieldDataElementPack(msgBuffer, FIELD_NII_TYPE, EcrData.instance.HYPER_COM_HN_NII);

        //FieldType = "H1"-Batch total sales count
        fieldDataElementPack(msgBuffer, FIELD_BATCH_TOTAL_SALES_COUNT_TYPE, EcrData.instance.BatchTotalSalesCount);

        //FieldType = "H2"-Batch total sales amount
        fieldDataElementPack(msgBuffer, FIELD_BATCH_TOTAL_SALES_AMOUNT_TYPE, EcrData.instance.BatchTotalSalesAmount);

        ProtocolPack(pduBuffer, msgBuffer);

        return 0;
    }

    public int auditReportTransactionPack(@NonNull ByteArrayOutputStream pduBuffer, String HostID) {
        ByteArrayOutputStream tmpStream = new ByteArrayOutputStream();
        ByteArrayOutputStream msgBuffer = new ByteArrayOutputStream();

        //Pack Message Data
        TransportHeaderPack(msgBuffer, new byte[]{'6', '0'}, String.format("%04d", 0).getBytes(), String.format("%04d", 0).getBytes());
        Log.d("TRANHDR_GEN :", Convert.getInstance().bcdToStr(msgBuffer.toByteArray()));

        PresentationHeaderPack(msgBuffer, new byte[]{'1'}, new byte[]{'1'}, TRANSACTION_CODE_AUDIT_REPORT_TYPE, EcrData.instance.RespCode, new byte[]{'0'});
        Log.d("PREHDR_GEN  :", Convert.getInstance().bcdToStr(msgBuffer.toByteArray()));

        //FieldType = "02", Response Text from host or EDC
        fieldDataElementPack(msgBuffer, FIELD_RESPONSE_TEXT_TYPE, EcrData.instance.HyperComRespText);

        //FieldType = "D0", Merchant Name and Address
        tmpStream.reset();
        tmpStream.write(EcrData.instance.MerName, 0, EcrData.instance.MerName.length);
        tmpStream.write(EcrData.instance.MerAddress, 0, EcrData.instance.MerAddress.length);
        tmpStream.write(EcrData.instance.MerAddress1, 0, EcrData.instance.MerAddress1.length);
        fieldDataElementPack(msgBuffer, FIELD_MERCHNT_NAME_AND_ADDR_TYPE, tmpStream.toByteArray());

        //FieldType = "16", Terminal Identification Number (TID)
        fieldDataElementPack(msgBuffer, FIELD_TERMINAL_ID_TYPE, EcrData.instance.TermID);

        //FieldType = "D1", Merchant Number (MID)
        fieldDataElementPack(msgBuffer, FIELD_MERCHNT_ID_TYPE, EcrData.instance.MerID);

        //FieldType = "50", Batch Number
        fieldDataElementPack(msgBuffer, FIELD_BATCH_NUMBER_TYPE, EcrData.instance.BatchNo);

        //FieldType = "03", Transaction Date
        fieldDataElementPack(msgBuffer, FIELD_TRANSACTION_DATE_TYPE, EcrData.instance.DateByte);

        //FieldType = "04", Transaction Time
        fieldDataElementPack(msgBuffer, FIELD_TRANSACTION_TIME_TYPE, EcrData.instance.TimeByte);

        //FieldType = 0x48 0x4E, HN-NII
        fieldDataElementPack(msgBuffer, FIELD_NII_TYPE, EcrData.instance.HYPER_COM_HN_NII);

        //FieldType = "H1"-Batch total sales count
        fieldDataElementPack(msgBuffer, FIELD_BATCH_TOTAL_SALES_COUNT_TYPE, EcrData.instance.BatchTotalSalesCount);

        //FieldType = "H2"-Batch total sales amount
        fieldDataElementPack(msgBuffer, FIELD_BATCH_TOTAL_SALES_AMOUNT_TYPE, EcrData.instance.BatchTotalSalesAmount);

        ProtocolPack(pduBuffer, msgBuffer);

        return 0;
    }

    public int rejectPack(@NonNull ByteArrayOutputStream pduBuffer, byte[] transType, byte[] respCode, byte[] respText) {
        ByteArrayOutputStream tmpStream = new ByteArrayOutputStream();
        ByteArrayOutputStream msgBuffer = new ByteArrayOutputStream();

        //Pack Message Data
        TransportHeaderPack(msgBuffer, new byte[]{'6', '0'}, String.format("%04d", 0).getBytes(), String.format("%04d", 0).getBytes());
        Log.d("REJECT:", Convert.getInstance().bcdToStr(msgBuffer.toByteArray()));

        PresentationHeaderPack(msgBuffer, new byte[]{'1'}, new byte[]{'1'}, transType, respCode, new byte[]{'0'});
        Log.d("REJECT:", Convert.getInstance().bcdToStr(msgBuffer.toByteArray()));

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

        ProtocolPack(pduBuffer, msgBuffer);
        return 0;
    }

    public int sendTestCommResponse() {
        ByteArrayOutputStream pduBuffer = new ByteArrayOutputStream();

        ByteArrayOutputStream msgBuffer = new ByteArrayOutputStream();

        //Pack Message Data
        TransportHeaderPack(msgBuffer, new byte[]{'6', '0'}, String.format("%04d", 0).getBytes(), String.format("%04d", 0).getBytes());
        Log.d("HyperComm:", Convert.getInstance().bcdToStr(msgBuffer.toByteArray()));

        PresentationHeaderPack(msgBuffer, new byte[]{'1'}, new byte[]{'2'}, TRANSACTION_CODE_TEST_COMMUNICATION_TYPE, new byte[]{'0', '0'}, new byte[]{'0'});
        Log.d("HyperComm:", Convert.getInstance().bcdToStr(msgBuffer.toByteArray()));

        ProtocolPack(pduBuffer, msgBuffer);

        sendPdu("sendTestCommResponse:", pduBuffer);
        return 0;
    }

    public int cancelSaleByUserSendResponse(byte[] transType, int debug_console) {
        ByteArrayOutputStream pduBuffer = new ByteArrayOutputStream();


        int ret = rejectPack(pduBuffer, transType, new byte[]{0, 0}, "".getBytes());

        if (ret >= 0) {
            if (debug_console > 0)
                Log.d("SendConsole:", "cancelByHostSendResponse:" + Convert.getInstance().bcdToStr(pduBuffer.toByteArray()));
            else {
                sendPdu("cancelByHostSendResponse:", pduBuffer);
            }
        }
        return 0;
    }

    public int cardNotDetectSendResponse(byte[] transType, byte[] respText, int debug_console) {
        ByteArrayOutputStream pduBuffer = new ByteArrayOutputStream();

        int ret = rejectPack(pduBuffer, transType, new byte[]{'R', 'B'}, "".getBytes());
        if (ret >= 0) {
            if (debug_console > 0)
                Log.d("SendConsole:", "cancelByHostSendResponse:" + Convert.getInstance().bcdToStr(pduBuffer.toByteArray()));
            else {
                sendPdu("cancelByHostSendResponse:", pduBuffer);
            }
        }
        return 0;
    }

    public int BSSNotDetectSendResponse(byte[] transType, int debug_console) {
        ByteArrayOutputStream pduBuffer = new ByteArrayOutputStream();
        int ret = rejectPack(pduBuffer, transType, new byte[]{'R', 'B'}, "02-RABBIT NOT TAP CARD".getBytes());
        if (ret >= 0) {
            if (debug_console > 0)
                Log.d("SendConsole:", "cancelByHostSendResponse:" + Convert.getInstance().bcdToStr(pduBuffer.toByteArray()));
            else {
                sendPdu("cancelByHostSendResponse:", pduBuffer);
            }
        }
        return 0;
    }

    public int cancelByHostSendResponse(byte[] transType, int debug_console) {
        ByteArrayOutputStream pduBuffer = new ByteArrayOutputStream();

        int ret = rejectPack(pduBuffer, transType, EcrData.instance.RespCode, "REJECT".getBytes());

        if (ret >= 0) {
            if (debug_console > 0)
                Log.d("SendConsole:", "cancelByHostSendResponse:" + Convert.getInstance().bcdToStr(pduBuffer.toByteArray()));
            else {
                sendPdu("cancelByHostSendResponse:", pduBuffer);
            }
        }
        return 0;
    }

    public int settlementTransactionSendResponse(String HostID, int debug_console) {
        ByteArrayOutputStream pduBuffer = new ByteArrayOutputStream();

        int ret = settlementTransactionPack(pduBuffer, HostID);

        if (ret >= 0) {
            if (debug_console > 0)
                Log.d("SendConsole:", "SettlementTrans:" + Convert.getInstance().bcdToStr(pduBuffer.toByteArray()));
            else {
                sendPdu("SettlementTrans:", pduBuffer);
            }
        }

        return ret;
    }

    public int auditReportTransactionSendResponse(String HostID, int debug_console) {
        ByteArrayOutputStream pduBuffer = new ByteArrayOutputStream();

        int ret = auditReportTransactionPack(pduBuffer, HostID);

        if (ret >= 0) {
            if (debug_console > 0)
                Log.d("SendConsole:", "AuditReportTrans:" + Convert.getInstance().bcdToStr(pduBuffer.toByteArray()));
            else {
                sendPdu("AuditReportTrans:", pduBuffer);
            }
        }

        return ret;
    }

    public int voidTransactionSendResponse(int debug_console) {
        ByteArrayOutputStream pduBuffer = new ByteArrayOutputStream();
        TransData transData = Component.getTransDataInstance();
        SysParam sysParam = FinancialApplication.getSysParam();
        Acquirer acquirer = transData.getAcquirer();

        int ret = voidTransactionPack(pduBuffer);

        if (ret >= 0) {
            if (debug_console > 0)
                Log.d("SendConsole:", "VoidTrans:" + Convert.getInstance().bcdToStr(pduBuffer.toByteArray()));
            else {
                sendPdu("VoidTrans:", pduBuffer);
            }
        }

        return ret;
    }

    public int saleTransactionSendResponse(byte[] transType, int debug_console) {
        ByteArrayOutputStream pduBuffer = new ByteArrayOutputStream();
        TransData transData = Component.getTransDataInstance();
        SysParam sysParam = FinancialApplication.getSysParam();
        Acquirer acquirer = transData.getAcquirer();

        int ret = saleTransactionPack(pduBuffer, transType);

        if (ret >= 0) {
            if (debug_console > 0)
                Log.d("SendConsole:", "SaleCreditTrans: " + Convert.getInstance().bcdToStr(pduBuffer.toByteArray()));
            else {
                sendPdu("SaleCreditTrans:", pduBuffer);
            }
        }

        return ret;
    }

    public int ProtocolPack(ByteArrayOutputStream pduBuffer, ByteArrayOutputStream msgBuffer) {
        byte[] STXbuffer = new byte[]{STX};
        byte[] ETXbuffer = new byte[]{ETX};
        byte[] msgLength = new byte[2];
        byte[] LRCbuffer = new byte[1];

        //Pack STX
        pduBuffer.write(STXbuffer, 0, STXbuffer.length);

        //Pack data length BCD Encode
        int msg_length = /*msgLength.length + */((msgBuffer == null) ? 0 : msgBuffer.toByteArray().length)/* + ETXbuffer.length*/;
        msgLength[0] = (byte) ((msg_length / 1000) & 0x0F);
        msgLength[0] <<= 4;
        msgLength[0] |= (byte) (((msg_length % 1000) / 100) & 0x0F);
        msgLength[1] = (byte) (((msg_length % 100) / 10) & 0x0F);
        msgLength[1] <<= 4;
        msgLength[1] |= (byte) ((msg_length % 10) & 0x0F);
        pduBuffer.write(msgLength, 0, msgLength.length);

        //Pack Message Data
        if ((msgBuffer != null) && (msgBuffer.toByteArray().length > 0)) {
            pduBuffer.write(msgBuffer.toByteArray(), 0, msgBuffer.toByteArray().length);
        }

        //Pack ETX
        pduBuffer.write(ETXbuffer, 0, ETXbuffer.length);

        //Pack LRC
        LRCbuffer[0] = Checksum.calculateLRC(Arrays.copyOfRange(pduBuffer.toByteArray(), 1, pduBuffer.toByteArray().length));
        pduBuffer.write(LRCbuffer, 0, LRCbuffer.length);


        return 0;
    }

    public int TransportHeaderPack(ByteArrayOutputStream msgBuffer, byte[] tranHeaderType, byte[] destAddr, byte[] srcAddr) {

        //Pack tranHeaderType
        msgBuffer.write(tranHeaderType, 0, 2);

        //Pack destAddr
        msgBuffer.write(destAddr, 0, 4);

        //Pack srcAddr
        msgBuffer.write(srcAddr, 0, 4);

        return 0;
    }

    public int PresentationHeaderPack(ByteArrayOutputStream msgBuffer, byte[] formatVer, byte[] reqResIndicator, byte[] transactionCode, byte[] responseCode, byte[] moreIndicator) {
        byte[] separator = new byte[]{FIELD_SEPARATOR};

        //Pack formatVer data
        msgBuffer.write(formatVer, 0, 1);

        //Pack reqResIndicator data
        msgBuffer.write(reqResIndicator, 0, 1);

        //Pack transactionCode data
        msgBuffer.write(transactionCode, 0, 2);

        //Pack responseCode data
        msgBuffer.write(responseCode, 0, 2);

        //Pack moreIndicator data
        msgBuffer.write(moreIndicator, 0, 1);

        //Pack separator
        msgBuffer.write(separator, 0, separator.length);

        return 0;
    }

    public int fieldDataElementPack(ByteArrayOutputStream msgBuffer, byte[] fieldType, byte[] data) {
        return fieldDataElementPack(msgBuffer, fieldType, data, false);
    }

    public int fieldDataElementPack(ByteArrayOutputStream msgBuffer, byte[] fieldType, byte[] data, boolean zeroByteProcess) {
        byte[] fieldLength = new byte[2];
        byte[] separator = new byte[]{FIELD_SEPARATOR};


        if (data == null || ((data.length <= 0) && !zeroByteProcess)) return -1;

        //Pack fieldType
        msgBuffer.write(fieldType, 0, 2);


        //Pack data length BCD Encode
        fieldLength[0] = (byte) ((data.length / 1000) & 0x0F);
        fieldLength[0] <<= 4;
        fieldLength[0] |= (byte) (((data.length % 1000) / 100) & 0x0F);
        fieldLength[1] = (byte) (((data.length % 100) / 10) & 0x0F);
        fieldLength[1] <<= 4;
        fieldLength[1] |= (byte) ((data.length % 10) & 0x0F);


        msgBuffer.write(fieldLength, 0, fieldLength.length);

        //Pack data
        msgBuffer.write(data, 0, data.length);

        //Pack separator
        msgBuffer.write(separator, 0, separator.length);

        //Log.d("HyperCommClass :", "Inner PDU =" + Convert.getInstance().bcdToStr(pduBuffer.toByteArray()));
        return 0;
    }

    public int fieldDataElementPack(ByteArrayOutputStream msgBuffer, byte[] fieldType, String data) {
        byte[] fieldLength = new byte[2];
        byte[] separator = new byte[]{FIELD_SEPARATOR};

        if (data == null) return -1;
        if (data.getBytes().length <= 0) return -2;

        //Pack fieldType
        msgBuffer.write(fieldType, 0, 2);

        //Pack data length BCD Encode
        fieldLength[0] = (byte) ((data.getBytes().length / 1000) & 0x0F);
        fieldLength[0] <<= 4;
        fieldLength[0] |= (byte) (((data.getBytes().length % 1000) / 100) & 0x0F);
        fieldLength[1] = (byte) (((data.getBytes().length % 100) / 10) & 0x0F);
        fieldLength[1] <<= 4;
        fieldLength[1] |= (byte) ((data.getBytes().length % 10) & 0x0F);
        msgBuffer.write(fieldLength, 0, fieldLength.length);

        //Pack data
        msgBuffer.write(data.getBytes(), 0, data.getBytes().length);

        //Pack separator
        msgBuffer.write(separator, 0, separator.length);

        //Log.d("HyperCommClass :", "Inner PDU =" + Convert.getInstance().bcdToStr(pduBuffer.toByteArray()));
        return 0;
    }

    public boolean sendPdu(String sendTag, ByteArrayOutputStream pduBuffer) {
        pduBufferBackup = pduBuffer;
        if (pduBuffer != null) {
            if (isBaseReadyFlag) {
                if (mCommManage != null) {
                    mCommManage.ClearBuffer();

                    mPAXCommState = PAXCommState.WAIT_ACK;

                    if (mCommManage.MainIO != null) {
                        HashMap<Integer, byte[]> data_buf_hash = splitSendDataBuffer(pduBuffer.toByteArray());
                        if (data_buf_hash != null) {
                            if (data_buf_hash.size() > 0) {
                                for (int i = 0; i <= data_buf_hash.size() - 1; i++) {
                                    mCommManage.MainIO.Write(data_buf_hash.get(i));
                                }
                            }
                        }
                        //mCommManage.MainIO.Write(pduBuffer.toByteArray());

                        // Extend ECR-EDC-RDR
                        //bss_ecr_logging(pSTATE_SEND_MSG_EDC_TO_POS,pProtocol_ECR_HYP,pduBuffer.toByteArray());

                    }

                    mCommManage.StartReceive();

                    Log.d("HyperComm:", sendTag + Convert.getInstance().bcdToStr(pduBuffer.toByteArray()));
                    sndTry = 0;
                    sndTimeout.start();
                } else {
                    Log.d("HyperComm:", "mCommManage.MainIO == null");
                }
            } else {
                Log.d("HyperComm:", "pduBufferBackup:" + Convert.getInstance().bcdToStr(pduBuffer.toByteArray()));
                isSendPDUPendingFlag = true;
            }
        } else {
            Log.d("HyperComm:", "pduBuffer = null");
        }

        return false;
    }

    public HashMap<Integer, byte[]> splitSendDataBuffer(byte[] local_buff) {
        HashMap<Integer, byte[]> local_splitter = new HashMap<Integer, byte[]>();
        if (local_buff.length <= 512) {
            local_splitter.put(0, local_buff);
        } else {
            // size greater than 512
            int unitsize = 512;
            int loopCount = local_buff.length / unitsize;
            int lastUnitsize = local_buff.length % unitsize;
            if (lastUnitsize > 0) {
                loopCount += 1;
            }
            int currentIndex = 0;
            byte[] tmpBuff = null;
            for (int index = 0; index <= loopCount - 1; index++) {
                if (index < (loopCount - 1)) {
                    tmpBuff = new byte[unitsize];
                    System.arraycopy(local_buff, currentIndex, tmpBuff, 0, tmpBuff.length);
                    currentIndex += unitsize;
                    local_splitter.put(index, tmpBuff);
                } else {
                    tmpBuff = new byte[lastUnitsize];
                    System.arraycopy(local_buff, currentIndex, tmpBuff, 0, tmpBuff.length);
                    currentIndex += lastUnitsize;
                    local_splitter.put(index, tmpBuff);
                }
            }
        }
        return local_splitter;
    }

    public boolean sendPduPending() {
        if (pduBufferBackup != null) {
            if (mCommManage != null) mCommManage.ClearBuffer();
            mPAXCommState = PAXCommState.WAIT_ACK;
            mCommManage.MainIO.Write(pduBufferBackup.toByteArray());

            if (mCommManage != null) mCommManage.StartReceive();

            Log.d("HyperComm:", "Re-Send:saleTransactionSendResponseBackup:" + Convert.getInstance().bcdToStr(pduBufferBackup.toByteArray()));

            sndTry = 0;
            sndTimeout.start();
            return true;
        }
        return false;
    }


    public class FieldDataClass {
        public byte[] fieldType = new byte[]{0, 0};
        public byte[] data = new byte[1024];
        public int dataLength;

        public FieldDataClass() {
            this.fieldType[0] = 0;
            this.fieldType[1] = 0;
            this.dataLength = 0;
        }
    }
}
