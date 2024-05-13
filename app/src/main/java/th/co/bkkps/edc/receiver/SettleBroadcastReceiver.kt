package th.co.bkkps.edc.receiver

import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.pax.edc.R
import com.pax.pay.app.ActivityStack
import th.co.bkkps.edc.receiver.process.SettleAlarmProcess.Companion.TIME_SECONDS_NOTIFY

abstract class SettleBroadcastReceiver: BroadcastReceiver() {
    var wakeUpList = ArrayList<String>()
    var isExecuting : Boolean = false
    var isFromDetected: Boolean = false

    companion object {
        var waitUserCompletePayment: Boolean = false
    }

    abstract fun runProcess(context: Context)

    fun getAcquirerNames (acqList: ArrayList<String>) : String {
        var names = ""
        if (acqList.size > 0) {
            for (i:Int in 0 until acqList.size - 1) {
                names += (if (i>0) ", " else "") + acqList[i]
            }
        }

        return names
    }

    fun makeNotification(title: String, content: String) {
        try {
            val context : Context = ActivityStack.getInstance().top()
            val mBuilder = NotificationCompat.Builder(context)
            mBuilder.setSmallIcon(R.drawable.ic_bps_gray)
            mBuilder.color = context.resources.getColor(R.color.primary)
            mBuilder.setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.kaset_logo))
            mBuilder.setContentTitle(title)
            mBuilder.setContentText(content)
            mBuilder.setVisibility(Notification.VISIBILITY_PUBLIC)
            mBuilder.priority = Notification.PRIORITY_MAX
            mBuilder.setDefaults(Notification.DEFAULT_VIBRATE)
            mBuilder.setTimeoutAfter(TIME_SECONDS_NOTIFY - 1000L)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(0, mBuilder.build())
        } catch (ex: Exception) {
            ex.printStackTrace()
        }


    }
}