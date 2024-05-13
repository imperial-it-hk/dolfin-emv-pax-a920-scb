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
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.util.ArrayList;
import java.util.List;

/**
 * transaction detail generator
 *
 * @author Steven.W
 */
class ReceiptGeneratorTransDetail implements IReceiptGenerator {

    private List<TransData> transDataList;

    /**
     * generate transaction detail, use secondary construction method
     *
     * @param transDataList ：transDataList
     */
    public ReceiptGeneratorTransDetail(List<TransData> transDataList) {
        this.transDataList = transDataList;
    }

    /**
     * generate detail main information, use this construction method
     */
    public ReceiptGeneratorTransDetail() {
        //do nothing
    }

    @Override
    public Bitmap generateBitmap() {
        IPage page = Device.generatePage();

        for (TransData transData : transDataList) {
            Component.generateTransDetail(transData, page, FONT_SMALL);
        }

        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }

    public Bitmap generateBitmapPromptPay() {
        IPage page = Device.generatePage();

        for (TransData transData : transDataList) {
            Component.generateTransDetailPromptPay(transData, page, FONT_SMALL);
        }

        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }

    public Bitmap generateBitmapWallet() {
        IPage page = Device.generatePage();

        for (TransData transData : transDataList) {
            Component.generateTransDetailWallet(transData, page, FONT_SMALL);
        }

        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }

    public Bitmap generateBitmapKbank() {
        IPage page = Device.generatePage();

        for (TransData transData : transDataList) {
            Component.generateTransDetailWalletKbank(transData, page, FONT_SMALL);
        }

        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }

    public Bitmap generateBitmapQRSale() {
        IPage page = Device.generatePage();

        for (TransData transData : transDataList) {
            Component.generateTransDetailQRSale(transData, page, FONT_SMALL);
        }

        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }

    public Bitmap generateBitmapKbankRedeem() {
        IPage page = Device.generatePage();

        for (TransData transData : transDataList) {
            Component.generateTransDetailKbankRedeem(transData, page, FONT_SMALL);
        }

        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }

    public Bitmap generateBitmapInstalmentKbank() {
        IPage page = Device.generatePage();

        for (TransData transData : transDataList) {
            Component.generateTransDetailInstalmentKbank(transData, page, FONT_NORMAL);
        }

        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }

    public Bitmap generateBitmapDccKbank() {
        IPage page = Device.generatePage();

        Component.generateTransDetailDccKbank(transDataList, page);


        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }

    public Bitmap generateBitmapInstalmentAmex() {
        IPage page = Device.generatePage();

        for (TransData transData : transDataList) {
            Component.generateTransDetailAmexInstalment(transData, page, FONT_SMALL);
        }


        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }

    public Bitmap generateBitmapInstalmentBay() {
        IPage page = Device.generatePage();

        for (TransData transData : transDataList) {
            Component.generateTransDetailInstalmentBay(transData, page, FONT_SMALL);
        }


        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }


    public Bitmap generateBitmapAllAcquirer() {
        IPage page = Device.generatePage();
        List<TransData> transDataDccList = new ArrayList<>();

        for (TransData transData : transDataList) {
            String acquirerName = transData.getAcquirer().getName();
            if (Constants.ACQ_QR_PROMPT.equals(acquirerName)) {
                Component.generateTransDetailPromptPay(transData, page, FONT_SMALL);
            } else if (Constants.ACQ_WALLET.equals(acquirerName)) {
                Component.generateTransDetailWallet(transData, page, FONT_SMALL);
            } else if (Constants.ACQ_QRC.equals(acquirerName)) {
                Component.generateTransDetailQRSale(transData, page, FONT_SMALL);
            } else if (Constants.ACQ_KPLUS.equals(acquirerName)
                    || Constants.ACQ_ALIPAY.equals(acquirerName)
                    || Constants.ACQ_WECHAT.equals(acquirerName)
                    || Constants.ACQ_QR_CREDIT.equals(acquirerName)) {
                Component.generateTransDetailWalletKbank(transData, page, FONT_SMALL);
            } else if (Constants.ACQ_REDEEM.equals(acquirerName) || Constants.ACQ_REDEEM_BDMS.equals(acquirerName)) {
                Component.generateTransDetailKbankRedeem(transData, page, FONT_SMALL);
            } else if (Constants.ACQ_SMRTPAY.equals(acquirerName)
                    || Constants.ACQ_SMRTPAY_BDMS.equals(acquirerName)
                    || Constants.ACQ_DOLFIN_INSTALMENT.equals(acquirerName)) {
                Component.generateTransDetailInstalmentKbank(transData, page, FONT_SMALL);
            } else if (Constants.ACQ_DCC.equals(acquirerName)) {
                transDataDccList.add(transData);//Keep DCC trans for print as last section
            } else if (Constants.ACQ_AMEX_EPP.equals(acquirerName)) {
                Component.generateTransDetailAmexInstalment(transData, page, FONT_SMALL);
            } else if (Constants.ACQ_BAY_INSTALLMENT.equals(acquirerName)) {
                Component.generateTransDetailInstalmentBay(transData, page, FONT_SMALL);
            } else {
                Component.generateTransDetail(transData, page, FONT_SMALL);
            }
        }

        //print DCC trans detail
        if (transDataDccList.size() > 0) {
            Component.generateTransDetailDccKbank(transDataDccList, page);
        }


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
     * @param title
     * @return
     */
    public Bitmap generateMainInfo(String title, Acquirer acquirer) {//Added Acquirer parameter to support PromptPay.
        IPage page = Device.generatePage();

//        Acquirer acquirer = FinancialApplication.getAcqManager().getCurAcq();
        boolean isAllAcqs = Utils.getString(R.string.acq_all_acquirer).equals(acquirer.getName());

        /*-------------------------------- Header (logo) --------------------------------*/
        SysParam sysParam = FinancialApplication.getSysParam();
        // title
        Bitmap logo;
        if (isAllAcqs) {
            logo = Component.getImageFromInternalFile(Constants.SLIP_LOGO_NAME);
        } else {
            logo = Component.getImageFromInternalFile(Constants.SLIP_LOGO_NAME, acquirer.getNii() + "_" + acquirer.getName());
        }
        page.addLine()
                .addUnit(page.createUnit()
                        .setBitmap(logo)
                        .setGravity(Gravity.CENTER));
        page.addLine().addUnit(page.createUnit().setText(" "));

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

        // title
        page.addLine().addUnit(page.createUnit().setText(title).setFontSize(FONT_BIG).setTextStyle(Typeface.BOLD).setGravity(Gravity.CENTER));

        if (isAllAcqs) {
            page.addLine().addUnit(page.createUnit().setText("ALL ACQUIRERS").setFontSize(FONT_SMALL).setTextStyle(Typeface.BOLD).setGravity(Gravity.CENTER));
        }

//        // date/time
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

        if (!isAllAcqs) {
            String merchId = acquirer.getMerchantId();
            //  merchant ID
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_merchant_code))
                            .setFontSize(FONT_SMALL)
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(merchId)
                            .setGravity(Gravity.END)
                            .setWeight(3.0f));
            // terminal ID/operator ID
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_terminal_code))
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
                            .setText(Component.getPaddedNumber(acquirer.getCurrBatchNo(), 6))
                            .setFontSize(FONT_SMALL)
                            .setGravity(Gravity.END));
        }

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setGravity(Gravity.CENTER));


        // transaction information
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.audit_report_host_name))
                        .setFontSize(FONT_SMALL)
                        .setWeight(2.0f))
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.audit_report_host_nii))
                        .setFontSize(FONT_SMALL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.audit_report_trans_type))
                        .setFontSize(FONT_SMALL)
                        .setWeight(2.0f))
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_auth_code_short))
                        .setFontSize(FONT_SMALL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.audit_report_trace_number) + "                  " + Utils.getString(R.string.audit_report_stan_number))
                        .setFontSize(FONT_SMALL)
                        .setWeight(2.0f))
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.audit_report_batch_number))
                        .setFontSize(FONT_SMALL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_date_short))
                        .setFontSize(FONT_SMALL))
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_time_short))
                        .setFontSize(FONT_SMALL)
                        .setGravity(Gravity.END));

        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_card_no))
                        .setFontSize(FONT_SMALL)
                        .setWeight(2.0f))
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_card_type))
                        .setFontSize(FONT_SMALL)
                        .setGravity(Gravity.END));

        if (Constants.ACQ_REDEEM.equals(acquirer.getName()) || Constants.ACQ_REDEEM_BDMS.equals(acquirer.getName())) {
            page.addLine()
                    .addUnit(page.createUnit()
                            .setFontSize(FONT_SMALL)
                            .setText(Utils.getString(R.string.receipt_product_code)))
                    .addUnit(page.createUnit()
                            .setFontSize(FONT_SMALL)
                            .setText(Utils.getString(R.string.receipt_product_code))
                            .setGravity(Gravity.END));
            page.addLine()
                    .addUnit(page.createUnit()
                            .setFontSize(FONT_SMALL)
                            .setText(Utils.getString(R.string.receipt_redeem_detail)))
                    .addUnit(page.createUnit()
                            .setFontSize(FONT_SMALL)
                            .setText(Utils.getString(R.string.receipt_redeem_detail))
                            .setGravity(Gravity.END));
        } else {
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_amount_total))
                            .setFontSize(FONT_SMALL)
                            .setGravity(Gravity.END));
        }

        if (Constants.ACQ_DCC.equals(acquirer.getName())) {
            //Additional DCC description
            page.addLine()
                    .addUnit(page.createUnit()
                            .setFontSize(FONT_SMALL)
                            .setText(Utils.getString(R.string.receipt_dcc_ex_rate)))
                    .addUnit(page.createUnit()
                            .setFontSize(FONT_SMALL)
                            .setText(Utils.getString(R.string.receipt_dcc_ex_rate))
                            .setGravity(Gravity.END));

            page.addLine()
                    .addUnit(page.createUnit()
                            .setFontSize(FONT_SMALL)
                            .setText("CURR " + Utils.getString(R.string.receipt_amount_total))
                            .setGravity(Gravity.END));
        }
//        page.addLine()
//                .addUnit(page.createUnit()
//                        .setText(Utils.getString(R.string.receipt_voucher))
//                        .setFontSize(FONT_SMALL))
//                .addUnit(page.createUnit()
//                        .setText(Utils.getString(R.string.receipt_date))
//                        .setFontSize(FONT_SMALL)
//                        .setGravity(Gravity.END));
//        page.addLine()
//                .addUnit(page.createUnit()
//                        .setText(Utils.getString(R.string.receipt_type))
//                        .setFontSize(FONT_SMALL))
//                .addUnit(page.createUnit()
//                        .setText(Utils.getString(R.string.receipt_amount))
//                        .setFontSize(FONT_SMALL)
//                        .setGravity(Gravity.END));
//
//        if(acquirer.getName().equals(Constants.ACQ_QR_PROMPT)){
//            page.addLine()
//                    .addUnit(page.createUnit()
//                            .setText(Utils.getString(R.string.receipt_trans_id))
//                            .setFontSize(FONT_SMALL)
//                            .setWeight(2.0f))
//                    .addUnit(page.createUnit()
//                            .setText(Utils.getString(R.string.receipt_app_code))
//                            .setFontSize(FONT_SMALL)
//                            .setGravity(Gravity.END));
//        }else {
//            page.addLine()
//                    .addUnit(page.createUnit()
//                            .setText(Utils.getString(R.string.receipt_card_no))
//                            .setFontSize(FONT_SMALL)
//                            .setWeight(2.0f))
//                    .addUnit(page.createUnit()
//                            .setText(Utils.getString(R.string.receipt_auth_code))
//                            .setFontSize(FONT_SMALL)
//                            .setGravity(Gravity.END));
//        }

        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }

}
