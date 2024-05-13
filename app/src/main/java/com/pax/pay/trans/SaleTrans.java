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
import com.pax.abl.core.AAction.ActionStartListener;
import com.pax.abl.core.ActionResult;
import com.pax.appstore.DownloadManager;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.entity.CTransResult;
import com.pax.eemv.enums.ECvmResult;
import com.pax.eemv.enums.ETransResult;
import com.pax.jemv.clcommon.TransactionPath;
import com.pax.pay.ECR.EcrData;
import com.pax.pay.ECR.LawsonHyperCommClass;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.Issuer;
import com.pax.pay.base.TransTypeMapping;
import com.pax.pay.constant.Constants;
import com.pax.pay.emv.EmvQr;
import com.pax.pay.emv.EmvSP200;
import com.pax.pay.emv.EmvTags;
import com.pax.pay.emv.EmvTransProcess;
import com.pax.pay.emv.clss.ClssTransProcess;
import com.pax.pay.trans.action.ActionAdjustTip;
import com.pax.pay.trans.action.ActionCheckQR;
import com.pax.pay.trans.action.ActionClssAfterReadCardProcess;
import com.pax.pay.trans.action.ActionClssPreProc;
import com.pax.pay.trans.action.ActionClssReadCardProcess;
import com.pax.pay.trans.action.ActionConfirmDCC;
import com.pax.pay.trans.action.ActionEmvAfterReadCardProcess;
import com.pax.pay.trans.action.ActionEmvReadCardProcess;
import com.pax.pay.trans.action.ActionEnterAmount;
import com.pax.pay.trans.action.ActionEnterPhoneNumber;
import com.pax.pay.trans.action.ActionEnterPin;
import com.pax.pay.trans.action.ActionEnterRef1Ref2;
import com.pax.pay.trans.action.ActionInputTransData;
import com.pax.pay.trans.action.ActionOfflineSend;
import com.pax.pay.trans.action.ActionQrSaleInquiry;
import com.pax.pay.trans.action.ActionSearchCard;
import com.pax.pay.trans.action.ActionSearchCard.CardInformation;
import com.pax.pay.trans.action.ActionSearchCard.SearchMode;
import com.pax.pay.trans.action.ActionSignature;
import com.pax.pay.trans.action.ActionTcAdvice;
import com.pax.pay.trans.action.ActionTransOnline;
import com.pax.pay.trans.action.ActionUserAgreement;
import com.pax.pay.trans.action.activity.SearchCardActivity;
import com.pax.pay.trans.action.activity.UserAgreementActivity;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.AcqManager;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.MerchantProfileManager;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.model.TransData.EnterMode;
import com.pax.pay.trans.task.PrintTask;
import com.pax.pay.utils.ControlLimitUtils;
import com.pax.pay.utils.Convert;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.MultiMerchantUtils;
import com.pax.pay.utils.ToastUtils;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;
import com.pax.view.dialog.CustomAlertDialog;

import java.util.ArrayList;
import java.util.List;

import th.co.bkkps.amexapi.AmexTransService;
import th.co.bkkps.amexapi.action.ActionAmexCheckAID;
import th.co.bkkps.amexapi.action.ActionAmexSaleTrans;
import th.co.bkkps.utils.Log;

public class SaleTrans extends BaseTrans {
    private TransData dccGetRateTrans = null;
    private byte searchCardMode = -1; // search card mode
    private byte orgSearchCardMode = -1; // search card mode
    private String amount;
    private String tipAmount;
    private float percent;
    private String branchID;
    private EmvSP200 emvSP200;

    private boolean isFreePin;
    private boolean isSupportBypass = true;
    private boolean hasTip = false;
    private boolean needFallBack = false;

    private Context context = null;
    private byte currentMode;

    private int cntTryAgain;
    private CTransResult clssResult;

    private AAction.ActionEndListener mTransEMVProcEndListener = null;

    private boolean isVatb;
    private byte[] REF1;
    private byte[] REF2;
    private String vatAmount;
    private String taxAllowance;
    private String refSaleID;
    private byte[] mercUniqueValue;
    private byte[] campaignType;

    private ActionResult searchCardResult = null;

    /**
     * @param context   :context
     * @param amount    :total amount
     * @param isFreePin :true free; false not
     * @param mode      {@link com.pax.pay.trans.action.ActionSearchCard.SearchMode}, 如果等于-1，
     */
    public SaleTrans(Context context, String amount, byte mode, String branchID, boolean isFreePin, TransEndListener transListener) {
        super(context, ETransType.SALE, transListener);
        this.context = context;
        setParam(amount, "0", branchID, mode, isFreePin, false);
    }


    // This method may use for calling by EcrPaymentSelectActivity only
    public SaleTrans(Context context, String amount, byte mode, String branchID, boolean isFreePin, String refSaleID, TransEndListener transListener) {
        super(context, ETransType.SALE, transListener);
        this.context = context;
        this.refSaleID = refSaleID;
        setParam(amount, "0", branchID, mode, isFreePin, false);
    }


    /**
     * @param context   :context
     * @param amount    :total amount
     * @param tipAmount :tip amount
     * @param isFreePin :true free; false not
     * @param mode      {@link com.pax.pay.trans.action.ActionSearchCard.SearchMode}, 如果等于-1，
     */
    public SaleTrans(Context context, String amount, String tipAmount, byte mode, boolean isFreePin, TransEndListener transListener) {
        super(context, ETransType.SALE, transListener);
        setParam(amount, tipAmount, "", mode, isFreePin, true);
    }

    /**
     * @param context   :context
     * @param amount    :total amount
     * @param isFreePin :true free; false not
     * @param mode      {@link com.pax.pay.trans.action.ActionSearchCard.SearchMode}, 如果等于-1，
     */
    public SaleTrans(Context context, String amount, byte mode, String branchID, boolean isFreePin, TransEndListener transListener, boolean isVatb, byte[] REF1, byte[] REF2, byte[] vatAmount, byte[] taxAllowance, byte[] mercUniqueValue
            , byte[] campaignType) {
        super(context, ETransType.SALE, transListener);
        this.context = context;
        this.isVatb = isVatb;
        if (isVatb) {
            this.REF1 = (REF1 != null) ? new String(REF1).trim().getBytes() : "".getBytes();
            this.REF2 = (REF2 != null) ? new String(REF2).trim().getBytes() : "".getBytes();
            this.vatAmount = (vatAmount != null) ? new String(vatAmount) : "0000000000";
            this.taxAllowance = (taxAllowance != null) ? new String(taxAllowance) : "0000000000";
            this.mercUniqueValue = (mercUniqueValue != null) ? mercUniqueValue : "                    ".getBytes();
            this.campaignType = (campaignType != null) ? campaignType : "000000".getBytes();
        }
        setParam(amount, "0", branchID, mode, isFreePin, false);
    }

    public void setTransEMVProcEndListener(AAction.ActionEndListener listener) {
        mTransEMVProcEndListener = listener;
    }

    private void setParam(String amount, String tipAmount, String branchID, byte mode, boolean isFreePin, boolean hasTip) {
        this.searchCardMode = mode;
        this.orgSearchCardMode = mode;
        this.amount = amount;
        this.tipAmount = tipAmount;
        this.isFreePin = isFreePin;
        this.hasTip = hasTip;
        this.branchID = branchID;

//        if (searchCardMode == -1) { // 待机银行卡消费入口
//            searchCardMode = Component.getCardReadMode(ETransType.SALE);
//            this.transType = ETransType.SALE;
//        }
    }

    @Override
    public void bindStateOnAction() {
        if (amount != null && !amount.isEmpty()) {
            transData.setAmount(amount.replace(".", ""));
        }
        if (tipAmount != null && !tipAmount.isEmpty()) {
            transData.setTipAmount(tipAmount.replace(".", ""));
        }

        if (branchID != null && !branchID.isEmpty()) {
            transData.setBranchID(branchID);
        }
        if (isVatb) {
            transData.setIsVATB(isVatb);
            transData.setREF1(REF1);
            transData.setREF2(REF2);
            transData.setSaleReference1(new String(REF1));
            transData.setSaleReference2(new String(REF2));
            transData.setVatAmount(vatAmount);
            transData.setTaxAllowance(taxAllowance);
            transData.setMercUniqueValue(mercUniqueValue);
            transData.setCampaignType(campaignType);
        }
        // enter trans amount action(This action is mainly used to handle bank card consumption and flash close paid deals)
        ActionEnterAmount amountAction = new ActionEnterAmount(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionEnterAmount) action).setParam(getCurrentContext(),
                        getString(R.string.menu_sale), false);
            }
        });
        bind(State.ENTER_AMOUNT.toString(), amountAction, true);

        ActionEnterRef1Ref2 actionEnterRef1Ref2 = new ActionEnterRef1Ref2(action ->
                ((ActionEnterRef1Ref2) action).setParam(getCurrentContext(), getString(R.string.menu_sale))
        );
        bind(State.ENTER_REF1_REF2.toString(), actionEnterRef1Ref2, true);

        // search card action
        ActionSearchCard searchCardAction = new ActionSearchCard(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionSearchCard) action).setParam(getCurrentContext(), getString(R.string.menu_sale), searchCardMode, transData.getAmount(),
                        null, "", transData);
            }
        });
        bind(State.CHECK_CARD.toString(), searchCardAction, false);



        ActionEnterPhoneNumber actionInputPhoneNo = new ActionEnterPhoneNumber(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionEnterPhoneNumber) action).setParam(getCurrentContext(), transData);
            }
        });
        bind(State.ENTER_PHONE_NUMBER.toString(), actionInputPhoneNo, false);



        ActionAmexCheckAID actionAmexCheckAID = new ActionAmexCheckAID(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                if (transData.getAid() != null) {
                    ((ActionAmexCheckAID) action).setParam(getCurrentContext(), transData.getEnterMode(), transData.getAid());
                }
                else {
                    ((ActionAmexCheckAID) action).setParam(getCurrentContext(), transData.getEnterMode());
                }
            }
        });
        bind(State.CHECK_AMEX_AID.toString(), actionAmexCheckAID, false);

        //adjust tip action
        ActionAdjustTip adjustTipAction = new ActionAdjustTip(new ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                amount = String.valueOf(Utils.parseLongSafe(transData.getAmount(), 0) -
                        Utils.parseLongSafe(transData.getTipAmount(), 0));
                ((ActionAdjustTip) action).setParam(getCurrentContext(), getString(R.string.menu_sale), amount, percent);
            }
        });
        bind(State.ADJUST_TIP.toString(), adjustTipAction, true);

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
                        ActionEnterPin.EEnterPinType.ONLINE_PIN, transData);
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

        // online action
        ActionTransOnline dccGetRateAction = new ActionTransOnline(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionTransOnline) action).setParam(getCurrentContext(), dccGetRateTrans);
            }
        });
        bind(State.DCC_GET_RATE.toString(), dccGetRateAction);

        ActionEmvAfterReadCardProcess emvProcessAction = new ActionEmvAfterReadCardProcess(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionEmvAfterReadCardProcess) action).setParam(getCurrentContext(), emv, transData);
            }
        });
        bind(State.EMV_PROC.toString(), emvProcessAction, true);

        //clss preprocess action
        ActionClssPreProc clssPreProcAction = new ActionClssPreProc(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionClssPreProc) action).setParam(clss, transData);
            }
        });
        bind(State.CLSS_PREPROC.toString(), clssPreProcAction, true);

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
                        .setInputTransIDLine(getString(R.string.prompt_input_appr_code), ActionInputTransData.EInputType.TRANSID, 6, 0);
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

        // Agreement action
        ActionUserAgreement userAgreementAction = new ActionUserAgreement(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionUserAgreement) action).setParam(getCurrentContext());
            }
        });
        bind(State.USER_AGREEMENT.toString(), userAgreementAction, true);

        //offline send
        ActionOfflineSend offlineSendAction = new ActionOfflineSend(new ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionOfflineSend) action).setParam(getCurrentContext(), transData);
            }
        });
        //even it failed to upload offline, it will continue current transaction, so the 3rd argv is false
        bind(State.OFFLINE_SEND.toString(), offlineSendAction);

        //TC-Advice Sale trans. for BBL_HOST
        ActionTcAdvice tcAdviceAction = new ActionTcAdvice(new ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionTcAdvice) action).setParam(getCurrentContext(), transData);
            }
        });
        bind(State.TC_ADVICE_SEND.toString(), tcAdviceAction);

        ActionQrSaleInquiry qrSaleInquiry = new ActionQrSaleInquiry(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionQrSaleInquiry) action).setParam(getCurrentContext(),
                        getString(R.string.trans_wallet_sale), transData);
            }
        });
        bind(State.QR_INQUIRY.toString(), qrSaleInquiry, false);

        ActionCheckQR checkQR = new ActionCheckQR(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionCheckQR) action).setParam(getCurrentContext(),
                        getString(R.string.trans_wallet_sale), transData);
            }
        });
        bind(State.QR_CHECKQR.toString(), checkQR, true);

        //print preview action
        PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(SaleTrans.this, State.PRINT.toString()));
        bind(State.PRINT.toString(), printTask);

        ActionConfirmDCC confirmDccAction = new ActionConfirmDCC(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionConfirmDCC) action).setParam(getCurrentContext(),
                        getString(R.string.menu_sale), transData);
            }
        });
        bind(State.DCC_CONFIRM.toString(), confirmDccAction, true);

//        ActionAmexSaleTrans actionAmexSaleTrans = new ActionAmexSaleTrans( action ->
//                ((ActionAmexSaleTrans) action).setParam(getCurrentContext(), transData, emvSP200, mECRrocReturnListener)
//        );
        ActionAmexSaleTrans actionAmexSaleTrans = new ActionAmexSaleTrans(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                if (MerchantProfileManager.INSTANCE.isMultiMerchantEnable()
                        && MultiMerchantUtils.Companion.getCurrentMerchantName()!=null
                        && MultiMerchantUtils.Companion.getCurrentMerchantName().equals(MultiMerchantUtils.Companion.getMasterProfileName())) {
                    ((ActionAmexSaleTrans) action).setParam(getCurrentContext(), transData, emvSP200, mECRrocReturnListener);
                } else {
                    transEnd(new ActionResult(TransResult.ERR_CARD_UNSUPPORTED,null));
                    return;
                }
            }
        });
        bind(State.AMEX_API.toString(), actionAmexSaleTrans, false);

        // ERM Maximum TransExceed Check
        int ermExeccededResult = ErmLimitExceedCheck();
        if (ermExeccededResult != TransResult.SUCC) {
            transEnd(new ActionResult(ermExeccededResult,null));
            return;
        }

        // perform the first action
        if (amount == null || amount.isEmpty()) {
            if (searchCardMode == -1) { // 待机银行卡消费入口
                searchCardMode = Component.getCardReadMode(ETransType.SALE);
                this.transType = ETransType.SALE;
            }
            gotoState(State.ENTER_AMOUNT.toString());
        } else if (amount != null) {
            if (!EcrData.instance.isOnProcessing) {
                EcrData.instance.isOnProcessing = true;
            }

            cntTryAgain = 0;
            needFallBack = false;
            if (searchCardMode == -1) { // 待机银行卡消费入口
                searchCardMode = Component.getCardReadMode(ETransType.SALE);
                this.transType = ETransType.SALE;
            }
            transData.setAmount(this.amount);
            if (refSaleID != null) {
                transData.setReferenceSaleID(refSaleID);
            }

            if (Utils.parseLongSafe(amount, 0) > FinancialApplication.getSysParam().getCtlsTransLimit()) {
                searchCardMode &= ~SearchMode.WAVE;
            }

            int iMode = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_SUPPORT_REF1_2_MODE);
            DownloadManager.EdcRef1Ref2Mode mode = DownloadManager.EdcRef1Ref2Mode.getByMode(iMode);
            if (!isVatb && mode != DownloadManager.EdcRef1Ref2Mode.DISABLE) {
                gotoState(State.ENTER_REF1_REF2.toString());
            } else {
                if ((searchCardMode & SearchMode.WAVE) == SearchMode.WAVE) {
                    if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_CONTACTLESS)) {
                        gotoState(State.CLSS_PREPROC.toString());
                    } else {
                        gotoState(State.CHECK_CARD.toString());
//                    transEnd(new ActionResult(TransResult.ERR_CARD_UNSUPPORTED, null));
//                    return;
                    }
                } else {
                    gotoState(State.CHECK_CARD.toString());
                }
            }
        } else {
            if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.SUPPORT_USER_AGREEMENT)) {
                gotoState(State.USER_AGREEMENT.toString());
            } else {
                gotoState(State.CLSS_PREPROC.toString());
            }
        }

    }

    @Override
    public void gotoState(String state) {
        if (state.equals(State.PRINT.toString())) {
            if (transData != null && transData.isEcrProcess()) {
                try {
                    if (FinancialApplication.getEcrProcess().mHyperComm instanceof LawsonHyperCommClass) {
                        // set Signature Data
                        EcrData.instance.signatureImgData = (transData.getSignData() != null)
                                ? ((transData.getSignData().length > 0) ? transData.getSignData() : new byte[]{})
                                : new byte[]{};
                        // set AID
                        EcrData.instance.kioskPos_AID = Utils.checkStringContainNullOrEmpty(transData.getAid(), Utils.LengthValidatorMode.EQUALS, 7, true, " ").getBytes();
                        // set TVR
                        EcrData.instance.kioskPos_TVR = Utils.checkStringContainNullOrEmpty(transData.getTvr(), Utils.LengthValidatorMode.EQUALS, 5, true, " ").getBytes();
                        // set TSI
                        EcrData.instance.kioskPos_TSI = Utils.checkStringContainNullOrEmpty(transData.getTsi(), Utils.LengthValidatorMode.EQUALS, 2, true, " ").getBytes();

                        // set PrintSignatureBox
                        EcrData.instance.kioskPos_PrintSignatureBox = (transData.getSignData() == null && transData.isPinVerifyMsg() && transData.isTxnSmallAmt())
                                ? new byte[]{0x31} //Print signature box
                                : new byte[]{0x30}; //No signature box

                        // set SaleReferenceID
                        EcrData.instance.saleReferenceIDR0 = Utils.checkStringContainNullOrEmpty(transData.getReferenceSaleID()).getBytes();
                        //EcrData.instance.RefNo = Utils.checkStringContainNullOrEmpty(transData.getRefNo()).getBytes();
                        // set HostIndex
                        String hostIndex = LawsonHyperCommClass.getLawsonHostIndex(transData.getAcquirer());
                        EcrData.instance.hostIndex = Utils.checkStringContainNullOrEmpty(hostIndex).getBytes();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                ECRProcReturn(null, new ActionResult(TransResult.SUCC, null));
            }
        }

        super.gotoState(state);
    }

    //protected ActionResult magOnline_temp_actionResult = null;

    @Override
    public void onActionResult(String currentState, ActionResult result) {
        int ret = result.getRet();
        State state = State.valueOf(currentState);

        switch (state) {
            case ENTER_AMOUNT:
                // save trans amount
                cntTryAgain = 0;
                needFallBack = false;
                //searchCardMode = Component.getCardReadMode(ETransType.SALE);
                transData.setAmount(result.getData().toString());

                if (Utils.parseLongSafe(transData.getAmount(), 0) > FinancialApplication.getSysParam().getCtlsTransLimit()) {
                    searchCardMode &= ~SearchMode.WAVE;
                }

                int iMode = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_SUPPORT_REF1_2_MODE);
                DownloadManager.EdcRef1Ref2Mode mode = DownloadManager.EdcRef1Ref2Mode.getByMode(iMode);
                if (!isVatb && mode != DownloadManager.EdcRef1Ref2Mode.DISABLE) {
                    gotoState(State.ENTER_REF1_REF2.toString());
                } else {
                    if ((searchCardMode & SearchMode.WAVE) == SearchMode.WAVE) {
                        if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_CONTACTLESS)) {
                            gotoState(State.CLSS_PREPROC.toString());
                        } else {
                            gotoState(State.CHECK_CARD.toString());
                        }
                    }
                }
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

                if ((searchCardMode & SearchMode.WAVE) == SearchMode.WAVE) {
                    gotoState(State.CLSS_PREPROC.toString());
                } else {
                    gotoState(State.CHECK_CARD.toString());
                }
                break;
            case CHECK_CARD: // subsequent processing of check card
                if (result.getRet() == TransResult.ERR_NEED_FORWARD_TO_AMEX_API) {

                    if (Component.chkSettlementStatus(Constants.ACQ_AMEX)) {
                        transEnd(new ActionResult(TransResult.ERR_SETTLE_NOT_COMPLETED, null));
                        return;
                    }

                    ActionSearchCard.CardInformation cardInfo = (ActionSearchCard.CardInformation) result.getData();
                    if (cardInfo.getSearchMode() == SearchMode.KEYIN) {
                        transData.setEnterMode(EnterMode.MANUAL);
                    } else if (cardInfo.getSearchMode() == SearchMode.SWIPE) {
                        if (needFallBack) {
                            transData.setEnterMode(TransData.EnterMode.FALLBACK);
                        } else {
                            transData.setEnterMode(EnterMode.SWIPE);
                        }
                    }
                    transData.setPan(cardInfo.getPan());
                    transData.setExpDate(cardInfo.getExpDate());
                    transData.setTrack1(cardInfo.getTrack1());
                    transData.setTrack2(cardInfo.getTrack2());
                    transData.setTrack3(cardInfo.getTrack3());
                    BaseTrans.setTransRunning(false);
                    gotoState(State.AMEX_API.toString());
                } else if (result.getRet() != TransResult.SUCC) {
                    transEnd(result);
                } else {
                    onCheckCard(result);
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

                if (currentMode == SearchMode.INSERT) {
                    gotoState(State.EMV_PROC.toString());
                } else if (currentMode == SearchMode.WAVE ) {
                    gotoState(State.CLSS_PROC.toString());
                } else {
                    gotoState(State.MAG_ONLINE.toString());
                }
                break;
            case CHECK_AMEX_AID:
                if (result.getRet() == TransResult.ERR_NEED_FORWARD_TO_AMEX_API) {
                    BaseTrans.setTransRunning(false);
                    gotoState(State.AMEX_API.toString());
                } else if (result.getRet() == TransResult.ERR_CARD_UNSUPPORTED
                        || result.getRet() == TransResult.ERR_SETTLE_NOT_COMPLETED) {
                    transEnd(result);
                } else {
                    if (currentMode == SearchMode.INSERT) {
                        needRemoveCard = true;
                        gotoState(State.EMV_READ_CARD.toString());
                    } else if (currentMode == SearchMode.WAVE) {
                        needRemoveCard = true;
                        if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_CONTACTLESS)) {
                            gotoState(State.CLSS_READ_CARD.toString());
                        } else {
                            transEnd(new ActionResult(TransResult.ERR_CARD_UNSUPPORTED, null));
                        }
                    } else if (currentMode == SearchMode.SP200) {

                        if (isDoubleTransaction(transData)) {
                            return;
                        }

                        if (emvSP200 != null) {
                            switch (emvSP200.getiResult()) {
                                case 1://CLSS_APPROVE (offline)
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
                                    if (!transData.isPinFree() && transData.isSignFree()) {
                                        gotoState(State.ENTER_PIN.toString());
                                    } else {
                                        gotoState(State.MAG_ONLINE.toString());
                                    }
                                    return;
                            }
                        }
                    }
                }
                break;
            case SCAN_CODE:
                afterScanCode(result);
                break;
            case ADJUST_TIP:
                onAdjustTip(result);
                break;
            case ENTER_PIN: // subsequent processing of enter pin
                onEnterPin(result);
                break;
            case EMV_READ_CARD:
                onEmvReadCard(result);
                break;
            case DCC_GET_RATE:
                onDccGetRate(result);
                break;
            case DCC_CONFIRM:
                onDccConfirm(result);
                break;
            case MAG_ONLINE: // subsequent processing of online
                //magOnline_temp_actionResult = result;
                if (ret == TransResult.OFFLINE_APPROVED) {//脱机批准处理
                    //ECRProcReturn(null, new ActionResult(TransResult.SUCC, null));
                    transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));// Fixed EDCBBLAND-235
                    transData.setTraceNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO));
                    Component.saveOfflineTransNormalSale(transData);
                    toSignOrPrint();
                    return;
                }

                if (ret != TransResult.SUCC) {
                    if (ret == TransResult.ERR_REFERRAL_CALL_ISSUER) {
                        gotoState(State.ENTER_APPR_CODE.toString());
                        return;
                    } else {
                        transEnd(result);
                        return;
                    }
                }
                //ECRProcReturn(null, new ActionResult(result.getRet(), null));
                processTcAdvice();
                break;
            case EMV_PROC:
                // 不管emv处理结果成功还是失败，都更新一下冲正
                String pan = transData.getPan();
                byte[] f55Dup = EmvTags.getF55(emv, transType, true, pan);
                if (f55Dup.length > 0) {
                    TransData dupTransData = FinancialApplication.getTransDataDbHelper().findFirstDupRecord(transData.getAcquirer());
                    if (dupTransData != null) {
                        dupTransData.setDupIccData(Utils.bcd2Str(f55Dup));
                        FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);
                    }
                }
                /*if (ret == TransResult.ICC_TRY_AGAIN) {
                    cntTryAgain++;
                    if (cntTryAgain == 3) {
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
                    gotoState(State.CHECK_CARD.toString());
                    return;
                } else*/
                if (ret == TransResult.ERR_REFERRAL_CALL_ISSUER) {
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

                if (transResult.getTransResult() != ETransResult.ONLINE_APPROVED
                        && transResult.getTransResult() != ETransResult.OFFLINE_APPROVED) {
                    return;
                }

                //ECRProcReturn(null, new ActionResult(result.getRet(), null));
                if (transResult.getTransResult() == ETransResult.ONLINE_APPROVED) {
                    processTcAdvice();
                } else if (transResult.getTransResult() == ETransResult.OFFLINE_APPROVED) {
                    toSignOrPrint();
                }
                break;
            case CLSS_PREPROC:
                if (ret != TransResult.SUCC) {
                    searchCardMode &= 0x03;
                }
                gotoState(State.CHECK_CARD.toString());
                break;
            case CLSS_READ_CARD:
            case CLSS_PROC:
                if (currentState.equals(State.CLSS_READ_CARD.toString())) {
                    if (isDoubleTransaction(transData)) {
                        return;
                    }
                }
                if (ret == TransResult.ERR_NEED_FORWARD_TO_AMEX_API) {
                    BaseTrans.setTransRunning(false);
                    gotoState(State.AMEX_API.toString());
                    return;
                } else if (ret != TransResult.SUCC) {
                    transEnd(result);
                    return;
                }
                clssResult = (CTransResult) result.getData();
                if (clssResult != null) {
                    afterClssProcess(clssResult);
                    if (clssResult.getTransResult() == ETransResult.CLSS_OC_APPROVED
                            || clssResult.getTransResult() == ETransResult.OFFLINE_APPROVED
                            || clssResult.getTransResult() == ETransResult.ONLINE_APPROVED) {
                        //ECRProcReturn(null, new ActionResult(result.getRet(), null));
                        if (transData.isOnlineTrans()) {
                            checkOfflineTrans();
                        } else {
                            toSignOrPrint();
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

                if (signData != null && signData.length > 0/* &&
                        signPath != null && signPath.length > 0*/) {
                    transData.setSignData(signData);
                    transData.setSignPath(signPath);

                    // update trans data，save signature
                    FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                }

                gotoState(State.PRINT.toString());
                break;
            case USER_AGREEMENT:
                String agreement = (String) result.getData();
                if (agreement != null && agreement.equals(UserAgreementActivity.ENTER_BUTTON)) {
                    gotoState(State.CLSS_PREPROC.toString());
                } else {
                    transEnd(result);
                }
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
            case QR_INQUIRY:
                //toCheckQrOrPrint(new ActionResult(TransResult.SUCC,  transData));
                toCheckQrOrPrint(result);
                break;

            case QR_CHECKQR:
                if (result.getRet() == TransResult.SUCC) {
//                    transData = (TransData) result.getData();
                    gotoState(State.QR_INQUIRY.toString());
                } else {
                    gotoState(State.PRINT.toString());
                }
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
            case AMEX_API:
                if (result.getRet() == TransResult.SUCC) {
                    transEnd(result);
                } else {
                    transEnd(new ActionResult(TransResult.ERR_ABORTED, null));//no alert dialog
                }
                break;
            default:
                transEnd(result);
                break;
        }
    }

    private boolean isDoubleTransaction(TransData localTransData) {
        if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_DOUBLE_BLOCKED_TRANS_ENABLE)) {
            Log.d("BlockedDoubleTrans", "--blocked double transaction--enabled");
            TransData lastTransDataRecord = FinancialApplication.getTransDataDbHelper().findLatestSaleVoidTransData();
            if (lastTransDataRecord != null && localTransData != null) {
                if (Utils.getStringPadding(transData.getAmount(),12,"0", Convert.EPaddingPosition.PADDING_LEFT).equals(lastTransDataRecord.getAmount())
                        && localTransData.getPan().equals(lastTransDataRecord.getPan())) {
                    transEnd(new ActionResult(TransResult.ECR_FOUND_DOUBLE_TRANSACTION_BLOCKED, null));
                    return true;
                }
            } else {
                if (lastTransDataRecord == null) {
                    Log.d("BlockedDoubleTrans", "--lastTransDataRecord was not found");
                } else {
                    Log.d("BlockedDoubleTrans", "--localTransData was not found");
                }
            }
        } else {
            Log.d("BlockedDoubleTrans", "--blocked double transaction--disabled");
        }

        return false;
    }

    private void onCheckCard(ActionResult result) {
        if (transData.isEcrProcess()) {
            transData.setPosNo_ReceiptNo(new String(EcrData.instance.PosNo_ReceiptNo));
            transData.setCashierName(new String(EcrData.instance.CashierName));
        }

        transData.setTransType(ETransType.SALE);
        CardInformation cardInfo = (CardInformation) result.getData();
        if (cardInfo.getSearchMode() != SearchMode.SP200) {
            saveCardInfo(cardInfo, transData);
        }
        else {
            saveSP200Info(cardInfo.getEmvSP200());
            emvSP200 = cardInfo.getEmvSP200();
        }

        if (needFallBack) {
            transData.setEnterMode(EnterMode.FALLBACK);
        }
        // enter card number manually
        currentMode = cardInfo.getSearchMode();
        if (currentMode == SearchMode.INSERT || currentMode == SearchMode.WAVE || currentMode == SearchMode.SP200) {
            gotoState(State.CHECK_AMEX_AID.toString());
            return;
        }

        if (currentMode == SearchMode.SWIPE || currentMode == SearchMode.KEYIN) {
            goTipBranch();
        }
//        else if (currentMode == SearchMode.INSERT) {
//            needRemoveCard = true;
//            // EMV process
////            gotoState(State.EMV_PROC.toString());
//            gotoState(State.EMV_READ_CARD.toString());
//        } else if (currentMode == SearchMode.WAVE) {
//            needRemoveCard = true;
//            // AET-15
//            if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_CONTACTLESS)) {
//                gotoState(State.CLSS_READ_CARD.toString());
//            } else {
//                transEnd(new ActionResult(TransResult.ERR_CARD_UNSUPPORTED, null));
//                return;
//            }
//
//        } else if (currentMode == SearchMode.SP200) {
//            needRemoveCard = true;
//            int sp200Result = 0;// = saveSP200Info(cardInfo.getEmvSP200());
//            if (sp200Result == TransResult.ERR_NEED_FORWARD_TO_AMEX_API) {
//                BaseTrans.setTransRunning(false);
//                emvSP200 = cardInfo.getEmvSP200();
//                gotoState(State.AMEX_API.toString());
//            } else if (sp200Result != TransResult.SUCC) {
//                transEnd(new ActionResult(sp200Result, null));
//            } else {
//                if (cardInfo.getEmvSP200() != null) {
//
//                    if (isDoubleTransaction(transData)) {
//                        return;
//                    }
//
//                    switch (cardInfo.getEmvSP200().getiResult()) {
//                        case 1://CLSS_APPROVE (offline)
//                            transData.setOnlineTrans(false);
//                            transData.setOrigAuthCode(Utils.getString(R.string.response_Y1_str));
//                            transData.setAuthCode(Utils.getString(R.string.response_Y1_str));
//                            transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));// Fixed EDCBBLAND-235
//                            transData.setTraceNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO));
//                            Component.saveOfflineTransNormalSale(transData);
//                            toSignOrPrint();
//                            return;
//                        case 3://CLSS_TRY_ANOTHER_INTERFACE
//                            ToastUtils.showMessage("Please use contact");
//                            transEnd(new ActionResult(TransResult.ERR_ABORTED, null));
//                            return;
//                        case 0://CLSS_DECLINED
//                        case 4://CLSS_END_APPLICATION
//                            Device.beepErr();
//                            showMsgDialog(getCurrentContext(), getString(R.string.dialog_clss_declined) + ", " + getString(R.string.dialog_clss_try_contact));
//                            transEnd(new ActionResult(TransResult.ERR_ABORTED, null));
//                            return;
//                        default://CLSS_ONLINE_REQUEST
//                            if (!transData.isPinFree() && transData.isSignFree()) {
//                                gotoState(State.ENTER_PIN.toString());
//                            } else {
//                                gotoState(State.MAG_ONLINE.toString());
//                            }
//                            return;
//                    }
//                }
//            }
//        }
        else if (currentMode == SearchMode.QR) {
            needRemoveCard = false;
            //gotoState(State.SCAN_CODE.toString());

            Log.i("QRCode:", "QRData=" + cardInfo.getQRData());

            this.transData.setTransType(ETransType.QR_SALE_WALLET);
            initTransDataQr(new ActionResult(TransResult.SUCC, transData.getAmount()));
            this.transData.setQrBuyerCode(cardInfo.getQRData());

            //Check if last settlement not success
            if (Component.chkSettlementStatus(transData.getAcquirer().getName())) {
                transEnd(new ActionResult(TransResult.ERR_SETTLE_NOT_COMPLETED, null));
                return;
            }

            gotoState(State.QR_INQUIRY.toString());
        }
    }

    private void onAdjustTip(ActionResult result) {
        //get total amount
        String totalAmountStr = String.valueOf(CurrencyConverter.parse(result.getData().toString()));
        transData.setAmount(totalAmountStr);
        //get tip amount
        String tip = String.valueOf(CurrencyConverter.parse(result.getData1().toString()));
        transData.setTipAmount(tip);
        if (currentMode == SearchMode.SWIPE || currentMode == SearchMode.KEYIN) {
            // enter pin
            gotoState(State.ENTER_PIN.toString());
        }
    }

    private void onEnterPin(ActionResult result) {
        String pinBlock = (String) result.getData();
        transData.setPin(pinBlock);
        if (pinBlock != null && !pinBlock.isEmpty()) {
            transData.setPinVerifyMsg(true);
            transData.setHasPin(true);
        }

        //clss process
        if ((transData.getEnterMode() == EnterMode.CLSS || transData.getEnterMode() == EnterMode.SP200) &&
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

    private void checkOfflineTrans() {
        //get offline trans data list
        List<Acquirer> acqs = new ArrayList<>();
        List<TransData.OfflineStatus> excludes = new ArrayList<>();
        acqs.add(transData.getAcquirer());
        excludes.add(TransData.OfflineStatus.OFFLINE_SENT);
        excludes.add(TransData.OfflineStatus.OFFLINE_VOIDED);
        excludes.add(TransData.OfflineStatus.OFFLINE_ADJUSTED);
        List<TransData> offlineTransList = FinancialApplication.getTransDataDbHelper().findAllOfflineTransData(acqs, excludes);
        //AET-150
        if ((transData.getTransType().equals(ETransType.SALE) &&
                !offlineTransList.isEmpty() && offlineTransList.get(0).getId() != transData.getId())) { //AET-92
            //offline send
            gotoState(State.OFFLINE_SEND.toString());
        } else {
            toSignOrPrint();
        }
    }

    private void goTipBranch() {
        boolean enableTip = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_SUPPORT_TIP);
        //adjust tip
        long totalAmount = Utils.parseLongSafe(transData.getAmount(), 0);
        tipAmount = transData.getTipAmount();
        long lTipAmountLong = Utils.parseLongSafe(tipAmount, 0);
        long baseAmount = totalAmount - lTipAmountLong;
        percent = transData.getIssuer().getAdjustPercent();

        if (enableTip) {
            if (!hasTip)
                gotoState(State.ADJUST_TIP.toString());
            else if (baseAmount * percent / 100 < lTipAmountLong)
                showAdjustTipDialog(getCurrentContext());
            else {
                // enter pin
                gotoState(State.ENTER_PIN.toString());
            }
        } else {
            // Include TEST MODE
            Acquirer acq = FinancialApplication.getAcqManager().getCurAcq();
            if (acq.isEnableUpi() && !acq.isTestMode() && transData.getIssuer() != null &&
                    (Constants.ISSUER_UP.equals(transData.getIssuer().getName()) ||
                            Constants.ISSUER_TBA.equals(transData.getIssuer().getName()))) {
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
                    if (transData.isDccRequired()) {
                        dccGetRateTrans = new TransData(transData);
                        Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_DCC);
                        FinancialApplication.getAcqManager().setCurAcq(acquirer);
                        Component.transInit(dccGetRateTrans, acquirer);
                        dccGetRateTrans.setTransType(ETransType.KBANK_DCC_GET_RATE);

                        byte[] f55 = EmvTags.getF55(emv, transType, false, dccGetRateTrans.getPan());
                        dccGetRateTrans.setSendIccData(Utils.bcd2Str(f55));
                        gotoState(State.DCC_GET_RATE.toString());
                    } else {
                        if (ControlLimitUtils.Companion.isAllowEnterPhoneNumber(transData.getAcquirer().getName())) {
                            gotoState(State.ENTER_PHONE_NUMBER.toString());
                        } else {
                            // online process
                            gotoState(State.MAG_ONLINE.toString());
                        }
                    }
                }
            }
        }
    }

    private boolean checkBelowFloorLimitMag() {
        if (transData.getEnterMode() == EnterMode.SWIPE && Utils.parseLongSafe(transData.getAmount(), 0) < transData.getIssuer().getFloorLimit()) {
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
//            transEnd(new ActionResult(TransResult.SUCC, null));
//            return;
        }
        // if (transData.getIssuer().getName().equals(EKernelType.JCB.name()) && !transData.isOnlineTrans() && transData.isHasPin()){
        if (transData.isPinVerifyMsg() || (!transData.isOnlineTrans() && transData.isHasPin())) {
            transData.setSignFree(true);
            gotoState(State.PRINT.toString());
        } else {
            if ((currentMode == SearchMode.WAVE || currentMode == SearchMode.SP200) && transData.isSignFree()) {// signature free
                gotoState(State.PRINT.toString());
            } else {
                transData.setSignFree(false);
                boolean eSignature = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_E_SIGNATURE);
                if (eSignature && !transData.isTxnSmallAmt()) {
                    gotoState(State.SIGNATURE.toString());
                } else {
                    gotoState(State.PRINT.toString()); // Skip SIGNATURE process
                }
            }
        }
        FinancialApplication.getTransDataDbHelper().updateTransData(transData);
    }

    private void onDccGetRate(ActionResult result) {
        int ret = result.getRet();
        if (ret != TransResult.SUCC) {// do normal sale
            goBackToNormalSale();
        } else {
            transData.setDccAmount(dccGetRateTrans.getDccAmount());
            transData.setDccConversionRate(dccGetRateTrans.getDccConversionRate());
            transData.setDccCurrencyCode(dccGetRateTrans.getDccCurrencyCode());
            transData.setField63Byte(dccGetRateTrans.getField63RecByte());
            gotoState(State.DCC_CONFIRM.toString());
        }
    }

    // LOCAL CURRENCY = FALSE , DCC = TRUE
    private void onDccConfirm(ActionResult result) {
        boolean isDCC = (boolean) result.getData();
        Log.d("DCC", " USER pay by : " + ((isDCC) ? "Foreign currency (DCC)" : "Local currency (THB)"));
        if (!isDCC) {// click cancel, goto normal sale
            goBackToNormalSale();
        } else {
            Component.transInit(transData);
            switch (currentMode) {
                case SearchMode.KEYIN:
                case SearchMode.SWIPE:
                    gotoState(State.MAG_ONLINE.toString());
                    break;
                case SearchMode.INSERT:
                    gotoState(State.EMV_PROC.toString());
                    break;
                case SearchMode.WAVE:
                    gotoState(State.CLSS_PROC.toString());
                    break;
            }
        }
    }

    private void goBackToNormalSale() {
        transData.setDccRequired(false);
        FinancialApplication.getAcqManager().setCurAcq(transData.getAcquirer());
        Component.transInit(transData, transData.getAcquirer());
        switch (currentMode) {
            case SearchMode.KEYIN:
            case SearchMode.SWIPE:
                gotoState(State.MAG_ONLINE.toString());
                break;
            case SearchMode.INSERT:
                gotoState(State.EMV_PROC.toString());
                break;
            case SearchMode.WAVE:
                gotoState(State.CLSS_PROC.toString());
                break;
        }
    }

    private void onEmvReadCard(ActionResult result) {
        int ret = result.getRet();
        if (ret == TransResult.ICC_TRY_AGAIN) {
            cntTryAgain++;
            if (cntTryAgain == 3) {
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
        } else if (ret == TransResult.ERR_NEED_FORWARD_TO_AMEX_API) {
            if (!(MerchantProfileManager.INSTANCE.isMultiMerchantEnable()
                    && MultiMerchantUtils.Companion.getCurrentMerchantName()!=null
                    && MultiMerchantUtils.Companion.getCurrentMerchantName().equals(MultiMerchantUtils.Companion.getMasterProfileName()))) {
                transEnd(new ActionResult(TransResult.ERR_CARD_UNSUPPORTED,null));
                return;
            }

            BaseTrans.setTransRunning(false);
            gotoState(State.AMEX_API.toString());
            return;
        } else if (ret != TransResult.SUCC) {
            transEnd(result);
            return;
        }

        if (isDoubleTransaction(transData)) {
            return;
        }

        if (transData.isDccRequired()) {
            dccGetRateTrans = new TransData(transData);
            TransTypeMapping transMapping = FinancialApplication.getAcqManager().findTransMapping(transData.getTransType(), transData.getIssuer(), TransTypeMapping.SECOND_PRIORITY);
            if (transMapping == null) {
                goBackToNormalSale();
                return;
            }
            Acquirer acquirer = transMapping.getAcquirer();
            FinancialApplication.getAcqManager().setCurAcq(acquirer);
            FinancialApplication.getSysParam().set(SysParam.StringParam.ACQ_NAME, acquirer.getName());
            Component.transInit(dccGetRateTrans, acquirer);
            dccGetRateTrans.setTransType(ETransType.KBANK_DCC_GET_RATE);

            byte[] f55 = EmvTags.getF55(emv, transType, false, dccGetRateTrans.getPan());
            dccGetRateTrans.setSendIccData(Utils.bcd2Str(f55));
            gotoState(State.DCC_GET_RATE.toString());
        } else {
            if (ControlLimitUtils.Companion.isAllowEnterPhoneNumber(transData.getAcquirer().getName())) {
                gotoState(State.ENTER_PHONE_NUMBER.toString());
            } else {
                gotoState(State.EMV_PROC.toString());
            }
        }
    }

    private void afterEMVProcess(ETransResult transResult) {
        EmvTransProcess.emvTransResultProcess(transResult, emv, transData);
        if (transResult == ETransResult.ONLINE_APPROVED) {// 联机批准
            //nothing (handle printing outside)
        } else if (transResult == ETransResult.OFFLINE_APPROVED) {//脱机批准处理
            transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));// Fixed EDCBBLAND-235
            transData.setTraceNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO));
            Component.saveOfflineTransNormalSale(transData);
            //handle printing outside
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
        } */ else if (transResult == ETransResult.SIMPLE_FLOW_END) { // simplify the process
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
        if (transResult.getTransResult() == ETransResult.CLSS_OC_REFER_CONSUMER_DEVICE || transResult.getTransResult() == ETransResult.CLSS_OC_TRY_AGAIN) {
            gotoState(State.CLSS_PREPROC.toString());
            return;
        }

        int transPath = transData.getClssTypeMode();
        if ((transPath == TransactionPath.CLSS_MC_MAG || transPath == TransactionPath.CLSS_MC_MCHIP)
                && transResult.getTransResult() == ETransResult.CLSS_OC_DECLINED) {
            clss.setListener(null);// no memory leak
            //For Mastercard, If transaction is declined, need to prompt msg to perform a contact trans.
            searchCardMode = (byte) (SearchMode.INSERT | SearchMode.SWIPE | SearchMode.KEYIN);//prompt to perform contact transaction.
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
        transData.setPinVerifyMsg(transResult.getCvmResult() == ECvmResult.OFFLINE_PIN || transResult.getCvmResult() == ECvmResult.ONLINE_PIN || transResult.getCvmResult() == ECvmResult.ONLINE_PIN_SIG);

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

        // if transResult is not matched with conditions above, will assume that coming from CLSS_READ_CARD PROCESS
        if (transData.isDccRequired()) {
            dccGetRateTrans = new TransData(transData);
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_DCC);
            FinancialApplication.getAcqManager().setCurAcq(acquirer);
            Component.transInit(dccGetRateTrans, acquirer);
            dccGetRateTrans.setTransType(ETransType.KBANK_DCC_GET_RATE);

            byte[] f55 = EmvTags.getF55(emv, transType, false, dccGetRateTrans.getPan());
            dccGetRateTrans.setSendIccData(Utils.bcd2Str(f55));
            gotoState(State.DCC_GET_RATE.toString());
        } else {
            if (ControlLimitUtils.Companion.isAllowEnterPhoneNumber(transData.getAcquirer().getName())) {
                gotoState(State.ENTER_PHONE_NUMBER.toString());
            } else {
                gotoState(State.CLSS_PROC.toString());
            }
        }
    }

    private void processTcAdvice() {
        if (transData.getAcquirer().isEmvTcAdvice() && transData.getTransType() == ETransType.SALE && transData.getEnterMode() != EnterMode.SP200) {
            gotoState(State.TC_ADVICE_SEND.toString());
        } else {
            checkOfflineTrans();
        }
    }

    private void showAdjustTipDialog(final Context context) {

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
                gotoState(State.ADJUST_TIP.toString());
            }
        });
        dialog.show();
        dialog.setNormalText(getString(R.string.prompt_tip_exceed));
        dialog.showCancelButton(true);
        dialog.showConfirmButton(true);
    }

    private void afterScanCode(ActionResult result) {
        // 扫码
        String qrCode = (String) result.getData();
        if (qrCode == null || qrCode.length() == 0) {
            transEnd(new ActionResult(TransResult.ERR_INVALID_EMV_QR, null));
            return;
        }
        EmvQr emvQr = EmvQr.decodeEmvQr(transData, qrCode);
        if (emvQr == null) {
            transEnd(new ActionResult(TransResult.ERR_INVALID_EMV_QR, null));
            return;
        }
        if (!emvQr.isUpiAid()) {
            transEnd(new ActionResult(TransResult.ERR_NOT_SUPPORT_TRANS, null));
            return;
        }

        saveQrInfo(emvQr);
        gotoState(State.MAG_ONLINE.toString());
    }

    private void saveQrInfo(EmvQr emvQr) {
        Issuer matchedIssuer = FinancialApplication.getAcqManager().findIssuerByPan(emvQr.getPan());
        if (matchedIssuer == null || !FinancialApplication.getAcqManager().isIssuerSupported(matchedIssuer)) {
            transEnd(new ActionResult(TransResult.ERR_CARD_UNSUPPORTED, null));
            return;
        }

        if (!Issuer.validPan(matchedIssuer, emvQr.getPan())) {
            transEnd(new ActionResult(TransResult.ERR_CARD_INVALID, null));
            return;
        }

        if (!Issuer.validCardExpiry(matchedIssuer, emvQr.getExpireDate())) {
            transEnd(new ActionResult(TransResult.ERR_CARD_EXPIRED, null));
            return;
        }
        transData.setIssuer(matchedIssuer);
        transData.setCardSerialNo(emvQr.getCardSeqNum());
        transData.setSendIccData(emvQr.getIccData());
        transData.setPan(emvQr.getPan());
        transData.setExpDate(emvQr.getExpireDate());
        transData.setTrack2(emvQr.getTrackData());
        transData.setEnterMode(TransData.EnterMode.QR);
        transData.setSignFree(true);
        transData.setPinFree(true);
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
                gotoState(State.CHECK_CARD.toString());
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
                dialog.dismiss();
            }
        });
    }

    private void showMsgDialog(final Context context, final String msg) {

        final CustomAlertDialog dialog = new CustomAlertDialog(context, CustomAlertDialog.NORMAL_TYPE);
        dialog.setCancelClickListener(new CustomAlertDialog.OnCustomClickListener() {
            @Override
            public void onClick(CustomAlertDialog alertDialog) {
                dialog.dismiss();
                transEnd(new ActionResult(TransResult.ERR_USER_CANCEL, null));
            }
        });
        dialog.setConfirmClickListener(new CustomAlertDialog.OnCustomClickListener() {
            @Override
            public void onClick(CustomAlertDialog alertDialog) {
                dialog.dismiss();
                gotoState(State.CHECK_CARD.toString());
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
                dialog.dismiss();
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

    private void initTransDataQr(ActionResult result) {
        transData.setAmount(result.getData().toString());
        AcqManager acqManager = FinancialApplication.getAcqManager();
        Acquirer acquirer = acqManager.findAcquirer(Constants.ACQ_WALLET);
        Issuer issuer = acqManager.findIssuer(Constants.ISSUER_WALLET);
        transData.setBatchNo(acquirer.getCurrBatchNo());
        // 冲正原因
        transData.setTransState(TransData.ETransStatus.NORMAL);
        transData.setTpdu("600" + acquirer.getNii() + "0000");
        transData.setAcquirer(acquirer);
        transData.setIssuer(issuer);
        transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
        transData.setWalletRetryStatus(TransData.WalletRetryStatus.NORMAL);
    }

    private void toCheckQrOrPrint(ActionResult result) {
//        transData = (TransData) result.getData();
        if (result.getRet() == TransResult.SUCC) {
            Component.initField63Wallet(transData);
            transData.setSignFree(true);
            FinancialApplication.getTransDataDbHelper().updateTransData(transData);

            //ECRProcReturn(null, new ActionResult(TransResult.SUCC, null));

            PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(SaleTrans.this, State.PRINT.toString()));
            bind(State.PRINT.toString(), printTask);
            gotoState(State.PRINT.toString());
        } else {
            if (result.getRet() == TransResult.ERR_WALLET_RESP_UK) {
                gotoState(State.QR_CHECKQR.toString());
            } else {
                transEnd(result);
                return;
            }

        }
    }

    @Override
    protected void transEnd(final ActionResult result) {
        searchCardMode = orgSearchCardMode;
        emv.setListener(null);//no memory leak
        clss.setListener(null);//no memory leak
        super.transEnd(result);
    }

    enum State {
        ENTER_AMOUNT,
        ENTER_REF1_REF2,
        CHECK_CARD,
        ENTER_PHONE_NUMBER,
        CHECK_AMEX_AID,
        SCAN_CODE,
        ADJUST_TIP,
        ENTER_PIN,
        EMV_READ_CARD,
        DCC_GET_RATE,
        DCC_CONFIRM,
        MAG_ONLINE,
        EMV_PROC,
        ENTER_APPR_CODE,
        CLSS_PREPROC,
        CLSS_READ_CARD,
        CLSS_PROC,
        SIGNATURE,
        USER_AGREEMENT,
        OFFLINE_SEND,
        TC_ADVICE_SEND,
        QR_INQUIRY,
        QR_CHECKQR,
        PRINT,
        WALLET_TRAN,
        AMEX_API,
    }
}
