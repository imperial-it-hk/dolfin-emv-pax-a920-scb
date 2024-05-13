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
import com.pax.pay.emv.clss.ClssListenerImpl;
import com.pax.pay.emv.clss.ClssTransProcess;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.transmit.TransProcessListener;
import com.pax.pay.trans.transmit.TransProcessListenerImpl;
import com.pax.pay.utils.Utils;
import com.pax.view.ClssLight;

import java.util.Objects;

import th.co.bkkps.amexapi.AmexTransAPI;
import th.co.bkkps.utils.Log;

public class ActionClssReadCardProcess extends AAction {
    private Context context;
    private IClss clss;
    private TransData transData;
    private TransProcessListener transProcessListener;
    private ClssListenerImpl clssListener;

    public ActionClssReadCardProcess(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, IClss clss, TransData transData) {
        this.context = context;
        this.clss = clss;
        this.transData = transData;
        transProcessListener = new TransProcessListenerImpl(context);
        clssListener = new ClssListenerImpl(context, clss, transData, transProcessListener);
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
                Device.beepPrompt();
                showTransResultMsg(result);

                setResult(new ActionResult(TransResult.SUCC, result));
            } catch (EmvException e) {
                Log.e(TAG, "", e);

                if (transData.isOnlineTrans() && Component.isDemo()) {
                    setResult(new ActionResult(TransResult.SUCC, new CTransResult(ETransResult.ONLINE_APPROVED)));
                    return;
                }

                // no memory leak
                clss.setListener(null);
                FinancialApplication.getApp().doEvent(new SearchCardEvent(SearchCardEvent.Status.CLSS_LIGHT_STATUS_ERROR));
                //transProcessListener.onShowErrMessage(context.getString(R.string.prompt_please_retry), Constants.FAILED_DIALOG_SHOW_TIME, true);
                handleException(e);
            } finally {
                //move to end setResult
            }
        }

        private void showTransResultMsg(CTransResult result) {
            switch (result.getTransResult()) {
                case CLSS_OC_DECLINED://If transaction is declined by card, display message 'Clss Declined'
                    transProcessListener.onShowErrMessage(context.getString(R.string.dialog_clss_declined), Constants.FAILED_DIALOG_SHOW_TIME, true);
                    break;
                case CLSS_OC_TRY_AGAIN:
                    transProcessListener.onShowErrMessage(context.getString(R.string.prompt_please_retry), Constants.FAILED_DIALOG_SHOW_TIME, true);
                    break;
            }
        }

        private void handleException (EmvException e) {
            ActivityStack.getInstance().popTo((Activity) context);

            e = new EmvException(e.getErrCode());
            if (e.getErrCode() == EEmvExceptions.EMV_ERR_FORCE_SETTLEMENT.getErrCodeFromBasement()) {
                transProcessListener.onShowErrMessage(e.getErrMsg(), Constants.FAILED_DIALOG_SHOW_TIME, true);
            }
            /*else if (e.getErrCode() == EEmvExceptions.EMV_ERR_CARD_NOT_SUPPORT.getErrCodeFromBasement() ||
                    e.getErrCode() == EEmvExceptions.EMV_ERR_NO_APP.getErrCodeFromBasement()) {
                if (AmexTransAPI.getInstance().getProcess().isAppInstalled(context)) {
                    transProcessListener.onHideProgress();
                    setResult(new ActionResult(TransResult.ERR_NEED_FORWARD_TO_AMEX_API, null));
                } else {
                    transProcessListener.onShowErrMessage(e.getMessage(), Constants.FAILED_DIALOG_SHOW_TIME, true);
                    setResult(new ActionResult(TransResult.ERR_ABORTED, null));
                }
                return;
            } */
            else {
                if (e.getErrCode() != EEmvExceptions.EMV_ERR_UNKNOWN.getErrCodeFromBasement()) {
                    String respMsg = null;
                    if (transData.getResponseCode() != null) {
                        respMsg = transData.getResponseCode().getMessage();
                    }

                    if (respMsg != null && Objects.equals(transData.getResponseCode().getCode(), "00")) {
                        transProcessListener.onShowErrMessage(Utils.getString(R.string.err_remove_card),
                                Constants.FAILED_DIALOG_SHOW_TIME, true);
                    } else if (respMsg != null && !respMsg.equals(Utils.getString(R.string.err_undefine_info))) {

                        transProcessListener.onHideProgress();
                        setResult(new ActionResult(TransResult.ERR_ABORTED, null));
                        return;
                    } else {
                        transProcessListener.onShowErrMessage(e.getMessage(), Constants.FAILED_DIALOG_SHOW_TIME, true);
                        setResult(new ActionResult(TransResult.ERR_ABORTED, null));
                        return;
                    }
                }
                transProcessListener.onHideProgress();
                setResult(new ActionResult(TransResult.ERR_PROCESS_FAILED, null));
                return;
            }
            transProcessListener.onHideProgress();
            setResult(new ActionResult(TransResult.ERR_ABORTED, null));
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
