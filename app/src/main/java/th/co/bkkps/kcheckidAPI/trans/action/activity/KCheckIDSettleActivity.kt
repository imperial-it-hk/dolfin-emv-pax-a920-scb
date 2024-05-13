package th.co.bkkps.kcheckidAPI.trans.action.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.pax.abl.core.ActionResult
import com.pax.edc.kbank.bpskcheckidapi.*
import com.pax.pay.utils.Utils
import th.co.bkkps.kcheckidAPI.trans.action.bpsKCheckIDActivity
import java.text.SimpleDateFormat
import java.util.*

class KCheckIDSettleActivity : bpsKCheckIDActivity() {

    lateinit var transAPI : ITransAPI
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        transAPI = TransAPIFactory.createTransAPI()
        val reqMsg = SettlementMsg.Companion.Request()
        reqMsg.setDateTime(getCurrentDateTime())
        reqMsg.setSessionId("123456789012345678901234567890123456")
        transAPI.startTrans(this, reqMsg)
    }

    fun getCurrentDateTime(): String{
        return SimpleDateFormat("yyyyMMdd HH:mm:ss").format(Date())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        var resp : SettlementMsg.Companion.Response? = null
        try {
            resp = transAPI.onResult(requestCode, resultCode, data) as SettlementMsg.Companion.Response?
        } catch (e: Exception) {
            e.printStackTrace()
        }


        resp?.let {
            finish(ActionResult(it.getApiResultCode(), it.getBatchNumber()))
        } ?: run {
            finish(ActionResult(TransResult.ERR_PARAM, null))
        }
    }
}