package th.co.bkkps.amexapi.action

import android.content.Context
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.device.DeviceImplNeptune
import com.pax.edc.opensdk.TransResult
import com.pax.jemv.clcommon.RetCode
import com.pax.jemv.device.model.ApduRespL2
import com.pax.jemv.device.model.ApduSendL2
import com.pax.pay.app.FinancialApplication
import com.pax.pay.constant.Constants
import com.pax.pay.service.IccTester
import com.pax.pay.trans.component.Component
import com.pax.pay.trans.model.TransData
import com.pax.pay.utils.IApdu
import com.pax.pay.utils.Packer
import com.pax.pay.utils.Utils
import th.co.bkkps.amexapi.AmexTransService
import th.co.bkkps.utils.Log

class ActionAmexCheckAID(listener: ActionStartListener?) : AAction(listener) {
    private var context: Context? = null
    private var enterMode: TransData.EnterMode? = null
    private var aid: String = ""

    fun setParam(context: Context?, enterMode: TransData.EnterMode?) {
        this.context = context
        this.enterMode = enterMode
    }

    fun setParam(context: Context?, enterMode: TransData.EnterMode?, aid: String) {
        this.context = context
        this.enterMode = enterMode
        this.aid = aid
    }

    private fun findContactAID() : ByteArray {
        if (IccTester.getInstance().detect(0.toByte())) {
            val res = IccTester.getInstance().init(0.toByte())
            if (res == null) {
                Log.i("Test", "init ic card,but no response")
                return "".toByteArray()
            }

            IccTester.getInstance().autoResp(0.toByte(), true)
            val apdu = Packer.getInstance().apdu
            val dataIn = "1PAY.SYS.DDF01"

            // step1 select record
            var apduReq: IApdu.IApduReq = apdu.createReq(
                0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(),
                dataIn.toByteArray(), 0
            )
            apduReq.setLcAlwaysPresent()
            val req = apduReq.pack()
            val isoRes = IccTester.getInstance().isoCommand(0.toByte(), req)
            if (isoRes != null) {
                val apduResp = apdu.unpack(isoRes)
                val result_1 = apduResp.statusString
                if (result_1 == "9000")
                {   // step2 read record
                    apduReq = apdu.createReq (
                        0x00.toByte(), 0xB2.toByte(), 0x01.toByte(), 0x0C.toByte()
                    )
                    apduReq.setLcAlwaysPresent()
                    val req = apduReq.pack()
                    val isoRes2 = IccTester.getInstance().isoCommand(0.toByte(), req)
                    if (isoRes2 != null) {
                        val apduResp = apdu.unpack(isoRes2)
                        val result_2 = apduResp.statusString
                        if (result_2 == "9000") {
                            var ind = apduResp.data.indexOf(0x4F.toByte())
                            var len = apduResp.data[ind+1]
                            var aid : ByteArray = ByteArray (len.toInt())
                            System.arraycopy(apduResp.data, ind+2, aid, 0, len.toInt())
                            return aid
                        }
                    }
                }
            }
        }
        return "".toByteArray()
    }

    private fun findContactlessAID() : ByteArray {
        var apduSend = ApduSendL2()
        var apduResp = ApduRespL2()
        var sendCommand = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte()
        )
        System.arraycopy(sendCommand, 0, apduSend.command, 0, sendCommand.size)
        apduSend.lc = 14
        val dataIn = "2PAY.SYS.DDF01"
        System.arraycopy(dataIn.toByteArray(), 0, apduSend.dataIn, 0, dataIn.toByteArray().size )
        apduSend.le = 256
        var ret = DeviceImplNeptune.getInstance().piccIsoCommandDevice(apduSend, apduResp).toInt()
        if (ret != RetCode.EMV_OK) return "".toByteArray()

        if (apduResp.swa == 0x90.toByte() && apduResp.swb == 0x00.toByte())
        {
            var ind = apduResp.dataOut.indexOf(0x4F.toByte())
            var len = apduResp.dataOut[ind+1]
            var aid : ByteArray = ByteArray (len.toInt())
            System.arraycopy(apduResp.dataOut, ind+2, aid, 0, len.toInt())
            return aid
        }
        return "".toByteArray();
    }

    override fun process() {
        try {
            when (enterMode) {
                TransData.EnterMode.INSERT,
                TransData.EnterMode.CLSS,
                TransData.EnterMode.SP200 -> {
                    if (aid == null || aid == "") {
                        if (enterMode == TransData.EnterMode.INSERT) {
                            aid = Utils.bcd2Str(findContactAID())
                        }
                        else {
                            if (enterMode == TransData.EnterMode.CLSS) {
                                aid = Utils.bcd2Str(findContactlessAID())
                            }
                        }
                    }
                    //val amexaid : ByteArray = Constants.AMEX_AID_PREFIX . ByteArray()
                    if (aid.startsWith(Constants.AMEX_AID_PREFIX, true)) {
                        val acqAmex = FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_AMEX)
                        if (AmexTransService.isAmexAppInstalled(context!!) && acqAmex != null) {
                            if (Component.chkSettlementStatus(Constants.ACQ_AMEX)) {
                                setResult(ActionResult(TransResult.ERR_SETTLE_NOT_COMPLETED, null))
                            } else {
                                setResult(ActionResult(TransResult.ERR_NEED_FORWARD_TO_AMEX_API, null))
                            }
                        } else {
                            setResult(ActionResult(TransResult.ERR_CARD_UNSUPPORTED, null))
                        }
                    } else {
                        setResult(ActionResult(TransResult.SUCC, null))
                    }
                }
                else -> {
                    setResult(ActionResult(TransResult.SUCC, null))
                }
            }
        }
        catch (ex: Exception) {
            setResult(ActionResult(TransResult.ERR_CARD_INVALID, null))
        }
    }
}