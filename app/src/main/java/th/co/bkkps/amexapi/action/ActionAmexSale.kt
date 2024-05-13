package th.co.bkkps.amexapi.action

import android.content.Context
import android.content.Intent
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.edc.opensdk.TransResult
import th.co.bkkps.amexapi.action.activity.AmexSaleActivity
import th.co.bkkps.bps_amexapi.EmvSP200

class ActionAmexSale(listener: ActionStartListener?) : AAction(listener) {
    private var context: Context? = null
    private var amount: String? = null
    private var tipAmount: String? = null
    private var enterMode: Int = 0
    private var track1: String? = null
    private var track2: String? = null
    private var track3: String? = null
    private var pan: String? = null
    private var expDate: String? = null
    private var emvSP200: EmvSP200? = null
    private var ermPrintVoucher: Long = -999

    fun setParam(context: Context?, amount: String?, tipAmount: String?, enterMode: Int,
                 track1: String?, track2: String?, track3: String?,
                 pan: String?, expDate: String?, emvSP200: EmvSP200?) {
        this.context = context
        this.amount = amount
        this.tipAmount = tipAmount
        this.enterMode = enterMode
        this.track1 = track1
        this.track2 = track2
        this.track3 = track3
        this.pan = pan
        this.expDate = expDate
        this.emvSP200 = emvSP200
        this.ermPrintVoucher = -999L
    }
    fun setParam(context: Context?, amount: String?, tipAmount: String?, enterMode: Int,
                 track1: String?, track2: String?, track3: String?,
                 pan: String?, expDate: String?, emvSP200: EmvSP200?, ermPrintVoucher: Long) {
        this.context = context
        this.amount = amount
        this.tipAmount = tipAmount
        this.enterMode = enterMode
        this.track1 = track1
        this.track2 = track2
        this.track3 = track3
        this.pan = pan
        this.expDate = expDate
        this.emvSP200 = emvSP200
        this.ermPrintVoucher = ermPrintVoucher
    }

    override fun process() {
        try {
            val intent = Intent(context!!, AmexSaleActivity::class.java)
            intent.putExtra("AMEX_API_AMOUNT", amount)
            intent.putExtra("AMEX_API_TIP_AMOUNT", tipAmount)
            intent.putExtra("AMEX_API_ENTER_MODE", enterMode)
            intent.putExtra("AMEX_API_TRACK1", track1)
            intent.putExtra("AMEX_API_TRACK2", track2)
            intent.putExtra("AMEX_API_TRACK3", track3)
            intent.putExtra("AMEX_API_PAN", pan)
            intent.putExtra("AMEX_API_EXP_DATE", expDate)
            intent.putExtra("AMEX_API_EMV_SP200", emvSP200)
            if (this.ermPrintVoucher!=-999L) {intent.putExtra("AMEX_PRINT_VOUCHER_NO", ermPrintVoucher)}
            context!!.startActivity(intent)
        }
        catch (ex: Exception) {
            setResult(ActionResult(TransResult.ERR_USER_CANCEL, null))
        }
    }

    override fun setResult(result: ActionResult?) {
        super.setResult(result)
    }
}