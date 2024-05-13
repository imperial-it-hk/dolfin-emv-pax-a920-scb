/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-6-13
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
import com.pax.jemv.amex.api.ClssAmexApi;
import com.pax.jemv.amex.model.CLSS_AEAIDPARAM;
import com.pax.jemv.amex.model.Clss_AddReaderParam_AE;
import com.pax.jemv.amex.model.Clss_ReaderParam_AE;
import com.pax.jemv.amex.model.ONLINE_PARAM;
import com.pax.jemv.amex.model.TransactionMode;
import com.pax.jemv.clcommon.ACType;
import com.pax.jemv.clcommon.ByteArray;
import com.pax.jemv.clcommon.Clss_ProgramID_II;
import com.pax.jemv.clcommon.Clss_ReaderParam;
import com.pax.jemv.clcommon.EMV_CAPK;
import com.pax.jemv.clcommon.EMV_REVOCLIST;
import com.pax.jemv.clcommon.OnlineResult;
import com.pax.jemv.clcommon.RetCode;
import com.pax.jemv.entrypoint.api.ClssEntryApi;

import java.util.Arrays;
import java.util.List;

class ClssProcAE extends ClssProc {
    private Clss_ReaderParam_AE clssReaderParamAE;
    private Clss_AddReaderParam_AE clssAddReaderParamAE;
    private TransactionMode transactionMode;
    private boolean isFullOnline = false;
    private boolean isSupportDRL = true;
    private static boolean isSecondTap = false;

    static {
        System.loadLibrary("F_AE_LIB_PayDroid");
        System.loadLibrary("JNI_AE_v101");
    }

    ClssProcAE(IClssListener listener) {
        super(listener);
        clssReaderParamAE = new Clss_ReaderParam_AE();
        clssReaderParamAE.stReaderParam = new Clss_ReaderParam();
        clssAddReaderParamAE = new Clss_AddReaderParam_AE(new byte[4], (byte) 0, new byte[27]);
        transactionMode = new TransactionMode();
    }

    private int init() {
        int ret;
        if (!isSecondTap) {
            ret = ClssAmexApi.Clss_CoreInit_AE();
            Log.i(TAG, "Clss_CoreInit_AE = " + ret);
            if (ret != RetCode.EMV_OK) {
                return ret;
            }
        }

        //Reset flag
        isSecondTap = false;

        if (isSupportDRL) {
            ret = ClssAmexApi.Clss_SetExtendFunction_AE(new byte[]{0x01});
            if (ret != RetCode.EMV_OK) {
                return ret;
            }
        }

        ret = clssBaseParameterSet();
        if (ret != RetCode.EMV_OK) {
            return ret;
        }

        return ret;
    }

    private int clssBaseParameterSet() {
        //Set Param
        // FIXME Kim crash
        //ClssAmexApi.Clss_GetReaderParam_AE(clssReaderParamAE);
        clssReaderParamAE.aucUNRange = Tools.str2Bcd(cfg.getUnpredictableNumberRange());
        clssReaderParamAE.stReaderParam = Converter.toClssReaderParam(cfg);
        clssReaderParamAE.ucTmSupOptTrans = Tools.boolean2Byte(cfg.isSupportOptTrans());
        int ret = ClssAmexApi.Clss_SetReaderParam_AE(clssReaderParamAE);
        Log.i(TAG, "Clss_SetReaderParam_AE = " + ret);

        //Set Additional Param
        ClssAmexApi.Clss_GetAddReaderParam_AE(clssAddReaderParamAE);
        clssAddReaderParamAE.aucTmTransCapa = Tools.str2Bcd(cfg.getTransCap());
        clssAddReaderParamAE.ucDelayAuthFlag = 0;
        ret = ClssAmexApi.Clss_SetAddReaderParam_AE(clssAddReaderParamAE);
        Log.i(TAG, "Clss_SetAddReaderParam_AE = " + ret);
        return ret;
    }

    private int selectApp() {
        int ret = ClssAmexApi.Clss_SetAEAidParam_AE(genAEAidParam());
        Log.i(TAG, "SetFinalSelectData_AE = " + ret);
        ret = ClssAmexApi.Clss_SetFinalSelectData_AE(finalSelectData, finalSelectDataLen);
        Log.i(TAG, "SetFinalSelectData_AE = " + ret);
        return ret;
    }

    //FIXME Kim Why do we need this?
    private CLSS_AEAIDPARAM genAEAidParam() {
        CLSS_AEAIDPARAM clssAidParamAE = new CLSS_AEAIDPARAM();
        clssAidParamAE.AcquierId = aid.getAcquirerId();
        clssAidParamAE.FloorLimit = aid.getFloorLimit();
        clssAidParamAE.FloorLimitCheck = (byte) aid.getFloorLimitCheckFlg();
        clssAidParamAE.TACDefault = aid.getRdClssTacDefault();
        clssAidParamAE.TACDenial = aid.getRdClssTacDenial();
        clssAidParamAE.TACOnline = aid.getRdClssTacOnline();
        clssAidParamAE.dDOL = aid.getdDol();//Tools.str2Bcd("9F3704");
        clssAidParamAE.tDOL = aid.gettDol();//Tools.str2Bcd("9F02065F2A029A039C0195059F3704");
        clssAidParamAE.Version = aid.getVersion();
        clssAidParamAE.ucAETermCap = (byte) 0xC8;
        return clssAidParamAE;
    }

    @Override
    protected CTransResult processTrans() throws EmvException {

        int ret = init();
        if (ret != RetCode.EMV_OK) {
            throw new EmvException(ret);
        }

        ret = selectApp();
        if (ret != RetCode.EMV_OK) {
            throw new EmvException(ret);
        }

        ret = ClssAmexApi.Clss_SetTransData_AE(transParam, preProcInterInfo);
        if (ret != RetCode.EMV_OK) {
            throw new EmvException(ret);
        }

        ret = ClssAmexApi.Clss_Proctrans_AE(transactionMode);
        if (ret != RetCode.EMV_OK) {
            if (ret == RetCode.CLSS_RESELECT_APP) { // GPO
                ret = ClssEntryApi.Clss_DelCurCandApp_Entry();
            }
            throw new EmvException(ret);
        }

        ByteArray optimizeFlag = new ByteArray();
        ret = ClssAmexApi.Clss_ReadRecord_AE(optimizeFlag);
        if (ret != RetCode.EMV_OK) {
            throw new EmvException(ret);
        }

        //add capk
        ClssAmexApi.Clss_DelAllRevocList_AE();
        ClssAmexApi.Clss_DelAllCAPK_AE();
        addCapkRevList();

        ret = ClssAmexApi.Clss_CardAuth_AE();
        //returns	EMV_OK	EMV_DATA_ERR
        if (ret != RetCode.EMV_OK) {
            throw new EmvException(ret);
        }

        //execute DRL
        if (isSupportDRL) {
            expressPaySetDRLParam();
        }

        ACType acType = new ACType();
        ret = startTransaction(acType);
        if (ret != RetCode.EMV_OK) {
            if (ret == RetCode.CLSS_DECLINE) {
                return new CTransResult(ETransResult.CLSS_OC_DECLINED);
            } else if (ret == RetCode.CLSS_TRY_AGAIN) {
                if (isSecondTap) {
                    return new CTransResult(ETransResult.CLSS_OC_REFER_CONSUMER_DEVICE);
                }
                return new CTransResult(ETransResult.CLSS_OC_TRY_AGAIN);
            } else {
                throw new EmvException(ret);
            }
        }

        CTransResult result = new CTransResult(ETransResult.ABORT_TERMINATED);
        updateResult(result, acType);
        return result;
    }

    private int expressPaySetDRLParam() {
        int ret = 0;
        List<Clss_ProgramID_II> clssProgramIdlist = listener.onGetProgramId();
        if (!clssProgramIdlist.isEmpty()) {
            for (Clss_ProgramID_II i : clssProgramIdlist) {
                ret = ClssAmexApi.Clss_AddDRL_AE(i);
            }
        }
        return ret;
    }

    private void updateResult(CTransResult result, ACType acType) throws EmvException {
        byte cvmType = ClssAmexApi.Clss_GetCvmType_AE();
        if (acType.type == ACType.AC_TC) {
            result.setTransResult(ETransResult.CLSS_OC_APPROVED);
        } else if (acType.type == ACType.AC_ARQC) {
            result.setTransResult(ETransResult.CLSS_OC_ONLINE_REQUEST);
        } else {
            result.setTransResult(ETransResult.CLSS_OC_DECLINED);
        }
        Log.i("clssWaveGetCvmType", "CVMType = " + cvmType);
        if (cvmType < 0) {
            if (cvmType == RetCode.CLSS_DECLINE) {
                result.setTransResult(ETransResult.CLSS_OC_DECLINED);
            }
            throw new EmvException(cvmType);
        }
        result.setCvmResult(Converter.convertCVM(cvmType));
    }

    private int startTransaction(ACType acType) throws EmvException {
        ByteArray adviceFlag = new ByteArray();
        ByteArray onlineFlagByte = new ByteArray();
        ByteArray tmTransCapa = new ByteArray();
        int ret = ClssAmexApi.Clss_StartTrans_AE((byte) 0, adviceFlag, onlineFlagByte);
        getTlv(0x9F6E, tmTransCapa);

        boolean onlineFlag = onlineFlagByte.data[0] == 1;

        listener.onSetClssTypeMode(transactionMode.mode);

        if (ret == RetCode.EMV_OK) {
            if (!onlineFlag || (transactionMode.mode == TransactionMode.AE_MAGMODE) || ((tmTransCapa.data[0] & 0x20) == 0)) {
                //full online not supported
                listener.onPromptRemoveCard();
                //Inter_DisplayMsg(MSG_CARD_READ_OK);
            }
            acType.type = onlineFlag ? ACType.AC_ARQC : ACType.AC_TC;
        } else if ((ret == RetCode.CLSS_CVMDECLINE) || (ret == RetCode.EMV_DENIAL)) {
            acType.type = ACType.AC_AAC;
            throw new EmvException(RetCode.CLSS_DECLINE);
        } else if (ret == RetCode.CLSS_REFER_CONSUMER_DEVICE) {
            if (!onlineFlag || (transactionMode.mode == TransactionMode.AE_MAGMODE) || ((tmTransCapa.data[0] & 0x20) == 0)) {
                //full online not supported)
                listener.onPromptRemoveCard();
                //Inter_DisplayMsg(MSG_SEE_PHONE);
                ret = listener.onDisplaySeePhone();
                if (ret == RetCode.EMV_OK) {
                    isSecondTap = true;
                    acType.type = ACType.AC_AAC;
                    return RetCode.CLSS_TRY_AGAIN;
                } else if (ret == RetCode.EMV_USER_CANCEL) {
                    throw new EmvException(ret);
                }
            }
            acType.type = ACType.AC_AAC;
            return RetCode.CLSS_TRY_AGAIN;
        } else {
            throw new EmvException(ret);
        }

        //Delayed Authorization
        if (onlineFlag) {
            ret = ClssAmexApi.Clss_GetAddReaderParam_AE(clssAddReaderParamAE);
            if (ret != RetCode.EMV_OK) {
                throw new EmvException(ret);
            }

            ONLINE_PARAM onlineParam = new ONLINE_PARAM();
            onlineParam.aucRspCode = "00".getBytes();
            if (clssAddReaderParamAE.ucDelayAuthFlag == 1) {
                ret = ClssAmexApi.Clss_CompleteTrans_AE((byte) OnlineResult.ONLINE_APPROVE,
                        (byte) TransactionMode.AE_DELAYAUTH_PARTIALONLINE, onlineParam, adviceFlag);
                if (ret != RetCode.EMV_OK)
                    throw new EmvException(ret);
            }

            if (((tmTransCapa.data[4] & 0x20) != 0) && (transactionMode.mode == TransactionMode.AE_EMVMODE)) {
                //Full online processing,
                //set timer for online transaction,
                //if timeout prompt remove card, and change online mode to part online
                isFullOnline = false;
            }
        }
        listener.onPromptRemoveCard();
        return RetCode.EMV_OK;
    }

    @Override
    protected int setTlv(int tag, byte[] value) {
        return ClssAmexApi.Clss_SetTLVData_AE((short) tag, value, value.length);
    }

    @Override
    protected int getTlv(int tag, ByteArray value) {
        return ClssAmexApi.Clss_GetTLVData_AE((short) tag, value);
    }

    @Override
    protected void onAddCapkRevList(EMV_CAPK emvCapk, EMV_REVOCLIST emvRevoclist) {
        int ret = ClssAmexApi.Clss_AddCAPK_AE(emvCapk);
        Log.i(TAG, "set AE capk ret :" + ret);
        ret = ClssAmexApi.Clss_AddRevocList_AE(emvRevoclist);
        Log.i(TAG, "set AE revoclist ret :" + ret);
    }

    @Override
    String getTrack1() {
        if (transactionMode.mode == TransactionMode.AE_MAGMODE) {
            ByteArray track1 = new ByteArray();
            ClssAmexApi.Clss_nGetTrackMapData_AE((byte) 1, track1);
            return Tools.bcd2Str(track1.data, track1.length);
        }
        return "";
    }

    @Override
    String getTrack2() {
        ByteArray track2 = new ByteArray();
        if (transactionMode.mode == TransactionMode.AE_MAGMODE) {
            ClssAmexApi.Clss_nGetTrackMapData_AE((byte) 2, track2);
            return new String(track2.data).substring(1, track2.length - 1);
        }
        //chip or MSD without trk2map data
        if (getTlv(TagsTable.TRACK2, track2) == RetCode.EMV_OK) {
            return getTrack2FromTag57(Tools.bcd2Str(track2.data, track2.length));
        }
        return "";
    }

    @Override
    String getTrack3() {
        return "";
    }


    @Override
    protected CTransResult completeTrans(ETransResult result, byte[] tag91, byte[] tag71, byte[] tag72) throws EmvException {
        ByteArray authCode = new ByteArray();
        ByteArray respCode = new ByteArray();
        getTlv(0x89, authCode);
        getTlv(0x8A, respCode);
        int onlineResult;
        if (result == ETransResult.ABORT_TERMINATED)
            onlineResult = OnlineResult.ONLINE_FAILED;
        else if (result == ETransResult.ONLINE_APPROVED)
            onlineResult = OnlineResult.ONLINE_APPROVE;
        else if (!Arrays.equals(Arrays.copyOf(respCode.data, respCode.length), "89".getBytes()))
            onlineResult = OnlineResult.ONLINE_ABORT;
        else
            onlineResult = OnlineResult.ONLINE_DENIAL;

        transactionMode.mode = TransactionMode.AE_PARTIALONLINE;
        ByteArray tmTransCapa = new ByteArray();
        getTlv(0x9F6E, tmTransCapa);
        if ((tmTransCapa.data[0] & 0x20) != 0) {//full online supported
            transactionMode.mode = isFullOnline ? TransactionMode.AE_FULLONLINE : TransactionMode.AE_PARTIALONLINE;
        }

        ONLINE_PARAM onlineParam = new ONLINE_PARAM();
        System.arraycopy(respCode.data, 0, onlineParam.aucRspCode, 0, 2);
        onlineParam.nAuthCodeLen = authCode.length;
        System.arraycopy(authCode.data, 0, onlineParam.aucAuthCode, 0, 6);
        if (tag91 != null) {
            System.arraycopy(tag91, 0, onlineParam.aucIAuthData, 0, tag91.length);
            onlineParam.nIAuthDataLen = tag91.length;
        }
        byte[] issuScript = EmvImpl.clssCombine7172(tag71, tag72);
        if(issuScript != null) {
            System.arraycopy(issuScript, 0, onlineParam.aucScript, 0, issuScript.length);
            onlineParam.nScriptLen = issuScript.length;
        } else {
            onlineParam.aucScript = new byte[0];
            onlineParam.nScriptLen = 0;
        }


        ByteArray adviceFlag = new ByteArray();
        int ret = ClssAmexApi.Clss_CompleteTrans_AE((byte) onlineResult, (byte) transactionMode.mode, onlineParam, adviceFlag);
        if (ret != RetCode.EMV_OK) {
            throw new EmvException(ret);
        }
        return new CTransResult(ETransResult.ONLINE_APPROVED);
    }


}
