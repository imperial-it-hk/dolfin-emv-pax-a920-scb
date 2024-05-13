package com.pax.pay.trans;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.abl.utils.EncUtils;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.action.ActionInputTransData;
import com.pax.pay.trans.action.ActionQrSaleInquiry;
import com.pax.pay.trans.action.ActionShowQRRef;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.AcqManager;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.task.PrintTask;

/**
 * Created by SORAYA S on 26-Apr-18.
 */

public class PromptPayOfflineLastTrans extends BaseTrans {

    public PromptPayOfflineLastTrans(Context context, TransEndListener transListener) {
        super(context, ETransType.PROMPT_ADV, transListener);
    }

    @Override
    protected void bindStateOnAction() {
        ActionShowQRRef getLastTransAction = new ActionShowQRRef(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionShowQRRef) action).setParam(getCurrentContext(),
                        getString(R.string.menu_qr_last_offline), null, Constants.ACQ_QR_PROMPT);
            }
        });
        bind(PromptPayOfflineLastTrans.State.GET_LAST_TRANS.toString(), getLastTransAction, true);

        ActionInputTransData enterApprCodeAction = new ActionInputTransData(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionInputTransData) action).setParam(getCurrentContext(), getString(R.string.menu_qr_last_offline))
                        .setInputTransIDLine(getString(R.string.prompt_input_appr_code), ActionInputTransData.EInputType.TRANSID, 6,0);
            }
        });
        bind(PromptPayOfflineLastTrans.State.ENTER_APPR_CODE.toString(), enterApprCodeAction, true);

        //print preview action
        PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(PromptPayOfflineLastTrans.this, PromptPayOfflineLastTrans.State.PRINT.toString()));
        bind(PromptPayOfflineLastTrans.State.PRINT.toString(), printTask);

        ActionQrSaleInquiry qrSaleInquiry = new ActionQrSaleInquiry(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionQrSaleInquiry) action).setParam(getCurrentContext(),
                        getString(R.string.menu_qr_sale_inquiry), transData);
            }
        });
        bind(PromptPayOfflineLastTrans.State.ADVICE_INQ.toString(), qrSaleInquiry, false);

        gotoState(PromptPayOfflineLastTrans.State.GET_LAST_TRANS.toString());
    }

    enum State {
        GET_LAST_TRANS,
        ENTER_APPR_CODE,
        PRINT,
        ADVICE_INQ,
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {
        int ret = result.getRet();
        PromptPayOfflineLastTrans.State state = PromptPayOfflineLastTrans.State.valueOf(currentState);
        if (ret != TransResult.SUCC) {
            if(state == PromptPayOfflineLastTrans.State.ADVICE_INQ) {
                transData.setTransType(ETransType.BPS_QR_SALE_INQUIRY);
                FinancialApplication.getTransDataDbHelper().updateTransData(transData);
            }
            transEnd(result);
            return;
        }
        switch (state) {
            case GET_LAST_TRANS:
                transData = (TransData) result.getData();
                setTransDataQr(transData);
                gotoState(PromptPayOfflineLastTrans.State.ENTER_APPR_CODE.toString());
                break;
            case ENTER_APPR_CODE:
                validateApprCode(result.getData().toString());
                break;
            case PRINT:
                Component.incTraceNo(transData);// increase trace no. for PromptPay offline trans
                checkBeforeAdviceInq();
                break;
            case ADVICE_INQ:
//                transData = (TransData) result.getData();
                if (result.getRet() == TransResult.SUCC) {
                    transData.setTransType(ETransType.BPS_QR_SALE_INQUIRY);
                    transData.setQrSaleStatus(transData.getResponseCode().getCode());
                    FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                    transEnd(result);
                }
                break;
            default:
                transEnd(result);
                break;
        }
    }

    private TransData setTransDataQr(TransData transData){
        Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_QR_PROMPT);

        transData.setHeader("");
        transData.setTpdu("600" + acquirer.getNii() + "0000");
        transData.setDupReason("06");
        transData.setQrSaleState(TransData.QrSaleState.QR_SEND_OFFLINE);//set flag for offline last trans.

        return transData;
    }

    private void validateApprCode(String inputApprCode){
        String merchantId = Component.getPaddedString(transData.getAcquirer().getMerchantId(), 15, ' ');
        String terminalId = Component.getPaddedString(transData.getAcquirer().getTerminalId(), 8, '0');
        String amount = Component.getPaddedString(transData.getAmount(), 12, '0');
        String strHash = merchantId + terminalId + transData.getQrRef2() + amount + Constants.PROMPT_SALT;

        String hexStringSHA512 = EncUtils.sha512(strHash);
        int hexLength = hexStringSHA512.length();
        long apprCodeGen = Long.parseLong(hexStringSHA512.substring(hexLength-6, hexLength), 16);
        apprCodeGen = apprCodeGen % 1000000;

        String approveCode = Component.getPaddedNumber(apprCodeGen, 6);
        if(!approveCode.equals(inputApprCode)){
            transEnd(new ActionResult(TransResult.ERR_PROMPT_INVALID_APPR_CODE, null));
        }else {
            if(transData.getAuthCode() == null){
                transData.setAuthCode(approveCode);
            }
            PrintTask printTask = new PrintTask(getCurrentContext(), transData, PrintTask.genTransEndListener(PromptPayOfflineLastTrans.this, PromptPayOfflineLastTrans.State.PRINT.toString()));
            bind(PromptPayOfflineLastTrans.State.PRINT.toString(), printTask);
            gotoState(PromptPayOfflineLastTrans.State.PRINT.toString());
        }
        return;
    }

    private void checkBeforeAdviceInq(){
        if(transData.getQrSaleStatus().equals(TransData.QrSaleStatus.SUCCESS.toString())){
            transEnd(new ActionResult(TransResult.SUCC, null));
        }else{
            transData.setField63(transData.getQrRef2());
            transData.setTransType(ETransType.PROMPT_ADV);
            gotoState(PromptPayOfflineLastTrans.State.ADVICE_INQ.toString());
        }
        return;
    }

}
