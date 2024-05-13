package com.pax.pay.ECR;

import android.annotation.SuppressLint;
import android.os.CountDownTimer;

import androidx.annotation.NonNull;

import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.utils.Tools;
import com.pax.device.DeviceImplNeptune;
import com.pax.pay.uart.CommManageClass;
import com.pax.pay.uart.CommManageInterface;
import com.pax.pay.uart.ProtoFilterClass;
import com.pax.pay.utils.Checksum;
import com.pax.pay.utils.Convert;
import com.pax.pay.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import th.co.bkkps.utils.Log;


public class PosNetCommClass {
    private final byte ACK = 0x06;
    private final byte NACK = 0x15;
    private final byte STX = 0x02;
    private final byte ETX = 0x03;
    private boolean isSendPDUPendingFlag = false;
    private boolean isBaseReadyFlag = true;

    private ProtoFilterClass mProtoFilter;

    public enum PosNetCommState {
        SOF, TRANS, LEN, DATA, REF1, REF2, VAT_AMT, TAX_ALLOWANCE, MERC_UNIQUE_VALUE, CAMPAIGN_TYPE, POS_RECEIPT_NO, USER_ID, CASHIER_NAME, CHKSUM, EOF, WAIT_ACK
    }

    public PosNetCommState mPosNetCommState;

    public static final byte[] TRANSACTION_SALE_TYPE = new byte[]{0x53, 0x41, 0x4C, 0x45};
    public static final byte[] TRANSACTION_VOID_TYPE = new byte[]{0x56, 0x4F, 0x49, 0x44};
    public static final byte[] TRANSACTION_SETTLEMENT_TYPE = new byte[]{0x53, 0x45, 0x54, 0x54};
    public static final byte[] TRANSACTION_SALE_VATB_TYPE = new byte[]{0x56, 0x41, 0x54, 0x42}; ///VATB

    public static final byte[] TRANSACTION_GET_PAN_TYPE = new byte[]{0x47, 0x44, 0x54, 0x32};
    public static final byte[] TRANSACTION_GET_THE_ONE_TYPE = new byte[]{0x47, 0x54, 0x31, 0x43};


    public static final byte[][] TRANSACTION_TYPE_LIST = new byte[][]{
            TRANSACTION_SALE_TYPE,
            TRANSACTION_VOID_TYPE,
            TRANSACTION_SETTLEMENT_TYPE,
            TRANSACTION_GET_PAN_TYPE,
            TRANSACTION_GET_THE_ONE_TYPE,
            TRANSACTION_SALE_VATB_TYPE};

    private PosNetCommInterface mPosNetCommCbk;
    private CommManageClass mCommManage = null;
    public int sndTry = 0;
    private int i_tmp = 0;
    private byte[] msgBuffer = new byte[1024];
    byte msgLRC = 0;

    ByteArrayOutputStream pduBufferBackup = null;

    public PosNetCommClass(CommManageClass CommManage, PosNetCommInterface PosNetCommCbk) {

        this.mPosNetCommState = PosNetCommClass.PosNetCommState.SOF;

        this.mPosNetCommCbk = PosNetCommCbk;

        AddReceiveListener(CommManage);

        this.i_tmp = 0;
    }

    public PosNetCommClass(CommManageClass CommManage, ProtoFilterClass ProtoFilter, PosNetCommInterface PosNetCommCbk) {

        this.mPosNetCommState = PosNetCommClass.PosNetCommState.SOF;

        this.mPosNetCommCbk = PosNetCommCbk;

        this.mCommManage = CommManage;

        AddReceiveListener(ProtoFilter);

        this.i_tmp = 0;
    }

    public PosNetCommClass() {

    }

    public void initial(CommManageClass CommManage, ProtoFilterClass ProtoFilter, PosNetCommInterface PosNetCommCbk) {

        this.mPosNetCommState = PosNetCommClass.PosNetCommState.SOF;

        this.mPosNetCommCbk = PosNetCommCbk;

        this.mCommManage = CommManage;

        AddReceiveListener(ProtoFilter);

        this.i_tmp = 0;
    }

    public void AddReceiveListener(CommManageClass CommManage) {
        if (CommManage == null) return;
        resetState();
        mCommManage = CommManage;
        mCommManage.AddReceiveListener(new CommManageInterface() {
            public int onReceive(byte[] data, int len) {
                if (mCommManage != null) mCommManage.StopReceive();
                Log.d("onReceive:", "PosNetComm = " + Convert.getInstance().bcdToStr(data));
                return ProtocolUnpack(data, len);
            }
        });
    }

    public void AddReceiveListener(ProtoFilterClass ProtoFilter) {
        if (ProtoFilter == null) return;

        mProtoFilter = ProtoFilter;

        mProtoFilter.AddPosNetListener(new CommManageInterface() {
            public int onReceive(byte[] data, int len) {
                if (mCommManage != null) mCommManage.StopReceive();
                //Log.d("HyperComm:", "mPAXCommState = " + PAXCommState.values().toString());
                Log.d("onReceive:", "PosNetComm = " + Convert.getInstance().bcdToStr(data));

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

    }

    public void onBaseDisconnect() {
        isBaseReadyFlag = false;
    }

    CountDownTimer rcvTimeout = new CountDownTimer(3000, 500) {
        public void onTick(long millisUntilFinished) {

        }

        public void onFinish() {
            // if (mCommManage.MainIO != null) mCommManage.MainIO.Write(new byte[]{NACK});
            mPosNetCommState = PosNetCommClass.PosNetCommState.SOF;
            Log.d("PosNetComm:", "RX TIMEOUT OCCUR.....");
        }
    };


    CountDownTimer sndTimeout = new CountDownTimer(150, 50) {
        public void onTick(long millisUntilFinished) {
            if (sndTry > 0) {
                if (mCommManage.MainIO != null) {
                    if (pduBufferBackup != null) {
                        //mCommManage.MainIO.Write(pduBufferBackup.toByteArray());
                    }
                }
            }
            sndTry++;
        }

        public void onFinish() {
            mPosNetCommState = PosNetCommClass.PosNetCommState.SOF;
            Log.d("PosNetComm:", "TX TIMEOUT OCCUR....." + sndTry);
            pduBufferBackup = null;
            sndTry = 0;
        }
    };

    public class PDUClass {
        public byte[] transaction;
        public int length;
        public byte[] data;
        public byte chksum;
        public boolean isVatb;

        public byte[] REF1;
        public byte[] REF2;
        public byte[] vatAmount;
        public byte[] taxAllowance;
        public byte[] mercUniqueValue;
        public byte[] campaignType;
        public byte[] poSNoReceiptNo;
        public byte[] userId;
        public byte[] cashierName;

        public PDUClass() {
            chksum = 0;
            data = null;
            length = 0;
            transaction = null;

            REF1 = null;
            REF2 = null;
            vatAmount = null;
            taxAllowance = null;
            mercUniqueValue = null;
            campaignType = null;

            isVatb = false;
        }
    }

    PDUClass rcvPDU = null;

    public void resetState() {
        mPosNetCommState = PosNetCommState.SOF;
    }

    public int ProtocolUnpack(byte[] data, int len) {

        // Enhanced internal ECR_BSS_LOGGING :
        //bss_ecr_logging(pSTATE_RECV_MSG_POS_TO_EDC,pProtocol_ECR_PNT, data);

        if (mPosNetCommState != PosNetCommState.WAIT_ACK
                && data[0] == ACK) {
            return -1;
        }

        byte[] VATB_Type = new byte[]{0x56, 0x41, 0x54, 0x42};     // VATB


        for (int i_cnt = 0; i_cnt < len; i_cnt++) {
            switch (mPosNetCommState) {
                case SOF:
                    if (data[i_cnt] == STX) {

                        i_tmp = 0;
                        msgLRC = 0;
                        msgLRC ^= data[i_cnt];

                        mPosNetCommState = PosNetCommClass.PosNetCommState.TRANS;

                        Log.d("PosNetComm:", "SOF_PASS");
                    }

                    break;
                case TRANS:
                    msgBuffer[i_tmp++] = data[i_cnt];
                    msgLRC ^= data[i_cnt];
                    if (i_tmp == 4) {
                        msgBuffer[4] = 0;
                        i_tmp = 0;
                        byte[] tmp = new byte[4];

                        System.arraycopy(msgBuffer, 0, tmp, 0, 4);

                        if (TransactionAssert(tmp)) {
                            rcvPDU = new PDUClass();
                            rcvPDU.transaction = Arrays.copyOf(msgBuffer, 4);
                            rcvTimeout.start();
                            mPosNetCommState = PosNetCommState.LEN;
                            Log.d("PosNetComm:", "TRANS_PASS = " + new String(rcvPDU.transaction));
                        } else {
                            rcvPDU = null;
                            mPosNetCommState = PosNetCommState.SOF;
                            Log.d("PosNetComm:", "TRANS_FAIL = " + new String(tmp));
                        }

                    }

                    break;

                case LEN:
                    msgBuffer[i_tmp++] = data[i_cnt];
                    msgLRC ^= data[i_cnt];
                    if (i_tmp == 3) {
                        msgBuffer[3] = 0;
                        i_tmp = 0;
                        byte[] tmp = new byte[3];
                        System.arraycopy(msgBuffer, 0, tmp, 0, 3);
                        if (((tmp[0] < 0x30) || (tmp[0] > 0x39)) || ((tmp[1] < 0x30) || (tmp[1] > 0x39)) || ((tmp[2] < 0x30) || (tmp[2] > 0x39)))
                            mPosNetCommState = PosNetCommState.SOF;
                        rcvPDU.length = Integer.parseInt(new String(tmp));
                        if (rcvPDU.length == 0) {
                            //   Log.d("PosNetComm:", "LEN_PASS = " + rcvPDU.length);
                            mPosNetCommState = PosNetCommState.CHKSUM;
                        } else {
                            mPosNetCommState = PosNetCommClass.PosNetCommState.DATA;
                        }
                    } else {
                        mPosNetCommState = PosNetCommState.LEN;
                    }
                    break;

                case DATA:
                    msgBuffer[i_tmp++] = data[i_cnt];
                    msgLRC ^= data[i_cnt];

                    if (Arrays.equals(rcvPDU.transaction, TRANSACTION_SALE_VATB_TYPE)) {
                        rcvPDU.isVatb = true;
                        mPosNetCommState = PosNetCommClass.PosNetCommState.DATA;
                        if (i_tmp == 12) {
                            msgBuffer[12] = 0;
                            i_tmp = 0;
                            rcvPDU.data = Arrays.copyOf(msgBuffer, 12);
                            //Log.d("PosNetComm:", "DATA_PASS");
                            mPosNetCommState = PosNetCommClass.PosNetCommState.REF1;
                        }
                        break;
                    } else {
                        if (i_tmp == rcvPDU.length) {

                            rcvPDU.data = Arrays.copyOf(msgBuffer, rcvPDU.length);

                            Log.d("PosNetComm:", "DATA_PASS");
                            mPosNetCommState = PosNetCommClass.PosNetCommState.CHKSUM;

                            Log.d("PosNetComm:", "DATA" + i_tmp + "=" + data[i_cnt]);
                        }
                    }

                    break;
                case REF1:
                    msgBuffer[i_tmp++] = data[i_cnt];
                    msgLRC ^= data[i_cnt];
                    if (i_tmp == 20) {
                        msgBuffer[20] = 0;
                        i_tmp = 0;
                        rcvPDU.REF1 = Arrays.copyOf(msgBuffer, 20);
                        mPosNetCommState = PosNetCommClass.PosNetCommState.REF2;
                    }
                    break;
                case REF2:
                    msgBuffer[i_tmp++] = data[i_cnt];
                    msgLRC ^= data[i_cnt];

                    if (i_tmp == 20) {
                        msgBuffer[20] = 0;
                        i_tmp = 0;
                        rcvPDU.REF2 = Arrays.copyOf(msgBuffer, 20);
                        mPosNetCommState = ((data.length - (i_cnt + 1) == 28)) ? PosNetCommState.POS_RECEIPT_NO : PosNetCommState.VAT_AMT;
                    }
                    break;

                case VAT_AMT:
                    msgBuffer[i_tmp++] = data[i_cnt];
                    msgLRC ^= data[i_cnt];
                    if (i_tmp == 10) {
                        msgBuffer[10] = 0;
                        i_tmp = 0;
                        rcvPDU.vatAmount = Arrays.copyOf(msgBuffer, 10);
                        mPosNetCommState = PosNetCommClass.PosNetCommState.TAX_ALLOWANCE;
                    }
                    break;

                case TAX_ALLOWANCE:
                    msgBuffer[i_tmp++] = data[i_cnt];
                    msgLRC ^= data[i_cnt];

                    if (i_tmp == 10) {
                        msgBuffer[10] = 0;
                        i_tmp = 0;
                        rcvPDU.taxAllowance = Arrays.copyOf(msgBuffer, 10);
                        mPosNetCommState = PosNetCommClass.PosNetCommState.MERC_UNIQUE_VALUE;
                    }

                    break;
                case MERC_UNIQUE_VALUE:
                    msgBuffer[i_tmp++] = data[i_cnt];
                    msgLRC ^= data[i_cnt];

                    if (i_tmp == 20) {
                        msgBuffer[20] = 0;
                        i_tmp = 0;
                        rcvPDU.mercUniqueValue = Arrays.copyOf(msgBuffer, 20);
                        mPosNetCommState = PosNetCommClass.PosNetCommState.CAMPAIGN_TYPE;
                    }

                    break;
                case CAMPAIGN_TYPE:
                    msgBuffer[i_tmp++] = data[i_cnt];
                    msgLRC ^= data[i_cnt];

                    if (i_tmp == 6) {
                        msgBuffer[6] = 0;
                        i_tmp = 0;
                        rcvPDU.campaignType = Arrays.copyOf(msgBuffer, 6);
                        mPosNetCommState = (data.length - (i_cnt + 1) == 2) ? PosNetCommState.CHKSUM : PosNetCommState.POS_RECEIPT_NO;
                    }
                    break;
                case POS_RECEIPT_NO:
                    msgBuffer[i_tmp++] = data[i_cnt];
                    msgLRC ^= data[i_cnt];

                    if (i_tmp == 10) {
                        msgBuffer[10] = 0;
                        i_tmp = 0;
                        rcvPDU.poSNoReceiptNo = Arrays.copyOf(msgBuffer, 10);
                        EcrData.instance.PosNo_ReceiptNo = Arrays.copyOf(msgBuffer, 10);
                        mPosNetCommState = PosNetCommState.USER_ID;
                    }
                    break;
                case USER_ID:
                    msgBuffer[i_tmp++] = data[i_cnt];
                    msgLRC ^= data[i_cnt];

                    if (i_tmp == 6) {
                        msgBuffer[6] = 0;
                        i_tmp = 0;
                        rcvPDU.userId = Arrays.copyOf(msgBuffer, 6);
                        EcrData.instance.User_ID = Arrays.copyOf(msgBuffer, 6);
                        mPosNetCommState = PosNetCommState.CASHIER_NAME;
                    }
                    break;
                case CASHIER_NAME:
                    msgBuffer[i_tmp++] = data[i_cnt];
                    msgLRC ^= data[i_cnt];

                    if (i_tmp == 10) {
                        msgBuffer[10] = 0;
                        i_tmp = 0;
                        rcvPDU.cashierName = Arrays.copyOf(msgBuffer, 10);
                        EcrData.instance.CashierName = Arrays.copyOf(msgBuffer, 10);
                        mPosNetCommState = PosNetCommState.CHKSUM;
                    }
                    break;
                case CHKSUM:
                    rcvPDU.chksum = data[i_cnt];
                    mPosNetCommState = PosNetCommClass.PosNetCommState.EOF;
                    break;

                case EOF:
                    rcvTimeout.cancel();
                    mPosNetCommState = PosNetCommClass.PosNetCommState.SOF;
                    if (data[i_cnt] == ETX) {
                        Log.d("PosNetComm:", "EOF_PASS");
                        if (rcvPDU.chksum == msgLRC) {
                            Log.d("PosNetComm:", "CHKSUM_PASS");
                            //mCommManage.PauseReceive();
                            if (mCommManage != null) mCommManage.StopReceive();
                            if (rcvPDU.data != null) {
                                Log.d("PosNetComm:", "DATA = " + Convert.getInstance().bcdToStr(rcvPDU.data));
                            } else {
                                Log.d("PosNetComm:", "DATA = null");
                            }


                            if (mCommManage.MainIO != null) {
                                mCommManage.MainIO.Write(new byte[]{ACK});

                                // Extend ECR-EDC-RDR
                                //bss_ecr_logging(pSTATE_SEND_MSG_EDC_TO_POS,pProtocol_ECR_PNT,new byte[]{ACK});
                            }
                            if (mPosNetCommCbk != null) {
                                mPosNetCommCbk.onDataRcv(rcvPDU.transaction,
                                        rcvPDU.data,
                                        rcvPDU.isVatb,
                                        rcvPDU.REF1,
                                        rcvPDU.REF2,
                                        rcvPDU.vatAmount,
                                        rcvPDU.taxAllowance,
                                        rcvPDU.mercUniqueValue,
                                        rcvPDU.campaignType,
                                        rcvPDU.poSNoReceiptNo,
                                        rcvPDU.cashierName);
                            }

                            // mCommManage.StartReceive();
                            return 1;
                        } else {
                            // if (mCommManage.MainIO != null) mCommManage.MainIO.Write(new byte[]{NACK});
                            Log.d("PosNetComm:", "CHKSUM_FAIL, calCHKSUM = " + msgLRC + " rcvCHKSUM = " + rcvPDU.chksum);
                        }
                    }

                    break;

                case WAIT_ACK:
                    if (data[i_cnt] == ACK) {
                        sndTry = 0;
                        sndTimeout.cancel();
                        pduBufferBackup = null;
                        isSendPDUPendingFlag = false;
                        //mCommManage.StartReceive();
                        mPosNetCommState = PosNetCommClass.PosNetCommState.SOF;
                        return 1;
                    }
                    break;
                default:
                    mPosNetCommState = PosNetCommClass.PosNetCommState.SOF;
                    break;
            }

        }
        return 0;
    }

    //SALE AND Void Pack
    public int saleTransactionPack(@NonNull ByteArrayOutputStream pduBuffer, byte[] transType) {

        ByteArrayOutputStream msgBuffer = new ByteArrayOutputStream();
        msgBuffer.reset();

        //Response code
        msgBuffer.write(EcrData.instance.RespCode, 0, EcrData.instance.RespCode.length);

        // Stan
        msgBuffer.write(EcrData.instance.StanNo, 0, EcrData.instance.StanNo.length);

        // Batch Number
        msgBuffer.write(EcrData.instance.BatchNo, 0, EcrData.instance.BatchNo.length);

        //Trace/Invoice Number
        msgBuffer.write(EcrData.instance.TraceNo, 0, EcrData.instance.TraceNo.length);

        //FieldType = "56", Approval Code
        msgBuffer.write(EcrData.instance.ApprovalCode, 0, EcrData.instance.ApprovalCode.length);

        //Retrieval Reference
        msgBuffer.write(EcrData.instance.RefNo, 0, EcrData.instance.RefNo.length);

        // Card Number
        msgBuffer.write(EcrData.instance.PosNetCardNo, 0, EcrData.instance.PosNetCardNo.length);

        //Expiry Date
        msgBuffer.write(EcrData.instance.ExpDate, 0, EcrData.instance.ExpDate.length);

        //Card Issuer Name
        msgBuffer.write(EcrData.instance.CardIssuerName, 0, EcrData.instance.CardIssuerName.length);

        //Response Text from host or EDC
        msgBuffer.write(EcrData.instance.PosNetRespText, 0, EcrData.instance.PosNetRespText.length);

        //Transaction Date
        msgBuffer.write(EcrData.instance.DateByte, 0, EcrData.instance.DateByte.length);

        //Transaction Time
        msgBuffer.write(EcrData.instance.TimeByte, 0, EcrData.instance.TimeByte.length);

        //Terminal Identification Number (TID)
        msgBuffer.write(EcrData.instance.TermID, 0, EcrData.instance.TermID.length);

        //Merchant Number (MID)
        msgBuffer.write(EcrData.instance.MerID, 0, EcrData.instance.MerID.length);

        // NII
        msgBuffer.write(EcrData.instance.POSNET_HN_NII, 0, EcrData.instance.POSNET_HN_NII.length);

        //Merchant Name and Address
        msgBuffer.write(EcrData.instance.MerName, 0, EcrData.instance.MerName.length);
        msgBuffer.write(EcrData.instance.MerAddress, 0, EcrData.instance.MerAddress.length);
        msgBuffer.write(EcrData.instance.MerAddress1, 0, EcrData.instance.MerAddress1.length);

        //Card holder name
        msgBuffer.write(EcrData.instance.HolderName, 0, EcrData.instance.HolderName.length);

//        //Balance amount
//        msgBuffer.write(EcrData.instance.RabbitBalanceAmount,0, EcrData.instance.RabbitBalanceAmount.length);
//
//        //Reader ID
//        msgBuffer.write(EcrData.instance.RabbitID,0, EcrData.instance.RabbitID.length);
//
//        //Rabbit Trace (UDSN)
//        msgBuffer.write(EcrData.instance.RabbitTrace,0, EcrData.instance.RabbitTrace.length);


        ProtocolPack(transType, pduBuffer, msgBuffer);

        return 0;
    }

    //SALE AND Void Pack
    public int saleTransactionPackForTop(@NonNull ByteArrayOutputStream pduBuffer, byte[] transType) {

        ByteArrayOutputStream msgBuffer = new ByteArrayOutputStream();
        msgBuffer.reset();

        //Response code
        msgBuffer.write(EcrData.instance.RespCode, 0, EcrData.instance.RespCode.length);

        // Stan
        msgBuffer.write(EcrData.instance.StanNo, 0, EcrData.instance.StanNo.length);

        // Batch Number
        msgBuffer.write(EcrData.instance.BatchNo, 0, EcrData.instance.BatchNo.length);

        //Trace/Invoice Number
        msgBuffer.write(EcrData.instance.TraceNo, 0, EcrData.instance.TraceNo.length);

        //FieldType = "56", Approval Code
        msgBuffer.write(EcrData.instance.ApprovalCode, 0, EcrData.instance.ApprovalCode.length);

        //Retrieval Reference
        msgBuffer.write(EcrData.instance.RefNo, 0, EcrData.instance.RefNo.length);

        // Card Number
        msgBuffer.write(EcrData.instance.PosNetCardNo, 0, EcrData.instance.PosNetCardNo.length);

        //Expiry Date
        msgBuffer.write(EcrData.instance.ExpDate, 0, EcrData.instance.ExpDate.length);

        //Card Issuer Name
        msgBuffer.write(EcrData.instance.CardIssuerName, 0, EcrData.instance.CardIssuerName.length);

        //
        msgBuffer.write(EcrData.instance.PosNo_ReceiptNo, 0, EcrData.instance.PosNo_ReceiptNo.length);

        //
        msgBuffer.write(EcrData.instance.padding, 0, EcrData.instance.padding.length);

        //Response Text from host or EDC
        //msgBuffer.write(EcrData.instance.PosNetRespText,0, EcrData.instance.PosNetRespText.length);

        //Transaction Date
        msgBuffer.write(EcrData.instance.DateByte, 0, EcrData.instance.DateByte.length);

        //Transaction Time
        msgBuffer.write(EcrData.instance.TimeByte, 0, EcrData.instance.TimeByte.length);

        //Terminal Identification Number (TID)
        msgBuffer.write(EcrData.instance.TermID, 0, EcrData.instance.TermID.length);

        //Merchant Number (MID)
        msgBuffer.write(EcrData.instance.MerID, 0, EcrData.instance.MerID.length);

        // NII
        msgBuffer.write(EcrData.instance.POSNET_HN_NII, 0, EcrData.instance.POSNET_HN_NII.length);

        //Merchant Name and Address
        msgBuffer.write(EcrData.instance.MerName, 0, EcrData.instance.MerName.length);
        msgBuffer.write(EcrData.instance.MerAddress, 0, EcrData.instance.MerAddress.length);
        //msgBuffer.write(EcrData.instance.MerAddress1,0, EcrData.instance.MerAddress1.length);
        msgBuffer.write(EcrData.instance.transAmount, 0, EcrData.instance.transAmount.length);
        msgBuffer.write(EcrData.instance.padding2, 0, EcrData.instance.padding2.length);


        //Card holder name
        msgBuffer.write(EcrData.instance.HolderName, 0, EcrData.instance.HolderName.length);

//        //Balance amount
//        msgBuffer.write(EcrData.instance.RabbitBalanceAmount,0, EcrData.instance.RabbitBalanceAmount.length);
//
//        //Reader ID
//        msgBuffer.write(EcrData.instance.RabbitID,0, EcrData.instance.RabbitID.length);
//
//        //Rabbit Trace (UDSN)
//        msgBuffer.write(EcrData.instance.RabbitTrace,0, EcrData.instance.RabbitTrace.length);


        ProtocolPack(transType, pduBuffer, msgBuffer);

        return 0;
    }

    public int settlementTransactionPack(@NonNull ByteArrayOutputStream pduBuffer, byte[] transType) {
        ByteArrayOutputStream msgBuffer = new ByteArrayOutputStream();
        msgBuffer.reset();

        //Response code
        msgBuffer.write(EcrData.instance.RespCode, 0, EcrData.instance.RespCode.length);

        // Batch Number
        msgBuffer.write(EcrData.instance.BatchNo, 0, EcrData.instance.BatchNo.length);

        //Batch total sales count
        msgBuffer.write(EcrData.instance.BatchTotalSalesCount, 0, EcrData.instance.BatchTotalSalesCount.length);

        //Batch total sales amount
        msgBuffer.write(EcrData.instance.BatchTotalSalesAmount, 0, EcrData.instance.BatchTotalSalesAmount.length);

        //Batch total Refund count
        msgBuffer.write(EcrData.instance.BatchTotalRefundCount, 0, EcrData.instance.BatchTotalRefundCount.length);

        //Batch total Refund Amount
        msgBuffer.write(EcrData.instance.BatchTotalRefundAmount, 0, EcrData.instance.BatchTotalRefundAmount.length);


        ProtocolPack(transType, pduBuffer, msgBuffer);

        return 0;
    }

    // result['symbol1'] = rev[8:9]#1
    // result['card_number'] = rev[9:25]#16
    // result['symbol2'] = rev[25:26]#1
    // result['exp_date'] = rev[26:30]#4
    // result['holder_name'] = rev[30:46]#16
    // result['symbol3'] = rev[46:47]#1
    // 1 + 16 + 1 + 4 + 16 + 1 -> 39

    public int GetPanTransactionPack(@NonNull ByteArrayOutputStream pduBuffer, byte[] transType) {
        byte[] Data = new byte[39];

        //fill x
        Arrays.fill(Data, (byte) 0x78);
        int i = 0;

        //symbol 1
        Data[i++] = 0x3b;

        //Card num
        String pan = new String(EcrData.instance.PosNetCardNo);
        Utils.SaveArrayCopy(EcrData.instance.PosNetCardNo, 0, Data, i, pan.trim().length());
        i = i + pan.trim().length();

        //symbol 2
        Data[i++] = 0x3d;

        //exp
        Data[i++] = 0x58;
        Data[i++] = 0x58;
        Data[i++] = 0x58;
        Data[i++] = 0x58;

        //symbol 3
        Data[Data.length - 1] = 0x3f;

        ByteArrayOutputStream msgBuffer = new ByteArrayOutputStream();
        msgBuffer.reset();
        msgBuffer.write(Data, 0, Data.length);

        ProtocolPack(transType, pduBuffer, msgBuffer);

        return 0;
    }

    public int GetTheOneTransactionPack(@NonNull ByteArrayOutputStream pduBuffer, byte[] transType) {
        ByteArrayOutputStream msgBuffer = new ByteArrayOutputStream();
        msgBuffer.reset();

        //Batch total sales count
        //byte[] cardNum = Tools.string2Bytes("1011010042702665    ");
        if (EcrData.instance.T1C_MemberID != null) {
            if (EcrData.instance.T1C_MemberID.length > 0) {
                byte[] cardNum = EcrData.instance.T1C_MemberID;
                msgBuffer.write(cardNum, 0, cardNum.length);

                ProtocolPack(transType, pduBuffer, msgBuffer);
                return 0;
            }
        }

        return TransResult.ERR_CARD_INVALID;
    }

    public int ProtocolPack(byte[] transType, ByteArrayOutputStream pduBuffer, ByteArrayOutputStream msgBuffer) {
        byte[] STXbuffer = new byte[]{STX};
        byte[] ETXbuffer = new byte[]{ETX};
        byte[] msgLength = new byte[3];
        byte[] LRCbuffer = new byte[1];

        //Pack STX
        pduBuffer.write(STXbuffer, 0, STXbuffer.length);

        //Transaction Type
        pduBuffer.write(transType, 0, transType.length);

        //Pack data length
        int msg_length = ((msgBuffer == null) ? 0 : msgBuffer.toByteArray().length);
        Arrays.fill(msgLength, (byte) 0x20);
        @SuppressLint("DefaultLocale") String strLength = String.format("%03d", msg_length);
        System.arraycopy(strLength.getBytes(), 0, msgLength, 0, (strLength.getBytes().length > msgLength.length) ? msgLength.length : strLength.getBytes().length);
        pduBuffer.write(msgLength, 0, msgLength.length);

        //Pack Message Data
        if ((msgBuffer != null) && (msgBuffer.toByteArray().length > 0)) {
            pduBuffer.write(msgBuffer.toByteArray(), 0, msgBuffer.toByteArray().length);
        }

        //Pack LRC
        LRCbuffer[0] = Checksum.calculateLRC(Arrays.copyOfRange(pduBuffer.toByteArray(), 0, pduBuffer.toByteArray().length));
        pduBuffer.write(LRCbuffer, 0, LRCbuffer.length);

        //Pack ETX
        pduBuffer.write(ETXbuffer, 0, ETXbuffer.length);


        return 0;
    }

    public boolean sendPdu(String sendTag, ByteArrayOutputStream pduBuffer) {
        pduBufferBackup = pduBuffer;
        if (pduBuffer != null) {
            if (isBaseReadyFlag) {
                if (mCommManage != null) {
                    mCommManage.ClearBuffer();
                    if (mCommManage.MainIO != null) {
                        mCommManage.MainIO.Write(pduBuffer.toByteArray());
                        // Extend ECR-EDC-RDR
                        //bss_ecr_logging(pSTATE_SEND_MSG_EDC_TO_POS,pProtocol_ECR_PNT,pduBuffer.toByteArray());
                    }
                    mCommManage.StartReceive();
                    Log.d("PosNetComm:", sendTag + Convert.getInstance().bcdToStr(pduBuffer.toByteArray()));
                    mPosNetCommState = PosNetCommClass.PosNetCommState.WAIT_ACK;
                    sndTry = 0;
                    sndTimeout.start();
                } else {
                    Log.d("PosNetComm:", "mCommManage.MainIO == null");
                }
            } else {
                Log.d("PosNetComm:", "pduBufferBackup:" + Convert.getInstance().bcdToStr(pduBuffer.toByteArray()));
                isSendPDUPendingFlag = true;
            }
        } else {
            Log.d("PosNetComm:", "pduBuffer = null");
        }

        return false;
    }

    public boolean sendPduPending() {
        if (pduBufferBackup != null) {
            mCommManage.ClearBuffer();
            mCommManage.MainIO.Write(pduBufferBackup.toByteArray());

            mCommManage.StartReceive();

            Log.d("PosNetComm:", "Re-Send:saleTransactionSendResponseBackup:" + Convert.getInstance().bcdToStr(pduBufferBackup.toByteArray()));
            mPosNetCommState = PosNetCommClass.PosNetCommState.WAIT_ACK;
            sndTry = 0;
            sndTimeout.start();

            return true;
        }
        return false;
    }

    public int saleTransactionSendResponse(byte[] transType, int debug_console) {
        ByteArrayOutputStream pduBuffer = new ByteArrayOutputStream();

        int ret = saleTransactionPackForTop(pduBuffer, transType);

        if (ret >= 0) {
            if (debug_console > 0)
                Log.d("PosNetComm:", "saleTransactionSendResponseConsole: " + Convert.getInstance().bcdToStr(pduBuffer.toByteArray()));
            else {
                sendPdu("saleTransactionSendResponse", pduBuffer);
            }
        }

        return ret;
    }


    public int voidTransactionSendResponse(byte[] transType, int debug_console) {
        ByteArrayOutputStream pduBuffer = new ByteArrayOutputStream();

        int ret = saleTransactionPackForTop(pduBuffer, transType);

        if (ret >= 0) {
            if (debug_console > 0)
                Log.d("SendConsole:", "voidTransactionSendResponse: " + Convert.getInstance().bcdToStr(pduBuffer.toByteArray()));
            else {
                sendPdu("voidTransactionSendResponse", pduBuffer);
            }
        }

        return ret;
    }


    public int settlementTransactionSendResponse(byte[] transType, int debug_console) {
        ByteArrayOutputStream pduBuffer = new ByteArrayOutputStream();

        int ret = settlementTransactionPack(pduBuffer, transType);

        if (ret >= 0) {
            if (debug_console > 0)
                Log.d("SendConsole:", "settlementTransactionSendResponse: " + Convert.getInstance().bcdToStr(pduBuffer.toByteArray()));
            else {
                sendPdu("settlementTransactionSendResponse", pduBuffer);
            }
        }

        return ret;
    }

    public int GetPanTransactionSendResponse(byte[] transType, int debug_console) {
        ByteArrayOutputStream pduBuffer = new ByteArrayOutputStream();

        int ret = GetPanTransactionPack(pduBuffer, transType);

        if (ret >= 0) {
            if (debug_console > 0)
                Log.d("SendConsole:", "GetPanTransactionSendResponse: " + Convert.getInstance().bcdToStr(pduBuffer.toByteArray()));
            else {
                sendPdu("settlementTransactionSendResponse", pduBuffer);
            }
        }

        return ret;
    }

    public int GetTheOneTransactionSendResponse(byte[] transType, int debug_console) {
        ByteArrayOutputStream pduBuffer = new ByteArrayOutputStream();

        int ret = GetTheOneTransactionPack(pduBuffer, transType);

        if (ret >= 0) {
            if (debug_console > 0)
                Log.d("SendConsole:", "GetTheOneTransactionSendResponse: " + Convert.getInstance().bcdToStr(pduBuffer.toByteArray()));
            else {
                sendPdu("settlementTransactionSendResponse", pduBuffer);
            }
        }

        return ret;
    }


    public int BSSNotDetectSendResponse(int debug_console) {
        int ret = 0;
        return ret;
    }

    public int cancelSaleSendResponse(int debug_console) {
        int ret = 0;
        ByteArrayOutputStream pduBuffer = new ByteArrayOutputStream();
        byte[] saleCancel = new byte[]{0x02, 'S', 'A', 'L', 'E', 0x30, 0x30, 0x32, 'C', 'A', 0x29, 0x03};

        pduBuffer.write(saleCancel, 0, saleCancel.length);

        if (debug_console > 0)
            Log.d("SendConsole:", "cancelSaleSendResponse: " + Convert.getInstance().bcdToStr(saleCancel));
        else {
            sendPdu("cancelSaleSendResponse", pduBuffer);
        }

        return ret;
    }

    public int cancelVATBSendResponse(int debug_console) {
        int ret = 0;
        ByteArrayOutputStream pduBuffer = new ByteArrayOutputStream();
        byte[] saleCancel = new byte[]{0x02, 'V', 'A', 'T', 'B', 0x30, 0x30, 0x32, 'C', 'A', 0x29, 0x03};

        pduBuffer.write(saleCancel, 0, saleCancel.length);

        if (debug_console > 0)
            Log.d("SendConsole:", "cancelSaleSendResponse: " + Convert.getInstance().bcdToStr(saleCancel));
        else {
            sendPdu("cancelSaleSendResponse", pduBuffer);
        }

        return ret;
    }

    public int cancelGetPanSendResponse(int debug_console) {
        int ret = 0;
        ByteArrayOutputStream pduBuffer = new ByteArrayOutputStream();
        byte[] saleCancel = new byte[]{0x02, 'G', 'D', 'T', '2', 0x30, 0x30, 0x32, 'C', 'A', 0x29, 0x03};

        pduBuffer.write(saleCancel, 0, saleCancel.length);

        if (debug_console > 0)
            Log.d("SendConsole:", "cancelSaleSendResponse: " + Convert.getInstance().bcdToStr(saleCancel));
        else {
            sendPdu("cancelSaleSendResponse", pduBuffer);
        }

        return ret;
    }

    public int cancelGetT1CSendResponse(int debug_console) {
        int ret = 0;
        ByteArrayOutputStream pduBuffer = new ByteArrayOutputStream();
        byte[] saleCancel = new byte[]{0x02, 'G', 'T', '1', 'C', 0x30, 0x30, 0x32, 'C', 'A', 0x29, 0x03};

        pduBuffer.write(saleCancel, 0, saleCancel.length);

        if (debug_console > 0)
            Log.d("SendConsole:", "cancelSaleSendResponse: " + Convert.getInstance().bcdToStr(saleCancel));
        else {
            sendPdu("cancelSaleSendResponse", pduBuffer);
        }

        return ret;
    }

    public int cancelVoidSendResponse(int debug_console) {
        int ret = 0;
        ByteArrayOutputStream pduBuffer = new ByteArrayOutputStream();
        byte[] voidCancel = new byte[]{0x02, 'V', 'O', 'I', 'D', 0x30, 0x30, 0x32, 'C', 'A', 0x26, 0x03};

        pduBuffer.write(voidCancel, 0, voidCancel.length);

        if (debug_console > 0)
            Log.d("SendConsole:", "cancelVoidSendResponse: " + Convert.getInstance().bcdToStr(voidCancel));
        else {
            sendPdu("cancelSaleSendResponse", pduBuffer);
        }

        return ret;
    }

    public int cancelSettlementSendResponse(int debug_console) {
        int ret = 0;
        ByteArrayOutputStream pduBuffer = new ByteArrayOutputStream();
        byte[] settlementCancel = new byte[]{0x02, 'S', 'E', 'T', 'T', 0x30, 0x30, 0x32, 'C', 'A', 0x24, 0x03};

        pduBuffer.write(settlementCancel, 0, settlementCancel.length);

        if (debug_console > 0)
            Log.d("SendConsole:", "cancelSettlementSendResponse: " + Convert.getInstance().bcdToStr(settlementCancel));
        else {
            sendPdu("cancelSaleSendResponse", pduBuffer);
        }

        return ret;
    }

    public boolean TransactionAssert(byte[] transIn) {
        boolean ret = false;

        for (byte[] tranTable : TRANSACTION_TYPE_LIST) {
            if (Arrays.equals(transIn, tranTable)) {
                ret = true;
            }
        }
        return ret;
    }
}
