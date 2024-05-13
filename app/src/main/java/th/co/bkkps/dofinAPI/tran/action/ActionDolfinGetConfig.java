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
import com.pax.view.dialog.CustomAlertDialog;
import com.pax.view.dialog.DialogUtils;

import org.json.JSONException;
import org.json.JSONObject;

import th.co.bkkps.dofinAPI.DolfinApi;
//import th.co.bkkps.utils.Log;


public class ActionDolfinGetConfig extends AAction {
    private Context mContext;
    private String respMsg = null;
    private TransData transData = null;
    private boolean isTransError = false;
    private boolean isDone = false;

    public ActionDolfinGetConfig(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context) {
        this.mContext = context;
    }

    public void setParam(Context context,TransData transData) {
        this.mContext = context;
        this.transData = transData;
    }

    public void setParam(Context context,TransData transData, boolean isTransError) {
        this.mContext = context;
        this.transData = transData;
        this.isTransError = isTransError;
    }

    @Override
    protected void process() {
//        showProgress();
        FinancialApplication.getApp().runInBackground(new Runnable() {
            @Override
            public void run() {
              /*  TransProcessListener transProcessListenerImpl = new TransProcessListenerImpl(mContext);
                transProcessListenerImpl.onShowProgress(mContext.getString(R.string.wait_process), 0);*/

                String result = DolfinApi.getInstance().GetConfig();
                JSONObject json = null;
                int respCode = -1;

                if(Constants.DOLFIN_ERR.equalsIgnoreCase(result)) {
                    respMsg = Utils.getString(R.string.err_dolfin_no_connect);
                    setResult(new ActionResult(TransResult.SUCC, respCode, respMsg));
                }

                try {
                    json = new JSONObject(result);
                    respCode = json.getInt("localCode");
                    respMsg = json.getString("localMsg");
//                    Log.e("Dolfin","GET_CONFIG  json = " + json);
//                    Log.e("Dolfin","GET_CONFIG  respMsg = " + respMsg);
                    if(respCode == 0){
                        long stanNo = json.getLong("sysTraceAuditNo");
                        long traceNo = json.getLong("traceNo");
//                        Log.e("Dolfin","GET_CONFIG  traceNo = " + traceNo + ", stanNo = " + stanNo);
                        setTraceStan(traceNo,stanNo);
                    }else{
                        setTraceStan(transData);
                    }
                    isDone = true;
                }catch (JSONException err){
                    //Log.d("Error", err.toString());
                }

//                transProcessListenerImpl.onHideProgress();
                setResult(new ActionResult(TransResult.SUCC, respCode,respMsg));
            }
        });

    }

    private void showProgress() {
        final CustomAlertDialog dialog = DialogUtils.showProcessingMessage(mContext, mContext.getString(R.string.wait_process), 10);
        FinancialApplication.getApp().runInBackground(new Runnable() {
            @Override
            public void run() {
                boolean actionFinish = false;
                while(!actionFinish){
                    //Log.e("Dolfin","GET_CONFIG  actionFinish = " + actionFinish);
                    actionFinish = isDone;
                }
                if(actionFinish) {
                    FinancialApplication.getApp().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                        }
                    });
                }

            }
        });
    }


    private void setTraceStan(long traceNo,long stanNo){
            Component.incTraceNo(traceNo);
            Component.incStanNo(stanNo);
    }

    private void setTraceStan(TransData transData){
        if(!isTransError){
            Component.incTraceNo(transData);
        }
        Component.incStanNo(transData);
    }
}



