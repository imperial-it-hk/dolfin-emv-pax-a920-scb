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
package com.pax.pay.emv.clss;

import android.content.Context;

import com.pax.abl.utils.TrackUtils;
import com.pax.eemv.IClss;
import com.pax.eemv.enums.ECvmResult;
import com.pax.eemv.exception.EmvException;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.transmit.TransProcessListener;

public class ClssGetpanListenerImpl extends ClssListenerImpl {
    private static final String TAG = "ClssGetpanListenerImpl";


    public ClssGetpanListenerImpl(Context context, IClss clss, TransData transData, TransProcessListener listener) {
        super(context, clss, transData, listener);
    }

    @Override
    public int onCvmResult(ECvmResult result) {
        if (transProcessListener != null) {
            transProcessListener.onHideProgress();
        }
        intResult = 0;

        return intResult;
    }

    @Override
    public void onComfirmCardInfo(String track1, String track2, String track3) throws EmvException {
        transData.setTrack2(track2);
        transData.setTrack3(track3);
        String pan = TrackUtils.getPan(track2);
        transData.setPan(pan);
    }

    @Override
    public void onPromptRemoveCard() {
    }

}
