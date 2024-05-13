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
import com.pax.pay.trans.model.TransData.OfflineStatus;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * offline transaction upload failed detail generator
 *
 * @author Steven.W
 */

class ReceiptGeneratorFailedTransDetail implements IReceiptGenerator {

    private String title;

    private List<TransData> failedTransList;

    /**
     * @param failedTransList ：failedTransList
     */
    public ReceiptGeneratorFailedTransDetail(String title, List<TransData> failedTransList) {
        this.title = title;
        this.failedTransList = failedTransList;
    }

    @Override
    public Bitmap generateBitmap() {

        List<TransData> failedList = new ArrayList<>();
        List<TransData> rejectList = new ArrayList<>();

        for (TransData data : failedTransList) {

            if (data.getOfflineSendState() == null)
                continue;

            if (data.getOfflineSendState() == OfflineStatus.OFFLINE_ERR_SEND) {
                failedList.add(data);
            }
            if (data.getOfflineSendState() == OfflineStatus.OFFLINE_ERR_RESP) {
                rejectList.add(data);
            }
        }

        IPage page = Device.generatePage();

        // title
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(title)
                        .setFontSize(FONT_BIG)
                        .setGravity(Gravity.CENTER));

        generateFailedMainInfo(page);
        generateTransData(page, failedList);
        page.addLine().addUnit(page.createUnit().setText("\n")); //AET-71
        generateRejectMainInfo(page);
        generateTransData(page, rejectList);

        page.addLine().addUnit(page.createUnit().setText("\n\n\n\n")); //AET-71

        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }

    @Override
    public String generateString() {
        return null;
    }

    /**
     * generate failed transaction detail main information
     */
    private void generateFailedMainInfo(IPage page) {
        Acquirer acquirer = FinancialApplication.getAcqManager().getCurAcq();

        // merchant ID
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_merchant_code))
                        .setFontSize(FONT_SMALL)
                        .setWeight(2.0f))
                .addUnit(page.createUnit()
                        .setText(acquirer.getMerchantId())
                        .setGravity(Gravity.END)
                        .setWeight(3.0f));

        // terminal ID/operator ID
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_terminal_code_space))
                        .setFontSize(FONT_SMALL))
                .addUnit(page.createUnit()
                        .setText(acquirer.getTerminalId())
                        .setGravity(Gravity.END));

        // batch NO
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_batch_num_space))
                        .setFontSize(FONT_SMALL))
                .addUnit(page.createUnit()
                        .setText(Component.getPaddedNumber(FinancialApplication.getAcqManager().getCurAcq().getCurrBatchNo(), 6))
                        .setFontSize(FONT_SMALL)
                        .setGravity(Gravity.END));

        // data/time
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_date))
                        .setFontSize(FONT_SMALL)
                        .setWeight(2.0f))
                .addUnit(page.createUnit()
                        .setText(Device.getTime(Constants.TIME_PATTERN_DISPLAY))
                        .setFontSize(FONT_SMALL)
                        .setGravity(Gravity.END)
                        .setWeight(3.0f));

        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_failed_trans_details))
                        .setFontSize(FONT_SMALL));

        generateTranInfo(page);
    }

    private void generateTranInfo(IPage page) {
        // transaction information
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("VOUCHER")
                        .setFontSize(FONT_SMALL)
                        .setWeight(2.0f))
                .addUnit(page.createUnit()
                        .setText("TYPE")
                        .setFontSize(FONT_SMALL)
                        .setGravity(Gravity.CENTER)
                        .setWeight(1.0f))
                .addUnit(page.createUnit()
                        .setText("AMOUNT")
                        .setFontSize(FONT_SMALL)
                        .setGravity(Gravity.END)
                        .setWeight(3.0f));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("CARD NO")
                        .setFontSize(FONT_SMALL));
    }

    /**
     * generate transaction details
     */
    private void generateTransData(IPage page, List<TransData> list) {
        for (TransData transData : list) {
            String type = "";

            // transaction NO/transaction type/amount
            String temp = CurrencyConverter.convert(Utils.parseLongSafe(transData.getAmount(), 0), transData.getCurrency());

            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Component.getPaddedNumber(transData.getStanNo(), 6))
                            .setFontSize(FONT_SMALL)
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(type)
                            .setFontSize(FONT_SMALL)
                            .setGravity(Gravity.CENTER)
                            .setWeight(1.0f))
                    .addUnit(page.createUnit()
                            .setText(temp)
                            .setFontSize(FONT_SMALL)
                            .setGravity(Gravity.END)
                            .setWeight(3.0f));

            // card NO/auth code
            temp = transData.getPan();
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(temp)
                            .setFontSize(FONT_SMALL));
        }

    }

    /**
     * generate offline transaction upload rejected receipt main information
     */
    private void generateRejectMainInfo(IPage page) {
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_reject_trans_details))
                        .setFontSize(FONT_SMALL));

        generateTranInfo(page);

    }

}
