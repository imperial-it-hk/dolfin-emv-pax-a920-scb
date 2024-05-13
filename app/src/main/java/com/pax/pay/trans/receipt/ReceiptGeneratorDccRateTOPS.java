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
package com.pax.pay.trans.receipt;

import android.graphics.Bitmap;
import android.view.Gravity;

import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.glwrapper.imgprocessing.IImgProcessing;
import com.pax.glwrapper.page.IPage;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.TimeConverter;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import static com.pax.pay.utils.Utils.getString;

/**
 * receipt generator
 *
 * @author Steven.W
 */
public class ReceiptGeneratorDccRateTOPS implements IReceiptGenerator {

    TransData transData;
    String localAmount;
    String rate;
    String dccAmount;
    String localCurrency;
    String markUp;

    public ReceiptGeneratorDccRateTOPS(TransData transData, String localAmount, String rate, String dccAmount, String localCurrency, String markUp) {
        this.transData = transData;
        this.localAmount = localAmount;
        this.rate = rate;
        this.dccAmount = dccAmount;
        this.localCurrency = localCurrency;
        this.markUp = markUp;
    }

    @Override
    public Bitmap generateBitmap() {
        IPage page = Device.generatePage();
        page.adjustLineSpace(1);

        SysParam sysParam = FinancialApplication.getSysParam();
        if (transData != null) {

            /*Header*/
            // title
            Acquirer acquirer = FinancialApplication.getAcqManager().getCurAcq();
            page.addLine()
                    .addUnit(page.createUnit()
                            .setBitmap(Component.getImageFromInternalFile(Constants.SLIP_LOGO_NAME, acquirer.getNii() + "_" + acquirer.getName()))
                            .setGravity(Gravity.CENTER));
            page.addLine().addUnit(page.createUnit().setText(" "));

            //merchant name
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
                    .setText(getString(R.string.receipt_one_line))
                    .setGravity(Gravity.CENTER));

            //TID
            page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_terminal_code_short))
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                                .setText(acquirer.getTerminalId())
                                .setWeight(4.0f)
                                .setGravity(Gravity.LEFT)
                );
            //MID
            page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_merchant_code_short))
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(acquirer.getMerchantId())
                        .setGravity(Gravity.LEFT)
                        .setWeight(4.0f));

            //DATE TIME
            String formattedDate = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                    Constants.DATE_PATTERN_DISPLAY);

            String formattedTime = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                    Constants.TIME_PATTERN_DISPLAY4);

            page.addLine()
                .addUnit(page.createUnit()
                        .setText(formattedDate)
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(formattedTime)
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));

            page.addLine().addUnit(page.createUnit()
                    .setText(getString(R.string.receipt_one_line))
                    .setGravity(Gravity.CENTER));

            //Body
            //Local Amount
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(localCurrency +" "+Utils.getString(R.string.receipt_amount))
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                                    .setText(localAmount)
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.END));

            //DCC Amount
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(transData.getDccCurrencyName() +" "+ Utils.getString(R.string.receipt_amount))
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(dccAmount)
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.END));

            //DCC Exchange rate
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_dcc_ex_rate_full) + ":")
                            .setFontSize(FONT_NORMAL));
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("1 " + transData.getDccCurrencyName() +" = "+ localCurrency + " " +rate)
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.END));

            //DCC Margin
            if(markUp != null){
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_margin) + " = "+ markUp + "%")
                                .setFontSize(FONT_NORMAL)
                                .setGravity(Gravity.END));
            }


            page.addLine().addUnit(page.createUnit()
                    .setText(getString(R.string.receipt_one_line))
                    .setGravity(Gravity.CENTER));

            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_dcc_ack_tops,markUp + "%"))
                            .setFontSize(FONT_SMALL)
                            .setGravity(Gravity.CENTER));


            page.addLine().addUnit(page.createUnit().setText("\n\n\n\n"));

        }

        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }

    @Override
    public String generateString() {
        return null;
    }


}
