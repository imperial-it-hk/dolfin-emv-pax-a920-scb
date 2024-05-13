package com.pax.pay.trans;

import android.content.Context;
import android.content.DialogInterface;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.abl.utils.PanUtils;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.utils.Tools;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.action.ActionPrintTransReceipt;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.AcqManager;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.model.TransMultiAppData;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.TimeConverter;
import com.pax.pay.utils.Utils;
import com.pax.view.dialog.DialogUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import th.co.bkkps.amexapi.AmexTransAPI;
import th.co.bkkps.amexapi.action.ActionAmexReprint;
import th.co.bkkps.amexapi.action.activity.AmexReportActivity;
import th.co.bkkps.bps_amexapi.ReprintTransMsg;
import th.co.bkkps.dofinAPI.tran.action.ActionDolfinPrintTran;
import th.co.bkkps.scbapi.trans.action.ActionScbIppReprint;

/**
 * Created by SORAYA S on 05-Feb-18.
 */

public class BPSPrintLastTrans extends BaseTrans {

    private TransData transData;
    private TransMultiAppData multiAppLastTrans;
    private long origTraceNo;
    private long lastTraceNo;
    private List<Acquirer> supportAcquirers;

    public BPSPrintLastTrans(Context context, TransEndListener transListener) {
        super(context, ETransType.BPS_REPRINT, transListener);
    }

    @Override
    protected void bindStateOnAction() {

        ActionPrintTransReceipt printReceiptAction = new ActionPrintTransReceipt(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionPrintTransReceipt) action).setParam(getCurrentContext(), transData, true);
            }
        });
        bind(BPSPrintLastTrans.State.PRINT.toString(), printReceiptAction, true);

        ActionDolfinPrintTran actionDolfinPrintTran = new ActionDolfinPrintTran(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionDolfinPrintTran) action).setParam(getCurrentContext(), transData);
            }
        });
        bind(BPSPrintLastTrans.State.PRINT_DOLFIN.toString(), actionDolfinPrintTran, true);

        ActionScbIppReprint actionScbIppReprint = new ActionScbIppReprint(
                action -> ((ActionScbIppReprint) action).setParam(getCurrentContext(), transData.getTraceNo())
        );
        bind(BPSPrintLastTrans.State.PRINT_SCB_IPP_REDEEM.toString(), actionScbIppReprint, true);

        ActionAmexReprint actionAmexReprint = new ActionAmexReprint(action ->
                ((ActionAmexReprint) action).setParam(getCurrentContext(), origTraceNo, lastTraceNo, AmexReportActivity.ReportType.PRN_LAST_TXN.getType())
        );
        bind(BPSPrintLastTrans.State.PRINT_AMEX_API.toString(), actionAmexReprint);

        this.getSupportAcquirers();// init default acquirers.

        long traceNo = -1;
        multiAppLastTrans = FinancialApplication.getTransMultiAppDataDbHelper().findLastTransData();
        transData = FinancialApplication.getTransDataDbHelper().findLastTransDataByAcqsAndMerchant(supportAcquirers);
        if (transData != null) {
            traceNo = transData.getTraceNo();
        } else {
            transEnd(new ActionResult(TransResult.ERR_NO_TRANS, null));
            return;
        }

        if (multiAppLastTrans != null && traceNo < multiAppLastTrans.getTraceNo()) {
            goPrintLastTransByMultiApp(multiAppLastTrans, multiAppLastTrans.getTraceNo());
            return;
        }


        switch (transData.getAcquirer().getName()) {
            case Constants.ACQ_DOLFIN:
                gotoState(BPSPrintLastTrans.State.PRINT_DOLFIN.toString());
                break;
            case Constants.ACQ_SCB_IPP:
            case Constants.ACQ_SCB_REDEEM:
                gotoState(BPSPrintLastTrans.State.PRINT_SCB_IPP_REDEEM.toString());
                break;
            case Constants.ACQ_AMEX:
                gotoState(BPSPrintLastTrans.State.PRINT_AMEX_API.toString());
                break;
            default:
                gotoState(BPSPrintLastTrans.State.PRINT.toString());
                break;
        }
    }

    enum State {
        PRINT,
        PRINT_DOLFIN,
        PRINT_SCB_IPP_REDEEM,
        PRINT_AMEX_API,
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {
        BPSPrintLastTrans.State state = BPSPrintLastTrans.State.valueOf(currentState);

        switch (state) {
            case PRINT:
                if (result.getRet() == TransResult.SUCC) {
                    // end trans
                    transEnd(result);
                } else {
                    dispResult(transType.getTransName(), result, null);
                    gotoState(BPSPrintLastTrans.State.PRINT.toString());
                }
                break;
            case PRINT_DOLFIN:
                if((int)result.getData() != 0) {
                    showRespMsg(result);
                    return;
                }
                transEnd(result);
                break;
            case PRINT_AMEX_API:
                if (result.getRet() == TransResult.SUCC) {
                    ReprintTransMsg.Response resp = (ReprintTransMsg.Response) result.getData();
                    if (resp.getRspCode() == TransResult.ERR_ABORTED && origTraceNo == -1) { // enter without trace no.
                        if (lastTraceNo > 0) {
                            validateOrigTransData(lastTraceNo);
                        } else {
                            transEnd(new ActionResult(TransResult.ERR_NO_ORIG_TRANS, null));
                        }
                    } else {
                        transEnd(new ActionResult(TransResult.ERR_ABORTED, null));
                    }
                } else {
                    transEnd(result);
                }
                break;
            default:
                transEnd(result);
                break;
        }
    }

    private void onEnterTraceNo(ActionResult result) {
        String content = (String) result.getData();
        long traceNo = -1;
        if (content == null) {
            TransData transData = FinancialApplication.getTransDataDbHelper().findLastTransDataByAcqsAndMerchant(supportAcquirers);
            if (transData != null) {
                traceNo = transData.getTraceNo();
            }

            if (AmexTransAPI.getInstance().getProcess().isAppInstalled(context)) {
                this.origTraceNo = -1;
                this.lastTraceNo = traceNo;
                gotoState(BPSPrintLastTrans.State.PRINT_AMEX_API.toString());
                return;
            }
        } else {
            traceNo = Utils.parseLongSafe(content, -1);
            this.origTraceNo = traceNo;
        }
        validateOrigTransData(traceNo);
    }

    // check original trans data
    private void validateOrigTransData(long origTransNo) {
        transData = FinancialApplication.getTransDataDbHelper().findTransDataByTraceNo(origTransNo, false);
        if (transData == null) {
            // trans not exist
            if (AmexTransAPI.getInstance().getProcess().isAppInstalled(context)) {
                this.origTraceNo = origTransNo;
                gotoState(BPSPrintLastTrans.State.PRINT_AMEX_API.toString());
            }
            else {
                transEnd(new ActionResult(TransResult.ERR_NO_ORIG_TRANS, null));//no alert dialog
            }
            return;
        }
        if (transData.getTransState() == TransData.ETransStatus.VOIDED) {
            ETransType type;
            switch (transData.getAcquirer().getName()) {
                case Constants.ACQ_ALIPAY:
                case Constants.ACQ_ALIPAY_B_SCAN_C:
                    type = ETransType.QR_VOID_ALIPAY;
                    break;
                case Constants.ACQ_WECHAT:
                case Constants.ACQ_WECHAT_B_SCAN_C:
                    type = ETransType.QR_VOID_WECHAT;
                    break;
                case Constants.ACQ_KPLUS:
                    type = ETransType.QR_VOID_KPLUS;
                    break;
                case Constants.ACQ_QR_CREDIT:
                    type = ETransType.QR_VOID_CREDIT;
                    break;
                case Constants.ACQ_SMRTPAY:
                case Constants.ACQ_SMRTPAY_BDMS:
                    type = ETransType.KBANK_SMART_PAY_VOID;
                    break;
                case Constants.ACQ_REDEEM:
                case Constants.ACQ_REDEEM_BDMS:
                    type = ETransType.KBANK_REDEEM_VOID;
                    break;
                case Constants.ACQ_AMEX_EPP:
                    type = ETransType.AMEX_INSTALMENT;
                    break;
                case Constants.ACQ_DOLFIN_INSTALMENT:
                    type = ETransType.DOLFIN_INSTALMENT_VOID;
                    break;
                default:
                    type = ETransType.VOID;
                    break;
            }
            TransData trans = FinancialApplication.getTransDataDbHelper().findTransData(transData.getTraceNo(), type);
            if (trans != null) {
                transData.setEcrProcess(trans.isEcrProcess());
                transData.setRefNo(trans.getRefNo());
                transData.seteReceiptUploadStatus(trans.geteReceiptUploadStatus());
                transData.setTxnNo(trans.getTxnNo());
            }
        }
        gotoState(BPSPrintLastTrans.State.PRINT.toString());
    }

    private void goPrintLastTransByMultiApp(TransMultiAppData transMultiAppData, long origTransNo) {
        Acquirer acqAmex = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_AMEX);
        if (acqAmex.isEnable() && AmexTransAPI.getInstance().getProcess().isAppInstalled(context)
                && Constants.ACQ_AMEX.equals(transMultiAppData.getAcquirer().getName())) {
            this.origTraceNo = origTransNo;
            gotoState(BPSReprintTrans.State.PRINT_AMEX_API.toString());
        } else if (origTransNo != -1) {
            transEnd(new ActionResult(TransResult.ERR_NO_TRANS, null));
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

    private LinkedHashMap<String, String> getShowDetail(LinkedHashMap<String, String> map) {
        String transType = transData.getTransType().getTransName();
        TransData.ETransStatus transState = transData.getTransState();

        long amount = Utils.parseLongSafe(transData.getAmount(), 0);
        amount = transState == TransData.ETransStatus.VOIDED ? -amount : amount;

        String acqName = transData.getAcquirer() != null ? transData.getAcquirer().getName() : "";
        switch (acqName) {
            case Constants.ACQ_QR_PROMPT:
                if(transState == TransData.ETransStatus.VOIDED){
                    transType = transType + (transState == TransData.ETransStatus.VOIDED ? " (" +getString(R.string.trans_void)+ ")" : "");
                } else {
                    transType = transType + (transData.getQrSaleState() == TransData.QrSaleState.QR_SEND_OFFLINE ? " (" + getString(R.string.state_qr_offline)+ ")" : " (" + getString(R.string.state_qr_online)+ ")");
                }
                map.put(getString(R.string.history_detail_type), transType);
                map.put(getString(R.string.history_detail_amount), CurrencyConverter.convert(amount, transData.getCurrency()));
                map.put(getString(R.string.history_detail_qr_ref), transData.getQrRef2());
                map.put(getString(R.string.history_detail_trans_id), transData.getRefNo());
                map.put(getString(R.string.history_detail_auth_code), transData.getAuthCode());
                break;
            case Constants.ACQ_WALLET:
                transType = transType + (transState == TransData.ETransStatus.VOIDED ? " (" +getString(R.string.trans_void)+ ")" : "");
                String qrBuyerCode = transData.getQrBuyerCode() != null ? transData.getQrBuyerCode() : "";
                map.put(getString(R.string.history_detail_type), transType);
                map.put(getString(R.string.history_detail_amount), CurrencyConverter.convert(amount, transData.getCurrency()));
                map.put(getString(R.string.history_detail_wallet_card), PanUtils.maskCardNo(qrBuyerCode, transData.getIssuer().getPanMaskPattern()));
                map.put("", transData.getWalletName() != null ? transData.getWalletName() : "");
                map.put(getString(R.string.history_detail_auth_code), transData.getAuthCode());
                map.put(getString(R.string.history_detail_ref_no), transData.getRefNo());
                break;
            case Constants.ACQ_REDEEM:
            case Constants.ACQ_REDEEM_BDMS:
                if (transData.getTransType() == ETransType.KBANK_REDEEM_DISCOUNT) {
                    transType = "89999".equals(transData.getRedeemedDiscountType()) ? getString(R.string.trans_kbank_redeem_discount_var) : getString(R.string.trans_kbank_redeem_discount_fix);
                } else {
                    transType = transData.getTransType().getTransName();
                }
                transType = transType + (transState == TransData.ETransStatus.VOIDED ? " (" +getString(R.string.trans_void)+ ")" : "");
                amount = transState == TransData.ETransStatus.VOIDED ? -(Utils.parseLongSafe(transData.getRedeemedTotal(), 0)) : Utils.parseLongSafe(transData.getRedeemedTotal(), 0);
                int point = transState == TransData.ETransStatus.VOIDED ? -(transData.getRedeemedPoint()) : transData.getRedeemedPoint();
                map.put(getString(R.string.history_detail_type), transType);
                map.put(getString(R.string.history_detail_redeem_total), CurrencyConverter.convert(amount, transData.getCurrency()));
                map.put(getString(R.string.history_detail_redeem_point), String.format(Locale.getDefault(),"%,d", point));
                map.put(getString(R.string.history_detail_card_no), PanUtils.maskCardNo(transData.getPan(), transData.getIssuer().getPanMaskPattern()));
                map.put(getString(R.string.history_detail_auth_code), transData.getAuthCode());
                map.put(getString(R.string.history_detail_ref_no), transData.getRefNo());
                break;
            case Constants.ACQ_SMRTPAY:
            case Constants.ACQ_SMRTPAY_BDMS:
            case Constants.ACQ_DOLFIN_INSTALMENT:
                transType = Component.getTransByIPlanMode(transData);
                transType = transType + (transState == TransData.ETransStatus.VOIDED ? " (" +getString(R.string.trans_void)+ ")" : "");
                map.put(getString(R.string.history_detail_type), transType);
                map.put(getString(R.string.history_detail_amount), CurrencyConverter.convert(amount, transData.getCurrency()));
                map.put(getString(R.string.history_detail_card_no), PanUtils.maskCardNo(transData.getPan(), transData.getIssuer().getPanMaskPattern()));
                map.put(getString(R.string.history_detail_auth_code), transData.getAuthCode());
                map.put(getString(R.string.history_detail_ref_no), transData.getRefNo());
                break;
            default:
                transType = transData.getReferralStatus() != TransData.ReferralStatus.NORMAL ? transType + getString(R.string.receipt_amex_call_issuer) : transType;
                transType = transType + (transState == TransData.ETransStatus.VOIDED ? " (" +getString(R.string.trans_void)+ ")" : "");
                map.put(getString(R.string.history_detail_type), transType);
                map.put(getString(R.string.history_detail_amount), CurrencyConverter.convert(amount, transData.getCurrency()));
                if (transData.isDccRequired()) {
                    String currencyNumeric = Tools.bytes2String(transData.getDccCurrencyCode());
                    String dccAmount = CurrencyConverter.convert(Utils.parseLongSafe(transData.getDccAmount(), 0), currencyNumeric);
                    double exRate = transData.getDccConversionRate() != null ? Double.parseDouble(transData.getDccConversionRate()) / 10000 : 0;
                    map.put(getString(R.string.history_detail_dcc_ex_rate), String.format(Locale.getDefault(), "%.4f", exRate));
                    map.put(Utils.getString(R.string.history_detail_dcc_amount, CurrencyConverter.getCurrencySymbol(currencyNumeric, false)), dccAmount);
                }
                map.put(getString(R.string.history_detail_card_no), PanUtils.maskCardNo(transData.getPan(), transData.getIssuer().getPanMaskPattern()));
                map.put(getString(R.string.history_detail_auth_code), transData.getAuthCode());
                map.put(getString(R.string.history_detail_ref_no), transData.getRefNo());
                break;

        }
        long stanNo = transData.getTransState() == TransData.ETransStatus.VOIDED ? transData.getVoidStanNo() : transData.getStanNo();
        map.put(getString(R.string.history_detail_stan_no), Component.getPaddedNumber(stanNo, 6));
        map.put(getString(R.string.history_detail_trace_no), Component.getPaddedNumber(transData.getTraceNo(), 6));
        map.put(getString(R.string.dateTime), TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS, Constants.TIME_PATTERN_DISPLAY));
        return map;
    }

    private void showRespMsg(ActionResult result){
        String respMsg = (String)result.getData1();
        DialogInterface.OnDismissListener onDismissListener = new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                transEnd(new ActionResult(TransResult.ERR_ABORTED, null));
            }
        };
        DialogUtils.showErrMessage(getCurrentContext(), getString(R.string.trans_sale), respMsg, onDismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
    }
}
