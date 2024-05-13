package com.pax.pay.trans.receipt;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
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

import th.co.bkkps.utils.Log;
import th.co.bkkps.utils.PageToSlipFormat;

import static com.pax.pay.trans.model.TransData.ETransStatus.ADJUSTED;
import static com.pax.pay.trans.model.TransData.ETransStatus.NORMAL;

/**
 * Created by SORAYA S on 28-May-18.
 */

public class ReceiptGeneratorAuditReportTOPS implements IReceiptGenerator {

    private List<TransData> transDataList;
    private TransTotal total;
    private Acquirer acquirer;
    private static final List<TransData.ETransStatus> filter = new ArrayList<>();
    private static final List<ETransType> types = new ArrayList<>();
    private final static int MAX_SIZE = 10;

    public ReceiptGeneratorAuditReportTOPS(List<TransData> transDataList, TransTotal total, Acquirer acquirer) {
        this.transDataList = transDataList;
        this.total = total;
        this.acquirer = acquirer;
    }

    @Override
    public Bitmap generateBitmap() {
        IPage page = Device.generatePage(true);
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
                    Component.generateTransDetailWalletKbank(transData, page, FONT_SMALL);
                    break;
                case Constants.ACQ_ALIPAY_B_SCAN_C:
                    Component.generateTransDetailWalletKbank(transData, page, FONT_SMALL);
                    break;
                case Constants.ACQ_WECHAT:
                    Component.generateTransDetailWalletKbank(transData, page, FONT_SMALL);
                    break;
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
        IPage page = Device.generatePage(true);
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

        int transNo = 0, j, transPerPage;
        int tranSize = transDataList != null ? transDataList.size() : 0;
        int totalPage = (int) Math.ceil((double) tranSize / MAX_SIZE);
        for (int i = 1 ; i <= totalPage ; i++) {
            page = Device.generatePage(true);
            transPerPage = (transPerPage = i * MAX_SIZE) > tranSize ? tranSize : transPerPage;
            for (j = transNo ; j < transPerPage ; j++) {
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
                    case Constants.ACQ_ALIPAY_B_SCAN_C:
                    case Constants.ACQ_WECHAT:
                    case Constants.ACQ_WECHAT_B_SCAN_C:
                    case Constants.ACQ_QR_CREDIT:
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
                    case Constants.ACQ_AMEX_EPP:
                        Component.generateTransDetailAmexInstalment(transData, page, FONT_SMALL);
                        break;
                    case Constants.ACQ_BAY_INSTALLMENT:
                        Component.generateTransDetailInstalmentBay(transData, page, FONT_SMALL);
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
            page = Device.generatePage(true);
            Component.generateTransDetailDccKbank(transDataDccList, page);
            bitmaps.add(imgProcessing.pageToBitmap(page, 384));
        }

        page = Device.generatePage(true);
        generateFooterOfDetail(page);
        bitmaps.add(imgProcessing.pageToBitmap(page, 384));

        if (isAllAcq) {
//            List<Acquirer> acquirerList = FinancialApplication.getAcqManager().findEnableAcquirersExcept(Constants.ACQ_DOLFIN);//hw3
            List<String> excludeAcqs = Arrays.asList(Constants.ACQ_DOLFIN, Constants.ACQ_SCB_IPP, Constants.ACQ_SCB_REDEEM);
            List<Acquirer> acquirerList = FinancialApplication.getAcqManager().findEnableAcquirersExcept(excludeAcqs);
            for (Acquirer acquirer : acquirerList) {
                if ((Constants.ACQ_REDEEM.equals(acquirer.getName()) || Constants.ACQ_REDEEM_BDMS.equals(acquirer.getName())) && isRedeemKbank) {
                    page = Device.generatePage(true);
                    generateRedeemGrandTotal(page, acquirer, true);
                    bitmaps.add(imgProcessing.pageToBitmap(page, 384));
                } else {
                    generateTotalByIssuer(acquirer, true, bitmaps, imgProcessing);
				}
			}
            page = Device.generatePage(true);
            generateGrandTotal(page, acquirerList);
			bitmaps.add(imgProcessing.pageToBitmap(page, 384));
        } else {
            if (Constants.ACQ_REDEEM.equals(acquirer.getName()) || Constants.ACQ_REDEEM_BDMS.equals(acquirer.getName())) {
                page = Device.generatePage(true);
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
            IPage page = Device.generatePage(true);
            Component.generateTotalDetailMainInfoAuditTOPS(page, acquirer, "TOTALS BY CARD", FONT_NORMAL,isAllAcq);

            PageToSlipFormat.getInstance().isSettleMode=true;
            PageToSlipFormat.getInstance().Append(page);

            bitmaps.add(imgProcessing.pageToBitmap(page, 384));
            List<Issuer> issuerList = filterIssuers(issuers, filter, acquirer);
            if(Constants.ACQ_WALLET.equals(acquirer.getName())) {
                page = Device.generatePage(true);
                Component.generateTotalWalletByIssuer(page, acquirer, filter, issuerList, FONT_NORMAL);
                PageToSlipFormat.getInstance().isSettleMode=true;
                PageToSlipFormat.getInstance().Append(page);
                bitmaps.add(imgProcessing.pageToBitmap(page, 384));
            } else {
                Component.generateTotalByIssuerBitmapArrayTOPS(acquirer, filter, issuerList, FONT_NORMAL, bitmaps, imgProcessing);

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
                .setScaleX(1.2f)
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
                        .setFontSize(FONT_NORMAL_22)
                        .setWeight(3.0f))
                .addUnit(page.createUnit()
                        .setText(time)
                        .setFontSize(FONT_NORMAL_22)
                        .setGravity(Gravity.END)
                        .setWeight(3.0f));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setScaleX(1.2f)
                .setGravity(Gravity.CENTER));

        if(!isAllAcq){
            // Change acquirer name Prompt Pay on slip
            String acqName = acquirer.getName();
            //HOST
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("HOST")
                            .setFontSize(FONT_NORMAL_22))
                    .addUnit(page.createUnit()
                            .setText(": " + acqName)
                            .setWeight(3.0f)
                            .setFontSize(FONT_NORMAL_22));

            // terminal ID/operator ID
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("TID")
                            .setFontSize(FONT_NORMAL_22))
                    .addUnit(page.createUnit()
                                    .setText(": " +acquirer.getTerminalId())
                                    .setWeight(3.0f)
                                    .setGravity(Gravity.LEFT));
            // merchant ID
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("MID")
                            .setFontSize(FONT_NORMAL_22))
                    .addUnit(page.createUnit()
                            .setText(": " +acquirer.getMerchantId())
                            .setGravity(Gravity.LEFT)
                            .setWeight(3.0f));

            // batch NO
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("BATCH")
                            .setFontSize(FONT_NORMAL_22))
                    .addUnit(page.createUnit()
                            .setText(": " + Component.getPaddedNumber(acquirer.getCurrBatchNo(), 6))
                            .setGravity(Gravity.LEFT)
                            .setWeight(3.0f));
        }

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setScaleX(1.2f)
                .setGravity(Gravity.CENTER));

        /*-------------------------------- Header (Trans Info.) --------------------------------*/

        if(!Constants.ACQ_QR_PROMPT.equals(acquirer.getName())){
            // transaction information
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.audit_report_host_name))
                            .setFontSize(FONT_NORMAL_22)
                            .setWeight(2.0f)
                            .setGravity(Gravity.START))
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

        return;
    }

    private void generateFooterOfDetail(IPage page) {

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setScaleX(1.2f)
                .setGravity(Gravity.CENTER));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_end_audit_report))
                .setFontSize(FONT_NORMAL_22)
                .setTextStyle(Typeface.BOLD).setGravity(Gravity.CENTER));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_double_line))
                .setScaleX(0.8f)
                .setGravity(Gravity.CENTER));

        return;
    }

    private void generateGrandTotal(IPage page, List<Acquirer> acquirerList) {
        long grandSaleNum = 0, grandSaleAmt = 0;
        long grandSaleVoidNum = 0, grandSaleVoidAmt = 0;
        long grandRefundNum = 0, grandRefundAmt = 0;
        long grandTopUpNum = 0, grandTopUpAmt = 0;
        long grandTopUpRefundNum = 0, grandTopUpRefundAmt = 0;
        long grandSaleSmallAmtNum = 0, grandSaleSmallAmtAmt = 0;
        long grandSaleVoidSmallAmtNum = 0, grandSaleVoidSmallAmtAmt = 0;
        long grandVoidRefundNum = 0, grandVoidRefundAmt = 0;
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
            grandSaleSmallAmtNum = total.getSaleTotalSmallAmtNum();
            grandSaleSmallAmtAmt = total.getSaleTotalSmallAmt();
            grandSaleVoidSmallAmtNum = total.getVoidTotalSmallAmtNum();
            grandSaleVoidSmallAmtAmt = total.getVoidTotalSmallAmt();
            grandVoidRefundNum = total.getRefundVoidTotalNum();
            grandVoidRefundAmt = total.getRefundVoidTotalAmt();
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
        float halfWidthSize = 0.6f;
        float fullSizeOneLine = 1.2f;
        float fullSizeDoubleLine = 0.8f;

        if (numTotal > 0) {
            // title
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_grand_totals))
                            .setFontSize(FONT_NORMAL));
            page.addLine().addUnit(page.createUnit().setText(" "));

            // sale all
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.trans_sale).toUpperCase())
                            .setWeight(2.0f)
                            .setScaleX(halfWidthSize)
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(Component.getPaddedNumber((totalSaleNum),3))
                            .setFontSize(FONT_NORMAL)
                            .setWeight(1.0f)
                            .setScaleX(halfWidthSize)
                            .setGravity(Gravity.CENTER))
                    .addUnit(page.createUnit()
                            .setText(CurrencyConverter.convert(totalSaleAmt))
                            .setFontSize(FONT_NORMAL)
                            .setWeight(2.0f)
                            .setScaleX(halfWidthSize)
                            .setGravity(Gravity.END));

            // sale prompt
            if (salePromptNum > 0) {
                // sale
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText("SALE PROMPT")
                                .setWeight(2.0f)
                                .setScaleX(halfWidthSize)
                                .setFontSize(FONT_NORMAL))
                        .addUnit(page.createUnit()
                                .setText(Component.getPaddedNumber((salePromptNum),3))
                                .setFontSize(FONT_NORMAL)
                                .setWeight(1.0f)
                                .setScaleX(halfWidthSize)
                                .setGravity(Gravity.CENTER))
                        .addUnit(page.createUnit()
                                .setText(CurrencyConverter.convert(salePromptAmt))
                                .setFontSize(FONT_NORMAL)
                                .setWeight(2.0f)
                                .setScaleX(halfWidthSize)
                                .setGravity(Gravity.END));
            }

            // sale QRC
            if (saleQRCNum > 0) {
                // sale
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText("SALE QRC")
                                .setWeight(2.0f)
                                .setScaleX(halfWidthSize)
                                .setFontSize(FONT_NORMAL))
                        .addUnit(page.createUnit()
                                .setText(Component.getPaddedNumber((saleQRCNum),3))
                                .setFontSize(FONT_NORMAL)
                                .setWeight(1.0f)
                                .setScaleX(halfWidthSize)
                                .setGravity(Gravity.CENTER))
                        .addUnit(page.createUnit()
                                .setText(CurrencyConverter.convert(saleQRCAmt))
                                .setFontSize(FONT_NORMAL)
                                .setWeight(2.0f)
                                .setScaleX(halfWidthSize)
                                .setGravity(Gravity.END));
            }

            // refund
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.trans_refund).toUpperCase())
                            .setWeight(2.0f)
                            .setScaleX(halfWidthSize)
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(Component.getPaddedNumber(grandRefundNum,3))
                            .setWeight(1.0f)
                            .setScaleX(halfWidthSize)
                            .setGravity(Gravity.CENTER)
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(CurrencyConverter.convert(0 - grandRefundAmt))
                            .setFontSize(FONT_NORMAL)
                            .setWeight(2.0f)
                            .setScaleX(halfWidthSize)
                            .setGravity(Gravity.END));


            // void
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_void_sale).toUpperCase())
                            .setWeight(2.0f)
                            .setScaleX(halfWidthSize)
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(Component.getPaddedNumber(grandSaleVoidNum,3))
                            .setWeight(1.0f)
                            .setScaleX(halfWidthSize)
                            .setGravity(Gravity.CENTER)
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(CurrencyConverter.convert(0 - grandSaleVoidAmt))
                            .setWeight(2.0f)
                            .setScaleX(halfWidthSize)
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.END));


            // void refund
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.trans_void).toUpperCase() + " REFUND")
                            .setWeight(2.0f)
                            .setScaleX(halfWidthSize)
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(Component.getPaddedNumber(grandVoidRefundNum,3))
                            .setWeight(1.0f)
                            .setScaleX(halfWidthSize)
                            .setGravity(Gravity.CENTER)
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(CurrencyConverter.convert(0 - grandVoidRefundAmt))
                            .setWeight(2.0f)
                            .setScaleX(halfWidthSize)
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.END));

            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.receipt_one_line))
                    .setScaleX(fullSizeOneLine));

            long grandNum = grandSaleNum + grandRefundNum + grandTopUpNum + grandTopUpRefundNum;
            long grandAmt = grandSaleAmt - grandRefundAmt + grandTopUpAmt - grandTopUpRefundAmt;

            // total
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("GRAND TOTAL")
                            .setScaleX(halfWidthSize)
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(CurrencyConverter.convert(grandAmt))
                            .setScaleX(halfWidthSize)
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.END));

            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.receipt_double_line))
                    .setScaleX(fullSizeDoubleLine)
                    .setGravity(Gravity.CENTER));


            // sale small amount
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.trans_sale).toUpperCase() + " SMALL TICKET")
                            .setWeight(2.0f)
                            .setScaleX(halfWidthSize)
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(Component.getPaddedNumber(grandSaleSmallAmtNum,3))
                            .setWeight(1.0f)
                            .setScaleX(halfWidthSize)
                            .setGravity(Gravity.CENTER)
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(CurrencyConverter.convert(grandSaleSmallAmtAmt))
                            .setWeight(2.0f)
                            .setScaleX(halfWidthSize)
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.END));

            // sale normal
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.trans_sale).toUpperCase() + " NORMAL")
                            .setWeight(2.0f)
                            .setScaleX(halfWidthSize)
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(Component.getPaddedNumber((totalSaleNum - grandSaleSmallAmtNum),3))
                            .setWeight(1.0f)
                            .setScaleX(halfWidthSize)
                            .setGravity(Gravity.CENTER)
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(CurrencyConverter.convert(grandSaleAmt - grandSaleSmallAmtAmt))
                            .setWeight(2.0f)
                            .setScaleX(halfWidthSize)
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.END));

            // top up
            if (grandTopUpNum != 0.00) {
                String tmpTopUp = CurrencyConverter.convert(grandTopUpAmt);
                if (grandTopUpAmt != 0.00) {
                    tmpTopUp = "- " + tmpTopUp;
                }
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.trans_topup).toUpperCase())
                                .setWeight(2.0f)
                                .setScaleX(halfWidthSize)
                                .setFontSize(FONT_NORMAL))
                        .addUnit(page.createUnit()
                                .setText(Component.getPaddedNumber(grandTopUpNum,3))
                                .setWeight(1.0f)
                                .setScaleX(halfWidthSize)
                                .setGravity(Gravity.CENTER)
                                .setFontSize(FONT_NORMAL))
                        .addUnit(page.createUnit()
                                .setText(tmpTopUp)
                                .setWeight(2.0f)
                                .setScaleX(halfWidthSize)
                                .setFontSize(FONT_NORMAL)
                                .setGravity(Gravity.END));
            }


            // refund
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.trans_refund).toUpperCase())
                            .setWeight(2.0f)
                            .setScaleX(halfWidthSize)
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(Component.getPaddedNumber(grandRefundNum,3))
                            .setWeight(1.0f)
                            .setScaleX(halfWidthSize)
                            .setGravity(Gravity.CENTER)
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(CurrencyConverter.convert(0 - grandRefundAmt))
                            .setFontSize(FONT_NORMAL)
                            .setWeight(2.0f)
                            .setScaleX(halfWidthSize)
                            .setGravity(Gravity.END));

            // top up refund
            if (grandTopUpRefundNum != 0.00) {
                String tmpTopUpRefund = CurrencyConverter.convert(grandTopUpRefundAmt);
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText("TOPUP REFUND")
                                .setWeight(2.0f)
                                .setScaleX(halfWidthSize)
                                .setFontSize(FONT_NORMAL))
                        .addUnit(page.createUnit()
                                .setText(Component.getPaddedNumber(grandTopUpRefundNum,3))
                                .setWeight(1.0f)
                                .setScaleX(halfWidthSize)
                                .setGravity(Gravity.CENTER)
                                .setFontSize(FONT_NORMAL))
                        .addUnit(page.createUnit()
                                .setText(tmpTopUpRefund)
                                .setWeight(2.0f)
                                .setScaleX(halfWidthSize)
                                .setFontSize(FONT_NORMAL)
                                .setGravity(Gravity.END));
            }

            // void sale small amount
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.trans_void).toUpperCase()  + " SMALL TICKET")
                            .setWeight(2.0f)
                            .setScaleX(halfWidthSize)
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(Component.getPaddedNumber(grandSaleVoidSmallAmtNum,3))
                            .setWeight(1.0f)
                            .setScaleX(halfWidthSize)
                            .setGravity(Gravity.CENTER)
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(CurrencyConverter.convert(0 - grandSaleVoidSmallAmtAmt))
                            .setWeight(2.0f)
                            .setScaleX(halfWidthSize)
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.END));

            // void sale
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.trans_void).toUpperCase() + " NORMAL")
                            .setWeight(2.0f)
                            .setScaleX(halfWidthSize)
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(Component.getPaddedNumber((grandSaleVoidNum - grandSaleVoidSmallAmtNum),3))
                            .setWeight(1.0f)
                            .setScaleX(halfWidthSize)
                            .setGravity(Gravity.CENTER)
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(CurrencyConverter.convert(0 - (grandSaleVoidAmt- grandSaleVoidSmallAmtAmt)))
                            .setWeight(2.0f)
                            .setScaleX(halfWidthSize)
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.END));

            // void refund
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.trans_void).toUpperCase() + " REFUND")
                            .setWeight(2.0f)
                            .setScaleX(halfWidthSize)
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(Component.getPaddedNumber(grandVoidRefundNum,3))
                            .setWeight(1.0f)
                            .setScaleX(halfWidthSize)
                            .setGravity(Gravity.CENTER)
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(CurrencyConverter.convert(grandVoidRefundAmt))
                            .setWeight(2.0f)
                            .setScaleX(halfWidthSize)
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.END));

            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.receipt_one_line))
                    .setScaleX(fullSizeOneLine));
            // total
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("GRAND TOTAL")
                            .setScaleX(halfWidthSize)
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(CurrencyConverter.convert(grandAmt))
                            .setScaleX(halfWidthSize)
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.END));
            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.receipt_double_line))
                    .setScaleX(fullSizeDoubleLine)
                    .setGravity(Gravity.CENTER));
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
