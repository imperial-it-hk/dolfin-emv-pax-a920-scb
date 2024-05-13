package com.pax.pay.trans;

import android.content.Context;
import android.content.SharedPreferences;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.abl.utils.PanUtils;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.action.ActionDispMessage;
import com.pax.pay.trans.action.ActionInputTransData;
import com.pax.pay.trans.action.ActionPrintTransMessage;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.TimeConverter;
import com.pax.pay.utils.Utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by SORAYA S on 05-Feb-18.
 */

public class BPSPrintMsgTrans extends BaseTrans {

    private TransData transData;
    private String[] respMsg;
    private String[] reqMsg;

    public BPSPrintMsgTrans(Context context, TransEndListener transListener) {
        super(context, ETransType.BPS_SHOWMSG, transListener);
        setBackToMain(true);
    }

    @Override
    protected void bindStateOnAction() {

        ActionDispMessage confirmInfoAction = new ActionDispMessage(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {

                reqMsg = loadArray("ReqMsg",context);
                respMsg = loadArray("RespMsg",context);

                LinkedHashMap<String, String> map = new LinkedHashMap<>();
                List<String> list = new ArrayList<String>();

                for(String req : reqMsg){
                    list.add(req);
                }
                list.add(" ");
                for(String resp : respMsg){
                    list.add(resp);
                }
                ((ActionDispMessage) action).setParam(getCurrentContext(),
                        getString(R.string.trans_print_msg), list);
            }
        });
        bind(BPSPrintMsgTrans.State.TRANS_DETAIL.toString(), confirmInfoAction, true);

        ActionPrintTransMessage printReceiptAction = new ActionPrintTransMessage(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionPrintTransMessage) action).setParam(getCurrentContext(), reqMsg, respMsg);
            }
        });
        bind(BPSPrintMsgTrans.State.PRINT.toString(), printReceiptAction, true);

        gotoState(BPSPrintMsgTrans.State.TRANS_DETAIL.toString());
    }

    enum State {
        TRANS_DETAIL,
        PRINT,
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {
        BPSPrintMsgTrans.State state = BPSPrintMsgTrans.State.valueOf(currentState);

        switch (state) {
            case TRANS_DETAIL:
                gotoState(State.PRINT.toString());
                break;
            case PRINT:
                if (result.getRet() == TransResult.SUCC) {
                    // end trans
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

    public String[] loadArray(String arrayName, Context mContext) {
        SharedPreferences prefs = mContext.getSharedPreferences("preferencename", 0);
        int size = prefs.getInt(arrayName + "_size", 0);
        String array[] = new String[size];
        for(int i=0;i<size;i++)
            array[i] = prefs.getString(arrayName + "_" + i, null);
        return array;
    }
}
