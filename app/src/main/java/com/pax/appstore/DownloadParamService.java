package com.pax.appstore;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.IBinder;

import th.co.bkkps.utils.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ATransaction;
import com.pax.abl.core.ActionResult;
import com.pax.appstore.dto.RequestDto;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.market.android.app.sdk.StoreSdk;
import com.pax.market.api.sdk.java.base.constant.ResultCode;
import com.pax.market.api.sdk.java.base.dto.DownloadResultObject;
import com.pax.market.api.sdk.java.base.exception.NotInitException;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.EReceiptStatusTrans;
import com.pax.pay.trans.LoadTMKTrans;
import com.pax.pay.trans.TleStatusTrans;
import com.pax.pay.trans.action.ActionReIntialEReceipt;
import com.pax.pay.trans.action.ActionUpdateSp200;
import com.pax.pay.trans.model.Controller;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.uart.SP200_serialAPI;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DownloadParamService extends Service {
    private static final String TAG = DownloadParamService.class.getSimpleName();
    private List<RequestDto> mDto = new ArrayList<>();
    private static final String BASE_URL = "https://api.whatspos.cn/p-market-api/v1";
    public static String saveFilePath;
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    public NotificationManager notificationManager;
    private boolean needRestart = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    private boolean currKioskMode = false;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
//                int actionInt = Integer.parseInt(action);
//                switch (actionInt) {
//                    case REQ_ERCM_INITIAL:
//                        ReInitialERCM();
//                        break;
//                    case REQ_TLE_AUTO_DOWNLOAD:
//                        ReInitTLEAutoDownload();
//                        break;
////                    case REQ_TLE_PRINT_STATUS:
////                        RePrintTLEStatus(TLE_intState);
//                    case REQ_ERCM_PRINT_STATUS:
//                        PrintERCMStatus();
//                        break;
//                    case REQ_PUSH_PARAM_SP200:
//                        PushParamToSP200();
//                        break;
//                    case REQ_FINALIZE:
//                        TerminalLockCheck.getInstance().setKioskMode(currKioskMode);
//                        //ActivityStack.getInstance().popTo(MainActivity.class);
//                        break;
//                    default:
//                        break;
//                }
            } else {
                saveFilePath = getFilesDir() + "/Download/";
                //ToastUtils.showMessage("Downloading Parameter...");
                needRestart = intent.getBooleanExtra("NEED_RESTART", false);
                Log.d(TAG, "needRestart="+needRestart);

                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        DownloadResultObject downloadResult = null;
                        try {
                            Log.i(TAG, "call sdk API to download parameter");
                            downloadResult = StoreSdk.getInstance().paramApi().downloadParamToPath(getPackageName(), getVersion(), saveFilePath);
                            Log.i(TAG, downloadResult.toString());
                        } catch (NotInitException e) {
                            Log.e(TAG, "e:" + e);
                        }

                        //                businesscode==0, means download successful, if not equal to 0, please check the return message when need.
                        if (downloadResult != null && downloadResult.getBusinessCode() == ResultCode.SUCCESS) {
                            Log.i(TAG, "download successful.");

//                            currKioskMode = TerminalLockCheck.getInstance().getKioskMode();
//                            TerminalLockCheck.getInstance().setKioskMode(false);

                            handleSuccess();
                            Controller.set(Controller.IS_FIRST_INITIAL_NEEDED, true);
                            FinancialApplication.getSysParam().set(SysParam.BooleanParam.FLAG_UPDATE_PARAM, false);
                            //setServiceResult(REQ_ERCM_INITIAL);
                        } else {
                            //todo check the Error Code and Error Message for fail reason
                            Log.e(TAG, "ErrorCode: " + downloadResult.getBusinessCode() + "ErrorMessage: " + downloadResult.getMessage());
                            //update download fail info in main page for Demo
                            //ToastUtils.showMessage("Download Fail.");
                        }

                    }
                });
                thread.start();
            }
            return super.onStartCommand(intent, flags, startId);
        } else {
            return Service.START_NOT_STICKY;
        }
    }

    private void handleSuccess() {
        //file download to saveFilePath above.
        File parameterFile = null;
        File[] filelist = new File(saveFilePath).listFiles();
        boolean result = false;
        if (filelist != null && filelist.length > 0) {
            for (File f : filelist) {
                if (Constants.DOWNLOAD_PARAM_FILE_NAME.equals(f.getName())) {
                    parameterFile = f;
                }
            }
            if (parameterFile != null) {

                String bannerTextValue = "Your push parameters  - " + parameterFile.getName()
                        + " have been successfully pushed at " + sdf.format(new Date()) + ".";
                String bannerSubTextValue = "Files are stored in " + parameterFile.getPath();
                Log.i(TAG, "run=====: " + bannerTextValue);

                //FinancialApplication.getDownloadManager().setSaveFilePath(saveFilePath);
                FinancialApplication.getSysParam().set(SysParam.StringParam.SAVE_FILE_PATH_PARAM, saveFilePath);

                if (FinancialApplication.getTransDataDbHelper().countOf() == 0) {
                    // delete all transaction.
                    FinancialApplication.getTransDataDbHelper().deleteAllTransData();

                    //handleSuccess();
                    result = FinancialApplication.getDownloadManager().handleSuccess(getApplicationContext());
                } else {
                    FinancialApplication.getSysParam().set(SysParam.BooleanParam.NEED_UPDATE_PARAM, true);
                    Log.i(TAG, "App is busy, will update parameter after settlement");
                }

            } else {
                Log.i(TAG, "parameterFile is null ");
            }
        }
        //update successful info
        //ToastUtils.showMessage("Download Complete.");
        if (needRestart) Utils.restart();
        if (result) {
            Utils.initAPN(getApplicationContext(), true, null);

            try {
                notificationManager.cancel(Constants.NOTIFICATION_ID_PARAM);
            } catch (Exception e) {
                Log.w(TAG, "e:" + e);
            }
            FinancialApplication.getSysParam().set(SysParam.BooleanParam.NEED_CONSEQUENT_PARAM_INITIAL, true); // for ERCM/TLE auto download
            FinancialApplication.getSysParam().set(SysParam.BooleanParam.NEED_UPDATE_PARAM, false);
            makeNotification(getString(R.string.notif_param_load_complete), getString(R.string.notif_param_success));
        }
    }

    private int getVersion() {
        try {
            PackageManager manager = getPackageManager();
            PackageInfo packageInfo = manager.getPackageInfo(getPackageName(), 0);
            if (packageInfo != null) {
                return packageInfo.versionCode;
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
        return 0;
    }

    private void makeNotification(String title, String content) {
        Intent intent = new Intent(getApplicationContext(), DownloadParamService.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_bps_gray)
                        .setColor(getResources().getColor(R.color.primary))
                        .setLargeIcon(BitmapFactory.decodeResource(this.getResources(),
                                R.drawable.kaset_logo))
                        .setContentTitle(title)
                        .setContentText(content)
                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setDefaults(Notification.DEFAULT_VIBRATE)
                        .setContentIntent(pendingIntent);

        notificationManager = (NotificationManager) getSystemService(getApplicationContext().NOTIFICATION_SERVICE);
        notificationManager.notify(0, mBuilder.build());
    }

    private void setServiceResult(int actionVal) {
        final Intent intent = new Intent(DownloadParamService.this, DownloadParamService.class);
        intent.setAction(Integer.toString(actionVal));
        startService(intent);
    }

    protected void PushParamToSP200() {
        if (!SP200_serialAPI.getInstance().isSp200Enable()) return;

        ActionUpdateSp200 updateSp200 = new ActionUpdateSp200(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionUpdateSp200) action).setParam(DownloadParamService.this);
            }
        });
        updateSp200.setEndListener(new AAction.ActionEndListener() {
            @Override
            public void onEnd(AAction action, ActionResult result) {
                setServiceResult(REQ_FINALIZE);
            }
        });

        if (SP200_serialAPI.getInstance().isSp200Enable()) {
            updateSp200.execute();
        }
    }

    final int REQ_PUSH_PARAM_SP200      = 0 ;
    final int REQ_TLE_AUTO_DOWNLOAD     = 1 ;
    final int REQ_TLE_PRINT_STATUS      = 2 ;
    final int REQ_ERCM_INITIAL          = 3 ;
    final int REQ_ERCM_PRINT_STATUS     = 4 ;
    final int REQ_FINALIZE              = 5 ;


    EReceiptStatusTrans.ercmInitialResult ercmInitResult = EReceiptStatusTrans.ercmInitialResult.NONE;
    protected void ReInitialERCM() {
        if (!FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE)) {return;}

        boolean result = FinancialApplication.getEReceiptDataDbHelper().deleteErmSessionKey();

        if (result) {
            ActionReIntialEReceipt reIntialEReceipt = new ActionReIntialEReceipt(new AAction.ActionStartListener() {
                @Override
                public void onStart(AAction action) {
                    ((ActionReIntialEReceipt) action).setParam(DownloadParamService.this);
                }
            });
            reIntialEReceipt.setEndListener(new AAction.ActionEndListener() {
                @Override
                public void onEnd(AAction action, ActionResult result) {
                    ercmInitResult = (result.getRet()==TransResult.SUCC) ? EReceiptStatusTrans.ercmInitialResult.INIT_SUCCESS : EReceiptStatusTrans.ercmInitialResult.INIT_FAILED;
                    setServiceResult(REQ_ERCM_PRINT_STATUS);
                }
            });
            reIntialEReceipt.execute();
        } else {
            makeNotification("ERCM initail error", "Failed to delete session key data.");
            ercmInitResult=EReceiptStatusTrans.ercmInitialResult.CLEAR_SESSIONKEY_ERROR;
            setServiceResult(REQ_ERCM_PRINT_STATUS);
        }

    }

    protected void PrintERCMStatus() {
        ATransaction.TransEndListener eReceiptStatusPrintEndListener =  new ATransaction.TransEndListener() {
            @Override
            public void onEnd(ActionResult result) {
                setServiceResult(REQ_TLE_AUTO_DOWNLOAD);
            }
        };
        EReceiptStatusTrans eReceiptStatusTrans = new EReceiptStatusTrans(DownloadParamService.this, ETransType.ERCEIPT_TERMINAL_REGISTRATION,eReceiptStatusPrintEndListener, ercmInitResult);
        eReceiptStatusTrans.execute();
    }

    public enum TleAutoInitState {
        NONE,
        TLE_ACQUIRER_NOT_FOUND,
        EREASE_KEY_FAILED,
        MISSING_TEID_FILE
    }
    int TLE_intState = -999;


    private void ReInitTLEAutoDownload() {
        TleAutoInitState TLE_state = TleAutoInitState.NONE;
        List<Acquirer> acquirerList = FinancialApplication.getAcqManager().findEnableAcquirers();
        boolean displayError = false;
        if(acquirerList.size()>0) {
            for (Acquirer local_acq : acquirerList) {
                local_acq.setTMK(null);
                local_acq.setTWK(null);
                FinancialApplication.getAcqManager().updateAcquirer(local_acq);
            }

            boolean ret = Device.eraseKeys();
            if (ret) {
                String tleFilePath = FinancialApplication.getSysParam().get(SysParam.StringParam.TLE_PARAMETER_FILE_PATH);
                File f = new File(tleFilePath);
                if (f.exists() && !f.isDirectory()) {
                    displayError=false;
                    TLE_intState= -999;
                    TLE_state = TleAutoInitState.NONE;
                } else {
                    displayError=true;
                    TLE_intState= 3;
                    TLE_state = TleAutoInitState.MISSING_TEID_FILE;
                }

                if (TLE_state == TleAutoInitState.NONE) {
                    ATransaction.TransEndListener transEndListener = new ATransaction.TransEndListener() {
                        @Override
                        public void onEnd(ActionResult result) {
                            Log.d("INIT*","ONEND--AUTO-LOAD-TLE");
                            TLE_intState = (result.getRet()==TransResult.SUCC) ? 5 :  4;
                            ATransaction.TransEndListener transEndListener = new ATransaction.TransEndListener() {
                                @Override
                                public void onEnd(ActionResult result) {
                                    Log.d("INIT*","PRINT-TLE-STATUS--END");
                                    setServiceResult(REQ_PUSH_PARAM_SP200);

                                }
                            };
                            TleStatusTrans tleStatusTrans = new TleStatusTrans(DownloadParamService.this, transEndListener,true, TLE_intState);
                            tleStatusTrans.execute();
                            Log.d("INIT*","PRINT-TLE-STATUS--START");
                        }
                    };

                    LoadTMKTrans TLE_autoInit = new LoadTMKTrans(DownloadParamService.this, transEndListener, true);
                    TLE_autoInit.execute();
                }
            } else {
                displayError=true;
                TLE_intState=2;
                TLE_state = TleAutoInitState.EREASE_KEY_FAILED;
            }
        } else {
            displayError=true;
            TLE_intState=1;
            TLE_state = TleAutoInitState.TLE_ACQUIRER_NOT_FOUND;
        }

        if(displayError) {
            String tileStr      = "Error TLE Download";
            String errorContent = "" ;
            switch (TLE_state) {
                case EREASE_KEY_FAILED:
                    errorContent = "Sorry cannot clear keys";
                    break;
                case TLE_ACQUIRER_NOT_FOUND:
                    errorContent = "TLE Eanbled acquirer was not found";
                    break;
                case MISSING_TEID_FILE :
                    errorContent = "TEID.json file was missing";
                    break;

            }
            makeNotification(tileStr, errorContent);

            // set next state to process
            ATransaction.TransEndListener transEndListener = new ATransaction.TransEndListener() {
                @Override
                public void onEnd(ActionResult result) {
                    Log.d("INIT*","ONEND--PRINT-TLE-STATUS");
                    setServiceResult(REQ_PUSH_PARAM_SP200);
                }
            };
            TleStatusTrans tleStatusTrans = new TleStatusTrans(DownloadParamService.this, transEndListener,true, TLE_intState);
            tleStatusTrans.execute();
        }
    }

}
