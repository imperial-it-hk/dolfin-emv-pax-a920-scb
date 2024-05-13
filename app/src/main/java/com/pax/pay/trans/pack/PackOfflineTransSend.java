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
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.settings.SysParam;

public class PackOfflineTransSend extends PackIsoBase {

    public PackOfflineTransSend(PackListener listener) {
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
        boolean isAmex = Constants.ACQ_AMEX.equals(transData.getAcquirer().getName());
        boolean isOfflineSale = (ETransType.SALE == transData.getOrigTransType() ||
                (ETransType.ADJUST != transData.getOrigTransType() && ETransType.OFFLINE_TRANS_SEND == transData.getTransType()))
                        && transData.getOfflineSendState() != null;

        // h
        String pHeader = transData.getTpdu() + transData.getHeader();
        entity.setFieldValue("h", pHeader);
        // m
        ETransType transType = transData.getTransType();
        entity.setFieldValue("m", transType.getMsgType());

        setBitData2(transData);
        // field 3
        setBitData3(transData);
        setBitData4(transData);
        //field 11
        setBitData11(transData);
        //field 12-13
        setBitData12(transData);
        setBitData14(transData);
        // field 22
        setBitData22(transData);

        // field 24 NII
        transData.setNii(transData.getAcquirer().getNii());
        setBitData24(transData);

        setBitData25(transData);

        setBitData38(transData);

        // field 41
        setBitData41(transData);

        // field 42
        setBitData42(transData);

        // [52]PIN
        setBitData52(transData);

        TransData.EnterMode enterMode = transData.getEnterMode();
        if (enterMode == TransData.EnterMode.INSERT || enterMode == TransData.EnterMode.CLSS || enterMode == TransData.EnterMode.SP200) {
			if (!isAmex) {//AMEX, not present
                setBitData23(transData);
            }
			if (isOfflineSale) {
			    // field 55
                setBitData55(transData);
            }
        }

        setBitData62(transData);

        setBitData63(transData);//kerry api

        if (transData.getOrigTransType() == ETransType.ADJUST) {
            setBitData37(transData);
            setBitData54(transData);
            setBitData60(transData);
        }
    }

    @Override
    protected void setBitData3(@NonNull TransData transData) throws Iso8583Exception {
        if (transData.getOrigTransType() == ETransType.ADJUST) {
            setBitData("3", "024000");
        } else if (ETransType.SALE != transData.getOrigTransType()
                && ETransType.ADJUST != transData.getOrigTransType()
                && ETransType.OFFLINE_TRANS_SEND == transData.getTransType()) {
            setBitData("3", "004000");
        } else {
            super.setBitData3(transData);
        }
    }

    @Override
    protected void setBitData60(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("60", transData.getOrigAmount());
    }

    @Override
    protected void setBitData63(@NonNull TransData transData) throws Iso8583Exception {
        boolean isKerryAPI = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_KERRY_API);
        String tempBranch = transData.getBranchID();
        // if no data on D8
        if (isKerryAPI && tempBranch != null) {
            String pdType = "CPAC";
            if(tempBranch.length() < 8){
                tempBranch = Component.getPaddedStringRight(transData.getBranchID(), 8, ' ');
            }else if (tempBranch.length() > 8){
                tempBranch = tempBranch.substring(0,8);
            }
            String branchID = pdType + tempBranch;
            transData.setBranchID(branchID);
            setBitData("63",branchID);
        }
    }
}
