package th.co.bkkps.scbapi.trans.action

import android.content.Context
import android.content.Intent
import com.pax.abl.core.AAction
import com.pax.pay.constant.EUIParamKeys
import th.co.bkkps.scbapi.trans.action.activity.ScbRedeemSaleActivity

class ActionScbRedeemSale(listener: ActionStartListener?) : AAction(listener) {
    private var mContext: Context? = null
    private var mode: Int = 0

    fun setParam(context: Context?, mode: Int) {
        this.mContext = context
        this.mode = mode
    }

    override fun process() {
        val intent = Intent(mContext, ScbRedeemSaleActivity::class.java)
        intent.putExtra(EUIParamKeys.SCB_REDEEM_MODE.toString(), mode)
        mContext!!.startActivity(intent)
    }
}