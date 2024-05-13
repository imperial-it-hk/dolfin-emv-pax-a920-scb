/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-4-17
 * Module Author: Kim.L
 * Description:
 *
 * ============================================================================
 */
package com.pax.eemv.utils;

import com.pax.eemv.entity.AidParam;
import com.pax.eemv.entity.CandList;
import com.pax.eemv.entity.Capk;
import com.pax.eemv.entity.ClssInputParam;
import com.pax.eemv.entity.ClssTornLogRecord;
import com.pax.eemv.entity.Config;
import com.pax.eemv.enums.ECvmResult;
import com.pax.eemv.enums.EKernelType;
import com.pax.eemv.enums.ETransResult;
import com.pax.eemv.exception.EEmvExceptions;
import com.pax.eemv.exception.EmvException;
import com.pax.jemv.clcommon.CLSS_TORN_LOG_RECORD;
import com.pax.jemv.clcommon.Clss_PreProcInfo;
import com.pax.jemv.clcommon.Clss_ReaderParam;
import com.pax.jemv.clcommon.CvmType;
import com.pax.jemv.clcommon.EMV_APPLIST;
import com.pax.jemv.clcommon.EMV_CAPK;
import com.pax.jemv.clcommon.OnlineResult;

import java.util.List;

public class Converter {

    private Converter() {

    }

    public static EMV_APPLIST toEMVApp(AidParam aidParam) {
        EMV_APPLIST appList = new EMV_APPLIST();
        appList.appName = aidParam.getAppName().getBytes();
        appList.aid = aidParam.getAid();
        appList.aidLen = (byte) appList.aid.length;
        appList.selFlag = aidParam.getSelFlag();
        appList.priority = aidParam.getPriority();
        appList.floorLimit = aidParam.getFloorLimit();
        appList.floorLimitCheck = (byte) aidParam.getFloorLimitCheckFlg();
        appList.threshold = aidParam.getThreshold();
        appList.targetPer = aidParam.getTargetPer();
        appList.maxTargetPer = aidParam.getMaxTargetPer();
        appList.randTransSel = Tools.boolean2Byte(aidParam.getRandTransSel());
        appList.velocityCheck = Tools.boolean2Byte(aidParam.getVelocityCheck());
        appList.tacDenial = aidParam.getTacDenial();
        appList.tacOnline = aidParam.getTacOnline();
        appList.tacDefault = aidParam.getTacDefault();
        appList.acquierId = aidParam.getAcquirerId();
        appList.dDOL = aidParam.getdDol();
        appList.tDOL = aidParam.gettDol();
        appList.version = aidParam.getVersion();
        appList.riskManData = aidParam.getRiskManData();
        return appList;
    }

    public static CandList toCandList(EMV_APPLIST emvAppList) {
        CandList candList = new CandList();
        candList.setAid(emvAppList.aid);
        candList.setAidLen(emvAppList.aidLen);
        candList.setPriority(emvAppList.priority);
        candList.setAppName(Tools.bytes2String(emvAppList.appName));
        return candList;
    }

    public static EMV_CAPK toEMVCapk(Capk capk) {
        EMV_CAPK emvCapk = new EMV_CAPK();
        emvCapk.rID = capk.getRid();
        emvCapk.keyID = capk.getKeyID();
        emvCapk.hashInd = capk.getHashInd();
        emvCapk.arithInd = capk.getArithInd();
        emvCapk.modul = capk.getModul();
        emvCapk.modulLen = (short) capk.getModul().length;
        emvCapk.exponent = capk.getExponent();
        emvCapk.exponentLen = (byte) capk.getExponent().length;
        emvCapk.expDate = capk.getExpDate();
        emvCapk.checkSum = capk.getCheckSum();
        return emvCapk;
    }

    public static int toOnlineResult(ETransResult procResult) {
        switch (procResult) {
            case ONLINE_APPROVED:
                return OnlineResult.ONLINE_APPROVE;
            case ONLINE_DENIED:
                return OnlineResult.ONLINE_DENIAL;
            case ONLINE_FAILED:
                return OnlineResult.ONLINE_FAILED;
            default:
                return -1;
        }
    }

    public static CLSS_TORN_LOG_RECORD toClssTornLogRecord(ClssTornLogRecord clssTornLogRecord) {
        String pan = clssTornLogRecord.getPan();
        return new CLSS_TORN_LOG_RECORD(Tools.str2Bcd(pan),
                (byte) pan.length(),
                Tools.boolean2Byte(clssTornLogRecord.getPanSeqFlg()),
                clssTornLogRecord.getPanSeq(),
                clssTornLogRecord.getTornData(),
                clssTornLogRecord.getTornDataLen());
    }

    public static Clss_ReaderParam toClssReaderParam(Config cfg) {
        Clss_ReaderParam readerParam = new Clss_ReaderParam();

        readerParam.ulReferCurrCon = cfg.getReferCurrCon();
        readerParam.aucMchNameLoc = cfg.getMerchName().getBytes();
        readerParam.usMchLocLen = (short) cfg.getMerchName().length();
        readerParam.aucMerchCatCode = Tools.str2Bcd(cfg.getMerchCateCode());
        readerParam.aucMerchantID = Tools.str2Bcd(cfg.getMerchId());
        readerParam.acquierId = cfg.getAcquirerId();

        readerParam.aucTmID = Tools.str2Bcd(cfg.getTermId());
        readerParam.ucTmType = cfg.getTermType();
        readerParam.aucTmCap = Tools.str2Bcd(cfg.getCapability());
        readerParam.aucTmCapAd = Tools.str2Bcd(cfg.getExCapability());
        readerParam.aucTmCntrCode = Tools.str2Bcd(cfg.getCountryCode());
        readerParam.aucTmTransCur = Tools.str2Bcd(cfg.getTransCurrCode());
        readerParam.ucTmTransCurExp = cfg.getTransCurrExp();
        readerParam.aucTmRefCurCode = Tools.str2Bcd(cfg.getReferCurrCode());
        readerParam.ucTmRefCurExp = cfg.getReferCurrExp();
        return readerParam;
    }

    public static Clss_PreProcInfo genClssPreProcInfo(AidParam aid, ClssInputParam inputParam) {
        return new Clss_PreProcInfo(aid.getFloorLimit(), aid.getRdClssTxnLmt(), aid.getRdCVMLmt(), aid.getRdClssFLmt(),
                aid.getAid(), (byte) aid.getAid().length, EKernelType.DEF.getKernelType(),
                (byte) inputParam.getAmtZeroNoAllowedFlg(),
                Tools.boolean2Byte(inputParam.isCrypto17Flg()),
                Tools.boolean2Byte(inputParam.isStatusCheckFlg()),
                Tools.str2Bcd(inputParam.getReaderTTQ()),
                (byte) aid.getFloorLimitCheckFlg(),
                (byte) aid.getRdClssTxnLmtFlag(),
                (byte) aid.getRdCVMLmtFlag(),
                (byte) aid.getRdClssFLmtFlag(),
                new byte[2]);
    }

    public static Clss_PreProcInfo genClssPreProcInfoTPN(AidParam aid, ClssInputParam inputParam) {
        return new Clss_PreProcInfo(aid.getFloorLimit(), aid.getRdClssTxnLmt(), aid.getRdCVMLmt(), aid.getRdClssFLmt(),
                aid.getAid(), (byte) aid.getAid().length, EKernelType.PBOC.getKernelType(),
                Tools.boolean2Byte(inputParam.isCrypto17Flg()),
                (byte) inputParam.getAmtZeroNoAllowedFlg(),
                Tools.boolean2Byte(inputParam.isStatusCheckFlg()),
                Tools.str2Bcd(inputParam.getReaderTTQ()),
                (byte) aid.getFloorLimitCheckFlg(),
                (byte) aid.getRdClssTxnLmtFlag(),
                (byte) aid.getRdCVMLmtFlag(),
                (byte) aid.getRdClssFLmtFlag(),
                new byte[2]);
    }

    public static byte[] toCvmTypes(List<ECvmResult> cvmResultList) {

        byte[] list = new byte[cvmResultList.size()];
        for (int i = 0; i < cvmResultList.size(); ++i) {
            list[i] = toCvmType(cvmResultList.get(i));
        }
        return list;
    }

    public static byte toCvmType(ECvmResult cvm) {
        switch (cvm) {
            case CONSUMER_DEVICE:
                return CvmType.RD_CVM_CONSUMER_DEVICE;
            case NO_CVM:
                return CvmType.RD_CVM_NO;
            case OFFLINE_PIN:
                return CvmType.RD_CVM_OFFLINE_PIN;
            case ONLINE_PIN:
                return CvmType.RD_CVM_ONLINE_PIN;
            case REQ_ONLINE_PIN:
                return CvmType.RD_CVM_REQ_ONLINE_PIN;
            case REQ_SIG:
                return CvmType.RD_CVM_REQ_SIG;
            case SIG:
                return CvmType.RD_CVM_SIG;
            default:
                return -1;
        }
    }

    public static ECvmResult convertCVM(byte result) throws EmvException {
        switch (result) {
            case CvmType.RD_CVM_NO:
                return ECvmResult.NO_CVM;
            case CvmType.RD_CVM_ONLINE_PIN:
                return ECvmResult.ONLINE_PIN;
            case CvmType.RD_CVM_SIG:
                return ECvmResult.SIG;
            case CvmType.RD_CVM_CONSUMER_DEVICE:
                return ECvmResult.CONSUMER_DEVICE;
            case CvmType.RD_CVM_OFFLINE_PIN:
                return ECvmResult.OFFLINE_PIN;
            case CvmType.RD_CVM_REQ_ONLINE_PIN:
                return ECvmResult.REQ_ONLINE_PIN;
            case CvmType.RD_CVM_REQ_SIG:
                return ECvmResult.REQ_SIG;
            default:
                throw new EmvException(EEmvExceptions.EMV_ERR_INVALID_PARA);
        }
    }
}
