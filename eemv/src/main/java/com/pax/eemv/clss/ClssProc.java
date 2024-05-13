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


import com.pax.eemv.IClssListener;
import com.pax.eemv.entity.AidParam;
import com.pax.eemv.entity.CTransResult;
import com.pax.eemv.entity.Capk;
import com.pax.eemv.entity.ClssInputParam;
import com.pax.eemv.entity.ClssTornLogRecord;
import com.pax.eemv.entity.Config;
import com.pax.eemv.entity.TagsTable;
import com.pax.eemv.enums.ETransResult;
import com.pax.eemv.exception.EEmvExceptions;
import com.pax.eemv.exception.EmvException;
import com.pax.eemv.utils.Converter;
import com.pax.jemv.clcommon.ACType;
import com.pax.jemv.clcommon.ByteArray;
import com.pax.jemv.clcommon.Clss_PreProcInfo;
import com.pax.jemv.clcommon.Clss_PreProcInterInfo;
import com.pax.jemv.clcommon.Clss_TransParam;
import com.pax.jemv.clcommon.DDAFlag;
import com.pax.jemv.clcommon.EMV_CAPK;
import com.pax.jemv.clcommon.EMV_REVOCLIST;
import com.pax.jemv.clcommon.KernType;
import com.pax.jemv.clcommon.RetCode;
import com.pax.jemv.clcommon.TransactionPath;

import java.util.Arrays;
import java.util.List;

abstract class ClssProc {

    protected static final String TAG = "ClssProc";

    IClssListener listener;

    Clss_PreProcInterInfo preProcInterInfo;
    Clss_PreProcInfo[] arrayPreProcInfo;
    Clss_TransParam transParam;
    Config cfg;
    ClssInputParam inputParam;
    List<ClssTornLogRecord> tornLogRecords;
    AidParam aid;
    private List<Capk> capkList;
    byte[] finalSelectData = new byte[256];
    int finalSelectDataLen;

    TransactionPath transactionPath = new TransactionPath();

    ClssProc(IClssListener listener) {
        this.listener = listener;
    }

    static ClssProc generate(int kernelType, IClssListener listener) {
        switch (kernelType) {
            case KernType.KERNTYPE_VIS:
                return new ClssProcVis(listener);
            case KernType.KERNTYPE_MC:
                return new ClssProcMc(listener);
            case KernType.KERNTYPE_AE:
                return new ClssProcAE(listener);
            case KernType.KERNTYPE_PBOC:
                return new ClssProcPBOC(listener);
            case KernType.KERNTYPE_ZIP:
                return new ClssProcDpas(listener);
            case KernType.KERNTYPE_JCB:
                return new ClssProcJcb(listener);
            default:
                throw new IllegalArgumentException("Unsupported Kernel " + kernelType);
        }
    }

    void updateCardInfo() throws EmvException {
        if (listener != null) {
            listener.onComfirmCardInfo(getTrack1(), getTrack2(), getTrack3());
            return;
        }
        throw new EmvException(EEmvExceptions.EMV_ERR_LISTENER_IS_NULL);
    }

    void addCapkRevList() {
        ByteArray keyIdTLVDataList = new ByteArray(1);
        ByteArray aidTLVDataList = new ByteArray(17);
        if (getTlv(TagsTable.CAPK_ID, keyIdTLVDataList) == RetCode.EMV_OK &&
                getTlv(TagsTable.CAPK_RID, aidTLVDataList) == RetCode.EMV_OK) {
            byte index = keyIdTLVDataList.data[0];
            byte[] tempAid = new byte[5];
            System.arraycopy(aidTLVDataList.data, 0, tempAid, 0, 5);
            EMV_CAPK emvCapk = null;
            for (Capk capk : capkList) {
                if (Arrays.equals(capk.getRid(), tempAid) && capk.getKeyID() == index) {
                    emvCapk = Converter.toEMVCapk(capk);
                }
            }
            EMV_REVOCLIST emvRevocList = new EMV_REVOCLIST(tempAid, index, new byte[]{0x00, 0x07, 0x11});
            if (emvCapk != null) {
                onAddCapkRevList(emvCapk, emvRevocList);
            }
        }
    }

    void setClssTypeMode() throws EmvException {
        if (listener != null) {
            listener.onSetClssTypeMode(transactionPath.path);
            return;
        }
        throw new EmvException(EEmvExceptions.EMV_ERR_LISTENER_IS_NULL);
    }

    ClssProc setCapkList(List<Capk> capkList) {
        this.capkList = capkList;
        return this;
    }

    ClssProc setPreProcInfo(Clss_PreProcInfo[] arrayPreProcInfo) {
        this.arrayPreProcInfo = arrayPreProcInfo;
        return this;
    }

    ClssProc setPreProcInterInfo(Clss_PreProcInterInfo preProcInterInfo) {
        this.preProcInterInfo = preProcInterInfo;
        return this;
    }

    ClssProc setTransParam(Clss_TransParam transParam) {
        this.transParam = transParam;
        return this;
    }

    ClssProc setConfig(Config cfg) {
        this.cfg = cfg;
        return this;
    }

    ClssProc setAid(AidParam aid) {
        this.aid = aid;
        return this;
    }

    ClssProc setInputParam(ClssInputParam inputParam) {
        this.inputParam = inputParam;
        return this;
    }

    ClssProc setTornLogRecord(List<ClssTornLogRecord> tornLogRecords) {
        this.tornLogRecords = tornLogRecords;
        return this;
    }

    ClssProc setFinalSelectData(byte[] finalSelectData, int finalSelectDataLen) {
        this.finalSelectData = finalSelectData;
        this.finalSelectDataLen = finalSelectDataLen;
        return this;
    }

    int getCardAuthResult(int ret, ACType acType, DDAFlag flag, CTransResult result) throws EmvException {
        if (ret != RetCode.EMV_OK) {
            throw new EmvException(ret);
        } else if (acType.type == com.pax.jemv.clcommon.ACType.AC_AAC) {
            result.setTransResult(ETransResult.CLSS_OC_DECLINED);
            return RetCode.CLSS_DECLINE;
        } else if (flag.flag == DDAFlag.FAIL) {
            result.setTransResult(ETransResult.ABORT_TERMINATED);
            return RetCode.CLSS_FAILED;
        }
        return RetCode.EMV_OK;
    }

    int getTransPath() {
        return transactionPath.path;
    }

    abstract CTransResult processTrans() throws EmvException;

    abstract CTransResult completeTrans(ETransResult transResult, byte[] tag91, byte[] tag71, byte[] tag72) throws EmvException;

    protected abstract void onAddCapkRevList(EMV_CAPK emvCapk, EMV_REVOCLIST emvRevoclist);

    abstract int setTlv(int tag, byte[] value);

    abstract int getTlv(int tag, ByteArray value);

    abstract String getTrack1();

    abstract String getTrack2();

    abstract String getTrack3();

    /**
     *
     * @param tag57 emv tag57
     * @return track2
     */
    protected static String getTrack2FromTag57(String tag57){
        return tag57.split("F")[0];
    }
}
