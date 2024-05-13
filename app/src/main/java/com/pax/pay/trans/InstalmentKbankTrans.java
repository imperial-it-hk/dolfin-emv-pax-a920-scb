package com.pax.pay.trans;

import android.content.Context;
import android.content.DialogInterface;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.appstore.DownloadManager;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.entity.CTransResult;
import com.pax.eemv.enums.ETransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.emv.EmvTags;
import com.pax.pay.emv.EmvTransProcess;
import com.pax.pay.trans.action.ActionEmvAfterReadCardProcess;
import com.pax.pay.trans.action.ActionEmvReadCardProcess;
import com.pax.pay.trans.action.ActionEnterInstalmentKbank;
import com.pax.pay.trans.action.ActionEnterInstalmentKbank.InstalmentKbankInfo;
import com.pax.pay.trans.action.ActionEnterPhoneNumber;
import com.pax.pay.trans.action.ActionEnterRef1Ref2;
import com.pax.pay.trans.action.ActionSearchCard;
import com.pax.pay.trans.action.ActionSignature;
import com.pax.pay.trans.action.ActionTcAdvice;
import com.pax.pay.trans.action.ActionTransOnline;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.task.PrintTask;
import com.pax.pay.utils.ControlLimitUtils;
import com.pax.pay.utils.Convert;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;
import com.pax.view.dialog.CustomAlertDialog;

public class InstalmentKbankTrans extends BaseTrans {
    private byte searchCardMode = -1; // search card mode
    private byte orgSearchCardMode = -1; // search card mode
    protected String iPlanMode;
    protected String title;
    private boolean needFallBack = false;
    private byte currentMode;
    private int cntTryAgain = 0;

    public InstalmentKbankTrans(Context context, String iPlanMode, TransEndListener transListener) {
        super(context, ETransType.KBANK_SMART_PAY, Constants.ACQ_SMRTPAY, transListener);
        this.iPlanMode = iPlanMode;
        setTitle();
        setBackToMain(true);
    }

    public InstalmentKbankTrans(Context context, ETransType transType, String acquirer, String iPlanMode, TransEndListener transListener) {
        super(context, transType, acquirer, transListener);
        this.iPlanMode = iPlanMode;
        setBackToMain(true);
    }

    private void setTitle() {
        String resString = "menu_instalment_";
        title = getString(R.string.menu_instalment);
        if (iPlanMode != null) {
            resString += iPlanMode;
        }
        int resId = Utils.getResId(resString, "string");
        if (resId > 0)
            title = Utils.getString(resId);
    }

    @Override
    protected void bindStateOnAction() {
        // search card action
        ActionSearchCard searchCardAction = new ActionSearchCard(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionSearchCard) action).setParam(getCurrentContext(), title, searchCardMode, transData.getAmount(),
                        null, Utils.getString(R.string.prompt_default_searchCard_prompt), transData);
            }
        });
        bind(State.CHECK_CARD.toString(), searchCardAction, true);

        ActionEmvReadCardProcess emvReadCardAction = new ActionEmvReadCardProcess(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionEmvReadCardProcess) action).setParam(getCurrentContext(), emv, transData);
            }
        });
        bind(State.EMV_READ_CARD.toString(), emvReadCardAction);


        //todo: enter smart pay data action
        ActionEnterInstalmentKbank enterInstalmentAction = new ActionEnterInstalmentKbank(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionEnterInstalmentKbank) action).setParam(getCurrentContext(), title, transData,iPlanMode);
            }
        });
        bind(State.ENTER_SMART_PAY_DATA.toString(), enterInstalmentAction, true);

        ActionEnterRef1Ref2 actionEnterRef1Ref2 = new ActionEnterRef1Ref2(action ->
                ((ActionEnterRef1Ref2) action).setParam(getCurrentContext(), getString(R.string.menu_sale))
        );
        bind(State.ENTER_REF1_REF2.toString(), actionEnterRef1Ref2, true);


        ActionEnterPhoneNumber actionEnterPhoneNumber = new ActionEnterPhoneNumber(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionEnterPhoneNumber) action).setParam(getCurrentContext(),  transData);
            }
        });
        bind(State.ENTER_PHONE_NUMBER.toString(), actionEnterPhoneNumber);


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

        ActionTcAdvice tcAdviceAction = new ActionTcAdvice(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionTcAdvice) action).setParam(getCurrentContext(), transData);
            }
        });
        bind(State.TC_ADVICE_SEND.toString(), tcAdviceAction);

        // signature action
        ActionSignature signatureAction = new ActionSignature(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionSignature) action).setParam(getCurrentContext(), transData.getAmount(), !Component.isAllowSignatureUpload(transData));
            }
        });
        bind(State.SIGNATURE.toString(), signatureAction);

        //print preview action
        PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(InstalmentKbankTrans.this, State.PRINT.toString()));
        bind(State.PRINT.toString(), printTask);

        // ERM Maximum TransExceed Check
        int ErmExeccededResult = ErmLimitExceedCheck();
        if (ErmExeccededResult != TransResult.SUCC) {
            transEnd(new ActionResult(ErmExeccededResult,null));
            return;
        }

        if (searchCardMode == -1) {
            searchCardMode = Component.getCardReadMode(transType);
            orgSearchCardMode = searchCardMode;
        }
        transData.setAmount("0");

        if (transData.getTransType() == ETransType.DOLFIN_INSTALMENT)
            return;

        gotoState(State.CHECK_CARD.toString());
    }

    enum State {
        CHECK_CARD,
        EMV_READ_CARD,
        ENTER_SMART_PAY_DATA,
        ENTER_REF1_REF2,
        ENTER_PHONE_NUMBER,
        EMV_PROC,
        MAG_ONLINE,
        TC_ADVICE_SEND,
        SIGNATURE,
        PRINT,
    }


    @Override
    public void onActionResult(String currentState, ActionResult result) {
        int ret = result.getRet();
        State state = State.valueOf(currentState);
        switch (state) {
            case CHECK_CARD:
                onCheckCard(result);
                break;
            case EMV_READ_CARD:
                onEmvReadCard(result);
                break;
            case ENTER_SMART_PAY_DATA:
                onEnterSmartPayData(result);
                break;
            case ENTER_REF1_REF2:
                String ref1 = null, ref2 = null;
                if (result.getData() != null) {
                    ref1 = result.getData().toString();
                }
                if (result.getData1() != null) {
                    ref2 = result.getData1().toString();
                }

                transData.setSaleReference1(ref1);
                transData.setSaleReference2(ref2);

                if (ControlLimitUtils.Companion.isAllowEnterPhoneNumber(transData.getAcquirer().getName())) {
                    gotoState(State.ENTER_PHONE_NUMBER.toString());
                }  else {
                    if (currentMode == ActionSearchCard.SearchMode.SWIPE || currentMode == ActionSearchCard.SearchMode.KEYIN) {
                        gotoState(State.MAG_ONLINE.toString());
                    } else {
                        gotoState(State.EMV_PROC.toString());
                    }
                }
                break;
            case ENTER_PHONE_NUMBER:
                if (result.getRet() == TransResult.ERR_USER_CANCEL) {
                    transEnd(result);
                }

                String phoneNumStr = "";
                if (result.getData()==null || (result.getData() instanceof String && result.getData().equals(""))) {
                    phoneNumStr = Utils.getStringPadding("",10," ", Convert.EPaddingPosition.PADDING_LEFT);
                } else {
                    phoneNumStr = Utils.getStringPadding((String)result.getData(), 10, " ", Convert.EPaddingPosition.PADDING_LEFT);
                }

                transData.setPhoneNum(phoneNumStr);

                if (currentMode == ActionSearchCard.SearchMode.SWIPE || currentMode == ActionSearchCard.SearchMode.KEYIN) {
                    gotoState(SaleTrans.State.MAG_ONLINE.toString());
                } else {
                    gotoState(SaleTrans.State.EMV_PROC.toString());
                }
                break;
            case EMV_PROC:
                String pan = transData.getPan();
                byte[] f55Dup = EmvTags.getF55(emv, transType, true,pan);
                if (f55Dup.length > 0) {
                    TransData dupTransData = FinancialApplication.getTransDataDbHelper().findFirstDupRecord(transData.getAcquirer());
                    if (dupTransData != null) {
                        dupTransData.setDupIccData(Utils.bcd2Str(f55Dup));
                        FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);
                    }
                }
                if (ret != TransResult.SUCC) {
                    transEnd(result);
                    return;
                }
                CTransResult transResult = (CTransResult) result.getData();
                afterEMVProcess(transResult.getTransResult());
                break;
            case MAG_ONLINE:
                if (ret != TransResult.SUCC) {
                    transEnd(result);
                    return;
                }
                processTcAdvice();
                break;
            case TC_ADVICE_SEND:
                toSignOrPrint();
                break;
            case SIGNATURE:
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
                gotoState(State.PRINT.toString());
                break;
            case PRINT:
                if (result.getRet() == TransResult.SUCC) {
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
        ActionSearchCard.CardInformation cardInfo = (ActionSearchCard.CardInformation) result.getData();
        saveCardInfo(cardInfo, transData);

        if (needFallBack) {
            transData.setEnterMode(TransData.EnterMode.FALLBACK);
        }
        transData.setInstalmentIPlanMode(iPlanMode);

        currentMode = cardInfo.getSearchMode();
        if (currentMode == ActionSearchCard.SearchMode.SWIPE || currentMode == ActionSearchCard.SearchMode.KEYIN) {
            needRemoveCard = false;
            gotoState(State.ENTER_SMART_PAY_DATA.toString());
        } else {
            needRemoveCard = true;
            gotoState(State.EMV_READ_CARD.toString());
        }
    }

    private void onEmvReadCard(ActionResult result) {
        int ret = result.getRet();
        if(ret == TransResult.ICC_TRY_AGAIN) {
            cntTryAgain++;
            if(cntTryAgain == 3) {
                needFallBack = true;
                searchCardMode &= 0x01;
                showMsgDialog(getCurrentContext(), getString(R.string.prompt_fallback) + getString(R.string.prompt_swipe_card));
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

        boolean isUp = Constants.ISSUER_UP.equals(transData.getIssuer().getIssuerBrand());
        if (isUp) {
            transData.setEnterMode(TransData.EnterMode.FALLBACK);
        }

        gotoState(State.ENTER_SMART_PAY_DATA.toString());

    }

    private void onEnterSmartPayData(ActionResult result) {
        //todo smart pay data
        InstalmentKbankInfo instalmentInfo = (InstalmentKbankInfo)result.getData();
        transData.setAmount(instalmentInfo.getInstalmentAmount());
        transData.setInstalmentPromotionKey(instalmentInfo.getInstalmentPromotionKey());
        transData.setInstalmentSerialNo(instalmentInfo.getInstalmeniSerialNum());
        transData.setInstalmentPaymentTerm(instalmentInfo.getIntalmentTerms());
        transData.setInstalmentPromoProduct(instalmentInfo.isInstalmentPromoProduct());

        int iMode = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_SUPPORT_REF1_2_MODE);
        DownloadManager.EdcRef1Ref2Mode mode = DownloadManager.EdcRef1Ref2Mode.getByMode(iMode);
        if (mode != DownloadManager.EdcRef1Ref2Mode.DISABLE) {
            gotoState(State.ENTER_REF1_REF2.toString());
        } else if (ControlLimitUtils.Companion.isAllowEnterPhoneNumber(transData.getAcquirer().getName())) {
            gotoState(State.ENTER_PHONE_NUMBER.toString());
        }  else {
            if (currentMode == ActionSearchCard.SearchMode.SWIPE || currentMode == ActionSearchCard.SearchMode.KEYIN) {
                gotoState(State.MAG_ONLINE.toString());
            } else {
                gotoState(State.EMV_PROC.toString());
            }
        }

    }

    private void afterEMVProcess(ETransResult transResult) {
        EmvTransProcess.emvTransResultProcess(transResult, emv, transData);
        if (transResult == ETransResult.ONLINE_APPROVED) {// 联机批准
            processTcAdvice();
        } else if (transResult == ETransResult.OFFLINE_APPROVED) {//脱机批准处理
            transData.setOfflineSendState(TransData.OfflineStatus.OFFLINE_NOT_SENT);
            transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
            transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));// Fixed EDCBBLAND-235
            FinancialApplication.getTransDataDbHelper().insertTransData(transData);
            // increase trans no.
            Component.incStanNo(transData);
            Component.incTraceNo(transData);// Fixed EDCBBLAND-235 increase trace no. for offline trans.
            toSignOrPrint();
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

    private void processTcAdvice() {
        if (transData.getAcquirer() != null && transData.getAcquirer().isEmvTcAdvice()) {
            gotoState(State.TC_ADVICE_SEND.toString());
        } else {
            toSignOrPrint();
        }
    }

    // need electronic signature or send
    private void toSignOrPrint() {
        if (transData.isTxnSmallAmt() && transData.getNumSlipSmallAmt() == 0) {//EDCBBLAND-426 Support small amount
            FinancialApplication.getTransDataDbHelper().updateTransData(transData);
//            transEnd(new ActionResult(TransResult.SUCC, null));
//            return;
        }

        if(transData.isPinVerifyMsg() || (!transData.isOnlineTrans() && transData.isHasPin())
                || !transData.getAcquirer().isSignatureRequired()){
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
                gotoState(SaleTrans.State.CHECK_CARD.toString());
            }
        });
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
                gotoState(SaleTrans.State.CHECK_CARD.toString());
            }
        });
    }

    @Override
    protected void transEnd(final ActionResult result) {
        searchCardMode = orgSearchCardMode;
        emv.setListener(null);//no memory leak
        super.transEnd(result);
    }
}
