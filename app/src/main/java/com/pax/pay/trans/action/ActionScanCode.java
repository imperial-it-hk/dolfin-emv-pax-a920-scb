package com.pax.pay.trans.action;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.dal.IScanner;
import com.pax.dal.IScanner.IScanListener;
import com.pax.dal.entity.EScannerType;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.utils.TickTimer;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

public class ActionScanCode extends AAction {

    private Context context;
    private String title;
    private String amount;
    private String qrCode = null;
    private IScanner scanner = null;
    IScanListener iScanListener = null;

    /**
     * @param context
     * @param title
     * @param amount
     */
    public void setParam(Context context, String title, String amount) {
        this.context = context;
        this.title = title;
        this.amount = amount;
    }

    public ActionScanCode(ActionStartListener listener) {
        super(listener);

        iScanListener = new IScanListener() {
            @Override
            public void onCancel() {
                // DO NOT call setResult here since it will be can in onFinish
            }

            @Override
            public void onFinish() {
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
                scanner.setTimeOut(TickTimer.DEFAULT_TIMEOUT*1000);
                scanner.setContinuousTimes(1);
                scanner.setContinuousInterval(1000);
                scanner.start(iScanListener);
            }
        });
    }
}
