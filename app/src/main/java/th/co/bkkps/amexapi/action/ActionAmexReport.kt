package th.co.bkkps.amexapi.action

import android.content.Context
import android.content.Intent
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.edc.opensdk.TransResult
import th.co.bkkps.amexapi.action.activity.AmexReportActivity

class ActionAmexReport(listener: ActionStartListener?) : AAction(listener) {
    private var context: Context? = null
    private var reportType: AmexReportActivity.ReportType? = null

    fun setParam(context: Context?, reportType: AmexReportActivity.ReportType?) {
        this.context = context
        this.reportType = reportType
    }

    override fun process() {
        try {
            val intent = Intent(context!!, AmexReportActivity::class.java)
            intent.putExtra("AMEX_API_REPORT_TYPE", reportType!!.type)
            context!!.startActivity(intent)
        }
        catch (ex: Exception) {
            setResult(ActionResult(TransResult.ERR_USER_CANCEL, null))
        }
    }
}