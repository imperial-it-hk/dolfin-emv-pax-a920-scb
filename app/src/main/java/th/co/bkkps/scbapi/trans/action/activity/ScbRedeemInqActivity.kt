package th.co.bkkps.scbapi.trans.action.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.pax.abl.core.ActionResult
import com.pax.edc.opensdk.TransResult
import com.pax.pay.MainActivity
import com.pax.pay.app.ActivityStack
import com.pax.pay.trans.TransContext
import th.co.bkkps.bpsapi.ITransAPI
import th.co.bkkps.bpsapi.RedeemInqMsg
import th.co.bkkps.bpsapi.TransAPIFactory

class ScbRedeemInqActivity: AppCompatActivity() {
    private var transAPI: ITransAPI? = null
    private var redeemInqReq: RedeemInqMsg.Request? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        redeemInqReq = RedeemInqMsg.Request()
        transAPI = TransAPIFactory.createTransAPI()
        transAPI?.startTrans(this, redeemInqReq) ?: run {
            finish(ActionResult(TransResult.ERR_SCB_CONNECTION, null))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d("ScbRedeemInqActivity", "BpsApi onActivityResult invoked")
        val redeemResp = transAPI!!.onResult(requestCode, resultCode, data) as RedeemInqMsg.Response?
        redeemResp?.let {
            Log.d("BpsApi", "getRspCode=${it.rspCode}")
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