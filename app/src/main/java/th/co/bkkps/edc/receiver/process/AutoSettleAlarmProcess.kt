package th.co.bkkps.edc.receiver.process

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.pax.abl.core.ATransaction
import com.pax.abl.core.ActionResult
import com.pax.device.Device
import com.pax.edc.opensdk.TransResult
import com.pax.pay.SettlementRegisterActivity
import com.pax.pay.app.ActivityStack
import com.pax.pay.app.FinancialApplication
import com.pax.pay.base.Acquirer
import com.pax.pay.constant.Constants
import com.pax.pay.record.Printer
import com.pax.pay.trans.SettleTrans
import com.pax.settings.SysParam
import org.apache.commons.lang3.time.DateUtils
import th.co.bkkps.edc.receiver.AutoSettleAlarmReceiver
import th.co.bkkps.utils.Log
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class AutoSettleAlarmProcess(private val context: Context,
                             private val acqSettleList: List<SettlementRegisterActivity.Companion.SettlementInfo>,
                             private val transEndListener: ATransaction.TransEndListener): SettleAlarmProcess() {

    companion object {
        const val TAG : String = "ALARMMGR AUTO SETTLE"
    }

    @SuppressLint("SimpleDateFormat", "WrongConstant", "UnspecifiedImmutableFlag")
    override fun registerAlarmManager() {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//            val prefixIntentID : Int = ("${Device.getTime("yyMMdd")}00").toInt()

            for (itemIndex in acqSettleList.indices) {
                val autoSettleItem = acqSettleList[itemIndex]
                val intent = Intent(context, AutoSettleAlarmReceiver::class.java)
                intent.action = "AUTO_SETTLE"
                intent.putExtra(SettlementRegisterActivity.param_name, getAcquirerNames(autoSettleItem.acqName as ArrayList<String>))
                intent.putExtra(SettlementRegisterActivity.param_item_index, itemIndex.toString())

                val strDateTime = Device.getTime(STR_FORMAT_DATE_ONLY) + " ${autoSettleItem.settletime.replace(":","").replace(".","")}00"
                val dateTimeToDailySettle : Date? = SimpleDateFormat(STR_FORMAT_DATETIME_LAST_SETTLE).parse(strDateTime)
                val dateTimeCurrent : Date? = SimpleDateFormat(STR_FORMAT_DATETIME_LAST_SETTLE).parse(Device.getTime( STR_FORMAT_DATETIME_LAST_SETTLE ))
                val triggerMillis : Long

                val intentId = PREFIX_INTENT_ID + itemIndex
                Log.d(TAG, "IntentID = $intentId")

                val pendingIntent = PendingIntent.getBroadcast(context, intentId, intent, PendingIntent.FLAG_CANCEL_CURRENT)
                dateTimeToDailySettle?.let { timeToSettle->
                    dateTimeCurrent?.let { curr->
                        triggerMillis = timeToSettle.time - curr.time
                        val repeatInterval = calculateMillis(1,0,0,0)

                        acqSettleList[itemIndex].realAlarmTimeUTC = curr.time + triggerMillis

                        //As AlarmManager.setRepeating will be triggered immediately if the stated trigger time is in the past
                        //So, need to plus one day to disable the trigger as we have detected pending process
                        val alarmTimeUTC: Long = if (triggerMillis <= 0) { //The trigger time is in the past
                            curr.time + triggerMillis + AlarmManager.INTERVAL_DAY
                        } else {
                            curr.time + triggerMillis
                        }

                        alarmManager.cancel(pendingIntent)
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTimeUTC, pendingIntent)
                        //alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, alarmTimeUTC, repeatInterval, pendingIntent)
                        Log.d("SET AlarmManager for host : ${autoSettleItem.acqName} \ntargetSettleDateTime : ${Timestamp(alarmTimeUTC).toString()}")
                    }
                }
            }
            SystemClock.sleep(100)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    @SuppressLint("SimpleDateFormat")
    override fun detectAndRunPendingItems() {
        try {
            val acqList = ArrayList<String>()

            val dateFormat = SimpleDateFormat(STR_FORMAT_DATETIME_LAST_SETTLE)
            val dateTimeCurrent : Date? = dateFormat.parse(Device.getTime( STR_FORMAT_DATETIME_LAST_SETTLE ))

            for (aInfo : SettlementRegisterActivity.Companion.SettlementInfo in acqSettleList) {
                for (acq in aInfo.acqName) {
                    val strDateTimeAlarm = dateFormat.format(Date(aInfo.realAlarmTimeUTC))
                    val dateTimeAlarm = dateFormat.parse(strDateTimeAlarm)
                    val strDateTimeLastAlarm = dateFormat.format(Date(aInfo.realAlarmTimeUTC - AlarmManager.INTERVAL_DAY))
                    val dateTimeLastAlarm = dateFormat.parse(strDateTimeLastAlarm)

                    val acquirer = FinancialApplication.getAcqManager().findAcquirer(acq)
                    acquirer.latestSettledDateTime?.let {
                        val dateTimeLastSettle = dateFormat.parse(it)

                        if (dateTimeCurrent!!.after(dateTimeAlarm) || dateTimeCurrent == dateTimeAlarm
                            || (!DateUtils.isSameDay(dateTimeLastSettle!!, dateTimeAlarm) && dateTimeLastSettle.before(dateTimeLastAlarm))) {
                            if (dateTimeLastSettle!!.before(dateTimeAlarm)) {
                                acqList.add(acq)
                            }
                        }
                    }
                }
            }

            if (acqList.isNotEmpty() && acqList.size>0) {
                try {
                    val printOnExecution = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_PRINT_ON_EXECUTE_SETTLE, false)
                    if (printOnExecution) Printer.printAutoSettlementOnWakeUp(ActivityStack.getInstance().top(), acqList, "AUTO SETTLEMENT")
                    executeSettlement(acqList, transEndListener)
                    return
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            transEndListener.onEnd(ActionResult(TransResult.SUCC, null))
        } catch (ex: Exception) {
            ex.printStackTrace()
            transEndListener.onEnd(ActionResult(TransResult.ERR_ABORTED, null))
        }
    }

    private fun executeSettlement(acquirerNameList : ArrayList<String>, exListener: ATransaction.TransEndListener) {
        val currentHostName : String = acquirerNameList[0]
        val acquirer : Acquirer = FinancialApplication.getAcqManager().findAcquirer(currentHostName)

        Log.d(TAG, "\t\tHOST NAME : $currentHostName")
        Log.d(TAG, "\t\t\tENABLED AUTOSETTLE : ${SettlementRegisterActivity.isEnableSettleMode()}")
        Log.d(TAG, "\t\t\tDAILY AUTOSETTLE TIME : ${acquirer.settleTime}")
        Log.d(TAG, "\t\t\tLATEST SETTLEMENT DATE/TIME : ${acquirer.latestSettledDateTime}")

        val listener = ATransaction.TransEndListener { result ->
            if (currentHostName == Constants.ACQ_AMEX && result?.ret==0) {
                SettlementRegisterActivity.updateSettleTime(Constants.ACQ_AMEX)
            }
            acquirerNameList.removeAt(0)
            exListener.onEnd(ActionResult(TransResult.SUCC, null))
        }
        val settleTrans = SettleTrans(context, false,true,false, acquirerNameList, listener, true)
        settleTrans.execute()
    }

}