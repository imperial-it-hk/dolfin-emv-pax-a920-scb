package th.co.bkkps.dofinAPI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RespJsonParser {

    private StringBuilder textBuilder = new StringBuilder();

    public String parse(String transType, String jsonStr) {
        JSONObject json = null;
        try {
            json = new JSONObject(jsonStr);
        } catch (JSONException ignore) {
        }
        if (json == null) {
            return "";
        }
        parseCommon(json);
        switch (transType) {
            case "SALE":
            case "SALE_C_SCAN_B":
            case "VOID":
            case "SETTLE":
                parseTransData(json);
                break;
            case "RVCODE":
                parseRVCode(json);
                break;
            case "REPORT":
                parseTransDataList(json);
                break;
            case "CONFIG":
                parseConfig(json);
                break;
        }
        return textBuilder.toString();
    }

    private void parseCommon(JSONObject json) {
        appendString(json, "appId", "App ID");
        appendString(json, "localCode", "Local Code");
        appendString(json, "localMsg", "Local Msg");
    }

    private void parseTransDataList(JSONObject json) {
        JSONArray datas = null;
        try {
            datas = json.getJSONArray("datas");
        } catch (JSONException ignore) {
        }

        if (datas == null || datas.length() == 0) {
            return;
        }

        for (int i = 0, l = datas.length(); i < l; i++) {
            try {
                JSONObject result = datas.getJSONObject(i);
                if (result != null) {
                    textBuilder.append("\n- - - - - Trans Data [" + (i + 1) + "] - - - - -\n");
                    parseTransData(result);
                }
            } catch (JSONException ignore) {
            }
        }
    }

    private void parseTransData(JSONObject json) {
        appendString(json, "transType", "Trans Type");
        appendString(json, "orderNumber", "Order Number");
        appendString(json, "rspCode", "Host Rsp Code");
        appendString(json, "rspMsg", "Host Rsp Msg");
        appendString(json, "merchName", "Merchant Name");
        appendString(json, "merchId", "Merchant ID");
        appendString(json, "termId", "Terminal ID");
        appendString(json, "amount", "Amount");
        appendString(json, "traceNo", "Trace No");
        appendString(json, "sysTraceAuditNo", "System Trace Audit No");
        appendString(json, "batchNo", "Batch No");
        appendString(json, "refNo", "Ref No");
        appendString(json, "authCode", "Auth Code");
        appendString(json, "time", "Time");
        appendString(json, "date", "Date");
        appendString(json, "year", "Year");
        appendString(json, "origSysTraceAuditNo", "Orig System Trace Audit No");
        appendString(json, "origBatchNo", "Orig Batch No");
        appendString(json, "origRefNo", "Orig Ref No");
        appendString(json, "origAuthCode", "Orig Auth Code");
        appendString(json, "origTime", "Orig Time");
        appendString(json, "origDate", "Orig Date");
        appendString(json, "origYear", "Orig Year");
    }

    private void parseRVCode(JSONObject json) {
        appendString(json, "rvCode", "RV Code");
    }

    private void parseConfig(JSONObject json) {
        appendString(json, "merchName", "Merchant Name");
        appendString(json, "merchId", "Merchant ID");
        appendString(json, "termId", "Terminal ID");
        appendString(json, "storeId", "Store ID");
        appendString(json, "tpdu", "TPDU");
        appendString(json, "nii", "NII");
        appendString(json, "ip", "IP");
        appendString(json, "port", "Port");
        appendBool(json, "backupEnabled", "Backup Enabled");
        appendString(json, "backupIp", "Backup IP");
        appendString(json, "backupPort", "Backup Port");
        appendString(json, "connectTimeout", "Connect Timeout");
        appendString(json, "receiveTimeout", "Receive Timeout");
        appendString(json, "reconnectTimes", "Reconnect Times");
        appendString(json, "traceNo", "Trace Number");
        appendString(json, "sysTraceAuditNo", "System Trace Audit Number");
    }

    private void appendString(JSONObject json, String key, String label) {
        try {
            String value = json.getString(key);
            textBuilder.append(label).append(": ").append(value).append("\n");
        } catch (JSONException ignore) {
        }
    }

    private void appendBool(JSONObject json, String key, String label) {
        try {
            boolean value = json.getBoolean(key);
            textBuilder.append(label).append(": ").append(value).append("\n");
        } catch (JSONException ignore) {
        }
    }
}
