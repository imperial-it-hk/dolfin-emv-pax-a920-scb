package com.pax.pay.splash

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.pax.abl.core.ActionResult
import com.pax.appstore.DownloadParamService
import com.pax.edc.R
import com.pax.market.android.app.sdk.StoreSdk
import com.pax.market.api.sdk.java.base.constant.ResultCode
import com.pax.market.api.sdk.java.base.dto.DownloadResultObject
import com.pax.pay.BaseActivity
import com.pax.pay.SplashActivity
import com.pax.pay.app.FinancialApplication
import com.pax.pay.constant.Constants
import com.pax.pay.trans.model.Controller
import com.pax.pay.utils.Utils
import com.pax.settings.SysParam
import th.co.bkkps.utils.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DownloadParameterActivity : BaseActivity() {
    companion object {
        const val TAG = "SPLASH::"
    }


    override fun getLayoutId(): Int { return R.layout.activity_null }
    override fun initViews() { }
    override fun loadParam() { }

    var notificationManager: NotificationManager? = null
    lateinit var listener : SplashListener
    val saveFilePath : String = FinancialApplication.getApp().filesDir.path + "/Download/"

    override fun setListeners() {
        listener = object: SplashListener {
            override fun onEndProcess(activityResult: Instrumentation.ActivityResult) {
                progressAlertDialog?.dismiss()
                Log.d(TAG,"\t\t[DOWNLOADPARAMETER]---Finish() result = ${activityResult.resultCode}")
                setResult(activityResult.resultCode)
                finish()
                //finish(this@DownloadParameterActivity, activityResult)
                Log.d(TAG,"\t\tfinish called")
            }

            override fun onUpdataUI(title: String, dispText: String) {
                // do nothing
            }
        }
    }

    fun finish(context:Context, result : Instrumentation.ActivityResult) {
        var action = FinancialApplication.getApp().downloadParamAction
        action?.let {
            if(it.isFinished) { return }
            it.isFinished = true
            quickClickProtection.start()
            it.setResult(ActionResult(result.resultCode, null,null))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.setTitle("aram")
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG,"\t\t[DOWNLOADPARAMETER] start download parameter")
        downloadParameter()
    }

    var progressAlertDialog : ProgressDialog? = null
    private fun downloadParameter() {
        this.setTitleString("PARAMETER DOWNLOADING...")

        try {
            progressAlertDialog = ProgressDialog.show(this,"","Param download checking...")
            progressAlertDialog?.setCancelable(false)

            val thread = Thread {
                var downloadResult : DownloadResultObject? = StoreSdk.getInstance().paramApi().downloadParamToPath(packageName, getVersion(), saveFilePath)


                downloadResult?.let {
                    if (it.businessCode == ResultCode.SUCCESS) {
                        val resultHandleSuccess = handleSuccess()
                        if (resultHandleSuccess) {
                            Controller.set(Controller.IS_FIRST_INITIAL_NEEDED, true)
                            FinancialApplication.getSysParam()[SysParam.BooleanParam.FLAG_UPDATE_PARAM] = false
                            listener.onEndProcess(Instrumentation.ActivityResult(SplashResult.SUCCESS , null))
                        } else {
                            listener.onEndProcess(Instrumentation.ActivityResult(SplashResult.DOWNLOAD_PARAM_HANDLE_SUCCESS_FAILED,null))
                        }
                    } else if (it.businessCode == -10) {
                        listener.onEndProcess(Instrumentation.ActivityResult(SplashResult.NO_PARAM_FILE , null))
                    } else {
                        Log.d(TAG, "downloadResult: " + downloadResult.message)

                        listener.onEndProcess(Instrumentation.ActivityResult(SplashResult.DOWNLOAD_PARAM_FAILED, null))
                    }
                } ?: run {
                    listener.onEndProcess(Instrumentation.ActivityResult(SplashResult.DOWNLOAD_PARAM_FAILED, null))
                }
            }
            thread.start()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            listener.onEndProcess(Instrumentation.ActivityResult(SplashResult.DOWNLOAD_PARAM_THREAD_FAILED, null))
        }
    }

    private fun getVersion(): Int {
        try {
            val manager = packageManager
            val packageInfo = manager.getPackageInfo(packageName, 0)
            if (packageInfo != null) {
                return packageInfo.versionCode
            }
        } catch (e: java.lang.Exception) {
            Log.w(SplashActivity.TAG, e.message)
        }
        return 0
    }

    private fun handleSuccess() : Boolean {
        //file download to saveFilePath above.
        val filelist = File(saveFilePath).listFiles()
        val simpleDateFormat : SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        //val needRestart : Boolean = intent.getBooleanExtra("NEED_RESTART", false)

        var parameterFile: File? = null
        var result = false

        //Log.d(TAG, "needRestart=$needRestart")
        if (filelist != null && filelist.size > 0) {
            for (f in filelist) {
                if (Constants.DOWNLOAD_PARAM_FILE_NAME == f.name) {
                    parameterFile = f
                }
            }
            if (parameterFile != null) {
                val bannerTextValue = ("Your push parameters  - " + parameterFile.name  + " have been successfully pushed at " + simpleDateFormat.format(Date()) + ".")
                val bannerSubTextValue = "Files are stored in " + parameterFile.path
                Log.i(TAG, "\t\t[DOWNLOADPARAMETER] run=====: $bannerTextValue")

                FinancialApplication.getSysParam()[SysParam.StringParam.SAVE_FILE_PATH_PARAM] = saveFilePath
                if (FinancialApplication.getTransDataDbHelper().countOf() == 0L) {
                    // delete all transaction.
                    FinancialApplication.getTransDataDbHelper().deleteAllTransData()

                    //handleSuccess();
                    //result = FinancialApplication.getDownloadManager().handleSuccess(applicationContext)

                    //handleSuccess();
                    result = FinancialApplication.getDownloadManager().handleSuccess(applicationContext)
                } else {
                    FinancialApplication.getSysParam()[SysParam.BooleanParam.NEED_UPDATE_PARAM] = true
                    Log.i(TAG, "\t\tApp is busy, will update parameter after settlement")
                }
            } else {
                Log.i(TAG, "\t\t[DOWNLOADPARAMETER] parameterFile is null ")
            }
        }

        //update successful info
        //ToastUtils.showMessage("Download Complete.");
        //if (needRestart) Utils.restart()
        if (result) {
            Utils.initAPN(applicationContext, true, null)
            try {
                notificationManager?.let {
                    it.cancel(Constants.NOTIFICATION_ID_PARAM)
                }
            } catch (e: Exception) {
                Log.w(TAG, "\t\t[DOWNLOADPARAMETER] e:$e")
            }
            FinancialApplication.getSysParam()[SysParam.BooleanParam.NEED_CONSEQUENT_PARAM_INITIAL] = true // for ERCM/TLE auto download
            FinancialApplication.getSysParam()[SysParam.BooleanParam.NEED_UPDATE_PARAM] = false
            makeNotification(getString(R.string.notif_param_load_complete), getString(R.string.notif_param_success))
        }

        return result
    }

    private fun makeNotification(title: String, content: String) {
        val intent = Intent(applicationContext, DownloadParamService::class.java)
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, 0)
        val mBuilder = NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_bps_gray)
                .setColor(resources.getColor(R.color.primary))
                .setLargeIcon(BitmapFactory.decodeResource(this.resources, R.drawable.kaset_logo))
                .setContentTitle(title)
                .setContentText(content)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setPriority(Notification.PRIORITY_MAX)
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setContentIntent(pendingIntent)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager?.let {
            it.notify(0, mBuilder.build())
        }
    }
}