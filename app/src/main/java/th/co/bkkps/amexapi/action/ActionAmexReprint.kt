package th.co.bkkps.amexapi.action

import android.content.Context
import android.content.Intent
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.edc.opensdk.TransResult
import th.co.bkkps.amexapi.action.activity.AmexReprintActivity

class ActionAmexReprint(listener: ActionStartListener?) : AAction(listener) {
    private var context: Context? = null
    private var inputTransNo: Long = -1
    private var lastTransNo: Long = -1
    private var reprintType: Int = -1

    fun setParam(context: Context?, inputTransNo: Long, lastTransNo: Long, reprintType: Int) {
        this.context = context
        this.inputTransNo = inputTransNo
        this.lastTransNo = lastTransNo
        this.reprintType = reprintType
    }

    override fun process() {
        try {
            val intent = Intent(context!!, AmexReprintActivity::class.java)
            intent.putExtra("AMEX_API_TRACE_NO", inputTransNo)
            intent.putExtra("AMEX_API_LAST_TXN_NO", lastTransNo)
            intent.putExtra("AMEX_API_REPRINT_TYPE", reprintType)
            context!!.startActivity(intent)
        }
        catch (ex: Exception) {
            setResult(ActionResult(TransResult.ERR_USER_CANCEL, null))
        }
    }
}