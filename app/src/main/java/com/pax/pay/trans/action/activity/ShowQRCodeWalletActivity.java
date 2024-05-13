package com.pax.pay.trans.action.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.abl.utils.EncUtils;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.BaseActivityWithTickForAction;
import com.pax.pay.ECR.EcrData;
import com.pax.pay.ECR.EcrProcessClass;
import com.pax.pay.ECR.HyperComMsg;
import com.pax.pay.ECR.LawsonHyperCommClass;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.action.ActionInputPassword;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.uart.SP200_serialAPI;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.ToastUtils;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;
import com.pax.view.dialog.DialogUtils;

import java.util.Arrays;
import java.util.Hashtable;

import th.co.bkkps.utils.Log;

/**
 * Created by NANNAPHAT S on 12-Nov-18.
 */

public class ShowQRCodeWalletActivity extends BaseActivityWithTickForAction {

    private ImageView imageView;
    private ImageView paymentLogo;
    private Button okQrButton;
    private Button cancelQrButton;
    private Button verifyPaySlipButton;
    private LinearLayout verifyPaySlipLayout;
    private TextView textBaseAmount;
    private TextView textScanQr;
    private TextView textQrScanTips;
    private String amount;
    private String qrType;

    private String title;
    private String packQr;
    private Bitmap bitmapQr;

    private String walletRetryStatus;
    private boolean isRetryCheck;
    private boolean isWrongCancelPassword;
    private boolean isPosManualInquiry = false;
    private boolean isButtonVisibleOff = false;
    private ActionInputPassword inputPasswordAction = null;
    private final AAction currentAction = TransContext.getInstance().getCurrentAction();

    private boolean supportSP200 = false;
    private boolean isSP200run = false;
    private boolean isOkButtonPressed = false;
    private boolean isEcrProcess = false;

    private boolean isShowVerifyQRBtn = false ;

    @Override
    protected void onResume() {
        super.onResume();
        okQrButton.requestFocus();
        tickTimer.stop();

        if (!onExitActivity && isPosManualInquiry) {
            waitConsequentCommand();
        }
    }

    private void enableVerifyQRPaySlipButton() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                verifyPaySlipLayout.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_show_qr_code;
    }

    @Override
    protected String getTitleString() {
        return title;
    }

    @Override
    protected void initViews() {
//        tickTimer.start(300);
        imageView = (ImageView) findViewById(R.id.imageView);
        paymentLogo = (ImageView) findViewById(R.id.payment_logo);

        textScanQr = (TextView) findViewById(R.id.please_scan_qr);
        if (isRetryCheck) {
            textScanQr.setText(R.string.show_qr_text);
        }

        textQrScanTips = (TextView) findViewById(R.id.prompt_after_scan);
        if (textQrScanTips!=null  && isPosManualInquiry) {
            textQrScanTips.setText(R.string.pls_ask_cashier_for_qr_inquiry);
        } else {
            textQrScanTips.setText(R.string.pls_press_ok);
        }

        enableBackAction(false);

        Bitmap bitmap = textToImageEncode(500, 500);
        Bitmap bitmapLogo = BitmapFactory.decodeResource(getResources(), R.drawable.thai_qr_logo2);

        Bitmap tranLogo = null;

        if (title.equals(getString(R.string.menu_qr_thai_qr))) {
            bitmapQr = this.mergeBitmaps(bitmapLogo, bitmap);
            tranLogo = BitmapFactory.decodeResource(getResources(), R.drawable.promptpay_logo);

            if (SP200_serialAPI.getInstance().isSp200Enable() && SP200_serialAPI.getInstance().getiSp200Mode() > 0) {

                FinancialApplication.getApp().runInBackground(new Runnable() {

                    byte[] szMsgResult = new byte[200];
                    SP200_serialAPI sp200API = SP200_serialAPI.getInstance();

                    @Override
                    public void run() {
                        if (sp200API.isQrRunning()) {
                            return;
                        }

                        int iRet = -1;
                        isSP200run = true;
                        iRet = SP200_serialAPI.getInstance().ShowQR(30000, packQr, true);
                        if (iRet != TransResult.SUCC) {
                            FinancialApplication.getApp().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ToastUtils.showMessage(getString(R.string.err_pinpad_not_response));
                                }
                            });
                            isSP200run = false;
                            return;
                        }

                        iRet = sp200API.GetSp200Result(1, szMsgResult);
                        if (iRet == TransResult.SUCC) {
                            sp200API.setQrRunning(false);
                            sp200API.BreakReceiveThread();
                            isSP200run = false;
                            onConfirmOk();
                        }
                    }
                });
            }

        } else {
            bitmapQr = bitmap;

            if (SP200_serialAPI.getInstance().isSp200Enable() && SP200_serialAPI.getInstance().getiSp200Mode() > 0) {

                FinancialApplication.getApp().runInBackground(new Runnable() {

                    byte[] szMsgResult = new byte[200];
                    SP200_serialAPI sp200API = SP200_serialAPI.getInstance();

                    @Override
                    public void run() {
                        if (sp200API.isQrRunning()) {
                            return;
                        }

                        int iRet = -1;
                        isSP200run = true;
                        iRet = SP200_serialAPI.getInstance().ShowQR(30000, packQr, false);
                        if (iRet != TransResult.SUCC) {
                            FinancialApplication.getApp().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ToastUtils.showMessage(getString(R.string.err_pinpad_not_response));
                                }
                            });
                            isSP200run = false;
                            return;
                        }

                        iRet = sp200API.GetSp200Result(1, szMsgResult);
                        if (iRet == TransResult.SUCC) {
                            sp200API.setQrRunning(false);
                            sp200API.BreakReceiveThread();
                            isSP200run = false;
                            onConfirmOk();
                        }
                    }
                });
            }
        }

        if (title.equals(getString(R.string.trans_alipay_sale))) {
            tranLogo = BitmapFactory.decodeResource(getResources(), R.drawable.alipay_logo);
        }

        if (title.equals(getString(R.string.trans_wechat_sale))) {
            tranLogo = BitmapFactory.decodeResource(getResources(), R.drawable.wechatpay_logo);
        }

        if (title.equals(getString(R.string.trans_qr_credit_sale))) {
            tranLogo = BitmapFactory.decodeResource(getResources(), R.drawable.qr_credit_visa_mastercard_unionpay);
        }

        imageView.setImageBitmap(bitmapQr);

        if (tranLogo != null) {
            paymentLogo.setImageBitmap(tranLogo);
        }


//        if(bitmap != null){
//            Component.incTraceNo(null);
//        }

        okQrButton = (Button) findViewById(R.id.ok_qr_button);
        okQrButton.setEnabled(true);
        cancelQrButton = (Button) findViewById(R.id.cancel_qr_button);


//        cancelQrButton.setVisibility(View.INVISIBLE);

        textBaseAmount = (TextView) findViewById(R.id.amount_text);
        textBaseAmount.setText(amount);

        //int maxInqCountShowVerifyQrBtn = FinancialApplication.getSysParam().get(SysParam.NumberParam.THAI_QR_INQUIRY_MAX_COUNT_FOR_SHOW_VERIFY_QR_BUTTON, 2);
        verifyPaySlipLayout = (LinearLayout) findViewById(R.id.qr_verify_pay_slip_layout);
        verifyPaySlipLayout.setVisibility((isShowVerifyQRBtn) ? View.VISIBLE : View.GONE);
        verifyPaySlipButton = (Button) findViewById(R.id.verify_pay_slip_button);

    }

    private GestureDetector gd = null;

    @Override
    protected void setListeners() {
        if (EcrData.instance.isOnProcessing
                && FinancialApplication.getEcrProcess() != null
                && FinancialApplication.getEcrProcess().mHyperComm != null
                && FinancialApplication.getEcrProcess().mHyperComm instanceof LawsonHyperCommClass) {
            return;
        }

        okQrButton.setOnClickListener(this);
        cancelQrButton.setOnClickListener(this);
        verifyPaySlipButton.setOnClickListener(this);
    }

    private boolean onExitActivity = false;

    private void waitConsequentCommand() {
        if (isEcrProcess && FinancialApplication.getEcrProcess().mHyperComm instanceof LawsonHyperCommClass) {
            if (isPosManualInquiry) {
                okQrButton.setVisibility(View.INVISIBLE);
                cancelQrButton.setVisibility(View.INVISIBLE);
                isButtonVisibleOff = true;
            }

            try {
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (onExitActivity) {
                            return;
                        }
                        EcrData.instance.isOnProcessing = false;
                        EcrProcessClass ecrprocess = FinancialApplication.getEcrProcess();

                        if (FinancialApplication.getEcrProcess().mCommManage != null) {
                            if (FinancialApplication.getEcrProcess().mCommManage.isStarted())
                                FinancialApplication.getEcrProcess().mCommManage.StopReceive();
                        }


                        Log.d(TAG, "AsyncTask --> Start while loop");
                        boolean runningFlag = false;
                        byte[] incomingData = null;
                        boolean keepDataMode = false;
                        boolean canRead = false;
                        byte[] rawData = null;
                        while (!runningFlag) {
                            incomingData = ecrprocess.mLinkPosComm.read_blocked(512, 600);
                            if (incomingData != null) {
                                Log.d(".Activity", "[LEN = " + incomingData.length + "] IncomingData = " + Utils.bcd2Str(incomingData));
                                if (incomingData.length > 0) {
                                    if (Arrays.equals(new byte[]{incomingData[0]}, new byte[]{0x02})) {
                                        rawData = new byte[0];
                                        keepDataMode = true;
                                    }
                                    if (keepDataMode) {
                                        rawData = byteAppends(rawData, incomingData);
                                    }

                                    if (rawData != null) {
                                        if (rawData.length >= 3) {
                                            if (Arrays.equals(new byte[]{rawData[rawData.length - 3], rawData[rawData.length - 2]}, new byte[]{0x1C, 0x03})) {
                                                keepDataMode = false;
                                                canRead = true;
                                            }
                                        }
                                    }


                                    if (canRead) {
                                        try {
                                            byte[] tmpTransCode = new byte[2];
                                            boolean respAck = false;
                                            System.arraycopy(rawData, 15, tmpTransCode, 0, 2);
                                            if (Arrays.equals(tmpTransCode, new byte[]{0x37, 0x31})) {           // QR Inquiry
                                                Device.beepPrompt();
                                                HyperComMsg.instance.setTransactionCode(tmpTransCode);
                                                FinancialApplication.getEcrProcess().mCommManage.MainIO.Write(new byte[]{0x06});
                                                onExitActivity = true;
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        onConfirmOk();
                                                        EcrData.instance.isOnProcessing = true;
                                                    }
                                                });
                                                runningFlag = true;
                                                rawData = null;
                                                keepDataMode = false;
                                                canRead = false;
                                                break;
                                            } else if (Arrays.equals(tmpTransCode, new byte[]{0x37, 0x32})) {    // QR Cancel
                                                //else {                                                     // All other command still use for cancel QR activity
                                                Device.beepPrompt();
                                                HyperComMsg.instance.setTransactionCode(tmpTransCode);
                                                FinancialApplication.getEcrProcess().mCommManage.MainIO.Write(new byte[]{0x06});
                                                onExitActivity = true;
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {

                                                        onComfirmCancel();
                                                        EcrData.instance.isOnProcessing = true;
                                                    }
                                                });
                                                runningFlag = true;
                                                rawData = null;
                                                keepDataMode = false;
                                                canRead = false;
                                                break;
                                            }
                                        } catch (Exception ex2) {
                                            ex2.printStackTrace();
                                            Log.d(TAG, "error during find command number");
                                        }
                                    }
                                }
                            }
                        }
                        Log.d(TAG, "AsyncTask --> exit while loop");
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Log.d(TAG, "Error on loop waiting for next command");
            }
        }
    }

    private byte[] byteAppends(byte[] srcByte, byte[] appByte) {
        if (srcByte != null && appByte != null) {
            byte[] newBuf = new byte[srcByte.length + appByte.length];
            if (srcByte.length > 0) {
                System.arraycopy(srcByte, 0, newBuf, 0, srcByte.length);
            }
            System.arraycopy(appByte, 0, newBuf, srcByte.length, appByte.length);
            return newBuf;
        }
        return srcByte;
    }

    @Override
    protected void onClickProtected(View v) {
        switch (v.getId()) {
            case R.id.ok_qr_button:
                onConfirmOk();
                break;
            case R.id.cancel_qr_button:
                runInputPwdAction();
                break;
            case R.id.verify_pay_slip_button:
                onVerifyQrPaySlip();
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.printqr_action, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void loadParam() {
        title = getIntent().getStringExtra(EUIParamKeys.NAV_TITLE.toString());
        packQr = getIntent().getStringExtra(getString(R.string.qr_data));
        amount = getIntent().getStringExtra(EUIParamKeys.TRANS_AMOUNT.toString());
        //qrType = getIntent().getStringExtra(EUIParamKeys.TRANS_AMOUNT.toString());
        walletRetryStatus = getIntent().getStringExtra(getString(R.string.qr_retry_status));
        isPosManualInquiry = getIntent().getBooleanExtra(getString(R.string.linkpos_ecr_pos_manual_inquiry), false);
        isEcrProcess = getIntent().getBooleanExtra(EUIParamKeys.ECR_PROCESS.toString(), false);

        isSP200run = false;

        if (amount != null && !amount.isEmpty()) {
            Long longAmount = Utils.parseLongSafe(amount, 0);
            amount = CurrencyConverter.convert(longAmount);
        }

        isRetryCheck = walletRetryStatus != null && !walletRetryStatus.isEmpty() && walletRetryStatus.equals(TransData.WalletRetryStatus.RETRY_CHECK.toString());

        isShowVerifyQRBtn= getIntent().getBooleanExtra(EUIParamKeys.QR_INQUIRY_COUNTER.toString(), false);
    }

    @Override
    public boolean onOptionsItemSelectedSub(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                //finish(new ActionResult(TransResult.ERR_USER_CANCEL, packQr.getQrRef2()));
                return true;
            case R.id.print_qr_menu:
                //doPrintQrSlip(terminalId, merchantId, amount, datetime, packQr.getQrRef2(), bitmapQr,billerServiceCode);
                return true;
            default:
                return super.onOptionsItemSelectedSub(item);
        }
    }

    private void runInputPwdAction() {
        inputPasswordAction = new ActionInputPassword(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionInputPassword) action).setParam(ShowQRCodeWalletActivity.this, 6,
                        getString(R.string.prompt_cancel_pwd), null, true);
                ((ActionInputPassword) action).setParam(TransResult.ERR_USER_CANCEL);
                inputPasswordAction.setFinished(false);
            }
        });

        inputPasswordAction.setEndListener(new AAction.ActionEndListener() {

            @Override
            public void onEnd(AAction action, ActionResult result) {
                if (result.getRet() != TransResult.SUCC) {
                    return;
                }

                String data = EncUtils.sha1((String) result.getData());
                if (!data.equals(FinancialApplication.getSysParam().get(SysParam.StringParam.SEC_CANCEL_PWD))) {
                    isWrongCancelPassword = isOkButtonPressed ? false : true;
                    DialogUtils.showErrMessage(ShowQRCodeWalletActivity.this, getString(R.string.pwd_cancel),
                            getString(R.string.err_password), null, Constants.FAILED_DIALOG_SHOW_TIME);
                    TransContext.getInstance().setCurrentAction(currentAction); //fix leaks
                    inputPasswordAction.setFinished(true);
                } else {
                    isWrongCancelPassword = false;
                    onComfirmCancel();
                }
            }
        });

        inputPasswordAction.execute();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (isEcrProcess && FinancialApplication.getEcrProcess().mHyperComm instanceof LawsonHyperCommClass) {
            return false;
        }
        if (event.getAction() == KeyEvent.ACTION_UP) {
            isOkButtonPressed = false;
            if (inputPasswordAction != null && !inputPasswordAction.isFinished()) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    isOkButtonPressed = true;
                    cancelQrButton.clearFocus();
                    okQrButton.requestFocus();
                    return super.dispatchKeyEvent(event);
                } else {
                    isWrongCancelPassword = true;
                    return super.dispatchKeyEvent(event);
                }
            }
            if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && !isWrongCancelPassword
                    && okQrButton.isEnabled()) {
                onConfirmOk();
                return false;
            } else if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                runInputPwdAction();
                return false;
            } else {
                isWrongCancelPassword = false;//set back default
                return super.dispatchKeyEvent(event);
            }
        } else {
            return super.dispatchKeyEvent(event);
        }
    }

    private void onConfirmOk() {

        //if (isSP200run) {
            SP200_serialAPI.getInstance().BreakReceiveThread();
            FinancialApplication.getApp().runOnUiThreadDelay(new Runnable() {
                @Override
                public void run() {
                    SP200_serialAPI.getInstance().cancelQR();
                }
            }, 200);
        //}
        okQrButton.setEnabled(false);
        TransContext.getInstance().setCurrentAction(currentAction);
        finish(new ActionResult(TransResult.SUCC, null));
    }

    private void onComfirmCancel() {

        //if (isSP200run || SP200_serialAPI.getInstance().isQrRunning()) {
            SP200_serialAPI.getInstance().BreakReceiveThread();
            FinancialApplication.getApp().runOnUiThreadDelay(new Runnable() {
                @Override
                public void run() {
                    isSP200run = false;
                    SP200_serialAPI.getInstance().cancelQR();
                }
            }, 200);
        //}

        TransContext.getInstance().setCurrentAction(currentAction);
        finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
    }

    private void onVerifyQrPaySlip() {
        //if (isSP200run || SP200_serialAPI.getInstance().isQrRunning()) {
            SP200_serialAPI.getInstance().cancelQR();
            SP200_serialAPI.getInstance().BreakReceiveThread();
        //}

        TransContext.getInstance().setCurrentAction(currentAction);
        finish(new ActionResult(TransResult.VERIFY_THAI_QR_PAY_RECEIPT_REQUIRED, null));
    }

    @Override
    protected boolean onKeyBackDown() {
        //finish(new ActionResult(TransResult.ERR_USER_CANCEL, packQr.getQrRef2()));
        runInputPwdAction();
        return true;
    }

    private Bitmap textToImageEncode(int width, int height) {

        String formatEncode = packQr;

        Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);

        try {
            BitMatrix bitMatrix = new QRCodeWriter().encode(formatEncode, BarcodeFormat.QR_CODE, width, height, hints);

            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (bitMatrix.get(x, y)) {
                        pixels[y * width + x] = 0xff000000;
                    } else {
                        pixels[y * width + x] = 0xffffffff;
                    }
//                    pixels[offset + x] = bitMatrix.get(x, y) ? ResourcesCompat.getColor(getResources(), 0xff000000, null) : 0xffffffff;
                }
            }

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
        }

        return null;
    }

    private Bitmap mergeBitmaps(Bitmap overlay, Bitmap bitmap) {

        int height = bitmap.getHeight();
        int width = bitmap.getWidth();

        Bitmap combined = Bitmap.createBitmap(width, height, bitmap.getConfig());
        Canvas canvas = new Canvas(combined);
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();

        canvas.drawBitmap(bitmap, new Matrix(), null);

        int centreX = (canvasWidth - overlay.getWidth()) / 2;
        int centreY = (canvasHeight - overlay.getHeight()) / 2;
        canvas.drawBitmap(overlay, centreX, centreY, null);

        return combined;
    }
/*
    private void doPrintQrSlip(final String termId, final String merId, final String amt, final String datetime, final String qrRef2, final Bitmap bitmapQr, final String billerServiceCode) {
        FinancialApplication.getApp().runInBackground(new Runnable() {
            @Override
            public void run() {
                Printer.printQrSlip(ShowQRCodeWalletActivity.this, termId, merId, amt, datetime, qrRef2, bitmapQr,billerServiceCode,false);
            }
        });
    }
*/

/*    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        super.onKeyDown(keyCode,event);
        if (keyCode == KeyEvent.KEYCODE_BACK){
            runInputPwdAction();
        } else if (keyCode == KeyEvent.KEYCODE_ENTER){
            onConfirmOk();
        }
        return true;
//        return false;
    }*/


}
