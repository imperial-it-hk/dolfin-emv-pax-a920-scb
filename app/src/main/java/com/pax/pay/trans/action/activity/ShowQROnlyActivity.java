package com.pax.pay.trans.action.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.record.Printer;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.transmit.TransProcessListener;
import com.pax.pay.trans.transmit.TransProcessListenerImpl;
import com.pax.pay.trans.transmit.Transmit;
import com.pax.pay.utils.Utils;

import java.util.Hashtable;

/**
 * Created by WITSUTA A on 11/23/2018.
 */

public class ShowQROnlyActivity extends BasePromptPayShowQrActivity {

    private ImageView imageView;
    private Button okQrButton;
    private Button cancelQrButton;

    private String title;
    private String strQrInfo;

    private String terminalId;
    private String merchantId;
    private String amount;
    private String datetime;
    private String billerServiceCode;
    private String qrID;
    private String acqName;

    private Bitmap bitmapQr;

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
        imageView = (ImageView) findViewById(R.id.imageView);
        bitmapQr = generateQR();
        imageView.setImageBitmap(bitmapQr);

        okQrButton = (Button) findViewById(R.id.ok_qr_button);
        cancelQrButton = (Button) findViewById(R.id.cancel_qr_button);
    }

    @Override
    protected void setListeners() {
        okQrButton.setOnClickListener(this);
        cancelQrButton.setOnClickListener(this);
    }

    @Override
    protected void onClickProtected(View v) {
        switch (v.getId()) {
            case R.id.ok_qr_button:
                finish(new ActionResult(TransResult.SUCC, null));
                break;
            case R.id.cancel_qr_button:
                finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.printqr_action, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void loadParam() {
        title = getIntent().getStringExtra(EUIParamKeys.NAV_TITLE.toString());
        strQrInfo = getIntent().getStringExtra(Utils.getString(R.string.qr_code_info));
        qrID = getIntent().getStringExtra(Utils.getString(R.string.qr_id));
        terminalId = getIntent().getStringExtra(getString(R.string.qr_trans_tid));
        merchantId = getIntent().getStringExtra(getString(R.string.qr_trans_mid));
        billerServiceCode = getIntent().getStringExtra(getString(R.string.qr_trans_biller_service_code));
        amount = getIntent().getStringExtra(getString(R.string.qr_trans_amt));
        datetime = getIntent().getStringExtra(getString(R.string.qr_trans_datetime));
        acqName = getIntent().getStringExtra(getString(R.string.acquirer));
    }

    @Override
    public boolean onOptionsItemSelectedSub(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
                return true;
            case R.id.print_qr_menu:
                doPrintQrSlip(terminalId, merchantId, amount, datetime, qrID, bitmapQr, billerServiceCode, acqName);
                return true;
            default:
                return super.onOptionsItemSelectedSub(item);
        }
    }

    @Override
    protected boolean onKeyBackDown() {
        finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
        return true;
    }

    private Bitmap textToImageEncode(int width, int height){

        String formatEncode = strQrInfo;

        Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");

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

        int centreX = (canvasWidth  - overlay.getWidth()) /2;
        int centreY = (canvasHeight - overlay.getHeight()) /2 ;
        canvas.drawBitmap(overlay, centreX, centreY, null);

        return combined;
    }

    private void doPrintQrSlip(final String termId, final String merId, final String amt, final String datetime, final String qrRef2,
                               final Bitmap bitmapQr, final String billerServiceCode, final String acqName) {
        FinancialApplication.getApp().runInBackground(new Runnable() {
            @Override
            public void run() {
                Printer.printQrSlip(ShowQROnlyActivity.this, termId, merId, amt, datetime, qrRef2, bitmapQr, billerServiceCode, acqName);
            }
        });
    }

    @Override
    protected void onTimerFinish() {
        finish(new ActionResult(TransResult.ERR_TIMEOUT, null));
    }

    private Bitmap generateQR() {
        Bitmap bitmap = textToImageEncode(350, 350);
        if (Constants.ACQ_QRC.equals(acqName)) {
            Bitmap bitmapLogo = BitmapFactory.decodeResource(getResources(), R.drawable.thai_qr_logo);
            bitmap = this.mergeBitmaps(bitmapLogo, bitmap);
        }
        return bitmap;
    }
}