package th.co.bkkps.dofinAPI.tran.action;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import th.co.bkkps.dofinAPI.DolfinApi;
//import th.co.bkkps.utils.Log;


public class ActionDolfinVoid extends AAction {
    private Context mContext;
    private TransData transData;

    public ActionDolfinVoid(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, TransData transData) {
        this.mContext = context;
        this.transData = transData;
    }

    @Override
    protected void process() {

            String result = DolfinApi.getInstance().Void(String.valueOf(transData.getOrigTransNo()));
            JSONObject json = null;
            int respCode = -1;
            String respMsg = null;

            if(Constants.DOLFIN_ERR.equalsIgnoreCase(result)) {
                respMsg = Utils.getString(R.string.err_dolfin_no_connect);
                setResult(new ActionResult(TransResult.SUCC, respCode, respMsg));
            }

            try {
                json = new JSONObject(result);
                respCode = json.getInt("localCode");
                respMsg = json.getString("localMsg");
///                Log.e("Dolfin","VOID  json = " + json);
//                Log.e("Dolfin","VOID  respMsg = " + respMsg);
                if(respCode == 00 || respCode == 11){
                    transData.setStanNo(json.getLong("sysTraceAuditNo"));
                    transData.setTraceNo(json.getLong("traceNo"));
                    transData.setResponseCode(FinancialApplication.getRspCode().parse("00"));

                    FinancialApplication.getTransDataDbHelper().insertTransData(transData);
                }
                Component.incStanNo(null);
                transData.setResponseCode(FinancialApplication.getRspCode().parse("00"));

            }catch (JSONException err){
                //Log.d("Error", err.toString());
            }
            setResult(new ActionResult(TransResult.SUCC, respCode,respMsg));


    }


}



