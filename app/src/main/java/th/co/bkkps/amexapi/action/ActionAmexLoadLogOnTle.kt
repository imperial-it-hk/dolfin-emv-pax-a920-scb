package th.co.bkkps.amexapi.action

import android.content.Context
import android.content.Intent
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.edc.opensdk.TransResult
import com.pax.pay.app.FinancialApplication
import com.pax.pay.constant.Bank
import th.co.bkkps.amexapi.AmexAPIConstants
import th.co.bkkps.amexapi.action.activity.AmexLoadLogOnTleActivity

class ActionAmexLoadLogOnTle(listener: ActionStartListener?) : AAction(listener) {
    private var context: Context? = null
    private var jsonTeId: String? = null

    fun setParam(context: Context, jsonTeId: String?) {
        this.context = context
        this.jsonTeId = jsonTeId
    }

    override fun process() {
        try {
            val intent = Intent(context!!, AmexLoadLogOnTleActivity::class.java)
            if (jsonTeId == null) {
                val param = FinancialApplication.getUserParam().getTEParam(Bank.AMEX)
                if (param != null) {
                    jsonTeId = "{\"TLE\" : [{\"BANK_NAME\": \"AMEX\",\"TE_ID\": \"" + param.id + "\",\"TE_PIN\": \"" + param.pin + "\"}]}"
                }
            }
            intent.putExtra("AMEX_JSON_TE_ID", jsonTeId)
            context!!.startActivity(intent)
        }
        catch (ex: Exception) {
            setResult(ActionResult(TransResult.ERR_ABORTED, AmexAPIConstants.REQUEST_TLE))
        }
    }
}