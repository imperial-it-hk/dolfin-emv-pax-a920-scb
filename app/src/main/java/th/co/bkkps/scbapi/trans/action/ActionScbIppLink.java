package th.co.bkkps.scbapi.trans.action;

import android.content.Context;
import android.content.Intent;

import com.pax.abl.core.AAction;
import com.pax.pay.constant.EUIParamKeys;

import th.co.bkkps.scbapi.trans.action.activity.ScbIppLinkActivity;

public class ActionScbIppLink extends AAction {

    private Context mContext;
    private Class innerClass;
    private String acquirerName;

    private scbIppLinkType scbLinkType = scbIppLinkType.NONE;

    public ActionScbIppLink(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, scbIppLinkType type, String acquirerName) {
        this.mContext = context;
        this.scbLinkType = type;
        this.acquirerName = acquirerName;
    }

    @Override
    protected void process() {

        Intent intent = new Intent(mContext, ScbIppLinkActivity.class);
        intent.putExtra(EUIParamKeys.SCB_IPP_STATE_LINK_TYPE.toString(), scbLinkType.toString());
        intent.putExtra(EUIParamKeys.SCB_ACQ_NAME.toString(), acquirerName);
        mContext.startActivity(intent);
    }

    public enum scbIppLinkType {
        NONE,
        SETTLEMENT,
        PRINT_SUMMARY_REPORT,
        PRINT_DETAIL_REPORT,
        PRINT_LAST_BATCH,
        CLEAR_REVERSAL,
        CLEAR_TRADE_VOUCHER
    }




}
