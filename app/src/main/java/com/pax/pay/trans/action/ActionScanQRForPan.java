package com.pax.pay.trans.action;

import android.content.Context;
import android.os.ConditionVariable;
import android.os.SystemClock;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.dal.IScanner;
import com.pax.dal.IScanner.IScanListener;
import com.pax.dal.entity.EScannerType;
import com.pax.dal.entity.PollingResult;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.emv.EmvSP200;
import com.pax.pay.trans.action.activity.SearchCardForPanActivity;
import com.pax.pay.uart.SP200_serialAPI;
import com.pax.pay.utils.TickTimer;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import th.co.bkkps.utils.Log;

public class ActionScanQRForPan extends AAction {

    private Context context;
    private String title;
    private String qrCode = null;
    private String qrSP200 = null;
    private IScanner scanner = null;
    IScanListener iScanListener = null;
    boolean isS200run;
    private PollingResult pollingResult = null;
    private boolean isTimeOut = false;
    private SearchSP200Thread mSearchSP200Thread = null;
    private SP200_serialAPI sp200API = SP200_serialAPI.getInstance();
    private boolean isSP200Enable = false;
    private boolean sp200scan = false;
    private int timeout;

    /**
     * @param context
     * @param title
     * @param timeout
     */
    public void setParam(Context context, String title, int timeout) {
        this.context = context;
        this.title = title;
        this.timeout = timeout;
    }

    public ActionScanQRForPan(ActionStartListener listener) {
        super(listener);

        iScanListener = new IScanListener() {
            @Override
            public void onCancel() {
                // DO NOT call setResult here since it will be can in onFinish
            }

            @Override
            public void onFinish() {
                if (!sp200scan) {
                    if (isSP200Enable) {
                        SP200_serialAPI.getInstance().BreakReceiveThread();
                        SP200_serialAPI.getInstance().setSp200Cancel(true);
                        SP200_serialAPI.getInstance().cancelSP200();
                    }
                    if (scanner != null) {
                        scanner.close();
                    }
                    if (qrCode != null && qrCode.length() > 0) {
                        setResult(new ActionResult(TransResult.SUCC, qrCode));
                        qrCode = null;
                    } else { //FIXME press key back on A920C while scanning should return to onCancel firstly
                        setResult(new ActionResult(TransResult.ERR_USER_CANCEL, null));
                    }
                }
            }

            @Override
            public void onRead(String content) {
                qrCode = content;
            }
        };
    }

    @Override
    protected void process() {

        FinancialApplication.getApp().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (Utils.getString(R.string.back_camera).equals(FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_DEFAULT_CAMERA))) {
                    scanner = Device.getScanner(EScannerType.REAR);
                } else {
                    scanner = Device.getScanner(EScannerType.FRONT);
                }
                //scanner.close(); // 系统扫码崩溃之后，再调用掉不起来

                scanner.open();
                scanner.setTimeOut(timeout * 1000);
                scanner.setContinuousTimes(1);
                scanner.setContinuousInterval(1000);
                scanner.start(iScanListener);

                isSP200Enable = SP200_serialAPI.getInstance().isSp200Enable();
                if (isSP200Enable) {
                    scanQRviaSP200();
                }
            }
        });
    }

    protected void scanQRviaSP200() {
        SystemClock.sleep(200);
        int iRet = SP200_serialAPI.getInstance().checkStatusSP200();
        if (iRet == 0) {
            qrSP200 = SP200_serialAPI.getInstance().ScanQRForPan(timeout);
            mSearchSP200Thread = new SearchSP200Thread();
            isS200run = true;
            mSearchSP200Thread.start();
        } else {
            isSP200Enable = false;
        }
    }

    private class SearchSP200Thread extends Thread {
        SP200_serialAPI sp200API = SP200_serialAPI.getInstance();

        @Override
        public void run() {
            sp200API.StartReceiveThread(2);
            FinancialApplication.getApp().runInBackground(new Runnable() {
                @Override
                public void run() {
                    if (qrSP200 == null && isS200run) {
                        sp200API.BreakReceiveThread();
                        SP200_serialAPI.getInstance().cancelSP200();
                    } else {
                        onReadSP200Result();
                    }
                }
            });
        }

        private void onReadSP200Result() {
            if (qrSP200 != null && qrSP200 != "") {
                sp200scan = true;
                if (scanner != null) {
                    scanner.close();
                }
                if (qrSP200 != null && qrSP200.length() > 0) {
                    setResult(new ActionResult(TransResult.SUCC, qrSP200));
                    qrSP200 = null;
                } else { //FIXME press key back on A920C while scanning should return to onCancel firstly
                    setResult(new ActionResult(TransResult.ERR_USER_CANCEL, null));
                }
            }
        }
    }
}
