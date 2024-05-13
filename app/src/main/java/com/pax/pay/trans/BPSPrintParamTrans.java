package com.pax.pay.trans;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import th.co.bkkps.utils.Log;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.record.Printer;
import com.pax.pay.trans.action.ActionDispMessage;
import com.pax.pay.trans.action.ActionDispParam;
import com.pax.pay.trans.action.ActionPrintTransMessage;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.receipt.PrintListenerImpl;
import com.pax.pay.trans.receipt.ReceiptPreviewTrans;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by SORAYA S on 05-Feb-18.
 */

public class BPSPrintParamTrans extends BaseTrans {
    private Bitmap loadedBitmap = null;
    private PrintListenerImpl listener;
    public BPSPrintParamTrans(Context context, TransEndListener transListener) {
        super(context, ETransType.BPS_SHOWPARAM, transListener);
        setBackToMain(true);
    }

    @Override
    protected void bindStateOnAction() {
        ActionDispParam actionDispParam = new ActionDispParam(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                genReceipt();
                ((ActionDispParam) action).setParam(getCurrentContext(), "PARAMETER SETTINGS", loadedBitmap);
            }

            private void genReceipt() {
                if (loadedBitmap == null) {
                    //generate bitmap image of send preview
                    ReceiptPreviewTrans receiptPreviewTrans = new ReceiptPreviewTrans();
                    listener = new PrintListenerImpl(getCurrentContext());
                    loadedBitmap = receiptPreviewTrans.previewParam(listener);
                }
            }
        });
        bind(BPSPrintParamTrans.State.SHOW_PARAM.toString(), actionDispParam, true);

        ActionPrintTransMessage printReceiptAction = new ActionPrintTransMessage(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionPrintTransMessage) action).setParam(getCurrentContext(), loadedBitmap);
            }
        });
        bind(BPSPrintMsgTrans.State.PRINT.toString(), printReceiptAction, true);

        gotoState(BPSPrintParamTrans.State.SHOW_PARAM.toString());
    }

    enum State {
        SHOW_PARAM,
        PRINT,
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {
        BPSPrintParamTrans.State state = BPSPrintParamTrans.State.valueOf(currentState);

        switch (state) {
            case SHOW_PARAM:
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

}
