package th.co.bkkps.edc.trans.action;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.pax.abl.core.AAction;
import com.pax.edc.R;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.action.activity.EnterRedeemKbankActivity;
import com.pax.pay.utils.Utils;

import th.co.bkkps.edc.trans.action.activity.EnterInstalmentAmexActivity;

public class ActionEnterInstalmentAMEX extends AAction {
    private Context context;
    private String title;

    public ActionEnterInstalmentAMEX(ActionStartListener listener) {
        super(listener);
    }

    public static class InstalmentAmexInfo {
        private String instalmentAmount;
        private String instalmentTerms;
        private String instalmentMonthDue;

        public InstalmentAmexInfo(String instalmentAmount, String instalmentTerms, String instalmentMonthDue){
           this.instalmentAmount = instalmentAmount;
           this.instalmentTerms = instalmentTerms;
           this.instalmentMonthDue = instalmentMonthDue;
        }

        public String getInstalmentAmount() {return instalmentAmount;}
        public void setInstalmentAmount(String instalmentAmount) {this.instalmentAmount = instalmentAmount;}
        public String getInstalmentTerms() { return instalmentTerms;}
        public void setInstalmentTerms(String instalmentTerms) { this.instalmentTerms = instalmentTerms; }
        public String getInstalmentMonthDue() {return instalmentMonthDue;}
        public void setInstalmentMonthDue(String instalmentMonthDue) {this.instalmentMonthDue = instalmentMonthDue;}
    }

    public void setParam(Context context, String title) {
        this.context = context;
        this.title = title;
    }

    @Override
    protected void process() {
        /*Intent intent = new Intent(context, EnterInstalmentAmexActivity.class);
        context.startActivity(intent);*/

        Intent intent = new Intent(context, EnterInstalmentAmexActivity.class);
        intent.putExtra(EUIParamKeys.NAV_TITLE.toString(), title);
        intent.putExtra(EUIParamKeys.NAV_BACK.toString(), true);
        context.startActivity(intent);
    }


}
