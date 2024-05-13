package com.pax.pay.trans.action;

import android.app.Activity;
import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.device.Device;
import com.pax.device.DeviceImplNeptune;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.IEmv;
import com.pax.eemv.enums.ETransResult;
import com.pax.eemv.exception.EEmvExceptions;
import com.pax.eemv.exception.EmvException;
import com.pax.eventbus.EmvCallbackEvent;
import com.pax.jemv.device.DeviceManager;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.emv.EmvListenerImpl;
import com.pax.pay.emv.EmvTransProcess;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.transmit.TransProcessListener;
import com.pax.pay.trans.transmit.TransProcessListenerImpl;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import th.co.bkkps.amexapi.AmexTransAPI;
import th.co.bkkps.utils.Log;

public class ActionEmvReadCardProcess extends AAction {
    private Context context;
    private IEmv emv;
    private TransData transData;
    private TransProcessListener transProcessListener;
    private EmvListenerImpl emvListener;

    private boolean noError;

    private boolean icc_try_again = true;

    public ActionEmvReadCardProcess(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, IEmv emv, TransData transData) {
        this.context = context;
        this.emv = emv;
        this.transData = transData;
        transProcessListener = new TransProcessListenerImpl(context);
        emvListener = new EmvListenerImpl(context, emv, transData, transProcessListener);
    }

    public void setParam(Context context, IEmv emv, TransData transData, boolean noError) {
        this.context = context;
        this.emv = emv;
        this.transData = transData;
        this.noError = noError;
        transProcessListener = new TransProcessListenerImpl(context);
        emvListener = new EmvListenerImpl(context, emv, transData, transProcessListener);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onCardNumConfirmEvent(EmvCallbackEvent event) {
        switch ((EmvCallbackEvent.Status) event.getStatus()) {
            case OFFLINE_PIN_ENTER_READY:
                emvListener.offlinePinEnterReady();
                icc_try_again = false;
                break;
            case CARD_NUM_CONFIRM_SUCCESS:
                emvListener.cardNumConfigSucc((String[]) event.getData());
                icc_try_again = false;
                break;
            case CARD_NUM_CONFIRM_ERROR:
            default:
                emvListener.cardNumConfigErr();
                break;
        }
    }

    @Override
    protected void process() {
        DeviceManager.getInstance().setIDevice(DeviceImplNeptune.getInstance());
        FinancialApplication.getApp().runInBackground(new ProcessRunnable());
    }

    private class ProcessRunnable implements Runnable {
        private EmvTransProcess emvTransProcess;

        ProcessRunnable() {
            if (transData.getEnterMode() == TransData.EnterMode.INSERT) {
                transProcessListener.onShowProgress(context.getString(R.string.wait_process), 0);
            }
            emvTransProcess = new EmvTransProcess(emv);
            emvTransProcess.init();
        }

        @Override
        public void run() {
            try {
                FinancialApplication.getApp().register(ActionEmvReadCardProcess.this);
                emvTransProcess.readCardProcess(transData, emvListener);
                transProcessListener.onHideProgress();
                setResult(new ActionResult(TransResult.SUCC, emv));
            } catch (EmvException e) {
                Log.e(TAG, "", e);
                emv.setListener(null);//no memory leak
                handleException(e);
            } catch (Exception ex) {
                Log.e(TAG, "", ex);
                emv.setListener(null);//no memory leak
            } finally {
                FinancialApplication.getApp().unregister(ActionEmvReadCardProcess.this);
            }
        }
    }

    private void handleException(EmvException e) {
        if(!noError) {
            ActivityStack.getInstance().popTo((Activity) context);
            if (Component.isDemo() &&
                    e.getErrCode() == EEmvExceptions.EMV_ERR_UNKNOWN.getErrCodeFromBasement()) {
                transProcessListener.onHideProgress();
                // end the EMV process, and continue a mag process
                setResult(new ActionResult(TransResult.SUCC, ETransResult.ARQC));
                return;
            }

            Device.beepErr();

            e = new EmvException(e.getErrCode());
            if (e.getErrCode() != EEmvExceptions.EMV_ERR_UNKNOWN.getErrCodeFromBasement()) {
                if (e.getErrCode() == EEmvExceptions.EMV_ERR_FALL_BACK.getErrCodeFromBasement()) {
                    transProcessListener.onShowNormalMessage(
                            context.getString(R.string.prompt_fall_back),
                            Constants.SUCCESS_DIALOG_SHOW_TIME, true);
                    transProcessListener.onHideProgress();
                    setResult(new ActionResult(TransResult.NEED_FALL_BACK, null));
                    return;
                }
                /*else if (e.getErrCode() == EEmvExceptions.EMV_ERR_CARD_NOT_SUPPORT.getErrCodeFromBasement() ||
                        e.getErrCode() == EEmvExceptions.EMV_ERR_NO_APP.getErrCodeFromBasement()) {
                    if (AmexTransAPI.getInstance().getProcess().isAppInstalled(context)) {
                        transProcessListener.onHideProgress();
                        setResult(new ActionResult(TransResult.ERR_NEED_FORWARD_TO_AMEX_API, null));
                        return;
                    } else {
                        transProcessListener.onShowErrMessage(e.getErrMsg(), Constants.FAILED_DIALOG_SHOW_TIME, true);
                    }
                } */
                else if (icc_try_again && (e.getErrCode() == EEmvExceptions.EMV_ERR_ICC_RESET.getErrCodeFromBasement() ||
                        e.getErrCode() == EEmvExceptions.EMV_ERR_ICC_CMD.getErrCodeFromBasement() ||
                        e.getErrCode() == EEmvExceptions.EMV_ERR_RSP.getErrCodeFromBasement() ||
                        e.getErrCode() == EEmvExceptions.EMV_ERR_NO_APP.getErrCodeFromBasement() ||
                        e.getErrCode() == EEmvExceptions.EMV_ERR_DATA.getErrCodeFromBasement())) {
                    transProcessListener.onShowErrMessage(e.getErrMsg() + "\n" + context.getString(R.string.wait_remove_card),
                            Constants.FAILED_DIALOG_SHOW_TIME, true);
                    transProcessListener.onHideProgress();
                    setResult(new ActionResult(TransResult.ICC_TRY_AGAIN, null));
                    return;
                } else {
                    transProcessListener.onShowErrMessage(e.getErrMsg(), Constants.FAILED_DIALOG_SHOW_TIME, true);
                }
            }
            transProcessListener.onHideProgress();
        }
        setResult(new ActionResult(TransResult.ERR_ABORTED, null));
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
