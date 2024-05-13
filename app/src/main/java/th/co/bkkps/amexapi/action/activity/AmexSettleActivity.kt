package th.co.bkkps.amexapi.action.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pax.abl.core.ActionResult
import com.pax.device.Device
import com.pax.edc.opensdk.TransResult
import com.pax.pay.app.FinancialApplication
import com.pax.pay.constant.Constants
import com.pax.pay.trans.TransContext
import com.pax.pay.trans.action.ActionEReceiptInfoUpload
import com.pax.pay.trans.component.Component
import com.pax.pay.utils.EReceiptUtils
import com.pax.pay.utils.Utils
import org.json.JSONObject
import th.co.bkkps.amexapi.AmexAPIConstants
import th.co.bkkps.amexapi.AmexTransAPI
import th.co.bkkps.amexapi.AmexTransService
import th.co.bkkps.bps_amexapi.SettleMsg
import th.co.bkkps.utils.Log

class AmexSettleActivity: AppCompatActivity() {
    private var apiProcess = AmexTransAPI.getInstance().process
    private var state: Int = -1
    private var settleRequestBatchNo: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        state = AmexAPIConstants.REQUEST_PARAM_SETTING
        val intent = Intent(this@AmexSettleActivity, AmexParamsActivity::class.java)
        settleRequestBatchNo = getCurrentBatchNo()
        startActivityForResult(intent, state)
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.i(AmexAPIConstants.TAG, "onActivityResult")
        super.onActivityResult(requestCode, resultCode, data)

        when (state) {
            AmexAPIConstants.REQUEST_PARAM_SETTING -> {
                if (resultCode != TransResult.SUCC) {
                    finish(ActionResult(TransResult.ERR_PARAM, null))
                } else {
                    try {
                        state = AmexAPIConstants.REQUEST_SETTLE
                        apiProcess?.apply {
                            this.doSettle(this@AmexSettleActivity)
                        } ?: run {
                            finish(ActionResult(TransResult.ERR_AMEX_API_TRANS_EXCEPTION, null))
                        }
                    }
                    catch (ex: Exception) {
                        finish(ActionResult(TransResult.ERR_AMEX_API_TRANS_EXCEPTION, null))
                    }
                }
            }
            AmexAPIConstants.REQUEST_SETTLE -> {
                val response: SettleMsg.Response? = apiProcess.transAPI.onResult(requestCode, resultCode, data) as SettleMsg.Response?
                response?.let {
                    Log.i(AmexAPIConstants.TAG, "SETTLE response received")
                    Log.d(AmexAPIConstants.TAG, "getRspCode=${it.rspCode}")

                    var summaryReport : ByteArray? = null

                    try {
                        // save eSettle-Report
                        saveAmexESettleReport(it.ermInfoJson, it.refNo)

                        // extract ERM report
                        summaryReport = getErmSummaryReport(it.ermInfoJson)

                    } catch (e: Exception) {
                        summaryReport = null;
                    } finally {
                        // update batch
                        AmexTransService.updateBatchNo(it)
                        AmexTransService.updateEdcTraceStan(it)

                        finish(ActionResult(it.rspCode, it, summaryReport))
                    }
                } ?: run {
                    Log.e(AmexAPIConstants.TAG, "SETTLE response not received")
                    finish(ActionResult(TransResult.ERR_ABORTED, null))
                }
            }
            else -> {
                //do nothing
            }
        }
    }

    fun getCurrentBatchNo() : Int {
        val acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_AMEX)
        acquirer?.let {acq->
            return  acq.currBatchNo
        }

        return -1
    }

    fun finish(result: ActionResult?) {
        val action = TransContext.getInstance().currentAction
        action?.apply {
            if (this.isFinished) return
            this.isFinished = true
            this.setResult(result)
        }
        finish()
    }

    fun saveAmexESettleReport(ermInfoJson: String?, refNo: String?) {

        val acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_AMEX)
        val ermInitHostIndex = Component.getPaddedString(acquirer?.id.toString(), 3,'0')

        ermInfoJson?.let {ermInitHostIndex
            val eReceiptRawData : ByteArray? = doPrepareErmUploadData(it, ermInitHostIndex, true)

            try {
                eReceiptRawData?.let { eSettleReport->
                    if (acquirer.getEnableUploadERM()) {
                        val ercmSettledRefNo : String = refNo?.let { it }?: run { Device.getTime(Constants.TIME_PATTERN_TRANS).substring(2, 14) }
                        EReceiptUtils.setUnSettleRawData(acquirer, ercmSettledRefNo, eSettleReport)
                    } else {
                        Log.d(EReceiptUtils.TAG, "       AMEX-API :: ERCM upload was disable on Host : " + acquirer.getName())
                    }
                }
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
                Log.d(EReceiptUtils.TAG, " AMEX-API :: ERCM Generation error exception : " + ex.message)
            }
        }
    }


    fun doPrepareErmUploadData(ermJsonString : String, ermInitHostIndex: String, isESettleReport: Boolean) : ByteArray? {
        var ermESlipFormat : String? = null
        val converter = ActionEReceiptInfoUpload(null)
        val jsonObj = JSONObject(ermJsonString)
        jsonObj.let{
            ermESlipFormat = it.get("ERM_ERECEIPT_DATA") as String

            if (ermESlipFormat!=null) {
                //var tempSlipFormat : String = ermESlipFormat!!
//                var tempSlipFormat : String = Utils.str2Bcd(ermESlipFormat).toString()
//                converter?.let{
//                    tempSlipFormat = converter.replaceErmHostIndex(tempSlipFormat)
//                    if (tempSlipFormat.contains("?AMEX-API?")) {
//                        tempSlipFormat = tempSlipFormat?.replace("?AMEX-API?",ermInitHostIndex)
//                    }
//                }
//                ermESlipFormat = tempSlipFormat

                return Utils.str2Bcd(ermESlipFormat)
            }
        }
        return null
    }

    fun getErmSummaryReport(ermJsonString : String) : ByteArray? {
        var jsonESettleReport : String? = null
        val jsonObj = JSONObject(ermJsonString)
        jsonObj.let{
            if (it.has("ERM_SETTLE_REPORT")) {
                jsonESettleReport = it.get("ERM_SETTLE_REPORT") as String
                return jsonESettleReport!!.toByteArray()
            }
        }
        return null
    }
}
