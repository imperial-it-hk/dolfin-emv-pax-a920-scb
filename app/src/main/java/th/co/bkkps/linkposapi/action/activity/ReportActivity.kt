package th.co.bkkps.linkposapi.action.activity

import android.os.Bundle
import com.pax.abl.core.ActionResult
import com.pax.edc.R
import com.pax.pay.BaseActivity
import com.pax.pay.app.FinancialApplication
import com.pax.pay.base.Acquirer
import com.pax.pay.constant.EUIParamKeys
import com.pax.pay.record.Printer
import com.pax.pay.trans.TransContext
import com.pax.pay.utils.Utils
import th.co.bkkps.linkposapi.service.message.HypercomMessage

class ReportActivity : BaseActivity() {
    private var title: String? = null
    private var transCode: String? = null
    private var acquirerName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        printReport()
    }

    private fun printReport() {
        FinancialApplication.getApp().runInBackground {
            val acquirer: Acquirer
            if (Utils.getString(R.string.acq_all_acquirer) != acquirerName) {
                acquirer = FinancialApplication.getAcqManager().findAcquirer(acquirerName)
            } else {
                acquirer = Acquirer()
                acquirer.apply {
                    this.nii = "999"
                    this.name = Utils.getString(R.string.acq_all_acquirer)
                }
            }

            var ret = 0
            when (transCode) {
                HypercomMessage.TRANS_CODE_SUMMARY_TYPE -> {
                    ret = Printer.printSummaryReport(this, acquirer, TransContext.getInstance().currentAction)
                }
                HypercomMessage.TRANS_CODE_AUDIT_TYPE -> {
                    ret = Printer.printDetailReport(this, acquirer, TransContext.getInstance().currentAction)
                }
            }
            finish(ActionResult(ret, null))
        }
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_null
    }

    override fun initViews() {
        //do nothing
    }

    override fun setListeners() {
        //do nothing
    }

    override fun loadParam() {
        title = intent.getStringExtra(EUIParamKeys.NAV_TITLE.toString())
        transCode = intent.getStringExtra("LINKPOS_TRANS_CODE")
        acquirerName = intent.getStringExtra("LINKPOS_ACQ_NAME")
    }

    override fun getTitleString(): String {
        return title!!
    }

    private fun finish(result: ActionResult) {
        val action = TransContext.getInstance().currentAction
        if (action != null) {
            if (action.isFinished) return
            action.isFinished = true
            action.setResult(result)
        }
        finish()
    }
}