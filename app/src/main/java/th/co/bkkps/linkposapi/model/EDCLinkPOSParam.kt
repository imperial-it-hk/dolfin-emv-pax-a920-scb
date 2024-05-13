package th.co.bkkps.linkposapi.model

import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EDCLinkPOSParam(@Required @SerialName("PROTOCOL") val protocol: String,
                           @SerialName("MERCHANT") val merchant: String)
