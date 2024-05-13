package com.pax.pay;

import android.content.Context;
import android.content.SharedPreferences;
import th.co.bkkps.utils.Log;

import com.pax.pay.app.FinancialApplication;
import com.pax.settings.SysParam;

public class TerminalLockCheck {

    protected static final String TAG = TerminalLockCheck.class.getSimpleName();

    private static long INIT_TIME           = 0;
    private static TerminalLockCheck ref    = null;
    //private static SharedPreferences sp = null;

    private static int screenLockTimeoutMs = 30000;

    private static boolean KioskModeEnable = true;

    private static final String CONFIG_FILE_NAME = "generalParam";

    private TerminalLockCheck(){
        //sp = context.getSharedPreferences(CONFIG_FILE_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized TerminalLockCheck getInstance() {
        if (ref == null){
            ref = new TerminalLockCheck();
        }
        return ref;
    }

    public void init(){
        TerminalLockCheck.INIT_TIME = System.currentTimeMillis();
    }

    public void forceLock(){
        TerminalLockCheck.INIT_TIME = 0;
    }

    public boolean isLocked(){
        if (!KioskModeEnable){
            return false;
        }
        long currentTime    = System.currentTimeMillis();
        long threshold      = screenLockTimeoutMs; //30 sec //sp.getLong("lock_screen_timeout", 30000); // check here, might change in between calls
        long difftime = currentTime - TerminalLockCheck.INIT_TIME;

        Log.e(TAG, "difftime = " +  difftime);
        if (difftime > threshold){
            return true;
        }
        return false;
    }//end method

    public void setKioskMode(boolean enable){
        KioskModeEnable = enable;
    }

    public boolean getKioskMode(){
        KioskModeEnable = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_KIOSK_MODE);
        return KioskModeEnable;
    }

    public int setScreenLockTimeoutMs(int timeOut){
        return screenLockTimeoutMs;
    }

    public int getScreenLockTimeoutMs(){
        screenLockTimeoutMs = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_KIOSK_TIMEOUT,0)*1000;
        return screenLockTimeoutMs;
    }


}//end class