package th.co.bkkps.scbapi.trans.action;

import android.content.Context;
import android.content.Intent;

import com.pax.abl.core.AAction;
import com.pax.pay.constant.EUIParamKeys;

import th.co.bkkps.scbapi.trans.action.activity.ScbIppVoidActivity;

public class ActionScbIppVoidAPI extends AAction {

    private Context mContext;
    private long traceNo;

    public ActionScbIppVoidAPI(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, long traceNo) {
        this.mContext = context;
        this.traceNo = traceNo;
    }

    @Override
    protected void process() {
        Intent intent = new Intent(mContext, ScbIppVoidActivity.class);
        intent.putExtra(EUIParamKeys.SCB_TRACE_NO.toString(), traceNo);
        mContext.startActivity(intent);
    }
}
