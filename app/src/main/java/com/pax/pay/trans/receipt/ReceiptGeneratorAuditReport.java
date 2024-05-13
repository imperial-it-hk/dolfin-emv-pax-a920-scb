package com.pax.pay.trans.receipt;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import th.co.bkkps.utils.Log;
import android.view.Gravity;

import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.glwrapper.imgprocessing.IImgProcessing;
import com.pax.glwrapper.page.IPage;
import com.pax.pay.ECR.EcrData;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.Issuer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.model.TransRedeemKbankTotal;
import com.pax.pay.trans.model.TransTotal;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.TimeConverter;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.pax.pay.trans.model.TransData.ETransStatus.ADJUSTED;
import static com.pax.pay.trans.model.TransData.ETransStatus.NORMAL;
import static com.pax.pay.trans.model.TransData.ETransStatus.VOIDED;

/**
 * Created by SORAYA S on 28-May-18.
 */

public class ReceiptGeneratorAuditReport implements IReceiptGenerator {

    private List<TransData> transDataList;
    private TransTotal total;
    private Acquirer acquirer;
    private static final List<TransData.ETransStatus> filter = new ArrayList<>();
    private static final List<ETransType> types = new ArrayList<>();
    private final static int MAX_PAGE = 100;

    public ReceiptGeneratorAuditReport(List<TransData> transDataList, TransTotal total, Acquirer acquirer) {
        this.transDataList = transDataList;
        this.total = total;
        this.acquirer = acquirer;
    }

    @Override
    public Bitmap generateBitmap() {
        IPage page = Device.generatePage();
         boolean isAllAcq = false;
         boolean isRedeemKbank = false;
         if(Utils.getString(R.string.acq_all_acquirer).equals(acquirer.getName())){
             isAllAcq = true;
         }
        generateHeaderOfDetail(page,isAllAcq);

        List<TransData> transDataDccList =  new ArrayList<>();
        for (TransData transData : transDataList) {
            String acquirerName = transData.getAcquirer().getName();
            switch (acquirerName) {
                case Constants.ACQ_QR_PROMPT:
                    Component.generateTransDetailPromptPay(transData, page, FONT_SMALL);
                    break;
                case Constants.ACQ_WALLET:
                    Component.generateTransDetailWallet(transData, page, FONT_SMALL);
                    break;
                case Constants.ACQ_QRC:
                    Component.generateTransDetailQRSale(transData, page, FONT_SMALL);
                    break;
                case Constants.ACQ_KPLUS:
                    Component.generateTransDetailWalletKbank(transData, page, FONT_SMALL);
                    break;
                case Constants.ACQ_ALIPAY:
                case Constants.ACQ_ALIPAY_B_SCAN_C:
                    Component.generateTransDetailWalletKbank(transData, page, FONT_SMALL);
                    break;
                case Constants.ACQ_WECHAT:
                case Constants.ACQ_WECHAT_B_SCAN_C:
                    Component.generateTransDetailWalletKbank(transData, page, FONT_SMALL);
                    break;
                case Constants.ACQ_REDEEM:
                case Constants.ACQ_REDEEM_BDMS:
                    Component.generateTransDetailKbankRedeem(transData, page, FONT_SMALL);
                    isRedeemKbank = true;
                    break;
                case Constants.ACQ_SMRTPAY:
                case Constants.ACQ_SMRTPAY_BDMS:
                case Constants.ACQ_DOLFIN_INSTALMENT:
                    Component.generateTransDetailInstalmentKbank(transData, page,FONT_SMALL);
                    break;
                case Constants.ACQ_DCC:
                    transDataDccList.add(transData);
                    break;
                default:
                    Component.generateTransDetail(transData, page,FONT_SMALL);
                    break;
            }
        }

        if (transDataDccList.size() > 0){
            Component.generateTransDetailDccKbank(transDataDccList, page);
        }

        generateFooterOfDetail(page);

        if (Utils.getString(R.string.acq_all_acquirer).equals(acquirer.getName())) {
            List<Acquirer> acquirerList = FinancialApplication.getAcqManager().findEnableAcquirers();
            for (Acquirer acquirer : acquirerList) {
                if ((Constants.ACQ_REDEEM.equals(acquirer.getName()) || Constants.ACQ_REDEEM_BDMS.equals(acquirer.getName())) && isRedeemKbank) {
                    generateRedeemGrandTotal(page, acquirer, isAllAcq);
                } else {
                    generateTotalByIssuer(page, acquirer, isAllAcq);
                }
            }
            generateGrandTotal(page,acquirerList);
        } else {
            if (Constants.ACQ_REDEEM.equals(acquirer.getName()) || Constants.ACQ_REDEEM_BDMS.equals(acquirer.getName())) {
                generateRedeemGrandTotal(page, acquirer, isAllAcq);
            } else {
                generateTotalByIssuer(page, acquirer, isAllAcq);
            }
        }

        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }

    @Override
    public String generateString() {
        return null;
    }

    public List<Bitmap> generateArrayOfBitmap() {
        IPage page = Device.generatePage();
        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        List<Bitmap> bitmaps = new ArrayList<>();

        boolean isAllAcq = false;
        boolean isRedeemKbank = false;
        if(Utils.getString(R.string.acq_all_acquirer).equals(acquirer.getName())){
            isAllAcq = true;
        }
        generateHeaderOfDetail(page,isAllAcq);
        bitmaps.add(imgProcessing.pageToBitmap(page, 384));

        List<TransData> transDataDccList =  new ArrayList<>();

        int transNo = 0, j, tempSize;
        int tranSize = transDataList != null ? transDataList.size() : 0;
        int totalPage = (int) Math.ceil((double) tranSize / MAX_PAGE);
        for (int i = 1 ; i <= totalPage ; i++) {
            page = Device.generatePage();
            tempSize = (tempSize = i * MAX_PAGE) > tranSize ? tranSize : tempSize;
            for (j = transNo ; j < tempSize ; j++) {
                TransData transData = transDataList.get(j);
                String acquirerName = transData.getAcquirer().getName();
                switch (acquirerName) {
                    case Constants.ACQ_QR_PROMPT:
                        Component.generateTransDetailPromptPay(transData, page, FONT_SMALL);
                        break;
                    case Constants.ACQ_WALLET:
                        Component.generateTransDetailWallet(transData, page, FONT_SMALL);
                        break;
                    case Constants.ACQ_QRC:
                        Component.generateTransDetailQRSale(transData, page, FONT_SMALL);
                        break;
                    case Constants.ACQ_KPLUS:
                    case Constants.ACQ_ALIPAY:
                    case Constants.ACQ_WECHAT:
                    case Constants.ACQ_ALIPAY_B_SCAN_C:
                    case Constants.ACQ_WECHAT_B_SCAN_C:
                        Component.generateTransDetailWalletKbank(transData, page, FONT_SMALL);
                        break;
                    case Constants.ACQ_REDEEM:
                    case Constants.ACQ_REDEEM_BDMS:
                        Component.generateTransDetailKbankRedeem(transData, page, FONT_SMALL);
                        isRedeemKbank = true;
                        break;
                    case Constants.ACQ_SMRTPAY:
                    case Constants.ACQ_SMRTPAY_BDMS:
                    case Constants.ACQ_DOLFIN_INSTALMENT:
                        Component.generateTransDetailInstalmentKbank(transData, page, FONT_SMALL);
                        break;
                    case Constants.ACQ_DCC:
                        transDataDccList.add(transData);
                        break;
                    default:
                        Component.generateTransDetail(transData, page,FONT_SMALL);
                        break;
                }
            }
            transNo = j;
            bitmaps.add(imgProcessing.pageToBitmap(page, 384));
        }

        if (transDataDccList.size() > 0){
            page = Device.generatePage();
            Component.generateTransDetailDccKbank(transDataDccList, page);
            bitmaps.add(imgProcessing.pageToBitmap(page, 384));
        }

        page = Device.generatePage();
        generateFooterOfDetail(page);
        bitmaps.add(imgProcessing.pageToBitmap(page, 384));

        if (isAllAcq) {
//            List<Acquirer> acquirerList = FinancialApplication.getAcqManager().findEnableAcquirersExcept(Constants.ACQ_DOLFIN);///hw3
            List<String> excludeAcqs = Arrays.asList(Constants.ACQ_DOLFIN, Constants.ACQ_SCB_IPP, Constants.ACQ_SCB_REDEEM);
            List<Acquirer> acquirerList = FinancialApplication.getAcqManager().findEnableAcquirersExcept(excludeAcqs);
            for (Acquirer acquirer : acquirerList) {
                if ((Constants.ACQ_REDEEM.equals(acquirer.getName()) || Constants.ACQ_REDEEM_BDMS.equals(acquirer.getName())) && isRedeemKbank) {
                    page = Device.generatePage();
                    generateRedeemGrandTotal(page, acquirer, true);
                    bitmaps.add(imgProcessing.pageToBitmap(page, 384));
                } else {
                    generateTotalByIssuer(acquirer, true, bitmaps, imgProcessing);
				}
			}
            page = Device.generatePage();
            generateGrandTotal(page,acquirerList);
			bitmaps.add(imgProcessing.pageToBitmap(page, 384));
        } else {
            if (Constants.ACQ_REDEEM.equals(acquirer.getName()) || Constants.ACQ_REDEEM_BDMS.equals(acquirer.getName())) {
                page = Device.generatePage();
                generateRedeemGrandTotal(page, acquirer, false);
                bitmaps.add(imgProcessing.pageToBitmap(page, 384));
            } else {
                generateTotalByIssuer(acquirer, false, bitmaps, imgProcessing);
            }
        }

        bitmaps.removeAll(Collections.singleton(null));//remove null object

        return bitmaps;
    }

    private void generateTotalByIssuer(IPage page, Acquirer acquirer, boolean isAllAcq) {
        filter.add(NORMAL);
        filter.add(ADJUSTED);
        List<Issuer> issuers = FinancialApplication.getAcqManager().findAllIssuers();

        long totalOfAllIssuers = FinancialApplication.getTransDataDbHelper().countOf(acquirer, filter, issuers);

        if (totalOfAllIssuers != 0) {
            Component.generateTotalDetailMainInfoAudit(page, acquirer, Utils.getString(R.string.receipt_totals_by_issuer), FONT_NORMAL,isAllAcq);
            if(!Constants.ACQ_WALLET.equals(acquirer.getName())) {
                Component.generateTotalByIssuer(page, acquirer, filter, issuers, FONT_NORMAL);
            }else {
                Component.generateTotalWalletByIssuer(page, acquirer, filter, issuers, FONT_NORMAL);
            }
        }

        return;
    }

    private List<Bitmap> generateTotalByIssuer(Acquirer acquirer, boolean isAllAcq, List<Bitmap> bitmaps, IImgProcessing imgProcessing) {
        filter.add(NORMAL);
        filter.add(ADJUSTED);
        List<Issuer> issuers = FinancialApplication.getAcqManager().findAllIssuers();

        long totalOfAllIssuers = FinancialApplication.getTransDataDbHelper().countOf(acquirer, filter, issuers);

        if (totalOfAllIssuers != 0) {
            IPage page = Device.generatePage();
            Component.generateTotalDetailMainInfoAudit(page, acquirer, Utils.getString(R.string.receipt_totals_by_issuer), FONT_NORMAL,isAllAcq);
            bitmaps.add(imgProcessing.pageToBitmap(page, 384));
            List<Issuer> issuerList = filterIssuers(issuers, filter, acquirer);
            if(Constants.ACQ_WALLET.equals(acquirer.getName())) {
                page = Device.generatePage();
                Component.generateTotalWalletByIssuer(page, acquirer, filter, issuerList, FONT_NORMAL);
                bitmaps.add(imgProcessing.pageToBitmap(page, 384));
            } else {
                Component.generateTotalByIssuerBitmapArray(acquirer, filter, issuerList, FONT_NORMAL, bitmaps, imgProcessing);
            }
        }

        return bitmaps;
    }

    private void generateHeaderOfDetail(IPage page,boolean isAllAcq) {

        SysParam sysParam = FinancialApplication.getSysParam();
        /*-------------------------------- Header (logo) --------------------------------*/
        // title
        Bitmap logo;
        if (isAllAcq) {
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
        String merAddress= sysParam.get(SysParam.StringParam.EDC_MERCHANT_ADDRESS);
        String merAddress1= sysParam.get(SysParam.StringParam.EDC_MERCHANT_ADDRESS1);
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

        /*-------------------------------- Header (AUDIT REPORT) --------------------------------*/
        // title
        page.addLine().addUnit(page.createUnit().setText(Utils.getString(R.string.receipt_audit_report)).setFontSize(FONT_BIG).setTextStyle(Typeface.BOLD).setGravity(Gravity.CENTER));
        //page.addLine().addUnit(page.createUnit().setText(acquirer.getName().toUpperCase()).setFontSize(FONT_NORMAL).setTextStyle(Typeface.BOLD).setGravity(Gravity.CENTER));
        if(isAllAcq){
            page.addLine().addUnit(page.createUnit().setText("ALL ACQUIRERS").setFontSize(FONT_NORMAL).setTextStyle(Typeface.BOLD).setGravity(Gravity.CENTER));
        }

        String dateTime = Device.getTime(Constants.TIME_PATTERN_DISPLAY);
        String date = TimeConverter.convert(dateTime, Constants.TIME_PATTERN_DISPLAY, Constants.DATE_PATTERN_DISPLAY);
        String time = TimeConverter.convert(dateTime, Constants.TIME_PATTERN_DISPLAY, Constants.TIME_PATTERN_DISPLAY4);

        if (FinancialApplication.getEcrProcess() != null) {
            EcrData.instance.nDateTime = dateTime;
        }

        // date/time
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(date)
                        .setFontSize(FONT_NORMAL)
                        .setWeight(3.0f))
                .addUnit(page.createUnit()
                        .setText(time)
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END)
                        .setWeight(3.0f));

        if(!isAllAcq){
            // terminal ID/operator ID
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_terminal_code_short))
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(acquirer.getTerminalId())
                            .setWeight(4.0f)
                            .setGravity(Gravity.LEFT));
            // merchant ID
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_merchant_code_short))
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(acquirer.getMerchantId())
                            .setGravity(Gravity.LEFT)
                            .setWeight(4.0f));

            // Change acquirer name Prompt Pay on slip
            String acqName = acquirer.getName();

            // batch NO
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_batch_num_short))
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(Component.getPaddedNumber(transDataList.get(0).getBatchNo(), 6))
                            .setGravity(Gravity.LEFT)
                            .setWeight(1.0f))
                    .addUnit(page.createUnit()
                            .setText("HOST: " + acqName)
                            .setFontSize(FONT_SMALL)
                            .setGravity(Gravity.END)
                            .setWeight(2.0f));
        }

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setGravity(Gravity.CENTER));

        /*-------------------------------- Header (Trans Info.) --------------------------------*/

        if(!Constants.ACQ_QR_PROMPT.equals(acquirer.getName())){
            // transaction information
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.audit_report_host_name))
                            .setFontSize(FONT_NORMAL_22)
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.audit_report_host_nii))
                            .setFontSize(FONT_NORMAL_22)
                            .setGravity(Gravity.END));
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.audit_report_trans_type))
                            .setFontSize(FONT_NORMAL_22)
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_auth_code_short))
                            .setFontSize(FONT_NORMAL_22)
                            .setGravity(Gravity.END));
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.audit_report_trace_number) + "             " + Utils.getString(R.string.audit_report_stan_number))
                            .setFontSize(FONT_NORMAL_22)
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.audit_report_batch_number))
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
                            .setText(Utils.getString(R.string.receipt_card_no))
                            .setFontSize(FONT_NORMAL_22)
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_card_type))
                            .setFontSize(FONT_NORMAL_22)
                            .setGravity(Gravity.END));

            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_amount_total))
                            .setFontSize(FONT_NORMAL_22)
                            .setGravity(Gravity.END));

        }
        page.addLine().addUnit(page.createUnit().setText(" "));
        page.addLine().addUnit(page.createUnit().setText(" "));

        return;
    }

    private void generateFooterOfDetail(IPage page) {

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setGravity(Gravity.CENTER));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_end_audit_report))
                .setFontSize(FONT_NORMAL_22)
                .setTextStyle(Typeface.BOLD).setGravity(Gravity.CENTER));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_double_line))
                .setGravity(Gravity.CENTER));

        return;
    }

    private void generateGrandTotal(IPage page, List<Acquirer> acquirerList) {
        long grandSaleNum = 0, grandSaleAmt = 0;
        long grandSaleVoidNum = 0, grandSaleVoidAmt = 0;
        long grandRefundNum = 0, grandRefundAmt = 0;
        long grandTopUpNum = 0, grandTopUpAmt = 0;
        long grandTopUpRefundNum = 0, grandTopUpRefundAmt = 0;
        if (total != null) {
            grandSaleNum = total.getSaleTotalNum();
            grandSaleAmt = total.getSaleTotalAmt();
            grandSaleVoidNum = total.getSaleVoidTotalNum();
            grandSaleVoidAmt = total.getSaleVoidTotalAmt();
            grandRefundNum = total.getRefundTotalNum();
            grandRefundAmt = total.getRefundTotalAmt();
            grandTopUpNum = total.getTopupTotalNum();
            grandTopUpAmt = total.getTopupTotalAmt();
            grandTopUpRefundNum = total.getTopupVoidTotalNum();
            grandTopUpRefundAmt = total.getTopupVoidTotalAmt();
        }
        long totalSaleNum = grandSaleNum, totalSaleAmt = grandSaleAmt;
        long[] tempObj = new long[2];
        long salePromptNum = 0, salePromptAmt = 0;
        long saleQRCNum = 0, saleQRCAmt = 0;

        for(Acquirer acquirer : acquirerList){
            if(acquirer.getName().equalsIgnoreCase(Constants.ACQ_QR_PROMPT)){
                Issuer issuer = FinancialApplication.getAcqManager().findIssuer(Constants.ISSUER_PROMTPAY);

                //sale
                tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_SALE_INQUIRY, filter, issuer, false);
                long[] tempOffline = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_SALE_INQUIRY, filter, issuer, true);
                long [] obj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_INQUIRY_ID, filter, true, false);
                long[] tempObj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_INQUIRY_ID, filter, false, true);
                salePromptNum = tempObj[0] + tempOffline[0] + obj1[0] + tempObj1[0];
                salePromptAmt = tempObj[1] + tempOffline[1] + obj1[1] + tempObj1[1];

                if(salePromptNum > 0){
                    totalSaleNum -= salePromptNum;
                    totalSaleAmt -= salePromptAmt;
                }
            }else if (acquirer.getName().equalsIgnoreCase(Constants.ACQ_QRC)) {
                // only sale
                long[] saleObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_SALE_ALL_IN_ONE, filter, true, false);
                long[] InqObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.STATUS_INQUIRY_ALL_IN_ONE, filter, true, false);

                saleQRCNum = saleObj[0] + InqObj[0];
                saleQRCAmt = saleObj[1] + InqObj[1];

                if(saleQRCNum > 0){
                    totalSaleNum -= saleQRCNum;
                    totalSaleAmt -= saleQRCAmt;
                }
            }
        }

        long numTotal = totalSaleNum + grandSaleVoidNum + grandRefundNum + grandTopUpNum + grandTopUpRefundNum;

        if (numTotal > 0) {
            // sale
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_grand_totals))
                            .setFontSize(FONT_NORMAL));
            page.addLine().addUnit(page.createUnit().setText(" "));
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.trans_sale).toUpperCase())
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(" : " + totalSaleNum)
                            .setFontSize(FONT_NORMAL)
                            .setWeight(3.0f));
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(CurrencyConverter.convert(totalSaleAmt))
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.END));

            // sale prompt
            if (salePromptNum > 0) {
                // sale
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText("SALE PROMPT")
                                .setFontSize(FONT_NORMAL))
                        .addUnit(page.createUnit()
                                .setText(" : " + salePromptNum)
                                .setFontSize(FONT_NORMAL)).adjustTopSpace(1);
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(CurrencyConverter.convert(salePromptAmt))
                                .setFontSize(FONT_NORMAL)
                                .setGravity(Gravity.END)).adjustTopSpace(1);
            }

            // sale QRC
            if (saleQRCNum > 0) {
                // sale
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText("SALE QRC")
                                .setFontSize(FONT_NORMAL))
                        .addUnit(page.createUnit()
                                .setText(" : " + saleQRCNum)
                                .setFontSize(FONT_NORMAL)).adjustTopSpace(1);
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(CurrencyConverter.convert(saleQRCAmt))
                                .setFontSize(FONT_NORMAL)
                                .setGravity(Gravity.END)).adjustTopSpace(1);
            }

            // void
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.trans_void).toUpperCase())
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(" : " + grandSaleVoidNum)
                            .setFontSize(FONT_NORMAL)
                            .setWeight(3.0f));
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(CurrencyConverter.convert(0 - grandSaleVoidAmt))
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.END));

            // refund
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.trans_refund).toUpperCase())
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(" : " + grandRefundNum)
                            .setFontSize(FONT_NORMAL)
                            .setWeight(3.0f)).adjustTopSpace(1);
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(CurrencyConverter.convert(0 - grandRefundAmt))
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.END)).adjustTopSpace(1);

            // top up
            if (grandTopUpNum != 0.00) {
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.trans_topup).toUpperCase())
                                .setFontSize(FONT_NORMAL))
                        .addUnit(page.createUnit()
                                .setText(" : " + grandTopUpNum)
                                .setFontSize(FONT_NORMAL)
                                .setWeight(3.0f));
                String tmpTopUp = CurrencyConverter.convert(grandTopUpAmt);
                if (grandTopUpAmt != 0.00) {
                    tmpTopUp = "- " + tmpTopUp;
                }
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(tmpTopUp)
                                .setFontSize(FONT_NORMAL)
                                .setGravity(Gravity.END));
            }

            // top up refund
            if (grandTopUpRefundNum != 0.00) {
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText("TOPUP REFUND")
                                .setFontSize(FONT_NORMAL))
                        .addUnit(page.createUnit()
                                .setText(" : " + grandTopUpRefundNum)
                                .setFontSize(FONT_NORMAL)
                                .setWeight(1.0f));
                String tmpTopUpRefund = CurrencyConverter.convert(grandTopUpRefundAmt);
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(tmpTopUpRefund)
                                .setFontSize(FONT_NORMAL)
                                .setGravity(Gravity.END));
            }

            page.addLine().addUnit(page.createUnit()
                    .setText("---------")
                    .setGravity(Gravity.END));

            long grandNum = grandSaleNum + grandRefundNum + grandTopUpNum + grandTopUpRefundNum;
            long grandAmt = grandSaleAmt - grandRefundAmt + grandTopUpAmt - grandTopUpRefundAmt;

            // total
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("TOTAL")
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(" : " + grandNum)
                            .setFontSize(FONT_NORMAL)
                            .setWeight(3.0f));
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(CurrencyConverter.convert(grandAmt))
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.END));
        }
        return;
    }

    private void generateRedeemGrandTotal(IPage page, Acquirer acquirer, boolean isAllAcq) {
        TransRedeemKbankTotal rTotal = FinancialApplication.getTransTotalDbHelper().calTotalRedeemKbank(acquirer,false);

        Component.generateTotalDetailMainInfoAudit(page, acquirer, "REDEEM GRAND TOTAL", FONT_NORMAL, isAllAcq);

        long temp = 0, totalTemp = 0;
        if (rTotal != null && rTotal.getTotalSaleAllCards() > 0) {
            add2Units(page, "VISA", String.valueOf(rTotal.getVisaSum()));

            add2Units(page, "MASTERCARD", String.valueOf(rTotal.getMastercardSum()));

            add2Units(page, "JCB", String.valueOf(rTotal.getJcbSum()));

            add2Units(page, "OTHER", String.valueOf(rTotal.getOtherCardSum()));

            add2Units(page, "ALL CARD", String.valueOf(rTotal.getAllCardsSum()));

            page.addLine().addUnit(page.createUnit().setText(" "));

            add2Units(page, "ITEMS", String.valueOf(rTotal.getItemSum()));

            add2Units(page, "POINTS", String.format(Locale.getDefault(),"%,d", rTotal.getPointsSum()));

            add2Units(page, "REDEEM AMT", CurrencyConverter.convert(rTotal.getRedeemAmtSum()));

            add2Units(page, "CREDIT AMT", CurrencyConverter.convert(rTotal.getCreditSum()));

            add2Units(page, "TOTAL AMT", CurrencyConverter.convert(rTotal.getTotalSum()));
        } else {
            add2Units(page, "VISA", String.valueOf(temp));
            add2Units(page, "MASTER CARD", String.valueOf(temp));
            add2Units(page, "JCB", String.valueOf(temp));
            add2Units(page, "OTHER", String.valueOf(temp));
            add2Units(page, "ALL CARD", String.valueOf(temp));
            page.addLine().addUnit(page.createUnit().setText(" "));
            add2Units(page, "ITEMS", String.valueOf(temp));
            add2Units(page, "POINTS", String.valueOf(temp));
            add2Units(page, "REDEEM AMT", CurrencyConverter.convert(totalTemp));
            add2Units(page, "CREDIT AMT", CurrencyConverter.convert(totalTemp));
            add2Units(page, "TOTAL AMT", CurrencyConverter.convert(totalTemp));
        }

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_double_line))
                .setGravity(Gravity.CENTER));
    }

    private void add2Units(IPage page, String text1, String text2) {
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(text1)
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(text2)
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
    }

    private Bitmap getImageFromAssetsFile(String fileName) {
        Bitmap image = null;
        AssetManager am = FinancialApplication.getApp().getResources().getAssets();
        try {
            InputStream is = am.open(fileName);
            image = BitmapFactory.decodeStream(is);
            is.close();
        } catch (IOException e) {
            Log.e(TAG, "", e);
        }

        return image;

    }

    private List<Issuer> filterIssuers(List<Issuer> issuers, List<TransData.ETransStatus> filter, Acquirer acquirer) {
        List<Issuer> result = new ArrayList<>();
        for (Issuer issuer : issuers) {
            long[] tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, filter, issuer);
            if (tempObj[0] != 0 || tempObj[1] != 0) {
                result.add(issuer);
            }
        }
        return result;
    }
}
