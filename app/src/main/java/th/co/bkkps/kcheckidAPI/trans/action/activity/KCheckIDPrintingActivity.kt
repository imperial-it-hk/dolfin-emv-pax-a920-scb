package th.co.bkkps.kcheckidAPI.trans.action.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.pax.abl.core.ActionResult
import com.pax.edc.kbank.bpskcheckidapi.*
import com.pax.pay.app.FinancialApplication
import com.pax.pay.constant.EUIParamKeys
import com.pax.pay.trans.TransContext
import com.pax.settings.SysParam
import th.co.bkkps.bpsapi.TransResult
import th.co.bkkps.kcheckidAPI.KCheckIDService
import th.co.bkkps.utils.Log
import java.text.SimpleDateFormat
import java.util.*

class KCheckIDPrintingActivity : Activity() {
    companion object {
        const val TAG ="KCheckIDPrinting"
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

        val sessionID : String? = intent?.getStringExtra(EUIParamKeys.KCHECKID_SESSION_ID.toString())
        sessionID?:run{
            Log.d(KCheckIDPrintingActivity.TAG, "onCreate missing sessionId")
            finish(ActionResult(TransResult.ERR_PARAM, null))
            return
        }

        val ercmResult : Int? = intent?.getIntExtra(EUIParamKeys.KCHECKID_ERCM_UPLOAD_RESULT.toString(), 0)
        val origAgentTransID : String? = intent?.getStringExtra(EUIParamKeys.KCHECKID_TRANS_AGENT_ID.toString())
        val deliverErmReq = DeliverErmUploadResultMsg.Companion.Request()
        deliverErmReq.setERMResult(ercmResult!!)
        deliverErmReq.setOriginalAgentTransID(origAgentTransID!!)
        transAPI.startTrans(this, deliverErmReq as BaseRequest)
        Log.d(KCheckIDSettingActivity.TAG, "onCreate --- startTransCalled")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
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