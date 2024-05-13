package th.co.bkkps.kcheckidAPI.trans.action.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.pax.abl.core.ActionResult
import com.pax.device.Device
import com.pax.edc.kbank.bpskcheckidapi.ITransAPI
import com.pax.edc.kbank.bpskcheckidapi.TransAPIFactory
import com.pax.pay.trans.TransContext
import th.co.bkkps.bpsapi.TransResult
import th.co.bkkps.kcheckidAPI.KCheckIDService
import th.co.bkkps.utils.Log

class KCheckIDDisplayDetailReportActivity  : Activity() {

    companion object {
        const val TAG = "DispSummaryReport"
    }

    lateinit var transAPI : ITransAPI
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        transAPI = TransAPIFactory.createTransAPI()

        if (!KCheckIDService.isKCheckIDInstalled(this)) {
            Log.d(KCheckIDPrintingActivity.TAG, "onCreate KCheckIDApp isn't found")
            finish(ActionResult(TransResult.ERR_ABORTED, null))
            return
        }

        val reqMsg = com.pax.edc.kbank.bpskcheckidapi.DisplayDetailReportUIMsg.Companion.Request()
        reqMsg.setSessionID("123456789012345678901234567890123456")
        transAPI.startTrans(this, reqMsg)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Device.beepOk()
        finish(ActionResult(TransResult.SUCC, null))
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