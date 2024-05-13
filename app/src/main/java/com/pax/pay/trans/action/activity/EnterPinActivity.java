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
package com.pax.pay.trans.action.activity;

import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.SystemClock;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.dal.IIcc;
import com.pax.dal.IPed;
import com.pax.dal.entity.EKeyCode;
import com.pax.dal.entity.EPedType;
import com.pax.dal.entity.RSAPinKey;
import com.pax.dal.exceptions.EPedDevException;
import com.pax.dal.exceptions.IccDevException;
import com.pax.dal.exceptions.PedDevException;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.exception.EEmvExceptions;
import com.pax.eventbus.EmvCallbackEvent;
import com.pax.pay.BaseActivityWithTickForAction;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.action.ActionEnterPin.EEnterPinType;
import com.pax.pay.trans.action.ActionEnterPin.OfflinePinResult;
import com.pax.pay.trans.action.ActionRecoverKBankLoadTWK;
import com.pax.pay.trans.action.ActionUpiTransOnline;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.uart.SP200_serialAPI;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.IApdu;
import com.pax.pay.utils.Packer;
import com.pax.pay.utils.ToastUtils;
import com.pax.pay.utils.TransResultUtils;
import com.pax.pay.utils.Utils;
import com.pax.pay.utils.ViewUtils;
import com.pax.sdk.Sdk;
import com.pax.view.dialog.CustomAlertDialog;
import com.pax.view.dialog.DialogUtils;

import th.co.bkkps.utils.Log;

public class EnterPinActivity extends BaseActivityWithTickForAction {

    private TextView pwdTv;

    private String title;
    private String panBlock;
    private String prompt2;
    private String prompt1;
    private String totalAmount;
    private String tipAmount;

    private CustomAlertDialog promptDialog;

    private boolean supportBypass;
    private boolean isFirstStart = true;// 判断界面是否第一次加载

    private EEnterPinType enterPinType;
    private RSAPinKey rsaPinKey;

    private static final byte ICC_SLOT = 0x00;
    public static final String OFFLINE_EXP_PIN_LEN = "0,4,5,6,7,8,9,10,11,12";

    private boolean landscape = false;

    final ConditionVariable cv = new ConditionVariable();

    private Acquirer acquirer;

    private TransData transData;

    private IPed ped = FinancialApplication.getDal().getPed(EPedType.INTERNAL);

    private int activityTimeoutSec = 60;

    private boolean isSP200Enable = SP200_serialAPI.getInstance().isSp200Enable();
    private boolean supportSP200 = false;

    private IPed.IPedInputPinListener pedInputPinListener = new IPed.IPedInputPinListener() {

        @Override
        public void onKeyEvent(final EKeyCode key) {
            String temp;
            if (key == EKeyCode.KEY_CLEAR) {
                temp = "";
            } else if (key == EKeyCode.KEY_ENTER || key == EKeyCode.KEY_CANCEL) {
                // do nothing
                return;
            } else {
                temp = pwdTv.getText().toString();
                temp += "*";
            }
            setContentText(temp);
        }
    };

    private IPed.IPedInputPinListener inputPCIPinListener = new IPed.IPedInputPinListener() {

        @Override
        public void onKeyEvent(final EKeyCode key) {
            //AET-148
            String temp = pwdTv.getText().toString();
            if (key == EKeyCode.KEY_CLEAR) {
                temp = "";
            } else if (key == EKeyCode.KEY_CANCEL) {
                ped.setInputPinListener(null);
                finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
                return;
            } else if (key == EKeyCode.KEY_ENTER) {
                if (temp.length() > 3 || temp.length() == 0) {
                    ped.setInputPinListener(null);
                    finish(new ActionResult(TransResult.SUCC, null));
                    return;
                }
            } else {
                temp += "*";
            }

            setContentText(temp);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        // 界面不需要超时， 超时有输密码接口控制
        tickTimer.stop();
    }

    // 当页面加载完成之后再执行弹出键盘的动作
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        quickClickProtection.start();
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && isFirstStart) {
            FinancialApplication.getApp().runInBackground(new DetectFingerRunnable());
            isFirstStart = false;
        }
    }

    private void processUPLogon(final AAction currentAction) {
        final TransData transData = new TransData();
        Component.transInit(transData, acquirer);

        // TLE Logon
        transData.setTransType(ETransType.LOADTWK);
        ActionUpiTransOnline upiTrans = new ActionUpiTransOnline(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionUpiTransOnline) action).setParam(EnterPinActivity.this, transData);
            }
        });

        upiTrans.setEndListener(new AAction.ActionEndListener() {
            @Override
            public void onEnd(AAction action, ActionResult result) {
                if (result.getRet() == TransResult.SUCC && transData.getBytesField62() != null) {
                    // Recover UP TWK
                    ActionRecoverKBankLoadTWK recTWK = new ActionRecoverKBankLoadTWK(new AAction.ActionStartListener() {

                        @Override
                        public void onStart(AAction action) {
                            ((ActionRecoverKBankLoadTWK) action).setParam(EnterPinActivity.this, transData.getField62());
                        }
                    });

                    recTWK.setEndListener(new AAction.ActionEndListener() {
                        @Override
                        public void onEnd(AAction action, ActionResult result) {
                            cv.open();
                        }
                    });
                    recTWK.execute();
                } else {
                    cv.open();
                }
                ActivityStack.getInstance().popTo(EnterPinActivity.this);
                TransContext.getInstance().getCurrentAction().setFinished(true);
                TransContext.getInstance().setCurrentAction(currentAction);
            }
        });
        upiTrans.execute();
    }

    private class DetectFingerRunnable implements Runnable {
        @Override
        public void run() {
            //AET-226
            //workaround:get the touch event from by native code which may
            com.pax.sdk.Sdk.TouchEvent nte = Sdk.getInstance().getTouchEvent();
            while (nte.detect(200)) {
                FinancialApplication.getApp().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtils.showMessage(R.string.no_long_press);
                    }
                });
                SystemClock.sleep(500);
            }
            if (enterPinType == EEnterPinType.ONLINE_PIN) {
                if (Component.getTransDataInstance() != null
                        && Component.getTransDataInstance().getTransType() == ETransType.OFFLINE_TRANS_SEND) {
                    DialogUtils.showErrMessage(EnterPinActivity.this,
                            Component.getTransDataInstance().getTransType().getTransName(),
                            TransResultUtils.getMessage(TransResult.ERR_TRANS_NOW_ALLOW),
                            dialogInterface -> finish(new ActionResult(TransResult.ERR_TRANS_NOW_ALLOW, null)),
                            Constants.FAILED_DIALOG_SHOW_TIME);
                    return;
                }

                SystemClock.sleep(500);
                acquirer = FinancialApplication.getAcqManager().getCurAcq();
                if (acquirer.isEnableUpi() && !acquirer.isTestMode()) {
                    if (acquirer.getTMK() != null && acquirer.getTWK() == null) {
                        processUPLogon(TransContext.getInstance().getCurrentAction());
                        cv.close();
                        cv.block();
                    }
                }

                if (acquirer.isEnableUpi() && !acquirer.isTestMode()) {
                    if (acquirer.getTMK() == null || acquirer.getTWK() == null) {
                        FinancialApplication.getApp().runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                Device.beepErr();
                                promptDialog = new CustomAlertDialog(EnterPinActivity.this, CustomAlertDialog.ERROR_TYPE);
                                promptDialog.setTimeout(3);
                                promptDialog.setContentText(getString(R.string.err_upi_not_logon));
                                promptDialog.show();
                                promptDialog.showConfirmButton(true);
                                promptDialog.setOnDismissListener(new OnDismissListener() {

                                    @Override
                                    public void onDismiss(DialogInterface arg0) {
                                        finish(new ActionResult(TransResult.ERR_ABORTED, null));
                                    }
                                });
                            }
                        });
                        return;
                    }
                }

                enterOnlinePin(panBlock, supportBypass);
            } else if (enterPinType == EEnterPinType.OFFLINE_CIPHER_PIN) {
                enterOfflineCipherPin();
            } else if (enterPinType == EEnterPinType.OFFLINE_PLAIN_PIN) {
                enterOfflinePlainPin();
            } else if (enterPinType == EEnterPinType.OFFLINE_PCI_MODE) {
                enterOfflinePCIMode();
            }
        }

        private void enterOnlinePin(final String panBlock, final boolean supportBypass) {
            transData.setOnlinePin(true);
            FinancialApplication.getApp().runInBackground(new OnlinePinRunnable(panBlock, supportBypass));
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_input_pin;
    }

    @Override
    protected void loadParam() {
        title = getIntent().getStringExtra(EUIParamKeys.NAV_TITLE.toString());
        prompt1 = getIntent().getStringExtra(EUIParamKeys.PROMPT_1.toString());
        prompt2 = getIntent().getStringExtra(EUIParamKeys.PROMPT_2.toString());
        totalAmount = getIntent().getStringExtra(EUIParamKeys.TRANS_AMOUNT.toString());
        tipAmount = getIntent().getStringExtra(EUIParamKeys.TIP_AMOUNT.toString());

        enterPinType = (EEnterPinType) getIntent().getSerializableExtra(EUIParamKeys.ENTERPINTYPE.toString());
        if (enterPinType == EEnterPinType.ONLINE_PIN) {
            panBlock = getIntent().getStringExtra(EUIParamKeys.PANBLOCK.toString());
            supportBypass = getIntent().getBooleanExtra(EUIParamKeys.SUPPORTBYPASS.toString(), false);
        } else {
            rsaPinKey = getIntent().getParcelableExtra(EUIParamKeys.RSA_PIN_KEY.toString());
        }
        transData = Component.getTransDataInstance();
    }

    @Override
    protected String getTitleString() {
        return title;
    }

    @Override
    protected void initViews() {
        landscape = !ViewUtils.isScreenOrientationPortrait(EnterPinActivity.this);
        supportSP200 = false;

        enableBackAction(true);
        //enableActionBar(false);

        TextView totalAmountTv = (TextView) findViewById(R.id.total_amount_txt);
        LinearLayout totalAmountLayout = (LinearLayout) findViewById(R.id.trans_total_amount_layout);
        if (totalAmount != null && !totalAmount.isEmpty()) {
            totalAmount = CurrencyConverter.convert(Utils.parseLongSafe(totalAmount, 0));
            totalAmountTv.setText(totalAmount);
        } else {
            totalAmountLayout.setVisibility(View.INVISIBLE);
        }

        TextView tipAmountTv = (TextView) findViewById(R.id.tip_amount_txt);
        LinearLayout tipAmountLayout = (LinearLayout) findViewById(R.id.trans_tip_amount_layout);
        if (tipAmount != null && !tipAmount.isEmpty() && Utils.parseLongSafe(tipAmount, 0) > 0) {
            tipAmount = CurrencyConverter.convert(Utils.parseLongSafe(tipAmount, 0));
            tipAmountTv.setText(tipAmount);
        } else {
            tipAmountLayout.setVisibility(View.INVISIBLE);
        }

        TextView promptTv1 = (TextView) findViewById(R.id.prompt_title);
        promptTv1.setText(prompt1);

        TextView promptTv2 = (TextView) findViewById(R.id.prompt_no_pin);
        if (prompt2 != null) {
            promptTv2.setText(prompt2);
        } else {
            promptTv2.setVisibility(View.INVISIBLE);
        }

        pwdTv = (TextView) findViewById(R.id.pin_input_text);
    }

    @Override
    protected void setListeners() {
        //do nothing
    }

    public void setContentText(final String content) {
        FinancialApplication.getApp().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (pwdTv != null) {
                    pwdTv.setText(content);
                    pwdTv.setTextSize(FinancialApplication.getApp().getResources().getDimension(R.dimen.font_size_key));
                }
            }
        });
    }

    public void enterOfflineCipherPin() {
        FinancialApplication.getApp().runInBackground(new OfflineCipherPinRunnable());
    }

    public void enterOfflinePlainPin() {
        FinancialApplication.getApp().runInBackground(new OfflinePlainPinRunnable());
    }

    public void enterOfflinePCIMode() {
        byte[] pinData;
        supportSP200 = false;
        SP200_serialAPI.getInstance().setOfflinePin(false);
        SP200_serialAPI.getInstance().setSp200Cancel(false);
        SP200_serialAPI.getInstance().setNopinInput(false);

        if (isSP200Enable) {
            supportSP200 = true;
            pinData = SP200_serialAPI.getInstance().enterOfflinePin();

            if (TransContext.getInstance().getCurrentAction() == null
                    || TransContext.getInstance().getCurrentAction().isFinished()) {
                return;
            }

            if (pinData != null && pinData[0] == -1) {
                FinancialApplication.getApp().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView pinpadwarn = (TextView) findViewById(R.id.pinpad_warn);
                        pinpadwarn.setVisibility(View.VISIBLE);
                    }
                });
                //pinpadwarn.setText(getString(R.string.err_pinpad_not_response));
                supportSP200 = false;
            } else {
                //APDU request. CLA(1B) + INS(1B) + P1(1B) + P2(1B) + Lc(0, 1 or 2B) + data(Lc) + Le(0, 1 or 2B)
                //createReq(byte cla, byte ins, byte p1, byte p2, byte[] data, short le);
                //stApduSend.Lc = iPinLen;
                //stApduSend.Le = 0;
                //byte[] cmd = new byte[] {0x00, 0x20, 0x00, (byte)0x80, 0x08, 0x24, 0x12, 0x34, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x00};
                IApdu apdu = Packer.getInstance().getApdu();
                //byte[] data = new byte[] {0x04, 0x12, 0x34, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF};

                SP200_serialAPI.getInstance().setOfflinePin(true);
                SP200_serialAPI.getInstance().setNopinInput(false);

                if (pinData[0] == 0x20){ // ByPass
                    supportSP200 = false;
                    SP200_serialAPI.getInstance().setNopinInput(true);
                    FinancialApplication.getApp().doEvent(new EmvCallbackEvent(EmvCallbackEvent.Status.OFFLINE_PIN_ENTER_READY));
                    finish(new ActionResult(TransResult.SUCC, null));
                    return;
                }

                IApdu.IApduReq apduReq = apdu.createReq((byte)0x00, (byte)0x20, (byte)0x00, (byte)0x80, pinData);
                apduReq.setLeNotPresent();
                byte[] resp = isoCommand(ICC_SLOT, apduReq.pack());
                OfflinePinResult offlinePinResult = new OfflinePinResult();
                offlinePinResult.setRespOut(resp);
                SP200_serialAPI.getInstance().setIccRespOut(resp);

                if (resp != null && (resp[0] == (byte) 0x90 && resp[1] == (byte) 0x00)) {
                    offlinePinResult.setRet(EEmvExceptions.EMV_OK.getErrCodeFromBasement());
                    FinancialApplication.getApp().doEvent(new EmvCallbackEvent(EmvCallbackEvent.Status.OFFLINE_PIN_ENTER_READY));
                    finish(new ActionResult(TransResult.SUCC, offlinePinResult));
                    return;
                } else {
                    //offlinePinResult.setRet(EEmvExceptions.EMV_ERR_NO_PASSWORD.getErrCodeFromBasement());
                    FinancialApplication.getApp().doEvent(new EmvCallbackEvent(EmvCallbackEvent.Status.OFFLINE_PIN_ENTER_READY));
                    //finish(new ActionResult(TransResult.ERR_PASSWORD, offlinePinResult));
                }
            }
        }

        if(!supportSP200){
            try {
                ped.setIntervalTime(1, 1);
                ped.setInputPinListener(inputPCIPinListener);
                FinancialApplication.getApp().doEvent(new EmvCallbackEvent(EmvCallbackEvent.Status.OFFLINE_PIN_ENTER_READY));
            } catch (PedDevException e) {
                Log.e(TAG, "", e);
                OfflinePinResult offlinePinResult = new OfflinePinResult();
                offlinePinResult.setRet(e.getErrCode());
                finish(new ActionResult(TransResult.ERR_ABORTED, offlinePinResult));
            }
        }
    }

    private final IIcc icc = FinancialApplication.getDal().getIcc();
    public byte[] isoCommand(byte slot, byte[] send) {
        try {
            //byte[] initRes = icc.init(slot);
            byte[] resp = icc.isoCommand(slot, send);
            Log.i("isoCommand","isoCommand");
            return resp;
        } catch (IccDevException e) {
            e.printStackTrace();
            Log.e("isoCommand",e.toString());
            return null;
        }
    }

    private class OnlinePinRunnable implements Runnable {
        private final String onlinePanBlock;
        private final boolean isSupportBypass;

        OnlinePinRunnable(final String panBlock, final boolean supportBypass) {
            this.onlinePanBlock = panBlock;
            this.isSupportBypass = supportBypass;
            ped.setInputPinListener(pedInputPinListener);
        }

        @Override
        public void run() {
            try {
                ped.setIntervalTime(1, 1);
                byte[] pinData = null;
                boolean pinBypass = false;
                Acquirer acq = FinancialApplication.getAcqManager().getCurAcq();

                supportSP200 = false;
                if (acq.isEnableUpi()) {
                    if (acq.getTMK() == null || acq.getTWK() == null)
                    {
                        finish(new ActionResult(TransResult.ERR_UPI_NOT_LOGON, null));
                        return;
                    }

                    if (isSP200Enable) {
                        supportSP200 = true;
                        pinData = SP200_serialAPI.getInstance().enterUpiPin(onlinePanBlock.getBytes(), pinBypass);
                        if (pinData != null && pinData[0] == -1){
                            FinancialApplication.getApp().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    TextView pinpadwarn = (TextView) findViewById(R.id.pinpad_warn);
                                    pinpadwarn.setVisibility(View.VISIBLE);
                                }
                            });
                            supportSP200 = false;
                        }
                    }

                    if (!supportSP200) {
                        pinData = Device.getUpiPinBlock(onlinePanBlock, isSupportBypass, landscape,
                                activityTimeoutSec - 5);
                    }
                } else {
                    supportSP200 = false;
                    if (isSP200Enable) {
                        supportSP200 = true;
                        pinData = SP200_serialAPI.getInstance().enterPin(onlinePanBlock.getBytes());
                        if (pinData != null && pinData[0] == -1){
                            FinancialApplication.getApp().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    TextView pinpadwarn = (TextView) findViewById(R.id.pinpad_warn);
                                    pinpadwarn.setVisibility(View.VISIBLE);
                                }
                            });
                            supportSP200 = false;
                        }
                    }

                    if (!supportSP200) {
                        pinData = Device.getPinBlock(onlinePanBlock, isSupportBypass, landscape,
                                                   activityTimeoutSec - 5);
                    }
                }
                if (pinData == null || pinData.length == 0)
                    finish(new ActionResult(TransResult.SUCC, null));
                else {
                    finish(new ActionResult(TransResult.SUCC, Utils.bcd2Str(pinData)));
                }
            } catch (final PedDevException e) {
                Log.e(TAG, "", e);
                handleException(e);
            } finally {
                //no memory leak
                ped.setInputPinListener(null);
            }
        }

        private void handleException(final PedDevException e) {
            if (e.getErrCode() == EPedDevException.PED_ERR_INPUT_CANCEL.getErrCodeFromBasement()) {
                finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
            } else if (e.getErrCode() == EPedDevException.PED_ERR_INPUT_TIMEOUT.getErrCodeFromBasement()){
                finish(new ActionResult(TransResult.ERR_TIMEOUT, null));
            } else {
                FinancialApplication.getApp().runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                    Device.beepErr();
                    promptDialog = new CustomAlertDialog(EnterPinActivity.this, CustomAlertDialog.ERROR_TYPE);
                    promptDialog.setTimeout(3);
                    if (e.getErrCode() == 1)
                    {
                        promptDialog.setContentText(getString(R.string.err_upi_not_logon));
                    }
                    else
                    {
                        promptDialog.setContentText(e.getErrMsg());
                    }
                    promptDialog.show();
                    promptDialog.showConfirmButton(true);
                    promptDialog.setOnDismissListener(new OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface arg0) {
                            finish(new ActionResult(TransResult.ERR_ABORTED, null));
                        }
                    });
                    }
                });
            }
        }
    }

    private abstract class OfflinePinRunnable implements Runnable {
        OfflinePinRunnable() {
            ped.setInputPinListener(pedInputPinListener);
        }

        @Override
        public void run() {
            try {
                ped.setIntervalTime(1, 1);
                ped.setKeyboardLayoutLandscape(landscape);
                byte[] resp = callPed();
                OfflinePinResult offlinePinResult = new OfflinePinResult();
                offlinePinResult.setRet(EEmvExceptions.EMV_OK.getErrCodeFromBasement());
                offlinePinResult.setRespOut(resp);
                finish(new ActionResult(TransResult.SUCC, offlinePinResult));
            } catch (PedDevException e) {
                Log.e(TAG, "", e);
                handleException(e);
            } finally {
                //no memory leak
                ped.setInputPinListener(null);
            }
        }

        private void handleException(PedDevException e) {
            if (e.getErrCode() == EPedDevException.PED_ERR_INPUT_CANCEL.getErrCodeFromBasement()) {
                finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
            } else {
                OfflinePinResult offlinePinResult = new OfflinePinResult();
                offlinePinResult.setRet(e.getErrCode());
                finish(new ActionResult(TransResult.ERR_ABORTED, offlinePinResult));
            }
        }

        abstract byte[] callPed() throws PedDevException;
    }

    private class OfflineCipherPinRunnable extends OfflinePinRunnable {
        @Override
        byte[] callPed() throws PedDevException {
            return ped.verifyCipherPin(ICC_SLOT, OFFLINE_EXP_PIN_LEN, rsaPinKey, (byte) 0x00, 60 * 1000);
        }
    }

    private class OfflinePlainPinRunnable extends OfflinePinRunnable {
        @Override
        byte[] callPed() throws PedDevException {
            return ped.verifyPlainPin(ICC_SLOT, OFFLINE_EXP_PIN_LEN, (byte) 0x00, 60 * 1000);
        }
    }

    @Override
    protected boolean onKeyBackDown() {
        finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
        return true;
    }


    @Override
    public void finish(ActionResult result){
        if(supportSP200){
            SP200_serialAPI.getInstance().BreakReceiveThread();
            SP200_serialAPI.getInstance().setSp200Cancel(true);
            FinancialApplication.getApp().runOnUiThreadDelay(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, " cancelSP200");
                    SP200_serialAPI.getInstance().cancelSP200() ;
                }
            }, 1500);
        }

        supportSP200 = false;
        super.finish(result);
    }

}
