package th.co.bkkps.scbapi.trans.action.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.TransContext;
import com.pax.settings.SysParam;

import th.co.bkkps.bpsapi.ITransAPI;
import th.co.bkkps.bpsapi.SettingMsg;
import th.co.bkkps.bpsapi.TransAPIFactory;
import th.co.bkkps.bpsapi.TransResult;
import th.co.bkkps.scbapi.ScbIppService;

public class ScbIppSettingActivity extends Activity {
    ITransAPI transAPI;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        transAPI = TransAPIFactory.createTransAPI();

        if (!ScbIppService.isSCBInstalled(this)) {
            finish(new ActionResult(TransResult.ERR_ABORTED, null));
            return;
        }

        String jsonString = buildJsonParam();
        if (jsonString == null) {
            finish(new ActionResult(TransResult.ERR_PROCESS_FAILED, null));
            return;
        }

        SettingMsg.Request settingReq = new SettingMsg.Request();
        settingReq.setJsonSetting(jsonString);
        TransAPIFactory.createTransAPI().startTrans(this, settingReq);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d("ScbIppSettingActivity", "BpsApi onActivityResult invoked");
        SettingMsg.Response response = (SettingMsg.Response) transAPI.onResult(requestCode, resultCode, data);

        if (response != null) {
            Log.d("BpsApi", "getRspCode=" + response.getRspCode());
            finish(new ActionResult(response.getRspCode(), null));
            return;
        }
        Log.d("BpsApi", "response is null");

        finish(new ActionResult(TransResult.ERR_PARAM, null));
    }

    public void finish(ActionResult result) {
        AAction action = TransContext.getInstance().getCurrentAction();
        if (action != null) {
            if (action.isFinished())
                return;
            action.setFinished(true);
            action.setResult(result);
        }

        finish();
    }


    private String buildJsonParam() {
        Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_IPP);
        Acquirer acqRedeem = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_REDEEM);
        if ((acquirer == null || !acquirer.isEnable()) &&
                (acqRedeem == null || !acqRedeem.isEnable())) {
            return null;
        }

        JSONArray jsonArray = new JSONArray();

        JSONObject commObject = new JSONObject();
        commObject.put("type", FinancialApplication.getSysParam().get(SysParam.StringParam.COMM_TYPE));
        commObject.put("timeout", FinancialApplication.getSysParam().get(SysParam.NumberParam.COMM_TIMEOUT));
        commObject.put("apn", FinancialApplication.getSysParam().get(SysParam.StringParam.MOBILE_APN_SYSTEM));

        JSONObject edcObject = new JSONObject();
        edcObject.put("merchantName", FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_MERCHANT_NAME_EN));
        edcObject.put("merchantAdd", FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_MERCHANT_ADDRESS));
        edcObject.put("merchantAdd1", FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_MERCHANT_ADDRESS1));
        edcObject.put("receiptNo", FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_RECEIPT_NUM));
        edcObject.put("stanNo", FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
        edcObject.put("traceNo", FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO));
        edcObject.put("language", FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_LANGUAGE));
        edcObject.put("voidWithStan", FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_VOID_WITH_STAND));

        JSONArray jsonAcqArray = new JSONArray();
        if (acquirer != null) {
            JSONObject acqObject = new JSONObject();
            acqObject.put("name", acquirer.getName());
            acqObject.put("terminalId", acquirer.getTerminalId());
            acqObject.put("merchantId", acquirer.getMerchantId());
            acqObject.put("nii", acquirer.getNii());
            acqObject.put("ip", acquirer.getIp());
            acqObject.put("port", acquirer.getPort());
            acqObject.put("ipBak1", acquirer.getIpBak1());
            acqObject.put("portBak1", acquirer.getPortBak1());
            acqObject.put("ipBak2", acquirer.getIpBak2());
            acqObject.put("portBak2", acquirer.getPortBak2());
            acqObject.put("ipBak3", acquirer.getIpBak3());
            acqObject.put("portBak3", acquirer.getPortBak3());
            acqObject.put("isEnableTle", acquirer.isEnableTle());
            jsonAcqArray.add(acqObject);
        }

        if (acqRedeem != null) {
            JSONObject acqObject = new JSONObject();
            acqObject.put("name", acqRedeem.getName());
            acqObject.put("terminalId", acqRedeem.getTerminalId());
            acqObject.put("merchantId", acqRedeem.getMerchantId());
            acqObject.put("nii", acqRedeem.getNii());
            acqObject.put("ip", acqRedeem.getIp());
            acqObject.put("port", acqRedeem.getPort());
            acqObject.put("ipBak1", acqRedeem.getIpBak1());
            acqObject.put("portBak1", acqRedeem.getPortBak1());
            acqObject.put("ipBak2", acqRedeem.getIpBak2());
            acqObject.put("portBak2", acqRedeem.getPortBak2());
            acqObject.put("ipBak3", acqRedeem.getIpBak3());
            acqObject.put("portBak3", acqRedeem.getPortBak3());
            acqObject.put("isEnableTle", acqRedeem.isEnableTle());
            jsonAcqArray.add(acqObject);
        }

        JSONObject jObject = new JSONObject();
        jObject.put("comm", commObject);
        jObject.put("edc", edcObject);
        jObject.put("acquirers", jsonAcqArray);

        jsonArray.add(jObject);

        return jsonArray.toString();
    }
}
