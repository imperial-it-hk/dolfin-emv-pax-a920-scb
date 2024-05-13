package th.co.bkkps.linkposapi.model

import com.pax.pay.trans.model.TransTotal

data class SettleHostData(
    val total: TransTotal,
    val status: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SettleHostData

        if (total != other.total) return false
        if (!status.contentEquals(other.status)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = total.hashCode()
        result = 31 * result + status.contentHashCode()
        return result
    }
}
