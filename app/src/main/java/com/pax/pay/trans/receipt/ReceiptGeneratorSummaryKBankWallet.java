package com.pax.pay.trans.receipt;

import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.view.Gravity;

import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.glwrapper.imgprocessing.IImgProcessing;
import com.pax.glwrapper.page.IPage;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Issuer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.TransTotal;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.Utils;

import java.util.ArrayList;
import java.util.List;

class ReceiptGeneratorSummaryKBankWallet extends AReceiptGenerator {

    private boolean showHeader = true;
    private boolean showFooter = true;
    private String outIssuer = null;

    public ReceiptGeneratorSummaryKBankWallet(TransTotal total, String title) {
        super(total, title);
    }

    public ReceiptGeneratorSummaryKBankWallet(TransTotal total, boolean showHeader) {
        super(total, Utils.getString(R.string.print_history_summary).toUpperCase());
        this.showHeader = showHeader;
    }

    public ReceiptGeneratorSummaryKBankWallet(TransTotal total, boolean showHeader, boolean showFooter, String outIssuer) {
        super(total, Utils.getString(R.string.print_history_summary).toUpperCase());
        this.showHeader = showHeader;
        this.showFooter = showFooter;
        this.outIssuer = outIssuer;
    }


    /**
     * Generate bitmap of Summary Report For Alipay, WeChat, and QR Credit
     * @return List of Bitmap
     */
    @Override
    public List<Bitmap> generateBitmaps() throws Exception {
        List<Bitmap> bitmaps = new ArrayList<>();

        try {
            IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();

            switch (total.getAcquirer().getName()) {
                case Constants.ACQ_ALIPAY:
                case Constants.ACQ_ALIPAY_B_SCAN_C:
                case Constants.ACQ_WECHAT:
                case Constants.ACQ_WECHAT_B_SCAN_C:
                case Constants.ACQ_MY_PROMPT:
                    bitmaps.add(imgProcessing.pageToBitmap(generateHeader(), 384));
                    generateSummaryAliWeChat(bitmaps);
                    break;
                case Constants.ACQ_QR_CREDIT:
                    bitmaps.add(imgProcessing.pageToBitmap(generateHeader(), 384));
                    generateSummaryQRCredit(bitmaps);
                    break;
                case Constants.ACQ_KPLUS:
                    if (showHeader) {
                        bitmaps.add(imgProcessing.pageToBitmap(super.generateHeader(), 384));
                    }
                    //generateSummaryKPlusPromptPay(bitmaps);

                    ReceiptGeneratorDetailKBankWallet receiptQrTag31 = new ReceiptGeneratorDetailKBankWallet(total, title);
                    receiptQrTag31.printQrTag31ByQRSourceOfFund(bitmaps, false);
                    break;
            }

            if (showFooter) {
                if (!total.getAcquirer().getName().equals(Constants.ACQ_KPLUS)) {
                    bitmaps.add(imgProcessing.pageToBitmap(generateFooter(), 384));
                }
            }
        } catch (Exception e) {
            throw new Exception("Generate Bitmap fail", e);
        }

        return bitmaps;
    }

    @Override
    public IPage generateHeader() {
        IPage page;

        if (showHeader) {
            page = super.generateHeader();
        } else {
            page = Device.generatePage();
        }

        // CHANNEL For Alipay, WeChat, QR Credit
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_channel_colon).toUpperCase() + " " + total.getAcquirer().getName().replaceAll("_", " "))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.START)
                        .setTextStyle(Typeface.BOLD));
        page.addLine().addUnit(page.createUnit().setText(" "));

        return page;
    }

    private void generateSummaryAliWeChat(List<Bitmap> bitmaps) {
        IPage page = Device.generatePage();

        // SALE
        addPageTotalByIssuer(page, Utils.getString(R.string.trans_sale).toUpperCase(), total.getSaleTotalNum(), CurrencyConverter.convert(total.getSaleTotalAmt()));

        // VOID
        addPageTotalByIssuer(page, Utils.getString(R.string.trans_void).toUpperCase(), total.getSaleVoidTotalNum(), CurrencyConverter.convert(-total.getSaleVoidTotalAmt()));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setGravity(Gravity.CENTER));

        // TOTAL
        addPageTotalByIssuer(page, Utils.getString(R.string.receipt_amount_total).toUpperCase(), total.getSaleTotalNum(), CurrencyConverter.convert(total.getSaleTotalAmt()));
        page.addLine().addUnit(page.createUnit().setText(" "));

        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        bitmaps.add(imgProcessing.pageToBitmap(page, 384));
    }

    private void generateSummaryQRCredit(List<Bitmap> bitmaps) {
        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();

        List<Issuer> issuerByAcquirer = FinancialApplication.getAcqManager().findIssuerByAcquirerName(total.getAcquirer().getName());

        List<Issuer> issuers = new ArrayList<>(0);
        if (issuerByAcquirer != null) {
            for (Issuer issuer : issuerByAcquirer) {
                long[] tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(total.getAcquirer(), filter, issuer);
                if (tempObj[0] != 0 || tempObj[1] != 0) {
                    issuers.add(issuer);
                }
            }
        }

        int tranSize = issuers.size();
        int totalPage = (int) Math.ceil((double) tranSize / MAX_SIZE);

        IPage page;
        Issuer issuer;
        int transNo = 0, transPerPage, j;
        long[] sale, voidSale;

        for (int i = 1; i <= totalPage; i++) {
            page = Device.generatePage();
            transPerPage = (transPerPage = i * MAX_SIZE) > tranSize ? tranSize : transPerPage;
            for (j = transNo; j < transPerPage; j++) {
                issuer = issuers.get(j);
                // card name : issuer name
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText("CARD NAME : " + issuer.getIssuerName().replaceFirst("QRC_", ""))
                                .setFontSize(FONT_NORMAL));
                page.addLine().addUnit(page.createUnit().setText(" "));

                // SALE
                sale = getTotalSaleByIssuer(issuer);
                addPageTotalByIssuer(page, Utils.getString(R.string.trans_sale).toUpperCase(), sale[0], CurrencyConverter.convert(sale[1]));

                // VOID
                voidSale = getTotalVoidSaleByIssuer(issuer);
                addPageTotalByIssuer(page, Utils.getString(R.string.trans_void).toUpperCase(), voidSale[0], CurrencyConverter.convert(-voidSale[1]));

                page.addLine().addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_one_line))
                        .setGravity(Gravity.CENTER));

                // TOTAL
                addPageTotalByIssuer(page, Utils.getString(R.string.receipt_amount_total).toUpperCase(), sale[0], CurrencyConverter.convert(sale[1]));
                page.addLine().addUnit(page.createUnit().setText(" "));

                page.addLine().addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_double_line))
                        .setGravity(Gravity.CENTER));
            }

            transNo = j;
            bitmaps.add(imgProcessing.pageToBitmap(page, 384));
        }
    }

    private void generateSummaryKPlusPromptPay(List<Bitmap> bitmaps) {
        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();

        List<Issuer> issuers = new ArrayList<>(0);
        if (outIssuer == null) {
            List<Issuer> issuerByAcquirer = FinancialApplication.getAcqManager().findIssuerByAcquirerName(total.getAcquirer().getName());

            if (issuerByAcquirer != null) {
                for (Issuer issuer : issuerByAcquirer) {
                    long[] tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(total.getAcquirer(), filter, issuer);
                    if (tempObj[0] != 0 || tempObj[1] != 0) {
                        issuers.add(issuer);
                    }
                }
            }
        } else {
            Issuer issuer = FinancialApplication.getAcqManager().findIssuer(outIssuer);
            if (issuer != null) {
                issuers.add(issuer);
            }
        }

        int tranSize = issuers.size();
        int totalPage = (int) Math.ceil((double) tranSize / MAX_SIZE);

        IPage page;
        Issuer issuer;
        int transNo = 0, transPerPage, j;
        long[] sale, voidSale;

        for (int i = 1; i <= totalPage; i++) {
            page = Device.generatePage();
            transPerPage = (transPerPage = i * MAX_SIZE) > tranSize ? tranSize : transPerPage;
            for (j = transNo; j < transPerPage; j++) {
                issuer = issuers.get(j);
                // card name : issuer name
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(issuer.getIssuerName())
                                .setGravity(Gravity.CENTER)
                                .setFontSize(FONT_NORMAL));

                page.addLine().addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_double_line))
                        .setGravity(Gravity.CENTER));

                // SALE
                sale = getTotalSaleByIssuer(issuer);
                addPageTotalByIssuer(page, Utils.getString(R.string.trans_sale).toUpperCase(), sale[0], CurrencyConverter.convert(sale[1]));

                // VOID
                voidSale = getTotalVoidSaleByIssuer(issuer);
                addPageTotalByIssuer(page, Utils.getString(R.string.trans_void).toUpperCase(), voidSale[0], CurrencyConverter.convert(-voidSale[1]));

                page.addLine().addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_one_line))
                        .setGravity(Gravity.CENTER));

                // TOTAL
                addPageTotalByIssuer(page, Utils.getString(R.string.receipt_amount_total).toUpperCase(), sale[0], CurrencyConverter.convert(sale[1]));
                page.addLine().addUnit(page.createUnit().setText(" "));

                page.addLine().addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_double_line))
                        .setGravity(Gravity.CENTER));
            }

            transNo = j;
            bitmaps.add(imgProcessing.pageToBitmap(page, 384));
        }
    }

    @Override
    public IPage generateFooter() {
        IPage page = Device.generatePage();

        // GRAND TOTAL
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.grand_total).toUpperCase())
                        .setFontSize(FONT_BIG)
                        .setGravity(Gravity.CENTER)
                        .setTextStyle(Typeface.BOLD));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_double_line))
                .setGravity(Gravity.CENTER));
        page.addLine().addUnit(page.createUnit().setText(" "));

        // SALE
        addPageTotalByIssuer(page, Utils.getString(R.string.trans_sale).toUpperCase(), total.getSaleTotalNum(), CurrencyConverter.convert(total.getSaleTotalAmt()));

        // VOID
        addPageTotalByIssuer(page, Utils.getString(R.string.trans_void).toUpperCase(), total.getSaleVoidTotalNum(), CurrencyConverter.convert(-total.getSaleVoidTotalAmt()));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setGravity(Gravity.CENTER));

        // TOTAL
        addPageTotalByIssuer(page, Utils.getString(R.string.receipt_amount_total).toUpperCase(), total.getSaleTotalNum(), CurrencyConverter.convert(total.getSaleTotalAmt()));
        page.addLine().addUnit(page.createUnit().setText(" "));

        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_end_of_report))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.CENTER)
                        .setTextStyle(Typeface.BOLD));

        return page;
    }

    @Override
    public Bitmap generateBitmap() {
        return null;
    }

    @Override
    public String generateString() {
        return null;
    }
}
