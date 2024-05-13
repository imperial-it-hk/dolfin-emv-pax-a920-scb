package th.co.bkkps.extension

import com.pax.pay.base.Acquirer
import com.pax.pay.trans.model.ETransType
import com.pax.pay.trans.model.TransData
import java.lang.IllegalArgumentException
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.pow

fun Long.numberFormat(): String {
    val numberFormat = NumberFormat.getInstance().apply {
        this.currency = Currency.getInstance(Locale.US)
    }
    return numberFormat.format(this)
}

fun Long.currencyFormat(): String {
    val currency: Currency = Currency.getInstance(Locale.US)
    val formatter = NumberFormat.getCurrencyInstance(Locale.US)
    val symbols: DecimalFormatSymbols = (formatter as DecimalFormat).decimalFormatSymbols
    symbols.currencySymbol = ""
    formatter.decimalFormatSymbols = symbols;
    formatter.minimumFractionDigits = currency.defaultFractionDigits
    formatter.maximumFractionDigits = currency.defaultFractionDigits

    val amount = this
    val newAmount = if (amount < 0) -amount else amount // AET-58

    val prefix = if (amount < 0) "-" else ""
    try {
        val amt = java.lang.Double.valueOf(newAmount.toDouble()) / Math.pow(
                10.0,
                currency.defaultFractionDigits.toDouble()
        )
        return prefix + formatter.format(amt)
    } catch (e: IllegalArgumentException) {
        //do nothing
    }

    return ""
}

fun MutableList<Acquirer>.removeByName(acqName: String): MutableList<Acquirer> {
    this.removeAll { acq -> acq.name == acqName }
    return this
}

fun ArrayList<TransData>.removeAdjustState(): List<TransData> {
    this.removeAll { transData -> transData.transState == TransData.ETransStatus.ADJUSTED
            && transData.offlineSendState == TransData.OfflineStatus.OFFLINE_SENT }
    return this
}

fun ArrayList<TransData>.removePreAuthAndPreAuthCancel(): List<TransData> {
    this.removeAll { transData -> transData.transType == ETransType.PREAUTHORIZATION
            || transData.transType == ETransType.PREAUTHORIZATION_CANCELLATION }
    return this
}

fun Array<String>.foundItem(value: String): Boolean {
    return this.any { temp -> temp == value }
}

fun List<Acquirer>.getStringAcqNameList(): List<String> {
    return this.map { it.name }
}