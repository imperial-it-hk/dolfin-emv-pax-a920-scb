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
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.TransTotal;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.Utils;

import java.util.ArrayList;
import java.util.List;

class ReceiptGeneratorSummary extends AReceiptGenerator {

    private boolean showHeader = true;

    public ReceiptGeneratorSummary(TransTotal total, String title) {
        super(total, title);
    }

    public ReceiptGeneratorSummary(TransTotal total, boolean showHeader) {
        super(total, Utils.getString(R.string.print_history_summary).toUpperCase());
        this.showHeader = showHeader;
    }

    @Override
    public List<Bitmap> generateBitmaps() throws Exception {
        List<Bitmap> bitmaps = new ArrayList<>();

        try {
            IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();

            if (showHeader) {
                bitmaps.add(imgProcessing.pageToBitmap(generateHeader(), 384));
            }

            generateTotalByIssuer(bitmaps);
            bitmaps.add(imgProcessing.pageToBitmap(generateFooter(), 384));
        } catch (Exception e) {
            throw new Exception("Generate Bitmap fail", e);
        }

        return bitmaps;
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
        long saleNumExcludeOffline = total.getSaleTotalNum() - total.getOfflineTotalNum();
        long saleAmtExcludeOffline = total.getSaleTotalAmt() - total.getOfflineTotalAmt();
        addPageTotalByIssuer(page, Utils.getString(R.string.trans_sale).toUpperCase(),
                saleNumExcludeOffline < 0 ? -(saleNumExcludeOffline) : saleNumExcludeOffline,
                CurrencyConverter.convert(saleAmtExcludeOffline < 0 ? -(saleAmtExcludeOffline) : saleAmtExcludeOffline));

        // OFFLINE
        addPageTotalByIssuer(page, Utils.getString(R.string.trans_offline).toUpperCase(), total.getOfflineTotalNum(), CurrencyConverter.convert(total.getOfflineTotalAmt()));

        // VOID
        addPageTotalByIssuer(page, Utils.getString(R.string.trans_void).toUpperCase(), total.getSaleVoidTotalNum(), CurrencyConverter.convert(-total.getSaleVoidTotalAmt()));

        // REFUND
        addPageTotalByIssuer(page, Utils.getString(R.string.trans_refund).toUpperCase(), total.getRefundTotalNum(), CurrencyConverter.convert(-total.getRefundTotalAmt()));

        // VOID REFUND
        addPageTotalByIssuer(page, (Utils.getString(R.string.trans_void) + " " + Utils.getString(R.string.trans_refund)).toUpperCase(), total.getRefundVoidTotalNum(), CurrencyConverter.convert(-total.getRefundVoidTotalAmt()));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setGravity(Gravity.CENTER));

        // TOTAL
        long totalNum = total.getSaleTotalNum() + total.getRefundTotalNum();//Not + offline as SaleTotalNum already include offline
        long totalAmt = total.getSaleTotalAmt() - total.getRefundTotalAmt();//Not + offline as SaleTotalAmt already include offline
        addPageTotalByIssuer(page, Utils.getString(R.string.receipt_amount_total).toUpperCase(), totalNum, CurrencyConverter.convert(totalAmt));
        page.addLine().addUnit(page.createUnit().setText(" "));

        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_end_of_report))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.CENTER)
                        .setTextStyle(Typeface.BOLD));

        return page;
    }

    private void generateTotalByIssuer(List<Bitmap> bitmaps) {
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
        long[] sale, offline, voidSale, refund, voidRefund, totalSummary;

        for (int i = 1; i <= totalPage; i++) {
            page = Device.generatePage();
            transPerPage = (transPerPage = i * MAX_SIZE) > tranSize ? tranSize : transPerPage;
            totalSummary = new long[2];
            for (j = transNo; j < transPerPage; j++) {
                issuer = issuers.get(j);
                // card name : issuer name
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText("CARD NAME : " + issuer.getIssuerName())
                                .setFontSize(FONT_NORMAL));
                page.addLine().addUnit(page.createUnit().setText(" "));

                // SALE
                sale = getTotalSaleByIssuer(issuer);
                addPageTotalByIssuer(page, Utils.getString(R.string.trans_sale).toUpperCase(), sale[0], CurrencyConverter.convert(sale[1]));

                // OFFLINE
                offline = getTotalOfflineByIssuer(issuer);
                addPageTotalByIssuer(page, Utils.getString(R.string.trans_offline).toUpperCase(), offline[0], CurrencyConverter.convert(offline[1]));

                // VOID
                voidSale = getTotalVoidSaleByIssuer(issuer);
                addPageTotalByIssuer(page, Utils.getString(R.string.trans_void).toUpperCase(), voidSale[0], CurrencyConverter.convert(-voidSale[1]));

                // REFUND
                refund = getTotalRefundByIssuer(issuer);
                addPageTotalByIssuer(page, Utils.getString(R.string.trans_refund).toUpperCase(), refund[0], CurrencyConverter.convert(-refund[1]));

                // VOID REFUND
                voidRefund = getTotalVoidRefundByIssuer(issuer);
                addPageTotalByIssuer(page, (Utils.getString(R.string.trans_void) + " " + Utils.getString(R.string.trans_refund)).toUpperCase(), voidRefund[0], CurrencyConverter.convert(-voidRefund[1]));

                totalSummary[0] = sale[0] + offline[0] + refund[0];
                totalSummary[1] = (sale[1] + offline[1]) - refund[1];

                page.addLine().addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_one_line))
                        .setGravity(Gravity.CENTER));

                // TOTAL
                addPageTotalByIssuer(page, Utils.getString(R.string.receipt_amount_total).toUpperCase(), totalSummary[0], CurrencyConverter.convert(totalSummary[1]));
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
    public Bitmap generateBitmap() {
        return null;
    }

    @Override
    public String generateString() {
        return null;
    }
}
