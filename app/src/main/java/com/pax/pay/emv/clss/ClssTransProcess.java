/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-2-28
 * Module Author: lixc
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.emv.clss;

import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.IClss;
import com.pax.eemv.IClssListener;
import com.pax.eemv.entity.AidParam;
import com.pax.eemv.entity.CTransResult;
import com.pax.eemv.entity.Config;
import com.pax.eemv.entity.TagsTable;
import com.pax.eemv.enums.EKernelType;
import com.pax.eemv.enums.ETransResult;
import com.pax.eemv.exception.EmvException;
import com.pax.eemv.utils.Tools;
import com.pax.gl.pack.ITlv;
import com.pax.gl.pack.exception.TlvException;
import com.pax.jemv.amex.model.TransactionMode;
import com.pax.jemv.clcommon.TransactionPath;
import com.pax.jemv.emv.api.EMVApi;
import com.pax.jemv.emv.model.EmvParam;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.emv.EmvTags;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.util.List;

import th.co.bkkps.utils.Log;

public class ClssTransProcess {

    private static final String TAG = "ClssTransProcess";

    private static IClss clss;

    public ClssTransProcess(IClss clss) {
        this.clss = clss;
    }

    public static Config genClssConfig() {
        Config cfg = Component.genCommonEmvConfig();
        cfg.setCapability("E020C8");
        cfg.setExCapability("E000F0A001");
        cfg.setTransType((byte) 0);
        cfg.setUnpredictableNumberRange("0060");
        cfg.setSupportOptTrans(true);
        cfg.setTransCap("D8B04000");
        cfg.setDelayAuthFlag(false);
        return cfg;
    }

    public CTransResult transProcess(TransData transData, IClssListener listener) throws EmvException {
        clss.setListener(listener);
        CTransResult result = clss.process(Component.toInputParam(transData));
        Log.i(TAG, "clss PROC:" + result.getCvmResult() + " " + result.getTransResult());
        return result;
    }

    public CTransResult readCardProcess(TransData transData, IClssListener listener) throws EmvException {
        clss.setListener(listener);
        CTransResult result = clss.clssReadCardProcess(Component.toInputParam(transData));
        Log.i(TAG, "READ CARD CLSS PROC:" + result.toString());
        return result;
    }

    public CTransResult afterReadCardProcess(CTransResult cResult, IClssListener listener) throws EmvException {
        clss.setListener(listener);
        CTransResult result = clss.clssAfterReadCardProcess(cResult);
        Log.i(TAG, "AFTER READ CARD CLSS PROC:" + result.toString());
        return result;
    }

    public static void clssTransResultProcess(CTransResult result, IClss clss, TransData transData) {
        updateEmvInfo(clss, transData);
        List<ClssDE55Tag> clssDE55TagList = ClssDE55Tag.getClssDE55Tags(clss.getKernelType());

        if (result.getTransResult() == ETransResult.CLSS_OC_ONLINE_REQUEST) {
            try {
                clss.setTlv(TagsTable.CRYPTO, Utils.str2Bcd("80"));
            } catch (EmvException e) {
                Log.w(TAG, "", e);
                transData.setEmvResult(ETransResult.ABORT_TERMINATED);
                return;
            }

            // prepare online DE55 data
            if (setStdDe55(clss, result, transData, clssDE55TagList, clss.getKernelType()) != 0) {
                transData.setEmvResult(ETransResult.ABORT_TERMINATED);
            }
        } else if (result.getTransResult() == ETransResult.CLSS_OC_APPROVED || result.getTransResult() == ETransResult.OFFLINE_APPROVED) {
            if (transData.getAuthCode() == null) {
                transData.setOrigAuthCode(Utils.getString(R.string.response_Y1_str));
                transData.setAuthCode(Utils.getString(R.string.response_Y1_str));
            }
            try {
                clss.setTlv(0x8A, Utils.str2Bcd(transData.getAuthCode()));
            } catch (EmvException e) {
                Log.w(TAG, "", e);
                transData.setEmvResult(ETransResult.ABORT_TERMINATED);
                return;
            }
            // save for upload
            if (setStdDe55(clss, result, transData, clssDE55TagList, clss.getKernelType()) != 0) {
                transData.setEmvResult(ETransResult.ABORT_TERMINATED);
                return;
            }
            transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
            transData.setTraceNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO));
            Component.saveOfflineTransNormalSale(transData);
        }
    }

    private static void updateEmvInfo(IClss clss, TransData transData) {
        //AppLabel
        byte[] value = clss.getTlv(TagsTable.APP_LABEL);
        if (value != null) {
            transData.setEmvAppLabel(new String(value));
        }

        //TVR
        value = clss.getTlv(TagsTable.TVR);
        if (value != null) {
            transData.setTvr(Utils.bcd2Str(value));
        }

        //TSI
        value = clss.getTlv(TagsTable.TSI);
        if (value != null) {
            transData.setTsi(Utils.bcd2Str(value));
        }

        //ATC
        value = clss.getTlv(TagsTable.ATC);
        if (value != null) {
            transData.setAtc(Utils.bcd2Str(value));
        }

        //AppCrypto
        value = clss.getTlv(TagsTable.APP_CRYPTO);
        if (value != null) {
            transData.setArqc(Utils.bcd2Str(value));
        }

        //AppName
        value = clss.getTlv(TagsTable.APP_NAME);
        if (value != null) {
            transData.setEmvAppName(new String(value));
        }

        //AID
        value = clss.getTlv(TagsTable.CAPK_RID);
        if (value != null) {
            transData.setAid(Utils.bcd2Str(value));
        }

        //TC
        value = clss.getTlv(TagsTable.APP_CRYPTO);
        if (value != null) {
            transData.setTc(Utils.bcd2Str(value));
        }
    }

    // set ADVT/TIP bit 55
    private static int setStdDe55(IClss clss, CTransResult result, TransData transData, List<ClssDE55Tag> clssDE55TagList) {
        ITlv tlv = FinancialApplication.getPacker().getTlv();
        ITlv.ITlvDataObjList list = tlv.createTlvDataObjectList();

        for (ClssDE55Tag i : clssDE55TagList) {
            ITlv.ITlvDataObj tag = tlv.createTlvDataObject();
            byte[] value = clss.getTlv(i.getEmvTag());
            if(value == null || value.length == 0) {
                if (0x9F33 == i.getEmvTag()) {
                    EmvParam emvParam = new EmvParam();
                    EMVApi.EMVGetParameter(emvParam);
                    tag.setTag(i.getEmvTag());
                    tag.setValue(Tools.str2Bcd("E0B0C8"));//TODO: get from each aid
                }
                else {
                    continue;
                }
            }

            if (isJCBMagstripeIgnoreTag(clss.getKernelType(), transData.getClssTypeMode(), i.getEmvTag())) {
                continue;
            }

            if (clss.getKernelType() == EKernelType.VIS && 0x9F33 == i.getEmvTag()) {
                EmvParam emvParam = new EmvParam();
                EMVApi.EMVGetParameter(emvParam);
                tag.setTag(i.getEmvTag());
                tag.setValue(Tools.str2Bcd("E0B0C8"));//TODO: get from each aid
            }
            else {
                tag.setTag(i.getEmvTag());
                tag.setValue(value);
            }
            list.addDataObj(tag);
        }

        if (clss.getKernelType() == EKernelType.MC) {
            ITlv.ITlvDataObj tag = tlv.createTlvDataObject();
            //TODO: comment because BBL not send this tag.
//            tag.setTag(0x9F53);
//            tag.setValue(Utils.str2Bcd("82"));
//            list.addDataObj(tag);

            if (result.getTransResult() == ETransResult.CLSS_OC_APPROVED) {
                tag.setTag(0x91);
                tag.setValue(clss.getTlv(0x91));
                list.addDataObj(tag);
            }
        }

        byte[] f55Data;
        try {
            f55Data = tlv.pack(list);
        } catch (TlvException e) {
            Log.e(TAG, "", e);
            return TransResult.ERR_PACK;
        }

        if (f55Data.length > 255) {
            return TransResult.ERR_PACKET;
        }
        transData.setSendIccData(Utils.bcd2Str(f55Data));
        if (f55Data.length > 0) {
            transData.setDupIccData(Utils.bcd2Str(f55Data));
        }

        return TransResult.SUCC;
    }

    private static int setStdDe55Amex(IClss clss, TransData transData) {
        if (transData.getClssTypeMode() != TransactionMode.AE_MAGMODE) {
            try {
                /*switch (transData.getTransType()) {
                    case REFUND:
                        clss.setTlv(0x9C, Utils.str2Bcd("20"));
                        break;
                    case PREAUTH:
                        clss.setTlv(0x9C, Utils.str2Bcd("30"));
                        break;
                    default:
                        clss.setTlv(0x9C, Utils.str2Bcd("00"));
                        break;
                }*/

                byte[] bAid = clss.getTlv(0x4F);
                AidParam aid = null;
                for (AidParam a : clss.getAidParamList()) {
                    if (Utils.bcd2Str(bAid).contains(Utils.bcd2Str(a.getAid()).substring(0, 10))) {//check with AID prefix
                        aid = a;
                        break;
                    }
                }

                if (aid != null) {
                    byte[] cap = Utils.str2Bcd(aid.getCapability());
                    clss.setTlv(0x9F33, cap);

                    byte[] termApp = aid.getVersion();
                    if (termApp != null && termApp.length > 0) {
                        clss.setTlv(0x9F09, termApp);
                    }
                }

                byte[] f55 = EmvTags.getF55(clss, transData.getTransType(), false, transData.getPan());
                byte[] f55Dup = EmvTags.getF55(clss, transData.getTransType(), true, transData.getPan());

                transData.setSendIccData(Utils.bcd2Str(f55));
                if (f55Dup.length > 0) {
                    transData.setDupIccData(Utils.bcd2Str(f55Dup));
                }
            } catch (EmvException e) {
                Log.e(TAG, "", e);
            }
        }

        return TransResult.SUCC;
    }

    private static int setStdDe55(IClss clss, CTransResult result, TransData transData, List<ClssDE55Tag> clssDE55TagList, EKernelType eKernelType) {
        try {
            switch (transData.getTransType()) {
                case REFUND:
                    clss.setTlv(0x9C, Utils.str2Bcd("20"));
                    break;
                case PREAUTH:
                    clss.setTlv(0x9C, Utils.str2Bcd("30"));
                    break;
                default:
                    clss.setTlv(0x9C, Utils.str2Bcd("00"));
                    break;
            }
        } catch (EmvException e) {
            Log.e(TAG, "", e);
        }

        if (eKernelType == EKernelType.AE) {
            return setStdDe55Amex(clss, transData);
        }
        return setStdDe55(clss, result, transData, clssDE55TagList);
    }

    private static boolean isJCBMagstripeIgnoreTag(EKernelType kernelType, int clssTypeMode, int tag) {
        return kernelType == EKernelType.JCB && clssTypeMode == TransactionPath.CLSS_JCB_MAG
                && (0x82 == tag || TagsTable.CRYPTO == tag || TagsTable.TVR == tag || 0x9F37 == tag);
    }
}
