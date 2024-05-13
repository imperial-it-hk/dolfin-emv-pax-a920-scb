package th.co.bkkps.scbapi.util

import com.pax.pay.app.FinancialApplication
import com.pax.pay.base.Acquirer
import com.pax.pay.constant.Constants
import com.pax.pay.trans.model.ETransType
import com.pax.pay.trans.model.TransData
import com.pax.pay.utils.CurrencyConverter
import com.pax.pay.utils.Utils
import th.co.bkkps.extension.currencyFormat
import th.co.bkkps.extension.numberFormat

object ScbUtil {
    const val DETAIL_REDEEM_AMT = "REDEEM_AMT"
    const val DETAIL_REDEEM_PTS = "REDEEM_PTS"
    const val DETAIL_REDEEM_QTY = "REDEEM_QTY"
    const val DETAIL_REDEEM_CODE = "REDEEM_CODE"

    fun getRedeemTransDetail(transData: TransData): HashMap<String, String> {
        val redeemDetails = hashMapOf<String, String>()

        val isVoided = ETransType.VOID === transData.transType ||
                ETransType.VOID === transData.origTransType && TransData.ETransStatus.VOIDED == transData.transState
        var redeemAmt = if (transData.redeemedAmount != null) transData.redeemedAmount.toLong(10) else 0
        redeemAmt = if (isVoided) redeemAmt * -1 else redeemAmt

        var point = if (transData.redeemPoints != null) Utils.parseLongSafe(transData.redeemPoints, 0) else 0
        val formatPts: String
        if (point > 99) {
            point = Utils.parseLongSafe(transData.redeemPoints.substring(0, transData.redeemPoints.length - 2), 0)
            point = if (isVoided) point * -1 else point
            formatPts = point.numberFormat()
        } else {
            point = if (isVoided) point * -1 else point
            formatPts = point.currencyFormat()
        }

        redeemDetails[DETAIL_REDEEM_AMT] = CurrencyConverter.convert(redeemAmt)
        redeemDetails[DETAIL_REDEEM_PTS] = formatPts
        redeemDetails[DETAIL_REDEEM_QTY] = transData.productQty.toString(10)
        redeemDetails[DETAIL_REDEEM_CODE] = transData.productCode?.trim() ?: ""

        return redeemDetails
    }

    fun getScbActiveAcquirer(): ArrayList<String>? {
        val activeAcquirer = arrayListOf<String>()
        val scbIppAcq : Acquirer? = FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_SCB_IPP)
        val scbRedeemAcq : Acquirer? = FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_SCB_REDEEM)
        when {
            scbIppAcq != null && scbRedeemAcq != null -> {
                activeAcquirer.add(Constants.ACQ_SCB_IPP)
                activeAcquirer.add(Constants.ACQ_SCB_REDEEM)
            }
            scbIppAcq != null -> {
                activeAcquirer.add(Constants.ACQ_SCB_IPP)
            }
            scbRedeemAcq != null -> {
                activeAcquirer.add(Constants.ACQ_SCB_REDEEM)
            }
            else -> {
                return null
            }
        }
        return activeAcquirer
    }

    fun getScbActiveTleAcquirer(): ArrayList<String>? {
        val activeAcquirer = arrayListOf<String>()
        val scbIppAcq : Acquirer? = FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_SCB_IPP)
        val scbRedeemAcq : Acquirer? = FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_SCB_REDEEM)
        when {
            scbIppAcq != null && scbRedeemAcq != null &&
                    scbIppAcq.isEnableTle && scbRedeemAcq.isEnableTle -> {
                activeAcquirer.add(Constants.ACQ_SCB_IPP)
                activeAcquirer.add(Constants.ACQ_SCB_REDEEM)
            }
            scbIppAcq != null && scbIppAcq.isEnableTle -> {
                activeAcquirer.add(Constants.ACQ_SCB_IPP)
            }
            scbRedeemAcq != null && scbRedeemAcq.isEnableTle -> {
                activeAcquirer.add(Constants.ACQ_SCB_REDEEM)
            }
            else -> {
                return null
            }
        }
        return activeAcquirer
    }
}