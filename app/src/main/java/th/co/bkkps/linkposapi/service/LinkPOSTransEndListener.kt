package th.co.bkkps.linkposapi.service

import com.pax.abl.core.ATransaction
import com.pax.pay.trans.model.TransData

interface LinkPOSTransEndListener: ATransaction.TransEndListener {
    fun onSetData(transData: TransData): TransData
}