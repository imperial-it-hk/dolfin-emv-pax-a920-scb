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
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.record.Printer;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.pack.qr.PackQr;
import com.pax.settings.SysParam;

import java.util.Hashtable;

/**
 * Created by SORAYA S on 30-Jan-18.
 */

public class ShowQRCodeActivity extends BasePromptPayShowQrActivity {

    private ImageView imageView;
    private Button okQrButton;
    private Button cancelQrButton;

    private String title;
    private int typeQR;
    private String terminalId;
    private String merchantId;
    private String billerId;
    private String amount;
    private String datetime;
    private String billerServiceCode;
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
        Bitmap bitmap = textToImageEncode(350, 350);
        Bitmap bitmapLogo = BitmapFactory.decodeResource(getResources(), R.drawable.thai_qr_logo);
        bitmapQr = this.mergeBitmaps(bitmapLogo, bitmap);
        imageView.setImageBitmap(bitmapQr);

        if(bitmap != null){
            Component.incTraceNo(null);
        }

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
                finish(new ActionResult(TransResult.SUCC, packQr.getQrRef2()));
                break;
            case R.id.cancel_qr_button:
                finish(new ActionResult(TransResult.ERR_USER_CANCEL, packQr.getQrRef2()));
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
        typeQR = getIntent().getIntExtra(getString(R.string.qr_type), 0);
        terminalId = getIntent().getStringExtra(getString(R.string.qr_trans_tid));
        merchantId = getIntent().getStringExtra(getString(R.string.qr_trans_mid));
        billerId = getIntent().getStringExtra(getString(R.string.qr_trans_billerId));
        billerServiceCode = getIntent().getStringExtra(getString(R.string.qr_trans_biller_service_code));
        amount = getIntent().getStringExtra(getString(R.string.qr_trans_amt));
        datetime = getIntent().getStringExtra(getString(R.string.qr_trans_datetime));
        acqName = getIntent().getStringExtra(getString(R.string.acquirer));
        String traceNo = Component.getPaddedNumber(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO), 6);
        packQr = new PackQr(typeQR, billerId, terminalId, merchantId, amount, datetime, traceNo);
    }

    @Override
    public boolean onOptionsItemSelectedSub(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish(new ActionResult(TransResult.ERR_USER_CANCEL, packQr.getQrRef2()));
                return true;
            case R.id.print_qr_menu:
                doPrintQrSlip(terminalId, merchantId, amount, datetime, packQr.getQrRef2(), bitmapQr, billerServiceCode, acqName);
                return true;
            default:
                return super.onOptionsItemSelectedSub(item);
        }
    }

    @Override
    protected boolean onKeyBackDown() {
        finish(new ActionResult(TransResult.ERR_USER_CANCEL, packQr.getQrRef2()));
        return true;
    }

    private Bitmap textToImageEncode(int width, int height){

        String formatEncode = packQr.packQr();

        if((billerId == null || "".equals(billerId)) && (terminalId == null || "".equals(terminalId)) &&
                (merchantId == null || "".equals(merchantId)) && (amount == null || "".equals(amount))){
            return null;
        }

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
                Printer.printQrSlip(ShowQRCodeActivity.this, termId, merId, amt, datetime, qrRef2, bitmapQr,billerServiceCode,acqName);
            }
        });
    }
}
