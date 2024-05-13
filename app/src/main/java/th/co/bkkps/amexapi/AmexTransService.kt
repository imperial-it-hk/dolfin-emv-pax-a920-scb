package th.co.bkkps.amexapi

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.pax.abl.utils.TrackUtils
import com.pax.pay.app.FinancialApplication
import com.pax.pay.constant.Constants
import com.pax.pay.trans.action.ActionSearchCard
import com.pax.pay.trans.component.Component
import com.pax.pay.trans.model.TransData
import com.pax.pay.trans.model.TransMultiAppData
import com.pax.pay.utils.Utils
import com.pax.settings.SysParam
import th.co.bkkps.bps_amexapi.BaseResponse
import th.co.bkkps.bps_amexapi.TransResponse
import th.co.bkkps.bps_amexapi.TransResult

object AmexTransService {
    private val enterModeMap : MutableMap<Int, TransData.EnterMode> = HashMap()
    private var PACKAGE_NAME : String =  "th.co.bps.amex"

//    fun getPackageName () : String {
//        if (BuildConfig.FLAVOR.toLowerCase().equals("sandbox"))  {
//            return "th.co.bps.amex.sandbox"
//        } else {
//            return "th.co.bps.amex"
//        }
//    }

    init {
        enterModeMap[1] = TransData.EnterMode.SWIPE
        enterModeMap[2] = TransData.EnterMode.INSERT
        enterModeMap[3] = TransData.EnterMode.CLSS
        enterModeMap[4] = TransData.EnterMode.MANUAL
        enterModeMap[5] = TransData.EnterMode.FALLBACK
    }

    fun insertTransData(transData: TransData?, response: TransResponse?) {
        response?.let { rsp ->
            if (rsp.rspCode == TransResult.SUCC) {
                transData?.apply TransData@ {
                    this.dateTime = rsp.transTime
                    this.stanNo = rsp.stanNo
                    this.traceNo = rsp.voucherNo
                    this.batchNo = rsp.batchNo
                    this.pan = rsp.cardNo
                    this.enterMode = enterModeMap[rsp.cardType]
                    this.amount = rsp.amount
                    this.authCode = rsp.authCode
                    this.refNo = rsp.refNo
                    this.responseCode = FinancialApplication.getRspCode().parse("00")
                    this.issuer = FinancialApplication.getAcqManager().findIssuer(Constants.ISSUER_AMEX)
                    this.acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_AMEX)
//                    FinancialApplication.getTransDataDbHelper().insertTransData(this) // Requirement, no need to insert trans
                    val transMultiAppData = TransMultiAppData()
                    transMultiAppData.apply {
                        this.stanNo = response.stanNo
                        this.traceNo = response.voucherNo
                        this.acquirer = this@TransData.acquirer
                        FinancialApplication.getTransMultiAppDataDbHelper().insertTransData(this)
                    }
                    //TODO-change logic to get last trace/stan from multi app and update to main app
                    Component.incStanNo(this)
                    Component.incTraceNo(this)
                }
            }
            else {
                updateEdcTraceStan(rsp)
            }
        }
    }

    fun isAmexAppInstalled(context: Context) : Boolean {
        val packageInfo : PackageInfo?
        try {
            packageInfo = context.packageManager.getPackageInfo(PACKAGE_NAME, 0)
            packageInfo?.let {
                return true
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return false
    }

    fun updateEdcTraceStan(response: BaseResponse?) {
        response?.let {
            FinancialApplication.getSysParam().set(SysParam.NumberParam.EDC_STAN_NO, it.stanNo.toInt(), true)
            FinancialApplication.getSysParam().set(SysParam.NumberParam.EDC_TRACE_NO, it.voucherNo.toInt(), true)
        }
    }

    fun updateBatchNo(response: BaseResponse?) {
        response?.let {
            val acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_AMEX)
            acquirer?.apply {
                this.currBatchNo = it.batchNo.toInt()
                FinancialApplication.getAcqManager().updateAcquirer(this)
            }
        }
    }

    fun saveCardInfo(cardInfo: ActionSearchCard.CardInformation, transData: TransData) {
        cardInfo.let {
            transData.apply {
                when (cardInfo.searchMode) {
                    ActionSearchCard.SearchMode.KEYIN -> {
                        this.pan = it.pan
                        this.expDate = it.expDate
                        this.enterMode = TransData.EnterMode.MANUAL
                    }
                    ActionSearchCard.SearchMode.SWIPE -> {
                        val holderName = TrackUtils.getHolderName(it.track1)
                        holderName?.let { h -> this.track1 = Utils.splitHolderName(h) }
                        this.track2 = it.track2
                        this.track3 = it.track3
                        this.pan = it.pan
                        this.expDate = TrackUtils.getExpDate(it.track2)
                        this.enterMode = TransData.EnterMode.SWIPE
                    }
                    ActionSearchCard.SearchMode.INSERT -> {
                        this.enterMode = TransData.EnterMode.INSERT
                    }
                    ActionSearchCard.SearchMode.WAVE -> {
                        this.enterMode = TransData.EnterMode.CLSS
                    }
                    ActionSearchCard.SearchMode.SP200 -> {
                        this.enterMode = TransData.EnterMode.SP200
                    }
                }
            }
        }
    }
}
