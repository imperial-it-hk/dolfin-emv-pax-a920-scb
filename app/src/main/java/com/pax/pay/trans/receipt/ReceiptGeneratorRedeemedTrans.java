package com.pax.pay.trans.receipt;

import android.graphics.Bitmap;
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
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.EReceiptUtils;
import com.pax.pay.utils.TimeConverter;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import th.co.bkkps.utils.Log;
import th.co.bkkps.utils.PageToSlipFormat;

public class ReceiptGeneratorRedeemedTrans implements IReceiptGenerator {

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
    public ReceiptGeneratorRedeemedTrans(TransData transData, int currentReceiptNo, int receiptMax, boolean isReprint) {
        this.transData = transData;
        this.receiptNo = currentReceiptNo;
        this.isRePrint = isReprint;
        this.receiptMax = receiptMax;

        //Extra for ERM
        onErmMode=false;
        PageToSlipFormat.getInstance().Reset();
    }

    public ReceiptGeneratorRedeemedTrans(TransData transData, int currentReceiptNo, int receiptMax, boolean isReprint, boolean isPrintPreview) {
        this.transData = transData;
        this.receiptNo = currentReceiptNo;
        this.isRePrint = isReprint;
        this.receiptMax = receiptMax;
        this.isPrintPreview = isPrintPreview;

        //Extra for ERM
        onErmMode=false;
        PageToSlipFormat.getInstance().Reset();
    }

    public ReceiptGeneratorRedeemedTrans(TransData transData, int currentReceiptNo, int receiptMax, boolean isReprint, boolean isPrintPreview, boolean onErmMode) {
        this.transData = transData;
        this.receiptNo = currentReceiptNo;
        this.isRePrint = isReprint;
        this.receiptMax = receiptMax;
        this.isPrintPreview = isPrintPreview;

        //Extra for ERM
        this.onErmMode = onErmMode;
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
        if(!onErmMode) {
            page.addLine()
                    .addUnit(page.createUnit()
                            .setBitmap(Component.getImageFromInternalFile(Constants.SLIP_LOGO_NAME, acquirer.getNii() + "_" + acquirer.getName()))
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

            /*------------------Trans. Detail-------------------*/
            // card NO
            temp = (isAmex && receiptNo > 0) ? PanUtils.maskCardNo(transData.getPan(), Constants.PAN_MASK_PATTERN4) :
                    PanUtils.maskCardNo(transData.getPan(), transData.getIssuer().getPanMaskPattern());


            TransData.EnterMode enterMode = transData.getEnterMode();
            String tmpEnterMode = enterMode.toString();
            if (tmpEnterMode.equals("I")) {
                if (Constants.ISSUER_UP.equals(transData.getIssuer().getIssuerBrand())) {//todo improve later
                    tmpEnterMode = "F";
                } else {
                    tmpEnterMode = "C";
                }
            } else if (enterMode == TransData.EnterMode.CLSS || enterMode == TransData.EnterMode.SP200) {
                tmpEnterMode = "CTLS";
            }

            temp += " ( " + tmpEnterMode + " )";

            String temp1 = transData.getExpDate();
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

            if (ETransType.KBANK_REDEEM_INQUIRY != transType) {
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
            }

            // REDEEMED INFO.
            genRedeemedInfo(page, transData, true);

            // REF 1 & 2
            ReceiptGeneratorTransTOPS.printEdcRef1Ref2(page, transData);

            boolean isSignFree = transData.isSignFree();
            boolean issettleIncludeESignature = ((transData.getSettlementErmReprintMode() == TransData.SettleErmReprintMode.HideReprint_ShowSignature
                    || transData.getSettlementErmReprintMode() == TransData.SettleErmReprintMode.ShowReprint_ShowSignature) ? true : false) ;

            page.addLine().addUnit(page.createUnit().setText(" "));
            if (transType != ETransType.KBANK_REDEEM_INQUIRY) {
                if (isSignFree) {
                    page.addLine().addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_sign_not_required))
                            .setGravity(Gravity.CENTER));
                } else {
                    page.addLine()
                            .adjustTopSpace(10)
                            .addUnit(page.createUnit()
                                    .setText(Utils.getString(R.string.receipt_sign))
                                    .setFontSize(FONT_SMALL)
                                    .setGravity(Gravity.CENTER));
                    Bitmap bitmap = null;


                    bitmap = loadSignature(transData);
                    if ((bitmap != null && transType != ETransType.KBANK_REDEEM_VOID
                            && transData.getTransState() != TransData.ETransStatus.VOIDED && (!isRePrint || issettleIncludeESignature))) {
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
            //CARD NO
            temp = PanUtils.maskCardNo(transData.getPan(), "(?<=\\d{0})\\d(?=\\d{4})");
            TransData.EnterMode enterMode = transData.getEnterMode();
            String tmpEnterMode = enterMode.toString();

            if (tmpEnterMode.equals("I")) {
                if (Constants.ISSUER_UP.equals(transData.getIssuer().getIssuerBrand())) {//todo improve later
                    tmpEnterMode = "F";
                } else {
                    tmpEnterMode = "C";
                }
            } else if (enterMode == TransData.EnterMode.CLSS || enterMode == TransData.EnterMode.SP200) {
                tmpEnterMode = "CTLS";
            }

            temp += " ( " + tmpEnterMode + " )";

            //card NO
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(temp)
                            .setFontSize(FONT_BIG));

            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_app_code) + " : " + transData.getAuthCode())
                            .setFontSize(FONT_SMALL_18)
                            .setGravity(Gravity.START))
                    .addUnit(page.createUnit()
                            .setText("REDEEM")
                            .setFontSize(FONT_SMALL_18)
                            .setGravity(Gravity.END));

            // REDEEMED INFO.
            genRedeemedInfo(page, transData, false);

            /*------------------End Trans. Detail-------------------*/
        }

        /*------------------------------Footer------------------------------*/
        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_sign_line))
                .setGravity(Gravity.CENTER));

        if (transType != ETransType.KBANK_REDEEM_INQUIRY) {
//            //CardHolder Name
//            temp = transData.getTrack1();
//            if(temp != null){
//                page.addLine().addUnit(page.createUnit()
//                        .setText(temp.trim())
//                        .setFontSize(FONT_BIG)
//                        .setGravity(Gravity.CENTER));
//            }

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

//            page.addLine().addUnit(page.createUnit()
//                    .setText(Utils.getString(R.string.receipt_verify))
//                    .setFontSize(FONT_SMALL)
//                    .setGravity(Gravity.CENTER));
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

            if(! onErmMode) {
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
        }

        if (!isPrintPreview) {
            page.addLine().addUnit(page.createUnit().setText("\n\n\n\n"));
        }

        if(onErmMode) {
            PageToSlipFormat.isSettleMode=false;
            PageToSlipFormat.getInstance().Append(page);
        }
        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }

    private void genRedeemedInfo(IPage page, TransData transData, boolean isMerchantCopy) {
        boolean isStateVoided = transData.getTransState() == TransData.ETransStatus.VOIDED;
        boolean isTypeVoid = isStateVoided || transData.getTransType() == ETransType.KBANK_REDEEM_VOID;

        // TRANS. NAME
        ETransType transType = isTypeVoid && !isStateVoided ? transData.getOrigTransType() : transData.getTransType();
        String strTransType = "";
        if (transType != null) {
            if (transType == ETransType.KBANK_REDEEM_DISCOUNT) {
                strTransType = "89999".equals(transData.getRedeemedDiscountType()) ? Utils.getString(R.string.trans_kbank_redeem_discount_var) : Utils.getString(R.string.trans_kbank_redeem_discount_fix);
            } else {
                strTransType = transType.getTransName();
            }
            strTransType = isTypeVoid ? strTransType.replaceFirst(" ", " " + Utils.getString(R.string.receipt_redeem_void) + " ") : strTransType;
        }
        page.addLine()
                .adjustTopSpace(10)
                .addUnit(page.createUnit()
                        .setText(strTransType.toUpperCase())
                        .setFontSize(isMerchantCopy ? FONT_NORMAL_26 : FONT_NORMAL));

        if (isMerchantCopy && transData.isEcrProcess()) {
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(" " + Utils.getString(R.string.receipt_pos_tran))
                            .setFontSize(FONT_NORMAL_26));
        }

        ReceiptKbankRedeemedTransDetail.generateDetail(page, transData, isMerchantCopy, false, isRePrint);
    }

    private Bitmap loadSignature(TransData transData) {
        byte[] signData = transData.getSignData();
        if (signData == null) {
            return null;
        }
        return FinancialApplication.getGl().getImgProcessing().jbigToBitmap(signData);
    }

    @Override
    public String generateString() {
        return null;
    }
}
