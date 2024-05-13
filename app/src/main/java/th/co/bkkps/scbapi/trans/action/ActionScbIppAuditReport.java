package th.co.bkkps.scbapi.trans.action;

import android.content.Context;
import android.content.Intent;

import com.pax.abl.core.AAction;
import com.pax.pay.constant.EUIParamKeys;

import th.co.bkkps.scbapi.trans.action.activity.ScbIppNoDisplayActivity;

public class ActionScbIppAuditReport extends AAction {

    private Context mContext;

    public ActionScbIppAuditReport(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context) {
        this.mContext = context;
    }

    @Override
    protected void process() {
        Intent intent = new Intent(mContext, ScbIppNoDisplayActivity.class);
        intent.putExtra(EUIParamKeys.SCB_IPP_REPORT_TYPE.toString(), ScbIppNoDisplayActivity.ReportType.PRN_AUDIT_REPORT);
        mContext.startActivity(intent);
    }
}
