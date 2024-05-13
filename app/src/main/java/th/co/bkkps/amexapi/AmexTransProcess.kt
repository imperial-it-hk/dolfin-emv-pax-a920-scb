package th.co.bkkps.amexapi

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.pax.abl.core.AAction
import com.pax.abl.utils.EncUtils
import com.pax.edc.R
import com.pax.edc.opensdk.TransResult
import com.pax.pay.MainActivity
import com.pax.pay.app.ActivityStack
import com.pax.pay.app.FinancialApplication
import com.pax.pay.constant.Constants
import com.pax.pay.trans.TransContext
import com.pax.pay.trans.action.ActionInputPassword
import com.pax.pay.utils.TickTimer
import com.pax.settings.SysParam
import com.pax.view.dialog.DialogUtils
import th.co.bkkps.bps_amexapi.*

class AmexTransProcess(val transAPI: ITransAPI) {
    fun doSale(context: Context, amount: Long, tipAmount: Long): Boolean {
        val request = SaleMsg.Request()
        request.apply {
            this.amount = amount
            this.tipAmount = tipAmount
        }
        return transAPI.startTrans(context, request)
    }

    fun doSale(context: Context, amount: Long, tipAmount: Long, enterMode: Int,
               track1: String?, track2: String?, track3: String?,
               pan: String?, expDate: String?, emvSP200: EmvSP200?): Boolean {
        val request = SaleMsg.Request()
        request.apply {
            this.amount = amount
            this.tipAmount = tipAmount
            this.enterMode = enterMode
            this.track1 = track1
            this.track2 = track2
            this.track3 = track3
            this.pan = pan
            this.expDate = expDate
            this.emvSP200 = emvSP200
            this.ermUploadResult = -999 // set didn't conduct upload initial value
        }
        return transAPI.startTrans(context, request)
    }

    fun doSaleResponseUploadEReceipt(context: Context, amount: Long, tipAmount: Long, enterMode: Int,
                                    track1: String?, track2: String?, track3: String?,
                                    pan: String?, expDate: String?, emvSP200: EmvSP200?, ermUploadResult: Long): Boolean {
        val request = SaleMsg.Request()
        request.apply {
            this.amount = amount
            this.tipAmount = tipAmount
            this.enterMode = enterMode
            this.track1 = track1
            this.track2 = track2
            this.track3 = track3
            this.pan = pan
            this.expDate = expDate
            this.emvSP200 = emvSP200
            this.ermUploadResult = ermUploadResult
        }
        return transAPI.startTrans(context, request)
    }

    fun doRefund(context: Context, amount: Long): Boolean {
        val request = RefundMsg.Request()
        request.apply { this.amount = amount }
        return transAPI.startTrans(context, request)
    }

    fun doPreAuth(context: Context, amount: Long): Boolean {
        val request = PreAuthMsg.Request()
        request.apply { this.amount = amount }
        return transAPI.startTrans(context, request)
    }

    @SuppressLint("Range")
    fun doVoid(context: Context, transNo: Long, lastTransNo: Long): Boolean {
        val request = VoidMsg.Request()
        request.apply {
            this.voucherNo = transNo
            this.lastTransNo = lastTransNo
            this.ermUploadSuccessTraceNo = -999L
        }
        return transAPI.startTrans(context, request)
    }

    fun doVoidResponseUploadEReceipt(context: Context, transNo: Long, ermUploadResult: Long): Boolean {
        val request = VoidMsg.Request()
        request.apply {
            this.voucherNo = transNo
            this.ermUploadSuccessTraceNo = ermUploadResult
        }
        return transAPI.startTrans(context, request)
    }

    fun doSettle(context: Context): Boolean {
        return transAPI.startTrans(context, SettleMsg.Request())
    }

    fun doReprint(context: Context, inputTransNo: Long, lastTransNo: Long, reprintType: Int): Boolean {
        val request = ReprintTransMsg.Request()
        request.apply {
            this.voucherNo = inputTransNo
            this.lastTransNo = lastTransNo
            this.reprintType = reprintType
        }
        return transAPI.startTrans(context, request)
    }

    fun doReport(context: Context, reportType: Int): Boolean {
        val request = ReprintTotalMsg.Request()
        request.apply { this.reprintType = reportType }
        return transAPI.startTrans(context, request)
    }

    fun doUpdateParams(context: Context, jsonParams: String): Boolean {
        val request = SettingMsg.Request()
        request.apply { this.jsonSetting = jsonParams }
        return transAPI.startTrans(context, request)
    }

    fun doLoadLogOnTle(context: Context, jsonTeId: String): Boolean {
        val request = LoadTleMsg.Request()
        request.apply { this.jsonTe = jsonTeId }
        return transAPI.startTrans(context, request)
    }

    fun doLoadLogOnTpk(context: Context): Boolean {
        return transAPI.startTrans(context, LoadLogOnTpkMsg.Request())
    }

    fun doHistory(context: Context): Boolean {
        return transAPI.startTrans(context, HistoryMsg.Request())
    }

    fun doClearTradeVoucher(context: Context): Boolean {
        return transAPI.startTrans(context, ClearTradeVoucherMsg.Request())
    }

    fun doClearReversal(context: Context): Boolean {
        return transAPI.startTrans(context, ClearReversalMsg.Request())
    }

    fun doGetTotalBatch(context: Context): Boolean {
        return transAPI.startTrans(context, GetTotalBatchMsg.Request())
    }

    fun doHistory(context: Context, tickTimer: TickTimer): AAction {
        val actionInputPassword = ActionInputPassword {
            tickTimer.stop()
            (it as ActionInputPassword).setParam(context, 6, context.getString(R.string.prompt_merchant_pwd), null)
        }

        actionInputPassword.setEndListener(AAction.ActionEndListener { action, result ->
            if (result.ret != TransResult.SUCC) {
                TransContext.getInstance().currentAction.isFinished = false
                TransContext.getInstance().currentAction = null
                ActivityStack.getInstance().popTo(MainActivity::class.java)
                return@ActionEndListener
            }

            val data = EncUtils.sha1(result.data as String)
            if (!data.equals(FinancialApplication.getSysParam().get(SysParam.StringParam.SEC_TLE_PWD))) {
                val dismissListener = DialogInterface.OnDismissListener {
                    TransContext.getInstance().currentAction.isFinished = false
                    TransContext.getInstance().currentAction = null
                    ActivityStack.getInstance().popTo(MainActivity::class.java)
                }

                DialogUtils.showErrMessage(context, context.getString(R.string.menu_report),
                        context.getString(R.string.err_password), dismissListener, Constants.FAILED_DIALOG_SHOW_TIME)
                return@ActionEndListener
            }

            transAPI.startTrans(context, HistoryMsg.Request())
        })

        return actionInputPassword
    }

    fun doConfig(context: Context, tickTimer: TickTimer): AAction {
        val actionInputPassword = ActionInputPassword {
            tickTimer.stop()
            (it as ActionInputPassword).setParam(context, 6, context.getString(R.string.prompt_merchant_pwd), null)
        }

        actionInputPassword.setEndListener(AAction.ActionEndListener { action, result ->
            if (result.ret != TransResult.SUCC) {
                TransContext.getInstance().currentAction.isFinished = false
                TransContext.getInstance().currentAction = null
                ActivityStack.getInstance().popTo(MainActivity::class.java)
                return@ActionEndListener
            }

            val data = EncUtils.sha1(result.data as String)
            if (!data.equals(FinancialApplication.getSysParam().get(SysParam.StringParam.SEC_TLE_PWD))) {
                val dismissListener = DialogInterface.OnDismissListener {
                    TransContext.getInstance().currentAction.isFinished = false
                    TransContext.getInstance().currentAction = null
                    ActivityStack.getInstance().popTo(MainActivity::class.java)
                }

                DialogUtils.showErrMessage(context, context.getString(R.string.menu_report),
                        context.getString(R.string.err_password), dismissListener, Constants.FAILED_DIALOG_SHOW_TIME)
                return@ActionEndListener
            }

            transAPI.startTrans(context, ConfigMsg.Request())
        })

        return actionInputPassword
    }

    fun isAppInstalled(context: Context): Boolean {
        //todo to get from lib
        val packageInfo: PackageInfo?
        try {
            packageInfo = context.packageManager.getPackageInfo(AmexAPIConstants.PACKAGE_NAME, 0)
        }
        catch (ex: PackageManager.NameNotFoundException) {
            return false
        }
        return packageInfo != null
    }
}