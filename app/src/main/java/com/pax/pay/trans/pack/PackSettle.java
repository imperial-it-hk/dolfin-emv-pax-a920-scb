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
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.ControlLimitUtils;

public class PackSettle extends PackIso8583 {

    public PackSettle(PackListener listener) {
        super(listener);
    }

    @Override
    @NonNull
    public byte[] pack(@NonNull TransData transData) {
        try {
            boolean isAmex = Constants.ACQ_AMEX.equals(transData.getAcquirer().getName()) || Constants.ACQ_AMEX_EPP.equals(transData.getAcquirer().getName());

            setMandatoryData(transData);

            // field 11
            setBitData11(transData);

            setBitData60(transData);

            if (isAmex || ControlLimitUtils.Companion.isEnableControlLimit(transData.getAcquirer().getName())) {
                setBitData62(transData);
            }

            // field 63
            setBitData63(transData);

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

    @Override
    protected void setBitData48(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("48", transData.getField48());
    }

    @Override
    protected void setBitData60(@NonNull TransData transData) throws Iso8583Exception {
        String f60 = Component.getPaddedNumber(transData.getBatchNo(), 6); // f60.2
        if (transData.getTransType() == ETransType.SETTLE_END) {
            f60 = transData.getField60();
        }
        setBitData("60", f60);
    }

    @Override
    protected void setBitData63(@NonNull TransData transData) throws Iso8583Exception {
        if (transData.getField63() == null) {
            setBitData("63", "");
        } else {
            super.setBitData63(transData);
        }
    }
}
