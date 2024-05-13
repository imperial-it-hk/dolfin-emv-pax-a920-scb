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
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.action.ActionAliWechatReversal;
import com.pax.pay.trans.action.ActionEnterAmount;
import com.pax.pay.trans.action.ActionEnterRef1Ref2;
import com.pax.pay.trans.action.ActionGetQrAlipayFromServer;
import com.pax.pay.trans.action.ActionInquiryAlipay;
import com.pax.pay.trans.action.ActionShowQRCodeWallet;
import com.pax.pay.trans.action.activity.EcrPaymentSelectActivity;
import com.pax.pay.trans.model.AcqManager;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.task.PrintTask;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.util.Collections;

import static com.pax.pay.service.AlipayWechatTransService.WalletTransType.REVERSAL_BYPASS;
import static com.pax.pay.service.AlipayWechatTransService.WalletTransType.REVERSAL_NORMAL;

//import com.pax.pay.trans.action.ActionQrSaleInquiry;

/**
 * Created by NANNAPHAT S on 28/12/2018.
 */
public class AlipayQrSaleTrans extends BaseTrans {
    private String mAmount = null;
    private ActionResult lastResult;
    private String refSaleID = null;
    private boolean isPosManualInquiry = false;

    public AlipayQrSaleTrans(Context context, TransEndListener transListener) {
        super(context, ETransType.GET_QR_ALIPAY, Constants.ACQ_ALIPAY, transListener);
        setBackToMain(true);
    }

    public AlipayQrSaleTrans(Context context, String amount, TransEndListener transListener) {
        super(context, ETransType.GET_QR_ALIPAY, Constants.ACQ_ALIPAY, transListener);
        setBackToMain(true);
        this.mAmount = amount;
    }


    public AlipayQrSaleTrans(Context context, String amount, String refSaleID, boolean isPosManualInquiry, TransEndListener transListener) {
        super(context, ETransType.GET_QR_ALIPAY, Constants.ACQ_ALIPAY, transListener);
        setBackToMain(true);
        this.refSaleID = refSaleID;
        this.isPosManualInquiry = isPosManualInquiry;
        this.mAmount = amount;
    }

    private String qrCodeData;

    @Override
    protected void bindStateOnAction() {

        ActionEnterAmount amountAction = new ActionEnterAmount(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionEnterAmount) action).setParam(getCurrentContext(),
                        getString(R.string.trans_alipay_sale), false);
            }
        });
        bind(AlipayQrSaleTrans.State.ENTER_AMOUNT.toString(), amountAction, true);

        ActionEnterRef1Ref2 actionEnterRef1Ref2 = new ActionEnterRef1Ref2(action ->
                ((ActionEnterRef1Ref2) action).setParam(getCurrentContext(), getString(R.string.menu_sale))
        );
        bind(State.ENTER_REF1_REF2.toString(), actionEnterRef1Ref2, true);

        ActionGetQrAlipayFromServer getQrFromServer = new ActionGetQrAlipayFromServer(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionGetQrAlipayFromServer) action).setParam(getCurrentContext(),
                        /*getString(R.string.menu_wallet_sale),*/ transData);
            }
        });
        bind(AlipayQrSaleTrans.State.GEN_QR.toString(), getQrFromServer);

//        ActionGetQRReversal GetQRReversal = new ActionGetQRReversal(new AAction.ActionStartListener() {
//            @Override
//            public void onStart(AAction action) {
//                ((ActionGetQRReversal) action).setParam(getCurrentContext(),
//                        getString(R.string.trans_alipay_sale), transData);
//            }
//        });
//        bind(State.REVERSAL_QR.toString(), GetQRReversal);

        ActionAliWechatReversal getQRReversal = new ActionAliWechatReversal(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionAliWechatReversal) action).setParam(getCurrentContext(), transData, REVERSAL_BYPASS);
            }
        });
        bind(State.REVERSAL_QR.toString(), getQRReversal, true);

        ActionAliWechatReversal reversalEnd = new ActionAliWechatReversal(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionAliWechatReversal) action).setParam(getCurrentContext(), transData, REVERSAL_NORMAL);
            }
        });
        bind(State.REVERSAL_END.toString(), reversalEnd, true);

        ActionInquiryAlipay qrSaleInquiry = new ActionInquiryAlipay(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionInquiryAlipay) action).setParam(getCurrentContext(),
                        getString(R.string.trans_alipay_sale), transData);
            }
        });
        bind(State.INQUIRY_QR.toString(), qrSaleInquiry);

        ActionShowQRCodeWallet showQRCodeWallet = new ActionShowQRCodeWallet(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionShowQRCodeWallet) action).setParam(getCurrentContext(),
                        getString(R.string.trans_alipay_sale), R.string.trans_dynamic_qr, isPosManualInquiry, transData);
            }
        });
        bind(State.SHOW_CODE.toString(), showQRCodeWallet);

        ActionShowQRCodeWallet checkQRCodeWallet = new ActionShowQRCodeWallet(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionShowQRCodeWallet) action).setParam(getCurrentContext(),
                        getString(R.string.trans_alipay_sale), R.string.trans_dynamic_qr, isPosManualInquiry, transData);
            }
        });
        bind(State.CHECKQR.toString(), checkQRCodeWallet);

        // ERM Maximum TransExceed Check
        int ermExeccededResult = ErmLimitExceedCheck();
        if (ermExeccededResult != TransResult.SUCC) {
            transEnd(new ActionResult(ermExeccededResult,null));
            return;
        }

        int issuerSupportCheckResult = IssuerSupportTransactionCheck(Collections.singletonList(Constants.ISSUER_ALIPAY));
        if (issuerSupportCheckResult != TransResult.SUCC) {
            transEnd(new ActionResult(issuerSupportCheckResult,null));
            return;
        }

//        ActionCheckQRAlipay checkQR = new ActionCheckQRAlipay(new AAction.ActionStartListener() {
//            @Override
//            public void onStart(AAction action) {
//                ((ActionCheckQRAlipay) action).setParam(getCurrentContext(),
//                        getString(R.string.trans_alipay_sale), transData);
//            }
//        });
//        bind(AlipayQrSaleTrans.State.CHECKQR.toString(), checkQR, false);

        //print preview action
        PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(AlipayQrSaleTrans.this, AlipayQrSaleTrans.State.PRINT.toString()));
        bind(AlipayQrSaleTrans.State.PRINT.toString(), printTask);

        Acquirer acq = AcqManager.getInstance().findActiveAcquirer(Constants.ACQ_ALIPAY);
        if (acq == null || !acq.isEnable()) {
            transEnd(new ActionResult(TransResult.ERR_UNSUPPORTED_FUNC, null));
            return;
        }

        if (mAmount == null) {
            gotoState(State.ENTER_AMOUNT.toString());
        } else {
            transData.setAmount(mAmount);

            // ECR mode set current host index
            if (FinancialApplication.getEcrProcess().mHyperComm instanceof LawsonHyperCommClass) {
                String hostIndex = LawsonHyperCommClass.getLawsonHostIndex(transData.getAcquirer());
                EcrData.instance.qr_hostIndex = Utils.checkStringContainNullOrEmpty(hostIndex).getBytes();
                EcrData.instance.qr_IssuerName = Constants.ISSUER_ALIPAY.getBytes();
            }

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
        REVERSAL_QR,
        REVERSAL_END,
        INQUIRY_QR,
        CHECKQR,
        PRINT,
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {

        AlipayQrSaleTrans.State state = AlipayQrSaleTrans.State.valueOf(currentState);

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
                    //qrCodeData = (String) result.getData();
                    //transData = (TransData) result.getData();
                    if (transData.isEcrProcess() && FinancialApplication.getEcrProcess().mHyperComm instanceof LawsonHyperCommClass) {
                        ECRProcReturn(null, new ActionResult(result.getRet(), null));
                    }
                    gotoState(State.SHOW_CODE.toString());
                } else if (result.getRet() == TransResult.ERR_NO_RESPONSE) {
                    gotoState(State.REVERSAL_QR.toString());
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
                    transData.setWalletRetryStatus(TransData.WalletRetryStatus.NORMAL);
                    transData.setAmount(mAmount);
                    gotoState(State.REVERSAL_QR.toString());
                }
                break;
            case REVERSAL_QR:
            case REVERSAL_END:
                transEnd(lastResult);
                break;
            case INQUIRY_QR:
                toCheckQrOrPrint(result);
                break;
            case CHECKQR:
                lastResult = result;
                if (result.getRet() == TransResult.SUCC
                        || result.getRet() == TransResult.ERR_TIMEOUT
                        || result.getRet() == TransResult.ERR_USER_CANCEL) {
                    //transData = (TransData) result.getData();
                    if (result.getRet() == TransResult.ERR_USER_CANCEL) {
                        transData.setLastTrans(true);
                    }
                    gotoState(State.INQUIRY_QR.toString());
                    //} else if (result.getRet() == TransResult.ERR_USER_CANCEL) {
                    //    transEnd(result);
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
            if (state.equals(KplusQrSaleTrans.State.PRINT.toString())) {
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
                    EcrData.instance.walletType = EcrPaymentSelectActivity.SEL_PAYMENT_QR_ALP;
                    //EcrData.instance.RefNo = Utils.checkStringContainNullOrEmpty(transData.getRefNo()).getBytes();

                    //String hostIndex = LawsonHyperCommClass.getLawsonHostIndex(transData.getAcquirer());
                    //EcrData.instance.hostIndex = Utils.checkStringContainNullOrEmpty(hostIndex).getBytes();
                }
            }
        }

        super.gotoState(state);
    }

//    private void toCheckQrOrPrint(ActionResult result){
//        if (result.getRet() == TransResult.SUCC) {
//            //Component.initField63Wallet(transData);
//            transData.setSignFree(true);
//            transData.setWalletRetryStatus(TransData.WalletRetryStatus.NORMAL);
//            FinancialApplication.getTransDataDbHelper().updateTransData(transData);
//            ECRProcReturn(null, new ActionResult(result.getRet(), null));
//            gotoState(AlipayQrSaleTrans.State.PRINT.toString());
//        } else {
//            transData.setWalletRetryStatus(TransData.WalletRetryStatus.NORMAL);
//            transData.setAmount(mAmount);
//            if (lastResult.getRet() == TransResult.ERR_TIMEOUT
//                    || lastResult.getRet() == TransResult.ERR_USER_CANCEL) {
//                gotoState(State.REVERSAL_QR.toString());
//                return;
//            }
//
//            if (result.getRet() == TransResult.ERR_WALLET_RESP_UK) {
//                transData.setWalletRetryStatus(TransData.WalletRetryStatus.RETRY_CHECK);
//                gotoState(AlipayQrSaleTrans.State.CHECKQR.toString());
//            } else {
//                if (transData.getIssuer().isAutoReversal()){
//                    gotoState(State.REVERSAL_QR.toString());
//                } else {
//                    transEnd(result);
//                }
//            }
//        }
//    }

    private void toCheckQrOrPrint(ActionResult result) {
        if (result.getRet() == TransResult.SUCC) {
            transData.setSignFree(true);
            transData.setWalletRetryStatus(TransData.WalletRetryStatus.NORMAL);
            FinancialApplication.getTransDataDbHelper().updateTransData(transData);
            String additionalData = (transData.isEcrProcess() && FinancialApplication.getEcrProcess().mHyperComm instanceof LawsonHyperCommClass) ? "QR-INQUIRY" : null;
            ECRProcReturn(null, new ActionResult(result.getRet(), additionalData));
            gotoState(State.PRINT.toString());
        } else if (result.getRet() == TransResult.ERR_ABORTED) {
            transEnd(result);
        } else {
            if (lastResult.getRet() == TransResult.SUCC) {//Show QR - Press OK Button
                transData.setWalletRetryStatus(TransData.WalletRetryStatus.RETRY_CHECK);
                if (transData.isEcrProcess() && FinancialApplication.getEcrProcess().mHyperComm instanceof LawsonHyperCommClass) {
                    ECRProcReturn(null, new ActionResult(result.getRet(), null));
                }
                gotoState(State.CHECKQR.toString());
            } else if (lastResult.getRet() == TransResult.ERR_USER_CANCEL) {//Show QR - Press Cancel Button
                gotoState(State.REVERSAL_END.toString());
            }
        }
    }

    @Override
    protected void transEnd(ActionResult result) {
        mAmount = null;
        super.transEnd(result);
    }
}
