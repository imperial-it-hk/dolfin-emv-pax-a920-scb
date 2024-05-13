package th.co.bkkps.kcheckidAPI

import android.content.Context
import android.content.pm.PackageManager

class KCheckIDService {

    companion object {

        //private val enterModeMap: Map<Int, EnterMode> = HashMap()
        private const val PACKAGE_NAME = "com.pax.edc.kbank.kcheckid"

        fun isKCheckIDInstalled(context: Context): Boolean {
            val packageInfo = try {
                context.packageManager.getPackageInfo(this.PACKAGE_NAME, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                return false
            }
            return packageInfo != null
        }
    }
}