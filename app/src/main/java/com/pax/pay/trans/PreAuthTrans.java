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
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.entity.CTransResult;
import com.pax.eemv.enums.ETransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.emv.EmvTags;
import com.pax.pay.emv.EmvTransProcess;
import com.pax.pay.trans.action.ActionEmvProcess;
import com.pax.pay.trans.action.ActionEnterAmount;
import com.pax.pay.trans.action.ActionEnterPin;
import com.pax.pay.trans.action.ActionInputTransData;
import com.pax.pay.trans.action.ActionOfflineSend;
import com.pax.pay.trans.action.ActionSearchCard;
import com.pax.pay.trans.action.ActionSearchCard.CardInformation;
import com.pax.pay.trans.action.ActionSearchCard.SearchMode;
import com.pax.pay.trans.action.ActionSignature;
import com.pax.pay.trans.action.ActionTransOnline;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.model.TransData.EnterMode;
import com.pax.pay.trans.task.PrintTask;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;
import com.pax.view.dialog.CustomAlertDialog;

public class PreAuthTrans extends BaseTrans {
    private String amount;
    private boolean isNeedInputAmount = true; // is need input amount
    private boolean isFreePin = true;
    private boolean isSupportBypass = true;

    private byte searchCardMode = -1;
    private boolean needFallBack = false;

    private int cntTryAgain;

    public PreAuthTrans(Context context, boolean isFreePin, TransEndListener transListener) {
        super(context, ETransType.PREAUTH, transListener);
        this.isFreePin = isFreePin;
        isNeedInputAmount = true;
        searchCardMode = Component.getCardReadMode(ETransType.PREAUTH);
    }

    public PreAuthTrans(Context context, String amount, TransEndListener transListener) {
        super(context, ETransType.PREAUTH, transListener);
        this.amount = amount;
        isNeedInputAmount = false;
        searchCardMode = Component.getCardReadMode(ETransType.PREAUTH);
    }

    @Override
    protected void bindStateOnAction() {
        // enter amount action
        ActionEnterAmount amountAction = new ActionEnterAmount(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionEnterAmount) action).setParam(getCurrentContext(),
                        getString(R.string.trans_preAuth), false);
            }
        });
        bind(State.ENTER_AMOUNT.toString(), amountAction, true);
        // read card
        ActionSearchCard searchCardAction = new ActionSearchCard(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionSearchCard) action).setParam(getCurrentContext(), getString(R.string.trans_preAuth),
                        searchCardMode, transData.getAmount(),
                        null, getString(R.string.prompt_insert_swipe_card), transData);
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
                ((ActionEnterPin) action).setParam(getCurrentContext(), getString(R.string.trans_preAuth),
                        transData.getPan(), isSupportBypass, getString(R.string.prompt_pin),
                        getString(R.string.prompt_no_pin), transData.getAmount(), transData.getTipAmount(), ActionEnterPin.EEnterPinType.ONLINE_PIN, transData);
            }
        });
        bind(State.ENTER_PIN.toString(), enterPinAction, true);
        // emv action
        ActionEmvProcess emvProcessAction = new ActionEmvProcess(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionEmvProcess) action).setParam(getCurrentContext(), emv, transData);
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

        //offline send
        ActionOfflineSend offlineSendAction = new ActionOfflineSend(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionOfflineSend) action).setParam(getCurrentContext(), transData);
            }
        });
        //even it failed to upload offline, it will continue current transaction, so the 3rd argv is false
        bind(State.OFFLINE_SEND.toString(), offlineSendAction);

        //print preview action
        PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(PreAuthTrans.this, SaleTrans.State.PRINT.toString()));
        bind(State.PRINT.toString(), printTask);

        // execute the first action
        if (isNeedInputAmount) {
            gotoState(State.ENTER_AMOUNT.toString());
        } else {
            transData.setAmount(amount);
            gotoState(State.CHECK_CARD.toString());
        }

    }

    enum State {
        ENTER_AMOUNT,
        CHECK_CARD,
        ENTER_PIN,
        EMV_PROC,
        MAG_ONLINE,
        ENTER_APPR_CODE,
        SIGNATURE,
        OFFLINE_SEND,
        PRINT,
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {
        int ret = result.getRet();
        State state = State.valueOf(currentState);
        if (state == State.EMV_PROC) {
            // 不管emv处理结果成功还是失败，都更新一下冲正
            String pan = transData.getPan();
            byte[] f55Dup = EmvTags.getF55(emv, transType, true,pan);
            if (f55Dup.length > 0) {
                TransData dupTransData = FinancialApplication.getTransDataDbHelper().findFirstDupRecord(transData.getAcquirer());
                if (dupTransData != null) {
                    dupTransData.setDupIccData(Utils.bcd2Str(f55Dup));
                    FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);
                }
            }
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
            } else if (ret == TransResult.ERR_REFERRAL_CALL_ISSUER) {
                gotoState(State.ENTER_APPR_CODE.toString());
                return;
            } else if (ret != TransResult.SUCC) {
                transEnd(result);
                return;
            }
        }

        switch (state) {
            case ENTER_AMOUNT:// 输入交易金额后续处理
                // save amount
                transData.setAmount(result.getData().toString());
                gotoState(State.CHECK_CARD.toString());
                break;
            case CHECK_CARD: // 检测卡的后续处理
                onCheckCard(result);
                break;
            case ENTER_PIN: // 输入密码的后续处理
                onEnterPin(result);
                break;
            case MAG_ONLINE: // after online
                if (ret != TransResult.SUCC) {
                    if (ret == TransResult.ERR_REFERRAL_CALL_ISSUER) {
                        gotoState(State.ENTER_APPR_CODE.toString());
                        return;
                    } else {
                        transEnd(result);
                        return;
                    }
                }
                // judge whether need signature or print
                toSignOrPrint();

                break;
            case EMV_PROC: // emv后续处理
                onEmvProc(result);
                break;
            case ENTER_APPR_CODE:
                afterEnterApprCode(result.getData().toString());
                break;
            case SIGNATURE:
                onSignature(result);
                break;
            case OFFLINE_SEND:
                gotoState(State.PRINT.toString());
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

    private void onCheckCard(ActionResult result) {
        CardInformation cardInfo = (CardInformation) result.getData();
        saveCardInfo(cardInfo, transData);
        transData.setTransType(ETransType.PREAUTH);
        if (needFallBack) {
            transData.setEnterMode(EnterMode.FALLBACK);
        }
        // 手输卡号处理
        byte mode = cardInfo.getSearchMode();
        if (mode == SearchMode.KEYIN || mode == SearchMode.SWIPE) {
            gotoState(State.MAG_ONLINE.toString());
            // input password
//            gotoState(State.ENTER_PIN.toString());
        } else if (mode == SearchMode.INSERT || mode == SearchMode.WAVE) {
            needRemoveCard = true;
            // EMV处理
            gotoState(State.EMV_PROC.toString());
        }
    }

    private void onEnterPin(ActionResult result) {
        String pinBlock = (String) result.getData();
        transData.setPin(pinBlock);
        if (pinBlock != null && !pinBlock.isEmpty()) {
            transData.setHasPin(true);
        }
        // online
        gotoState(State.MAG_ONLINE.toString());
    }

    private void onEmvProc(ActionResult result) {
        ETransResult transResult = ((CTransResult) result.getData()).getTransResult();
        // EMV完整流程 脱机批准或联机批准都进入签名流程
        EmvTransProcess.emvTransResultProcess(transResult, emv, transData);
        if (transResult == ETransResult.ONLINE_APPROVED || transResult == ETransResult.OFFLINE_APPROVED) {// 联机批准/脱机批准处理
            // judge whether need signature or print
            toSignOrPrint();

        } /* will not hit this branch, will remove it
        else if (transResult == ETransResult.ARQC || transResult == ETransResult.SIMPLE_FLOW_END) { // 请求联机/简化流程

            if (!isFreePin) {
                transData.setPinFree(false);
                gotoState(State.ENTER_PIN.toString());
                return;
            }

            if (transResult == ETransResult.ARQC && !EmvTransProcess.isQpbocNeedOnlinePin(emv)) {
                gotoState(State.MAG_ONLINE.toString());
                return;
            }
            if (EmvTransProcess.clssQPSProcess(emv, transData)) { // pin free
                gotoState(State.MAG_ONLINE.toString());
            } else {
                // input password
                transData.setPinFree(false);
                gotoState(State.ENTER_PIN.toString());
            }
        } */else if (transResult == ETransResult.ONLINE_DENIED) { // online denied
            // transaction end
            transEnd(new ActionResult(TransResult.ERR_HOST_REJECT, null));
        } else if (transResult == ETransResult.ONLINE_CARD_DENIED) {// platform approve card denied
            transEnd(new ActionResult(TransResult.ERR_CARD_DENIED, null));
        } else if (transResult == ETransResult.ABORT_TERMINATED) { // emv terminated
            // transaction end
            transEnd(new ActionResult(TransResult.ERR_ABORTED, null));
        } else if (transResult == ETransResult.OFFLINE_DENIED) {
            // transaction end
            Device.beepErr();
            transEnd(new ActionResult(TransResult.ERR_ABORTED, null));
        }
    }

    // 判断是否需要电子签名或打印
    private void toSignOrPrint() {
        if(transData.isPinVerifyMsg() || (!transData.isOnlineTrans() && transData.isHasPin())){
            transData.setSignFree(true);
            gotoState(State.PRINT.toString());
        } else {
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

        if (signData != null && signData.length > 0 &&
                signPath != null && signPath.length > 0) {
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

    private void afterEnterApprCode(String appCode) {
        transData.setReferralStatus(TransData.ReferralStatus.PENDING);
        transData.setAuthCode(appCode);
        transData.setRefNo(null);
        transData.setResponseCode(FinancialApplication.getRspCode().parse("00"));
        FinancialApplication.getTransDataDbHelper().insertTransData(transData);

        if (transData.getEnterMode() == EnterMode.INSERT) {
            EmvTransProcess.emvTransResultProcess(ETransResult.ONLINE_DENIED, emv, transData);
        }

        Component.incTraceNo(transData);//reserved trace no. for referred transaction before settlement
        Component.incTraceNo(transData);//trace no. of next transaction
        toSignOrPrint();
    }
}
