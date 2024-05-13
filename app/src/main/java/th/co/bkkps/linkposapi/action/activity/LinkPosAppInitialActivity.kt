package th.co.bkkps.linkposapi.action.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pax.edc.BuildConfig
import com.pax.edc.opensdk.TransResult
import com.pax.pay.ECR.EcrProcessClass
import th.co.bkkps.linkposapi.LinkPOSApi
import th.co.bkkps.linkposapi.LinkPOSApi.bindService
import th.co.bkkps.utils.Log

class LinkPosAppInitialActivity: AppCompatActivity() {
    companion object {
        const val REQ_INIT_LINKPOS_APPLICATION: Int = 19
    }

    private var mContext: Context? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("LinkPosInit", "onCreate")
        super.onCreate(savedInstanceState)

        mContext = applicationContext

        if (LinkPOSApi.isPackageInstalled(packageManager)) {
            if (LinkPOSApi.isAppLaunched) {
                Log.i("LinkPosInit", "LinkPOS is already launched")
                if (EcrProcessClass.useLinkPos) {
                    bindService(mContext!!)
                }
                onActivityResult(REQ_INIT_LINKPOS_APPLICATION, TransResult.SUCC, null)
            } else {
                val launchLinkPos: Intent?
                try {
                    launchLinkPos = packageManager.getLaunchIntentForPackage(LinkPOSApi.PACKAGE_NAME)
                } catch (ex: Exception) {
                    Log.e("LinkPosInit", "Unable to start LinkPOS app", ex)
                    onActivityResult(REQ_INIT_LINKPOS_APPLICATION, TransResult.ERR_UNABLE_TO_INIT_LINKPOS_APP, null)
                    return
                }

                if (launchLinkPos != null) {
                    launchLinkPos.putExtra("LINKPOS_CLIENT_PACKAGE_NAME", BuildConfig.APPLICATION_ID)
                    launchLinkPos.putExtra("LINKPOS_PROTOCOL", LinkPOSApi.getProtocol())
                    launchLinkPos.putExtra("LINKPOS_MERCHANT", LinkPOSApi.getMerchant())
                    launchLinkPos.putExtra("LINKPOS_COMM_CHANNEL", LinkPOSApi.getCommChannel())
                    Log.i(
                            "LinkPosInit",
                            "Start LinkPOS app with intent {${BuildConfig.APPLICATION_ID}, " +
                                    "${LinkPOSApi.getProtocol()}, ${LinkPOSApi.getMerchant()}, ${LinkPOSApi.getCommChannel()}}"
                    )
                    startActivityForResult(launchLinkPos, REQ_INIT_LINKPOS_APPLICATION)
                }
            }
        } else {
            Log.i("LinkPosInit", "LinkPOS not installed")
            onActivityResult(REQ_INIT_LINKPOS_APPLICATION, TransResult.ERR_NO_LINKPOS_APP, null)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.i("LinkPosInit", "onActivityResult requestCode=$requestCode")
        if (resultCode != TransResult.SUCC) {
            super.onActivityResult(requestCode, resultCode, data)
        } else {
            super.onActivityResult(requestCode, TransResult.SUCC, data)
        }
        LinkPOSApi.isAppLaunched = true
        setResult(resultCode)
        finish()
    }
}