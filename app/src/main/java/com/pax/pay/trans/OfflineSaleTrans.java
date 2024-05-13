/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-1-10
 * Module Author: lixc
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
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.entity.CTransResult;
import com.pax.eemv.enums.ETransResult;
import com.pax.pay.ECR.EcrData;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.Issuer;
import com.pax.pay.constant.Constants;
import com.pax.pay.db.TransDataDb;
import com.pax.pay.emv.EmvTransProcess;
import com.pax.pay.trans.action.ActionEmvAfterReadCardProcess;
import com.pax.pay.trans.action.ActionEmvReadCardProcess;
import com.pax.pay.trans.action.ActionEnterAmount;
import com.pax.pay.trans.action.ActionEnterAuthCode;
import com.pax.pay.trans.action.ActionEnterPin;
import com.pax.pay.trans.action.ActionInputPassword;
import com.pax.pay.trans.action.ActionSearchCard;
import com.pax.pay.trans.action.ActionSearchCard.SearchMode;
import com.pax.pay.trans.action.ActionSignature;
import com.pax.pay.trans.action.ActionTransOnline;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.task.PrintTask;
import com.pax.settings.SysParam;
import com.pax.view.dialog.CustomAlertDialog;

public class OfflineSaleTrans extends BaseTrans {
    private String amount;
    private boolean isNeedInputAmount = true; // is need input amount
    private boolean isFreePin = true;
    private boolean isSupportBypass = true;

    private byte searchCardMode = -1;
    private byte orgSearchCardMode = -1; // search card mode
    private boolean needFallBack = false;
    private byte currentMode;

    private int cntTryAgain;

    public OfflineSaleTrans(Context context, byte mode, boolean isFreePin, TransEndListener transListener) {
        super(context, ETransType.OFFLINE_TRANS_SEND, transListener);
        isNeedInputAmount = true;
        setParam(null, mode, isFreePin);
    }

    public OfflineSaleTrans(Context context, String amount, byte mode, TransEndListener transListener) {
        super(context, ETransType.OFFLINE_TRANS_SEND, transListener);
        isNeedInputAmount = false;
        setParam(amount, mode, isFreePin);
    }

    private void setParam(String amount, byte mode, boolean isFreePin) {
        this.amount = amount;
        this.searchCardMode = mode;
        this.orgSearchCardMode = mode;
        this.isFreePin = isFreePin;
    }

    protected void ErmErrorExceedCheck() {
        if (TransDataDb.getInstance().findCountTransDataWithEReceiptUploadStatus(true) >= 30) {
            transEnd(new ActionResult(TransResult.ERCM_MAXIMUM_TRANS_EXCEED_ERROR, null));
            return;
        }
    }

    @Override
    protected void bindStateOnAction() {

        ActionInputPassword inputPasswordAction = new ActionInputPassword(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionInputPassword) action).setParam(getCurrentContext(), 6,
                        getString(R.string.prompt_offline_pwd), null);
            }
        });
        bind(State.INPUT_PWD.toString(), inputPasswordAction);

        // input amount and tip amount action
        ActionEnterAmount amountAction = new ActionEnterAmount(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
//                float percent = transData.getIssuer().getAdjustPercent();
                ((ActionEnterAmount) action).setParam(getCurrentContext(),
                        getString(R.string.menu_offline), false);
            }
        });
        bind(State.ENTER_AMOUNT.toString(), amountAction, true);

        // search card action
        ActionSearchCard searchCardAction = new ActionSearchCard(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionSearchCard) action).setParam(getCurrentContext(), getString(R.string.menu_offline), searchCardMode, transData.getAmount(),
                        null, getString(R.string.prompt_insert_swipe_card), transData);
            }
        });

        bind(State.CHECK_CARD.toString(), searchCardAction, true);

        //enter auth code action
        ActionEnterAuthCode enterAuthCodeAction = new ActionEnterAuthCode(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionEnterAuthCode) action).setParam(getCurrentContext(),
                        getString(R.string.menu_offline),
                        getString(R.string.prompt_auth_code),
                        transData.getAmount());
            }
        });
        bind(State.ENTER_AUTH_CODE.toString(), enterAuthCodeAction, true);

        // enter pin action
        ActionEnterPin enterPinAction = new ActionEnterPin(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                // if quick pass by pin, set isSupportBypass as false,input password
                if (!isFreePin) {
                    isSupportBypass = false;
                }
                ((ActionEnterPin) action).setParam(getCurrentContext(),
                        getString(R.string.menu_offline), transData.getPan(), isSupportBypass,
                        getString(R.string.prompt_pin),
                        getString(R.string.prompt_no_pin),
                        transData.getAmount(),
                        transData.getTipAmount(),
                        ActionEnterPin.EEnterPinType.ONLINE_PIN, transData);
            }
        });
        bind(State.ENTER_PIN.toString(), enterPinAction, true);

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

        // online action
        ActionTransOnline transOnlineAction = new ActionTransOnline(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionTransOnline) action).setParam(getCurrentContext(), transData);
            }
        });

        bind(State.MAG_ONLINE.toString(), transOnlineAction);

        // signature action
        ActionSignature signatureAction = new ActionSignature(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionSignature) action).setParam(getCurrentContext(), transData.getAmount(), !Component.isAllowSignatureUpload(transData));
            }
        });
        bind(State.SIGNATURE.toString(), signatureAction);

        //print preview action
        PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(OfflineSaleTrans.this, State.PRINT.toString()));
        bind(State.PRINT.toString(), printTask);

        // ERM Maximum Exceed Transaction check
        ErmErrorExceedCheck();

        // perform the first action
        if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.OTHTC_VERIFY)) {
            gotoState(State.INPUT_PWD.toString());
        } else if (isNeedInputAmount) {
            if (searchCardMode == -1) {
                searchCardMode = Component.getCardReadMode(ETransType.OFFLINE_TRANS_SEND);
                orgSearchCardMode = searchCardMode;
                this.transType = ETransType.OFFLINE_TRANS_SEND;
            }
            gotoState(State.ENTER_AMOUNT.toString());
        } else {
            cntTryAgain = 0;
            needFallBack = false;
            if (searchCardMode == -1) {
                searchCardMode = Component.getCardReadMode(ETransType.OFFLINE_TRANS_SEND);
                orgSearchCardMode = searchCardMode;
                this.transType = ETransType.OFFLINE_TRANS_SEND;
            }
            transData.setAmount(this.amount);
            gotoState(State.CHECK_CARD.toString());
        }
    }

    enum State {
        INPUT_PWD,
        ENTER_AMOUNT,
        CHECK_CARD,
        ENTER_AUTH_CODE,
        ENTER_PIN,
        EMV_READ_CARD,
        EMV_PROC,
        MAG_ONLINE,
        SIGNATURE,
        PRINT
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
            case ENTER_AUTH_CODE:
                onEnterAuthCode(result);
                break;
            case CHECK_CARD:
                onCheckCard(result);
                break;
            case ENTER_PIN:
                onEnterPin(result);
                break;
            case EMV_READ_CARD:
                onEmvReadCard(result);
                break;
            case EMV_PROC:
                if (ret != TransResult.SUCC) {
                    transEnd(result);
                    return;
                }
                //get trans result
                CTransResult transResult = (CTransResult) result.getData();

                afterEMVProcess(transResult.getTransResult());

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
        if (!data.equals(FinancialApplication.getSysParam().get(SysParam.StringParam.SEC_VOID_PWD))) {
            EcrData.instance.isOnHomeScreen = true;
            transEnd(new ActionResult(TransResult.ERR_PASSWORD, null));
            return;
        }

        if (isNeedInputAmount) {
            if (searchCardMode == -1) {
                searchCardMode = Component.getCardReadMode(ETransType.OFFLINE_TRANS_SEND);
                orgSearchCardMode = searchCardMode;
                this.transType = ETransType.OFFLINE_TRANS_SEND;
            }
            gotoState(State.ENTER_AMOUNT.toString());
        } else {
            cntTryAgain = 0;
            needFallBack = false;
            if (searchCardMode == -1) {
                searchCardMode = Component.getCardReadMode(ETransType.OFFLINE_TRANS_SEND);
                orgSearchCardMode = searchCardMode;
                this.transType = ETransType.OFFLINE_TRANS_SEND;
            }
            transData.setAmount(this.amount);
            gotoState(State.CHECK_CARD.toString());
        }
    }

    private void onEnterAmount(ActionResult result) {
        //set total amount
        transData.setAmount(result.getData().toString());
        //set tip amount
        transData.setTipAmount(result.getData1().toString());
        //read card
        gotoState(State.CHECK_CARD.toString());
    }

    private void onEnterAuthCode(ActionResult result) {
        //get auth code
        String authCode = (String) result.getData();
        //set auth code
        transData.setOrigAuthCode(authCode);
        transData.setAuthCode(authCode);

        if (currentMode == SearchMode.INSERT) {
            gotoState(State.EMV_PROC.toString());
        } else {
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
                    // save trans data
                    Component.saveOfflineTransNormalSale(transData);
                    // signature
                    toSignOrPrint();
                }
            }
        }
    }

    private void onCheckCard(ActionResult result) {
        ActionSearchCard.CardInformation cardInfo = (ActionSearchCard.CardInformation) result.getData();
        saveCardInfo(cardInfo, transData);

        transData.setTransType(ETransType.OFFLINE_TRANS_SEND);

        boolean isSwipeKeyin = false;
        currentMode = cardInfo.getSearchMode();
        switch (currentMode) {
            case SearchMode.INSERT:
                if (needFallBack) {
                    transData.setEnterMode(TransData.EnterMode.FALLBACK);
                }
                needRemoveCard = true;
               gotoState(State.EMV_READ_CARD.toString());
                return;
            case SearchMode.SWIPE:
                isSwipeKeyin = true;
                break;
            case SearchMode.KEYIN:
                isSwipeKeyin = true;
                break;
        }

        if (isSwipeKeyin && isSupportedOfflineTrans()) {
            //enter auth code
            gotoState(State.ENTER_AUTH_CODE.toString());
        }
    }

    private boolean isSupportedOfflineTrans() {
        Issuer issuer = transData.getIssuer();
        if (issuer == null) {
            transEnd(new ActionResult(TransResult.ERR_CARD_UNSUPPORTED, null));
            return false;
        }
        if (!issuer.isEnableOffline()) {
            transEnd(new ActionResult(TransResult.ERR_NOT_SUPPORT_TRANS, null));
            return false;
        }
        return true;
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
            gotoState(SaleTrans.State.CHECK_CARD.toString());
            return;
        } else if (ret != TransResult.SUCC) {
            transEnd(result);
            return;
        }

        if (isSupportedOfflineTrans()) {
            //enter auth code
            gotoState(State.ENTER_AUTH_CODE.toString());
        }
    }

    private void afterEMVProcess(ETransResult transResult) {
        EmvTransProcess.emvTransResultProcess(transResult, emv, transData);
        if (transResult == ETransResult.OFFLINE_APPROVED) {
            // save trans data
            Component.saveOfflineTransNormalSale(transData);
            toSignOrPrint();
        }
    }

    private void onEnterPin(ActionResult result) {
        String pinBlock = (String) result.getData();
        transData.setPin(pinBlock);
        if (pinBlock != null && !pinBlock.isEmpty()) {
            transData.setHasPin(true);
        }
        // save trans data
        Component.saveOfflineTransNormalSale(transData);

//        ECRProcReturn(null, new ActionResult(result.getRet(), null));

        // signature
        toSignOrPrint();
//        gotoState(State.SIGNATURE.toString());
    }

    // need electronic signature or send
    private void toSignOrPrint() {
        if (transData.isTxnSmallAmt() && transData.getNumSlipSmallAmt() == 0) {//EDCBBLAND-426 Support small amount
            FinancialApplication.getTransDataDbHelper().updateTransData(transData);
//            transEnd(new ActionResult(TransResult.SUCC, null));
//            return;
        }

        if(transData.isPinVerifyMsg() || (!transData.isOnlineTrans() && transData.isHasPin())){
            transData.setSignFree(true);
            gotoState(State.PRINT.toString());
        }else{
            transData.setSignFree(false);
            boolean eSignature = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_E_SIGNATURE);
            if (eSignature && !transData.isTxnSmallAmt()) {
                gotoState(State.SIGNATURE.toString());
            }else {
                //gotoState(State.SIGNATURE.toString());
                gotoState(State.PRINT.toString()); // Skip SIGNATURE process
            }
        }
        FinancialApplication.getTransDataDbHelper().updateTransData(transData);
    }

    private void onSignature(ActionResult result) {
        // save signature data
        byte[] signData = (byte[]) result.getData();
        byte[] signPath = (byte[]) result.getData1();

        if (signData != null && signData.length > 0/* &&
                signPath != null && signPath.length > 0*/) {
            transData.setSignData(signData);
            transData.setSignPath(signPath);
            // update trans data，save signature
            FinancialApplication.getTransDataDbHelper().updateTransData(transData);
        }
        // if terminal does not support signature ,card holder does not sign or time out，print preview directly.
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
        dialog.setNormalText(getString(R.string.prompt_try_again) + getString(R.string.prompt_insert_card));
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

    private void showMsgNotAllowed(){
        FinancialApplication.getApp().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final CustomAlertDialog dialog = new CustomAlertDialog(context, CustomAlertDialog.NORMAL_TYPE);
                dialog.setConfirmClickListener(new CustomAlertDialog.OnCustomClickListener() {
                    @Override
                    public void onClick(CustomAlertDialog alertDialog) {
                        dialog.dismiss();
                    }
                });
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        transEnd(new ActionResult(TransResult.ERR_ABORTED, null));
                    }
                });
                dialog.setTimeout(3);
                dialog.show();
                dialog.setNormalText(getString(R.string.err_not_allowed));
                dialog.showCancelButton(false);
                dialog.showConfirmButton(true);
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
