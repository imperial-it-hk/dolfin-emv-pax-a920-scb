package com.pax.pay.trans.receipt;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import th.co.bkkps.utils.Log;
import android.view.Gravity;

import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.glwrapper.imgprocessing.IImgProcessing;
import com.pax.glwrapper.page.IPage;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.TransTotal;
import com.pax.pay.utils.TimeConverter;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by SORAYA S on 22-May-18.
 */

public class ReceiptGeneratorTotalDetailWallet implements IReceiptGenerator {

    private Acquirer acquirer;
    private TransTotal total;
    private boolean isReprint;

    /**
     *
     *
     * @param acquirer ：acquirer
     * @param total : transTotal
     */
    ReceiptGeneratorTotalDetailWallet(Acquirer acquirer, TransTotal total, boolean isReprint) {
        this.acquirer = acquirer;
        this.total = total;
        this.isReprint = isReprint;
    }

    @Override
    public Bitmap generateBitmap() {
        IPage page = Device.generatePage();
        page.setTypefaceObj(Typeface.createFromAsset(FinancialApplication.getApp().getAssets(), Constants.FONT_NAME_MONO));

        /*============= Header =============*/
        // title
        page.addLine()
                .addUnit(page.createUnit()
                        .setBitmap(Component.getImageFromInternalFile(Constants.SLIP_LOGO_NAME))
                        .setGravity(Gravity.CENTER));
        page.addLine().addUnit(page.createUnit().setText(" "));

        //merchant name
        SysParam sysParam = FinancialApplication.getSysParam();
        String merName = sysParam.get(SysParam.StringParam.EDC_MERCHANT_NAME_EN);
        String merAddress= sysParam.get(SysParam.StringParam.EDC_MERCHANT_ADDRESS);
        String merAddress1= sysParam.get(SysParam.StringParam.EDC_MERCHANT_ADDRESS1);

        page.addLine()
                .addUnit(page.createUnit()
                        .setText(merName)
                        .setFontSize(FONT_NORMAL_26)
                        .setGravity(Gravity.CENTER));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(merAddress)
                        .setFontSize(FONT_NORMAL_26)
                        .setGravity(Gravity.CENTER));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(merAddress1)
                        .setFontSize(FONT_NORMAL_26)
                        .setGravity(Gravity.CENTER));
        page.addLine().addUnit(page.createUnit().setText(" "));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setFontSize(FONT_SMALL_19)
                .setGravity(Gravity.CENTER)
                .setTextStyle(Typeface.BOLD));

        //TID
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_terminal_code_short_sharp) + "  " + acquirer.getTerminalId())
                        .setFontSize(FONT_NORMAL_22));
        //MER
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_merchant_code_short_sharp) + "  " + acquirer.getMerchantId())
                        .setFontSize(FONT_NORMAL_22));
        //BATCH NO.
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_batch_num_short_sharp) + "  " + Component.getPaddedNumber(acquirer.getCurrBatchNo(), 6))
                        .setFontSize(FONT_NORMAL_22));
        //HOST NAME
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_host_name) + "  " + acquirer.getName())
                        .setFontSize(FONT_NORMAL_22));
        //HOST NII
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_host_nii) + "  " + acquirer.getNii())
                        .setFontSize(FONT_NORMAL_22));

        //Datetime
        String dateTime = total.getDateTime();
        String dateFormat = TimeConverter.convert(dateTime, Constants.TIME_PATTERN_TRANS, Constants.DATE_PATTERN_DISPLAY);
        String timeFormat = TimeConverter.convert(dateTime, Constants.TIME_PATTERN_TRANS, Constants.TIME_PATTERN_DISPLAY4);

        page.addLine()
                .addUnit(page.createUnit()
                        .setText(dateFormat)
                        .setFontSize(FONT_NORMAL_22))
                .addUnit(page.createUnit()
                        .setText(timeFormat)
                        .setFontSize(FONT_NORMAL_22)
                        .setGravity(Gravity.END));

        /*============= Body =============*/
        //wallet settlement info.
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(total.getWalletSettleSlipInfo())
                        .setFontSize(FONT_SMALL_19)
                        .setTextStyle(Typeface.BOLD));

        if (isReprint) {
            page.addLine()
                    .adjustTopSpace(5)
                    .addUnit(page.createUnit()
                            .setText("* " + Utils.getString(R.string.receipt_print_again) + " *")
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.CENTER));
        }

        page.addLine().addUnit(page.createUnit().setText("\n\n\n\n"));

        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }

    @Override
    public String generateString() {
        return null;
    }

    /**
     * generate detail information
     *
     * @return
     */
    public Bitmap generateMainInfo() {
        IPage page = Device.generatePage();

        // title
        page.addLine()
                .addUnit(page.createUnit()
                        .setBitmap(Component.getImageFromInternalFile(Constants.SLIP_LOGO_NAME))
                        .setGravity(Gravity.CENTER));
        page.addLine().addUnit(page.createUnit().setText(" "));

        //merchant name
        SysParam sysParam = FinancialApplication.getSysParam();
        String merName = sysParam.get(SysParam.StringParam.EDC_MERCHANT_NAME_EN);
        String merAddress= sysParam.get(SysParam.StringParam.EDC_MERCHANT_ADDRESS);
        String merAddress1= sysParam.get(SysParam.StringParam.EDC_MERCHANT_ADDRESS1);

        page.addLine()
                .addUnit(page.createUnit()
                        .setText(merName)
                        .setFontSize(FONT_NORMAL_26)
                        .setGravity(Gravity.CENTER));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(merAddress)
                        .setFontSize(FONT_NORMAL_26)
                        .setGravity(Gravity.CENTER));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(merAddress1)
                        .setFontSize(FONT_NORMAL_26)
                        .setGravity(Gravity.CENTER));
        page.addLine().addUnit(page.createUnit().setText(" "));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setGravity(Gravity.CENTER));

        //TID
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_terminal_code_short_sharp) + "  " + acquirer.getTerminalId())
                        .setFontSize(FONT_NORMAL));
        //MER
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_merchant_code_short_sharp) + "  " + acquirer.getMerchantId())
                        .setFontSize(FONT_NORMAL));
        //BATCH NO.
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_batch_num_short_sharp) + "  " + Component.getPaddedNumber(acquirer.getCurrBatchNo(), 6))
                        .setFontSize(FONT_NORMAL));
        //HOST NAME
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_host_name) + "  " + acquirer.getName())
                        .setFontSize(FONT_NORMAL));
        //HOST NII
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_host_nii) + "  " + acquirer.getNii())
                        .setFontSize(FONT_NORMAL));

        //Datetime
        String dateTime = total.getDateTime();
        String dateFormat = TimeConverter.convert(dateTime, Constants.TIME_PATTERN_TRANS, Constants.DATE_PATTERN_DISPLAY);
        String timeFormat = TimeConverter.convert(dateTime, Constants.TIME_PATTERN_TRANS, Constants.TIME_PATTERN_DISPLAY4);

        page.addLine()
                .addUnit(page.createUnit()
                        .setText(dateFormat)
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(timeFormat)
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));

        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
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
}
