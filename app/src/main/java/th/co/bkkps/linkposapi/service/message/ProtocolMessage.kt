package th.co.bkkps.linkposapi.service.message

import android.content.Context
import com.pax.pay.trans.BaseTrans

interface ProtocolMessage {
    fun onProcess(mContext: Context)
    fun onFinish()
}