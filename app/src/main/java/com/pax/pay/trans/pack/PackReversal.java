/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-11-25
 * Module Author: Steven.W
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.trans.pack;

import androidx.annotation.NonNull;

import com.pax.abl.core.ipacker.PackListener;
import com.pax.appstore.DownloadManager;
import com.pax.device.UserParam;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.glwrapper.convert.IConvert;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.settings.SysParam;

import th.co.bkkps.utils.Log;

public class PackReversal extends PackIso8583 {

    private ETransType transType;
    private boolean isRefund;
    private boolean isAmex;
    private boolean isUPTPN;

    public PackReversal(PackListener listener) {
        super(listener);
    }

    @Override
    @NonNull
    public byte[] pack(@NonNull TransData transData) {
        try {
            setCommonData(transData);
            //setBitData60(transData);

            if (IsTransTLE(transData)) {
                transData.setTpdu("600" + UserParam.TLENI01 + "8000");
                setBitHeader(transData);
            }
            return pack(false, transData);

        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
        return "".getBytes();
    }

    /**
     * 设置冲正公共类数据
     * <p>
     * <p>
     * 设置域
     * <p>
     * filed 2, field 4,field 11,field 14,field 22,field 23,field 38,
     * <p>
     * field 39,field 49,field 55,field 61
     *
     * @param transData
     * @return
     */
    @Override
    protected void setCommonData(@NonNull TransData transData) throws Iso8583Exception {
        transType = transData.getTransType();
        ETransType origTransType = transData.getOrigTransType();

        isAmex = Constants.ACQ_AMEX.equals(transData.getAcquirer().getName()) ||
                        Constants.ACQ_AMEX_EPP.equals(transData.getAcquirer().getName()) ||
                        Constants.ISSUER_AMEX.equals(transData.getIssuer().getName());
        isUPTPN = Constants.ACQ_UP.equals(transData.getAcquirer().getName());
        boolean isOfflineTransSend = ETransType.OFFLINE_TRANS_SEND == origTransType;//offline via menu
        isRefund = ETransType.REFUND == transType || ETransType.REFUND == origTransType;

        setMandatoryData(transData);

        TransData.EnterMode enterMode = transData.getEnterMode();

        /*if (enterMode != TransData.EnterMode.SWIPE && enterMode != TransData.EnterMode.FALLBACK) {// 磁条卡交易冲正,不上送2域
            // field 2 主账号
            setBitData2(transData);
        }*/

        // field 2 主账号
        setBitData2(transData);

        // field 4 交易金額
        setBitData4(transData);

        // field 11 流水号
        setBitData11(transData);

        // field 14 有效期
        setBitData14(transData);

        // field 22 服务点输入方式码
        setBitData22(transData);

        // field 23 卡片序列号
        if (enterMode == TransData.EnterMode.INSERT || enterMode == TransData.EnterMode.CLSS || enterMode == TransData.EnterMode.SP200) {
            if (!isAmex) {//AMEX, not present
                setBitData23(transData);
                if (!isOfflineTransSend) {
                    // [55]IC卡数据域
                    setBitData55(transData);
                }
            }
        }

        // field 24 NII
        setBitData24(transData);

        setBitData25(transData);

        // field 38
        //setBitData38(transData);

        // filed 39
        //setBitData39(transData);

        setBitData62(transData);

        if((transType == ETransType.VOID && !isAmex)/* || (isRefund && isUP)*/){
            // field 12-13
            setBitData12(transData);
        }

        if (transType == ETransType.VOID && !isAmex) {
            setBitData37(transData);
            setBitData38(transData);
        }

        if (transData.isDccRequired()) {
            setBitData6(transData);
            setBitData10(transData);
            setBitData51(transData);
        }

        setBitData63(transData);
    }

    @Override
    protected void setBitData3(@NonNull TransData transData) throws Iso8583Exception {
        if (isRefund && !isAmex) {
            int iProcCode = transType == ETransType.VOID ? 220000 : 200000;
            if (isUPTPN) {
                setBitData("3", String.valueOf(iProcCode));
            } else {
                setBitData("3", String.valueOf(iProcCode + 4000));
            }
        } else {
            super.setBitData3(transData);
        }
    }

    @Override
    protected void setBitData24(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("24", FinancialApplication.getAcqManager().getCurAcq().getNii());
    }

    @Override
    protected void setBitData37(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("37", transData.getOrigRefNo());
    }

    @Override
    protected void setBitData39(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("39", transData.getDupReason());
    }

    @Override
    protected void setBitData55(@NonNull TransData transData) throws Iso8583Exception {
        String temp = transData.getDupIccData();
        //String temp = transData.getSendIccData();
        if (temp != null && temp.length() > 0) {
            setBitData("55", FinancialApplication.getConvert().strToBcd(temp, IConvert.EPaddingPosition.PADDING_LEFT));
        }
    }

    @Override
    protected void setBitData63(@NonNull TransData transData) throws Iso8583Exception {
        int iMode = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_SUPPORT_REF1_2_MODE);
        DownloadManager.EdcRef1Ref2Mode mode = DownloadManager.EdcRef1Ref2Mode.getByMode(iMode);
        if (mode != DownloadManager.EdcRef1Ref2Mode.DISABLE) {
            setBitData("63", PackSale.setBitData63Ref1Ref2(transData));
        }
    }
}
