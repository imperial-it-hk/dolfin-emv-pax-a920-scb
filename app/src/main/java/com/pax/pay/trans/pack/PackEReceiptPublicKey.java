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
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;

import th.co.bkkps.utils.Log;

public class PackEReceiptPublicKey extends PackIsoBase {

    public PackEReceiptPublicKey(PackListener listener) {
        super(listener);
    }

    @Override
    @NonNull
    public byte[] pack(@NonNull TransData transData) {
        try {
            setMandatoryData(transData);
            return pack(false,transData);
        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
        return "".getBytes();
    }

    @Override
    protected void setMandatoryData(@NonNull TransData transData) throws Iso8583Exception {
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

        // field 24 NII
        transData.setNii(FinancialApplication.getAcqManager().getCurAcq().getNii());
        setBitData24(transData);

        setBitData46(transData);            // field 46 Terminal Serial Number
//        setBitData55(transData);            // field 55 Bank code
//        setBitData56(transData);            // field 56 Merchant code
//        setBitData57(transData);            // field 57 Store code
//        setBitData61(transData);            // field 61 Header Image
//        setBitData62(transData);            // field 62 Footer Image
//        setBitData63(transData);            // field 63 Logo Image
    }

    @Override
    protected  void setBitData46(@NonNull TransData transData) throws Iso8583Exception {
        String serialNo = transData.getERCMTerminalSerialNumber();
        if (serialNo.length() != 15) {
            serialNo = String.format("%1$" + 15 + "s", serialNo);
        }
        setBitData("46", serialNo);
    }
}
