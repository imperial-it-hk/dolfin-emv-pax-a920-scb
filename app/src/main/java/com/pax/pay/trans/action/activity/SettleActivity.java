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
package com.pax.pay.trans.action.activity;

import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.BaseActivityWithTickForAction;
import com.pax.pay.ECR.EcrData;
import com.pax.pay.ECR.LawsonHyperCommClass;
import com.pax.pay.SettlementRegisterActivity;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.record.Printer;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.action.ActionEReceiptInfoUpload;
import com.pax.pay.trans.action.ActionRecoverKBankLoadTWK;
import com.pax.pay.trans.action.ActionTleTransOnline;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.Controller;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.MerchantProfileManager;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.model.TransTotal;
import com.pax.pay.trans.pack.PackEReceiptSettleUpload;
import com.pax.pay.trans.receipt.PrintListenerImpl;
import com.pax.pay.trans.receipt.ReceiptPrintTransForERM;
import com.pax.pay.trans.transmit.TransOnline;
import com.pax.pay.trans.transmit.TransProcessListener;
import com.pax.pay.trans.transmit.TransProcessListenerImpl;
import com.pax.pay.trans.transmit.Transmit;
import com.pax.pay.utils.Convert;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.EReceiptUtils;
import com.pax.pay.utils.TransResultUtils;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;
import com.pax.view.dialog.CustomAlertDialog;
import com.pax.view.dialog.CustomAlertDialog.OnCustomClickListener;
import com.pax.view.dialog.DialogUtils;

import org.json.JSONObject;

import kotlin.text.Charsets;
import th.co.bkkps.bpsapi.ITransAPI;
import th.co.bkkps.dofinAPI.DolfinApi;
import th.co.bkkps.dofinAPI.tran.action.ActionDolfinSettle;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import kotlin.text.Charsets;
import th.co.bkkps.amexapi.action.ActionAmexSettle;
import th.co.bkkps.bpsapi.ITransAPI;
import th.co.bkkps.dofinAPI.tran.action.ActionDolfinSettle;
import th.co.bkkps.kcheckidAPI.trans.action.ActionGetKCheckIDRecordCount;
import th.co.bkkps.kcheckidAPI.trans.action.ActionKCheckIDSettlement;
import th.co.bkkps.kcheckidAPI.trans.action.ActionKCheckIDUpdateParam;
import th.co.bkkps.scbapi.trans.action.ActionScbIppLink;
import th.co.bkkps.scbapi.trans.action.ActionScbUpdateParam;
import th.co.bkkps.utils.ArrayListUtils;
import th.co.bkkps.utils.Log;
import th.co.bkkps.utils.PageToSlipFormat;

public class
SettleActivity extends BaseActivityWithTickForAction {
    final ConditionVariable cv = new ConditionVariable();
    boolean result = true;
    private ArrayList<String> selectAcqs = new ArrayList<>();
    private ArrayList<SettleAcquirer> settleAcqList = new ArrayList<>();
    private TransTotal total;
    private String navTitle;
    private boolean navBack;
    private boolean isEcrProcess;
    private boolean isBypassConfirmSettle = false;
    private boolean isBypassConfirmAutoSettle = false;
    private TextView acquirerName;
    private TextView merchantName;
    private TextView merchantId;
    private TextView terminalId;
    private TextView batchNo;
    private Acquirer acquirer;
    private boolean isNeedChkReaderId;
    private boolean isSettlementReceiptuploaded;
    private boolean isFinishLoadTLE;
    private int isDolfinSettleResult;
    private int isScbIppResult;
    private int isScbRedeemResult;
    private int iKCheckIDResult;
    private int isAmexApiResult;
    //AET-41
    private String acquirerDef;
    private CustomAlertDialog confirmDialog;

    private boolean isAllSelected;
    private boolean isSilentOnZeroTransFound = false;
    private ArrayList<String> settleFailHosts = new ArrayList<>();

    private LinearLayout transTotalLayout;
    private LinearLayout redeemKbankTotalLayout;

    /* ERM */
    private long[] ermSuccessTotal;
    private long[] ermUnsuccessfulTotal;
    private long[] ermVoidSuccessTotal;
    private long[] ermVoidUnsuccessfulTotal;
    private List<TransData> uploadList = new ArrayList<>(0);
    private boolean isSupportEReceipt;
    private boolean isSettleAllMerchants;
    private String ercmSettleTempFileName;
    private String ercmSettledRefNo;
    private String localSettledRefNo;
    private AAction currAction;
    private ITransAPI transAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currAction = TransContext.getInstance().getCurrentAction();
        doSettlement();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        if (!isEcrProcess) { //disable timeout both standalone and ecrProcess
        tickTimer.stop();
//        }
        if (isBypassConfirmAutoSettle) {
            super.setTitle("AutoSettlement");
        }

    }

    @Override
    protected void loadParam() {
        Bundle bundle = getIntent().getExtras();
        selectAcqs = bundle.getStringArrayList(EUIParamKeys.ARRAY_LIST_2.toString());
        navTitle = getIntent().getStringExtra(EUIParamKeys.NAV_TITLE.toString());
        navBack = getIntent().getBooleanExtra(EUIParamKeys.NAV_BACK.toString(), false);
        isEcrProcess = getIntent().getBooleanExtra(EUIParamKeys.ECR_PROCESS.toString(), false);
        isBypassConfirmSettle = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_LINKPOS_BYPASS_CONFIRM_SETTLE, false);     // this config must use with ECR/LINPOS process
        isBypassConfirmAutoSettle = getIntent().getBooleanExtra(EUIParamKeys.BYPASS_CONFIRM_SETTLE.toString(), false);
        isSettleAllMerchants = getIntent().getBooleanExtra(EUIParamKeys.SETTLE_ALL_MERCHANTS.toString(), false);

        //AET-41
        acquirerDef = FinancialApplication.getAcqManager().getCurAcq().getName();
        isSupportEReceipt = getIntent().getBooleanExtra(EUIParamKeys.SUPPORT_E_RECEIPT.toString(), false);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_settle_layout;
    }

    @Override
    protected String getTitleString() {
        return navTitle;
    }

    @Override
    protected void initViews() {
        acquirerName = (TextView) findViewById(R.id.settle_acquirer_name);
        merchantName = (TextView) findViewById(R.id.settle_merchant_name);
        merchantId = (TextView) findViewById(R.id.settle_merchant_id);
        terminalId = (TextView) findViewById(R.id.settle_terminal_id);
        batchNo = (TextView) findViewById(R.id.settle_batch_num);

        transTotalLayout = (LinearLayout) findViewById(R.id.layout_trans_total);
        redeemKbankTotalLayout = (LinearLayout) findViewById(R.id.layout_redeem_kbank);


        filterSelectedAcqs();
        setCurrAcquirerContent(settleAcqList.get(0));
        getEReceiptStatusBeforeSettlement();
    }

    public void setCurrAcquirerContent(final SettleAcquirer settleAcq) {
        acquirer = settleAcq.getAcquirer();
        //set current acquirer,settle print need it
        FinancialApplication.getAcqManager().setCurAcq(acquirer);

        this.acquirerName.setText(acquirer.getName());

        merchantName.setText(FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_MERCHANT_NAME_EN));
        merchantId.setText(acquirer.getMerchantId());
        terminalId.setText(acquirer.getTerminalId());
        batchNo.setText(String.valueOf(acquirer.getCurrBatchNo()));

        total = settleAcq.getTotal();

        if (acquirer.getName().equals(Constants.ACQ_REDEEM) || acquirer.getName().equals(Constants.ACQ_REDEEM_BDMS)) {
            redeemKbankTotalLayout.setVisibility(View.VISIBLE);
            transTotalLayout.setVisibility(View.GONE);

            ((TextView) findViewById(R.id.redeem_product_total_sum)).setText(String.valueOf(total.getTransRedeemKbankTotal().getProductAllCard()));
            ((TextView) findViewById(R.id.redeem_product_credit_total_sum)).setText(String.valueOf(total.getTransRedeemKbankTotal().getProductCreditAllCard()));
            ((TextView) findViewById(R.id.redeem_voucher_total_sum)).setText(String.valueOf(total.getTransRedeemKbankTotal().getVoucherAllCard()));
            ((TextView) findViewById(R.id.redeem_voucher_credit_total_sum)).setText(String.valueOf(total.getTransRedeemKbankTotal().getVoucherCreditAllCard()));
            ((TextView) findViewById(R.id.redeem_discount_total_sum)).setText(String.valueOf(total.getTransRedeemKbankTotal().getDiscountAllCard()));
            ((TextView) findViewById(R.id.settle_redeem_items_total)).setText(String.valueOf(total.getTransRedeemKbankTotal().getItemSum()));
            ((TextView) findViewById(R.id.settle_redeem_points_total)).setText(String.valueOf(total.getTransRedeemKbankTotal().getPointsSum()));
            ((TextView) findViewById(R.id.settle_redeem_amount_total)).setText(CurrencyConverter.convert(total.getTransRedeemKbankTotal().getRedeemAmtSum()));
            ((TextView) findViewById(R.id.settle_redeem_credit_total)).setText(CurrencyConverter.convert(total.getTransRedeemKbankTotal().getCreditSum()));
            ((TextView) findViewById(R.id.settle_redeem_total)).setText(CurrencyConverter.convert(total.getTransRedeemKbankTotal().getTotalSum()));

        } else {
            transTotalLayout.setVisibility(View.VISIBLE);
            redeemKbankTotalLayout.setVisibility(View.GONE);

            String saleAmt = CurrencyConverter.convert(total.getSaleTotalAmt());
            //AET-18
            String refundAmt = CurrencyConverter.convert(0 - total.getRefundTotalAmt());
            String topupAmt = CurrencyConverter.convert(0 - total.getTopupTotalAmt());
            String voidSaleAmt = CurrencyConverter.convert(0 - total.getSaleVoidTotalAmt());
            String voidRefundAmt = CurrencyConverter.convert(0 - total.getRefundVoidTotalAmt());
            String voidTopUpAmt = CurrencyConverter.convert(total.getTopupVoidTotalAmt());

            ((TextView) findViewById(R.id.sale_total_sum)).setText(String.valueOf(total.getSaleTotalNum()));
            ((TextView) findViewById(R.id.sale_total_amount)).setText(saleAmt);
            ((TextView) findViewById(R.id.refund_total_sum)).setText(String.valueOf(total.getRefundTotalNum()));
            ((TextView) findViewById(R.id.refund_total_amount)).setText(refundAmt);
//            ((TextView) findViewById(R.id.topup_total_sum)).setText(String.valueOf(total.getTopupTotalNum()));
//            ((TextView) findViewById(R.id.topup_total_amount)).setText(topupAmt);

            ((TextView) findViewById(R.id.void_sale_total_sum)).setText(String.valueOf(total.getSaleVoidTotalNum()));
            ((TextView) findViewById(R.id.void_sale_total_amount)).setText(voidSaleAmt);
            ((TextView) findViewById(R.id.void_refund_total_sum)).setText(String.valueOf(total.getRefundVoidTotalNum()));
            ((TextView) findViewById(R.id.void_refund_total_amount)).setText(voidRefundAmt);
//            ((TextView) findViewById(R.id.void_topup_total_sum)).setText(String.valueOf(total.getTopupVoidTotalNum()));
//            ((TextView) findViewById(R.id.void_topup_total_amount)).setText(voidTopUpAmt);
        }

        cv.open();
    }

    /**
     * Filter acquirers to do settlement.
     */
    private void filterSelectedAcqs() {

        if (isEcrProcess && FinancialApplication.getEcrProcess().mHyperComm instanceof LawsonHyperCommClass) {
            isSilentOnZeroTransFound = true;
        }

        List<String> tempAcqs = new ArrayList<>();
        boolean isEmptyBatch = FinancialApplication.getTransDataDbHelper().countOfmerchant() == 0;

        List<Acquirer> listAllAcqs = FinancialApplication.getAcqManager().findEnableAcquirersWithSortMode(true);
        int allAcqs = listAllAcqs.size();

        if (isSettleAllMerchants) {
            selectAcqs.clear();
            selectAcqs.addAll(ArrayListUtils.INSTANCE.getStringAcqNameList(listAllAcqs));
        }

        int selectedAcqs = selectAcqs.size();
        isAllSelected = allAcqs == selectedAcqs;
        for (String acq : selectAcqs) {
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(acq);
            if (acquirer.getName().equals(Constants.ACQ_AMEX)) {
                TransTotal total = new TransTotal();
                total.setMerchantName(MerchantProfileManager.INSTANCE.getCurrentMerchant());
                settleAcqList.add(new SettleAcquirer(acquirer, total, true));
                tempAcqs.add(acq);
            }
            else {
                TransTotal total = FinancialApplication.getTransTotalDbHelper().calcTotal(acquirer, true);
                boolean emptyTransByAcq = total.isZero();
                if (isEmptyBatch
                        || !isAllSelected
                        || (isAllSelected && !isEmptyBatch && !emptyTransByAcq)) {
                    // If No transaction, add all selected acquirers and settle with total zero.
                    // If not select all acquirers, settlement without checking total of transaction.
                    // If select all acquirers and no empty batch, need to settle only acquirers that have transactions.
//                if (Constants.ACQ_BSS_HOST.equals(acq) || Constants.ACQ_LINEPAY.equals(acq)) {
//                    isNeedChkReaderId = true;
//                }
                    settleAcqList.add(new SettleAcquirer(acquirer, total, emptyTransByAcq));
                    tempAcqs.add(acq);
                }
            }
        }
        selectAcqs.clear();
        selectAcqs.addAll(tempAcqs);
    }

    @Override
    protected void setListeners() {
        enableBackAction(navBack);
    }

    private void doSettlement() {
        FinancialApplication.getApp().runInBackground(new Runnable() {

            private String genPromptMsg(int cnt, int total) {
                return Utils.getString(R.string.settle_settled) + "[" + cnt + "/" + total + "]";
            }


            @Override
            public void run() {
                try {
                    int ret = 0;
                    int cnt = 0;
                    // check is sett


                    TransProcessListener transProcessListenerImpl = new TransProcessListenerImpl(
                            SettleActivity.this);

                    // Verify path to store unsettlement list
                    EReceiptUtils.verifyErcmUnsettleStorage(Environment.getExternalStorageDirectory().getAbsolutePath() + "/PAX/BPSLoader/ERCM/UnsettlementList");

                    Log.d(EReceiptUtils.TAG, "ERM-PRE-SETTLEMENT >> PRINTERECEIPTTXNUPLOADFAIL START");
                    printEreceiptTxnUploadFail();

                    Log.d("SETTLE", "Settlement -- ECR Mode enabled : " + isEcrProcess);
                    Log.d("SETTLE", "Settlement -- Bypass Confirm Settlement : " + isBypassConfirmSettle);

                    boolean confirmSettleResult = ((isEcrProcess && isBypassConfirmSettle) || isBypassConfirmAutoSettle) ? true : confirmSettle();
                    if (!confirmSettleResult) {
                        finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
                        return;
                    }

                    ArrayList<TransTotal> successSettleList = new ArrayList<>();
                    boolean isMyPrompt = false;
                    boolean isPromptPay = false;
                    boolean isWallet = false;
                    boolean isPromptpayKbank = false;
                    boolean isAlipayKbank = false;
                    boolean isAlipayBscanCKbank = false;
                    boolean isWechatKbank = false;
                    boolean isWechatBscanCKbank = false;
                    boolean isQRCreditKbank = false;
                    boolean isQRSale;
                    boolean isRedeemKbank = false, isInstalmentKbank;
                    boolean isDolfin = false;
                    boolean isBayInstallment = false;
                    boolean isScbIpp = false;
                    boolean isScbRedeem = false;
                    boolean isAmex = false;
                    boolean isKCheckID = false;
                    boolean isSubordinatedApp = false;
                    boolean isOnSettlementPrintEnabled = true;
                    boolean isDolfinInstalment = false;

                    if (EcrData.instance.isOnProcessing) {
                        if (FinancialApplication.getEcrProcess().mHyperComm instanceof LawsonHyperCommClass) {
                            isOnSettlementPrintEnabled = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_SETTLEMENT_RECEIPT_ENABLE) ;
                        }
                    }
                    int countTransSettled = -1;
                    int countTransPendingSettle = -1;
                    for (SettleAcquirer settleAcq : settleAcqList) {
                        TransContext.getInstance().getCurrentAction().setFinished(false);
                        //AET-37 268
                        PageToSlipFormat.getInstance().Reset();
                        cv.close();
                        onResetView(settleAcq);
                        cv.block(30000);
                        Log.d(TAG, "onResetView ==> cv unblock");


                        total.setAcquirer(acquirer);
                        total.setDateTime(Device.getTime(Constants.TIME_PATTERN_TRANS));
                        total.setMerchantID(acquirer.getMerchantId());
                        total.setTerminalID(acquirer.getTerminalId());
                        total.setBatchNo(acquirer.getCurrBatchNo());


                        // check acquirtStatus
                        isMyPrompt = Constants.ACQ_MY_PROMPT.equals(acquirer.getName());
                        isPromptPay = Constants.ACQ_QR_PROMPT.equals(acquirer.getName());
                        isWallet = Constants.ACQ_WALLET.equals(acquirer.getName());
                        isQRSale = Constants.ACQ_QRC.equals(acquirer.getName());
                        isPromptpayKbank = Constants.ACQ_KPLUS.equals(acquirer.getName());
                        isAlipayKbank = Constants.ACQ_ALIPAY.equals(acquirer.getName());
                        isAlipayBscanCKbank = Constants.ACQ_ALIPAY_B_SCAN_C.equals(acquirer.getName());
                        isWechatKbank = Constants.ACQ_WECHAT.equals(acquirer.getName());
                        isWechatBscanCKbank = Constants.ACQ_WECHAT_B_SCAN_C.equals(acquirer.getName());
                        isQRCreditKbank = Constants.ACQ_QR_CREDIT.equals(acquirer.getName());
                        isRedeemKbank = Constants.ACQ_REDEEM.equals(acquirer.getName()) || Constants.ACQ_REDEEM_BDMS.equals(acquirer.getName());
                        isInstalmentKbank = Constants.ACQ_SMRTPAY.equals(acquirer.getName()) || Constants.ACQ_SMRTPAY_BDMS.equals(acquirer.getName());
                        isDolfin = Constants.ACQ_DOLFIN.equals(acquirer.getName());
                        isBayInstallment = Constants.ACQ_BAY_INSTALLMENT.equals(acquirer.getName());
                        isScbIpp = Constants.ACQ_SCB_IPP.equals(acquirer.getName());
                        isScbRedeem = Constants.ACQ_SCB_REDEEM.equals(acquirer.getName());
                        isAmex = Constants.ACQ_AMEX.equals(acquirer.getName());
                        isKCheckID = Constants.ACQ_KCHECKID.equals(acquirer.getName());
                        isDolfinInstalment = Constants.ACQ_DOLFIN_INSTALMENT.equals(acquirer.getName());

                        isSubordinatedApp = (isDolfin || isScbIpp || isScbRedeem || isAmex || isKCheckID);


                        if (total.isZero() && !isKCheckID && !isAmex) {

                            // AutoSettlement
                            SettlementRegisterActivity.Companion.updateSettleTime(acquirer.getName());

                            if (EcrData.instance.isOnProcessing) {
                                EcrData.instance.createSettlementEcrResponse(total, EcrData.SETTLE_SKIPPED);
                            }
                            if (!isSilentOnZeroTransFound) {
                                transProcessListenerImpl.onShowErrMessage(Utils.getString(R.string.err_batch_not_found), Constants.FAILED_DIALOG_SHOW_TIME, true);
                            }
                            if (settleAcqList.size() == 1) {
                                finish(new ActionResult(TransResult.ERR_NO_TRANS, null));
                                return;
                            } else {
                                continue;
                            }
                        }

                        if (Component.getSettlementStatusByAcquirer(acquirer.getName()) == Controller.Constant.WORKED) {
                            Log.d("SETTLEMENT", acquirer.getName() + " RESET Status from        WORKED --------------------------------------------------------->> SETTLE");
                            // reset this status only when this status already as WORKED
                            Component.setSettleStatus(Controller.Constant.SETTLE, acquirer.getName());
                        }

                        TransOnline transOnline = new TransOnline();
                        if (Component.getSettlementStatusByAcquirer(acquirer.getName()) == Controller.Constant.SETTLE) {
                            Log.d("SETTLEMENT", acquirer.getName() + "--------------------------------------------------------->> STATE : SETTLE");
                            if (isPromptPay) {
                                transOnline.settlePromptPay(acquirer, transProcessListenerImpl);
                                transProcessListenerImpl.onHideProgress();
                            } else if (isQRSale) {
                                ret = new Transmit().sendReversalQRSale(transProcessListenerImpl);
                                if (ret == TransResult.SUCC) {
                                    ret = new Transmit().sendAdviceQrSale(null, transProcessListenerImpl);
                                }
                                transProcessListenerImpl.onHideProgress();
                            } else if (isMyPrompt) {
                                ret = transOnline.settleMyPrompt(total, transProcessListenerImpl);
                            } else if (isWallet) {
                                ret = transOnline.settleWallet(total, transProcessListenerImpl);
                            } else if (isPromptpayKbank) {
                                ret = transOnline.settleKbankPromptpay(total, transProcessListenerImpl);
                            } else if (isAlipayKbank) {
                                ret = transOnline.settleKbankAlipay(total, transProcessListenerImpl);
                            } else if (isAlipayBscanCKbank) {
                                ret = transOnline.settleKbankAlipayBscanC(total, transProcessListenerImpl);
                            } else if (isWechatKbank) {
                                ret = transOnline.settleKbankWechat(total, transProcessListenerImpl);
                            }  else if (isWechatBscanCKbank) {
                                ret = transOnline.settleKbankWechatBscanC(total, transProcessListenerImpl);
                            } else if (isQRCreditKbank) {
                                ret = transOnline.settleKbankQRCredit(total, transProcessListenerImpl);
                            } else if (isRedeemKbank) {
                                ret = transOnline.settleRedeem(total, transProcessListenerImpl);
                            } else if (isInstalmentKbank) {
                                ret = transOnline.settleInstalment(total, transProcessListenerImpl);
                            } else if (isDolfinInstalment) {
                                ret = transOnline.settleDoflinInstal(total, transProcessListenerImpl); //TODO
                            } else if (isDolfin && DolfinApi.getInstance().getDolfinServiceBinded()) {
                                //ret = transOnline.settleRabbit(total, transProcessListenerImpl);
                                cv.close();
                                ret = processDolfinSettle(TransContext.getInstance().getCurrentAction(), transProcessListenerImpl);
                                cv.block(30000);
                                Log.d(TAG, "processDolfinSettle ==> cv unblock");
                                if (ret != 0) { continue; }
                            } else if (isScbIpp) {
                                cv.close();
                                processScbIppSettle(TransContext.getInstance().getCurrentAction());
                                cv.block(40000);
                                Log.d(TAG, "processScbIppSettle ==> cv unblock");
                                Log.d("SETTLEMENT", "SCB-SETTLEMENT-RESULT = " + ret);
                                if (isScbIppResult != 0) { continue; }
                            } else if (isScbRedeem) {
                                cv.close();
                                processScbRedeemSettle(TransContext.getInstance().getCurrentAction());
                                cv.block(40000);
                                Log.d(TAG, "processScbRedeemSettle ==> cv unblock");
                                Log.d("SETTLEMENT","SCB-REDEEM-SETTLEMENT-RESULT = " + ret);
                                if (isScbRedeemResult != 0) { continue; }
                            } else if (isAmex) {
                                cv.close();
                                processAmexSettle(TransContext.getInstance().getCurrentAction());
                                cv.block(40000);
                                Log.d(TAG, "processAmexSettle ==> cv unblock");
                                Log.d("SETTLEMENT","AMEX-SETTLEMENT-RESULT = " + ret);
                                if (isAmexApiResult != 0) { continue; }
                            }  else if (isKCheckID) {
                                cv.close();
                                ret = processKCheckIDSettle(TransContext.getInstance().getCurrentAction(), acquirer, transProcessListenerImpl);
                                cv.block(30000);
                                Log.d(TAG, "processKCheckIDSettle ==> cv unblock");
                                Log.d("SETTLEMENT", "KCHECKID-SETTLEMENT-RESULT = " + ret);
                                if (ret != 0) {
                                    continue;
                                }
                            } else {
                                // 处理冲正
                                new Transmit().sendReversal(transProcessListenerImpl, total.getAcquirer()); // AET-255
                                ret = transOnline.settle(total, transProcessListenerImpl);
                            }

                            if (!isSubordinatedApp && !isCompleteSettle(ret, transOnline, transProcessListenerImpl)) {
                                if (isEcrProcess && settleAcqList.size() == 1) {
                                    EcrData.instance.setSettleSingleHostTotalData(total, transOnline.getRespErrorCode());
                                }
                                if (EcrData.instance.isOnProcessing) {
                                    EcrData.instance.createSettlementEcrResponse(total, EcrData.SETTLE_UNSUCCESS);
                                }
                                SettlementRegisterActivity.Companion.updateSettleTime(acquirer.getName());
                                continue;
                            } else {
                                ++cnt;
                                if (EcrData.instance.isOnProcessing) {
                                    EcrData.instance.createSettlementEcrResponse(total, EcrData.SETTLE_SUCCESS);
                                }
                                if (isSubordinatedApp) {
                                    // AutoSettlement
                                    SettlementRegisterActivity.Companion.updateSettleTime(acquirer.getName());
                                    
                                    Component.setSettleStatus(Controller.Constant.CLEAR_TRANSTOTAL_REQUIRED, acquirer.getName());
                                } else {
                                    Component.setSettleStatus(Controller.Constant.PRINT_SETTLE_REPORT_REQUIRED, acquirer.getName());
                                }
                            }
                        } else {
                            ret = TransResult.SUCC;
                        }

                        if (Component.getSettlementStatusByAcquirer(acquirer.getName()) == Controller.Constant.PRINT_SETTLE_REPORT_REQUIRED) {
                            Log.d("SETTLEMENT", acquirer.getName() + "--------------------------------------------------------->> STATE : PRINT_SETTLE_REPORT_REQUIRED");
                            if (isQRSale) {
                                Printer.printPromptPaySettlement(SettleActivity.this, total, !isOnSettlementPrintEnabled);
                            } else if(!isSubordinatedApp) {
                                Printer.printSettle(SettleActivity.this, getString(R.string.settle_title), total, false, null, !isOnSettlementPrintEnabled);
                            }

                            if (!isSubordinatedApp && !isWallet && !isQRSale && !isRedeemKbank) {
                                Printer.printTotalDetail("TOTALS BY ISSUER", SettleActivity.this, acquirer, settleAcq.isEmptyTrans, total, true, !isOnSettlementPrintEnabled);
                            }
                            successSettleList.add(total);
                            Component.setSettleStatus(Controller.Constant.GENERATE_ERCM_REQUIRED, acquirer.getName());
                        } else {
                            if (!isSubordinatedApp) {
                                Log.d("SETTLEMENT", "Print LastBatch for settlemented trans.");
                                if (Component.getSettlementStatusByAcquirer(acquirer.getName()) == Controller.Constant.CLEAR_TRANSDATA_REQUIRED) {
                                    Printer.printLastBatch(SettleActivity.this, acquirer, TransContext.getInstance().getCurrentAction());
                                } else {
                                    Printer.printLastBatchOnSettlement(SettleActivity.this, acquirer, TransContext.getInstance().getCurrentAction(), total, !isOnSettlementPrintEnabled);
                                }
                            } else {
                                Log.d("SETTLEMENT", "Subordinated application, don't need to reprint settlement report here");
                            }
                            successSettleList.add(total);
                        }

                        if (Component.getSettlementStatusByAcquirer(acquirer.getName()) == Controller.Constant.GENERATE_ERCM_REQUIRED) {
                            Log.d("SETTLEMENT", acquirer.getName() + "--------------------------------------------------------->> STATE : GENERATE_ERCM_REQUIRED");
                            Log.d(EReceiptUtils.TAG, "SETTLEMENT: Keep eSettlement Report");
                            try {
                                if( !isSubordinatedApp && !isBayInstallment) {
                                    if (acquirer.getEnableUploadERM()) {
                                        String AcqIndex = EReceiptUtils.StringPadding(String.valueOf(acquirer.getId()), 3, "0", Convert.EPaddingPosition.PADDING_LEFT);
                                        String AcqCurrBatch = Component.getPaddedNumber(acquirer.getCurrBatchNo(), 6);
                                        if (transOnline.getSettledRefNo() != null) {
                                            ercmSettledRefNo = transOnline.getSettledRefNo();
                                        } else {
                                            ercmSettledRefNo = Device.getTime(Constants.TIME_PATTERN_TRANS).substring(2, 14);
                                        }
                                        PackEReceiptSettleUpload settleReport = new PackEReceiptSettleUpload(null);
                                        EReceiptUtils.setUnSettleRawData(acquirer, ercmSettledRefNo, settleReport.generateReceiptFormat(AcqIndex));

                                        Log.d(EReceiptUtils.TAG, "       acquirerID = " + AcqIndex);
                                        Log.d(EReceiptUtils.TAG, "       CurrentBatch = " + AcqCurrBatch);
                                        Log.d(EReceiptUtils.TAG, "       ReferenceNo = " + ercmSettledRefNo);
                                    } else {
                                        Log.d(EReceiptUtils.TAG, "       ERCM upload was disable on Host : " + acquirer.getName());
                                    }
                                } else {
                                    Log.d(EReceiptUtils.TAG, "       Host : " + acquirer.getName() + " dont need to perform e-settle-report upload to ERCM host");
                                }
                            } catch (Exception ex) {
                                Log.d(EReceiptUtils.TAG, " ERCM Generation error exception : " + ex.getMessage());
                            }
                            Component.setSettleStatus(Controller.Constant.CLEAR_TRANSTOTAL_REQUIRED, acquirer.getName());
                        }


                        if (Component.getSettlementStatusByAcquirer(acquirer.getName()) == Controller.Constant.CLEAR_TRANSTOTAL_REQUIRED) {
                            Log.d("SETTLEMENT", acquirer.getName() + "--------------------------------------------------------->> STATE : CLEAR_TRANSTOTAL_REQUIRED");
                            if (isKCheckID) {
                                Log.d(EReceiptUtils.TAG, "===> Skip delete trans. for acquirer name :: " + acquirer.getName());
                            } else {
                                total.setClosed(true);
                                Log.d(EReceiptUtils.TAG, "===> Delete settlemented trans.");
                                FinancialApplication.getTransTotalDbHelper().deleteAllTransTotal(acquirer);// delete last settlement by acquirer
                                FinancialApplication.getTransTotalDbHelper().insertTransTotal(total);
                            }
                            Component.setSettleStatus(Controller.Constant.CLEAR_TRANSDATA_REQUIRED, acquirer.getName());
                        }

                        String currentBatch = Component.getPaddedNumber(acquirer.getCurrBatchNo(), 6);
                        if (Component.getSettlementStatusByAcquirer(acquirer.getName()) == Controller.Constant.CLEAR_TRANSDATA_REQUIRED) {
                            Log.d("SETTLEMENT", acquirer.getName() + "--------------------------------------------------------->> STATE : CLEAR_TRANSDATA_REQUIRED");
                            if (isKCheckID) {
                                Component.setSettleStatus(Controller.Constant.WORKED, acquirer.getName());

                                // update latestAutoSettlementTime
                                SettlementRegisterActivity.Companion.updateSettleTime(acquirer.getName());
                            } else {
                                if (FinancialApplication.getTransDataDbHelper().deleteAllTransDataExceptPreAuth(acquirer)) {
                                    Component.incBatchNo(acquirer);
                                    Component.setSettleStatus(Controller.Constant.WORKED, acquirer.getName());

                                    // update latestAutoSettlementTime
                                    SettlementRegisterActivity.Companion.updateSettleTime(acquirer.getName());

                                    // delete Pre-Auth + Sale Comp and Pre-Auth Cancel
                                    FinancialApplication.getTransDataDbHelper().deleteAllPreAuthCancelAndSaleCompTransData(acquirer);
                                    FinancialApplication.getTransDataDbHelper().deleteAllPreAuthTransDataWithSaleCompState(acquirer);

                                    //Log.d("SETTLEMENT",acquirer.getName() + "--------------------------------------------------------->> STATE : TRANSDATA CLEARED");
                                }
                            }
                        }


                        transProcessListenerImpl.onHideProgress();
                        if (ret == TransResult.ERR_RECONCILE_FAILED) {
                            transProcessListenerImpl.onShowNormalMessage(TransResultUtils.getMessage(ret), 5, true);
                        } else {
                            String showMsg = null;
                            if (isKCheckID) {
                                showMsg = iKCheckIDResult==TransResult.SUCC ? getString(R.string.settle_batch_closed, currentBatch): getString(R.string.settle_no_kcheckid_txn_total);
                            } else {
                                showMsg = settleAcq.isEmptyTrans ? getString(R.string.settle_no_txn_total) : getString(R.string.settle_batch_closed, currentBatch);
                            }
                            if (!isScbIpp && !isScbRedeem && !isAmex) {
                                transProcessListenerImpl.onShowNormalMessage(showMsg, 5, (!isEcrProcess));
                            }
                        }

                        isFinishLoadTLE = true;
//                        isFinishLoadTLE = false;
//                        // EDCBBLAND-361 Modify settlement flow for Log on TLE/UP host by host
//                        if (!isDolfin && !isScbIpp && (acquirer.isEnableTle() || acquirer.isEnableUpi())) {
//                            cv.close();
//                            processTleUPLogOn(TransContext.getInstance().getCurrentAction());
//                            //if (!isFinishLoadTLE) {
//                            cv.block(40000);
//                            Log.d(TAG, "processTleUPLogOn ==> cv unblock");
//                            //}
//                        }
                    }

                    boolean isGrandTotal = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_GRAND_TOTAL);
                    if (!isSubordinatedApp && isGrandTotal) {
                        if ((isAllSelected || settleAcqList.size() > 1) && successSettleList.size() > 0) {
                            TransTotal grandTotal = FinancialApplication.getTransTotalDbHelper().sumTransTotal(successSettleList);
                            Printer.printSettleGrandTotal(SettleActivity.this, grandTotal, !isOnSettlementPrintEnabled);
                        }

                        Printer.printAppNameVersion(new PrintListenerImpl(SettleActivity.this), true, !isOnSettlementPrintEnabled);
                        if (!isWallet && cnt > 0 || (isAllSelected || settleAcqList.size() > 1)) {
                            Printer.printSpace(SettleActivity.this, !isOnSettlementPrintEnabled);
                        }

                    } else {
                        if (successSettleList.size()>0) {
                            Printer.printAppNameVersion(new PrintListenerImpl(SettleActivity.this), true, !isOnSettlementPrintEnabled);
                        }
                        if (!isSubordinatedApp && !isWallet && cnt > 0) {
                            Printer.printSpace(SettleActivity.this, !isOnSettlementPrintEnabled);
                        }
                    }

                    if ((isSupportEReceipt && successSettleList.size() > 0)) {
                        Printer.printERMReport(SettleActivity.this, ermSuccessTotal, ermUnsuccessfulTotal, ermVoidSuccessTotal, ermVoidUnsuccessfulTotal, !isOnSettlementPrintEnabled);
                    }

                    // AET-256
                    // transProcessListenerImpl.onShowNormalMessage(genPromptMsg(cnt, settleAcqList.size()), Constants.SUCCESS_DIALOG_SHOW_TIME, true);
                    TransContext.getInstance().setCurrentAction(currAction);                // avoid interupt finish with wrong Action
                    if (settleFailHosts != null && !settleFailHosts.isEmpty()) {
                        String[] acqs = settleFailHosts.toArray(new String[settleFailHosts.size()]);
                        transProcessListenerImpl.onHideProgress();
                        transProcessListenerImpl.onShowErrMessage(getString(R.string.settle_fail_host,
                                Arrays.toString(acqs).replace("[", "").replace("]", "")), Constants.FAILED_DIALOG_SHOW_TIME, true);
                        Log.d("SETTLEMENT", "Finish--Settlement-Failed");
                        finish(new ActionResult(TransResult.ERR_SETTLEMENT_FAIL, null));
                    } else {
                        Log.d("SETTLEMENT", "Finish--Success");
                        finish(new ActionResult(TransResult.SUCC, null));
                    }
                } finally {
                    //AET-41, AET-62, AET-280
                    FinancialApplication.getAcqManager().setCurAcq(FinancialApplication.getAcqManager().findAcquirer(acquirerDef));
                }

                Log.d("SETTLEMENT", "SettleActivity---END");
            }
        });
    }


    private void onResetView(final SettleAcquirer settleAcquirer) {
        FinancialApplication.getApp().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setCurrAcquirerContent(settleAcquirer);
            }
        });
    }

    private boolean isCompleteSettle(int ret, TransOnline transOnline, TransProcessListener transProcessListenerImpl) {
        transProcessListenerImpl.onHideProgress();
        if (ret != TransResult.SUCC && ret != TransResult.SUCC_NOREQ_BATCH && ret != TransResult.ERR_RECONCILE_FAILED) {
            if (ret == TransResult.ERR_HOST_REJECT && transOnline.isSettleFail()) {
                Printer.printSettle(SettleActivity.this, getString(R.string.settle_title), total, true, transOnline.getRespErrorCode());
            } else {
                if (ret != TransResult.ERR_ABORTED) {
                    transProcessListenerImpl.onShowErrMessage(TransResultUtils.getMessage(ret), Constants.FAILED_DIALOG_SHOW_TIME, true);
                }
            }

            //Component.setSettleStatus(false, acquirer.getName());

            settleFailHosts.add(acquirer.getName());
            return false;
        }

        return true;
    }

    private void printDetail(final boolean isFailDetail) {
        final ConditionVariable cv2 = new ConditionVariable();
        FinancialApplication.getApp().runOnUiThread(new PrintDetailRunnable(isFailDetail, cv2));
        cv2.block();
    }

    private void processTleUPLogOn(final AAction currentAction) {
        final TransData transData = new TransData();
        Component.transInit(transData, acquirer);

        // TLE Logon
        transData.setTransType(ETransType.LOADTWK);
        ActionTleTransOnline tleTrans = new ActionTleTransOnline(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                FinancialApplication.getAcqManager().setCurAcq(acquirer);
                transData.setAcquirer(acquirer);
                ((ActionTleTransOnline) action).setParam(SettleActivity.this, transData);
            }
        });

        tleTrans.setEndListener(new AAction.ActionEndListener() {
            @Override
            public void onEnd(AAction action, ActionResult result) {
                if (result.getRet() == TransResult.SUCC && transData.getField62() != null) {
                    // Recover TWK
                    ActionRecoverKBankLoadTWK recTWK = new ActionRecoverKBankLoadTWK(new AAction.ActionStartListener() {

                        @Override
                        public void onStart(AAction action) {
                            ((ActionRecoverKBankLoadTWK) action).setParam(SettleActivity.this, transData.getField62());
                        }
                    });

                    recTWK.setEndListener(new AAction.ActionEndListener() {
                        @Override
                        public void onEnd(AAction action, ActionResult result) {
                            Component.incStanNo(transData);
                            transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
                            cv.open();
                            isFinishLoadTLE = true;
                        }
                    });
                    recTWK.execute();
                } else {
                    if (acquirer.isEnableUpi()) {
                        acquirer.setTWK(null);
                        FinancialApplication.getAcqManager().updateAcquirer(acquirer);
                    }
                    cv.open();
                    isFinishLoadTLE = true;
                }
                ActivityStack.getInstance().popTo(SettleActivity.this);
                TransContext.getInstance().getCurrentAction().setFinished(true);
                TransContext.getInstance().setCurrentAction(currentAction);
            }
        });
        tleTrans.execute();
    }

    private void eSettleUploadProcess(final AAction currentAction, String exReferenceNo) {
        localSettledRefNo = exReferenceNo;
        ActionEReceiptInfoUpload eSettleUpload = new ActionEReceiptInfoUpload(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionEReceiptInfoUpload) action).setParam(total, SettleActivity.this, ActionEReceiptInfoUpload.EReceiptType.ERECEIPT_SETTLE, localSettledRefNo);
            }
        });

        eSettleUpload.setEndListener(new AAction.ActionEndListener() {
            @Override
            public void onEnd(AAction action, ActionResult result) {
                if (result.getRet() == TransResult.SUCC) {
                    DialogUtils.showSuccMessage(SettleActivity.this,
                            getString(R.string.trans_ereceipt_settle_upload),
                            new OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface arg0) {
                                    // DeleteTempSettlementFile(ercmSettleTempFileName);
                                    cv.open();
                                }
                            }, Constants.SUCCESS_DIALOG_SHOW_TIME);
                } else {
                    DialogUtils.showErrMessage(SettleActivity.this,
                            getString(R.string.trans_ereceipt_settle_upload),
                            getString(R.string.msg_erm_upload_fail), new OnDismissListener() {

                                @Override
                                public void onDismiss(DialogInterface arg0) {
                                    cv.open();
                                }
                            }, Constants.FAILED_DIALOG_SHOW_TIME);
                }
                isSettlementReceiptuploaded = true;
                //setResult(result.getRet());
                ActivityStack.getInstance().popTo(SettleActivity.this);
                TransContext.getInstance().getCurrentAction().setFinished(true);
                TransContext.getInstance().setCurrentAction(currentAction);
            }
        });
        eSettleUpload.execute();
    }

    private void DeleteTempSettlementFile(String FileName) {
//        String path = "/sdcard/PAX/BPSLoader/ERCM/UnsettlementList" ;
        String path = EReceiptUtils.getERM_UnsettleExternalStorageDirectory();
        String fName = "/" + FileName;
        if (new File(path).exists()) {
            if (new File(path + fName).exists()) {
                new File(path + fName).delete();
            }
        }
    }

    @Override
    protected boolean onKeyBackDown() {
        finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
        return true;
    }

    public boolean confirmSettle() {
        final ConditionVariable cv = new ConditionVariable();
        result = false;
        FinancialApplication.getApp().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (confirmDialog != null) {
                    confirmDialog.dismiss();
                }
                confirmDialog = new CustomAlertDialog(SettleActivity.this, CustomAlertDialog.NORMAL_TYPE);
                confirmDialog.show();
                confirmDialog.setTimeout(30);
                confirmDialog.setTitleText(getString(R.string.settle_dlg_confirm));
                confirmDialog.setContentText("");
                confirmDialog.setCancelable(true);
                confirmDialog.setCanceledOnTouchOutside(false);
                confirmDialog.showCancelButton(true);
                confirmDialog.setCancelClickListener(new OnCustomClickListener() {

                    @Override
                    public void onClick(CustomAlertDialog alertDialog) {
                        result = false;
                        alertDialog.dismiss();
                    }
                });
                confirmDialog.showConfirmButton(true);
                confirmDialog.setConfirmClickListener(new OnCustomClickListener() {

                    @Override
                    public void onClick(CustomAlertDialog alertDialog) {
                        result = true;
                        alertDialog.dismiss();
                    }
                });
                confirmDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        if (result == false) {
                            Device.beepErr();
                        }
                        if (cv != null) {
                            cv.open();
                        }
                    }
                });
                confirmDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                            result = false;
                            dialog.dismiss();
                        } else if (keyCode == KeyEvent.KEYCODE_ENTER) {
                            result = true;
                            dialog.dismiss();
                        }
                        return true;
                    }
                });
                confirmDialog.show();

            }
        });
        cv.block();
        return result;
    }

    private void getEReceiptStatusBeforeSettlement() {
        if (isSupportEReceipt) {
            //Sale Success ERM :
            ermSuccessTotal = FinancialApplication.getTransDataDbHelper().countERMReport(false, false);
            long ermSuccessAdjust = FinancialApplication.getTransDataDbHelper().countERMReportForAdjust(false);
            ermSuccessTotal[0] = ermSuccessTotal[0] + ermSuccessAdjust;

            //Sale Unsuccessful ERM :
            ermUnsuccessfulTotal = FinancialApplication.getTransDataDbHelper().countERMReport(true, false);
            long ermUnsuccessfulAdjust = FinancialApplication.getTransDataDbHelper().countERMReportForAdjust(true);
            ermUnsuccessfulTotal[0] = ermUnsuccessfulTotal[0] + ermUnsuccessfulAdjust;

            //Void Success ERM :
            ermVoidSuccessTotal = FinancialApplication.getTransDataDbHelper().countERMReport(false, true);
            long[] ermVoidAdjustSuccessTotal = FinancialApplication.getTransDataDbHelper().countERMReportForVoidAdjust(false);
            ermVoidSuccessTotal[0] = ermVoidSuccessTotal[0] + ermVoidAdjustSuccessTotal[0];
            ermVoidSuccessTotal[1] = ermVoidSuccessTotal[1] + ermVoidAdjustSuccessTotal[1];

            //Void Unsuccessful ERM :
            ermVoidUnsuccessfulTotal = FinancialApplication.getTransDataDbHelper().countERMReport(true, true);
            long[] ermVoidAdjustUnsuccessfulTotal = FinancialApplication.getTransDataDbHelper().countERMReportForVoidAdjust(true);
            ermVoidUnsuccessfulTotal[0] = ermVoidUnsuccessfulTotal[0] + ermVoidAdjustUnsuccessfulTotal[0];
            ermVoidUnsuccessfulTotal[1] = ermVoidUnsuccessfulTotal[1] + ermVoidAdjustUnsuccessfulTotal[1];

            uploadList = FinancialApplication.getTransDataDbHelper().findAllTransDataWithEReceiptUploadStatus(true);
            Log.d(EReceiptUtils.TAG, "SETTLEMENT >> PENDING LIST COUNT = " + uploadList.toArray().length);
        }
    }

    private void updateErmSummaryReportPrintingParam(byte[] ermReport) {
        if (selectAcqs.contains(Constants.ACQ_AMEX)) {
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_AMEX);
            if ( FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_AMEX).isEnable()
                    && FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_AMEX).isEnableUploadERM()) {
                try {
                    String jsonStr = new String(ermReport, Charsets.UTF_8);
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    long saleSuccCount = Long.parseLong(jsonObj.getString("TRANS_COUNT_SALE_SUCC")) ;
                    long saleSuccAmount = Long.parseLong(jsonObj.getString("TRANS_AMOUNT_SALE_SUCC")) ;
                    ermSuccessTotal[0] += saleSuccCount;
                    ermSuccessTotal[1] += saleSuccAmount;

                    long saleFailCount = Long.parseLong(jsonObj.getString("TRANS_COUNT_SALE_FAIL")) ;
                    long saleFailAmount = Long.parseLong(jsonObj.getString("TRANS_AMOUNT_SALE_FAIL")) ;
                    ermUnsuccessfulTotal[0] += saleFailCount;
                    ermUnsuccessfulTotal[1] += saleFailAmount;

                    long voidSuccCount = Long.parseLong(jsonObj.getString("TRANS_COUNT_VOID_SUCC")) ;
                    long voidSuccAmount = Long.parseLong(jsonObj.getString("TRANS_AMOUNT_VOID_SUCC")) ;
                    ermVoidSuccessTotal[0] += voidSuccCount;
                    ermVoidSuccessTotal[1] += voidSuccAmount;

                    long voidFailCount = Long.parseLong(jsonObj.getString("TRANS_COUNT_VOID_FAIL")) ;
                    long voidFailAmount = Long.parseLong(jsonObj.getString("TRANS_AMOUNT_VOID_FAIL")) ;
                    ermVoidUnsuccessfulTotal[0] += voidFailCount;
                    ermVoidUnsuccessfulTotal[1] += voidFailAmount;
                } catch (Exception e) {

                }
            }
        }
    }

    private byte[] hasExternalAppErmReport(Acquirer acquirer) {
        String path = EReceiptUtils.getERM_UnsettleExternalStorageDirectory();
        File dir = new File(path);
        if (dir!=null && dir.exists() && dir.isDirectory()) {
            String[] nameList = dir.list();
            if (nameList!=null && nameList.length>0) {
                for (String fName : nameList) {
                    if (fName.endsWith(".esr") && fName.startsWith(acquirer.getNii()+acquirer.getName())) {
                        try {
                            File ermReportFile = new File(fName);
                            FileInputStream fIps = new FileInputStream(ermReportFile);
                            byte[] buffer = new byte[fIps.available()];
                            fIps.read(buffer, 0, buffer.length);

                            return buffer;
                        } catch (Exception ex) {
                           ex.printStackTrace();
                        }
                    }
                }
            }
        }

        return null;
    }

    private void printEreceiptTxnUploadFail() {
        Log.d(EReceiptUtils.TAG, "ERM-PRE-SETTLEMENT >> ON PRINTINGTXNUPLOADFAIL");
        if (isSupportEReceipt) {
            if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE_FORCE_SETTLE_PRINT_ALL_TRANS)) {
                uploadList = FinancialApplication.getTransDataDbHelper().findAllTransDataErmPendingUpload(false);
                Log.d(EReceiptUtils.TAG, "SETTLEMENT >> PRINT ALL TRANS >> PENDING LIST COUNT = " + uploadList.toArray().length);
            } else {
                uploadList = FinancialApplication.getTransDataDbHelper().findAllTransDataErmPendingUpload(true);
                Log.d(EReceiptUtils.TAG, "SETTLEMENT >> NEVER PRINT ONLY >> PENDING LIST COUNT = " + uploadList.toArray().length);
            }

            for (TransData txnPrint : uploadList) {
                String tmpDateTime = txnPrint.getDateTime();
                String tmpRefNo = txnPrint.getRefNo();
                String tmpTxnNo = txnPrint.getTxnNo();
                TransData.ETransStatus tmpTransState = txnPrint.getTransState();
                if (tmpTransState == TransData.ETransStatus.VOIDED) {
                    TransData origTxnPrint = FinancialApplication.getTransDataDbHelper().findTransDataByStanNo(txnPrint.getVoidStanNo(), false);
                    txnPrint.setTransState(TransData.ETransStatus.NORMAL);
                    txnPrint.setDateTime((origTxnPrint.getDateTime()!=null) ? origTxnPrint.getDateTime() : tmpDateTime);
                    txnPrint.setRefNo(origTxnPrint.getRefNo());
                    txnPrint.setTxnNo(origTxnPrint.getTxnNo());
                }

                /* bypass ERM can print signature image on slip (only settlement transaction) */
                if (!txnPrint.getAcquirer().getName().equals(Constants.ACQ_KPLUS)
                        && !txnPrint.getAcquirer().getName().equals(Constants.ACQ_ALIPAY)
                        && !txnPrint.getAcquirer().getName().equals(Constants.ACQ_WECHAT)
                        && !txnPrint.getAcquirer().getName().equals(Constants.ACQ_QR_CREDIT)) {
                    txnPrint.setSettlementErmReprintMode((txnPrint.isEReceiptReprint()) ? TransData.SettleErmReprintMode.ShowReprint_ShowSignature : TransData.SettleErmReprintMode.HideReprint_ShowSignature);
                }

                boolean allowPrinting = false;
                if ((FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE_FORCE_SETTLE_PRINT_ALL_TRANS) == true)
                        || (FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE_FORCE_SETTLE_PRINT_ALL_TRANS) == false
                        && txnPrint.getNumberOfErmPrintingCount() == 0)) {
                    allowPrinting = true;
                    if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE_FORCE_SETTLE_PRINT_ALL_TRANS) == true) {
                        Log.d(EReceiptUtils.TAG, " ERM-PRE-SETTLEMENT >> PRINT ALLOW >> FORCE PRINT ALL TRANS. MODE");
                    } else {
                        Log.d(EReceiptUtils.TAG, " ERM-PRE-SETTLEMENT >> PRINT ALLOW >> PRINT TICKET COUNT = 0");
                    }
                } else {
                    allowPrinting = false;
                    Log.d(EReceiptUtils.TAG, " ERM-PRE-SETTLEMENT >> SKIP PRINTING.");
                }

                if (allowPrinting) {
                    Log.d(EReceiptUtils.TAG, "ERM-PRE-SETTLEMENT >> ALLOW PRINTING");
                    switch (txnPrint.getAcquirer().getName()) {
                        case Constants.ACQ_REDEEM:
                        case Constants.ACQ_REDEEM_BDMS:
                            Printer.printEReceiptSlip(ReceiptPrintTransForERM.PrintType.REDEEM, txnPrint, txnPrint.isEReceiptReprint(), false, SettleActivity.this);
                            break;
                        case Constants.ACQ_SMRTPAY:
                        case Constants.ACQ_SMRTPAY_BDMS:
                        case Constants.ACQ_DOLFIN_INSTALMENT:
                            Printer.printEReceiptSlip(ReceiptPrintTransForERM.PrintType.SMARTPAY, txnPrint, txnPrint.isEReceiptReprint(), false, SettleActivity.this);
                            break;
                        default:
                            Printer.printEReceiptSlip(ReceiptPrintTransForERM.PrintType.DEFAULT, txnPrint, txnPrint.isEReceiptReprint(), false, SettleActivity.this);
                            break;
                    }
                    if (tmpTransState == TransData.ETransStatus.VOIDED) {
                        txnPrint.setTransState(tmpTransState);
                        txnPrint.setDateTime(tmpDateTime);
                        txnPrint.setRefNo(tmpRefNo);
                        txnPrint.setTxnNo(tmpTxnNo);
                    }
                    txnPrint.increaseNumberOfErmPrintingCount();
                    FinancialApplication.getTransDataDbHelper().updateTransData(txnPrint);
                }
            }
        } else {
            Log.d(EReceiptUtils.TAG, "ERM-PRE-SETTLEMENT >> ERM MODE WAS DISABLED");
        }

    }

    private class PrintDetailRunnable implements Runnable {
        final ConditionVariable cv;
        final boolean isFailDetail;

        PrintDetailRunnable(boolean isFailDetail, ConditionVariable cv) {
            this.isFailDetail = isFailDetail;
            this.cv = cv;
        }

        @Override
        public void run() {
            final CustomAlertDialog dialog = new CustomAlertDialog(SettleActivity.this,
                    CustomAlertDialog.IMAGE_TYPE);
            String info = getString(R.string.settle_print_detail_or_not);
            if (isFailDetail) {
                info = getString(R.string.settle_print_fail_detail_or_not);
            }
            //AET-76
            dialog.setTimeout(30);
            dialog.setContentText(info);
            dialog.setOnKeyListener(new DialogInterface.OnKeyListener() { // AET-77
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        dialog.dismiss();
                        cv.open();
                        return true;
                    }
                    return false;
                }
            });
            dialog.setCancelClickListener(new OnCustomClickListener() {
                @Override
                public void onClick(CustomAlertDialog alertDialog) {
                    dialog.dismiss();
                    cv.open();
                }
            });
            dialog.setConfirmClickListener(new OnCustomClickListener() {
                @Override
                public void onClick(CustomAlertDialog alertDialog) {
                    dialog.dismiss();
                    FinancialApplication.getApp().runInBackground(new Runnable() {

                        @Override
                        public void run() {

                            int result;
                            if (isFailDetail) {
                                // 打印失败交易明细
                                result = Printer.printFailDetail(getString(R.string.print_offline_send_failed),
                                        SettleActivity.this);
                            } else {
                                // 打印交易明细
                                result = Printer.printTransDetail(getString(R.string.print_settle_detail),
                                        SettleActivity.this, acquirer);
                            }
                            if (result != TransResult.SUCC) {
                                DialogUtils.showErrMessage(SettleActivity.this,
                                        getString(R.string.transType_print),
                                        getString(isFailDetail ?
                                                R.string.err_no_failed_trans :
                                                R.string.err_no_succ_trans), new OnDismissListener() {

                                            @Override
                                            public void onDismiss(DialogInterface arg0) {
                                                cv.open();
                                            }
                                        }, Constants.FAILED_DIALOG_SHOW_TIME);

                            } else {
                                cv.open();
                            }

                        }
                    });

                }
            });

            dialog.show();
            dialog.setImage(BitmapFactory.decodeResource(getResources(), R.drawable.ic19));
        }
    }

    private int processDolfinSettle(final AAction currentAction, TransProcessListener transProcessListenerImpl) {
        isDolfinSettleResult = TransResult.ERR_SETTLE_NOT_COMPLETED;
        ActionDolfinSettle actionDolfinSettle = new ActionDolfinSettle(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionDolfinSettle) action).setParam(SettleActivity.this);
            }
        });
        actionDolfinSettle.setEndListener(new AAction.ActionEndListener() {
            @Override
            public void onEnd(AAction action, ActionResult result) {
                SettlementRegisterActivity.Companion.updateSettleTime(acquirer.getName());
                if ((Integer) result.getData() == 0) {
                    isDolfinSettleResult = 0;
                }
                ActivityStack.getInstance().popTo(SettleActivity.this);
                TransContext.getInstance().getCurrentAction().setFinished(true);
                TransContext.getInstance().setCurrentAction(currentAction);
                cv.open();
            }

        });
        actionDolfinSettle.execute();
        return isDolfinSettleResult;
    }

    private int processKCheckIDSettle(final AAction currentAction, Acquirer acquirer,TransProcessListener listener) {

        // set default settle result
        iKCheckIDResult = TransResult.ERR_SETTLEMENT_FAIL;

        ActionKCheckIDUpdateParam actionKCheckIDUpdateParam = new ActionKCheckIDUpdateParam(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                if (listener!=null) {listener.onShowProgress("push configuration to KCheckID...", Constants.FAILED_DIALOG_SHOW_TIME);}
                ((ActionKCheckIDUpdateParam) action).setParam(SettleActivity.this);
            }
        });
        actionKCheckIDUpdateParam.setEndListener(new AAction.ActionEndListener() {
            @Override
            public void onEnd(AAction kCheckIDUpDateParamAction, ActionResult result) {
                SettlementRegisterActivity.Companion.updateSettleTime(acquirer.getName());
                int updateParamResult = result.getRet() ;
                if (updateParamResult==TransResult.SUCC) {

                    // process getRecordCount
                    getKCheckIDCountRecord(currentAction, acquirer, listener, addAActionToArrayList(null, kCheckIDUpDateParamAction) );

                } else {
                    if (listener!=null) {listener.onShowProgress("Update parameter failed.", Constants.FAILED_DIALOG_SHOW_TIME);}
                    Log.d("SETTLEMENT", "KCHECKID-SETTLEMENT-API--END-[UPDATE PARAM ERROR]");
                    ActivityStack.getInstance().popTo(SettleActivity.this);
                    TransContext.getInstance().getCurrentAction().setFinished(true);
                    TransContext.getInstance().setCurrentAction(currentAction);
                    cv.open();
                }
            }
        });
        actionKCheckIDUpdateParam.execute();
        Log.d("SETTLEMENT", "KCHECKID-SEND-CONFIG-API--START");
        return iKCheckIDResult;
    }

    private void getKCheckIDCountRecord(AAction currentAction, Acquirer acquirer, TransProcessListener listener,  ArrayList<AAction> aActionsList) {
        ActionGetKCheckIDRecordCount actionGetKCheckIDRecordCount = new ActionGetKCheckIDRecordCount(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                if (listener!=null) {listener.onShowProgress("Retrieve KCheckID record, please wait...", Constants.FAILED_DIALOG_SHOW_TIME);}
                ((ActionGetKCheckIDRecordCount) action).setParam(SettleActivity.this);
            }
        });
        actionGetKCheckIDRecordCount.setEndListener(new AAction.ActionEndListener() {
            @Override
            public void onEnd(AAction kCheckIDRecordCountAction, ActionResult result) {
                int recordCountKCheckID = 0;
                if (result!=null && result.getData()!=null && result.getData() instanceof Integer) {
                    recordCountKCheckID = (int)result.getData();
                }

                if (recordCountKCheckID > 0) {

                    // Run KCheckID settlement process
                    executKCheckIDSettlement(currentAction, acquirer, listener, addAActionToArrayList(aActionsList, kCheckIDRecordCountAction));

                } else {
                    if (listener!=null) {listener.onShowProgress("No KCheckID trans for settlement.", Constants.FAILED_DIALOG_SHOW_TIME);}
                    Log.d("SETTLEMENT", "KCHECKID-GET-RECORD-COUNT-API--END-[NO K-CHECK-ID RECORD]");
                    ActivityStack.getInstance().popTo(SettleActivity.this);
                    TransContext.getInstance().getCurrentAction().setFinished(true);
                    TransContext.getInstance().setCurrentAction(currentAction);
                    cv.open();
                }
            }
        });
        actionGetKCheckIDRecordCount.execute();
        Log.d("SETTLEMENT", "KCHECKID-SEND-CONFIG-API--START");
    }

    private void executKCheckIDSettlement(AAction currentAction, Acquirer acquirer, TransProcessListener listener,  ArrayList<AAction> aActionsList) {


        ActionKCheckIDSettlement actionKCheckIDSettlement = new ActionKCheckIDSettlement(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction settleAction) {
                if (listener!=null) {listener.onShowProgress("KCheckID settlement, please wait...", Constants.FAILED_DIALOG_SHOW_TIME);}
                ((ActionKCheckIDSettlement) settleAction).setParam(SettleActivity.this);
            }
        });
        actionKCheckIDSettlement.setEndListener(new AAction.ActionEndListener() {
            @Override
            public void onEnd(AAction kCheckIDSettleAction, ActionResult settleResult) {
                Log.d("SETTLEMENT", "KCHECKID-SETTLEMENT-API--END");
                iKCheckIDResult = settleResult.getRet();

                if (iKCheckIDResult==TransResult.SUCC && settleResult.getData()!=null) {
                    int currBatch = acquirer.getCurrBatchNo();
                    int kCheckIDBatchNo = Integer.parseInt((String)settleResult.getData());
                    acquirer.setCurrBatchNo(kCheckIDBatchNo);
                    FinancialApplication.getAcqManager().updateAcquirer(acquirer);
                    if (listener!=null) {listener.onShowProgress("update batch to KCheckID host.", Constants.FAILED_DIALOG_SHOW_TIME);}

                    if (acquirer.getEnableUploadERM()) {
                        // keep Settlement report to disk
                        try {
                            String AcqIndex = EReceiptUtils.StringPadding(String.valueOf(acquirer.getId()), 3, "0", Convert.EPaddingPosition.PADDING_LEFT);
                            String AcqCurrBatch = Component.getPaddedNumber(currBatch, 6);
                            ercmSettledRefNo = Device.getTime(Constants.TIME_PATTERN_TRANS).substring(2, 14);
                            PackEReceiptSettleUpload settleReport = new PackEReceiptSettleUpload(null);
                            EReceiptUtils.setUnSettleRawData(acquirer, ercmSettledRefNo, settleReport.generateReceiptFormat(AcqIndex));
                            if (listener!=null) {listener.onShowProgress("Generate KCheck ERM-Settle report success", Constants.FAILED_DIALOG_SHOW_TIME);}
                            Log.d("SETTLEMENT", "GENERATE-KCHECKID-E-SETTLE-REPORT SUCCESS");
                        } catch (Exception e) {
                            e.printStackTrace();
                            if (listener!=null) {listener.onShowProgress("Generate KCheck ERM-Settle report failed", Constants.FAILED_DIALOG_SHOW_TIME);}
                            Log.e("SETTLEMENT", "GENERATE-KCHECKID-E-SETTLE-REPORT FAILED");
                        }
                    }

                    if (listener!=null) {listener.onShowProgress("BATCH CLOSED", Constants.SUCCESS_DIALOG_SHOW_TIME);}
                    Log.d("SETTLEMENT", "KCHECKID-SETTLEMENT-API--END-[SUCCESS]");
                }
                else {
                    if (listener!=null) {listener.onShowProgress("KCHECKID SETTLEMENT FAILED", Constants.FAILED_DIALOG_SHOW_TIME);}
                    Log.d("SETTLEMENT", "KCHECKID-SETTLEMENT-API--END-[SETTLEMENT FAILED]");
                }

                ActivityStack.getInstance().popTo(SettleActivity.this);
                setFinishedToAActionArrayList(addAActionToArrayList(aActionsList, kCheckIDSettleAction));
                TransContext.getInstance().setCurrentAction(currentAction);
                cv.open();
            }
        });
        Log.d("SETTLEMENT", "KCHECKID-SETTLEMENT-API--START");
        actionKCheckIDSettlement.execute();
    }

    private ArrayList<AAction> addAActionToArrayList(ArrayList<AAction> arrList, @NonNull AAction targetAAction) {
        if (arrList == null) {arrList = new ArrayList<AAction>();}
        arrList.add(targetAAction);
        return arrList;
    }

    private void setFinishedToAActionArrayList(ArrayList<AAction> aActionArrayList) {
        if (aActionArrayList!=null && aActionArrayList.size() > 0) {
            for (AAction targAAction : aActionArrayList) {
                if (targAAction!=null) {
                    targAAction.setFinished(true);
                }
            }
        }
    }



    private void processScbIppSettle(final AAction currentAction) {
        isScbIppResult = TransResult.ERR_SETTLE_NOT_COMPLETED;
        ActionScbUpdateParam actionScbUpdateParam = new ActionScbUpdateParam(action ->
                ((ActionScbUpdateParam) action).setParam(SettleActivity.this)
        );
        actionScbUpdateParam.setEndListener((action, result) -> {
            SettlementRegisterActivity.Companion.updateSettleTime(acquirer.getName());
            if ((isScbIppResult = result.getRet()) == TransResult.SUCC) {
                ActionScbIppLink actionScbIppLink = new ActionScbIppLink(action1 ->
                        ((ActionScbIppLink) action1).setParam(SettleActivity.this, ActionScbIppLink.scbIppLinkType.SETTLEMENT, Constants.ACQ_SCB_IPP)
                );
                actionScbIppLink.setEndListener((action12, result1) -> {
                    Log.d("SETTLEMENT", "SCB-SETTLEMENT-API--END");
                    isScbIppResult = result1.getRet();
                    if (isScbIppResult == TransResult.SUCC) {
                        FinancialApplication.getTransDataDbHelper().deleteAllTransData(total.getAcquirer());
                        FinancialApplication.getTransMultiAppDataDbHelper().deleteAllTransData(total.getAcquirer());
                        Component.incBatchNo(total.getAcquirer());
                        Component.setSettleStatus(Controller.Constant.WORKED, total.getAcquirer().getName());
                    }
                    ActivityStack.getInstance().popTo(SettleActivity.this);
                    TransContext.getInstance().getCurrentAction().setFinished(true);
                    TransContext.getInstance().setCurrentAction(currentAction);
                    cv.open();
                });
                actionScbIppLink.execute();
            } else {
                Log.d("SETTLEMENT", "SCB-SETTLEMENT-API--END-[UPDATE PARAM ERROR]");
                ActivityStack.getInstance().popTo(SettleActivity.this);
                TransContext.getInstance().getCurrentAction().setFinished(true);
                TransContext.getInstance().setCurrentAction(currentAction);
                cv.open();
            }
        });
        actionScbUpdateParam.execute();
        Log.d("SETTLEMENT", "SCB-SETTLEMENT-API--START");
    }

    private void processScbRedeemSettle(final AAction currentAction) {
        isScbRedeemResult = TransResult.ERR_SETTLE_NOT_COMPLETED;
        ActionScbUpdateParam actionScbUpdateParam = new ActionScbUpdateParam(action ->
                ((ActionScbUpdateParam) action).setParam(SettleActivity.this)
        );
        actionScbUpdateParam.setEndListener((action, result) -> {
            SettlementRegisterActivity.Companion.updateSettleTime(acquirer.getName());
            if ((isScbRedeemResult = result.getRet()) == TransResult.SUCC) {
                ActionScbIppLink actionScbIppLink = new ActionScbIppLink(action1 ->
                        ((ActionScbIppLink) action1).setParam(SettleActivity.this, ActionScbIppLink.scbIppLinkType.SETTLEMENT, Constants.ACQ_SCB_REDEEM)
                );
                actionScbIppLink.setEndListener((action12, result1) -> {
                    Log.d("SETTLEMENT","SCB-REDEEM-SETTLEMENT-API--END");
                    isScbRedeemResult = result1.getRet();
                    if (isScbRedeemResult == TransResult.SUCC) {
                        FinancialApplication.getTransDataDbHelper().deleteAllTransData(total.getAcquirer());
                        FinancialApplication.getTransMultiAppDataDbHelper().deleteAllTransData(total.getAcquirer());
                        Component.incBatchNo(total.getAcquirer());
                        Component.setSettleStatus(Controller.Constant.WORKED, total.getAcquirer().getName());
                    }
                    ActivityStack.getInstance().popTo(SettleActivity.this);
                    TransContext.getInstance().getCurrentAction().setFinished(true);
                    TransContext.getInstance().setCurrentAction(currentAction);
                    cv.open();
                });
                actionScbIppLink.execute();
            } else {
                Log.d("SETTLEMENT","SCB-REDEEM-SETTLEMENT-API--END-[UPDATE PARAM ERROR]");
                ActivityStack.getInstance().popTo(SettleActivity.this);
                TransContext.getInstance().getCurrentAction().setFinished(true);
                TransContext.getInstance().setCurrentAction(currentAction);
                cv.open();
            }
        });
        actionScbUpdateParam.execute();
        Log.d("SETTLEMENT","SCB-REDEEM-SETTLEMENT-API--START");
    }

    private void processAmexSettle(final AAction currentAction) {
        isAmexApiResult = TransResult.ERR_SETTLE_NOT_COMPLETED;
        ActionAmexSettle actionAmexSettle = new ActionAmexSettle(action -> ((ActionAmexSettle) action).setParam(SettleActivity.this));
        actionAmexSettle.setEndListener((action, result) -> {
            Log.d("SETTLEMENT","AMEX-SETTLEMENT-API--END");
            isAmexApiResult = result.getRet();
            SettlementRegisterActivity.Companion.updateSettleTime(acquirer.getName());
            if (isAmexApiResult == TransResult.SUCC) {
                FinancialApplication.getTransDataDbHelper().deleteAllTransData(total.getAcquirer());
                FinancialApplication.getTransMultiAppDataDbHelper().deleteAllTransData(total.getAcquirer());
                Component.incBatchNo(total.getAcquirer());
                Component.setSettleStatus(Controller.Constant.WORKED, total.getAcquirer().getName());
                if (result.getData1()!=null) {
                    updateErmSummaryReportPrintingParam((byte[])result.getData1());
                }
            }
            ActivityStack.getInstance().popTo(SettleActivity.this);
            TransContext.getInstance().getCurrentAction().setFinished(true);
            TransContext.getInstance().setCurrentAction(currentAction);
            cv.open();
        });
        actionAmexSettle.execute();
        Log.d("SETTLEMENT","AMEX-SETTLEMENT-API--START");
    }

    @Override
    public void finish(ActionResult result) {
        if (isEcrProcess && settleAcqList.size() == 1) {
            if (result != null) {
                if (result.getRet() == TransResult.SUCC) {
                    EcrData.instance.setSettleSingleHostTotalData(total, "00");
                }
                else if (result.getRet() == TransResult.ERR_USER_CANCEL) {
                    EcrData.instance.setSettleSingleHostTotalData(total, "CL");
                }
                else if (result.getRet() == TransResult.ERR_NO_TRANS) {
                    EcrData.instance.setSettleSingleHostTotalData(total, "ND");
                }
            }
        }

        super.finish(result);
    }

    private class SettleAcquirer {
        private Acquirer acquirer;
        private TransTotal total;
        private boolean isEmptyTrans;

        SettleAcquirer(Acquirer acquirer, TransTotal total, boolean isEmptyTrans) {
            this.acquirer = acquirer;
            this.total = total;
            this.isEmptyTrans = isEmptyTrans;
        }

        public Acquirer getAcquirer() {
            return acquirer;
        }

        public void setAcquirer(Acquirer acquirer) {
            this.acquirer = acquirer;
        }

        public TransTotal getTotal() {
            return total;
        }

        public void setTotal(TransTotal total) {
            this.total = total;
        }

        public boolean isEmptyTrans() {
            return isEmptyTrans;
        }

        public void setEmptyTrans(boolean emptyTrans) {
            isEmptyTrans = emptyTrans;
        }
    }

}
