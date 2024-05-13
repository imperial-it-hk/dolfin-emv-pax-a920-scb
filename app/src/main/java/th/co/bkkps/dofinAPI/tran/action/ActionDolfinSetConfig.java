package th.co.bkkps.dofinAPI.tran.action;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.utils.Convert;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import th.co.bkkps.dofinAPI.DolfinApi;
//import th.co.bkkps.utils.Log;


public class ActionDolfinSetConfig extends AAction {
    private Context mContext;
    private String respMsg = null;

    public ActionDolfinSetConfig(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context) {
        this.mContext = context;
    }

    @Override
    protected void process() {

            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_DOLFIN);
            String merchName = FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_MERCHANT_NAME_EN);
            //String merchId = acquirer.getMerchantId() + "   ";
            String merchId = acquirer.getMerchantId().length() < 15
                             ? Convert.getInstance().stringPadding(acquirer.getMerchantId(),' ',15,Convert.EPaddingPosition.PADDING_RIGHT)
                             : acquirer.getMerchantId();
            String termId  = acquirer.getTerminalId();
            String storeId = acquirer.getStoreId();
            String storeName = acquirer.getStoreName();
            String tpdu = "600" + acquirer.getNii() + "0000";
            String nii = acquirer.getNii();
            HashMap<String,String> ipPort = findConfigIP(acquirer);
            String ip = ipPort.get("ip");
            String port = ipPort.get("port");
            String connectTimeout = String.valueOf(FinancialApplication.getSysParam().get(SysParam.NumberParam.COMM_TIMEOUT));
            String receiveTimeout = String.valueOf(acquirer.getRecvTimeout());
            String reconnectTimes = "3";
            String traceNo = String.valueOf(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO,0));
            String sysTraceAuditNo = String.valueOf(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO,0));

        String isEnableCScanB = String.valueOf(acquirer.isEnableCScanBMode());
        String cScanBDisplayQrTimeOut = String.valueOf(acquirer.getCScanBDisplayQrTimeout());
        //String cScanBDisplayQrTimeOut = String.valueOf(FinancialApplication.getSysParam().get(SysParam.NumberParam.DOLFIN_QR_ON_SCREEN_TIMEOUT));
        String cScanBRetryTimes = String.valueOf(acquirer.getCScanBRetryTimes());
        String cScanBDelayRetry = String.valueOf(acquirer.getCScanBDelayRetry());

        String language = FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_LANGUAGE);
        if (language != null && language.equals("THAI")) {
            language = "th";
        } else{
            language = "en";
        }

        String result = DolfinApi.getInstance().SetConfig(
                    merchName,
                    merchId,
                    termId,
                    storeId,
                    storeName,
                    tpdu,
                    nii,
                    ip,
                    port,
                    connectTimeout,
                    receiveTimeout,
                    reconnectTimes,
                    traceNo,
                sysTraceAuditNo,
                isEnableCScanB,
                cScanBDisplayQrTimeOut,
                cScanBRetryTimes,
                cScanBDelayRetry,
                language
        );
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
//                Log.e("Dolfin","SET_CONFIG  json = " + json);
//                Log.e("Dolfin","SET_CONFIG  respMsg = " + respMsg);
            }catch (JSONException err){
                ///Log.d("Error", err.toString());
            }
            setResult(new ActionResult(TransResult.SUCC, respCode,respMsg));

    }

    private HashMap<String,String> findConfigIP(Acquirer acquirer){
        HashMap<String,String> result = new HashMap<>();
        result.put("ip",acquirer.getIp());
        result.put("port", String.valueOf(acquirer.getPort()));
        return result;
    }

}



