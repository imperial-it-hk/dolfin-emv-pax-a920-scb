package th.co.bkkps.scbapi.trans.action;

import android.content.Context;
import android.content.Intent;

import com.pax.abl.core.AAction;
import com.pax.pay.constant.EUIParamKeys;

import th.co.bkkps.scbapi.trans.action.activity.ScbIppNoDisplayActivity;

public class ActionScbIppReport extends AAction {

    private Context mContext;
    private @ScbIppNoDisplayActivity.ReportType int type;
    private String acquirerName;

    public ActionScbIppReport(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, @ScbIppNoDisplayActivity.ReportType int type, String acquirerName) {
        this.mContext = context;
        this.type = type;
        this.acquirerName = acquirerName;
    }

    @Override
    protected void process() {
        Intent intent = new Intent(mContext, ScbIppNoDisplayActivity.class);
        intent.putExtra(EUIParamKeys.SCB_IPP_REPORT_TYPE.toString(), type);
        intent.putExtra(EUIParamKeys.SCB_ACQ_NAME.toString(), acquirerName);
        mContext.startActivity(intent);
    }
}
