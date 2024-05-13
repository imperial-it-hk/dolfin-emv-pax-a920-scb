package th.co.bkkps.utils

import com.pax.pay.base.Acquirer
import com.pax.pay.trans.model.TransData
import th.co.bkkps.extension.foundItem
import th.co.bkkps.extension.getStringAcqNameList
import th.co.bkkps.extension.removeAdjustState
import th.co.bkkps.extension.removePreAuthAndPreAuthCancel

object ArrayListUtils {
    fun removeAdjustState(list: ArrayList<TransData>): List<TransData> {
        return list.removeAdjustState()
    }

    fun removePreAuthAndPreAuthCancel(list: ArrayList<TransData>): List<TransData> {
        return list.removePreAuthAndPreAuthCancel()
    }

    fun getStringAcqNameList(list: List<Acquirer>): ArrayList<String> {
        return ArrayList(list.getStringAcqNameList())
    }

    fun isFoundItem(stringArray: Array<String>, item: String?): Boolean {
        return item?.let {
            stringArray.foundItem(item)
        } ?: run {
            false
        }
    }
}