package com.pax.pay.trans.action

import android.content.Context
import android.os.SystemClock
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.dal.IScanner
import com.pax.dal.IScanner.IScanListener
import com.pax.dal.entity.EScannerType
import com.pax.dal.entity.PollingResult
import com.pax.device.Device
import com.pax.edc.R
import com.pax.edc.opensdk.TransResult
import com.pax.pay.app.FinancialApplication
import com.pax.pay.uart.SP200_serialAPI
import com.pax.pay.utils.Utils
import com.pax.settings.SysParam

class ActionScanQr(val listener: ActionStartListener?) : AAction(listener) {

    var context: Context? = null
    var title: String? = null
    var qrCode: String? = null
    var qrSP200: String? = null
    var scanner: IScanner? = null
    var iScanListener: IScanListener? = null
    var isS200run = false
    val isTimeOut = false
    var isSP200Enable = false
    var sp200scan = false
    var timeout = 0

    /**
     * @param context
     * @param title
     * @param timeout
     */
    fun setParam(context: Context, title: String, timeout: Int) {
        this.context = context
        this.title = title
        this.timeout = timeout
    }

    override fun process() {
        initCamera()

        FinancialApplication.getApp().runInBackground {
            scanner =
                if (Utils.getString(R.string.back_camera) == FinancialApplication.getSysParam()[SysParam.StringParam.EDC_DEFAULT_CAMERA]) {
                    Device.getScanner(EScannerType.REAR)
                } else {
                    Device.getScanner(EScannerType.FRONT)
                }
            scanner?.let {
                it.open()
                it.setTimeOut(timeout * 1000)
                it.setContinuousTimes(1)
                it.setContinuousInterval(1000)
                it.start(iScanListener)
            }

            isSP200Enable = SP200_serialAPI.getInstance().isSp200Enable
            if (isSP200Enable) {
                scanQRviaSP200()
            }
        }
    }

    private fun initCamera() {
        iScanListener = object : IScanListener {
            override fun onCancel() {
                // DO NOT call setResult here since it will be can in onFinish
            }

            override fun onFinish() {
                if (!sp200scan) {
                    if (isSP200Enable) {
                        SP200_serialAPI.getInstance().BreakReceiveThread()
                        SP200_serialAPI.getInstance().isSp200Cancel = true
                        SP200_serialAPI.getInstance().cancelSP200()
                    }
                    if (scanner != null) {
                        scanner!!.close()
                    }
                    if (qrCode != null && qrCode!!.isNotEmpty()) {
                        setResult(ActionResult(TransResult.SUCC, qrCode))
                        qrCode = null
                    } else { //FIXME press key back on A920C while scanning should return to onCancel firstly
                        setResult(ActionResult(TransResult.ERR_USER_CANCEL, null))
                    }
                }
            }

            override fun onRead(content: String) {
                qrCode = content
            }
        }
    }

    private fun scanQRviaSP200() {
        SystemClock.sleep(200)
        val iRet = SP200_serialAPI.getInstance().checkStatusSP200()
        if (iRet == 0) {
            qrSP200 = SP200_serialAPI.getInstance().ScanQRForPan(timeout)
            isS200run = true
            searchSP200Thread()
        } else {
            isSP200Enable = false
        }
    }

    private fun searchSP200Thread()  {
        var sp200API = SP200_serialAPI.getInstance()
        sp200API.StartReceiveThread(2)
        FinancialApplication.getApp().runInBackground {
            if (qrSP200 == null && isS200run) {
                sp200API.BreakReceiveThread()
                SP200_serialAPI.getInstance().cancelSP200()
            } else {
                onReadSP200Result()
            }
        }
    }

    private fun onReadSP200Result() {
        if (qrSP200 != null && qrSP200 !== "") {
            sp200scan = true
            if (scanner != null) {
                scanner!!.close()
            }
            if (qrSP200 != null && qrSP200!!.isNotEmpty()) {
                setResult(ActionResult(TransResult.SUCC, qrSP200))
                qrSP200 = null
            } else { //FIXME press key back on A920C while scanning should return to onCancel firstly
                setResult(ActionResult(TransResult.ERR_USER_CANCEL, null))
            }
        }
    }
}
