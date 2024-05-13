package com.pax.pay.uart;

import th.co.bkkps.utils.Log;

import com.pax.pay.ECR.EcrProcessClass.*;
import com.pax.pay.ECR.HyperCommClass.*;
import com.pax.pay.ECR.PosNetCommClass.*;

import java.util.Arrays;

import static com.pax.pay.ECR.PosNetCommClass.TRANSACTION_TYPE_LIST;

public class ProtoFilterClass {
    private String TAG = "ProtoFilterClass:";
    private final byte ACK = 0x06;
    private final byte NACK = 0x15;
    private final byte STX = 0x02;
    private final byte ETX = 0x03;
    private static PROTOCOL mProtoSelect = PROTOCOL.AUTO;
    private static PROTOCOL_RESULT mProtocolResult = PROTOCOL_RESULT.UNKNOWN;

    private PAXCommState mPAXCommState;
    private CommManageClass mCommManage;
    private PosNetCommState mPosNetCommState;
    private CommManageInterface mHyperComRcvCbk;
    private CommManageInterface mPosNetRcvCbk;

    public ProtoFilterClass(CommManageClass CommManage) {
        mPAXCommState = PAXCommState.SOF;
        mPosNetCommState = PosNetCommState.SOF;
        mCommManage = CommManage;
    }

    public void AddHyperComListener(CommManageInterface RcvCbk) {
        mHyperComRcvCbk = RcvCbk;
    }

    public void AddPosNetListener(CommManageInterface RcvCbk) {
        mPosNetRcvCbk = RcvCbk;
    }

    public PROTOCOL getProtoSelect() {
        return mProtoSelect;
    }

    public void setProtoSelect(PROTOCOL protoSelect) {
        mProtoSelect = protoSelect;
    }

    public void setProtoSelect(String protoSelect) {
        switch (protoSelect) {
            case "AUTO":
                mProtoSelect = PROTOCOL.AUTO;
                break;
            case "DISABLE":
                mProtoSelect = PROTOCOL.DISABLE;
                break;
            case "HYPERCOM":
                mProtoSelect = PROTOCOL.HYPERCOM;
                break;
            case "POSNET":
                mProtoSelect = PROTOCOL.POSNET;
                break;
        }
    }

    public PROTOCOL_RESULT getProtocolResult() {
        return mProtocolResult;
    }

    public PROTOCOL_RESULT protoIdentifier(byte[] buf) {
        if (mProtoSelect != PROTOCOL.DISABLE) {
            if ((mProtoSelect == PROTOCOL.AUTO) || (mProtoSelect == PROTOCOL.HYPERCOM) || (mProtoSelect == PROTOCOL.AUTO_HYPERCOM_IND))
                if (filterHyperComProto(buf) == 1) {
                    if (mHyperComRcvCbk != null) {
                        int rc = mHyperComRcvCbk.onReceive(buf, buf.length);
                        if (rc == -1) {
                            mCommManage.StartReceive();
                        }
                        return PROTOCOL_RESULT.HYPERCOM;
                    }
                }

            if ((mProtoSelect == PROTOCOL.AUTO) || (mProtoSelect == PROTOCOL.POSNET) || (mProtoSelect == PROTOCOL.AUTO_POSNET_IND))
                if (filterPosNetProto(buf) == 1) {
                    if (mPosNetRcvCbk != null) {
                        int rc = mPosNetRcvCbk.onReceive(buf, buf.length);
                        if (rc == -1) {
                            mCommManage.StartReceive();
                        }
                        return PROTOCOL_RESULT.POSNET;
                    }
                }

            if (mCommManage.MainIO != null) {
                mCommManage.StartReceive();
                mCommManage.MainIO.Write(new byte[]{NACK});
            }
            return PROTOCOL_RESULT.UNKNOWN;
        }
        mProtocolResult = PROTOCOL_RESULT.DISABLE;
        return mProtocolResult;
    }

    byte[] lenBuffer = new byte[4];
    int msgLen;
    byte msgLRC;
    int i_tmp = 0;

    public int filterHyperComProto(byte[] buf) {
        mPAXCommState = PAXCommState.SOF;

        for (int i_cnt = 0; i_cnt < buf.length; i_cnt++) {
            switch (mPAXCommState) {
                case SOF:
                    if (buf[i_cnt] == STX) {
                        i_tmp = 0;
                        msgLRC = 0;
                        mPAXCommState = PAXCommState.LEN;
                    }
                    break;

                case LEN:
                    lenBuffer[i_tmp++] = buf[i_cnt];
                    msgLRC ^= buf[i_cnt];
                    if (i_tmp == 2) {
                        i_tmp = 0;
                        msgLen = (((lenBuffer[0] & 0xFF) >> 4) * 1000);//
                        msgLen += ((lenBuffer[0] & 0xF) * 100);//
                        msgLen += (((lenBuffer[1] & 0xFF) >> 4) * 10);//
                        msgLen += ((lenBuffer[1] & 0xF));//

                        if ((msgLen >= 1024) || (msgLen < 15)) {
                            Log.d(TAG, "HyperComm: LEN_FAIL = " + msgLen);
                            return -1;
                        } else {
                            Log.d(TAG, "HyperComm: LEN_PASS = " + msgLen);
                            mPAXCommState = PAXCommState.MSG;
                        }
                    }

                    break;

                case MSG:
                    msgLRC ^= buf[i_cnt];
                    if (++i_tmp == msgLen) {
                        //    Log.d(TAG, "HyperComm: MSG_PASS");
                        mPAXCommState = PAXCommState.EOF;
                    }

                    break;
                case EOF:
                    msgLRC ^= buf[i_cnt];

                    if (buf[i_cnt] == ETX) {
                        Log.d(TAG, "HyperComm: EOF_PASS");
                        mPAXCommState = PAXCommState.LRC;
                    } else {
                        return -1;
                        //Log.d(TAG, "EOF.............");
                    }
                    break;

                case LRC:
                    if (msgLRC == buf[i_cnt]) {
                        //     Log.d(TAG, "HyperComm: LRC_PASS");
                        mProtoSelect = PROTOCOL.AUTO_HYPERCOM_IND;
                        if (mProtoSelect == PROTOCOL.AUTO)
                            mProtoSelect = PROTOCOL.AUTO_HYPERCOM_IND;

                        if ((mProtoSelect == PROTOCOL.AUTO_HYPERCOM_IND) || (mProtoSelect == PROTOCOL.HYPERCOM)) {
                            return 1;
                        } else return 0;
                    } else {
                        //    Log.d(TAG, "HyperComm: LRC_FAIL, calLRC = " + msgLRC + " rcvLRC = " + buf[i_cnt]);
                        return -1;
                    }

                default:
                    mPAXCommState = PAXCommState.SOF;
                    break;
            }

        }

        if (buf != null) if (buf.length > 0) if (buf[0] == ACK) return 1;

        return 0;
    }


    byte[] transBuffer = new byte[5];
    byte tmpLRC;

    public int filterPosNetProto(byte[] buf) {

        mPosNetCommState = PosNetCommState.SOF;

        for (int i_cnt = 0; i_cnt < buf.length; i_cnt++) {
            switch (mPosNetCommState) {
                case SOF:
                    if (buf[i_cnt] == STX) {
                        i_tmp = 0;
                        msgLRC = 0;
                        msgLRC ^= buf[i_cnt];
                        mPosNetCommState = PosNetCommState.TRANS;
                        Log.d(TAG, "PosNetComm: SOF_PASS");
                    } else if (buf[i_cnt] == ACK) {
                        continue;
                    }
                    break;
                case TRANS:
                    transBuffer[i_tmp++] = buf[i_cnt];
                    msgLRC ^= buf[i_cnt];
                    if (i_tmp == 4) {
                        transBuffer[4] = 0;
                        i_tmp = 0;
                        byte[] tmp = new byte[4];

                        System.arraycopy(transBuffer, 0, tmp, 0, 4);

                        if (TransactionAssert(tmp)) {
                            mPosNetCommState = PosNetCommState.LEN;
                            Log.d(TAG, "PosNetComm: TRANS_PASS = " + new String(transBuffer));
                        } else {
                            Log.d(TAG, "PosNetComm: TRANS_FAIL = " + new String(tmp));
                            return -1;
                        }

                    }

                    break;

                case LEN:
                    lenBuffer[i_tmp++] = buf[i_cnt];
                    msgLRC ^= buf[i_cnt];
                    if (i_tmp == 3) {
                        lenBuffer[3] = 0;
                        i_tmp = 0;
                        byte[] tmp = new byte[3];
                        System.arraycopy(lenBuffer, 0, tmp, 0, 3);
                        if (((tmp[0] < 0x30) || (tmp[0] > 0x39)) || ((tmp[1] < 0x30) || (tmp[1] > 0x39)) || ((tmp[2] < 0x30) || (tmp[2] > 0x39))) {
                            return -1;
                        } else {
                            msgLen = Integer.parseInt(new String(tmp));
                            Log.d(TAG, "PosNetComm: LEN_PASS = " + msgLen);
                            if (msgLen == 0) {
                                mPosNetCommState = PosNetCommState.CHKSUM;
                            } else {
                                mPosNetCommState = PosNetCommState.DATA;
                            }
                        }
                    }
                    break;

                case DATA:

                    msgLRC ^= buf[i_cnt];

                    if (++i_tmp == msgLen) {

                        Log.d(TAG, "PosNetComm: DATA_PASS");
                        mPosNetCommState = PosNetCommState.CHKSUM;
                    }

                    if (i_tmp > msgLen) {
                        return -1;
                    }
                    //Log.d("PosNetComm:", "DATA" + i_tmp + "=" + data[i_cnt]);

                    break;

                case CHKSUM:
                    tmpLRC = buf[i_cnt];
                    mPosNetCommState = PosNetCommState.EOF;
                    break;

                case EOF:

                    if (buf[i_cnt] == ETX) {
                        Log.d(TAG, "PosNetComm: EOF_PASS");
                        if (tmpLRC == msgLRC) {
                            Log.d(TAG, "PosNetComm: CHKSUM_PASS");
                            if (mProtoSelect == PROTOCOL.AUTO)
                                mProtoSelect = PROTOCOL.AUTO_POSNET_IND;

                            if ((mProtoSelect == PROTOCOL.AUTO_POSNET_IND) || (mProtoSelect == PROTOCOL.POSNET)) {
                                return 1;
                            } else return 0;
                        } else {
                            Log.d(TAG, "PosNetComm: CHKSUM_FAIL, calCHKSUM = " + msgLRC + " rcvCHKSUM = " + tmpLRC);
                            return -1;
                        }
                    }
                    break;
                default:
                    mPosNetCommState = PosNetCommState.SOF;
                    break;
            }

        }

        //if (buf != null) if (buf[0] == ACK) return 1;
        if (buf != null)
            if (buf.length > 0)
                if (buf[0] == ACK)
                    return 1;

        return 0;
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
