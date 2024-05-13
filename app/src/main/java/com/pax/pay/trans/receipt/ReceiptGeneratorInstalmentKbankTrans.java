package com.pax.pay.trans.receipt;

import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.view.Gravity;

import com.pax.abl.utils.PanUtils;
import com.pax.device.Device;
import com.pax.edc.BuildConfig;
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

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;

import th.co.bkkps.utils.Log;
import th.co.bkkps.utils.PageToSlipFormat;

import static com.pax.pay.utils.Utils.getString;

public class ReceiptGeneratorInstalmentKbankTrans implements IReceiptGenerator {

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
    public ReceiptGeneratorInstalmentKbankTrans(TransData transData, int currentReceiptNo, int receiptMax, boolean isReprint) {
        this.transData = transData;
        this.receiptNo = currentReceiptNo;
        this.isRePrint = isReprint;
        this.receiptMax = receiptMax;

        // Extra for ERM
        onErmMode=false;
        PageToSlipFormat.getInstance().Reset();
    }

    public ReceiptGeneratorInstalmentKbankTrans(TransData transData, int currentReceiptNo, int receiptMax, boolean isReprint, boolean isPrintPreview) {
        this.transData = transData;
        this.receiptNo = currentReceiptNo;
        this.isRePrint = isReprint;
        this.receiptMax = receiptMax;
        this.isPrintPreview = isPrintPreview;

        // Extra for ERM
        onErmMode=false;
        PageToSlipFormat.getInstance().Reset();
    }

    public ReceiptGeneratorInstalmentKbankTrans(TransData transData, int currentReceiptNo, int receiptMax, boolean isReprint, boolean isPrintPreview, boolean onErmMode) {
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
        Bitmap logo = null;

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
        String temp1;
        TransData.EnterMode enterMode = transData.getEnterMode();
        Boolean isVoid = (transStatus == TransData.ETransStatus.VOIDED
                        || transType == ETransType.KBANK_SMART_PAY_VOID
                        || transType == ETransType.DOLFIN_INSTALMENT_VOID);

        boolean isDolfinIpp = Constants.ACQ_DOLFIN_INSTALMENT.equals(transData.getAcquirer().getName());
        if (isDolfinIpp)
            logo = Component.getImageFromInternalFile(Constants.SLIP_LOGO_NAME, acquirer.getNii() + "_" + acquirer.getName());

        /*------------------------------Header------------------------------*/
        // title
        if (!onErmMode) {
            if (logo == null)
                logo = Component.getSmartPayFromInternalFile(acquirer.getNii() + "_" + acquirer.getName());

            page.addLine()
                    .addUnit(page.createUnit()
                            .setBitmap(logo)
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
        page.addLine().addUnit(page.createUnit().setText(" "));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setGravity(Gravity.CENTER));

        /*------------------------------Body------------------------------*/
        if(isPrintSign) { // MERCHANT COPY
            /*------------------Acquirer Info.-------------------*/
            // terminal ID/operator ID
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_terminal_code_short))
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(acquirer.getTerminalId())
                            .setWeight(4.0f)
                            .setGravity(Gravity.LEFT)
                    );
            // merchant ID
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_merchant_code_short))
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(acquirer.getMerchantId())
                            .setGravity(Gravity.LEFT)
                            .setWeight(4.0f));
            // batch NO
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_batch_num_short))
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(Component.getPaddedNumber(transData.getBatchNo(), 6))
                            .setGravity(Gravity.LEFT)
                            .setWeight(1.0f))
                    .addUnit(page.createUnit()
                            .setText("HOST: " + acquirer.getName())
                            .setFontSize(FONT_SMALL)
                            .setGravity(Gravity.END)
                            .setWeight(2.0f));
            //trace NO , STAN NO
            String stanno = Component.getPaddedNumber(transData.getStanNo(), 6);
            stanno = transStatus == TransData.ETransStatus.VOIDED ? Component.getPaddedNumber(transData.getVoidStanNo(), 6) : stanno;
            String traceno = Component.getPaddedNumber(transData.getTraceNo(), 6);
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_trans_no_short))
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(traceno)
                            .setGravity(Gravity.LEFT)
                            .setWeight(1.0f))
                    .addUnit(page.createUnit()
                            .setText("STAN: " + stanno)
                            .setFontSize(FONT_SMALL)
                            .setGravity(Gravity.END)
                            .setWeight(2.0f));

            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.receipt_double_line))
                    .setGravity(Gravity.CENTER));

            if (isDolfinIpp) {
                if (transData.getPan() != null)
                    page.addLine().addUnit(page.createUnit().setText(transData.getPan()).setFontSize(FONT_BIG));
            }
            else {
                /*------------------Trans. Detail-------------------*/
                // card NO
                temp = (isAmex && receiptNo > 0) ? PanUtils.maskCardNo(transData.getPan(), Constants.PAN_MASK_PATTERN4) :
                        PanUtils.maskCardNo(transData.getPan(), transData.getIssuer().getPanMaskPattern());


                String tmpEnterMode = enterMode.toString();
                if (enterMode == TransData.EnterMode.INSERT) {
                    tmpEnterMode = "C";
                }
                temp += " ( " + tmpEnterMode + " )";

                temp1 = transData.getExpDate();
                if (temp1 != null && !temp1.isEmpty()) {
                    if (transData.getIssuer().isRequireMaskExpiry()) {
                        temp1 = "**/**";
                    } else {
                        temp1 = temp1.substring(2) + "/" + temp1.substring(0, 2);
                    }
                } else {
                    temp1 = "";
                }

                //card NO
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(temp)
                                .setFontSize(FONT_BIG));

                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(transData.getIssuer().getName())
                                .setFontSize(FONT_NORMAL))
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_card_date_short) + temp1)
                                .setFontSize(FONT_NORMAL)
                                .setGravity(Gravity.END));
            }
            // date/time
            String formattedDate = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                    Constants.DATE_PATTERN_DISPLAY);

            String formattedTime = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                    Constants.TIME_PATTERN_DISPLAY4);

            page.addLine()
                    .adjustTopSpace(10)
                    .addUnit(page.createUnit()
                            .setText(formattedDate)
                            .setFontSize(FONT_BIG))
                    .addUnit(page.createUnit()
                            .setText(formattedTime)
                            .setFontSize(FONT_BIG)
                            .setGravity(Gravity.END));

            // reference NO
            temp = transData.getRefNo();
            if (temp == null) {
                temp = "";
            }
            temp1 = transData.getAuthCode();
            //APP CODE
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_app_code))
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(temp1)
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.END));

            // REF. NO.
            if (transData.isOnlineTrans()) {
                if (!temp.isEmpty()) {
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText(Utils.getString(R.string.receipt_ref_no))
                                    .setFontSize(FONT_NORMAL))
                            .addUnit(page.createUnit()
                                    .setText(temp)
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

            //TRANS_TYPE
            temp = Component.getTransByIPlanMode(transData);
            if (isVoid) {
                temp = Utils.getString(R.string.trans_void).toUpperCase() + " " + temp;
            }
            page.addLine()
                    .adjustTopSpace(10)
                    .addUnit(page.createUnit()
                            .setText(temp)
                            .setFontSize(FONT_BIG));

            if (transData.isEcrProcess()) {
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(" " + Utils.getString(R.string.receipt_pos_tran))
                                .setFontSize(FONT_BIG));
            }

            //Instalment info.
            genInstalmentInfo(page, transData, true);

            // REF 1 & 2
            ReceiptGeneratorTransTOPS.printEdcRef1Ref2(page, transData);

            boolean isSignFree = transData.isSignFree();
            boolean issettleIncludeESignature = ((transData.getSettlementErmReprintMode() == TransData.SettleErmReprintMode.HideReprint_ShowSignature
                    || transData.getSettlementErmReprintMode() == TransData.SettleErmReprintMode.ShowReprint_ShowSignature) ? true : false) ;

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
                if (isSignFree) {
                    page.addLine().addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_sign_not_required))
                            .setGravity(Gravity.CENTER));
                } else {
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText(Utils.getString(R.string.receipt_sign))
                                    .setFontSize(FONT_SMALL)
                                    .setGravity(Gravity.CENTER));
                    Bitmap bitmap = loadSignature(transData);
                    if ((bitmap != null && transType != ETransType.KBANK_SMART_PAY_VOID && transStatus != TransData.ETransStatus.VOIDED && (!isRePrint || issettleIncludeESignature))) {
                        if (onErmMode) {
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
            if (isDolfinIpp) {
                if (transData.getPan() != null)
                    page.addLine().addUnit(page.createUnit().setText(transData.getPan()).setFontSize(FONT_BIG));
            }
            else {
                //CARD NO
                temp = PanUtils.maskCardNo(transData.getPan(), "(?<=\\d{0})\\d(?=\\d{4})");
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
            }
            //APP.CODE / TRANS_TYPE
            temp = Component.getTransByIPlanMode(transData);
            if (isVoid) {
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
            genInstalmentInfo(page, transData, false);

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

    private Bitmap loadSignature(TransData transData) {
        byte[] signData = transData.getSignData();
        if (signData == null) {
            return null;
        }
        return FinancialApplication.getGl().getImgProcessing().jbigToBitmap(signData);
    }

    private enum enumFreeMonthStatus {
        NONE,
        APPEAR_ZERO_PERCENT,
        APPEAR_NON_ZERO_PERCENT,
        APPEAR_PARTIAL_FREE_MONTH,
    }

    private void genInstalmentInfo(IPage page, TransData transData, boolean isMerchantCopy) {
        HashMap<ReservedFieldHandle.FieldTables, byte[]> paymentTerm = ReservedFieldHandle.unpackReservedField(transData.getField61RecByte(), ReservedFieldHandle.smtp_f61_response, true);
        HashMap<ReservedFieldHandle.FieldTables, byte[]> productInfo = ReservedFieldHandle.unpackReservedField(transData.getField63RecByte(), ReservedFieldHandle.smtp_f63_response, true);

        boolean correctiveSlipFormat = true;

        boolean isDolfin = transData.getAcquirer().getName().equals(Constants.ACQ_DOLFIN_INSTALMENT);
        if (isDolfin) {
            byte[] f63 = transData.getField63RecByte();
            paymentTerm = ReservedFieldHandle.unpackReservedField(f63, ReservedFieldHandle.dolfinIpp_f63_response, true);
            productInfo = transData.isInstalmentPromoProduct()? paymentTerm: null;
            correctiveSlipFormat = false;
        }

        // amount
        long amount = Utils.parseLongSafe(transData.getAmount(), 0);
        if (transData.getTransType().isSymbolNegative() || transData.getTransState() == TransData.ETransStatus.VOIDED){
            amount = -amount;
        }

        String totalAmt = CurrencyConverter.convert(amount, transData.getCurrency());
        String currencySymbol = null ;
        String shortenAmountCaption = (receiptNo==1) ? Utils.getString(R.string.receipt_amount_total) : Utils.getString(R.string.receipt_amount_short);
        if (correctiveSlipFormat){
            currencySymbol = totalAmt.replace("-", "").substring(0,3);
            totalAmt =totalAmt.replace(currencySymbol,"");
            shortenAmountCaption += ":" + currencySymbol;
        }


        //if (isMerchantCopy) addSingleLine(page);
        addSingleLine(page);
        add2Units(shortenAmountCaption, totalAmt, isMerchantCopy ? FONT_BIG : FONT_NORMAL, Typeface.NORMAL, page);

        if (paymentTerm != null) {
            String payment_term = transData.getInstalmentPaymentTerm() + " MONTHS";
            String int_rate = new String(paymentTerm.get(ReservedFieldHandle.FieldTables.INTEREST_RATE));
            int waive_interest_free_month = Integer.parseInt(new String(paymentTerm.get(ReservedFieldHandle.FieldTables.INTEREST_FREE)));
            String int_free_month = "" + waive_interest_free_month + " MONTH" + ((waive_interest_free_month > 1) ? "S": "");
            String handing_fee = new String(paymentTerm.get(ReservedFieldHandle.FieldTables.HANDING_FEE));
            String total_pay_amt = new String(paymentTerm.get(ReservedFieldHandle.FieldTables.TOTAL_PAY_AMOUNT));
            String month_pay_amt = new String(paymentTerm.get(ReservedFieldHandle.FieldTables.MONTH_PAY_AMOUNT));

            int_rate = String.format(Locale.getDefault(), "%.2f", Double.parseDouble(int_rate.substring(0, int_rate.length()-1))/100);

            amount = Utils.parseLongSafe(handing_fee, 0);
            handing_fee = CurrencyConverter.convert(amount, transData.getCurrency());

            amount = Utils.parseLongSafe(total_pay_amt, 0);
            total_pay_amt = CurrencyConverter.convert(amount, transData.getCurrency());

            amount = Utils.parseLongSafe(month_pay_amt, 0);
            month_pay_amt = CurrencyConverter.convert(amount, transData.getCurrency());

            /* Waive Free Month new printing ---- START */
            String monthly_zero_interest_pcnt =  Utils.getString(R.string.receipt_instalment_pay_interest).replace("%i","0") ;
            String monthly_pay_interest_pcnt =  Utils.getString(R.string.receipt_instalment_pay_interest).replace("%i",int_rate) ;
            String pcnt_non_zero_caption = Utils.getString(R.string.receipt_instalment_monthly_non_zero_pcnt).replace("%i",int_rate) ;
            String pcnt_zero_caption     = Utils.getString(R.string.receipt_instalment_monthly_non_zero_pcnt).replace("%i","0") ;
            int term_pay_interest = (transData.getInstalmentPaymentTerm() - waive_interest_free_month);
            String interest_month = String.format("%s %s",term_pay_interest, "MONTH" + ((term_pay_interest>1) ? "S" : "")) ;
            enumFreeMonthStatus freeMonthsStatus = enumFreeMonthStatus.NONE;
            if (waive_interest_free_month==0 && transData.getInstalmentPaymentTerm()> 0) {
                if (Float.parseFloat(int_rate)==0.0f) {
                    freeMonthsStatus=enumFreeMonthStatus.APPEAR_ZERO_PERCENT;
                } else {
                    freeMonthsStatus=enumFreeMonthStatus.APPEAR_NON_ZERO_PERCENT;
                }
            } else{
                freeMonthsStatus=enumFreeMonthStatus.APPEAR_PARTIAL_FREE_MONTH;
            }

            String zero_paid_monthly     = new String(paymentTerm.get(ReservedFieldHandle.FieldTables.FIRST_PAY_AMOUNT));
            String non_zero_paid_monthly = new String(paymentTerm.get(ReservedFieldHandle.FieldTables.LAST_PAY_AMOUNT));

            amount = Utils.parseLongSafe(zero_paid_monthly, 0);
            zero_paid_monthly = CurrencyConverter.convert(amount, transData.getCurrency());

            amount = Utils.parseLongSafe(non_zero_paid_monthly, 0);
            non_zero_paid_monthly = CurrencyConverter.convert(amount, transData.getCurrency());
            /* Waive Free Month new Config ---- END */

            if (correctiveSlipFormat && currencySymbol != null) {
                payment_term  = "*" + payment_term;
                handing_fee  = "*" + handing_fee.replace(currencySymbol,"");
                int_free_month  = "*" + int_free_month;
                interest_month  = "*" + interest_month;
                total_pay_amt = "*" +total_pay_amt.replace(currencySymbol,"");
                month_pay_amt = "*" +month_pay_amt.replace(currencySymbol,"");
                zero_paid_monthly = "*" +zero_paid_monthly.replace(currencySymbol,"");
                non_zero_paid_monthly = "*" +non_zero_paid_monthly.replace(currencySymbol,"");
                int_rate  = "*" + int_rate;             // process interest for the last, to avoid * sign show in another label
            }

            add2Units(Utils.getString(R.string.receipt_instalment_payment_term), payment_term, isMerchantCopy ? FONT_NORMAL : FONT_SMALL_18, Typeface.NORMAL, page);
            add2Units(Utils.getString(R.string.receipt_instalment_interest),int_rate, isMerchantCopy ? FONT_NORMAL : FONT_SMALL_18, Typeface.NORMAL, page);
            add2Units(Utils.getString(R.string.receipt_instalment_mgt_fee),handing_fee, isMerchantCopy ? FONT_NORMAL : FONT_SMALL_18, Typeface.NORMAL, page);
//            if (freeMonthsStatus == enumFreeMonthStatus.APPEAR_FREE_MONTH_ONLY
//                    || freeMonthsStatus == enumFreeMonthStatus.APPEAR_PARTIAL_FREE_MONTH) {
//                add2Units(monthly_zero_interest_pcnt,     int_free_month, isMerchantCopy ? FONT_SMALL : FONT_SMALL_18, Typeface.NORMAL, page);
//            }
//            if (freeMonthsStatus == enumFreeMonthStatus.APPEAR_WITHOUT_FREE_MONTH
//                    || freeMonthsStatus == enumFreeMonthStatus.APPEAR_PARTIAL_FREE_MONTH) {
//                add2Units(monthly_pay_interest_pcnt,      interest_month, isMerchantCopy ? FONT_SMALL : FONT_SMALL_18, Typeface.NORMAL, page);
//            }
            if (freeMonthsStatus == enumFreeMonthStatus.APPEAR_ZERO_PERCENT) {
                add2Units(monthly_zero_interest_pcnt,     interest_month, isMerchantCopy ? FONT_SMALL : FONT_SMALL_18, Typeface.NORMAL, page);
            } else if (freeMonthsStatus == enumFreeMonthStatus.APPEAR_NON_ZERO_PERCENT) {
                add2Units(monthly_pay_interest_pcnt,      interest_month, isMerchantCopy ? FONT_SMALL : FONT_SMALL_18, Typeface.NORMAL, page);
            } else if (freeMonthsStatus == enumFreeMonthStatus.APPEAR_PARTIAL_FREE_MONTH) {
                add2Units(monthly_zero_interest_pcnt,     int_free_month, isMerchantCopy ? FONT_SMALL : FONT_SMALL_18, Typeface.NORMAL, page);
                add2Units(monthly_pay_interest_pcnt,      interest_month, isMerchantCopy ? FONT_SMALL : FONT_SMALL_18, Typeface.NORMAL, page);
            }

            Log.d("INSTALLMENT-DE63","" + freeMonthsStatus.toString());
                     //Waive Free Month new printing
            //if (isMerchantCopy) addSingleLine(page);
            addSingleLine(page);
            add2Units(Utils.getString(R.string.receipt_instalment_total_due_ex), total_pay_amt, isMerchantCopy ? FONT_NORMAL : FONT_SMALL_18, Typeface.NORMAL, page);

//            if (freeMonthsStatus == enumFreeMonthStatus.APPEAR_FREE_MONTH_ONLY
//                    || freeMonthsStatus == enumFreeMonthStatus.APPEAR_PARTIAL_FREE_MONTH) {
//                add2Units(pcnt_zero_caption,zero_paid_monthly, isMerchantCopy ? FONT_SMALL : FONT_SMALL_18, Typeface.NORMAL, page);       //Waive Free Month new printing
//            }
//            if (freeMonthsStatus == enumFreeMonthStatus.APPEAR_WITHOUT_FREE_MONTH
//                    || freeMonthsStatus == enumFreeMonthStatus.APPEAR_PARTIAL_FREE_MONTH) {
//                add2Units(pcnt_non_zero_caption,non_zero_paid_monthly, isMerchantCopy ? FONT_SMALL : FONT_SMALL_18, Typeface.NORMAL, page);   //Waive Free Month new printing
//            }
            if (freeMonthsStatus == enumFreeMonthStatus.APPEAR_ZERO_PERCENT) {
                add2Units(pcnt_zero_caption,zero_paid_monthly, isMerchantCopy ? FONT_SMALL : FONT_SMALL_18, Typeface.NORMAL, page);       //Waive Free Month new printing
            } else if (freeMonthsStatus == enumFreeMonthStatus.APPEAR_NON_ZERO_PERCENT) {
                add2Units(pcnt_non_zero_caption,non_zero_paid_monthly, isMerchantCopy ? FONT_SMALL : FONT_SMALL_18, Typeface.NORMAL, page);   //Waive Free Month new printing
            } else if (freeMonthsStatus == enumFreeMonthStatus.APPEAR_PARTIAL_FREE_MONTH) {
                add2Units(pcnt_zero_caption,zero_paid_monthly, isMerchantCopy ? FONT_SMALL : FONT_SMALL_18, Typeface.NORMAL, page);       //Waive Free Month new printing
                add2Units(pcnt_non_zero_caption,non_zero_paid_monthly, isMerchantCopy ? FONT_SMALL : FONT_SMALL_18, Typeface.NORMAL, page);   //Waive Free Month new printing
            }



        }

        if (productInfo != null/* && (transData.isInstalmentPromoProduct() || "03".equals(transData.getInstalmentIPlanMode()) || "04".equals(transData.getInstalmentIPlanMode()))*/) {
            String sup_name = new String(productInfo.get(ReservedFieldHandle.FieldTables.SUPPLIER_NAME)).trim();
            String prod_name = new String(productInfo.get(ReservedFieldHandle.FieldTables.PRODUCT_NAME)).trim();
            String model_name = new String(productInfo.get(ReservedFieldHandle.FieldTables.MODEL_NAME)).trim();
            String serial_no = Component.getPaddedStringRight(transData.getInstalmentSerialNo(), 18, '9');
            //if (isMerchantCopy) addSingleLine(page);
            addSingleLine(page);
            add1Unit(Utils.getString(R.string.receipt_instalment_brand) + "   " + sup_name, isMerchantCopy ? FONT_SMALL : FONT_SMALL_18, Typeface.NORMAL, page);
            add1Unit(Utils.getString(R.string.receipt_instalment_product) + "   " + prod_name, isMerchantCopy ? FONT_SMALL : FONT_SMALL_18, Typeface.NORMAL, page);
            add1Unit(Utils.getString(R.string.receipt_instalment_model) + "   " + model_name, isMerchantCopy ? FONT_SMALL : FONT_SMALL_18, Typeface.NORMAL, page);
            add1Unit(Utils.getString(R.string.receipt_instalment_sn) + "   " + serial_no, isMerchantCopy ? FONT_SMALL : FONT_SMALL_18, Typeface.NORMAL, page);
            if (isMerchantCopy) addSingleLine(page);
        }
    }

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
