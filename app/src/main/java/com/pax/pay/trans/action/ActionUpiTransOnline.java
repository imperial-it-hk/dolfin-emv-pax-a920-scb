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
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.transmit.TransProcessListener;
import com.pax.pay.trans.transmit.TransProcessListenerImpl;
import com.pax.pay.trans.transmit.Transmit;

public class ActionUpiTransOnline extends AAction {
    private TransProcessListener transProcessListenerImpl;
    private Context context;
    private TransData transData;

    public ActionUpiTransOnline(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, TransData transData) {
        this.context = context;
        this.transData = transData;
    }

    @Override
    protected void process() {

        if (FinancialApplication.getAcqManager().getCurAcq().isEnableUpi()) {
            FinancialApplication.getApp().runInBackground(new Runnable() {
                @Override
                public void run() {
                    transProcessListenerImpl = new TransProcessListenerImpl(context);
                    int ret = new Transmit().transmit(transData, transProcessListenerImpl);
                    transProcessListenerImpl.onHideProgress();
                    setResult(new ActionResult(ret, null));
                }
            });
        }
        else
        {
            setResult(new ActionResult(TransResult.ERR_UPI_LOAD, null));
        }
    }

}