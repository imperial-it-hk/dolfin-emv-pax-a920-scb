/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-7-27
 * Module Author: Kim.L
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
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.Convert;
import com.pax.pay.utils.Utils;

class PackIsoBase extends PackIso8583 {

    PackIsoBase(PackListener listener) {
        super(listener);
    }

    public void setIsErcmEnable(boolean exBool) {super.setIsErcmEnable(exBool);}
    public boolean getIsErcmEnable() {return super.getIsErcmEnable();}

    @Override
    @NonNull
    public byte[] pack(@NonNull TransData transData) {
        try {
            setFinancialData(transData);
            setBitData60(transData);

            return pack(true,transData);

        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
        return "".getBytes();
    }

    @Override
    protected void setBitData60(@NonNull TransData transData) throws Iso8583Exception {
        String f60 = Component.getPaddedNumber(transData.getBatchNo(), 6); // f60.2
        f60 += "600";
        setBitData("60", f60);
    }


}
