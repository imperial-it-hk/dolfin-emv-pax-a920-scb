package com.pax.pay.trans;

import android.content.Context;
import android.content.DialogInterface;
import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.entity.CTransResult;
import com.pax.eemv.enums.ECvmResult;
import com.pax.eemv.enums.ETransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.emv.EmvTags;
import com.pax.pay.emv.EmvTransProcess;
import com.pax.pay.emv.clss.ClssTransProcess;
import com.pax.pay.trans.action.*;
import com.pax.pay.trans.action.ActionSearchCard.CardInformation;
import com.pax.pay.trans.action.ActionSearchCard.SearchMode;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.task.PrintTask;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;
import com.pax.view.dialog.CustomAlertDialog;
import com.pax.view.dialog.DialogUtils;
import th.co.bkkps.edc.trans.action.ActionEnterInstalmentBay;

public class InstalmentBayTrans extends BaseTrans {

    private byte searchCardMode = -1; // search card mode
    private byte orgSearchCardMode = -1; // search card mode
    private String amount;
    private float percent;

    private boolean isFreePin;
    private boolean isSupportBypass = true;
    private boolean needFallBack = false;

    private Context context = null;
    private byte currentMode;

    private int cntTryAgain;

    private boolean isSpMsc;

    private AAction.ActionEndListener mTransEMVProcEndListener = null;

    /**
     * @param context   :context
     * @param isFreePin :true free; false not
     * @param mode      {@link SearchMode}, 如果等于-1，
     */
    public InstalmentBayTrans(Context context, byte mode, boolean isFreePin,
                              TransEndListener transListener,boolean isSpMsc) {
        super(context, ETransType.BAY_INSTALMENT, transListener);
        this.context = context;
        setParam( mode, isFreePin, isSpMsc);
        setBackToMain(true);
    }


    public void setTransEMVProcEndListener(AAction.ActionEndListener listener) {
        mTransEMVProcEndListener = listener;
    }

    private void setParam( byte mode, boolean isFreePin, boolean isSpMsc) {
        this.searchCardMode = mode;
        this.orgSearchCardMode = mode;
        this.isFreePin = isFreePin;
        this.isSpMsc = isSpMsc;

//        if (searchCardMode == -1) { // 待机银行卡消费入口
//            searchCardMode = Component.getCardReadMode(ETransType.SALE);
//            this.transType = ETransType.SALE;
//        }
    }

    @Override
    public void bindStateOnAction() {

        //Enter and Cal Instalment amounts
        ActionEnterInstalmentBay actionEnterInstalment = new ActionEnterInstalmentBay(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionEnterInstalmentBay) action).setParam(getCurrentContext(),getString(R.string.menu_instalment_bay),isSpMsc);
            }
        });
        bind(State.ENTER_INSTALMENT.toString(), actionEnterInstalment, true);

        // search card action
        ActionSearchCard searchCardAction = new ActionSearchCard(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionSearchCard) action).setParam(getCurrentContext(), getString(R.string.menu_instalment_bay), searchCardMode, transData.getAmount(),
                        null, "", transData);
            }
        });
        bind(State.CHECK_CARD.toString(), searchCardAction, true);

        // enter pin action
        ActionEnterPin enterPinAction = new ActionEnterPin(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {

                // if flash pay by pwd,set isSupportBypass=false,need to enter pin
                if (!isFreePin) {
                    isSupportBypass = false;
                }
                ((ActionEnterPin) action).setParam(getCurrentContext(), getString(R.string.menu_instalment_bay),
                        transData.getPan(), isSupportBypass, getString(R.string.prompt_pin),
                        getString(R.string.prompt_no_pin), transData.getAmount(), transData.getTipAmount(),
                        ActionEnterPin.EEnterPinType.ONLINE_PIN, transData);
            }
        });
        bind(State.ENTER_PIN.toString(), enterPinAction, true);

        // emv process action
        ActionEmvProcess emvProcessAction = new ActionEmvProcess(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionEmvProcess) action).setParam(getCurrentContext(), emv, transData);
            }
        });
        bind(State.EMV_PROC.toString(), emvProcessAction);

        //clss process action
        ActionClssProcess clssProcessAction = new ActionClssProcess(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionClssProcess) action).setParam(getCurrentContext(), clss, transData);
            }
        });
        bind(State.CLSS_PROC.toString(), clssProcessAction);

        //clss preprocess action
        ActionClssPreProc clssPreProcAction = new ActionClssPreProc(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionClssPreProc) action).setParam(clss, transData);
            }
        });
        bind(State.CLSS_PREPROC.toString(), clssPreProcAction);

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
                ((ActionInputTransData) action).setParam(getCurrentContext(), getString(R.string.menu_sale))
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

        //TC-Advice Sale trans.
        ActionTcAdvice tcAdviceAction = new ActionTcAdvice(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionTcAdvice) action).setParam(getCurrentContext(), transData);
            }
        });
        bind(State.TC_ADVICE_SEND.toString(), tcAdviceAction);

        //print preview action
        PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(InstalmentBayTrans.this, State.PRINT.toString()));
        bind(State.PRINT.toString(), printTask);


        transData.setBayInstalmentSpecific(isSpMsc);
        gotoState(State.ENTER_INSTALMENT.toString());

    }

    enum State {
        ENTER_INSTALMENT,
        CHECK_CARD,
        ENTER_PIN,
        MAG_ONLINE,
        EMV_PROC,
        ENTER_APPR_CODE,
        CLSS_PREPROC,
        CLSS_PROC,
        SIGNATURE,
        TC_ADVICE_SEND,
        PRINT,

    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {
        int ret = result.getRet();
        State state = State.valueOf(currentState);

        switch (state) {
            case ENTER_INSTALMENT:
                // save trans amount
                cntTryAgain = 0;
                needFallBack = false;
                onInstalmentAmt(result);
                gotoState(State.CHECK_CARD.toString());
                break;
            case CHECK_CARD: // subsequent processing of check card
                onCheckCard(result);
                break;
            case ENTER_PIN: // subsequent processing of enter pin
                onEnterPin(result);
                break;
            case MAG_ONLINE: // subsequent processing of online
                if (ret != TransResult.SUCC) {
                    if (ret == TransResult.ERR_REFERRAL_CALL_ISSUER) {
                        gotoState(State.ENTER_APPR_CODE.toString());
                        return;
                    } else {
                        transEnd(result);
                        return;
                    }
                }
                processTcAdvice();
                // determine whether need electronic signature or print
//                toSignOrPrint();

                break;
            case EMV_PROC:
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
                }// emv后续处理
                //get trans result
                CTransResult transResult = (CTransResult) result.getData();
                // EMV完整流程 脱机批准或联机批准都进入签名流程
                afterEMVProcess(transResult.getTransResult());

                break;
            case CLSS_PREPROC:
                if (ret != TransResult.SUCC) {
                    searchCardMode &= 0x03;
                }
                gotoState(State.CHECK_CARD.toString());
                break;
            case CLSS_PROC:
                CTransResult clssResult = (CTransResult) result.getData();
                afterClssProcess(clssResult);
                break;
            case ENTER_APPR_CODE:
                afterEnterApprCode(result.getData().toString());
                break;
            case SIGNATURE:
                // save signature data
                byte[] signData = (byte[]) result.getData();
                byte[] signPath = (byte[]) result.getData1();

                if (signData != null && signData.length > 0) {
                    transData.setSignData(signData);
                    transData.setSignPath(signPath);
                    // update trans data，save signature
                    FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                }

                gotoState(State.PRINT.toString());

                //check offline trans
//                checkOfflineTrans();
                break;
            case TC_ADVICE_SEND:
                //if (result.getRet() == TransResult.SUCC) {
                //    checkOfflineTrans();
                //} else {
                    toSignOrPrint();
                //}
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
        transData.setTransType(ETransType.BAY_INSTALMENT);

        if (needFallBack) {
            transData.setEnterMode(TransData.EnterMode.FALLBACK);
        }

        // enter card number manually
        currentMode = cardInfo.getSearchMode();
        if (currentMode == SearchMode.SWIPE || currentMode == SearchMode.KEYIN) {
            //Check for BAY card
            if(transData.getAcquirer().getName() != null && !transData.getAcquirer().getName().equals(Constants.ACQ_BAY_INSTALLMENT)){
                transEnd(new ActionResult(TransResult.ERR_CARD_UNSUPPORTED, null));
            }
            goTipBranch();
        } else if (currentMode == SearchMode.INSERT) {
            needRemoveCard = true;
            // EMV process
            gotoState(State.EMV_PROC.toString());
        } else if (currentMode == SearchMode.WAVE) {
            needRemoveCard = true;
            // AET-15
            gotoState(State.CLSS_PROC.toString());
        } /*else if (currentMode == ActionSearchCard.SearchMode.QR) {
            needRemoveCard = false;
            this.transData.setTransType(ETransType.QR_SALE_WALLET);
            initTransDataQr(new ActionResult(TransResult.SUCC, transData.getAmount()));
            this.transData.setQrBuyerCode(cardInfo.getQRData());

            //Check if last settlement not success
            if (Component.chkSettlementStatus(transData.getAcquirer().getName())) {
                transEnd(new ActionResult(TransResult.ERR_SETTLE_NOT_COMPLETED, null));
                return;
            }
            gotoState(State.QR_INQUIRY.toString());
        }*/
    }

    private void onEnterPin(ActionResult result) {
        String pinBlock = (String) result.getData();
        transData.setPin(pinBlock);
        if (pinBlock != null && !pinBlock.isEmpty()) {
            transData.setHasPin(true);
        }

        if (checkBelowFloorLimitMag()) {// check below floor limit for Mag-stripe
            toSignOrPrint();
            return;
        }

        //clss process
        if (transData.getEnterMode() == TransData.EnterMode.CLSS &&
                transData.getEmvResult() == ETransResult.CLSS_OC_APPROVED) {
            if (!transData.isSignFree()) {
                //gotoState(State.SIGNATURE.toString());
                gotoState(State.PRINT.toString()); // Skip SIGNATURE process
            } else {
                gotoState(State.PRINT.toString());
            }
            return;
        }

        // online process
        gotoState(State.MAG_ONLINE.toString());
    }

    private void goTipBranch() {

        // Include TEST MODE
        Acquirer acq = FinancialApplication.getAcqManager().getCurAcq();
        if (acq.isEnableUpi() && !acq.isTestMode() && transData.getIssuer()!=null &&
                (Constants.ISSUER_UP.equals(transData.getIssuer().getName()) ||
                        Constants.ISSUER_TBA.equals(transData.getIssuer().getName())))
        {
            gotoState(State.ENTER_PIN.toString());
        } else {
            // enter pin
            if (!isFreePin) {
                gotoState(State.ENTER_PIN.toString());
            } else {
            /*//clss process
                if (!transData.isSignFree()) {
                    gotoState(State.SIGNATURE.toString());
                } else {
                    gotoState(State.PRINT.toString());
                }
                return;*/
                if (checkBelowFloorLimitMag()) {// check below floor limit for Mag-stripe
                    toSignOrPrint();
                } else {
                    // online process
                    gotoState(State.MAG_ONLINE.toString());
                }
            }
        }

    }

    private boolean checkBelowFloorLimitMag() {
        if (transData.getEnterMode() == TransData.EnterMode.SWIPE && Utils.parseLongSafe(transData.getAmount(), 0) < transData.getIssuer().getFloorLimit()) {
            long authCode = transData.getStanNo() % 100;
            transData.setOrigAuthCode(String.valueOf(authCode));
            transData.setAuthCode(String.valueOf(authCode));
            Component.saveOfflineTransNormalSale(transData);
            return true;
        }
        return false;
    }

    // need electronic signature or send
    private void toSignOrPrint() {
        if (transData.isTxnSmallAmt() && transData.getNumSlipSmallAmt() == 0) {//EDCBBLAND-426 Support small amount
            FinancialApplication.getTransDataDbHelper().updateTransData(transData);
            transEnd(new ActionResult(TransResult.SUCC, null));
            return;
        }
        // if(transData.getIssuer().getName().equals(EKernelType.JCB.name()) && !transData.isOnlineTrans() && transData.isHasPin()){
        if(transData.isPinVerifyMsg() || (!transData.isOnlineTrans() && transData.isHasPin())){
            transData.setSignFree(true);
            gotoState(State.PRINT.toString());
        }else{
            if (Component.isSignatureFree(transData,null)) {// signature free
                // print preview
                transData.setSignFree(true);
                gotoState(State.PRINT.toString());
            } else {
                boolean eSignature = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_E_SIGNATURE);
                if (eSignature && !transData.isTxnSmallAmt()) {
                    transData.setSignFree(false);
                    gotoState(State.SIGNATURE.toString());
                }else {
                    transData.setSignFree(true);
                    //gotoState(State.SIGNATURE.toString());
                    gotoState(State.PRINT.toString()); // Skip SIGNATURE process
                }
            }
        }
        FinancialApplication.getTransDataDbHelper().updateTransData(transData);
    }

    private void afterEMVProcess(ETransResult transResult) {
        EmvTransProcess.emvTransResultProcess(transResult, emv, transData);
        if (transResult == ETransResult.ONLINE_APPROVED) {// 联机批准
            //check offline trans
            processTcAdvice();
//            toSignOrPrint();
        } else if (transResult == ETransResult.OFFLINE_APPROVED) {//脱机批准处理
            transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));// Fixed EDCBBLAND-235
            transData.setTraceNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO));
            Component.saveOfflineTransNormalSale(transData);
            toSignOrPrint();
        } /* will not hit this branch, will remove it
        else if (transResult == ETransResult.ARQC) { // request online
            if (!EmvTransProcess.isQpbocNeedOnlinePin(emv)) {
                gotoState(State.MAG_ONLINE.toString());
                return;
            }
            if (isFreePin && EmvTransProcess.clssQPSProcess(emv, transData)) { // pin free
                gotoState(State.MAG_ONLINE.toString());
            } else {
                // enter pwd
                transData.setPinFree(false);
                gotoState(State.ENTER_PIN.toString());
            }
        } */else if (transResult == ETransResult.SIMPLE_FLOW_END) { // simplify the process
            // trans not support simplified process
            // end trans
            transEnd(new ActionResult(TransResult.ERR_ABORTED, null));
        } else if (transResult == ETransResult.ONLINE_DENIED) { // refuse online
            // end trans
            transEnd(new ActionResult(TransResult.ERR_HOST_REJECT, null));
        } else if (transResult == ETransResult.ONLINE_CARD_DENIED) {// 平台批准卡片拒绝
            transEnd(new ActionResult(TransResult.ERR_CARD_DENIED, null));
        } else if (transResult == ETransResult.ABORT_TERMINATED ||
                transResult == ETransResult.OFFLINE_DENIED) { // emv interrupt
            Device.beepErr();
            transEnd(new ActionResult(TransResult.ERR_ABORTED, null));
        }
    }

    private void afterClssProcess(CTransResult transResult) {
        if (transResult.getTransResult() == ETransResult.CLSS_OC_TRY_AGAIN) {
            DialogUtils.showErrMessage(getCurrentContext(), transType.getTransName(), getString(R.string.prompt_please_retry), null, Constants.FAILED_DIALOG_SHOW_TIME);
            //AET-175
            gotoState(State.CLSS_PREPROC.toString());
            return;
        }

        // 流水号增加
        Component.incStanNo(transData);
        // 设置交易结果
        transData.setEmvResult(transResult.getTransResult());
        if (transResult.getTransResult() == ETransResult.ABORT_TERMINATED ||
                transResult.getTransResult() == ETransResult.CLSS_OC_DECLINED) { // emv interrupt
            Device.beepErr();
            transEnd(new ActionResult(TransResult.ERR_ABORTED, null));
            return;
        }

        ClssTransProcess.clssTransResultProcess(transResult, clss, transData);

        if (transResult.getCvmResult() == ECvmResult.SIG || !Component.isSignatureFree(transData,null)) { // AET-283
            //FIXME don't upload the signature
            //do signature after online
            if(!transData.isPinVerifyMsg()){
                transData.setSignFree(false);
            }
        } else {
            transData.setSignFree(true);
        }

/*        if (transResult.getTransResult() == ETransResult.CLSS_OC_APPROVED || transResult.getTransResult() == ETransResult.ONLINE_APPROVED) {
            transData.setOnlineTrans(transResult.getTransResult() == ETransResult.ONLINE_APPROVED);
            checkOfflineTrans();
//            if (!transData.isSignFree()) {
//                //gotoState(State.SIGNATURE.toString());
//                gotoState(State.PRINT.toString()); // Skip SIGNATURE process
//            } else {
//                gotoState(State.PRINT.toString());
//            }
        }*/
        // ETransResult.CLSS_OC_ONLINE_REQUEST, online is handled in the module
    }

    private void processTcAdvice() {
        if (transData.getAcquirer().isEmvTcAdvice() && transData.getTransType() == ETransType.BAY_INSTALMENT) {
            gotoState(State.TC_ADVICE_SEND.toString());
        } else {
            //checkOfflineTrans();
            //transEnd(new ActionResult(TransResult.ERR_ABORTED, null));
            toSignOrPrint();

        }
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

        if (transData.getEnterMode() == TransData.EnterMode.INSERT) {
            EmvTransProcess.emvTransResultProcess(ETransResult.ONLINE_DENIED, emv, transData);
        }

        Component.incTraceNo(transData);//reserved trace no. for referred transaction before settlement
        Component.incTraceNo(transData);//trace no. of next transaction
        toSignOrPrint();
    }

    private void onInstalmentAmt(ActionResult result){
        ActionEnterInstalmentBay.InstalmentBayInfo amtInfo = (ActionEnterInstalmentBay.InstalmentBayInfo) result.getData();
        transData.setAmount(amtInfo.getInstalmentAmount().replace(".",""));
        transData.setInstalmentTerms(amtInfo.getInstalmentTerms());
        transData.setInstalmentInterest(amtInfo.getInstalmentInterest());
        transData.setSerialNum(amtInfo.getInstalmentSerialNum());
        transData.setMktCode(amtInfo.getInstalmentMktCode());
        transData.setSkuCode(amtInfo.getInstalmentSku());
    }



    @Override protected void transEnd(final ActionResult result) {
        searchCardMode = orgSearchCardMode;
        super.transEnd(result);
    }

}
