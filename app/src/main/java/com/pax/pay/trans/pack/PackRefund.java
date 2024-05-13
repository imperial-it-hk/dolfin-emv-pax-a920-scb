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

import th.co.bkkps.utils.Log;
import androidx.annotation.NonNull;
import com.pax.abl.core.ipacker.PackListener;
import com.pax.device.UserParam;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;

public class PackRefund extends PackIsoBase {

    private boolean isAmex;
    private boolean isUPTPN;

    public PackRefund(PackListener listener) {
        super(listener);
    }

    @Override
    @NonNull
    public byte[] pack(@NonNull TransData transData) {
        try {
            setMandatoryData(transData);

            if (IsTransTLE(transData)) {
                transData.setTpdu("600" + UserParam.TLENI01 + "8000");
                setBitHeader(transData);
                return packWithTLE(transData);
            } else {
                return pack(false, transData);
            }
        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
        return "".getBytes();
    }

    @Override
    protected void setMandatoryData(@NonNull TransData transData) throws Iso8583Exception {
        isAmex = Constants.ACQ_AMEX.equals(transData.getAcquirer().getName());
        isUPTPN = Constants.ACQ_UP.equals(transData.getAcquirer().getName());

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

        // field 3
        setBitData3(transData);
        setBitData4(transData);
        //field 11
        setBitData11(transData);

        /*if (isUP) {
            // field 12-13
            setBitData12(transData);
        }*/ //Comment as KBank request msg spec.

        // field 22
        setBitData22(transData);

        // field 24 NII
        transData.setNii(FinancialApplication.getAcqManager().getCurAcq().getNii());
        setBitData24(transData);

        setBitData25(transData);

        TransData.EnterMode enterMode = transData.getEnterMode();

        if (enterMode == TransData.EnterMode.MANUAL) {
            setBitData2(transData);

            setBitData14(transData);

        } else if (enterMode == TransData.EnterMode.SWIPE || enterMode == TransData.EnterMode.FALLBACK) {
            setBitData35(transData);

        } else if (enterMode == TransData.EnterMode.INSERT || enterMode == TransData.EnterMode.CLSS || enterMode == TransData.EnterMode.SP200) {
            if (!isAmex) {//AMEX, not present
                setBitData23(transData);
            }

            setBitData35(transData);

//            if (isAmex) {
                // field 55 ICC
                setBitData55(transData);
//            } //Comment as KBank allow Full EMV
        }

        // field 37
        setBitData37(transData);//TPN|UPI - from manually entry, otherwise - null (not present)

        // field 41
        setBitData41(transData);

        // field 42
        setBitData42(transData);

        // [52]PIN
        setBitData52(transData);

        setBitData62(transData);
    }

    @Override
    protected void setBitData3(@NonNull TransData transData) throws Iso8583Exception {
        if (isAmex) {
            super.setBitData3(transData);
        } else {
            setBitData("3", String.valueOf(isUPTPN ? 200000 : 204000));
        }
    }
}
