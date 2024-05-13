package th.co.bkkps.linkposapi.service

enum class LinkPOSProtocol(val nameForClass: String) {
    HYPERCOM("Hypercom"),
    POSNET("Posnet");

    companion object {
        private val map = values().associateBy(LinkPOSProtocol::nameForClass)
        fun getByProtocolName(protocol: String) = map[protocol]
    }
}