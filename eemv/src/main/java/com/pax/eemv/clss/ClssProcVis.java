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

import com.pax.eemv.EmvImpl;
import com.pax.eemv.IClssListener;
import com.pax.eemv.entity.CTransResult;
import com.pax.eemv.entity.TagsTable;
import com.pax.eemv.enums.ETransResult;
import com.pax.eemv.exception.EmvException;
import com.pax.eemv.utils.Converter;
import com.pax.eemv.utils.Tools;
import com.pax.jemv.clcommon.ACType;
import com.pax.jemv.clcommon.ByteArray;
import com.pax.jemv.clcommon.Clss_PreProcInfo;
import com.pax.jemv.clcommon.Clss_ProgramID;
import com.pax.jemv.clcommon.Clss_VisaAidParam;
import com.pax.jemv.clcommon.DDAFlag;
import com.pax.jemv.clcommon.EMV_CAPK;
import com.pax.jemv.clcommon.EMV_REVOCLIST;
import com.pax.jemv.clcommon.KernType;
import com.pax.jemv.clcommon.RetCode;
import com.pax.jemv.clcommon.TransactionPath;
import com.pax.jemv.entrypoint.api.ClssEntryApi;
import com.pax.jemv.paywave.api.ClssWaveApi;

import java.util.Arrays;

class ClssProcVis extends ClssProc {

    private Clss_PreProcInfo clssPreProcInfo;
    private String track2;

    static {
//        System.loadLibrary("F_WAVE_LIB_Android");
//        System.loadLibrary("JniWave_V1.00.00_20170616");
        System.loadLibrary("F_WAVE_LIB_PayDroid");
        System.loadLibrary("JNI_WAVE_v100");
    }

    ClssProcVis(IClssListener listener) {
        super(listener);
    }

    private int init() {
        int ret = ClssWaveApi.Clss_CoreInit_Wave();
        Log.i("clssWaveCoreInit", "ret = " + ret);
        if (ret != RetCode.EMV_OK) {
            return ret;
        }

        ret = ClssWaveApi.Clss_SetFinalSelectData_Wave(finalSelectData, finalSelectDataLen);
        Log.i("WaveSetFinalSelectData", "ret = " + ret);
        if (ret != RetCode.EMV_OK) {
            return ret;
        }

        setTlv(TagsTable.COUNTRY_CODE, Tools.str2Bcd(cfg.getCountryCode()));
        setTlv(TagsTable.CURRENCY_CODE, Tools.str2Bcd(cfg.getTransCurrCode()));

        return ClssWaveApi.Clss_SetReaderParam_Wave(Converter.toClssReaderParam(cfg));
    }

    private int setTransParam() {
        byte[] cvmTypes = Converter.toCvmTypes(inputParam.getCvmReq());
        int ret = ClssWaveApi.Clss_SetVisaAidParam_Wave(
                new Clss_VisaAidParam(aid.getRdClssFLmt(), Tools.boolean2Byte(inputParam.isDomesticOnly()),
                        (byte) cvmTypes.length, cvmTypes, inputParam.getEnDDAVerNo()));
        Log.i("clssWaveSetVisaAidParam", "ret = " + ret);
        if (ret != RetCode.EMV_OK) {
            return ret;
        }

        setTlv(0x9F5A, "123".getBytes()); // why?

        ByteArray proID = new ByteArray();
        ret = getTlv(TagsTable.PRO_ID, proID);
        if (ret != RetCode.EMV_OK) {
            return ret;
        }

        for (Clss_PreProcInfo i : arrayPreProcInfo) {
            if (Arrays.equals(aid.getAid(), i.aucAID)) {
                clssPreProcInfo = i;
                break;
            }
        }

        if (clssPreProcInfo != null) {
            Clss_ProgramID clssProgramID = new Clss_ProgramID(clssPreProcInfo.ulRdClssTxnLmt, clssPreProcInfo.ulRdCVMLmt,
                    clssPreProcInfo.ulRdClssFLmt, clssPreProcInfo.ulTermFLmt, proID.data, (byte) proID.length,
                    clssPreProcInfo.ucRdClssFLmtFlg, clssPreProcInfo.ucRdClssTxnLmtFlg, clssPreProcInfo.ucRdCVMLmtFlg,
                    clssPreProcInfo.ucTermFLmtFlg, clssPreProcInfo.ucStatusCheckFlg, (byte) 0, new byte[4]);
            ret = ClssWaveApi.Clss_SetDRLParam_Wave(clssProgramID);
            Log.i("clssWaveSetDRLParam", "ret = " + ret);
            if (ret != RetCode.EMV_OK) {
                return ret;
            }
        }

        setTlv(0x9C, new byte[]{transParam.ucTransType});
        ret = ClssWaveApi.Clss_SetTransData_Wave(transParam, preProcInterInfo);
        Log.i("clssWaveSetTransData", "ret = " + ret);
        return ret;
    }

    @Override
    protected CTransResult processTrans() throws EmvException {
        while (true) {
            int ret = init();
            if (ret != RetCode.EMV_OK) {
                throw new EmvException(ret);
            }

            ret = setTransParam();
            if (ret != RetCode.EMV_OK) {
                throw new EmvException(ret);
            }

            ACType acType = new ACType();
            ret = ClssWaveApi.Clss_Proctrans_Wave(transactionPath, acType);
            Log.i("clssWaveProcTrans", "ret = " + ret);
            if (ret != RetCode.EMV_OK) {

                if (ret == RetCode.CLSS_RESELECT_APP) {
                    ret = ClssEntryApi.Clss_DelCurCandApp_Entry();
                    if (ret != RetCode.EMV_OK) {
                        throw new EmvException(ret);
                    }
                    continue;
                }

                //On Device CVM, show message 'Please See Phone'
                if (ret == RetCode.CLSS_REFER_CONSUMER_DEVICE) {
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
                } else if (ret == RetCode.CLSS_USE_CONTACT) {
                    //CLSS_OC_TRY_ANOTHER_INTERFACE is equal to CLSS_USE_CONTACT
                    return new CTransResult(ETransResult.CLSS_OC_TRY_ANOTHER_INTERFACE);
                }

                throw new EmvException(ret);
            }
            Log.i("clssWaveProcTrans", "TransPath = " + transactionPath.path + ", ACType = " + acType.type);

            if (listener != null) {
                listener.onPromptRemoveCard();
            }

            CTransResult result = new CTransResult(ETransResult.ABORT_TERMINATED);
            if (!continueUpdateResult(acType, result)) {
                return result;
            }

            updateResult(result);
            return result;
        }
    }

    private void updateResult(CTransResult result) throws EmvException {
        byte cvmType = ClssWaveApi.Clss_GetCvmType_Wave();
        Log.i("clssWaveGetCvmType", "CVMType = " + cvmType);
        if (cvmType < 0) {
            if (cvmType == RetCode.CLSS_DECLINE) {
                result.setTransResult(ETransResult.CLSS_OC_DECLINED);
            }
            throw new EmvException(cvmType);
        }
        result.setCvmResult(Converter.convertCVM(cvmType));
    }

    private int processMSD() throws EmvException {
        byte msdType = ClssWaveApi.Clss_GetMSDType_Wave();
        Log.i("clssWaveGetMSDType", "msdType = " + msdType);
        //get MSD track 2 data
        ByteArray waveGetTrack2List = new ByteArray();
        int ret = ClssWaveApi.Clss_nGetTrack2MapData_Wave(waveGetTrack2List);
        if (ret != RetCode.EMV_OK) {
            throw new EmvException(ret);
        }

        track2 = getTrack2FromTag57(Tools.bcd2Str(waveGetTrack2List.data, waveGetTrack2List.length));
        return RetCode.EMV_OK;
    }

    private int processQVSDC(ACType acType, CTransResult result) throws EmvException {
        int ret = ClssWaveApi.Clss_ProcRestric_Wave();
        Log.i("clssWaveProcRestric", "ret = " + ret);
        if (ret != RetCode.EMV_OK) {
            throw new EmvException(ret);
        }

        if ((acType.type == com.pax.jemv.clcommon.ACType.AC_TC)
                && transParam.ucTransType != 0x20) { //no refund

            // TODO: Exception file check

            //according to EDC
            ClssWaveApi.Clss_DelAllRevocList_Wave();
            ClssWaveApi.Clss_DelAllCAPK_Wave();
            addCapkRevList();

            DDAFlag flag = new DDAFlag();
            ret = ClssWaveApi.Clss_CardAuth_Wave(acType, flag);
            Log.i("clssWaveCardAuth", "ret = " + ret);
            return getCardAuthResult(ret, acType, flag, result);
        }
        return RetCode.EMV_OK;
    }

    private int processWAVE2(ACType acType, CTransResult result) throws EmvException {
        // TODO: Exception file check
        result.setTransResult(ETransResult.CLSS_OC_APPROVED);
        //according to EDC
        ClssWaveApi.Clss_DelAllRevocList_Wave();
        ClssWaveApi.Clss_DelAllCAPK_Wave();
        addCapkRevList();

        DDAFlag flag = new DDAFlag();
        int ret = ClssWaveApi.Clss_CardAuth_Wave(acType, flag);
        Log.i("clssWaveCardAuth", "ret = " + ret);
        return getCardAuthResult(ret, acType, flag, result);
    }

    private boolean continueUpdateResult(ACType acType, CTransResult result) throws EmvException {
        if (acType.type == com.pax.jemv.clcommon.ACType.AC_AAC) {
            result.setTransResult(ETransResult.CLSS_OC_DECLINED);
            return false;
        }

        if (transactionPath.path == TransactionPath.CLSS_VISA_MSD
                || transactionPath.path == TransactionPath.CLSS_VISA_MSD_CVN17
                || transactionPath.path == TransactionPath.CLSS_VISA_MSD_LEGACY) {

            if (transactionPath.path == TransactionPath.CLSS_VISA_MSD) {
                processMSD();
            }
        } else if (transactionPath.path == TransactionPath.CLSS_VISA_QVSDC) {
            if (processQVSDC(acType, result) != RetCode.EMV_OK) {
                return false;
            }
        } else if (transactionPath.path == TransactionPath.CLSS_VISA_WAVE2
                && acType.type == com.pax.jemv.clcommon.ACType.AC_TC) {
            if (processWAVE2(acType, result) != RetCode.EMV_OK) {
                return false;
            }
        } else {
            return false;
        }

        if (acType.type == ACType.AC_TC) {
            result.setTransResult(ETransResult.CLSS_OC_APPROVED);
        } else if (acType.type == ACType.AC_ARQC) {
            result.setTransResult(ETransResult.CLSS_OC_ONLINE_REQUEST);
        }
        return true;
    }

    @Override
    protected int setTlv(int tag, byte[] value) {
        return ClssWaveApi.Clss_SetTLVData_Wave((short) tag, value, value.length);
    }

    @Override
    protected int getTlv(int tag, ByteArray value) {
        return ClssWaveApi.Clss_GetTLVData_Wave((short) tag, value);
    }

    @Override
    protected void onAddCapkRevList(EMV_CAPK emvCapk, EMV_REVOCLIST emvRevoclist) {
        ClssWaveApi.Clss_AddCAPK_Wave(emvCapk);
        ClssWaveApi.Clss_AddRevocList_Wave(emvRevoclist);
        Log.i("ClssProc", "set VISA capk and revoclist");
    }

    @Override
    String getTrack1() {
        ByteArray waveGetTrack1List = new ByteArray();
        ClssWaveApi.Clss_nGetTrack1MapData_Wave(waveGetTrack1List);
        return Tools.bcd2Str(waveGetTrack1List.data, waveGetTrack1List.length);
    }

    @Override
    String getTrack2() {
        if (track2 == null) {
            ByteArray waveGetTrack2List = new ByteArray();
            getTlv(TagsTable.TRACK2, waveGetTrack2List);
            track2 = getTrack2FromTag57(Tools.bcd2Str(waveGetTrack2List.data, waveGetTrack2List.length));
        }
        return track2;
    }

    @Override
    String getTrack3() {
        return "";
    }

    @Override
    protected CTransResult completeTrans(ETransResult result, byte[] tag91, byte[] tag71, byte[] tag72) throws EmvException {
//        issScrCon(tag91, tag71, tag72);

        byte[] authData = new byte[16];        // authentication data from issuer
        int authDataLen = 0;

        if (tag91 != null && tag91.length > 0) {
            authDataLen = Math.min(tag91.length, 16);
            System.arraycopy(tag91, 0, authData, 0, authDataLen);
            Log.i("saveRspICCData", "aucAuthData = " + Arrays.toString(authData) + "iAuthDataLen = " + authDataLen);
        }

        byte[] issuScript = EmvImpl.clssCombine7172(tag71, tag72);

        if(issuScript == null)
            issuScript = new byte[0];

        int ret = clssCompleteTrans(authData, authDataLen, issuScript, issuScript.length);
        if (ret != RetCode.EMV_OK) {
            throw new EmvException(ret);
        }
        return new CTransResult(ETransResult.CLSS_OC_APPROVED);
    }

//    private int issScrCon(byte[] tag91, byte[] tag71, byte[] tag72) {
//        int ret;
//        if (listener != null) {
//            ret = listener.onIssScrCon();
//            if (ret != RetCode.EMV_OK)
//                return ret;
//        }
//
//        byte[] script = EmvImpl.combine917172(tag91, tag71, tag72);
//
//        return ClssWaveApi.Clss_IssScriptProc_Wave(script, script.length);
//    }

    private int clssCompleteTrans(byte[] authData, int authDataLen, byte[] issuerScript, int scriptLen) {
        ByteArray aucCTQ = new ByteArray();
        KernType kernType = new KernType();
        ByteArray sltData = new ByteArray();

        if (authDataLen == 0 && scriptLen == 0) {
            return RetCode.EMV_NO_DATA;
        }

        int ret = getTlv(0x9F6C, aucCTQ);
        if (ret != RetCode.EMV_OK) {
            return ret;
        }

        if ((clssPreProcInfo.aucReaderTTQ[2] & 0x80) == 0x80 && (aucCTQ.data[1] & 0x40) != 0x40) {
            ret = ClssEntryApi.Clss_FinalSelect_Entry(kernType, sltData);
            Log.i("Clss_FinalSelect_Entry", "ret = " + ret);
            if (ret != RetCode.EMV_OK) {
                return ret;
            }

            ret = ClssWaveApi.Clss_IssuerAuth_Wave(authData, authDataLen);
            Log.i("clssWaveIssuerAuth", "ret = " + ret);
            if (ret != RetCode.EMV_OK) {
                return ret;
            }

            ret = ClssWaveApi.Clss_IssScriptProc_Wave(issuerScript, scriptLen);
            Log.i("clssWaveIssScriptProc", "ret = " + ret);
            if (ret != RetCode.EMV_OK) {
                return ret;
            }
        }

        return RetCode.EMV_OK;
    }

}
