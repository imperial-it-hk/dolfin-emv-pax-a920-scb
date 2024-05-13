package com.pax.pay.trans.receipt;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import th.co.bkkps.utils.Log;
import android.view.Gravity;

import com.pax.dal.entity.ETermInfoKey;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.glwrapper.imgprocessing.IImgProcessing;
import com.pax.glwrapper.page.IPage;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.TimeConverter;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Created by SORAYA S on 18-Apr-18.
 */

public class ReceiptGenerateQrSlip implements IReceiptGenerator {

    private String terminalId;
    private String merchantId;
    private String amount;
    private String datetime;
    private String qrRef2;
    private Bitmap bitmapQr;
    private String billerServiceCode;
    private boolean visaQr;


    ReceiptGenerateQrSlip(String terminalId, String merchantId, String amount, String datetime, String qrRef2, Bitmap bitmapQr, String billerServiceCode) {
        this.terminalId = terminalId;
        this.merchantId = merchantId;
        this.amount = amount;
        this.datetime = datetime;
        this.qrRef2 = qrRef2;
        this.bitmapQr = bitmapQr;
        this.billerServiceCode = billerServiceCode;
    }

    @Override
    public Bitmap generateBitmap() {
        SysParam sysParam = FinancialApplication.getSysParam();

        IPage page = Device.generatePage();

        /*Header*/
        page.addLine()
                .addUnit(page.createUnit()
                        .setBitmap(getImageFromAssetsFile("thai_qr_payment_logo.png"))
                        .setGravity(Gravity.CENTER));

        /*Body*/
        // Qr code
        page.addLine()
                .addUnit(page.createUnit()
                        .setBitmap(bitmapQr)
                        .setGravity(Gravity.CENTER));


        //Biller Service Code
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(billerServiceCode)
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.CENTER));

        //merchant name
        String merName = sysParam.get(SysParam.StringParam.EDC_MERCHANT_NAME_EN);
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(merName)
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.CENTER));

        //TID
        page.addLine()
                .adjustTopSpace(10)
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_terminal_code_short_sharp) + terminalId)
                        .setFontSize(FONT_SMALL));
        //MER
        page.addLine()
                .adjustTopSpace(3)
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_ref1_merchant_code) + merchantId)
                        .setFontSize(FONT_SMALL));

        // date/time
        String formattedDate = TimeConverter.convert(datetime, Constants.TIME_PATTERN_TRANS,
                Constants.DATE_PATTERN_DISPLAY);

        String formattedTime = TimeConverter.convert(datetime, Constants.TIME_PATTERN_TRANS,
                Constants.TIME_PATTERN_DISPLAY4);

        page.addLine()
                .adjustTopSpace(3)
                .addUnit(page.createUnit()
                        .setText(formattedDate)
                        .setFontSize(FONT_SMALL))
                .addUnit(page.createUnit()
                        .setText(formattedTime)
                        .setFontSize(FONT_SMALL)
                        .setGravity(Gravity.END));

        String title = Utils.getString(R.string.receipt_ref2_colon);
        //Ref2
        page.addLine()
                .adjustTopSpace(3)
                .addUnit(page.createUnit()
                        .setText(title + this.formatQrRef2())
                        .setFontSize(FONT_SMALL)
                        .setGravity(Gravity.RIGHT));

        //AMT
        long amt = Utils.parseLongSafe(amount, 0);
        String temp = CurrencyConverter.convert(amt, CurrencyConverter.getDefCurrency());
        page.addLine()
                .adjustTopSpace(10)
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_amount_short))
                        .setFontSize(FONT_BIG)
                        .setWeight(4.0f))
                .addUnit(page.createUnit()
                        .setText(temp)
                        .setFontSize(FONT_BIG)
                        .setGravity(Gravity.END)
                        .setWeight(9.0f));
        //////

        /*Footer*/
        page.addLine()
                .adjustTopSpace(3)
                .addUnit(page.createUnit()
                        .setBitmap(getImageFromAssetsFile("logo_bbl_th_black.jpg"))
                        .setGravity(Gravity.CENTER));

        page.addLine().addUnit(page.createUnit().setText("\n\n\n\n"));

        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }

    public Bitmap generateVisaQRBitmap() {
        SysParam sysParam = FinancialApplication.getSysParam();

        IPage page = Device.generatePage();

        /*Header*/
        page.addLine()
                .addUnit(page.createUnit()
                        .setBitmap(getImageFromAssetsFile("thai_qr_payment_logo.png"))
                        .setGravity(Gravity.CENTER));
        page.addLine()
                .addUnit(page.createUnit()
                        .setBitmap(getImageFromAssetsFile("qr-visa.png"))
                        .setGravity(Gravity.CENTER));

        /*Body*/
        // Qr code
        page.addLine()
                .addUnit(page.createUnit()
                        .setBitmap(bitmapQr)
                        .setGravity(Gravity.CENTER));


        //Biller Service Code
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(billerServiceCode)
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.CENTER));

        //merchant name
        String merName = sysParam.get(SysParam.StringParam.EDC_MERCHANT_NAME_EN);
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(merName)
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.CENTER));

        //TID
        page.addLine()
                .adjustTopSpace(10)
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_terminal_code_short_sharp) + terminalId)
                        .setFontSize(FONT_SMALL));
        //MER
        page.addLine()
                .adjustTopSpace(3)
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_ref1_merchant_code) + merchantId)
                        .setFontSize(FONT_SMALL));

        // date/time
        String formattedDate = TimeConverter.convert(datetime, Constants.TIME_PATTERN_TRANS,
                Constants.DATE_PATTERN_DISPLAY);

        String formattedTime = TimeConverter.convert(datetime, Constants.TIME_PATTERN_TRANS,
                Constants.TIME_PATTERN_DISPLAY4);

        page.addLine()
                .adjustTopSpace(3)
                .addUnit(page.createUnit()
                        .setText(formattedDate)
                        .setFontSize(FONT_SMALL))
                .addUnit(page.createUnit()
                        .setText(formattedTime)
                        .setFontSize(FONT_SMALL)
                        .setGravity(Gravity.END));

        String title = Utils.getString(R.string.receipt_qr_id_colon);

        //Ref2
        page.addLine()
                .adjustTopSpace(3)
                .addUnit(page.createUnit()
                        .setText(title + this.formatQrRef2())
                        .setFontSize(FONT_SMALL)
                        .setGravity(Gravity.RIGHT));

        //AMT
        long amt = Utils.parseLongSafe(amount, 0);
        String temp = CurrencyConverter.convert(amt, CurrencyConverter.getDefCurrency());
        page.addLine()
                .adjustTopSpace(10)
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_amount_short))
                        .setFontSize(FONT_BIG)
                        .setWeight(4.0f))
                .addUnit(page.createUnit()
                        .setText(temp)
                        .setFontSize(FONT_BIG)
                        .setGravity(Gravity.END)
                        .setWeight(9.0f));
        //////

        /*Footer*/
        page.addLine()
                .adjustTopSpace(3)
                .addUnit(page.createUnit()
                        .setBitmap(getImageFromAssetsFile("logo_bbl_th_black.jpg"))
                        .setGravity(Gravity.CENTER));

        page.addLine().addUnit(page.createUnit().setText("\n\n\n\n"));

        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }

    public Bitmap generateWalletBitmap() {
        SysParam sysParam = FinancialApplication.getSysParam();

        IPage page = Device.generatePage();

        /*Header*/
        page.addLine()
                .adjustTopSpace(3)
                .addUnit(page.createUnit()
                        .setBitmap(getImageFromAssetsFile("logo_bbl_th_black.jpg"))
                        .setGravity(Gravity.CENTER));

        /*Body*/
        // Qr code
        page.addLine()
                .addUnit(page.createUnit()
                        .setBitmap(bitmapQr)
                        .setGravity(Gravity.CENTER));


        //Biller Service Code
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(billerServiceCode)
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.CENTER));

        //merchant name
        String merName = sysParam.get(SysParam.StringParam.EDC_MERCHANT_NAME_EN);
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(merName)
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.CENTER));

        //TID
        page.addLine()
                .adjustTopSpace(10)
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_terminal_code_short_sharp) + terminalId)
                        .setFontSize(FONT_SMALL));
        //MER
        page.addLine()
                .adjustTopSpace(3)
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_merchant_code_short_sharp) + merchantId)
                        .setFontSize(FONT_SMALL));

        // date/time
        String formattedDate = TimeConverter.convert(datetime, Constants.TIME_PATTERN_TRANS,
                Constants.DATE_PATTERN_DISPLAY);

        String formattedTime = TimeConverter.convert(datetime, Constants.TIME_PATTERN_TRANS,
                Constants.TIME_PATTERN_DISPLAY4);

        page.addLine()
                .adjustTopSpace(3)
                .addUnit(page.createUnit()
                        .setText(formattedDate)
                        .setFontSize(FONT_SMALL))
                .addUnit(page.createUnit()
                        .setText(formattedTime)
                        .setFontSize(FONT_SMALL)
                        .setGravity(Gravity.END));

        //AMT
        long amt = Utils.parseLongSafe(amount, 0);
        String temp = CurrencyConverter.convert(amt, CurrencyConverter.getDefCurrency());
        page.addLine()
                .adjustTopSpace(10)
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_amount_short))
                        .setFontSize(FONT_BIG)
                        .setWeight(4.0f))
                .addUnit(page.createUnit()
                        .setText(temp)
                        .setFontSize(FONT_BIG)
                        .setGravity(Gravity.END)
                        .setWeight(9.0f));
        //////

        page.addLine().addUnit(page.createUnit().setText("\n\n\n\n"));

        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }

    @Override
    public String generateString() {
        return null;
    }

    private Bitmap getImageFromAssetsFile(String fileName) {
        Bitmap image = null;
        AssetManager am = FinancialApplication.getApp().getResources().getAssets();
        try {
            InputStream is = am.open(fileName);
            image = BitmapFactory.decodeStream(is);
            is.close();
        } catch (IOException e) {
            Log.e(TAG, "", e);
        }

        return image;

    }

    private String formatQrRef2() {
        StringBuilder str = new StringBuilder(qrRef2);
        int idx = str.length() - 4;

        while (idx > 0)
        {
            str.insert(idx, "-");
            idx = idx - 4;
        }
        return str.toString();
    }
}
