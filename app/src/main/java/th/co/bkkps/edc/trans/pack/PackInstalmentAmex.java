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
package th.co.bkkps.edc.trans.pack;

import android.util.Log;

import androidx.annotation.NonNull;

import com.pax.abl.core.ipacker.PackListener;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.glwrapper.convert.IConvert;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.pack.PackIso8583;

public class PackInstalmentAmex extends PackIso8583 {

    public PackInstalmentAmex(PackListener listener) {
        super(listener);
    }

    @Override
    @NonNull
    public byte[] pack(@NonNull TransData transData) {
        try {
            setFinancialData(transData);
//            setBitData60(transData);

            if (IsTransTLE(transData)) {
                transData.setTpdu("600" + "007" + "0000");
                setBitHeader(transData);
                return packWithTLE(transData);
            }
            else
                return pack(false,transData);

        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
        return "".getBytes();
    }

    @Override
    protected void setFinancialData(@NonNull TransData transData) throws Iso8583Exception {
        setMandatoryData(transData);
    }

    @Override
    protected void setMandatoryData(@NonNull TransData transData) throws Iso8583Exception {
        boolean isReferral = false;

        // h
        String pHeader = transData.getTpdu() + transData.getHeader();
        entity.setFieldValue("h", pHeader);
        // m
        ETransType transType = transData.getTransType();
        if (transData.getReversalStatus() == TransData.ReversalStatus.REVERSAL) {
            entity.setFieldValue("m", transType.getDupMsgType());
        } else if (transData.getReferralStatus() == TransData.ReferralStatus.REFERRED) {
            entity.setFieldValue("m", "0220");
            isReferral = true;
        } else {
            entity.setFieldValue("m", transType.getMsgType());
        }

        // field 3
        setBitData3(transData);
        setBitData4(transData);
        //field 11
        setBitData11(transData);
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
            if (!isReferral) {
                setBitData35(transData);
            }

            //[54]tip amount  by lixc
//            setBitData54(transData);

        } else if (enterMode == TransData.EnterMode.INSERT || enterMode == TransData.EnterMode.CLSS) {
            // [2]主账号
//            setBitData2(transData);

            // [14]有效期
//            setBitData14(transData);


            if (!isReferral) {
                setBitData35(transData);

                // field 55 ICC
                setBitData55(transData);
            }
        }

        // field 41
        setBitData41(transData);

        // field 42
        setBitData42(transData);

        setBitData48(transData);

        // [52]PIN
        setBitData52(transData);

        setBitData62(transData);

        if ( isReferral) {
            setBitData2(transData);
            setBitData12(transData);
            setBitData14(transData);
            setBitData38(transData);
        }
    }

    @Override
    protected void setBitData38(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("38", transData.getOrigAuthCode());
    }

    @Override
    protected void setBitData48(@NonNull TransData transData) throws Iso8583Exception {
        byte[] terms = FinancialApplication.getConvert().strToBcd(transData.getInstalmentTerms(), IConvert.EPaddingPosition.PADDING_LEFT);
        setBitData("48", new byte[]{0x05, terms[0]});
    }
}
