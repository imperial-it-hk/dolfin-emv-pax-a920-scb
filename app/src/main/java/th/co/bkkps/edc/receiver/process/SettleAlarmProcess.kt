package th.co.bkkps.edc.receiver.process

import android.content.ComponentName
import android.content.pm.PackageManager
import com.pax.pay.app.FinancialApplication
import th.co.bkkps.utils.Log

open class SettleAlarmProcess: AlarmProcess {

    companion object {
        const val TAG : String = "ALARMMGMT PROCESS"
        const val STR_FORMAT_DATETIME_LAST_SETTLE = "yyyyMMdd HHmmss"
        const val STR_FORMAT_DATE_ONLY = "yyyyMMdd"
        const val TIME_SECONDS_NOTIFY = 10000L
        const val PREFIX_INTENT_ID: Int = 8900

        fun calculateMillis (day: Int, hour : Int, minute : Int, sec : Int) : Long {
            val secPerMinute : Int = 60
            val minutePerHour : Int = 60
            val hourPerDay : Int = 24

            val millisPerSec : Long = 1000L
            val millisPerMinute = secPerMinute * millisPerSec
            val millisPerHour = minutePerHour * secPerMinute * millisPerSec
            val millisPerDay  = hourPerDay * minutePerHour * secPerMinute * millisPerSec

            var totalMillis : Long = 0L
            totalMillis += day * millisPerDay
            totalMillis += hour * millisPerHour
            totalMillis += minute * millisPerMinute
            totalMillis += sec *  millisPerSec

            return totalMillis
        }
    }

    enum class SettlementMode(val value: String) {
        DISABLE("0"),
        AUTO_SETTLE("1"),
        FORCE_SETTLE("2");

        companion object {
            private val mapping = values().associateBy(SettlementMode::value)
            fun getMode(value: String) = mapping[value]
        }
    }

    override fun enableBroadcastReceiver(isEnable: Boolean, clazz: Class<*>) {
        Log.d(TAG, "---------------Enable AutoSettle : BroadcastReceiver = $isEnable")
        val state = if (isEnable) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        val flag = PackageManager.DONT_KILL_APP
        val compName = ComponentName(FinancialApplication.getApp().applicationContext, clazz)
        FinancialApplication.getApp().applicationContext.packageManager.setComponentEnabledSetting(compName,state,flag)
    }

    override fun registerAlarmManager() {
        //do nothing
    }

    override fun detectAndRunPendingItems() {
        //do nothing
    }

    fun calculateMillis (day: Int, hour : Int, minute : Int, sec : Int) : Long {
        val secPerMinute : Int = 60
        val minutePerHour : Int = 60
        val hourPerDay : Int = 24

        val millisPerSec : Long = 1000L
        val millisPerMinute = secPerMinute * millisPerSec
        val millisPerHour = minutePerHour * secPerMinute * millisPerSec
        val millisPerDay  = hourPerDay * minutePerHour * secPerMinute * millisPerSec

        var totalMillis : Long = 0L
        totalMillis += day * millisPerDay
        totalMillis += hour * millisPerHour
        totalMillis += minute * millisPerMinute
        totalMillis += sec *  millisPerSec

        return totalMillis
    }

    fun getAcquirerNames(acqList : ArrayList<String>) : String {
        var acqNames : String = ""
        if (acqList.isNotEmpty()) {
            for (acqName in acqList) {
                acqNames += (if (acqNames.equals("")) "" else "|" ) + acqName
            }
        }

        return acqNames
    }
}