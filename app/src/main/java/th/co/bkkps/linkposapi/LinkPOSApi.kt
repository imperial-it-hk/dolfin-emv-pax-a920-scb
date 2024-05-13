package th.co.bkkps.linkposapi

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.pax.pay.ECR.EcrProcessClass
import com.pax.pay.app.FinancialApplication
import com.pax.settings.SysParam
import th.co.bkkps.linkpos.ILinkPOSService

object LinkPOSApi {
    const val TAG = "LinkPOSApi"
    const val PACKAGE_NAME = "th.co.bkkps.linkpos"
    private const val SERVICE_ACTION = "$PACKAGE_NAME.service.LinkPOSService.SERVICE_ACTION"

    var linkPOSService: ILinkPOSService? = null
    var isServiceBound: Boolean = false
    var isServiceBind: Boolean = false
    var isAppLaunched: Boolean = false

    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Service has connected")
            isServiceBound = true
            linkPOSService = ILinkPOSService.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(className: ComponentName?) {
            Log.e(TAG, "Service has unexpectedly disconnected")
            isServiceBound = false
            linkPOSService = null
        }
    }

    fun isPackageInstalled(packageManager: PackageManager): Boolean {
        return try {
            packageManager.getPackageInfo(PACKAGE_NAME, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun bindService(context: Context) {
        if (isPackageInstalled(context.packageManager)) {
            val intent = Intent()
            intent.action = SERVICE_ACTION
            intent.`package` = PACKAGE_NAME
            if (linkPOSService == null) {
                isServiceBind = context.bindService(intent, mConnection, Service.BIND_AUTO_CREATE)
                if (isServiceBind) {
                    Toast.makeText(context, "LinkPOS Service connected", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "LinkPOS Service disconnected", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun unbindService(context: Context) {
        linkPOSService?.let {
            if (isServiceBind) {
                context.unbindService(mConnection)
            }
        }
    }

    fun sendResponse(jsonResp: String): Boolean {
//        if (!isServiceBound) {
//            Log.d(TAG, "Can't call service as LinkPOS Service disconnected.")
//            return false
//        }
        return try {
            linkPOSService?.let {
                Log.i(TAG, "Call LinkPOS Service.")
                linkPOSService!!.call(jsonResp)
            }
            true
        } catch (ex: Exception) {
            false
        }
    }

    fun getMerchant(): String {
        return FinancialApplication.getSysParam().get(SysParam.StringParam.COMM_MERC_NAME, "Disable")!!
    }

    fun getProtocol(): String {
        var protocol = FinancialApplication.getSysParam().get(SysParam.StringParam.LINKPOS_PROTOCOL, "Hypercom")!!
        protocol = when {
            EcrProcessClass.PROTOCOL.HYPERCOM.name.equals(protocol, true) -> "Hypercom"
            EcrProcessClass.PROTOCOL.POSNET.name.equals(protocol, true) -> "Posnet"
            else -> "Hypercom"
        }
        return protocol
    }

    fun getCommChannel(): String {
        return FinancialApplication.getSysParam().get(SysParam.StringParam.LINKPOS_COMM_TYPE, "USB_TO_SERIAL")!!
    }

    private const val responseMessage: String = "{" +
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