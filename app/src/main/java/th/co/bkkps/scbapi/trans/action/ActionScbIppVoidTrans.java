package th.co.bkkps.scbapi.trans.action;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.abl.core.ATransaction.TransEndListener;
import com.pax.pay.trans.model.TransData;

import th.co.bkkps.scbapi.trans.ScbIppVoidTran;

public class ActionScbIppVoidTrans extends AAction {

    private Context mContext;
    private TransData origTransData;

    public ActionScbIppVoidTrans(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, TransData origTransData) {
        this.mContext = context;
        this.origTransData = origTransData;
    }

    @Override
    protected void process() {
        new ScbIppVoidTran(mContext, endListener, origTransData).execute();
    }

    private TransEndListener endListener = new TransEndListener() {

        @Override
        public void onEnd(ActionResult result) {
            setResult(result);
        }
    };
}
