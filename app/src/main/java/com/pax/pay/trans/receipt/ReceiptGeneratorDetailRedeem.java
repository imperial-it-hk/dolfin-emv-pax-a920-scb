package com.pax.pay.trans.receipt;

import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.view.Gravity;

import com.pax.abl.utils.PanUtils;
import com.pax.device.Device;
import com.pax.edc.R;
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
import java.util.List;

class ReceiptGeneratorDetailRedeem extends AReceiptGenerator {

    public ReceiptGeneratorDetailRedeem(TransTotal total, String title) {
        super(total, title);
    }

    @Override
    public List<Bitmap> generateBitmaps() throws Exception {
        List<Bitmap> bitmaps = new ArrayList<>();

        try {
            IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();

            bitmaps.add(imgProcessing.pageToBitmap(generateHeader(), 384));
            addTransDetail(bitmaps);
            bitmaps.add(imgProcessing.pageToBitmap(generateFooter(), 384));
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
                        .setText(Utils.getString(R.string.receipt_redeem_trans_type))
                        .setFontSize(FONT_NORMAL_22)
                        .setWeight(2.0f)
                        .setGravity(Gravity.START));

        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_redeem_detail))
                        .setFontSize(FONT_NORMAL_22)
                        .setWeight(2.0f)
                        .setGravity(Gravity.START));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_double_line))
                .setGravity(Gravity.CENTER));

        return page;
    }

    @Override
    public IPage generateFooter() {
        IPage page = Device.generatePage();

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
                        .setText(String.valueOf(total.getTransRedeemKbankTotal().getVisaSum()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("MASTERCARD")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(total.getTransRedeemKbankTotal().getMastercardSum()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("JCB")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(total.getTransRedeemKbankTotal().getJcbSum()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("OTHER")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(total.getTransRedeemKbankTotal().getOtherCardSum()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("ALL CARD")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(total.getTransRedeemKbankTotal().getAllCardsSum()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .adjustTopSpace(10)
                .addUnit(page.createUnit()
                        .setText("ITEMS")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(total.getTransRedeemKbankTotal().getItemSum()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("POINTS")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(String.valueOf(total.getTransRedeemKbankTotal().getPointsSum()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("REDEEM BHT")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(total.getTransRedeemKbankTotal().getRedeemAmtSum()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("CREDIT BHT")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(total.getTransRedeemKbankTotal().getCreditSum()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("TOTAL BHT")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(total.getTransRedeemKbankTotal().getTotalSum()))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));

        page.addLine().addUnit(page.createUnit().setText(" "));

        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_end_of_report))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.CENTER)
                        .setTextStyle(Typeface.BOLD));

        return page;
    }

    private void addTransDetail(List<Bitmap> bitmaps) {
        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();

        List<TransData> details = FinancialApplication.getTransDataDbHelper().findTransData(getTransTypeByAcquirer(), filter2, total.getAcquirer());

        IPage page;
        int transNo = 0, j, transPerPage;
        int tranSize = details != null ? details.size() : 0;
        int totalPage = (int) Math.ceil((double) tranSize / MAX_SIZE);
        for (int i = 1 ; i <= totalPage ; i++) {
            page = Device.generatePage();
            transPerPage = (transPerPage = i * MAX_SIZE) > tranSize ? tranSize : transPerPage;
            for (j = transNo ; j < transPerPage ; j++) {
                TransData transData = details.get(j);
                generateTransDetail(transData, page);
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

    public void generateTransDetail(TransData transData, IPage page) {
        String temp;
        String temp2;
        boolean isVoid = transData.getTransType() == ETransType.KBANK_REDEEM_VOID || transData.getTransState() == TransData.ETransStatus.VOIDED;

        // TRANS. NAME
        ETransType transType = isVoid ? transData.getOrigTransType() : transData.getTransType();
        String strTransType = "";
        if (transType != null) {
            if (transType == ETransType.KBANK_REDEEM_DISCOUNT) {
                strTransType = "89999".equals(transData.getRedeemedDiscountType()) ? Utils.getString(R.string.trans_kbank_redeem_discount_var) : Utils.getString(R.string.trans_kbank_redeem_discount_fix);
            } else {
                strTransType = transType.getTransName();
            }
            strTransType = isVoid ? strTransType.replaceFirst(" ", " " + Utils.getString(R.string.receipt_redeem_void) + " ") : strTransType;
        }

        //CARD NUMBER
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

        // REDEEM TRANS. TYPE
        page.addLine()
                .addUnit(page.createUnit().setText(strTransType).setGravity(Gravity.START).setFontSize(FONT_SMALL));

        // REDEEM DETAIL
        ReceiptKbankRedeemedTransDetail.generateDetail(page, transData, false, true, false, FONT_SMALL);

        page.addLine().addUnit(page.createUnit().setText(" "));
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
