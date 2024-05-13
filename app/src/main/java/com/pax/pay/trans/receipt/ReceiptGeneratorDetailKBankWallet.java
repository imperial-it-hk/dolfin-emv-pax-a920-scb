package com.pax.pay.trans.receipt;

import android.graphics.Bitmap;
import android.view.Gravity;

import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.glwrapper.imgprocessing.IImgProcessing;
import com.pax.glwrapper.page.IPage;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Issuer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.model.TransTotal;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.QrTag31Utils;
import com.pax.pay.utils.TimeConverter;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class ReceiptGeneratorDetailKBankWallet extends AReceiptGenerator {

    public ReceiptGeneratorDetailKBankWallet(TransTotal total, String title) {
        super(total, title);
    }

    @Override
    public List<Bitmap> generateBitmaps() throws Exception {
        List<Bitmap> bitmaps = new ArrayList<>();

        try {
            IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();

            bitmaps.add(imgProcessing.pageToBitmap(generateHeader(), 384));

            switch (total.getAcquirer().getName()) {
                case Constants.ACQ_MY_PROMPT:
                    List<TransData.ETransStatus> filter = new ArrayList<>();
                    List<TransData> list = FinancialApplication.getTransDataDbHelper().findTransData(getTransTypeByAcquirer(), filter, total.getAcquirer());
                    addTransDetail(bitmaps, list);
                    bitmaps.addAll(new ReceiptGeneratorSummaryKBankWallet(total, false).generateBitmaps());
                    break;
                case Constants.ACQ_ALIPAY:
                case Constants.ACQ_ALIPAY_B_SCAN_C:
                case Constants.ACQ_WECHAT:
                case Constants.ACQ_WECHAT_B_SCAN_C:
                case Constants.ACQ_QR_CREDIT:
                    List<TransData> details = FinancialApplication.getTransDataDbHelper().findTransData(getTransTypeByAcquirer(), filter2, total.getAcquirer());
                    addTransDetail(bitmaps, details);
                    bitmaps.addAll(new ReceiptGeneratorSummaryKBankWallet(total, false).generateBitmaps());
                    break;
                case Constants.ACQ_KPLUS:
                    boolean isQRTag31Enable = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_QR_TAG_31_ENABLE, false);
                    boolean isOldStyleReport = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_QR_TAG_31_REPORT_GROUPING_OLD_STYLE, false);

                    if (isQRTag31Enable && !isOldStyleReport) {
                        printQrTag31ByQRSourceOfFund(bitmaps, true);
                    } else {
                        Issuer issuer = FinancialApplication.getAcqManager().findIssuer(Constants.ISSUER_KPLUS_PROMPYPAY);
                        List<TransData> records = FinancialApplication.getTransDataDbHelper().findTransData(getTransTypeByAcquirer(), filter2, total.getAcquirer(), issuer);
                        addTransDetail(bitmaps, records);

                        issuer = FinancialApplication.getAcqManager().findIssuer(Constants.ISSUER_KPLUS);
                        records = FinancialApplication.getTransDataDbHelper().findTransData(getTransTypeByAcquirer(), filter2, total.getAcquirer(), issuer);
                        addTransDetail(bitmaps, records);

                        bitmaps.addAll(new ReceiptGeneratorSummaryKBankWallet(total, false, false, Constants.ISSUER_KPLUS_PROMPYPAY).generateBitmaps());
                        bitmaps.addAll(new ReceiptGeneratorSummaryKBankWallet(total, false, false, Constants.ISSUER_KPLUS).generateBitmaps());
                        bitmaps.addAll(new ReceiptGeneratorSummaryKBankWallet(total, false, true, Constants.ISSUER_KPLUS).generateBitmaps());
                    }
                    break;
            }
        } catch (Exception e) {
            throw new Exception("Generate Bitmap fail", e);
        }

        return bitmaps;
    }


    public List<Bitmap> printQrTag31ByQRSourceOfFund(List<Bitmap> bitmaps, boolean isPrintDetailRequire)  {
        List<TransData> transList = FinancialApplication.getTransDataDbHelper().findTransData(getTransTypeByAcquirer(), filter2, total.getAcquirer());
        if (transList.size()>0) {

            if (isPrintDetailRequire) {
                addTransDetail(bitmaps, transList);
            }

            Map<String, List<TransData>> groupByItem = QrTag31Utils.Companion.getDistinctSourceOfFund(transList);
            if (!groupByItem.isEmpty()) {
                List<String> keys = new ArrayList<>(groupByItem.keySet());
                String lastKeyStr = keys.get(keys.size()-1);
                for (String targetSoruceOfFund : groupByItem.keySet()) {
                    boolean isLastKey = (lastKeyStr == targetSoruceOfFund);

                    try {
                        List<TransData> localTransList = groupByItem.get(targetSoruceOfFund);
                        bitmaps.addAll(new ReceiptGeneratorSummaryThaiQrTag31(total, Constants.ACQ_KPLUS, false, true,false, targetSoruceOfFund, localTransList).generateBitmaps());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                bitmaps.addAll(new ReceiptGeneratorSummaryThaiQrTag31(total, Constants.ACQ_KPLUS, false, false,true, "", transList).generateBitmaps());
            }
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
                        .setText(Utils.getString(R.string.receipt_card_name))
                        .setFontSize(FONT_NORMAL_22)
                        .setWeight(2.0f))
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_card_exp_date))
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

    private void addTransDetail(List<Bitmap> bitmaps, List<TransData> details) {
        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();

        IPage page;
        int transNo = 0, j, transPerPage;
        int tranSize = details != null ? details.size() : 0;
        int totalPage = (int) Math.ceil((double) tranSize / MAX_SIZE);
        for (int i = 1 ; i <= totalPage ; i++) {
            page = Device.generatePage();
            transPerPage = (transPerPage = i * MAX_SIZE) > tranSize ? tranSize : transPerPage;
            for (j = transNo ; j < transPerPage ; j++) {
                TransData transData = details.get(j);
                switch (total.getAcquirer().getName()) {
                    case Constants.ACQ_ALIPAY:
                    case Constants.ACQ_ALIPAY_B_SCAN_C:
                    case Constants.ACQ_WECHAT:
                    case Constants.ACQ_WECHAT_B_SCAN_C:
                        generateTransDetailAlipayWeChat(transData, page);
                        break;
                    case Constants.ACQ_KPLUS:
                    case Constants.ACQ_MY_PROMPT:
                        generateTransDetailKPlus(transData, page);
                        break;
                    case Constants.ACQ_QR_CREDIT:
                        generateTransDetailQRCredit(transData, page);
                        break;
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

    public void generateTransDetailAlipayWeChat(TransData transData, IPage page) {
        String temp;
        String temp2;
        ETransType transType = transData.getTransType();
        TransData.ETransStatus transState = transData.getTransState();

        String state = "", strTransType = "";
        if (transState != null && transState != TransData.ETransStatus.NORMAL) {
            state = transState == TransData.ETransStatus.VOIDED ? "VOID" : transState.toString().toUpperCase();
        }
        if (transType != null && transType.getTransName() != null) {
            strTransType = transType == ETransType.QR_INQUIRY_ALIPAY || transType == ETransType.QR_INQUIRY_WECHAT ? Utils.getString(R.string.trans_sale) : Utils.getString(R.string.trans_void);
        }
        String type = !state.isEmpty() ? strTransType.toUpperCase() + "(" + state + ")" : strTransType.toUpperCase();

        //CARD NUMBER
        temp = transData.getWalletPartnerID() != null ? transData.getWalletPartnerID().trim() : "";
        page.addLine()
                .addUnit(page.createUnit().setText(temp).setGravity(Gravity.START).setFontSize(FONT_SMALL));

        //CARD NAME
        if (Constants.ACQ_ALIPAY.equals(total.getAcquirer().getName()) || Constants.ACQ_ALIPAY_B_SCAN_C.equals(total.getAcquirer().getName())) {
            temp = transData.getBuyerLoginID() != null ? transData.getBuyerLoginID().trim() : "";
            page.addLine()
                    .addUnit(page.createUnit().setText(temp).setGravity(Gravity.START).setFontSize(FONT_SMALL));
        }

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

        page.addLine().addUnit(page.createUnit().setText(" "));
    }

    public void generateTransDetailKPlus(TransData transData, IPage page) {
        String temp;
        String temp2;
        ETransType transType = transData.getTransType();
        TransData.ETransStatus transState = transData.getTransState();

        String state = "", strTransType = "";
        if (transState != null && transState != TransData.ETransStatus.NORMAL) {
            state = transState == TransData.ETransStatus.VOIDED ? "VOID" : transState.toString().toUpperCase();
        }
        if (transType != null && transType.getTransName() != null) {
            strTransType = (transType == ETransType.QR_INQUIRY || transType == ETransType.QR_VERIFY_PAY_SLIP || transType == ETransType.QR_MYPROMPT_SALE) ? Utils.getString(R.string.trans_sale) : Utils.getString(R.string.trans_void);
        }
        String type = !state.isEmpty() ? strTransType.toUpperCase() + "(" + state + ")" : strTransType.toUpperCase();

        //CARD NAME
//        String promocode = transData.getPromocode() != null ? transData.getPromocode().trim() : "";
//        temp = "2".equalsIgnoreCase(promocode) ? "PromptPay" : Utils.getString(R.string.receipt_kplus);
        temp = (transData.getQrSourceOfFund() != null) ? transData.getQrSourceOfFund() : "-";

        if(transData.getAcquirer().getName().equals(Constants.ACQ_MY_PROMPT)) {
            temp = (!transData.getChannel().equals("") && transData.getChannel() != null) ? transData.getChannel() : "-";
        } else {
            temp = (transData.getQrSourceOfFund() != null) ? transData.getQrSourceOfFund() : "-";
        }
        page.addLine()
                .addUnit(page.createUnit().setText(temp).setGravity(Gravity.START).setFontSize(FONT_SMALL));

        //TRACE NO. - APPR CODE
        temp = transData.getAuthCode() == null ? "-" : transData.getAuthCode();
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

        page.addLine().addUnit(page.createUnit().setText(" "));
    }

    public void generateTransDetailQRCredit(TransData transData, IPage page) {
        String temp;
        String temp2;
        ETransType transType = transData.getTransType();
        TransData.ETransStatus transState = transData.getTransState();

        String state = "", strTransType = "";
        if (transState != null && transState != TransData.ETransStatus.NORMAL) {
            state = transState == TransData.ETransStatus.VOIDED ? "VOID" : transState.toString().toUpperCase();
        }
        if (transType != null && transType.getTransName() != null) {
            strTransType = transType == ETransType.QR_INQUIRY_CREDIT ? Utils.getString(R.string.trans_sale) : Utils.getString(R.string.trans_void);
        }
        String type = !state.isEmpty() ? strTransType.toUpperCase() + "(" + state + ")" : strTransType.toUpperCase();

        //CARD NUMBER
        temp = transData.getBuyerLoginID() != null ? transData.getBuyerLoginID().trim() : "";
        page.addLine()
                .addUnit(page.createUnit().setText(temp).setGravity(Gravity.START).setFontSize(FONT_SMALL));

        //CARD NAME
        temp = transData.getMerchantInfo() != null ? transData.getMerchantInfo().trim() : "";
        page.addLine()
                .addUnit(page.createUnit().setText(temp).setGravity(Gravity.START).setFontSize(FONT_SMALL));

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
