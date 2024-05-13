/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-3-16
 * Module Author: laiyi
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.trans.action;

import th.co.bkkps.utils.Log;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.device.DeviceImplNeptune;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.IClss;
import com.pax.eemv.exception.EmvException;
import com.pax.jemv.device.DeviceManager;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.emv.EmvAid;
import com.pax.pay.emv.EmvCapk;
import com.pax.pay.emv.clss.ClssTransProcess;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.TransData;
import com.pax.sdk.Sdk;

public class ActionClssPreProc extends AAction {

    private TransData transData;
    private IClss clss;

    public ActionClssPreProc(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(IClss clss, TransData transData) {
        this.clss = clss;
        this.transData = transData;
    }

    @Override
    protected void process() {
        if (Sdk.isPaxDevice())
            FinancialApplication.getApp().runInBackground(new ProcessRunnable());
        else
            setResult(new ActionResult(TransResult.SUCC, null));
    }

    private class ProcessRunnable implements Runnable {

        ProcessRunnable() {
            DeviceManager.getInstance().setIDevice(DeviceImplNeptune.getInstance());
        }

        @Override
        public void run() {
            try {
                clss.init();
                clss.setConfig(ClssTransProcess.genClssConfig());
                clss.setAidParamList(EmvAid.toAidParams());
                clss.setCapkList(EmvCapk.toCapk());
                clss.preTransaction(Component.toClssInputParam(transData));
            } catch (EmvException e) {
                Log.e(TAG, "", e);
                setResult(new ActionResult(TransResult.ERR_CLSS_PRE_PROC, null));
            }
            setResult(new ActionResult(TransResult.SUCC, null));
        }


    }

    @Override
    public void setResult(ActionResult result) {
        if (isFinished()) {
            return;
        }
        setFinished(true);
        super.setResult(result);
    }

}
