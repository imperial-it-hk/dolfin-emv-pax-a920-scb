package th.co.bkkps.amexapi.action

import android.content.Context
import android.content.Intent
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.edc.opensdk.TransResult
import th.co.bkkps.amexapi.action.activity.AmexSettleActivity

class ActionAmexSettle(listener: ActionStartListener?) : AAction(listener) {
    private var context: Context? = null

    fun setParam(context: Context?) {
        this.context = context
    }

    override fun process() {
        try {
            val intent = Intent(context!!, AmexSettleActivity::class.java)
            context!!.startActivity(intent)
        }
        catch (ex: Exception) {
            setResult(ActionResult(TransResult.ERR_ABORTED, null))
        }
    }
}