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
import com.pax.device.UserParam;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;

import th.co.bkkps.utils.Log;

public class PackBatchUp extends PackIso8583 {

    public PackBatchUp(PackListener listener) {
        super(listener);
    }

    @Override
    @NonNull
    public byte[] pack(@NonNull TransData transData) {
        try {
            setMandatoryData(transData);

//            setCommonData(transData);
//            setBitData60(transData);
            if (IsTransTLE(transData)) {
                transData.setTpdu("600" + UserParam.TLENI01 + "8000");
                setBitHeader(transData);
            }
            return pack(false,transData);
        } catch (Iso8583Exception e) {
            Log.e(TAG, "", e);
        }
        return "".getBytes();
    }

    protected void setMandatoryData(@NonNull TransData transData) throws Iso8583Exception {
        boolean isAmex = Constants.ACQ_AMEX.equals(transData.getAcquirer().getName()) ||
                        Constants.ACQ_AMEX_EPP.equals(transData.getAcquirer().getName()) ||
                        Constants.ISSUER_AMEX.equals(transData.getIssuer().getName());

        // h
        String pHeader = transData.getTpdu() + transData.getHeader();
        entity.setFieldValue("h", pHeader);
        // m
        ETransType transType = transData.getTransType();
        if (transData.getReversalStatus() == TransData.ReversalStatus.REVERSAL) {
            entity.setFieldValue("m", transType.getDupMsgType());
        } else {
            entity.setFieldValue("m", transType.getMsgType());
        }

        TransData.EnterMode enterMode = transData.getEnterMode();

        /*if (enterMode == TransData.EnterMode.MANUAL) {

        } else if (enterMode == TransData.EnterMode.SWIPE || enterMode == TransData.EnterMode.FALLBACK) {

        } else */
        if (enterMode == TransData.EnterMode.INSERT || enterMode == TransData.EnterMode.CLSS || enterMode == TransData.EnterMode.SP200) {
            if (!isAmex) {
                // field 55 ICC
                setBitData55(transData);
            }
        }

        //field 2
        setBitData2(transData);

        // field 3
        setBitData3(transData);

        // field 4
        setBitData4(transData);

        // field 11
        setBitData11(transData);

        //field 12 - 13
        setBitData12(transData);

        //field 14
        setBitData14(transData);

        // field 22
        setBitData22(transData);

        // field 24 NII
        transData.setNii(FinancialApplication.getAcqManager().getCurAcq().getNii());
        setBitData24(transData);

        // field 25
        setBitData25(transData);

        //field 37
        setBitData37(transData);

        //field 38
        setBitData38(transData);

        if (!isAmex) {
            //field 39
            setBitData39(transData);
        }

        // field 41
        setBitData41(transData);

        // field 42
        setBitData42(transData);

        // field 60
        setBitData60(transData);

        // field 62
        setBitData62(transData);

        if (transData.isDccRequired()) {
            setBitData6(transData);
            setBitData10(transData);
            setBitData51(transData);
        }
    }

    @Override
    protected void setBitData3(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("3", transData.getProcCode());
    }

    @Override
    protected void setBitData25(@NonNull TransData transData) throws Iso8583Exception {
        String serviceCode = transData.getOrigTransType().getServiceCode();
        if ((Constants.ACQ_AMEX.equals(transData.getAcquirer().getName())
                || Constants.ACQ_AMEX_EPP.equals(transData.getAcquirer().getName()))&& transData.isReferralSentSuccess()) {
            serviceCode = "06";
        }
        setBitData("25", serviceCode);
    }

    @Override
    protected void setBitData39(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("39", transData.getResponseCode().getCode());
    }

    /**
     * 设置批上送公共数据
     * <p>
     * 设置域： h,m, field 3, field 25, field 41,field 42
     */
    /*@Override
    protected void setCommonData(@NonNull TransData transData) throws Iso8583Exception {
        //field 2
        setBitData2(transData);

        // field 4 交易金額
        setBitData4(transData);

        // field 11 流水号
        setBitData11(transData);

        //field 12
        setBitData12(transData);

        //field 14
        setBitData14(transData);

        // field 22
        setBitData22(transData);

        //field 37
        setBitData37(transData);
    }*/

}
