/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-11-27
 * Module Author: Steven.W
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.utils;

import android.content.Context;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.StringRes;
import com.pax.edc.R;
import com.pax.pay.app.FinancialApplication;

public class ToastUtils {

    /**
     * old message
     */
    private static String oldMsg;
    /**
     * Toast object
     */
    private static Toast toast = null;
    /**
     * first time
     */
    private static long oneTime = 0;
    /**
     * second time
     */
    private static long twoTime = 0;

    private ToastUtils() {
        //do nothing
    }

    public static void showMessage(@StringRes int strId) {
        showMessage(FinancialApplication.getApp(), FinancialApplication.getApp().getString(strId));
    }

    public static void showMessage(String message) {
        showMessage(FinancialApplication.getApp(), message);
    }

    public static void showMessage(Context context, String message) {
        LayoutInflater inflate = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflate.inflate(R.layout.toast_layout, null);
        TextView textView = (TextView) view.findViewById(R.id.message);
        if (toast == null) {
            textView.setText(message);
            toast = new Toast(context);
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);// set gravity center
            toast.setView(view);
            toast.show();
            oneTime = System.currentTimeMillis();
        } else {
            twoTime = System.currentTimeMillis();

            if (message.equals(oldMsg)) {
                if (twoTime - oneTime > Toast.LENGTH_SHORT) {
                    toast.show();
                }
            } else {
                oldMsg = message;
                textView.setText(message);
                toast.setView(view);
                toast.show();
            }
        }

        oneTime = twoTime;
    }

    public static void makeText(Context context, String message, long timeout) {
        toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        CountDownTimer cd;
        cd = new CountDownTimer(timeout, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                toast.show();
            }

            @Override
            public void onFinish() {
                toast.cancel();
            }
        };
        toast.show();
        cd.start();
    }
}
