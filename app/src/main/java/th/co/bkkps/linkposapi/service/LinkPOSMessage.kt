package th.co.bkkps.linkposapi.service

import android.content.Context
import com.pax.device.Device
import com.pax.pay.ECR.EcrData
import com.pax.pay.app.FinancialApplication
import th.co.bkkps.linkposapi.LinkPOSApi
import th.co.bkkps.linkposapi.model.LinkPOSModel
import th.co.bkkps.linkposapi.service.message.HypercomMessage
import th.co.bkkps.linkposapi.service.message.ProtocolMessage
import th.co.bkkps.linkposapi.service.message.ProtocolMessageFactory
import th.co.bkkps.linkposapi.util.LinkPOSConvertMessage
import th.co.bkkps.utils.Log

class LinkPOSMessage internal constructor(val context: Context) {
    var linkPOSModel: LinkPOSModel? = null

    companion object {
        const val PROTOCOL_HYPERCOM = "HYPERCOM"
        const val PROTOCOL_POSNET = "POSNET"

        fun getHostIndexByMerchant(): Map<String, Int> {
            return when (LinkPOSApi.getMerchant()) {
                "Disable" -> {
                    HypercomMessage.hostIndex
                }
                else -> mapOf()
            }
        }
    }

    fun doTrans(msg: String) {
        linkPOSModel = LinkPOSConvertMessage.jsonToModel(msg)
        linkPOSModel?.let {
            try {
                EcrData.instance.isOnProcessing = true
//                val protocol: ProtocolMessage? = when (it.protocol) {
//                    PROTOCOL_HYPERCOM -> {
//                        HypercomMessage(it)
//                    }
//                    PROTOCOL_POSNET -> {
//                        PosnetMessage(it)
//                    }
//                    else -> null
//                }
                val protocol: ProtocolMessage? = ProtocolMessageFactory.createInstance(it)
                Device.beepPrompt()
                protocol?.onProcess(context)
            }
            catch (ex: Exception) {
                Log.e("LinkPOSMessage", "", ex)
                FinancialApplication.EcrProcess.resetFlag()
            }
        }
    }
}