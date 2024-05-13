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
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;

public class PackUpdateScriptResult extends PackIsoBase {

    public PackUpdateScriptResult(PackListener listener) {
        super(listener);
    }

    @Override
    @NonNull
    public byte[] pack(@NonNull TransData transData) {
        try {
            setCommonData(transData);
//            setBitData60(transData);

            return pack(false,transData);

        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
        return "".getBytes();
    }



    @Override
    protected void setCommonData(@NonNull TransData transData) throws Iso8583Exception {

        setMandatoryData(transData);

        ETransType transType = transData.getTransType();
        // field 2 主账号
        setBitData2(transData);
        // field 3
        setBitData3(transData);
        setBitData4(transData);
        //field 11
        setBitData11(transData);
        //field 12
        setBitData12(transData);
       //field 13
        setBitData13(transData);
        //field 14
        setBitData14(transData);

        // field 22
        setBitData22(transData);

        // field 23
        setBitData23(transData);

        // field 24 NII
        transData.setNii(FinancialApplication.getAcqManager().getCurAcq().getNii());
        setBitData24(transData);

        setBitData25(transData);

        // field 37
        setBitData37(transData);
        // field 38
        setBitData38(transData);

        // field 41
        setBitData41(transData);

        // field 42
        setBitData42(transData);


        setBitData55(transData);

        setBitData62(transData);


    }

    @Override
    protected void setBitData38(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("38", transData.getOrigAuthCode());
    }
}
