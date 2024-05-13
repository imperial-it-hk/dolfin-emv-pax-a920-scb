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
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.ReservedFieldHandle;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.EReceiptUtils;
import com.pax.pay.utils.TimeConverter;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.text.DecimalFormat;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.Locale;

import th.co.bkkps.utils.Log;
import th.co.bkkps.utils.PageToSlipFormat;

public class ReceiptGeneratorInstallmentBAYTrans implements IReceiptGenerator {

    private int receiptNo = 0;
    private TransData transData;
    private boolean isRePrint = false;
    private int receiptMax = 2;
    private boolean isPrintPreview = false;

    /**
     * @param transData        ：transData
     * @param currentReceiptNo : currentReceiptNo
     * @param receiptMax       ：generate which one, start from 0
     * @param isReprint        ：is reprint?
     */
    public ReceiptGeneratorInstallmentBAYTrans(TransData transData, int currentReceiptNo, int receiptMax, boolean isReprint) {
        this.transData = transData;
        this.receiptNo = currentReceiptNo;
        this.isRePrint = isReprint;
        this.receiptMax = receiptMax;

        // Extra for ERM
        onErmMode=false;
        PageToSlipFormat.getInstance().Reset();
    }

    public ReceiptGeneratorInstallmentBAYTrans(TransData transData, int currentReceiptNo, int receiptMax, boolean isReprint, boolean isPrintPreview) {
        this.transData = transData;
        this.receiptNo = currentReceiptNo;
        this.isRePrint = isReprint;
        this.receiptMax = receiptMax;
        this.isPrintPreview = isPrintPreview;

        // Extra for ERM
        onErmMode=false;
        PageToSlipFormat.getInstance().Reset();
    }

    public ReceiptGeneratorInstallmentBAYTrans(TransData transData, int currentReceiptNo, int receiptMax, boolean isReprint, boolean isPrintPreview, boolean onErmMode) {
        this.transData = transData;
        this.receiptNo = currentReceiptNo;
        this.isRePrint = isReprint;
        this.receiptMax = receiptMax;
        this.isPrintPreview = isPrintPreview;

        // Extra for ERM
        this.onErmMode =onErmMode;
        PageToSlipFormat.getInstance().Reset();
    }

    private boolean onErmMode =false;

    @Override
    public Bitmap generateBitmap() {
        boolean isPrintSign = false;
        boolean isAmex = Constants.ACQ_AMEX.equals(transData.getAcquirer().getName());

        if (receiptNo > receiptMax) {
            receiptNo = 0;
        }

        if (receiptNo == 0 || ((receiptMax == 3) && receiptNo == 1)) {
            isPrintSign = true;
        }

        IPage page = Device.generatePage();
        // transaction type
        ETransType transType = transData.getTransType();
        TransData.ETransStatus transStatus = transData.getTransState();

        SysParam sysParam = FinancialApplication.getSysParam();
        Acquirer acquirer = transData.getAcquirer();
        String temp;

        /*------------------------------Header------------------------------*/
        // title
        if (!onErmMode) {
            page.addLine()
                    .addUnit(page.createUnit()
                            .setBitmap(Component.getSmartPayFromInternalFile(acquirer.getNii() + "_" + acquirer.getName()))
                            .setGravity(Gravity.CENTER));
        } else {
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("[@]" + transData.getInitAcquirerIndex() +"[$]")
                            .setGravity(Gravity.LEFT));
        }

        page.addLine().addUnit(page.createUnit().setText(" "));
        if (isRePrint) {
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("----" + Utils.getString(R.string.receipt_print_again) + "----")
                            .setFontSize(FONT_BIG)
                            .setGravity(Gravity.CENTER));
        }

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
        //page.addLine().addUnit(page.createUnit().setText(" "));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setGravity(Gravity.CENTER));

        /*------------------------------Body------------------------------*/
        if(isPrintSign) { // MERCHANT COPY
            /*------------------Acquirer Info.-------------------*/
            // terminal ID/operator ID
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_terminal_code_short_sharp))
                            .setFontSize(FONT_NORMAL)
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(acquirer.getTerminalId())
                            .setWeight(3.0f)
                            .setGravity(Gravity.LEFT)
                    );
            // merchant ID
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_merchant_code_short_sharp2))
                            .setFontSize(FONT_NORMAL)
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(acquirer.getMerchantId())
                            .setGravity(Gravity.LEFT)
                            .setWeight(3.0f));
//            // batch NO
//            page.addLine()
//                    .addUnit(page.createUnit()
//                            .setText(Utils.getString(R.string.receipt_batch_num_short))
//                            .setFontSize(FONT_NORMAL))
//                    .addUnit(page.createUnit()
//                            .setText(Component.getPaddedNumber(transData.getBatchNo(), 6))
//                            .setGravity(Gravity.LEFT)
//                            .setWeight(1.0f))
//                    .addUnit(page.createUnit()
//                            .setText("HOST: " + acquirer.getName())
//                            .setFontSize(FONT_SMALL)
//                            .setGravity(Gravity.END)
//                            .setWeight(2.0f));
            //Batch No. , trace NO , STAN NO
            String batchno = Component.getPaddedNumber(transData.getBatchNo(), 6);
            String stanno  = Component.getPaddedNumber(transData.getStanNo(), 6);
            stanno = transStatus == TransData.ETransStatus.VOIDED ? Component.getPaddedNumber(transData.getVoidStanNo(), 6) : stanno;
            String traceno = Component.getPaddedNumber(transData.getTraceNo(), 6);
//            page.addLine()
//                    .addUnit(page.createUnit()
//                            .setText(Utils.getString(R.string.receipt_trans_no_short))
//                            .setFontSize(FONT_NORMAL))
//                    .addUnit(page.createUnit()
//                            .setText(traceno)
//                            .setGravity(Gravity.LEFT)
//                            .setWeight(1.0f))
//                    .addUnit(page.createUnit()
//                            .setText("STAN: " + stanno)
//                            .setFontSize(FONT_SMALL)
//                            .setGravity(Gravity.END)
//                            .setWeight(2.0f));
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_stan_code))
                            .setFontSize(FONT_NORMAL)
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(stanno)
                            .setGravity(Gravity.LEFT)
                            .setWeight(3.0f));
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_batch_num_short_sharp))
                            .setFontSize(FONT_NORMAL)
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(batchno)
                            .setGravity(Gravity.LEFT)
                            .setWeight(3.0f));
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_trans_no_short_sharp2))
                            .setFontSize(FONT_NORMAL)
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(traceno)
                            .setGravity(Gravity.LEFT)
                            .setWeight(3.0f));
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_one_line))
                            .setGravity(Gravity.CENTER));



            //TRANS_TYPEN
            String TransType = Utils.getString(R.string.trans_aycap_installment_plan);
            TransType = Utils.getString(R.string.trans_aycap_installment_plan);//Component.getTransByIPlanMode(transData.getInstalmentIPlanMode());
            if(transStatus == TransData.ETransStatus.VOIDED || transType == ETransType.VOID){
                TransType = Utils.getString(R.string.trans_void).toUpperCase() + " " + TransType;
            }
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(TransType)
                            .setFontSize(FONT_BIG));

            if (transData.isEcrProcess()) {
                page.addLine()
                    .addUnit(page.createUnit()
                            .setText(" " + Utils.getString(R.string.receipt_pos_tran))
                            .setFontSize(FONT_BIG));
            }

            // Card No.
            String cardNo = null;

            cardNo = (isAmex && receiptNo > 0)
                       ? PanUtils.maskCardNo(transData.getPan(), Constants.PAN_MASK_PATTERN4)
                       : PanUtils.maskCardNo(transData.getPan(), transData.getIssuer().getPanMaskPattern());


            TransData.EnterMode enterMode = transData.getEnterMode();
            String tmpEnterMode = enterMode.toString();
            if      (enterMode == TransData.EnterMode.INSERT)   { tmpEnterMode = "CHIPCARD"; }
            else if (enterMode == TransData.EnterMode.MANUAL)   { tmpEnterMode = "MANUAL"; }
            else if (enterMode == TransData.EnterMode.SWIPE)    { tmpEnterMode = "SWIPE"; }
            else if (enterMode == TransData.EnterMode.CLSS)     { tmpEnterMode = "CTLS"; }
            else if (enterMode == TransData.EnterMode.FALLBACK) { tmpEnterMode = "FALLBACK"; }

            String tmpExpStr = transData.getExpDate();
            if (tmpExpStr != null && !tmpExpStr.isEmpty()) {
                if (transData.getIssuer().isRequireMaskExpiry()) {
                    tmpExpStr = "**/**";
                } else {
                    tmpExpStr = tmpExpStr.substring(2) + "/" + tmpExpStr.substring(0, 2);
                }
            } else {
                tmpExpStr = "";
            }

            //card NO
            page.addLine()
                    .adjustTopSpace(3)
                    .addUnit(page.createUnit()
                            .setText(cardNo)
                            .setFontSize(FONT_BIG));
            // Card Type
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_card_type2))
                            .setFontSize(FONT_NORMAL)
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(transData.getIssuer().getIssuerName())
                            .setWeight(3.0f)
                            .setGravity(Gravity.LEFT));
            // EXP.DATE
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_card_exp_date) + tmpExpStr)
                            .setFontSize(FONT_SMALL_18)
                            .setGravity(Gravity.LEFT)
                            .setWeight(2.3f))
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_entry_type) + " " + tmpEnterMode)
                            .setFontSize(FONT_SMALL_18)
                            .setGravity(Gravity.END)
                            .setWeight(2.7f));

            // date/time
            String formattedDate = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                    Constants.DATE_PATTERN_DISPLAY4);

            String formattedTime = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                    Constants.TIME_PATTERN_DISPLAY4);

            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(formattedDate)
                            .setFontSize(FONT_BIG)
                            .setGravity(Gravity.START))
                    .addUnit(page.createUnit()
                            .setText(formattedTime)
                            .setFontSize(FONT_BIG)
                            .setGravity(Gravity.END));

            // APP CODE
            String appCode = transData.getAuthCode();
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_appr_code2))
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(appCode)
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.END));

            // REF. NO.
            String refCode = transData.getRefNo();
            if (refCode == null) { refCode = ""; }
            if (transData.isOnlineTrans()) {
                if (!refCode.isEmpty()) {
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText(Utils.getString(R.string.receipt_ref_no))
                                    .setFontSize(FONT_NORMAL))
                            .addUnit(page.createUnit()
                                    .setText(refCode)
                                    .setFontSize(FONT_NORMAL)
                                    .setGravity(Gravity.END));
                }
            }

            if (enterMode == TransData.EnterMode.INSERT) {

                //APP
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText("APP: " + transData.getEmvAppLabel())
                                .setFontSize(FONT_SMALL_18));
                //AID
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText("AID: " + transData.getAid())
                                .setFontSize(FONT_SMALL_18));

                //TVR
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText("TVR: " + transData.getTvr())
                                .setFontSize(FONT_SMALL_18));

                //TC
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText("TC: " + transData.getTc())
                                .setFontSize(FONT_SMALL_18));
            }




            // Generate installment deital slip
            getInstallmentInformation(page, transData,true);




            page.addLine().addUnit(page.createUnit().setText(" "));
            if (transData.isPinVerifyMsg()) {//PIN VERIFY
//                page.addLine().addUnit(page.createUnit()
//                        .setText(Utils.getString(R.string.receipt_pin_verify))
//                        .setGravity(Gravity.CENTER));
                page.addLine().addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_no_sig_required))
                        .setGravity(Gravity.CENTER));
                page.addLine().addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_pin_verify_success))
                        .setGravity(Gravity.CENTER));
            } else {// NO PIN VERIFY
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_sign))
                                .setFontSize(FONT_SMALL)
                                .setGravity(Gravity.CENTER));
                Bitmap bitmap = loadSignature(transData);
                if ((bitmap != null && transType != ETransType.KBANK_SMART_PAY_VOID && transStatus != TransData.ETransStatus.VOIDED && (!isRePrint))) {
                    if(onErmMode) {
                        // Extra for ERM
                        page.addLine().addUnit(page.createUnit()
                                .setText("[@][@SIGN_DATA]")
                                .setGravity(Gravity.LEFT));
                    } else {
                        page.addLine().addUnit(page.createUnit()
                                .setBitmap(loadSignature(transData))
                                .setGravity(Gravity.CENTER));
                    }
                } else {
                    page.addLine().addUnit(page.createUnit().setText(" "));
                    //Skip Signature process, print space for cardholder manual Sign
                    page.addLine().addUnit(page.createUnit().setText("\n\n"));
                }
            }
            /*------------------End Trans. Detail-------------------*/
        } else { // CUSTOMER COPY
            /*------------------Acquirer Info.-------------------*/
            //TID /MER
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_terminal_code_short) + acquirer.getTerminalId())
                            .setFontSize(FONT_SMALL_18))
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_merchant_code_short) + acquirer.getMerchantId())
                            .setFontSize(FONT_SMALL_18)
                            .setGravity(Gravity.END));

            //TRACE / BATCH
            temp = Component.getPaddedNumber(transData.getTraceNo(), 6);
            page.addLine()
                    .addUnit(page.createUnit()
                                    .setText(Utils.getString(R.string.receipt_trans_no_short) + temp)
                                    .setFontSize(FONT_SMALL_18)
                            //.setWeight(5)
                    )
                    .addUnit(page.createUnit()
                                    .setText(Utils.getString(R.string.receipt_batch_num_short) + Component.getPaddedNumber(transData.getBatchNo(), 6))
                                    .setFontSize(FONT_SMALL_18)
                                    .setGravity(Gravity.END)
                            //.setWeight(7)
                    );

            //STAN NO
            String stanno = Component.getPaddedNumber(transData.getStanNo(), 6);
            stanno = transStatus == TransData.ETransStatus.VOIDED ? Component.getPaddedNumber(transData.getVoidStanNo(), 6) : stanno;
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_stan_code3) + stanno)
                            .setFontSize(FONT_SMALL_18)
                            .setGravity(Gravity.START)
                    );

            //Card type / Datetime
            String formattedTime = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                    Constants.TIME_PATTERN_DISPLAY3);
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(transData.getIssuer().getName())
                            .setFontSize(FONT_SMALL_18))
                    .addUnit(page.createUnit()
                            .setText(formattedTime)
                            .setFontSize(FONT_SMALL_18)
                            .setGravity(Gravity.END));

            /*------------------Trans. Detail-------------------*/
            //CARD NO
            temp = PanUtils.maskCardNo(transData.getPan(), "(?<=\\d{0})\\d(?=\\d{4})");
            TransData.EnterMode enterMode = transData.getEnterMode();
            String tmpEnterMode = enterMode.toString();
            if (enterMode == TransData.EnterMode.INSERT) {
                tmpEnterMode = "C";
            }
            temp += " ( " + tmpEnterMode + " )";

            //card NO
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(temp)
                            .setFontSize(FONT_BIG));

            //APP.CODE / TRANS_TYPE
            temp = Utils.getString(R.string.trans_aycap_installment_plan);//Component.getTransByIPlanMode(transData.getInstalmentIPlanMode());
            if(transStatus == TransData.ETransStatus.VOIDED || transType == ETransType.VOID){
                temp = Utils.getString(R.string.trans_void).toUpperCase() + " " + temp;
            }
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_app_code) + " : " + transData.getAuthCode())
                            .setFontSize(FONT_SMALL_18)
                            .setGravity(Gravity.START))
                    .addUnit(page.createUnit()
                            .setText(temp)
                            .setFontSize(FONT_SMALL_18)
                            .setGravity(Gravity.END));

            //Instalment info.
            getInstallmentInformation(page, transData, false);

            /*------------------End Trans. Detail-------------------*/
        }

        /*------------------------------Footer------------------------------*/
        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_sign_line))
                .setGravity(Gravity.CENTER));

//        //CardHolder Name
//        temp = transData.getTrack1();
//        if(temp != null){
//            page.addLine().addUnit(page.createUnit()
//                    .setText(temp.trim())
//                    .setFontSize(FONT_BIG)
//                    .setGravity(Gravity.CENTER));
//        }

        String[] CardHolderName ;
        if (transData.getTrack1() != null){
            try {
                CardHolderName = EReceiptUtils.stringSplitter(transData.getTrack1(),EReceiptUtils.MAX23_CHAR_PER_LINE) ;
                if (CardHolderName.length == 1 && CardHolderName[0] != null){
                    page.addLine().addUnit(page.createUnit()
                            .setText(CardHolderName[0])
                            .setFontSize(FONT_NORMAL_26)
                            .setGravity(Gravity.CENTER));
                } else {
                    page.addLine().addUnit(page.createUnit()
                            .setText(CardHolderName[0])
                            .setFontSize(FONT_NORMAL_26)
                            .setGravity(Gravity.CENTER));
                    page.addLine().addUnit(page.createUnit()
                            .setText(CardHolderName[1])
                            .setFontSize(FONT_NORMAL_26)
                            .setGravity(Gravity.CENTER));
                }
            } catch (Exception e) {
                Log.d(TAG,e.getMessage() + " : Error during reformat [CardHolderName]");
            }
        }

//        page.addLine().addUnit(page.createUnit()
//                .setText(Utils.getString(R.string.receipt_verify))
//                .setFontSize(FONT_SMALL)
//                .setGravity(Gravity.CENTER));
        try {
            String Disclaimer[] = EReceiptUtils.stringSplitter(Utils.getString(R.string.receipt_verify), 34);
            for ( int dcmrIndex = 0 ; dcmrIndex <= Disclaimer.length-1 ; dcmrIndex++ ){
                page.addLine().addUnit(page.createUnit()
                        .setText(Disclaimer[dcmrIndex])
                        .setFontSize(FONT_SMALL)
                        .setGravity(Gravity.CENTER));
            }
        } catch (Exception ex) {

        }

        Component.genAppVersiononSlip(page);

        page.addLine().adjustTopSpace(5)
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_no_refund))
                        .setFontSize(FONT_NORMAL_22)
                        .setGravity(Gravity.CENTER));

        if (! onErmMode) {
            if (Component.isAllowSignatureUpload(transData) && transData.geteReceiptUploadStatus() != TransData.UploadStatus.NORMAL && receiptNo == 0) {
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.footer_ereceipt_upload_not_success))
                                .setFontSize(FONT_BIG)
                                .setGravity(Gravity.CENTER));
            }
        }



        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_trusted_trans))
                .setFontSize(FONT_SMALL)
                .setGravity(Gravity.CENTER));

        if (receiptMax == 3) {
            if (receiptNo == 0) {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_acquire))
                                .setGravity(Gravity.CENTER));
            } else if (receiptNo == 1) {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_merchant))
                                .setGravity(Gravity.CENTER));
            } else {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_cust))
                                .setGravity(Gravity.CENTER));
            }
        } else {
            if (receiptNo == 0) {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_merchant))
                                .setGravity(Gravity.CENTER));
            } else {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_cust))
                                .setGravity(Gravity.CENTER));
            }
        }

        page.addLine()
                .adjustTopSpace(10)
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_aycap_installment_footer_disclaimer))
                        .setFontSize(FONT_SMALL_18)
                        .setGravity(Gravity.CENTER));

        if (!isPrintPreview) {
            page.addLine().addUnit(page.createUnit().setText("\n\n\n\n"));
        }

        if (onErmMode) {
            PageToSlipFormat.isSettleMode=false;
            PageToSlipFormat.getInstance().Append(page);
        }
        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }

    private void getInstallmentInformation(IPage page, TransData transData, boolean isMerchantCopy) {
        page.addLine().addUnit(page.createUnit().setText(" "));

        // amount
        long amount = Utils.parseLongSafe(transData.getAmount(), 0);
        if (transData.getTransType().isSymbolNegative() || transData.getTransState() == TransData.ETransStatus.VOIDED)
            amount = -amount;
        String totalAmt = CurrencyConverter.convert(amount, transData.getCurrency());
        add2Units((receiptNo==1) ? Utils.getString(R.string.receipt_amount_total) : Utils.getString(R.string.receipt_amount_short), totalAmt, true ? FONT_BIG : FONT_NORMAL_26, Typeface.NORMAL, page);

        page.addLine().addUnit(page.createUnit().setText(" "));

        HashMap<String,byte[]> hash_F63 = new  HashMap<String,byte[]>();
        byte[] f63 = transData.getField63RecByte();
        if (f63 != null) {
            boolean allowPrintAllMissingData = false;
            byte[] bx_table_len         = new byte[2];
            byte[] bx_table_id          = new byte[2];
            byte[] bx_non_value         = new byte[12];
            byte[] bx_brand_name        = new byte[20];
            byte[] bx_prod_type         = new byte[10];
            byte[] bx_model_name        = new byte[20];
            byte[] bx_sf_slip_no        = new byte[10];
            byte[] bx_int1              = new byte[4];
            byte[] bx_cuc1              = new byte[4];
            byte[] bx_int2              = new byte[4];
            byte[] bx_cuc2              = new byte[4];
            byte[] bx_total_charge      = new byte[12];
            byte[] bx_total_due_payment = new byte[12];
            byte[] bx_monthly_due1      = new byte[12];
            byte[] bx_monthly_due2      = new byte[12];

            int curPoS = 0 ;
            if (f63.length > 4) {
                if(curPoS+bx_table_len.length <=f63.length)
                        {System.arraycopy(f63, curPoS , bx_table_len,        0, bx_table_len.length);            curPoS+=bx_table_len.length;           hash_F63.put("LEN",bx_table_len);}
                if(curPoS+bx_table_id.length <=f63.length)
                        {System.arraycopy(f63, curPoS , bx_table_id,         0, bx_table_id.length);             curPoS+=bx_table_id.length;            hash_F63.put("ID",bx_table_id);}
                if(curPoS+bx_non_value.length <=f63.length)
                        {System.arraycopy(f63, curPoS , bx_non_value,        0, bx_non_value.length);            curPoS+=bx_non_value.length;           hash_F63.put("NON_VAL",bx_non_value);}
                if(curPoS+bx_brand_name.length <=f63.length)
                        {System.arraycopy(f63, curPoS , bx_brand_name,       0, bx_brand_name.length);           curPoS+=bx_brand_name.length;          hash_F63.put("BRAND",bx_brand_name);}
                if(curPoS+bx_prod_type.length <=f63.length)
                        {System.arraycopy(f63, curPoS , bx_prod_type,        0, bx_prod_type.length);            curPoS+=bx_prod_type.length;           hash_F63.put("PROD_TYPE",bx_prod_type);}
                if(curPoS+bx_model_name.length <=f63.length)
                        {System.arraycopy(f63, curPoS , bx_model_name,       0, bx_model_name.length);           curPoS+=bx_model_name.length;          hash_F63.put("MODEL",bx_model_name);}
                if(curPoS+bx_sf_slip_no.length <=f63.length)
                        {System.arraycopy(f63, curPoS , bx_sf_slip_no,       0, bx_sf_slip_no.length);           curPoS+=bx_sf_slip_no.length;          hash_F63.put("SLIP_NO",bx_sf_slip_no);}
                if(curPoS+bx_int1.length <=f63.length)
                        {System.arraycopy(f63, curPoS , bx_int1,             0, bx_int1.length);                 curPoS+=bx_int1.length;                hash_F63.put("INT_01",bx_int1);}
                if(curPoS+bx_cuc1.length <=f63.length)
                        {System.arraycopy(f63, curPoS , bx_cuc1,             0, bx_cuc1.length);                 curPoS+=bx_cuc1.length;                hash_F63.put("CUC_01",bx_cuc1);}
                if(curPoS+bx_int2.length <=f63.length)
                        {System.arraycopy(f63, curPoS , bx_int2,             0, bx_int2.length);                 curPoS+=bx_int2.length;                hash_F63.put("INT_02",bx_int2);}
                if(curPoS+bx_cuc2.length <=f63.length)
                        {System.arraycopy(f63, curPoS , bx_cuc2,             0, bx_cuc2.length);                 curPoS+=bx_cuc2.length;                hash_F63.put("CUC_02",bx_cuc2);}
                if(curPoS+bx_total_charge.length <=f63.length)
                        {System.arraycopy(f63, curPoS , bx_total_charge,     0, bx_total_charge.length);         curPoS+=bx_total_charge.length;        hash_F63.put("TOT_CHARGE",bx_total_charge);}
                if(curPoS+bx_total_due_payment.length <=f63.length)
                        {System.arraycopy(f63, curPoS , bx_total_due_payment,0, bx_total_due_payment.length);    curPoS+=bx_total_due_payment.length;   hash_F63.put("TOT_PAYMENT_DUE",bx_total_due_payment);}
                if(curPoS+bx_monthly_due1.length <=f63.length)
                        {System.arraycopy(f63, curPoS , bx_monthly_due1,     0, bx_monthly_due1.length);         curPoS+=bx_monthly_due1.length;        hash_F63.put("MONTHLY_DUE_01",bx_monthly_due1);}
                if(curPoS+bx_monthly_due2.length <=f63.length)
                        {System.arraycopy(f63, curPoS , bx_monthly_due2,     0, bx_monthly_due2.length);         curPoS+=bx_monthly_due2.length;        hash_F63.put("MONTHLY_DUE_02",bx_monthly_due2);}
            }
            if (transData.isBayInstalmentSpecific()) {
                hash_F63.put("MKT-CODE",(transData.getMktCode() != null) ? transData.getMktCode().getBytes() : "".getBytes());
                hash_F63.put("SKU-CODE",(transData.getSkuCode() != null) ? transData.getSkuCode().getBytes() : "".getBytes());
            } else {
                hash_F63.put("INTEREST",(transData.getInstalmentInterest() != null) ? transData.getInstalmentInterest().getBytes() : "0.00".getBytes());
            }
            hash_F63.put("TERM",(transData.getInstalmentTerms() != null) ? transData.getInstalmentTerms().getBytes() : "".getBytes());
            hash_F63.put("TOT_TERM",(transData.getInstalmentTerms() != null) ? (transData.getInstalmentTerms() + " Months").getBytes() : "".getBytes());
            hash_F63.put("SN",(transData.getInstalmentSerialNo() != null) ? transData.getInstalmentSerialNo().getBytes() : "N/A".getBytes());
            hash_F63.put("TOT_AMT",totalAmt.getBytes());

            // default cosmetic
            hash_F63.put("DASH-LINE","".getBytes());
            hash_F63.put("INTEREST/CUC","".getBytes());
            hash_F63.put("INTEREST/CUC-VAL","".getBytes());
            hash_F63.put("MONTHLY_DUE","".getBytes());


            if (hash_F63.size() > 0) {
                String[] Keys  = null;
                HashMap<String,Integer> fontsize = new HashMap<String,Integer>();
                fontsize.put("default", FONT_SMALL_18);
                if(isMerchantCopy) {
                    Keys = new String[] {"BRAND", "PROD_TYPE", "MODEL","DASH-LINE","MKT-CODE","SKU-CODE","SN","TOT_AMT","TOT_TERM", "INTEREST/CUC", "INTEREST/CUC-VAL","TOT_CHARGE","TOT_PAYMENT_DUE","MONTHLY_DUE","MONTHLY_DUE_01","DASH-LINE"};
                    fontsize.put("DASH-LINE", FONT_NORMAL);
                    fontsize.put("TOT_PAYMENT_DUE", FONT_NORMAL);
                    fontsize.put("MONTHLY_DUE_01", FONT_NORMAL);
                    fontsize.put("INTEREST/CUC", FONT_NORMAL_26);
                    fontsize.put("INTEREST/CUC-VAL", FONT_NORMAL);
                } else  {
                    Keys = new String[] {"BRAND", "PROD_TYPE", "MODEL","MKT-CODE","SKU-CODE","SN","TOT_AMT","TOT_TERM", "INTEREST/CUC","TOT_CHARGE","TOT_PAYMENT_DUE","MONTHLY_DUE"};
                }





                int lineFontSize =-1;
                for (int id=0; id <=Keys.length-1; id++) {
                    lineFontSize = (fontsize.get(Keys[id]) != null ? fontsize.get(Keys[id]) : fontsize.get("default"));
                    printTargetHashMap(page,hash_F63, Keys[id], lineFontSize, isMerchantCopy, allowPrintAllMissingData);
                }

            }
        }
    }
    private void printTargetHashMap(IPage page, HashMap<String,byte[]> hash_F63, String FieldName, int fontSize, boolean isMerchantCopy,  boolean isPrintOnMissingValue) {
        try {
            if (hash_F63.get(FieldName) != null) {
                String label_str = "";
                String value_str = "";
                int numbOfItem = 2;
                switch (FieldName) {
                    case "BRAND" :              label_str = Utils.getString(R.string.receipt_aycap_installment_brand); break;
                    case "PROD_TYPE" :          label_str = Utils.getString(R.string.receipt_aycap_installment_prod_type); break;
                    case "MODEL" :              label_str = Utils.getString(R.string.receipt_aycap_installment_model); break;
                    case "SLIP_NO" :            label_str = " "; break;
                    case "CUC_01" :             label_str = " "; break;
                    case "CUC_02" :             label_str = " "; break;
                    case "TOT_CHARGE" :         label_str = Utils.getString(R.string.receipt_aycap_installment_interest_cuc_amt); break;
                    case "TOT_PAYMENT_DUE" :    label_str = Utils.getString(R.string.receipt_aycap_installment_total_due); break;
                    case "MONTHLY_DUE" :        label_str = Utils.getString(R.string.receipt_aycap_installment_monthly_due);  numbOfItem = (isMerchantCopy) ? 1 : 2 ; break;
                    case "MONTHLY_DUE_01" :     label_str = " "; numbOfItem = (isMerchantCopy) ? 1 : 2 ; break;
                    case "MONTHLY_DUE_02" :     label_str = " "; break;
                    case "MKT-CODE" :           label_str = Utils.getString(R.string.receipt_aycap_installment_mkt_code); break;
                    case "SKU-CODE" :           label_str = Utils.getString(R.string.receipt_aycap_installment_sku_code); break;
                    case "TOT_TERM" :           label_str = Utils.getString(R.string.receipt_aycap_installment_total_term); break;
                    case "DASH-LINE" :          label_str = Utils.getString(R.string.receipt_one_line); numbOfItem = (isMerchantCopy) ? 1 : 2 ;  break;
                    case "INTEREST/CUC" :       label_str = Utils.getString(R.string.receipt_aycap_installment_interest_cuc); numbOfItem = (isMerchantCopy) ? 1 : 2 ;  break;
                    case "INTEREST/CUC-VAL" :   label_str = " "; numbOfItem = (isMerchantCopy) ? 1 : 2 ;  break;
                    case "SN" :                 label_str = Utils.getString(R.string.receipt_aycap_installment_serial_number);   break;
                    case "TOT_AMT" :            label_str = Utils.getString(R.string.receipt_aycap_installment_total_amount);   break;
                    default:                    label_str = null; break;
                }

                if(isMerchantCopy) {
                    if(numbOfItem ==1) {
                        int iGravity ;
                        if (FieldName.equals("INTEREST/CUC-VAL") )  {
                            iGravity = Gravity.END;
                            label_str  = percentageConvert((new String(hash_F63.get("INT_01"),"UTF-8")).trim()) ;
                            label_str += "/ " + percentageConvert((new String(hash_F63.get("CUC_01"),"UTF-8")).trim()) ;
                            label_str += " "  + Component.getPaddedNumber(Long.valueOf(new String(hash_F63.get("TERM"),"UTF-8")),2) +"M"  ;
                        } else if (FieldName.equals("INTEREST/CUC") || FieldName.equals("MONTHLY_DUE"))       {
                            iGravity = Gravity.START;
                        } else if (FieldName.equals("MONTHLY_DUE_01"))       {
                            iGravity = Gravity.END;
                            label_str = CurrencyConverter.convert(Utils.parseLongSafe((new String(hash_F63.get(FieldName),"UTF-8")).trim(), 0), transData.getCurrency());
                            label_str += " ("  + Component.getPaddedNumber(Long.valueOf(new String(hash_F63.get("TERM"),"UTF-8")),2) +"M)";
                        } else {
                            iGravity = Gravity.CENTER;
                        }
                        page.addLine()
                                .adjustTopSpace(1)
                                .addUnit(page.createUnit()
                                        .setText(label_str)
                                        .setGravity(iGravity)
                                        .setFontSize(fontSize));
                    } else if(numbOfItem==2) {
                        value_str = (new String(hash_F63.get(FieldName),"UTF-8")).trim();
                        if(FieldName.equals("TOT_CHARGE") || FieldName.equals("TOT_PAYMENT_DUE")) {
                            value_str = CurrencyConverter.convert(Utils.parseLongSafe(value_str, 0), transData.getCurrency());
                        }
                        Log.d("BAY-INSTALLMENT" , " FIELD63."+ FieldName + " : VALUE is '" + value_str + "'");
                        if (label_str!=null) {
                            if(value_str.trim().length() > 0 || isPrintOnMissingValue){
                                page.addLine()
                                        .addUnit(page.createUnit()
                                                .setText(label_str)
                                                .setGravity(Gravity.START)
                                                .setFontSize(fontSize))
                                        .addUnit(page.createUnit()
                                                .setText(value_str)
                                                .setGravity(Gravity.END)
                                                .setFontSize(fontSize));
                            }
                        }
                    }
                } else {
                    value_str = (new String(hash_F63.get(FieldName),"UTF-8")).trim();
                    if(FieldName.equals("TOT_CHARGE") || FieldName.equals("TOT_PAYMENT_DUE")) {
                        value_str = CurrencyConverter.convert(Utils.parseLongSafe(value_str, 0), transData.getCurrency());
                    } else if (FieldName.equals("INTEREST/CUC")) {
                        value_str  = percentageConvert((new String(hash_F63.get("INT_01"),"UTF-8")).trim()) ;
                        value_str += "/ " + percentageConvert((new String(hash_F63.get("CUC_01"),"UTF-8")).trim()) ;
                        value_str += " "  + Component.getPaddedNumber(Long.valueOf(new String(hash_F63.get("TERM"),"UTF-8")),2) +"M"  ;
                    } else if (FieldName.equals("MONTHLY_DUE")) {
                        value_str = CurrencyConverter.convert(Utils.parseLongSafe((new String(hash_F63.get("MONTHLY_DUE_01"),"UTF-8")).trim(), 0), transData.getCurrency());
                        value_str += " ("  + Component.getPaddedNumber(Long.valueOf(new String(hash_F63.get("TERM"),"UTF-8")),2) +"M)";
                    }
                    Log.d("BAY-INSTALLMENT" , " FIELD63."+ FieldName + " : VALUE is '" + value_str + "'");
                    if (label_str!=null) {
                        if(value_str.trim().length() > 0 || isPrintOnMissingValue){
                            page.addLine()
                                    .addUnit(page.createUnit()
                                            .setText(label_str)
                                            .setGravity(Gravity.START)
                                            .setFontSize(fontSize))
                                    .addUnit(page.createUnit()
                                            .setText(value_str)
                                            .setGravity(Gravity.END)
                                            .setFontSize(fontSize));
                        }
                    }}

            }
        } catch (Exception ex) {
            Log.e("BAY-INSTALLAMENT","FieldName=" + FieldName + " was missing in hashmap-data-collector");
        }
    }
    private String percentageConvert(String fVal) {
        if(fVal==null) {
            return "0.00%";
        } else {
            return Integer.valueOf(fVal.substring(0,2)) + "." + fVal.substring(2,4) + "%";
        }
    }

    private Bitmap loadSignature(TransData transData) {
        byte[] signData = transData.getSignData();
        if (signData == null) {
            return null;
        }
        return FinancialApplication.getGl().getImgProcessing().jbigToBitmap(signData);
    }

//    private void genInstalmentInfo(IPage page, TransData transData, boolean isMerchantCopy) {
//        HashMap<ReservedFieldHandle.FieldTables, byte[]> f61 = ReservedFieldHandle.unpackReservedField(transData.getField61RecByte(), ReservedFieldHandle.smtp_f61_response, true);
//        HashMap<ReservedFieldHandle.FieldTables, byte[]> f63 = ReservedFieldHandle.unpackReservedField(transData.getField63RecByte(), ReservedFieldHandle.smtp_f63_response, true);
//
//        // amount
//        long amount = Utils.parseLongSafe(transData.getAmount(), 0);
//        if (transData.getTransType().isSymbolNegative() || transData.getTransState() == TransData.ETransStatus.VOIDED)
//            amount = -amount;
//        String totalAmt = CurrencyConverter.convert(amount, transData.getCurrency());
//
//        add2Units((receiptNo==1) ? Utils.getString(R.string.receipt_amount_total) : Utils.getString(R.string.receipt_amount_short), totalAmt, isMerchantCopy ? FONT_BIG : FONT_NORMAL_26, Typeface.NORMAL, page);
//
//        if (f61 != null) {
//            String payment_term = transData.getInstalmentPaymentTerm() + " MONTHS";
//            String int_rate = new String(f61.get(ReservedFieldHandle.FieldTables.INTEREST_RATE));
//            String handing_fee = new String(f61.get(ReservedFieldHandle.FieldTables.HANDING_FEE));
//            String total_pay_amt = new String(f61.get(ReservedFieldHandle.FieldTables.TOTAL_PAY_AMOUNT));
//            String month_pay_amt = new String(f61.get(ReservedFieldHandle.FieldTables.MONTH_PAY_AMOUNT));
//
//            int_rate = String.format(Locale.getDefault(), "%.2f", Double.parseDouble(int_rate.substring(0, int_rate.length()-1))/100);
//
//            amount = Utils.parseLongSafe(handing_fee, 0);
//            handing_fee = CurrencyConverter.convert(amount, transData.getCurrency());
//
//            amount = Utils.parseLongSafe(total_pay_amt, 0);
//            total_pay_amt = CurrencyConverter.convert(amount, transData.getCurrency());
//
//            amount = Utils.parseLongSafe(month_pay_amt, 0);
//            month_pay_amt = CurrencyConverter.convert(amount, transData.getCurrency());
//
//            if (isMerchantCopy) addSingleLine(page);
//            add2Units(Utils.getString(R.string.receipt_instalment_term), payment_term, isMerchantCopy ? FONT_NORMAL : FONT_SMALL, Typeface.NORMAL, page);
//            add2Units(Utils.getString(R.string.receipt_instalment_interest), int_rate, isMerchantCopy ? FONT_NORMAL : FONT_SMALL, Typeface.NORMAL, page);
//            add2Units(Utils.getString(R.string.receipt_instalment_mgt_fee), handing_fee, isMerchantCopy ? FONT_NORMAL : FONT_SMALL, Typeface.NORMAL, page);
//            add2Units(Utils.getString(R.string.receipt_instalment_tot_due), total_pay_amt, isMerchantCopy ? FONT_NORMAL : FONT_SMALL, Typeface.NORMAL, page);
//            add2Units(Utils.getString(R.string.receipt_instalment_monthly), month_pay_amt, isMerchantCopy ? FONT_NORMAL : FONT_SMALL, Typeface.NORMAL, page);
//        }
//
//        if (f63 != null/* && (transData.isInstalmentPromoProduct() || "03".equals(transData.getInstalmentIPlanMode()) || "04".equals(transData.getInstalmentIPlanMode()))*/) {
//            String sup_name = new String(f63.get(ReservedFieldHandle.FieldTables.SUPPLIER_NAME)).trim();
//            String prod_name = new String(f63.get(ReservedFieldHandle.FieldTables.PRODUCT_NAME)).trim();
//            String model_name = new String(f63.get(ReservedFieldHandle.FieldTables.MODEL_NAME)).trim();
//            String serial_no = Component.getPaddedStringRight(transData.getInstalmentSerialNo(), 18, '9');
//
//            if (isMerchantCopy) addSingleLine(page);
//            add1Unit(Utils.getString(R.string.receipt_instalment_brand) + "   " + sup_name, isMerchantCopy ? FONT_NORMAL : FONT_NORMAL_22, Typeface.NORMAL, page);
//            add1Unit(Utils.getString(R.string.receipt_instalment_product) + "   " + prod_name, isMerchantCopy ? FONT_NORMAL : FONT_NORMAL_22, Typeface.NORMAL, page);
//            add1Unit(Utils.getString(R.string.receipt_instalment_model) + "   " + model_name, isMerchantCopy ? FONT_NORMAL : FONT_NORMAL_22, Typeface.NORMAL, page);
//            add1Unit(Utils.getString(R.string.receipt_instalment_sn) + "   " + serial_no, isMerchantCopy ? FONT_NORMAL : FONT_NORMAL_22, Typeface.NORMAL, page);
//            if (isMerchantCopy) addSingleLine(page);
//        }
//    }

    private static void add2Units(String title, String value, int size, int style, IPage page) {
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(title)
                        .setFontSize(size)
                        .setTextStyle(style))
                .addUnit(page.createUnit()
                        .setText(value)
                        .setFontSize(size)
                        .setTextStyle(style)
                        .setGravity(Gravity.END));
    }

    private static void add1Unit(String text, int size, int style, IPage page) {
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(text)
                        .setFontSize(size)
                        .setTextStyle(style)
                        .setGravity(Gravity.LEFT));
    }

    private static void addSingleLine(IPage page) {
        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setGravity(Gravity.CENTER));
    }

    @Override
    public String generateString() {
        return null;
    }
}
