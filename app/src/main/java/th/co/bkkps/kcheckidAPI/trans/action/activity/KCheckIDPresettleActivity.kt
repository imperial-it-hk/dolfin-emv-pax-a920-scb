package th.co.bkkps.kcheckidAPI.trans.action.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.pax.abl.core.ActionResult
import com.pax.edc.kbank.bpskcheckidapi.*
import com.pax.pay.trans.TransContext
import th.co.bkkps.kcheckidAPI.KCheckIDService
import th.co.bkkps.utils.Log

class KCheckIDPresettleActivity: Activity() {
    companion object{
        const val TAG = "KCheckIDPreSettle"
    }

    lateinit var transAPI : ITransAPI
    var isReceived : Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        transAPI = TransAPIFactory.createTransAPI()

        if (!KCheckIDService.isKCheckIDInstalled(this)) {
            Log.d(KCheckIDSettingActivity.TAG, "onCreate KCheckIDApp isn't found")
            finish(ActionResult(th.co.bkkps.bpsapi.TransResult.ERR_ABORTED, null))
            return
        }

        val reqMsg = GetRecordCountMsg.Companion.Request()
        reqMsg.setSessionId("123456789012345678901234567890123456")
        reqMsg.setRecordCount(-1)
        transAPI.startTrans(this, reqMsg as BaseRequest)
        Log.d(KCheckIDSettingActivity.TAG, "onCreate --- startTransCalled")
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        data?.let{
            if (it.hasExtra("command_type")
                && it.hasExtra("_resp_record_count")
                && it.getIntExtra("command_type",-1) == Constants.REQUEST_RECORD_COUNT) {

                val recCount = it.getIntExtra("_resp_record_count",0)
                finish(ActionResult(TransResult.SUCC, recCount))

            } else {
                finish(ActionResult(TransResult.ERR_PARAM, null))
            }
        }?:run{
            finish(ActionResult(TransResult.ERR_PARAM, null))
        }





//        val respMsg  = transAPI.onResult(requestCode, resultCode, data)
//        respMsg?.let {
//            if (it.getApiResultCode() == 0) {
//                finish(ActionResult(TransResult.SUCC, it.getRecordCount()))
//            } else {
//                finish(ActionResult(it.getApiResultCode(), -1))
//            }
//        }?:run{
//            finish(ActionResult(TransResult.ERR_PARAM, -1))
//        }
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