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
import android.graphics.Typeface;
import android.view.Gravity;

import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.glwrapper.imgprocessing.IImgProcessing;
import com.pax.glwrapper.page.IPage;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.model.TransRedeemKbankTotal;
import com.pax.pay.trans.model.TransTotal;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.TimeConverter;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.util.ArrayList;
import java.util.List;

import th.co.bkkps.utils.PageToSlipFormat;

class ReceiptGeneratorTotalTOPS implements IReceiptGenerator {
    float halfWidthSize = 0.6f;
    float fullSizeOneLine = 1.2f;
    float fullSizeDoubleLine = 0.8f;
    private String title;
    private String result;
    private TransTotal total;
    private int acqNum;
    private String errorCode;
    private boolean isReprint;

    /**
     * @param title      ：title
     * @param result     : result
     * @param transTotal ：transTotal
     */
    ReceiptGeneratorTotalTOPS(String title, String result, TransTotal transTotal, int acqNum, boolean isReprint) {
        this.title = title;
        this.result = result;
        this.total = transTotal;
        this.acqNum = acqNum;
        this.isReprint = isReprint;
    }

    ReceiptGeneratorTotalTOPS(String title, TransTotal transTotal, String errorCode) {
        this.title = title;
        this.total = transTotal;
        this.errorCode = errorCode;
    }

    @Override
    public Bitmap generateBitmap() {
        IPage page = Device.generatePage(true);
        Boolean isSummary = false;

        if (title.equalsIgnoreCase(Utils.getString(R.string.print_history_summary))) {
            isSummary = true;
        }

        /*Header*/
        // title
        //if (!isSummary || (isSummary && acqNum == 1) || (isSummary && acqNum == 999)) {//acqNum 999 = summary 1 acq
            Bitmap logo;
//            if (acqNum == 1) {// summary report all acquirer
//                logo = Component.getImageFromInternalFile(Constants.SLIP_LOGO_NAME);
//            } else {// by acquirer
                logo = Component.getImageFromInternalFile(Constants.SLIP_LOGO_NAME, total.getAcquirer().getNii() + "_" + total.getAcquirer().getName());
//            }
            page.addLine()
                    .addUnit(page.createUnit()
                            .setBitmap(logo)
                            .setGravity(Gravity.CENTER));
            page.addLine().addUnit(page.createUnit().setText(" "));


            //merchant name
            SysParam sysParam = FinancialApplication.getSysParam();
            String merName = sysParam.get(SysParam.StringParam.EDC_MERCHANT_NAME_EN);
            String merAddress = sysParam.get(SysParam.StringParam.EDC_MERCHANT_ADDRESS);
            String merAddress1 = sysParam.get(SysParam.StringParam.EDC_MERCHANT_ADDRESS1);
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
                    .setText(Utils.getString(R.string.receipt_double_line))
                    .setScaleX(fullSizeDoubleLine)
                    .setGravity(Gravity.CENTER));
        //}

        // Change acquirer name Prompt Pay on slip
        String acqName = total.getAcquirer().getName();

        //HOST
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("HOST")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(": " + acqName)
                        .setWeight(3.0f)
                        .setFontSize(FONT_NORMAL));

        // terminal ID/operator ID
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("TID")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(": " + total.getTerminalID())
                        .setWeight(3.0f)
                        .setGravity(Gravity.LEFT));

        /*if(!isSummary || (isSummary && acqNum == 999)){ //acqNum 999 = summary 1 acq*/
        // merchant ID
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("MID")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(": " + total.getMerchantID())
                        .setGravity(Gravity.LEFT)
                        .setWeight(3.0f));

        // batch NO
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("BATCH")
                        .setFontSize(FONT_NORMAL_22))
                .addUnit(page.createUnit()
                        .setText(": " + Component.getPaddedNumber(total.getAcquirer().getCurrBatchNo(), 6))
                        .setGravity(Gravity.LEFT)
                        .setWeight(3.0f));


           /* }else if(isSummary && acqNum == 1){
                page.addLine().addUnit(page.createUnit().setText("< ALL ACQUIRER >").setFontSize(FONT_NORMAL).setGravity(Gravity.CENTER));
            }*/

        page.addLine().addUnit(page.createUnit()
                .setText(" ")
                .setGravity(Gravity.CENTER));
        // title
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(title)
                        .setFontSize(FONT_BIG)
                        .setTextStyle(Typeface.BOLD));
        page.addLine().addUnit(page.createUnit()
                .setText(" ")
                .setGravity(Gravity.CENTER));

        if (!isSummary) {
            // batch NO
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_batch_num_short))
                            .setWeight(1.0f)
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(Component.getPaddedNumber(total.getBatchNo(), 6))
                            .setWeight(1.0f)
                            .setGravity(Gravity.LEFT))
                    .addUnit(page.createUnit()
                            .setText("CLOSED")
                            .setWeight(1.0f)
                            .setGravity(Gravity.END)
                            .setFontSize(FONT_NORMAL));
        }

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


        page.addLine().addUnit(page.createUnit()
                .setText(" ")
                .setGravity(Gravity.CENTER));
        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setScaleX(fullSizeOneLine)
                .setGravity(Gravity.CENTER));


        //Host Total
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("TOTALS-DEBIT/CR")
                        .setFontSize(FONT_NORMAL));
        page.addLine().addUnit(page.createUnit()
                .setText(" ")
                .setGravity(Gravity.CENTER));

        // sale all
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.trans_sale).toUpperCase())
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(Component.getPaddedNumber((total.getSaleTotalNum()), 3))
                        .setFontSize(FONT_NORMAL)
                        .setWeight(1.0f)
                        .setScaleX(halfWidthSize)
                        .setGravity(Gravity.CENTER))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(total.getSaleTotalAmt()))
                        .setFontSize(FONT_NORMAL)
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setGravity(Gravity.END));
        // refund
        String tmpRefund = CurrencyConverter.convert(total.getRefundTotalAmt());
        ;
        if (total.getRefundTotalAmt() != 0.00) {
            tmpRefund = "- " + tmpRefund;
        }
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.trans_refund).toUpperCase())
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(Component.getPaddedNumber(total.getRefundTotalNum(), 3))
                        .setWeight(1.0f)
                        .setScaleX(halfWidthSize)
                        .setGravity(Gravity.CENTER)
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(tmpRefund)
                        .setFontSize(FONT_NORMAL)
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setGravity(Gravity.END));

        // void sale
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_void_sale).toUpperCase())
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(Component.getPaddedNumber(total.getSaleVoidTotalNum(), 3))
                        .setWeight(1.0f)
                        .setScaleX(halfWidthSize)
                        .setGravity(Gravity.CENTER)
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(0 - total.getSaleVoidTotalAmt()))
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));

        // void refund
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.trans_void).toUpperCase() + " REFUND")
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(Component.getPaddedNumber(total.getRefundVoidTotalNum(), 3))
                        .setWeight(1.0f)
                        .setScaleX(halfWidthSize)
                        .setGravity(Gravity.CENTER)
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(total.getRefundVoidTotalAmt()))
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));

        // top up
        long topupNumtemp = total.getTopupTotalNum();
        if (topupNumtemp != 0.00) {
            String tmpTopUp = CurrencyConverter.convert(total.getTopupTotalAmt());
            if (total.getTopupTotalAmt() != 0.00) {
                tmpTopUp = "- " + tmpTopUp;
            }
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.trans_topup).toUpperCase())
                            .setWeight(2.0f)
                            .setScaleX(halfWidthSize)
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(Component.getPaddedNumber(topupNumtemp, 3))
                            .setWeight(1.0f)
                            .setScaleX(halfWidthSize)
                            .setGravity(Gravity.CENTER)
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(tmpTopUp)
                            .setWeight(2.0f)
                            .setScaleX(halfWidthSize)
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.END));
        }


        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setScaleX(fullSizeOneLine));

        long tempTotalNum = total.getSaleTotalNum() + total.getRefundTotalNum() + total.getTopupTotalNum();
        long tempTotalAmt = total.getSaleTotalAmt() - total.getRefundTotalAmt() + total.getTopupTotalAmt();
        // Grand total (Host Total)
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("GRAND TOTAL")
                        .setScaleX(halfWidthSize)
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(tempTotalAmt))
                        .setScaleX(halfWidthSize)
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_double_line))
                .setScaleX(fullSizeDoubleLine)
                .setGravity(Gravity.CENTER));

        // sale small amount
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.trans_sale).toUpperCase() + " SMALL TICKET")
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(Component.getPaddedNumber(total.getSaleTotalSmallAmtNum(), 3))
                        .setWeight(1.0f)
                        .setScaleX(halfWidthSize)
                        .setGravity(Gravity.CENTER)
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(total.getSaleTotalSmallAmt()))
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));

        // sale normal
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.trans_sale).toUpperCase() + " NORMAL")
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(Component.getPaddedNumber((total.getSaleTotalNum() - total.getSaleTotalSmallAmtNum()), 3))
                        .setWeight(1.0f)
                        .setScaleX(halfWidthSize)
                        .setGravity(Gravity.CENTER)
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(total.getSaleTotalAmt() - total.getSaleTotalSmallAmt()))
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));

        //Refund
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.trans_refund).toUpperCase())
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(Component.getPaddedNumber(total.getRefundTotalNum(), 3))
                        .setWeight(1.0f)
                        .setScaleX(halfWidthSize)
                        .setGravity(Gravity.CENTER)
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(tmpRefund)
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));

        // void sale small amount
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.trans_void).toUpperCase() + " SMALL TICKET")
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(Component.getPaddedNumber(total.getVoidTotalSmallAmtNum(), 3))
                        .setWeight(1.0f)
                        .setScaleX(halfWidthSize)
                        .setGravity(Gravity.CENTER)
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(0 - total.getVoidTotalSmallAmt()))
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));

        // void sale
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.trans_void).toUpperCase() + " NORMAL")
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(Component.getPaddedNumber((total.getSaleVoidTotalNum() - total.getVoidTotalSmallAmtNum()), 3))
                        .setWeight(1.0f)
                        .setScaleX(halfWidthSize)
                        .setGravity(Gravity.CENTER)
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(0 - (total.getSaleVoidTotalAmt() - total.getVoidTotalSmallAmt())))
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));

        // void refund
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.trans_void).toUpperCase() + " REFUND")
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(Component.getPaddedNumber(total.getRefundVoidTotalNum(), 3))
                        .setWeight(1.0f)
                        .setScaleX(halfWidthSize)
                        .setGravity(Gravity.CENTER)
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(total.getRefundVoidTotalAmt()))
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));


        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setScaleX(fullSizeOneLine));

        // Grand total (Host Total)
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("GRAND TOTAL")
                        .setScaleX(halfWidthSize)
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(tempTotalAmt))
                        .setScaleX(halfWidthSize)
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_double_line))
                .setScaleX(fullSizeDoubleLine)
                .setGravity(Gravity.CENTER));

        ////////////////////////////////

        if (Constants.ACQ_WALLET.equals(total.getAcquirer().getName())) {//WALLET
            List<TransData.ETransStatus> filter = new ArrayList<>();
            filter.add(TransData.ETransStatus.NORMAL);
            List<String[]> tempList = FinancialApplication.getTransDataDbHelper().countSumOfWallet(total.getAcquirer(), filter, null);
            if (tempList != null && !tempList.isEmpty()) {
                for (String[] tempObj : tempList) {
                    Object[] rawResults = getRawResults(tempObj);
                    if (rawResults != null) {
                        String walletName = (String) rawResults[2];
                        if (walletName != null) {
                            page.addLine()
                                    .adjustTopSpace(10)
                                    .addUnit(page.createUnit()
                                            .setText(walletName)
                                            .setFontSize(FONT_NORMAL));

                            long[] temp = FinancialApplication.getTransDataDbHelper().countSumOfWallet(total.getAcquirer(), ETransType.QR_SALE_WALLET, filter, null, walletName);
                            long saleWalletNum = temp[0];
                            long saleWalletTotalAmt = temp[1];

                            // sale
                            page.addLine()
                                    .adjustTopSpace(8)
                                    .addUnit(page.createUnit()
                                            .setText(Utils.getString(R.string.trans_sale).toUpperCase())
                                            .setWeight(2.0f)
                                            .setScaleX(halfWidthSize)
                                            .setFontSize(FONT_NORMAL))
                                    .addUnit(page.createUnit()
                                            .setText(Component.getPaddedNumber(saleWalletNum, 3))
                                            .setWeight(1.0f)
                                            .setScaleX(halfWidthSize)
                                            .setGravity(Gravity.CENTER)
                                            .setFontSize(FONT_NORMAL))
                                    .addUnit(page.createUnit()
                                            .setText(CurrencyConverter.convert(saleWalletTotalAmt))
                                            .setFontSize(FONT_NORMAL)
                                            .setWeight(2.0f)
                                            .setScaleX(halfWidthSize)
                                            .setGravity(Gravity.END));


                            temp = FinancialApplication.getTransDataDbHelper().countSumOfWallet(total.getAcquirer(), ETransType.REFUND_WALLET, filter, null, walletName);
                            long refundWalletNum = temp[0];
                            long refundWalletTotalAmt = temp[1];

                            // refund
                            tmpRefund = CurrencyConverter.convert(refundWalletTotalAmt);
                            if (refundWalletTotalAmt != 0.00) {
                                tmpRefund = "- " + tmpRefund;
                            }
                            page.addLine()
                                    .addUnit(page.createUnit()
                                            .setText(Utils.getString(R.string.trans_refund).toUpperCase())
                                            .setWeight(2.0f)
                                            .setScaleX(halfWidthSize)
                                            .setFontSize(FONT_NORMAL))
                                    .addUnit(page.createUnit()
                                            .setText(Component.getPaddedNumber(refundWalletNum, 3))
                                            .setWeight(1.0f)
                                            .setScaleX(halfWidthSize)
                                            .setGravity(Gravity.CENTER)
                                            .setFontSize(FONT_NORMAL))
                                    .addUnit(page.createUnit()
                                            .setText(tmpRefund)
                                            .setFontSize(FONT_NORMAL)
                                            .setWeight(2.0f)
                                            .setScaleX(halfWidthSize)
                                            .setGravity(Gravity.END));

                            page.addLine().addUnit(page.createUnit()
                                    .setText("---------")
                                    .setGravity(Gravity.END));

                            tempTotalNum = saleWalletNum + refundWalletNum;
                            tempTotalAmt = saleWalletTotalAmt - refundWalletTotalAmt;
                            // total
                            page.addLine()
                                    .addUnit(page.createUnit()
                                            .setText("TOTAL")
                                            .setWeight(2.0f)
                                            .setScaleX(halfWidthSize)
                                            .setFontSize(FONT_NORMAL))
                                    .addUnit(page.createUnit()
                                            .setText(Component.getPaddedNumber(tempTotalNum, 3))
                                            .setWeight(1.0f)
                                            .setScaleX(halfWidthSize)
                                            .setGravity(Gravity.CENTER)
                                            .setFontSize(FONT_NORMAL))
                                    .addUnit(page.createUnit()
                                            .setText(CurrencyConverter.convert(tempTotalAmt))
                                            .setFontSize(FONT_NORMAL)
                                            .setWeight(2.0f)
                                            .setScaleX(halfWidthSize)
                                            .setGravity(Gravity.END));
                        }
                    }
                }
            }
        }

        // message complete
        /*if(title.equalsIgnoreCase(Utils.getString(R.string.settle_title))){
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("*** SETTLEMENT ***")
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.CENTER));
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("CLOSED SUCCESSFULLY")
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.CENTER));

            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.receipt_double_line))
                    .setGravity(Gravity.CENTER));
        }else if(!isSummary){
            page.addLine().addUnit(page.createUnit().setText("\n\n"));
        }*/


        page.addLine().addUnit(page.createUnit().setText("\n"));
        page.addLine().addUnit(page.createUnit().setText("TOTALS BY CARD"));


        if (Component.isDemo()) {
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.demo_mode))
                            .setGravity(Gravity.CENTER));
        }

        // check is sett
        PageToSlipFormat.getInstance().isSettleMode = true;
        PageToSlipFormat.getInstance().Append(page);

        //page.addLine().addUnit(page.createUnit().setText("\n\n\n\n"));
        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();

        return imgProcessing.pageToBitmap(page, 384);
    }

    public Bitmap generateBitmapPrompt() {
        IPage page = Device.generatePage(true);
        Boolean isSummary = false;

        if (title.equalsIgnoreCase(Utils.getString(R.string.print_history_summary))) {
            isSummary = true;
        }
        /*Header*/
        // title
        //if (!isSummary || (isSummary && acqNum == 1) || (isSummary && acqNum == 999)) {//acqNum 999 = summary 1 acq
            Bitmap logo;
//                if (acqNum == 1) {// summary report all acquirer
//                    logo = Component.getImageFromInternalFile(Constants.SLIP_LOGO_NAME);
//                } else {// by acquirer
            logo = Component.getImageFromInternalFile(Constants.SLIP_LOGO_NAME, total.getAcquirer().getNii() + "_" + total.getAcquirer().getName());
//                }
            page.addLine()
                    .addUnit(page.createUnit()
                            .setBitmap(logo)
                            .setGravity(Gravity.CENTER));
            page.addLine().addUnit(page.createUnit().setText(" "));


            //merchant name
            SysParam sysParam = FinancialApplication.getSysParam();
            String merName = sysParam.get(SysParam.StringParam.EDC_MERCHANT_NAME_EN);
            String merAddress = sysParam.get(SysParam.StringParam.EDC_MERCHANT_ADDRESS);
            String merAddress1 = sysParam.get(SysParam.StringParam.EDC_MERCHANT_ADDRESS1);
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
                    .setText(Utils.getString(R.string.receipt_double_line))
                    .setScaleX(fullSizeDoubleLine)
                    .setGravity(Gravity.CENTER));
        //}
        //HOST
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("HOST")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(": " + total.getAcquirer().getName())
                        .setWeight(3.0f)
                        .setFontSize(FONT_NORMAL));
        // host nii
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("NII")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(": " + total.getAcquirer().getNii())
                        .setWeight(3.0f)
                        .setFontSize(FONT_NORMAL));

        // terminal ID/operator ID
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("TID")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(": " + total.getTerminalID())
                        .setWeight(3.0f)
                        .setGravity(Gravity.LEFT)
                );
        // merchant ID
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("MID")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(": " + total.getMerchantID())
                        .setGravity(Gravity.LEFT)
                        .setWeight(3.0f));

        page.addLine().addUnit(page.createUnit()
                .setText(" ")
                .setGravity(Gravity.CENTER));

        // title
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("Summary Report")
                        .setFontSize(FONT_BIG)
                        .setTextStyle(Typeface.BOLD));
        page.addLine().addUnit(page.createUnit()
                .setText(" ")
                .setGravity(Gravity.CENTER));

        // batch NO
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_batch_num_short))
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(Component.getPaddedNumber(total.getBatchNo(), 6))
                        .setGravity(Gravity.LEFT)
                        .setWeight(3.0f));

        // date/time
        String strDateTime = total.getDateTime();
        String formattedDate = TimeConverter.convert(strDateTime, Constants.TIME_PATTERN_TRANS,
                Constants.DATE_PATTERN_DISPLAY);
        String formattedTime = TimeConverter.convert(strDateTime, Constants.TIME_PATTERN_TRANS,
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
                .setText(" ")
                .setGravity(Gravity.CENTER));
        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setScaleX(fullSizeOneLine)
                .setGravity(Gravity.CENTER));

        // sale
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.settle_payment_totals).toUpperCase())
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(Long.toString(total.getSaleTotalNum()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END)
                        .setWeight(0.5f));

        // total
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.settle_amount_totals).toUpperCase())
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(total.getSaleTotalAmt()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END)
                        .setWeight(0.5f));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setScaleX(fullSizeOneLine)
                .setGravity(Gravity.CENTER));

        if (isReprint) {
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("* " + Utils.getString(R.string.receipt_print_again) + " *")
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.CENTER));
        }

        //page.addLine().addUnit(page.createUnit().setText("\n\n\n\n"));
        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();

        return imgProcessing.pageToBitmap(page, 384);
    }

    public Bitmap generateBitmapRabbit() {
        IPage page = Device.generatePage(true);
        boolean isSummary = title.equalsIgnoreCase(Utils.getString(R.string.print_history_summary));

        /*Header*/
        // title
        if (!isSummary || (isSummary && acqNum == 1) || (isSummary && acqNum == 999)) {
            page.addLine()
                    .addUnit(page.createUnit()
                            .setBitmap(Component.getImageFromInternalFile(Constants.SLIP_LOGO_NAME))
                            .setGravity(Gravity.CENTER));
            page.addLine().addUnit(page.createUnit().setText(" "));


            //merchant name
            SysParam sysParam = FinancialApplication.getSysParam();
            String merName = sysParam.get(SysParam.StringParam.EDC_MERCHANT_NAME_EN);
            String merAddress = sysParam.get(SysParam.StringParam.EDC_MERCHANT_ADDRESS);
            String merAddress1 = sysParam.get(SysParam.StringParam.EDC_MERCHANT_ADDRESS1);
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
                    .setText(Utils.getString(R.string.receipt_double_line))
                    .setScaleX(fullSizeDoubleLine)
                    .setGravity(Gravity.CENTER));
        }

        //HOST
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("HOST")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(": " + total.getAcquirer().getName())
                        .setWeight(3.0f)
                        .setFontSize(FONT_NORMAL));

        // terminal ID/operator ID
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("TID")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(": " + total.getTerminalID())
                        .setWeight(3.0f)
                        .setGravity(Gravity.LEFT)
                );

        // merchant ID
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("MID")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(": " + total.getMerchantID())
                        .setGravity(Gravity.LEFT)
                        .setWeight(3.0f));
        page.addLine().addUnit(page.createUnit()
                .setText(" ")
                .setGravity(Gravity.CENTER));

        // title
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(title)
                        .setFontSize(FONT_BIG)
                        .setTextStyle(Typeface.BOLD));
        page.addLine().addUnit(page.createUnit()
                .setText(" ")
                .setGravity(Gravity.CENTER));

        if (!isSummary) {
            // batch NO
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_batch_num_short))
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(Component.getPaddedNumber(total.getBatchNo(), 6))
                            .setGravity(Gravity.LEFT)
                            .setWeight(3.0f));
        }

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

        page.addLine().addUnit(page.createUnit()
                .setText(" ")
                .setGravity(Gravity.CENTER));


        if (!isSummary) {
            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.receipt_one_line))
                    .setScaleX(fullSizeOneLine)
                    .setGravity(Gravity.CENTER));

            //Host Total
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("TOTALS")
                            .setFontSize(FONT_NORMAL));

            // sale all
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.trans_sale).toUpperCase())
                            .setWeight(2.0f)
                            .setScaleX(halfWidthSize)
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(Component.getPaddedNumber(total.getSaleTotalNum(), 3))
                            .setFontSize(FONT_NORMAL)
                            .setWeight(1.0f)
                            .setScaleX(halfWidthSize)
                            .setGravity(Gravity.CENTER))
                    .addUnit(page.createUnit()
                            .setText(CurrencyConverter.convert(total.getSaleTotalAmt()))
                            .setFontSize(FONT_NORMAL)
                            .setWeight(2.0f)
                            .setScaleX(halfWidthSize)
                            .setGravity(Gravity.END));

            // refund
            String tmpRefund = CurrencyConverter.convert(total.getRefundTotalAmt());
            if (total.getRefundTotalAmt() != 0.00) {
                tmpRefund = "- " + tmpRefund;
            }
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.trans_refund).toUpperCase())
                            .setWeight(2.0f)
                            .setScaleX(halfWidthSize)
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(Component.getPaddedNumber(total.getRefundTotalNum(), 3))
                            .setWeight(1.0f)
                            .setScaleX(halfWidthSize)
                            .setGravity(Gravity.CENTER)
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(tmpRefund)
                            .setFontSize(FONT_NORMAL)
                            .setWeight(2.0f)
                            .setScaleX(halfWidthSize)
                            .setGravity(Gravity.END));

            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.receipt_one_line))
                    .setScaleX(fullSizeOneLine)
                    .setGravity(Gravity.CENTER));


            // Total Sale
            long totalSaleNum = total.getSaleTotalNum() + total.getRefundTotalNum();
            long totalSaleAmt = total.getSaleTotalAmt() - total.getRefundTotalAmt();
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("TOTAL SALES")
                            .setWeight(2.0f)
                            .setScaleX(halfWidthSize)
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(Component.getPaddedNumber(totalSaleNum, 3))
                            .setWeight(1.0f)
                            .setScaleX(halfWidthSize)
                            .setGravity(Gravity.CENTER)
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(CurrencyConverter.convert(totalSaleAmt))
                            .setWeight(2.0f)
                            .setScaleX(halfWidthSize)
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.END));

            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.receipt_double_line))
                    .setScaleX(fullSizeDoubleLine)
                    .setGravity(Gravity.CENTER));

            // top up
            String tmpTopUp = CurrencyConverter.convert(total.getTopupTotalAmt());
            if (total.getTopupTotalAmt() != 0.00) {
                tmpTopUp = "- " + tmpTopUp;
            }
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.trans_topup).toUpperCase())
                            .setWeight(2.0f)
                            .setScaleX(halfWidthSize)
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(Component.getPaddedNumber(total.getTopupTotalNum(), 3))
                            .setWeight(1.0f)
                            .setScaleX(halfWidthSize)
                            .setGravity(Gravity.CENTER)
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(tmpTopUp)
                            .setWeight(2.0f)
                            .setScaleX(halfWidthSize)
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.END));

            // top up refund
            String tmpTopUpVoid = CurrencyConverter.convert(total.getTopupVoidTotalAmt());
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("TOPUP REFUND")
                            .setWeight(2.0f)
                            .setScaleX(halfWidthSize)
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(Component.getPaddedNumber(total.getTopupVoidTotalNum(), 3))
                            .setWeight(1.0f)
                            .setScaleX(halfWidthSize)
                            .setGravity(Gravity.CENTER)
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(tmpTopUpVoid)
                            .setWeight(2.0f)
                            .setScaleX(halfWidthSize)
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.END));

            // receipt one line
            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.receipt_one_line))
                    .setScaleX(fullSizeOneLine));

            long totalTopupNum = total.getTopupTotalNum() + total.getTopupVoidTotalNum();
            long totalTopupAmt = total.getTopupTotalAmt() - total.getTopupVoidTotalAmt();
            // total

            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("TOTAL TOPUP")
                            .setWeight(2.0f)
                            .setScaleX(halfWidthSize)
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(Component.getPaddedNumber(totalTopupNum, 3))
                            .setWeight(1.0f)
                            .setScaleX(halfWidthSize)
                            .setGravity(Gravity.CENTER)
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(CurrencyConverter.convert(totalTopupAmt))
                            .setWeight(2.0f)
                            .setScaleX(halfWidthSize)
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.END));

            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.receipt_double_line))
                    .setScaleX(fullSizeDoubleLine)
                    .setGravity(Gravity.CENTER));
        }
        if (title.equalsIgnoreCase(Utils.getString(R.string.settle_title))) {
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("SETTLEMENT")
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.CENTER));
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("SUCCESSFULL")
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.CENTER));

            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.receipt_one_line))
                    .setScaleX(fullSizeOneLine));
        }

        // message complete
        if (Component.isDemo()) {
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.demo_mode))
                            .setGravity(Gravity.CENTER));
        }

        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }

    @Override
    public String generateString() {
        return null;
    }

    private Object[] getRawResults(String[] value) {
        Object[] obj = new Object[]{0, 0, ""};
        if (value != null) {
            obj[0] = value[0] == null ? 0 : Utils.parseLongSafe(value[0], 0);
            obj[1] = value[1] == null ? 0 : Utils.parseLongSafe(value[1], 0);
            obj[2] = value[2];
        }
        return obj;
    }

    public Bitmap generateGrandTotal(TransTotal transTotal) {
        IPage page = Device.generatePage();
        if (transTotal != null) {//settlement
            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.receipt_one_line))
                    .setGravity(Gravity.CENTER));
        }
        Component.generateGrandTotal(page, FONT_NORMAL, transTotal);

        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }

    public Bitmap generateReprint() {
        IPage page = Device.generatePage();
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("* " + Utils.getString(R.string.receipt_print_again) + " *")
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.CENTER));
        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }

    public Bitmap generateBitmapRedeemKbank() {
        IPage page = Device.generatePage(true);
        Boolean isSummary = false;

        if (title.equalsIgnoreCase(Utils.getString(R.string.print_history_summary))) {
            isSummary = true;
        }

        TransRedeemKbankTotal redeemTotal = total.getTransRedeemKbankTotal();

        //if (!isSummary || (isSummary && acqNum == 1) || (isSummary && acqNum == 999)) {//acqNum 999 = summary 1 acq
            Bitmap logo;
//            if (acqNum == 1) {// summary report all acquirer
//                logo = Component.getImageFromInternalFile(Constants.SLIP_LOGO_NAME);
//            } else {// by acquirer
                logo = Component.getImageFromInternalFile(Constants.SLIP_LOGO_NAME, total.getAcquirer().getNii() + "_" + total.getAcquirer().getName());
//            }
            page.addLine()
                    .addUnit(page.createUnit()
                            .setBitmap(logo)
                            .setGravity(Gravity.CENTER));
            page.addLine().addUnit(page.createUnit().setText(" "));


            //merchant name
            SysParam sysParam = FinancialApplication.getSysParam();
            String merName = sysParam.get(SysParam.StringParam.EDC_MERCHANT_NAME_EN);
            String merAddress = sysParam.get(SysParam.StringParam.EDC_MERCHANT_ADDRESS);
            String merAddress1 = sysParam.get(SysParam.StringParam.EDC_MERCHANT_ADDRESS1);
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
                    .setText(Utils.getString(R.string.receipt_double_line))
                    .setScaleX(fullSizeDoubleLine)
                    .setGravity(Gravity.CENTER));
//        }
        //HOST
        String acqName = total.getAcquirer().getName();
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("HOST")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(": " + acqName)
                        .setWeight(3.0f)
                        .setFontSize(FONT_NORMAL));

        // terminal ID/operator ID
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("TID")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(": " + total.getTerminalID())
                        .setWeight(3.0f)
                        .setGravity(Gravity.LEFT)
                );

        // merchant ID
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("MID")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(": " + total.getMerchantID())
                        .setGravity(Gravity.LEFT)
                        .setWeight(3.0f));

        page.addLine().addUnit(page.createUnit()
                .setText(" ")
                .setGravity(Gravity.CENTER));
        // title
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(title)
                        .setFontSize(FONT_BIG)
                        .setTextStyle(Typeface.BOLD));
        page.addLine().addUnit(page.createUnit()
                .setText(" ")
                .setGravity(Gravity.CENTER));

        // batch NO
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_batch_num_short))
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(Component.getPaddedNumber(total.getBatchNo(), 6))
                        .setGravity(Gravity.LEFT)
                        .setWeight(3.0f));


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

        page.addLine().addUnit(page.createUnit()
                .setText(" ")
                .setGravity(Gravity.CENTER));
        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setScaleX(fullSizeOneLine)
                .setGravity(Gravity.CENTER));


   /*     if(isSummary && acqNum > 1 && acqNum != 999){//acqNum 999 = summary 1 acq
            // terminal ID/operator ID
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_terminal_code_short))
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(total.getTerminalID())
                            .setWeight(4.0f)
                            .setGravity(Gravity.LEFT)
                    );

            // merchant ID
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_merchant_code_short))
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(total.getMerchantID())
                            .setGravity(Gravity.LEFT)
                            .setWeight(4.0f));
        }

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setGravity(Gravity.CENTER));
        */

        //PRODUCT
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.settle_redeem_product).toUpperCase())
                        .setFontSize(FONT_NORMAL_26)
                        .setGravity(Gravity.CENTER)
                        .setTextStyle(Typeface.BOLD));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("VISA")
                        .setFontSize(FONT_NORMAL_22))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getProductVisa()))
                        .setFontSize(FONT_NORMAL_22)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("MASTERCARD")
                        .setFontSize(FONT_NORMAL_22))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getProductMastercard()))
                        .setFontSize(FONT_NORMAL_22)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("JCB")
                        .setFontSize(FONT_NORMAL_22))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getProductJcb()))
                        .setFontSize(FONT_NORMAL_22)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("OTHER")
                        .setFontSize(FONT_NORMAL_22))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getProductOther()))
                        .setFontSize(FONT_NORMAL_22)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("ALL CARD")
                        .setFontSize(FONT_NORMAL_22))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getProductAllCard()))
                        .setFontSize(FONT_NORMAL_22)
                        .setGravity(Gravity.END));
        page.addLine()
                .adjustTopSpace(10)
                .addUnit(page.createUnit()
                        .setText("ITEMS")
                        .setFontSize(FONT_NORMAL_22))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getProductItems()))
                        .setFontSize(FONT_NORMAL_22)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("POINTS")
                        .setFontSize(FONT_NORMAL_22))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getProductPoints()))
                        .setFontSize(FONT_NORMAL_22)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("REDEEM BHT")
                        .setFontSize(FONT_NORMAL_22))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(redeemTotal.getProductRedeem()))
                        .setFontSize(FONT_NORMAL_22)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("TOTAL BHT")
                        .setFontSize(FONT_NORMAL_22))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(redeemTotal.getProductTotal()))
                        .setFontSize(FONT_NORMAL_22)
                        .setGravity(Gravity.END));
        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setScaleX(fullSizeOneLine)
                .setGravity(Gravity.CENTER));

        //PRODUCT+CREDIT
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.settle_redeem_product_credit).toUpperCase())
                        .setFontSize(FONT_BIG)
                        .setGravity(Gravity.CENTER)
                        .setTextStyle(Typeface.BOLD));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("VISA")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getProductCreditVisa()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("MASTERCARD")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getProductCreditMastercard()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("JCB")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getProductCreditJcb()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("OTHER")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getProductCreditOther()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("ALL CARD")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getProductCreditAllCard()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .adjustTopSpace(10)
                .addUnit(page.createUnit()
                        .setText("ITEMS")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getProductCreditItems()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("POINTS")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getProductCreditPoints()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("REDEEM BHT")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(redeemTotal.getProductCreditRedeem()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("CREDIT BHT")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(redeemTotal.getProductCreditCredit()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("TOTAL BHT")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(redeemTotal.getProductCreditTotal()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setScaleX(fullSizeOneLine)
                .setGravity(Gravity.CENTER));

        //VOUCHER
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.settle_redeem_voucher).toUpperCase())
                        .setFontSize(FONT_BIG)
                        .setGravity(Gravity.CENTER)
                        .setTextStyle(Typeface.BOLD));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("VISA")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getVoucherVisa()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("MASTERCARD")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getVoucherMastercard()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("JCB")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getVoucherJcb()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("OTHER")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getVoucherOther()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("ALL CARD")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getVoucherAllCard()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .adjustTopSpace(10)
                .addUnit(page.createUnit()
                        .setText("ITEMS")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getVoucherItems()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("POINTS")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getVoucherPoints()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("REDEEM BHT")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(redeemTotal.getVoucherRedeem()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("TOTAL BHT")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(redeemTotal.getVoucherTotal()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setScaleX(fullSizeOneLine)
                .setGravity(Gravity.CENTER));

        //VOUCHER+CREDIT
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.settle_redeem_voucher_credit).toUpperCase())
                        .setFontSize(FONT_BIG)
                        .setGravity(Gravity.CENTER)
                        .setTextStyle(Typeface.BOLD));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("VISA")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getVoucherCreditVisa()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("MASTERCARD")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getVoucherCreditMastercard()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("JCB")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getVoucherCreditJcb()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("OTHER")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getVoucherCreditOther()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("ALL CARD")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getVoucherCreditAllCard()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .adjustTopSpace(10)
                .addUnit(page.createUnit()
                        .setText("ITEMS")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getVoucherCreditItems()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("POINTS")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getVoucherCreditPoints()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("REDEEM BHT")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(redeemTotal.getVoucherCreditRedeem()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("CREDIT BHT")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(redeemTotal.getVoucherCreditCredit()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("TOTAL BHT")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(redeemTotal.getVoucherCreditTotal()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setScaleX(fullSizeOneLine)
                .setGravity(Gravity.CENTER));


        //DISCOUNT
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.settle_redeem_discount).toUpperCase())
                        .setFontSize(FONT_BIG)
                        .setGravity(Gravity.CENTER)
                        .setTextStyle(Typeface.BOLD));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("VISA")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getDiscountVisa()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("MASTERCARD")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getDiscountMastercard()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("JCB")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getDiscountJcb()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("OTHER")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getDiscountOther()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("ALL CARD")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getDiscountAllCard()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .adjustTopSpace(10)
                .addUnit(page.createUnit()
                        .setText("ITEMS")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getDiscountItems()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("POINTS")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getDiscountPoints()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("REDEEM BHT")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(redeemTotal.getDiscountRedeem()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("CREDIT BHT")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(redeemTotal.getDiscountCredit()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("TOTAL BHT")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(redeemTotal.getDiscountTotal()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setScaleX(fullSizeOneLine)
                .setGravity(Gravity.CENTER));

        //GRAND TOTAL
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.settle_redeem_grand_total).toUpperCase())
                        .setFontSize(FONT_BIG)
                        .setGravity(Gravity.CENTER)
                        .setTextStyle(Typeface.BOLD));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("VISA")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getVisaSum()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("MASTERCARD")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getMastercardSum()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("JCB")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getJcbSum()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("OTHER")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getOtherCardSum()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("ALL CARD")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getAllCardsSum()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .adjustTopSpace(10)
                .addUnit(page.createUnit()
                        .setText("ITEMS")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getItemSum()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("POINTS")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(redeemTotal.getPointsSum()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("REDEEM BHT")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(redeemTotal.getRedeemAmtSum()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("CREDIT BHT")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(redeemTotal.getCreditSum()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("TOTAL BHT")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(redeemTotal.getTotalSum()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_double_line))
                .setScaleX(fullSizeDoubleLine)
                .setGravity(Gravity.CENTER));

        // message complete
        if (title.equalsIgnoreCase(Utils.getString(R.string.settle_title))) {
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("*** SETTLEMENT ***")
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.CENTER));
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("CLOSED SUCCESSFULLY")
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.CENTER));

            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.receipt_double_line))
                    .setScaleX(fullSizeDoubleLine)
                    .setGravity(Gravity.CENTER));
        } else if (!isSummary) {
            page.addLine().addUnit(page.createUnit().setText("\n\n"));
        }

        PageToSlipFormat.getInstance().isSettleMode = true;
        PageToSlipFormat.getInstance().Append(page);
        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }
}
