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
package com.pax.pay;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import com.king.wechat.qrcode.WeChatQRCodeDetector;
import org.opencv.OpenCV;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.pax.abl.core.AAction;
import com.pax.abl.core.AAction.ActionStartListener;
import com.pax.abl.core.ATransaction;
import com.pax.abl.core.ATransaction.TransEndListener;
import com.pax.abl.core.ActionResult;
import com.pax.abl.utils.EncUtils;
import com.pax.appstore.DownloadParamService;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.ECR.EcrData;
import com.pax.pay.ECR.EcrProcessClass;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.MerchantProfile;
import com.pax.pay.constant.Constants;
import com.pax.pay.menu.AlipayMenuActivity;
import com.pax.pay.menu.HistoryMenuActivity;
import com.pax.pay.menu.InstalmentRootMenuActivity;
import com.pax.pay.menu.ManageMenuActivity;
import com.pax.pay.menu.QRCreditMenuActivity;
import com.pax.pay.menu.RedemptionRootMenuActivity;
import com.pax.pay.menu.WechatMenuActivity;
import com.pax.pay.trans.BPSReprintTrans;
import com.pax.pay.trans.BaseTrans;
import com.pax.pay.trans.KplusQrSaleTrans;
import com.pax.pay.trans.LoadTMKTrans;
import com.pax.pay.trans.MyPromptSaleTrans;
import com.pax.pay.trans.OfflineTrans;
import com.pax.pay.trans.PreAuthCancellationTrans;
import com.pax.pay.trans.PreAuthorizationTrans;
import com.pax.pay.trans.RefundTrans;
import com.pax.pay.trans.SaleCompletionTrans;
import com.pax.pay.trans.SaleTrans;
import com.pax.pay.trans.SaleVoidTrans;
import com.pax.pay.trans.SelectMerchantTrans;
import com.pax.pay.trans.SettleTrans;
import com.pax.pay.trans.TipAdjustTrans;
import com.pax.pay.trans.TleStatusTrans;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.action.ActionEReceipt;
import com.pax.pay.trans.action.ActionInputPassword;
import com.pax.pay.trans.action.ActionLockTerminal;
import com.pax.pay.trans.action.activity.DynamicOfflineActivity;
import com.pax.pay.trans.action.activity.SelectMerchantActivity;
import com.pax.pay.trans.model.Controller;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.MerchantProfileManager;
import com.pax.pay.trans.task.SettlementMerchantTask;
import com.pax.pay.uart.BaseL920BMCommClass;
import com.pax.pay.uart.SP200_serialAPI;
import com.pax.pay.utils.CommunicationUtils;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.EReceiptUtils;
import com.pax.pay.utils.MultiMerchantUtils;
import com.pax.pay.utils.TickTimer;
import com.pax.pay.utils.UILanguage;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;
import com.pax.view.MenuPage;
import com.pax.view.dialog.CustomAlertDialog;
import com.pax.view.dialog.DialogUtils;
import com.pax.view.keyboard.CustomKeyboardEditText;

import java.util.Locale;

import th.co.bkkps.dofinAPI.DolfinApi;
import th.co.bkkps.dofinAPI.menu.DolfinMenuActivity;
import th.co.bkkps.edc.receiver.SettleBroadcastReceiver;
import th.co.bkkps.kcheckidAPI.KCheckIDService;
import th.co.bkkps.kcheckidAPI.trans.KCheckIDTrans;
import th.co.bkkps.kcheckidAPI.trans.KCheckIDTrans.Companion.ProcessType;
import th.co.bkkps.linkposapi.LinkPOSApi;
import th.co.bkkps.linkposapi.action.activity.LinkPosAppInitialActivity;
import th.co.bkkps.utils.Log;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.pax.pay.trans.action.ActionSearchCard.SearchMode.INSERT;
import static com.pax.pay.trans.action.ActionSearchCard.SearchMode.KEYIN;
import static com.pax.pay.trans.action.ActionSearchCard.SearchMode.SP200;
import static com.pax.pay.trans.action.ActionSearchCard.SearchMode.SWIPE;
import static com.pax.pay.trans.action.ActionSearchCard.SearchMode.WAVE;

public class MainActivity extends BaseActivity {
    public static final int REQ_SELF_TEST = 1;
    public static final int REQ_ERCM_INITIAL = 2;
    public static final int REQ_TLE_AUTO_DOWNLOAD = 3;
    public static final int REQ_TLE_STAUTS_PRINT = 4;
    private static final String TAG = "MainActivity";
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static TerminalLockCheck lockCheck = TerminalLockCheck.getInstance();
    private static String[] PERMISSIONS_STORAGE = {
            READ_EXTERNAL_STORAGE,
            WRITE_EXTERNAL_STORAGE};
    public NotificationManager notificationManager;
    public CustomAlertDialog alertDialog;
    Context mContext;

    byte mode;
    private boolean needSelfTest = true;
    private ImageView mainMenuLogo;
    private TickTimer tickTimer;
    // AET-87 remove payType
    private CustomKeyboardEditText edtAmount; // input amount
    private MenuPage menuPage;
    private ATransaction.TransEndListener listener = new ATransaction.TransEndListener() {
        @Override
        public void onEnd(ActionResult result) {
            FinancialApplication.getApp().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    resetUI();
                }
            });
        }
    };
    private ProgressDialog progressDialog;
    private boolean setMultipathEnded = false;
    private TextView carrierName;
    private TextView appVersion;
    private TextView merchantName;

    private enum enumSpecialFeatureTypes {PREAUTH, PREAUTH_CANCEL, SALECOMP, OFFLINE, REFUND, TIP_ADJUSTMENT}

    public static void verifyStoragePermissions(Activity activity) {
        try {
            //check permissions
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // request permissions if don't have
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("InitialState::" , "STATE = " + SplashActivity.Companion.getInitialstate().toString());
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        mContext = getApplicationContext();

        if (!lockCheck.getKioskMode()) {
            Intent intent = new Intent(MainActivity.this, LinkPosAppInitialActivity.class);
            startActivity(intent);
        }

        if (!Controller.isFirstInitNeeded() && !FinancialApplication.getSysParam().get(SysParam.BooleanParam.FLAG_UPDATE_PARAM, false)) {
            lockCheck.forceLock();
        }

        verifyStoragePermissions(this);

        if (FinancialApplication.getL920BM_BSSComm() == null) {
            FinancialApplication.setL920BM_BSSComm(new BaseL920BMCommClass("RABBIT", FinancialApplication.getDal().getCommManager()));
        }

        tickTimer = new TickTimer(new TickTimer.OnTickTimerListener() {
            @Override
            public void onTick(long leftTime) {
                Log.i(TAG, "onTick:" + leftTime);
            }

            @Override
            public void onFinish() {
                onTimerFinish();
            }
        });

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
            StrictMode.setThreadPolicy(policy);
        }

        //init opencv
        if (FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_ALIPAY_B_SCAN_C).isEnable()
                || FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_WECHAT_B_SCAN_C).isEnable()) {
            OpenCV.initAsync(this);
            WeChatQRCodeDetector.init(this);
        }
        
        Log.d("MainActivity" , " OnCreate--Success");
    }

    private void CallTestConfig() {
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_PREAUTH, true);
        FinancialApplication.getSysParam().set(SysParam.NumberParam.EDC_NUMBER_OF_DAY_KEEP_PREAUTH_TRANS, 30);
        FinancialApplication.getSysParam().set(SysParam.NumberParam.EDC_MAX_PERCENTAGE_SALE_COMPLETION, 15);
        FinancialApplication.getSysParam().set(SysParam.StringParam.SEC_PREAUTH_PWD, EncUtils.sha1("999009"));

        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_KEYIN, true);
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.d("MainActivity" , " OnStart--Started");
        if (EcrProcessClass.useLinkPos) {
            EcrProcess();
        }

        Intent intent = new Intent("th.co.bkkps.paxsdk.InitialReceiver.ACTION_INIT");
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        sendBroadcast(intent);

        Log.d("MainActivity" , " OnStart--Success");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("MainActivity" , " OnPause--Started");
        if (!Controller.isFirstInitNeeded() && !FinancialApplication.getSysParam().get(SysParam.BooleanParam.FLAG_UPDATE_PARAM, false)) {
            lockCheck.forceLock();
        }
        tickTimer.stop();
//        FinancialApplication.getEcrProcess().mCommManage.StopReceive();//todo linkpos_cz
        EcrData.instance.isOnHomeScreen = false;
        SettleBroadcastReceiver.Companion.setWaitUserCompletePayment(true);

        Log.d("MainActivity" , " OnStart--Success");
    }

    private boolean isOnAutoInitProcess = false;


    @Override
    protected void onResume() {
        super.onResume();

  		Log.d("MainActivity" , " OnResume--Started");
  
        if(Utils.isDebugBuild())
            Device.enableBackKey(true);
		else
			Device.enableBackKey(false);

        Device.enableHomeRecentKey(false);
        Device.enableStatusBar(false);
        enableBackAction(true);

        SettleBroadcastReceiver.Companion.setWaitUserCompletePayment(false);


        if (EcrProcessClass.useLinkPos) {
            LinkPOSApi.INSTANCE.bindService(mContext);
            EcrData.instance.isOnHomeScreen = true;
        }

        //bind Dolfin service
        DolfinApi.getInstance().bindService(mContext);
        if (needSelfTest)
            resetUI();

        if (Controller.isFirstInitNeeded() && !Controller.isFirstRun()) {
            if (isOnAutoInitProcess == true) {
                return;
            }
        }

        String language = FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_LANGUAGE);
        Locale targetLocale = UILanguage.valueOf(language).getLocale();

        Locale currentLocale = mContext.getResources().getConfiguration().locale;
        if (!currentLocale.equals(targetLocale)) {
            Utils.changeAppLanguage(FinancialApplication.getApp(), targetLocale);
        }

        //
        // init SP200
        //
        FinancialApplication.getApp().runInBackground(new Runnable() {
            @Override
            public void run() {
                SP200_serialAPI.getInstance().initSP200();
            }
        });


        //
        //
        //
        if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.NEED_UPDATE_SCREEN_TIMEOUT)) {
            int time = FinancialApplication.getSysParam().get(SysParam.NumberParam.SCREEN_TIMEOUT, -1);
            boolean settingsCanWrite = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? Settings.System.canWrite(this) : true;
            Log.e("main", "settingsCanWrite = " + settingsCanWrite);
            if (!settingsCanWrite) {
                askPermission();
                Log.e("main", "after ask permission");
            } else {
                Log.e("main", "no need ask permission");
                if (time == -1) {
                    time = Integer.MAX_VALUE;
                }
                android.provider.Settings.System.putInt(getContentResolver(),
                        Settings.System.SCREEN_OFF_TIMEOUT, time);
                FinancialApplication.getSysParam().set(SysParam.BooleanParam.NEED_UPDATE_SCREEN_TIMEOUT, false);
            }
        }

        //
        //
        //
        if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.NEED_UPDATE_PARAM)) {
            makeNotification(getString(R.string.notif_param_need_update), getString(R.string.notif_param_req_settle), true);
        } else {
            cancelNotification();
        }

        //
        //
        //
        ActivityStack.getInstance().popTo(this);

        //
        //
        //
        if (lockCheck.getKioskMode()) {
            // check terminal lock
            if (lockCheck.isLocked()) {
                LockTerminal();
            }

            tickTimer.start(lockCheck.getScreenLockTimeoutMs() / 1000);
        }


        if (MerchantProfileManager.INSTANCE.isMultiMerchantEnable()) {
            MerchantProfileManager.INSTANCE.restoreCurrentMerchant();
            mainMenuLogo.setImageBitmap(MultiMerchantUtils.Companion.getMerchantScreenLogoBitmap(null));
            merchantName.setText(MerchantProfileManager.INSTANCE.getCurrentMerchant());
        }


        //
        //
        //
        reConstructMenuItem();

        String carrier = CommunicationUtils.getCarrierName();
        if (carrier != null && !carrier.isEmpty()) {
            carrierName.setText(String.format("%s %s", getString(R.string.default_carrier_name), carrier));
        } else {
            carrierName.setText("");
        }
        appVersion.setText(String.format("%s %s", getString(R.string.default_app_version), FinancialApplication.getVersion()));


        Log.d("MainActivity" , " OnResume--Success");
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onStop() {
        super.onStop();
//        LinkPOSApi.INSTANCE.unbindService(mContext);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(SplashActivity.TAG, "-----onDestroy");
        Device.enableBackKey(true);
        Device.enableHomeRecentKey(true);
        Device.enableStatusBar(true);

        Log.d(SplashActivity.TAG, "-----kill linkpos");
        LinkPOSApi.INSTANCE.setAppLaunched(false);
        ActivityManager activityManager = (ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE);
        activityManager.killBackgroundProcesses(LinkPOSApi.PACKAGE_NAME);
    }

    @Override
    protected void loadParam() {
        //If widget call MainActivity, need to show keyboard immediately.
        Intent intent = getIntent();
        CurrencyConverter.setDefCurrency(FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_CURRENCY_LIST));
    }

    /**
     * reset MainActivity
     */
    private void resetUI() {
        menuPage.setCurrentPager(0);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initViews() {
        if (needSelfTest) {
            if (FinancialApplication.getController().isFirstRun()) {
                //isNeedReInitERCM = true;
            }
            //reloadPublicKeyDataFromFile();
            SelfTestActivity.onSelfTest(MainActivity.this, REQ_SELF_TEST);
        }
        LoadLanguage();
        enableActionBar(false);
        LinearLayout mLayout = (LinearLayout) findViewById(R.id.ll_gallery);
        menuPage = createMenu();
        mLayout.addView(menuPage);
        mainMenuLogo = (ImageView) findViewById(R.id.main_menu_logo);
        mainMenuLogo.setImageResource(R.drawable.kasikornbanklogo);
        carrierName = findViewById(R.id.carrier_name);
        appVersion = findViewById(R.id.app_version);
        merchantName = findViewById(R.id.merchant_name);
        merchantName.setText("Waiting for Merchant Name...");
    }

    private void reloadPublicKeyDataFromFile() {
        try {
            ActionEReceipt actionEReceipt = new ActionEReceipt(null);
            actionEReceipt.readAndSetKeyVersion();
        } catch (Exception ex) {
            Log.e(EReceiptUtils.TAG, "Unable to reload PublicKeyData from File");
        }
    }

    protected void reConstructMenuItem() {
        LinearLayout mLayout = (LinearLayout) findViewById(R.id.ll_gallery);
        if (menuPage.getChildCount() > 0) {
            menuPage.removeAllViews();
        }
        menuPage = createMenu();
        mLayout.addView(menuPage);
    }

    /**
     * create menu
     */
    private MenuPage createMenu() {
        Utils.setEnableAcquirer();
        MenuPage.Builder builder = new MenuPage.Builder(MainActivity.this, 9, 3);

        // Select Merchant
        if (MerchantProfileManager.INSTANCE.isMultiMerchantEnable()) {
            //builder.addMenuItem(getString(R.string.menu_select_merchant), R.drawable.app_manage, SelectMerchantActivity.class);
            builder.addTransItem(getString(R.string.menu_select_merchant), R.drawable.app_manage, new SelectMerchantTrans(MainActivity.this));
        }

        // Sale
        mode = (SWIPE | INSERT | WAVE | KEYIN);
        if (Utils.isEnableSaleNormal()) {
            builder.addTransItem(getString(R.string.menu_sale), R.drawable.app_sale,
                    new SaleTrans(MainActivity.this, null, mode, null, true, listener));
        }

        // void
        if (Utils.isEnableVoid()) {
            builder.addTransItem(getString(R.string.menu_void), R.drawable.app_void, new SaleVoidTrans(MainActivity.this, listener));
        }

        if (Utils.isEnableKplus()) {
//            builder.addMenuItem("K+", R.drawable.app_sale, KplusMenuActivity.class);
            builder.addTransItem(getString(R.string.trans_kplus), R.drawable.icon_thaiqr, new KplusQrSaleTrans(MainActivity.this, listener));
        }

        if (Utils.isEnableMyPromt()) {
             builder.addTransItem(getString(R.string.trans_my_prompt_qr), R.drawable.icon_thaiqr,
                new MyPromptSaleTrans(MainActivity.this,  ETransType.QR_MYPROMPT_SALE, listener));
        }

        if (Utils.isEnableAlipay()||Utils.isEnableAlipayBscanC()) {
            builder.addMenuItem(getString(R.string.trans_alipay), R.drawable.icon_alipay, AlipayMenuActivity.class);
        }

        if (Utils.isEnableWechat()||Utils.isEnableWechatBscanC()) {
            builder.addMenuItem(getString(R.string.trans_wechat), R.drawable.icon_wechatpay, WechatMenuActivity.class);
        }

        if (Utils.isEnableQRCredit()) {
            builder.addMenuItem(getString(R.string.trans_qr_credit), R.drawable.app_sale, QRCreditMenuActivity.class);
        }

        if (Utils.isEnableInstalmentRootMenu()) {
            builder.addMenuItem(getString(R.string.menu_instalment_str), R.drawable.app_sale, InstalmentRootMenuActivity.class);
        }

        if (Utils.isEnableRedeemRootMenu()) {
            builder.addMenuItem(getString(R.string.menu_redemption_str), R.drawable.app_sale, RedemptionRootMenuActivity.class);
        }
//
//        if (Utils.isEnableInstalmentKbank() || Utils.isEnableInstalmentKbankBdms()) {
//            builder.addMenuItem(getString(R.string.menu_instalment), R.drawable.icon_smartpay, InstalmentMenuActivity.class);
//        }
//
//        if (Utils.isEnableRedeemKbank()) {
//            builder.addMenuItem(getString(R.string.menu_redemption), R.drawable.icon_rewardpoint, RedeemedMenuActivity.class);
//        }

        if (MultiMerchantUtils.Companion.isMasterMerchant() && KCheckIDService.Companion.isKCheckIDInstalled(this) && Utils.isEnableKCheckId()) {
            //builder.addMenuItem(getString(R.string.menu_ekyc_kcheckid), R.drawable.icon_kcheckid, KCheckIDMenuActivity.class);
            builder.addTransItem( getString(R.string.menu_ekyc_kcheckid), R.drawable.icon_kcheckid,  new KCheckIDTrans(this, ETransType.KCHECKID_DUMMY, ProcessType.VERIFY, null));
            builder.addTransItem( getString(R.string.menu_ekyc_kcheckid_inquiry), R.drawable.verify_status, new KCheckIDTrans(this, ETransType.KCHECKID_DUMMY, ProcessType.INQUIRY, null));

        }

//        if (Utils.isEnableBay()) {
//            builder.addMenuItem(getString(R.string.menu_bay_ipp), R.drawable.icons_bay, SubMenuActivityBay.class);
//        }

//        if (Utils.isEnableAmexInstalment()) {
//            builder.addTransItem(getString(R.string.menu_instalment_amex), R.drawable.icon_amex,
//                    new InstalmentAmexTrans(MainActivity.this, (byte) (SWIPE | INSERT | KEYIN), true, listener));
//        }

//        if (Utils.isEnableScbIpp() || Utils.isEnableScbRedeem()) {
//            builder.addMenuItem(getString(R.string.menu_scb), R.drawable.icons_scb, ScbMenuActivity.class);
//        }

        // Dolfin
        if (MultiMerchantUtils.Companion.isMasterMerchant() && (DolfinApi.getInstance().getDolfinServiceBinded() && Utils.isEnableDolfin())
                || Utils.isEnableDolfinInstalment())
        {
            builder.addMenuItem(getString(R.string.menu_dolfin), R.drawable.dolfin_icon, DolfinMenuActivity.class);
        }

        // PreAuth
        if (MultiMerchantUtils.Companion.isMasterMerchant() && FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_PREAUTH, false)) {
            builder.addActionItem(getString(R.string.menu_preauth_sale), R.drawable.app_sale, verifyInputPasswordForSpecialFeature(enumSpecialFeatureTypes.PREAUTH, listener));
        }

        // Sale Completion
        if (MultiMerchantUtils.Companion.isMasterMerchant() && FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_PREAUTH, false)) {
            builder.addActionItem(getString(R.string.menu_sale_completion), R.drawable.app_sale, verifyInputPasswordForSpecialFeature(enumSpecialFeatureTypes.SALECOMP, listener));
        }

        // PreAuthCancellation
        if (MultiMerchantUtils.Companion.isMasterMerchant() && FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_PREAUTH, false)
                && FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_UP).isEnable()) {
            builder.addActionItem(getString(R.string.menu_preauth_cancel), R.drawable.app_void, verifyInputPasswordForSpecialFeature(enumSpecialFeatureTypes.PREAUTH_CANCEL, listener));
        }

        //offline
        if (MultiMerchantUtils.Companion.isMasterMerchant() && FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_OFFLINE, false)) {
            builder.addTransItem(getString(R.string.menu_offline), R.drawable.app_sale,
                    new OfflineTrans(MainActivity.this, (byte) (INSERT), true, listener));
        }

        // refund
        if (MultiMerchantUtils.Companion.isMasterMerchant() && FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_SUPPORT_REFUND, false)) {
            builder.addTransItem(getString(R.string.menu_refund), R.drawable.app_refund,
                    new RefundTrans(MainActivity.this, (byte) (SWIPE | INSERT | KEYIN), true, listener));
        }

        //Tip adjust
        if (MultiMerchantUtils.Companion.isMasterMerchant() && FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_TIP_ADJUST, false)) {
            builder.addTransItem(getString(R.string.menu_adjust), R.drawable.app_adjust,
                    new TipAdjustTrans(MainActivity.this, listener));
        }


        // Reprint
        builder.addTransItem(getString(R.string.menu_reprint), R.drawable.app_print, new BPSReprintTrans(MainActivity.this, listener));

        //Report (History)
        builder.addMenuItem(getString(R.string.menu_report), R.drawable.app_query, HistoryMenuActivity.class);

        // Settlement
        if (MerchantProfileManager.INSTANCE.isMultiMerchantEnable()) {
            builder.addTransItem(getString(R.string.menu_settle), R.drawable.app_settle, new SettlementMerchantTask(MainActivity.this, new TransEndListener() {
                @Override
                public void onEnd(ActionResult result) {
                    ActivityStack.getInstance().popTo(MainActivity.class);
                }
            }));
        } else {
            builder.addTransItem(getString(R.string.menu_settle), R.drawable.app_settle, getSettleTran());
        }
        // management
        builder.addActionItem(getString(R.string.menu_manage), R.drawable.app_manage, createInputPwdActionForManagement());

        if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.KBANK_DYNAMIC_OFFLINE_SHOW_MENU)) {
            builder.addMenuItem(getString(R.string.menu_dynamic_offline), R.drawable.dnmoffln_main, DynamicOfflineActivity.class);
        }

        if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_KIOSK_MODE)) {
            builder.addActionItem(getString(R.string.trans_lock), R.drawable.pwd_small, new ActionLockTerminal(new AAction.ActionStartListener() {
                @Override
                public void onStart(AAction action) {
                    ((ActionLockTerminal) action).setParam(MainActivity.this);
                }
            }));
        }

        return builder.create();
    }

    @Override
    protected void setListeners() {
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_SELF_TEST) {
            if (resultCode == RESULT_CANCELED) {
                finish();
                return;
            }
            needSelfTest = false;

            if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.FLAG_UPDATE_PARAM, false)) {
                Log.d(TAG, "Call DownloadParamService");
                Intent intentDownloadParam = new Intent(mContext, DownloadParamService.class);
                intentDownloadParam.putExtra("downloadParam", "true");
                intentDownloadParam.putExtra("NEED_RESTART", true);
                mContext.startService(intentDownloadParam);
            }
        } else if (requestCode == REQ_ERCM_INITIAL) {
            ReInitialERCM();
        } else if (requestCode == REQ_TLE_AUTO_DOWNLOAD) {
            ReInitTLEAutoDownload();
        } else if (requestCode == REQ_TLE_STAUTS_PRINT) {
            RePrintTLEStatus();
        }
    }

    @Override
    protected boolean onKeyBackDown() {
        // exit current app
        DialogUtils.showExitAppDialog(MainActivity.this);
        return true;
    }

    private AAction verifyInputPasswordForSpecialFeature(enumSpecialFeatureTypes type, TransEndListener localListener) {

        BaseTrans transaction = null;

        ActionInputPassword inputPasswordAction = new ActionInputPassword(new ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                tickTimer.stop();
                EcrData.instance.isOnHomeScreen = false;

                String displayCaption = null;
                switch (type) {
                    case PREAUTH:           displayCaption = getString(R.string.prompt_preauth_pwd); break;
                    case PREAUTH_CANCEL:    displayCaption = getString(R.string.prompt_preauth_cancel_pwd); break;
                    case SALECOMP:          displayCaption = getString(R.string.prompt_salecomp_pwd); break;
                    default:
                        return;
                }
                ((ActionInputPassword) action).setParam(MainActivity.this, 6, displayCaption, null);
            }
        });

        inputPasswordAction.setEndListener(new AAction.ActionEndListener() {
            @Override
            public void onEnd(AAction action, ActionResult result) {
                TransContext.getInstance().setCurrentAction(null); //fix leaks
                tickTimer.start(); // reset main Activity timer
                if (result.getRet() != TransResult.SUCC) {
                    EcrData.instance.isOnHomeScreen = true;
                    return;
                }

                String displayCaption = null;
                SysParam.StringParam paramPwdHash = null;
                switch (type) {
                    case PREAUTH:        paramPwdHash = SysParam.StringParam.SEC_PREAUTH_PWD;        displayCaption = getString(R.string.menu_preauth_sale);    break;
                    case PREAUTH_CANCEL: paramPwdHash = SysParam.StringParam.SEC_PREAUTH_PWD;        displayCaption = getString(R.string.menu_preauth_cancel);  break;
                    case SALECOMP:       paramPwdHash = SysParam.StringParam.SEC_PREAUTH_PWD;        displayCaption = getString(R.string.menu_sale_completion); break;
                    default:
                        return;
                }

                String userPwdHash = EncUtils.sha1((String) result.getData());
                if (!userPwdHash.equals(FinancialApplication.getSysParam().get(paramPwdHash))) {
                        DialogUtils.showErrMessage(MainActivity.this, displayCaption,
                                getString(R.string.err_password), dialog -> EcrData.instance.isOnHomeScreen = true, Constants.FAILED_DIALOG_SHOW_TIME);
                    return;
                }

                switch (type) {
                    case PREAUTH:
                        mode = SWIPE | INSERT;
                        //if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_KEYIN, false))  { mode |= KEYIN; }
                        if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_SUPPORT_SP200,false)) {
                            mode |= SP200 | WAVE;
                        } else if(FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_CONTACTLESS, false)) {
                            mode |= WAVE;
                        }

                        PreAuthorizationTrans preAuthTrans = new PreAuthorizationTrans(MainActivity.this, ETransType.PREAUTHORIZATION, mode, true, localListener);
                        preAuthTrans.execute();
                        break;
                    case PREAUTH_CANCEL:
                        PreAuthCancellationTrans preAuthCancelTrans = new PreAuthCancellationTrans(MainActivity.this, ETransType.PREAUTHORIZATION_CANCELLATION, localListener);
                        preAuthCancelTrans.execute();
                        break;
                    case SALECOMP:
                        SaleCompletionTrans saleCompTrans = new SaleCompletionTrans(MainActivity.this, ETransType.SALE_COMPLETION, localListener);
                        saleCompTrans.execute();
                        break;
                }
            }
        });

        return inputPasswordAction;
    }

    private AAction createInputPwdActionForManagement() {
        ActionInputPassword inputPasswordAction = new ActionInputPassword(new ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                tickTimer.stop();
                EcrData.instance.isOnHomeScreen = false;
                ((ActionInputPassword) action).setParam(MainActivity.this, 12,
                        getString(R.string.prompt_sys_pwd), null);
            }
        });

        inputPasswordAction.setEndListener(new AAction.ActionEndListener() {

            @Override
            public void onEnd(AAction action, ActionResult result) {
                TransContext.getInstance().setCurrentAction(null); //fix leaks
                tickTimer.start(); // reset main Activity timer
                if (result.getRet() != TransResult.SUCC) {
                    EcrData.instance.isOnHomeScreen = true;
                    return;
                }
/*
                String data = EncUtils.sha1((String) result.getData());
                int currentPwdFailedCount = FinancialApplication.getSysParam().get(SysParam.NumberParam.PASSWORD_FAILED_COUNT);
                if (!data.equals(FinancialApplication.getSysParam().get(SysParam.StringParam.SEC_SYS_PWD))) {
                    if (currentPwdFailedCount++ < 5) {
                        DialogUtils.showErrMessage(MainActivity.this, getString(R.string.menu_manage),
                            getString(R.string.err_password), dialog -> EcrData.instance.isOnHomeScreen = true, Constants.FAILED_DIALOG_SHOW_TIME);
                    } else {
                        displayWaitScreenForAuthenFailed();
                    }

                    FinancialApplication.getSysParam().set(SysParam.NumberParam.PASSWORD_FAILED_COUNT, currentPwdFailedCount);
                    return;
                }


 */
                FinancialApplication.getSysParam().set(SysParam.NumberParam.PASSWORD_FAILED_COUNT, 0);
                Intent intent = new Intent(MainActivity.this, ManageMenuActivity.class);
                startActivity(intent);
            }
        });

        return inputPasswordAction;
    }

    private void displayWaitScreenForAuthenFailed() {

        Device.beepErr();

        ProgressDialog pgsDialog = ProgressDialog.show(this, "Authentication Failed > 5 times", "please wait...");
        pgsDialog.show();

        CountDownTimer countDownTimer =  new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int remainSec = Math.round(millisUntilFinished / 1000l);
                pgsDialog.setMessage("please wait... ( " + remainSec + " Sec.)");
            }

            @Override
            public void onFinish() {
                pgsDialog.setMessage("please wait...( 0 Sec.)");
                pgsDialog.dismiss();
            }
        };
        countDownTimer.start();
    }

    private void LoadLanguage() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(FinancialApplication.getApp());
        String language = prefs.getString(Constants.USER_LANG, null);
        Log.d("LoadLanguage", (language == null) ? "NULL" : language);
        if (language == null) {
            language = "en";
        }

        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    void EcrProcess() {
        if (FinancialApplication.getEcrProcess() == null) {
            FinancialApplication.setEcrProcess(new EcrProcessClass(MainActivity.this));
            if (FinancialApplication.getEcrProcess() != null) {
                FinancialApplication.getEcrProcess().onBaseConnect();
            }
        }
    }

    private void makeNotification(String title, String content, boolean ongoing) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_bps_gray)
                        .setColor(getResources().getColor(R.color.primary))
                        .setLargeIcon(BitmapFactory.decodeResource(this.getResources(),
                                R.drawable.kaset_logo))
                        .setContentTitle(title)
                        .setContentText(content)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setDefaults(Notification.DEFAULT_VIBRATE)
                        .setOngoing(ongoing)
                        .setOnlyAlertOnce(false)
                        .setFullScreenIntent(pendingIntent, true)
                        .setContentIntent(pendingIntent);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(Constants.NOTIFICATION_ID_PARAM, mBuilder.build());
    }

    private void cancelNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            StatusBarNotification[] barNotifications = notificationManager.getActiveNotifications();
            for (StatusBarNotification notification : barNotifications) {
                if (notification.getId() == Constants.NOTIFICATION_ID_PARAM) {
                    //return notification.getNotification();
                    notificationManager.cancel(Constants.NOTIFICATION_ID_PARAM);
                }
            }
        } else {
            try {
                notificationManager.cancel(Constants.NOTIFICATION_ID_PARAM);
            } catch (Exception e) {
                Log.w(TAG, "e:" + e);
            }
        }

    }

    private void askPermission() {
        if (alertDialog != null && alertDialog.isShowing()) return;

        //final CustomAlertDialog customAlertDialog = new CustomAlertDialog(ActivityStack.getInstance().top(), CustomAlertDialog.NORMAL_TYPE);
        alertDialog = new CustomAlertDialog(ActivityStack.getInstance().top(), CustomAlertDialog.NORMAL_TYPE);
        alertDialog.setCancelClickListener(new CustomAlertDialog.OnCustomClickListener() {
            @Override
            public void onClick(CustomAlertDialog alertDialog) {
                alertDialog.dismiss();
            }
        });
        alertDialog.setConfirmClickListener(new CustomAlertDialog.OnCustomClickListener() {

            @Override
            public void onClick(CustomAlertDialog alertDialog) {
                alertDialog.dismiss();
                Device.enableBackKey(true);
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
        alertDialog.show();
        alertDialog.setNormalText(getString(R.string.err_request_modify_setting));
        alertDialog.showCancelButton(true);
        alertDialog.showConfirmButton(true);
    }

    protected void onTimerFinish() {
        LockTerminal();
    }

    protected void LockTerminal() {
        if (!Controller.isFirstRun() && !Controller.isFirstInitNeeded() && !FinancialApplication.getSysParam().get(SysParam.BooleanParam.FLAG_UPDATE_PARAM, false)) {
            Intent intent = new Intent(MainActivity.this, InputPwdActivity.class);
            MainActivity.this.startActivity(intent, null);
        }
    }

    protected void ReInitialERCM() {
        ActionEReceipt eReceiptActionInit = new ActionEReceipt(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionEReceipt) action).setParam(ActionEReceipt.eReceiptMode.INIT_TERMINAL, MainActivity.this, FinancialApplication.getDownloadManager().getSn(), null);
                ((ActionEReceipt) action).setExtraParam(true);
                Log.d("INIT*", "ERCM--AUTO--INIT--START");
            }
        });
        eReceiptActionInit.setEndListener(new AAction.ActionEndListener() {
            @Override
            public void onEnd(AAction action, ActionResult result) {
                if (result.getRet() == TransResult.SUCC) {
                    DialogUtils.showSuccMessage(MainActivity.this, " ", null, Constants.SUCCESS_DIALOG_SHOW_TIME);
                } else {
                    DialogUtils.showErrMessage(MainActivity.this, "ERCM Initial Error", Utils.getString(TransResult.ERCM_INITIAL_PROCESS_FAILED), null, Constants.FAILED_DIALOG_SHOW_TIME);
                }
                ActivityStack.getInstance().popTo(MainActivity.this);
                onActivityResult(REQ_TLE_AUTO_DOWNLOAD, result.getRet(), null);
            }
        });
        eReceiptActionInit.execute();

    }

    private void ReInitTLEAutoDownload() {
        TransEndListener transEndListener = new TransEndListener() {
            @Override
            public void onEnd(ActionResult result) {
                ActivityStack.getInstance().popTo(MainActivity.this);
                onActivityResult(REQ_TLE_STAUTS_PRINT, result.getRet(), null);
            }
        };

        LoadTMKTrans TLE_autoInit = new LoadTMKTrans(FinancialApplication.getApp().getApplicationContext(), transEndListener, true);
        TLE_autoInit.execute();
    }

    private void RePrintTLEStatus() {
        TransEndListener transEndListener = new TransEndListener() {
            @Override
            public void onEnd(ActionResult result) {
                //onActivityResult(REQ_TLE_STAUTS_PRINT, result.getRet(), null);
                ActivityStack.getInstance().popTo(MainActivity.this);
            }
        };
        TleStatusTrans tleStatusTrans = new TleStatusTrans(FinancialApplication.getApp().getApplicationContext(), transEndListener, true, 4);
        tleStatusTrans.execute();
    }

    private SettleTrans getSettleTran(){
        return new SettleTrans(MainActivity.this, new TransEndListener() {
            @Override
            public void onEnd(ActionResult result) {
                if (result.getRet() != TransResult.SUCC) {
                    return;
                }

                boolean paramResult;
                if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.NEED_UPDATE_PARAM)) {
                    paramResult = FinancialApplication.getDownloadManager().handleSuccess(mContext);
                    cancelNotification();
                    FinancialApplication.getSysParam().set(SysParam.BooleanParam.NEED_UPDATE_PARAM, false);
                    if (paramResult) {
                        makeNotification(getString(R.string.notif_param_complete), getString(R.string.notif_param_success), false);
                    } else {
                        makeNotification(getString(R.string.notif_param_fail), getString(R.string.notif_param_call_bank), false);
                    }
                }
            }
        });
    }
}
