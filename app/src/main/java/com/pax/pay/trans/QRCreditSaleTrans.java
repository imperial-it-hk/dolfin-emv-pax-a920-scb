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
import com.pax.pay.db.TransDataDb;
import com.pax.pay.trans.action.ActionEnterAmount;
import com.pax.pay.trans.action.ActionEnterRef1Ref2;
import com.pax.pay.trans.action.ActionGetQRReversal;
import com.pax.pay.trans.action.ActionGetQrCreditFromServer;
import com.pax.pay.trans.action.ActionInquiry;
import com.pax.pay.trans.action.ActionShowQRCodeWallet;
import com.pax.pay.trans.action.activity.EcrPaymentSelectActivity;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.AcqManager;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.task.PrintTask;
import com.pax.pay.utils.EReceiptUtils;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;
import com.pax.view.dialog.CustomAlertDialog;

import java.text.SimpleDateFormat;
import java.util.Date;

import th.co.bkkps.utils.Log;

public class QRCreditSaleTrans extends BaseTrans {
    private String mAmount = null;
    private ActionResult lastResult;
    private String refSaleID = null;
    private boolean isPosManualInquiry = false;

    public QRCreditSaleTrans(Context context, TransEndListener transListener) {
        super(context, ETransType.QR_INQUIRY_CREDIT, Constants.ACQ_QR_CREDIT, transListener);
        setBackToMain(true);
    }

    public QRCreditSaleTrans(Context context, String amount, TransEndListener transListener) {
        super(context, ETransType.QR_INQUIRY_CREDIT, Constants.ACQ_QR_CREDIT, transListener);
        setBackToMain(true);
        this.mAmount = amount;
    }

    public QRCreditSaleTrans(Context context, String amount, String refSaleID, boolean isPosManualInquiry, TransEndListener transListener) {
        super(context, ETransType.QR_INQUIRY_CREDIT, Constants.ACQ_QR_CREDIT, transListener);
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
                        getString(R.string.trans_qr_credit_sale), false);
            }
        });
        bind(State.ENTER_AMOUNT.toString(), amountAction, true);

        ActionEnterRef1Ref2 actionEnterRef1Ref2 = new ActionEnterRef1Ref2(action ->
                ((ActionEnterRef1Ref2) action).setParam(getCurrentContext(), getString(R.string.menu_sale))
        );
        bind(State.ENTER_REF1_REF2.toString(), actionEnterRef1Ref2, true);

        ActionGetQrCreditFromServer getQrFromServer = new ActionGetQrCreditFromServer(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionGetQrCreditFromServer) action).setParam(getCurrentContext(), transData);
            }
        });
        bind(State.GEN_QR.toString(), getQrFromServer);

        ActionGetQRReversal GetQRReversal = new ActionGetQRReversal(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionGetQRReversal) action).setParam(getCurrentContext(),
                        getString(R.string.trans_qr_credit_sale), transData);
            }
        });
        bind(State.REVERSAL_QR.toString(), GetQRReversal);

        ActionInquiry qrSaleInquiry = new ActionInquiry(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionInquiry) action).setParam(getCurrentContext(),
                        getString(R.string.trans_qr_credit_sale), transData);
            }
        });
        bind(State.INQUIRY_QR.toString(), qrSaleInquiry);

        ActionShowQRCodeWallet showQRCodeWallet = new ActionShowQRCodeWallet(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionShowQRCodeWallet) action).setParam(getCurrentContext(),
                        getString(R.string.trans_qr_credit_sale), R.string.trans_dynamic_qr, isPosManualInquiry, transData);
            }
        });
        bind(State.SHOW_CODE.toString(), showQRCodeWallet);

        ActionShowQRCodeWallet checkQRCodeWallet = new ActionShowQRCodeWallet(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionShowQRCodeWallet) action).setParam(getCurrentContext(),
                        getString(R.string.trans_qr_credit_sale), R.string.trans_dynamic_qr, isPosManualInquiry, transData);
            }
        });
        bind(State.CHECKQR.toString(), checkQRCodeWallet);

        //print preview action
        PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(QRCreditSaleTrans.this, State.PRINT.toString()));
        bind(State.PRINT.toString(), printTask);

//        if (DynamicOffline.getInstance().isDynamicOfflineActiveStatus()) {
//            transEnd(new ActionResult(TransResult.DYNAMIC_OFFLINE_TRANS_NOT_ALLOW,null));
//            return;
//        }



        Acquirer acq = AcqManager.getInstance().findActiveAcquirer(Constants.ACQ_QR_CREDIT);
        if (acq == null || !acq.isEnable()) {
            transEnd(new ActionResult(TransResult.ERR_UNSUPPORTED_FUNC, null));
            return;
        }

        // ERM Maximum TransExceed Check
        int ErmExeccededResult =  ErmLimitExceedCheck();
        if (ErmExeccededResult != TransResult.SUCC) {
            transEnd(new ActionResult(ErmExeccededResult,null));
            return;
        }

        int QrcMissingConfigResult =  checkQrcConfigMissing(context, acq);
        if (QrcMissingConfigResult != TransResult.SUCC) {
            transEnd(new ActionResult(QrcMissingConfigResult,null));
            return;
        }

        int QrcForcedSettleResult =  checkQrcForcedSettle(context, acq);
        if (QrcForcedSettleResult != TransResult.SUCC) {
            transEnd(new ActionResult(QrcForcedSettleResult,null));
            return;
        }


        if (mAmount == null) {
            gotoState(State.ENTER_AMOUNT.toString());
        } else {
            transData.setAmount(mAmount.replace(".", ""));

            // ECR mode set current host index
            if (FinancialApplication.getEcrProcess().mHyperComm instanceof LawsonHyperCommClass) {
                String hostIndex = LawsonHyperCommClass.getLawsonHostIndex(transData.getAcquirer());
                EcrData.instance.qr_hostIndex = Utils.checkStringContainNullOrEmpty(hostIndex).getBytes();
                EcrData.instance.qr_IssuerName = Constants.ISSUER_QRCREDIT.getBytes();
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
        INQUIRY_QR,
        CHECKQR,
        PRINT,
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {

        State state = State.valueOf(currentState);

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
                    transData.setWalletRetryStatus(TransData.WalletRetryStatus.NORMAL);
                    transData.setAmount(mAmount);
                    gotoState(State.REVERSAL_QR.toString());
                }
                break;
            case REVERSAL_QR:
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
                    if (result.getRet() == TransResult.ERR_USER_CANCEL) {
                        transData.setLastTrans(true);
                    }
                    gotoState(State.INQUIRY_QR.toString());
                } else {
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
            } else if (state.equals(KplusQrSaleTrans.State.GEN_QR.toString())
                    || state.equals((KplusQrSaleTrans.State.INQUIRY_QR.toString()))
                    || state.equals((WechatQrSaleTrans.State.SHOW_CODE.toString()))) {
                if (transData != null) {
                    EcrData.instance.saleReferenceIDR0 = Utils.checkStringContainNullOrEmpty(refSaleID, Utils.LengthValidatorMode.EQUALS, 8, true, " ").getBytes();
                    EcrData.instance.walletType = EcrPaymentSelectActivity.SEL_PAYMENT_QR_QRC;
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
            Component.splitField63Wallet(transData, transData.getField63RecByte());
            setIssuerFromResp();
            transData.setSignFree(true);
            transData.setWalletRetryStatus(TransData.WalletRetryStatus.NORMAL);
            FinancialApplication.getTransDataDbHelper().updateTransData(transData);
            String additionalData = (transData.isEcrProcess() && FinancialApplication.getEcrProcess().mHyperComm instanceof LawsonHyperCommClass) ? "QR-INQUIRY" : null;
            ECRProcReturn(null, new ActionResult(result.getRet(), additionalData));
            gotoState(State.PRINT.toString());
        } else {
            transData.setWalletRetryStatus(TransData.WalletRetryStatus.NORMAL);
            transData.setAmount(mAmount);
            if (lastResult.getRet() == TransResult.ERR_TIMEOUT
                    || lastResult.getRet() == TransResult.ERR_USER_CANCEL) {
                gotoState(State.REVERSAL_QR.toString());
                return;
            }

            if (result.getRet() == TransResult.ERR_WALLET_RESP_UK) {
                transData.setWalletRetryStatus(TransData.WalletRetryStatus.RETRY_CHECK);
                if (transData.isEcrProcess() && FinancialApplication.getEcrProcess().mHyperComm instanceof LawsonHyperCommClass) {
                    ECRProcReturn(null, new ActionResult(result.getRet(), null));
                }
                gotoState(State.CHECKQR.toString());
            } else {
                Issuer issuer = transData.getIssuer();
                if (issuer != null && transData.getIssuer().isAutoReversal()) {
                    gotoState(State.REVERSAL_QR.toString());
                } else {
                    gotoState(State.CHECKQR.toString());                                                // [EDCMGNT-1190]
                    //transEnd(result);
                }
            }
        }
    }

    private void setIssuerFromResp() {
        if (transData.getMerchantInfo() != null) {
            Issuer issuer = FinancialApplication.getAcqManager().findIssuer("QRC_" + transData.getMerchantInfo().trim().toUpperCase());
            transData.setIssuer(issuer);
        }
    }

    @Override
    protected void transEnd(ActionResult result) {
        mAmount = null;
        super.transEnd(result);
    }


    public int checkQrcForcedSettle (Context context, Acquirer acquirer) {
        TransData QRCredit_lastTransData = TransDataDb.getInstance().findLastQRCreditTransData();
        if (acquirer.getForceSettleTime() == null) { // No one set the time to force settlement for QR Credit
            String message = "Force-settlement-time config was missing";

            Log.d(TAG, message);
//            showErrorDialog(context, message);
            return TransResult.ERR_QR_CREDIT_MISSING_FORCE_SETTLE_TIME;
        }

        SimpleDateFormat tranDateTimeFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        String forceSettlementDate = "";
        String forceSettlementTime = acquirer.getForceSettleTime();

        if (QRCredit_lastTransData != null) { // There is a chance to meet the requirement for force settlement
            try {
                // Get Settlement Date from the last QR Credit transaction
                forceSettlementDate = dateFormat.format(tranDateTimeFormat.parse(QRCredit_lastTransData.getDateTime()));

                // Get Force Settlement Date object for the last QR Credit transaction
                Date forceSettlementDateTime = dateTimeFormat.parse(String.format("%1$s %2$s", forceSettlementDate, forceSettlementTime));

                // Get Current Date object
                Date currentDateTime = new Date();

                // Get linux time from Force Settlement Date object
                long forceSettlementDateTimeLinuxTime = forceSettlementDateTime.getTime();

                // Get linux time from current Date object
                long currentDateTimeLinuxTime = currentDateTime.getTime();

                // if the diff is more than zero, it means it passed the settlement time already.
                if (currentDateTimeLinuxTime - forceSettlementDateTimeLinuxTime > 0) {
//                    EReceiptUtils.getInstance().showMsgErmError(
//                            context,
//                            CustomAlertDialog.NORMAL_TYPE,
//                            context.getString(R.string.alert_qr_credit_force_settlement),
//                            Constants.FAILED_DIALOG_SHOW_TIME);
//                    return true;
                    return TransResult.ERR_SETTLE_NOT_COMPLETED;
                }
            } catch (Exception ex) {
                String message = ex.getMessage();

                Log.d(TAG, message);
                //showErrorDialog(context, message);
                return TransResult.ERR_QR_CREDIT_FORCE_SETTLE_INTERNAL_PROCESS;
            }
        }

        return TransResult.SUCC;
    }

    public int checkQrcConfigMissing(Context context, Acquirer acquirer) {
        if ((acquirer == null || !acquirer.isEnable()) ||
                (FinancialApplication.getAcqManager().findIssuer(Constants.ISSUER_QRC_VISA) == null
                        && FinancialApplication.getAcqManager().findIssuer(Constants.ISSUER_QRC_MASTERCARD) == null
                        && FinancialApplication.getAcqManager().findIssuer(Constants.ISSUER_QRC_UNIONPAY) == null)) {
            String message = "QR-Credit wasn't found.";
            Log.d(TAG, message);

            //showErrorDialog(context, getString(R.string.err_not_allowed));
            return TransResult.ERR_NOT_ALLOW;
        }

        return TransResult.SUCC;
    }

    public int checkErmReachLimitExceed(Context context) {
        if (TransDataDb.getInstance().findCountTransDataWithEReceiptUploadStatus(true) >= 30) {
//            EReceiptUtils.getInstance().showMsgErmError(
//                    context,
//                    CustomAlertDialog.NORMAL_TYPE,
//                    context.getString(R.string.ereceipt_upload_trans_maximum_exceed),
//                    Constants.FAILED_DIALOG_SHOW_TIME);
            return TransResult.ERCM_MAXIMUM_TRANS_EXCEED_ERROR;
        }

        return TransResult.SUCC;
    }

    private void showErrorDialog(Context context, String message) {
        EReceiptUtils.getInstance().showMsgErmError(
                context,
                CustomAlertDialog.NORMAL_TYPE,
                message,
                Constants.FAILED_DIALOG_SHOW_TIME);
    }
}
