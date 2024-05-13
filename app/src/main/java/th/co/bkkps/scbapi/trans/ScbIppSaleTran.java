package th.co.bkkps.scbapi.trans;

import android.content.Context;

import androidx.annotation.IntDef;

import com.pax.abl.core.ActionResult;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.trans.BaseTrans;
import com.pax.pay.trans.model.ETransType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import th.co.bkkps.bpsapi.TransResponse;
import th.co.bkkps.scbapi.ScbIppService;
import th.co.bkkps.scbapi.trans.action.ActionScbIppSale;
import th.co.bkkps.scbapi.trans.action.ActionScbUpdateParam;

public class ScbIppSaleTran extends BaseTrans {

    private @ScbIppType long scbIppType;

    @IntDef({ScbIppType.MERCHANT, ScbIppType.CUSTOMER, ScbIppType.SPECIAL , ScbIppType.PROMO_FIX})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ScbIppType {
        int MERCHANT = 0;
        int CUSTOMER = 1;
        int SPECIAL = 2;
        int PROMO_FIX = 3;
    }

    public ScbIppSaleTran(Context context, TransEndListener transListener,/* ITransAPI transAPI,*/ @ScbIppType int ippType) {
        super(context, ETransType.SALE, transListener);
        setBackToMain(true);
        this.scbIppType = ippType;
    }

    @Override
    protected void bindStateOnAction() {
        ActionScbUpdateParam actionScbUpdateParam = new ActionScbUpdateParam(action -> {
            ((ActionScbUpdateParam) action).setParam(getCurrentContext());
        });
        bind(State.SENT_CONFIG.toString(), actionScbUpdateParam);

        ActionScbIppSale actionScbIppSale = new ActionScbIppSale(action -> {
            ((ActionScbIppSale) action).setParam(getCurrentContext(), scbIppType);
        });
        bind(State.SALE.toString(), actionScbIppSale, false);

        if (!ScbIppService.isSCBInstalled(getCurrentContext())) {
            transEnd(new ActionResult(TransResult.ERR_SCB_CONNECTION, null));
            return;
        }
        gotoState(State.SENT_CONFIG.toString());
    }

    enum State {
        SENT_CONFIG,
        SALE
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {
        State state = State.valueOf(currentState);

        switch (state) {
            case SENT_CONFIG:
                if (result.getRet() == TransResult.SUCC) {
                    gotoState(State.SALE.toString());
                    return;
                }
                transEnd(result);
                break;
            case SALE:
                ScbIppService.insertTransData(transData, (TransResponse) result.getData());
                transEnd(new ActionResult(TransResult.ERR_ABORTED, null));//no alert dialog
                break;
            default:
                transEnd(result);
                break;
        }
    }
}
