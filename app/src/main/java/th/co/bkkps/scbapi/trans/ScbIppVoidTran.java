package th.co.bkkps.scbapi.trans;

import android.content.Context;

import com.pax.abl.core.ActionResult;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.trans.BaseTrans;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.settings.SysParam;

import th.co.bkkps.bpsapi.TransResponse;
import th.co.bkkps.scbapi.ScbIppService;
import th.co.bkkps.scbapi.trans.action.ActionScbIppVoidAPI;
import th.co.bkkps.scbapi.trans.action.ActionScbUpdateParam;

public class ScbIppVoidTran extends BaseTrans {

    private TransData origTransData;

    public ScbIppVoidTran(Context context, TransEndListener transListener, TransData origTransData) {
        super(context, ETransType.VOID, transListener);
        setBackToMain(true);
        this.origTransData = origTransData;
    }

    @Override
    protected void bindStateOnAction() {
        ActionScbUpdateParam actionScbUpdateParam = new ActionScbUpdateParam(action -> {
            ((ActionScbUpdateParam) action).setParam(getCurrentContext());
        });
        bind(ScbIppSaleTran.State.SENT_CONFIG.toString(), actionScbUpdateParam);

        ActionScbIppVoidAPI actionScbIppVoidAPI = new ActionScbIppVoidAPI(action -> {
            long voucherNo = 0;
            if (origTransData != null) {
                voucherNo = origTransData.getTraceNo();
                if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_VOID_WITH_STAND, false)) {
                    voucherNo = origTransData.getStanNo();
                }
            }
            ((ActionScbIppVoidAPI) action).setParam(getCurrentContext(), voucherNo);
        });
        bind(State.VOID.toString(), actionScbIppVoidAPI, false);

        if (!ScbIppService.isSCBInstalled(getCurrentContext())) {
            transEnd(new ActionResult(TransResult.ERR_SCB_CONNECTION, null));
            return;
        }
        gotoState(State.SENT_CONFIG.toString());
    }

    enum State {
        SENT_CONFIG,
        VOID
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {
        State state = State.valueOf(currentState);

        switch (state) {
            case SENT_CONFIG:
                if (result.getRet() == TransResult.SUCC) {
                    gotoState(State.VOID.toString());
                    return;
                }
                transEnd(result);
                break;
            case VOID:
                if (result.getRet() != TransResult.SUCC) {
                    TransResponse response = (TransResponse) result.getData();
                    ScbIppService.updateEdcTraceStan(response);
                } else {
                    if (result.getData() != null) {
                        TransResponse response = (TransResponse) result.getData();
                        if (origTransData == null) {
                            origTransData = FinancialApplication.getTransDataDbHelper().findTransDataByTraceNo(response.getVoucherNo(), false);
                        }

                        if (origTransData == null) {
                            transEnd(new ActionResult(TransResult.ERR_ABORTED, null));//no alert dialog
                            return;
                        }

                        transData.setOrigBatchNo(origTransData.getBatchNo());
                        transData.setOrigAuthCode(origTransData.getAuthCode());
                        transData.setOrigRefNo(origTransData.getRefNo());
                        transData.setOrigTransNo(origTransData.getTraceNo());
                        transData.setOrigTransType(origTransData.getTransType());
                        transData.setOrigDateTime(origTransData.getDateTime());
                        ScbIppService.insertTransData(transData, response);

                        origTransData.setVoidStanNo(transData.getStanNo());
                        origTransData.setDateTime(transData.getDateTime());
                        origTransData.setTransState(TransData.ETransStatus.VOIDED);
                        String authCode = transData.getAuthCode() != null ? transData.getAuthCode() : transData.getOrigAuthCode();
                        origTransData.setAuthCode(authCode);
                        FinancialApplication.getTransDataDbHelper().updateTransData(origTransData);
                    }
                }

                transEnd(new ActionResult(TransResult.ERR_ABORTED, null));//no alert dialog
                break;
            default:
                transEnd(result);
                break;
        }
    }
}
