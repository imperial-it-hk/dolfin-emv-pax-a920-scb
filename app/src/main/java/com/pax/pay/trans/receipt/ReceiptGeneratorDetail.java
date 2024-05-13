package com.pax.pay.trans.receipt;

import android.graphics.Bitmap;
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

class ReceiptGeneratorDetail extends AReceiptGenerator {

    public ReceiptGeneratorDetail(TransTotal total, String title) {
        super(total, title);
    }

    @Override
    public List<Bitmap> generateBitmaps() throws Exception {
        List<Bitmap> bitmaps = new ArrayList<>();

        try {
            IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();

            bitmaps.add(imgProcessing.pageToBitmap(generateHeader(), 384));
            addTransDetail(bitmaps);
            bitmaps.addAll(new ReceiptGeneratorSummary(total, false).generateBitmaps());
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

    protected void addTransDetail(List<Bitmap> bitmaps) {
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

    protected void generateTransDetail(TransData transData, IPage page) {
        String temp;
        String temp2;
        ETransType transType = transData.getTransType();
        boolean isOfflineTransSend = ETransType.OFFLINE_TRANS_SEND == transType || ETransType.OFFLINE_TRANS_SEND == transData.getOrigTransType();

        //CARD NUMBER
        temp = PanUtils.maskCardNo(transData.getPan(), transData.getIssuer().getPanMaskPattern());
        page.addLine()
                .addUnit(page.createUnit().setText(temp).setGravity(Gravity.START).setFontSize(FONT_SMALL));

        //EXP DATE - CARD NAME
        temp = Constants.ACQ_AMEX_EPP.equals(total.getAcquirer().getName()) ? Utils.getString(R.string.receipt_amex_instalment_issuer) : transData.getIssuer().getName();
        page.addLine()
                .addUnit(page.createUnit().setText("XX/XX").setFontSize(FONT_SMALL))
                .addUnit(page.createUnit().setText(temp).setGravity(Gravity.END).setWeight(4f).setFontSize(FONT_SMALL));

        //TRACE NO. - APPR CODE
        String authCode = (!isOfflineTransSend) ? (transData.getAuthCode() == null ? "" : transData.getAuthCode()) : transData.getOrigAuthCode();
        page.addLine()
                .addUnit(page.createUnit().setText(Component.getPaddedNumber(transData.getTraceNo(), 6)).setFontSize(FONT_SMALL))
                .addUnit(page.createUnit().setText(authCode).setGravity(Gravity.END).setWeight(0.3f).setFontSize(FONT_SMALL));

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
            TransData adjustedTrans = FinancialApplication.getTransDataDbHelper()
                    .findAdjustedTransDataByTraceNo(transData.getTraceNo(), transData.getAcquirer());
            if (adjustedTrans != null) {
                temp = CurrencyConverter.convert(-Utils.parseLongSafe(adjustedTrans.getAmount(), 0), transData.getCurrency());
            } else {
                temp = CurrencyConverter.convert(-Utils.parseLongSafe(transData.getAmount(), 0), transData.getCurrency());
            }
        } else {
            temp = CurrencyConverter.convert(Utils.parseLongSafe(transData.getAmount(), 0), transData.getCurrency());
        }
        page.addLine()
                .addUnit(page.createUnit().setText(getTextTransTypeByAcquirer(transData)).setWeight(4f).setFontSize(FONT_SMALL))
                .addUnit(page.createUnit().setText(temp).setGravity(Gravity.END).setWeight(2.5f).setFontSize(FONT_SMALL));

        if (transData.getTransState() == TransData.ETransStatus.ADJUSTED) {
            temp = CurrencyConverter.convert(Utils.parseLongSafe(transData.getOrigAmount(), 0), transData.getCurrency());
            temp2 = CurrencyConverter.convert(Utils.parseLongSafe(transData.getTipAmount(), 0), transData.getCurrency());
            page.addLine()
                    .addUnit(page.createUnit().setText(temp + "+" + temp2).setGravity(Gravity.END).setWeight(6.0f).setFontSize(FONT_SMALL));
        }

        page.addLine().addUnit(page.createUnit().setText(" "));
    }

    protected String getTextTransTypeByAcquirer(TransData transData) {
        ETransType transType = transData.getTransType();
        boolean isVoid = transType == ETransType.VOID
                || transData.getTransType() == ETransType.KBANK_SMART_PAY_VOID
                || transData.getTransType() == ETransType.DOLFIN_INSTALMENT_VOID
                || transData.getTransState() == TransData.ETransStatus.VOIDED;

        switch (transData.getAcquirer().getName()) {
            case Constants.ACQ_SMRTPAY:
            case Constants.ACQ_SMRTPAY_BDMS:
            case Constants.ACQ_DOLFIN_INSTALMENT:
                return isVoid ? Component.getTransByIPlanMode(transData) + "(VOID)" : Component.getTransByIPlanMode(transData);
            case Constants.ACQ_BAY_INSTALLMENT:
                return isVoid ? transData.getOrigTransType().getTransName() + "(VOID)" : transType.getTransName();
            default:
                boolean isOfflineTransSend = ETransType.OFFLINE_TRANS_SEND == transType || ETransType.OFFLINE_TRANS_SEND == transData.getOrigTransType();
                boolean isRefund = ETransType.REFUND == transType || ETransType.REFUND == transData.getOrigTransType();
                String type = (!isOfflineTransSend) ? transType.toString() : transType.getTransName().toUpperCase();
                if (transType == ETransType.SALE_COMPLETION) {
                    type = ETransType.SALE_COMPLETION.getTransName();
                }
                if (isVoid) {
                    if (transData.getOrigTransType() == ETransType.SALE_COMPLETION) {
                        type = Utils.getString(R.string.trans_offline).toUpperCase() + " " + ETransType.SALE_COMPLETION.getTransName() + " (" + type + ")";
                        return type;
                    } else {
                        type = (!isOfflineTransSend) ? (!isRefund) ? "SALE(VOID)" : "REFUND(VOID)" : "OFFLINE(VOID)";
                    }
                } else if (transData.getTransState() == TransData.ETransStatus.ADJUSTED) {
                    type += "(ADJUST)";
                }
                if (!isOfflineTransSend && transData.getOfflineSendState() != null
                        && transData.getTransState() != TransData.ETransStatus.ADJUSTED) {
                    type = Utils.getString(R.string.trans_offline).toUpperCase() + " " + type;
                }
                return type;
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
