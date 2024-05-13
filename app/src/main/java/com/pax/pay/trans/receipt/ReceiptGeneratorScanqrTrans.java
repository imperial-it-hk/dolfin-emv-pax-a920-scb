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
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.TimeConverter;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

/**
 * @author Minson
 * @Date 2022/04/10
 * @Description receipt for scan trans(sale and void)
 */
public class ReceiptGeneratorScanqrTrans implements IReceiptGenerator {

    private int receiptNo = 0;
    private TransData transData;
    private boolean isRePrint = false;
    private int receiptMax = 2;
    private boolean isPrintPreview = false;
    private boolean isShowQRBarcode = false;

    /**
     * @param transData        ：transData
     * @param currentReceiptNo : currentReceiptNo
     * @param receiptMax       ：generate which one, start from 0
     * @param isReprint        ：is reprint?
     */
    public ReceiptGeneratorScanqrTrans(TransData transData, int currentReceiptNo, int receiptMax, boolean isReprint) {
        this.transData = transData;
        this.receiptNo = currentReceiptNo;
        this.isRePrint = isReprint;
        this.receiptMax = receiptMax;
    }

    public ReceiptGeneratorScanqrTrans(TransData transData, int currentReceiptNo, int receiptMax, boolean isReprint, boolean isPrintPreview) {
        this.transData = transData;
        this.receiptNo = currentReceiptNo;
        this.isRePrint = isReprint;
        this.receiptMax = receiptMax;
        this.isPrintPreview = isPrintPreview;
    }

    @Override
    public Bitmap generateBitmap() {
        // transaction type
        ETransType transType = transData.getTransType();

        // 生成第几张凭单不合法时， 默认0
        if (receiptNo > receiptMax) {
            receiptNo = 0;
        } else if (receiptMax == 1) {
            receiptNo = receiptMax;
        }


        IPage page = Device.generatePage();
        TransData.ETransStatus transStatus = transData.getTransState();

        SysParam sysParam = FinancialApplication.getSysParam();
        Acquirer acquirer = transData.getAcquirer();
        String temp, panMask;



        /*Header*/
        // title
        page.addLine()
                .addUnit(page.createUnit()
                        .setBitmap(Component.getImageFromInternalFile(Constants.SLIP_LOGO_NAME, acquirer.getNii() + "_" + acquirer.getName()))
                        .setGravity(Gravity.CENTER));
        page.addLine().addUnit(page.createUnit().setText(" "));
        if (isRePrint) {
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("----" + Utils.getString(R.string.receipt_print_again) + "----")
                            .setFontSize(FONT_BIG)
                            .setGravity(Gravity.CENTER));
        }

        //merchant name
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
                .setText(Utils.getString(R.string.receipt_one_line))
                .setGravity(Gravity.CENTER));

        /*Body*/
        if (receiptNo == 0) {
            //host
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_host_label))
                            .setFontSize(FONT_NORMAL)
                            .setTextStyle(IPage.ILine.IUnit.BOLD)
                            .setGravity(Gravity.START)
                            .setWeight(1.0f))
                    .addUnit(page.createUnit()
                            .setText(acquirer.getName())
                            .setFontSize(FONT_NORMAL)
                            .setWeight(3.0f)
                            .setGravity(Gravity.START)
                    );
            // terminal ID
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_terminal_code_short))
                            .setFontSize(FONT_NORMAL)
                            .setTextStyle(IPage.ILine.IUnit.BOLD)
                            .setGravity(Gravity.START)
                            .setWeight(1.0f))
                    .addUnit(page.createUnit()
                            .setText(acquirer.getTerminalId())
                            .setFontSize(FONT_NORMAL)
                            .setWeight(3.0f)
                            .setGravity(Gravity.START)
                    );
            // merchant ID
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_merchant_code_short))
                            .setFontSize(FONT_NORMAL)
                            .setTextStyle(IPage.ILine.IUnit.BOLD)
                            .setGravity(Gravity.START)
                            .setWeight(1.0f))
                    .addUnit(page.createUnit()
                            .setText(acquirer.getMerchantId())
                            .setFontSize(FONT_NORMAL)
                            .setWeight(3.0f)
                            .setGravity(Gravity.START)
                    );
            //STAN NO
            String stanno = Component.getPaddedNumber(transData.getStanNo(), 6);
            stanno = (transType == ETransType.SALE && transStatus == TransData.ETransStatus.VOIDED) ? Component.getPaddedNumber(transData.getVoidStanNo(), 6) : stanno;
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_stan_code3))
                            .setFontSize(FONT_NORMAL)
                            .setTextStyle(IPage.ILine.IUnit.BOLD)
                            .setGravity(Gravity.START)
                            .setWeight(1.0f))
                    .addUnit(page.createUnit()
                            .setText(stanno)
                            .setFontSize(FONT_NORMAL)
                            .setWeight(3.0f)
                            .setGravity(Gravity.START)
                    );

            // batch NO
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_batch_num_short))
                            .setFontSize(FONT_NORMAL)
                            .setTextStyle(IPage.ILine.IUnit.BOLD)
                            .setGravity(Gravity.START)
                            .setWeight(1.0f))
                    .addUnit(page.createUnit()
                            .setText(Component.getPaddedNumber(transData.getBatchNo(), 6))
                            .setFontSize(FONT_NORMAL)
                            .setWeight(3.0f)
                            .setGravity(Gravity.START)
                    );

            //trace NO
            String traceno = Component.getPaddedNumber(transData.getTraceNo(), 6);

            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_trans_no_short))
                            .setFontSize(FONT_NORMAL)
                            .setTextStyle(IPage.ILine.IUnit.BOLD)
                            .setGravity(Gravity.START)
                            .setWeight(1.0f))
                    .addUnit(page.createUnit()
                            .setText(traceno)
                            .setFontSize(FONT_NORMAL)
                            .setWeight(3.0f)
                            .setGravity(Gravity.START)
                    );
            // single line
            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.receipt_sign_line))
                    .setGravity(Gravity.CENTER));

            // date/time
            String formattedDate = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                    Constants.DATE_PATTERN_DISPLAY);

            String formattedTime = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                    Constants.TIME_PATTERN_DISPLAY4);

            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(formattedDate)
                            .setFontSize(FONT_NORMAL)
                            .setWeight(1)
                            .setGravity(Gravity.START))
                    .addUnit(page.createUnit()
                            .setText(formattedTime)
                            .setFontSize(FONT_NORMAL)
                            .setWeight(1)
                            .setGravity(Gravity.END));

            //APP CODE
            temp = transData.getAuthCode();
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_app_code))
                            .setFontSize(FONT_NORMAL)
                            .setWeight(1)
                            .setGravity(Gravity.START))
                    .addUnit(page.createUnit()
                            .setText(temp)
                            .setFontSize(FONT_NORMAL)
                            .setWeight(1)
                            .setGravity(Gravity.END));

            //blank line
            page.addLine().addUnit(page.createUnit().setText(" "));

            //trans type
            String transName = "";
            if (transStatus.equals(TransData.ETransStatus.VOIDED) || transType == ETransType.VOID || transType == ETransType.QR_VOID_ALIPAY || transType == ETransType.QR_VOID_WECHAT) {
                transName = "VOID";
            } else {
                transName = "SALE";
            }
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(transName)
                            .setFontSize(FONT_BIG)
                            .setTextStyle(IPage.ILine.IUnit.BOLD)
                            .setGravity(Gravity.START));

//            if (transData.getTransType() == ETransType.QR_ALIPAY_SCAN || transData.getTransType() == ETransType.QR_WECHAT_SCAN) {
////                if (transStatus.equals(TransData.ETransStatus.VOIDED) || transType == ETransType.VOID || transType == ETransType.QR_VOID_ALIPAY || transType == ETransType.QR_VOID_WECHAT) {
//                page.addLine()
//                        .addUnit(page.createUnit()
//                                .setText(Utils.getString(R.string.receipt_qr_sale))
//                                .setFontSize(FONT_BIG)
//                                .setTextStyle(IPage.ILine.IUnit.BOLD)
//                                .setGravity(Gravity.START));
//            }else if (transData.getTransType() == ETransType.QR_VOID_ALIPAY || transData.getTransType() == ETransType.QR_VOID_WECHAT) {
//                page.addLine()
//                        .addUnit(page.createUnit()
//                                .setText(Utils.getString(R.string.receipt_qr_void))
//                                .setFontSize(FONT_BIG)
//                                .setTextStyle(IPage.ILine.IUnit.BOLD)
//                                .setGravity(Gravity.START));
//            }

            // trans id.
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_trans_id_2))
                            .setFontSize(FONT_NORMAL)
                            .setWeight(1)
                            .setGravity(Gravity.START))
                    .addUnit(page.createUnit()
                            .setText(transData.getRefNo())
                            .setFontSize(FONT_NORMAL)
                            .setWeight(1)
                            .setGravity(Gravity.END));

            //blank line
            page.addLine().addUnit(page.createUnit().setText(" "));

            //CHANNEL
            switch (transData.getAcquirer().getName()) {
                case Constants.ACQ_KPLUS:
                    temp = Utils.getString(R.string.receipt_kplus);
                    break;
                case Constants.ACQ_ALIPAY:
                case Constants.ACQ_ALIPAY_B_SCAN_C:
                    temp = Utils.getString(R.string.receipt_alipay);
                    break;
                case Constants.ACQ_WECHAT:
                case Constants.ACQ_WECHAT_B_SCAN_C:
                    temp = Utils.getString(R.string.receipt_wechat);
                    break;
            }
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_channel_colon))
                            .setFontSize(FONT_NORMAL)
                            .setWeight(1)
                            .setGravity(Gravity.START))
                    .addUnit(page.createUnit()
                            .setText(temp)
                            .setFontSize(FONT_NORMAL)
                            .setWeight(1)
                            .setGravity(Gravity.END));

            // amount
            long amount = Utils.parseLongSafe(transData.getAmount(), 0);
            if (transType.isSymbolNegative() || transData.getTransState() == TransData.ETransStatus.VOIDED)
                amount = -amount;
            temp = CurrencyConverter.convert(amount, transData.getCurrency());
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_amount_total))
                            .setFontSize(FONT_NORMAL)
                            .setTextStyle(IPage.ILine.IUnit.BOLD)
                            .setGravity(Gravity.START)
                            .setWeight(1.0f))
                    .addUnit(page.createUnit()
                            .setText(temp)
                            .setFontSize(FONT_NORMAL)
                            .setTextStyle(IPage.ILine.IUnit.BOLD)
                            .setGravity(Gravity.END)
                            .setWeight(1.0f));
            page.addLine().addUnit(page.createUnit().setText(" "));

            //pay time
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_pay_time_colon))
                            .setFontSize(FONT_SMALL)
                            .setWeight(1)
                            .setGravity(Gravity.START))
                    .addUnit(page.createUnit()
                            .setText(formattedDate + " " + formattedTime)
                            .setFontSize(FONT_SMALL)
                            .setWeight(1)
                            .setGravity(Gravity.END));

            // single line
            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.receipt_sign_line))
                    .setGravity(Gravity.CENTER));


            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.receipt_no_sig_required))
                    .setGravity(Gravity.CENTER));

        } else { //  customer copy

            //TID /MID
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_terminal_code_short) + acquirer.getTerminalId())
                            .setFontSize(FONT_SMALL_18)
                            .setWeight(1.0f)
                            .setGravity(Gravity.START))
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_merchant_code_short) + acquirer.getMerchantId())
                            .setFontSize(FONT_SMALL_18)
                            .setWeight(1.0f)
                            .setGravity(Gravity.END));

            //TRACE / BATCH
            temp = Component.getPaddedNumber(transData.getTraceNo(), 6);
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_trans_no_short) + temp)
                            .setFontSize(FONT_SMALL_18).setWeight(1.0f)
                            .setGravity(Gravity.START)
                    )
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_batch_num_short) + Component.getPaddedNumber(transData.getBatchNo(), 6))
                            .setFontSize(FONT_SMALL_18)
                            .setWeight(1.0f)
                            .setGravity(Gravity.END)
                    );

            //stan /host
            //STAN NO
            String stanno = Component.getPaddedNumber(transData.getStanNo(), 6);
            stanno = (transType == ETransType.SALE && transStatus == TransData.ETransStatus.VOIDED) ? Component.getPaddedNumber(transData.getVoidStanNo(), 6) : stanno;

            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_stan_number) + stanno)
                            .setFontSize(FONT_SMALL_18).setWeight(1.0f)
                            .setGravity(Gravity.START)
                    )
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_host_label) + acquirer.getName())
                            .setFontSize(FONT_SMALL_18)
                            .setWeight(1.0f)
                            .setGravity(Gravity.END)
                    );


            //app.code / Datetime
            String formattedTime = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                    Constants.TIME_PATTERN_DISPLAY3);
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_app_code_colon) + transData.getAuthCode())
                            .setFontSize(FONT_SMALL_18).setWeight(1.0f)
                            .setGravity(Gravity.START)
                    )
                    .addUnit(page.createUnit()
                            .setText(formattedTime)
                            .setFontSize(FONT_SMALL_18)
                            .setWeight(1.0f)
                            .setGravity(Gravity.END)
                    );

            //double dash line

            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.receipt_double_line))
                    .setGravity(Gravity.CENTER));

            //TransType
            String transName = "";
            if (transStatus.equals(TransData.ETransStatus.VOIDED) || transType == ETransType.VOID || transType == ETransType.QR_VOID_ALIPAY || transType == ETransType.QR_VOID_WECHAT) {
                transName = "VOID";
            } else {
                transName = "SALE";
            }
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(transName)
                            .setFontSize(FONT_BIG)
                            .setTextStyle(IPage.ILine.IUnit.BOLD)
                            .setGravity(Gravity.START));
//            if (transData.getTransType() == ETransType.QR_ALIPAY_SCAN || transData.getTransType() == ETransType.QR_WECHAT_SCAN) {
//                if (transStatus.equals(TransData.ETransStatus.VOIDED) || transType == ETransType.VOID) {
//                    page.addLine()
//                            .addUnit(page.createUnit()
//                                    .setText(Utils.getString(R.string.receipt_qr_void))
//                                    .setFontSize(FONT_NORMAL)
//                                    .setTextStyle(IPage.ILine.IUnit.BOLD)
//                                    .setGravity(Gravity.START));
//                } else {
//                    page.addLine()
//                            .addUnit(page.createUnit()
//                                    .setText(Utils.getString(R.string.receipt_qr_sale))
//                                    .setFontSize(FONT_NORMAL)
//                                    .setTextStyle(IPage.ILine.IUnit.BOLD)
//                                    .setGravity(Gravity.START));
//                }
//            }

            // trans id.
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_trans_id))
                            .setFontSize(FONT_SMALL_18)
                            .setWeight(1)
                            .setGravity(Gravity.START))
                    .addUnit(page.createUnit()
                            .setText(transData.getRefNo())
                            .setFontSize(FONT_SMALL_18)
                            .setWeight(1)
                            .setGravity(Gravity.END));

            // amount
            long amount = Utils.parseLongSafe(transData.getAmount(), 0);
            if (transType.isSymbolNegative() || transData.getTransState() == TransData.ETransStatus.VOIDED)
                amount = -amount;
            temp = CurrencyConverter.convert(amount, transData.getCurrency());
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_amount_total))
                            .setFontSize(FONT_NORMAL)
                            .setTextStyle(IPage.ILine.IUnit.BOLD)
                            .setGravity(Gravity.START)
                            .setWeight(1.0f))
                    .addUnit(page.createUnit()
                            .setText(temp)
                            .setFontSize(FONT_NORMAL)
                            .setTextStyle(IPage.ILine.IUnit.BOLD)
                            .setGravity(Gravity.END)
                            .setWeight(1.0f));


            //pay time
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_pay_time_colon))
                            .setFontSize(FONT_SMALL)
                            .setWeight(1)
                            .setGravity(Gravity.START))
                    .addUnit(page.createUnit()
                            .setText(formattedTime)
                            .setFontSize(FONT_SMALL)
                            .setWeight(1)
                            .setGravity(Gravity.END));

            page.addLine().addUnit(page.createUnit().setText(" "));

        }


        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_verify))
                .setFontSize(FONT_SMALL)
                .setGravity(Gravity.CENTER));

        Component.genAppVersiononSlip(page);

        page.addLine().adjustTopSpace(5)
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_no_refund))
                        .setFontSize(FONT_NORMAL_22)
                        .setGravity(Gravity.CENTER));


        if (receiptNo == 0) {
            page.addLine()
                    .adjustTopSpace(5)
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_stub_merchant))
                            .setGravity(Gravity.CENTER));
        } else {
            page.addLine()
                    .adjustTopSpace(5)
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_stub_cust))
                            .setGravity(Gravity.CENTER));
        }

        if (Component.isDemo()) {
            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.demo_mode))
                    .setGravity(Gravity.CENTER));
        }

        if (!isPrintPreview) {
            page.addLine().addUnit(page.createUnit().setText("\n\n\n\n"));
        }

        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }


    @Override
    public String generateString() {
        return "Card No:" + transData.getPan() + "\nTrans Type:" + transData.getTransType().toString()
                + "\nAmount:" + CurrencyConverter.convert(Utils.parseLongSafe(transData.getAmount(), 0), transData.getCurrency())
                + "\nTip:" + CurrencyConverter.convert(Utils.parseLongSafe(transData.getTipAmount(), 0), transData.getCurrency())
                + "\nTransData:" + transData.getDateTime();
    }

}
