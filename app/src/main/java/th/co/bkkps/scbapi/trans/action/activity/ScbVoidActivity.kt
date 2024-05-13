package th.co.bkkps.scbapi.trans.action.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.pax.abl.core.ActionResult
import com.pax.edc.opensdk.TransResult
import com.pax.pay.MainActivity
import com.pax.pay.app.ActivityStack
import com.pax.pay.constant.EUIParamKeys
import com.pax.pay.trans.TransContext
import th.co.bkkps.bpsapi.ITransAPI
import th.co.bkkps.bpsapi.TransAPIFactory
import th.co.bkkps.bpsapi.VoidMsg

class ScbVoidActivity: AppCompatActivity() {
    private var transAPI: ITransAPI? = null
    private var voidReq: VoidMsg.Request? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val traceNo = intent.getLongExtra(EUIParamKeys.SCB_TRACE_NO.toString(), 0)
        voidReq = VoidMsg.Request()
        voidReq?.apply {
            this.voucherNo = traceNo.toInt()
        }
        transAPI = TransAPIFactory.createTransAPI()
        transAPI?.startTrans(this, voidReq) ?: run {
            finish(ActionResult(TransResult.ERR_SCB_CONNECTION, null))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d("ScbVoidActivity", "BpsApi onActivityResult invoked")
        val voidRsp = transAPI!!.onResult(requestCode, resultCode, data) as VoidMsg.Response?
        voidRsp?.let {
            Log.d("BpsApi", "getAuthCode=${it.authCode}")
            Log.d("BpsApi", "getRefNo=${it.refNo}")
            Log.d("BpsApi", "getStanNo=${it.stanNo}")
            Log.d("BpsApi", "getVoucherNo=${it.voucherNo}")
            finish(ActionResult(it.rspCode, it))
        } ?: run {
            Log.d("BpsApi", "response is null")
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
        ActivityStack.getInstance().popTo(MainActivity::class.java)
        finish()
    }
}