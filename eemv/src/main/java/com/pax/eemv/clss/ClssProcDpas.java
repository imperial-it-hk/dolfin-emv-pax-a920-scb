/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-7-17
 * Module Author: huangwp
 * Description:
 *
 * ============================================================================
 */

package com.pax.eemv.clss;

import android.util.Log;

import com.pax.eemv.EmvImpl;
import com.pax.eemv.IClssListener;
import com.pax.eemv.entity.CTransResult;
import com.pax.eemv.entity.TagsTable;
import com.pax.eemv.enums.ECvmResult;
import com.pax.eemv.enums.ETransResult;
import com.pax.eemv.exception.EmvException;
import com.pax.eemv.utils.Tools;
import com.pax.jemv.clcommon.ByteArray;
import com.pax.jemv.clcommon.Clss_PreProcInterInfo;
import com.pax.jemv.clcommon.EMV_CAPK;
import com.pax.jemv.clcommon.EMV_REVOCLIST;
import com.pax.jemv.clcommon.RetCode;
import com.pax.jemv.clcommon.TransactionPath;
import com.pax.jemv.dpas.api.ClssDPASApi;
import com.pax.jemv.entrypoint.api.ClssEntryApi;

import java.util.Arrays;


class ClssProcDpas extends ClssProc {
    private static final String TAG = "ClssProcDpas";
    private ClssDpassSendOutcome sendOutcome;
    private Clss_PreProcInterInfo clssPreProcInterInfo;

    static {
        System.loadLibrary("F_DPAS_LIB_Android");
        System.loadLibrary("JniDpas_V1.00.00_20170823");
    }

    ClssProcDpas(IClssListener listener) {
        super(listener);
        sendOutcome = new ClssDpassSendOutcome();
        clssPreProcInterInfo = new Clss_PreProcInterInfo();
    }

    private int init() {
        int ret;

        ret = ClssDPASApi.Clss_CoreInit_DPAS();
        if (ret != RetCode.EMV_OK) {
            return ret;
        }

        ret = ClssDPASApi.Clss_SetFinalSelectData_DPAS(finalSelectData, finalSelectDataLen);
        if (ret != RetCode.EMV_OK) {
            return ret;
        }

        return ClssEntryApi.Clss_GetPreProcInterFlg_Entry(clssPreProcInterInfo);
    }

    private void setParam() {
        setTlv(TagsTable.APP_VER, aid.getVersion());
        setTlv(TagsTable.TERM_DEFAULT, aid.getTacDefault());
        setTlv(TagsTable.TERM_DENIAL, aid.getTacDenial());
        setTlv(TagsTable.TERM_ONLINE, aid.getTacOnline());

        setTlv(TagsTable.TERMINAL_CAPABILITY, new byte[]{(byte) 0xE0, (byte) 0xE1, (byte) 0xC8});
        setTlv(TagsTable.ACQUIRER_ID, new byte[]{0x00, 0x00, 0x00, 0x12, 0x34, 0x56});
        setTlv(TagsTable.MERCHANT_CATEGORY_CODE, Tools.str2Bcd("0001"));
        setTlv(TagsTable.MERCHANT_NAME_LOCATION, new byte[]{0x00});
        setTlv(TagsTable.COUNTRY_CODE, Tools.str2Bcd(cfg.getCountryCode()));
        setTlv(0x9F35, new byte[]{0x22});//TerminalType
        setTlv(TagsTable.CURRENCY_CODE, Tools.str2Bcd(cfg.getTransCurrCode()));
        setTlv(0x5F36, new byte[]{0x02});//Transaction Currency Exponent
        setTlv(0x9F66, clssPreProcInterInfo.aucReaderTTQ);

        if (clssPreProcInterInfo.ucRdCLFLmtExceed == 1) {
            ByteArray TVR = new ByteArray();
            getTlv(0x95, TVR);
            TVR.data[3] = (byte) (TVR.data[3] | 0x80);
            setTlv(0x95, Arrays.copyOfRange(TVR.data, 0, 5));
        }

        byte[] tmp = Tools.str2Bcd(String.valueOf(transParam.ulAmntAuth));
        byte[] amount = new byte[6];
        System.arraycopy(tmp, 0, amount, 6 - tmp.length, tmp.length);
        setTlv(TagsTable.AMOUNT, amount);

        tmp = Tools.str2Bcd(Long.toString(transParam.ulAmntOther));
        amount = new byte[6];
        System.arraycopy(tmp, 0, amount, 6 - tmp.length, tmp.length);
        setTlv(TagsTable.AMOUNT_OTHER, amount);

        setTlv(TagsTable.TRANS_TYPE, new byte[]{transParam.ucTransType});
        setTlv(TagsTable.TRANS_DATE, transParam.aucTransDate);
        setTlv(TagsTable.TRANS_TIME, transParam.aucTransTime);
    }

    @Override
    CTransResult processTrans() throws EmvException {
        int ret = init();
        if (ret != RetCode.EMV_OK) {
            throw new EmvException(ret);
        }

        setParam();

        ret = ClssDPASApi.Clss_InitiateApp_DPAS(transactionPath);
        if (ret != RetCode.EMV_OK) {
            throw new EmvException(ret);
        }

        ret = processDpas(transactionPath);
        if (ret != RetCode.EMV_OK) {
            throw new EmvException(ret);
        }
        if (listener != null) {
            listener.onPromptRemoveCard();
        }

        CTransResult result = genTransResult();
        updateCVMResult(result);
        return result;
    }

    private int processDpas(TransactionPath pathType) throws EmvException {
        int ret;
        ret = ClssDPASApi.Clss_ReadData_DPAS();
        if (ret != RetCode.EMV_OK) {
            throw new EmvException(ret);
        }

        if (pathType.path == TransactionPath.CLSS_DPAS_EMV) {
            ClssDPASApi.Clss_DelAllRevocList_DPAS();
            ClssDPASApi.Clss_DelAllCAPK_DPAS();
            addCapkRevList();
        }

        ret = ClssDPASApi.Clss_TransProc_DPAS((byte) 0x00);
        Log.i(TAG, "ClssProcDpas ClssDPASApi.Clss_TransProc_DPAS ret = " + ret);
        Log.i(TAG, "ClssProcDpas ClssDPASApi.Clss_GetDebugInfo_DPAS ret = " + ClssDPASApi.Clss_GetDebugInfo_DPAS());
        sendOutcome.sendTransDataOutput((byte) 0x07);

        return ret;
    }

    private CTransResult genTransResult() {
        if (sendOutcome.outcomeParamSet.data[0] == 0x70 || sendOutcome.outcomeParamSet.data[1] != (byte) 0xF0) {
            return new CTransResult(ETransResult.CLSS_OC_TRY_AGAIN);
        }

        switch (sendOutcome.outcomeParamSet.data[0] & 0xF0) {
            case 0x10:
                Log.i(TAG, "genTransResult CLSS_OC_APPROVED");
                return new CTransResult(ETransResult.CLSS_OC_APPROVED);
            case 0x30:
                Log.i(TAG, "genTransResult CLSS_OC_ONLINE_REQUEST");
                return new CTransResult(ETransResult.CLSS_OC_ONLINE_REQUEST);
            case 0x60:
                Log.i(TAG, "genTransResult CLSS_OC_TRY_ANOTHER_INTERFACE");
                return new CTransResult(ETransResult.CLSS_OC_TRY_ANOTHER_INTERFACE);
            case 0x20:
                Log.i(TAG, "genTransResult CLSS_OC_DECLINED");
                return new CTransResult(ETransResult.CLSS_OC_DECLINED);
            default:
                Log.i(TAG, "default genTransResult CLSS_OC_DECLINED");
                return new CTransResult(ETransResult.CLSS_OC_DECLINED);
        }
    }

    private void updateCVMResult(CTransResult result) {
        switch (sendOutcome.outcomeParamSet.data[3] & 0x30) {
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
    protected CTransResult completeTrans(ETransResult result, byte[] tag91, byte[] tag71, byte[] tag72) throws EmvException {
        int ret;

        Log.i(TAG, "ClssProcDpas completeTrans) = " + result);
        byte[] script = EmvImpl.clssCombine7172(tag71, tag72);
        if (script == null)
            script = new byte[0];

        ret = ClssDPASApi.Clss_IssuerUpdateProc_DPAS(result.ordinal(), script, script.length);
        Log.i(TAG, "ClssProcDpas Clss_IssuerUpdateProc_DPAS result.ordinal() = " + result.ordinal() + "ret =" + ret);
        if (ret != RetCode.EMV_OK) {
            throw new EmvException(ret);
        }

        return new CTransResult(ETransResult.CLSS_OC_APPROVED);
    }

    @Override
    protected void onAddCapkRevList(EMV_CAPK emvCapk, EMV_REVOCLIST emvRevoclist) {
        int ret = ClssDPASApi.Clss_AddCAPK_DPAS(emvCapk);
        Log.i(TAG, "Clss_AddCAPK_DPAS ret = " + ret);
        ret = ClssDPASApi.Clss_AddRevocList_DPAS(emvRevoclist);
        Log.i(TAG, "Clss_AddRevocList_DPAS ret = " + ret);
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

        int ret = ClssDPASApi.Clss_SetTLVDataList_DPAS(buf, buf.length);
        Log.i(TAG, "ClssProcDpas ClssDPASApi.Clss_SetTLVDataList_DPAS() ret = " + ret + "buf = " + Tools.bcd2Str(buf));
        return ret;
    }

    @Override
    protected int getTlv(int tag, ByteArray value) {
        int ret;
        byte[] bcdTag = Tools.int2ByteArray(tag);

        ret = ClssDPASApi.Clss_GetTLVDataList_DPAS(bcdTag, (byte) bcdTag.length, value.length, value);
        Log.i(TAG, "Dpas getTlv  tag :" + tag
                + " value: " + Tools.bcd2Str(value.data).substring(0, 2 * value.length) + " ret :" + ret + "value.length = " + value.length);
        return ret;
    }

    @Override
    protected String getTrack1() {
        ByteArray track = new ByteArray();
        Log.i(TAG, "ClssProcDpas getTrack1 ");
        if ((transactionPath.path == TransactionPath.CLSS_DPAS_MAG)
                || (transactionPath.path == TransactionPath.CLSS_DPAS_ZIP)) {
            ClssDPASApi.Clss_GetTrackMapData_DPAS((byte) 0x01, track);
            return Tools.bcd2Str(track.data, track.length);
        }
        return "";

    }

    @Override
    protected String getTrack2() {
        ByteArray track = new ByteArray();
        if ((transactionPath.path == TransactionPath.CLSS_DPAS_MAG)
                || (transactionPath.path == TransactionPath.CLSS_DPAS_ZIP)) {
            ClssDPASApi.Clss_GetTrackMapData_DPAS((byte) 0x02, track);
            return new String(Arrays.copyOfRange(track.data, 1, track.length - 2));

        }

        if (getTlv(TagsTable.TRACK2, track) == RetCode.EMV_OK) {
            return getTrack2FromTag57(Tools.bcd2Str(track.data, track.length));
        }
        return "";
    }

    @Override
    protected String getTrack3() {
        return "";
    }

    private class ClssDpassSendOutcome {
        ByteArray outcomeParamSet = new ByteArray(8);
        ByteArray userInterReqData = new ByteArray(22);
        ByteArray errIndication = new ByteArray(6);

        void sendTransDataOutput(byte b) {
            if ((b & 0x01) != 0) {
                getTlv(TagsTable.LIST, outcomeParamSet);
            }

            if ((b & 0x04) != 0) {
                getTlv(0xDF8116, userInterReqData);
            }

            if ((b & 0x02) != 0) {
                getTlv(0xDF8115, errIndication);
            }
        }
    }
}
