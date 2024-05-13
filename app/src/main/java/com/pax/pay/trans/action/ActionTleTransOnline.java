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
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.transmit.TransProcessListener;
import com.pax.pay.trans.transmit.TransProcessListenerImpl;
import com.pax.pay.trans.transmit.Transmit;

public class ActionTleTransOnline extends AAction {
    private TransProcessListener transProcessListenerImpl;
    private Context context;
    private TransData transData;
    private boolean isAutoDownloadMode;

    public ActionTleTransOnline(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, TransData transData) {
        this.context = context;
        this.transData = transData;
    }

    public void setParam(Context context, TransData transData, boolean isAutoDownloadMode) {
        this.context = context;
        this.transData = transData;
        this.isAutoDownloadMode = isAutoDownloadMode;
    }
    @Override
    protected void process() {
        Acquirer acq = FinancialApplication.getAcqManager().getCurAcq();
        if(Constants.ACQ_SCB_IPP.equals(acq.getName())){
//            Intent intent = new Intent(ActivityStack.getInstance().top(), ScbIppLoadTle.class);
//            context.startActivity(intent);
            setResult(new ActionResult(TransResult.SUCC, null));
        } else {
            if (FinancialApplication.getAcqManager().getCurAcq().isEnableTle()) {
                FinancialApplication.getApp().runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        transProcessListenerImpl = new TransProcessListenerImpl(context);
                        if (transData.getAcquirer() != null && transData.getAcquirer().isEnableTle()) {
                            transProcessListenerImpl.onShowNormalMessage("Host : " +transData.getAcquirer().getName() + "\nTLE download please wait..."  , 1, false);
                            int ret = new Transmit().transmit(transData, transProcessListenerImpl);
                            transProcessListenerImpl.onHideProgress();
                            setResult(new ActionResult(ret, null));
                        } else {
                            if (isAutoDownloadMode) {
                                setResult(new ActionResult(TransResult.ERR_ABORTED, null));
                            } else {
                                setResult(new ActionResult(TransResult.ERR_UNSUPPORTED_TLE, null));
                            }
                        }
                    }
                });
            } else {
                if (isAutoDownloadMode) {
                    setResult(new ActionResult(TransResult.ERR_ABORTED, null));
                } else {
                    setResult(new ActionResult(TransResult.ERR_TLE_REQUEST, null));
                }

            }
        }
    }
}