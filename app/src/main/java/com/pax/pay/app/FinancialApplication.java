/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-11-25
 * Module Author: Steven.W
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.app;

import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Environment;
import android.os.Handler;
import android.os.RemoteException;

import th.co.bkkps.utils.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;

import com.pax.abl.core.AAction;
import com.pax.appstore.DownloadManager;
import com.pax.dal.IDAL;
import com.pax.dal.entity.EPedType;
import com.pax.dal.entity.ERoute;
import com.pax.dal.entity.ETermInfoKey;
import com.pax.device.Device;
import com.pax.device.GeneralParam;
import com.pax.device.UserParam;
import com.pax.edc.R;
import com.pax.eventbus.Event;
import com.pax.glwrapper.IGL;
import com.pax.glwrapper.convert.IConvert;
import com.pax.glwrapper.impl.GL;
import com.pax.glwrapper.packer.IPacker;
import com.pax.market.android.app.sdk.BaseApiService;
import com.pax.market.android.app.sdk.StoreSdk;
import com.pax.pay.ECR.EcrProcessClass;
import com.pax.pay.SplashActivity;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.document.AcqDocumentRead;
import com.pax.pay.base.document.CardRangeDocument;
import com.pax.pay.base.document.IssuerDocument;
import com.pax.pay.base.document.RelationDocument;
import com.pax.pay.constant.Constants;
import com.pax.pay.db.*;
import com.pax.pay.ped.PedManager;
import com.pax.pay.trans.model.AcqManager;
import com.pax.pay.trans.model.Controller;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.uart.BaseL920BMCommClass;
import com.pax.pay.uart.UsbSerialManagerClass;
import com.pax.pay.utils.*;
import com.pax.sdk.Sdk;
import com.pax.settings.SysParam;
import com.pax.view.dialog.DialogUtils;
import com.squareup.leakcanary.LeakCanary;
import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * customized application
 */
public class FinancialApplication extends Application {
    public static final String TAG = "FinancialApplication";
    private static FinancialApplication mApp;
    private static SysParam sysParam;
    private static Controller controller;
    private static ResponseCode rspCode;
    private static GeneralParam generalParam;
    private static UserParam userParam;
    private static AcqManager acqManager;
    private static CardBinDb cardBinDb;
    private static EmvDb emvDbHelper;
    private static TransDataDb transDataDbHelper;
    private static TransTotalDb transTotalDbHelper;
    private static TornLogDb tornLogDbHelper;
    private static TemplateLinePayDb templateLinePayDbHelper;
    private static EReceiptDataDb eReceiptDataDbHelper;
    private static TransMultiAppDataDb transMultiAppDataDbHelper;

    public static EcrProcessClass EcrProcess = null;
    public static UsbSerialManagerClass UsbSerialManager = null;
    public static BaseL920BMCommClass L920BM_BSSComm = null;

    private AAction updateSp200Action;
    private AAction autoInitAction;
    private AAction downloadParamAction;

    // Neptune interface
    private static IDAL dal;
    private static IGL gl;
    private static IConvert convert;
    private static IPacker packer;
    private String[] cameraIdList = null;
    private static PedManager pedManager;

    //App Store
    private static DownloadManager downloadManager;
    private boolean isReadyToUpdate=false;

    // app version
    private static String version;

    private Handler handler;
    private ExecutorService backgroundExecutor;

    // Routing Table :: PatcharaK
    private RoutingTable routingTable;

    // Progress Dialog :: PatcharaK
    private ProgressDialog progressDialog;

    private static final SysParam.UpdateListener updateListener = new SysParam.UpdateListener() {

        @Override
        public void onErr(String prompt) {
            DialogUtils.showUpdateDialog(getApp(), prompt);
        }
    };

    // TODO : SPLASHACTIVITY - AFTER MERGE
    public enum InitState {NONE, APP_EXISTS, APP_UPGARDED}
    private static InitState initialState = InitState.NONE;
    //

    @Override
    public void onCreate() {
        super.onCreate();
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);
        FinancialApplication.mApp = this;
        version = updateVersion();
        CrashHandler.getInstance();
        System.setProperty("org.xml.sax.driver","org.xmlpull.v1.sax2.Driver");
        init();
        initData();
        initPaxStoreSdk();
        Utils.initAPN(getApplicationContext(), false, null);

        handler = new Handler();
        backgroundExecutor = Executors.newFixedThreadPool(10, new ThreadFactory() {
            @Override
            public Thread newThread(@NonNull Runnable runnable) {
                Thread thread = new Thread(runnable, "Background executor service");
                thread.setPriority(Thread.MIN_PRIORITY);
                thread.setDaemon(true);
                return thread;
            }
        });


        routingTable = RoutingTable.getInstance();
        List<Acquirer> acquirers = acqManager.findEnableAcquirers();
        if(acquirers != null){
            for(Acquirer acquirer:acquirers){
                routingTable.add(acquirer.getIp(), ERoute.MOBILE);
            }
        }


        //add by Minson
        initCameraList();

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (FinancialApplication.getSysParam() != null) {
            Locale targetLocale;
            String language = FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_LANGUAGE);
            if(language != null) {
                if(language.equalsIgnoreCase(UILanguage.ENGLISH.getDisplay())){
                    targetLocale = UILanguage.valueOf(language).getLocale();
                }else{
                    targetLocale = UILanguage.THAI.getLocale();
                }
            }else{
                targetLocale = UILanguage.THAI.getLocale();
            }
            Utils.changeAppLanguage(FinancialApplication.getApp(), targetLocale);
        }
    }

    public static void init() {
        // get Neptune instance
        dal = Sdk.getInstance().getDal(getApp());
        gl = GL.getInstance(getApp());
        convert = getGl().getConvert();
        packer = getGl().getPacker();

        cardBinDb = CardBinDb.getInstance();
        emvDbHelper = EmvDb.getInstance();
        transDataDbHelper = TransDataDb.getInstance();
        transTotalDbHelper = TransTotalDb.getInstance();
        tornLogDbHelper = TornLogDb.getInstance();
        templateLinePayDbHelper = TemplateLinePayDb.getInstance();

        eReceiptDataDbHelper = EReceiptDataDb.getInstance();
        transMultiAppDataDbHelper = TransMultiAppDataDb.getInstance();


        // init
        sysParam = SysParam.getInstance();
        SysParam.setUpdateListener(updateListener);
        controller = new Controller();
        generalParam = new GeneralParam();
        userParam = new UserParam();
        acqManager = AcqManager.getInstance(true);

        downloadManager = DownloadManager.getInstance();
        //downloadManager.setAppKey();
        //downloadManager.setAppSecret();
        downloadManager.setSn(dal.getSys().getTermInfo().get(ETermInfoKey.SN));
        downloadManager.setFilePath(Environment.getExternalStorageDirectory().getPath() + File.separator + "ParamEDC/");
        downloadManager.addDocument(new AcqDocumentRead(Constants.ACQ_PATH))
                .addDocument(new IssuerDocument(Constants.ISSUER_PATH))
                .addDocument(new CardRangeDocument(Constants.CARD_RANGE_PATH))
                .addDocument(new RelationDocument(Constants.RELATION_PATH));
    }

    private void initCameraList() {
        try {
            CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            cameraIdList = cameraManager.getCameraIdList();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public int getCameraIdList() {
        return cameraIdList == null ? 0 : cameraIdList.length;
    }

    public static void initData() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                // init response code
                rspCode = ResponseCode.getInstance();
                getRspCode().init();
            }
        }).start();
    }

    /**
     * get app version
     */
    private String updateVersion() {
        try {
            PackageManager manager = getPackageManager();
            PackageInfo info = manager.getPackageInfo(getPackageName(), 0);
            return info.versionName;
        } catch (Exception e) {
            Log.w(TAG, e);
            return null;
        }
    }

    //PAX Store SDK
    private void initPaxStoreSdk() {
        StoreSdk.getInstance().init(getApplicationContext(), Utils.getAppKey(getApplicationContext()), Utils.getAppSecret(getApplicationContext()),downloadManager.getSn(), new BaseApiService.Callback() {
            @Override
            public void initSuccess() {
                Log.i(TAG, "initSuccess.");
                SplashActivity.Companion.setInitialState(SplashActivity.InitialState.APP_EXISTS);
                initInquirer();
            }

            @Override
            public void initFailed(RemoteException e) {
                Log.i(TAG, "initFailed: "+e.getMessage());
                Toast.makeText(getApplicationContext(), "Cannot get API URL from PAXSTORE, Please install PAXSTORE first.", Toast.LENGTH_LONG).show();
            }


        });
    }
    private void initInquirer() {
        StoreSdk.getInstance().initInquirer(new StoreSdk.Inquirer() {
            @Override
            public boolean isReadyUpdate() {

                if(FinancialApplication.getTransDataDbHelper().countOf() == 0){
                    if(FinancialApplication.getTransDataDbHelper().countOfReversal() == 0){
                        // delete all transaction.
                        FinancialApplication.getTransDataDbHelper().deleteAllTransData();
                        isReadyToUpdate = true;
                    }else{
                        //having reversal trans in db, do not update app
                        isReadyToUpdate = false;
                        NotificationUtils.cancelNotification(getApplicationContext());
                        NotificationUtils.makeNotification(getApplicationContext(), Utils.getString(R.string.notif_app_update_err), Utils.getString(R.string.notif_app_err_reversal), false);
                    }
                }else{
                    //having transaction in db, do not update app
                    isReadyToUpdate = false;
                    NotificationUtils.cancelNotification(getApplicationContext());
                    NotificationUtils.makeNotification(getApplicationContext(), Utils.getString(R.string.notif_app_update_err), Utils.getString(R.string.notif_app_req_settle), false);
                }
                Log.i(TAG, "call business function....isReadyUpdate = " + isReadyToUpdate);

                Log.i(SplashActivity.TAG, "\t\t:: Status :: IsReadyToUpdate = " + isReadyToUpdate);

                if (FinancialApplication.getTransDataDbHelper().countOf() > 0 || FinancialApplication.getTransDataDbHelper().countOfReversal() > 0) {
                    loadLogExistingTransData();
                }
                SplashActivity.InitialState initiState = (isReadyToUpdate) ? SplashActivity.InitialState.APP_READY_TO_UPGRADE : SplashActivity.InitialState.APP_UPGRADE_WITH_SETTLE_REQUIRED;
                SplashActivity.Companion.setInitialState(initiState);

                return isReadyToUpdate;
            }
        });
    }


    private void loadLogExistingTransData() {
        Log.i(SplashActivity.TAG, "=====================================================================================================");
        Log.i(SplashActivity.TAG, "\t\tCount : All TransData record\t\t\t\t= " + FinancialApplication.getTransDataDbHelper().countOf());
        Log.i(SplashActivity.TAG, "\t\tCount : Pending Reversal TransData record\t= " + FinancialApplication.getTransDataDbHelper().countOfReversal());
        Log.i(SplashActivity.TAG, "=====================================================================================================");
        Log.i(SplashActivity.TAG, " ");
        Log.i(SplashActivity.TAG, " Checking Date/Time = " + Device.getTime("dd/MM/yyyy HH:mm:ssss"));
        Log.i(SplashActivity.TAG, " ");
        List<TransData> transDataList = FinancialApplication.getTransDataDbHelper().findAllTransData();
        if (transDataList != null && transDataList.size() > 0) {
            Log.i(SplashActivity.TAG, " \t\t TransData infos count : " + FinancialApplication.getTransDataDbHelper().countOf() + " records.");
            Log.i(SplashActivity.TAG, " ");
            for (TransData record : transDataList) {
                try {
                    String transId = "TRANS.ID: "
                            + "S" + Utils.getStringPadding(record.getStanNo(),6, "0", Convert.EPaddingPosition.PADDING_LEFT)
                            + "T" + Utils.getStringPadding(record.getTraceNo(),6, "0", Convert.EPaddingPosition.PADDING_LEFT)
                            + "B" + Utils.getStringPadding(record.getBatchNo(),6, "0", Convert.EPaddingPosition.PADDING_LEFT);
                    String amount = "\tAMT: " + Utils.getStringPadding(record.getAmount(),12, "0", Convert.EPaddingPosition.PADDING_LEFT);
                    String cno = "\tCNO: " + record.getPan();
                    String acquirer = "\tACQUIRER: " + record.getAcquirer().getName();
                    String issuer = "\tISSUER: " + record.getIssuer().getName()  + " (" + record.getIssuer().getIssuerBrand() +")";
                    String type = "\tTRANS.TYPE: " + record.getTransType().getTransName();
                    String appc = "\tAPPCODE: " + record.getAppCode();
                    String athc = "\tAUTHCODE: " + record.getAuthCode();
                    String revs = "\tREVERSAL: " + ((record.getReversalStatus()== TransData.ReversalStatus.REVERSAL) ? "Y" : "N");
                    String voided = "\tVOIDED: " + ((record.getTransState()== TransData.ETransStatus.VOIDED) ? "Y" : "N");

                    Log.i(SplashActivity.TAG, transId + amount + cno + acquirer + issuer + type + appc + athc + revs + voided);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        Log.i(SplashActivity.TAG, " ");
        Log.i(SplashActivity.TAG, "=====================================================================================================");
    }

    // getter
    @org.jetbrains.annotations.Contract(pure = true)
    public static FinancialApplication getApp() {
        return mApp;
    }

    public static SysParam getSysParam() {
        return sysParam;
    }

    public static Controller getController() {
        return controller;
    }

    public static ResponseCode getRspCode() {
        return rspCode;
    }

    public static GeneralParam getGeneralParam() {
        return generalParam;
    }

    public static UserParam getUserParam() {
        return userParam;
    }

    public static AcqManager getAcqManager() {
        return acqManager;
    }

    public static CardBinDb getCardBinDb() {
        return cardBinDb;
    }

    public static EmvDb getEmvDbHelper() {
        return emvDbHelper;
    }

    public static TransDataDb getTransDataDbHelper() { return transDataDbHelper; }

    public static EReceiptDataDb getEReceiptDataDbHelper() { return eReceiptDataDbHelper; }

    public static TransTotalDb getTransTotalDbHelper() {
        return transTotalDbHelper;
    }

    public static TornLogDb getTornLogDbHelper() {
        return tornLogDbHelper;
    }

    public static TransMultiAppDataDb getTransMultiAppDataDbHelper() {
        return transMultiAppDataDbHelper;
    }

    public static IDAL getDal() {
        return dal;
    }

    public static IGL getGl() {
        return gl;
    }

    public static IConvert getConvert() {
        return convert;
    }

    public static IPacker getPacker() {
        return packer;
    }

    public static DownloadManager getDownloadManager() {
        return downloadManager;
    }

    public static String getVersion() {
        return version;
    }

    public static String getApplicationName() {
        ApplicationInfo applicationInfo = getApp().getApplicationInfo();
        int stringId = applicationInfo.labelRes;
        return stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : getApp().getString(stringId);
    }

    public static PedManager getPed() { return pedManager; }

    public static PedManager getPedInstance() { return pedManager.getInstance(EPedType.INTERNAL); }

    // merge handles from all activities
    public void runInBackground(final Runnable runnable) {
        backgroundExecutor.submit(runnable);
    }

    public void runOnUiThread(final Runnable runnable) {
        handler.post(runnable);
    }

    public void runOnUiThreadDelay(final Runnable runnable, long delayMillis) {
        handler.postDelayed(runnable, delayMillis);
    }

    // eventbus helper
    public void register(Object obj) {
        EventBus.getDefault().register(obj);
    }

    public void unregister(Object obj) {
        EventBus.getDefault().unregister(obj);
    }

    public void doEvent(Event event) {
        EventBus.getDefault().post(event);
    }

    public static TemplateLinePayDb getTemplateLinePayDbHelper() {
        return templateLinePayDbHelper;
    }

    public boolean isReadyToUpdate() {
        return isReadyToUpdate;
    }

    public void setReadyToUpdate(boolean readyToUpdate) {
        isReadyToUpdate = readyToUpdate;
        if(isReadyToUpdate){
            Toast.makeText(getApplicationContext(), "App could be updated by PAXSTORE app now.", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(getApplicationContext(), "App is busy, can not be updated by PAXSTORE app now.", Toast.LENGTH_SHORT).show();
        }
    }

    public static EcrProcessClass getEcrProcess() { return EcrProcess; }

    public static void setEcrProcess(EcrProcessClass ecrProcess) {
        EcrProcess = ecrProcess;
    }

    public static UsbSerialManagerClass getUsbSerialManager() {
        return UsbSerialManager;
    }

    public static void setUsbSerialManager(UsbSerialManagerClass usbSerialManager) {
        UsbSerialManager = usbSerialManager;
    }

    public static BaseL920BMCommClass getL920BM_BSSComm() {
        return L920BM_BSSComm;
    }

    public static void setL920BM_BSSComm(BaseL920BMCommClass l920BM_BSSComm) {
        L920BM_BSSComm = l920BM_BSSComm;
    }

    public void setUpdateSp200Action(AAction action) {
        this.updateSp200Action = action;
    }

    public AAction getUpdateSp200Action() {
        return this.updateSp200Action;
    }

    public void setAutoInitAction(AAction action) {
        this.autoInitAction = action;
    }

    public AAction getAutoInitAction() {
        return this.autoInitAction;
    }


    // TODO : SPLASHACTIVITY - AFTER MERGE
    private static void setInitialState(InitState exInitialState) {
        Log.d("InitialState::" ," STATE = " + exInitialState.toString());
        initialState = exInitialState;
    }
    public InitState getInitialState() {return initialState;}
    public void setDownloadParamAction(AAction action) {
        this.downloadParamAction = action;
    }
    //

    public AAction getDownloadParamAction() {
        return this.downloadParamAction;
    }

    /*
    public static void setMultiPath(final MultiPathProgressiveListener listener, String linkPosCommType) {
        if (CommunicationUtils.getSimState() == TelephonyManager.SIM_STATE_READY) {
            String commType = FinancialApplication.getSysParam().get(SysParam.StringParam.COMM_TYPE);
            boolean needInitialize = SysParam.Constant.CommType.MOBILE.toString().equals(commType) &&
                    SysParam.Constant.LinkPosCommType.LINK_POS_WIFI.toString().equals(linkPosCommType);
            if (listener != null && needInitialize) {
                listener.onStart();
            }

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    IDalCommManager commManager = getDal().getCommManager();
                    boolean enableMultipath = commManager.enableMultiPath();
                    if (enableMultipath) {
                        boolean setRoute = false;

                        IChannel wifi = commManager.getChannel(EChannelType.WIFI);
                        IChannel mobile = commManager.getChannel(EChannelType.MOBILE);

                        try {
                            RoutingTable routingTable = RoutingTable.getInstance();
                            List<String> keyList = routingTable.getKeys();

                            wifi.disable();
                            if (!mobile.isEnabled()) {
                                mobile.enable();
                            }

                            boolean mobileLoaded = mobile.isEnabled();
                            while (mobileLoaded == false) {
                                Thread.sleep(500);
                                mobile.enable();
                                mobileLoaded = mobile.isEnabled();
                            }

                            wifi.enable();
                            boolean wifiLoaded = wifi.isEnabled();
                            while (wifiLoaded == false) {
                                Thread.sleep(500);
                                wifi.enable();
                                wifiLoaded = wifi.isEnabled();
                            }

                            Thread.sleep(500);
                            for (String key : keyList) {
                                ERoute route = routingTable.get(key);
                                setRoute = commManager.setRoute(key, route);

                                if (!setRoute) {
                                    Log.e("MainActivity", String.format("Unable to set route for %1$s to [%2$s] interface.", key, route.name()));

                                    if (listener != null) {
                                        listener.onError(route, key);
                                    }

                                    throw new Exception();
                                }
                            }
                        } catch (Exception ex) {
                            enableMultipath = false;
                        }
                    }

                    if (listener != null) {
                        listener.onFinish(enableMultipath);
                    }
                }
            });
            if (needInitialize)
                thread.start();
        }
    }

     */


}
