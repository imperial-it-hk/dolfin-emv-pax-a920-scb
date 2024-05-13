package th.co.bkkps.kcheckidAPI.trans.action.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.pax.abl.core.ActionResult
import com.pax.edc.kbank.bpskcheckidapi.*
import com.pax.pay.trans.TransContext
import th.co.bkkps.bpsapi.TransResult
import th.co.bkkps.kcheckidAPI.KCheckIDService
import th.co.bkkps.kcheckidAPI.trans.action.ActionKCheckIDPrintReport
import th.co.bkkps.utils.Log

class KCheckIDPrintReportActivity() :Activity() {

    var transAPI : ITransAPI? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        transAPI = TransAPIFactory.createTransAPI()

        if (!KCheckIDService.isKCheckIDInstalled(this)) {
            Log.d(KCheckIDVerifyIdCardActivity.TAG, "onCreate KCheckIDApp isn't found")
            finish(ActionResult(TransResult.ERR_ABORTED, null))
            return
        }

        val mode : Int = intent.getIntExtra("MODE" , 0)
        if (mode == ActionKCheckIDPrintReport.processMode.DETAIL_REPORT.intValue) {
            val reqDetailReportMsg = DetailReportPrintMsg.Companion.Request()
            transAPI?.startTrans(this, reqDetailReportMsg as BaseRequest)
        } else {
            val reqSummaryReportMsg = SummaryReportPrintMsg.Companion.Request()
            transAPI?.startTrans(this, reqSummaryReportMsg as BaseRequest)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val respDetailReport = transAPI?.onResult(requestCode, resultCode, data) as DetailReportPrintMsg.Companion.Response?
        respDetailReport?.let {
            finish(ActionResult(it.getApiResultCode(), null))
        }?: run {
            val respSummaryReport = transAPI?.onResult(requestCode, resultCode, data) as SummaryReportPrintMsg.Companion.Response?
            respSummaryReport?.let {
                finish(ActionResult(it.getApiResultCode(), null))
            } ?: run {
                finish(ActionResult(TransResult.ERR_PARAM, null))
            }
        }
    }

    fun finish(result: ActionResult?) {
        val action = TransContext.getInstance().currentAction
        if (action != null) {
            if (action.isFinished) return
            action.isFinished = true
            action.setResult(result)
        }
        finish()
    }
}