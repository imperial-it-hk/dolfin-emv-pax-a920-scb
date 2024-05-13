/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-1-12
 * Module Author: lixc
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.trans.action;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.transmit.TransProcessListener;
import com.pax.pay.trans.transmit.TransProcessListenerImpl;
import com.pax.pay.trans.transmit.Transmit;

import java.util.ArrayList;
import java.util.List;

public class ActionOfflineSend extends AAction {
    private TransProcessListener transProcessListenerImpl;
    private Context context;
    private TransData transData;

    public ActionOfflineSend(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context) {
        this.context = context;
    }

    public void setParam(Context context, TransData transData) {
        this.context = context;
        this.transData = transData;
    }

    @Override
    protected void process() {
        FinancialApplication.getApp().runInBackground(new Runnable() {
            @Override
            public void run() {
                transProcessListenerImpl = new TransProcessListenerImpl(context);
//                int ret = new Transmit().sendOfflineTrans(transProcessListenerImpl, true, false);
                List<TransData.OfflineStatus> excludes = new ArrayList<>();
                excludes.add(TransData.OfflineStatus.OFFLINE_SENT);
                excludes.add(TransData.OfflineStatus.OFFLINE_VOIDED);
                excludes.add(TransData.OfflineStatus.OFFLINE_ADJUSTED);
                TransData offlineData = FinancialApplication.getTransDataDbHelper().findOfflineTransData(transData.getAcquirer(), excludes);
                int ret = new Transmit().sendOfflineNormalSale(offlineData, transProcessListenerImpl);
                transProcessListenerImpl.onHideProgress();
                setResult(new ActionResult(ret, null));
            }
        });
    }
}
