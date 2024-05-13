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
import com.pax.pay.base.TransTypeMapping;
import com.pax.pay.trans.model.TransDccKbankTotal;
import com.pax.pay.trans.model.TransTotal;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.Utils;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

class ReceiptGeneratorSummaryDcc extends AReceiptGenerator {

    private boolean showHeader = true;

    public ReceiptGeneratorSummaryDcc(TransTotal total, String title) {
        super(total, title);
    }

    public ReceiptGeneratorSummaryDcc(TransTotal total, boolean showHeader) {
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
        addPageTotalByIssuer(page, Utils.getString(R.string.trans_sale).toUpperCase(), total.getSaleTotalNum(), CurrencyConverter.convert(total.getSaleTotalAmt()));

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
        long totalNum = total.getSaleTotalNum() + total.getOfflineTotalNum() + total.getRefundTotalNum();
        long totalAmt = (total.getSaleTotalAmt() + total.getOfflineTotalAmt()) - total.getRefundTotalAmt();
        addPageTotalByIssuer(page, Utils.getString(R.string.receipt_amount_total).toUpperCase(), totalNum, CurrencyConverter.convert(totalAmt));
        page.addLine().addUnit(page.createUnit().setText(" "));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setGravity(Gravity.CENTER));

        generateDccTotal(page, null);

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

        List<TransTypeMapping> transMappings = FinancialApplication.getAcqManager().findTransMapping(total.getAcquirer());

        List<Issuer> issuers = new ArrayList<>(0);
        if (transMappings != null) {
            for (TransTypeMapping mapping : transMappings) {
                long[] tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(total.getAcquirer(), filter, mapping.getIssuer());
                if (tempObj[0] != 0 || tempObj[1] != 0) {
                    issuers.add(mapping.getIssuer());
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
                        .setText(Utils.getString(R.string.receipt_one_line))
                        .setGravity(Gravity.CENTER));

                generateDccTotal(page, issuer);
            }

            transNo = j;
            bitmaps.add(imgProcessing.pageToBitmap(page, 384));
        }
    }

    private void generateDccTotal(IPage page, Issuer issuer) {
        boolean isDoubleLine = false;
        List<TransDccKbankTotal> totals;
        if ((isDoubleLine = issuer != null)) {
            totals = FinancialApplication.getTransTotalDbHelper().calTotalDccGroupByCurrency(total.getAcquirer(), issuer);
        } else {
            totals = FinancialApplication.getTransTotalDbHelper().calTotalDccGroupByCurrency(total.getAcquirer());
        }

        if (!totals.isEmpty()) {
            for (TransDccKbankTotal t : totals) {
                Currency dccCurrency = Currency.getInstance(t.getCurrencyCode());
                page.addLine().addUnit(page.createUnit()
                        .setText(dccCurrency.getDisplayName() + " (" + dccCurrency.getCurrencyCode() + ")")
                        .setFontSize(IReceiptGenerator.FONT_NORMAL_26)
                        .setTextStyle(Typeface.BOLD).setGravity(Gravity.CENTER));

                page.addLine().addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_one_line))
                        .setGravity(Gravity.CENTER));

                page.addLine().addUnit(page.createUnit().setText(" "));

                // SALE
                addPageTotalByIssuer(page, Utils.getString(R.string.trans_sale).toUpperCase(), t.getSaleTotalNum(), CurrencyConverter.convert(t.getSaleDccTotalAmt(), t.getCurrencyNumericCode()));

                // OFFLINE
                addPageTotalByIssuer(page, Utils.getString(R.string.trans_offline).toUpperCase(), t.getSaleOfflineTotalNum(), CurrencyConverter.convert(t.getSaleOfflineTotalAmt(), t.getCurrencyNumericCode()));

                // VOID
                addPageTotalByIssuer(page, Utils.getString(R.string.trans_void).toUpperCase(), t.getSaleVoidTotalNum(), CurrencyConverter.convert(-t.getSaleDccVoidTotalAmt(), t.getCurrencyNumericCode()));

                // REFUND
                addPageTotalByIssuer(page, Utils.getString(R.string.trans_refund).toUpperCase(), t.getRefundTotalNum(), CurrencyConverter.convert(-t.getRefundTotalAmt(), t.getCurrencyNumericCode()));

                // VOID REFUND
                addPageTotalByIssuer(page, (Utils.getString(R.string.trans_void) + " " + Utils.getString(R.string.trans_refund)).toUpperCase(), t.getRefundVoidTotalNum(), CurrencyConverter.convert(-t.getRefundVoidTotalAmt(), t.getCurrencyNumericCode()));

                if (isDoubleLine) {
                    page.addLine().addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_double_line))
                            .setGravity(Gravity.CENTER));
                } else {
                    page.addLine().addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_one_line))
                            .setGravity(Gravity.CENTER));
                }
            }
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
