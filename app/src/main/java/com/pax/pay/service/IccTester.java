package com.pax.pay.service;

import th.co.bkkps.utils.Log;

import com.pax.dal.IIcc;
import com.pax.dal.exceptions.IccDevException;
import com.pax.pay.app.FinancialApplication;

public class IccTester {

    private static IccTester iccTester;

    private IIcc icc;

    private IccTester() {
        icc = FinancialApplication.getDal().getIcc();
    }

    public static IccTester getInstance() {
        if (iccTester == null) {
            iccTester = new IccTester();
        }
        return iccTester;
    }

    public byte[] init(byte slot) {
        byte[] initRes = null;
        try {
            initRes = icc.init(slot);
            Log.i("init","init");
            return initRes;
        } catch (IccDevException e) {
            e.printStackTrace();
            Log.e("init",e.toString());
            return null;
        }
    }

    public boolean detect(byte slot) {
        boolean res = false;
        try {
            res = icc.detect(slot);
            Log.i("detect","detect");
            return res;
        } catch (IccDevException e) {
            e.printStackTrace();
            Log.e("detect",e.toString());
            return res;
        }
    }

    public void close(byte slot) {
        try {
            icc.close(slot);
            Log.i("close","close");
        } catch (IccDevException e) {
            e.printStackTrace();
            Log.e("close",e.toString());
        }
    }

    public void autoResp(byte slot, boolean autoresp) {
        try {
            icc.autoResp(slot, autoresp);
            Log.i("autoResp","autoResp");
        } catch (IccDevException e) {
            e.printStackTrace();
            Log.e("autoResp",e.toString());
        }
    }

    public byte[] isoCommand(byte slot, byte[] send) {
        try {
            byte[] resp = icc.isoCommand(slot, send);
            Log.i("isoCommand","isoCommand");
            return resp;
        } catch (IccDevException e) {
            e.printStackTrace();
            Log.e("isoCommand",e.toString());
            return null;
        }
    }
    
    public void light(boolean flag){
        try {
            icc.light(flag);
            Log.i("light","light");
        } catch (IccDevException e) {
            e.printStackTrace();
            Log.e("light",e.toString());
        }
    }
}
