package th.co.bkkps.edc.trans.action;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.pax.abl.core.AAction;
import com.pax.edc.R;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.Utils;

import th.co.bkkps.edc.trans.action.activity.*;

public class ActionEnterRedeemBay extends AAction {
    private Context context;
    private String title;
    private int type = 0;//RedeemType 1=Full,2=Partial,3=Catalogue
    TransData transData;

    public ActionEnterRedeemBay(ActionStartListener listener) {
        super(listener);
    }

    public static class RedeemBayInfo {
        private String redeemAmount;
        private String redeemMktCode;
        private String redeemPrdCode;
        private String redeemPoints;
        private String redeemType;

        public RedeemBayInfo(String redeemAmount,String redeemPoints){
           this.redeemAmount = redeemAmount;
           this.redeemPoints = redeemPoints;
        }

        public RedeemBayInfo(String redeemAmount, String redeemMktCode, String redeemPrdCode, String redeemPoints, String redeemType){
            this.redeemAmount = redeemAmount;
            this.redeemMktCode = redeemMktCode;
            this.redeemPrdCode = redeemPrdCode;
            this.redeemPoints = redeemPoints;
            this.redeemType = redeemType;
        }

        public String getRedeemAmount() { return redeemAmount;}
        public void setRedeemAmount(String redeemAmount) { this.redeemAmount = redeemAmount;  }
        public String getRedeemMktCode() {return redeemMktCode;}
        public void setRedeemMktCode(String redeemMktCode) {this.redeemMktCode = redeemMktCode;}
        public String getRedeemPrdCode() {return redeemPrdCode;}
        public void setRedeemPrdCode(String redeemPrdCode) {this.redeemPrdCode = redeemPrdCode;}
        public String getRedeemPoints() {return redeemPoints;}
        public void setRedeemPoints(String redeemPoints) {
            this.redeemPoints = redeemPoints;
        }
        public String getRedeemType() { return redeemType; }
        public void setRedeemType(String redeemType) { this.redeemType = redeemType; }
    }

    public void setParam(Context context, String title, TransData transData, int type) {
        this.context = context;
        this.title = title;
        this.type = type;
        this.transData = transData;
    }

    @Override
    protected void process() {
        //RedeemType 1=Full,2=Partial,3=Catalogue
        Log.e("menu"," ActionEnterRedeemBay type = " + type);

        Intent intent = new Intent(context, EnterRedeemBayActivity.class);
        intent.putExtra(Utils.getString(R.string.redeem_type), type);
        intent.putExtra(EUIParamKeys.TRANS_TYPE.toString(), transData.getTransType());
        intent.putExtra(EUIParamKeys.NAV_TITLE.toString(), title);
        intent.putExtra(EUIParamKeys.NAV_BACK.toString(), true);
        context.startActivity(intent);
    }


}
