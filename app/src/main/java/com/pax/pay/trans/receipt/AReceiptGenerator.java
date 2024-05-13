package com.pax.pay.trans.receipt;

import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.view.Gravity;

import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.glwrapper.page.IPage;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Issuer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.model.TransTotal;
import com.pax.pay.utils.TimeConverter;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.util.ArrayList;
import java.util.List;

import static com.pax.pay.trans.model.TransData.ETransStatus.ADJUSTED;
import static com.pax.pay.trans.model.TransData.ETransStatus.NORMAL;
import static com.pax.pay.trans.model.TransData.ETransStatus.VOIDED;

abstract class AReceiptGenerator implements IReceiptGenerator {

    protected static final List<TransData.ETransStatus> filter = new ArrayList<>();
    protected static final List<TransData.ETransStatus> filter2 = new ArrayList<>();
    protected static final int MAX_SIZE = 10;
    protected TransTotal total;
    protected String title;

    static {
        filter.add(NORMAL);
        filter.add(ADJUSTED);

        filter2.add(VOIDED);
    }

    public AReceiptGenerator(TransTotal total, String title) {
        this.total = total;
        this.title = title;
    }

    public abstract List<Bitmap> generateBitmaps() throws Exception;

    public abstract IPage generateFooter();

    public IPage generateHeader() {
        IPage page = Device.generatePage();

        Bitmap logo = Component.getImageFromInternalFile(Constants.SLIP_LOGO_NAME, total.getAcquirer().getNii() + "_" + total.getAcquirer().getName());
        page.addLine()
                .addUnit(page.createUnit()
                        .setBitmap(logo)
                        .setGravity(Gravity.CENTER));
        page.addLine().addUnit(page.createUnit().setText(" "));

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
                .setText(Utils.getString(R.string.receipt_one_line))
                .setGravity(Gravity.CENTER));

        //HOST
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("HOST : " + total.getAcquirer().getNii())
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(total.getAcquirer().getName())
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        // terminal ID/operator ID
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_terminal_code_short))
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(total.getTerminalID())
                        .setWeight(4.0f)
                        .setGravity(Gravity.END)
                );
        // merchant ID
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_merchant_code_short))
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(total.getMerchantID())
                        .setWeight(4.0f)
                        .setGravity(Gravity.END));
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
        // batch NO
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_batch_num_short))
                        .setFontSize(FONT_NORMAL)
                        .setWeight(3.0f))
                .addUnit(page.createUnit()
                        .setText(Component.getPaddedNumber(total.getAcquirer().getCurrBatchNo(), 6))
                        .setWeight(3.0f)
                        .setGravity(Gravity.END));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setGravity(Gravity.CENTER));

        // title
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(title)
                        .setFontSize(FONT_BIG)
                        .setGravity(Gravity.CENTER)
                        .setTextStyle(Typeface.BOLD));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_double_line))
                .setGravity(Gravity.CENTER));

        return page;
    }

    public long[] getTotalSaleByIssuer(Issuer issuer) {
        ETransType transType = null;

        switch (total.getAcquirer().getName()) {
            case Constants.ACQ_KBANK:
            case Constants.ACQ_KBANK_BDMS:
            case Constants.ACQ_UP:
            case Constants.ACQ_AMEX:
            case Constants.ACQ_DCC:
                transType = ETransType.SALE;
                break;
            case Constants.ACQ_AMEX_EPP:
                transType = ETransType.AMEX_INSTALMENT;
                break;
            case Constants.ACQ_SMRTPAY:
            case Constants.ACQ_SMRTPAY_BDMS:
                transType = ETransType.KBANK_SMART_PAY;
                break;
            case Constants.ACQ_BAY_INSTALLMENT:
                transType = ETransType.BAY_INSTALMENT;
                break;
            case Constants.ACQ_ALIPAY:
                transType = ETransType.QR_INQUIRY_ALIPAY;
                break;
            case Constants.ACQ_ALIPAY_B_SCAN_C:
                transType = ETransType.QR_ALIPAY_SCAN;
                break;
            case Constants.ACQ_WECHAT:
                transType = ETransType.QR_INQUIRY_WECHAT;
                break;
            case Constants.ACQ_WECHAT_B_SCAN_C:
                transType = ETransType.QR_WECHAT_SCAN;
                break;
            case Constants.ACQ_KPLUS:
                transType = ETransType.QR_INQUIRY;
                break;
            case Constants.ACQ_QR_CREDIT:
                transType = ETransType.QR_INQUIRY_CREDIT;
                break;
            case Constants.ACQ_DOLFIN_INSTALMENT:
                transType = ETransType.DOLFIN_INSTALMENT;
                break;
        }

        if (transType != null) {
            return FinancialApplication.getTransDataDbHelper().countSumOf(total.getAcquirer(), transType, filter, issuer, false);
        }

        return new long[]{0, 0};
    }

    //TODO: implement get offline state?
    public long[] getTotalOfflineByIssuer(Issuer issuer) {
        long[] offline = FinancialApplication.getTransDataDbHelper().countSumOf(this.total.getAcquirer(), ETransType.OFFLINE_TRANS_SEND, filter, issuer, false);
        long[] offlineSaleComp = FinancialApplication.getTransDataDbHelper().countSumOf(this.total.getAcquirer(), ETransType.SALE_COMPLETION, filter, issuer, false);
        return new long[]{offline[0] + offlineSaleComp[0], offline[1] + offlineSaleComp[1]};
    }

    public long[] getTotalVoidSaleByIssuer(Issuer issuer) {
        ETransType transType = null;

        switch (total.getAcquirer().getName()) {
            case Constants.ACQ_KBANK:
            case Constants.ACQ_KBANK_BDMS:
            case Constants.ACQ_UP:
            case Constants.ACQ_AMEX:
            case Constants.ACQ_DCC:
                transType = ETransType.SALE;
                break;
            case Constants.ACQ_AMEX_EPP:
                transType = ETransType.AMEX_INSTALMENT;
                break;
            case Constants.ACQ_SMRTPAY:
            case Constants.ACQ_SMRTPAY_BDMS:
                transType = ETransType.KBANK_SMART_PAY;
                break;
            case Constants.ACQ_BAY_INSTALLMENT:
                transType = ETransType.BAY_INSTALMENT;
                break;
            case Constants.ACQ_ALIPAY:
                transType = ETransType.QR_INQUIRY_ALIPAY;
                break;
            case Constants.ACQ_ALIPAY_B_SCAN_C:
                transType = ETransType.QR_ALIPAY_SCAN;
                break;
            case Constants.ACQ_WECHAT:
                transType = ETransType.QR_INQUIRY_WECHAT;
                break;
            case Constants.ACQ_WECHAT_B_SCAN_C:
                transType = ETransType.QR_WECHAT_SCAN;
                break;
            case Constants.ACQ_KPLUS:
                transType = ETransType.QR_INQUIRY;
                break;
            case Constants.ACQ_QR_CREDIT:
                transType = ETransType.QR_INQUIRY_CREDIT;
                break;
            case Constants.ACQ_DOLFIN_INSTALMENT:
                transType = ETransType.DOLFIN_INSTALMENT;
                break;
        }

        if (transType != null) {
            long[] tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(total.getAcquirer(), issuer, transType, VOIDED);
            long[] tempOffline = FinancialApplication.getTransDataDbHelper().countSumOf(total.getAcquirer(), issuer, ETransType.OFFLINE_TRANS_SEND, VOIDED);
            long[] tempSaleComp = FinancialApplication.getTransDataDbHelper().countSumOf(total.getAcquirer(), issuer, ETransType.SALE_COMPLETION, VOIDED);
            tempObj[0] = tempObj[0] + tempOffline[0] + tempSaleComp[0];
            tempObj[1] = tempObj[1] + tempOffline[1] + tempSaleComp[1];

            return new long[]{tempObj[0], tempObj[1]};
        }

        return new long[]{0, 0};
    }

    public long[] getTotalRefundByIssuer(Issuer issuer) {
        return FinancialApplication.getTransDataDbHelper().countSumOf(total.getAcquirer(), ETransType.REFUND, filter, issuer, false);
    }

    public long[] getTotalVoidRefundByIssuer(Issuer issuer) {
        return FinancialApplication.getTransDataDbHelper().countSumOf(total.getAcquirer(), issuer, ETransType.REFUND, VOIDED);
    }

    public void addPageTotalByIssuer(IPage page, String text, long totalNum, String totalAmt) {
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(text)
                        .setWeight(3.0f)
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText("" + totalNum)
                        .setFontSize(FONT_NORMAL)
                        .setWeight(3.0f)
                        .setGravity(Gravity.END));

        page.addLine()
                .adjustTopSpace(1)
                .addUnit(page.createUnit()
                        .setText(totalAmt)
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
    }

    public List<ETransType> getTransTypeByAcquirer() {
        List<ETransType> list = new ArrayList<>();

        switch (total.getAcquirer().getName()) {
            case Constants.ACQ_KPLUS:
                list.add(ETransType.QR_INQUIRY);
                list.add(ETransType.QR_VOID_KPLUS);
                list.add(ETransType.QR_VERIFY_PAY_SLIP);
                break;
            case Constants.ACQ_ALIPAY:
                list.add(ETransType.QR_INQUIRY_ALIPAY);
                list.add(ETransType.QR_VOID_ALIPAY);
                break;
            case Constants.ACQ_ALIPAY_B_SCAN_C:
                list.add(ETransType.QR_ALIPAY_SCAN);
                list.add(ETransType.QR_VOID_ALIPAY);
                break;
            case Constants.ACQ_WECHAT:
                list.add(ETransType.QR_INQUIRY_WECHAT);
                list.add(ETransType.QR_VOID_WECHAT);
                break;
            case Constants.ACQ_WECHAT_B_SCAN_C:
                list.add(ETransType.QR_WECHAT_SCAN);
                list.add(ETransType.QR_VOID_WECHAT);
                break;
            case Constants.ACQ_QR_CREDIT:
                list.add(ETransType.QR_INQUIRY_CREDIT);
                list.add(ETransType.QR_VOID_CREDIT);
                break;
            case Constants.ACQ_REDEEM:
            case Constants.ACQ_REDEEM_BDMS:
                list.add(ETransType.KBANK_REDEEM_VOID);
                list.add(ETransType.KBANK_REDEEM_PRODUCT);
                list.add(ETransType.KBANK_REDEEM_PRODUCT_CREDIT);
                list.add(ETransType.KBANK_REDEEM_VOUCHER);
                list.add(ETransType.KBANK_REDEEM_VOUCHER_CREDIT);
                list.add(ETransType.KBANK_REDEEM_DISCOUNT);
                break;
            case Constants.ACQ_SMRTPAY:
            case Constants.ACQ_SMRTPAY_BDMS:
                list.add(ETransType.KBANK_SMART_PAY);
                list.add(ETransType.KBANK_SMART_PAY_VOID);
                break;
            case Constants.ACQ_AMEX_EPP:
                list.add(ETransType.AMEX_INSTALMENT);
                list.add(ETransType.VOID);
                break;
            case Constants.ACQ_BAY_INSTALLMENT:
                list.add(ETransType.BAY_INSTALMENT);
                list.add(ETransType.VOID);
                break;
            case Constants.ACQ_MY_PROMPT:
                list.add(ETransType.QR_MYPROMPT_SALE);
                list.add(ETransType.QR_MYPROMPT_VOID);
                break;
            case Constants.ACQ_DOLFIN_INSTALMENT:
                list.add(ETransType.DOLFIN_INSTALMENT);
                list.add(ETransType.DOLFIN_INSTALMENT_VOID);
                break;
            default:
                list.add(ETransType.SALE);
                list.add(ETransType.OFFLINE_TRANS_SEND);
                list.add(ETransType.SALE_COMPLETION);
                list.add(ETransType.VOID);
                list.add(ETransType.REFUND);
                break;
        }

        return list;
    }

}
