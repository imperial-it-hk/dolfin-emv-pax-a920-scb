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
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.TimeConverter;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.util.List;

public class ReceiptGeneratorEReceiptDetailReport implements IReceiptGenerator {

    private List<TransData> transDatas;

    public ReceiptGeneratorEReceiptDetailReport() {
    }

    public ReceiptGeneratorEReceiptDetailReport(List<TransData> transDatas) {
        this.transDatas = transDatas;
    }

    @Override
    public Bitmap generateBitmap() {
        IPage page = Device.generatePage();

        for (TransData transData : transDatas) {
            generateDetails(page, transData);
        }

        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();

        return imgProcessing.pageToBitmap(page, 384);
    }

    @Override
    public String generateString() {
        return null;
    }

    private void generateDetails(IPage page, TransData transData) {
        String temp;
        String temp2;
        ETransType transType = transData.getTransType();
        ETransType origTransType = transData.getOrigTransType();
        boolean isOfflineTransSend = ETransType.OFFLINE_TRANS_SEND == transType || ETransType.OFFLINE_TRANS_SEND == transData.getOrigTransType();
        boolean isRefund = ETransType.REFUND == transType || ETransType.REFUND == transData.getOrigTransType();
        String type = (!isOfflineTransSend) ? transType.toString().replaceAll("_", " ") : transType.getTransName().toUpperCase();

        //todo may be remove later if preauth/complete has not been merged
//        if (ETransType.PREAUTH == transType && !transData.getAcquirer().getName().equals(Constants.ACQ_UP)) {
//            type = Utils.getString(R.string.receipt_authorize);
//        }

        if (ETransType.VOID == transType) {
            type = (!isOfflineTransSend) ? (!isRefund) ? "VOID SALE" : "VOID REFUND" : "VOID OFFLINE";
//            type = (ETransType.PREAUTH_COMPLETE == origTransType) ? "VOID PREAUTH COMPLETE" : type;
        } /*else if (TransData.ETransStatus.ADJUSTED == transData.getTransState()) {
            type += Utils.getString(R.string.receipt_adj_round_bracket);
        }*/

        // AET-18
        // transaction NO/transaction type/auth
        temp = (!isOfflineTransSend) ? (transData.getAuthCode() == null ? "" : transData.getAuthCode()) : transData.getOrigAuthCode();
        temp2 = Component.getPaddedNumber(transData.getTraceNo(), 6);// TRACE

//        if (ETransType.PREAUTH_COMPLETE == transType || ETransType.PREAUTH_COMPLETE == origTransType) {
//            page.addLine()
//                    .addUnit(page.createUnit().setText(temp2))
//                    .addUnit(page.createUnit().setText(temp).setGravity(Gravity.END).setWeight(0.3f));
//            page.addLine()
//                    .addUnit(page.createUnit().setText(type).setGravity(Gravity.LEFT).setWeight(0.3f));
//        } else {
            page.addLine()
                    .addUnit(page.createUnit().setText(temp2 + "  " + type))
                    .addUnit(page.createUnit().setText(temp).setGravity(Gravity.END).setWeight(0.3f));
//        }

        //card NO/card type
        temp = PanUtils.maskCardNo(transData.getPan(), transData.getIssuer().getPanMaskPattern());
        temp2 = transData.getIssuer().getName();
        page.addLine()
                .addUnit(page.createUnit().setText(temp).setWeight(6))
                .addUnit(page.createUnit().setText(temp2).setWeight(5).setGravity(Gravity.END));

        //TOTAL AMOUNT
        page.addLine().addUnit(page.createUnit().setText(getTotalAmount(transData)).setGravity(Gravity.END));

        /* E-Receipt and E-Signature Status */
        // Upload date/time
        temp = TimeConverter.convert(transData.geteReceiptUploadDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.DATE_PATTERN_DISPLAY);

        temp2 = TimeConverter.convert(transData.geteReceiptUploadDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.TIME_PATTERN_DISPLAY4);
        page.addLine()
                .addUnit(page.createUnit().setText(temp))
                .addUnit(page.createUnit().setText(temp2).setGravity(Gravity.END));

        // E-Receipt type
        String eReceiptType = getEReceiptType(transData);
        page.addLine().addUnit(page.createUnit().setText(Utils.getString(R.string.history_ereceipt_type) + " " +
                Utils.getString(Utils.getResId("history_ereceipt_type_" + eReceiptType, "string"))).setGravity(Gravity.START));

        // Receipt
        page.addLine().addUnit(page.createUnit().setText(Utils.getString(R.string.report_ereceipt_title)));
        page.addLine()
                .addUnit(page.createUnit().setText("   " + Utils.getString(R.string.report_ereceipt_retry_num)))
                .addUnit(page.createUnit().setText("" + transData.geteReceiptRetry()).setGravity(Gravity.END));
        page.addLine()
                .addUnit(page.createUnit().setText("   " + Utils.getString(R.string.report_ereceipt_stauts)))
                .addUnit(page.createUnit().setText(transData.geteReceiptUploadStatus().toString()).setGravity(Gravity.END));

        // Printing Merchant copy status
        page.addLine().addUnit(page.createUnit().setText(Utils.getString(R.string.report_ereceipt_printing_status)));
        page.addLine().addUnit(page.createUnit().setText("   " + getPrintingStatus(transData)));
        /* **************** */

        page.addLine().addUnit(page.createUnit().setText(" "));
        page.addLine().addUnit(page.createUnit().setText(" "));

    }

    private String getTotalAmount(TransData transData) {
        String temp;
        if (transData.getTransType() != null && transData.getTransType().isSymbolNegative()) {
            temp = CurrencyConverter.convert(0 - Utils.parseLongSafe(transData.getAmount(), 0), transData.getCurrency());
        } else {
            temp = CurrencyConverter.convert(Utils.parseLongSafe(transData.getAmount(), 0), transData.getCurrency());
        }
        return temp;
    }

    //todo may be remove later if tip/adjustment has not been implemented
    /*private String getBaseTipAmount(TransData transData) {
        String result = null;

        String acq = transData.getAcquirer().getName();
        boolean enableTip = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_SUPPORT_TIP);
        if (enableTip && !(Constants.ACQ_BBL_PROMPT.equals(acq) || Constants.ACQ_QRC.equals(acq)
                || Constants.ACQ_WALLET.equals(acq) || Constants.ACQ_LINEPAY.equals(acq) || Constants.ACQ_BBL_BSS.equals(acq))) {

            String temp, temp2;
            if (transData.isDccRequired()) {
                long localBaseamt = Utils.parseLongSafe(transData.getDccAmount(), 0) - Utils.parseLongSafe(transData.getDccTipAmount(), 0);
                long localTipamt = Utils.parseLongSafe(transData.getDccTipAmount(), 0);
                temp = (localTipamt > 0) ? CurrencyConverter.convert(Component.convertToLocalAmount(transData, localBaseamt), transData.getCurrency()) :
                        CurrencyConverter.convert(Utils.parseLongSafe(transData.getAmount(), 0), transData.getCurrency());
                temp2 = CurrencyConverter.convert(Component.convertToLocalAmount(transData, localTipamt), transData.getCurrency());
            } else {
                temp = CurrencyConverter.convert(Utils.parseLongSafe(transData.getAmount(), 0) -
                        Utils.parseLongSafe(transData.getTipAmount(), 0), transData.getCurrency());
                temp2 = CurrencyConverter.convert(Utils.parseLongSafe(transData.getTipAmount(), 0), transData.getCurrency());
            }

            if (transData.getTransType() != null && transData.getTransType().isSymbolNegative()) {
                result = "-(" + temp + " + " + temp2 + ")";
            } else {
                result = temp + " + " + temp2;
            }
        }

        return result;
    }*/

    private String getEReceiptType(TransData transData) {
        if (transData.isPinVerifyMsg()) {
            return "01";
        } else {
            if (transData.isTxnSmallAmt()) {
                return "03";
            } else {
                if (transData.getSignData() != null) {
                    return "04";
                }
            }
        }
        return "00";
    }

    private String getPrintingStatus(TransData transData) {
        TransData.UploadStatus uploadStatus = transData.geteReceiptUploadStatus();

        if (transData.geteReceiptUploadStatus() == TransData.UploadStatus.NORMAL)
            return "On E-Receipt Server.";// in case success, just refer to PRP server.

        switch (uploadStatus) {
            case NORMAL:
                if (transData.isEReceiptManualPrint()) {
                    return "Already print out.";
                }
                return "Printing not required.";
            case PENDING:
                if (transData.isEReceiptManualPrint()) {
                    return "Already print out.";
                }
                return "Printing not required.";
            case UPLOAD_FAILED_MANUAL_PRINT:
                if (transData.isEReceiptManualPrint()) {
                    return "Already print out and save E-Receipt image to storage.";
                }
                return "Printing not required and save E-Receipt image to storage.";
        }

        return "";
    }

    public Bitmap generateHeaderBitmap() {
        IPage page = Device.generatePage();
        /*-------------------------------- Header (logo) --------------------------------*/
        // title
        page.addLine()
                .addUnit(page.createUnit()
                        .setBitmap(Component.getImageFromInternalFile(Constants.SLIP_LOGO_NAME))
                        .setGravity(Gravity.CENTER));
        page.addLine().addUnit(page.createUnit().setText(" "));

        //merchant name
        String merName = FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_MERCHANT_NAME_EN);
        String merAddress= FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_MERCHANT_ADDRESS);
        String merAddress1= FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_MERCHANT_ADDRESS1);
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
        // title
        page.addLine().addUnit(page.createUnit().setText(Utils.getString(R.string.menu_manage_item_detail_report)).setFontSize(FONT_BIG).setTextStyle(Typeface.BOLD).setGravity(Gravity.CENTER));

        String dateTime = Device.getTime(Constants.TIME_PATTERN_DISPLAY);
        String date = TimeConverter.convert(dateTime, Constants.TIME_PATTERN_DISPLAY, Constants.DATE_PATTERN_DISPLAY);
        String time = TimeConverter.convert(dateTime, Constants.TIME_PATTERN_DISPLAY, Constants.TIME_PATTERN_DISPLAY4);

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

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setGravity(Gravity.CENTER));

        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_trans) + "     " + Utils.getString(R.string.trans))
                        .setFontSize(FONT_NORMAL_22)
                        .setWeight(2.0f))
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_auth_code_short))
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

        page.addLine()
                .addUnit(page.createUnit()
                        .setText("UPLOAD " + Utils.getString(R.string.receipt_date_short))
                        .setFontSize(FONT_NORMAL_22))
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_time_short))
                        .setFontSize(FONT_NORMAL_22)
                        .setGravity(Gravity.END));

        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.history_ereceipt_type_cap))
                        .setFontSize(FONT_NORMAL_22))
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.history_ereceipt_desc))
                        .setFontSize(FONT_NORMAL_22)
                        .setGravity(Gravity.END));

        page.addLine().addUnit(page.createUnit().setText(" "));

        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();

        return imgProcessing.pageToBitmap(page, 384);
    }
}
