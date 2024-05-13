package com.pax.pay.trans.action.activity

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.AsyncTask
import android.os.Bundle
import android.os.SystemClock
import com.pax.abl.core.ActionResult
import com.pax.device.Device
import com.pax.edc.R
import com.pax.edc.opensdk.TransResult
import com.pax.pay.BaseActivity
import com.pax.pay.app.FinancialApplication
import com.pax.pay.base.Acquirer
import com.pax.pay.base.MerchantProfile
import com.pax.pay.constant.Constants
import com.pax.pay.record.Printer
import com.pax.pay.trans.TransContext
import com.pax.pay.trans.model.MerchantProfileManager
import com.pax.pay.utils.TransResultUtils
import com.pax.pay.utils.Utils
import com.pax.view.dialog.DialogUtils

class PrintReportActivity : BaseActivity() {
    private var title: String? = null
    private var reportType: String = ""

    companion object {
        const val REPORT_TYPE = "REPORT_TYPE"
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_null
    }

    override fun getTitleString(): String {
        return title!!
    }

    override fun initViews() {

    }

    override fun setListeners() {

    }
    override fun loadParam() {
        reportType = intent.getStringExtra(REPORT_TYPE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(reportType == getString(R.string.history_menu_print_trans_detail) ){
            PrintDetailTask(this).execute()
        } else if (reportType == getString(R.string.history_menu_print_trans_total)) {
            PrintSummaryTask(this).execute()
        } else if (reportType == getString(R.string.history_menu_print_last_total)) {
            PrintLastSettleTask(this).execute()
        } else {
            finish(ActionResult(TransResult.SUCC, null), true, false)
        }

    }

    fun finish(result: ActionResult?, needFinish: Boolean, ignoreActionFinished: Boolean) {
        val action = TransContext.getInstance().currentAction
        if (action != null) {
            if (action.isFinished) {
                if (ignoreActionFinished) { finish()}
                return
            }
            action.isFinished = true
            quickClickProtection.start() // AET-93
            action.setResult(result)
            if (needFinish) {
                finish()
            }
        } else {
            finish()
        }
    }



    class PrintLastSettleTask(context: PrintReportActivity) : AsyncTask<Unit, Unit, String>() {

        val innerContext = context
        var printLastSettleReportResult : Int = -999

        @SuppressLint("StaticFieldLeak")
        override fun doInBackground(vararg params: Unit?): String? {
            val acquirer = Acquirer()
            acquirer.apply {
                this.nii = "999"
                this.name = Utils.getString(R.string.acq_all_acquirer)
            }


            var currMerchantProfile = MerchantProfileManager.getCurrentMerchant()
            var allMercProfile :List<MerchantProfile>? = MerchantProfileManager.getAllMerchant()
            val allMercProfAsc = MerchantProfileManager.getAllMerchant(true)
            allMercProfile?.let {
                allMercProfAsc?.let {
                    for (mercProf : MerchantProfile in it) {
                        for (merProfile: MerchantProfile in it) {
                            if (mercProf.merchantLabelName.equals(merProfile.merchantLabelName))
                            MerchantProfileManager.applyProfileAndSave(merProfile.merchantLabelName)
                            MerchantProfileManager.restoreCurrentMerchant()

                            Printer.printLastBatch(innerContext,acquirer,TransContext.getInstance().currentAction)

                            SystemClock.sleep(100)
                        }
                    }
                }


                Device.beepOk()
            }
            MerchantProfileManager.applyProfileAndSave(currMerchantProfile)
            printLastSettleReportResult = 0
            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            var listener = object : DialogInterface.OnDismissListener {
                override fun onDismiss(dialog: DialogInterface?) {
                    innerContext.finish(ActionResult(printLastSettleReportResult, null), true, true)
                }
            }
            try {
                if (printLastSettleReportResult!=TransResult.SUCC) {
                    DialogUtils.showErrMessage(innerContext, "", TransResultUtils.getMessage(printLastSettleReportResult), listener, Constants.FAILED_DIALOG_SHOW_TIME)
                } else {
                    listener?.onDismiss(null)
                }
            } catch (ex: Exception) {
                listener?.onDismiss(null)
            }
        }
    }

    class PrintSummaryTask(context: PrintReportActivity) : AsyncTask<Unit, Unit, String>() {

        val innerContext = context
        var summaryReportResult : Int = -999

        @SuppressLint("StaticFieldLeak")
        override fun doInBackground(vararg params: Unit?): String? {
            val acquirer = Acquirer()
            acquirer.apply {
                this.nii = "999"
                this.name = Utils.getString(R.string.acq_all_acquirer)
            }

            val merchantDistinctList  = FinancialApplication.getTransDataDbHelper().findAllTransData(false).groupBy { it.merchantName }
            val allMercProfAsc = MerchantProfileManager.getAllMerchant(true);
            if (merchantDistinctList.isEmpty()) {
                summaryReportResult = TransResult.ERR_NO_TRANS
            } else {
                var currMerchantProfile = MerchantProfileManager.getCurrentMerchant()

                allMercProfAsc?.let {
                    for (mercProf : MerchantProfile in it) {
                        for (mercName: String in merchantDistinctList.keys) {
                            if (mercProf.merchantLabelName.equals(mercName)) {
                                MerchantProfileManager.applyProfileAndSave(mercName)
                                MerchantProfileManager.restoreCurrentMerchant()

                                Printer.printSummaryReport(innerContext, acquirer, null)
                                SystemClock.sleep(100)
                            }
                        }
                    }
                }

                Device.beepOk()
                MerchantProfileManager.applyProfileAndSave(currMerchantProfile)
                summaryReportResult = TransResult.SUCC
            }
            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            var listener = object : DialogInterface.OnDismissListener {
                override fun onDismiss(dialog: DialogInterface?) {
                    innerContext.finish(ActionResult(summaryReportResult, null), true, true)
                }
            }
            try {
                if (summaryReportResult!=TransResult.SUCC) {
                    DialogUtils.showErrMessage(innerContext, "", TransResultUtils.getMessage(summaryReportResult), listener, Constants.FAILED_DIALOG_SHOW_TIME)
                } else {
                    listener?.onDismiss(null)
                }
            } catch (ex: Exception) {
                listener?.onDismiss(null)
            }
        }
    }

    class PrintDetailTask(context: PrintReportActivity) : AsyncTask<Unit, Unit, String>() {

        val innerContext = context
        var detailReportResult : Int = -999

        @SuppressLint("StaticFieldLeak")
        override fun doInBackground(vararg params: Unit?): String? {
            val acquirer = Acquirer()
            acquirer.apply {
                this.nii = "999"
                this.name = Utils.getString(R.string.acq_all_acquirer)
            }

            val merchantDistinctList  = FinancialApplication.getTransDataDbHelper().findAllTransData(false).groupBy { it.merchantName }
            val allMercProfAsc = MerchantProfileManager.getAllMerchant(true)
            if (merchantDistinctList.isEmpty()) {
                detailReportResult = TransResult.ERR_NO_TRANS
            } else {
                var currMerchantProfile = MerchantProfileManager.getCurrentMerchant()

                allMercProfAsc?.let {
                    for (mercProf : MerchantProfile in it) {
                        for (mercName: String in merchantDistinctList.keys) {
                            if (mercProf.merchantLabelName.equals(mercName)) {
                                MerchantProfileManager.applyProfileAndSave(mercName)
                                MerchantProfileManager.restoreCurrentMerchant()

                                Printer.printDetailReport(innerContext, acquirer, null )
                                SystemClock.sleep(100)
                            }
                        }
                    }
                }

                Device.beepOk()
                MerchantProfileManager.applyProfileAndSave(currMerchantProfile)
                detailReportResult = TransResult.SUCC
            }
            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            var listener = object : DialogInterface.OnDismissListener {
                override fun onDismiss(dialog: DialogInterface?) {
                    innerContext.finish(ActionResult(detailReportResult, null), true, true)
                }
            }
            try {
                if (detailReportResult!=TransResult.SUCC) {
                    DialogUtils.showErrMessage(innerContext, "", TransResultUtils.getMessage(detailReportResult), listener, Constants.FAILED_DIALOG_SHOW_TIME)
                } else {
                    listener?.onDismiss(null)
                }
            } catch (ex: Exception) {
                listener?.onDismiss(null)
            }
        }
    }

}