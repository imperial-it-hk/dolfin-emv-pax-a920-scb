package com.pax.pay.trans;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.abl.utils.EncUtils;
import com.pax.abl.utils.PanUtils;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.action.ActionDispTransDetail;
import com.pax.pay.trans.action.ActionInputPassword;
import com.pax.pay.trans.action.ActionInputTransData;
import com.pax.pay.trans.action.ActionOfflineSend;
import com.pax.pay.trans.action.ActionSignature;
import com.pax.pay.trans.action.ActionTransOnline;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.task.PrintTask;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.TimeConverter;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by SORAYA S on 30-Jan-18.
 */

public class BPSVoidTrans extends BaseTrans {

    private TransData origTransData;
    private String origTransNo;

    /**
     * whether need to read the original trans data or not
     */
    private boolean isNeedFindOrigTrans = true;
    /**
     * whether need to input trans no. or not
     */
    private boolean isNeedInputTransNo = true;

    public BPSVoidTrans(Context context, TransEndListener transListener) {
        super(context, ETransType.BPS_VOID, transListener);
        isNeedFindOrigTrans = true;
        isNeedInputTransNo = true;
    }

    public BPSVoidTrans(Context context, TransData origTransData, TransEndListener transListener) {
        super(context, ETransType.BPS_VOID, transListener);
        this.origTransData = origTransData;
        isNeedFindOrigTrans = false;
        isNeedInputTransNo = false;
    }

    public BPSVoidTrans(Context context, String origTransNo, TransEndListener transListener) {
        super(context, ETransType.BPS_VOID, transListener);
        this.origTransNo = origTransNo;
        isNeedFindOrigTrans = true;
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
        bind(SaleVoidTrans.State.INPUT_PWD.toString(), inputPasswordAction, true);

        ActionInputTransData enterTransNoAction = new ActionInputTransData(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionInputTransData) action).setParam(getCurrentContext(), getString(R.string.trans_void))
                        .setInputLine(getString(R.string.prompt_input_transno), ActionInputTransData.EInputType.NUM, 6, true);
            }
        });
        bind(SaleVoidTrans.State.ENTER_TRANSNO.toString(), enterTransNoAction, true);

        // confirm information
        ActionDispTransDetail confirmInfoAction = new ActionDispTransDetail(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {

                String transType = origTransData.getTransType().getTransName();
                String amount = CurrencyConverter.convert(Utils.parseLongSafe(origTransData.getAmount(), 0), transData.getCurrency());

                transData.setEnterMode(origTransData.getEnterMode());
                transData.setTrack2(origTransData.getTrack2());
                transData.setTrack3(origTransData.getTrack3());

                // date and time
                //AET-95
                String formattedDate = TimeConverter.convert(origTransData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                        Constants.TIME_PATTERN_DISPLAY);

                LinkedHashMap<String, String> map = new LinkedHashMap<>();
                map.put(getString(R.string.history_detail_type), transType);
                map.put(getString(R.string.history_detail_amount), amount);
                map.put(getString(R.string.history_detail_card_no), PanUtils.maskCardNo(origTransData.getPan(), origTransData.getIssuer().getPanMaskPattern()));
                map.put(getString(R.string.history_detail_auth_code), origTransData.getAuthCode());
                map.put(getString(R.string.history_detail_ref_no), origTransData.getRefNo());
                map.put(getString(R.string.history_detail_trace_no), Component.getPaddedNumber(origTransData.getTraceNo(), 6));
                map.put(getString(R.string.dateTime), formattedDate);
                ((ActionDispTransDetail) action).setParam(getCurrentContext(),
                        getString(R.string.trans_void), map);
            }
        });
        bind(SaleVoidTrans.State.TRANS_DETAIL.toString(), confirmInfoAction, true);

        // online action
        ActionTransOnline transOnlineAction = new ActionTransOnline(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionTransOnline) action).setParam(getCurrentContext(), transData);
            }
        });

        bind(SaleVoidTrans.State.MAG_ONLINE.toString(), transOnlineAction, true);
        // signature action
        ActionSignature signatureAction = new ActionSignature(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionSignature) action).setParam(getCurrentContext(), transData.getAmount());
            }
        });
        bind(SaleVoidTrans.State.SIGNATURE.toString(), signatureAction);
        //offline send
        ActionOfflineSend offlineSendAction = new ActionOfflineSend(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionOfflineSend) action).setParam(getCurrentContext());
            }
        });
        //even it failed to upload offline, it will continue current transaction, so the 3rd argv is false
        bind(SaleVoidTrans.State.OFFLINE_SEND.toString(), offlineSendAction);

        //print preview action
        PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(BPSVoidTrans.this, SaleTrans.State.PRINT.toString()));
        bind(SaleVoidTrans.State.PRINT.toString(), printTask);

        // whether void trans need to input password or not
        if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.OTHTC_VERIFY)) {
            gotoState(SaleVoidTrans.State.INPUT_PWD.toString());
        } else if (isNeedInputTransNo) {// need to input trans no.
            gotoState(SaleVoidTrans.State.ENTER_TRANSNO.toString());
        } else {// not need to input trans no.
            if (isNeedFindOrigTrans) {
                validateOrigTransData(Utils.parseLongSafe(origTransNo, -1));
            } else { // not need to read trans data
                copyOrigTransData();
                checkCardAndPin();
            }
        }
    }

    enum State {
        INPUT_PWD,
        ENTER_TRANSNO,
        TRANS_DETAIL,
        MAG_ONLINE,
        SIGNATURE,
        OFFLINE_SEND,
        PRINT,
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {
        SaleVoidTrans.State state = SaleVoidTrans.State.valueOf(currentState);

        switch (state) {
            case INPUT_PWD:
                onInputPwd(result);
                break;
            case ENTER_TRANSNO:
                onEnterTraceNo(result);
                break;
            case TRANS_DETAIL:
                gotoState(SaleVoidTrans.State.MAG_ONLINE.toString()); // online
                break;
            case MAG_ONLINE: //  subsequent processing of online
                // update original trans data
                origTransData.setTransState(TransData.ETransStatus.VOIDED);
                FinancialApplication.getTransDataDbHelper().updateTransData(origTransData);
                gotoState(SaleVoidTrans.State.SIGNATURE.toString());

                break;
            case SIGNATURE:
                onSignature(result);
                break;
            case OFFLINE_SEND:
                gotoState(SaleVoidTrans.State.PRINT.toString());
                break;
            case PRINT:
                if (result.getRet() == TransResult.SUCC) {
                    // end trans
                    transEnd(result);
                } else {
                    dispResult(transType.getTransName(), result, null);
                    gotoState(SaleVoidTrans.State.PRINT.toString());
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
            transEnd(new ActionResult(TransResult.ERR_PASSWORD, null));
            return;
        }

        if (isNeedInputTransNo) {// need to input trans no.
            gotoState(SaleVoidTrans.State.ENTER_TRANSNO.toString());
        } else {// not need to input trans no.
            if (isNeedFindOrigTrans) {
                validateOrigTransData(Utils.parseLongSafe(origTransNo, -1));
            } else { // not need to read trans data
                copyOrigTransData();
                checkCardAndPin();
            }
        }
    }

    private void onEnterTraceNo(ActionResult result) {
        String content = (String) result.getData();
        long transNo;
        if (content == null) {
            TransData transData = FinancialApplication.getTransDataDbHelper().findLastTransData();
            if (transData == null) {
                transEnd(new ActionResult(TransResult.ERR_NO_TRANS, null));
                return;
            }
            transNo = transData.getTraceNo();
        } else {
            transNo = Utils.parseLongSafe(content, -1);
        }
        validateOrigTransData(transNo);
    }

    private void onSignature(ActionResult result) {
        // save signature data
        byte[] signData = (byte[]) result.getData();
        byte[] signPath = (byte[]) result.getData1();

        if (signData != null && signData.length > 0 &&
                signPath != null && signPath.length > 0) {
            transData.setSignData(signData);
            transData.setSignPath(signPath);
            // update trans data，save signature
            FinancialApplication.getTransDataDbHelper().updateTransData(transData);
        }

        //get offline trans data list
        List<TransData.OfflineStatus> filter = new ArrayList<>();
        filter.add(TransData.OfflineStatus.OFFLINE_NOT_SENT);
        List<TransData> offlineTransList = FinancialApplication.getTransDataDbHelper().findOfflineTransData(filter);
        if (!offlineTransList.isEmpty() && offlineTransList.get(0).getId() != transData.getId()) { //AET-92
            //offline send
            gotoState(SaleVoidTrans.State.OFFLINE_SEND.toString());
            return;
        }

        // if terminal does not support signature ,card holder does not sign or time out，print preview directly.
        gotoState(SaleVoidTrans.State.PRINT.toString());
    }

    // check original trans data
    private void validateOrigTransData(long origTransNo) {
        origTransData = FinancialApplication.getTransDataDbHelper().findTransDataByTraceNo(origTransNo, true);
        if (origTransData == null) {
            // trans not exist
            transEnd(new ActionResult(TransResult.ERR_NO_ORIG_TRANS, null));
            return;
        }
        ETransType trType = origTransData.getTransType();

        // only sale and refund trans can be revoked
        // AET-101 AET-139
        boolean isOfflineSent = trType == ETransType.OFFLINE_TRANS_SEND &&
                origTransData.getOfflineSendState() != null &&
                origTransData.getOfflineSendState() == TransData.OfflineStatus.OFFLINE_SENT;
        boolean isAdjustedNotSent = origTransData.getTransState() == TransData.ETransStatus.ADJUSTED &&
                origTransData.getOfflineSendState() != null &&
                origTransData.getOfflineSendState() == TransData.OfflineStatus.OFFLINE_NOT_SENT;
        if ((!trType.isVoidAllowed() && !isOfflineSent) || isAdjustedNotSent) {
            transEnd(new ActionResult(TransResult.ERR_VOID_UNSUPPORTED, null));
            return;
        }

        TransData.ETransStatus trStatus = origTransData.getTransState();
        // void trans can not be revoked again
        if (trStatus.equals(TransData.ETransStatus.VOIDED)) {
            transEnd(new ActionResult(TransResult.ERR_HAS_VOIDED, null));
            return;
        }

        copyOrigTransData();
        gotoState(SaleVoidTrans.State.TRANS_DETAIL.toString());
    }

    // set original trans data
    private void copyOrigTransData() {
        transData.setAmount(origTransData.getAmount());
        transData.setOrigBatchNo(origTransData.getBatchNo());
        transData.setOrigAuthCode(origTransData.getAuthCode());
        transData.setOrigRefNo(origTransData.getRefNo());
        transData.setOrigTransNo(origTransData.getStanNo());
        transData.setPan(origTransData.getPan());
        transData.setExpDate(origTransData.getExpDate());
        transData.setAcquirer(origTransData.getAcquirer());
        transData.setIssuer(origTransData.getIssuer());
        transData.setTrack1(origTransData.getTrack1());
        transData.setTraceNo(origTransData.getTraceNo());
    }

    // check whether void trans need to swipe card or not
    private void checkCardAndPin() {
        // not need to swipe card
        transData.setEnterMode(TransData.EnterMode.MANUAL);
        checkPin();
    }

    // check whether void trans need to enter pin or not
    private void checkPin() {
        // not need to enter pin
        transData.setPin("");
        transData.setHasPin(false);
        gotoState(SaleVoidTrans.State.MAG_ONLINE.toString());
    }
}
