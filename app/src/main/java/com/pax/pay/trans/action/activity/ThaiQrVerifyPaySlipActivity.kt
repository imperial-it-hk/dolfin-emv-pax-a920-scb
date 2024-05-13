package com.pax.pay.trans.action.activity

import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.text.Html
import android.text.Spanned
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.pax.abl.core.ActionResult
import com.pax.dal.IScanner
import com.pax.dal.entity.EScannerType
import com.pax.device.Device
import com.pax.edc.R
import com.pax.edc.opensdk.TransResult
import com.pax.pay.BaseActivityWithTickForAction
import com.pax.pay.app.FinancialApplication
import com.pax.pay.uart.SP200_serialAPI
import com.pax.pay.utils.Convert
import com.pax.pay.utils.TickTimer
import com.pax.pay.utils.Utils
import com.pax.settings.SysParam
import th.co.bkkps.utils.Log


class ThaiQrVerifyPaySlipActivity : BaseActivityWithTickForAction() {

    var isSp200Connected : Boolean = false
    lateinit var scanner : IScanner
    lateinit var iScanListener : IScanner.IScanListener
    var timer : TickTimer? = null
    lateinit var onTickTimerListener: TickTimer.OnTickTimerListener
    val defaultTimeoutSec : Int = TickTimer.DEFAULT_TIMEOUT
    var qrCodeData : String? = null
    var isSP200run : Boolean = false

    override fun getLayoutId(): Int { return R.layout.activity_verify_pay_slip_qr_b_scan_c }


    lateinit var txv_display_status : TextView
    lateinit var txv_display_detail : TextView
    lateinit var btn_verify_qr_cancel : Button
    lateinit var cancel_btn_onclick_listener : View.OnClickListener

    override fun initViews() {
        txv_display_status = findViewById(R.id.txv_verify_qr_dispStatus)
        txv_display_detail = findViewById(R.id.txv_verify_qr_details)
        btn_verify_qr_cancel = findViewById(R.id.btn_verify_qr_cancel)
    }

    override fun setListeners() {
        onTickTimerListener = object : TickTimer.OnTickTimerListener {
            override fun onTick(leftTime: Long) {
                runOnUiThread {
                    var statusText = "VERIFY-QR"
                    var detailText : Spanned = Html.fromHtml("automatically cancel Verify-QR \nin <b>$leftTime Sec.</b>")

                    updateUIText(statusText, detailText)
                }
            }

            override fun onFinish() {
                validateQrData()
            }
        }

        cancel_btn_onclick_listener = object : View.OnClickListener {
            override fun onClick(p0: View?) {
                runOnUiThread {
                    btn_verify_qr_cancel.isEnabled = false
                    btn_verify_qr_cancel.setBackgroundColor(Color.rgb(166,166,166))
                    updateUIText("Verify-QR cancelling", "Please wait...")
                }

                timer?.stop()

                if (SP200_serialAPI.getInstance().getiSp200Mode() > 0 ){
                    cancelSP200()
                }
            }
        }

        // bind listener to control
        btn_verify_qr_cancel.setOnClickListener(cancel_btn_onclick_listener)
        btn_verify_qr_cancel.visibility = View.GONE
    }

    override fun loadParam() { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.setTitle("VERIFY-QR")
        tickTimer.stop()

        // local timer
        timer = TickTimer(onTickTimerListener)
        timer?.start(defaultTimeoutSec)

        try {
            startSP200()
        } catch (ex : Exception) {
            ex.printStackTrace()
            Log.e("VERIFYQR", "onCreate:StartSP200 ::"  + ex.printStackTrace())
        }

        try {
            startEdcFrontCamera(defaultTimeoutSec)
        } catch (ex : Exception) {
            Log.e("VERIFYQR", "onCreate:StartEDCFrontCamera ::"  + ex.printStackTrace())
        }


    }

    override fun finish(actionResult: ActionResult) {
        //timer?.stop()
        super.finish(actionResult)
    }

    fun updateUIText(statusCaption: String, detailsCaption: String) {
        runOnUiThread {
            txv_display_status?.text = statusCaption
            txv_display_detail?.text = detailsCaption
        }
    }

    fun updateUIText(statusCaption: String, detailsCaption: Spanned) {
        runOnUiThread {
            txv_display_status?.text = statusCaption
            txv_display_detail?.text = detailsCaption
            SystemClock.sleep(1000)
        }
    }

    fun validateQrData() {
        Device.enableHomeRecentKey(false)
        updateUIText("VERIFY-QR", "Terminate all QR-Reader, please wait...")
        timer?.stop()
        scanner?.close()
        cancelSP200()

        updateUIText("VERIFY-QR", "Terminate all receiver threads")
        threadFrontCam?.interrupt()
        threadSp200ReceiveQR?.interrupt()



        qrCodeData?.let {
            var paddedQrCode : String? = null
            if(it.length < 400) {
                paddedQrCode = Utils.getStringPadding(it, 400, " ", Convert.EPaddingPosition.PADDING_RIGHT)
            } else {
                paddedQrCode = it
            }
            updateUIText("VERIFY-QR", "Success")
            finish(ActionResult(TransResult.SUCC, paddedQrCode))
        }?:run{
            updateUIText("VERIFY-QR", "QR Code wasn't found")
            finish(ActionResult(TransResult.ERR_TIMEOUT, null))
        }
    }

    lateinit var sp200ScanQRlistener      : SP200_serialAPI.SP200ReturnListener
    var threadSp200ReceiveQR : Thread? = null
    var isRecvQrViaSP200 : Boolean = false

    fun startSP200 () {
        sp200ScanQRlistener = object : SP200_serialAPI.SP200ReturnListener {
            override fun onReturnResult(result: ActionResult?) {
                if (result?.ret ==  TransResult.SUCC) {
                    isRecvQrViaSP200 = true
                    qrCodeData = result?.data as String
                }

                threadSp200ReceiveQR?.interrupt()
                if (isRecvQrViaFrontCam) {return}
                validateQrData()
            }
        }

        if (SP200_serialAPI.getInstance().isSp200Enable()) {
            val iRet = SP200_serialAPI.getInstance().checkStatusSP200()
            if (iRet == 0) {
                threadSp200ReceiveQR ?: run {
                    try {
                        threadSp200ReceiveQR = Thread(Runnable {
                            Thread.sleep(100)
                            SP200_serialAPI.getInstance()
                                .ScanQRForPan(defaultTimeoutSec - 2, sp200ScanQRlistener)
                        })
                        threadSp200ReceiveQR?.start()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    var threadCancelSP200 : Thread? = null
    fun cancelSP200() {
        try {
            SP200_serialAPI.getInstance().BreakReceiveThread()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
           //SP200_serialAPI.getInstance().initSP200() //this line may throw interrupt exception
        }

        try {
            while (SP200_serialAPI.getInstance().cancelSP200() != 0) {
                SystemClock.sleep(100)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    var threadFrontCam : Thread? = null
    var isRecvQrViaFrontCam : Boolean = false

    fun startEdcFrontCamera(timeout : Int) {
        iScanListener = object: IScanner.IScanListener {
            override fun onRead(content: String?) {
                qrCodeData = content
            }

            override fun onFinish() {
                Device.enableHomeRecentKey(false)
                isRecvQrViaFrontCam = true
                threadFrontCam?.interrupt()
                scanner.close()
                if (isRecvQrViaSP200) {return}
                validateQrData()
            }

            override fun onCancel() {
                // do nothing
            }

        }

        threadFrontCam?:run{
            threadFrontCam = Thread(Runnable {
                var scannerRunning : IScanner = Device.getScanner(EScannerType.FRONT)
                scannerRunning.open()
                scannerRunning.setTimeOut((defaultTimeoutSec-1) * 1000)
                scannerRunning.setContinuousTimes(1)
                scannerRunning.setContinuousInterval(1000)
                scannerRunning.start(iScanListener)
                scanner = scannerRunning
            })
            threadFrontCam?.start()
        }
    }
}