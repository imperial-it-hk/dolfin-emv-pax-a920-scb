package th.co.bkkps.edc.trans.action;

import android.content.Context;
import android.content.Intent;
import com.pax.abl.core.AAction;
import com.pax.pay.constant.EUIParamKeys;

import th.co.bkkps.edc.trans.action.activity.EnterInstalmentBayActivity;
import th.co.bkkps.edc.trans.action.activity.EnterInstalmentBaySpMscActivity;

public class ActionEnterInstalmentBay extends AAction {
    private Context context;
    private String title;
    private Boolean isSpMsc;

    public ActionEnterInstalmentBay(ActionStartListener listener) {
        super(listener);
    }

    public static class InstalmentBayInfo {
        private String instalmentAmount;
        private String instalmentTerms;
        private String instalmentInterest;
        private String instalmentSerialNum;
        private String instalmentMktCode;
        private String instalmentSku;

        public InstalmentBayInfo(String instalmentAmount, String instalmentTerms, String instalmentInterest,String instalmentSerialNum){
           this.instalmentAmount = instalmentAmount;
           this.instalmentTerms = instalmentTerms;
           this.instalmentInterest = instalmentInterest;
            this.instalmentSerialNum = instalmentSerialNum;
        }

        public InstalmentBayInfo(String instalmentAmount, String instalmentTerms, String instalmentSerialNum, String instalmentMktCode, String instalmentSku){
            this.instalmentAmount = instalmentAmount;
            this.instalmentTerms = instalmentTerms;
            this.instalmentSerialNum = instalmentSerialNum;
            this.instalmentMktCode = instalmentMktCode;
            this.instalmentSku = instalmentSku;
        }

        public String getInstalmentAmount() { return instalmentAmount;}
        public void setInstalmentAmount(String instalmentAmount) { this.instalmentAmount = instalmentAmount;  }
        public String getInstalmentTerms() { return instalmentTerms; }
        public void setInstalmentTerms(String instalmentTerms) { this.instalmentTerms = instalmentTerms;}
        public String getInstalmentInterest() {return instalmentInterest;}
        public void setInstalmentInterest(String instalmentInterest) {this.instalmentInterest = instalmentInterest; }
        public String getInstalmentSerialNum() {return instalmentSerialNum;}
        public void setInstalmentSerialNum(String instalmentSerialNum) {this.instalmentSerialNum = instalmentSerialNum;}
        public String getInstalmentMktCode() {return instalmentMktCode;}
        public void setInstalmentMktCode(String instalmentMktCode) {this.instalmentMktCode = instalmentMktCode;}
        public String getInstalmentSku() {return instalmentSku; }
        public void setInstalmentSku(String instalmentSku) {this.instalmentSku = instalmentSku;}
    }

    public void setParam(Context context, String title,Boolean isSpMsc) {
        this.context = context;
        this.title = title;
        this.isSpMsc = isSpMsc;
    }

    @Override
    protected void process() {
        if(isSpMsc) {
            Intent intent = new Intent(context, EnterInstalmentBaySpMscActivity.class);
            intent.putExtra(EUIParamKeys.NAV_TITLE.toString(), title);
            intent.putExtra(EUIParamKeys.NAV_BACK.toString(), true);
            context.startActivity(intent);
        }else{
            Intent intent = new Intent(context, EnterInstalmentBayActivity.class);
            intent.putExtra(EUIParamKeys.NAV_TITLE.toString(), title);
            intent.putExtra(EUIParamKeys.NAV_BACK.toString(), true);
            context.startActivity(intent);
        }
    }


}
