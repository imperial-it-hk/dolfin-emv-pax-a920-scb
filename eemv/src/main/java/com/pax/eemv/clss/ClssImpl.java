/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-6-15
 * Module Author: Kim.L
 * Description:
 *
 * ============================================================================
 */
package com.pax.eemv.clss;

import android.util.Log;

import com.pax.eemv.EmvBase;
import com.pax.eemv.IClss;
import com.pax.eemv.IClssListener;
import com.pax.eemv.entity.AidParam;
import com.pax.eemv.entity.CTransResult;
import com.pax.eemv.entity.ClssInputParam;
import com.pax.eemv.entity.ClssTornLogRecord;
import com.pax.eemv.entity.InputParam;
import com.pax.eemv.enums.ECvmResult;
import com.pax.eemv.enums.EKernelType;
import com.pax.eemv.enums.EOnlineResult;
import com.pax.eemv.enums.ETransResult;
import com.pax.eemv.exception.EEmvExceptions;
import com.pax.eemv.exception.EmvException;
import com.pax.eemv.utils.Converter;
import com.pax.eemv.utils.Tools;
import com.pax.jemv.clcommon.ByteArray;
import com.pax.jemv.clcommon.Clss_CandList;
import com.pax.jemv.clcommon.Clss_PreProcInfo;
import com.pax.jemv.clcommon.Clss_PreProcInterInfo;
import com.pax.jemv.clcommon.Clss_TransParam;
import com.pax.jemv.clcommon.KernType;
import com.pax.jemv.clcommon.RetCode;
import com.pax.jemv.clcommon.TransactionPath;
import com.pax.jemv.entrypoint.api.ClssEntryApi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClssImpl extends EmvBase implements IClss {

    private static final String TAG = "ClssImpl";

    private KernType kernType;
    private List<Clss_PreProcInfo> preProcInfos = new ArrayList<>();
    private Clss_TransParam transParam;
    private ClssInputParam inputParam;
    private List<ClssTornLogRecord> tornLogRecords;

    private IClssListener listener;
    private ClssProc clssProc = null;
    private Clss_PreProcInterInfo clssPreProcInterInfo;
    private AidParam aid;

    static {
//        System.loadLibrary("F_PUBLIC_LIB_Android");
//        System.loadLibrary("F_ENTRY_LIB_Android");
//        System.loadLibrary("JniEntry_V1.00.00_20170616");
        System.loadLibrary("F_PUBLIC_LIB_PayDroid");
        System.loadLibrary("F_ENTRY_LIB_PayDroid");
        System.loadLibrary("JNI_ENTRY_v100");
    }

    public ClssImpl() {
        kernType = new KernType();
    }

    @Override
    public IClss getClss() {
        return this;
    }

    @Override
    public EKernelType getKernelType() {
        switch (kernType.kernType) {
            case KernType.KERNTYPE_VIS:
                return EKernelType.VIS;
            case KernType.KERNTYPE_MC:
                return EKernelType.MC;
            case KernType.KERNTYPE_AE:
                return EKernelType.AE;
            case KernType.KERNTYPE_JCB:
                return EKernelType.JCB;
            case KernType.KERNTYPE_ZIP:
                return EKernelType.ZIP;
            case KernType.KERNTYPE_EFT:
                return EKernelType.EFT;
            case KernType.KERNTYPE_FLASH:
                return EKernelType.FLASH;
            case KernType.KERNTYPE_PBOC:
                return EKernelType.PBOC;
            case KernType.KERNTYPE_DEF:
            default:
                return EKernelType.DEF;
        }
    }

    @Override
    public void init() throws EmvException {
        super.init();
        if (ClssEntryApi.Clss_CoreInit_Entry() != RetCode.EMV_OK) {
            throw new EmvException(EEmvExceptions.EMV_ERR_NO_KERNEL);
        }
    }

    private void addApp() {
        preProcInfos.clear();
        for (AidParam i : aidParamList) {
            Clss_PreProcInfo clssPreProcInfo;
            if (Tools.bcd2Str(i.getAid()).contains("A000000677")) { //TBA_AID_PREFIX
                ClssEntryApi.Clss_AddAidList_Entry(i.getAid(), (byte) i.getAid().length,
                        i.getSelFlag(), (byte) KernType.KERNTYPE_PBOC);
                clssPreProcInfo = Converter.genClssPreProcInfoTPN(i, inputParam);
            } else {
                ClssEntryApi.Clss_AddAidList_Entry(i.getAid(), (byte) i.getAid().length,
                        i.getSelFlag(), (byte) KernType.KERNTYPE_DEF);
                clssPreProcInfo = Converter.genClssPreProcInfo(i, inputParam);
            }

            if (i.getReaderTTQ() != null) {
                clssPreProcInfo.aucReaderTTQ = Tools.str2Bcd(i.getReaderTTQ());
            }
            preProcInfos.add(clssPreProcInfo);
            ClssEntryApi.Clss_SetPreProcInfo_Entry(clssPreProcInfo);
        }
    }

    @Override
    public void preTransaction(ClssInputParam inputParam) throws EmvException {
        this.inputParam = inputParam;
        addApp();

        long ulAmtAuth = Long.parseLong(inputParam.getAmount());
        Log.i("amount", "amount = " + ulAmtAuth);
        String date = inputParam.getTransDate();
        String time = inputParam.getTransTime();
        transParam = new Clss_TransParam(ulAmtAuth, 0,
                Integer.parseInt(inputParam.getTransStanNo()),
                inputParam.getTag9CValue(), Tools.str2Bcd(date.substring(2)), Tools.str2Bcd(time));

        int ret = ClssEntryApi.Clss_PreTransProc_Entry(transParam);
        if (ret != RetCode.EMV_OK) {
            throw new EmvException(ret);
        }
    }

    @Override
    public CTransResult process(InputParam inputParam) throws EmvException {
        int ret = ClssEntryApi.Clss_SetMCVersion_Entry((byte) 0x03);
        if (ret != RetCode.EMV_OK) {
            throw new EmvException(ret);
        }

        ret = ClssEntryApi.Clss_AppSlt_Entry(0, 0);
        if (ret != RetCode.EMV_OK) {
            throw new EmvException(ret);
        }

        CTransResult result = startTransaction();
        if (result.getTransResult() == ETransResult.CLSS_OC_TRY_AGAIN
                || result.getTransResult() == ETransResult.CLSS_OC_REFER_CONSUMER_DEVICE
                || result.getTransResult() == ETransResult.CLSS_OC_TRY_ANOTHER_INTERFACE) {
            return result;
        }

        updateCardInfo();
        setClssTypeMode();

        if (result.getTransResult() != ETransResult.CLSS_OC_ONLINE_REQUEST) {
            return result;
        }

        if (!aid.getOnlinePin() && (result.getCvmResult() == ECvmResult.ONLINE_PIN || result.getCvmResult() == ECvmResult.ONLINE_PIN_SIG)) {
            result.setCvmResult(ECvmResult.SIG);
        }

        cvmResult(result.getCvmResult());

        ETransResult transResult = onlineProc(result);
        if (transResult != ETransResult.ONLINE_APPROVED &&
                transResult != ETransResult.ONLINE_DENIED &&
                transResult != ETransResult.ONLINE_FAILED) {
            if (isReversalFail) {
                throw new EmvException(EEmvExceptions.EMV_ERR_REVERSAL_FAIL);
            }
            if (isTleFail) {
                throw new EmvException(EEmvExceptions.EMV_ERR_TLE_FAIL);
            }
            throw new EmvException(EEmvExceptions.EMV_ERR_ONLINE_TRANS_ABORT_NO_DIALOG);
        }

        if (transResult == ETransResult.ONLINE_FAILED) {
            if (isOfflineApprNeedChkReverse) {
                result.setTransResult(ETransResult.OFFLINE_APPROVED_NEED_CHK_REVERSE);
                return result;
            }
            throw new EmvException(EEmvExceptions.EMV_ERR_ONLINE_TRANS_ABORT);
        }

        if (transResult == ETransResult.ONLINE_DENIED) {
            throw new EmvException(EEmvExceptions.EMV_ERR_DENIAL);
        }

        result.setTransResult(completeTransaction(transResult).getTransResult());
        return result;
    }

    @Override
    public CTransResult clssReadCardProcess(InputParam inputParam) throws EmvException {
        int ret = ClssEntryApi.Clss_SetMCVersion_Entry((byte) 0x03);
        if (ret != RetCode.EMV_OK) {
            throw new EmvException(ret);
        }

        ret = ClssEntryApi.Clss_AppSlt_Entry(0, 0);
        if (ret != RetCode.EMV_OK) {
            throw new EmvException(ret);
        }

        CTransResult result = startTransaction();
        if (result.getTransResult() == ETransResult.CLSS_OC_TRY_AGAIN
                || result.getTransResult() == ETransResult.CLSS_OC_REFER_CONSUMER_DEVICE
                || result.getTransResult() == ETransResult.CLSS_OC_TRY_ANOTHER_INTERFACE) {
            return result;
        }

        updateCardInfo();
        setClssTypeMode();

        if (result.getTransResult() != ETransResult.CLSS_OC_ONLINE_REQUEST) {
            return result;
        }

        if (!aid.getOnlinePin() && (result.getCvmResult() == ECvmResult.ONLINE_PIN || result.getCvmResult() == ECvmResult.ONLINE_PIN_SIG)) {
            result.setCvmResult(ECvmResult.SIG);
        }

        return result;
    }

    @Override
    public CTransResult clssAfterReadCardProcess(CTransResult result) throws EmvException {
        cvmResult(result.getCvmResult());

        if (onChkIsDynamicOffline()) {
            result.setTransResult(ETransResult.CLSS_OC_APPROVED);
            return result;
        }

        int ret = processEnterRefNo();
        if (ret != RetCode.EMV_OK) {
            throw new EmvException(ret);
        }

        ETransResult transResult = onlineProc(result);
        if (transResult != ETransResult.ONLINE_APPROVED &&
                transResult != ETransResult.ONLINE_DENIED &&
                transResult != ETransResult.ONLINE_FAILED) {
            if (isReversalFail) {
                throw new EmvException(EEmvExceptions.EMV_ERR_REVERSAL_FAIL);
            }
            if (isTleFail) {
                throw new EmvException(EEmvExceptions.EMV_ERR_TLE_FAIL);
            }
            throw new EmvException(EEmvExceptions.EMV_ERR_ONLINE_TRANS_ABORT_NO_DIALOG);
        }

        if (transResult == ETransResult.ONLINE_FAILED) {
            if (isOfflineApprNeedChkReverse) {
                result.setTransResult(ETransResult.OFFLINE_APPROVED_NEED_CHK_REVERSE);
                return result;
            }
            throw new EmvException(EEmvExceptions.EMV_ERR_ONLINE_TRANS_ABORT);
        }

        if (transResult == ETransResult.ONLINE_DENIED) {
            throw new EmvException(EEmvExceptions.EMV_ERR_DENIAL);
        }

        result.setTransResult(completeTransaction(transResult).getTransResult());
        return result;
    }

    private void updateCardInfo() throws EmvException {
        if (clssProc != null) {
            clssProc.updateCardInfo();
            if (chkForceSettlement()) {
                // check force settlement after reading chip card
                // If settlement is not successfully, the transaction should be blocked.
                throw new EmvException(EEmvExceptions.EMV_ERR_FORCE_SETTLEMENT);
            }
            return;
        }
        throw new EmvException(EEmvExceptions.EMV_ERR_NO_KERNEL);
    }

    private CTransResult startTransaction() throws EmvException {
        while (true) {
            ByteArray daArray = new ByteArray();
            if (continueSelectKernel(daArray))
                continue;

            clssPreProcInterInfo = new Clss_PreProcInterInfo();
            int ret = ClssEntryApi.Clss_GetPreProcInterFlg_Entry(clssPreProcInterInfo);
            if (ret != RetCode.EMV_OK) {
                throw new EmvException(ret);
            }

            ByteArray finalSelectData = new ByteArray();
            ret = ClssEntryApi.Clss_GetFinalSelectData_Entry(finalSelectData);
            if (ret != RetCode.EMV_OK) {
                throw new EmvException(ret);
            }

            aid = selectApp(daArray);
            if (aid == null) {
                throw new EmvException(EEmvExceptions.EMV_ERR_NO_APP);
            }

            cfg.setAcquirerId(aid.getAcquirerId());
            cfg.setCapability(aid.getCapability());

            try {
                clssProc = ClssProc.generate(kernType.kernType, listener)
                        .setAid(aid)
                        .setCapkList(capkList)
                        .setFinalSelectData(finalSelectData.data, finalSelectData.length)
                        .setTransParam(transParam)
                        .setConfig(cfg)
                        .setInputParam(inputParam)
                        .setPreProcInfo(preProcInfos.toArray(new Clss_PreProcInfo[0]))
                        .setPreProcInterInfo(clssPreProcInterInfo)
                        .setTornLogRecord(tornLogRecords);
                return clssProc.processTrans();
            } catch (EmvException e) {
                Log.w(TAG, "", e);
                if (e.getErrCode() != RetCode.CLSS_RESELECT_APP) {
                    throw e;
                }
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "", e);
                throw new EmvException(EEmvExceptions.EMV_ERR_NO_KERNEL);
            }

            //to re-select app
            ret = ClssEntryApi.Clss_DelCurCandApp_Entry();
            if (ret != RetCode.EMV_OK) {
                throw new EmvException(ret);
            }
        }
    }

    private boolean continueSelectKernel(ByteArray daArray) throws EmvException {
        kernType = new KernType();

        int ret = ClssEntryApi.Clss_FinalSelect_Entry(kernType, daArray);// output parameter?
        Log.i("clssEntryFinalSelect", "ret = " + ret + ", Kernel Type = " + kernType.kernType);
        if (ret == RetCode.EMV_RSP_ERR || ret == RetCode.EMV_APP_BLOCK
                || ret == RetCode.ICC_BLOCK || ret == RetCode.CLSS_RESELECT_APP) {
            ret = ClssEntryApi.Clss_DelCurCandApp_Entry();
            if (ret != RetCode.EMV_OK) {
                // 候选列表为空，进行相应错误处理，退出
                throw new EmvException(ret);
            }
            return true;
        } else if (ret != RetCode.EMV_OK) {
            throw new EmvException(ret);
        }
        return false;
    }

    private AidParam selectApp(ByteArray daArray) {
        AidParam aid = null;
        String da = Tools.bcd2Str(daArray.data, daArray.length);
        for (AidParam i : aidParamList) {
            if (da.contains(Tools.bcd2Str(i.getAid()))) {
                aid = i;
                acquirerName = i.getAcqHostName();
                break;
            }
        }

        return aid;
    }

    private CTransResult completeTransaction(ETransResult transResult) throws EmvException {
        byte[] value91 = getTlv(0x91);
        byte[] value71 = getTlv(0x71);
        byte[] value72 = getTlv(0x72);
        //not ask for 2nd tap if no script returned from Issuer
        if (!need2ndTap() || (value91 == null && value71 == null && value72 == null) || (clssPreProcInterInfo.aucReaderTTQ[2] & 0x80) != 0x80)
            return new CTransResult(ETransResult.ONLINE_APPROVED);

        if (detect2ndTap()) {
            if (clssProc != null) {
                return clssProc.completeTrans(transResult, value91, value71, value72);
            }
            throw new EmvException(EEmvExceptions.EMV_ERR_NO_KERNEL);
        }
        return new CTransResult(ETransResult.ABORT_TERMINATED);
    }

    private boolean detect2ndTap() throws EmvException {
        if (listener != null) {
            return listener.onDetect2ndTap();
        }
        throw new EmvException(EEmvExceptions.EMV_ERR_LISTENER_IS_NULL);
    }

    private void setClssTypeMode() throws EmvException {
        if (clssProc != null) {
            clssProc.setClssTypeMode();
            return;
        }
        throw new EmvException(EEmvExceptions.EMV_ERR_NO_KERNEL);
    }

    private boolean chkForceSettlement() {
        return listener != null && listener.onChkForceSettlement();
    }

    private boolean onChkIsDynamicOffline() {
        return listener != null && listener.onChkIsDynamicOffline();
    }

    private int processEnterRefNo() throws EmvException {
        if (listener != null) {
            return listener.onProcessEnterRefNo();
        }
        throw new EmvException(EEmvExceptions.EMV_ERR_LISTENER_IS_NULL);
    }

    @Override
    public byte[] getTlvSub(int tag) {
        if (clssProc != null) {
            ByteArray value = new ByteArray();
            if (clssProc.getTlv(tag, value) == RetCode.EMV_OK) {
                return Arrays.copyOf(value.data, value.length);
            }
        }
        return null;
    }

    @Override
    public void setTlvSub(int tag, byte[] value) throws EmvException {
        if (clssProc != null) {
            clssProc.setTlv(tag, value);
            return;
        }
        throw new EmvException(EEmvExceptions.EMV_ERR_NO_KERNEL);
    }

    @Override
    public void setTornLogRecords(List<ClssTornLogRecord> tornLogRecords) {
        this.tornLogRecords = tornLogRecords;
    }

    @Override
    public List<ClssTornLogRecord> getTornLogRecords() {
        return tornLogRecords;
    }

    @Override
    public void setListener(IClssListener listener) {
        this.listener = listener;
    }

    @Override
    public String getVersion() {
        return "1.00.00";
    }

    private boolean need2ndTap() {
        return (getKernelType() != EKernelType.MC
                && getTransPath() != TransactionPath.CLSS_VISA_MSD
                && getKernelType() != EKernelType.PBOC
                && getTransPath() != TransactionPath.CLSS_DPAS_MAG
                && getTransPath() != TransactionPath.CLSS_DPAS_ZIP
                && getTransPath() != TransactionPath.CLSS_JCB_MAG
                && getTransPath() != TransactionPath.CLSS_JCB_LEGACY);
    }

    private int getTransPath() {
        if (clssProc != null) {
            return clssProc.getTransPath();
        }
        return TransactionPath.CLSS_PATH_NORMAL;
    }

    private void cvmResult(ECvmResult result) throws EmvException {
        if (listener == null) {
            throw new EmvException(EEmvExceptions.EMV_ERR_LISTENER_IS_NULL);
        }
        int ret = listener.onCvmResult(result);
        if (ret != EEmvExceptions.EMV_OK.getErrCodeFromBasement()) {
            throw new EmvException(ret);
        }
    }

    private ETransResult onlineProc(CTransResult result) throws EmvException {
        if (listener == null) {
            throw new EmvException(EEmvExceptions.EMV_ERR_LISTENER_IS_NULL);
        }
        EOnlineResult ret = listener.onOnlineProc(result);
        if (ret == EOnlineResult.APPROVE) {
            return ETransResult.ONLINE_APPROVED;
        } else if (ret == EOnlineResult.ABORT || ret == EOnlineResult.REVERSAL_FAILED || ret == EOnlineResult.TLE_FAILED || ret == EOnlineResult.AMEX_ERR_GOTO_SECOND_GEN) {
            isReversalFail = (ret == EOnlineResult.REVERSAL_FAILED);
            isTleFail = (ret == EOnlineResult.TLE_FAILED);
            return ETransResult.ABORT_TERMINATED;
        } else if (ret == EOnlineResult.FAILED || ret == EOnlineResult.ONLINE_FAILED_NEED_CHK_REVERSAL) {
            isOfflineApprNeedChkReverse = (ret == EOnlineResult.ONLINE_FAILED_NEED_CHK_REVERSAL);
            return ETransResult.ONLINE_FAILED;
        } else {
            return ETransResult.ONLINE_DENIED;
        }
    }

    @Override
    public void readCardProcess(InputParam inputParam) throws EmvException {
        //do nothing
    }

    @Override
    public CTransResult afterReadCardProcess(InputParam inputParam) throws EmvException {
        //do nothing
        return null;
    }
}
