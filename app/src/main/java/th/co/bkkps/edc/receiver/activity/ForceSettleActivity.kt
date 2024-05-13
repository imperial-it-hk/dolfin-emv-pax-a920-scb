package th.co.bkkps.edc.receiver.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.*
import android.widget.TextView
import com.pax.edc.R
import com.pax.edc.opensdk.TransResult
import com.pax.pay.BaseActivity
import com.pax.pay.MainActivity
import com.pax.pay.SettlementRegisterActivity
import com.pax.pay.app.FinancialApplication
import com.pax.pay.constant.Constants
import com.pax.pay.record.Printer
import com.pax.pay.trans.component.Component
import com.pax.pay.trans.model.Controller
import th.co.bkkps.amexapi.AmexTransService
import th.co.bkkps.edc.receiver.process.ForceSettleAlarmProcess
import th.co.bkkps.kcheckidAPI.KCheckIDService
import th.co.bkkps.kcheckidAPI.trans.action.ActionGetKCheckIDRecordCount
import th.co.bkkps.utils.Log

class ForceSettleActivity: BaseActivity() {
    lateinit var txvCaption: TextView
    lateinit var txvWaiting: TextView
    lateinit var wakeupList: ArrayList<String>
    var isFromDetected: Boolean = false
    var isCallKCheckIdSucc: Boolean = false
    private var acqKCheckId: String? = null
    private var kCheckIdCountRecord: Int = 0

    override fun getLayoutId(): Int {
        return R.layout.activity_auto_settlement
    }

    override fun initViews() {
        txvCaption = findViewById(R.id.txv_caption)
        txvCaption.text = titleString

        txvWaiting = findViewById(R.id.txv_waiting)

        val acqKCheckId = wakeupList.firstOrNull { acqName -> acqName == Constants.ACQ_KCHECKID }
        if (acqKCheckId != null && KCheckIDService.isKCheckIDInstalled(this@ForceSettleActivity)) {
            val actionGetKCheckIDRecordCount = ActionGetKCheckIDRecordCount {
                (it as ActionGetKCheckIDRecordCount).setParam(FinancialApplication.getApp().applicationContext)
            }
            actionGetKCheckIDRecordCount.setEndListener { _, result ->
                kCheckIdCountRecord = if (result.ret == TransResult.SUCC) {
                    result.data as Int
                } else {
                    0
                }
                isCallKCheckIdSucc = true
                Log.d(TAG, "actionGetKCheckIDRecordCount end listener count=$kCheckIdCountRecord")
            }
            actionGetKCheckIDRecordCount.execute()
        }

        FinancialApplication.getApp().runOnUiThreadDelay({
            val forceSettlementAsyncTask = ExecuteForceSettlementAsyncTask()
            forceSettlementAsyncTask.execute()
        }, 500)
    }

    override fun setListeners() {
        //do nothing
    }

    override fun loadParam() {
        wakeupList = intent.getStringArrayListExtra("WAKE_UP_LIST")
        isFromDetected = intent.getBooleanExtra("IS_FROM_DETECTED", false)
    }

    override fun getTitleString(): String {
        return "Force Settlement"
    }

    @SuppressLint("StaticFieldLeak")
    inner class ExecuteForceSettlementAsyncTask: AsyncTask<Void, String, Void>() {
        override fun doInBackground(vararg p0: Void?): Void? {
            publishProgress("Start setting force settlement")
            SystemClock.sleep(500)

            if (acqKCheckId != null && KCheckIDService.isKCheckIDInstalled(this@ForceSettleActivity)) {
                while (!isCallKCheckIdSucc) {
                    SystemClock.sleep(200)
                }
            }

            for (acqName in wakeupList) {
                Log.d(ForceSettleAlarmProcess.TAG, "\t\tSet Force Settlement [$acqName]")
                SettlementRegisterActivity.updateSettleTime(acqName)
                when (acqName) {
                    Constants.ACQ_AMEX -> {
                        val acqAmex = FinancialApplication.getAcqManager().findAcquirer(acqName)
                        val lastTxnAmex = FinancialApplication.getTransMultiAppDataDbHelper().findLastTransData(acqAmex)
                        val amexSettleStatus = AmexTransService.isAmexAppInstalled(this@ForceSettleActivity) && lastTxnAmex != null
                        setSettleStatusByHost(acqName, amexSettleStatus)
                    }
                    Constants.ACQ_KCHECKID -> {
                        val kCheckIDSettleStatus = KCheckIDService.isKCheckIDInstalled(this@ForceSettleActivity) && kCheckIdCountRecord > 0
                        setSettleStatusByHost(acqName, kCheckIDSettleStatus)
                    }
                    else -> {
                        val targetAcq = FinancialApplication.getAcqManager().findAcquirer(acqName)
                        val total = FinancialApplication.getTransTotalDbHelper().calcTotal(targetAcq)
                        setSettleStatusByHost(acqName, !total.isZero)
                    }
                }
                SystemClock.sleep(500)
            }

            publishProgress("Finish and go back to Idle screen")
            SystemClock.sleep(500)
            finish()

            if (isFromDetected) {
                val intent = Intent(this@ForceSettleActivity, MainActivity::class.java)
                this@ForceSettleActivity.startActivity(intent)
            }
            return null
        }

        override fun onPostExecute(result: Void?) {
            super.onPostExecute(result)
        }

        override fun onProgressUpdate(vararg values: String?) {
            txvWaiting.text = values[0]
        }

        private fun setSettleStatusByHost(acqName: String, status: Boolean) {
            if (status) {
                Log.d(ForceSettleAlarmProcess.TAG, "\t\tSet Force Settlement [$acqName] -- DONE")
                Component.setSettleStatus(Controller.Constant.SETTLE, acqName)
                publishProgress("Set Force Settle for [$acqName] - DONE")
            } else {
                Log.d(ForceSettleAlarmProcess.TAG, "\t\tSet Force Settlement [$acqName] -- SKIP")
                publishProgress("Set Force Settle for [$acqName] - SKIP")
            }
        }
    }
}