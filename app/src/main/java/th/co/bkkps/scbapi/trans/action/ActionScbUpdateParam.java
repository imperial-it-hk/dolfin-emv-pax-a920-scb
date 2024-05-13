package th.co.bkkps.scbapi.trans.action;

import android.content.Context;
import android.content.Intent;

import com.pax.abl.core.AAction;

import th.co.bkkps.scbapi.trans.action.activity.ScbIppSettingActivity;

public class ActionScbUpdateParam extends AAction {

    private Context mContext;

    public ActionScbUpdateParam(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context) {
        this.mContext = context;
    }

    @Override
    protected void process() {
        Intent intent = new Intent(mContext, ScbIppSettingActivity.class);
        mContext.startActivity(intent);
    }
}
