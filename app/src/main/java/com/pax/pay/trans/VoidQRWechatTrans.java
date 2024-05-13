package com.pax.pay.trans;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.action.ActionDispTransDetail;
import com.pax.pay.trans.action.ActionInputTransData;
import com.pax.pay.trans.action.ActionQrSaleInquiry;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.task.PrintTask;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.TimeConverter;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.util.Collections;
import java.util.LinkedHashMap;

/**
 * Created by NANNAPHAT S on 12-Feb-19.
 */

public class VoidQRWechatTrans extends BaseTrans {

    private TransData origTransData;

    /**
     * whether need to read the original trans data or not
     */
    private boolean isNeedFindOrigTrans = true;
    /**
     * whether need to input trans no. or not
     */
    private boolean isNeedInputTransNo = true;

    public VoidQRWechatTrans(Context context, TransEndListener transListener) {
        super(context, ETransType.QR_VOID_WECHAT, transListener);
        isNeedFindOrigTrans = true;
        isNeedInputTransNo = true;
        setBackToMain(true);
    }

    public VoidQRWechatTrans(Context context, TransData origTransData, TransEndListener transListener) {
        super(context, ETransType.QR_VOID_WECHAT, transListener);
        this.origTransData = origTransData;
        isNeedFindOrigTrans = false;
        isNeedInputTransNo = false;
        setBackToMain(true);
    }

    @Override
    protected void bindStateOnAction() {

        ActionInputTransData enterTransNoAction = new ActionInputTransData(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                String promptMsg = getString(R.string.prompt_input_transno);
                if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_VOID_WITH_STAND)) {
                    promptMsg = getString(R.string.prompt_input_stanno);
                }
                ((ActionInputTransData) action).setParam(getCurrentContext(), getString(R.string.menu_wallet_void))
                        .setInputLine(promptMsg, ActionInputTransData.EInputType.NUM, 6, true);
            }
        });
        bind(VoidQRWechatTrans.State.ENTER_TRACENO.toString(), enterTransNoAction, true);

        // confirm information
        ActionDispTransDetail confirmInfoAction = new ActionDispTransDetail(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {

                String transType = origTransData.getTransType().getTransName();
                String amount = CurrencyConverter.convert(Utils.parseLongSafe(origTransData.getAmount(), 0), transData.getCurrency());

                // date and time
                String formattedDate = TimeConverter.convert(origTransData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                        Constants.TIME_PATTERN_DISPLAY);

                LinkedHashMap<String, String> map = new LinkedHashMap<>();
                map.put(getString(R.string.history_detail_type), transType);
                map.put(getString(R.string.history_detail_amount), amount);
               // map.put(getString(R.string.history_detail_wallet_card), PanUtils.maskCardNo(origTransData.getQrBuyerCode(), origTransData.getIssuer().getPanMaskPattern()));
               // map.put("", origTransData.getWalletName() != null ? origTransData.getWalletName() : "");
                map.put(getString(R.string.history_detail_auth_code), origTransData.getAuthCode());
                map.put(getString(R.string.history_detail_ref_no), origTransData.getRefNo());
                map.put(getString(R.string.history_detail_stan_no), Component.getPaddedNumber(origTransData.getStanNo(), 6));
                map.put(getString(R.string.history_detail_trace_no), Component.getPaddedNumber(origTransData.getTraceNo(), 6));
                map.put(getString(R.string.dateTime), formattedDate);
                ((ActionDispTransDetail) action).setParam(getCurrentContext(),
                        getString(R.string.menu_wallet_void), map);
            }
        });
        bind(VoidQRWechatTrans.State.TRANS_DETAIL.toString(), confirmInfoAction, true);

        ActionQrSaleInquiry qrSaleInquiry = new ActionQrSaleInquiry(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionQrSaleInquiry) action).setParam(getCurrentContext(),
                        getString(R.string.trans_wallet_void), transData);
            }
        });
        bind(VoidQRWechatTrans.State.INQUIRY.toString(), qrSaleInquiry, false);

        //print preview action
        PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(VoidQRWechatTrans.this, VoidQRWechatTrans.State.PRINT.toString()));
        bind(VoidQRWechatTrans.State.PRINT.toString(), printTask);

        // ERM Maximum TransExceed Check
        int ermExeccededResult = ErmLimitExceedCheck();
        if (ermExeccededResult != TransResult.SUCC) {
            transEnd(new ActionResult(ermExeccededResult,null));
            return;
        }

        int issuerSupportCheckResult = IssuerSupportTransactionCheck(Collections.singletonList(Constants.ISSUER_WECHAT));
        if (issuerSupportCheckResult != TransResult.SUCC) {
            transEnd(new ActionResult(issuerSupportCheckResult,null));
            return;
        }

        // whether void trans need to input password or not
        if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.OTHTC_VERIFY)) {
            gotoState(VoidQRWechatTrans.State.ENTER_TRACENO.toString());
        }
    }

    enum State {
        ENTER_TRACENO,
        TRANS_DETAIL,
        INQUIRY,
        PRINT,
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {
        VoidQRWechatTrans.State state = VoidQRWechatTrans.State.valueOf(currentState);
        switch (state) {
            case ENTER_TRACENO:
                onEnterTraceNo(result);
                break;
            case TRANS_DETAIL:
                gotoState(VoidQRWechatTrans.State.INQUIRY.toString());
                break;
            case INQUIRY:
//                transData = (TransData) result.getData();
                if(result.getRet() == TransResult.SUCC || result.getRet() == TransResult.ERR_ADVICE){
                    updateStateTransData();
                } else {
                    transEnd(result);
                }
                break;
            case PRINT:
                if (result.getRet() == TransResult.SUCC) {
                    transEnd(result);
                } else {
                    dispResult(transType.getTransName(), result, null);
                    gotoState(VoidQRWechatTrans.State.PRINT.toString());
                }
                break;
            default:
                transEnd(result);
                break;
        }
    }

    private void onEnterTraceNo(ActionResult result) {
        String content = (String) result.getData();
        long transNo;
        if (content == null) {
            TransData transData = FinancialApplication.getTransDataDbHelper().findLastWechatTransData();
            if (transData == null || transData.getTransType() == ETransType.QR_VOID_WECHAT) {
                transEnd(new ActionResult(TransResult.ERR_NO_TRANS, null));
                return;
            }
            transNo = transData.getTraceNo();
            if(FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_VOID_WITH_STAND)) {
                transNo = transData.getStanNo();
            }
        } else {
            transNo = Utils.parseLongSafe(content, -1);
        }
        validateOrigTransData(transNo);
    }

    // check original trans data
    private void validateOrigTransData(long origTransNo) {
        if(FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_VOID_WITH_STAND)) {
            origTransData = FinancialApplication.getTransDataDbHelper().findWechatTransDataByStanNo(origTransNo);
        } else{
            origTransData = FinancialApplication.getTransDataDbHelper().findWechatTransDataByTraceNo(origTransNo);
        }
        if (origTransData == null) {
            // trans not exist
            transEnd(new ActionResult(TransResult.ERR_NO_ORIG_TRANS, null));
            return;
        }

        if (Component.chkSettlementStatus(origTransData.getAcquirer().getName())) {
            // Last settlement not success, need to settle firstly
            transEnd(new ActionResult(TransResult.ERR_SETTLE_NOT_COMPLETED,  null));
            return;
        }

        ETransType trType = origTransData.getTransType();

        if (!trType.isVoidAllowed()) {
            transEnd(new ActionResult(TransResult.ERR_VOID_UNSUPPORTED, null));
            return;
        }

        TransData.ETransStatus trStatus = origTransData.getTransState();
        // void trans can not be revoked again
        if (trStatus.equals(TransData.ETransStatus.VOIDED)
                || trType == ETransType.QR_VOID_WECHAT) {
            transEnd(new ActionResult(TransResult.ERR_HAS_VOIDED, null));
            return;
        }

        Component.copyOrigTransDataWallet(transData, origTransData);
        transData.setRefNo(origTransData.getRefNo());
        gotoState(SaleVoidTrans.State.TRANS_DETAIL.toString());
        return;
    }

    private void updateStateTransData(){
        Component.initField63Wallet(transData);
        String authCode = transData.getAuthCode() != null ? transData.getAuthCode() : transData.getOrigAuthCode();
        transData.setAuthCode(authCode);
        FinancialApplication.getTransDataDbHelper().updateTransData(transData);

        origTransData.setAuthCode(authCode);
        origTransData.setTransState(TransData.ETransStatus.VOIDED);
        //origTransData.setAdviceStatus(transData.getAdviceStatus());
        origTransData.setWalletSlipInfo(transData.getWalletSlipInfo());
        origTransData.setOrigDateTime(origTransData.getDateTime());
        origTransData.setDateTime(transData.getDateTime());
        origTransData.setVoidStanNo(transData.getStanNo());
        FinancialApplication.getTransDataDbHelper().updateTransData(origTransData);

        origTransData.setEcrProcess(transData.isEcrProcess());

        PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(VoidQRWechatTrans.this, VoidQRWechatTrans.State.PRINT.toString()));
        bind(VoidQRWechatTrans.State.PRINT.toString(), printTask);
        gotoState(VoidQRWechatTrans.State.PRINT.toString());
    }
}
