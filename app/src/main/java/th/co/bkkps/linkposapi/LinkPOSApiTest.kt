package th.co.bkkps.linkposapi

import android.annotation.SuppressLint
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import th.co.bkkps.linkpos.ILinkPOSService
import th.co.bkkps.linkposapi.service.LinkPOSMessage

class LinkPOSApiTest constructor(val mContext: Context) {
    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: LinkPOSApiTest? = null

        fun getInstance(mContext: Context): LinkPOSApiTest {
            return instance ?: synchronized(this) {
                instance ?: LinkPOSApiTest(mContext).also { instance = it }
            }
        }

        private const val TAG = "LinkPOSApi"
        private const val PACKAGE_NAME = "th.co.bkkps.linkpos"
        const val SERVICE_ACTION = "$PACKAGE_NAME.service.LinkPOSService.SERVICE_ACTION"
    }
    var linkPOSService: ILinkPOSService? = null
    var isServiceBound: Boolean = false
    var linkPosMessage: LinkPOSMessage? = null

    init {
//        linkPosMessage = LinkPOSMessage()
    }

    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Service has connected")
            linkPOSService = ILinkPOSService.Stub.asInterface(service)
//            linkPosMessage = LinkPOSMessage()
        }

        override fun onServiceDisconnected(className: ComponentName?) {
            Log.e(TAG, "Service has unexpectedly disconnected")
            linkPOSService = null
        }
    }

    private fun isPackageInstalled(packageManager: PackageManager): Boolean {
        return try {
            packageManager.getPackageInfo(PACKAGE_NAME, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /*fun bindService(context: Context) {
        if (isPackageInstalled(context.packageManager)) {
            val intent = Intent()
            intent.action = SERVICE_ACTION
            intent.`package` = PACKAGE_NAME
            if (linkPOSService == null) {
                isServiceBound = context.bindService(intent, mConnection, Service.BIND_AUTO_CREATE)
                if (isServiceBound) {
                    Toast.makeText(context, "LinkPOS Service connected", Toast.LENGTH_SHORT) .show()
                } else {
                    Toast.makeText(context, "LinkPOS Service disconnected", Toast.LENGTH_SHORT) .show()
                }
            }
        }
    }*/

    fun bindService() {
        if (isPackageInstalled(mContext.packageManager)) {
            val intent = Intent()
            intent.action = SERVICE_ACTION
            intent.`package` = PACKAGE_NAME
            if (linkPOSService == null) {
                isServiceBound = mContext.bindService(intent, mConnection, Service.BIND_AUTO_CREATE)
                if (isServiceBound) {
                    Toast.makeText(mContext, "LinkPOS Service connected", Toast.LENGTH_SHORT) .show()
                } else {
                    Toast.makeText(mContext, "LinkPOS Service disconnected", Toast.LENGTH_SHORT) .show()
                }
            }
        }
    }

    fun sendResponse(jsonResp: String) {
//        if (LinkPOSApi.isServiceBound) {
//            LinkPOSApi.sendResponse()
//        }
        linkPOSService?.let {
            Log.d(TAG, "Call LinkPOS Service.")
            linkPOSService!!.call(jsonResp)
            return
        }
        Log.d(TAG, "Can't call service as LinkPOS Service disconnected.")
    }

    private val responseMessage: String = "{" +
            "\"PROTOCOL\" : \"HYPERCOM\"," +
            "\"REQUEST\" : {" +
            "   \"TRANS_CODE\" : \"20\"," +
            "   \"AMOUNT\" : \"000000000100\"" +
//                "   \"TRACE_NO\" : \"\"," +
//                "   \"NII\" : \"\"," +
//                "   \"OPT01\" : \"\"," +
//                "   \"OPT02\" : \"\"" +
            "}," +
            "\"RESPONSE\" : {" +
            "\"STATUS\" : \"SUCCESS\"," +
            "\"F00\" : \"00\"," +
            "\"F01\" : \"123456\"," +
            "\"F02\" : \"APPROVE\"," +
            "\"F03\" : \"201111\"," +
            "\"F04\" : \"191112\"," +
            "\"F06\" : \"MER Name\"," +
            "\"F16\" : \"87654321\"," +
            "\"F30\" : \"XXXXXXXXXX\"," +
            "\"F31\" : \"XXXX\"," +
//                    "\"F40\" : \"Amt Trans.\"," +
//                    "\"F41\" : \"Amt Tip\"," +
//                    "\"F42\" : \"Amt Cash Back\"," +
//                    "\"F43\" : \"Amt Tax\"," +
//                    "\"F44\" : \"Amt Balance\"," +
//                    "\"F45\" : \"Amt Balance Indicator\"," +
            "\"F50\" : \"000001\"," +
            "\"F65\" : \"000028\"," +
            "\"D0\" : \"BPS TEST MER\n123 SUN TOWERS\nBANGKOK\"," +
//            "\"D0_1\" : \"BPS TEST MER\"," +
//            "\"D0_2\" : \"123 SUN TOWERS\"," +
//            "\"D0_3\" : \"BANGKOK\"," +
            "\"D1\" : \"123456789012345\"," +
            "\"D2\" : \"VISA-CARD\"," +
            "\"D3\" : \"123456789012\"," +
            "\"D4\" : \"8\"" +
//                    "\"D5\" : \"Card holder name\"," +
//                    "\"D6\" : \"Generic Data\"," +
//                    "\"D7\" : \"Generic Data2\"," +
//                    "\"D8\" : \"Generic Data3\"," +
//                    "\"D9\" : \"QR Payment Type\"," +
//                    "\"E1\" : \"AID\"," +
//                    "\"E2\" : \"TVR\"," +
//                    "\"E3\" : \"TC\"," +
//                    "\"E4\" : \"APP Name\"," +
//                    "\"H1\" : \"Batch Total sales count\"," +
//                    "\"H2\" : \"Batch total sales amount\"," +
//                    "\"HN\" : \"Nii\"" +
            "}" +
            "}"
}