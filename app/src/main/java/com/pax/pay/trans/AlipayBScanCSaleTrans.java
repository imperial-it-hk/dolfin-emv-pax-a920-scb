package com.pax.pay.trans;

import android.content.Context;
import android.util.Log;

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
import com.pax.pay.trans.action.ActionScanQRPay;
import com.pax.pay.trans.action.ActionTransOnline;
import com.pax.pay.trans.model.AcqManager;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.task.PrintTask;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

//import com.pax.pay.trans.action.ActionQrSaleInquiry;

/**
 * Created by NANNAPHAT S on 28/12/2018.
 */
public class AlipayBScanCSaleTrans extends BaseTrans {
    private String mAmount = null;
    private String refSaleID = null;
    private String qrCodeData;

    public AlipayBScanCSaleTrans(Context context, TransEndListener transListener) {
        super(context, ETransType.QR_ALIPAY_SCAN, Constants.ACQ_ALIPAY_B_SCAN_C, transListener);
        setBackToMain(true);
    }

    public AlipayBScanCSaleTrans(Context context, String amount, TransEndListener transListener) {
        super(context, ETransType.QR_ALIPAY_SCAN, Constants.ACQ_ALIPAY_B_SCAN_C, transListener);
        setBackToMain(true);
        this.mAmount = amount;
    }

    public AlipayBScanCSaleTrans(Context context, String amount, String refSaleID, TransEndListener transListener) {
        super(context, ETransType.QR_ALIPAY_SCAN, Constants.ACQ_ALIPAY_B_SCAN_C, transListener);
        setBackToMain(true);
        this.refSaleID = refSaleID;
        this.mAmount = amount;
    }

    @Override
    protected void bindStateOnAction() {

        ActionEnterAmount amountAction = new ActionEnterAmount(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionEnterAmount) action).setParam(getCurrentContext(),
                        getString(R.string.trans_scan_alipay_sale), false);
            }
        });
        bind(State.ENTER_AMOUNT.toString(), amountAction, true);

        ActionEnterRef1Ref2 actionEnterRef1Ref2 = new ActionEnterRef1Ref2(action ->
                ((ActionEnterRef1Ref2) action).setParam(getCurrentContext(), getString(R.string.menu_sale))
        );
        bind(State.ENTER_REF1_REF2.toString(), actionEnterRef1Ref2, true);

        //scan alipay
        ActionScanQRPay actionScanQRPay = new ActionScanQRPay(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionScanQRPay) action).setParam(getCurrentContext(),
                        getString(R.string.trans_scan_alipay_sale), transData);
            }
        });
        bind(State.SCAN_QR.toString(), actionScanQRPay, true);

        // online action
        ActionTransOnline transOnlineAction = new ActionTransOnline(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionTransOnline) action).setParam(getCurrentContext(), transData);
            }
        });

        bind(State.ONLINE_PROCESS.toString(), transOnlineAction, true);

        //print preview action
        PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(AlipayBScanCSaleTrans.this, State.PRINT.toString()));
        bind(State.PRINT.toString(), printTask);

        //added by Allen 2022-03-25
        Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_ALIPAY_B_SCAN_C);
        FinancialApplication.getAcqManager().setCurAcq(acquirer);
        if (acquirer == null || !acquirer.isEnable()) {
            transEnd(new ActionResult(TransResult.ERR_UNSUPPORTED_FUNC, null));
            return;
        }
        //add end

        //add issue
        Issuer issuer = FinancialApplication.getAcqManager().findIssuer(Constants.ISSUER_ALIPAY);
        transData.setIssuer(issuer);

        //add Acquirer

        transData.setAcquirer(acquirer);
        transData.setNii(String.valueOf(acquirer.getNii()));
        transData.setTpdu("600" + acquirer.getNii() + "8000");

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
                gotoState(State.SCAN_QR.toString());
            }
        }
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
                    gotoState(State.SCAN_QR.toString());
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

                gotoState(State.SCAN_QR.toString());
                break;
            case SCAN_QR:
                if (result.getRet() == TransResult.SUCC) {
                    qrCodeData = (String) result.getData();
                    transData.setQrResult(qrCodeData);
                    Log.i(TAG, "allen onActionResult: qrCode: " + transData.getQrResult());
                    gotoState(State.ONLINE_PROCESS.toString());
                } else {
                    transEnd(result);
                }
                break;
            case ONLINE_PROCESS:
                FinancialApplication.getTransDataDbHelper().updateTransData(transData);
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


    @Override
    protected void transEnd(ActionResult result) {
        mAmount = null;
        super.transEnd(result);
    }

    enum State {
        ENTER_AMOUNT,
        ENTER_REF1_REF2,
        SCAN_QR,
        SHOW_CODE,
        ONLINE_PROCESS,
        PRINT,
    }
}
