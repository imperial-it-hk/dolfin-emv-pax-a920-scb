package th.co.bkkps.scbapi.trans.action;

import android.content.Context;
import android.content.Intent;

import com.pax.abl.core.AAction;
import com.pax.pay.constant.EUIParamKeys;

import th.co.bkkps.scbapi.trans.action.activity.ScbIppSaleActivity;

public class ActionScbIppSale extends AAction {

    private Context mContext;
    private long ippType;

    public ActionScbIppSale(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, long ippType) {
        this.mContext = context;
        this.ippType = ippType;
    }

    @Override
    protected void process() {
        Intent intent = new Intent(mContext, ScbIppSaleActivity.class);
        intent.putExtra(EUIParamKeys.SCB_IPP_TYPE.toString(), ippType);
        mContext.startActivity(intent);
    }

}
