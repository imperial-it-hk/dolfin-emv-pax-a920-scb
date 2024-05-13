package com.pax.appstore;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.j256.ormlite.stmt.query.In;
import com.pax.pay.MainActivity;
import com.pax.pay.SplashActivity;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.trans.model.Controller;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import th.co.bkkps.utils.Log;

public class DownloadParamReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(SplashActivity.TAG, "======================================================================");
        Log.d(SplashActivity.TAG, "                   [ BROADCAST RECEIVER : RECEIVED]                   ");
        Log.d(SplashActivity.TAG, "======================================================================");

        // Clear Existing activity in activity stack
        ActivityStack.getInstance().popAll();
        Log.d(SplashActivity.TAG, "  > Pop-all-activity ");

        // set download parameter is require to download --> SplashActivity
        Controller.set(Controller.IS_FIRST_DOWNLOAD_PARAM_NEEDED, true);
        Log.d(SplashActivity.TAG, "  > Set Required_param_download = true");
        Log.d(SplashActivity.TAG, "  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
        Log.d(SplashActivity.TAG, "  > Controller.IS_FIRST_RUN = " + Controller.isFirstRun());
        Log.d(SplashActivity.TAG, "  > Controller.IS_FIRST_DOWNLOAD_PARAM_NEEDED = " + Controller.isRequireDownloadParam());
        Log.d(SplashActivity.TAG, "  > Controller.IS_FIRST_INITIAL_NEEDED = " + Controller.isFirstInitNeeded());
        Log.d(SplashActivity.TAG, "  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");

        // set SplashActivity.companion object to state : download_param_only
        SplashActivity.Companion.setInitialState(SplashActivity.InitialState.DOWNLOAD_PARAM_ONLY);

        Log.d(SplashActivity.TAG, "  > Start ::  SplashActivity ");

        // Intent new activity to Splash Activity & keep log
        Intent intentParamDownload = new Intent(context, SplashActivity.class);
        intentParamDownload.putExtra("RequestType" , SplashActivity.REQ_DOWNLOAD_PARAM);
        intentParamDownload.putExtra( "NeedRestart" , true);
        context.startActivity(intentParamDownload);
        Log.d(SplashActivity.TAG, "-----DownloadParamReceiver---Start");
        Log.d(SplashActivity.TAG, "======================================================================");



//        Log.d("log", "broadcast received");
//        if (Controller.isFirstRun()) {
//            Log.d("DownloadParamReceiver", "Waiting for Self-Test Initialization...");
//            FinancialApplication.getSysParam().set(SysParam.BooleanParam.FLAG_UPDATE_PARAM, true);
//            Controller.set(Controller.IS_FIRST_INITIAL_NEEDED, false);
//            Utils.restart();
//            return;
//        }
//        Intent intentDownloadParam = new Intent(context, DownloadParamService.class);
//        intentDownloadParam.putExtra("downloadParam","true");
//        intentDownloadParam.putExtra("NEED_RESTART", true);
//        context.startService(intentDownloadParam);
//        /*Log.i("DownloadParamReceiver", "broadcast received");
//        //todo receive the broadcast from paxstore, start a service to download parameter files
//        context.startService(new Intent(context, DownloadParamService.class));*
    }
}
