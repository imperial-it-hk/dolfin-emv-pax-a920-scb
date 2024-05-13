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
package com.pax.device;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.ConditionVariable;
import android.os.Looper;
import android.os.SystemClock;
import th.co.bkkps.utils.Log;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import com.pax.dal.ICardReaderHelper;
import com.pax.dal.IPed;
import com.pax.dal.IPicc;
import com.pax.dal.IScanner;
import com.pax.dal.entity.*;
import com.pax.dal.exceptions.IccDevException;
import com.pax.dal.exceptions.MagDevException;
import com.pax.dal.exceptions.PedDevException;
import com.pax.dal.exceptions.PiccDevException;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.glwrapper.convert.IConvert.EPaddingPosition;
import com.pax.glwrapper.page.IPage;
import com.pax.jemv.clcommon.RetCode;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;
import com.pax.view.dialog.CustomAlertDialog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Device method
 */
public class Device {

    private static final String TAG = "Device";

    private Device() {
        //do nothing
    }

    /**
     * beep ok
     */
    public static void beepOk() {/*
        FinancialApplication.getDal().getSys().beep(EBeepMode.FREQUENCE_LEVEL_3, 100);
        FinancialApplication.getDal().getSys().beep(EBeepMode.FREQUENCE_LEVEL_4, 100);
        FinancialApplication.getDal().getSys().beep(EBeepMode.FREQUENCE_LEVEL_5, 100);
        */
    }

    /**
     * beep error
     */
    public static void beepErr() {
        /*
        if (FinancialApplication.getDal() != null) {
            FinancialApplication.getDal().getSys().beep(EBeepMode.FREQUENCE_LEVEL_6, 200);
        }

         */
    }

    /**
     * beep prompt
     */
    public static void beepPrompt() {
        //FinancialApplication.getDal().getSys().beep(EBeepMode.FREQUENCE_LEVEL_6, 50);
    }

    /**
     * get formatted date/time
     *
     * @param pattern date format, e.g.{@link Constants#TIME_PATTERN_TRANS}
     * @return formatted data valueshowSuccMessage
     */
    public static String getTime(String pattern) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, Locale.US);
        return dateFormat.format(new Date());
    }

    /**
     * enable/disable status bar
     *
     * @param enable true/false
     */
    public static void enableStatusBar(boolean enable) {
        FinancialApplication.getDal().getSys().enableStatusBar(enable);
    }

    /**
     * enable/disable home and recent key
     *
     * @param enable true/false
     */
    public static void enableHomeRecentKey(boolean enable) {
        FinancialApplication.getDal().getSys().enableNavigationKey(ENavigationKey.HOME, enable);
        FinancialApplication.getDal().getSys().enableNavigationKey(ENavigationKey.RECENT, enable);
    }

    /**
     * enable/disable back key
     *
     * @param enable true/false
     */
    public static void enableBackKey(boolean enable) {
        FinancialApplication.getDal().getSys().enableNavigationKey(ENavigationKey.BACK, enable);
    }

    /**
     * write TMK
     *
     * @param tmkIndex TMK index
     * @param tmkValue TMK value
     * @throws PedDevException exception
     */
    public static void writeTMK(int tmkIndex, byte[] tmkValue) throws PedDevException {
        // write TMK
        FinancialApplication.getDal().getPed(EPedType.INTERNAL).writeKey(EPedKeyType.TLK, (byte) 0,
                EPedKeyType.TMK, (byte) Utils.getMainKeyIndex(tmkIndex),
                tmkValue, ECheckMode.KCV_NONE, null);
    }

    /**
     * write TPK, index is {@link Constants#INDEX_TPK}
     *
     * @param tpkValue TPK value
     * @param tpkKcv   TPK KCV
     * @throws PedDevException exception
     */
    public static void writeTPK(byte[] tpkValue, byte[] tpkKcv) throws PedDevException {
        int mKeyIndex = Utils.getMainKeyIndex(FinancialApplication.getSysParam().get(SysParam.NumberParam.MK_INDEX));
        ECheckMode checkMode = ECheckMode.KCV_ENCRYPT_0;
        if (tpkKcv == null || tpkKcv.length == 0) {
            checkMode = ECheckMode.KCV_NONE;
        }
        FinancialApplication.getDal().getPed(EPedType.INTERNAL).writeKey(EPedKeyType.TMK, (byte) mKeyIndex,
                EPedKeyType.TPK, Constants.INDEX_TPK, tpkValue, checkMode, tpkKcv);
    }


    /**
     * write TAK, index is {@link Constants#INDEX_TAK}
     *
     * @param takValue TAK value
     * @param takKcv   TAK KCV
     * @throws PedDevException exception
     */
    public static void writeTAK(byte[] takValue, byte[] takKcv) throws PedDevException {
        int mKeyIndex = Utils.getMainKeyIndex(FinancialApplication.getSysParam().get(SysParam.NumberParam.MK_INDEX));
        ECheckMode checkMode = ECheckMode.KCV_ENCRYPT_0;
        if (takKcv == null || takKcv.length == 0) {
            checkMode = ECheckMode.KCV_NONE;
        }
        FinancialApplication.getDal().getPed(EPedType.INTERNAL).writeKey(EPedKeyType.TMK, (byte) mKeyIndex,
                EPedKeyType.TAK, Constants.INDEX_TAK, takValue, checkMode, takKcv);
    }


    /**
     * write TDK index is {@link Constants#INDEX_TDK}
     *
     * @param tdkValue TDK value
     * @param tdkKcv   TDK KCV
     * @throws PedDevException exception
     */
    public static void writeTDK(byte[] tdkValue, byte[] tdkKcv) throws PedDevException {
        int mKeyIndex = Utils.getMainKeyIndex(FinancialApplication.getSysParam().get(SysParam.NumberParam.MK_INDEX));
        ECheckMode checkMode = ECheckMode.KCV_ENCRYPT_0;
        if (tdkKcv == null || tdkKcv.length == 0) {
            checkMode = ECheckMode.KCV_NONE;
        }
        FinancialApplication.getDal().getPed(EPedType.INTERNAL).writeKey(EPedKeyType.TMK, (byte) mKeyIndex,
                EPedKeyType.TDK, Constants.INDEX_TDK, tdkValue, checkMode, tdkKcv);
    }

    /**
     * calculate PIN block
     *
     * @param panBlock shifted pan block
     * @return PIN block
     * @throws PedDevException exception
     */
    public static byte[] getPinBlock(String panBlock, boolean supportBypass, boolean landscape, int timeoutSec) throws PedDevException {
        String tpk = FinancialApplication.getGeneralParam().get(GeneralParam.TPK);
        if (tpk != null && !tpk.isEmpty()) {
            writeTPK(FinancialApplication.getConvert().strToBcd(tpk, EPaddingPosition.PADDING_RIGHT), null);
        }
        IPed ped = FinancialApplication.getDal().getPed(EPedType.INTERNAL);
        String pinLen = "4,5,6,7,8,9,10,11,12";
        if (supportBypass) {
            pinLen = "0," + pinLen;
        }
        ped.setKeyboardLayoutLandscape(landscape);
        EPinBlockMode block = EPinBlockMode.ISO9564_0;
        return ped.getPinBlock(Constants.INDEX_TPK, pinLen, panBlock.getBytes(), block, timeoutSec * 1000);
    }

    public static byte[] getUpiPinBlock(String panBlock, boolean supportBypass, boolean landscape, int timeoutSec) throws PedDevException {
        Acquirer acq = FinancialApplication.getAcqManager().getCurAcq();
        if (!acq.isEnableUpi())
        {
            return "".getBytes();
        }

        if (UserParam.TEST_MODE)
        {
            return "".getBytes();
        }

        int keyID = acq.getKeyId();

        IPed ped = FinancialApplication.getDal().getPed(EPedType.INTERNAL);
        String pinLen = "4,5,6,7,8,9,10,11,12";
        if (supportBypass) {
            pinLen = "0," + pinLen;
        }
        ped.setKeyboardLayoutLandscape(landscape);
        EPinBlockMode block = EPinBlockMode.ISO9564_0;
        return ped.getPinBlock((byte)(UserParam.getTPK_TPKID(acq)), pinLen, panBlock.getBytes(), block,  timeoutSec* 1000);
    }

    /**
     * erase all keys
     */
    public static boolean eraseKeys() {
        try {
            return FinancialApplication.getDal().getPed(EPedType.INTERNAL).erase();
        } catch (PedDevException e) {
            Log.e(TAG, "", e);
        }
        return false;
    }

    /**
     * calculate MAC with TAK
     *
     * @param data input data
     * @return MAC value
     */
    @NonNull
    public static byte[] calcMac(String data) {
        try {
            String tak = FinancialApplication.getGeneralParam().get(GeneralParam.TAK);
            if (tak != null && !tak.isEmpty()) {
                writeTAK(FinancialApplication.getConvert().strToBcd(tak, EPaddingPosition.PADDING_RIGHT), null);
            }
            IPed ped = FinancialApplication.getDal().getPed(EPedType.INTERNAL);
            return ped.getMac(Constants.INDEX_TAK, data.getBytes(), EPedMacMode.MODE_00);
        } catch (PedDevException e) {
            Log.w(TAG, e);
        }
        return "".getBytes();
    }

    public static void setPiccLed(final @IntRange(from = -1, to = 3) int index, int status){
        final IPicc picc = FinancialApplication.getDal().getPicc(EPiccType.INTERNAL);
        try {
            if(index >= 0 && status > 0) {
                picc.setLed((byte) (1 << (3 - index)));
            } else {
                picc.setLed((byte)0);
            }
        } catch (PiccDevException e) {
            Log.e(TAG, "", e);
        }
    }

    /**
     * calculate DES with TDK
     *
     * @param data input data
     * @return DES value
     * @throws PedDevException exception
     */
    public static byte[] calcDes(byte[] data) throws PedDevException {
        String tdk = FinancialApplication.getGeneralParam().get(GeneralParam.TDK);
        if (tdk != null && !tdk.isEmpty()) {
            writeTDK(FinancialApplication.getConvert().strToBcd(tdk, EPaddingPosition.PADDING_RIGHT), null);
        }
        IPed ped = FinancialApplication.getDal().getPed(EPedType.INTERNAL);
        return ped.calcDes(Constants.INDEX_TDK, data, EPedDesMode.ENCRYPT);
    }

    /**
     * generate IPage with pre-load type face
     *
     * @return {@link IPage}
     */
    public static IPage generatePage() {
        IPage page = FinancialApplication.getGl().getImgProcessing().createPage();
        page.adjustLineSpace(-9);
        page.setTypefaceObj(Typeface.createFromAsset(FinancialApplication.getApp().getAssets(), Constants.FONT_NAME));
        return page;
    }

    /**
     * generate IPage with pre-load type face
     *
     * @return {@link IPage}
     */
    public static IPage generatePage(Boolean checkTOPS) {
        if(checkTOPS && Constants.isTOPS){
            IPage page = FinancialApplication.getGl().getImgProcessing().createPage();
            page.adjustLineSpace(-5);
            page.setTypefaceObj(Typeface.createFromAsset(FinancialApplication.getApp().getAssets(),Constants.FONT_NAME_TAHOMA));
            return page;
        }else{
            IPage page = FinancialApplication.getGl().getImgProcessing().createPage();
            page.adjustLineSpace(-9);
            page.setTypefaceObj(Typeface.createFromAsset(FinancialApplication.getApp().getAssets(), Constants.FONT_NAME));
            return page;
        }
    }

    /**
     * remove card listener, for showing message which polling
     */
    public interface RemoveCardListener {
        void onShowMsg(PollingResult result);
    }

    /**
     * remove card listener, for showing message which polling, and return result
     */
    public interface RemoveCardWithResultListener {
        int onShowMsg(PollingResult result);
    }

    /**
     * force to remove card with prompting message
     *
     * @param listener remove card listener
     */
    public static void removeCard(RemoveCardListener listener) {
        ICardReaderHelper helper = FinancialApplication.getDal().getCardReaderHelper();

        try {
            PollingResult result;
            while ((result = helper.polling(EReaderType.ICC_PICC, 100)).getReaderType() == EReaderType.ICC || result.getReaderType() == EReaderType.PICC) {
                // remove card prompt
                if (listener != null)
                    listener.onShowMsg(result);
                SystemClock.sleep(500);
                Device.beepErr();
            }
        } catch (MagDevException | IccDevException | PiccDevException e) {
            //ignore the warning
        }
    }

    /**
     * 获取扫码器
     */
    public static IScanner getScanner(EScannerType type) {
        return FinancialApplication.getDal().getScanner(type);
    }

    /**
     * Force to tap card on EDC with prompt message
     * @param listener
     */
    public static int detectCard(RemoveCardWithResultListener listener) {
        ICardReaderHelper helper = FinancialApplication.getDal().getCardReaderHelper();
        try {
            PollingResult result;
            boolean isShown = false;
            Looper.prepare();
            while (true) {
                if ((result = helper.polling(EReaderType.ICC_PICC, 100)).getReaderType() == EReaderType.ICC || result.getReaderType() == EReaderType.PICC) {
                    break;
                } else {
                    if (isShown) {
                        continue;
                    }
                    if (listener != null) {
                        listener.onShowMsg(result);
                        isShown = true;
                    }
                    SystemClock.sleep(500);
                }
            }
        } catch (MagDevException | IccDevException | PiccDevException e) {
            Log.e(TAG, "", e);
            return e.getErrCode();
        }
        return TransResult.SUCC;
    }

    private static int retResult;
    private static CustomAlertDialog dialog;

    /**
     * Force to tap card on EDC with prompt message (allow cancel transaction)
     * @param message
     * @param timeout
     * @param context
     * @return
     */
    public static int detectCard(final String message, final int timeout, final Context context) {
        dialog = null;
        retResult = RetCode.EMV_OK;
        final ConditionVariable cv = new ConditionVariable();
        FinancialApplication.getApp().runInBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    PollingResult result;
                    ICardReaderHelper helper = FinancialApplication.getDal().getCardReaderHelper();
                    Looper.prepare();
                    while (true) {
                        if ((result = helper.polling(EReaderType.ICC_PICC, 100)).getReaderType() == EReaderType.ICC || result.getReaderType() == EReaderType.PICC) {
                            if (dialog != null) {
                                dialog.dismiss();
                            }
                            cv.open();
                            break;
                        } else {
                            if (dialog != null) {
                                continue;
                            }
                            FinancialApplication.getApp().runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    if (dialog == null) {
                                        dialog = new CustomAlertDialog(context, CustomAlertDialog.WARN_TYPE);
                                        dialog.show();
                                        dialog.setImage(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic16));
                                    }
                                    dialog.setTimeout(timeout);
                                    dialog.setContentText(message);
                                    dialog.showConfirmButton(true);
                                    dialog.setCancelable(true);
                                    dialog.setCanceledOnTouchOutside(false);
                                    dialog.setConfirmClickListener(new CustomAlertDialog.OnCustomClickListener() {
                                        @Override
                                        public void onClick(CustomAlertDialog alertDialog) {
                                            cv.open();
                                        }
                                    });
                                    dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                        @Override
                                        public void onDismiss(DialogInterface dialog) {
                                            retResult = RetCode.EMV_USER_CANCEL;
                                            cv.open();
                                        }
                                    });
                                }
                            });
                        }
                    }
                } catch (MagDevException | IccDevException | PiccDevException e) {
                    Log.e(TAG, "", e);
                    retResult = e.getErrCode();
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                    cv.open();
                }
            }
        });
        cv.block();

        return retResult;
    }
}
