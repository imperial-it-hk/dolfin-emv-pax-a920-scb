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
import android.util.Log;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.abl.utils.EncUtils;
import com.pax.abl.utils.PanUtils;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.utils.Tools;
import com.pax.pay.ECR.EcrData;
import com.pax.pay.ECR.LawsonHyperCommClass;
import com.pax.pay.ECR.LemonFarmHyperCommClass;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.MerchantAcqProfile;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.action.ActionDispTransDetail;
import com.pax.pay.trans.action.ActionInputPassword;
import com.pax.pay.trans.action.ActionInputTransData;
import com.pax.pay.trans.action.ActionInputTransData.EInputType;
import com.pax.pay.trans.action.ActionOfflineSend;
import com.pax.pay.trans.action.ActionQrSaleInquiry;
import com.pax.pay.trans.action.ActionSignature;
import com.pax.pay.trans.action.ActionTransOnline;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.AcqManager;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.MerchantProfileManager;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.model.TransData.ETransStatus;
import com.pax.pay.trans.model.TransMultiAppData;
import com.pax.pay.trans.task.PrintTask;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.TimeConverter;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;
import com.pax.view.dialog.DialogUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import th.co.bkkps.amexapi.AmexTransAPI;
import th.co.bkkps.amexapi.action.ActionAmexVoidTrans;
import th.co.bkkps.dofinAPI.tran.action.ActionDolfinGetConfig;
import th.co.bkkps.dofinAPI.tran.action.ActionDolfinSetConfig;
import th.co.bkkps.dofinAPI.tran.action.ActionDolfinVoid;
import th.co.bkkps.scbapi.trans.action.ActionScbVoidTrans;

public class SaleVoidTrans extends BaseTrans {

    private TransData origTransData;
    private TransMultiAppData multiAppLastTrans;
    private long origTransNo = -1;
    private long lastTransNo = -1;
    private List<Acquirer> supportAcquirers;
    private boolean isMyPrompt;
    private boolean isKplus;
    private boolean isAliPay;
    private boolean isWchatPay;
    private boolean isQRCredit;
    private boolean isRedeem;
    private boolean isSmrtpay;
    private boolean isDolfin;
    private boolean isDolfinIpp;
    private boolean isScbIpp;
    private boolean isScbRedeem;
    private boolean isAmex;
    boolean isVoidErr = false;
    private String respMsg = null;
    private String swapMerchantName = null;

    /**
     * whether need to read the original trans data or not
     */
    private boolean isNeedFindOrigTrans = true;
    /**
     * whether need to input trans no. or not
     */
    private boolean isNeedInputTransNo = true;

    private AAction.ActionEndListener mVoidOnlineProcEndListener = null;

    public SaleVoidTrans(Context context, TransEndListener transListener) {
        super(context, ETransType.VOID, transListener);
        isNeedFindOrigTrans = true;
        isNeedInputTransNo = true;
    }

    public SaleVoidTrans(Context context, TransData origTransData, TransEndListener transListener) {
        super(context, ETransType.VOID, transListener);
        this.origTransData = origTransData;
        isNeedFindOrigTrans = false;
        isNeedInputTransNo = false;
    }

    public SaleVoidTrans(Context context, long origTransNo, TransEndListener transListener) {
        super(context, ETransType.VOID, transListener);
        this.origTransNo = origTransNo;
        isNeedFindOrigTrans = true;
        isNeedInputTransNo = false;
    }

    public void setVoidOnlineProcEndListener(AAction.ActionEndListener listener) {
        mVoidOnlineProcEndListener = listener;
    }


    @Override
    protected void bindStateOnAction() {

        ActionInputPassword inputPasswordAction = new ActionInputPassword(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionInputPassword) action).setParam(getCurrentContext(), 6,
                        getString(R.string.prompt_void_pwd), null);
            }
        });
        bind(State.INPUT_PWD.toString(), inputPasswordAction);

        ActionInputTransData enterTransNoAction = new ActionInputTransData(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                String promptMsg = getString(R.string.prompt_input_transno);
                if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_VOID_WITH_STAND)) {
                    promptMsg = getString(R.string.prompt_input_stanno);
                }
                ((ActionInputTransData) action).setParam(getCurrentContext(), getString(R.string.menu_void))
                        .setInputLine(promptMsg, EInputType.NUM, 6, true);
            }
        });
        bind(State.ENTER_TRANSNO.toString(), enterTransNoAction, true);

        // confirm information
        ActionDispTransDetail confirmInfoAction = new ActionDispTransDetail(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                LinkedHashMap<String, String> map = new LinkedHashMap<>();
                String title;
                if (isKplus || isMyPrompt) {
                    mapWalletDetails(map);
                    title = getString(R.string.trans_kplus_void);
                } else if (isAliPay) {
                    mapWalletDetails(map);
                    title = getString(R.string.trans_alipay_void);
                } else if (isWchatPay) {
                    mapWalletDetails(map);
                    title = getString(R.string.trans_wechat_void);
                } else if (isQRCredit) {
                    mapWalletDetails(map);
                    title = getString(R.string.trans_qr_credit_void);
                } else if (isRedeem) {
                    mapRedeemedDetails(map);
                    title = getString(R.string.menu_redeem_void);
                } else if (isSmrtpay) {
                    mapInstalmentDetails(map);
                    title = getString(R.string.menu_instalment_void);
                } else if (isDolfinIpp) {
                    mapDolfinInstalmentDetails(map);
                    title = getString(R.string.menu_dolfin_instalment_void);
                }
                else {
                    mapCreditDetails(map);
                    title = getString(R.string.menu_void);
                }

                ((ActionDispTransDetail) action).setParam(getCurrentContext(), title, map, FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_LINKPOS_BYPASS_CONFIRM_VOID,false));
            }
        });
        bind(State.TRANS_DETAIL.toString(), confirmInfoAction, true);

        // online action
        ActionTransOnline transOnlineAction = new ActionTransOnline(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionTransOnline) action).setParam(getCurrentContext(), transData);
            }
        });

        bind(State.MAG_ONLINE.toString(), transOnlineAction, true);

        ActionQrSaleInquiry qrSaleInquiry = new ActionQrSaleInquiry(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionQrSaleInquiry) action).setParam(getCurrentContext(),
                        getString(R.string.trans_wallet_void), transData);
            }
        });
        bind(State.INQUIRY.toString(), qrSaleInquiry, false);

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
        PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(SaleVoidTrans.this, State.PRINT.toString()));
        bind(State.PRINT.toString(), printTask);

        ActionDolfinVoid actionDolfinVoid = new ActionDolfinVoid(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionDolfinVoid) action).setParam(getCurrentContext(), transData);
            }
        });

        bind(State.DOLFIN_VOID.toString(), actionDolfinVoid, true);
        ActionDolfinSetConfig configAction = new ActionDolfinSetConfig(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionDolfinSetConfig) action).setParam(getCurrentContext());
            }
        });

        bind(State.DOLFIN_SENT_CONFIG.toString(), configAction, true);

        ActionDolfinGetConfig getConfig = new ActionDolfinGetConfig(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionDolfinGetConfig) action).setParam(getCurrentContext(), transData, isVoidErr);
            }
        });
        bind(State.DOLFIN_GET_CONFIG.toString(), getConfig, true);

        ActionScbVoidTrans actionScbVoidTrans = new ActionScbVoidTrans(
                action -> ((ActionScbVoidTrans) action).setParam(getCurrentContext(), origTransData)
        );
        bind(State.SCB_VOID.toString(), actionScbVoidTrans, false);

        ActionAmexVoidTrans actionAmexVoidTrans = new ActionAmexVoidTrans(action -> ((ActionAmexVoidTrans) action).setParam(getCurrentContext(), origTransNo, lastTransNo, mECRrocReturnListener));
        bind(State.AMEX_API.toString(), actionAmexVoidTrans, false);

        // ERM Maximum TransExceed Check
        int ermExeccededResult = ErmLimitExceedCheck();
        if (ermExeccededResult != TransResult.SUCC) {
            transEnd(new ActionResult(ermExeccededResult,null));
            return;
        }

        this.getSupportAcquirers();// init default acquirers for void trans.

        // whether void trans need to input password or not
        if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.OTHTC_VERIFY) && isNeedInputTransNo) {
            gotoState(State.INPUT_PWD.toString());

        } else if (isNeedInputTransNo) {// need to input trans no.
            gotoState(State.ENTER_TRANSNO.toString());
        } else {// not need to input trans no.
            if (isNeedFindOrigTrans) {
                multiAppLastTrans = FinancialApplication.getTransMultiAppDataDbHelper().findLastTransData();
                validateOrigTransData(origTransNo);
            } else { // not need to read trans data
                copyOrigTransData();
                checkCardAndPin();
            }
        }
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
        State state = State.valueOf(currentState);

        switch (state) {
            case INPUT_PWD:
                if (result.getRet() != TransResult.SUCC) {
                    EcrData.instance.isOnHomeScreen = true;
                    transEnd(result);
                }
                else {
                    onInputPwd(result);
                }
                break;
            case ENTER_TRANSNO:
                onEnterTraceNo(result);
                break;
            case TRANS_DETAIL:
                if (isDolfin) {
                    gotoState(State.DOLFIN_SENT_CONFIG.toString());
                }
                if (isWchatPay || isAliPay || isKplus || isQRCredit || isMyPrompt) {
                    gotoState(State.INQUIRY.toString());
                } else {
                    if (!checkIsOfflineSent()) {
                        ECRProcReturn(null, result);
                        toSignOrPrint();
                    } else {
                        gotoState(State.MAG_ONLINE.toString()); // online
                    }
                }
                break;
            case MAG_ONLINE: //  subsequent processing of online
                ETransStatus origTransState = origTransData.getTransState();
                // update original trans data
                origTransData.setVoidStanNo(transData.getStanNo());
                origTransData.setDateTime(transData.getDateTime());
                origTransData.setTransState(ETransStatus.VOIDED);
                origTransData.setOfflineSendState(transData.getOfflineSendState());
                if (isRedeem) {
                    origTransData.setField63RecByte(transData.getField63RecByte());
                }

                String authCode = transData.getAuthCode() != null ? transData.getAuthCode() : transData.getOrigAuthCode();
                origTransData.setAuthCode(authCode);

                transData.setAuthCode(authCode);
                if (isSmrtpay || isDolfinIpp) {
                    transData.setField61RecByte(origTransData.getField61RecByte());
                    transData.setField63RecByte(origTransData.getField63RecByte());
                }

                FinancialApplication.getTransDataDbHelper().updateTransData(origTransData);
                FinancialApplication.getTransDataDbHelper().updateTransData(transData);

                //gotoState(State.SIGNATURE.toString()); //Skip Signature actions
//                toSignOrPrint();

                ECRProcReturn(null, result);

                if (origTransState == ETransStatus.ADJUSTED) {
                    TransData adjustedTran = FinancialApplication.getTransDataDbHelper().findAdjustedTransDataByTraceNo(
                            origTransData.getTraceNo(), origTransData.getAcquirer());
                    if (adjustedTran != null) {
                        adjustedTran.setOfflineSendState(TransData.OfflineStatus.OFFLINE_VOIDED);
                        FinancialApplication.getTransDataDbHelper().updateTransData(adjustedTran);
                    }
                }

                checkOfflineTrans();

                break;
            case INQUIRY:
//                transData = (TransData) result.getData();
                if (result.getRet() == TransResult.SUCC || result.getRet() == TransResult.ERR_ADVICE) {
                    updateStateTransDataWallet();
                    ECRProcReturn(null, result);
                } else {
                    transEnd(result);
                }
                break;
            case SIGNATURE:
                onSignature(result);
                break;
            case OFFLINE_SEND:
                toSignOrPrint();
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
            case DOLFIN_SENT_CONFIG:
                if ((int) result.getData() != 0) {
                    ECRProcReturn(null, new ActionResult((int) result.getData(), null));
                    respMsg = (String) result.getData1() != null ? (String) result.getData1() : null;
                    showRespMsg(result, respMsg);
                    return;
                }
                gotoState(State.DOLFIN_VOID.toString());
                break;
            case DOLFIN_VOID:
                int ret = (int) result.getData();
                ECRProcReturn(null, new ActionResult(ret, null));
                if (ret == 0 || ret == 11) {
                    // update original trans data
                    origTransData.setVoidStanNo(transData.getStanNo());
                    origTransData.setOrigDateTime(origTransData.getDateTime());
                    origTransData.setDateTime(transData.getDateTime());
                    origTransData.setTransState(TransData.ETransStatus.VOIDED);
                    origTransData.setOfflineSendState(transData.getOfflineSendState());
                    isVoidErr = false;
                    FinancialApplication.getTransDataDbHelper().updateTransData(origTransData);
                    transEnd(result);
                } else {
                    respMsg = (String) result.getData1() != null ? (String) result.getData1() : null;
                    isVoidErr = true;
                }
                gotoState(State.DOLFIN_GET_CONFIG.toString());
                break;
            case DOLFIN_GET_CONFIG:
                if (isVoidErr) {
                    showRespMsg(result, respMsg);
                    return;
                }
                transEnd(result);
                break;
            case SCB_VOID:
                transEnd(new ActionResult(TransResult.ERR_ABORTED, null));//no alert dialog
                break;
            case AMEX_API:
                if (result.getRet() == TransResult.SUCC) {
                    transEnd(result);
                } else if (result.getRet() == TransResult.ERR_ABORTED && origTransNo == -1) { // enter without trans no.
                    if (lastTransNo > 0) {
                        validateOrigTransData(lastTransNo);
                    } else {
                        transEnd(new ActionResult(TransResult.ERR_NO_ORIG_TRANS, null));
                    }
                } else if (result.getRet() == TransResult.ERR_ABORTED && origTransData == null) {
                    transEnd(new ActionResult(TransResult.ERR_NO_ORIG_TRANS, null));
                } else {
                    transEnd(new ActionResult(TransResult.ERR_ABORTED, null));
                }
                break;
            default:
                transEnd(result);
                break;
        }

    }

    @Override
    protected void ECRProcReturn(AAction action, ActionResult result) {
        if (transData.isEcrProcess() && FinancialApplication.getEcrProcess().mHyperComm instanceof LawsonHyperCommClass) {
            if (transData != null) {
                if (transData.getAcquirer() != null) {
                    if (!transData.getAcquirer().isEnableQR()) {
                        // set Signature Data
                        EcrData.instance.signatureImgData = (transData.getSignData() != null) ? ((transData.getSignData().length > 0) ? transData.getSignData() : new byte[]{}) : new byte[]{};
                        // set AID
                        EcrData.instance.kioskPos_AID = Utils.checkStringContainNullOrEmpty(transData.getAid(), Utils.LengthValidatorMode.EQUALS, 7, true, " ").getBytes();
                        // set TVR
                        EcrData.instance.kioskPos_TVR = Utils.checkStringContainNullOrEmpty(transData.getTvr(), Utils.LengthValidatorMode.EQUALS, 5, true, " ").getBytes();
                        // set TSI
                        EcrData.instance.kioskPos_TSI = Utils.checkStringContainNullOrEmpty(transData.getTsi(), Utils.LengthValidatorMode.EQUALS, 2, true, " ").getBytes();

                        // set PrintSignatureBox
                        EcrData.instance.kioskPos_PrintSignatureBox = (transData.getSignData() == null && transData.isPinVerifyMsg() && transData.isTxnSmallAmt()) ? new byte[]{0x31} : new byte[]{0x30};
                    } else {
                        // set Signature Data
                        EcrData.instance.signatureImgData = new byte[0];
                        // set AID
                        EcrData.instance.kioskPos_AID = new byte[0];
                        // set TVR
                        EcrData.instance.kioskPos_TVR = new byte[0];
                        // set TSI
                        EcrData.instance.kioskPos_TSI = new byte[0];

                        // set PrintSignatureBox
                        EcrData.instance.kioskPos_PrintSignatureBox = new byte[]{0x30};
                    }
                    // set SaleReferenceID
                    EcrData.instance.saleReferenceIDR0 = Utils.checkStringContainNullOrEmpty(transData.getReferenceSaleID()).getBytes();
                    //EcrData.instance.RefNo = Utils.checkStringContainNullOrEmpty(transData.getRefNo()).getBytes();
                    // set HostIndex
                    String hostIndex = LawsonHyperCommClass.getLawsonHostIndex(transData.getAcquirer());
                    EcrData.instance.hostIndex = Utils.checkStringContainNullOrEmpty(hostIndex).getBytes();
                }
            }
        }
        super.ECRProcReturn(action, result);
    }

    private void onInputPwd(ActionResult result) {
        String data = EncUtils.sha1((String) result.getData());
        if (!data.equals(FinancialApplication.getSysParam().get(SysParam.StringParam.SEC_VOID_PWD))) {
            EcrData.instance.isOnHomeScreen = true;
            transEnd(new ActionResult(TransResult.ERR_PASSWORD, null));
            return;
        }

        if (isNeedInputTransNo) {// need to input trans no.
            gotoState(State.ENTER_TRANSNO.toString());
        } else {// not need to input trans no.
            if (isNeedFindOrigTrans) {
                multiAppLastTrans = FinancialApplication.getTransMultiAppDataDbHelper().findLastTransData();
                validateOrigTransData(origTransNo);
            } else { // not need to read trans data
                copyOrigTransData();
                checkCardAndPin();
            }
        }
    }

    private void onEnterTraceNo(ActionResult result) {
        String content = (String) result.getData();
        multiAppLastTrans = FinancialApplication.getTransMultiAppDataDbHelper().findLastTransData();
        long transNo = -1;
        if (content == null) {
            TransData transData = FinancialApplication.getTransDataDbHelper().findLastTransDataByAcqs(supportAcquirers);
            if (transData != null) {
                transNo = transData.getTraceNo();
                if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_VOID_WITH_STAND)) {
                    transNo = transData.getStanNo();
                }
            }

            if (multiAppLastTrans != null) {
                if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_VOID_WITH_STAND)) {
                    if (transNo < multiAppLastTrans.getStanNo()) {
                        goToVoidTransByMultiApp(multiAppLastTrans, multiAppLastTrans.getStanNo());
                        return;
                    }
                } else {
                    if (transNo < multiAppLastTrans.getTraceNo()) {
                        goToVoidTransByMultiApp(multiAppLastTrans, multiAppLastTrans.getTraceNo());
                        return;
                    }
                }
            }

//            if (AmexTransAPI.getInstance().getProcess().isAppInstalled(context)) {
//                this.origTransNo = -1;
//                this.lastTransNo = transNo;
//                BaseTrans.setTransRunning(false);
//                gotoState(State.AMEX_API.toString());
//                return;
//            }
        } else {
            transNo = Utils.parseLongSafe(content, -1);
            this.origTransNo = transNo;
        }
        validateOrigTransData(transNo);
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
        //if (isKplus||isAliPay||isWchatPay) transData = origTransData;
        gotoState(State.PRINT.toString());
    }

    // check original trans data
    private void validateOrigTransData(long origTransNo) {
        if (origTransNo <= 0) {
            transEnd(new ActionResult(TransResult.ERR_NO_ORIG_TRANS, null));
            return;
        }

        List<ETransType> excludeTrans = Arrays.asList(ETransType.PREAUTH, ETransType.PREAUTHORIZATION, ETransType.PREAUTHORIZATION_CANCELLATION);
        if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_VOID_WITH_STAND)) {
            origTransData = FinancialApplication.getTransDataDbHelper().findTransDataByStanNoAndAcqs(origTransNo, supportAcquirers, excludeTrans);
        } else {
            origTransData = FinancialApplication.getTransDataDbHelper().findTransDataByTraceNoAndAcqs(origTransNo, supportAcquirers, excludeTrans);
        }

        if (origTransData == null) {
            // trans not exist
            TransMultiAppData transMultiAppData;
            if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_VOID_WITH_STAND)) {
                transMultiAppData = FinancialApplication.getTransMultiAppDataDbHelper().findTransDataByStanNo(origTransNo);
            } else {
                transMultiAppData = FinancialApplication.getTransMultiAppDataDbHelper().findTransDataByTraceNo(origTransNo);
            }

            if (transMultiAppData != null) {
                goToVoidTransByMultiApp(transMultiAppData, origTransNo);
            } else {
                transEnd(new ActionResult(TransResult.ERR_NO_ORIG_TRANS, null));
            }
            return;
//            if (AmexTransAPI.getInstance().getProcess().isAppInstalled(context)) {
//                this.origTransNo = origTransNo;
//                BaseTrans.setTransRunning(false);
//                gotoState(State.AMEX_API.toString());
//            }
//            else {
//                transEnd(new ActionResult(TransResult.ERR_NO_ORIG_TRANS, null));
//            }
//            return;
        }

        if (transData.isEcrProcess() && FinancialApplication.getEcrProcess().mHyperComm instanceof LemonFarmHyperCommClass) {
            if (origTransData.getReferenceSaleID() != null) {
                if (!origTransData.getReferenceSaleID().isEmpty()) {
                    EcrData.instance.transAmount = origTransData.getAmount().getBytes();
                }
            }
        } else if (transData.isEcrProcess() && FinancialApplication.getEcrProcess().mHyperComm instanceof LawsonHyperCommClass) {
            if (origTransData.getReferenceSaleID() != null) {
                if (!origTransData.getReferenceSaleID().isEmpty()) {
                    EcrData.instance.transAmount = origTransData.getAmount().getBytes();
                    if (EcrData.instance.saleReferenceIDR0.length == origTransData.getReferenceSaleID().length()) {
                        transData.setReferenceSaleID(origTransData.getReferenceSaleID());
                        EcrData.instance.saleReferenceIDR0 = origTransData.getReferenceSaleID().getBytes();
                    } else {
                        if (EcrData.instance.saleReferenceIDR0.length < origTransData.getReferenceSaleID().length()) {
                            Log.d(TAG, "ReferenceSaleID len is less than define buffer");
                        } else if (EcrData.instance.saleReferenceIDR0.length > origTransData.getReferenceSaleID().length()) {
                            Log.d(TAG, "ReferenceSaleID len is greater than define buffer");
                        }
                    }
                }
            }
        }

        if (isSettleFail()) {
            // Last settlement not success, need to settle firstly
            transEnd(new ActionResult(TransResult.ERR_SETTLE_NOT_COMPLETED, null));
            return;
        }

        // for multi merchant use void across merchant profile
        if (MerchantProfileManager.INSTANCE.isMultiMerchantEnable()) {
            if (!origTransData.getMerchantName().equals(MerchantProfileManager.INSTANCE.getCurrentMerchant())) {
                swapMerchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
                MerchantProfileManager.INSTANCE.applyProfileAndSave(origTransData.getMerchantName());

                String txnMercName = origTransData.getMerchantName();
                String txnAcqName = origTransData.getAcquirer().getName();
                if(txnMercName!=null && txnAcqName!=null) {
                    MerchantAcqProfile mercAcqProfile = MerchantProfileManager.INSTANCE.getSpecificAcquirerFromMerchantName(txnMercName, txnAcqName);
                    if (mercAcqProfile!=null) {
                        origTransData.getAcquirer().setTerminalId(mercAcqProfile.getTerminalId());
                        origTransData.getAcquirer().setMerchantId(mercAcqProfile.getMerchantId());
                        origTransData.getAcquirer().setCurrBatchNo(mercAcqProfile.getCurrBatchNo());
                    }
                }
            }
        }

        ETransType trType = origTransData.getTransType();

        // only sale and refund trans can be revoked
        // AET-101 AET-139
        boolean isOfflineSent = trType == ETransType.OFFLINE_TRANS_SEND &&
                origTransData.getOfflineSendState() != null &&
                origTransData.getOfflineSendState() == TransData.OfflineStatus.OFFLINE_SENT;
        boolean isAdjustedNotSent = origTransData.getTransState() == ETransStatus.ADJUSTED &&
                origTransData.getOfflineSendState() != null &&
                origTransData.getOfflineSendState() == TransData.OfflineStatus.OFFLINE_NOT_SENT;
        isMyPrompt = origTransData.getAcquirer() != null && Constants.ACQ_MY_PROMPT.equals(origTransData.getAcquirer().getName());
        isKplus = origTransData.getAcquirer() != null && Constants.ACQ_KPLUS.equals(origTransData.getAcquirer().getName())
                && origTransData.getTransType() != ETransType.QR_MYPROMPT_SALE && origTransData.getTransType() != ETransType.QR_MYPROMPT_VOID;
        isAliPay = origTransData.getAcquirer() != null &&
                    (Constants.ACQ_ALIPAY.equals(origTransData.getAcquirer().getName())
                    || Constants.ACQ_ALIPAY_B_SCAN_C.equals(origTransData.getAcquirer().getName()));
        isWchatPay = origTransData.getAcquirer() != null &&
                        (Constants.ACQ_WECHAT.equals(origTransData.getAcquirer().getName())
                        || Constants.ACQ_WECHAT_B_SCAN_C.equals(origTransData.getAcquirer().getName()));
        isQRCredit = origTransData.getAcquirer() != null && Constants.ACQ_QR_CREDIT.equals(origTransData.getAcquirer().getName());
        isRedeem = origTransData.getAcquirer() != null && (Constants.ACQ_REDEEM.equals(origTransData.getAcquirer().getName()) ||
                Constants.ACQ_REDEEM_BDMS.equals(origTransData.getAcquirer().getName()));
        isSmrtpay = origTransData.getAcquirer() != null && (Constants.ACQ_SMRTPAY.equals(origTransData.getAcquirer().getName()) ||
                Constants.ACQ_SMRTPAY_BDMS.equals(origTransData.getAcquirer().getName()));
        isDolfin = origTransData.getAcquirer() != null && Constants.ACQ_DOLFIN.equals(origTransData.getAcquirer().getName());
        isScbIpp = origTransData.getAcquirer() != null && Constants.ACQ_SCB_IPP.equals(origTransData.getAcquirer().getName());
        isScbRedeem = origTransData.getAcquirer()!=null && Constants.ACQ_SCB_REDEEM.equals(origTransData.getAcquirer().getName());
        isAmex = origTransData.getAcquirer()!=null && Constants.ACQ_AMEX.equals(origTransData.getAcquirer().getName());
        isDolfinIpp = origTransData.getAcquirer() != null && Constants.ACQ_DOLFIN_INSTALMENT.equals(origTransData.getAcquirer().getName());

        TransData.ETransStatus trStatus = origTransData.getTransState();
        // void trans can not be revoked again
        if (trStatus.equals(ETransStatus.VOIDED)
                || trType == ETransType.VOID
                || trType == ETransType.QR_VOID_ALIPAY
                || trType == ETransType.QR_VOID_WECHAT
                || trType == ETransType.QR_VOID_KPLUS
                || trType == ETransType.QR_VOID_CREDIT
                || trType == ETransType.KBANK_SMART_PAY_VOID
                || trType == ETransType.KBANK_REDEEM_VOID
                || trType == ETransType.DOLFIN_INSTALMENT_VOID) {
            transEnd(new ActionResult(TransResult.ERR_HAS_VOIDED, null));
            return;
        }

        if (isMyPrompt) transData.setTransType(ETransType.QR_MYPROMPT_VOID);
        if (isKplus) transData.setTransType(ETransType.QR_VOID_KPLUS);
        if (isAliPay) transData.setTransType(ETransType.QR_VOID_ALIPAY);
        if (isWchatPay) transData.setTransType(ETransType.QR_VOID_WECHAT);
        if (isQRCredit) transData.setTransType(ETransType.QR_VOID_CREDIT);

        if (isKplus || isAliPay || isWchatPay || isQRCredit || isMyPrompt) {
            Component.copyOrigTransDataWallet(transData, origTransData);
            transData.setRefNo(origTransData.getRefNo());
            transData.setSaleReference1(origTransData.getSaleReference1());
            transData.setSaleReference2(origTransData.getSaleReference2());
        } else if (isRedeem) {
            setTransType(ETransType.KBANK_REDEEM_VOID);
            transData.setTransType(transType);
            copyOrigTransData();
        } else if (isSmrtpay) {
            setTransType(ETransType.KBANK_SMART_PAY_VOID);
            transData.setTransType(transType);
            copyOrigTransData();
        } else if (isDolfinIpp) {
            setTransType(ETransType.DOLFIN_INSTALMENT_VOID);
            transData.setTransType(transType);
            copyOrigTransData();
        }
        else if (isDolfin) {
            copyOrigTransData();
            gotoState(State.DOLFIN_SENT_CONFIG.toString());
            return;
        } else {
            copyOrigTransData();
        }

        if (isScbIpp || isScbRedeem) {
            BaseTrans.setTransRunning(false);
            gotoState(State.SCB_VOID.toString());
            return;
        } else if (isAmex) {
            BaseTrans.setTransRunning(false);
            gotoState(State.AMEX_API.toString());
            return;
        }
        gotoState(State.TRANS_DETAIL.toString());
    }

    // set original trans data
    private void copyOrigTransData() {
        //EDCBBLAND-437 Void transaction send incorrect TPDU. and NII
        Acquirer acquirer = origTransData.getAcquirer();
        FinancialApplication.getAcqManager().setCurAcq(acquirer);
        Component.transInit(transData, acquirer);

        transData.setAmount(origTransData.getAmount());
        transData.setOrigBatchNo(origTransData.getBatchNo());
        transData.setOrigAuthCode(origTransData.getAuthCode());
        transData.setOrigRefNo(origTransData.getRefNo());
        transData.setOrigTransNo(origTransData.getTraceNo());
        transData.setPan(origTransData.getPan());
        transData.setExpDate(origTransData.getExpDate());
        transData.setAcquirer(acquirer);
        transData.setIssuer(origTransData.getIssuer());
        transData.setCardSerialNo(origTransData.getCardSerialNo());
        transData.setSendIccData(origTransData.getSendIccData());
        transData.setOrigTransType(origTransData.getTransType());
        transData.setEnterMode(origTransData.getEnterMode());
        transData.setAid(origTransData.getAid());
        transData.setTvr(origTransData.getTvr());
        transData.setTc(origTransData.getTc());
        transData.setEmvAppLabel(origTransData.getEmvAppLabel());
        transData.setTraceNo(origTransData.getTraceNo());
        transData.setTxnSmallAmt(origTransData.isTxnSmallAmt());//EDCBBLAND-426 support small amount
        transData.setNumSlipSmallAmt(origTransData.getNumSlipSmallAmt());//EDCBBLAND-426 support small amount
        transData.setPinVerifyMsg(origTransData.isPinVerifyMsg());//EDCBBLAND-467 show pin verify msg on receipt if original trans. have Offline/Online PIN.
        transData.setSignFree(origTransData.isSignFree());//EDCBBLAND-467 remove signature part on receipt if original trans. have Offline/Online PIN.
        transData.setDateTime(origTransData.getDateTime());//EDCBBLAND-604 Fix issue send incorrect datetime
        transData.setOrigDateTime(origTransData.getDateTime());
        transData.setRefNo(origTransData.getRefNo());
        transData.setTrack1(origTransData.getTrack1());
        transData.setBranchID(origTransData.getBranchID());
        transData.setOfflineSendState(origTransData.getOfflineSendState());
        transData.setSaleReference1(origTransData.getSaleReference1());
        transData.setSaleReference2(origTransData.getSaleReference2());
        transData.setPosNo_ReceiptNo(origTransData.getPosNo_ReceiptNo());
        transData.setCashierName(origTransData.getCashierName());

        //AmexInstalment
        transData.setInstalmentTerms(origTransData.getInstalmentTerms());
        transData.setInstalmentMonthDue(origTransData.getInstalmentMonthDue());

        if (transData.getAcquirer().getName().equals(Constants.ACQ_BAY_INSTALLMENT)) {
            transData.setField63Byte(origTransData.getField63Byte());
            transData.setField63RecByte(origTransData.getField63RecByte());
        }
        transData.setAuthCode(origTransData.getAuthCode());
        transData.setQrSourceOfFund(origTransData.getQrSourceOfFund());
    }

    private void copyOrigTransQR() {
        transData.setAuthCode(origTransData.getAuthCode());
        transData.setRefNo(origTransData.getRefNo());
        transData.setQrID(origTransData.getQrID());
        transData.setQrRef2(origTransData.getQrRef2());
        transData.setOrigDateTime(origTransData.getDateTime());
    }

    // check whether void trans need to swipe card or not
    private void checkCardAndPin() {
        // not need to swipe card
//        transData.setEnterMode(EnterMode.MANUAL); TODO: use current mode for each transaction data (BPS comment)
        checkPin();
    }

    // check whether void trans need to enter pin or not
    private void checkPin() {
        // not need to enter pin
        transData.setPin("");
        transData.setHasPin(false);
        gotoState(State.MAG_ONLINE.toString());
    }

    private void goToVoidTransByMultiApp(TransMultiAppData transMultiAppData, long origTransNo) {
        if (origTransNo == -1) {
            if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_VOID_WITH_STAND)) {
                origTransNo = transMultiAppData.getStanNo();
            } else {
                origTransNo = transMultiAppData.getTraceNo();
            }
        }

        Acquirer acqAmex = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_AMEX);
        if (acqAmex.isEnable() && AmexTransAPI.getInstance().getProcess().isAppInstalled(context)
                && Constants.ACQ_AMEX.equals(transMultiAppData.getAcquirer().getName())) {
            this.origTransNo = origTransNo;
            BaseTrans.setTransRunning(false);
            gotoState(State.AMEX_API.toString());
        } else if (origTransNo != -1) {
            transEnd(new ActionResult(TransResult.ERR_NO_ORIG_TRANS, null));//no alert dialog
        }
    }

    private void getSupportAcquirers() {
        AcqManager acqManager = FinancialApplication.getAcqManager();
        List<Acquirer> acqs = new ArrayList<>();
        acqs.add(acqManager.findAcquirer(Constants.ACQ_KBANK));
        acqs.add(acqManager.findAcquirer(Constants.ACQ_KBANK_BDMS));
        acqs.add(acqManager.findAcquirer(Constants.ACQ_AMEX));
        acqs.add(acqManager.findAcquirer(Constants.ACQ_UP));
        acqs.add(acqManager.findAcquirer(Constants.ACQ_REDEEM));
        acqs.add(acqManager.findAcquirer(Constants.ACQ_REDEEM_BDMS));
        acqs.add(acqManager.findAcquirer(Constants.ACQ_SMRTPAY));
        acqs.add(acqManager.findAcquirer(Constants.ACQ_SMRTPAY_BDMS));
        acqs.add(acqManager.findAcquirer(Constants.ACQ_DCC));
        acqs.add(acqManager.findAcquirer(Constants.ACQ_KPLUS));
        acqs.add(acqManager.findAcquirer(Constants.ACQ_ALIPAY));
        acqs.add(acqManager.findAcquirer(Constants.ACQ_ALIPAY_B_SCAN_C));
        acqs.add(acqManager.findAcquirer(Constants.ACQ_WECHAT));
        acqs.add(acqManager.findAcquirer(Constants.ACQ_WECHAT_B_SCAN_C));
        acqs.add(acqManager.findAcquirer(Constants.ACQ_AMEX_EPP));
        acqs.add(acqManager.findAcquirer(Constants.ACQ_BAY_INSTALLMENT));
        acqs.add(acqManager.findAcquirer(Constants.ACQ_QR_CREDIT));
        acqs.add(acqManager.findAcquirer(Constants.ACQ_DOLFIN));
        acqs.add(acqManager.findAcquirer(Constants.ACQ_SCB_IPP));
        acqs.add(acqManager.findAcquirer(Constants.ACQ_SCB_REDEEM));
        acqs.add(acqManager.findAcquirer(Constants.ACQ_MY_PROMPT));
        acqs.add(acqManager.findAcquirer(Constants.ACQ_DOLFIN_INSTALMENT));
        supportAcquirers = acqs;
    }

    private void updateStateTransDataWallet() {
        String authCode = transData.getAuthCode() != null ? transData.getAuthCode() : transData.getOrigAuthCode();
        transData.setAuthCode(authCode);
        if (transData.getTransType() == ETransType.QR_VOID_CREDIT) {
            VoidQRCreditTrans.setVoidTransId(transData);
            FinancialApplication.getTransDataDbHelper().updateTransData(transData);
        } else {
            Component.initField63Wallet(transData);
        }

        origTransData.setAuthCode(authCode);
        origTransData.setTransState(TransData.ETransStatus.VOIDED);
        //origTransData.setAdviceStatus(transData.getAdviceStatus());
        origTransData.setWalletSlipInfo(transData.getWalletSlipInfo());
        origTransData.setOrigDateTime(origTransData.getDateTime());
        origTransData.setDateTime(transData.getDateTime());
        origTransData.setVoidStanNo(transData.getStanNo());
        FinancialApplication.getTransDataDbHelper().updateTransData(origTransData);

        origTransData.setEcrProcess(transData.isEcrProcess());
        PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(SaleVoidTrans.this, State.PRINT.toString()));
        bind(State.PRINT.toString(), printTask);
        gotoState(State.PRINT.toString());
    }

    private void updateStateTransDataVisaQR() {
        origTransData.setTransState(TransData.ETransStatus.VOIDED);
        origTransData.setWalletSlipInfo(transData.getWalletSlipInfo());
        origTransData.setOrigDateTime(origTransData.getDateTime());
        origTransData.setDateTime(transData.getDateTime());
        origTransData.setOrigStanNo(origTransData.getStanNo());
        origTransData.setStanNo(transData.getStanNo());
        transData = Component.initTextOnSlipQRVisa(transData);
        origTransData.setWalletSlipInfo(transData.getWalletSlipInfo());
        origTransData.setQrType(transData.getQrType());

        FinancialApplication.getTransDataDbHelper().updateTransData(origTransData);

        PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(SaleVoidTrans.this, QrVoidTrans.State.PRINT.toString()));
        bind(QrVoidTrans.State.PRINT.toString(), printTask);
        gotoState(QrVoidTrans.State.PRINT.toString());
    }

    private void updateStateTransDataPromptPay() {
        origTransData.setTransState(TransData.ETransStatus.VOIDED);
        origTransData.setOrigDateTime(origTransData.getDateTime());
        origTransData.setDateTime(transData.getDateTime());
        origTransData.setOrigStanNo(origTransData.getStanNo());
        origTransData.setStanNo(transData.getStanNo());
        origTransData.setWalletSlipInfo(transData.getWalletSlipInfo());
        FinancialApplication.getTransDataDbHelper().updateTransData(origTransData);

        PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(SaleVoidTrans.this, State.PRINT.toString()));
        bind(State.PRINT.toString(), printTask);
        gotoState(State.PRINT.toString());
    }

    private LinkedHashMap<String, String> mapCreditDetails(LinkedHashMap<String, String> map) {
        String transType = origTransData.getTransType().getTransName();
        TransData.ETransStatus transState = origTransData.getTransState();
        transType = transType + (transState == TransData.ETransStatus.ADJUSTED ? " (" + getString(R.string.receipt_state_adjust) + ")" : "");

        String amount = CurrencyConverter.convert(Utils.parseLongSafe(origTransData.getAmount(), 0), transData.getCurrency());
        if (Utils.parseLongSafe(origTransData.getAdjustedAmount(), 0) > 0) {
            amount = CurrencyConverter.convert(Utils.parseLongSafe(origTransData.getAdjustedAmount(), 0), transData.getCurrency());
        }

        transData.setEnterMode(origTransData.getEnterMode());
        transData.setTrack1(origTransData.getTrack1());
        transData.setTrack2(origTransData.getTrack2());
        transData.setTrack3(origTransData.getTrack3());
        transData.setCardSerialNo(origTransData.getCardSerialNo());
        transData.setSendIccData(origTransData.getSendIccData());
        transData.setDupIccData(origTransData.getDupIccData());
        transData.setEmvAppName(origTransData.getEmvAppName());
        transData.setEmvAppLabel(origTransData.getEmvAppLabel());
        transData.setAid(origTransData.getAid());
        transData.setTvr(origTransData.getTvr());
        transData.setTc(origTransData.getTc());
        transData.setTraceNo(origTransData.getTraceNo());
        transData.setDccRequired(origTransData.isDccRequired());
        transData.setDccAmount(origTransData.getDccAmount());
        transData.setDccConversionRate(origTransData.getDccConversionRate());
        transData.setDccCurrencyCode(origTransData.getDccCurrencyCode());
        transData.setDccCurrencyName(origTransData.getDccCurrencyName());
        transData.setField63Byte(origTransData.getField63Byte());
        transData.setOrigAmount(origTransData.getOrigAmount());
        transData.setOrigDccAmount(origTransData.getOrigDccAmount());
        transData.setOrigTransState(origTransData.getTransState());
        transData.setAdjustedAmount(origTransData.getAdjustedAmount());
        transData.setAdjustedDccAmount(origTransData.getAdjustedDccAmount());

        // date and time
        //AET-95
        String formattedDate = TimeConverter.convert(origTransData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.TIME_PATTERN_DISPLAY);

        map.put(getString(R.string.history_detail_type), transType);
        map.put(getString(R.string.history_detail_amount), amount);
        if (origTransData.isDccRequired()) {
            String currencyNumeric = Tools.bytes2String(origTransData.getDccCurrencyCode());
            if (Utils.parseLongSafe(origTransData.getAdjustedDccAmount(), 0) > 0) {
                amount = CurrencyConverter.convert(Utils.parseLongSafe(origTransData.getAdjustedDccAmount(), 0), currencyNumeric);
            } else {
                amount = CurrencyConverter.convert(Utils.parseLongSafe(origTransData.getDccAmount(), 0), currencyNumeric);
            }
            double exRate = origTransData.getDccConversionRate() != null ? Double.parseDouble(origTransData.getDccConversionRate()) / 10000 : 0;
            map.put(getString(R.string.history_detail_dcc_ex_rate), String.format(Locale.getDefault(), "%.4f", exRate));
            map.put(Utils.getString(R.string.history_detail_dcc_amount, CurrencyConverter.getCurrencySymbol(currencyNumeric, false)), amount);
        }
        map.put(getString(R.string.history_detail_card_no), PanUtils.maskCardNo(origTransData.getPan(), origTransData.getIssuer().getPanMaskPattern()));
        map.put(getString(R.string.history_detail_auth_code), origTransData.getAuthCode());
        map.put(getString(R.string.history_detail_ref_no), origTransData.getRefNo());
        map.put(getString(R.string.history_detail_stan_no), Component.getPaddedNumber(origTransData.getStanNo(), 6));
        map.put(getString(R.string.history_detail_trace_no), Component.getPaddedNumber(origTransData.getTraceNo(), 6));
        map.put(getString(R.string.dateTime), formattedDate);
        return map;
    }

    private LinkedHashMap<String, String> mapWalletDetails(LinkedHashMap<String, String> map) {
        String transType = origTransData.getTransType().getTransName();
        String amount = CurrencyConverter.convert(Utils.parseLongSafe(origTransData.getAmount(), 0), transData.getCurrency());

        // date and time
        String formattedDate = TimeConverter.convert(origTransData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.TIME_PATTERN_DISPLAY);

        map.put(getString(R.string.history_detail_type), transType);
        map.put(getString(R.string.history_detail_amount), amount);
        map.put(getString(R.string.history_detail_auth_code), origTransData.getAuthCode());
        map.put(getString(R.string.history_detail_ref_no), origTransData.getRefNo());
        map.put(getString(R.string.history_detail_stan_no), Component.getPaddedNumber(origTransData.getStanNo(), 6));
        map.put(getString(R.string.history_detail_trace_no), Component.getPaddedNumber(origTransData.getTraceNo(), 6));
        map.put(getString(R.string.dateTime), formattedDate);
        return map;
    }

    private LinkedHashMap<String, String> mapRedeemedDetails(LinkedHashMap<String, String> map) {
        String transType;
        if (origTransData.getTransType() == ETransType.KBANK_REDEEM_DISCOUNT) {
            transType = "89999".equals(origTransData.getRedeemedDiscountType()) ? getString(R.string.trans_kbank_redeem_discount_var) : getString(R.string.trans_kbank_redeem_discount_fix);
        } else {
            transType = origTransData.getTransType().getTransName();
        }

        String amount = CurrencyConverter.convert(Utils.parseLongSafe(origTransData.getRedeemedTotal(), 0), transData.getCurrency());

        transData.setEnterMode(origTransData.getEnterMode());
        transData.setTrack1(origTransData.getTrack1());
        transData.setTrack2(origTransData.getTrack2());
        transData.setTrack3(origTransData.getTrack3());
        transData.setEmvAppName(origTransData.getEmvAppName());
        transData.setEmvAppLabel(origTransData.getEmvAppLabel());
        transData.setTraceNo(origTransData.getTraceNo());

        transData.setField63Byte(origTransData.getField63RecByte());
        transData.setRedeemedTotal(origTransData.getRedeemedTotal());
        transData.setRedeemedPoint(origTransData.getRedeemedPoint());
        transData.setRedeemedCredit(origTransData.getRedeemedCredit());
        transData.setRedeemedDiscountType(origTransData.getRedeemedDiscountType());

        // date and time
        //AET-95
        String formattedDate = TimeConverter.convert(origTransData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.TIME_PATTERN_DISPLAY);

        map.put(getString(R.string.history_detail_type), transType);
        map.put(getString(R.string.history_detail_redeem_total), amount);
        map.put(getString(R.string.history_detail_redeem_point), String.format(Locale.getDefault(), "%,d", origTransData.getRedeemedPoint()));
        map.put(getString(R.string.history_detail_card_no), PanUtils.maskCardNo(origTransData.getPan(), origTransData.getIssuer().getPanMaskPattern()));
        map.put(getString(R.string.history_detail_auth_code), origTransData.getAuthCode());
        map.put(getString(R.string.history_detail_ref_no), origTransData.getRefNo());
        map.put(getString(R.string.history_detail_stan_no), Component.getPaddedNumber(origTransData.getStanNo(), 6));
        map.put(getString(R.string.history_detail_trace_no), Component.getPaddedNumber(origTransData.getTraceNo(), 6));
        map.put(getString(R.string.dateTime), formattedDate);
        return map;
    }

    private LinkedHashMap<String, String> mapInstalmentDetails(LinkedHashMap<String, String> map) {
        String transType = Component.getTransByIPlanMode(origTransData);
        String amount = CurrencyConverter.convert(Utils.parseLongSafe(origTransData.getAmount(), 0), transData.getCurrency());

        transData.setEnterMode(origTransData.getEnterMode());
        transData.setTrack1(origTransData.getTrack1());
        transData.setTrack2(origTransData.getTrack2());
        transData.setTrack3(origTransData.getTrack3());
        transData.setCardSerialNo(origTransData.getCardSerialNo());
        transData.setSendIccData(origTransData.getSendIccData());
        transData.setDupIccData(origTransData.getDupIccData());
        transData.setEmvAppName(origTransData.getEmvAppName());
        transData.setEmvAppLabel(origTransData.getEmvAppLabel());
        transData.setAid(origTransData.getAid());
        transData.setTvr(origTransData.getTvr());
        transData.setTc(origTransData.getTc());
        transData.setTraceNo(origTransData.getTraceNo());

        transData.setInstalmentIPlanMode(origTransData.getInstalmentIPlanMode());
        transData.setInstalmentPaymentTerm(origTransData.getInstalmentPaymentTerm());
        transData.setInstalmentPromoProduct(origTransData.isInstalmentPromoProduct());
        transData.setInstalmentPromotionKey(origTransData.getInstalmentPromotionKey());
        transData.setInstalmentSerialNo(origTransData.getInstalmentSerialNo());
        transData.setField61Byte(origTransData.getField61Byte());
        transData.setField63Byte(origTransData.getField63Byte());

        // date and time
        //AET-95
        String formattedDate = TimeConverter.convert(origTransData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.TIME_PATTERN_DISPLAY);

        map.put(getString(R.string.history_detail_type), transType);
        map.put(getString(R.string.history_detail_amount), amount);
        map.put(getString(R.string.history_detail_card_no), PanUtils.maskCardNo(origTransData.getPan(), origTransData.getIssuer().getPanMaskPattern()));
        map.put(getString(R.string.history_detail_auth_code), origTransData.getAuthCode());
        map.put(getString(R.string.history_detail_ref_no), origTransData.getRefNo());
        map.put(getString(R.string.history_detail_stan_no), Component.getPaddedNumber(origTransData.getStanNo(), 6));
        map.put(getString(R.string.history_detail_trace_no), Component.getPaddedNumber(origTransData.getTraceNo(), 6));
        map.put(getString(R.string.dateTime), formattedDate);
        return map;
    }

    private LinkedHashMap<String, String> mapDolfinInstalmentDetails(LinkedHashMap<String, String> map) {
        String transType = Component.getTransByIPlanMode(origTransData);
        String amount = CurrencyConverter.convert(Utils.parseLongSafe(origTransData.getAmount(), 0), transData.getCurrency());

        transData.setEnterMode(origTransData.getEnterMode());
        transData.setTraceNo(origTransData.getTraceNo());

        transData.setSkuCode(origTransData.getSkuCode());
        transData.setProductCode(origTransData.getProductCode());
        transData.setInstalmentIPlanMode(origTransData.getInstalmentIPlanMode());
        transData.setInstalmentPaymentTerm(origTransData.getInstalmentPaymentTerm());
        transData.setInstalmentPromoProduct(origTransData.isInstalmentPromoProduct());
        transData.setInstalmentSerialNo(origTransData.getInstalmentSerialNo());
        transData.setField61Byte(origTransData.getField61Byte());
        transData.setField63Byte(origTransData.getField63Byte());

        // date and time
        //AET-95
        String formattedDate = TimeConverter.convert(origTransData.getDateTime(), Constants.TIME_PATTERN_TRANS, Constants.TIME_PATTERN_DISPLAY);
        map.put(getString(R.string.history_detail_type), transType);
        map.put(getString(R.string.history_detail_amount), amount);
        map.put(getString(R.string.history_detail_card_no), PanUtils.maskCardNo(origTransData.getPan(), origTransData.getIssuer().getPanMaskPattern()));
        map.put(getString(R.string.payment_terms), transData.getInstalmentPaymentTerm() + " months");
        map.put(getString(R.string.history_detail_auth_code), origTransData.getAuthCode());
        map.put(getString(R.string.history_detail_stan_no), Component.getPaddedNumber(origTransData.getStanNo(), 6));
        map.put(getString(R.string.history_detail_trace_no), Component.getPaddedNumber(origTransData.getTraceNo(), 6));
        if (transData.isInstalmentPromoProduct()) {
            map.put(getString(R.string.instalment_supplier_code), transData.getSkuCode());
            map.put(getString(R.string.instalment_product_code), transData.getProductCode());
            if (transData.getInstalmentSerialNo() != null && !transData.getInstalmentSerialNo().isEmpty())
                map.put(getString(R.string.instalment_serial_number), transData.getInstalmentSerialNo());
        }
        map.put(getString(R.string.dateTime), formattedDate);
        return map;
    }

    // need electronic signature or send
    private void toSignOrPrint() {
        //EDCBBLAND-426 Support small amount
        if (transData.isTxnSmallAmt() && transData.getNumSlipSmallAmt() == 0) {
            FinancialApplication.getTransDataDbHelper().updateTransData(transData);
//            transEnd(new ActionResult(TransResult.SUCC, null));
//            return;
        }

//        transData.setPinVerifyMsg(false);//for all void trans. must sign
//        transData.setSignFree(false);//for all void trans. must sign
//        boolean eSignature = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_E_SIGNATURE);
        //if (eSignature && !transData.isTxnSmallAmt()) {
        //    gotoState(SaleVoidTrans.State.SIGNATURE.toString());
        //}else
        {
            //gotoState(State.SIGNATURE.toString());
            gotoState(SaleVoidTrans.State.PRINT.toString()); // Skip SIGNATURE process
        }
        FinancialApplication.getTransDataDbHelper().updateTransData(transData);
    }

    private boolean isSettleFail() {
        String acqName = null;
        if (transData != null) {
            acqName = transData.getAcquirer().getName();
        }
        return Component.chkSettlementStatus(acqName);
    }

    private boolean checkIsOfflineSent() {
        ETransType transType = transData.getTransType();
        ETransType origTransType = transData.getOrigTransType();
        boolean isOfflineNormalSale = ETransType.OFFLINE_TRANS_SEND == transType
                || ETransType.OFFLINE_TRANS_SEND == origTransType
                || ((ETransType.SALE == transType || ETransType.SALE_COMPLETION == transType) && transData.getOfflineSendState() != null)
                || ((ETransType.SALE == origTransType || ETransType.SALE_COMPLETION == origTransType) && transData.getOfflineSendState() != null);

        if (isOfflineNormalSale
                && transData.getOfflineSendState() != TransData.OfflineStatus.OFFLINE_SENT
                && transData.getOfflineSendState() != TransData.OfflineStatus.OFFLINE_VOIDED) {

            /* for void offline txn not upload */

            transData.setDateTime(Device.getTime(Constants.TIME_PATTERN_TRANS));
            transData.setAuthCode(origTransData.getAuthCode());
//            transData.setStanNo(origTransData.getStanNo());
            transData.setOfflineSendState(TransData.OfflineStatus.OFFLINE_VOIDED);
            transData.setResponseCode(origTransData.getResponseCode());
            FinancialApplication.getTransDataDbHelper().insertTransData(transData);

            origTransData.setVoidStanNo(transData.getStanNo());
            origTransData.setDateTime(transData.getDateTime());
            origTransData.setTransState(ETransStatus.VOIDED);
            origTransData.setOfflineSendState(TransData.OfflineStatus.OFFLINE_VOIDED);
            FinancialApplication.getTransDataDbHelper().updateTransData(origTransData);

            Component.incStanNo(transData);//increase stan no. for next txn.

            if (origTransData.getTransState() == ETransStatus.ADJUSTED) {
                TransData adjustedTran = FinancialApplication.getTransDataDbHelper().findAdjustedTransDataByTraceNo(
                        origTransData.getTraceNo(), origTransData.getAcquirer());
                if (adjustedTran != null) {
                    adjustedTran.setOfflineSendState(TransData.OfflineStatus.OFFLINE_VOIDED);
                    FinancialApplication.getTransDataDbHelper().updateTransData(adjustedTran);
                }
            }

            return false;
        }
        return true;
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
        if (((origTransData.getTransType().equals(ETransType.SALE) || origTransData.getTransType().equals(ETransType.OFFLINE_TRANS_SEND)) &&
                !offlineTransList.isEmpty() && offlineTransList.get(0).getId() != transData.getId())) { //AET-92
            //offline send
            gotoState(SaleVoidTrans.State.OFFLINE_SEND.toString());
        } else {
            toSignOrPrint();
        }
    }

    @Override
    protected void transEnd(final ActionResult result) {
        super.transEnd(result);
        isMyPrompt = false;
        isKplus = false;
        isAliPay = false;
        isWchatPay = false;
        isQRCredit = false;
        isRedeem = false;
        isSmrtpay = false;
        isDolfinIpp = false;
        setTransType(ETransType.VOID);//set default transType to void credit for next trans.
        if (swapMerchantName!=null) {
            MerchantProfileManager.INSTANCE.applyProfileAndSave(swapMerchantName);
        }
    }

    enum State {
        INPUT_PWD,
        ENTER_TRANSNO,
        TRANS_DETAIL,
        MAG_ONLINE,
        INQUIRY,
        SIGNATURE,
        OFFLINE_SEND,
        PRINT,
        DOLFIN_VOID,
        DOLFIN_SENT_CONFIG,
        DOLFIN_GET_CONFIG,
        SCB_VOID,
        AMEX_API,
    }

    private void showRespMsg(ActionResult result, String respMsg) {
        DialogInterface.OnDismissListener onDismissListener = new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                transEnd(new ActionResult(TransResult.ERR_ABORTED, null));
            }
        };
        DialogUtils.showErrMessage(getCurrentContext(), getString(R.string.menu_void), respMsg, onDismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
    }
}
