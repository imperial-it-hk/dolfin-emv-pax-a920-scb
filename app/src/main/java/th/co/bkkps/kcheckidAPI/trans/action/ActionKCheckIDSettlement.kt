package th.co.bkkps.kcheckidAPI.trans.action

import android.content.Context
import android.content.Intent
import com.pax.abl.core.AAction
import com.pax.abl.core.ATransaction
import th.co.bkkps.kcheckidAPI.trans.action.activity.KCheckIDSettleActivity

class ActionKCheckIDSettlement(listener: AAction.ActionStartListener) : AAction(listener) {

    lateinit var context : Context
    fun setParam(context: Context) {
        this.context = context
    }
    override fun process() {
       val intent = Intent(context, KCheckIDSettleActivity::class.java)
       context.startActivity(intent)
    }
}