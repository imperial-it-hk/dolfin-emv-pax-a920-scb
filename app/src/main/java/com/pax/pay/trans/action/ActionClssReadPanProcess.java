package com.pax.pay.trans.action;

import android.app.Activity;
import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.device.Device;
import com.pax.device.DeviceImplNeptune;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.IClss;
import com.pax.eemv.entity.CTransResult;
import com.pax.eemv.enums.ETransResult;
import com.pax.eemv.exception.EEmvExceptions;
import com.pax.eemv.exception.EmvException;
import com.pax.eventbus.SearchCardEvent;
import com.pax.jemv.device.DeviceManager;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.emv.clss.ClssGetpanListenerImpl;
import com.pax.pay.emv.clss.ClssTransProcess;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.transmit.TransProcessListener;
import com.pax.pay.trans.transmit.TransProcessListenerImpl;
import com.pax.pay.utils.Utils;
import com.pax.view.ClssLight;

import java.util.Objects;

import th.co.bkkps.utils.Log;

public class ActionClssReadPanProcess extends AAction {
    private Context context;
    private IClss clss;
    private TransData transData;
    private TransProcessListener transProcessListener;
    private ClssGetpanListenerImpl clssListener;
    private boolean isNoError;

    public ActionClssReadPanProcess(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, IClss clss, TransData transData) {
        this.context = context;
        this.clss = clss;
        this.transData = transData;
        transProcessListener = new TransProcessListenerImpl(context);
        clssListener = new ClssGetpanListenerImpl(context, clss, transData, transProcessListener);
    }

    @Override
    protected void process() {
        DeviceManager.getInstance().setIDevice(DeviceImplNeptune.getInstance());
        FinancialApplication.getApp().runInBackground(new ProcessRunnable());
    }

    private class ProcessRunnable implements Runnable {
        private final ClssTransProcess clssTransProcess;

        ProcessRunnable() {
            if (transData.getEnterMode() == TransData.EnterMode.CLSS) {
                transProcessListener.onShowProgress(context.getString(R.string.wait_process), 0);
            }
            clssTransProcess = new ClssTransProcess(clss);
        }

        @Override
        public void run() {
            try {
                FinancialApplication.getApp().doEvent(new SearchCardEvent(SearchCardEvent.Status.CLSS_LIGHT_STATUS_PROCESSING));
                CTransResult result = clssTransProcess.readCardProcess(transData, clssListener);
            } catch (EmvException e) {
                Log.e(TAG, "", e);
            } finally {
                setResult(new ActionResult(TransResult.SUCC, null));
            }
        }
    }

    @Override
    public void setResult(ActionResult result) {
        if (TransContext.getInstance().getCurrentAction() == null
             || isFinished()) {
            return;
        }

        Device.setPiccLed(-1, ClssLight.OFF);
        transProcessListener.onHideProgress();
        TransContext.getInstance().getCurrentAction().setFinished(true); //AET-229
        TransContext.getInstance().setCurrentAction(null); //fix leaks
        super.setResult(result);
    }
}
