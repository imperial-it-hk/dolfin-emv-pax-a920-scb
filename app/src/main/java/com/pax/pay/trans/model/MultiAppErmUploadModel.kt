package com.pax.pay.trans.model

import com.pax.pay.base.Acquirer
import th.co.bkkps.bps_amexapi.SaleMsg
import th.co.bkkps.bps_amexapi.VoidMsg
import java.io.Serializable

class MultiAppErmUploadModel {

    var stanNo : String? = null
    var traceNo : String? = null
    var batchNo : String? = null
    var transNumber : String? = null
    var acquirer : Acquirer? = null
    var saleResp : SaleMsg.Response? = null
    var voidResp : VoidMsg.Response? = null
    var eSlipData : ByteArray? = null

}