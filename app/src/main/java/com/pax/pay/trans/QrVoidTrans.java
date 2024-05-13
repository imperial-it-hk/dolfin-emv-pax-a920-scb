package com.pax.pay.trans;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.abl.utils.EncUtils;
import com.pax.abl.utils.PanUtils;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.utils.Tools;
import com.pax.pay.ECR.EcrData;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.action.ActionDispTransDetail;
import com.pax.pay.trans.action.ActionInputPassword;
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

import java.util.Arrays;
import java.util.LinkedHashMap;

/**
 * Created by NANNAPHAT S on 29-Nov-18.
 */

public class QrVoidTrans extends BaseTrans {

    private TransData origTransData;

    /**
     * whether need to read the original trans data or not
     */
    private boolean isNeedFindOrigTrans = true;
    /**
     * whether need to input trans no. or not
     */
    private boolean isNeedInputTransNo = true;

    public QrVoidTrans(Context context, TransEndListener transListener) {
        super(context, ETransType.QR_VOID, transListener);
        isNeedFindOrigTrans = true;
        isNeedInputTransNo = true;
    }

    public QrVoidTrans(Context context, TransData origTransData, TransEndListener transListener) {
        super(context, ETransType.QR_VOID, transListener);
        this.origTransData = origTransData;
        isNeedFindOrigTrans = false;
        isNeedInputTransNo = false;
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
        bind(QrVoidTrans.State.INPUT_PWD.toString(), inputPasswordAction);

        ActionInputTransData enterTransNoAction = new ActionInputTransData(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionInputTransData) action).setParam(getCurrentContext(), "QR Void")
                        .setInputLine(getString(R.string.prompt_input_transno), ActionInputTransData.EInputType.NUM, 6, true);
            }
        });
        bind(QrVoidTrans.State.ENTER_TRACENO.toString(), enterTransNoAction, true);

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
                map.put(getString(R.string.history_detail_amount), "-" +amount);
                map.put(getString(R.string.history_detail_auth_code), origTransData.getAuthCode());
                map.put(getString(R.string.history_detail_ref_no), origTransData.getRefNo());
                map.put(getString(R.string.history_detail_trace_no), Component.getPaddedNumber(origTransData.getTraceNo(), 6));
                map.put(getString(R.string.dateTime), formattedDate);
                ((ActionDispTransDetail) action).setParam(getCurrentContext()," QR Void", map);
            }
        });
        bind(QrVoidTrans.State.TRANS_DETAIL.toString(), confirmInfoAction, true);

        ActionQrSaleInquiry qrSaleInquiry = new ActionQrSaleInquiry(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionQrSaleInquiry) action).setParam(getCurrentContext(),
                        "QR void", transData);
            }
        });
        bind(QrVoidTrans.State.INQUIRY.toString(), qrSaleInquiry, false);

        //print preview action
        PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(QrVoidTrans.this, QrVoidTrans.State.PRINT.toString()));
        bind(QrVoidTrans.State.PRINT.toString(), printTask);

        // whether void trans need to input password or not
        if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.OTHTC_VERIFY)) {
            gotoState(QrVoidTrans.State.INPUT_PWD.toString());
        }
    }

    enum State {
        INPUT_PWD,
        ENTER_TRACENO,
        TRANS_DETAIL,
        INQUIRY,
        PRINT,
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
        QrVoidTrans.State state = QrVoidTrans.State.valueOf(currentState);
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
            case ENTER_TRACENO:
                onEnterTraceNo(result);
                break;
            case TRANS_DETAIL:
                gotoState(QrVoidTrans.State.INQUIRY.toString());
                break;
            case INQUIRY:
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
                    gotoState(QrVoidTrans.State.PRINT.toString());
                }
                break;
            default:
                transEnd(result);
                break;
        }
    }

    private void onInputPwd(ActionResult result) {
        String data = EncUtils.sha1((String) result.getData());
        if (!data.equals(FinancialApplication.getSysParam().get(SysParam.StringParam.SEC_VOID_PWD))) {
            EcrData.instance.isOnHomeScreen = true;
            transEnd(new ActionResult(TransResult.ERR_PASSWORD, null));
            return;
        }

        if (isNeedInputTransNo) {// need to input trans no.
            gotoState(QrVoidTrans.State.ENTER_TRACENO.toString());
        } else {// not need to input trans no.
            if (isNeedFindOrigTrans) {
                // nothing
            } else { // not need to read trans data
                Component.copyOrigTransDataWallet(transData, origTransData);
                gotoState(QrVoidTrans.State.TRANS_DETAIL.toString());
            }
        }
    }

    private void onEnterTraceNo(ActionResult result) {
        String content = (String) result.getData();
        long transNo;
        if (content == null) {
            TransData transData = FinancialApplication.getTransDataDbHelper().findLastQRVisaTransData(false);
            if (transData == null || transData.getTransType() == ETransType.QR_VOID) {
                transEnd(new ActionResult(TransResult.ERR_NO_TRANS, null));
                return;
            }
            transNo = transData.getTraceNo();
        } else {
            transNo = Utils.parseLongSafe(content, -1);
        }
        validateOrigTransData(transNo);
    }

    // check original trans data
    private void validateOrigTransData(long origTransNo) {
        origTransData = FinancialApplication.getTransDataDbHelper().findTransDataByTraceNo(origTransNo, false);
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
        if (trStatus.equals(TransData.ETransStatus.VOIDED)) {
            transEnd(new ActionResult(TransResult.ERR_HAS_VOIDED, null));
            return;
        }

        Component.copyOrigTransDataWallet(transData, origTransData);
        gotoState(SaleVoidTrans.State.TRANS_DETAIL.toString());
        transData.setRefNo(origTransData.getRefNo());
        transData.setQrID(origTransData.getQrID());
        transData.setQrRef2(origTransData.getQrRef2());
        transData.setDateTime(origTransData.getDateTime());
        transData.setOrigDateTime(origTransData.getDateTime());
        return;
    }

    private void updateStateTransData(){
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

        PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(QrVoidTrans.this, QrVoidTrans.State.PRINT.toString()));
        bind(QrVoidTrans.State.PRINT.toString(), printTask);
        gotoState(QrVoidTrans.State.PRINT.toString());
    }
}
