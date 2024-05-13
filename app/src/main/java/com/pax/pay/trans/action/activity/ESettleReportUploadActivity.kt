package com.pax.pay.trans.action.activity

import android.app.AlarmManager
import android.content.Context
import android.content.DialogInterface
import android.os.AsyncTask
import android.os.SystemClock
import android.widget.TextView
import com.pax.abl.core.ActionResult
import com.pax.device.Device
import com.pax.edc.R
import com.pax.edc.opensdk.TransResult
import com.pax.pay.BaseActivity
import com.pax.pay.app.ActivityStack
import com.pax.pay.app.FinancialApplication
import com.pax.pay.base.AcqIssuerRelation
import com.pax.pay.base.Acquirer
import com.pax.pay.constant.Constants
import com.pax.pay.trans.TransContext
import com.pax.pay.trans.action.ActionEReceiptInfoUpload.EReceiptType
import com.pax.pay.trans.component.Component
import com.pax.pay.trans.model.ETransType
import com.pax.pay.trans.model.TransData
import com.pax.pay.trans.model.TransTotal
import com.pax.pay.trans.transmit.Online
import com.pax.pay.trans.transmit.TransProcessListener
import com.pax.pay.trans.transmit.TransProcessListenerImpl
import com.pax.pay.utils.Convert
import com.pax.pay.utils.EReceiptUtils
import com.pax.pay.utils.TickTimer
import com.pax.pay.utils.TransResultUtils
import com.pax.settings.SysParam
import com.pax.view.dialog.DialogUtils
import th.co.bkkps.utils.Log
import java.io.File
import java.util.*

class ESettleReportUploadActivity : BaseActivity() {

    private var total: TransTotal? = null
    private var referencNo_settlemented: String? = null
    private var online : Online? = null
    lateinit var type : EReceiptType

    lateinit var ermKMS : Acquirer
    lateinit var ermRMS : Acquirer
    lateinit var title : String

    lateinit var txv_caption : TextView
    lateinit var txv_waiting : TextView
    lateinit var startTimer : TickTimer
    lateinit var timerListener : TickTimer.OnTickTimerListener
    private var settleResult: Int = 0

    override fun getLayoutId(): Int { return R.layout.activity_auto_settlement }
    override fun initViews() {
        txv_caption = findViewById(R.id.txv_caption)
        txv_caption.textSize = 24f
        txv_waiting = findViewById(R.id.txv_waiting)

        displayText("Upload E-Settlement Report", "Please wait...")
    }
    override fun setListeners() {
//        timerListener = object : TickTimer.OnTickTimerListener {
//            override fun onTick(leftTime: Long) {
//                displayText(null, "Please wait...")
//            }
//
//            override fun onFinish() {
//                ermKMS = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_ERCM_KEY_MANAGEMENT_SERVICE)
//                ermRMS = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_ERCM_RECEIPT_MANAGEMENT_SERVICE)
//                if (!ermKMS.isEnable || !ermRMS.isEnable) {
//                    displayText(null, "Unable to upload : host was disabled")
//                    finish(ActionResult(TransResult.ERR_UNSUPPORTED_FUNC, null))
//                    return
//                }
//
//                type  = EReceiptType.ERECEIPT_REPORT_FROM_FILE          // fix type for upload Settlement-Report only
//                online = Online()                                       // instant online object
//                uploadESettlementFromFile()
//            }
//        }
    }
    override fun loadParam() {
        settleResult = intent.getIntExtra("settleResult", 0)
    }
    override fun getTitleString(): String {
        return "UPLOAD SETTLEMENT REPORT"
    }

    override fun onResume() {
        super.onResume()
//        startTimer = TickTimer(timerListener)
//        startTimer.start(1)

        ermKMS = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_ERCM_KEY_MANAGEMENT_SERVICE)
        ermRMS = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_ERCM_RECEIPT_MANAGEMENT_SERVICE)
        if (!ermKMS.isEnable || !ermRMS.isEnable) {
            displayText(null, "Unable to upload : host was disabled")
            displayResultDialog(TransResult.ERR_UNSUPPORTED_FUNC)
            return
        }

        type  = EReceiptType.ERECEIPT_REPORT_FROM_FILE          // fix type for upload Settlement-Report only
        online = Online()                                       // instant online object


        FinancialApplication.getApp().runOnUiThreadDelay({
            val executor = ExecuteESettleReportUploadAsyncTask();
            executor.execute()
        }, 500L)

    }

    fun displayText(caption : String?, message: String?) {
        this.runOnUiThread(Runnable {
            caption?.let{ txv_caption.text = it }
            message?.let{ txv_waiting.text = it }
        })
    }

    inner class ExecuteESettleReportUploadAsyncTask : AsyncTask<Void, String, Void>() {
        override fun doInBackground(vararg p0: Void?): Void? {
            uploadESettlementFromFile()
            return null
        }

        override fun onPostExecute(result: Void?) {
            super.onPostExecute(result)
        }

        override fun onProgressUpdate(vararg values: String?) {
            txv_waiting.text = values[0]
            SystemClock.sleep(1000L)
            AlarmManager.INTERVAL_FIFTEEN_MINUTES
        }

        private fun uploadESettlementFromFile() {
            try {
                val path = EReceiptUtils.getERM_UnsettleExternalStorageDirectory()
                val settleExStorage = File(path)

                if (!settleExStorage.exists()) {
                    // in-case Settlement directory was missing
                    Log.e(EReceiptUtils.TAG, "Target storage was not found")
                    publishProgress( "Target storage was not found")
                    displayResultDialog(TransResult.ERCM_ESETTLE_REPORT_STORAGE_NOT_FOUND)
                    return
                }

                if (!settleExStorage.isDirectory) {
                    // in-case storage is not directory
                    Log.e(EReceiptUtils.TAG, "Invalid report directory file type")
                    publishProgress( "Invalid report directory file type")
                    displayResultDialog(TransResult.ERCM_ESETTLE_REPORT_INVALID_DIRECTORY_TYPE)
                    return
                }

                if (settleExStorage.listFiles()!!.size == 0) {
                    // in-case no file target storage directory
                    Log.e(EReceiptUtils.TAG, "settlement report for upload was empty")
                    publishProgress("settlement report for upload was empty")
                    displayResultDialog(TransResult.SUCC)
                    return
                }


                publishProgress( "Ready for upload...")


                val SettleReportMaps = Hashtable<String, String>()
                lateinit var AcqNii : String
                lateinit var AcqName : String
                lateinit var key_AcqName: String
                lateinit var val_AcqNii: String
                var isAllowAdd = false
                for (localFile in settleExStorage.listFiles()!!) {
                    AcqNii = localFile.name.split("-").toTypedArray()[0].substring(0, 3)
                    AcqName = localFile.name.split("-").toTypedArray()[0].substring(3)
                    isAllowAdd = allowAddToHashtable(SettleReportMaps, AcqName, AcqNii)
                    if (isAllowAdd) {
                        SettleReportMaps[AcqName] = AcqNii
                    }
                }

                var result: Int = TransResult.ERCM_UPLOAD_FAIL
                if (SettleReportMaps.size > 0) {
                    //displayText(null, "found ${SettleReportMaps.size} host to upload...")
                    for (key:String in SettleReportMaps.keys) {
                        key_AcqName = key
                        val_AcqNii = SettleReportMaps.getValue(key)

                        displayText(null, "$key_AcqName ($val_AcqNii)")
                        SystemClock.sleep(2000)

                        result = trySend_AlleSettleReport( key_AcqName, val_AcqNii)
                    }
                    displayResultDialog(result)
                }
            } catch (ex: Exception) {
                Log.e(EReceiptUtils.TAG, "Failed to upload AllSettleReport :: " + ex.message)
                publishProgress( "E-SettleReport upload failed")
                displayResultDialog(TransResult.ERCM_ESETTLE_REPORT_UPLOAD_FAIL)
            }
        }



        private fun trySend_AlleSettleReport(AcquirerName: String, Nii: String): Int {
            val path = EReceiptUtils.getERM_UnsettleExternalStorageDirectory()
            val dir = File(path)
            var result = -1
            if (dir.isDirectory && dir.listFiles().size > 0) {
                val eSettleReportFileList = dir.listFiles()
                var targAcquirer: Acquirer? = null
                var currHostBatchNum = -1
                for (file in eSettleReportFileList) {
                    if (file.name.contains(Nii + AcquirerName)) {
                        var reESettleReportTransData: TransData
                        reESettleReportTransData = TransData()
                        Component.transInit(reESettleReportTransData)
                        val fileName = file.name.replace(".erm", "")
                        val fileLen = fileName.length
                        targAcquirer = null

                        val transInfoStr : String = fileName.split("-").get(1)
                        val midReadLen = (transInfoStr.substring(0 , 3)).toInt()
                        val originalNII = transInfoStr.substring(3 , 6)
                        val refNo = transInfoStr.substring(6, 18)
                        val terminalID = transInfoStr.substring(18, 26)
                        val merchantID = transInfoStr.substring(26, 26 + midReadLen)
                        val batchNo = transInfoStr.substring(26 + midReadLen)

                        currHostBatchNum = batchNo.toInt()  //fileName.substring(fileLen - 6).toInt()
                        referencNo_settlemented = refNo


                        // Extra settlement from file
                        reESettleReportTransData.seteReceiptUploadSource(TransData.SettleUploadSource.FROM_FILE)
                        reESettleReportTransData.seteReceiptUploadSourcePath(file.name)
                        reESettleReportTransData.transType = ETransType.ERCEIPT_SETTLE_UPLOAD
                        reESettleReportTransData.setSettleTransTotal(total)
                        val acquirer = FinancialApplication.getAcqManager()
                            .findAcquirer(Constants.ACQ_ERCM_RECEIPT_MANAGEMENT_SERVICE)
                        if (total != null) {
                            reESettleReportTransData.initAcquirerIndex = EReceiptUtils.StringPadding(total!!.getAcquirer().getId().toString(),3,"0",Convert.EPaddingPosition.PADDING_LEFT)
                            reESettleReportTransData.initAcquirerNii = EReceiptUtils.StringPadding(total!!.getAcquirer().getNii().toString(),3,"0",Convert.EPaddingPosition.PADDING_LEFT)
                            reESettleReportTransData.initAcquirerName = total!!.getAcquirer().getName()
                            acquirer.currBatchNo = total!!.getBatchNo() //set current batch num for each acquirer
                        } else {
                            targAcquirer = FinancialApplication.getAcqManager().findAcquirer(AcquirerName)
                            reESettleReportTransData.initAcquirerIndex = EReceiptUtils.StringPadding(targAcquirer.id.toString(),3,"0",Convert.EPaddingPosition.PADDING_LEFT)
                            reESettleReportTransData.initAcquirerNii = EReceiptUtils.StringPadding(targAcquirer.nii.toString(),3,"0",Convert.EPaddingPosition.PADDING_LEFT)
                            reESettleReportTransData.initAcquirerName = AcquirerName
                            acquirer.currBatchNo = currHostBatchNum
                        }
                        reESettleReportTransData.acquirer = acquirer
                        reESettleReportTransData.ercmBankCode = FinancialApplication.getSysParam()[SysParam.StringParam.VERIFONE_ERCM_BANK_CODE]

                        reESettleReportTransData.tpdu = "60" + EReceiptUtils.StringPadding(acquirer.nii,4,"0",Convert.EPaddingPosition.PADDING_LEFT) + "8000"
                        reESettleReportTransData.refNo = referencNo_settlemented

                        displayText(null, "${reESettleReportTransData.transType.transName}, uploading...")
                        val ret: Int = uploadESettleReportFromFile( reESettleReportTransData, file.name.replace(".erm", ""), ETransType.ERCEIPT_UPLOAD, targAcquirer!! )

                        if (ret == TransResult.SUCC) {
                            publishProgress("Result : upload successful")
                            Log.d( EReceiptUtils.TAG, " E-SETTLEMENT--REPORT--SUCCESS--UPLOAD--FILE   : " + file.name )
                            DeleteTempSettlementFile(file.name)
                            result = ret
                        } else {
                            publishProgress( "Result : upload was rejected")
                            Log.d( EReceiptUtils.TAG, " E-SETTLEMENT--REPORT--FAILED--UPLOAD--FILE   : " + file.name )
                            result = ret
                        }
                        if (result != TransResult.SUCC) {
                            break
                        }
                    }
                }

                return result
            } else {
                publishProgress( "Result : upload failed !!")
//                finish(ActionResult(TransResult.ERCM_UPLOAD_FAIL, null))
                return TransResult.ERCM_UPLOAD_FAIL
            }
        }

        private fun allowAddToHashtable(hash: Hashtable<*, *>?, name: String?, nii: String?): Boolean {
            if (hash != null) {
                if (hash.size == 0) {
                    return true
                } else {
                    if (name != null && nii != null) {
                        return !(hash.containsKey(name) && hash.containsValue(nii))
                    }
                }
            }
            return false
        }

        private fun uploadESettleReportFromFile( localTransData: TransData?, fName: String?, transType: ETransType?, acquirer: Acquirer ): Int {
            var result = TransResult.ERCM_UPLOAD_FAIL
            try {
                if (localTransData == null || fName == null || transType == null) {
                    publishProgress("missing ERM transaction data")
                    Log.e( EReceiptUtils.TAG, "TransData or Uploadfilename or input transtype was missing" )
                    return result
                }

                val path = EReceiptUtils.getERM_UnsettleExternalStorageDirectory()
                if (path == null) {
                    publishProgress("missing external storage path")
                    Log.e(EReceiptUtils.TAG, "path was not found")
                    return result
                }

                val dir = File(path)
                if (dir == null || !dir.exists() || !dir.isDirectory) {
                    publishProgress("Settlement directory not found")
                    Log.e(EReceiptUtils.TAG, "Target storage was not found")
                    return result
                }

                val fileName = "$fName.erm"
                val fullPathName = path + File.separator + fileName
                val targFileUpload = File(fullPathName)
                if (!targFileUpload.exists() || !targFileUpload.isFile) {
                    publishProgress("Settlement report file was missing")
                    Log.e(EReceiptUtils.TAG,"Target file was not found on '$fullPathName")
                    return result
                }
                localTransData.transType = ETransType.ERCEIPT_SETTLE_UPLOAD
                localTransData.seteReceiptUploadSource(TransData.SettleUploadSource.FROM_FILE)
                localTransData.seteReceiptUploadSourcePath(fileName)
                result = sendUploadErm(localTransData, fullPathName, transType, acquirer, true)
                return result
            } catch (e: java.lang.Exception) {
                Log.e(EReceiptUtils.TAG, "Failed to upload eReceipt to ERCM :: " + e.message)
            }
            return result
        }

        private fun sendUploadErm( paymentTrans: TransData, fullPathName: String, transType: ETransType, acquirer: Acquirer, isSettlementUpload: Boolean ): Int {
            val file : File = File(fullPathName)
            val ermReadyUpload: Boolean = isErmReadyForUpload(acquirer)
            if (!ermReadyUpload) {
                publishProgress("ERM isn't ready for upload")
//                finish(ActionResult(TransResult.ERCM_INITIAL_PROCESS_FAILED, null))
                return TransResult.ERCM_INITIAL_PROCESS_FAILED
            }

            val ermRMS = FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_ERCM_RECEIPT_MANAGEMENT_SERVICE)
            var result = -1
            try {
                if (file != null && file.exists() && file.isFile) {
                    var ermTrans = Component.transInit()
                    ermTrans = setErmPrerequisiteInfo(paymentTrans, ermTrans, acquirer, ermRMS, isSettlementUpload )
                    val ret: Int = online!!.online(ermTrans, null)
                    Log.d("ERM-MULTIAPP", "$fullPathName upload result = $result")
                    result =
                        if (ret == TransResult.SUCC && ermTrans.responseCode != null && ermTrans.responseCode.code == "00") {
                            if (isSettlementUpload) {
                                DeleteTempSettlementFile(file.name)
                            } else {
                                DeleteTempTransactionFile(this@ESettleReportUploadActivity, acquirer, file.name)
                            }
                            TransResult.SUCC
                        } else {
                            TransResult.ERCM_UPLOAD_FAIL
                        }
                } else {
                    result = TransResult.ERCM_UPLOAD_FAIL
                }
            } catch (e: java.lang.Exception) {
                result = TransResult.ERCM_UPLOAD_FAIL
            }
            return result
        }

        private fun DeleteTempSettlementFile(FileName: String) {
            val path = EReceiptUtils.getERM_UnsettleExternalStorageDirectory()
            val fName = "/$FileName"
            if (File(path).exists()) {
                if (File(path + fName).exists()) {
                    File(path + fName).delete()
                }
            }
        }

        private fun DeleteTempTransactionFile(context: Context, acquirer: Acquirer, FileName: String) {
            val path = EReceiptUtils.getERM_ExternalAppUploadDicrectory(context, acquirer)
            val fName = "/$FileName"
            if (File(path).exists()) {
                if (File(path + fName).exists()) {
                    File(path + fName).delete()
                }
                if (File(path + fName.replace(".erm", ".irm")).exists()) {
                    File(path + fName.replace(".erm", ".irm")).delete()
                }
            }
        }

        fun isErmReadyForUpload(targetAcquirer: Acquirer): Boolean {
            if (!targetAcquirer.isEnable || !targetAcquirer.isEnableUploadERM) { return false }

            val ermKMS = FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_ERCM_RECEIPT_MANAGEMENT_SERVICE)
            if (ermKMS==null) { return false }

            val ermRMS = FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_ERCM_RECEIPT_MANAGEMENT_SERVICE)
            if (ermRMS==null) { return false }

            val ermCodeBank = FinancialApplication.getSysParam()[SysParam.StringParam.VERIFONE_ERCM_BANK_CODE]
            if (ermCodeBank == null || ermCodeBank.isEmpty()) {return false}

            val ermCodeMerc = FinancialApplication.getSysParam()[SysParam.StringParam.VERIFONE_ERCM_MERCHANT_CODE]
            if (ermCodeMerc == null || ermCodeMerc.isEmpty()) {return false}

            val ermCodeStore = FinancialApplication.getSysParam()[SysParam.StringParam.VERIFONE_ERCM_STORE_CODE]
            if (ermCodeStore == null || ermCodeStore.isEmpty()) {return false}

            val ermKeyVersion = FinancialApplication.getSysParam()[SysParam.StringParam.VERIFONE_ERCM_KEK_VERSION]
            if (ermKeyVersion == null || ermKeyVersion.isEmpty()) {return false}

            try {
                val sskInfo = FinancialApplication.getEReceiptDataDbHelper().FindSessionKeyByAcquirerInfos(targetAcquirer.nii, targetAcquirer.name)
                if (sskInfo == null || sskInfo.sessionKeyInfosFile == null) {
                    return false
                }
            } catch (e: java.lang.Exception) {
                return false
            }
            return true
        }

        private fun setErmPrerequisiteInfo( paymentTrans: TransData, ermTrans: TransData, paymentAcquirer: Acquirer, ermAcquirer: Acquirer, isSettlementUpload: Boolean ): TransData? {
            // ERM config
            ermTrans.acquirer = ermAcquirer
            ermTrans.transType = if (isSettlementUpload) ETransType.ERCEIPT_SETTLE_UPLOAD else ETransType.ERCEIPT_UPLOAD_FOR_MULTI_APP
            ermTrans.ercmBankCode = FinancialApplication.getSysParam()[SysParam.StringParam.VERIFONE_ERCM_BANK_CODE]
            ermTrans.ercmMerchantCode = FinancialApplication.getSysParam()[SysParam.StringParam.VERIFONE_ERCM_MERCHANT_CODE]
            ermTrans.ercmStoreCode = FinancialApplication.getSysParam()[SysParam.StringParam.VERIFONE_ERCM_STORE_CODE]
            ermTrans.seteReceiptUploadDateTime(Device.getTime(Constants.TIME_PATTERN_TRANS))
            ermTrans.tpdu = "60" + getLeftPadding(ermAcquirer.nii, 4, "0") + "8000"
            if (isSettlementUpload) {
                ermTrans.seteReceiptUploadSource(paymentTrans.geteReceiptUploadSource())
                ermTrans.seteReceiptUploadSourcePath(paymentTrans.geteReceiptUploadSourcePath())
                ermTrans.acquirer = ermAcquirer
            }

            // using payment Acquirer
            ermTrans.initAcquirerIndex = getLeftPadding(paymentAcquirer.id, 3, "0")
            ermTrans.initAcquirerNii = getLeftPadding(paymentAcquirer.nii, 3, "0")
            ermTrans.initAcquirerName = paymentAcquirer.name

            // using payment TransData
            ermTrans.seteSlipFormat(paymentTrans.geteSlipFormat())
            ermTrans.origTransType = paymentTrans.transType
            ermTrans.pan = paymentTrans.pan
            ermTrans.amount = paymentTrans.amount
            ermTrans.stanNo = paymentTrans.stanNo
            ermTrans.traceNo = paymentTrans.traceNo
            ermTrans.dateTime = paymentTrans.dateTime
            ermTrans.expDate = paymentTrans.expDate
            ermTrans.refNo = paymentTrans.refNo
            ermTrans.authCode = paymentTrans.authCode
            ermTrans.authCode = paymentTrans.authCode
            ermTrans.issuer = paymentTrans.issuer
            ermTrans.signData = paymentTrans.signData
            ermTrans.isPinVerifyMsg = paymentTrans.isPinVerifyMsg
            ermTrans.isTxnSmallAmt = paymentTrans.isTxnSmallAmt
            return ermTrans
        }

        private fun getLeftPadding(`val`: Int, maxlen: Int, padStr: String): String? {
            return EReceiptUtils.StringPadding(`val`.toString(),maxlen,padStr,Convert.EPaddingPosition.PADDING_LEFT)
        }

        private fun getLeftPadding(`val`: Long, maxlen: Int, padStr: String): String? {
            return EReceiptUtils.StringPadding(`val`.toString(),maxlen,padStr,Convert.EPaddingPosition.PADDING_LEFT)
        }

        private fun getLeftPadding(`val`: String, maxlen: Int, padStr: String): String? {
            return EReceiptUtils.StringPadding(`val`,maxlen,padStr,Convert.EPaddingPosition.PADDING_LEFT)
        }



    }

    private fun displayResultDialog(result: Int) {
        if (result == TransResult.SUCC) {
            DialogUtils.showSuccMessage(this@ESettleReportUploadActivity, ETransType.ERCEIPT_SETTLE_UPLOAD.transName,
                {
                    displaySettlementResult()
                },
                Constants.SUCCESS_DIALOG_SHOW_TIME
            )
        } else {
            DialogUtils.showErrMessage(this@ESettleReportUploadActivity, ETransType.ERCEIPT_SETTLE_UPLOAD.transName,
                TransResultUtils.getMessage(result),
                {
                    displaySettlementResult()
                },
                Constants.FAILED_DIALOG_SHOW_TIME
            )
        }
    }

    private fun displaySettlementResult() {
        if (settleResult == TransResult.SUCC) {
            DialogUtils.showSuccMessage(
                this@ESettleReportUploadActivity, ETransType.SETTLE.transName,
                {
                    finish(ActionResult(TransResult.ERR_ABORTED, null)) //to skip dialog when transEnd
                },
                Constants.SUCCESS_DIALOG_SHOW_TIME
            )
        } else {
            DialogUtils.showErrMessage(
                this@ESettleReportUploadActivity, ETransType.SETTLE.transName,
                TransResultUtils.getMessage(settleResult),
                {
                    finish(ActionResult(TransResult.ERR_ABORTED, null)) //to skip dialog when transEnd
                },
                Constants.FAILED_DIALOG_SHOW_TIME
            )
        }
    }

    fun finish(result : ActionResult) {
        SystemClock.sleep(2000L)
        val action = TransContext.getInstance().currentAction
        if (action!=null) {
            if (action.isFinished) {return}
            action.isFinished = true
            action.setResult(result)
        }
        finish()
    }
}











