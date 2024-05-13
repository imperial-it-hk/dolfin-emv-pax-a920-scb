package com.pax.pay.trans.action;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.service.AlipayWechatTransService;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.transmit.TransProcessListener;
import com.pax.pay.trans.transmit.TransProcessListenerImpl;

public class ActionAliWechatReversal extends AAction {

    private Context context;
    private TransData transData;
    private int reversalType;

    /**
     * derived classes must call super(listener) to set
     *
     * @param listener {@link ActionStartListener}
     */
    public ActionAliWechatReversal(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, TransData transData, int reversalType) {
        this.context = context;
        this.transData = transData;
        this.reversalType = reversalType;
    }

    @Override
    protected void process() {

        FinancialApplication.getApp().runInBackground(new Runnable() {
            @Override
            public void run() {
                TransProcessListener listener = new TransProcessListenerImpl(context);
                AlipayWechatTransService service = new AlipayWechatTransService(reversalType, transData, listener);
                int ret = service.process();
                setResult(new ActionResult(ret, null));
            }
        });
    }
}
