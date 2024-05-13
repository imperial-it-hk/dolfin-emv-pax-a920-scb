package com.pax.pay.trans;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.appstore.DownloadManager;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.ECR.EcrData;
import com.pax.pay.ECR.LawsonHyperCommClass;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.Issuer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.action.ActionEnterAmount;
import com.pax.pay.trans.action.ActionEnterRef1Ref2;
import com.pax.pay.trans.action.ActionGetQRReversal;
import com.pax.pay.trans.action.ActionGetQrFromKPlusReceipt;
import com.pax.pay.trans.action.ActionGetQrFromServer;
import com.pax.pay.trans.action.ActionInquiry;
import com.pax.pay.trans.action.ActionShowQRCodeWallet;
import com.pax.pay.trans.action.ActionVerifyPaySlipThaiQr;
import com.pax.pay.trans.action.activity.EcrPaymentSelectActivity;
import com.pax.pay.trans.model.AcqManager;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.task.PrintTask;
import com.pax.pay.utils.QrTag31Utils;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import th.co.bkkps.utils.Log;

//import com.pax.pay.trans.action.ActionQrSaleInquiry;

/**
 * Created by NANNAPHAT S on 16/1/2019.
 */
public class KplusQrSaleTrans extends BaseTrans {
    private String mAmount = null;
    private ActionResult lastResult;
    private String refSaleID = null;
    private boolean isPosManualInquiry = false;
    private boolean isSuccByVerifyPaySlip = false;
    private int inquiryCounter = 0 ;
    private boolean isShowVerifyQRBtn = false;

    public KplusQrSaleTrans(Context context, TransEndListener transListener) {
        super(context, ETransType.QR_INQUIRY, Constants.ACQ_KPLUS, transListener);
        setBackToMain(true);
    }

    public KplusQrSaleTrans(Context context, String amount, TransEndListener transListener) {
        super(context, ETransType.QR_INQUIRY, Constants.ACQ_KPLUS, transListener);
        setBackToMain(true);
        this.mAmount = amount;
    }

    public KplusQrSaleTrans(Context context, String amount, String refSaleID, boolean isPosManualInquiry, TransEndListener transListener) {
        super(context, ETransType.QR_INQUIRY, Constants.ACQ_KPLUS, transListener);
        setBackToMain(true);
        this.refSaleID = refSaleID;
        this.isPosManualInquiry = isPosManualInquiry;
        this.mAmount = amount;
    }

    @Override
    protected void bindStateOnAction() {

        ActionEnterAmount amountAction = new ActionEnterAmount(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionEnterAmount) action).setParam(getCurrentContext(),
                        getString(R.string.menu_qr_thai_qr), false);
            }
        });
        bind(KplusQrSaleTrans.State.ENTER_AMOUNT.toString(), amountAction, true);

        ActionEnterRef1Ref2 actionEnterRef1Ref2 = new ActionEnterRef1Ref2(action ->
                ((ActionEnterRef1Ref2) action).setParam(getCurrentContext(), getString(R.string.menu_sale))
        );
        bind(State.ENTER_REF1_REF2.toString(), actionEnterRef1Ref2, true);

        ActionGetQrFromServer getQrFromServer = new ActionGetQrFromServer(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionGetQrFromServer) action).setParam(getCurrentContext(),
                        /*getString(R.string.menu_wallet_sale),*/ transData);
            }
        });
        bind(KplusQrSaleTrans.State.GEN_QR.toString(), getQrFromServer);

//        ActionGetQRReversal GetQRReversal = new ActionGetQRReversal(new AAction.ActionStartListener() {
//            @Override
//            public void onStart(AAction action) {
//                ((ActionGetQRReversal) action).setParam(getCurrentContext(),
//                        getString(R.string.menu_qr_prompt), transData);
//            }
//        });
//        bind(State.REVERSAL_QR.toString(), GetQRReversal);

        ActionInquiry qrSaleInquiry = new ActionInquiry(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionInquiry) action).setParam(getCurrentContext(),
                        getString(R.string.menu_qr_thai_qr), transData);
            }
        });
        bind(State.INQUIRY_QR.toString(), qrSaleInquiry);

        ActionShowQRCodeWallet showQRCodeWallet = new ActionShowQRCodeWallet(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionShowQRCodeWallet) action).setParam(getCurrentContext(),
                        getString(R.string.menu_qr_thai_qr), R.string.trans_dynamic_qr, isPosManualInquiry, transData, isShowVerifyQRBtn);
            }
        });
        bind(State.SHOW_CODE.toString(), showQRCodeWallet);

        ActionShowQRCodeWallet checkQRCodeWallet = new ActionShowQRCodeWallet(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                transData.setAmount(mAmount);
                ((ActionShowQRCodeWallet) action).setParam(getCurrentContext(),
                        getString(R.string.menu_qr_thai_qr), R.string.trans_dynamic_qr, isPosManualInquiry, transData, isShowVerifyQRBtn);
            }
        });
        bind(State.CHECKQR.toString(), checkQRCodeWallet);


        ActionGetQrFromKPlusReceipt getQrFromKPlusReceiptAction = new ActionGetQrFromKPlusReceipt(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionGetQrFromKPlusReceipt) action).setParam(getCurrentContext(), transData);
            }
        });
        bind(State.GET_QR_PAYSLIP.toString(), getQrFromKPlusReceiptAction);


        ActionVerifyPaySlipThaiQr verifyPaySlipThaiQr = new ActionVerifyPaySlipThaiQr(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionVerifyPaySlipThaiQr) action).setParam(getCurrentContext(), transData);
            }
        });
        bind(State.VERIFY_QR_PAY_SLIP.toString(), verifyPaySlipThaiQr);


//        ActionCheckQRKplus checkQR = new ActionCheckQRKplus(new AAction.ActionStartListener() {
//            @Override
//            public void onStart(AAction action) {
//                ((ActionCheckQRKplus) action).setParam(getCurrentContext(),
//                        getString(R.string.menu_qr_thai_qr), transData);
//            }
//        });
//        bind(KplusQrSaleTrans.State.CHECKQR.toString(), checkQR, false);

        //print preview action
        PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(KplusQrSaleTrans.this, KplusQrSaleTrans.State.PRINT.toString()));
        bind(KplusQrSaleTrans.State.PRINT.toString(), printTask);

        // ERM Maximum TransExceed Check
        int ermExeccededResult = ErmLimitExceedCheck();
        if (ermExeccededResult != TransResult.SUCC) {
            transEnd(new ActionResult(ermExeccededResult,null));
            return;
        }

        int issuerSupportCheckResult = IssuerSupportTransactionCheck(Collections.singletonList(Constants.ISSUER_KPLUS));
        if (issuerSupportCheckResult != TransResult.SUCC) {
            transEnd(new ActionResult(issuerSupportCheckResult,null));
            return;
        }

        Acquirer acq = AcqManager.getInstance().findActiveAcquirer(Constants.ACQ_KPLUS);
        if (acq == null || !acq.isEnable()) {
            transEnd(new ActionResult(TransResult.ERR_UNSUPPORTED_FUNC, null));
            return;
        }

        if (mAmount == null) {
            gotoState(KplusQrSaleTrans.State.ENTER_AMOUNT.toString());
        } else {
            transData.setAmount(mAmount.replace(".", ""));

            // ECR mode set current host index
            if (FinancialApplication.getEcrProcess().mHyperComm instanceof LawsonHyperCommClass) {
                String hostIndex = LawsonHyperCommClass.getLawsonHostIndex(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_KPLUS));
                EcrData.instance.qr_hostIndex = Utils.checkStringContainNullOrEmpty(hostIndex).getBytes();
                EcrData.instance.qr_IssuerName = Constants.ECR_QR_PAYMENT.getBytes();
            }

            // Lemon farm extra param
            if (refSaleID != null) transData.setReferenceSaleID(refSaleID);

            int iMode = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_SUPPORT_REF1_2_MODE);
            DownloadManager.EdcRef1Ref2Mode mode = DownloadManager.EdcRef1Ref2Mode.getByMode(iMode);
            if (mode != DownloadManager.EdcRef1Ref2Mode.DISABLE) {
                gotoState(State.ENTER_REF1_REF2.toString());
            } else {
                gotoState(State.GEN_QR.toString());
            }
        }
    }

    enum State {
        ENTER_AMOUNT,
        ENTER_REF1_REF2,
        GEN_QR,
        SHOW_CODE,
        //REVERSAL_QR,
        INQUIRY_QR,
        GET_QR_PAYSLIP,
        VERIFY_QR_PAY_SLIP,
        CHECKQR,
        PRINT,
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {

        Log.d("KPLUS.Activity", String.format(" on state : %s ", currentState));
        Log.d("KPLUS.Activity", String.format(" \t\tresult = %s", result.getRet()));
        Log.d("KPLUS.Activity", String.format(" \t\t\t\tdata = %s", result.getData()));
        Log.d("KPLUS.Activity", String.format(" \t\t\t\tdata1 = %s", result.getData1()));
        KplusQrSaleTrans.State state = KplusQrSaleTrans.State.valueOf(currentState);

        switch (state) {
            case ENTER_AMOUNT:
                mAmount = result.getData().toString();
                transData.setAmount(result.getData().toString());

                int iMode = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_SUPPORT_REF1_2_MODE);
                DownloadManager.EdcRef1Ref2Mode mode = DownloadManager.EdcRef1Ref2Mode.getByMode(iMode);
                if (mode != DownloadManager.EdcRef1Ref2Mode.DISABLE) {
                    gotoState(State.ENTER_REF1_REF2.toString());
                } else {
                    gotoState(State.GEN_QR.toString());
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

                gotoState(State.GEN_QR.toString());
                break;
            case GEN_QR:
                if (result.getRet() == TransResult.SUCC) {
//                    qrCodeData = (String) result.getData();
                    //transData = (TransData) result.getData();
                    if (transData.isEcrProcess() && FinancialApplication.getEcrProcess().mHyperComm instanceof LawsonHyperCommClass) {
                        ECRProcReturn(null, new ActionResult(result.getRet(), null));
                    }
                    gotoState(State.SHOW_CODE.toString());
                } else {
                    transEnd(result);
                }
                break;
            case SHOW_CODE:
                lastResult = result;
                if (result.getRet() == TransResult.SUCC
                        || result.getRet() == TransResult.ERR_TIMEOUT
                        || result.getRet() == TransResult.ERR_USER_CANCEL) {
                    if (result.getRet() == TransResult.ERR_USER_CANCEL) {
                        transData.setLastTrans(true);
                    }
                    gotoState(State.INQUIRY_QR.toString());
                } else {
                    deleteThaiQrTrans(transData);                                                   // [EDCMGNT-1190]
                    transEnd(new ActionResult(lastResult.getRet(), null));                     // [EDCMGNT-1190]
//                    transData.setWalletRetryStatus(TransData.WalletRetryStatus.NORMAL);
//                    transData.setAmount(mAmount);
//                    gotoState(State.REVERSAL_QR.toString());
                }
                break;
//            case REVERSAL_QR:
//                transEnd(lastResult);
//                break;
            case INQUIRY_QR:
                toCheckQrOrPrint(result);
                break;
            case GET_QR_PAYSLIP:
                if (result.getRet() == TransResult.SUCC && result.getData()!=null) {
                    transData.setAmount(mAmount);
                    transData.setTransType(ETransType.QR_VERIFY_PAY_SLIP);
                    transData.setWalletVerifyPaySlipQRCode((String)result.getData());
                    gotoState(State.VERIFY_QR_PAY_SLIP.toString());
                } else {
                    transData.setTransType(ETransType.QR_INQUIRY);
                    gotoState(State.CHECKQR.toString());
                }
                break;
            case VERIFY_QR_PAY_SLIP:
                if (result.getRet() == TransResult.SUCC) {
                    transData.setAmount(mAmount);
                    String additionalData = (transData.isEcrProcess() && FinancialApplication.getEcrProcess().mHyperComm instanceof LawsonHyperCommClass) ? "QR-INQUIRY" : null;
                    ECRProcReturn(null, new ActionResult(result.getRet(), additionalData));
                    gotoState(State.PRINT.toString());
                } else {
                    transData.setTransType(ETransType.QR_INQUIRY);
                    FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                    gotoState(State.CHECKQR.toString());
                }

                break;
            case CHECKQR:
                lastResult = result;
                if (result.getRet() == TransResult.SUCC
                        || result.getRet() == TransResult.ERR_TIMEOUT
                        || result.getRet() == TransResult.ERR_USER_CANCEL) {
                    if (result.getRet() == TransResult.ERR_USER_CANCEL) {
                        transData.setLastTrans(true);
                    }
                    //transData = (TransData) result.getData();
                    gotoState(State.INQUIRY_QR.toString());
                } else if (result.getRet() == TransResult.VERIFY_THAI_QR_PAY_RECEIPT_REQUIRED) {
                    gotoState(State.GET_QR_PAYSLIP.toString());
                } else {
                    //ECRProcReturn(null, new ActionResult(result.getRet(), null));
                    //gotoState(AlipayQrSaleTrans.State.PRINT.toString());
                    transEnd(result);
                }
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

    @Override
    public void gotoState(String state) {
        if (transData.isEcrProcess() && FinancialApplication.getEcrProcess().mHyperComm instanceof LawsonHyperCommClass) {
            if (state.equals(State.PRINT.toString())) {
                if (transData != null) {
                    // set SaleReferenceID
                    EcrData.instance.saleReferenceIDR0 = Utils.checkStringContainNullOrEmpty(transData.getReferenceSaleID()).getBytes();
                    //EcrData.instance.RefNo = Utils.checkStringContainNullOrEmpty(transData.getRefNo()).getBytes();
                    // set HostIndex
                    //String hostIndex = LawsonHyperCommClass.getLawsonHostIndex(transData.getAcquirer());
                    //EcrData.instance.hostIndex = Utils.checkStringContainNullOrEmpty(hostIndex).getBytes();
                }
            } else if (state.equals(State.GEN_QR.toString())
                    || state.equals((State.INQUIRY_QR.toString()))
                    || state.equals((State.SHOW_CODE.toString()))) {
                if (transData != null) {
                    EcrData.instance.saleReferenceIDR0 = Utils.checkStringContainNullOrEmpty(refSaleID, Utils.LengthValidatorMode.EQUALS, 8, true, " ").getBytes();
                    EcrData.instance.walletType = EcrPaymentSelectActivity.SEL_PAYMENT_QR_TQR;
                    //EcrData.instance.RefNo = Utils.checkStringContainNullOrEmpty(transData.getRefNo()).getBytes();

                    //String hostIndex = LawsonHyperCommClass.getLawsonHostIndex(transData.getAcquirer());
                    //EcrData.instance.hostIndex = Utils.checkStringContainNullOrEmpty(hostIndex).getBytes();
                }
            }
        }

        super.gotoState(state);
    }

    private void toCheckQrOrPrint(ActionResult result) {


        if (result.getRet() == TransResult.SUCC) {

            // detect promoCode
            String promocode = transData.getPromocode() != null ? transData.getPromocode().trim() : "";

            // QR-TAG-31
            boolean isQRTag31Enable = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_QR_TAG_31_ENABLE, false);
            boolean isQRTag31OldReportStyle = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_QR_TAG_31_REPORT_GROUPING_OLD_STYLE, false);
            if (isQRTag31Enable) {
                if (isQRTag31OldReportStyle) {
                    if (!(promocode.toUpperCase().equals(Constants.ACQ_KPLUS) || promocode.toUpperCase().equals(Constants.ISSUER_KPLUS))) {
                        transData.setQrSourceOfFund(Constants.ISSUER_KPLUS);
                    } else {
                        Issuer issuer = FinancialApplication.getAcqManager().findIssuer(Constants.ISSUER_KPLUS_PROMPYPAY);
                        transData.setIssuer(issuer);
                        transData.setQrSourceOfFund(Constants.ISSUER_KPLUS_PROMPYPAY);
                    }
                } else {
                    transData.setQrSourceOfFund(promocode);
                }
            } else {
                if ("2".equalsIgnoreCase(promocode)) {
                    Issuer issuer = FinancialApplication.getAcqManager().findIssuer(Constants.ISSUER_KPLUS_PROMPYPAY);
                    transData.setIssuer(issuer);
                    transData.setQrSourceOfFund(Constants.ISSUER_KPLUS_PROMPYPAY);
                } else {
                    transData.setQrSourceOfFund(Constants.ISSUER_KPLUS);
                }
            }

            if ((EcrData.instance.isEcrProcess || EcrData.instance.isOnProcessing) && QrTag31Utils.Companion.isEcrReturnSourceOfFund()== QrTag31Utils.QRTAG31_TENDER_MODE.SOURCE_OF_FUNDS.getIntVal()) {
                EcrData.instance.CardIssuerName = promocode.getBytes(StandardCharsets.UTF_8);
            }

            Log.d("QRTAG31", " ----------------------- [QR-TAG-31 enabling = '" + isQRTag31Enable + "']");
            Log.d("QRTAG31", " ----------------------- [QR-TAG-31 report-old-style = '" + isQRTag31OldReportStyle + "']");
            Log.d("QRTAG31", " PromoCode = '" + promocode + "'");
            Log.d("QRTAG31", " Issuer    = '" + transData.getIssuer().getName() + "'");
            Log.d("QRTAG31", " PromoCode = '" + promocode + "'");
            Log.d("QRTAG31", " SourceOfFunds = '" + transData.getQrSourceOfFund() + "'");

            transData.setSignFree(true);
            transData.setWalletRetryStatus(TransData.WalletRetryStatus.NORMAL);
            FinancialApplication.getTransDataDbHelper().updateTransData(transData);
            String additionalData = (transData.isEcrProcess() && FinancialApplication.getEcrProcess().mHyperComm instanceof LawsonHyperCommClass) ? "QR-INQUIRY" : null;
            ECRProcReturn(null, new ActionResult(result.getRet(), additionalData));
            gotoState(KplusQrSaleTrans.State.PRINT.toString());
        } else {
            transData.setWalletRetryStatus(TransData.WalletRetryStatus.NORMAL);
            transData.setAmount(mAmount);
            if (lastResult.getRet() == TransResult.ERR_TIMEOUT
                    || lastResult.getRet() == TransResult.ERR_USER_CANCEL) {
                deleteThaiQrTrans(transData);                                                       // [EDCMGNT-1190]
                transEnd(new ActionResult(lastResult.getRet(), null));
                //gotoState(State.REVERSAL_QR.toString());
                return;
            }

            if (result.getRet() == TransResult.ERR_WALLET_RESP_UK) {

                // VERIFY-QR
                inquiryCounter +=1;
                int maxInqCountShowVerifyQrBtn = FinancialApplication.getSysParam().get(SysParam.NumberParam.THAI_QR_INQUIRY_MAX_COUNT_FOR_SHOW_VERIFY_QR_BUTTON, 2);
                isShowVerifyQRBtn = (inquiryCounter >= maxInqCountShowVerifyQrBtn);

                transData.setWalletRetryStatus(TransData.WalletRetryStatus.RETRY_CHECK);
                if (transData.isEcrProcess() && FinancialApplication.getEcrProcess().mHyperComm instanceof LawsonHyperCommClass) {
                    ECRProcReturn(null, new ActionResult(result.getRet(), null));
                }
                gotoState(KplusQrSaleTrans.State.CHECKQR.toString());
            } else {
                if (transData.isEcrProcess() && FinancialApplication.getEcrProcess().mHyperComm instanceof LawsonHyperCommClass) {
                    ECRProcReturn(null, new ActionResult(99, null));
                }
                gotoState(State.CHECKQR.toString());                                              // [EDCMGNT-1190]
//                if (transData.getIssuer().isAutoReversal()) {
//                    gotoState(State.REVERSAL_QR.toString());
//                } else {
//                    transEnd(result);
//                }
            }
        }
    }

    private void deleteThaiQrTrans(TransData targetDeleteTransData) {                               // [EDCMGNT-1190]
        if (targetDeleteTransData!=null && targetDeleteTransData.getId() >0) {
            FinancialApplication.getTransDataDbHelper().deleteTransData(targetDeleteTransData.getId());
        }
    }

    @Override
    protected void transEnd(ActionResult result) {
        mAmount = null;
        super.transEnd(result);
    }
}
