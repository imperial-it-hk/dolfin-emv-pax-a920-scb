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
package com.pax.pay.trans;

import android.content.Context;
import android.content.DialogInterface;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.abl.utils.EncUtils;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.entity.CTransResult;
import com.pax.eemv.enums.ECvmResult;
import com.pax.eemv.enums.ETransResult;
import com.pax.jemv.clcommon.TransactionPath;
import com.pax.pay.ECR.EcrData;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.emv.EmvSP200;
import com.pax.pay.emv.EmvTags;
import com.pax.pay.emv.EmvTransProcess;
import com.pax.pay.emv.clss.ClssTransProcess;
import com.pax.pay.trans.action.ActionClssAfterReadCardProcess;
import com.pax.pay.trans.action.ActionClssPreProc;
import com.pax.pay.trans.action.ActionClssReadCardProcess;
import com.pax.pay.trans.action.ActionEmvAfterReadCardProcess;
import com.pax.pay.trans.action.ActionEmvReadCardProcess;
import com.pax.pay.trans.action.ActionEnterAmount;
import com.pax.pay.trans.action.ActionEnterPin;
import com.pax.pay.trans.action.ActionInputPassword;
import com.pax.pay.trans.action.ActionInputTransData;
import com.pax.pay.trans.action.ActionOfflineSend;
import com.pax.pay.trans.action.ActionSearchCard;
import com.pax.pay.trans.action.ActionSearchCard.CardInformation;
import com.pax.pay.trans.action.ActionSearchCard.SearchMode;
import com.pax.pay.trans.action.ActionSignature;
import com.pax.pay.trans.action.ActionTcAdvice;
import com.pax.pay.trans.action.ActionTransOnline;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.task.PrintTask;
import com.pax.pay.utils.ToastUtils;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;
import com.pax.view.dialog.CustomAlertDialog;

import java.util.ArrayList;
import java.util.List;

public class RefundTrans extends BaseTrans {

    private String amount;
    private boolean isNeedInputAmount; // is need input amount
    private boolean isFreePin = true;
    private boolean isSupportBypass = true;

    private byte searchCardMode = -1; // search card mode
    private byte orgSearchCardMode = -1; // search card mode
    private byte currentMode;
    private boolean needFallBack = false;

    private int cntTryAgain;
    private CTransResult clssResult;

    public RefundTrans(Context context, byte mode, boolean isFreePin, TransEndListener transListener) {
        super(context, ETransType.REFUND, transListener);
        isNeedInputAmount = true;
        setParam(null, mode, isFreePin);
    }

    public RefundTrans(Context context, String amount, byte mode, TransEndListener transListener) {
        super(context, ETransType.REFUND, transListener);
        isNeedInputAmount = false;
        setParam(amount, mode, isFreePin);
    }

    private void setParam(String amount, byte mode, boolean isFreePin) {
        this.amount = amount;
        this.searchCardMode = mode;
        this.orgSearchCardMode = mode;
        this.isFreePin = isFreePin;
    }

    @Override
    protected void bindStateOnAction() {

        ActionInputPassword inputPasswordAction = new ActionInputPassword(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionInputPassword) action).setParam(getCurrentContext(), 6,
                        getString(R.string.prompt_refund_pwd), null);
            }
        });
        bind(State.INPUT_PWD.toString(), inputPasswordAction);

        // enter amount action
        ActionEnterAmount amountAction = new ActionEnterAmount(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionEnterAmount) action).setParam(getCurrentContext(),
                        getString(R.string.menu_refund), false);
            }
        });
        bind(State.ENTER_AMOUNT.toString(), amountAction, true);

        // search card
        ActionSearchCard searchCardAction = new ActionSearchCard(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionSearchCard) action).setParam(getCurrentContext(), getString(R.string.menu_refund),
                        searchCardMode, transData.getAmount(),
                        null, "", transData);
            }
        });
        bind(State.CHECK_CARD.toString(), searchCardAction, true);

        // input password action
        ActionEnterPin enterPinAction = new ActionEnterPin(new AAction.ActionStartListener() {


            @Override
            public void onStart(AAction action) {
                // if quick pass by pin, set isSupportBypass as false,input password
                if (!isFreePin) {
                    isSupportBypass = false;
                }
                ((ActionEnterPin) action).setParam(getCurrentContext(),
                        getString(R.string.menu_refund), transData.getPan(), isSupportBypass,
                        getString(R.string.prompt_pin),
                        getString(R.string.prompt_no_pin),
                        "-" + transData.getAmount(),
                        null,
                        ActionEnterPin.EEnterPinType.ONLINE_PIN, transData);
            }
        });
        bind(State.ENTER_PIN.toString(), enterPinAction, true);

        //input original trance no
        ActionInputTransData enterTransNoAction = new ActionInputTransData(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                Context context = getCurrentContext();
                ((ActionInputTransData) action).setParam(context, getString(R.string.menu_refund))
                        .setInputLine1(Utils.getString(R.string.prompt_input_orig_refer), ActionInputTransData.EInputType.TRANSID, 12, 1, false, false);
            }
        });
        bind(State.ENTER_TRANSNO.toString(), enterTransNoAction, true);

        // emv read card action
        ActionEmvReadCardProcess emvReadCardAction = new ActionEmvReadCardProcess(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionEmvReadCardProcess) action).setParam(getCurrentContext(), emv, transData);
            }
        });
        bind(State.EMV_READ_CARD.toString(), emvReadCardAction);

        // emv action
        ActionEmvAfterReadCardProcess emvProcessAction = new ActionEmvAfterReadCardProcess(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionEmvAfterReadCardProcess) action).setParam(getCurrentContext(), emv, transData);
            }
        });
        bind(State.EMV_PROC.toString(), emvProcessAction);

        //clss preprocess action
        ActionClssPreProc clssPreProcAction = new ActionClssPreProc(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionClssPreProc) action).setParam(clss, transData);
            }
        });
        bind(State.CLSS_PREPROC.toString(), clssPreProcAction);

        ActionClssReadCardProcess clssReadCardProcessAction = new ActionClssReadCardProcess(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionClssReadCardProcess) action).setParam(getCurrentContext(), clss, transData);
            }
        });
        bind(State.CLSS_READ_CARD.toString(), clssReadCardProcessAction);

        ActionClssAfterReadCardProcess clssProcessAction = new ActionClssAfterReadCardProcess(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionClssAfterReadCardProcess) action).setParam(getCurrentContext(), clss, transData, clssResult);
            }
        });
        bind(State.CLSS_PROC.toString(), clssProcessAction);

        // online action
        ActionTransOnline transOnlineAction = new ActionTransOnline(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionTransOnline) action).setParam(getCurrentContext(), transData);
            }
        });
        bind(State.MAG_ONLINE.toString(), transOnlineAction, true);

        //TC-Advice
        ActionTcAdvice tcAdviceAction = new ActionTcAdvice(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionTcAdvice) action).setParam(getCurrentContext(), transData);
            }
        });
        bind(State.TC_ADVICE_SEND.toString(), tcAdviceAction);

        //offline send
        ActionOfflineSend offlineSendAction = new ActionOfflineSend(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionOfflineSend) action).setParam(getCurrentContext(), transData);
            }
        });
        //even it failed to upload offline, it will continue current transaction, so the 3rd argv is false
        bind(State.OFFLINE_SEND.toString(), offlineSendAction);

        // enter app code
        ActionInputTransData enterApprCodeAction = new ActionInputTransData(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionInputTransData) action).setParam(getCurrentContext(), getString(R.string.trans_sale))
                        .setInputTransIDLine(getString(R.string.prompt_input_appr_code), ActionInputTransData.EInputType.TRANSID, 6,0);
            }
        });
        bind(State.ENTER_APPR_CODE.toString(), enterApprCodeAction, true);

        // signature action
        ActionSignature signatureAction = new ActionSignature(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionSignature) action).setParam(getCurrentContext(), transData.getAmount(), !Component.isAllowSignatureUpload(transData));
            }
        });
        bind(State.SIGNATURE.toString(), signatureAction);

        //print preview action
        PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(RefundTrans.this, State.PRINT.toString()));
        bind(State.PRINT.toString(), printTask);

        // perform the first action
        if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.OTHTC_VERIFY)) {
            gotoState(State.INPUT_PWD.toString());
        } else if (isNeedInputAmount) {
            if (searchCardMode == -1) {
                searchCardMode = Component.getCardReadMode(ETransType.REFUND);
                orgSearchCardMode = searchCardMode;
                this.transType = ETransType.REFUND;
            }
            gotoState(State.ENTER_AMOUNT.toString());
        } else {
            cntTryAgain = 0;
            needFallBack = false;
            if (searchCardMode == -1) {
                searchCardMode = Component.getCardReadMode(ETransType.REFUND);
                orgSearchCardMode = searchCardMode;
                this.transType = ETransType.REFUND;
            }
            transData.setAmount(this.amount);
            if ((searchCardMode & SearchMode.WAVE) == SearchMode.WAVE) {
                gotoState(State.CLSS_PREPROC.toString());
            } else {
                gotoState(State.CHECK_CARD.toString());
            }
        }
//        showMsgNotAllowed();
    }

    private enum State {
        INPUT_PWD,
        ENTER_AMOUNT,
        CHECK_CARD,
        ENTER_PIN,
        ENTER_TRANSNO,
        MAG_ONLINE,
        EMV_READ_CARD,
        EMV_PROC,
        TC_ADVICE_SEND,
        OFFLINE_SEND,
        ENTER_APPR_CODE,
        CLSS_PREPROC,
        CLSS_READ_CARD,
        CLSS_PROC,
        SIGNATURE,
        PRINT,
    }

    @Override
    public void gotoState(String state) {
        if (state.equals(State.INPUT_PWD.toString())) {
            EcrData.instance.isOnHomeScreen = false;
        }
        super.gotoState(state);
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {
        int ret = result.getRet();
        State state = State.valueOf(currentState);

        switch (state) {
            case INPUT_PWD:
                if (result.getRet() != TransResult.SUCC) {
                    EcrData.instance.isOnHomeScreen = true;
                    transEnd(result);
                }
                else {
                    cntTryAgain = 0;
                    needFallBack = false;
                    onInputPwd(result);
                }
                break;
            case ENTER_AMOUNT:
                cntTryAgain = 0;
                needFallBack = false;
                onEnterAmount(result);
                break;
            case CLSS_PREPROC:
                if (ret != TransResult.SUCC) {
                    searchCardMode &= 0x03;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // do nothing
                }
                gotoState(State.CHECK_CARD.toString());
                break;
            case CHECK_CARD: // check card
                onCheckCard(result);
                break;
            case ENTER_PIN: // enter pin
                onEnterPin(result);
                break;
            case ENTER_TRANSNO:
                onEnterRefNo(result);
                break;
            case EMV_READ_CARD:
                onEmvReadCard(result);
                break;
            case EMV_PROC: // emv
                String pan = transData.getPan();
                byte[] f55Dup = EmvTags.getF55(emv, transType, true,pan);
                if (f55Dup.length > 0) {
                    TransData dupTransData = FinancialApplication.getTransDataDbHelper().findFirstDupRecord(transData.getAcquirer());
                    if (dupTransData != null) {
                        dupTransData.setDupIccData(Utils.bcd2Str(f55Dup));
                        FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);
                    }
                }

                if (ret == TransResult.ERR_REFERRAL_CALL_ISSUER) {
                    gotoState(State.ENTER_APPR_CODE.toString());
                    return;
                } else if (ret != TransResult.SUCC && ret != TransResult.ERR_NEED_MAG_ONLINE) {
                    transEnd(result);
                    return;
                }

                //get trans result
                CTransResult transResult = (CTransResult) result.getData();
                onEmvProc(transResult.getTransResult());

                if (ret == TransResult.ERR_NEED_MAG_ONLINE) {
                    gotoState(State.MAG_ONLINE.toString());
                    return;
                }

                if (transResult.getTransResult() != ETransResult.ONLINE_APPROVED
                        && transResult.getTransResult() != ETransResult.OFFLINE_APPROVED) {
                    return;
                }

//                ECRProcReturn(null, new ActionResult(result.getRet(), null));
                if (transResult.getTransResult() == ETransResult.ONLINE_APPROVED) {
                    processTcAdvice();
                } else if (transResult.getTransResult() == ETransResult.OFFLINE_APPROVED) {
//                    toSignOrPrint(); //Comment as Refund offline not support
                    transEnd(new ActionResult(TransResult.ERR_OFFLINE_UNSUPPORTED, null));
                }

                break;
            case MAG_ONLINE: // after online
                if (ret != TransResult.SUCC) {
                    transEnd(result);
                    return;
                }
                processTcAdvice();
                break;
            case TC_ADVICE_SEND:
                if (result.getRet() == TransResult.SUCC) {
                    checkOfflineTrans();
                } else {
                    toSignOrPrint();
                }
                break;
            case OFFLINE_SEND:
                toSignOrPrint();
                break;
            case ENTER_APPR_CODE:
                afterEnterApprCode(result.getData().toString());
                break;
            case CLSS_READ_CARD:
            case CLSS_PROC:
                if (ret != TransResult.SUCC) {
                    transEnd(result);
                    return;
                }
                clssResult = (CTransResult) result.getData();
                if (clssResult != null) {
                    afterClssProcess(clssResult);
                    if (clssResult.getTransResult() == ETransResult.CLSS_OC_APPROVED
                            || clssResult.getTransResult() == ETransResult.OFFLINE_APPROVED
                            || clssResult.getTransResult() == ETransResult.ONLINE_APPROVED) {
//                        ECRProcReturn(null, new ActionResult(result.getRet(), null));
                        if (transData.isOnlineTrans()) {
                            checkOfflineTrans();
                        } else {
//                            toSignOrPrint(); //Comment as Refund offline not support
                            transEnd(new ActionResult(TransResult.ERR_OFFLINE_UNSUPPORTED, null));
                        }
                    }
                } else {
                    transEnd(result);
                }
                break;
            case SIGNATURE:
                onSignature(result);
                break;
            case PRINT:
                if (result.getRet() == TransResult.SUCC) {
                    // end trans
                    transEnd(result);
                } else {
                    dispResult(transType.getTransName(), result, null);
                    gotoState(State.PRINT.toString());
                }
                break;
            default:
                transEnd(result);
                break;
        }
    }

    private void onInputPwd(ActionResult result) {
        String data = EncUtils.sha1((String) result.getData());
        if (!data.equals(FinancialApplication.getSysParam().get(SysParam.StringParam.SEC_REFUND_PWD))) {
            EcrData.instance.isOnHomeScreen = true;
            transEnd(new ActionResult(TransResult.ERR_PASSWORD, null));
            return;
        }
        gotoState(State.ENTER_AMOUNT.toString());
//        gotoState(State.CHECK_CARD.toString());
    }

    private void onEnterAmount(ActionResult result) {
        transData.setAmount(result.getData().toString());
        if ((searchCardMode & SearchMode.WAVE) == SearchMode.WAVE) {
            gotoState(State.CLSS_PREPROC.toString());
        } else {
            gotoState(State.CHECK_CARD.toString());
        }
    }

    private void onCheckCard(ActionResult result) {
        CardInformation cardInfo = (CardInformation) result.getData();
        if(cardInfo.getSearchMode() != SearchMode.RABBIT && cardInfo.getSearchMode() != SearchMode.SP200) {
            saveCardInfo(cardInfo, transData);
        }

        transData.setTransType(ETransType.REFUND);
        if (needFallBack) {
            transData.setEnterMode(TransData.EnterMode.FALLBACK);
        }

        // manual enter card NO
        if (transData.getAmount() == null || transData.getAmount().isEmpty()) {
            gotoState(State.ENTER_AMOUNT.toString());
            return;
        }

        boolean isSwipeKeyin = false;
        currentMode = cardInfo.getSearchMode();
        switch (currentMode) {
            case SearchMode.INSERT:
                needRemoveCard = true;
                // EMV process
                gotoState(State.EMV_READ_CARD.toString());
                return;
            case SearchMode.SP200:
                needRemoveCard = true;
                saveSP200Info(cardInfo.getEmvSP200());
                handleResultSP200(cardInfo.getEmvSP200());
                return;
            case SearchMode.WAVE:
                needRemoveCard = true;
                gotoState(State.CLSS_READ_CARD.toString());
                break;
            case SearchMode.SWIPE:
            case SearchMode.KEYIN:
                isSwipeKeyin = true;
                break;
        }

        if (isSwipeKeyin && checkSupportedRefund()) {
            goCheckPin();
        }
    }

    private boolean checkSupportedRefund() {
        if (transData.getIssuer() != null) {
            if (!transData.getIssuer().isAllowRefund()) {
                transEnd(new ActionResult(TransResult.ERR_NOT_SUPPORT_TRANS, null));
                return false;
            }
        } else {
            transEnd(new ActionResult(TransResult.ERR_CARD_UNSUPPORTED, null));
            return false;
        }
        return true;
    }

    private void handleResultSP200(EmvSP200 emvSP200) {
        if (emvSP200 != null) {
            switch (emvSP200.getiResult()) {
                case 1://CLSS_APPROVE (offline)
                    if (!checkSupportedRefund()) {
                        return;
                    }
                    transData.setOnlineTrans(false);
                    transData.setOrigAuthCode(Utils.getString(R.string.response_Y1_str));
                    transData.setAuthCode(Utils.getString(R.string.response_Y1_str));
                    transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));// Fixed EDCBBLAND-235
                    transData.setTraceNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO));
                    Component.saveOfflineTransNormalSale(transData);
                    toSignOrPrint();
                    return;
                case 3://CLSS_TRY_ANOTHER_INTERFACE
                    ToastUtils.showMessage("Please use contact");
                    transEnd(new ActionResult(TransResult.ERR_ABORTED, null));
                    return;
                case 0://CLSS_DECLINED
                case 4://CLSS_END_APPLICATION
                    Device.beepErr();
                    showMsgDialog(getCurrentContext(), getString(R.string.dialog_clss_declined) + ", " + getString(R.string.dialog_clss_try_contact));
                    transEnd(new ActionResult(TransResult.ERR_ABORTED, null));
                    return;
                default://CLSS_ONLINE_REQUEST
                    if (checkSupportedRefund()) {
                        if (!transData.isPinFree() && transData.isSignFree()) {
                            gotoState(State.ENTER_PIN.toString());
                        } else {
                            beforeGoMagOnline();
                        }
                    }
                    break;
            }
        }
    }

    private void goCheckPin() {
        // Include TEST MODE
        Acquirer acq = FinancialApplication.getAcqManager().getCurAcq();
        if (acq.isEnableUpi() && !acq.isTestMode() && transData.getIssuer()!=null &&
                (Constants.ISSUER_UP.equals(transData.getIssuer().getName()) ||
                        Constants.ISSUER_TBA.equals(transData.getIssuer().getName())))
        {
            gotoState(State.ENTER_PIN.toString());
        } else {
            if (!isFreePin) {
                gotoState(State.ENTER_PIN.toString());
            } else {
                beforeGoMagOnline();
            }
        }
    }

    private void onEnterPin(ActionResult result) {
        String pinBlock = (String) result.getData();
        transData.setPin(pinBlock);
        if (pinBlock != null && !pinBlock.isEmpty()) {
            transData.setPinVerifyMsg(true);
            transData.setHasPin(true);
        }
        // online
        beforeGoMagOnline();
    }

    private void beforeGoMagOnline() {
        /*  //Remove for KBANK
        if (Constants.ACQ_UP.equals(transData.getAcquirer().getName())) {
            gotoState(State.ENTER_TRANSNO.toString());
            return;
        }*/
        gotoState(State.MAG_ONLINE.toString());
    }

    private void onEnterRefNo(ActionResult result) {
        String refNo = (String) result.getData();
        transData.setRefNo(refNo);
        gotoState(State.MAG_ONLINE.toString());
    }

    // need electronic signature or send
    private void toSignOrPrint() {
        if (transData.isTxnSmallAmt() && transData.getNumSlipSmallAmt() == 0) {//EDCBBLAND-426 Support small amount
            FinancialApplication.getTransDataDbHelper().updateTransData(transData);
//            transEnd(new ActionResult(TransResult.SUCC, null));
//            return;
        }
        // if(transData.getIssuer().getName().equals(EKernelType.JCB.name()) && !transData.isOnlineTrans() && transData.isHasPin()){
        if(transData.isPinVerifyMsg() || (!transData.isOnlineTrans() && transData.isHasPin())){
            transData.setSignFree(true);
            gotoState(State.PRINT.toString());
        }else{
            if ((currentMode == SearchMode.WAVE || currentMode == SearchMode.SP200) && transData.isSignFree()) {// signature free
                gotoState(SaleTrans.State.PRINT.toString());
            } else {
                transData.setSignFree(false);
                boolean eSignature = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_E_SIGNATURE);
                if (eSignature && !transData.isTxnSmallAmt()) {
                    gotoState(SaleTrans.State.SIGNATURE.toString());
                }else {
                    gotoState(SaleTrans.State.PRINT.toString()); // Skip SIGNATURE process
                }
            }
        }
        FinancialApplication.getTransDataDbHelper().updateTransData(transData);
    }

    private void onEmvReadCard(ActionResult result) {
        int ret = result.getRet();
        if(ret == TransResult.ICC_TRY_AGAIN) {
            cntTryAgain++;
            if(cntTryAgain == 3) {
                needFallBack = true;
                searchCardMode &= 0x01;
                showFallbackMsgDialog(getCurrentContext());
            } else {
                showTryAgainDialog(getCurrentContext());
            }
            return;
        } else if (ret == TransResult.NEED_FALL_BACK) {
            needFallBack = true;
            searchCardMode &= 0x01;
            gotoState(State.CHECK_CARD.toString());
            return;
        } else if (ret != TransResult.SUCC) {
            transEnd(result);
            return;
        }

        gotoState(State.EMV_PROC.toString());
    }

    private void onEmvProc(ETransResult transResult) {
        EmvTransProcess.emvTransResultProcess(transResult, emv, transData);
        if (transResult == ETransResult.ONLINE_APPROVED) {
            //nothing (handle printing outside)
        } else if (transResult == ETransResult.OFFLINE_APPROVED) {
//            transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));// Fixed EDCBBLAND-235
//            transData.setTraceNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO));
//            Component.saveOfflineTransNormalSale(transData);
            //handle printing outside
        } else if (transResult == ETransResult.ARQC || transResult == ETransResult.SIMPLE_FLOW_END) { // request online/simplify process
            // enter pin
            gotoState(State.ENTER_PIN.toString());
        } else if (transResult == ETransResult.ONLINE_DENIED) { // online denied
            // transaction end
            transEnd(new ActionResult(TransResult.ERR_HOST_REJECT, null));
        } else if (transResult == ETransResult.ONLINE_CARD_DENIED) {// platform approve card denied
            transEnd(new ActionResult(TransResult.ERR_CARD_DENIED, null));
        } else if (transResult == ETransResult.ABORT_TERMINATED ||
                transResult == ETransResult.OFFLINE_DENIED) { // emv interrupt
            Device.beepErr();
            transEnd(new ActionResult(TransResult.ERR_ABORTED, null));
        }
    }

    private void afterClssProcess(CTransResult transResult) {
        if (!checkSupportedRefund()) {
            return;
        }

        if(transResult.getTransResult()==ETransResult.CLSS_OC_REFER_CONSUMER_DEVICE || transResult.getTransResult() == ETransResult.CLSS_OC_TRY_AGAIN){
            gotoState(State.CLSS_PREPROC.toString());
            return;
        }

        int transPath = transData.getClssTypeMode();
        if ((transPath == TransactionPath.CLSS_MC_MAG || transPath == TransactionPath.CLSS_MC_MCHIP)
                && transResult.getTransResult() == ETransResult.CLSS_OC_DECLINED) {
            clss.setListener(null);// no memory leak
            //For Mastercard, If transaction is declined, need to prompt msg to perform a contact trans.
            searchCardMode = (byte)(SearchMode.INSERT | SearchMode.SWIPE | SearchMode.KEYIN);//prompt to perform contact transaction.
            showMsgDialog(getCurrentContext(), getString(R.string.dialog_clss_declined) + ", " + getString(R.string.dialog_clss_try_contact));
            return;
        }

        // 流水号增加
//        Component.incStanNo(transData);
        // 设置交易结果
        transData.setEmvResult(transResult.getTransResult());
        if (transResult.getTransResult() == ETransResult.ABORT_TERMINATED ||
                transResult.getTransResult() == ETransResult.CLSS_OC_DECLINED) { // emv interrupt
            Device.beepErr();
            transEnd(new ActionResult(TransResult.ERR_ABORTED, null));
            return;
        }

        if (transResult.getTransResult() == ETransResult.CLSS_OC_TRY_ANOTHER_INTERFACE) {
            //todo: use contact
            //此处应当结束整个交易，重新发起交易时，客户自然会使用接触式
            ToastUtils.showMessage("Please use contact");
            transEnd(new ActionResult(TransResult.ERR_ABORTED, null));
            return;
        }

        ClssTransProcess.clssTransResultProcess(transResult, clss, transData);
        transData.setPinVerifyMsg(transResult.getCvmResult() == ECvmResult.ONLINE_PIN || transResult.getCvmResult() == ECvmResult.ONLINE_PIN_SIG);

        if ((transResult.getCvmResult() == ECvmResult.SIG || !Component.isSignatureFree(transData, transResult)) && !transData.isPinVerifyMsg()) { // AET-283
            //FIXME don't upload the signature
            //do signature after online
            transData.setSignFree(false);
        } else {
            transData.setSignFree(true);
        }

        if (transResult.getTransResult() == ETransResult.CLSS_OC_APPROVED || transResult.getTransResult() == ETransResult.OFFLINE_APPROVED || transResult.getTransResult() == ETransResult.ONLINE_APPROVED) {
            transData.setOnlineTrans(transResult.getTransResult() == ETransResult.ONLINE_APPROVED);
            //handle printing outside
            return;
        }
        // ETransResult.CLSS_OC_ONLINE_REQUEST, online is handled in the module

        gotoState(State.CLSS_PROC.toString());
    }

    private void processTcAdvice() {
        if (transData.getAcquirer().isEmvTcAdvice()) {
            gotoState(State.TC_ADVICE_SEND.toString());
        } else {
            checkOfflineTrans();
        }
    }

    private void checkOfflineTrans() {
        //get offline trans data list
        List<Acquirer> acqs = new ArrayList<>();
        List<TransData.OfflineStatus> excludes = new ArrayList<>();
        acqs.add(transData.getAcquirer());
        excludes.add(TransData.OfflineStatus.OFFLINE_SENT);
        excludes.add(TransData.OfflineStatus.OFFLINE_VOIDED);
        excludes.add(TransData.OfflineStatus.OFFLINE_ADJUSTED);
        List<TransData> offlineTransList = FinancialApplication.getTransDataDbHelper().findAllOfflineTransData(acqs, excludes);

        if (!offlineTransList.isEmpty() && offlineTransList.get(0).getId() != transData.getId()) {
            //offline send
            gotoState(State.OFFLINE_SEND.toString());
        } else {
            toSignOrPrint();
        }
    }

    private void onSignature(ActionResult result) {
        // save signature
        byte[] signData = (byte[]) result.getData();
        byte[] signPath = (byte[]) result.getData1();

        if (signData != null && signData.length > 0/* &&
                signPath != null && signPath.length > 0*/) {
            transData.setSignData(signData);
            transData.setSignPath(signPath);
            // update trans data，save signature
            FinancialApplication.getTransDataDbHelper().updateTransData(transData);
        }
        // if terminal not support electronic signature, user do not make signature or signature time out, print preview
        gotoState(State.PRINT.toString());
    }

    private void showTryAgainDialog(final Context context) {

        final CustomAlertDialog dialog = new CustomAlertDialog(context, CustomAlertDialog.NORMAL_TYPE, 5000);
        dialog.setCancelClickListener(new CustomAlertDialog.OnCustomClickListener() {
            @Override
            public void onClick(CustomAlertDialog alertDialog) {
                dialog.dismiss();
                transEnd(new ActionResult(TransResult.ERR_ABORTED, null));
            }
        });
        dialog.setConfirmClickListener(new CustomAlertDialog.OnCustomClickListener() {
            @Override
            public void onClick(CustomAlertDialog alertDialog) {
                dialog.dismiss();
                //gotoState(State.CHECK_CARD.toString());
            }
        });
        dialog.setTimeout(Constants.FAILED_DIALOG_SHOW_TIME);
        dialog.show();
        dialog.setNormalText( getString(R.string.prompt_try_again) + getString(R.string.prompt_insert_card));
        dialog.showCancelButton(true);
        dialog.showConfirmButton(true);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface arg0) {
                gotoState(State.CHECK_CARD.toString());
            }
        });
    }

    private void showFallbackMsgDialog(final Context context) {

        final CustomAlertDialog dialog = new CustomAlertDialog(context, CustomAlertDialog.NORMAL_TYPE);
        dialog.setCancelClickListener(new CustomAlertDialog.OnCustomClickListener() {
            @Override
            public void onClick(CustomAlertDialog alertDialog) {
                dialog.dismiss();
                transEnd(new ActionResult(TransResult.ERR_ABORTED, null));
            }
        });
        dialog.setConfirmClickListener(new CustomAlertDialog.OnCustomClickListener() {
            @Override
            public void onClick(CustomAlertDialog alertDialog) {
                dialog.dismiss();
                //gotoState(State.CHECK_CARD.toString());
            }
        });
        dialog.setTimeout(Constants.FAILED_DIALOG_SHOW_TIME);
        dialog.show();
        dialog.setNormalText(getString(R.string.prompt_fallback) + getString(R.string.prompt_swipe_card));
        dialog.showCancelButton(true);
        dialog.showConfirmButton(true);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface arg0) {
                gotoState(State.CHECK_CARD.toString());
            }
        });
    }

    private void afterEnterApprCode(String appCode) {
        transData.setReferralStatus(TransData.ReferralStatus.PENDING);
        transData.setAuthCode(appCode);
        transData.setRefNo(null);
        transData.setResponseCode(FinancialApplication.getRspCode().parse("00"));
        FinancialApplication.getTransDataDbHelper().insertTransData(transData);

        if (transData.getEnterMode() == TransData.EnterMode.INSERT) {
            EmvTransProcess.emvTransResultProcess(ETransResult.ONLINE_DENIED, emv, transData);
        }

        Component.incTraceNo(transData);//reserved trace no. for referred transaction before settlement
        Component.incTraceNo(transData);//trace no. of next transaction
        toSignOrPrint();
    }

    private void showMsgDialog(final Context context, final String msg) {

        final CustomAlertDialog dialog = new CustomAlertDialog(context, CustomAlertDialog.NORMAL_TYPE);
        dialog.setCancelClickListener(new CustomAlertDialog.OnCustomClickListener() {
            @Override
            public void onClick(CustomAlertDialog alertDialog) {
                dialog.dismiss();
                transEnd(new ActionResult(TransResult.ERR_ABORTED, null));
            }
        });
        dialog.setConfirmClickListener(new CustomAlertDialog.OnCustomClickListener() {
            @Override
            public void onClick(CustomAlertDialog alertDialog) {
                dialog.dismiss();
                //gotoState(State.CHECK_CARD.toString());
            }
        });
        dialog.setTimeout(Constants.FAILED_DIALOG_SHOW_TIME);
        dialog.show();
        dialog.setNormalText(msg);
        dialog.showCancelButton(true);
        dialog.showConfirmButton(true);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface arg0) {
                gotoState(State.CHECK_CARD.toString());
            }
        });
    }

    @Override
    protected void transEnd(final ActionResult result) {
        searchCardMode = orgSearchCardMode;
        emv.setListener(null);//no memory leak
        clss.setListener(null);//no memory leak
        super.transEnd(result);
    }
}
