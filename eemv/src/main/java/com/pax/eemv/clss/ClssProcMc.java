/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-4-20
 * Module Author: linhb
 * Description:
 *
 * ============================================================================
 */
package com.pax.eemv.clss;

import android.util.Log;

import com.pax.eemv.IClssListener;
import com.pax.eemv.entity.AidParam;
import com.pax.eemv.entity.CTransResult;
import com.pax.eemv.entity.ClssTornLogRecord;
import com.pax.eemv.entity.TagsTable;
import com.pax.eemv.enums.ECvmResult;
import com.pax.eemv.enums.ETransResult;
import com.pax.eemv.exception.EmvException;
import com.pax.eemv.utils.Converter;
import com.pax.eemv.utils.Tools;
import com.pax.jemv.clcommon.ACType;
import com.pax.jemv.clcommon.ByteArray;
import com.pax.jemv.clcommon.CLSS_TORN_LOG_RECORD;
import com.pax.jemv.clcommon.Clss_PreProcInfo;
import com.pax.jemv.clcommon.Clss_TransParam;
import com.pax.jemv.clcommon.EMV_CAPK;
import com.pax.jemv.clcommon.EMV_REVOCLIST;
import com.pax.jemv.clcommon.RetCode;
import com.pax.jemv.clcommon.TransactionPath;
import com.pax.jemv.device.model.ApduRespL2;
import com.pax.jemv.device.model.ApduSendL2;
import com.pax.jemv.paypass.api.ClssPassApi;
import com.pax.jemv.paypass.listener.ClssPassCBFunApi;
import com.pax.jemv.paypass.listener.IClssPassCBFun;

import java.util.Arrays;

class ClssProcMc extends ClssProc {

    private static final int ERR_INDICATION = 0xDF8115;
    private static final int USER_INTER_REQ = 0xDF8116;
    private ClssPassListener clssPassListener = new ClssPassListener();
    private ClssPassCBFunApi passCBFun = ClssPassCBFunApi.getInstance();

    static {
//        System.loadLibrary("F_MC_LIB_Android");
//        System.loadLibrary("JniMC_V1.00.00_20170616");
        System.loadLibrary("F_MC_LIB_PayDroid");
        System.loadLibrary("JNI_MC_v100");
    }

    ClssProcMc(IClssListener listener) {
        super(listener);
    }

    private int init() {
        int ret = ClssPassApi.Clss_CoreInit_MC((byte) 0x01);
        Log.i(TAG, "Clss_CoreInit_MC = " + ret);
        if (ret != RetCode.EMV_OK) {
            return ret;
        }

        ret = setParamMc("010104", 3);
        Log.i(TAG, "Clss_SetParam_MC = " + ret);

        passCBFun.setICBFun(clssPassListener);
        ret = ClssPassApi.Clss_SetCBFun_SendTransDataOutput_MC();
        Log.i(TAG, "Clss_SetCBFun_SendTransDataOutput_MC = " + ret);
        ret = ClssPassApi.Clss_SetFinalSelectData_MC(finalSelectData, finalSelectDataLen);
        Log.i(TAG, "Clss_SetFinalSelectData_MC = " + ret);
        Log.i(TAG, "setDetData :" + Tools.bcd2Str(clssPassListener.outcomeParamSet.data));
        if (ret != RetCode.EMV_OK) {
            ByteArray aucOutcomeParamSet_MC = new ByteArray();
            getTlv(TagsTable.LIST, aucOutcomeParamSet_MC);
            if((clssPassListener.outcomeParamSet.data[1] & 0xF0) == 0x20) {//Start : C
                return RetCode.CLSS_RESELECT_APP;
            }
            return ret;
        }

        return ret;
    }

    private int setParamMc(String tlv, int len) {
        return ClssPassApi.Clss_SetParam_MC(Tools.str2Bcd(tlv), len);
    }

    private int selectApp() {
        for (Clss_PreProcInfo info : arrayPreProcInfo) {
            if (info != null && Arrays.equals(aid.getAid(), info.aucAID)) {
                setMcTermParam(transParam, aid, info);
            }
        }

        int ret = ClssPassApi.Clss_InitiateApp_MC();
        Log.i(TAG, "Clss_InitiateApp_MC = " + ret);
        if (ret != RetCode.EMV_OK) {
            ByteArray aucOutcomeParamSet_MC = new ByteArray();
            getTlv(TagsTable.LIST, aucOutcomeParamSet_MC);
            if((clssPassListener.outcomeParamSet.data[1] & 0xF0) == 0x20) {//Start : C
                return RetCode.CLSS_RESELECT_APP;
            }
            return ret;
        }
        Log.i(TAG, "setDetData :" + Tools.bcd2Str(clssPassListener.outcomeParamSet.data));

        ret = ClssPassApi.Clss_ReadData_MC(transactionPath);    // PathTypeOut
        Log.i(TAG, "Clss_ReadData_MC = " + ret);
        return ret;
    }

    private int processMChip(ACType acType) {
        ClssPassApi.Clss_DelAllRevocList_MC_MChip();
        ClssPassApi.Clss_DelAllCAPK_MC_MChip();
        addCapkRevList();

        if (tornLogRecords != null && !tornLogRecords.isEmpty()) {
            int tornSum = tornLogRecords.size();
            Log.i(TAG, "ClssTornLog = " + tornSum);
            CLSS_TORN_LOG_RECORD[] records = new CLSS_TORN_LOG_RECORD[tornSum];
            for (int i = 0; i < tornSum; ++i) {
                records[i] = Converter.toClssTornLogRecord(tornLogRecords.get(i));
            }
            ClssPassApi.Clss_SetTornLog_MC_MChip(records, tornSum);
        }

        int ret = ClssPassApi.Clss_TransProc_MC_MChip(acType);
        Log.i(TAG, "Clss_TransProc_MC_MChip = " + ret + "  ACType = " + acType.type);
        if (ret != RetCode.EMV_OK) {
            return ret;
        }

        CLSS_TORN_LOG_RECORD[] records = new CLSS_TORN_LOG_RECORD[5];
        int[] updateFlg = new int[2];
        ret = ClssPassApi.Clss_GetTornLog_MC_MChip(records, updateFlg);
        Log.i(TAG, "Clss_GetTornLog_MC_MChip = " + ret);
        if (ret == RetCode.EMV_OK && updateFlg[1] == 1 && tornLogRecords != null) {
            tornLogRecords.clear();
            for (CLSS_TORN_LOG_RECORD i : records) {
                tornLogRecords.add(new ClssTornLogRecord(i));
            }
        }
        return RetCode.EMV_OK;
    }

    private int processMag(ACType acType) {
        int ret = ClssPassApi.Clss_TransProc_MC_Mag(acType);
        Log.i(TAG, "Clss_TransProc_MC_Mag = " + ret + "  ACType = " + acType.type);
        return ret;
    }

    private CTransResult genTransResult() {

        Log.i(TAG, Tools.bcd2Str(clssPassListener.outcomeParamSet.data));
        switch (clssPassListener.outcomeParamSet.data[0] & 0xF0) {
            case 0x10:
                return new CTransResult(ETransResult.CLSS_OC_APPROVED);
            case 0x30:
                return new CTransResult(ETransResult.CLSS_OC_ONLINE_REQUEST);
            case 0x60:
                return new CTransResult(ETransResult.CLSS_OC_TRY_ANOTHER_INTERFACE);
            case 0x20:
            default:
                return new CTransResult(ETransResult.CLSS_OC_DECLINED);
        }
    }

    private void updateCVMResult(CTransResult result) {
        switch (clssPassListener.outcomeParamSet.data[3] & 0x30) {
            case 0x10:
                result.setCvmResult(ECvmResult.SIG);
                Log.i(TAG, "CVM = signature");
                break;
            case 0x20:
                result.setCvmResult(ECvmResult.ONLINE_PIN);
                Log.i(TAG, "CVM = online pin");
                break;
            default:
                result.setCvmResult(ECvmResult.NO_CVM);
                Log.i(TAG, "CVM = no cvm");
                break;
        }
    }

    @Override
    public CTransResult processTrans() throws EmvException {
        try {
            int ret = init();
            if (ret != RetCode.EMV_OK) {
                throw new EmvException(ret);
            }

            ret = selectApp();
            if (ret != RetCode.EMV_OK) {
                throw new EmvException(ret);
            }

            ACType acType = new ACType();
            if (transactionPath.path == TransactionPath.CLSS_MC_MCHIP) {// MChip = 6
                ret = processMChip(acType);

            } else if (transactionPath.path == TransactionPath.CLSS_MC_MAG) {// mag = 5
                ret = processMag(acType);
            }

            Log.i(TAG, "userInterReqData = "+ Tools.bcd2Str(clssPassListener.userInterReqData.data, 22));
            if (clssPassListener.userInterReqData.data[0]  == (byte)0x20) {//MI_SEE_PHONE
                if (listener != null) {
                    listener.onPromptRemoveCard();
                    ret = listener.onDisplaySeePhone();
                    if (ret == RetCode.EMV_OK) {
                        return new CTransResult(ETransResult.CLSS_OC_REFER_CONSUMER_DEVICE);
                    } else if (ret == RetCode.EMV_USER_CANCEL) {
                        throw new EmvException(ret);
                    }
                    return new CTransResult(ETransResult.ABORT_TERMINATED);
                }
            }

            if (ret != RetCode.EMV_OK) {
                throw new EmvException(ret);
            }

            if (acType.type == ACType.AC_AAC) {
                return new CTransResult(ETransResult.CLSS_OC_DECLINED);
            }

            Log.i(TAG, "setDetData :" + Tools.bcd2Str(clssPassListener.outcomeParamSet.data));
            if (clssPassListener.outcomeParamSet.data[0] == 0x70 || clssPassListener.outcomeParamSet.data[1] != (byte) 0xF0) {
                return new CTransResult(ETransResult.CLSS_OC_TRY_AGAIN);
            }

            if (listener != null) {
                listener.onPromptRemoveCard();
            }

            CTransResult result = genTransResult();
            updateCVMResult(result);
            return result;
        } finally {
            passCBFun.setICBFun(null);  //fix leaks
        }
    }

    private int setEmptyTlv(int tag) {
        byte[] bcdTag = Tools.int2ByteArray(tag);
        return ClssPassApi.Clss_SetTLVDataList_MC(bcdTag, 0);
    }

    @Override
    public int setTlv(int tag, byte[] value) {
        byte[] bcdTag = Tools.int2ByteArray(tag);
        byte[] buf = new byte[bcdTag.length + 1 + (value != null ? value.length : 0)];

        System.arraycopy(bcdTag, 0, buf, 0, bcdTag.length);
        if (value != null) {
            buf[bcdTag.length] = (byte) value.length;
            System.arraycopy(value, 0, buf, bcdTag.length + 1, value.length);
        } else {
            buf[bcdTag.length] = 0x00;
        }
        return ClssPassApi.Clss_SetTLVDataList_MC(buf, buf.length);
    }

    private void setMcTermParam(Clss_TransParam clssTransParam, AidParam aid, Clss_PreProcInfo info) {

        setTlv(TagsTable.CARD_DATA, new byte[]{(byte) 0xE0});
        setTlv(TagsTable.CVM_REQ, new byte[]{(byte) 0x20});//ONLINE PIN :40 /SIG:20 /OFFLINE PIN : 10 //0xF8
        setTlv(TagsTable.CVM_NO, new byte[]{(byte) 0x00});//0xF8
        setTlv(TagsTable.DEF_UDOL, new byte[]{(byte) 0x08});//08:CDA      //40:DDA
        //ONLINE ONLY : 21  OFFLINE ONLY : 23   ONLINE/OFFLINE : 22
        setTlv(0x9F35, new byte[]{0x22});

        setTlv(TagsTable.SEC, new byte[]{(byte) 0x9F, 0x6A, 0x04});

        setTlv(TagsTable.MAG_CVM_REQ, new byte[]{(byte) 0x40});//SIG:40 /ONLINE PIN: 20
        setTlv(TagsTable.MAG_CVM_NO, new byte[]{(byte) 0x00});

        byte[] tmp = Tools.str2Bcd(String.valueOf(clssTransParam.ulAmntAuth));
        byte[] amount = new byte[6];
        System.arraycopy(tmp, 0, amount, 6 - tmp.length, tmp.length);
        setTlv(TagsTable.AMOUNT, amount);

        tmp = Tools.str2Bcd(Long.toString(clssTransParam.ulAmntOther));
        amount = new byte[6];
        System.arraycopy(tmp, 0, amount, 6 - tmp.length, tmp.length);
        setTlv(TagsTable.AMOUNT_OTHER, amount);

        setTlv(TagsTable.TRANS_TYPE, new byte[]{clssTransParam.ucTransType});
        setTlv(TagsTable.TRANS_DATE, clssTransParam.aucTransDate);
        setTlv(TagsTable.TRANS_TIME, clssTransParam.aucTransTime);

        setTlv(TagsTable.APP_VER, aid.getVersion());
        setTlv(TagsTable.TERM_DEFAULT, aid.getRdClssTacDefault());
        setTlv(TagsTable.TERM_DENIAL, aid.getRdClssTacDenial());
        setTlv(TagsTable.TERM_ONLINE, aid.getRdClssTacOnline());

        setTlv(TagsTable.FLOOR_LIMIT, Tools.str2Bcd(Tools.getPaddedNumber(info.ulRdClssFLmt, 12)));
        setTlv(TagsTable.TRANS_LIMIT, Tools.str2Bcd(Tools.getPaddedNumber(info.ulRdClssTxnLmt, 12)));
        setTlv(TagsTable.TRANS_CVM_LIMIT, Tools.str2Bcd(Tools.getPaddedNumber(info.ulRdClssTxnLmt, 12)));
        setTlv(TagsTable.CVM_LIMIT, Tools.str2Bcd(Tools.getPaddedNumber(info.ulRdCVMLmt, 12)));

        setTlv(TagsTable.MAX_TORN, new byte[]{(byte) 0x00});
        setTlv(TagsTable.COUNTRY_CODE, Tools.str2Bcd(cfg.getCountryCode()));
        setTlv(TagsTable.CURRENCY_CODE, Tools.str2Bcd(cfg.getTransCurrCode()));

        if (listener != null) {
            byte[] kernelCfg = listener.onUpdateKernelCfg(Tools.bcd2Str(aid.getAid()));
            if (kernelCfg != null) {
                setTlv(TagsTable.KERNEL_CFG, kernelCfg);
            }
        }

        setTlv(TagsTable.ACCOUNT_TYPE, null);
        setTlv(TagsTable.ACQUIRER_ID, aid.getAcquirerId());//null
        setTlv(TagsTable.INTER_DEV_NUM, Tools.str2Bcd("1122334455667788"));
        setTlv(TagsTable.MERCHANT_CATEGORY_CODE, Tools.str2Bcd("0001"));
        setTlv(TagsTable.MERCHANT_ID, null);
        setTlv(TagsTable.MERCHANT_NAME_LOCATION, null);
        setTlv(TagsTable.TERMINAL_CAPABILITY, Tools.str2Bcd(cfg.getCapability()));//null
        setTlv(TagsTable.TERMINAL_ID, null);
        setTlv(TagsTable.MOB_SUP, null);

        setTlv(TagsTable.DS_AC_TYPE, null);
        setTlv(TagsTable.DS_INPUT_CARD, null);
        setTlv(TagsTable.DS_INPUT_TERMINAL, null);
        setTlv(TagsTable.DS_ODS_INFO, null);
        setTlv(TagsTable.DS_ODS_READER, null);
        setTlv(TagsTable.DS_ODS_TERMINAL, null);

        setTagPresent(TagsTable.BALANCE_BEFORE_GAC, false);
        setTagPresent(TagsTable.BALANCE_AFTER_GAC, false);
        setTagPresent(TagsTable.MESS_HOLD_TIME, false);

        setEmptyTlv(TagsTable.FST_WRITE);
        setEmptyTlv(TagsTable.READ);
        setEmptyTlv(TagsTable.WIRTE_BEFORE_AC);
        setEmptyTlv(TagsTable.WIRTE_AFTER_AC);
        setEmptyTlv(TagsTable.TIMEOUT);

        setTlv(TagsTable.DS_OPERATOR_ID, Tools.str2Bcd("7A45123EE59C7F40"));
        setTagPresent(0xDF810D, false);
        setTagPresent(0x9F70, false);
        setTagPresent(0x9F75, false);

        setTlv(TagsTable.ADDITIONAL_CAPABILITY, Tools.str2Bcd("0000000000"));//additional capability
        setTlv(0x9F6D, new byte[]{0x00, 0x01});

        setTlv(0xDF811C, new byte[]{0x00, 0x00});
        setTlv(0xDF810C, new byte[]{0x02});
        setTagPresent(0xDF8130, false);
        setTagPresent(0xDF812D, false);

        //AET-164
//        setTlv(0xDF811A, Tools.str2Bcd("9F6A04")); //already set
//        setTlv(0xDF811E, new byte[]{0x20}); //already set
//        setTlv(0xDF812C, new byte[]{0x00}); //already set
//        setTlv(0xDF811B, new byte[]{0x00}); //already set
        setTlv(0x9F1D, Tools.str2Bcd("2430000000000000"));
    }

    private void setTagPresent(int tag, boolean present) {
        ClssPassApi.Clss_SetTagPresent_MC(Tools.int2ByteArray(tag), Tools.boolean2Byte(present));
    }

    @Override
    public int getTlv(int tag, ByteArray value) {
        byte[] bcdTag = Tools.int2ByteArray(tag);

        int ret = ClssPassApi.Clss_GetTLVDataList_MC(bcdTag, (byte) bcdTag.length, value.length, value);
        Log.i(TAG, " getClssTlv_MC  tag :" + tag
                + " value: " + Tools.bcd2Str(value.data).substring(0, 2 * value.length) + " ret :" + ret);
        return ret;
    }

    @Override
    protected void onAddCapkRevList(EMV_CAPK emvCapk, EMV_REVOCLIST emvRevoclist) {
        int ret = ClssPassApi.Clss_AddCAPK_MC_MChip(emvCapk);
        Log.i(TAG, "set MC capk ret :" + ret);
        ret = ClssPassApi.Clss_AddRevocList_MC_MChip(emvRevoclist);
        Log.i(TAG, "set MC revoclist ret :" + ret);
    }

    @Override
    protected String getTrack1() {
        ByteArray track = new ByteArray();
        int ret = getTlv(TagsTable.TRACK1, track);
        if (ret == RetCode.EMV_OK)
            return Tools.bcd2Str(track.data, track.length);
        return "";
    }

    @Override
    protected String getTrack2() {
        ByteArray track = new ByteArray();
        int ret = -1;
        if (transactionPath.path == TransactionPath.CLSS_MC_MCHIP) {
            ret = getTlv(TagsTable.TRACK2, track);
        } else if (transactionPath.path == TransactionPath.CLSS_MC_MAG) {
            ret = getTlv(TagsTable.TRACK2_1, track);
        }

        if (ret == RetCode.EMV_OK) {
            //AET-173
            return getTrack2FromTag57(Tools.bcd2Str(track.data, track.length));
        }
        return "";
    }

    @Override
    protected String getTrack3() {
        return "";
    }

    @Override
    protected CTransResult completeTrans(ETransResult transResult, byte[] tag91, byte[] tag71, byte[] tag72) {
        //do nothing
        return new CTransResult(ETransResult.CLSS_OC_APPROVED);
    }

    private class ClssPassListener implements IClssPassCBFun {
        ByteArray outcomeParamSet = new ByteArray(8);
        ByteArray userInterReqData = new ByteArray(22);
        ByteArray errIndication = new ByteArray(6);

        @Override
        public int sendDEKData(byte[] bytes, int i) {
            return 0;
        }

        @Override
        public int receiveDETData(ByteArray byteArray, byte[] bytes) {
            return 0;
        }

        @Override
        public int addAPDUToTransLog(ApduSendL2 apduSendL2, ApduRespL2 apduRespL2) {
            return 0;
        }

        @Override
        public int sendTransDataOutput(byte b) {
            if ((b & 0x01) != 0) {
                getTlv(TagsTable.LIST, outcomeParamSet);
            }

            if ((b & 0x04) != 0) {
                getTlv(USER_INTER_REQ, userInterReqData);
            }

            if ((b & 0x02) != 0) {
                getTlv(ERR_INDICATION, errIndication);
            }
            return RetCode.EMV_OK;
        }
    }
}
