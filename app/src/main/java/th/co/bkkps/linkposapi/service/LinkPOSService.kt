package th.co.bkkps.linkposapi.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.pax.pay.ECR.EcrData
import th.co.bkkps.linkpos.ILinkPOSService

class LinkPOSService: Service() {
    companion object {
        const val TAG = "LinkPOSService"
        const val SERVICE_ACTION = "SERVICE_ACTION"
    }

    private val binder = object : ILinkPOSService.Stub() {
        override fun call(jsonMessage: String?) {
            jsonMessage?.let {
                Log.d(TAG, "Received jsonMessage=$it")
                LinkPOSMessage(applicationContext).doTrans(it)
            }
        }

        override fun checkOnProcessing(): Boolean {
            return EcrData.instance.isOnProcessing
        }

        override fun checkOnHomeScreen(): Boolean {
            return EcrData.instance.isOnHomeScreen
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "${intent?.action} bind")
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "${intent?.action } unbind")
        return super.onUnbind(intent)
    }
}