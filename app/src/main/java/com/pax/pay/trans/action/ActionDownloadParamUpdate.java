/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-10-26
 * Module Author: laiyi
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.trans.action;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import th.co.bkkps.utils.Log;
import androidx.core.app.NotificationCompat;
import com.pax.abl.core.AAction;
import com.pax.edc.R;
import com.pax.pay.MainActivity;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.settings.SysParam;

public class ActionDownloadParamUpdate extends AAction {
    private Context context;
    private boolean disNoParam;
    public NotificationManager notificationManager;
    /**
     * derived classes must call super(listener) to set
     *
     * @param listener {@link ActionStartListener}
     */
    public ActionDownloadParamUpdate(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, boolean disNoParam) {
        this.context = context;
        this.disNoParam = disNoParam;
    }

    @Override
    protected void process() {
        if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.NEED_UPDATE_PARAM)) {
            boolean paramResult = FinancialApplication.getDownloadManager().handleSuccess(context);
            if (paramResult) {
                cancelNotification();
                FinancialApplication.getSysParam().set(SysParam.BooleanParam.NEED_UPDATE_PARAM, false);
                makeNotification(context.getString(R.string.notif_param_complete), context.getString(R.string.notif_param_success), false);
            } else {
                cancelNotification();
                FinancialApplication.getSysParam().set(SysParam.BooleanParam.NEED_UPDATE_PARAM, false);
                makeNotification(context.getString(R.string.notif_param_fail), context.getString(R.string.notif_param_call_bank), false);
            }
        }

    }

    private void makeNotification(String title, String content, boolean ongoing) {
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_bps_gray)
                        .setColor(context.getResources().getColor(R.color.primary))
                        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                                R.drawable.kaset_logo))
                        .setContentTitle(title)
                        .setContentText(content)
                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setDefaults(Notification.DEFAULT_VIBRATE)
                        .setOngoing(ongoing)
                        .setOnlyAlertOnce(false)
                        .setFullScreenIntent(pendingIntent, true)
                        .setContentIntent(pendingIntent);
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(Constants.NOTIFICATION_ID_PARAM, mBuilder.build());
    }

    private void cancelNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
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
}
