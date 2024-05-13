package th.co.bkkps.scbapi.trans.action.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.TransContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import th.co.bkkps.bpsapi.BaseRequest;
import th.co.bkkps.bpsapi.BaseResponse;
import th.co.bkkps.bpsapi.ITransAPI;
import th.co.bkkps.bpsapi.ReprintTotalMsg;
import th.co.bkkps.bpsapi.ReprintTransMsg;
import th.co.bkkps.bpsapi.TransAPIFactory;
import th.co.bkkps.bpsapi.TransResult;
import th.co.bkkps.scbapi.ScbIppService;

public class ScbIppNoDisplayActivity extends Activity {

    ITransAPI transAPI;
    ReprintTransMsg.Request reprintReq;
    ReprintTotalMsg.Request totalReq;
    BaseRequest baseRequest;
    BaseResponse baseResponse;

    private @ReportType int reportType;
    private String acquirerName;

    @IntDef({ReportType.PRN_LAST_TXN,
            ReportType.PRN_AUDIT_REPORT,
            ReportType.PRN_SUMMARY_REPORT,
            ReportType.PRN_DETAIL_REPORT,
            ReportType.PRN_LAST_BATCH,
            ReportType.CLEAR_REVERSAL,
            ReportType.CLEAR_TRADE_VOUCHER})

    @Retention(RetentionPolicy.SOURCE)
    public @interface ReportType {
        int PRN_LAST_TXN = 0;
        int PRN_SUMMARY_REPORT = 1;
        int PRN_DETAIL_REPORT = 2;
        int PRN_LAST_BATCH = 3;
        int PRN_AUDIT_REPORT = 4;
        int CLEAR_REVERSAL = 5;
        int CLEAR_TRADE_VOUCHER = 6;
    }

    private void executeType() {
        switch (reportType) {
            case ReportType.PRN_LAST_TXN:
                long traceNo = getIntent().getLongExtra(EUIParamKeys.SCB_TRACE_NO.toString(), 0);
                baseRequest = new ReprintTransMsg.Request();
                ((ReprintTransMsg.Request) baseRequest).setVoucherNo(traceNo);
                break;
            case ReportType.PRN_SUMMARY_REPORT:
            case ReportType.PRN_DETAIL_REPORT:
            case ReportType.PRN_LAST_BATCH:
            case ReportType.PRN_AUDIT_REPORT:
                baseRequest = new ReprintTotalMsg.Request();
                ((ReprintTotalMsg.Request) baseRequest).setReprintType(reportType);
                ((ReprintTotalMsg.Request) baseRequest).setAcquirerName(acquirerName);
                break;
        }
        transAPI.startTrans(this, baseRequest);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!ScbIppService.isSCBInstalled(this)) {
            finish(new ActionResult(TransResult.ERR_ABORTED, null));
            return;
        }
        reportType = getIntent().getIntExtra(EUIParamKeys.SCB_IPP_REPORT_TYPE.toString(), 0);
        acquirerName = getIntent().getStringExtra(EUIParamKeys.SCB_ACQ_NAME.toString());
        transAPI = TransAPIFactory.createTransAPI();
        executeType();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d("ScbIppNoDisplayActivity", "BpsApi onActivityResult invoked");
        baseResponse = transAPI.onResult(requestCode, resultCode, data);
        if (baseResponse != null) {
            Log.d("BpsApi", "getRspCode="+baseResponse.getRspCode());
            finish(new ActionResult(baseResponse.getRspCode(), baseResponse));
            return;
        }
        Log.d("BpsApi", "response is null");
        finish(new ActionResult(TransResult.ERR_ABORTED, null));
    }

    public void finish(ActionResult result) {
        AAction action = TransContext.getInstance().getCurrentAction();
        if (action != null) {
            if (action.isFinished())
                return;
            action.setFinished(true);
            action.setResult(result);
        }
//        ActivityStack.getInstance().popTo(MainActivity.class);
        finish();
    }
}
