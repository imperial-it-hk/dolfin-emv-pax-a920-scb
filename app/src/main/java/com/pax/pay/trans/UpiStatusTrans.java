package com.pax.pay.trans;

import android.content.Context;
import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.action.ActionDispMessage;
import com.pax.pay.trans.action.ActionPrintTransMessage;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import java.util.ArrayList;
import java.util.List;

public class UpiStatusTrans extends BaseTrans {

    private TransData transData;
    private String[] statusMsg;

    public UpiStatusTrans(Context context, TransEndListener transListener) {
        super(context, ETransType.UPI_STATUS, transListener);
    }

    @Override
    protected void bindStateOnAction() {

        ActionDispMessage confirmInfoAction = new ActionDispMessage(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {

                List<Acquirer> acquirers = FinancialApplication.getAcqManager().findEnableAcquirers();
                List<String> list = new ArrayList<String>();

                statusMsg = new String[acquirers.size()+1];

                String status;
                int ind=1;

                list.add("*** UPI STATUS ***");
                statusMsg[0] = "*** UPI STATUS ***";

                for (Acquirer i : acquirers) {
                    if(i.getName().equalsIgnoreCase(Constants.ACQ_UP)){
                        if (i.getUP_TMK()!=null && i.getUP_TWK()!=null) {
                            status = String.format("%s = %s",i.getName(),String.valueOf("TRUE"));
                        }
                        else
                        {
                            status = String.format("%s = %s",i.getName(),String.valueOf("FALSE"));
                        }
                        list.add(status);
                        statusMsg[ind++] = status;
                    }
                }

                ((ActionDispMessage) action).setParam(getCurrentContext(),
                        getString(R.string.trans_upi_status), list);
            }
        });
        bind(UpiStatusTrans.State.TRANS_DETAIL.toString(), confirmInfoAction, true);

        ActionPrintTransMessage printReceiptAction = new ActionPrintTransMessage(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionPrintTransMessage) action).setParam(getCurrentContext(), statusMsg, null);
            }
        });
        bind(UpiStatusTrans.State.PRINT.toString(), printReceiptAction, true);

        gotoState(UpiStatusTrans.State.TRANS_DETAIL.toString());
    }

    enum State {
        TRANS_DETAIL,
        PRINT,
    }

    @Override
    public void onActionResult(String currentState, ActionResult result) {
        UpiStatusTrans.State state = UpiStatusTrans.State.valueOf(currentState);

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
}
