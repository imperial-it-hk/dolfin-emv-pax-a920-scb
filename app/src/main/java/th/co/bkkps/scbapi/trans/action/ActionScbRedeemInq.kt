package th.co.bkkps.scbapi.trans.action

import android.content.Context
import android.content.Intent
import com.pax.abl.core.AAction
import th.co.bkkps.scbapi.trans.action.activity.ScbRedeemInqActivity

class ActionScbRedeemInq(listener: ActionStartListener?) : AAction(listener) {
    private var mContext: Context? = null

    fun setParam(context: Context?) {
        this.mContext = context
    }

    override fun process() {
        val intent = Intent(mContext, ScbRedeemInqActivity::class.java)
        mContext!!.startActivity(intent)
    }
}