package th.co.bkkps.dofinAPI.tran.action;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.Issuer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.AcqManager;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import org.json.JSONException;
import org.json.JSONObject;

import th.co.bkkps.dofinAPI.DolfinApi;
import th.co.bkkps.utils.Log;

public class ActionDolfinSaleCScanB extends AAction {
    private Context mContext;
    private String mAmount;
    private TransData transData;
    private boolean isPrintQR;
    private String respMsg = null;

    public ActionDolfinSaleCScanB(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, String amount, TransData transData,boolean isPrintQR) {
        this.mContext = context;
        this.mAmount = amount;
        this.transData = transData;
        this.isPrintQR = isPrintQR;
    }

    @Override
    protected void process() {
        new Thread(() -> {

            initTransdataDolfin(transData);
            String result= DolfinApi.getInstance().SaleCScanB(mAmount,isPrintQR);
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
//                Log.e("Dolfin","SALE  json = " + json);
//                Log.e("Dolfin","SALE  respMsg = " + respMsg);
                if(respCode == 0 || respCode == 11){
                    transData.setAuthCode(json.getString("authCode"));
                    transData.setRefNo(json.getString("refNo"));
                    transData.setOnlineTrans(true);
                    transData.setResponseCode(FinancialApplication.getRspCode().parse("00"));

                    transData.setStanNo(json.getLong("sysTraceAuditNo"));
                    transData.setTraceNo(json.getLong("traceNo"));
                    FinancialApplication.getTransDataDbHelper().insertTransData(transData);
                    Component.incTraceNo(transData);
                }
                Component.incStanNo(transData);
            }catch (JSONException err){
                Log.d("Error", err.toString());
            }
            setResult(new ActionResult(TransResult.SUCC, respCode,respMsg));

        }).start();

    }

    private void initTransdataDolfin(TransData transData){
        AcqManager acqManager = FinancialApplication.getAcqManager();
        Acquirer acquirer = acqManager.findAcquirer(Constants.ACQ_DOLFIN);
        Issuer issuer = acqManager.findIssuer(Constants.ISSUER_DOLFIN);
        transData.setAcquirer(acquirer);
        transData.setIssuer(issuer);
        transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
        transData.setTraceNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO));
        transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
        transData.setBatchNo(acquirer.getCurrBatchNo());
        transData.setTransState(TransData.ETransStatus.NORMAL);
    }

}
