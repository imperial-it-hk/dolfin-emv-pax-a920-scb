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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import th.co.bkkps.utils.Log;
import android.view.KeyEvent;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.TickTimer;
import com.pax.pay.utils.UILanguage;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;
import com.pax.view.dialog.InputPwdDialog;
import com.pax.view.dialog.InputPwdDialog.OnPwdListener;

import java.util.Locale;

public class ActionChangeLanguage extends AAction {
    private Context context;
    private String title;
    private boolean allowCanceledOnTouchOutside = true;
    private int backKeyResult = TransResult.ERR_ABORTED;

    private Locale locale = null;

    private ProcessRunnable processRunnable = null;

    public ActionChangeLanguage(ActionStartListener listener) {
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

            String[] languages =  context.getResources().getStringArray(R.array.languages);

            builder.setSingleChoiceItems(languages, -1, new DialogInterface.OnClickListener() {

               @Override
                public void onClick(DialogInterface dialog, int which) {
                    String language = null;

                    if(which == 1){
                        locale = UILanguage.THAI.getLocale(); //new Locale("th");
                        language = UILanguage.THAI.getDisplay();
                        Utils.changeAppLanguage(context,locale);
                    }else{
                        locale = UILanguage.ENGLISH.getLocale(); //Locale.US;
                        language = UILanguage.ENGLISH.getDisplay();
                        Utils.changeAppLanguage(context,locale);
                    }

                    String key = FinancialApplication.getApp().getString(R.string.EDC_LANGUAGE);
                    saveString(key, language);
                    Utils.restart();
                    setResult(new ActionResult(TransResult.SUCC, null));
                    close(dialog);
                }
            });

            builder.setPositiveButton(context.getString(R.string.dialog_cancel),
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            setResult(new ActionResult(TransResult.ERR_ABORTED, null));
                            tickTimer.stop();
                            close(dialog);
                            return;
                        }
                    });
            builder.setCancelable(false);
            //builder.create().show();
            dialog = builder.show();
        }

        private void close(DialogInterface dialog) {
            tickTimer.stop();
            dialog.dismiss();
        }

    }

    public void saveString(String name, String value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(FinancialApplication.getApp());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(name,value);
        editor.commit();
    }
}

