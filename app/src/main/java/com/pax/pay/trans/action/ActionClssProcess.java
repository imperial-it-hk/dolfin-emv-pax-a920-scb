/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-2-28
 * Module Author: lixc
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.trans.action;

import android.app.Activity;
import android.content.Context;
import th.co.bkkps.utils.Log;

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
import com.pax.pay.app.quickclick.QuickClickProtection;
import com.pax.pay.constant.Constants;
import com.pax.pay.emv.clss.ClssListenerImpl;
import com.pax.pay.emv.clss.ClssTransProcess;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.model.TransData.EnterMode;
import com.pax.pay.trans.transmit.TransProcessListener;
import com.pax.pay.trans.transmit.TransProcessListenerImpl;
import com.pax.pay.trans.transmit.Transmit;
import com.pax.pay.utils.Utils;
import com.pax.view.ClssLight;

import java.util.Objects;

public class ActionClssProcess extends AAction {
    private Context context;
    private IClss clss;
    private TransData transData;
    private TransProcessListener transProcessListener;
    private ClssListenerImpl clssListener;
    protected QuickClickProtection quickClickProtection = QuickClickProtection.getInstance();
    private boolean isOnlineApproved;

    public ActionClssProcess(ActionStartListener listener) {
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

    public void finish(ActionResult result) {
        if(isFinished())
            return;
        setFinished(true);
        quickClickProtection.start(); // AET-93
        setResult(result);
    }

    private class ProcessRunnable implements Runnable {
        private final ClssTransProcess clssTransProcess;

        ProcessRunnable() {
            if (transData.getEnterMode() == EnterMode.CLSS) {
                transProcessListener.onShowProgress(context.getString(R.string.wait_process), 0);
            }
            clssTransProcess = new ClssTransProcess(clss);
        }

        @Override
        public void run() {
            try {
                FinancialApplication.getApp().doEvent(new SearchCardEvent(SearchCardEvent.Status.CLSS_LIGHT_STATUS_PROCESSING));
                CTransResult result = clssTransProcess.transProcess(transData, clssListener);
                Device.beepPrompt();
                showTransResultMsg(result);

                ETransResult transResult = result.getTransResult();
                isOnlineApproved = (transResult == ETransResult.ONLINE_APPROVED);

                // Fixed EDCBBLAND-235 Modify auto reversal to support offline trans
                TransData.ReversalStatus reversalStatus = TransData.ReversalStatus.NORMAL;
                boolean isNeedChkReversal = false;
                if (!isOnlineApproved) {
                    isNeedChkReversal = (transResult == ETransResult.OFFLINE_APPROVED_NEED_CHK_REVERSE);
                    result.setTransResult(ETransResult.OFFLINE_APPROVED);
                    if (isNeedChkReversal) {
                        TransData dupTransData = FinancialApplication.getTransDataDbHelper().findFirstDupRecord(transData.getAcquirer());//reversal status is pending.
                        reversalStatus = dupTransData != null ? TransData.ReversalStatus.PENDING : reversalStatus;
                    }
                }

                updateReversalStatus(true, reversalStatus);

                if (!isOnlineApproved) {
                    // send reversal for offline approved
                    if (transData.getIssuer().isAutoReversal() && isNeedChkReversal && reversalStatus == TransData.ReversalStatus.PENDING) {
                        new Transmit().sendReversal(transProcessListener, transData.getAcquirer());
                        transProcessListener.onHideProgress();
                    }
                }

                finish(new ActionResult(TransResult.SUCC, result));
                FinancialApplication.getApp().doEvent(new SearchCardEvent(SearchCardEvent.Status.CLSS_LIGHT_STATUS_COMPLETE));
            } catch (EmvException e) {
                Log.e(TAG, "", e);

                if (transData.isOnlineTrans() && Component.isDemo()) {
                    updateReversalStatus(false, TransData.ReversalStatus.NORMAL);
                    finish(new ActionResult(TransResult.SUCC, new CTransResult(ETransResult.ONLINE_APPROVED)));
                    return;
                }

                FinancialApplication.getApp().doEvent(new SearchCardEvent(SearchCardEvent.Status.CLSS_LIGHT_STATUS_ERROR));
                handleException(e);
            } finally {
                Device.setPiccLed(-1, ClssLight.OFF);
                byte[] value95 = clss.getTlv(0x95);
                byte[] value9B = clss.getTlv(0x9B);

                Log.e("TLV", "95:" + Utils.bcd2Str(value95));
                Log.e("TLV", "9b:" + Utils.bcd2Str(value9B));

                // no memory leak
                clss.setListener(null);
                transProcessListener.onHideProgress();
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
            Device.beepErr();

            if (e.getErrCode() == EEmvExceptions.EMV_ERR_FORCE_SETTLEMENT.getErrCodeFromBasement()) {
                transProcessListener.onShowErrMessage(e.getErrMsg(), Constants.FAILED_DIALOG_SHOW_TIME, true);
            } else {
                if (e.getErrCode() != EEmvExceptions.EMV_ERR_UNKNOWN.getErrCodeFromBasement()) {
                    String respMsg = null;
                    if (transData.getResponseCode() != null) {
                        respMsg = transData.getResponseCode().getMessage();
                    }

                    int errTransAbort = EEmvExceptions.EMV_ERR_ONLINE_TRANS_ABORT.getErrCodeFromBasement();
                    int errTransAbortNoDialog = EEmvExceptions.EMV_ERR_ONLINE_TRANS_ABORT_NO_DIALOG.getErrCodeFromBasement();

                    //send auto reversal
                    if (sendAutoReversal(e)) {
                        if (e.getErrCode() == errTransAbort || e.getErrCode() == errTransAbortNoDialog) {
                            if (e.getErrCode() == errTransAbort) {
                                transProcessListener.onShowErrMessage(e.getErrMsg(), Constants.FAILED_DIALOG_SHOW_TIME, true);
                            }
                            new Transmit().sendReversal(transProcessListener, transData.getAcquirer());
                            transProcessListener.onHideProgress();
                            setResult(new ActionResult(TransResult.ERR_PROCESS_FAILED, null));
                            return;
                        }
                        new Transmit().sendReversal(transProcessListener, transData.getAcquirer());
                        transProcessListener.onHideProgress();
                    }

                    if (respMsg != null && Objects.equals(transData.getResponseCode().getCode(), "00")) {
                        transProcessListener.onShowErrMessage(Utils.getString(R.string.err_remove_card), Constants.FAILED_DIALOG_SHOW_TIME, true);
                    } else if (respMsg != null && !Objects.equals(respMsg, Utils.getString(R.string.err_undefine_info))) {
                        showMsgHostReject(respMsg);

                        if ((transData.getIssuer().isReferral() || (Constants.ACQ_AMEX.equals(transData.getAcquirer().getName()) || Constants.ACQ_AMEX_EPP.equals(transData.getAcquirer().getName())))
                                && "01".equals(transData.getResponseCode().getCode())) {
                            //For AMEX or another issuers that are set Referral, when transaction is referred by host, need to do referred transaction
                            transProcessListener.onHideProgress();
                            updateReversalStatus(false, TransData.ReversalStatus.NORMAL);
                            setResult(new ActionResult(TransResult.ERR_REFERRAL_CALL_ISSUER, null));
                            return;
                        }
                        transProcessListener.onHideProgress();
                        finish(new ActionResult(TransResult.ERR_ABORTED, null));
                        return;
                    } else {
                        if (e.getErrCode() == EEmvExceptions.EMV_ERR_REVERSAL_FAIL.getErrCodeFromBasement()) {
                            transProcessListener.onHideProgress();
                        finish(new ActionResult(TransResult.ERR_ABORTED, null));
                        return;
                        }
                        if (e.getErrCode() != errTransAbortNoDialog && e.getErrCode() != EEmvExceptions.EMV_ERR_RSP.getErrCodeFromBasement()) {
                            transProcessListener.onShowErrMessage(e.getErrMsg(), Constants.FAILED_DIALOG_SHOW_TIME, true);
                        }
                    }
                }
                transProcessListener.onHideProgress();
                finish(new ActionResult(TransResult.ERR_PROCESS_FAILED, null));
                return;
            }
            transProcessListener.onHideProgress();
            finish(new ActionResult(TransResult.ERR_ABORTED, null));
        }

        private void updateReversalStatus(boolean isSucc, TransData.ReversalStatus reversalStatus) {
            transData.setReversalStatus(reversalStatus);
            transData.setDupReason("");
            if (transData.getAcquirer().isEmvTcAdvice() && isSucc && isOnlineApproved &&
                    (transData.getTransType() == ETransType.SALE || checkIssuerAllowAdviceInstalment())) {
                transData.setAdviceStatus(TransData.AdviceStatus.PENDING);
            }
            FinancialApplication.getTransDataDbHelper().updateTransData(transData);
        }

        private boolean checkIssuerAllowAdviceInstalment() {
            byte[] aid = clss.getTlv(0x4F);
            boolean isVisa = (aid != null && aid.length > 0 && Utils.bcd2Str(aid).contains(Constants.VISA_AID_PREFIX));
            boolean isMaster = (aid != null && aid.length > 0 && Utils.bcd2Str(aid).contains(Constants.MASTER_AID_PREFIX));
            boolean isJCB = (aid != null && aid.length > 0 && Utils.bcd2Str(aid).contains(Constants.JCB_AID_PREFIX));
            return transData.getTransType() == ETransType.KBANK_SMART_PAY && (isVisa || isMaster || isJCB);
        }

        private boolean sendAutoReversal(EmvException e) {
            byte[] aid = clss.getTlv(0x4F);
            boolean isJCB = (aid != null && aid.length > 0 && Utils.bcd2Str(aid).contains(Constants.JCB_AID_PREFIX));

            int errTransAbort = EEmvExceptions.EMV_ERR_ONLINE_TRANS_ABORT.getErrCodeFromBasement();
            int errTransAbortNoDialog = EEmvExceptions.EMV_ERR_ONLINE_TRANS_ABORT_NO_DIALOG.getErrCodeFromBasement();
            int errReverseBeforeNextTrans = EEmvExceptions.EMV_ERR_REVERSAL_FAIL.getErrCodeFromBasement();

            TransData dupTransData = FinancialApplication.getTransDataDbHelper().findFirstDupRecord(transData.getAcquirer());//reversal status is pending.
            if (transData.isOnlineTrans() && dupTransData != null && e.getErrCode() != errReverseBeforeNextTrans &&
                    ((transData.getIssuer().isAutoReversal() && (e.getErrCode() == errTransAbort || e.getErrCode() == errTransAbortNoDialog))
                            || (!isJCB && e.getErrCode() != errTransAbort && e.getErrCode() != errTransAbortNoDialog))) {// Fixed EDCBBLAND-235 Modify auto reversal
                //Decline in 2nd generate AC or after finish online process, need to reversal for all acquirers except JCB (even if auto reversal flag is disabled).
                //Online transaction abort for Diners and other issuers (need to check flag auto reversal)
                return true;
            }
            return false;
        }

        // Fixed EDCBBLAND-383: Error message mapping with S500, support LE error code
        private void showMsgHostReject(String respMsg) {
            if ("LE".equals(transData.getResponseCode().getCode())) {
                transProcessListener.onShowErrMessage(
                        Utils.getString(R.string.prompt_err_code) + transData.getResponseCode().getCode()
                        + "\n" +
                        Component.unpackBit63Credit(transData.getField63RecByte(), transData.getField63()), Constants.FAILED_DIALOG_SHOW_TIME, true);
            } else {
                transProcessListener.onShowErrMessage(respMsg, Constants.FAILED_DIALOG_SHOW_TIME, true);
            }
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
