package th.co.bkkps.amexapi.action.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.pax.abl.core.ActionResult
import com.pax.edc.opensdk.TransResult
import com.pax.pay.trans.TransContext
import th.co.bkkps.amexapi.AmexAPIConstants
import th.co.bkkps.amexapi.AmexTransAPI
import th.co.bkkps.bps_amexapi.ReprintTransMsg
import th.co.bkkps.utils.Log

class AmexReprintActivity: Activity() {
    private var apiProcess = AmexTransAPI.getInstance().process
    private var reportType: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reportType = intent.getIntExtra("AMEX_API_REPORT_TYPE", 0)
        try {
            apiProcess?.apply {
                val inputTransNo: Long = intent.getLongExtra("AMEX_API_TRACE_NO", 0)
                val lastTransNo: Long = intent.getLongExtra("AMEX_API_LAST_TXN_NO", 0)
                val reprintType: Int = intent.getIntExtra("AMEX_API_REPRINT_TYPE", -1)
                this.doReprint(this@AmexReprintActivity, inputTransNo, lastTransNo, reprintType)
            } ?: run {
                finish(ActionResult(TransResult.ERR_AMEX_API_TRANS_EXCEPTION, null))
            }
        }
        catch (ex: Exception) {
            finish(ActionResult(TransResult.ERR_AMEX_API_TRANS_EXCEPTION, null))
        }
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.i(AmexAPIConstants.TAG, "onActivityResult")
        super.onActivityResult(requestCode, resultCode, data)
        val response: ReprintTransMsg.Response? = apiProcess.transAPI.onResult(requestCode, resultCode, data) as ReprintTransMsg.Response?
        response?.let {
            Log.i(AmexAPIConstants.TAG, "REPRINT response received")
            Log.d(AmexAPIConstants.TAG, "getStanNo=${it.stanNo}")
            Log.d(AmexAPIConstants.TAG, "getVoucherNo=${it.voucherNo}")
            finish(ActionResult(TransResult.SUCC, it))
        } ?: run {
            Log.e(AmexAPIConstants.TAG, "REPRINT response not received")
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
