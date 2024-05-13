package th.co.bkkps.amexapi.action.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pax.abl.core.ActionResult
import com.pax.edc.opensdk.TransResult
import com.pax.pay.app.FinancialApplication
import com.pax.pay.constant.Constants
import com.pax.pay.trans.TransContext
import th.co.bkkps.amexapi.AmexAPIConstants
import th.co.bkkps.amexapi.AmexTransAPI
import th.co.bkkps.amexapi.AmexTransService
import th.co.bkkps.bps_amexapi.LoadTleMsg
import th.co.bkkps.utils.Log

class AmexLoadLogOnTleActivity: AppCompatActivity() {
    private var apiProcess = AmexTransAPI.getInstance().process
    private var state: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            state = AmexAPIConstants.REQUEST_PARAM_SETTING
            val intent = Intent(this@AmexLoadLogOnTleActivity, AmexParamsActivity::class.java)
            startActivityForResult(intent, state)
        }
        catch (ex: Exception) {
            finish(ActionResult(TransResult.ERR_AMEX_API_TRANS_EXCEPTION, null))
        }
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.i(AmexAPIConstants.TAG, "onActivityResult")
        super.onActivityResult(requestCode, resultCode, data)

        when (state) {
            AmexAPIConstants.REQUEST_PARAM_SETTING -> {
                if (resultCode != TransResult.SUCC) {
                    finish(ActionResult(TransResult.ERR_PARAM, null))
                } else {
                    try {
                        state = AmexAPIConstants.REQUEST_TLE
                        apiProcess?.apply {
                            val jsonTeId: String? = intent.getStringExtra("AMEX_JSON_TE_ID")
                            this.doLoadLogOnTle(this@AmexLoadLogOnTleActivity, jsonTeId!!)
                        } ?: run {
                            finish(ActionResult(TransResult.ERR_AMEX_API_TRANS_EXCEPTION, null))
                        }
                    }
                    catch (ex: Exception) {
                        finish(ActionResult(TransResult.ERR_AMEX_API_TRANS_EXCEPTION, null))
                    }
                }
            }
            AmexAPIConstants.REQUEST_TLE -> {
                val response: LoadTleMsg.Response? = apiProcess.transAPI.onResult(requestCode, resultCode, data) as LoadTleMsg.Response?
                response?.let {
                    Log.i(AmexAPIConstants.TAG, "LOAD_TLE response received")
                    Log.d(AmexAPIConstants.TAG, "getRspCode=${it.rspCode}")
                    Log.d(AmexAPIConstants.TAG, "getStanNo=${it.stanNo}")
                    Log.d(AmexAPIConstants.TAG, "getVoucherNo=${it.voucherNo}")
                    AmexTransService.updateEdcTraceStan(response)
                    if (it.rspCode == TransResult.SUCC) {
                        val acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_AMEX)
                        acquirer?.apply {
                            this.tmk = "Y"; this.twk = "Y"
                            FinancialApplication.getAcqManager().updateAcquirer(this)
                        }
                    }
                    finish(ActionResult(it.rspCode, null))
                } ?: run {
                    Log.e(AmexAPIConstants.TAG, "LOAD_TLE response not received")
                    finish(ActionResult(TransResult.ERR_ABORTED, null))
                }
            }
            else -> {
                finish(ActionResult(TransResult.ERR_ABORTED, null))
            }
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