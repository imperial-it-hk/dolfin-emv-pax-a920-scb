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
import com.pax.pay.app.FinancialApplication
import org.apache.commons.lang3.time.DateUtils
import th.co.bkkps.edc.receiver.ForceSettleAlarmReceiver
import th.co.bkkps.edc.receiver.SettleBroadcastReceiver
import th.co.bkkps.utils.Log
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class ForceSettleAlarmProcess(private val context: Context,
                              private val acqSettleList: List<SettlementRegisterActivity.Companion.SettlementInfo>,
                              private val transEndListener: ATransaction.TransEndListener): SettleAlarmProcess() {

    companion object {
        const val TAG : String = "ALARMMGMT FORCE SETTLE"
    }

    @SuppressLint("SimpleDateFormat", "WrongConstant", "UnspecifiedImmutableFlag")
    override fun registerAlarmManager() {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//            val prefixIntentID : Int = ("${Device.getTime("yyMMdd")}00").toInt()

            for (itemIndex in acqSettleList.indices) {
                val forceSettleItem = acqSettleList[itemIndex]
                val intent = Intent(context, ForceSettleAlarmReceiver::class.java)
                intent.action = "FORCE_SETTLE"
                intent.putExtra(SettlementRegisterActivity.param_name, getAcquirerNames(forceSettleItem.acqName))
                intent.putExtra(SettlementRegisterActivity.param_item_index, itemIndex.toString())

                val strDateTime = Device.getTime(STR_FORMAT_DATE_ONLY) + " ${forceSettleItem.settletime.replace(":","").replace(".","")}00"
                val dateTimeToDailySettle : Date? = SimpleDateFormat(STR_FORMAT_DATETIME_LAST_SETTLE).parse(strDateTime)
                val dateTimeCurrent : Date? = SimpleDateFormat(STR_FORMAT_DATETIME_LAST_SETTLE).parse(
                    Device.getTime(
                        STR_FORMAT_DATETIME_LAST_SETTLE
                ))
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
                        Log.d(TAG, "SET AlarmManager for host : ${forceSettleItem.acqName} \ntargetSettleDateTime : ${Timestamp(alarmTimeUTC)}")
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
            val dateTimeCurrent : Date? = dateFormat.parse(Device.getTime(
                STR_FORMAT_DATETIME_LAST_SETTLE
            ))

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

            if (acqList.isNotEmpty()) {
                SettleBroadcastReceiver.waitUserCompletePayment = false
                val intent = Intent(context, ForceSettleAlarmReceiver::class.java)
                intent.action = "FORCE_SETTLE"
                intent.putExtra(SettlementRegisterActivity.param_name, getAcquirerNames(acqList))
                intent.putExtra(SettlementRegisterActivity.param_item_index, "0")
                intent.putExtra("IS_FROM_DETECTED", true)
                context.sendBroadcast(intent)
            } else {
                transEndListener.onEnd(ActionResult(TransResult.SUCC, null))
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            transEndListener.onEnd(ActionResult(TransResult.ERR_ABORTED, null))
        }
    }
}