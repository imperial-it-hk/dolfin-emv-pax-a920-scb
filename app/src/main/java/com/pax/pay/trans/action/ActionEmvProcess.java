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

import android.app.Activity;
import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.device.Device;
import com.pax.device.DeviceImplNeptune;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.IEmv;
import com.pax.eemv.entity.CTransResult;
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
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.model.TransData.EnterMode;
import com.pax.pay.trans.transmit.TransProcessListener;
import com.pax.pay.trans.transmit.TransProcessListenerImpl;
import com.pax.pay.trans.transmit.Transmit;
import com.pax.pay.utils.Utils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Objects;

import th.co.bkkps.utils.Log;

public class ActionEmvProcess extends AAction {
    private Context context;
    private IEmv emv;
    private TransData transData;
    private TransProcessListener transProcessListener;
    private EmvListenerImpl emvListener;

    private boolean icc_try_again = true;
    private boolean isOnlineApproved;

    public ActionEmvProcess(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, IEmv emv, TransData transData) {
        this.context = context;
        this.emv = emv;
        this.transData = transData;
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
            if (transData.getEnterMode() == EnterMode.INSERT) {
                transProcessListener.onShowProgress(context.getString(R.string.wait_process), 0);
            }
            emvTransProcess = new EmvTransProcess(emv);
            emvTransProcess.init();
        }

        @Override
        public void run() {
            try {
                FinancialApplication.getApp().register(ActionEmvProcess.this);
                CTransResult result = emvTransProcess.transProcess(transData, emvListener);
                transProcessListener.onHideProgress();

                ETransResult transResult = result.getTransResult();

                // Fixed EDCBBLAND-370 send TC-ADVICE when transaction is approved online only. (prevent app crash)
                isOnlineApproved = (transResult == ETransResult.ONLINE_APPROVED);

                // Fixed EDCBBLAND-235 Modify auto reversal to support offline trans
                TransData.ReversalStatus reversalStatus = TransData.ReversalStatus.NORMAL;
                boolean isNeedChkReversal = false;
                if (!isOnlineApproved) {
                    isNeedChkReversal = (transResult == ETransResult.OFFLINE_APPROVED_NEED_CHK_REVERSE);
                    result = new CTransResult(ETransResult.OFFLINE_APPROVED);
                    if (isNeedChkReversal) {
                        TransData dupTransData = FinancialApplication.getTransDataDbHelper().findFirstDupRecord(transData.getAcquirer());//reversal status is pending.
                        reversalStatus = dupTransData != null ? TransData.ReversalStatus.PENDING : reversalStatus;
                    }
                }

                updateReversalStatus(true, reversalStatus);
//                if (transData.getAcquirer().isEmvTcAdvice() && transData.getTransType() == ETransType.SALE && isOnlineApproved) {// send TC-ADVICE for only Sale transaction
//                    Acquirer acquirer = transData.getAcquirer();
//                    TransData originalTransData = FinancialApplication.getTransDataDbHelper().findAdviceRecord(acquirer);
//                    int ret = new Transmit().sendTcAdvice(originalTransData, acquirer, transProcessListener);
//                    transProcessListener.onHideProgress();
//                    updateAdviceStatus(ret, originalTransData);
//                }

                if (!isOnlineApproved) {
                    // send reversal for offline approved
	                if (transData.getIssuer().isAutoReversal() && isNeedChkReversal && reversalStatus == TransData.ReversalStatus.PENDING) {
	                    new Transmit().sendReversal(transProcessListener, transData.getAcquirer());
	                    transProcessListener.onHideProgress();
	                }
				}

                setResult(new ActionResult(TransResult.SUCC, result));
            } catch (EmvException e) {
                Log.e(TAG, "", e);
                handleException(e);
            } catch (Exception ex) {
                Log.e(TAG, "", ex);
            } finally {
                byte[] value95 = emv.getTlv(0x95);
                byte[] value9B = emv.getTlv(0x9B);

                Log.e("TLV", "95:" + Utils.bcd2Str(value95));
                Log.e("TLV", "9b:" + Utils.bcd2Str(value9B));

                //read cvm
                byte[] sTemp = emv.getTlv(0x9F34);
                if (sTemp != null && sTemp.length > 0) {
                    sTemp[0] &= 0x3F;
                    if (sTemp[2]==0x02)		// last CVM succeed
                    {
                        if (sTemp[0]==0x01 ||	// plaintext PIN
                                sTemp[0]==0x03 ||	// plaintext PIN and signature
                                sTemp[0]==0x04 ||	// enciphered PIN
                                sTemp[0]==0x05)	// enciphered PIN and signature
                        {
                            transData.setPinVerifyMsg(true);
                            transData.setSignFree(true);
                        }
                    }
                }
                // no memory leak
                emv.setListener(null);
                FinancialApplication.getApp().unregister(ActionEmvProcess.this);
            }
        }

        private void handleException(EmvException e) {
            ActivityStack.getInstance().popTo((Activity) context);
            if (Component.isDemo() &&
                    e.getErrCode() == EEmvExceptions.EMV_ERR_UNKNOWN.getErrCodeFromBasement()) {
                transProcessListener.onHideProgress();
                updateReversalStatus(false, TransData.ReversalStatus.NORMAL);
                // end the EMV process, and continue a mag process
                setResult(new ActionResult(TransResult.SUCC, ETransResult.ARQC));
                return;
            }

            if (isNeedBeepError(e))
                Device.beepErr();

            e = new EmvException(e.getErrCode());
            if (e.getErrCode() == EEmvExceptions.EMV_ERR_FORCE_SETTLEMENT.getErrCodeFromBasement()) {
                transProcessListener.onShowErrMessage(e.getErrMsg(), Constants.FAILED_DIALOG_SHOW_TIME, true);
            } else {
                if (e.getErrCode() != EEmvExceptions.EMV_ERR_UNKNOWN.getErrCodeFromBasement()) {
                    if (e.getErrCode() == EEmvExceptions.EMV_ERR_FALL_BACK.getErrCodeFromBasement()) {
                        transProcessListener.onShowNormalMessage(
                                context.getString(R.string.prompt_fall_back),
                                Constants.SUCCESS_DIALOG_SHOW_TIME, true);
                        transProcessListener.onHideProgress();
                        setResult(new ActionResult(TransResult.NEED_FALL_BACK, null));
                        return;
                    } else {
                        if (icc_try_again && (e.getErrCode() == EEmvExceptions.EMV_ERR_ICC_RESET.getErrCodeFromBasement() ||
                                e.getErrCode() == EEmvExceptions.EMV_ERR_ICC_CMD.getErrCodeFromBasement() ||
                                e.getErrCode() == EEmvExceptions.EMV_ERR_RSP.getErrCodeFromBasement() ||
                                e.getErrCode() == EEmvExceptions.EMV_ERR_NO_APP.getErrCodeFromBasement() ||
                                e.getErrCode() == EEmvExceptions.EMV_ERR_DATA.getErrCodeFromBasement())) {
                            transProcessListener.onShowErrMessage(e.getErrMsg() + "\n" + context.getString(R.string.wait_remove_card),
                                    Constants.FAILED_DIALOG_SHOW_TIME, true);
                            transProcessListener.onHideProgress();
                            setResult(new ActionResult(TransResult.ICC_TRY_AGAIN, null));

                            // no memory leak
                            emv.setListener(null);
                            FinancialApplication.getApp().unregister(ActionEmvProcess.this);
                            return;
                        } else if (e.getErrCode() == EEmvExceptions.EMV_ERR_UNSUPPORTED_TRANS.getErrCodeFromBasement()) {
                            transProcessListener.onShowErrMessage(e.getErrMsg(), Constants.FAILED_DIALOG_SHOW_TIME, true);
                        } else if (e.getErrCode() == EEmvExceptions.EMV_NEED_MAG_ONLINE.getErrCodeFromBasement()) {
                            transProcessListener.onHideProgress();
                            setResult(new ActionResult(TransResult.ERR_NEED_MAG_ONLINE, null));
                            return;
                        } else {
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
                                transProcessListener.onShowErrMessage(Utils.getString(R.string.err_remove_card),
                                        Constants.FAILED_DIALOG_SHOW_TIME, true);
                            } else if (respMsg != null && !Objects.equals(respMsg, Utils.getString(R.string.err_undefine_info))) {
                                showMsgHostReject(respMsg);

                                if ((transData.getIssuer().isReferral() || Constants.ACQ_AMEX.equals(transData.getAcquirer()))
                                        && "01".equals(transData.getResponseCode().getCode())) {
                                    //For AMEX or another issuers that are set Referral, when transaction is referred by host, need to do referred transaction
                                    transProcessListener.onHideProgress();
                                    updateReversalStatus(false, TransData.ReversalStatus.NORMAL);
                                    setResult(new ActionResult(TransResult.ERR_REFERRAL_CALL_ISSUER, null));
                                    return;
                                }
                                transProcessListener.onHideProgress();
                                setResult(new ActionResult(TransResult.ERR_ABORTED, null));
                                return;
                            } else {
                                if (e.getErrCode() == EEmvExceptions.EMV_ERR_REVERSAL_FAIL.getErrCodeFromBasement()) {
                                    transProcessListener.onHideProgress();
                                    setResult(new ActionResult(TransResult.ERR_ABORTED, null));
                                    return;
                                }
                                if (e.getErrCode() != errTransAbortNoDialog && e.getErrCode() != EEmvExceptions.EMV_ERR_RSP.getErrCodeFromBasement()) {
                                    transProcessListener.onShowErrMessage(e.getErrMsg(), Constants.FAILED_DIALOG_SHOW_TIME, true);
                                }
                            }

                        }
                    }
                }
                transProcessListener.onHideProgress();
                setResult(new ActionResult(TransResult.ERR_PROCESS_FAILED, null));
                return;
            }
            transProcessListener.onHideProgress();
            setResult(new ActionResult(TransResult.ERR_ABORTED, null));
        }

        private void updateReversalStatus(boolean isSucc, TransData.ReversalStatus reversalStatus) {
            transData.setReversalStatus(reversalStatus);
            transData.setDupReason("");
            if (transData.getAcquirer().isEmvTcAdvice() && transData.getTransType() == ETransType.SALE && isSucc && isOnlineApproved) {
                transData.setAdviceStatus(TransData.AdviceStatus.PENDING);
            }
            FinancialApplication.getTransDataDbHelper().updateTransData(transData);
        }

        private boolean sendAutoReversal(EmvException e) {
            byte[] aid = emv.getTlv(0x4F);
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

        private void updateAdviceStatus(int ret, TransData adviceTrans) {
            if (ret == TransResult.SUCC) {
                if (adviceTrans.getTraceNo() == transData.getTraceNo()) {//if current transaction
                    transData.setAdviceStatus(TransData.AdviceStatus.NORMAL);
                    FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                } else {//another transaction
                    adviceTrans.setAdviceStatus(TransData.AdviceStatus.NORMAL);
                    FinancialApplication.getTransDataDbHelper().updateTransData(adviceTrans);
                }
            }
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

        private boolean isNeedBeepError(EmvException e) {
            return e.getErrCode() != EEmvExceptions.EMV_NEED_MAG_ONLINE.getErrCodeFromBasement();
        }

    }
}

