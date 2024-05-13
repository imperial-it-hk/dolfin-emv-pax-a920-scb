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


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.JsonReader;
import android.util.JsonToken;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.alibaba.fastjson.JSONReader;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.pax.dal.IChannel;
import com.pax.dal.IDAL;
import com.pax.dal.IDalCommManager;
import com.pax.dal.ISys;
import com.pax.dal.entity.EChannelType;
import com.pax.dal.entity.LanParam;
import com.pax.dal.exceptions.ChannelException;
import com.pax.device.UserParam;
import com.pax.edc.BuildConfig;
import com.pax.edc.R;
import com.pax.eemv.utils.Tools;
import com.pax.glwrapper.convert.IConvert;
import com.pax.jemv.clcommon.ByteArray;
import com.pax.pay.MainActivity;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.app.MultiPathProgressiveListener;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.CardRange;
import com.pax.pay.base.Issuer;
import com.pax.pay.base.MerchantAcqProfile;
import com.pax.pay.base.MerchantProfile;
import com.pax.pay.constant.Constants;
import com.pax.pay.db.MerchantAcqProfileDb;
import com.pax.pay.emv.EmvAid;
import com.pax.pay.trans.model.Controller;
import com.pax.pay.trans.model.MerchantProfileManager;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.models.SP200FirmwareInfos;
import com.pax.settings.SysParam;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import th.co.bkkps.utils.Log;

import static android.content.Context.TELEPHONY_SERVICE;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class Utils {

    private static final String TAG = "Utils";

    private Utils() {
        //do nothing
    }

    /**
     * 获取主秘钥索引
     *
     * @param index 0~99的主秘钥索引值
     * @return 1~100的主秘钥索引值
     */
    public static int getMainKeyIndex(int index) {
        return index + 1;
    }

    public static boolean checkIp(String ip) {
        return ip.matches("((2[0-4]\\d|25[0-5]|[01]?\\d\\d?)\\.){3}(2[0-4]\\d|25[0-5]|[01]?\\d\\d?)");
    }

    public static void changeAppLanguage(Context context, Locale locale) {
        if (context == null) {
            return;
        }
        Locale.setDefault(locale);
        Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = locale;
        res.updateConfiguration(conf, dm);
    }

    @NonNull
    public static String getString(@StringRes int resId) {
        return FinancialApplication.getApp().getString(resId);
    }

    @NonNull
    public static String getString(@StringRes int resId, Object... formatArgs) {
        return FinancialApplication.getApp().getResources().getString(resId, formatArgs);
    }

    public static int getResId(String name, String defType) {
        return FinancialApplication.getApp().getResources().getIdentifier(name, defType, FinancialApplication.getApp().getPackageName());
    }

    public static byte[] str2Bcd(String str) {
        return FinancialApplication.getConvert().strToBcd(str, IConvert.EPaddingPosition.PADDING_LEFT);
    }

    public static String bcd2Str(byte[] bcd) {
        return FinancialApplication.getConvert().bcdToStr(bcd);
    }

    // easy way to get permission, for getting more interactions, should show request permission rationale
    public static void callPermission(@NonNull Activity activity, String permission, @NonNull Action action, final String failedMsg) {
        RxPermissions rxPermissions = new RxPermissions(activity); // where this is an Activity instance
        rxPermissions
                .request(permission)
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean granted) throws Exception {
                        Log.e(TAG, "{accept Boolean}");
                        if (!granted) {
                            // 未获取权限
                            ToastUtils.showMessage(failedMsg);
                        }
                        // 在android 6.0之前会默认返回true
                        // 已经获取权限 do nothing
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.e(TAG, "{accept Throwable}");//可能是授权异常的情况下的处理
                    }
                }, action);
    }

    public static boolean isDebugBuild() {
        return "debug".equals(BuildConfig.BUILD_TYPE);
    }

    public static boolean isSandboxBuild() {
        return "sandBox".equals(BuildConfig.FLAVOR);
    }


    public static boolean isSMSAvailable(Context context) {
        TelephonyManager manager = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);
        return (manager.getSimState() == TelephonyManager.SIM_STATE_READY) && (
                ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED);
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            if (info != null && info.isConnected() && info.getState() == NetworkInfo.State.CONNECTED) {
                return true;
            }
        }
        return false;
    }

    public static void wakeupScreen(int timeout) {
        PowerManager pm = (PowerManager) FinancialApplication.getApp().getSystemService(Context.POWER_SERVICE);
        final PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright");
        wl.acquire();
        FinancialApplication.getApp().runOnUiThreadDelay(new Runnable() {
            @Override
            public void run() {
                wl.release();
            }
        }, 1000L * (timeout + 1));
    }

    public static void restart() {
        ActivityStack.getInstance().popAll();
        Intent intent = new Intent();
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        intent.setClass(FinancialApplication.getApp(), MainActivity.class);
        FinancialApplication.getApp().startActivity(intent);
    }

    public static void callSystemSettings(Context context, String action) {
        context.startActivity(new Intent(action));
    }

    public static <T> List<T> readObjFromJSON(String fileName, Class<T> clz) {
        List<T> list = new ArrayList<>();
        try (InputStreamReader reader = new InputStreamReader(FinancialApplication.getApp().getAssets().open(fileName))) {
            JSONReader jsonReader = new JSONReader(reader);
            jsonReader.startArray();
            while (jsonReader.hasNext()) {
                T obj = jsonReader.readObject(clz);
                list.add(obj);
            }
            jsonReader.endArray();
            jsonReader.close();
        } catch (IOException e) {
            Log.e(TAG, "", e);
        }
        return list;
    }

    public static <T> List<T> readObjFromJSON(String saveFilePath, String filename, Class<T> clazz) {
        if (filename != null && !filename.isEmpty()) {
            File f = new File(saveFilePath, filename);
            if (f.exists()) {
                int result = 0;
                try {
                    FileInputStream in = new FileInputStream(f);
                    if (!isFieldExists(new FileInputStream(f), clazz)) {
                        throw new Exception("Field is not exist.");
                    }
                    return readObjFromJSON(in, clazz);
                } catch (Exception e) {
                    result = 1;
                    Log.e(TAG, "", e);
                } finally {
                    if (clazz == EmvAid.class)
                        FinancialApplication.getSysParam().set(SysParam.NumberParam.EDC_AID_FILE_UPLOAD_STATUS, result);
                    else if (clazz == CardRange.class)
                        FinancialApplication.getSysParam().set(SysParam.NumberParam.EDC_CARD_RANGE_FILE_UPLOAD_STATUS, result);
                    else if (clazz == Issuer.class)
                        FinancialApplication.getSysParam().set(SysParam.NumberParam.EDC_ISSUER_FILE_UPLOAD_STATUS, result);
                }
            }
        }
        return new ArrayList<>(0);
    }

    private static <T> List<T> readObjFromJSON(FileInputStream in, Class<T> clz) throws Exception {
        List<T> list = new ArrayList<>();
        InputStreamReader reader = new InputStreamReader(in);
        JSONReader jsonReader = new JSONReader(reader);
        jsonReader.startArray();
        while (jsonReader.hasNext()) {
            T obj = jsonReader.readObject(clz);
            list.add(obj);
        }
        jsonReader.endArray();
        jsonReader.close();
        return list;
    }

    private static <T> boolean isFieldExists(FileInputStream in, Class<T> clz) throws Exception {
        boolean isExist = false;
        JsonReader jReader = new JsonReader(new InputStreamReader(in));
        jReader.setLenient(true);
        jReader.beginArray();
        while (jReader.hasNext()) {
            if (isExist) break;

            jReader.beginObject();
            while (jReader.peek() != JsonToken.END_OBJECT) {
                String key = jReader.nextName();
                if (clz == EmvAid.class && ("appName".equals(key) || "aid".equals(key))) {
                    isExist = true;
                    break;
                } else if (clz == CardRange.class && ("panRangeLow".equals(key) || "panRangeHigh".equals(key))) {
                    isExist = true;
                    break;
                } else if (clz == Issuer.class && ("issuerID".equals(key) || "panMaskPattern".equals(key))) {
                    isExist = true;
                    break;
                } else {
                    jReader.skipValue();
                }
            }
            if (!isExist) jReader.endObject();
        }
        if (!isExist) jReader.endArray();
        jReader.close();
        return isExist;
    }

    public static long parseLongSafe(String longStr, long safeValue) {
        if (longStr == null)
            return safeValue;
        try {
            return Long.parseLong(longStr);
        } catch (NumberFormatException e) {
            return safeValue;
        }
    }

    public static int parseIntSafe(String intStr, int safeValue) {
        if (intStr == null)
            return safeValue;
        try {
            return Integer.parseInt(intStr);
        } catch (NumberFormatException e) {
            return safeValue;
        }
    }

    public static Double parseDoubleSafe(String decimalStr, Double safeValue) {
        if (decimalStr == null)
            return safeValue;
        try {
            return Double.parseDouble(decimalStr);
        } catch (NumberFormatException e) {
            return safeValue;
        }
    }

    public static byte[] concat(byte[]... arrays) {
        // Determine the length of the result array
        int totalLength = 0;
        for (int i = 0; i < arrays.length; i++) {
            if (arrays[i] != null)
                totalLength += arrays[i].length;
        }

        // create the result array
        byte[] result = new byte[totalLength];

        // copy the source arrays into the result array
        int currentIndex = 0;
        for (int i = 0; i < arrays.length; i++) {
            if (arrays[i] != null) {
                System.arraycopy(arrays[i], 0, result, currentIndex, arrays[i].length);
                currentIndex += arrays[i].length;
            }
        }

        return result;
    }

    public static String splitHolderName(String name) {
        //KBank requirement: Comment to print actual value from card
        /*String[] array = name.split("/+");
        String temp = null;
        for (int counter = array.length - 1; counter >= 0; counter--) {
            if (temp == null) {
                temp = array[counter] + " ";
            } else {
                temp += array[counter] + " ";
            }

        }
        return temp;*/
        return name != null ? name.trim() : "";
    }

    public static String[] buildRespMsg(TransData transData, Map<String, String> respMsg) {
        List<String> list = new ArrayList<String>();


        list.add("RECEIVED MSG");
        for (Map.Entry<String, String> entry : respMsg.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            switch (key) {
                case "tpdu":
                    list.add("tpdu=" + value.replaceAll("..(?!$)", "$0 "));
                    break;
                case "Msg Type":
                    list.add("Msg Type=" + value);
                    break;
                case "55":
                case "57":
                case "60":
                case "61":
                case "62":
                case "63":
                    list.add("[" + key + "]=" + "(" + value.length() / 2 + ")" + value.replaceAll("..(?!$)", "$0 "));
                    break;
                case "64":
                    list.add("[" + key + "]=" + value.replaceAll("..(?!$)", "$0 "));
                    break;
                default:
                    list.add("[" + key + "]=" + value);
                    break;
            }
        }
        list.add("Pack OK!");


        List<String> tempList = new ArrayList<String>();
        for (String l : list) {
            if (!l.contains("=null")) {
                tempList.add(l);
            }
        }
        String[] stringArray = tempList.toArray(new String[0]);
        return stringArray;
    }

    public static String[] buildReqMsg(TransData transData, Map<String, String> reqMsg) {
        List<String> list = new ArrayList<String>();
        List<String> keysStr = new ArrayList<>(reqMsg.keySet());
        List<Integer> keysInt = new ArrayList<Integer>();
        for (String s : keysStr) keysInt.add(Integer.valueOf(s));
        Collections.sort(keysInt);

        list.add("REQUEST MSG");
        list.add("tpdu=" + transData.getTpdu().replaceAll("..(?!$)", "$0 "));

        if (transData.getReversalStatus() == TransData.ReversalStatus.REVERSAL) {
            list.add("Msg Type=" + transData.getTransType().getDupMsgType());
        } else if (transData.getAdviceStatus() == TransData.AdviceStatus.ADVICE && Constants.ACQ_WALLET.equals(transData.getAcquirer().getName())) {
            list.add("Msg Type=" + transData.getTransType().getRetryChkMsgType());
        } else {
            list.add("Msg Type=" + transData.getTransType().getMsgType());
        }

        for (Integer s : keysInt) {
            if (!FinancialApplication.getAcqManager().getCurAcq().isEnableTle() || (FinancialApplication.getAcqManager().getCurAcq().isEnableTle() && !UserParam.encField.contains(s))) {
                if (s == 55 || s == 57 || s == 61) {
                    list.add("[" + s + "]=" + "(" + reqMsg.get(s.toString()).length() / 2 + ")" + reqMsg.get(s.toString()).replaceAll("..(?!$)", "$0 "));
                } else if (s == 35 || s == 60 || s == 62) {
                    list.add("[" + s + "]=" + "(" + reqMsg.get(s.toString()).length() + ")" + reqMsg.get(s.toString()).replaceFirst("D", "="));
                } else if (s == 63) {
                    if (Constants.ACQ_REDEEM.equals(transData.getAcquirer().getName()) ||
                            Constants.ACQ_REDEEM_BDMS.equals(transData.getAcquirer().getName()) ||
                            Constants.ACQ_SMRTPAY.equals(transData.getAcquirer().getName()) ||
                            Constants.ACQ_SMRTPAY_BDMS.equals(transData.getAcquirer().getName()) ||
                            Constants.ACQ_DOLFIN_INSTALMENT.equals(transData.getAcquirer().getName())) {
                        list.add("[" + s + "]=" + "(" + reqMsg.get(s.toString()).length() / 2 + ")" + reqMsg.get(s.toString()).replaceAll("..(?!$)", "$0 "));
                    } else {
                        byte[] value = Tools.string2Bytes(reqMsg.get(s.toString()));
                        list.add("[" + s + "]=" + "(" + reqMsg.get(s.toString()).length() + ")" + bcd2Str(value).replaceAll("..(?!$)", "$0 "));
                    }
                } else if (s == 64) {
                    list.add("[" + s + "]=" + reqMsg.get(s.toString()).replaceAll("..(?!$)", "$0 "));
                } else {
                    list.add("[" + s + "]=" + reqMsg.get(s.toString()));
                }
            }
        }
        list.add("Pack OK!");
        String[] stringArray = list.toArray(new String[0]);
        return stringArray;
    }


    public static void Delay100Ms(int msX100) {
        DelayThread delayThread = new DelayThread();
        delayThread.start();

        try {
            delayThread.join(msX100 * 100);
            delayThread.interrupt();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void SaveArrayCopy(byte[] srt, int srtOffset, byte[] det, int detOffset, int len) {
        System.arraycopy(srt, srtOffset, det, detOffset, (len <= srt.length && len <= det.length) ? len : ((srt.length > det.length) ? det.length : srt.length));
    }

    public static String getCurentDateTime() {
        // format "yyyy/MM/dd HH:mm:ss"
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DATE);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int min = calendar.get(Calendar.MINUTE);
        int sec = calendar.get(Calendar.SECOND);
        String dateTime = year + "/" + month + "/" + day + " " + hour + ":" + min + ":" + sec;

        return dateTime;
    }

    public static String getCurentDateTime2() {
        // format "yyyyMMddHHmmss"
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DATE);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int min = calendar.get(Calendar.MINUTE);
        int sec = calendar.get(Calendar.SECOND);


        String dateTime = String.format("%04d%02d%02d%02d%02d%02d", year, month, day, hour, min, sec);

        return dateTime;
    }

    public static void LoadLanguage(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(FinancialApplication.getApp());
        String language = prefs.getString(Constants.USER_LANG, null);
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
    }

    public static String getAppKey(Context context) {
        String appKeyName = "app_key_" + BuildConfig.FLAVOR + "_" + BuildConfig.BUILD_TYPE;
        int key = context.getResources().getIdentifier(appKeyName, "string", context.getPackageName());
        return getString(key);
    }

    public static String getAppSecret(Context context) {
        String appSecretName = "app_secret_" + BuildConfig.FLAVOR + "_" + BuildConfig.BUILD_TYPE;
        int key = context.getResources().getIdentifier(appSecretName, "string", context.getPackageName());
        return getString(key);
    }

    public static void SetEDCTime(String dataTime) {
        String temp = dataTime.substring(2);
        IDAL idal = FinancialApplication.getDal();
        ISys iSys = idal.getSys();
        iSys.setDate(temp);
    }

    public static int switchAPN(final Context context, String apn, long succTimeout, long failTimeout, MultiPathProgressiveListener listener) {
        int ret = -1;
        String message;
        String sysAPN = apn != null ? apn : FinancialApplication.getSysParam().get(SysParam.StringParam.MOBILE_APN_SYSTEM);
        Log.d(TAG, "switchAPN apnName = " + sysAPN);
        try {
            long timeout;
            boolean isNone = Utils.getString(R.string.apn_none).equals(sysAPN);
            ret = FinancialApplication.getDal().getCommManager().switchAPN(sysAPN, sysAPN, null, null, 0);
            if (ret == 1) {//success
                timeout = succTimeout;
                message = Utils.getString(R.string.dialog_apn_config_succ);
            } else {//fail
                timeout = failTimeout;
                message = Utils.getString(R.string.dialog_apn_config_fail);
            }
            //if (!isNone) ToastUtils.makeText(context, message, timeout);
            Log.d(TAG, "switchAPN result = " + ret + ((ret == 1) ? " (successful)" : " (failed)"));
        } catch (Exception e) {
            Log.e(TAG, "switchAPN: ", e);
        } finally {
            //FinancialApplication.setMultiPath(listener, FinancialApplication.getSysParam().get(SysParam.StringParam.LINKPOS_COMM_TYPE));
        }
        return ret;
    }

    public static void initAPN(final Context context, boolean isConfigWithParam, MultiPathProgressiveListener listener) {
        int ret = -1;
        if (isConfigWithParam) {
            Log.d(TAG, "Init APN, init switchAPN.");
            ret = switchAPN(context, null, 7000, 7000, listener);
        } else {
            if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.NEED_SWITCH_APN)) {
                Log.d(TAG, "Need call switchAPN.");
                ret = switchAPN(context, null, 7000, 7000, listener);
            }
        }
        if (ret == 1) {//success
            FinancialApplication.getSysParam().set(SysParam.BooleanParam.NEED_SWITCH_APN, false);
        } else {//fail
            FinancialApplication.getSysParam().set(SysParam.BooleanParam.NEED_SWITCH_APN, true);
        }
    }

    public static Bitmap generateQrCode(String content, int width, int height) {
        Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);

        try {
            BitMatrix bitMatrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints);

            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (bitMatrix.get(x, y)) {
                        pixels[y * width + x] = 0xff000000;
                    } else {
                        pixels[y * width + x] = 0xffffffff;
                    }
                }
            }

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        } catch (WriterException e) {
            Log.e(TAG, "", e);
        }
        return null;
    }

    public static Bitmap generateQrCodeLevelM(String content, int width, int height) {
        Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);

        try {
            BitMatrix bitMatrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints);

            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (bitMatrix.get(x, y)) {
                        pixels[y * width + x] = 0xff000000;
                    } else {
                        pixels[y * width + x] = 0xffffffff;
                    }
                }
            }

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        } catch (WriterException e) {
            Log.e(TAG, "", e);
        }
        return null;
    }

    public static Bitmap generateBarCode(String content, int width, int height) {
        Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        try {
            BitMatrix bitMatrix = new Code128Writer().encode(content, BarcodeFormat.CODE_128, width, height, hints);

            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (bitMatrix.get(x, y)) {
                        pixels[y * width + x] = 0xff000000;
                    } else {
                        pixels[y * width + x] = 0xffffffff;
                    }
                }
            }

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        } catch (WriterException e) {
            Log.e(TAG, "", e);
        }
        return null;
    }

    public static boolean isUpgrade(Context context) {
        boolean result = false;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int versionCode = sharedPreferences.getInt("VERSION_CODE", 0);

        boolean isFirstrun = Controller.isFirstRun();


        if (versionCode != BuildConfig.VERSION_CODE && !isFirstrun) {
            result = true;
        }
        sharedPreferences.edit().putInt("VERSION_CODE", BuildConfig.VERSION_CODE).apply();
        return result;
    }

    public static String randomString(int len) {
        final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++)
            sb.append(AB.charAt(rnd.nextInt(AB.length())));
        return sb.toString();
    }

    public static String getTerminalAndAppVersion() {
        return FinancialApplication.getApplicationName() + " " + FinancialApplication.getVersion();
    }

    public static String getStringPadding(long targStr, int maxLen, String PaddStr, Convert.EPaddingPosition paddingPosition) {
        return getStringPadding(String.valueOf(targStr), maxLen, PaddStr, paddingPosition);
    }

    public static String getStringPadding(int targStr, int maxLen, String PaddStr, Convert.EPaddingPosition paddingPosition) {
        return getStringPadding(String.valueOf(targStr), maxLen, PaddStr, paddingPosition);
    }

    public static String getStringPadding(String targStr, int maxLen, String PaddStr, Convert.EPaddingPosition paddingPosition) {
        String retuenStr = "";
        String tempReturnStr = "";
        if (targStr.length() < maxLen) {
            for (int idx = 0; idx <= (maxLen - targStr.length() - 1); idx++) {
                retuenStr += PaddStr;
            }

            if (paddingPosition == Convert.EPaddingPosition.PADDING_LEFT) {
                tempReturnStr = retuenStr + targStr;
            } else {
                tempReturnStr = targStr + retuenStr;
            }
        } else {
            tempReturnStr = targStr;
        }

        return tempReturnStr;
    }

    static class DelayThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (!Thread.interrupted()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static boolean isStringNotNullOrEmpty(String srcStr) {
        if (srcStr != null) {
            if (srcStr.length() > 0) {
                if (!srcStr.trim().equals("")) {
                    return true;
                }
            }
        }
        return false;
    }

    public enum LengthValidatorMode {NOT_REQUIRED, GRATER_THAN, LESS_THAN, EQUALS, EQUALS_RETURN}

    public static String checkStringContainNullOrEmpty(String srcStr) {
        return checkStringContainNullOrEmpty(srcStr, LengthValidatorMode.NOT_REQUIRED, -1);
    }

    public static String checkStringContainNullOrEmpty(String srcStr, LengthValidatorMode validateMode) {
        return checkStringContainNullOrEmpty(srcStr, validateMode, -1, false, null);
    }

    public static String checkStringContainNullOrEmpty(String srcStr, LengthValidatorMode validateMode, int preferLength) {
        return checkStringContainNullOrEmpty(srcStr, validateMode, preferLength, false, null);
    }

    public static String checkStringContainNullOrEmpty(String srcStr, LengthValidatorMode validateMode, int preferLength, boolean ReturnEmptyStrEqualLen, String defaultStrReturnOnEmpty) {
        String tmpReturnStr = "";
        if (srcStr != null) {
            if (srcStr.length() > 0) {
                if (!srcStr.trim().equals("")) {
                    if (validateMode == LengthValidatorMode.NOT_REQUIRED) {
                        tmpReturnStr = srcStr;
                    } else if (preferLength > -1) {
                        if (validateMode == LengthValidatorMode.GRATER_THAN) {
                            if (srcStr.length() > preferLength) {
                                tmpReturnStr = srcStr;
                            }
                        } else if (validateMode == LengthValidatorMode.LESS_THAN) {
                            if (srcStr.length() < preferLength) {
                                tmpReturnStr = srcStr;
                            }
                        } else if (validateMode == LengthValidatorMode.EQUALS) {
                            if (srcStr.length() == preferLength) {
                                tmpReturnStr = srcStr;
                            } else {
                                if (ReturnEmptyStrEqualLen) {
                                    tmpReturnStr = Utils.getStringPadding("", preferLength, defaultStrReturnOnEmpty, Convert.EPaddingPosition.PADDING_LEFT);
                                }
                            }
                        }
                    }
                }
            }
        }
        return tmpReturnStr;
    }

    private static boolean enableKBank = false;
    private static boolean enableTPN = false;
    private static boolean enableDcc = false;
    private static boolean enableAmex = false;
    private static boolean enablePromtpay = false;
    private static boolean enableWallet = false;
    private static boolean enableQRC = false;
    private static boolean enableSaleNormal = false;
    private static boolean enableVoid = false;
    private static boolean enableRedeemKbank = false;
    private static boolean enableKCheckId = false;
    private static boolean enableInstalmentKbank = false;
    private static boolean enableInstalmentKbankBdms = false;
    private static boolean enableKplus = false;
    private static boolean enableAlipay = false;
    private static boolean enableWechat = false;
    private static boolean enableBay = false;
    private static boolean enableAmexInstalment = false;
    private static boolean enableQRCredit = false;
    private static boolean enableDolfin = false;
    private static boolean enableScbIpp = false;
    private static boolean enableScbRedeem = false;
    private static boolean enableDolfinInstalment = false;
    private static boolean enableMyPromt = false;
    private static boolean enableAlipayBscanC = false;
    private static boolean enableWechatBscanC = false;


    public static void setEnableAcquirer() {
        resetMenu();
        
        List<Acquirer> acqs = FinancialApplication.getAcqManager().findEnableAcquirers();
        if (acqs != null) {
            for (Acquirer acquirer : acqs) {
                if (acquirer.getName().equalsIgnoreCase(Constants.ACQ_QR_PROMPT) && acquirer.isEnable()) {
                    enablePromtpay = true;
                } else if (acquirer.getName().equalsIgnoreCase(Constants.ACQ_WALLET) && acquirer.isEnable()) {
                    enableWallet = true;
                } else if (acquirer.getName().equalsIgnoreCase(Constants.ACQ_QRC) && acquirer.isEnable()) {
                    enableQRC = true;
                } else if ((acquirer.getName().equalsIgnoreCase(Constants.ACQ_REDEEM) && acquirer.isEnable()) ||
                        (acquirer.getName().equalsIgnoreCase(Constants.ACQ_REDEEM_BDMS) && acquirer.isEnable())) {
                    enableRedeemKbank = true;
                } else if ((acquirer.getName().equalsIgnoreCase(Constants.ACQ_SMRTPAY) && acquirer.isEnable()) ||
                        (acquirer.getName().equalsIgnoreCase(Constants.ACQ_SMRTPAY_BDMS) && acquirer.isEnable())) {
                    enableInstalmentKbank = true;
                    enableInstalmentKbankBdms = true;
                } else if (acquirer.getName().equalsIgnoreCase(Constants.ACQ_KPLUS) && acquirer.isEnable()) {
                    enableKplus = true;
                } else if (acquirer.getName().equalsIgnoreCase(Constants.ACQ_ALIPAY) && acquirer.isEnable()) {
                    enableAlipay = true;
                } else if (acquirer.getName().equalsIgnoreCase(Constants.ACQ_WECHAT) && acquirer.isEnable()) {
                    enableWechat = true;
                } else if (acquirer.getName().equalsIgnoreCase(Constants.ACQ_BAY_INSTALLMENT) && acquirer.isEnable()) {
                    enableBay = true;
                } else if (acquirer.getName().equalsIgnoreCase(Constants.ACQ_AMEX_EPP) && acquirer.isEnable()) {
                    enableAmexInstalment = true;
                } else if (acquirer.getName().equalsIgnoreCase(Constants.ACQ_QR_CREDIT) && acquirer.isEnable()) {
                    enableQRCredit = true;
                } else if (acquirer.getName().equalsIgnoreCase(Constants.ACQ_DOLFIN) && acquirer.isEnable()) {
                    enableDolfin = true;
                } else if (acquirer.getName().equalsIgnoreCase(Constants.ACQ_SCB_IPP) && acquirer.isEnable()) {
                    enableScbIpp = true;
                } else if (acquirer.getName().equalsIgnoreCase(Constants.ACQ_SCB_REDEEM) && acquirer.isEnable()) {
                    enableScbRedeem = true;
                } else if ((acquirer.getName().equalsIgnoreCase(Constants.ACQ_KBANK) && acquirer.isEnable()) ||
                        (acquirer.getName().equalsIgnoreCase(Constants.ACQ_KBANK_BDMS) && acquirer.isEnable())) {
                    enableKBank = true;
                } else if (acquirer.getName().equalsIgnoreCase(Constants.ACQ_UP) && acquirer.isEnable()) {
                    enableTPN = true;
                } else if (acquirer.getName().equalsIgnoreCase(Constants.ACQ_DCC) && acquirer.isEnable()) {
                    enableDcc = true;
                } else if (acquirer.getName().equalsIgnoreCase(Constants.ACQ_AMEX) && acquirer.isEnable()) {
                    enableAmex = true;
                } else if (acquirer.getName().equalsIgnoreCase(Constants.ACQ_KCHECKID) && acquirer.isEnable()) {
                    enableKCheckId = true;
                } else if (acquirer.getName().equalsIgnoreCase(Constants.ACQ_DOLFIN_INSTALMENT) && acquirer.isEnable()) {
                    enableDolfinInstalment = true;
                } else if (acquirer.getName().equalsIgnoreCase(Constants.ACQ_MY_PROMPT) && acquirer.isEnable()) {
                    enableMyPromt = true;
                }else if (acquirer.getName().equalsIgnoreCase(Constants.ACQ_ALIPAY_B_SCAN_C) && acquirer.isEnable()) {
                    enableAlipayBscanC = true;
                }else if (acquirer.getName().equalsIgnoreCase(Constants.ACQ_WECHAT_B_SCAN_C) && acquirer.isEnable()) {
                    enableWechatBscanC = true;
                }

            }
        }

        setEnableMenuByMerchant();
        setEnableMenu();
    }

    private static void setEnableMenu() {
        SysParam sParam = FinancialApplication.getSysParam();

        enableSaleNormal = (enableKBank || enableTPN || enableDcc || enableAmex) && sParam.get(SysParam.BooleanParam.EDC_ENABLE_SALE_CREDIT_MENU, true);
        enableVoid = sParam.get(SysParam.BooleanParam.EDC_ENABLE_VOID_MENU, true);
        enableKplus = enableKplus && sParam.get(SysParam.BooleanParam.EDC_ENABLE_KPLUS_MENU, true);
        enableAlipay = enableAlipay && sParam.get(SysParam.BooleanParam.EDC_ENABLE_ALIPAY_MENU, true);
        enableWechat = enableWechat && sParam.get(SysParam.BooleanParam.EDC_ENABLE_WECHAT_MENU, true);
        enableQRCredit = enableQRCredit && sParam.get(SysParam.BooleanParam.EDC_ENABLE_QR_CREDIT_MENU, true);
        enableInstalmentKbank = enableInstalmentKbank && sParam.get(SysParam.BooleanParam.EDC_ENABLE_SMART_PAY_MENU, true);
        enableInstalmentKbankBdms = enableInstalmentKbankBdms && sParam.get(SysParam.BooleanParam.EDC_ENABLE_SMART_PAY_MENU, true);
        enableRedeemKbank = enableRedeemKbank && sParam.get(SysParam.BooleanParam.EDC_ENABLE_REDEEM_MENU, true);
        enableBay = enableBay && sParam.get(SysParam.BooleanParam.EDC_ENABLE_CT1_EPP_MENU, true);
        enableAmexInstalment = enableAmexInstalment && sParam.get(SysParam.BooleanParam.EDC_ENABLE_AMEX_EPP_MENU, true);
        enableScbIpp = enableScbIpp && sParam.get(SysParam.BooleanParam.EDC_ENABLE_SCB_IPP_MENU, true);
        enableScbRedeem = enableScbRedeem && sParam.get(SysParam.BooleanParam.EDC_ENABLE_SCB_REDEEM_MENU, true);
        enableDolfin = enableDolfin && sParam.get(SysParam.BooleanParam.EDC_ENABLE_DOLFIN_MENU, true);
        enableKCheckId =  enableKCheckId && sParam.get(SysParam.BooleanParam.EDC_ENABLE_KCHECKID_MAIN_MENU, true);
        enableDolfinInstalment = true;//enableDolfinInstalment && sParam.get(SysParam.BooleanParam.EDC_ENABLE_DOLFIN_IPP_MENU, true);
        enableMyPromt =  enableMyPromt && sParam.get(SysParam.BooleanParam.EDC_ENABLE_MYPROMPT_MENU, true);
        enableAlipayBscanC = enableAlipayBscanC && sParam.get(SysParam.BooleanParam.EDC_ENABLE_ALIPAY_BSCANC_MENU, true);
        enableWechatBscanC = enableWechatBscanC && sParam.get(SysParam.BooleanParam.EDC_ENABLE_WECHAT_BSCANC_MENU, true);
    }

    private static void setEnableMenuByMerchant() {
        String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
        //Disable cross-bank menu if it's not master merchant
        if (merchantName != null && !merchantName.equals(MultiMerchantUtils.Companion.getMasterProfileName())) {
            enableScbIpp = false;
            enableScbRedeem = false;
            enableDolfin = false;
            enableDolfinInstalment = false;
            enableKCheckId = false;
        }
    }

    private static void resetMenu() {
        enableKBank = false;
        enableTPN = false;
        enableDcc = false;
        enableAmex = false;
        enablePromtpay = false;
        enableWallet = false;
        enableQRC = false;
        enableSaleNormal = false;
        enableVoid = false;
        enableRedeemKbank = false;
        enableInstalmentKbank = false;
        enableInstalmentKbankBdms = false;
        enableKplus = false;
        enableAlipay = false;
        enableWechat = false;
        enableBay = false;
        enableAmexInstalment = false;
        enableQRCredit = false;
        enableDolfin = false;
        enableScbIpp = false;
        enableScbRedeem = false;
        enableKCheckId = false;
        enableDolfinInstalment = false;
        enableMyPromt = false;
        enableAlipayBscanC = false;
        enableWechatBscanC = false;
    }

    public static boolean isEnablePromtpay() {
        return enablePromtpay;
    }

    public static boolean isEnableWallet() {
        return enableWallet;
    }

    public static boolean isEnableQRC() {
        return enableQRC;
    }

    public static boolean isEnableSaleNormal() {
        return enableSaleNormal;
    }

    public static boolean isEnableVoid() {
        return enableVoid;
    }

    public static boolean isEnableRedeemKbank() {
        return enableRedeemKbank;
    }

    public static boolean isEnableInstalmentKbank() {
        return enableInstalmentKbank;
    }

    public static boolean isEnableInstalmentKbankBdms() {
        return enableInstalmentKbankBdms;
    }

    public static boolean isEnableKplus() {
        return enableKplus;
    }

    public static boolean isEnableAlipay() {
        return enableAlipay;
    }

    public static boolean isEnableWechat() {
        return enableWechat;
    }

    public static boolean isEnableBay() {
        return enableBay;
    }

    public static boolean isEnableAmexInstalment() {
        return enableAmexInstalment;
    }

    public static boolean isEnableQRCredit() {
        return enableQRCredit;
    }

    public static boolean isEnableDolfin() {
        return enableDolfin;
    }

    public static boolean isEnableDolfinInstalment() { return enableDolfinInstalment; }

    public static boolean isEnableScbIpp() {
        return enableScbIpp;
    }

    public static boolean isEnableScbRedeem() {
        return enableScbRedeem;
    }

    public static boolean isEnableKCheckId() {
        return enableKCheckId;
    }

    public static boolean isEnableInstalmentRootMenu() {return enableInstalmentKbank || enableAmexInstalment || enableBay || enableScbIpp || enableInstalmentKbankBdms || enableDolfinInstalment; }

    public static boolean isEnableRedeemRootMenu() {return enableRedeemKbank || enableScbRedeem; }

    public static boolean isEnableMyPromt() {
        return enableMyPromt;
    }

    public static boolean isEnableAlipayBscanC() {
        return enableAlipayBscanC;
    }

    public static boolean isEnableWechatBscanC() {
        return enableWechatBscanC;
    }

    public static void showAlert(Context context,
                                 @NonNull String title,
                                 @NonNull String caption,
                                 @NonNull boolean cancelable,
                                 @NonNull DialogInterface.OnDismissListener dismissListener) {
        if (context != null) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    try {
                        builder.setTitle(title);
                        builder.setMessage(caption);
                        builder.setCancelable(cancelable);
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        builder.setOnDismissListener(dismissListener);
                        builder.show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public static List<String> getFilesInAssetDirByExtension(String fileExtension) {
        BufferedReader reader = null;
        String[] filesArr = new String[]{};
        List<String> assetFiles = new ArrayList<>();
        try {
            AssetManager assetMgr = FinancialApplication.getApp().getAssets();
            if (assetMgr != null) {
                filesArr = assetMgr.list("");
                if (filesArr != null && filesArr.length > 0) {
                    for (String fileName : filesArr) {
                        if (fileName.endsWith(fileExtension)){
                            assetFiles.add(fileName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return assetFiles;
    }

    public static String getSingleFileAssetDirByExtension(String fileExtension) {
       List<String> asstFileName = getFilesInAssetDirByExtension(fileExtension);
       AssetManager assetMgr = FinancialApplication.getApp().getAssets();
       String returnStr = null;
       if (asstFileName == null || asstFileName.size() == 0 || assetMgr == null) {
           returnStr = null;
       } else if (asstFileName.size() == 1) {
           returnStr = asstFileName.get(0);
           Log.d("AssetFile"," use '" + returnStr + "'");
       }

       return returnStr;
    }

    public static SP200FirmwareInfos getSP200FirmwareInfos(String fileExtension){
        List<String> asstFileName = getFilesInAssetDirByExtension(fileExtension);
        AssetManager assetMgr = FinancialApplication.getApp().getAssets();
        SP200FirmwareInfos FwInfos = null;
        if (!(asstFileName == null || asstFileName.size() == 0 || assetMgr == null)) {
            String[] fileNames = asstFileName.toArray(new String[0]);
            ZipFile zipFile = null;
            for (String name : fileNames) {
                try {
                    //File tmpFile = new File(defAssetPath + name);
                    InputStream ips = assetMgr.open(name);
                    ZipInputStream zips = new ZipInputStream(ips);
                    ZipEntry zE = null ;
                    if (zips != null) {
                        byte[] BaopS = null;
                        while ((zE = zips.getNextEntry()) != null) {
                            if (!zE.isDirectory() && zE.getName().equals("appinfo")) {
                                Log.d("getSp200FirmWareVersion" , "" + zE.getName());
                                BaopS =  new byte[(int)zE.getSize()];
                                zips.read(BaopS, 0 , BaopS.length);
                                //ByteArrayOutputStream BaopS = new ByteArrayOutputStream(zips.read());
                                if (BaopS != null && BaopS.length > 0) {
                                    String content = new String(BaopS, "UTF-8");
                                    if (content != null) {
                                        String[] splitter = content.split("\n");
                                        FwInfos = new SP200FirmwareInfos();
                                        for (String ln : splitter) {
                                            if (ln.contains("version=")) {
                                                FwInfos.FirmwareName = name;
                                                FwInfos.FirmwareSource = SP200FirmwareInfos.enumSourceFirmware.SOFTFILE_FIRMWARE;
                                                FwInfos.FirmwareVersion = ln.replace("version=","").trim();
                                                Log.d("getSp200FirmWareVersion","Firmware version = " +FwInfos.FirmwareVersion);

                                                return FwInfos;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    Log.e("getSp200FirmWareVersion" , "Error during open zip filename '" + name +  "' "+ ioe.getMessage());
                } finally {
                    try {
                        if (zipFile != null) {
                            zipFile.close();
                        }
                    } catch (IOException ioex) {
                        ioex.printStackTrace();
                        Log.e("getSp200FirmWareVersion" , "Error while closing zip filename '" + name +  "' "+ ioex.getMessage());
                    }
                }
            }
        }

        return null;
    }


    /* =========================================================================================================================================
    *    This section is use for TLE Singlehost download onlt
    * =========================================================================================================================================  */

    public static final String TLE_BANK_NAME_KBANK = "KBANK";
    public static final String TLE_BANK_NAME___BAY = "BAY";
    public static final String TLE_BANK_NAME___SCB = "SCB";
    public static final String TLE_BANK_NAME___AMEX = "AMEX";
    public enum Mode {TleBankName, TleAcquirer}

    public static String TleBankAndAcquirerMapper (String tleBankName) {
        HashMap<String, String> map = new HashMap<>();
        map.put(TLE_BANK_NAME_KBANK, Constants.ACQ_KBANK);
        map.put(TLE_BANK_NAME___BAY, Constants.ACQ_BAY_INSTALLMENT);
        map.put(TLE_BANK_NAME___SCB, Constants.ACQ_SCB_IPP);

        if (map.containsKey(tleBankName)) {
            return map.get(tleBankName);
        } else {
            return null;
        }
    }


    public static ArrayList<String> getTLEPrimaryAcquirerList() {
        return getTLEPrimaryAcquirerList(Mode.TleAcquirer);
    }
    public static ArrayList<String> getTLEPrimaryAcquirerList(Mode mode) {
        if (mode==Mode.TleBankName) {
            ArrayList<String> tleBankHostList = new ArrayList<String>();
            tleBankHostList.add(TLE_BANK_NAME_KBANK);          // KBANK + TPN + DCC + REDEEM + SMART PAY
            tleBankHostList.add(TLE_BANK_NAME___BAY);          // BAY
            return tleBankHostList;
        } else {
            ArrayList<String> primaryAcqNameList = new ArrayList<String>();
            primaryAcqNameList.add(TleBankAndAcquirerMapper(TLE_BANK_NAME_KBANK));          // KBANK + TPN + DCC + REDEEM + SMART PAY
            primaryAcqNameList.add(TleBankAndAcquirerMapper(TLE_BANK_NAME___BAY));          // BAY
            return primaryAcqNameList;
        }
    }

    public static ArrayList<Acquirer> getSubHostByTleBankName(String targAcqName, String targetTleBankName) {
        List<Acquirer> acqList = FinancialApplication.getAcqManager().findAllAcquirers();
        ArrayList<Acquirer> expotAcqList = new ArrayList<Acquirer>();
        if (acqList != null && acqList.size() >0) {
            for (Acquirer localAcquire : acqList) {
                if (localAcquire.isEnable()
                        && localAcquire.isEnableTle()
                        && localAcquire.getTleBankName()!=null
                        && localAcquire.getTleBankName().equals(targetTleBankName)
                        && !localAcquire.getName().equals(targAcqName))  {
                    expotAcqList.add(localAcquire);
                }
            }
        } else {
            expotAcqList = new ArrayList<Acquirer>();
        }

        return expotAcqList;
    }

    //=========================================================================================================================================

    @NonNull
    public static String getEnString(@StringRes int resId) {
        Locale desiredLocale = UILanguage.ENGLISH.getLocale();
        Configuration conf = FinancialApplication.getApp().getResources().getConfiguration();
        conf = new Configuration(conf);
        conf.setLocale(desiredLocale);
        Context localizedContext = FinancialApplication.getApp().createConfigurationContext(conf);
        return localizedContext.getResources().getString(resId);
    }

    @NonNull
    public static String getEnString(@StringRes int resId, Object... formatArgs) {
        Locale desiredLocale = UILanguage.ENGLISH.getLocale();
        Configuration conf = FinancialApplication.getApp().getResources().getConfiguration();
        conf = new Configuration(conf);
        conf.setLocale(desiredLocale);
        Context localizedContext = FinancialApplication.getApp().createConfigurationContext(conf);
        return localizedContext.getResources().getString(resId, formatArgs);
    }

    @NonNull
    public static String getThString(@StringRes int resId) {
        Locale desiredLocale = UILanguage.THAI.getLocale();
        Configuration conf = FinancialApplication.getApp().getResources().getConfiguration();
        conf = new Configuration(conf);
        conf.setLocale(desiredLocale);
        Context localizedContext = FinancialApplication.getApp().createConfigurationContext(conf);
        return localizedContext.getResources().getString(resId);
    }

    @NonNull
    public static String getThString(@StringRes int resId, Object... formatArgs) {
        Locale desiredLocale = UILanguage.THAI.getLocale();
        Configuration conf = FinancialApplication.getApp().getResources().getConfiguration();
        conf = new Configuration(conf);
        conf.setLocale(desiredLocale);
        Context localizedContext = FinancialApplication.getApp().createConfigurationContext(conf);
        return localizedContext.getResources().getString(resId, formatArgs);
    }

    public static void updateAndSaveLanParam(LanParam lanParam) {
        SysParam sysParam = FinancialApplication.getSysParam();
        sysParam.set(SysParam.BooleanParam.LAN_DHCP, lanParam.isDhcp());
        sysParam.set(SysParam.StringParam.LAN_DNS1, lanParam.getDns1());
        sysParam.set(SysParam.StringParam.LAN_DNS2, lanParam.getDns2());
        sysParam.set(SysParam.StringParam.LAN_GATEWAY, lanParam.getGateway());
        sysParam.set(SysParam.StringParam.LAN_LOCAL_IP, lanParam.getLocalIp());
        sysParam.set(SysParam.StringParam.LAN_NETMASK,  lanParam.getSubnetMask());

        IDalCommManager commManager = FinancialApplication.getDal().getCommManager();
        try {
            commManager.setLanParam(lanParam);
            IChannel channel = commManager.getChannel(EChannelType.LAN);
            if (channel.isEnabled()) {
                channel.disable();
                channel.enable();
            }
        } catch (ChannelException e) {
            e.printStackTrace();
        }
    }

    public static String safeConvertByteArrayToString (byte[] srcArr) {
        if (srcArr == null) { return  null ;}
        if (srcArr.length == 0) {return null;}
        try {
            String returnStr = new String(srcArr,"UTF8");
            return returnStr;
        } catch (Exception ex) {
            Log.e("GT1C","ERROR on convert byte-array to string") ;
        }
        return null;
    }

    @Nullable
    public static Bitmap getImageFromPath(@NonNull String path) {
        Bitmap bmp = null;
        try {
            FileInputStream fInput = new FileInputStream(path);
            int fSize = fInput.available();
            if (fSize > 0) {
                byte[] bArrInput = new byte[fSize - 1];
                fInput.read(bArrInput, 0, bArrInput.length);
                fInput.close();

                if (bArrInput.length > 0 ) {
                    bmp = BitmapFactory.decodeByteArray(bArrInput, 0, bArrInput.length);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return bmp;
    }

}




