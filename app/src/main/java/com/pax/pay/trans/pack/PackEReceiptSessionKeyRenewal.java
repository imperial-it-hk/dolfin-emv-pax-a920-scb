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
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.EReceiptUtils;

import th.co.bkkps.utils.ERMUtil;
import th.co.bkkps.utils.Log;

public class PackEReceiptSessionKeyRenewal extends PackIsoBase {

    public PackEReceiptSessionKeyRenewal(PackListener listener) {
        super(listener);
        super.setIsErcmEnable(true);
    }

    @Override
    @NonNull
    public byte[] pack(@NonNull TransData transData) {
        try {
            setMandatoryData(transData);
            return pack(false, transData);
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

        // field 03 Processing Code
        setBitData3(transData);

        // field 24 NII
        //transData.setNii(FinancialApplication.getAcqManager().getCurAcq().getNii());
        transData.setNii(transData.getInitAcquirerIndex());
        setBitData24(transData);

        setBitData46(transData);            // field 46 Terminal Serial Number
        setBitData55(transData);            // field 55 Bank code
        setBitData56(transData);            // field 56 Merchant code
        setBitData57(transData);            // field 57 Store code
        setBitData61(transData);            // field 61 Header Image
    }


    @Override
    protected void setBitData46(@NonNull TransData transData) throws Iso8583Exception {
        String serialNo = transData.getERCMTerminalSerialNumber();
        int max_len = 15;
        if (serialNo.length() < max_len) {
            serialNo = EReceiptUtils.getInstance().GetSerialNumber(transData.getERCMTerminalSerialNumber());
        }
        setBitData("46", EReceiptUtils.getInstance().getSize(serialNo.getBytes()));
    }

    @Override
    protected void setBitData55(@NonNull TransData transData) throws Iso8583Exception {
        String BankCode = transData.getERCMBankCode();
        if (BankCode != null) {
            setBitData("55", BankCode.getBytes());
            Log.i(EReceiptUtils.TAG, "ERCM-BankCode has been set.");
        } else {
            Log.i(EReceiptUtils.TAG, "getERCMBankCode in transdata was missing.");
        }

    }

    private void setBitData56(@NonNull TransData transData) throws Iso8583Exception {
        String mercCode = transData.getERCMMerchantCode();
        if (mercCode != null) {
            setBitData("56", mercCode.getBytes());
            Log.i(EReceiptUtils.TAG, "ERCM-MerchantCode has been set.");
        } else {
            Log.i(EReceiptUtils.TAG, "getERCMMerchantCode in transdata was missing.");
        }
    }

    private void setBitData57(@NonNull TransData transData) throws Iso8583Exception {
        String storeCode = ERMUtil.INSTANCE.getErmStoreCode(transData);
        //String storeCode = transData.getERCMStoreCode();
        if (storeCode != null) {
            setBitData("57", storeCode.getBytes());
            Log.i(EReceiptUtils.TAG, "ERCM-StoreCode has been set.");
        } else {
            Log.i(EReceiptUtils.TAG, "getERCMStoreCode in transdata was missing.");
        }
    }

    @Override
    protected void setBitData61(@NonNull TransData transData) throws Iso8583Exception {
        byte[] DE61Data = transData.getSessionKeyBlock();
        if (DE61Data != null) {
            Log.i(EReceiptUtils.TAG, "SessionKeyBlock was created.");
            //transData.setField61Byte(EReceiptUtils.getInstance().getSize(DE61Data));
            setBitData("61", DE61Data);
        } else {
            Log.i(EReceiptUtils.TAG, "SessionKeyBlock is missing.");
        }

    }

}
