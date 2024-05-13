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
package com.pax.pay.trans.action;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.dal.entity.EReaderType;
import com.pax.dal.entity.PollingResult;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.trans.transmit.TransProcessListener;
import com.pax.pay.trans.transmit.TransProcessListenerImpl;
import com.pax.pay.utils.Utils;

/**
 * result boolean : continue or not
 */
public class ActionRemoveCard extends AAction {
    private Context context;
    private String title;
    private TransProcessListener transProcessListener;

    public ActionRemoveCard(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, String title) {
        this.context = context;
        this.title = title;
        transProcessListener = new TransProcessListenerImpl(context);
    }

    @Override
    protected void process() {

        Device.removeCard(new Device.RemoveCardListener() {
            @Override
            public void onShowMsg(PollingResult result) {
                transProcessListener.onUpdateProgressTitle(title);
//                    transProcessListener.onShowWarning(result.getReaderType() == EReaderType.ICC
//                            ? context.getString(R.string.wait_pull_card)
//                            : context.getString(R.string.wait_remove_card), -1, 35);
                transProcessListener.onShowWarning(context.getString(R.string.wait_remove_card), -1, 35);
            }
        });
        transProcessListener.onHideProgress();

        setResult(new ActionResult(TransResult.SUCC, null));
    }

}
