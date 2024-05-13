package th.co.bkkps.amexapi.action

import android.content.Context
import android.content.Intent
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.edc.opensdk.TransResult
import com.pax.pay.MainActivity
import com.pax.pay.app.ActivityStack
import com.pax.pay.utils.Utils
import th.co.bkkps.amexapi.action.activity.AmexVoidActivity

class ActionAmexVoid(listener: ActionStartListener?) : AAction(listener) {
    private var context: Context? = null
    private var origTransNo: Long = -1
    private var lastTransNo: Long = -1
    private var ermPrintVoucher: Long = -999L

    fun setParam(context: Context?, origTransNo: Long, lastTransNo: Long) {
        this.context = context
        this.origTransNo = origTransNo
        this.lastTransNo = lastTransNo
    }
    fun setParam(context: Context?, origTransNo: Long, lastTransNo: Long, ermPrintVoucher: Long) {
        this.context = context
        this.origTransNo = origTransNo
        this.lastTransNo = lastTransNo
        this.ermPrintVoucher = ermPrintVoucher
    }

    override fun process() {
        try {
            val intent = Intent(context!!, AmexVoidActivity::class.java)
            intent.putExtra("AMEX_API_TRANS_NO", origTransNo)
            intent.putExtra("AMEX_API_LAST_TXN_NO", lastTransNo)
            if (this.ermPrintVoucher!=-999L) {intent.putExtra("AMEX_PRINT_VOUCHER_NO", ermPrintVoucher)}
            context!!.startActivity(intent)
        }
        catch (ex: Exception) {
            setResult(ActionResult(TransResult.ERR_USER_CANCEL, null))
        }
    }

    override fun setResult(result: ActionResult?) {
        super.setResult(result)
        ActivityStack.getInstance().popTo(MainActivity::class.java)
    }
}