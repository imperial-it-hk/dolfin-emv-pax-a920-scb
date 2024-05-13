package th.co.bkkps.linkposapi.service.message

import th.co.bkkps.linkposapi.LinkPOSApi
import th.co.bkkps.linkposapi.model.LinkPOSModel
import th.co.bkkps.linkposapi.service.LinkPOSProtocol
import java.util.*

object ProtocolMessageFactory {
    fun createInstance(model: LinkPOSModel): ProtocolMessage? {

        return try {
            val protocol = LinkPOSProtocol.valueOf(model.protocol)
            val className: String = if (LinkPOSApi.getMerchant() == "Disable") {
                "th.co.bkkps.linkposapi.service.message.${protocol.nameForClass}Message"
            } else {
                "th.co.bkkps.linkposapi.service.message.${LinkPOSApi.getMerchant()}${protocol.nameForClass}Message"
            }
            Class.forName(className).getConstructor(
                LinkPOSModel::class.java
            ).newInstance(
                model
            ) as ProtocolMessage
        } catch (ex: Exception) {
            try {
                when (LinkPOSProtocol.valueOf(model.protocol).nameForClass) {
                    LinkPOSProtocol.HYPERCOM.nameForClass -> {
                        Class.forName("th.co.bkkps.linkposapi.service.message.HypercomMessage")
                            .getConstructor(LinkPOSModel::class.java)
                            .newInstance(model) as ProtocolMessage
                    }
                    LinkPOSProtocol.POSNET.nameForClass -> {
                        Class.forName("th.co.bkkps.linkposapi.service.message.PosnetMessage")
                            .getConstructor(LinkPOSModel::class.java)
                            .newInstance(model) as ProtocolMessage
                    }
                    else -> {
                        null
                    }
                }
            } catch (ex: Exception) {
                null
            }
        }
    }
}