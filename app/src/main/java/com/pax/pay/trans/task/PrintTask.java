/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-7-31
 * Module Author: Kim.L
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.trans.task;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.trans.action.ActionEReceipt;
import com.pax.pay.trans.action.ActionEReceiptInfoUpload;
import com.pax.pay.trans.action.ActionPrintPreview;
import com.pax.pay.trans.action.ActionPrintTransReceipt;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.EReceiptUtils;
import com.pax.settings.SysParam;

import th.co.bkkps.utils.Log;

public class PrintTask extends BaseTask {
    private TransData transData;
    //private boolean isSupportESignature;

    public PrintTask(Context context, TransData transData, TransEndListener transListener) {
        super(context, transListener);
        this.transData = transData;
        //this.isSupportESignature = Component.isAllowSignatureUpload(transData);
        //Log.d(EReceiptUtils.TAG, "       isSupportESignature = " + isSupportESignature);
    }

    public static TransEndListener genTransEndListener(final BaseTask task, final String state) {
        return new TransEndListener() {

            @Override
            public void onEnd(final ActionResult result) {
                FinancialApplication.getApp().runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        task.onActionResult(state, result);
                    }
                });
            }
        };
    }

    enum State {
        CHECK_AND_INIT_ERCM,
        SIGNATURE_UPLOAD,
        PRINT_PREVIEW,
        PRINT_TICKET,
        SESSIONKEY_RENEWAL
    }

    @Override
    protected void bindStateOnAction() {
        ActionEReceipt checkAndInitERCM = new ActionEReceipt(action -> {
            ((ActionEReceipt) action).setParam(ActionEReceipt.eReceiptMode.CHECK_AND_INIT_TERMINAL_AND_SESSION_KEY, getCurrentContext(), FinancialApplication.getDownloadManager().getSn(), transData.getAcquirer());
        });
        bind(State.CHECK_AND_INIT_ERCM.toString(), checkAndInitERCM, false);

        // EReceipt & ESignature Upload
        ActionEReceiptInfoUpload eReceiptInfoUploadAction = new ActionEReceiptInfoUpload(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                Log.d(EReceiptUtils.TAG, "       transType       = " + transData.getTransType().toString());
                Log.d(EReceiptUtils.TAG, "       transState      = " + transData.getTransState().toString());
                Log.d(EReceiptUtils.TAG, "       OfflineState    = " + (transData.getOfflineSendState() != null ? transData.getOfflineSendState().toString() : "Online"));
                ((ActionEReceiptInfoUpload) action).setParam(getCurrentContext(), transData, ActionEReceiptInfoUpload.EReceiptType.ERECEIPT);
            }
        });
        bind(State.SIGNATURE_UPLOAD.toString(), eReceiptInfoUploadAction, false);


        ActionEReceipt sessionKeyRenewal = new ActionEReceipt(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                // for this action during initial sessionkey renewal must send specified target host to ActionEReceipt too.
                ((ActionEReceipt) action).setParam(ActionEReceipt.eReceiptMode.DL_SESSION_KEY_ALL_HOST, getCurrentContext(), FinancialApplication.getDownloadManager().getSn(), origTransData.getAcquirer());
            }
        });
        bind(State.SESSIONKEY_RENEWAL.toString(), sessionKeyRenewal, false);

        //print preview action
        ActionPrintPreview printPreviewAction = new ActionPrintPreview(
                new AAction.ActionStartListener() {

                    @Override
                    public void onStart(AAction action) {
                        ((ActionPrintPreview) action).setParam(getCurrentContext(), transData);
                    }
                });
        bind(State.PRINT_PREVIEW.toString(), printPreviewAction);

        // print action
        ActionPrintTransReceipt printTransReceiptAction = new ActionPrintTransReceipt(
                new AAction.ActionStartListener() {

                    @Override
                    public void onStart(AAction action) {
                        ((ActionPrintTransReceipt) action).setParam(getCurrentContext(), transData);
                    }
                }, Component.isAllowSignatureUpload(transData));
        bind(State.PRINT_TICKET.toString(), printTransReceiptAction, true);

        IsSessionKeyRenewCompleted = false;
        if (Component.isAllowSignatureUpload(transData)) {
            Log.d(EReceiptUtils.TAG, "[" + transData.getAcquirer().getNii() + "-" + transData.getAcquirer().getName() + "] - Allow to upload");
            gotoState(State.CHECK_AND_INIT_ERCM.toString());
        } else {
            // check small amount when EDC close ERCM mode or for host that set disable upload ERCM
            if (transData.isTxnSmallAmt() && transData.getNumSlipSmallAmt() == 0) {//EDCBBLAND-426 Support small amount
                FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                transEnd(new ActionResult(TransResult.SUCC, null));
                return;
            }
            Log.d(EReceiptUtils.TAG, "[" + transData.getAcquirer().getNii() + "-" + transData.getAcquirer().getName() + "] - ERM upload wasn't allow");
            gotoState(State.PRINT_PREVIEW.toString());
        }
    }

    private TransData origTransData = null;
    private boolean IsSessionKeyRenewCompleted;
    private ActionResult sessionKeyRenewalResult = null;

    //secondupload trans
    private TransData firstERMTrans = null;
    private int nextTransupload = 0;
    private final int maxNextTransupload = 1;
    private int currentNumPrintSlip = 0;

    @Override
    public void onActionResult(String currentState, ActionResult result) {
        State state = State.valueOf(currentState);

        switch (state) {
            case CHECK_AND_INIT_ERCM:
                if (result.getRet() == TransResult.SUCC) {
                    gotoState(State.SIGNATURE_UPLOAD.toString());
                } else {
                    gotoState(State.PRINT_PREVIEW.toString());
                }

                break;
            case SIGNATURE_UPLOAD:
                boolean supportNextTransUpload = FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE_NEXT_TRANS_UPLOAD);
                if (supportNextTransUpload) {
                    if (result.getRet() == TransResult.SUCC) {
                        if (nextTransupload == 0) {
                            Log.d(EReceiptUtils.TAG, " >> ERM Signature upload completed");
                        } else {
                            Log.d(EReceiptUtils.TAG, " >> ERM Additional ERM Re-upload : " + (nextTransupload) + " trans. completed");
                        }
                        if (nextTransupload < maxNextTransupload) {
                            int pendingRecordCount = FinancialApplication.getTransDataDbHelper().findCountTransDataWithEReceiptUploadStatus(true);
                            if (pendingRecordCount > 0) {
                                int ignorTransID = transData.getId();
                                Log.d(EReceiptUtils.TAG, " >> TransactionID (FirstUpload)\t= " + transData.getId() + "\t| TRANS.TYPE : " + transData.getTransType().toString() + "\t| TRANS.STATE : " + transData.getTransState().toString());

                                firstERMTrans = transData;
                                transData = FinancialApplication.getTransDataDbHelper().findFistTransDataWithEReceiptPedingStatus();
                                Log.d(EReceiptUtils.TAG, " >> TransactionID (NextUpload)\t= " + transData.getId() + "\t| TRANS.TYPE : " + transData.getTransType().toString() + "\t| TRANS.STATE : " + transData.getTransState().toString());

                                if (transData != null) {
                                    Log.d(EReceiptUtils.TAG, " >> NEXTUPLOAD >> GET PENDING TRANS FOR NEXT UPLOAD");
                                    nextTransupload += 1;
                                    gotoState(State.SIGNATURE_UPLOAD.toString());
                                    return;
                                } else {
                                    Log.d(EReceiptUtils.TAG, " >> NEXTUPLOAD >> PENDING TRANS WAS MISSING");
                                }
                            }
                        } else {
                            // switch back first transaction and when Nextupload approved by host
                            if (firstERMTrans != null) {
                                Log.d(EReceiptUtils.TAG, " >> switch back from next upload mode");
                                transData = firstERMTrans;
                            } else {
                                Log.d(EReceiptUtils.TAG, " >> Use existing transData for printing");
                            }
                        }
                    } else {
                        // switch back first transaction and when Nextupload was rejected by host
                        if (firstERMTrans != null) {
                            Log.d(EReceiptUtils.TAG, " >> switch back from next upload mode");
                            transData = firstERMTrans;
                        } else {
                            Log.d(EReceiptUtils.TAG, " >> Use existing transData for printing");

                        }
                    }
                } else {
                    Log.d(EReceiptUtils.TAG, " >> Next upload transaction was disabled");
                }


                boolean isEnablePrintAfterTxn = FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE_PRINT_AFTER_TXN);
                int eReceiptNum = FinancialApplication.getSysParam().get(SysParam.NumberParam.VF_ERCM_NO_OF_SLIP);
                int eReceiptNumUnableUpload = FinancialApplication.getSysParam().get(SysParam.NumberParam.VF_ERCM_NO_OF_SLIP_UNABLE_UPLOAD);
                int edcReceiptNum = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_RECEIPT_NUM);
                boolean isSmallAmtPrn = transData.isTxnSmallAmt() && transData.getNumSlipSmallAmt() > 0;
                boolean skipPrintingProc = false;
                if (result.getRet() == TransResult.ERCM_UPLOAD_SESSIONKEY_RENEWAL_REQUIRED) {
                    if (!IsSessionKeyRenewCompleted) {
                        //DialogUtils.showErrMessage(getCurrentContext(),"Upload E-Receipt result (Err-code:51)"  ,"SessionKey renewal proc. required",null, Constants.FAILED_DIALOG_SHOW_TIME + 10);
                        skipPrintingProc = true;
                        origTransData = FinancialApplication.getTransDataDbHelper().findTransData(transData.getId());
                        gotoState(State.SESSIONKEY_RENEWAL.toString());
                        return;
                    }
                }

                if (!skipPrintingProc) {
                    if (result.getRet() == TransResult.SUCC) {
                        // onTransaction Success
                        if (isSmallAmtPrn || (!transData.isTxnSmallAmt() && ((isEnablePrintAfterTxn && eReceiptNum > 0) || edcReceiptNum > 0 ))) {

                            currentNumPrintSlip = transData.getNumberOfErmPrintingCount();

                            if ((FinancialApplication.getSysParam().get(SysParam.NumberParam.VF_ERCM_NO_OF_SLIP) == 1
                                 || FinancialApplication.getSysParam().get(SysParam.NumberParam.VF_ERCM_NO_OF_SLIP) == 2)) {
                                currentNumPrintSlip += 1;
                            }
                            gotoState(State.PRINT_PREVIEW.toString());
                        } else {
                            transData.setNumberOfErmPrintingCount(0);
                            FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                            transEnd(new ActionResult(TransResult.SUCC, null));
                        }
                    } else {
                        // ontrTransaction failed
                        if (eReceiptNumUnableUpload > 0) {
                            currentNumPrintSlip = transData.getNumberOfErmPrintingCount();
                            if ((FinancialApplication.getSysParam().get(SysParam.NumberParam.VF_ERCM_NO_OF_SLIP_UNABLE_UPLOAD) == 1
                                 || FinancialApplication.getSysParam().get(SysParam.NumberParam.VF_ERCM_NO_OF_SLIP_UNABLE_UPLOAD) == 2)) {
                                currentNumPrintSlip +=1;
                            }
                            gotoState(State.PRINT_PREVIEW.toString());
                        } else {
                            transData.setNumberOfErmPrintingCount(0);
                            FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                            transEnd(new ActionResult(TransResult.SUCC, null));
                        }
                    }
                }

                break;
            case SESSIONKEY_RENEWAL:
                // get sessionkey_renewal_result
                sessionKeyRenewalResult = result;
                if (result.getRet() == TransResult.SUCC) {
                    IsSessionKeyRenewCompleted = true;
                    gotoState(State.SIGNATURE_UPLOAD.toString());
                } else {
                    transEnd(result);
                }

                break;
            case PRINT_PREVIEW:
                transData.setNumberOfErmPrintingCount(currentNumPrintSlip);
                FinancialApplication.getTransDataDbHelper().updateTransData(transData);

                gotoState(State.PRINT_TICKET.toString());
                break;
            case PRINT_TICKET:
                transEnd(result);
                break;
            default:
                transEnd(result);
                break;
        }
    }
}
