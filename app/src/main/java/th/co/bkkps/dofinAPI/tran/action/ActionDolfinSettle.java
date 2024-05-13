package th.co.bkkps.dofinAPI.tran.action;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import th.co.bkkps.dofinAPI.DolfinApi;
///import th.co.bkkps.utils.Log;


public class ActionDolfinSettle extends AAction {
    private Context mContext;
    private TransData transData;

    public ActionDolfinSettle(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context) {
        this.mContext = context;
    }

    @Override
    protected void process() {
        String result = DolfinApi.getInstance().Settle();
        JSONObject json = null;
        int errCode = -1;
        int respCode = -1;
        String respMsg = null;

        if(Constants.DOLFIN_ERR.equalsIgnoreCase(result)) {
            respMsg = Utils.getString(R.string.err_dolfin_no_connect);
            setResult(new ActionResult(TransResult.SUCC, respCode, respMsg));
            return;
        }

        try {
            json = new JSONObject(result);
            respCode = json.getInt("localCode");
            respMsg = json.getString("localMsg");

            if(respCode == 0 || respCode == 11){
                Component.incStanNo(null);
                Component.incTraceNo(null);
            }else{
                Component.incStanNo(null);
            }

        }catch (JSONException err){
            ///Log.d("Error", err.toString());
        }
        setResult(new ActionResult(TransResult.SUCC, respCode,respMsg));
    }
}



