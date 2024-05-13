package com.pax.pay

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.SystemClock
import android.widget.TextView
import com.pax.abl.core.ATransaction
import com.pax.device.Device
import com.pax.edc.R
import com.pax.pay.app.FinancialApplication
import com.pax.pay.base.Acquirer
import com.pax.pay.constant.Constants
import com.pax.pay.utils.TickTimer
import com.pax.settings.SysParam
import th.co.bkkps.edc.receiver.AutoSettleAlarmReceiver
import th.co.bkkps.edc.receiver.ForceSettleAlarmReceiver
import th.co.bkkps.edc.receiver.process.AlarmProcess
import th.co.bkkps.edc.receiver.process.AutoSettleAlarmProcess
import th.co.bkkps.edc.receiver.process.ForceSettleAlarmProcess
import th.co.bkkps.edc.receiver.process.SettleAlarmProcess
import th.co.bkkps.edc.receiver.process.SettleAlarmProcess.SettlementMode
import th.co.bkkps.utils.Log
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.KClass

class SettlementRegisterActivity: BaseActivity() {

    val TAG = "ALARMMGR"
    lateinit var txvWaiting : TextView
    lateinit var txvCaption : TextView
    lateinit var lateStartTimer: TickTimer
    lateinit var timerEndListener: TickTimer.OnTickTimerListener
    var transEndListener: ATransaction.TransEndListener? = null
    private var title: String = ""
    private var settleMode: SettlementMode? = null

    companion object {
        class SettlementInfo {
            lateinit var acqName : ArrayList<String>
            lateinit var settletime : String
            var multiAcquirer : Boolean = false
            var latestSettleTime : ArrayList<String>? = null
            var diffTimeOver24hours : ArrayList<Boolean>? = null
            var realAlarmTimeUTC: Long = 0L
        }

        var param_name : String = "ACQ_NAME"
        var param_item_index : String = "ACQ_ITEM_INDEX"

        fun updateSettleTime(acqName: String) {
            val acquirer = FinancialApplication.getAcqManager().findAcquirer(acqName)
            if (isEnableSettleMode() && acquirer.isEnable && acquirer.settleTime !=null) {
                val lastSettleDateTime = Device.getTime(SettleAlarmProcess.STR_FORMAT_DATETIME_LAST_SETTLE)
                Log.d(SettleAlarmProcess.TAG, " AFTER SETTLE PROCESS UPDATE LATEST_SETTLE_TIME : $lastSettleDateTime")
                acquirer.latestSettledDateTime = lastSettleDateTime
                FinancialApplication.getAcqManager().updateAcquirer(acquirer)
            }
        }

        fun isEnableSettleMode(): Boolean {
            return getEDCSettlementMode() != SettlementMode.DISABLE.value
        }

        fun getEDCSettlementMode(): String {
            return FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_SETTLEMENT_MODE, SettlementMode.DISABLE.value)
        }

        fun roundingMillis(millis : Long) : Long {
            return millis - (millis % 100000)
        }
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_auto_settlement
    }

    override fun initViews() {
        txvWaiting = findViewById(R.id.txv_waiting)
        txvCaption = findViewById(R.id.txv_caption)
        txvCaption.text = title
    }

    override fun setListeners() {
        timerEndListener = object: TickTimer.OnTickTimerListener {
            override fun onTick(leftTime: Long) {
                updateStatus("$title register & checking in  (${leftTime } Sec.)")
            }

            override fun onFinish() {
                updateStatus("Please wait...")
                val registerSettleByAcquirer = RegisterSettleByAcquirerAsyncTask()
                registerSettleByAcquirer.execute()    // start register process
            }

        }
    }

    override fun loadParam() {
        settleMode = SettlementMode.getMode(getEDCSettlementMode())

        when (settleMode) {
            SettlementMode.AUTO_SETTLE -> {
                title = "Auto Settlement"
            }
            SettlementMode.FORCE_SETTLE -> {
                title = "Force Settlement"
            }
            else -> {}
        }

        super.setTitle("register $title by host")
    }

    override fun getTitleString(): String {
        return title
    }

    override fun onStart() {
        super.onStart()

        lateStartTimer = TickTimer(timerEndListener)
        lateStartTimer.start(4)
    }

    private fun updateStatus(msg : String?) {
        runOnUiThread {
            msg.let {
                if (it!!.isNotEmpty()) {
                    txvWaiting.text = it
                }
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    inner class RegisterSettleByAcquirerAsyncTask: AsyncTask<Void, String, Void>() {
        override fun doInBackground(vararg p0: Void?): Void? {
            try {
                val acqSettleList : ArrayList<SettlementInfo> = foundHostToRegisterSettle()
                if (acqSettleList.isNotEmpty()) {

                    transEndListener = ATransaction.TransEndListener {
                        openMainActivity()
                    }

                    var enableReceiverKClazz: KClass<*>? = null
                    var disableReceiverKClazz: KClass<*>? = null
                    val alarmProcess: AlarmProcess? = when (settleMode) {
                        SettlementMode.AUTO_SETTLE -> {
                            enableReceiverKClazz = AutoSettleAlarmReceiver::class
                            disableReceiverKClazz = ForceSettleAlarmReceiver::class
                            AutoSettleAlarmProcess(this@SettlementRegisterActivity, acqSettleList, transEndListener!!)
                        }
                        SettlementMode.FORCE_SETTLE -> {
                            enableReceiverKClazz = ForceSettleAlarmReceiver::class
                            disableReceiverKClazz = AutoSettleAlarmReceiver::class
                            ForceSettleAlarmProcess(this@SettlementRegisterActivity, acqSettleList, transEndListener!!)
                        }
                        else -> null
                    }

                    alarmProcess?.apply {
                        // 1. set enable broadcast receiver on
                        Log.d("ALARMMGMT Register - Enable AlarmReceiver")
                        publishProgress("Enable AlarmReceiver")
                        SystemClock.sleep(500)
                        this.enableBroadcastReceiver(true, enableReceiverKClazz!!.java)//Enable receiver as per configuration
                        this.enableBroadcastReceiver(false, disableReceiverKClazz!!.java)//Disable another receiver

                        // 2. run register exact-real-time-wakeup for each target host
                        Log.d("ALARMMGMT Register - Real-time-wakeup registering...")
                        publishProgress("Real-time-wakeup registering...")
                        SystemClock.sleep(500)
                        this.registerAlarmManager()

                        // 3. run auto-force-settle first
                        Log.d("ALARMMGMT Register - Detect pending settle")
                        publishProgress("Detect pending settle")
                        SystemClock.sleep(500)
                        this.detectAndRunPendingItems()
                    }

                } else {
                    publishProgress("No acquirer to register AlarmReceiver")
                    SystemClock.sleep(1000)
                    transEndListener?:run{
                        openMainActivity()
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                transEndListener?:run{
                    openMainActivity()
                }
            }

            return null
        }

        override fun onPostExecute(result: Void?) {
            super.onPostExecute(result)
        }

        override fun onProgressUpdate(vararg values: String?) {
            txvWaiting.text = values[0]
        }
    }

    private fun openMainActivity() {
        updateStatus("Done, returning to IdleScreen please wait...")
        SystemClock.sleep(2000)
        intent = Intent(this, MainActivity::class.java)
        this.startActivity(intent)
    }

    private fun foundHostToRegisterSettle () : ArrayList<SettlementInfo> {
        val returnArrayList = ArrayList<SettlementInfo>()
        try {
            if (isEnableSettleMode()) {
                val excludeAcqs: List<String> = listOf(Constants.ACQ_ERCM_KEY_MANAGEMENT_SERVICE, Constants.ACQ_ERCM_RECEIPT_MANAGEMENT_SERVICE)
                val allAcquirers : List<Acquirer>? = FinancialApplication.getAcqManager().findEnableAcquirersExcept(excludeAcqs)
                allAcquirers?.let { allAcqs ->
                    val acqActiveSettle : List<Acquirer> = allAcqs.filter{ it.isEnable && it.settleTime != null }
                    val acqActiveDistinctList = acqActiveSettle.groupBy {it.settleTime }
                    if (acqActiveSettle.isNotEmpty() && acqActiveDistinctList.isNotEmpty()) {
                        returnArrayList.clear()
                        if (acqActiveSettle.size == acqActiveDistinctList.size) {
                            // in case all acquirer was set different time
                            for (currentAcquirer in acqActiveSettle) {
                                val info = SettlementInfo()
                                info.apply {
                                    this.acqName = arrayListOf(currentAcquirer.name)
                                    this.multiAcquirer = false
                                    this.settletime = currentAcquirer.settleTime
                                    this.latestSettleTime = currentAcquirer.latestSettledDateTime?.let {
                                        arrayListOf(it)
                                    } ?: run {
                                        null
                                    }
                                    this.diffTimeOver24hours = arrayListOf(isDiffTimeOver24Hrs(currentAcquirer.latestSettledDateTime, currentAcquirer.settleTime))

                                    returnArrayList.add(this)
                                }
                            }
                        } else {
                            if (acqActiveDistinctList.isNotEmpty()) {
                                val timeList = acqActiveDistinctList.keys

                                for (settleTime in timeList) {
                                    val groupActiveSettleAcq : List<Acquirer> = allAcqs.filter{ it.isEnable && it.settleTime == settleTime }
                                    val isMultiAcquirer = groupActiveSettleAcq.size > 1
                                    val multiAcquirerInfos = transformMultiAcquirerList(groupActiveSettleAcq, settleTime)

                                    val info = SettlementInfo()
                                    info.apply {
                                        this.acqName = multiAcquirerInfos[0] as ArrayList<String>
                                        this.multiAcquirer = isMultiAcquirer
                                        this.settletime = settleTime
                                        this.latestSettleTime = multiAcquirerInfos[1] as ArrayList<String>
                                        this.diffTimeOver24hours = multiAcquirerInfos[2] as ArrayList<Boolean>

                                        returnArrayList.add(this)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (ex : Exception) {
            ex.printStackTrace()
        }
        return returnArrayList
    }

    private fun transformMultiAcquirerList(multiAcquirerList: List<Acquirer>, settleTime : String) : List<ArrayList<Any?>> {
        var returnList = listOf<ArrayList<Any?>>()
        if (multiAcquirerList.isNotEmpty()) {
            val acqNamesList = ArrayList<Any?>()
            val lastSettleTimeList = ArrayList<Any?>()
            val overThan24hrsList = ArrayList<Any?>()
            for (acq in multiAcquirerList) {
                acqNamesList.add(acq.name)
                lastSettleTimeList.add(acq.latestSettledDateTime)
                overThan24hrsList.add(isDiffTimeOver24Hrs(acq.latestSettledDateTime, settleTime))
            }
            returnList = listOf(acqNamesList, lastSettleTimeList, overThan24hrsList)
        }

        return returnList
    }

    @SuppressLint("SimpleDateFormat")
    private fun isDiffTimeOver24Hrs(lastSettleDateTime: String?, hostSettleTime: String) : Boolean {
        try {
            val dateTimeCurrent : Date? = SimpleDateFormat(SettleAlarmProcess.STR_FORMAT_DATETIME_LAST_SETTLE).parse(Device.getTime(
                SettleAlarmProcess.STR_FORMAT_DATETIME_LAST_SETTLE
            ))
            lastSettleDateTime?.let {
                val dateTimeLastSettle : Date? = SimpleDateFormat(SettleAlarmProcess.STR_FORMAT_DATETIME_LAST_SETTLE).parse(lastSettleDateTime)
                dateTimeLastSettle?.let{ lastSettle->
                    dateTimeCurrent?.let { curr->
                        val diff : Long = (curr.time - lastSettle.time) / (1000 * 60 * 60 * 24)
                        if (diff >= 1) {
                            return true
                        }
                    }
                }
            }?:run{
//                val strDateTime = Device.getTime(SettleAlarmProcess.STR_FORMAT_DATE_ONLY) + " ${hostSettleTime.replace(":","")}00"
//                val dateTimeToDailySettle : Date? = SimpleDateFormat(SettleAlarmProcess.STR_FORMAT_DATETIME_LAST_SETTLE).parse(strDateTime)
//                dateTimeToDailySettle?.let{ timeToSettle->
//                    dateTimeCurrent?.let { curr->
//                        val diff : Long = (curr.time - timeToSettle.time) / (1000 * 60)
//                        if (diff >= 1) {
//                            return true
//                        }
//                    }
//                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return false
    }

    fun setNextDaySettlement(context: Context, hostName: String, itemIndex: String, currentMilis: Long) {
        // [1] detect settle mode
        Log.d(this.TAG, "\t=======================================================")
        Log.d(this.TAG, "\t SET-NEXT-DAY-SETTLEMENT")
        Log.d(this.TAG, "\t=======================================================")
        val mode = SettlementMode.getMode(getEDCSettlementMode())
        val actionStr : String? = when (mode) {
            SettlementMode.AUTO_SETTLE  -> "AUTO_SETTLE"
            SettlementMode.FORCE_SETTLE -> "FORCE_SETTLE"
            else-> null
        }
        val clazz : KClass<*>? = when (mode) {
            SettlementMode.AUTO_SETTLE  -> AutoSettleAlarmReceiver::class
            SettlementMode.FORCE_SETTLE -> ForceSettleAlarmReceiver::class
            else-> null
        }
        Log.d(this.TAG, "\tMODE : $actionStr")
        Log.d(this.TAG, "\tCLASS : ${clazz.toString()}")

        if (clazz==null) {
            // cannot detect settlement-mode that it make unable to map process to target settlement-class
            return
        }

        // [2] get AlarmManager
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        Log.d(this.TAG, "\tALARM MANAGER : OK")

        // [3] create intent for OnWakeup call
        val intent  = Intent(context, clazz.java)
        intent.action = actionStr
        intent.putExtra(SettlementRegisterActivity.param_name, hostName)
        intent.putExtra(SettlementRegisterActivity.param_item_index, itemIndex)

        Log.d(this.TAG, "\t\tPARAM-HOSTNAME    : $hostName")
        Log.d(this.TAG, "\t\tPARAM-ITEM-INDEX  : $itemIndex")

        val intentId = SettleAlarmProcess.PREFIX_INTENT_ID + itemIndex.toInt()
        val pendingIntent = PendingIntent.getBroadcast(context, intentId, intent, PendingIntent.FLAG_CANCEL_CURRENT)

        Log.d(this.TAG, "\tPENDING-INTENT-ID  : $intentId")

        // [4] Detect repeat time send from PAX STORE
        val testmode = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_SETTLE_MODE_TESTING, false)
        val milisStr : String? = FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_SETTLE_MODE_TESTING_TIME_INTERVAL)
        val repeatMillis : Long = if (testmode && milisStr!=null) {milisStr.toLong() } else { AlarmManager.INTERVAL_DAY }

        // [5] Cancel pending intent and set new Exact time
        alarmManager.cancel(pendingIntent)
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, currentMilis + repeatMillis, pendingIntent)
        Log.d(this.TAG, "\tSET RTC_WAKEUP MODE SUCCESS")
        Log.d(this.TAG, "\t=======================================================")
    }
}