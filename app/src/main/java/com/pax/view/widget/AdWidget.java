/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-3-8
 * Module Author: huangwp
 * Description:
 *
 * ============================================================================
 */
package com.pax.view.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.AppWidgetTarget;
import com.pax.edc.R;
import com.pax.pay.WebViewActivity;
import com.pax.pay.constant.AdConstants;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.ALARM_SERVICE;

public class AdWidget extends BaseWidget {

    public static final String ACTION_UPDATE_ALL = "com.pax.pay.AdWidget.UPDATE_ALL";
    private static final String TAG = "AdWidget";

    private static final long UPDATE_PERIOD = (long) 5 * 1000;     //5s
    private PendingIntent pi;
    private AlarmManager am;

    private static final List<String> MAP_KEY_LIST = new ArrayList<>(AdConstants.getAd().keySet());
    private static final List<String> MAP_VALUE_LIST = new ArrayList<>(AdConstants.getAd().values());

    private int currentPage = 0;
    private static final int TOTAL_PAGE = MAP_KEY_LIST.size();

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent.getAction().equals(AppWidgetManager.ACTION_APPWIDGET_ENABLED)) {
            Intent timerIntent = new Intent(ACTION_UPDATE_ALL);
            pi = PendingIntent.getBroadcast(context, 0, timerIntent, 0);
            am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), UPDATE_PERIOD, pi);
        }

        if (AppWidgetManager.ACTION_APPWIDGET_DISABLED.equals(intent.getAction()) && (am != null) && (pi != null)) {
            am.cancel(pi);
        }
/* // don't know why it fail during build
        if (ACTION_UPDATE_ALL.equals(intent.getAction())) {
            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_ad);
            AppWidgetTarget appWidgetTarget = new AppWidgetTarget(context, rv, R.id.iv, new ComponentName(context, AdWidget.class));
            Glide.with(context.getApplicationContext()) // safer!
                    .load(MAP_KEY_LIST.get(currentPage))
                    .asBitmap()
                    .into(appWidgetTarget);
            pushWidgetUpdate(context, rv, AdWidget.class);

            Intent webIntent = new Intent(context, WebViewActivity.class);
            webIntent.putExtra(WebViewActivity.KEY, MAP_VALUE_LIST.get(currentPage));
            webIntent.putExtra(WebViewActivity.IS_FROM_WIDGET, true);
            PendingIntent webPendingIntent = PendingIntent.getActivity(context, 0, webIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setOnClickPendingIntent(R.id.iv, webPendingIntent);
            pushWidgetUpdate(context, rv, AdWidget.class);
            currentPage++;
            if (currentPage >= TOTAL_PAGE) {
                currentPage = 0;
            }
        }
        */
    }
}
