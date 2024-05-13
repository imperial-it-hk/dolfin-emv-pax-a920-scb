/*
 * ===========================================================================================
 * = COPYRIGHT
 *          PAX Computer Technology(Shenzhen) CO., LTD PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or nondisclosure
 *   agreement with PAX Computer Technology(Shenzhen) CO., LTD and may not be copied or
 *   disclosed except in accordance with the terms in that agreement.
 *     Copyright (C) 2019-? PAX Computer Technology(Shenzhen) CO., LTD All rights reserved.
 * Description: // Detail description about the function of this module,
 *             // interfaces with the other modules, and dependencies.
 * Revision History:
 * Date                  Author	                 Action
 * 20190108  	         lixc                    Create
 * ===========================================================================================
 */
package com.pax.eemv.clss;

import android.util.Log;

import com.pax.eemv.IClssListener;
import com.pax.eemv.entity.AidParam;
import com.pax.eemv.entity.CTransResult;
import com.pax.eemv.entity.TagsTable;
import com.pax.eemv.enums.ECvmResult;
import com.pax.eemv.enums.ETransResult;
import com.pax.eemv.exception.EmvException;
import com.pax.eemv.utils.Tools;
import com.pax.jemv.clcommon.ByteArray;
import com.pax.jemv.clcommon.Clss_PreProcInfo;
import com.pax.jemv.clcommon.Clss_TransParam;
import com.pax.jemv.clcommon.EMV_CAPK;
import com.pax.jemv.clcommon.EMV_REVOCLIST;
import com.pax.jemv.clcommon.OutcomeParam;
import com.pax.jemv.clcommon.RetCode;
import com.pax.jemv.clcommon.TransactionPath;
import com.pax.jemv.entrypoint.api.ClssEntryApi;
import com.pax.jemv.jcb.api.ClssJCBApi;

import java.util.Arrays;

import static com.pax.eemv.utils.Tools.bcd2Str;

class ClssProcJcb extends ClssProc {


    static {
        System.loadLibrary("F_JCB_LIB_PayDroid");
        System.loadLibrary("JNI_JCB_v100");
    }

    ClssProcJcb(IClssListener listener) {
        super(listener);
    }

    private int jcbFlow() {
        int ret = ClssJCBApi.Clss_CoreInit_JCB();
        Log.i(TAG, "Clss_CoreInit_JCB = " + ret);
        if (ret != RetCode.EMV_OK) {
            return ret;
        }
        ret = ClssJCBApi.Clss_SetFinalSelectData_JCB(finalSelectData, finalSelectDataLen);
        Log.i(TAG, "Clss_SetFinalSelectData_JCB = " + ret);


        for (Clss_PreProcInfo info : arrayPreProcInfo) {
            if (info != null && Arrays.equals(aid.getAid(), info.aucAID)) {
                setTransParamJcb(transParam, aid, info);
            }
        }

        ret = ClssJCBApi.Clss_InitiateApp_JCB(transactionPath);
        if (ret != RetCode.EMV_OK) {
            Log.e(TAG, "ClssJCBApi.Clss_InitiateApp_JCB(transactionPath) error, ret = " + ret);
            return ret;
        }

        Log.i(TAG, "ClssJCBApi transactionPath = " + transactionPath.path);

        ret = ClssJCBApi.Clss_ReadData_JCB();
        if (ret != RetCode.EMV_OK) {
            Log.e(TAG, "ClssJCBApi.Clss_ReadData_JCB() error, ret = " + ret);
            return ret;
        }
        ret = appTransProc((byte) transactionPath.path);
        Log.i(TAG, "appTransProc ret = " + ret);
        if (ret != RetCode.EMV_OK) {
            Log.e(TAG, "appTransProc(transactionPath) error, ret = " + ret);
            return ret;
        }
        return ret;
    }

    @Override
    protected CTransResult processTrans() throws EmvException {
        int ret = jcbFlow();
        if (ret == RetCode.CLSS_RESELECT_APP) {
            ret = ClssEntryApi.Clss_DelCurCandApp_Entry();
            if (ret != RetCode.EMV_OK) {
                throw new EmvException(ret);
            }
            ret = RetCode.CLSS_TRY_AGAIN;
            throw new EmvException(ret);
        }

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


    private CTransResult genTransResult() {
        byte[] szBuff = new byte[]{(byte) 0xDF, (byte) 0x81, 0x29};//Outcome Parameter
        ByteArray aucOutcomeParamSetJcb = new ByteArray();

        int ret = ClssJCBApi.Clss_GetTLVDataList_JCB(szBuff, (byte) 3, 24, aucOutcomeParamSetJcb);
        if (ret != RetCode.EMV_OK) {
            return new CTransResult(ETransResult.CLSS_OC_DECLINED);
        }

        switch (aucOutcomeParamSetJcb.data[0] & 0xF0) {
            case OutcomeParam.CLSS_OC_APPROVED:
                return new CTransResult(ETransResult.CLSS_OC_APPROVED);
            case OutcomeParam.CLSS_OC_ONLINE_REQUEST:
                return new CTransResult(ETransResult.CLSS_OC_ONLINE_REQUEST);
            case OutcomeParam.CLSS_OC_TRY_ANOTHER_INTERFACE:
                return new CTransResult(ETransResult.CLSS_OC_TRY_ANOTHER_INTERFACE);
            case OutcomeParam.CLSS_OC_DECLINED:
            default://CLSS_OC_END_APPLICATION
                return new CTransResult(ETransResult.CLSS_OC_DECLINED);
        }
    }

    private void updateCVMResult(CTransResult result) {
        byte[] szBuff = new byte[]{(byte) 0xDF, (byte) 0x81, 0x29};//Outcome Parameter
        ByteArray aucOutcomeParamSetJcb = new ByteArray();

        int ret = ClssJCBApi.Clss_GetTLVDataList_JCB(szBuff, (byte) 3, 24, aucOutcomeParamSetJcb);
        if (ret == RetCode.EMV_OK) {
            switch (aucOutcomeParamSetJcb.data[3] & 0xF0) {
                case OutcomeParam.CLSS_OC_OBTAIN_SIGNATURE:
                    result.setCvmResult(ECvmResult.SIG);
                    Log.i(TAG, "CVM = signature");
                    break;
                case OutcomeParam.CLSS_OC_ONLINE_PIN:
                    result.setCvmResult(ECvmResult.ONLINE_PIN);
                    Log.i(TAG, "CVM = online pin");
                    break;
                case OutcomeParam.CLSS_OC_CONFIRM_CODE_VER:
                    result.setCvmResult(ECvmResult.OFFLINE_PIN);
                    Log.i(TAG, "CVM = CLSS_OC_CONFIRM_CODE_VER");
                    break;
                case OutcomeParam.CLSS_OC_NO_CVM:
                    result.setCvmResult(ECvmResult.NO_CVM);
                    Log.i(TAG, "CVM = no cvm");
                    break;
                default:
                    result.setCvmResult(ECvmResult.NO_CVM);
                    Log.i(TAG, " default CVM = no cvm");
                    break;
            }
        }
    }

    @Override
    protected void onAddCapkRevList(EMV_CAPK emvCapk, EMV_REVOCLIST emvRevoclist) {
        int ret = ClssJCBApi.Clss_AddCAPK_JCB(emvCapk);
        Log.i(TAG, "set JCB capk ret :" + ret);
        ret = ClssJCBApi.Clss_AddRevocList_JCB(emvRevoclist);
        Log.i(TAG, "set JCB revoclist ret :" + ret);
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
        int ret;
        if (transactionPath.path == TransactionPath.CLSS_JCB_MAG) {
            ret = getTlv(TagsTable.TRACK2_1, track);
            if (ret != RetCode.EMV_OK) {
                ret = getTlv(TagsTable.TRACK2, track);
            }
        } else {
            ret = getTlv(TagsTable.TRACK2, track);
        }

        if (ret == RetCode.EMV_OK) {
            //AET-173
            return Tools.bcd2Str(track.data, track.length).split("F")[0];
        }
        return "";
    }

    @Override
    String getTrack3() {
        return "";
    }

    private int appTransProc(byte transPath) {
        int ret;
        byte ucExceptFileFlg = 0;

        if (transPath == TransactionPath.CLSS_JCB_EMV) {// 0x06)
            ClssJCBApi.Clss_DelAllRevocList_JCB();
            ClssJCBApi.Clss_DelAllCAPK_JCB();

            addCapkRevList();

            ret = ClssJCBApi.Clss_TransProc_JCB(ucExceptFileFlg);
            if (ret != RetCode.EMV_OK) {
                Log.e(TAG, "EMV Clss_TransProc_JCB error, ret = " + ret);
                return ret;
            }

            ret = ClssJCBApi.Clss_CardAuth_JCB();
            Log.i(TAG, "ClssJCBApi.Clss_CardAuth_JCB ret = " + ret);
        } else {// 0x05)
            ret = ClssJCBApi.Clss_TransProc_JCB(ucExceptFileFlg);
            Log.i(TAG, "MAG or LEGACY ClssJCBApi.Clss_TransProc_JCB ret = " + ret);
        }

        ByteArray byteArray = new ByteArray();
        int iRet = ClssJCBApi.Clss_GetTLVDataList_JCB(new byte[]{(byte) 0x95}, (byte) 1, 10, byteArray);
        byte[] a = new byte[byteArray.length];
        System.arraycopy(byteArray.data, 0, a, 0, byteArray.length);
        String tvr = bcd2Str(a);
        Log.i("Clss_TLV_JCB iRet 0x95", Integer.toString(iRet) + "");
        Log.i("Clss_JCB TVR 0x95", tvr + "");
        return ret;
    }

    private void setTransParamJcb(Clss_TransParam clssTransParam, AidParam aid, Clss_PreProcInfo info) {
        //9f02,9f03
        byte[] tmp = Tools.str2Bcd(String.valueOf(clssTransParam.ulAmntAuth));
        byte[] amount = new byte[6];
        System.arraycopy(tmp, 0, amount, 6 - tmp.length, tmp.length);
        setTlv(TagsTable.AMOUNT, amount);

        tmp = Tools.str2Bcd(Long.toString(clssTransParam.ulAmntOther));
        amount = new byte[6];
        System.arraycopy(tmp, 0, amount, 6 - tmp.length, tmp.length);
        setTlv(TagsTable.AMOUNT_OTHER, amount);
        //9a,9f21,9c
        setTlv(TagsTable.TRANS_DATE, clssTransParam.aucTransDate);
        setTlv(TagsTable.TRANS_TIME, clssTransParam.aucTransTime);
        setTlv(TagsTable.TRANS_TYPE, new byte[]{clssTransParam.ucTransType});

        //9f01,9f4e,9f15,9f1a,9f35,5f2a,5f36 9f53 9f52
        setTlv(TagsTable.ACQUIRER_ID, null);
        setTlv(TagsTable.MERCHANT_NAME_LOCATION, null);
        setTlv(TagsTable.MERCHANT_CATEGORY_CODE, Tools.str2Bcd("0000"));
        setTlv(TagsTable.COUNTRY_CODE, Tools.str2Bcd(cfg.getCountryCode()));
        setTlv(0x9F35, new byte[]{0x22});//terminal type
        setTlv(TagsTable.CURRENCY_CODE, Tools.str2Bcd(cfg.getTransCurrCode()));
        setTlv(0x5f36, new byte[]{0x02});
        setTlv(0x9f53, new byte[]{(byte) 0xF3, (byte) 0x80, (byte) 0x00});
        setTlv(0x9f52, new byte[]{0x03});

        //Df8120,Df8121,Df8122 ff8130
        setTlv(TagsTable.TERM_DEFAULT, aid.getRdClssTacDefault());
        setTlv(TagsTable.TERM_DENIAL, aid.getRdClssTacDenial());
        setTlv(TagsTable.TERM_ONLINE, aid.getRdClssTacOnline());
        setTlv(0xFF8130, new byte[]{(byte) 0x3B, 0x00});

        setTlv(TagsTable.FLOOR_LIMIT, Tools.str2Bcd(Tools.getPaddedNumber(info.ulRdClssFLmt, 12)));
//        setTlv(TagsTable.TRANS_LIMIT, Tools.str2Bcd(Tools.getPaddedNumber(info.ulRdClssTxnLmt, 12))); // not set, handled by Clss_GetPreProcInterFlg_Entry
//        setTlv(TagsTable.TRANS_CVM_LIMIT, Tools.str2Bcd(Tools.getPaddedNumber(info.ulRdClssTxnLmt, 12))); // not set, handled by Clss_GetPreProcInterFlg_Entry
        setTlv(TagsTable.CVM_LIMIT, Tools.str2Bcd(Tools.getPaddedNumber(info.ulRdCVMLmt, 12)));

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
        return ClssJCBApi.Clss_SetTLVDataList_JCB(buf, buf.length);
    }

    @Override
    public int getTlv(int tag, ByteArray value) {
        byte[] bcdTag = Tools.int2ByteArray(tag);

        int ret = ClssJCBApi.Clss_GetTLVDataList_JCB(bcdTag, (byte) bcdTag.length, value.length, value);
        Log.i(TAG, " getClssTlv_JCB  tag :" + tag
                + " value: " + bcd2Str(value.data).substring(0, 2 * value.length) + " ret :" + ret);
        return ret;
    }

    @Override
    protected CTransResult completeTrans(ETransResult transResult, byte[] tag91, byte[] tag71, byte[] tag72) {
        //do nothing
        return new CTransResult(ETransResult.CLSS_OC_APPROVED);
    }
}
