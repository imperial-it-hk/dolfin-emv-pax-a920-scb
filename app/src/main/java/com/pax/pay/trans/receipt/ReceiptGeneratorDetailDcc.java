package com.pax.pay.trans.receipt;

import android.graphics.Bitmap;
import android.view.Gravity;

import com.pax.abl.utils.PanUtils;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.eemv.utils.Tools;
import com.pax.glwrapper.imgprocessing.IImgProcessing;
import com.pax.glwrapper.page.IPage;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.model.TransTotal;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.TimeConverter;
import com.pax.pay.utils.Utils;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import th.co.bkkps.utils.Log;

import static com.pax.pay.utils.Utils.getString;

class ReceiptGeneratorDetailDcc extends AReceiptGenerator {

    public ReceiptGeneratorDetailDcc(TransTotal total, String title) {
        super(total, title);
    }

    @Override
    public List<Bitmap> generateBitmaps() throws Exception {
        List<Bitmap> bitmaps = new ArrayList<>();

        try {
            IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();

            bitmaps.add(imgProcessing.pageToBitmap(generateHeader(), 384));
            generateTransDetailDccKBank(bitmaps);
            bitmaps.addAll(new ReceiptGeneratorSummaryDcc(total, false).generateBitmaps());
        } catch (Exception e) {
            throw new Exception("Generate Bitmap fail", e);
        }

        return bitmaps;
    }

    @Override
    public IPage generateHeader() {
        IPage page = super.generateHeader();

        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_card_number))
                        .setFontSize(FONT_NORMAL_22)
                        .setWeight(2.0f)
                        .setGravity(Gravity.START));

        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_card_exp_date))
                        .setFontSize(FONT_NORMAL_22)
                        .setWeight(2.0f))
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_card_name))
                        .setFontSize(FONT_NORMAL_22)
                        .setGravity(Gravity.END));

        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_trans_no))
                        .setFontSize(FONT_NORMAL_22)
                        .setWeight(2.0f))
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_appr_code2))
                        .setFontSize(FONT_NORMAL_22)
                        .setGravity(Gravity.END));

        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_date_short))
                        .setFontSize(FONT_NORMAL_22))
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_time_short))
                        .setFontSize(FONT_NORMAL_22)
                        .setGravity(Gravity.END));

        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_trans_type))
                        .setFontSize(FONT_NORMAL_22)
                        .setWeight(2.0f))
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_amount))
                        .setFontSize(FONT_NORMAL_22)
                        .setGravity(Gravity.END));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_double_line))
                .setGravity(Gravity.CENTER));

        return page;
    }

    @Override
    public IPage generateFooter() {
        return null;
    }

    public void generateTransDetailDccKBank(List<Bitmap> bitmaps) {
        List<TransData> details = FinancialApplication.getTransDataDbHelper().findTransData(getTransTypeByAcquirer(), filter2, total.getAcquirer());

        List<String[]> dccCurencys = FinancialApplication.getTransDataDbHelper().findAllDccCurrency();
        if (!dccCurencys.isEmpty()) {
            IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();

            for (String[] c : dccCurencys) {
                Log.e("menu", "dccCurrency = " + c[0]);
                Currency dccCurrency = Currency.getInstance(c[0]);

                IPage page = Device.generatePage();
                page.addLine().addUnit(page.createUnit()
                        .setText(getString(R.string.receipt_one_line))
                        .setGravity(Gravity.CENTER));
                page.addLine().addUnit(page.createUnit().setText(dccCurrency.getDisplayName() + " (" + dccCurrency.getCurrencyCode() + ")").setGravity(Gravity.CENTER));
                page.addLine().addUnit(page.createUnit()
                        .setText(getString(R.string.receipt_one_line))
                        .setGravity(Gravity.CENTER));
                bitmaps.add(imgProcessing.pageToBitmap(page, 384));

                int transNo = 0, j, transPerPage;
                int tranSize = details != null ? details.size() : 0;
                int totalPage = (int) Math.ceil((double) tranSize / MAX_SIZE);
                for (int i = 1 ; i <= totalPage ; i++) {
                    page = Device.generatePage();
                    transPerPage = (transPerPage = i * MAX_SIZE) > tranSize ? tranSize : transPerPage;
                    for (j = transNo ; j < transPerPage ; j++) {
                        TransData transData = details.get(j);
                        if (transData.getDccCurrencyName() != null && transData.getDccCurrencyName().equalsIgnoreCase(c[0])) {
                            buildDccTransDetails(transData, page);
                        }
                        if (j == (tranSize - 1)) { // last record
                            page.addLine().addUnit(page.createUnit()
                                    .setText(Utils.getString(R.string.receipt_double_line))
                                    .setGravity(Gravity.CENTER));
                        }
                    }
                    transNo = j;
                    bitmaps.add(imgProcessing.pageToBitmap(page, 384));
                }
            }

        }
    }

    private void buildDccTransDetails(TransData transData, IPage page) {
        String temp;
        String temp2;
        ETransType transType = transData.getTransType();
        String type = transType.toString();
        if (transData.getTransState() == TransData.ETransStatus.ADJUSTED) {
            type += "(ADJUST)";
        }
        if (type.equals("VOID")) {
            type = "SALE(VOID)";
        }

        temp = PanUtils.maskCardNo(transData.getPan(), transData.getIssuer().getPanMaskPattern());
        page.addLine()
                .addUnit(page.createUnit().setText(temp).setGravity(Gravity.START).setFontSize(FONT_SMALL));

        //EXP DATE - CARD NAME
        page.addLine()
                .addUnit(page.createUnit().setText("XX/XX").setFontSize(FONT_SMALL))
                .addUnit(page.createUnit().setText(transData.getIssuer().getName()).setGravity(Gravity.END).setWeight(4f).setFontSize(FONT_SMALL));

        //TRACE NO. - APPR CODE
        temp = transData.getAuthCode() == null ? "" : transData.getAuthCode();
        page.addLine()
                .addUnit(page.createUnit().setText(Component.getPaddedNumber(transData.getTraceNo(), 6)).setFontSize(FONT_SMALL))
                .addUnit(page.createUnit().setText(temp).setGravity(Gravity.END).setWeight(0.3f).setFontSize(FONT_SMALL));

        // DATE - TIME
        temp = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.DATE_PATTERN_DISPLAY);
        temp2 = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.TIME_PATTERN_DISPLAY4);
        page.addLine()
                .addUnit(page.createUnit().setText(temp).setFontSize(FONT_SMALL))
                .addUnit(page.createUnit().setText(temp2).setGravity(Gravity.END).setFontSize(FONT_SMALL));

        // TRANS. TYPE - AMOUNT
        if (transType.isSymbolNegative()) {
            temp = CurrencyConverter.convert(-Utils.parseLongSafe(transData.getAmount(), 0), transData.getCurrency());
        } else {
            temp = CurrencyConverter.convert(Utils.parseLongSafe(transData.getAmount(), 0), transData.getCurrency());
        }
        page.addLine()
                .addUnit(page.createUnit().setText(type).setWeight(4f).setFontSize(FONT_SMALL))
                .addUnit(page.createUnit().setText(temp).setGravity(Gravity.END).setWeight(2.5f).setFontSize(FONT_SMALL));

        // DCC AMOUNT
        long amount = Utils.parseLongSafe(transData.getDccAmount(), 0);
        if (transData.getTransType().isSymbolNegative() || transData.getTransState() == TransData.ETransStatus.VOIDED)
            amount = -amount;

        String currencyNumeric = Tools.bytes2String(transData.getDccCurrencyCode());
        temp = CurrencyConverter.convert(amount, currencyNumeric);
        page.addLine()
                .addUnit(page.createUnit().setText(temp).setGravity(Gravity.END).setWeight(2.5f).setFontSize(FONT_SMALL));

        if (transData.getTransState() == TransData.ETransStatus.ADJUSTED) {
            temp = CurrencyConverter.convert(Utils.parseLongSafe(transData.getOrigAmount(), 0), transData.getCurrency());
            temp2 = CurrencyConverter.convert(Utils.parseLongSafe(transData.getTipAmount(), 0), transData.getCurrency());
            page.addLine()
                    .addUnit(page.createUnit().setText(temp + "+" + temp2).setGravity(Gravity.END).setWeight(6.0f).setFontSize(FONT_SMALL));

            temp = CurrencyConverter.convert(Utils.parseLongSafe(transData.getOrigDccAmount(), 0), currencyNumeric);
            temp2 = CurrencyConverter.convert(Utils.parseLongSafe(transData.getDccTipAmount(), 0), currencyNumeric);
            page.addLine()
                    .addUnit(page.createUnit().setText(temp + "+" + temp2).setGravity(Gravity.END).setWeight(6.0f).setFontSize(FONT_SMALL));
        }

        page.addLine().addUnit(page.createUnit().setText(" "));

//        //dcc rate
//        double exRate = transData.getDccConversionRate() != null ? Double.parseDouble(transData.getDccConversionRate()) / 10000 : 0;
//        page.addLine()
//                .addUnit(page.createUnit().setText(Utils.getString(R.string.receipt_dcc_ex_rate)))
//                .addUnit(page.createUnit().setText(String.format(Locale.getDefault(), "%.4f", exRate)).setGravity(Gravity.END));
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
