package th.co.bkkps.amexapi.action.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.pax.abl.core.ActionResult
import com.pax.edc.opensdk.TransResult
import com.pax.pay.trans.TransContext
import th.co.bkkps.amexapi.AmexAPIConstants
import th.co.bkkps.amexapi.AmexTransAPI
import th.co.bkkps.bps_amexapi.*
import th.co.bkkps.utils.Log

class AmexReportActivity: Activity() {
    enum class ReportType(val type: Int) {
        PRN_LAST_TXN(0),
        PRN_SUMMARY_REPORT(1),
        PRN_DETAIL_REPORT(2),
        PRN_LAST_BATCH(3),
        PRN_AUDIT_REPORT(4)
    }

    private var apiProcess = AmexTransAPI.getInstance().process
    private var state: Int = -1
    private var reportType: Int = -1

    private fun executeType() {
        if (AmexTransAPI.getInstance().process.isAppInstalled(this)) {
            apiProcess?.apply {
                when (reportType) {
                    ReportType.PRN_LAST_TXN.type -> {
                        state = AmexAPIConstants.REQUEST_REPRINT
                        val inputTransNo: Long = intent.getLongExtra("AMEX_API_TRACE_NO", 0)
                        val lastTransNo: Long = intent.getLongExtra("AMEX_API_LAST_TXN_NO", 0)
                        apiProcess.doReprint(this@AmexReportActivity, inputTransNo, lastTransNo, reportType)
                    }
                    ReportType.PRN_SUMMARY_REPORT.type,
                    ReportType.PRN_DETAIL_REPORT.type,
                    ReportType.PRN_LAST_BATCH.type,
                    ReportType.PRN_AUDIT_REPORT.type -> {
                        state = AmexAPIConstants.REQUEST_REPORT
                        apiProcess.doReport(this@AmexReportActivity, reportType)
                    }
                    else -> {
                        finish(ActionResult(TransResult.ERR_ABORTED, null))
                    }
                }
            } ?: run {
                finish(ActionResult(TransResult.ERR_AMEX_API_TRANS_EXCEPTION, null))
            }
        } else {
            Log.e(AmexAPIConstants.TAG, "Amex app not installed.")
            finish(ActionResult(TransResult.ERR_AMEX_APP_NOT_INSTALLED, null))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reportType = intent.getIntExtra("AMEX_API_REPORT_TYPE", 0)
        executeType()
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.i(AmexAPIConstants.TAG, "onActivityResult")
        super.onActivityResult(requestCode, resultCode, data)
        val response = apiProcess.transAPI.onResult(requestCode, resultCode, data)
        response?.let {
            Log.i(AmexAPIConstants.TAG, "REPRINT/REPORT response received")
            Log.d(AmexAPIConstants.TAG, "getRspCode=${it.rspCode}")
            finish(ActionResult(it.rspCode, it))
        } ?: run {
            Log.e(AmexAPIConstants.TAG, "REPRINT/REPORT response not received")
            finish(ActionResult(TransResult.ERR_ABORTED, null))
        }
    }

    fun finish(result: ActionResult?) {
        val action = TransContext.getInstance().currentAction
        action?.apply {
            if (this.isFinished) return
            this.isFinished = true
            this.setResult(result)
        }
        finish()
    }
}