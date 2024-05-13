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
import com.pax.device.DeviceImplNeptune;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.IEmv;
import com.pax.eemv.IEmvListener;
import com.pax.eemv.entity.CTransResult;
import com.pax.eemv.exception.EmvException;
import com.pax.eventbus.EmvCallbackEvent;
import com.pax.jemv.device.DeviceManager;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.emv.EmvTransProcess;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.model.TransData.EnterMode;
import com.pax.pay.trans.transmit.TransProcessListener;
import com.pax.pay.trans.transmit.TransProcessListenerImpl;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import th.co.bkkps.utils.Log;

public class ActionGetPanProcess extends AAction {
    private Context context;
    private IEmv emv;
    private TransData transData;
    private TransProcessListener transProcessListener;
    private IEmvListener emvListener;

    private boolean icc_try_again = true;
    private boolean isOnlineApproved;
    private String pan = null;

    public ActionGetPanProcess(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, IEmv emv, TransData transData) {
        this.context = context;
        this.emv = emv;
        this.transData = transData;
        transProcessListener = new TransProcessListenerImpl(context);
        /*
        emvListener = new IEmvListener(){

            public Amounts onGetAmounts(){
                return null;
            }

            public int onWaitAppSelect(boolean isFirstSelect, List<CandList> candList){
                return 0;
            }

            public int onConfirmCardNo(String cardNo){
                pan = cardNo;
                return 0;
            }

            public int onCardHolderPwd(boolean bOnlinePin, int leftTimes, byte[] pinData){
                return 0;
            }

            public EOnlineResult onOnlineProc(){
                return null;
            }

            public boolean onChkExceptionFile(){
                return true;
            }

            public int setDe55ForReversal() throws EmvException{
                return 0;
            }

            public EOnlineResult onUpdateScriptResult(){
                return null;
            }

            public boolean onChkForceSettlement(){
                return true;
            }

            public void setCurAcq(){
                return;
            }
        };
//            @Override
//            public int onConfirmCardNo(String cardNo){
//                return super.onConfirmCardNo(cardNo);
//            }
//        }
//        emvListener = new EmvListenerImpl(context, emv, transData, transProcessListener){
//            @Override
//            public int onConfirmCardNo(String cardNo){
//                return super.onConfirmCardNo(cardNo);
//            }
//        };

         */
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onCardNumConfirmEvent(EmvCallbackEvent event) {
        switch ((EmvCallbackEvent.Status) event.getStatus()) {
            case OFFLINE_PIN_ENTER_READY:
                //emvListener.offlinePinEnterReady();
                icc_try_again = false;
                break;
            case CARD_NUM_CONFIRM_SUCCESS:
                //emvListener.cardNumConfigSucc((String[]) event.getData());
                icc_try_again = false;
                break;
            case CARD_NUM_CONFIRM_ERROR:
            default:
                //emvListener.cardNumConfigErr();
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
            if (transData.getEnterMode() == EnterMode.INSERT) {
                transProcessListener.onShowProgress(context.getString(R.string.wait_process), 0);
            }
            emvTransProcess = new EmvTransProcess(emv);
            emvTransProcess.init();
        }

        @Override
        public void run() {
            FinancialApplication.getApp().register(ActionGetPanProcess.this);
            try {
                CTransResult result = emvTransProcess.transProcess(transData, emvListener);
            } catch (EmvException e) {
                Log.e(TAG, "", e);
            } catch (Exception ex) {
                Log.e(TAG, "", ex);
            }


            transData.setPan(pan);


            emv.setListener(null);
            FinancialApplication.getApp().unregister(ActionGetPanProcess.this);
            setResult(new ActionResult(TransResult.SUCC, null));
        }

    }
}

