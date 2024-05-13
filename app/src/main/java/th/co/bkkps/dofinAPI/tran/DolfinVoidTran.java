package th.co.bkkps.dofinAPI.tran;

import android.content.Context;
import android.content.DialogInterface;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.abl.utils.PanUtils;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.BaseTrans;
import com.pax.pay.trans.action.ActionDispTransDetail;
import com.pax.pay.trans.action.ActionInputTransData;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.AcqManager;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.TimeConverter;
import com.pax.pay.utils.Utils;
import com.pax.view.dialog.DialogUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import th.co.bkkps.dofinAPI.tran.action.ActionDolfinGetConfig;
import th.co.bkkps.dofinAPI.tran.action.ActionDolfinSetConfig;
import th.co.bkkps.dofinAPI.tran.action.ActionDolfinVoid;

public class DolfinVoidTran extends BaseTrans {
    private TransData origTransData;
    private String origTransNo;
    private List<Acquirer> supportAcquirers;
    private String amount = null;
    boolean isVoidErr = false;
    private String respMsg = null;
    /**
     * whether need to read the original trans data or not
     */
    private boolean isNeedFindOrigTrans = true;
    /**
     * whether need to input trans no. or not
     */
    private boolean isNeedInputTransNo = true;

    public DolfinVoidTran(Context context, TransEndListener transListener) {
        super(context, ETransType.VOID, transListener);
        setBackToMain(true);
    }

    @Override
    protected void bindStateOnAction() {

        ActionInputTransData enterTransNoAction = new ActionInputTransData(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionInputTransData) action).setParam(getCurrentContext(), getString(R.string.menu_void))
                        .setInputLine(getString(R.string.prompt_input_transno), ActionInputTransData.EInputType.NUM, 6, true);
            }
        });
        bind(State.ENTER_TRANSNO.toString(), enterTransNoAction, true);

        // confirm information
        ActionDispTransDetail confirmInfoAction = new ActionDispTransDetail(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                LinkedHashMap<String, String> map = new LinkedHashMap<>();
                String title = "Dolfin Void";
                mapCreditDetails(map);
                ((ActionDispTransDetail) action).setParam(getCurrentContext(), title, map);
            }
        });
        bind(State.TRANS_DETAIL.toString(), confirmInfoAction, true);

        ActionDolfinVoid actionDolfinVoid = new ActionDolfinVoid(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionDolfinVoid) action).setParam(getCurrentContext(),transData);
            }
        });
        bind(State.VOID.toString(), actionDolfinVoid, true);

        ActionDolfinSetConfig configAction = new ActionDolfinSetConfig(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionDolfinSetConfig) action).setParam(getCurrentContext());
            }
        });
        bind(State.SENT_CONFIG.toString(), configAction, true);

        ActionDolfinGetConfig getConfig = new ActionDolfinGetConfig(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionDolfinGetConfig) action).setParam(getCurrentContext(),transData,isVoidErr);
            }
        });
        bind(State.GET_CONFIG.toString(), getConfig, true);

        this.getSupportAcquirers();// init default acquirers for void trans.
        if (isNeedInputTransNo) {// need to input trans no.
            gotoState(State.ENTER_TRANSNO.toString());
        } else {// not need to input trans no.
            if (isNeedFindOrigTrans) {
                validateOrigTransData(Utils.parseLongSafe(origTransNo, -1));
            } else { // not need to read trans data
                copyOrigTransData();
            }
        }
    }

    enum State{
        ENTER_TRANSNO,
        SENT_CONFIG,
        TRANS_DETAIL,
        VOID,
        GET_CONFIG
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {
        int rc = result.getRet();
        DolfinVoidTran.State state = State.valueOf(currentState);

        switch (state) {
            case ENTER_TRANSNO:
                onEnterTraceNo(result);
                break;
            case TRANS_DETAIL:
                gotoState(State.SENT_CONFIG.toString());
                break;
            case SENT_CONFIG:
                if((int)result.getData() != 0){
                    respMsg = (String)result.getData1() != null ? (String)result.getData1() : null;
                    showRespMsg(result,respMsg);
                    return;
                }
                gotoState(State.VOID.toString());
                break;
            case VOID:
                int ret = (int)result.getData();
                if(ret == 0 || ret == 11){
                    // update original trans data
                    origTransData.setVoidStanNo(transData.getStanNo());
                    origTransData.setOrigDateTime(origTransData.getDateTime());
                    origTransData.setDateTime(transData.getDateTime());
                    origTransData.setTransState(TransData.ETransStatus.VOIDED);
                    origTransData.setOfflineSendState(transData.getOfflineSendState());
                    isVoidErr = false;
                    FinancialApplication.getTransDataDbHelper().updateTransData(origTransData);
                    //transEnd(result);
                }else{
                    /*showRespMsg(result,(String)result.getData1());
                    return;*/
                    respMsg = (String)result.getData1() != null ? (String)result.getData1() : null;
                    isVoidErr = true;
                }
                gotoState(State.GET_CONFIG.toString());
                break;
            case GET_CONFIG:
                if(isVoidErr){
                    showRespMsg(result,respMsg);
                    return;
                }
                transEnd(result);
                break;
            default:
                transEnd(result);
        }
    }

    private void getSupportAcquirers() {
        AcqManager acqManager = FinancialApplication.getAcqManager();
        List<Acquirer> acqs = new ArrayList<>();
        acqs.add(acqManager.findAcquirer(Constants.ACQ_DOLFIN));
        supportAcquirers = acqs;
        return;
    }

    private void onEnterTraceNo(ActionResult result) {
        String content = (String) result.getData();
        long transNo;
        if (content == null) {
            TransData transData = FinancialApplication.getTransDataDbHelper().findLastTransDataByAcqsAndMerchant(supportAcquirers);
            if (transData == null) {
                transEnd(new ActionResult(TransResult.ERR_NO_TRANS, null));
                return;
            }
            transNo = transData.getTraceNo();
        } else {
            transNo = Utils.parseLongSafe(content, -1);
        }
        origTransNo = String.valueOf(transNo);
        validateOrigTransData(transNo);
    }

    // check original trans data
    private void validateOrigTransData(long origTransNo) {
        origTransData = FinancialApplication.getTransDataDbHelper().findTransDataByTraceNoAndAcqs(origTransNo, supportAcquirers);
        if (origTransData == null) {
            // trans not exist
            transEnd(new ActionResult(TransResult.ERR_NO_ORIG_TRANS, null));
            return;
        }

        TransData.ETransStatus trStatus = origTransData.getTransState();
        // void trans can not be revoked again
        if (trStatus.equals(TransData.ETransStatus.VOIDED)) {
            transEnd(new ActionResult(TransResult.ERR_HAS_VOIDED, null));
            return;
        }
        copyOrigTransData();
        gotoState(State.TRANS_DETAIL.toString());

    }

    // set original trans data
    private void copyOrigTransData() {
        //EDCBBLAND-437 Void transaction send incorrect TPDU. and NII
        Acquirer acquirer = origTransData.getAcquirer();
        FinancialApplication.getAcqManager().setCurAcq(acquirer);
        Component.transInit(transData, acquirer);

        transData.setAmount(origTransData.getAmount());
        transData.setOrigBatchNo(origTransData.getBatchNo());
        transData.setOrigAuthCode(origTransData.getAuthCode());
        transData.setOrigRefNo(origTransData.getRefNo());
        transData.setOrigTransNo(origTransData.getTraceNo());
        transData.setAcquirer(acquirer);
        transData.setIssuer(origTransData.getIssuer());
        transData.setOrigTransType(origTransData.getTransType());
        transData.setTraceNo(origTransData.getTraceNo());
        transData.setDateTime(origTransData.getDateTime());//EDCBBLAND-604 Fix issue send incorrect datetime
        transData.setRefNo(origTransData.getRefNo());
        transData.setAuthCode(origTransData.getAuthCode());
    }

    private LinkedHashMap<String, String> mapCreditDetails(LinkedHashMap<String, String> map) {
        String transType = origTransData.getTransType().getTransName();
        String amount = CurrencyConverter.convert(Utils.parseLongSafe(origTransData.getAmount(), 0), transData.getCurrency());

        transData.setEnterMode(origTransData.getEnterMode());
        transData.setTraceNo(origTransData.getTraceNo());
        transData.setOrigTranState(origTransData.getTransState());
        transData.setOnlineTrans(origTransData.isOnlineTrans());
        long baseAmount = Utils.parseLongSafe(origTransData.getAmount(), 0) - Utils.parseLongSafe(origTransData.getTipAmount(), 0);
        transData.setAmount(String.format(Locale.getDefault(), "%012d", baseAmount));

        // date and time
        //AET-95
        String formattedDate = TimeConverter.convert(origTransData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.TIME_PATTERN_DISPLAY);

        map.put(getString(R.string.history_detail_type), transType);
        map.put(getString(R.string.history_detail_amount), amount);
//        if (origTransData.isDccRequired()) {
//            String currencyNumeric = Tools.bytes2String(origTransData.getDccCurrencyCode());
//            amount = CurrencyConverter.convert(Utils.parseLongSafe(origTransData.getDccAmount(), 0), currencyNumeric);
//            map.put(getString(R.string.history_detail_dcc_ex_rate), Component.findDccExchangeRate(origTransData));
//            map.put(Utils.getString(R.string.history_detail_dcc_amount, CurrencyConverter.getCurrencySymbol(currencyNumeric, false)), amount);
//        }
        map.put(getString(R.string.history_detail_card_no), PanUtils.maskCardNo(origTransData.getPan(), origTransData.getIssuer().getPanMaskPattern()));
        map.put(getString(R.string.history_detail_auth_code), origTransData.getAuthCode());
        map.put(getString(R.string.history_detail_ref_no), origTransData.getRefNo());
        map.put(getString(R.string.history_detail_trace_no), Component.getPaddedNumber(origTransData.getTraceNo(), 6));
        map.put(getString(R.string.dateTime), formattedDate);
        return map;
    }

    private void showRespMsg(ActionResult result,String respMsg){
        DialogInterface.OnDismissListener onDismissListener = new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                transEnd(new ActionResult(TransResult.ERR_ABORTED, null));
            }
        };
        DialogUtils.showErrMessage(getCurrentContext(), getString(R.string.menu_void), respMsg, onDismissListener, Constants.FAILED_DIALOG_SHOW_TIME);

    }
}

