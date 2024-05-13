package com.pax.pay.trans.receipt;

import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.view.Gravity;

import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.glwrapper.imgprocessing.IImgProcessing;
import com.pax.glwrapper.page.IPage;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.Utils;

public class ReceiptGeneratorEReceiptUploadStatus implements IReceiptGenerator {
    float halfWidthSize = 0.6f;
    float fullSizeOneLine = 1.2f;
    float fullSizeDoubleLine = 0.8f;

    private long[] ermSuccessTotal;
    private long[] ermUnsuccessfulTotal;
    private long[] ermVoidSuccessTotal;
    private long[] ermVoidUnsuccessfulTotal;

    public ReceiptGeneratorEReceiptUploadStatus(long[] ermSuccessTotal, long[] ermUnsuccessfulTotal, long[] ermVoidSuccessTotal, long[] ermVoidUnsuccessfulTotal) {
        this.ermSuccessTotal = ermSuccessTotal;
        this.ermUnsuccessfulTotal = ermUnsuccessfulTotal;
        this.ermVoidSuccessTotal = ermVoidSuccessTotal;
        this.ermVoidUnsuccessfulTotal = ermVoidUnsuccessfulTotal;
    }

    @Override
    public Bitmap generateBitmap() {
        IPage page = Device.generatePage(true);

        long erm_total_slip = ermSuccessTotal[0] + ermUnsuccessfulTotal[0] + ermVoidSuccessTotal[0] + ermVoidUnsuccessfulTotal[0];
        long erm_total_amt = (ermSuccessTotal[1] + ermUnsuccessfulTotal[1]) - (ermVoidSuccessTotal[1] + ermVoidUnsuccessfulTotal[1]);

        // title
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.title_ereceipt_upload_status))
                        .setFontSize(FONT_BIG)
                        .setGravity(Gravity.CENTER)
                        .setTextStyle(Typeface.BOLD));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_double_line))
                .setScaleX(fullSizeDoubleLine)
                .setGravity(Gravity.CENTER));

        page.addLine()
                .addUnit(page.createUnit()
                        .setText("Sale Success ERM" + " :")
                        .setFontSize(FONT_NORMAL)
                        .setScaleX(halfWidthSize)
                        .setGravity(Gravity.START))
                .addUnit(page.createUnit()
                        .setText("" + ermSuccessTotal[0])
                        .setFontSize(FONT_NORMAL)
                        .setScaleX(halfWidthSize)
                        .setGravity(Gravity.END));

        page.addLine()
                .addUnit(page.createUnit()
                        .setText("" + CurrencyConverter.convert(ermSuccessTotal[1]))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));

        page.addLine()
                .addUnit(page.createUnit()
                        .setText("Sale Unsuccessful ERM" + " :")
                        .setFontSize(FONT_NORMAL)
                        .setScaleX(halfWidthSize)
                        .setGravity(Gravity.START))
                .addUnit(page.createUnit()
                        .setText("" + ermUnsuccessfulTotal[0])
                        .setFontSize(FONT_NORMAL)
                        .setScaleX(halfWidthSize)
                        .setGravity(Gravity.END));

        page.addLine()
                .addUnit(page.createUnit()
                        .setText("" + CurrencyConverter.convert(ermUnsuccessfulTotal[1]))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));

        page.addLine()
                .addUnit(page.createUnit()
                        .setText("Void Success ERM" + " :")
                        .setFontSize(FONT_NORMAL)
                        .setScaleX(halfWidthSize)
                        .setGravity(Gravity.START))
                .addUnit(page.createUnit()
                        .setText("" + ermVoidSuccessTotal[0])
                        .setFontSize(FONT_NORMAL)
                        .setScaleX(halfWidthSize)
                        .setGravity(Gravity.END));

        page.addLine()
                .addUnit(page.createUnit()
                        .setText("" + CurrencyConverter.convert(-1 * ermVoidSuccessTotal[1]))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));

        page.addLine()
                .addUnit(page.createUnit()
                        .setText("Void Unsuccessful ERM" + " :")
                        .setFontSize(FONT_NORMAL)
                        .setScaleX(halfWidthSize)
                        .setGravity(Gravity.START))
                .addUnit(page.createUnit()
                        .setText("" + ermVoidUnsuccessfulTotal[0])
                        .setFontSize(FONT_NORMAL)
                        .setScaleX(halfWidthSize)
                        .setGravity(Gravity.END));

        page.addLine()
                .addUnit(page.createUnit()
                        .setText("" + CurrencyConverter.convert(-1 * ermVoidUnsuccessfulTotal[1]))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setScaleX(fullSizeOneLine)
                .setGravity(Gravity.CENTER));

        page.addLine()
                .addUnit(page.createUnit()
                        .setText("ERM Total Slip" + " :")
                        .setFontSize(FONT_NORMAL)
                        .setScaleX(halfWidthSize)
                        .setGravity(Gravity.START))
                .addUnit(page.createUnit()
                        .setText("" + erm_total_slip)
                        .setFontSize(FONT_NORMAL)
                        .setScaleX(halfWidthSize)
                        .setGravity(Gravity.END));

        page.addLine()
                .addUnit(page.createUnit()
                        .setText("" + CurrencyConverter.convert(erm_total_amt))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_double_line))
                .setScaleX(fullSizeDoubleLine)
                .setGravity(Gravity.CENTER));

        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }

    @Override
    public String generateString() {
        return null;
    }
}
