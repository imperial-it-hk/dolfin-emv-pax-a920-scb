package th.co.bkkps.linkposapi.action

import android.content.Context
import android.content.Intent
import com.pax.abl.core.AAction
import com.pax.pay.constant.EUIParamKeys
import th.co.bkkps.linkposapi.action.activity.ReportActivity

class ActionLinkPosReport(listener: ActionStartListener?) : AAction(listener) {
    private var context: Context? = null
    private var title: String? = null
    private var transCode: String? = null
    private var acquirerName: String? = null

    fun setParam(context: Context, title: String, transCode: String, acquirerName: String) {
        this.context = context
        this.title = title
        this.transCode = transCode
        this.acquirerName = acquirerName
    }

    override fun process() {
        val intent = Intent(context, ReportActivity::class.java)
        intent.putExtra(EUIParamKeys.NAV_TITLE.toString(), title)
        intent.putExtra("LINKPOS_TRANS_CODE", transCode)
        intent.putExtra("LINKPOS_ACQ_NAME", acquirerName)
        context!!.startActivity(intent)
    }
}