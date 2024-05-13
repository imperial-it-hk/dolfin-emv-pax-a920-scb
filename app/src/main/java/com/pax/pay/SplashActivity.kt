package com.pax.pay

import android.app.Activity
import android.app.Instrumentation
import android.app.ProgressDialog
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.fasterxml.jackson.databind.ObjectMapper
import com.pax.abl.core.AAction
import com.pax.abl.core.ATransaction
import com.pax.abl.core.ActionResult
import com.pax.appstore.DownloadParamReceiver
import com.pax.device.Device
import com.pax.edc.BuildConfig
import com.pax.edc.R
import com.pax.edc.opensdk.TransResult
import com.pax.pay.app.ActivityStack
import com.pax.pay.app.FinancialApplication
import com.pax.pay.base.Acquirer
import com.pax.pay.base.EReceiptLogoMapping
import com.pax.pay.constant.Constants
import com.pax.pay.splash.DownloadParameterActivity
import com.pax.pay.splash.SplashListener
import com.pax.pay.splash.SplashResult
import com.pax.pay.trans.EReceiptStatusTrans
import com.pax.pay.trans.EReceiptStatusTrans.ercmInitialResult
import com.pax.pay.trans.LoadTLETrans
import com.pax.pay.trans.TleStatusTrans
import com.pax.pay.trans.TransContext
import com.pax.pay.trans.action.ActionEReceipt
import com.pax.pay.trans.action.ActionPrintTransMessage
import com.pax.pay.trans.action.ActionUpdateSp200
import com.pax.pay.trans.component.Component
import com.pax.pay.trans.model.Controller
import com.pax.pay.uart.SP200_serialAPI
import com.pax.pay.utils.*
import com.pax.settings.SysParam
import com.pax.settings.SysParam.Constant.CommType
import com.pax.view.dialog.DialogUtils
import th.co.bkkps.amexapi.AmexTransService
import th.co.bkkps.amexapi.action.ActionAmexLoadLogOnTle
import th.co.bkkps.amexapi.action.ActionAmexLoadLogOnTpk
import th.co.bkkps.bpsapi.ITransAPI
import th.co.bkkps.bpsapi.LoadTleMsg
import th.co.bkkps.bpsapi.TransAPIFactory
import th.co.bkkps.dofinAPI.DolfinApi
import th.co.bkkps.dofinAPI.tran.action.ActionDolfinSetConfig
import th.co.bkkps.edc.receiver.AutoSettleAlarmReceiver
import th.co.bkkps.edc.receiver.ForceSettleAlarmReceiver
import th.co.bkkps.edc.receiver.process.SettleAlarmProcess
import th.co.bkkps.linkposapi.LinkPOSApi
import th.co.bkkps.linkposapi.action.activity.LinkPosAppInitialActivity
import th.co.bkkps.scbapi.ScbIppService
import th.co.bkkps.scbapi.trans.action.ActionScbUpdateParam
import th.co.bkkps.scbapi.util.ScbUtil
import th.co.bkkps.utils.Log
import java.io.File
import java.util.*


/*  *** NOTICE ***
*   ==== This activity use for displaying status/result of application version upgrading only ====

* */
class SplashActivity: BaseActivity() {
    companion object{
        const val TAG : String = "SPLASH::"

        const val REQ_OPEN_MAINACTIVITY             : Int = 0
        const val REQ_DOWNLOAD_PARAM                : Int = 1
        const val REQ_INITIAL_PROCESS               : Int = 2
        const val REQ_SELF_TEST                     : Int = 3

        const val REQ_INIT_ERCM_REGISTER            : Int = 11
        const val REQ_INIT_ERCM_PRINT_REPORT        : Int = 12
        const val REQ_INIT_PERMISSION_DOLFIN        : Int = 13
        const val REQ_INIT_EREASE_TLE_KEYS          : Int = 14
        const val REQ_INIT_LOAD_SCB_TLE             : Int = 15
        const val REQ_INIT_LOAD_AMEX_TLE            : Int = 16
        const val REQ_INIT_LOAD_AMEX_TPK            : Int = 17
        const val REQ_INIT_LOAD_KBANK_TLE           : Int = 18
        const val REQ_INIT_KBANK_TLE_PRINT_REPORT   : Int = 19
        const val REQ_INIT_LINKPOS_APPLICATION      : Int = 20
        const val REQ_INIT_UPDATE_SP200_FIRMWARE    : Int = 21

        const val REQ_INIT_SCBIPP_TRANS_API_CALLBACK: Int = 100

        const val STR_DISP_PENDING                  : String = "PROCESSING..."
        const val STR_DISP_DONE                     : String = "DONE"
        const val STR_DISP_DISABLED                 : String = "DISABLED"
        const val STR_DISP_NO_PARAM_FILE            : String = "NO PARAM"
        const val STR_DISP_WAITING_PARAM            : String = "WAITING PARAM..."
        const val STR_DISP_FAILED                   : String = "FAILED"
        const val STR_DISP_SERVICE_NOT_BIND         : String = "NOT INSTALL"
        const val STR_DISP_SERVICE_FAILED           : String = "SERVICE FAILED"
        const val STR_DISP_NO_ACTIVE_HOST           : String = "HOST NOT FOUND"
        const val STR_DISP_TLE_DISABLED             : String = "TLE DISABLED"
        const val STR_DISP_HOST_DISABLED            : String = "HOST DISABLED"
        const val STR_DISP_MISSING_CONFIG           : String = "MISSING CONFIG"
        const val STR_DISP_MISSING_TEID_FILE        : String = "MISSING TEID FILE"
        const val STR_DISP_WAIT_TRANS_CALLBACK      : String = "WAIT TRANS-API"
        const val STR_DISP_SP200_MISSING_AIP_FILE   : String = "MISSING API FILE"
        const val STR_DISP_DEVICE_NOT_SUPPORTED     : String = "UNSUPPORTED"


        const val logDateTimeformat : String = "dd/MM/yyyy HH:mm:ss"

        private var initialState : InitialState     = InitialState.NONE
        private var isUpgrade : Boolean             = false
        private var isFirstInitial : Boolean        = false
        private var isUnabletoBackupTle : Boolean   = false
        fun getInitialstate() : InitialState { return  initialState }
        fun getUpgradStatus() : Boolean {return isUpgrade }
        fun getFirstInitial() : Boolean {return isFirstInitial }
        fun getBackupTleStatus() : Boolean {return isUnabletoBackupTle }
        fun setInitialState(exInitState: InitialState) {
            Log.d(TAG, "InitiState :: set = " + exInitState.name)
            initialState = exInitState
        }
        fun setUpgradeStatus(exFlag: Boolean) {
            Log.d(TAG, "UpgradeStatus :: set = " + exFlag)
            isUpgrade = exFlag;
        }
        fun setBackupTleStatus(exFlag: Boolean) {
            Log.d(TAG, "BackupTleStatus :: set = " + exFlag)
            isUnabletoBackupTle=exFlag
        }

        private var transAPI : ITransAPI? = null

        fun enableBroadcastReceiver(enableBool: Boolean, clz: Class<*>) {
            Log.d(TAG, "\t\t\t----Enable----BroadcastReceiver = {$enableBool}")
            val state = if (enableBool) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            val flag = PackageManager.DONT_KILL_APP
            val compName = ComponentName(FinancialApplication.getApp().applicationContext, clz)
            FinancialApplication.getApp().applicationContext.packageManager.setComponentEnabledSetting(
                compName,
                state,
                flag
            )
        }

        fun detectDeveloperMode() : Boolean {
            return BuildConfig.FLAVOR.equals("sandbox", ignoreCase = true)
        }

        private fun createUpgrading() {

        }

        fun CleanParameterDownloadDirectory(context: Context) {
            val path = EReceiptUtils.getApp_RootDirectory(context)
            val rootDir = File(path)
            rootDir.let {
                if (it.isDirectory) {
                    val deleteOnRootDirList = arrayListOf<String>("app_imageDir","app_sskDir","files")
                    for (suffixPath:String in deleteOnRootDirList) {
                        val tmpDelDir = File(path + "/${suffixPath}")
                        tmpDelDir.let{
                            it.deleteRecursively()
                        }
                    }
                }
            }
        }
    }

    enum class InitialState {
        NONE,
        DOWNLOAD_PARAM_ONLY,
        APP_EXISTS,
        APP_FIRST_INITIAL,
        APP_UPGRADE_WITH_SETTLE_REQUIRED,
        APP_READY_TO_UPGRADE,
        APP_UPGRADING,
        APP_UPGRADED
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_splash_layout
    }

    var progressDialog : ProgressDialog? = null
    var currentInitialState : InitialState? = InitialState.NONE


    // layout control
    var txv_status_version_current: TextView? = null
    var txv_status_version_incoming: TextView? = null
    var txv_status_version_diff_state: TextView? = null

    var layout_disp_initial_states : ConstraintLayout? = null
    var txv_status_01 : TextView? = null
    var txv_status_02 : TextView? = null
    var txv_status_03 : TextView? = null
    var txv_status_04 : TextView? = null
    var txv_status_05 : TextView? = null
    var txv_status_06 : TextView? = null
    var txv_status_07 : TextView? = null
    var txv_status_08 : TextView? = null
    var txv_status_09 : TextView? = null
    var txv_status_10 : TextView? = null
    var txv_status_11 : TextView? = null
    var txv_status_12 : TextView? = null
    var btn_ok : Button? = null
    lateinit var textviewMapper : HashMap<Int, TextView?>

    var developerMode : Boolean = false;

    override fun initViews() {
        Log.d(TAG, "==================== SplashActivity - - - -> Start")

        developerMode = BuildConfig.FLAVOR.equals("sandbox", ignoreCase = true)
        if (developerMode)  { Log.d(TAG, " Run as developer mode")}


        super.enableActionBar(false);
        super.enableBackAction(false);
        super.enableDisplayTitle(false);


        Log.d(TAG, "==================== SplashActivity - - - -> Start")

        // Detect developer mode
        detectDeveloperMode();

        // Stop Broadcast receiver when enter to SplashScreen
        enableBroadcastReceiver(false, DownloadParamReceiver::class.java as Class<*>);

        // map UI control
        txv_status_version_current = findViewById(R.id.tx_status_version_exists)
        txv_status_version_incoming = findViewById(R.id.tx_status_version_incoming)
        txv_status_version_diff_state = findViewById(R.id.tx_status_version_diff_state)

        layout_disp_initial_states  = findViewById(R.id.layout_states)
        layout_disp_initial_states?.visibility = View.VISIBLE

        btn_ok = findViewById(R.id.btn_ok);
        btn_ok?.visibility = View.GONE

        txv_status_01 = findViewById(R.id.txv_status_01)
        txv_status_02 = findViewById(R.id.txv_status_02)
        txv_status_03 = findViewById(R.id.txv_status_03)
        txv_status_04 = findViewById(R.id.txv_status_04)
        txv_status_05 = findViewById(R.id.txv_status_05)
        txv_status_06 = findViewById(R.id.txv_status_06)
        txv_status_07 = findViewById(R.id.txv_status_07)
        txv_status_08 = findViewById(R.id.txv_status_08)
        txv_status_09 = findViewById(R.id.txv_status_09)
        txv_status_10 = findViewById(R.id.txv_status_10)
        txv_status_11 = findViewById(R.id.txv_status_11)
        txv_status_12 = findViewById(R.id.txv_status_12)


        textviewMapper = HashMap<Int, TextView?>()
        textviewMapper.put(REQ_SELF_TEST, txv_status_01)
        textviewMapper.put(REQ_DOWNLOAD_PARAM, txv_status_02)
        textviewMapper.put(REQ_INIT_ERCM_REGISTER, txv_status_03)
        textviewMapper.put(REQ_INIT_ERCM_PRINT_REPORT, txv_status_04)
        textviewMapper.put(REQ_INIT_PERMISSION_DOLFIN, txv_status_05)
        textviewMapper.put(REQ_INIT_LOAD_SCB_TLE, txv_status_06)
        textviewMapper.put(REQ_INIT_LOAD_AMEX_TLE, txv_status_07)
        textviewMapper.put(REQ_INIT_LOAD_AMEX_TPK, txv_status_08)
        textviewMapper.put(REQ_INIT_LOAD_KBANK_TLE, txv_status_09)
        textviewMapper.put(REQ_INIT_KBANK_TLE_PRINT_REPORT, txv_status_10)
        textviewMapper.put(REQ_INIT_LINKPOS_APPLICATION, txv_status_11)
        textviewMapper.put(REQ_INIT_UPDATE_SP200_FIRMWARE, txv_status_12)

        isUpgrade = false;
        isFirstInitial = false;

        btn_ok = findViewById(R.id.btn_ok);
        btn_ok?.visibility = View.GONE
    }

    var externalRequestCode : Int? = -999
    override fun loadParam()    {
        intent?.extras?.let{
            externalRequestCode = it.getInt("RequestType", -999)
        }
    }


    fun getERCMResult(resultCode: Int) :Int {
        var eResult:Int = -999
        if (resultCode==SplashResult.SUCCESS) {
            eResult=3;
        } else {
            eResult = 2
        }

        return eResult
    }

    fun getKBankTLEResult(resultCode: Int) : Int {
        var tleResult : Int = -999
        when (resultCode) {
            SplashResult.SUCCESS -> {
                tleResult = 5
            }
            SplashResult.TLE_KBANK_ERROR_ACQUIRER_NOT_FOUND -> {
                tleResult = 1
            }
            SplashResult.TLE_KBANK_TEID_FILE_NOT_FOUND -> {
                tleResult = 3
            }
            else -> {tleResult = 4}
        }
        return tleResult
    }

    lateinit var okButtonOnClickListener: View.OnClickListener
    lateinit var selfTestActionEndListener : AAction.ActionEndListener
    lateinit var dialogDisplayDismissListener : DialogInterface.OnDismissListener
    lateinit var downloadParamActionEndListener : AAction.ActionEndListener
    lateinit var ercmInitialEndListener : AAction.ActionEndListener
    //lateinit var ercmReportPrintTransEndListener : ATransaction.TransEndListener
    lateinit var ercmReportPrintActionEndListener: AAction.ActionEndListener
    lateinit var controlLimitReportPrintingListener: AAction.ActionEndListener
    lateinit var dolfinPermissionActionEndListener: AAction.ActionEndListener
    lateinit var eraseTLEKeyListener: SplashListener
    lateinit var loadScbTLEKeyEndListener: AAction.ActionEndListener
    lateinit var loadAmexTLEKeyEndListener: AAction.ActionEndListener
    lateinit var loadAmexTpkEndListener: AAction.ActionEndListener
    lateinit var loadKbankTLETransEndListener:ATransaction.TransEndListener
    lateinit var loadKbankTLEProgressListener:SplashListener
    lateinit var kBankTlePrintReportTransEndListener:ATransaction.TransEndListener
    lateinit var autoSettlementListener : SplashListener
    lateinit var updateSP200FirmwareActionEndListener:AAction.ActionEndListener
    lateinit var timerOnTickTimerListener:TickTimer.OnTickTimerListener
    override fun setListeners() {
        okButtonOnClickListener = object  : View.OnClickListener {
            override fun onClick(v: View?) {
                if (developerMode) {
                    // reset all required controller
                    Controller.set(Controller.IS_FIRST_RUN, false)
                    Controller.set(Controller.IS_FIRST_DOWNLOAD_PARAM_NEEDED, false)
                    Controller.set(Controller.IS_FIRST_INITIAL_NEEDED, false)
                }
                btn_ok?.isEnabled = false
                ActivityStack.getInstance().popAll()
                onEndTimer?.let{ it.stop() }
                openMainActivity(true)
            }
        }
        btn_ok?.setOnClickListener(okButtonOnClickListener)

        dialogDisplayDismissListener = object : DialogInterface.OnDismissListener {
            override fun onDismiss(dialog: DialogInterface?) {
                Log.d(TAG, "==================== DownloadParameter - - - -> on Error")
                if (!developerMode) {
                    finish()
                }

            }
        }
        selfTestActionEndListener = object : AAction.ActionEndListener {
            override fun onEnd(action: AAction?, result: ActionResult?) {
                Log.d(TAG, "==================== SelfTest - - - -> result = ${result?.ret}")
                onActivityResult(REQ_SELF_TEST, result!!.ret, null)
            }
        }
        downloadParamActionEndListener = object : AAction.ActionEndListener {
            override fun onEnd(action: AAction?, result: ActionResult?) {
                Log.d(
                    TAG,
                    "==================== DownloadParameter - - - -> result = ${result?.ret}"
                )
                onActivityResult(REQ_DOWNLOAD_PARAM, result!!.ret, null)
            }
        }
        ercmInitialEndListener = object: AAction.ActionEndListener {
            override fun onEnd(action: AAction?, result: ActionResult?) {
                Log.d(TAG, "==================== ERCM-Initial - - - -> result = ${result?.ret}")
                val requestCode : Int = REQ_INIT_ERCM_REGISTER
                result?.let {
                    if (it.ret==TransResult.SUCC) {
                        updateStatuswithRequestCode(requestCode, STR_DISP_DONE)
                        setDisplayTextUi(null, null, "Initial Successfull !!")
                    } else {
                        if (it.ret==TransResult.ERCM_INITIAL_INFO_ERCM_DISABLED
                            || it.ret==TransResult.ERCM_INITIAL_INFO_ESIG_DISABLED) {

                            updateStatuswithRequestCode(requestCode, STR_DISP_DISABLED)
                            setDisplayTextUi(null, null, "ERCM / ESIG has been disabled")

                        } else if (it.ret == TransResult.ERCM_INITIAL_INFO_HOST_KMS_DISABLED
                            || it.ret == TransResult.ERCM_INITIAL_INFO_HOST_KMS_DISABLED) {

                            updateStatuswithRequestCode(requestCode, STR_DISP_HOST_DISABLED)
                            setDisplayTextUi(null, null, "Host KMS/RMS was disabled")

                        } else if (it.ret == TransResult.ERCM_INITIAL_INFO_MISSING_BANK_CODE
                            || it.ret == TransResult.ERCM_INITIAL_INFO_MISSING_MERC_CODE
                            || it.ret == TransResult.ERCM_INITIAL_INFO_MISSING_STORE_CODE
                            || it.ret == TransResult.ERCM_INITIAL_INFO_MISSING_KEY_VERSION) {

                            updateStatuswithRequestCode(requestCode, STR_DISP_MISSING_CONFIG)
                            setDisplayTextUi(null, null, "Missing base ERCM configuration")

                        } else {
                            updateStatuswithRequestCode(requestCode, STR_DISP_FAILED)
                            setDisplayTextUi(null, null, "ERM registration failed !!")
                        }
                    }
                }
                onActivityResult(requestCode, getERCMResult(result!!.ret), null)
            }
        }
        ercmReportPrintActionEndListener = object : AAction.ActionEndListener {
            override fun onEnd(action: AAction?, result: ActionResult?) {
                startPrintControlLimitReport();
            }
        }
        controlLimitReportPrintingListener = object : AAction.ActionEndListener {
            override fun onEnd(action: AAction?, result: ActionResult?) {
                Log.d(TAG, "==================== ERCM-PrintReport - - - -> result = ${result?.ret}")
                val requestCode : Int = REQ_INIT_ERCM_PRINT_REPORT
                updateStatuswithRequestCode(requestCode, STR_DISP_DONE)
                setDisplayTextUi(null, null, "ERCM Report was printed")
                onActivityResult(REQ_INIT_ERCM_PRINT_REPORT, result!!.ret, null)
            }
        }
        dolfinPermissionActionEndListener = object: AAction.ActionEndListener {
            override fun onEnd(action: AAction?, result: ActionResult?) {
                result?.let {
                    if (it.ret == SplashResult.SUCCESS) {
                        setDisplayTextUi(null, null, "permission completed")
                        updateStatuswithRequestCode(REQ_INIT_PERMISSION_DOLFIN, STR_DISP_DONE)
                        onActivityResult(REQ_INIT_PERMISSION_DOLFIN, it.ret, null)

                    } else if (it.ret == SplashResult.DOLFIN_PERMISSION_SERVICE_NOT_BINDED) {
                        setDisplayTextUi(null, null, "Dolfin service not binded")
                        updateStatuswithRequestCode(REQ_INIT_PERMISSION_DOLFIN, STR_DISP_SERVICE_NOT_BIND)
                        onActivityResult(REQ_INIT_PERMISSION_DOLFIN, it.ret, null)

                    } else if (it.ret == SplashResult.DOLFIN_PERMISSION_ACQUIRER_DISABLED
                        || it.ret == SplashResult.DOLFIN_PERMISSION_ACQUIRER_NOT_FOUND) {
                        setDisplayTextUi(null, null, "Dolfin host disabled")
                        updateStatuswithRequestCode(REQ_INIT_PERMISSION_DOLFIN, STR_DISP_HOST_DISABLED)
                        onActivityResult(REQ_INIT_PERMISSION_DOLFIN, it.ret, null)

                    } else if (it.ret == SplashResult.DOLFIN_PERMISSION_FAILED_TO_START_SERVICE) {
                        setDisplayTextUi(null, null, "Failed to start Dolfin service")
                        updateStatuswithRequestCode(REQ_INIT_PERMISSION_DOLFIN, STR_DISP_SERVICE_FAILED)
                        onActivityResult(REQ_INIT_PERMISSION_DOLFIN, it.ret, null)

                    }
                }?:run{
                    setDisplayTextUi(null, null, "Dolfin ")
                    updateStatuswithRequestCode(REQ_INIT_PERMISSION_DOLFIN, STR_DISP_DISABLED)
                    onActivityResult(REQ_INIT_PERMISSION_DOLFIN, SplashResult.INITIAL_FAILED, null)
                }
            }
        }
        eraseTLEKeyListener = object: SplashListener {
            override fun onEndProcess(activityResult: Instrumentation.ActivityResult) {
                Log.d(
                    TAG,
                    "==================== TLE EraseKey - - - -> result = ${activityResult?.resultCode}"
                )
                if (activityResult.resultCode==SplashResult.SUCCESS) {
                    setDisplayTextUi(null, null, "TLE Key was deleted by Successful!!")
                } else {
                    setDisplayTextUi(null, null, "Failed to delete TLE Key")
                }

                onActivityResult(REQ_INIT_EREASE_TLE_KEYS, activityResult.resultCode, null)
            }

            override fun onUpdataUI(title: String, dispText: String) {
                //do notihng
            }
        }
        loadScbTLEKeyEndListener = object: AAction.ActionEndListener {
            override fun onEnd(action: AAction?, result: ActionResult?) {
                Log.d(TAG, "==================== DownloadScbTLE - - - -> result = ${result?.ret}")
                val requestCode : Int = REQ_INIT_LOAD_SCB_TLE
                if (result!!.ret==SplashResult.SUCCESS) {
                    updateStatuswithRequestCode(requestCode, STR_DISP_DONE)
                    setDisplayTextUi(null, null, "SCB TLE downloaded Successful!!")
                } else if (result!!.ret==SplashResult.WAITING_TRANS_CALLBACK) {
                    updateStatuswithRequestCode(requestCode, STR_DISP_DONE)
                    setDisplayTextUi(null, null, "ScbApiService was call TransApi successful")
                } else if (result!!.ret==SplashResult.TLE_SCB_SERVICE_NOT_BINDED) {
                    updateStatuswithRequestCode(requestCode, STR_DISP_SERVICE_NOT_BIND)
                    setDisplayTextUi(null, null, "SCB App wasn't installed")
                } else if (result!!.ret==SplashResult.TLE_SCB_TRANS_API_UNABLE_TO_START) {
                    updateStatuswithRequestCode(requestCode, STR_DISP_SERVICE_FAILED)
                    setDisplayTextUi(null, null, "Cannot connect to SCB application")
                } else if (result!!.ret==SplashResult.TLE_SCB_HOST_DISABLE_TLE) {
                    updateStatuswithRequestCode(requestCode, STR_DISP_TLE_DISABLED)
                    setDisplayTextUi(null, null, "TLE configuration was disable for SCB-IPP/REDEEM")
                } else if (result!!.ret==SplashResult.TLE_SCB_HOST_NOT_FOUND) {
                    updateStatuswithRequestCode(requestCode, STR_DISP_DISABLED)
                    setDisplayTextUi(null, null, "SCB host not found")
                } else if (result!!.ret==SplashResult.TLE_SCB_TEID_FILE_NOT_FOUND
                    || result!!.ret==SplashResult.TLE_SCB_TEID_WAS_EMPTY
                    || result!!.ret==SplashResult.TLE_SCB_TEID_READ_FILE_ERROR) {
                    updateStatuswithRequestCode(requestCode, STR_DISP_MISSING_TEID_FILE)
                    setDisplayTextUi(null, null, "SCB TEID file not found")
                } else  {
                    updateStatuswithRequestCode(requestCode, STR_DISP_FAILED)
                    setDisplayTextUi(null, null, "Failed to download SCB TLE")
                }

                if (result!!.ret!=SplashResult.WAITING_TRANS_CALLBACK) {
                    onActivityResult(requestCode, result!!.ret, null)
                }
            }
        }
        loadAmexTLEKeyEndListener = AAction.ActionEndListener { _, result ->
            val requestCode : Int = REQ_INIT_LOAD_AMEX_TLE
            result?.let {
                when (it.ret) {
                    SplashResult.SUCCESS -> {
                        updateStatuswithRequestCode(requestCode, STR_DISP_DONE)
                        setDisplayTextUi(null, null, "AMEX TLE downloaded Successful!!")
                    }
                    SplashResult.TLE_AMEX_SERVICE_NOT_BINDED -> {
                        updateStatuswithRequestCode(requestCode, STR_DISP_SERVICE_NOT_BIND)
                        setDisplayTextUi(null, null, "AMEX App wasn't installed")
                    }
                    SplashResult.TLE_AMEX_HOST_NOT_FOUND -> {
                        updateStatuswithRequestCode(requestCode, STR_DISP_DISABLED)
                        setDisplayTextUi(null, null, "AMEX host not found")
                    }
                    SplashResult.TLE_AMEX_HOST_DISABLE_TLE -> {
                        updateStatuswithRequestCode(requestCode, STR_DISP_TLE_DISABLED)
                        setDisplayTextUi(null, null, "TLE configuration was disable for AMEX")
                    }
                    SplashResult.TLE_AMEX_TEID_WAS_EMPTY -> {
                        updateStatuswithRequestCode(requestCode, STR_DISP_MISSING_TEID_FILE)
                        setDisplayTextUi(null, null, "AMEX TEID file not found")
                    }
                    else -> {
                        updateStatuswithRequestCode(requestCode, STR_DISP_FAILED)
                        setDisplayTextUi(null, null, "Failed to download AMEX TLE")
                    }
                }

                onActivityResult(requestCode, it.ret, null)
            } ?: run {
                onActivityResult(requestCode, SplashResult.TLE_AMEX_TRANS_API_RESULT_MISSING, null)
            }
        }
        loadAmexTpkEndListener = AAction.ActionEndListener { _, result ->
            val requestCode : Int = REQ_INIT_LOAD_AMEX_TPK
            result?.let {
                when (it.ret) {
                    SplashResult.SUCCESS -> {
                        updateStatuswithRequestCode(requestCode, STR_DISP_DONE)
                        setDisplayTextUi(null, null, "AMEX TPK downloaded Successful!!")
                    }
                    SplashResult.TLE_AMEX_SERVICE_NOT_BINDED -> {
                        updateStatuswithRequestCode(requestCode, STR_DISP_SERVICE_NOT_BIND)
                        setDisplayTextUi(null, null, "AMEX App wasn't installed")
                    }
                    SplashResult.TLE_AMEX_HOST_NOT_FOUND -> {
                        updateStatuswithRequestCode(requestCode, STR_DISP_DISABLED)
                        setDisplayTextUi(null, null, "AMEX host not found")
                    }
                    SplashResult.TLE_AMEX_HOST_DISABLE_TLE -> {
                        updateStatuswithRequestCode(requestCode, STR_DISP_TLE_DISABLED)
                        setDisplayTextUi(null, null, "TLE configuration was disable for AMEX")
                    }
                    else -> {
                        updateStatuswithRequestCode(requestCode, STR_DISP_FAILED)
                        setDisplayTextUi(null, null, "Failed to download AMEX TPK")
                    }
                }

                onActivityResult(requestCode, it.ret, null)
            } ?: run {
                onActivityResult(requestCode, SplashResult.TLE_AMEX_TRANS_API_RESULT_MISSING, null)
            }
        }
        loadKbankTLETransEndListener = object: ATransaction.TransEndListener {
            override fun onEnd(result: ActionResult?) {
                Log.d(TAG, "==================== DownloadKBankTLE - - - -> result = ${result?.ret}")
                Log.d(TAG, "\t\t\tLoad TLE KBANK Result = ${result?.ret}")
                Log.d(TAG, "\t\t\tLoadTMKTrans----end")
                Log.d(TAG, "\t\t----LoadKBankTLE----end")
                val requestCode : Int = REQ_INIT_LOAD_KBANK_TLE
                var tleInitResult : Int = -999
                result?.let {
                    when (it.ret) {
                        SplashResult.SUCCESS,
                        SplashResult.TLE_KBANK_DOWNLOAD_SUCCESS-> {
                            tleInitResult = 5
                            Log.d(TAG, "\t\t\tTLE KBANK DOWNLOAD : SUCCESS")
                            updateStatuswithRequestCode(requestCode, STR_DISP_DONE)
                        }
                        SplashResult.TLE_KBANK_ACTIVE_ACQUIRER_NOT_FOUND-> {
                            tleInitResult = 4
                            updateStatuswithRequestCode(requestCode, STR_DISP_NO_ACTIVE_HOST)
                            Log.d(TAG, "\t\t\tTLE KBANK DOWNLOAD : No any active host to download ")
                        }
                        SplashResult.TLE_KBANK_TEID_FILE_NOT_FOUND,
                        SplashResult.TLE_KBANK_TEID_READ_FILE_ERROR-> {
                            tleInitResult = 3
                            updateStatuswithRequestCode(requestCode, STR_DISP_MISSING_TEID_FILE)
                            Log.d(TAG, "\t\t\tTLE KBANK DOWNLOAD : TEID FILE NOT FOUND")
                        }
                        else -> {
                            updateStatuswithRequestCode(requestCode, STR_DISP_FAILED)
                            when (it.ret) {
                                SplashResult.TLE_KBANK_ERROR_ACQUIRER_NOT_FOUND -> {
                                    tleInitResult = 1
                                    Log.d(TAG,"\t\t\tTLE KBANK DOWNLOAD : TARGET ACQUIRER NOT FOUND")
                                }
                                else -> {
                                    tleInitResult = 4
                                    Log.d(TAG, "\t\t\tTLE KBANK DOWNLOAD : unspecified error")
                                }
                            }
                        }
                    }
                }
                setDisplayTextUi(null, null, "KBANK TLE download process was end")
                onActivityResult(requestCode, tleInitResult, null)
            }
        }
        loadKbankTLEProgressListener = object : SplashListener {
            override fun onEndProcess(activityResult: Instrumentation.ActivityResult) {
                // do nothing
            }

            override fun onUpdataUI(title: String, dispText: String) {
                setDisplayTextUi(null, title, dispText)
            }
        }
        kBankTlePrintReportTransEndListener  = object: ATransaction.TransEndListener {
            override fun onEnd(result: ActionResult?) {
                Log.d(
                    TAG,
                    "==================== KBank-TLE PrintReport - - - -> result = ${result?.ret}"
                )
                val requestCode : Int = REQ_INIT_KBANK_TLE_PRINT_REPORT
                updateStatuswithRequestCode(requestCode, STR_DISP_DONE)
                setDisplayTextUi(null, null, "KBANK TLE Report was printed")
                onActivityResult(requestCode, result!!.ret, null)
            }
        }
        updateSP200FirmwareActionEndListener   = object: AAction.ActionEndListener {
            override fun onEnd(action: AAction?, result: ActionResult?) {
                Log.d(TAG, "==================== SP200 upgrade firmware - - - -> result = ${result?.ret}")
                val requestCode : Int = REQ_INIT_UPDATE_SP200_FIRMWARE
                result?.let { tmpResult ->
                    when (tmpResult.ret) {
                        // Detected by SplashResult --> this result return by internal SplashActivity
                        SplashResult.SUCCESS -> {
                            setDisplayTextUi(null, null, "SP200 Upgrade Successful!!")
                            updateStatuswithRequestCode(requestCode, STR_DISP_DONE)
                        }
                        SplashResult.UPDATE_FIRMWARE_SP200_DISABLE -> {
                            setDisplayTextUi(null,null,"SP200 configuration was disabled")
                            updateStatuswithRequestCode(requestCode, STR_DISP_DISABLED)
                        }
                        SplashResult.DEVICE_NOT_SUPPORT_SP200 -> {
                            setDisplayTextUi(null,null,"Device : ${Build.MODEL.toString()} is unsupported model")
                            updateStatuswithRequestCode(requestCode, STR_DISP_DEVICE_NOT_SUPPORTED)
                        }

                        // Detected by TransResult --> this result return by ActionUpdateSP200
                        TransResult.SP200_FIRMWARE_UPTODATE -> {
                            setDisplayTextUi(null, null, "SP200 Firmware up-to-date")
                            updateStatuswithRequestCode(requestCode, STR_DISP_DONE)
                        }
                        TransResult.SP200_FIRMWARE_NO_AIP_FILE -> {
                            setDisplayTextUi(null,null,"missing AIP file")
                            updateStatuswithRequestCode(requestCode, STR_DISP_SP200_MISSING_AIP_FILE)
                        }
                        TransResult.ERR_SP200_UPDATE_INTERNAL_FAILED,
                        TransResult.ERR_SP200_UPDATE_FAILED -> {
                            setDisplayTextUi(null,null,"Failed during upgrade SP200 firmware")
                            updateStatuswithRequestCode(requestCode, STR_DISP_FAILED)
                        }
                        else-> {
                            setDisplayTextUi(null,null,"unknown error code : " + tmpResult.ret)
                            updateStatuswithRequestCode(requestCode, STR_DISP_FAILED)
                        }
                    }
                }?:run{
                    setDisplayTextUi(null, null, "No response result back")
                }
                onActivityResult(requestCode, result!!.ret, null)
            }
        }
        timerOnTickTimerListener = object : TickTimer.OnTickTimerListener {
            override fun onTick(leftTime: Long) {
                btn_ok?.let { setTexttoButton(it, "Enter Payment Feature\n(Automatic close dialog in $leftTime Sec.)", Color.parseColor("#FFFFFF"))}
            }

            override fun onFinish() {
                btn_ok?.let { setTexttoButton(it, "Enter Payment Feature\n(Automatic closing...)", Color.parseColor("#FFFFFF"))}
                ActivityStack.getInstance().popAll()
                openMainActivity(true)
//                Utils.restart()             // app restart and
            }

        }
        onEndTimer = TickTimer(timerOnTickTimerListener)
    }

    private fun updateStatuswithRequestCode(requestCode: Int, displayText: String) {
        updateStatuswithRequestCode(requestCode, displayText, null)
    }
    private fun updateStatuswithRequestCode(requestCode: Int, displayText: String, failedCode: String?) {
        var localTextView : TextView? = textviewMapper.get(requestCode)
        localTextView?.let { txv->
            this.runOnUiThread(Runnable {
                try {
                    var dispStr : String? = null
                    failedCode?.let {
                        dispStr = "$displayText ($failedCode)"
                    }?:run {
                        dispStr = displayText
                    }
                    if (dispStr?.length!! > 17) {
                        dispStr = dispStr?.substring(0,17)
                    }

                    txv.setText(dispStr)
                    if (displayText.equals(STR_DISP_DONE)) {
                        txv.setTextColor(Color.parseColor("#FFFFFF"))
                        txv.setBackgroundColor(Color.parseColor("#22B14C"))
                    } else if (displayText.contains(STR_DISP_FAILED)
                        || displayText.equals(STR_DISP_SERVICE_FAILED)
                        || displayText.equals(STR_DISP_MISSING_TEID_FILE)
                        || displayText.equals(STR_DISP_DEVICE_NOT_SUPPORTED)) {
                        txv.setTextColor(Color.parseColor("#FFFFFF"))
                        txv.setBackgroundColor(Color.parseColor("#FF0000"))
                    } else if (displayText.contains(STR_DISP_DISABLED)
                        || displayText.equals(STR_DISP_SERVICE_NOT_BIND)) {
                        txv.setTextColor(Color.parseColor("#333333"))
                        txv.setBackgroundColor(Color.parseColor("#D4D4D4"))
                    } else {
                        txv.setTextColor(Color.parseColor("#614A00"))
                        txv.setBackgroundColor(Color.parseColor("#FDEFB2"))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })
        }
    }

    fun openMainActivity(includeFinish: Boolean) {
        if (SettlementRegisterActivity.isEnableSettleMode()) {
            Log.d( TAG, "\t\t=======================================================================>> OPEN SETTLEMENT REGISTER BEFORE MAIN ACTIVITY <<" )
            openNewActivity(REQ_OPEN_MAINACTIVITY, SettlementRegisterActivity::class.java as Class<Any>)
        } else {
            Log.d( TAG, "\t\t=======================================================================>> OPEN MAIN ACTIVITY <<" )
            val settleAlarmProcess = SettleAlarmProcess()
            settleAlarmProcess.enableBroadcastReceiver(false, AutoSettleAlarmReceiver::class.java)
            settleAlarmProcess.enableBroadcastReceiver(false, ForceSettleAlarmReceiver::class.java)
            openNewActivity(REQ_OPEN_MAINACTIVITY, MainActivity::class.java as Class<Any>)
        }

        if (includeFinish) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scbIntent = Intent("th.co.bkkps.paxsdk.InitialScbReceiver.ACTION_INIT")
        scbIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        sendBroadcast(scbIntent)

        val amexIntent = Intent("th.co.bkkps.paxsdk.InitialReceiver.ACTION_INIT")
        amexIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        sendBroadcast(amexIntent)

        super.setTitleString("Parameter checking...")
        // check initialize state
        currentInitialState = getInitialstate()
        checkInitState()
    }

    override fun setTitleString(title: String?) {
        super.setTitleString("Parameter checking...")
    }


    fun setDisplayTextUi(firstText: String?, SecondText: String?, ThirdText: String?) {
        try {
            this.runOnUiThread(Runnable {
                firstText?.let { txv_status_version_current?.text = it }
                SecondText?.let { txv_status_version_incoming?.text = it }
                ThirdText?.let { txv_status_version_diff_state?.text = it }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun enableConstriantLayout () {
        // to enable/disable constraint layout
        this.runOnUiThread(Runnable {
            layout_disp_initial_states?.visibility = View.VISIBLE
        })
    }

    fun setTexttoButton (targBtn : Button, displayText: String , textColor: Int) {
        this.runOnUiThread(Runnable {
            targBtn?.setText(displayText)
            targBtn?.setTextColor(textColor)

        })
    }
    fun setButtonVisible (targBtn : Button, visibleStatus: Int) {
        this.runOnUiThread(Runnable {
            targBtn?.visibility = visibleStatus
        })
    }

    fun setStartTimer (timer : TickTimer?) {
        this.runOnUiThread(Runnable {
            timer?.let {
                it.start(30)
            }
        })
    }

    private fun checkInitState() {
        //Log.d(TAG, "\t\tCURRENT VERS. = ${BuildConfig.VERSION_NAME}")
        setDisplayTextUi("Application version : ${BuildConfig.VERSION_NAME}", "-", "Please wait...")
        when (currentInitialState) {
            InitialState.APP_UPGRADE_WITH_SETTLE_REQUIRED -> {
                // in case the application detect and found, It's still have pending settlement transaction
                txv_status_version_diff_state?.setText("process failed, Settlement required")
                Log.d(TAG, "\t\t----required----settlement-before-upgrade")
                DialogUtils.showErrMessage( this, "Initialization Process", "Please settlement before do application upgrade", dialogDisplayDismissListener, Constants.FAILED_DIALOG_SHOW_TIME )
            } else -> {
            setDisplayTextUi( null, "Pleas wait...", "Now, download param checking..." )
            downloadParamAndInitialProcesses()
        }
        }
    }

    private fun downloadParamAndInitialProcesses() {
        Log.d(TAG, "startDownloadParamAndInitialProcess on() " + Device.getTime(logDateTimeformat))
        if ((currentInitialState == InitialState.APP_FIRST_INITIAL || currentInitialState == InitialState.APP_EXISTS)
            && Controller.isFirstRun()) {
            // This support
            //    [1] FirstInitial
            //    [2] User open application and has SelfTest with failed status
            if (currentInitialState == InitialState.APP_FIRST_INITIAL) { Controller.set(
                Controller.IS_FIRST_INITIAL_NEEDED,
                true
            ) }


            this.runOnUiThread(Runnable { layout_disp_initial_states?.visibility = View.VISIBLE })
            setDisplayTextUi(null, "SelfTest executing", "Waiting for user allow SelfTest")
            startSelfTest(REQ_SELF_TEST)
        } else {
            // The isRequireDownloadParam will set = TRUE in 2 points
            //    [1] in DownloadParamReceiver ----> This event EDC will set InitState = 'DOWNLOAD_PARAM_ONLY' also.
            //    [2] in SplashActivity when parameter-file was downloaded and DownloadManager return success status back
            if (developerMode) {
                btn_ok?.text = "Skip initial process >>"
                btn_ok?.visibility =View.VISIBLE
            }

            // if user re-open application and passed the self-test stage
            if (!Controller.isFirstRun()) {
                // Display constraint layout
                enableConstriantLayout()
                // update status : SELFTEST = DONE
                updateStatuswithRequestCode(REQ_SELF_TEST, STR_DISP_DONE)
            }

            // this code section is use for detect the stage of the required process.
            if (Controller.isRequireDownloadParam()) {
                startDownloadParameterFromPAXStore(REQ_DOWNLOAD_PARAM)
            } else if (Controller.isFirstInitNeeded()) {
                startERCMRegister(REQ_INIT_ERCM_REGISTER)
            } else {
                // if all required flag was set to OFF, EDC will open mainactivity to let use payment feature
                openMainActivity(true)
            }
        }
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        Log.d(TAG, "\t\tonActivityResult : REQUEST CODE = " + requestCode.toString())
//        if (Controller.isFirstRun()) {
//            updateStatuswithRequestCode(REQ_SELF_TEST, STR_DISP_DONE)
//        }
//    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "\t\tonActivityResult : REQUEST CODE = " + requestCode.toString())
        when (requestCode) {
            REQ_OPEN_MAINACTIVITY -> {
                Log.d(TAG, "\t\t----MainActivity----End")

                // This ActivityResult : occur only EDC is back from MainActivity when received Broadcast command through DownloadParamReceiver,
                //                       It will set 'DOWNLOAD_PARAM_ONLY' state and set Controller 'IS_FIRST_DOWNLOAD_PARAM_NEEDED' = true
                //                       before open & entering to SplashActivity, after this activity got this flag EDC will toggle to download param process immediately
                Log.d(TAG, "\t\t----MainActivity----End")
                if (getInitialstate() == InitialState.DOWNLOAD_PARAM_ONLY && Controller.isRequireDownloadParam()) {
                    Log.d(TAG, "\t\t----Received::DownloadParamBroadcast")
                    setDisplayTextUi(
                        null,
                        "DOWNLOAD PARAM PROCESS",
                        "Prepare to download parameter..."
                    )
                    startDownloadParameterFromPAXStore(REQ_DOWNLOAD_PARAM)
                }
            }
            REQ_DOWNLOAD_PARAM -> {
                Log.d(TAG, "\t\t----DownloadParam----End")
                if (resultCode == SplashResult.SUCCESS) {
                    updateStatuswithRequestCode(requestCode, STR_DISP_DONE)
                    Log.d(TAG, "\t\t\t\t>> Result : Success")

                    // set downloadParam & Initial required flag
                    Controller.set(Controller.IS_FIRST_DOWNLOAD_PARAM_NEEDED, false)
                    Controller.set(Controller.IS_FIRST_INITIAL_NEEDED, true)

                    val commType = CommType.valueOf(SysParam.getInstance().get(SysParam.StringParam.COMM_TYPE))
                    if (commType.equals(CommType.LAN)) {
                        for (i in 1..7) {
                            setDisplayTextUi( null, "Communication perform", "Delaying for communication ready in ${7-i} sec.")
                            SystemClock.sleep(1000)
                        }
                    }

                    setDisplayTextUi( null, "ERCM Initial process", "EReceipt register, please wait...")
                    startERCMRegister(REQ_INIT_ERCM_REGISTER)
                } else if (resultCode == SplashResult.NO_PARAM_FILE) {
                    updateStatuswithRequestCode(requestCode, STR_DISP_WAITING_PARAM)
                    if (Controller.isFirstInitNeeded()) {
                        Controller.set(Controller.IS_FIRST_DOWNLOAD_PARAM_NEEDED, true)
                        Log.d(TAG, "\t\t\t\t>> Result : NoFile -- with Initial process required")
                        setDisplayTextUi( null, "INITIAL REQUIRED", "Waiting for parameter file from PAXSTORE" )
                    } else {
                        Controller.set(Controller.IS_FIRST_DOWNLOAD_PARAM_NEEDED, false)
                        Log.d(TAG, "\t\t\t\t>> Result : NoFile -- Start MainActivity")
                        openMainActivity(false)
                    }
                } else if (resultCode == SplashResult.DOWNLOAD_PARAM_HANDLE_SUCCESS_FAILED) {
                    updateStatuswithRequestCode(requestCode, STR_DISP_FAILED, resultCode.toString())
                    Log.d(TAG, "\t\t\t\t>> Result : Handle Success after download param failed")
                    DialogUtils.showErrMessage( this, "Download parameter Failed", "Unable to process param's file, Operation has been abort", dialogDisplayDismissListener, Constants.FAILED_DIALOG_SHOW_TIME)
                } else {
                    updateStatuswithRequestCode(requestCode, STR_DISP_FAILED, resultCode.toString())
                    Log.d(TAG, "\t\t\t\t>> Result : Failed")
                    DialogUtils.showErrMessage( this, "Download parameter Failed", "Error code : ${resultCode}", dialogDisplayDismissListener, Constants.FAILED_DIALOG_SHOW_TIME)
                }
            }
            REQ_SELF_TEST -> {
                Log.d(TAG, "\t\t----SelfTest----End")
                if (resultCode == Activity.RESULT_OK) {
                    Controller.set(Controller.IS_FIRST_DOWNLOAD_PARAM_NEEDED, true)
                    updateStatuswithRequestCode(requestCode, STR_DISP_DONE)
                    startDownloadParameterFromPAXStore(REQ_DOWNLOAD_PARAM)

                    if (developerMode) {
                        btn_ok?.text = "Skip initial process >>"
                        btn_ok?.visibility =View.VISIBLE
                    }

                    Controller.set(Controller.IS_FIRST_DOWNLOAD_PARAM_NEEDED, true)
                    updateStatuswithRequestCode(requestCode, STR_DISP_DONE)
                    //gotoNextProcess(requestCode, resultCode)      // EX001
                } else {
                    Controller.set(Controller.IS_FIRST_RUN, true)
                    updateStatuswithRequestCode(requestCode, STR_DISP_FAILED)
                    DialogUtils.showErrMessage(
                        this,
                        "Application Terminated",
                        "Failed on executing SelfTest",
                        null,
                        Constants.FAILED_DIALOG_SHOW_TIME
                    )
                }
            }
            REQ_INIT_ERCM_REGISTER -> {
                Log.d(TAG, "\t\t----ErcmInitial----End")
                setDisplayTextUi(null, "ERCM Initial process", "EReceipt register, please wait...")
                startPrintErcmReport(REQ_INIT_ERCM_PRINT_REPORT, resultCode)
            }
            REQ_INIT_ERCM_PRINT_REPORT -> {
                Log.d(TAG, "\t\t----PrintInitialErcmReport----End")
                setDisplayTextUi(
                    null,
                    "Dolfin permission",
                    "Waiting for user allow to access Dolfin application"
                )
                startAskPermissionDolfin(REQ_INIT_PERMISSION_DOLFIN)
            }
            REQ_INIT_PERMISSION_DOLFIN -> {
                Log.d(TAG, "\t\t----DolfinPermissionAsk----End")
                setDisplayTextUi(null, "Erase TLE Key", "Now, TLE key is start for erasing...")
                startEraseTerminalLineEncryptionKeys(REQ_INIT_EREASE_TLE_KEYS)
            }
            REQ_INIT_EREASE_TLE_KEYS -> {
                Log.d(TAG, "\t\t----EraseTleKey----End")
                setDisplayTextUi(null, "SCB-IPP TLE LOAD", "Now, TLE Load for SCB is checking...")
                startLoadScbTLE(REQ_INIT_LOAD_SCB_TLE)
            }
            REQ_INIT_LOAD_SCB_TLE -> {
                Log.d(TAG, "\t\t----DownloadScbTLE----End")
                setDisplayTextUi( null, "AMEX TLE LOAD", "Now, TLE Load for AMEX is downloading..." )
                startLoadAmexTLE(REQ_INIT_LOAD_AMEX_TLE)
            }
            REQ_INIT_SCBIPP_TRANS_API_CALLBACK -> {
                Log.d(TAG, "\t\t----ScbIPPCallBack----End")
                updateStatuswithRequestCode(requestCode, STR_DISP_DONE)
                setDisplayTextUi(null, null, "Waiting for user allow to access SCB-IPP application")
                afterScbTransApiCallBack(requestCode, resultCode, data)
            }
            REQ_INIT_LOAD_AMEX_TLE -> {
                Log.d(TAG, "\t\t----DownloadAmexTLE----End")
                setDisplayTextUi( null, "AMEX TPK LOAD", "Now, TPK Load for AMEX is downloading..." )
                startLoadAmexTPK(REQ_INIT_LOAD_AMEX_TPK)
            }
            REQ_INIT_LOAD_AMEX_TPK -> {
                Log.d(TAG, "\t\t----DownloadAmexTPK----End")
                setDisplayTextUi( null, "KBANK TLE LOAD", "Now, TLE Load for KBANK is downloading..." )
                startLoadTLEKBank(REQ_INIT_LOAD_KBANK_TLE)
            }
            REQ_INIT_LOAD_KBANK_TLE -> {
                Log.d(TAG, "\t\t----DownloadKBankTLE----End")
                setDisplayTextUi(null, null, "Now, Report is printing...")
                startPrintTleDownloadReport(REQ_INIT_KBANK_TLE_PRINT_REPORT, resultCode)
            }
            REQ_INIT_KBANK_TLE_PRINT_REPORT -> {
                Log.d(TAG, "\t\t----PrintKBankTLEReport----End")
                setDisplayTextUi(null,null, "LinkPOS Application is initializing...")
                startInitialLinkPosApp(REQ_INIT_LINKPOS_APPLICATION)
            }
            REQ_INIT_LINKPOS_APPLICATION -> {
                Log.d(TAG, "==================== InitLinkPosApp----End - - - -> result = $resultCode")
                if (resultCode != TransResult.SUCC) {
                    if (resultCode == TransResult.ERR_NO_LINKPOS_APP) {
                        setDisplayTextUi(null, null, "LinkPOS Application wasn't installed")
                        updateStatuswithRequestCode(requestCode, STR_DISP_SERVICE_NOT_BIND)
                    } else {
                        setDisplayTextUi(null, null, "Init LinkPOS application failed")
                        updateStatuswithRequestCode(requestCode, STR_DISP_FAILED)
                    }
                } else {
                    setDisplayTextUi(null, null, "Init LinkPOS application completed")
                    updateStatuswithRequestCode(requestCode, STR_DISP_DONE)
                }
                Log.d(TAG, "\t\t----InitLinkPosApp----End")
                setDisplayTextUi(null,"Firmware SP200 update","SP200 Firmware upgrade checking...")
                startUpdateSp200Firmware(REQ_INIT_UPDATE_SP200_FIRMWARE)
            }
            REQ_INIT_UPDATE_SP200_FIRMWARE -> {
                Log.d(TAG, "\t\t----UpdateSP200Firmware----End")
                setDisplayTextUi(null, "Initial Process, finalize", "Application Restarting...")
                if (resultCode == TransResult.SUCC
                    || resultCode == TransResult.SP200_FIRMWARE_UPTODATE
                    || resultCode == SplashResult.UPDATE_FIRMWARE_SP200_DISABLE
                    || resultCode == SplashResult.DEVICE_NOT_SUPPORT_SP200
                ) {

                    // reset all required controller
                    Controller.set(Controller.IS_FIRST_RUN, false)
                    Controller.set(Controller.IS_FIRST_DOWNLOAD_PARAM_NEEDED, false)
                    Controller.set(Controller.IS_FIRST_INITIAL_NEEDED, false)

                    // So after reset all required controller and restart
                    //      EDC may open MainActivity through restart process
                    //Device.beepOk()
                    //Utils.restart()
                    //btn_ok?.text = "Enter Payment Feature"
                    //btn_ok?.visibility=View.VISIBLE


                    setTexttoButton(btn_ok!!, "Enter Payment Feature", Color.parseColor("#FFFFFF"))
                    setButtonVisible(btn_ok!!, View.VISIBLE)
                    Device.beepOk()
                    setStartTimer(onEndTimer);
                } else {
                    if (resultCode == TransResult.ERR_SP200_UPDATE_INTERNAL_FAILED
                        && FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_SUPPORT_SP200)) {
                        setDisplayTextUi(null,"Firmware SP200 upgrade", getString(R.string.err_sp200_not_connect))
                    }
                }
            }
        }
    }

    fun deleteLog() {
//        val path = File.separator + "sdcard" + File.separator + "bpsedc.log"
//        var file : File = File(path)
//        file?.let {
//            if (file.isFile && !file.isDirectory && file.exists()){
//                file.delete();
//            }
//        }

        try {
            val path = File.separator + "sdcard"
            val dirs : File = File(path)
            dirs?.let {
                if (dirs.exists() && dirs.isDirectory) {
                    val fileList  = dirs.listFiles()
                    fileList?.let {
                        if (fileList.size>0) {
                            for (index in 0..fileList.size) {
                                if (!fileList[index].isDirectory && fileList[index].name.toString().contains("edc.log")) {
                                    fileList[index].delete()
                                }
                            }
                        }
                    }
                }
            }
        } catch (e:Exception) {
            e.printStackTrace()
        }
    }

    private var onEndTimer : TickTimer? = null;

    fun afterScbTransApiCallBack(requestCode: Int, result: Int, data: Intent?) {
        try {
            transAPI?.let{
                val apiResult : LoadTleMsg.Response? = (it.onResult(requestCode, result, data) as LoadTleMsg.Response)
                apiResult?.let { result ->
                    Component.incStanNo(result.stanNo)
                    Component.incTraceNo(result.voucherNo)
                    val isExecuteOnEnd = handleScbTleStatus(result.extraBundle)
                    if (isExecuteOnEnd) {
                        if (result.rspCode == TransResult.SUCC) {
                            executeListenerOnEnd(loadScbTLEKeyEndListener, SplashResult.SUCCESS)
                        } else {
                            executeListenerOnEnd(loadScbTLEKeyEndListener, SplashResult.TLE_SCB_TLE_DOWNLOAD_ERROR)
                        }
                    } else {
                        executeListenerOnEnd(
                            loadScbTLEKeyEndListener,
                            SplashResult.TLE_SCB_TLE_DOWNLOAD_ERROR
                        )
                        return
                    }
                } ?: run {
                    executeListenerOnEnd(loadScbTLEKeyEndListener,SplashResult.TLE_SCB_TRANS_API_RESULT_MISSING)
                    return
                }
            } ?: run {
                executeListenerOnEnd( loadScbTLEKeyEndListener,SplashResult.TLE_SCB_TRANS_API_FACTORY_MISSING)
                return
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }
    }

    fun executeListenerOnEnd(listener: AAction.ActionEndListener, result: Int) {
        (listener as AAction.ActionEndListener).onEnd(null, ActionResult(result, null))
    }

    fun openNewActivity(requestCode: Int, clz: Class<Any>) {
        Log.d(
            TAG,
            "\t\t\t----OpenNewActivity----RequestCode= ${requestCode}  | Class::${clz.toString()}"
        )
        enableBroadcastReceiver(true, DownloadParamReceiver::class.java as Class<*>)
        lateinit var intent : Intent
        intent = Intent(this, clz)
        this.startActivityForResult(intent, requestCode)
    }

    private fun startSelfTest(requestCode: Int){
        updateStatuswithRequestCode(requestCode, STR_DISP_PENDING)
        enableBroadcastReceiver(false, DownloadParamReceiver::class.java as Class<*>)
        val intent = Intent(this, SelfTestActivity::class.java)
        this.startActivityForResult(intent, requestCode)
    }

    private fun startDownloadParameterFromPAXStore(requestCode: Int) {
        // Clear parameter file / app_imgDir / app_sskDir
        CleanParameterDownloadDirectory(this)

        // preprocess update status
        //    in case user re-open application with InitialState == None
        if (!Controller.isFirstRun()) {
            updateStatuswithRequestCode(REQ_SELF_TEST, STR_DISP_DONE)
        }

        // open ConstriantLayout to show process status
        enableConstriantLayout()

        // enable BroadcastReceiver
        enableBroadcastReceiver(true, DownloadParamReceiver::class.java as Class<*>)

        // start Activity for DownloadParameter
        val intent = Intent(this, DownloadParameterActivity::class.java)
        this.startActivityForResult(intent, requestCode)
    }

    private fun isERCMReadyToInit() : Int {
        val param_Flag_enabled_ercm = FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE,false)
        val param_Flag_enabled_eSig = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_E_SIGNATURE,false)
        val param_init_bank_code : String? = FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_BANK_CODE,null)
        val param_init_merc_code : String? = FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_MERCHANT_CODE, null)
        val param_init_store_code : String? = FinancialApplication.getSysParam().get( SysParam.StringParam.VERIFONE_ERCM_STORE_CODE, null )
        val param_init_keys_vers : String? = FinancialApplication.getSysParam().get( SysParam.StringParam.VERIFONE_ERCM_KEK_VERSION, null )
        val param_ercm_kms_host : Acquirer? = FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_ERCM_KEY_MANAGEMENT_SERVICE)
        val param_ercm_rms_host : Acquirer? = FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_ERCM_RECEIPT_MANAGEMENT_SERVICE)


        if (param_Flag_enabled_ercm
            && param_Flag_enabled_eSig
            && param_init_bank_code != null
            && param_init_merc_code != null
            && param_init_store_code != null
            && param_init_keys_vers != null
            && param_ercm_kms_host != null
            && param_ercm_rms_host != null) {
            return TransResult.SUCC
        } else {
            if (!param_Flag_enabled_ercm) {
                return TransResult.ERCM_INITIAL_INFO_ERCM_DISABLED
            }
            else if (!param_Flag_enabled_eSig) {
                return TransResult.ERCM_INITIAL_INFO_ESIG_DISABLED
            }
            else if (param_init_bank_code==null) {
                return TransResult.ERCM_INITIAL_INFO_MISSING_BANK_CODE
            }
            else if (param_init_merc_code==null) {
                return TransResult.ERCM_INITIAL_INFO_MISSING_MERC_CODE
            }
            else if (param_init_store_code==null) {
                return TransResult.ERCM_INITIAL_INFO_MISSING_STORE_CODE
            }
            else if (param_init_keys_vers==null) {
                return TransResult.ERCM_INITIAL_INFO_MISSING_KEY_VERSION
            }
            else if (param_ercm_kms_host==null) {
                return TransResult.ERCM_INITIAL_INFO_HOST_KMS_DISABLED
            }
            else if (param_ercm_rms_host==null) {
                return TransResult.ERCM_INITIAL_INFO_HOST_RMS_DISABLED
            } else {
                return TransResult.ERCM_INITIAL_INFO_NOT_READY
            }
        }
    }


    private fun startERCMRegister(requestCode: Int) {
        // preprocess update status
        //    in case user re-open application with InitialState == None
        if (currentInitialState == InitialState.NONE) {
            updateStatuswithRequestCode(REQ_SELF_TEST, STR_DISP_DONE)
            updateStatuswithRequestCode(REQ_DOWNLOAD_PARAM, STR_DISP_DONE)
        }

        updateStatuswithRequestCode(requestCode, STR_DISP_PENDING)
        val ercmReadyResult : Int = isERCMReadyToInit()
        if (ercmReadyResult != TransResult.SUCC) {
            Log.d(TAG, "\t\tERCM ready checking result = ${ercmReadyResult}")
            ercmInitialEndListener.onEnd( null, ActionResult( ercmReadyResult, null ) )
            return
        }

//        val delSessionResult = FinancialApplication.getEReceiptDataDbHelper().deleteErmSessionKey();
//        if (delSessionResult) {
            val ercmInitial = ActionEReceipt {
                //(it as ActionEReceipt).setExtraParam(true);
                (it as ActionEReceipt).setParam(ActionEReceipt.eReceiptMode.INIT_TERMINAL, this, FinancialApplication.getDownloadManager().sn, null)
            }
            ercmInitial.setEndListener(ercmInitialEndListener);
            ercmInitial.setEndProgressListener(loadKbankTLEProgressListener);
            ercmInitial.execute()
            Log.d(TAG, "\t\tERCM--initialization--begin")
//        } else {
//            ercmInitialEndListener.onEnd(null, ActionResult( SplashResult.ERCM_STATUS_CLAER_SESSIONKEY_ERROR, EReceiptStatusTrans.ercmInitialResult.CLEAR_SESSIONKEY_ERROR))
//            Log.d(TAG, "\t\tERCM--ClearSessionKey--failed")
//        }
    }

    private fun startPrintErcmReport(requestCode: Int, initialResult: Int) {
        Log.d(TAG, "\t\t----ErcmPrintReport----start")
        updateStatuswithRequestCode(requestCode, STR_DISP_PENDING)
        var ERCMReportStrArray = GenerateERCMStatus(initialResult)
        var ercmReportPrinting = ActionPrintTransMessage{
            (it as ActionPrintTransMessage).setParam(
                this,
                ERCMReportStrArray,
                null
            )
        }
        ercmReportPrinting.setEndListener(ercmReportPrintActionEndListener)
        ercmReportPrinting.execute()
    }

    private fun startPrintControlLimitReport() {
        Log.d(TAG, "\t\t----ControlLimitReport----start")
        var CtrlLimitReportStrArray = GenerateControlLimit()
        var ctrlLimitReportPrinting = ActionPrintTransMessage{
            (it as ActionPrintTransMessage).setParam( this, CtrlLimitReportStrArray, null )
        }
        ctrlLimitReportPrinting.setEndListener(controlLimitReportPrintingListener)
        ctrlLimitReportPrinting.execute()
    }

    private fun startAskPermissionDolfin(requestCode: Int) {
        Log.d(TAG, "\t\t----DolfinPermission----start")
        updateStatuswithRequestCode(requestCode, STR_DISP_PENDING)
        val isDolfinServiceBinded : Boolean = DolfinApi.getInstance().dolfinServiceBinded
        if (!isDolfinServiceBinded) {
            Log.d(TAG, "\t\t\t[Dolfin] service isnot binded")
            dolfinPermissionActionEndListener.onEnd(null, ActionResult(SplashResult.DOLFIN_PERMISSION_SERVICE_NOT_BINDED,null))
            return
        }

        val acquirer : Acquirer? = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_DOLFIN)
        acquirer?.let {
            if (it.isEnable) {
                try {
                    val dolfinPermissionAsk = ActionDolfinSetConfig(AAction.ActionStartListener {
                        (it as ActionDolfinSetConfig).setParam(this)
                    })
                    dolfinPermissionAsk.setEndListener(dolfinPermissionActionEndListener)
                    dolfinPermissionAsk.execute()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e(TAG, "\t\t\t[Dolfin] failed to start ActionDolfinSetConfig")
                    dolfinPermissionActionEndListener.onEnd(null, ActionResult(SplashResult.DOLFIN_PERMISSION_FAILED_TO_START_SERVICE,null))
                    return
                }
            } else {
                Log.d(TAG, "\t\t\t[Dolfin] acqirer was disabled")
                dolfinPermissionActionEndListener.onEnd(null, ActionResult(SplashResult.DOLFIN_PERMISSION_ACQUIRER_DISABLED,null))
                return
            }
        } ?: run {
            Log.d(TAG, "\t\t\t[Dolfin] acqirer was not found")
            dolfinPermissionActionEndListener.onEnd( null, ActionResult( SplashResult.DOLFIN_PERMISSION_ACQUIRER_NOT_FOUND, null))
            return
        }
    }

    private fun startEraseTerminalLineEncryptionKeys(requestCode: Int) {
        Log.d(TAG, "\t\t----EraseTleKeys----start")
        val acqList : List<Acquirer>? = FinancialApplication.getAcqManager().findEnableAcquirer()
        var returnCode = SplashResult.TLE_ERASE_KEY_FAILED

        acqList?.let {
            for (acq:Acquirer in it) {
                acq.tmk = null
                acq.twk = null
                FinancialApplication.getAcqManager().updateAcquirer(acq)
            }

            if (Device.eraseKeys()) {
                returnCode = SplashResult.SUCCESS
            }
        }

        eraseTLEKeyListener.onEndProcess(Instrumentation.ActivityResult(returnCode, null))
    }

    private fun startLoadScbTLE(requestCode: Int) {
        Log.d(TAG, "\t\t----LoadScbTLE----start")
        updateStatuswithRequestCode(requestCode, STR_DISP_PENDING)

        var jsonTE :String? = null

        // check SCB-IPP service is installed
        val isScbIppAppInstalled = ScbIppService.isSCBInstalled(this)
        if (!isScbIppAppInstalled) {
            loadScbTLEKeyEndListener.onEnd(null, ActionResult(SplashResult.TLE_SCB_SERVICE_NOT_BINDED, null))
            return
        }

        // check SCB-IPP/SCB-REDEEM acquirer exists?
        val acqSupportTle = ScbUtil.getScbActiveAcquirer()
        if (acqSupportTle == null) {
            loadScbTLEKeyEndListener.onEnd(null, ActionResult(SplashResult.TLE_SCB_HOST_NOT_FOUND,null))
            return
        }

        if (acqSupportTle.isNotEmpty()) {
            acqSupportTle.forEach { name ->
                val acq = FinancialApplication.getAcqManager().findActiveAcquirer(name)
                if (!acq.isEnableTle) {
                    loadScbTLEKeyEndListener.onEnd(null, ActionResult(SplashResult.TLE_SCB_HOST_DISABLE_TLE,null))
                    return
                }
            } ?: run {
                loadScbTLEKeyEndListener.onEnd(
                    null, ActionResult(
                        SplashResult.TLE_SCB_TEID_FILE_NOT_FOUND,
                        null
                    )
                )
                return
            }
        }

        // check Has TEID file path?
        val tlePathFile : String? = FinancialApplication.getSysParam().get( SysParam.StringParam.TLE_PARAMETER_FILE_PATH, null )
        tlePathFile?.let{
            jsonTE = readTeidFromFile(it)
        }?: run {
            loadScbTLEKeyEndListener.onEnd(null, ActionResult(SplashResult.TLE_SCB_TEID_FILE_NOT_FOUND,null))
            return
        }

        // check Json TEID read contain null data or not?
        jsonTE?.let {
            transAPI = TransAPIFactory.createTransAPI()
            transAPI?.let{
                val scbUpdateParam = ActionScbUpdateParam(AAction.ActionStartListener {
                    (it as ActionScbUpdateParam).setParam(this)
                })
                scbUpdateParam.setEndListener(AAction.ActionEndListener { action, result ->
                    if (result.ret == TransResult.SUCC) {
                        val reqTLE = LoadTleMsg.Request()
                        reqTLE.jsonTe = jsonTE
                        reqTLE.selectAcq = acqSupportTle
                        try {
                            transAPI!!.startTrans(this, reqTLE)
                            loadScbTLEKeyEndListener.onEnd(null, ActionResult(SplashResult.WAITING_TRANS_CALLBACK,null))
                        } catch (e: Exception) {
                            loadScbTLEKeyEndListener.onEnd(null, ActionResult(SplashResult.TLE_SCB_TRANS_API_UNABLE_TO_START,null))
                        }
                    } else {
                        loadScbTLEKeyEndListener.onEnd(null, ActionResult(SplashResult.TLE_SCB_UPDATE_PARAM_FAILED,null))
                    }
                })
                scbUpdateParam.execute()
            }?:run {
                loadScbTLEKeyEndListener.onEnd(null, ActionResult(SplashResult.TLE_SCB_TRANS_API_FACTORY_MISSING,null))
                return
            }
        }?:run{
            loadScbTLEKeyEndListener.onEnd(null, ActionResult(SplashResult.TLE_SCB_TEID_WAS_EMPTY,null))
            return
        }
    }

    fun readTeidFromFile(filePath: String) : String? {
        try {
            val localFile : File? = File(filePath)
            localFile?.let {
                if (it.exists() && it.isFile) {
                    val mapper = ObjectMapper()
                    return mapper.readTree(it).toString()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }


    private fun startLoadTLEKBank(requestCode: Int) {
        Log.d(TAG, "\t\t----LoadKBankTLE----start")
        updateStatuswithRequestCode(requestCode, STR_DISP_PENDING)

        var jsonTE : String? = null

        // check KBANK's TLE host is exist
        val enabledAcquirer : List<Acquirer>? = FinancialApplication.getAcqManager().findEnableAcquirer()
        enabledAcquirer?:run{
            loadKbankTLETransEndListener.onEnd(ActionResult(SplashResult.TLE_KBANK_ACTIVE_ACQUIRER_NOT_FOUND,null))
            return
        }

        // check Acquirer List is empty
        if (enabledAcquirer.size == 0 ) {
            loadKbankTLETransEndListener.onEnd(ActionResult(SplashResult.TLE_KBANK_ERROR_ACQUIRER_NOT_FOUND,null))
            return
        }

        val path : String? = FinancialApplication.getSysParam().get( SysParam.StringParam.TLE_PARAMETER_FILE_PATH,null )
        path?.let{
            jsonTE = readTeidFromFile(it)
            jsonTE?:run{
                loadKbankTLETransEndListener.onEnd(ActionResult(SplashResult.TLE_KBANK_TEID_READ_FILE_ERROR,null))
                return
            }
        }?:run{
            loadKbankTLETransEndListener.onEnd(ActionResult(SplashResult.TLE_KBANK_TEID_FILE_NOT_FOUND, null))
            return
        }

        Log.d(TAG, "\t\t\tLoadTMKTrans----start")
        val loadTMKtrans  = LoadTLETrans(this, loadKbankTLETransEndListener, LoadTLETrans.Mode.DownloadTMK, true)
        loadTMKtrans.setSplashEndListener(loadKbankTLEProgressListener);
        loadTMKtrans.execute()
    }


    private fun startPrintTleDownloadReport(requestCode: Int, initialTleResult: Int) {
        Log.d(TAG, "\t\t----PrintTLEDownloadReport----start")
        updateStatuswithRequestCode(requestCode, STR_DISP_PENDING)
        val tleStatusReport : TleStatusTrans = TleStatusTrans( this, kBankTlePrintReportTransEndListener, true,initialTleResult)
        tleStatusReport.execute()
    }

    private fun startInitialLinkPosApp(requestCode: Int) {
        Log.d(TAG, "\t\t----InitLinkPosApp----start")
        updateStatuswithRequestCode(requestCode, STR_DISP_PENDING)
        if (LinkPOSApi.isPackageInstalled(packageManager)) {
            val intent = Intent(this, LinkPosAppInitialActivity::class.java)
            this.startActivityForResult(intent, REQ_INIT_LINKPOS_APPLICATION)
        } else {
            onActivityResult(requestCode, TransResult.ERR_NO_LINKPOS_APP, null);
        }
    }

    private fun startUpdateSp200Firmware(requestCode: Int) {
        Log.d(TAG, "\t\t----UpdateSP200FirmwareProcess----start")
        updateStatuswithRequestCode(requestCode, STR_DISP_PENDING)
        val isSP200Enabled = SP200_serialAPI.getInstance().isSp200Enable()
        val isUsePinPadDevice = !isModelUnableConnectWithPinPad()
        if (isSP200Enabled) {
            if (isUsePinPadDevice) {
                try {
                    var updateFwSp200 = ActionUpdateSp200(AAction.ActionStartListener {
                        (it as ActionUpdateSp200).setParam(this@SplashActivity)
                    })
                    updateFwSp200.setEndListener(updateSP200FirmwareActionEndListener)
                    updateFwSp200.execute()
                } catch (e: Exception) {
                    updateSP200FirmwareActionEndListener.onEnd(null, ActionResult(SplashResult.UPDATE_FIRMWARE_SP200_UPDATE_FAILED,null))
                }
            } else {
                updateSP200FirmwareActionEndListener.onEnd(null, ActionResult(SplashResult.DEVICE_NOT_SUPPORT_SP200,null))
            }
        } else {
            updateSP200FirmwareActionEndListener.onEnd(null, ActionResult(SplashResult.UPDATE_FIRMWARE_SP200_DISABLE,null))
        }
    }

    private fun isModelUnableConnectWithPinPad(): Boolean {
        val FatoryManufacturer = "PAX"
        val deviceModelsList : List<String>? = Arrays.asList("A920")
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL

        if (deviceModelsList==null || deviceModelsList.isEmpty() || deviceModelsList.size==0) {
            return false
        }

        if (manufacturer==null || manufacturer.isEmpty() || manufacturer.isBlank()) {
            return false
        }

        if (model==null || model.isEmpty() || model.isBlank()) {
            return false
        }

        if (!manufacturer.equals(FatoryManufacturer)) {
            return false
        }

        for (m: String in deviceModelsList) {
            if (model.equals(m)) {
                return true
            }
        }
        return false
    }


    private fun handleTransContext(clz: Class<Any>) {
        ActivityStack.getInstance()?.let {
            ActivityStack.getInstance().popTo(clz)
        }

        TransContext.getInstance()?.let {
            TransContext.getInstance().getCurrentAction()?.let {
                TransContext.getInstance().currentAction.isFinished = true
            }
        }
    }

    private fun startBackUpProcess(){
        try {
            Log.d(TAG, "startBackUpProcess on() " + Device.getTime(logDateTimeformat))

        } catch (e: Exception) {
            e.printStackTrace()

        }
    }

    private fun GenerateControlLimit() : Array<String?>? {
        val contentList: MutableList<String> = ArrayList()
        var localControlLimitStr: Array<String?>? = null
        contentList.add("===========================")
        contentList.add("    CONTROL LIMIT REPORT ")
        contentList.add("===========================")
        if (ControlLimitUtils.getEnabledControlLimitHostCount() > 0) {

            val acqList = ControlLimitUtils.getEnabledControlLimitHosts()
            val acqCount = acqList.size
            for (i in 0..acqList.size-1) {
                val hostNo = Utils.getStringPadding(i+1, 2, " ", Convert.EPaddingPosition.PADDING_LEFT)
                val hostName = acqList.get(i).name
                val optionalPrint = if (acqList.get(i).enablePhoneNumberInput) "(Phone enabled)" else ""
                contentList.add(" $hostNo. $hostName ${optionalPrint}")
            }
            contentList.add("===========================")
            contentList.add(" TOTAL : $acqCount HOSTS")
            contentList.add("===========================")

            localControlLimitStr = arrayOfNulls(contentList.size )
            var index = 0
            for (str in contentList) {
                localControlLimitStr[index++] = str
            }
        } else {
            contentList.add(" ")
            contentList.add(" HOST WAS NOT FOUND !!")
            contentList.add(" ")
            contentList.add("===========================")
        }

        return localControlLimitStr
    }

    private fun GenerateERCMStatus(initResult: Int): Array<String?>? {

        var previousErcmInitialStatus : EReceiptStatusTrans.ercmInitialResult = ercmInitialResult.NONE
        when (initResult){
            1 -> previousErcmInitialStatus = ercmInitialResult.CLEAR_SESSIONKEY_ERROR
            2 -> previousErcmInitialStatus = ercmInitialResult.INIT_FAILED
            3 -> previousErcmInitialStatus = ercmInitialResult.INIT_SUCCESS
        }

        // During Fail on download
        //if (previousErcmInitialStatus != ercmInitialResult.INIT_SUCCESS) { return GenerateFailedPrint();}
        val acquirerList = FinancialApplication.getAcqManager().findAllAcquirers()
        val HeaderList: MutableList<String> = ArrayList()
        val printList: MutableList<String> = ArrayList()
        val FooterList: MutableList<String> = ArrayList()
        var hostnumber = 0
        var countRegister = 0
        var countUnregister = 0
        var countDisable = 0
        var countUnsupported = 0
        val initiated_host: MutableList<String> = ArrayList()
        val uninitiated_host: MutableList<String> = ArrayList()
        val disabled_host: MutableList<String> = ArrayList()
        val unsupported_host: MutableList<String> = ArrayList()
        HeaderList.add("===========================")
        HeaderList.add("          ERCM INITIAL REPORT ")
        HeaderList.add("===========================")
        HeaderList.add(" ERCM CONFIG")
        HeaderList.add("  > EDC S/N  : " + nullCast(FinancialApplication.getSysParam()[SysParam.StringParam.VERIFONE_ERCM_TERMINAL_SERIALNUMBER]))
        HeaderList.add("  > ERCM FLAG ENABLED   : " + FinancialApplication.getSysParam()[SysParam.BooleanParam.VF_ERCM_ENABLE])
        HeaderList.add("  > E-SIGNATURE ENABLED : " + FinancialApplication.getSysParam()[SysParam.BooleanParam.EDC_ENABLE_E_SIGNATURE])
        HeaderList.add("  > BANK  : " + nullCast(FinancialApplication.getSysParam()[SysParam.StringParam.VERIFONE_ERCM_BANK_CODE]))
        HeaderList.add("  > MERC  : " + nullCast(FinancialApplication.getSysParam()[SysParam.StringParam.VERIFONE_ERCM_MERCHANT_CODE]))
        HeaderList.add("  > STORE : " + nullCast(FinancialApplication.getSysParam()[SysParam.StringParam.VERIFONE_ERCM_STORE_CODE]))
        HeaderList.add("  > KEY VER. : " + nullCast(FinancialApplication.getSysParam()[SysParam.StringParam.VERIFONE_ERCM_KEK_VERSION]))
        HeaderList.add("- - - - - - - - - - - - - - - - - - - - - -")
        HeaderList.add(" PRINTING CONFIG")
        HeaderList.add("  > PRINT AFTER TRANS : " + if (FinancialApplication.getSysParam()[SysParam.BooleanParam.VF_ERCM_ENABLE_PRINT_AFTER_TXN]) "ON" else "OFF")
        if (FinancialApplication.getSysParam()[SysParam.BooleanParam.VF_ERCM_ENABLE_PRINT_AFTER_TXN]) {
            HeaderList.add("      1. SUCCESS = " + getSlipNumbDesc(FinancialApplication.getSysParam()[SysParam.NumberParam.VF_ERCM_NO_OF_SLIP]))
            HeaderList.add("      2. FAILED = " + getSlipNumbDesc(FinancialApplication.getSysParam()[SysParam.NumberParam.VF_ERCM_NO_OF_SLIP_UNABLE_UPLOAD]))
        }
        HeaderList.add("  > NEXT TRANS UPLOAD : " + if (FinancialApplication.getSysParam()[SysParam.BooleanParam.VF_ERCM_ENABLE_NEXT_TRANS_UPLOAD]) "ON" else "OFF")
        HeaderList.add("  > PRINT ON PRESETTLE FAILED")
        HeaderList.add("       = " + if (FinancialApplication.getSysParam()[SysParam.BooleanParam.VF_ERCM_ENABLE_FORCE_SETTLE_PRINT_ALL_TRANS]) "FORCE PRINT ALL RECORDS" else "NEVER PRINT RECORDS ONLY")
        HeaderList.add("-----------------------------------")
        initiated_host.add("1 REGISTERED HOST")

        //uninitiated_host.add("----------------------------------");
        uninitiated_host.add("2 PENDING-REGISTER HOST")

        //disabled_host.add(   "----------------------------------");
        disabled_host.add("3 DISABLED UPLOAD HOST")

        //unsupported_host.add("----------------------------------");
        unsupported_host.add("4 UNSUPPORTED ERCM HOST")
        if (acquirerList.size > 0) {
            for (acquirer in acquirerList) {
                if (acquirer.name == Constants.ACQ_ERCM_KEY_MANAGEMENT_SERVICE || acquirer.name == Constants.ACQ_ERCM_RECEIPT_MANAGEMENT_SERVICE) {
                    // Skip on ERCM_KMS // ERCM_RMS
                    break
                }
                var result: EReceiptLogoMapping?
                if (acquirer.isEnable) {
                    hostnumber += 1
                    if (acquirer.enableUploadERM) {
                        result = try {
                            FinancialApplication.getEReceiptDataDbHelper()
                                .FindSessionKeyByAcquirerIndex(acquirer.id.toString())
                        } catch (e: java.lang.Exception) {
                            null
                        }
                        if (result != null) {
                            // Able to intialize
                            countRegister += 1
                            printList.add(
                                String.format(
                                    " %s. %s = %s",
                                    hostnumber,
                                    acquirer.name,
                                    "true"
                                )
                            )
                            initiated_host.add(
                                String.format(
                                    "   1.%s %s (NII=%s)",
                                    countRegister,
                                    acquirer.name,
                                    acquirer.nii
                                )
                            )
                        } else {
                            // Unable to initalize ERCM
                            countUnregister += 1
                            printList.add(
                                String.format(
                                    " %s. %s = %s",
                                    hostnumber,
                                    acquirer.name,
                                    "false"
                                )
                            )
                            uninitiated_host.add(
                                String.format(
                                    "   2.%s %s (NII=%s)",
                                    countUnregister,
                                    acquirer.name,
                                    acquirer.nii
                                )
                            )
                        }
                    } else {
                        // Disabled upload Acquirer
                        countDisable += 1
                        printList.add(
                            String.format(
                                " %s. %s = %s",
                                hostnumber,
                                acquirer.name,
                                "disabled upload"
                            )
                        )
                        disabled_host.add(
                            String.format(
                                "   3.%s %s (NII=%s)",
                                countDisable,
                                acquirer.name,
                                acquirer.nii
                            )
                        )
                    }
                } else {
                    countUnsupported += 1
                    printList.add(
                        String.format(
                            " %s. %s = %s",
                            hostnumber,
                            acquirer.name,
                            "unsupported"
                        )
                    )
                    unsupported_host.add(
                        String.format(
                            "   4.%s %s (NII=%s)",
                            countUnsupported,
                            acquirer.name,
                            acquirer.nii
                        )
                    )
                }
            }
        }
        if (countRegister == 0) {
            initiated_host.add("         ** No host record **\n")
        }
        if (countUnregister == 0) {
            uninitiated_host.add("         ** No host record **\n")
        }
        if (countDisable == 0) {
            disabled_host.add("         ** No host record **\n")
        }
        if (countUnsupported == 0) {
            unsupported_host.add("         ** No host record **\n")
        }
        var StatusStr: String? = null
        if (previousErcmInitialStatus == EReceiptStatusTrans.ercmInitialResult.INIT_SUCCESS) {
            StatusStr =
                if (FinancialApplication.getSysParam()[SysParam.StringParam.VERIFONE_ERCM_TERMINAL_INTIATED] == null) "NOT READY" else "READY"
        } else if (previousErcmInitialStatus == EReceiptStatusTrans.ercmInitialResult.INIT_FAILED) {
            StatusStr = "INITIALIZE FAILED"
        } else if (previousErcmInitialStatus == EReceiptStatusTrans.ercmInitialResult.CLEAR_SESSIONKEY_ERROR) {
            StatusStr = "CLEAR SESSION KEY FAILED"
        }
        FooterList.add("===========================")
        FooterList.add(" ERCM STATUS : $StatusStr")
        FooterList.add(
            "                DATE : " + getDateTime(
                FinancialApplication.getSysParam()[SysParam.StringParam.VERIFONE_ERCM_TERMINAL_INTIATED],
                DateTimeType.DATE
            )
        )
        FooterList.add(
            "                TIME : " + getDateTime(
                FinancialApplication.getSysParam()[SysParam.StringParam.VERIFONE_ERCM_TERMINAL_INTIATED],
                DateTimeType.TIME
            )
        )
        FooterList.add("    - - - - - - - - - - - - - - - - - - - - ")
        FooterList.add("    INITIATED = " + countRegister + " HOST" + if (countRegister > 1) "S" else "")
        if (countUnregister > 0) {
            FooterList.add("    PENDING INITAL = " + countUnregister + " HOST" + if (countUnregister > 1) "S" else "")
        }
        if (countDisable > 0) {
            FooterList.add("    DISABLED UPLOAD = " + countDisable + " HOST" + if (countDisable > 1) "S" else "")
        }
        //FooterList.add("    UNSUPPORTED ERCM = " + countUnsupported + " HOST" + ((countUnsupported >1) ? "S" :""));
        FooterList.add("===========================")
        FooterList.add(" TOTAL =" + hostnumber + " HOST" + if (hostnumber > 1) "S" else "")
        FooterList.add("===========================")
        var local_ercmStatus: Array<String?>? = null
        if (hostnumber > 0) {
            //local_ercmStatus = new String[HeaderList.size() + printList.size() + FooterList.size() +1];
            local_ercmStatus =
                arrayOfNulls(HeaderList.size + printList.size + FooterList.size + 1 + initiated_host.size + uninitiated_host.size + disabled_host.size + unsupported_host.size)
            var index = 0
            for (str in HeaderList) {
                local_ercmStatus[index++] = str
            }
            //for (String str: printList) { local_ercmStatus[index++] = str; }
            for (str in initiated_host) {
                local_ercmStatus[index++] = str
            }
            for (str in uninitiated_host) {
                local_ercmStatus[index++] = str
            }
            for (str in disabled_host) {
                local_ercmStatus[index++] = str
            }
            //for (String str: unsupported_host) { local_ercmStatus[index++] = str; }
            for (str in FooterList) {
                local_ercmStatus[index++] = str
            }
        } else {
            local_ercmStatus = arrayOf()
        }
        return local_ercmStatus
    }

    private fun nullCast(exString: String?): String? {
        return exString ?: " ** missing ** "
    }

    private enum class DateTimeType {
        DATE, TIME
    }

    private fun getDateTime(exDateTime: String?, type: DateTimeType): String? {
        if (exDateTime == null) {
            return nullCast(exDateTime)
        }
        return if (type == DateTimeType.DATE) {
            String.format(
                "%s/%s/%s",
                exDateTime.substring(6, 8),
                exDateTime.substring(4, 6),
                exDateTime.substring(0, 4)
            )
        } else {
            String.format(
                "%s:%s:%s",
                exDateTime.substring(8, 10),
                exDateTime.substring(10, 12),
                exDateTime.substring(12, 14)
            )
        }
    }

    private fun getSlipNumbDesc(slipNumb: Int): String? {
        return when (slipNumb) {
            0 -> "No printing required"
            1 -> "MERC Copy only"
            2 -> "MERC + CUST Copy"
            3 -> "CUST Copy only"
            else -> ""
        }
    }

    private fun handleScbTleStatus(bundle: Bundle?): Boolean {
        bundle?.let { b ->
            var tleStatus: Int = -1
            if (b.containsKey(th.co.bkkps.bpsapi.Constants.SCB_TLE_STATUS)) {
                tleStatus = b.getInt(th.co.bkkps.bpsapi.Constants.SCB_TLE_STATUS, -1)
            }

            val scbIppAcq = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_IPP)
            val scbRedeemAcq = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_REDEEM)
            if (scbIppAcq != null && scbRedeemAcq != null) {
                scbIppAcq.tmk = null; scbIppAcq.twk = null
                scbRedeemAcq.tmk = null; scbRedeemAcq.twk = null
                when (tleStatus) {
                    th.co.bkkps.bpsapi.Constants.SCB_TLE_RESULT_IPP_SUCCESS -> {
                        scbIppAcq.tmk = "Y"; scbIppAcq.twk = "Y"
                    }
                    th.co.bkkps.bpsapi.Constants.SCB_TLE_RESULT_OLS_SUCCESS -> {
                        scbRedeemAcq.tmk = "Y"; scbRedeemAcq.twk = "Y"
                    }
                    th.co.bkkps.bpsapi.Constants.SCB_TLE_RESULT_ALL_SUCCESS -> {
                        scbIppAcq.tmk = "Y"; scbIppAcq.twk = "Y"
                        scbRedeemAcq.tmk = "Y"; scbRedeemAcq.twk = "Y"
                    }
                    else -> {
                        executeListenerOnEnd(loadScbTLEKeyEndListener, SplashResult.TLE_SCB_TLE_DOWNLOAD_ERROR)
                        return false
                    }
                }
                FinancialApplication.getAcqManager().updateAcquirer(scbIppAcq)
                FinancialApplication.getAcqManager().updateAcquirer(scbRedeemAcq)
                return true
            } else {
                executeListenerOnEnd(
                    loadScbTLEKeyEndListener,
                    SplashResult.TLE_SCB_TLE_UPDATE_ACQUIRER_NOT_FOUND
                )
                return false
            }
        } ?: run {
            executeListenerOnEnd(loadScbTLEKeyEndListener,SplashResult.TLE_SCB_TLE_DOWNLOAD_ERROR)
            return false
        }
    }

    private fun startLoadAmexTLE(requestCode: Int) {
        Log.d(TAG, "\t\t----LoadAmexTLE----start")
        updateStatuswithRequestCode(requestCode, STR_DISP_PENDING)

        var jsonTE :String? = null

        // check Amex service is installed
        val isAppInstalled = AmexTransService.isAmexAppInstalled(this)
        if (!isAppInstalled) {
            loadAmexTLEKeyEndListener.onEnd(null, ActionResult(SplashResult.TLE_AMEX_SERVICE_NOT_BINDED, null))
            return
        }

        // check AMEX acquirer exists?
        val acq = FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_AMEX)
        acq?.let {
            if (!it.isEnableTle) {
                loadAmexTLEKeyEndListener.onEnd(null, ActionResult(SplashResult.TLE_AMEX_HOST_DISABLE_TLE,null))
                return
            }
        } ?: run {
            loadAmexTLEKeyEndListener.onEnd(null, ActionResult(SplashResult.TLE_AMEX_HOST_NOT_FOUND,null))
            return
        }

        // check Has TEID file path?
        val tlePathFile : String? = FinancialApplication.getSysParam().get( SysParam.StringParam.TLE_PARAMETER_FILE_PATH, null )
        tlePathFile?.let{
            jsonTE = readTeidFromFile(it)
        }?: run {
            loadAmexTLEKeyEndListener.onEnd(null, ActionResult(SplashResult.TLE_AMEX_TEID_FILE_NOT_FOUND,null))
            return
        }

        // check Json TEID read contain null data or not?
        jsonTE?.let { teId ->
            val actionAmexLoadLogOnTle = ActionAmexLoadLogOnTle(AAction.ActionStartListener {
                (it as ActionAmexLoadLogOnTle).setParam(this, teId)
            })
            actionAmexLoadLogOnTle.setEndListener(loadAmexTLEKeyEndListener)
            actionAmexLoadLogOnTle.execute()
        } ?: run{
            loadAmexTLEKeyEndListener.onEnd(null, ActionResult(SplashResult.TLE_AMEX_TEID_WAS_EMPTY,null))
            return
        }
    }

    private fun startLoadAmexTPK(requestCode: Int) {
        Log.d(TAG, "\t\t----LoadAmexTPK----start")
        updateStatuswithRequestCode(requestCode, STR_DISP_PENDING)

        // check Amex service is installed
        val isAppInstalled = AmexTransService.isAmexAppInstalled(this)
        if (!isAppInstalled) {
            loadAmexTpkEndListener.onEnd(null, ActionResult(SplashResult.TLE_AMEX_SERVICE_NOT_BINDED, null))
            return
        }

        // check AMEX acquirer exists?
        val acq = FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_AMEX)
        acq?.let {
            if (!it.isEnableTle) {
                loadAmexTpkEndListener.onEnd(null, ActionResult(SplashResult.TLE_AMEX_HOST_DISABLE_TLE,null))
                return
            }
        } ?: run {
            loadAmexTpkEndListener.onEnd(null, ActionResult(SplashResult.TLE_AMEX_HOST_NOT_FOUND,null))
            return
        }

        val actionAmexLoadLogOnTpk = ActionAmexLoadLogOnTpk(AAction.ActionStartListener {
            (it as ActionAmexLoadLogOnTpk).setParam(this)
        })
        actionAmexLoadLogOnTpk.setEndListener(loadAmexTpkEndListener)
        actionAmexLoadLogOnTpk.execute()
    }



}
