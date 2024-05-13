package com.pax.pay.trans.action.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.dal.ISys;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.BaseActivity;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.TransContext;
import com.pax.pay.uart.SP200_serialAPI;
import com.pax.pay.uart.Uart;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.Utils;

import java.io.ByteArrayOutputStream;

import th.co.bkkps.utils.Log;


public class GetSignatureActivity extends BaseActivity{
    Context context;
    private final String TAG = "GetSignatureActivity";
    private final int timeout = 10 * 60; //10 min
    private ISys iSys = null;
    private ProgressDialog progressDialog;
    private Thread threadHandleDevice;

    private String amount;

    private Button clearBtn;
    private Button confirmBtn;
    private RelativeLayout writeUserName = null;
    ImageView sigImage = null;

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(event.getAction() == KeyEvent.ACTION_UP){
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_BACK:
                    if(clearBtn.isShown()){
                        getSignatureFromSp200();
                    }
                    break;
                case KeyEvent.KEYCODE_ENTER:
                    if(confirmBtn.isShown() && confirmBtn.isEnabled()){
                        confirmBtn.setEnabled(false);
                        byte[] data = FinancialApplication.getGl().getImgProcessing().bitmapToJbig(bitmap, Constants.rgb2MonoAlgo);
                        finish(new ActionResult(TransResult.SUCC, data, null));
                    }
                    break;
                default:
                    break;
            }
            return false;
        }else{
            return false;
        }
    }

    @Override
    protected void setListeners() {
        clearBtn.setOnClickListener(this);
        confirmBtn.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void loadParam() {
        amount = getIntent().getStringExtra(EUIParamKeys.TRANS_AMOUNT.toString());
        amount = FinancialApplication.getConvert().stringPadding(amount, '0', 12, com.pax.glwrapper.convert.IConvert.EPaddingPosition.PADDING_LEFT);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_authgraph_layout;
    }


    @Override
    protected void initViews() {
        context = this;
        enableBackAction(false);

        ImageView imageView = (ImageView) findViewById(R.id.singpad);
        imageView.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.sig_pinpad));

        TextView amountText = (TextView) findViewById(R.id.trans_amount_tv);
        String amountTxt = CurrencyConverter.convert(Utils.parseLongSafe(amount, 0));
        amountText.setText(amountTxt);

        writeUserName = (RelativeLayout) findViewById(R.id.writeUserNameSpace);
        clearBtn = (Button) findViewById(R.id.clear_btn);
        confirmBtn = (Button) findViewById(R.id.confirm_btn);

        clearBtn.setVisibility(View.GONE);
        confirmBtn.setVisibility(View.GONE);

        RelativeLayout.LayoutParams imParams =
                new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);

        imParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        sigImage = new ImageView(context);
        //image.setImageBitmap(bitmap);
        //image.setId(2);
        writeUserName.addView(sigImage,imParams);

    }

    @Override
    protected String getTitleString() {
        return getString(R.string.trans_signature);
    }

    byte[] data = null;
    int iRet;
    private static ByteArrayOutputStream rxStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_authgraph_layout);
        getSignatureFromSp200();
       // SystemClock.sleep(1000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Device.enableBackKey(false);
    }

    @Override
    public void onClickProtected(View v) {

        switch (v.getId()) {
            case R.id.clear_btn:
                getSignatureFromSp200();
                break;
            case R.id.confirm_btn:
                byte[] data = FinancialApplication.getGl().getImgProcessing().bitmapToJbig(bitmap, Constants.rgb2MonoAlgo);
                finish(new ActionResult(TransResult.SUCC, data, null));
                break;
            default:
                break;
        }

    }

    private void getSignatureFromSp200(){

        rxStream = new ByteArrayOutputStream();

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                sigImage.setImageResource(android.R.color.transparent);

                clearBtn.setVisibility(View.GONE);
                confirmBtn.setVisibility(View.GONE);
            }
        });

        threadHandleDevice = new Thread(new Runnable() {

            @Override
            public void run() {



                SP200_serialAPI sp200API = SP200_serialAPI.getInstance();
                iRet = sp200API.GetSignature(amount);
                if (iRet != 0){
                    onGetSignatureDone(new ActionResult(TransResult.ERR_SP200_FAIL, null));
                    return;
                }

                data = null;

                Thread receiveThread = new ReceiveThread(sp200API, Thread::interrupt);

                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    // handle error
                }

                receiveThread.start();

                try {
                    receiveThread.join(); // timeout 5 min //remove timeout
                    receiveThread.interrupt();
                    receiveThread = null;

                } catch (Exception ex) {
                    ex.printStackTrace();
                    onGetSignatureDone(new ActionResult(TransResult.ERR_ABORTED, null));
                }

//                File myCaptureFile = new File("/sdcard/Download/sig.bmp");
//                try (FileOutputStream fos = new FileOutputStream(myCaptureFile)) {
//                    rxStream.writeTo(fos);
//                } catch (Exception e1) {
//                    Log.w("BSS", e1);
//                }

                bitmap = BitmapFactory.decodeByteArray(rxStream.toByteArray(), 0, rxStream.toByteArray().length);
                if (bitmap != null) {
                    bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight()/2, false);
                    onGetSignatureDone(new ActionResult(TransResult.SUCC, data));
                } else {
                    finish(new ActionResult(TransResult.SUCC, new byte[0]));
                }

            }
        });

        threadHandleDevice.start();
    }

    private class ReceiveThread extends Thread {
        private final Thread handleDevice;
        private final SP200_serialAPI sp200API;
        private final GetSignatureListener listener;

        public ReceiveThread(SP200_serialAPI sp200API, GetSignatureListener listener) {
            this.sp200API = sp200API;
            this.listener = listener;
            handleDevice = Thread.currentThread();
        }

        @Override
        public void run() {
            super.run();
            rxStream.reset();
            int PackageSize = 8192; // total size is 8192
            int len = 0;
            int wait = 0;

            boolean isDone = false;
            while (!Thread.interrupted()) {
                data = Uart.getInstance().recv(1);
                if (data != null && data.length >0 ) { // waitting for first byte
                    len = len + data.length;
                    rxStream.write(data, 0, data.length);
                    do{
                        while(len < PackageSize && !isDone){
                            Log.d(TAG, "len =" + len);
                            data = Uart.getInstance().recvNonBlocking();
                            if (data != null && data.length >0 ){
                                wait = 0;
                                len = len + data.length;
                                rxStream.write(data, 0, data.length);
                            } else {
                                Log.e(TAG, "no data");
                                wait++;
                                if(wait > 10)isDone = true;
                            }
                        }
                        SystemClock.sleep(100);
                        Uart.getInstance().send(new byte[]{6});
                        len = 0;
                        Log.d("get", "total =" + rxStream.size());

                    } while (rxStream.size() < 8192 && !isDone);

                    data = Uart.getInstance().recvNonBlocking();
                    listener.onReceiveSignatureData(handleDevice);
                    break;
                }
            }
        }
    }

    interface GetSignatureListener {
        void onReceiveSignatureData(Thread handleDeviceThread);
    }

    Bitmap bitmap;

    private void onGetSignatureDone(ActionResult result){
        if(result.getRet() == TransResult.SUCC){
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    sigImage.setImageBitmap(bitmap);
                }
            });

        }

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                SystemClock.sleep(3000);
                clearBtn.setVisibility(View.VISIBLE);
                confirmBtn.setVisibility(View.VISIBLE);
                confirmBtn.setEnabled(true);
                confirmBtn.requestFocus();
            }
        });
    }


    public void finish(ActionResult result) {
        AAction action = TransContext.getInstance().getCurrentAction();
        if (action != null) {
            if (action.isFinished())
                return;
            action.setFinished(true);
            quickClickProtection.start(); // AET-93
            action.setResult(result);
        } else {
            super.finish();
        }
    }

}

