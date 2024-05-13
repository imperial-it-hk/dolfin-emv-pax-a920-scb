package th.co.bkkps.amexapi.action.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pax.abl.core.ActionResult
import com.pax.edc.opensdk.TransResult
import com.pax.pay.MainActivity
import com.pax.pay.app.ActivityStack
import com.pax.pay.trans.TransContext
import th.co.bkkps.amexapi.AmexAPIConstants
import th.co.bkkps.amexapi.AmexTransAPI
import th.co.bkkps.amexapi.AmexTransService
import th.co.bkkps.utils.Log

class AmexHistoryActivity: AppCompatActivity() {
    private var apiProcess = AmexTransAPI.getInstance().process

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (AmexTransService.isAmexAppInstalled(this)) {
            apiProcess.doHistory(this)
        } else {
            finish(ActionResult(TransResult.ERR_AMEX_APP_NOT_INSTALLED, null))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.i(AmexAPIConstants.TAG, "History response received")
        finish(ActionResult(TransResult.SUCC, null))
        ActivityStack.getInstance().popTo(MainActivity::class.java)
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