package th.co.bkkps.kcheckidAPI.trans.action

import android.app.Activity
import com.pax.abl.core.ActionResult
import com.pax.pay.trans.TransContext

abstract class bpsKCheckIDActivity() : Activity() {
    open fun finish(result: ActionResult?) {
        val action = TransContext.getInstance().currentAction
        if (action != null) {
            if (action.isFinished) return
            action.isFinished = true
            action.setResult(result)
        }
        finish()
    }
}