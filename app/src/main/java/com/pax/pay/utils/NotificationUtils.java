package com.pax.pay.utils;

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
import com.pax.edc.R;
import com.pax.pay.MainActivity;
import com.pax.pay.constant.Constants;

public class NotificationUtils {

    private static final String TAG = "NotificationUtils";

    private NotificationUtils() {
        //do nothing
    }

    public static void makeNotification(Context context, String title, String content, boolean ongoing) {
        NotificationManager notificationManager;
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

    public static void cancelNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
