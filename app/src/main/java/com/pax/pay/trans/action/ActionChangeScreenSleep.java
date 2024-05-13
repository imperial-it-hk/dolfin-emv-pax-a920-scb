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
package com.pax.pay.trans.action;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import th.co.bkkps.utils.Log;
import android.widget.Button;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.utils.TickTimer;
import com.pax.pay.utils.Utils;
import com.pax.view.dialog.CustomAlertDialog;
import com.pax.view.dialog.DialogUtils;

import java.util.Locale;

import static com.pax.pay.utils.Utils.getString;

public class ActionChangeScreenSleep extends AAction {
    private Context context;
    private String title;
    private boolean allowCanceledOnTouchOutside = true;
    private int backKeyResult = TransResult.ERR_ABORTED;

    private Locale locale = null;

    private ProcessRunnable processRunnable = null;

    public ActionChangeScreenSleep(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(int backKeyResult) {
        this.backKeyResult = backKeyResult;
    }

    public void setParam(Context context, String title, boolean allowCanceledOnTouchOutside) {
        this.context = context;
        this.title = title;
        this.allowCanceledOnTouchOutside = allowCanceledOnTouchOutside;
    }

    @Override
    protected void process() {
        processRunnable = new ProcessRunnable();
        FinancialApplication.getApp().runOnUiThreadDelay(processRunnable, 100);
    }

    @Override
    public void setResult(ActionResult result) {
        if (processRunnable != null && result.getRet() == TransResult.ERR_TIMEOUT)
            processRunnable.dialog.dismiss();
        else
            super.setResult(result);
    }

    private class ProcessRunnable implements Runnable {
        AlertDialog dialog = null;
        protected TickTimer tickTimer;

        ProcessRunnable() {

        }

        private void onTimerFinish() {
            setResult(new ActionResult(TransResult.ERR_ABORTED, null));
        }

        @Override
        public void run() {
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
            tickTimer.start(30);

            AlertDialog.Builder builder = new AlertDialog.Builder(context);

            builder.setTitle(title);

            String[] sleepTime =  context.getResources().getStringArray(R.array.screen_sleep_time);
            final String[] sleepTimeMills =  context.getResources().getStringArray(R.array.screen_sleep_time_mills);

            final int[] current = {0};
            int item = -1;
            final int[] time = new int[1];
            try {
                current[0] = Settings.System.getInt(context.getContentResolver(),
                        Settings.System.SCREEN_OFF_TIMEOUT);
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }

            if(current[0] == Integer.MAX_VALUE){
                item = sleepTime.length - 1;
            }else{
                for (int i=0;i<sleepTimeMills.length;i++) {
                    if (sleepTimeMills[i].equals(String.valueOf(current[0]))) {

                        item = i;
                        break;
                    }
                }
            }

            builder.setSingleChoiceItems(sleepTime, item, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {

                    time[0] = Integer.parseInt(sleepTimeMills[which]);

                    if(time[0] == -1){
                        time[0] = Integer.MAX_VALUE;
                    }

                    if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
                        boolean settingsCanWrite = Settings.System.canWrite(context);
                        if(!settingsCanWrite) {
                            showPermissionDialog();
                            current[0] = time[0];
                        }else {
                            // If has permission then show an alert dialog with message.
                            changeSystemTimeout(time[0]);
                            close(dialog);
                        }
                    }else{
                        changeSystemTimeout(time[0]);
                        close(dialog);
                    }
                    //setResult(new ActionResult(TransResult.SUCC, null));
                   // close(dialog);
                }


            });

            final int finalCurrent = current[0];
            builder.setNeutralButton(context.getString(R.string.dialog_ok),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
                                boolean settingsCanWrite = Settings.System.canWrite(context);
                                if(!settingsCanWrite) {
                                    showPermissionDialog();
                                }else{
                                    changeSystemTimeout(finalCurrent);
                                    close(dialog);
                                }
                            }else{
                                changeSystemTimeout(finalCurrent);
                                close(dialog);
                            }
                            tickTimer.stop();
                        }
                    });

            builder.setPositiveButton(context.getString(R.string.dialog_cancel),
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            tickTimer.stop();
                            setResult(new ActionResult(TransResult.ERR_ABORTED, null));
                            close(dialog);
                            return;
                        }
                    });

            builder.setCancelable(false);
            dialog = builder.show();
        }

        private void close(DialogInterface dialog) {
            dialog.dismiss();
        }

    }

    private void changeSystemTimeout(int time){
        boolean result = android.provider.Settings.System.putInt(context.getContentResolver(),
                Settings.System.SCREEN_OFF_TIMEOUT, time);

        if(result){
            setResult(new ActionResult(TransResult.SUCC, null));
            DialogUtils.showSuccMessage(context, title, null,
                    Constants.SUCCESS_DIALOG_SHOW_TIME);
        }else{
            DialogUtils.showErrMessage(context,"",getString(R.string.err_change_screen_timeout),null, Constants.FAILED_DIALOG_SHOW_TIME);
        }
    }

    private void showPermissionDialog(){
            final CustomAlertDialog alertDialog = new CustomAlertDialog(context, CustomAlertDialog.NORMAL_TYPE);
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
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    intent.setData(Uri.parse("package:" + context.getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            });
            alertDialog.show();
            alertDialog.setNormalText(getString(R.string.err_request_modify_setting));
            alertDialog.showCancelButton(true);
            alertDialog.showConfirmButton(true);
    }
}

