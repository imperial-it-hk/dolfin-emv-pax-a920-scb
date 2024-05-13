package th.co.bkkps.edc.trans;

import android.content.Context;
import android.content.DialogInterface;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ATransaction;
import com.pax.abl.core.ActionResult;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.entity.CTransResult;
import com.pax.eemv.enums.ECvmResult;
import com.pax.eemv.enums.ETransResult;
import com.pax.jemv.clcommon.TransactionPath;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.emv.EmvTags;
import com.pax.pay.emv.EmvTransProcess;
import com.pax.pay.emv.clss.ClssTransProcess;
import com.pax.pay.trans.BaseTrans;
import com.pax.pay.trans.action.ActionClssAfterReadCardProcess;
import com.pax.pay.trans.action.ActionClssPreProc;
import com.pax.pay.trans.action.ActionClssReadCardProcess;
import com.pax.pay.trans.action.ActionEmvAfterReadCardProcess;
import com.pax.pay.trans.action.ActionEmvReadCardProcess;
import com.pax.pay.trans.action.ActionEnterPin;
import com.pax.pay.trans.action.ActionInputTransData;
import com.pax.pay.trans.action.ActionSearchCard;
import com.pax.pay.trans.action.ActionSearchCard.CardInformation;
import com.pax.pay.trans.action.ActionSearchCard.SearchMode;
import com.pax.pay.trans.action.ActionSignature;
import com.pax.pay.trans.action.ActionTcAdvice;
import com.pax.pay.trans.action.ActionTransOnline;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.AmexPaymentPlanCards;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.task.PrintTask;
import com.pax.pay.utils.ToastUtils;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;
import com.pax.view.dialog.CustomAlertDialog;

import java.util.List;

import th.co.bkkps.edc.trans.action.ActionEnterInstalmentAMEX;

public class InstalmentAmexTrans extends BaseTrans {

    private String amount;
    private float percent;

    private boolean isFreePin;
    private boolean isSupportBypass = true;

    private byte searchCardMode = -1; // search card mode
    private byte orgSearchCardMode = -1; // search card mode
    private byte currentMode;
    private boolean needFallBack = false;


    private int cntTryAgain;
    private CTransResult clssResult;

    private AAction.ActionEndListener mTransEMVProcEndListener = null;

    /**
     * @param context   :context
     * @param isFreePin :true free; false not
     * @param mode      {@link com.pax.pay.trans.action.ActionSearchCard.SearchMode}, 如果等于-1，
     */
    public InstalmentAmexTrans(Context context, byte mode, boolean isFreePin,
                               ATransaction.TransEndListener transListener) {
        super(context, ETransType.AMEX_INSTALMENT, transListener);
        this.context = context;
        setParam( mode, isFreePin);
        setBackToMain(true);
    }


    public void setTransEMVProcEndListener(AAction.ActionEndListener listener) {
        mTransEMVProcEndListener = listener;
    }

    private void setParam( byte mode, boolean isFreePin) {
        this.searchCardMode = mode;
        this.orgSearchCardMode = mode;
        this.isFreePin = isFreePin;
    }

    @Override
    public void bindStateOnAction() {

        //Enter and Cal Instalment amounts
        ActionEnterInstalmentAMEX actionEnterInstalmentAMEX = new ActionEnterInstalmentAMEX(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionEnterInstalmentAMEX) action).setParam(getCurrentContext(),getString(R.string.menu_instalment_amex));
            }
        });
        bind(State.ENTER_INSTALMENT.toString(), actionEnterInstalmentAMEX, true);

        // search card action
        ActionSearchCard searchCardAction = new ActionSearchCard(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionSearchCard) action).setParam(getCurrentContext(), getString(R.string.menu_instalment_amex), searchCardMode, transData.getAmount(),
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
                ((ActionEnterPin) action).setParam(getCurrentContext(), getString(R.string.menu_sale),
                        transData.getPan(), isSupportBypass, getString(R.string.prompt_pin),
                        getString(R.string.prompt_no_pin), transData.getAmount(), transData.getTipAmount(),
                        ActionEnterPin.EEnterPinType.ONLINE_PIN,transData);
            }
        });
        bind(State.ENTER_PIN.toString(), enterPinAction, true);

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

        //TC-Advice Sale trans. for BBL_HOST
        ActionTcAdvice tcAdviceAction = new ActionTcAdvice(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionTcAdvice) action).setParam(getCurrentContext(), transData);
            }
        });
        bind(State.TC_ADVICE_SEND.toString(), tcAdviceAction);

        //print preview action
        PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(InstalmentAmexTrans.this, State.PRINT.toString()));
        bind(State.PRINT.toString(), printTask);

        gotoState(State.ENTER_INSTALMENT.toString());

    }

    enum State {
        ENTER_INSTALMENT,
        CHECK_CARD,
        ENTER_PIN,
        MAG_ONLINE,
        EMV_READ_CARD,
        EMV_PROC,
        ENTER_APPR_CODE,
        CLSS_PREPROC,
        CLSS_READ_CARD,
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
                afterEMVProcess(transResult.getTransResult());

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
//                    toSignOrPrint(); //Comment as EPP offline not support
                    transEnd(new ActionResult(TransResult.ERR_OFFLINE_UNSUPPORTED, null));
                }

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
                            processTcAdvice();
                        } else {
//                            toSignOrPrint(); //Comment as EPP offline not support
                            transEnd(new ActionResult(TransResult.ERR_OFFLINE_UNSUPPORTED, null));
                        }
                    }
                } else {
                    transEnd(result);
                }
                break;
            case ENTER_APPR_CODE:
                afterEnterApprCode(result.getData().toString());
                break;
            case SIGNATURE:
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

                gotoState(State.PRINT.toString());
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
        transData.setTransType(ETransType.AMEX_INSTALMENT);

        if (needFallBack) {
            transData.setEnterMode(TransData.EnterMode.FALLBACK);
        }

        // enter card number manually
        currentMode = cardInfo.getSearchMode();
        if (currentMode == SearchMode.SWIPE || currentMode == SearchMode.KEYIN) {
            //Check for AMEX card
            if(transData.getAcquirer().getName() != null && !transData.getAcquirer().getName().equals(Constants.ACQ_AMEX_EPP)){
                transEnd(new ActionResult(TransResult.ERR_CARD_UNSUPPORTED, null));
                return;
            }
            if (chkPaymentPlanCardSupport()) {
                goCheckPin();
            }
        } else if (currentMode == SearchMode.INSERT) {
            needRemoveCard = true;
            // EMV process
            gotoState(State.EMV_READ_CARD.toString());
        } else if (currentMode == SearchMode.WAVE) {
            needRemoveCard = true;
            gotoState(State.CLSS_READ_CARD.toString());
        }
    }

    private void onEnterPin(ActionResult result) {
        String pinBlock = (String) result.getData();
        transData.setPin(pinBlock);
        if (pinBlock != null && !pinBlock.isEmpty()) {
            transData.setPinVerifyMsg(true);
            transData.setHasPin(true);
        }

        // online process
        gotoState(State.MAG_ONLINE.toString());
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
            // enter pin
            if (!isFreePin) {
                gotoState(State.ENTER_PIN.toString());
            } else {
                // online process
                gotoState(State.MAG_ONLINE.toString());
            }
        }

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
                gotoState(State.PRINT.toString());
            } else {
                transData.setSignFree(false);
                boolean eSignature = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_E_SIGNATURE);
                if (eSignature && !transData.isTxnSmallAmt()) {
                    gotoState(State.SIGNATURE.toString());
                }else {
                    gotoState(State.PRINT.toString()); // Skip SIGNATURE process
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

        if (chkPaymentPlanCardSupport()) {
            gotoState(State.EMV_PROC.toString());
        }
    }

    private void afterEMVProcess(ETransResult transResult) {
        EmvTransProcess.emvTransResultProcess(transResult, emv, transData);
        if (transResult == ETransResult.ONLINE_APPROVED) {
            //nothing (handle printing outside)
        } else if (transResult == ETransResult.OFFLINE_APPROVED) {
//            transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));// Fixed EDCBBLAND-235
//            transData.setTraceNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO));
//            Component.saveOfflineTransNormalSale(transData);
            //handle printing outsideD, null));
        } else if (transResult == ETransResult.SIMPLE_FLOW_END) { // simplify the process
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
        if (!chkPaymentPlanCardSupport()) {
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

        if (transResult.getTransResult() == ETransResult.ONLINE_APPROVED) {
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
        ActionEnterInstalmentAMEX.InstalmentAmexInfo amtInfo = (ActionEnterInstalmentAMEX.InstalmentAmexInfo) result.getData();
        transData.setAmount(amtInfo.getInstalmentAmount().replace(".",""));
        transData.setInstalmentTerms(amtInfo.getInstalmentTerms());
        transData.setInstalmentMonthDue(amtInfo.getInstalmentMonthDue().replace(".",""));

        if (searchCardMode == -1) {
            searchCardMode = Component.getCardReadMode(ETransType.AMEX_INSTALMENT);
            orgSearchCardMode = searchCardMode;
        }
        if ((searchCardMode & SearchMode.WAVE) == SearchMode.WAVE) {
            gotoState(State.CLSS_PREPROC.toString());
        } else {
            gotoState(State.CHECK_CARD.toString());
        }
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

    private boolean chkPaymentPlanCardSupport() {
        List<AmexPaymentPlanCards> paymentPlanCards = Utils.readObjFromJSON("amex_epp_cards.json", AmexPaymentPlanCards.class);
        if (paymentPlanCards.isEmpty()) {
            transEnd(new ActionResult(TransResult.ERR_CARD_UNSUPPORTED, null));
            return false;
        }

        if (transData.getPan() != null) {
            long lPan = Utils.parseLongSafe(transData.getPan().substring(0, 10), 0);
            for (AmexPaymentPlanCards planCard : paymentPlanCards) {
                long low = Utils.parseLongSafe(planCard.getPanRangeLow(), 0);
                long high = Utils.parseLongSafe(planCard.getPanRangeHigh(), 0);
                if (lPan >= low && lPan <= high) {
                    return true;
                }
            }
        }

        transEnd(new ActionResult(TransResult.ERR_CARD_UNSUPPORTED, null));
        return false;
    }

    @Override
    protected void transEnd(final ActionResult result) {
        searchCardMode = orgSearchCardMode;
        super.transEnd(result);
    }

}
