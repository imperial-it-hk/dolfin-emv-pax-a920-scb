/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-2-13
 * Module Author: Kim.L
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.app;


import android.app.Activity;
import android.app.ActivityManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;

import com.pax.device.Device;
import com.pax.pay.constant.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import th.co.bkkps.linkposapi.LinkPOSApi;
import th.co.bkkps.utils.Log;

/**
 * For crashing the uncaught exception, save the exception info as files to /crash
 */
public class CrashHandler implements UncaughtExceptionHandler {

    public static final String TAG = "CrashHandler";

    private final UncaughtExceptionHandler mHandler;
    //CrashHandler Singleton
    private static final CrashHandler theInstance = new CrashHandler();

    private final DateFormat formatter = new SimpleDateFormat(Constants.TIME_PATTERN_TRANS, Locale.US);

    private CrashHandler() {
        mHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    public static CrashHandler getInstance() {
        return theInstance;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable e) {
        Device.enableStatusBar(true);
        Device.enableHomeRecentKey(true);

        try {
            saveCrashInfo2File(e);
        } catch (IOException e1) {
            Log.e(TAG, "", e1);
        }

        ActivityManager activityManager = (ActivityManager) FinancialApplication.getApp().getApplicationContext().getSystemService(Activity.ACTIVITY_SERVICE);
        activityManager.killBackgroundProcesses(LinkPOSApi.PACKAGE_NAME);

        System.exit(2);

        mHandler.uncaughtException(thread, e);
    }

    /**
     * get device info
     *
     * @return device info
     */
    private Map<String, String> getDeviceInfo() {
        Map<String, String> infos = new HashMap<>();
        try {
            PackageManager pm = FinancialApplication.getApp().getPackageManager();
            PackageInfo pi = pm.getPackageInfo(FinancialApplication.getApp().getPackageName(), PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                String versionName = pi.versionName == null ? "null" : pi.versionName;
                String versionCode = pi.versionCode + "";
                infos.put("versionName", versionName);
                infos.put("versionCode", versionCode);
            }
        } catch (NameNotFoundException e) {
            Log.w(TAG, e);
        }
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                infos.put(field.getName(), field.get(null).toString());
            } catch (Exception e) {
                Log.w(TAG, e);
            }
        }
        return infos;
    }

    /**
     * save error infos to file
     *
     * @param e the uncaught exception
     */
    private void saveCrashInfo2File(Throwable e) throws IOException {

        StringBuilder sb = new StringBuilder();
        Map<String, String> infos = getDeviceInfo();
        for (Map.Entry<String, String> entry : infos.entrySet()) {
            String value = entry.getKey() + "=" + entry.getValue() + "\n";
            sb.append(value);
        }

        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        e.printStackTrace(printWriter);
        Throwable cause = e.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String result = writer.toString();
        sb.append(result);
        String time = formatter.format(new Date());
        String fileName = "crash-" + time + ".log";

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            String path = Environment.getExternalStorageDirectory().getPath() + File.separator + "crash/";
            File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            // using a  try-with-resources for JAVA7
            try (FileOutputStream fos = new FileOutputStream(path + fileName)) {
                fos.write(sb.toString().getBytes());
            } catch (Exception e1) {
                Log.w(TAG, e1);
            }
        }
    }

}
