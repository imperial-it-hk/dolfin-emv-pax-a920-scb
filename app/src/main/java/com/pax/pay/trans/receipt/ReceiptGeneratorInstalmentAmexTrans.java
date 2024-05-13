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
import com.pax.pay.trans.model.TransData.EnterMode;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.EReceiptUtils;
import com.pax.pay.utils.TimeConverter;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import th.co.bkkps.utils.Log;
import th.co.bkkps.utils.PageToSlipFormat;

public class ReceiptGeneratorInstalmentAmexTrans implements IReceiptGenerator {

    private int receiptNo = 0;
    private TransData transData;
    private boolean isRePrint = false;
    private int receiptMax = 2;
    private boolean isPrintPreview = false;
    /*                                  +------------------+------------------+
                                        | CurrentReceiptNo |     receiptMax   |
        +-------------------------------+------------------+------------------+
        |    For Merchant copy          |      set = 0     |      set = 2     |
        +-------------------------------+------------------+------------------+
     */
    private boolean onErmMode = false;

    /**
     * @param transData        ：transData
     * @param currentReceiptNo : currentReceiptNo
     * @param receiptMax       ：generate which one, start from 0
     * @param isReprint        ：is reprint?
     */
    public ReceiptGeneratorInstalmentAmexTrans(TransData transData, int currentReceiptNo, int receiptMax, boolean isReprint) {
        this.transData = transData;
        this.receiptNo = currentReceiptNo;
        this.isRePrint = isReprint;
        this.receiptMax = receiptMax;
        this.onErmMode = false;
        PageToSlipFormat.getInstance().Reset();
    }

    public ReceiptGeneratorInstalmentAmexTrans(TransData transData, int currentReceiptNo, int receiptMax, boolean isReprint, boolean isPrintPreview) {
        this.transData = transData;
        this.receiptNo = currentReceiptNo;
        this.isRePrint = isReprint;
        this.receiptMax = receiptMax;
        this.isPrintPreview = isPrintPreview;
        this.onErmMode = false;
        PageToSlipFormat.getInstance().Reset();
    }

    public ReceiptGeneratorInstalmentAmexTrans(TransData transData, int currentReceiptNo, int receiptMax, boolean isReprint, boolean isPrintPreview, boolean onERMMode) {
        this.transData = transData;
        this.receiptNo = currentReceiptNo;
        this.isRePrint = isReprint;
        this.receiptMax = receiptMax;
        this.isPrintPreview = isPrintPreview;
        this.onErmMode = onERMMode;
        PageToSlipFormat.getInstance().Reset();
    }

    @Override
    public Bitmap generateBitmap() {
        // transaction type
        ETransType transType = transData.getTransType();
        TransData.OfflineStatus offlineSendState = transData.getOfflineSendState();

        boolean isPrintSign = false;
        boolean isTxnSmallAmt = transData.isTxnSmallAmt();//EDCBBLAND-426 Support small amount

        // 生成第几张凭单不合法时， 默认0
        if (receiptNo > receiptMax) {
            receiptNo = 0;
        } else if (isTxnSmallAmt && receiptMax == 1) {
            receiptNo = receiptMax;
        }

        if ((receiptNo == 0 || ((receiptMax == 3) && receiptNo == 1))) {
            isPrintSign = true;
        }

        IPage page = Device.generatePage();
        TransData.ETransStatus transStatus = transData.getTransState();

        SysParam sysParam = FinancialApplication.getSysParam();
        Acquirer acquirer = transData.getAcquirer();
        String temp, panMask;

        /*Header*/
        // title
        if (!onErmMode) {
            page.addLine()
                    .addUnit(page.createUnit()
                            .setBitmap(Component.getImageFromInternalFile(Constants.SLIP_LOGO_NAME, acquirer.getNii() + "_" + acquirer.getName()))
                            .setGravity(Gravity.CENTER));
        } else {
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("[@]" + transData.getInitAcquirerIndex() + "[$]")
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

        /*Body*/
        if (isPrintSign) {
            //Sign copy, full info
            // terminal ID/operator ID
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_terminal_code_short_sharp))
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(acquirer.getTerminalId())
                            .setWeight(3.0f)
                            .setGravity(Gravity.LEFT)
                    );
            // merchant ID
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_merchant_code_short_sharp))
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(acquirer.getMerchantId())
                            .setGravity(Gravity.LEFT)
                            .setWeight(3.0f));

            //trace NO , STAN NO
            String stanno = Component.getPaddedNumber(transData.getStanNo(), 6);
            stanno = (transStatus == TransData.ETransStatus.VOIDED) ? Component.getPaddedNumber(transData.getVoidStanNo(), 6) : stanno;
            String traceno = Component.getPaddedNumber(transData.getTraceNo(), 6);

            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_stan_code))
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(stanno)
                            .setGravity(Gravity.LEFT)
                            .setWeight(3.0f));
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_trans_no_short_sharp))
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(traceno)
                            .setGravity(Gravity.LEFT)
                            .setWeight(3.0f));

            // batch NO
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_batch_num_short_sharp))
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(Component.getPaddedNumber(transData.getBatchNo(), 6))
                            .setGravity(Gravity.LEFT)
                            .setWeight(3.0f));


            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.receipt_double_line))
                    .setGravity(Gravity.CENTER));


            // card NO
            panMask = PanUtils.maskCardNo(transData.getPan(), Constants.PAN_MASK_PATTERN4);//Amex Pattern

            TransData.EnterMode enterMode = transData.getEnterMode();
            String tmpEnterMode = enterMode.toString();
            if (tmpEnterMode.equals("I")) {
                tmpEnterMode = "C";
            } else if (enterMode == TransData.EnterMode.CLSS || enterMode == TransData.EnterMode.SP200) {
                tmpEnterMode = "CTLS";
            }
            temp = panMask + " ( " + tmpEnterMode + " )";

            String temp1 = transData.getExpDate();
            if (temp1 != null && !temp1.isEmpty()) {
                if (transData.getIssuer().isRequireMaskExpiry()) {
                    temp1 = "**/**";
                } else {
                    temp1 = temp1.substring(2) + "/" + temp1.substring(0, 2);// 将yyMM转换成MMyy
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
                            .setText(Utils.getString(R.string.receipt_amex_instalment_issuer))
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(temp1)
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.END));

            //blank line
            page.addLine().addUnit(page.createUnit().setText(" "));

            // date/time
            String formattedDate = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                    Constants.DATE_PATTERN_DISPLAY);

            String formattedTime = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                    Constants.TIME_PATTERN_DISPLAY4);

            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(formattedDate)
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(formattedTime)
                            .setFontSize(FONT_NORMAL)
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


            if (enterMode == EnterMode.INSERT || enterMode == EnterMode.CLSS || enterMode == EnterMode.SP200) {
                page.addLine().addUnit(page.createUnit().setText(" "));
                if (transData.getEmvAppLabel() != null) {
                    //APP
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText("APP: " + transData.getEmvAppLabel())
                                    .setFontSize(FONT_SMALL_18));
                }

                if (transData.getAid() != null) {
                    //AID
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText("AID: " + transData.getAid())
                                    .setFontSize(FONT_SMALL_18));
                }

                if (transData.getTvr() != null) {
                    //TVR
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText("TVR: " + transData.getTvr())
                                    .setFontSize(FONT_SMALL_18));
                }

                if (transData.getTc() != null) {
                    //TC
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText("TC: " + transData.getTc())
                                    .setFontSize(FONT_SMALL_18));
                }
            }

            page.addLine().addUnit(page.createUnit().setText(" "));

            temp = transStatus.equals(TransData.ETransStatus.NORMAL) ? "" : " (" + transStatus.toString() + ")";
            String transName = Utils.getString(R.string.trans_sale).toUpperCase();
            if (transData.getReferralStatus() != null && transData.getReferralStatus() != TransData.ReferralStatus.NORMAL) {
                transName = transName + Utils.getString(R.string.receipt_amex_call_issuer) + temp;

            } else if (transStatus.equals(TransData.ETransStatus.VOIDED) || transType == ETransType.VOID) {
                transName = Utils.getString(R.string.receipt_void_sale);
            }

            transName += transData.isEcrProcess() ? Utils.getString(R.string.receipt_pos_tran) : "";
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(transName)
                            .setFontSize(FONT_NORMAL));


            //base & tip
       /* if (transType.isAdjustAllowed()) {
            long base = Utils.parseLongSafe(transData.getAmount(), 0) - Utils.parseLongSafe(transData.getTipAmount(), 0);
            temp = CurrencyConverter.convert(base, transData.getCurrency());
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_amount_base))
                            .setFontSize(FONT_NORMAL)
                            .setWeight(4.0f))
                    .addUnit(page.createUnit()
                            .setText(temp)
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.END)
                            .setWeight(9.0f));

            long tips = Utils.parseLongSafe(transData.getTipAmount(), 0);
            temp = CurrencyConverter.convert(tips, transData.getCurrency());

            if(!transData.getTipAmount().equals("0")){
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_amount_tip))
                                .setFontSize(FONT_NORMAL)
                                .setWeight(2.0f))
                        .addUnit(page.createUnit().setText(temp)
                                .setFontSize(FONT_NORMAL)
                                .setGravity(Gravity.END)
                                .setWeight(3.0f));
            }

            page.addLine().addUnit(page.createUnit().setText(" "));
        }*/

            if (isTxnSmallAmt && receiptNo == 0 && transData.getIssuer() != null
                    && !(transData.getEnterMode() == EnterMode.CLSS || transData.getEnterMode() == EnterMode.SP200)) {
                String txtSmallAmtbyIssuer = Component.getReceiptTxtSmallAmt(transData.getIssuer().getIssuerBrand());
                if (txtSmallAmtbyIssuer != null) {
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText(txtSmallAmtbyIssuer)
                                    .setFontSize(FONT_NORMAL)
                                    .setGravity(Gravity.START));
                }
            }

            // amount
            long amount = Utils.parseLongSafe(transData.getAmount(), 0);
            if (transType.isSymbolNegative() || transData.getTransState() == TransData.ETransStatus.VOIDED)
                amount = -amount;
            temp = CurrencyConverter.convert(amount, transData.getCurrency());
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_amount_total))
                            .setFontSize(FONT_BIG)
                            .setWeight(4.0f))
                    .addUnit(page.createUnit()
                            .setText(temp)
                            .setFontSize(FONT_BIG)
                            .setGravity(Gravity.END)
                            .setWeight(9.0f));
            page.addLine().addUnit(page.createUnit().setText(" "));


            page.addLine().addUnit(page.createUnit().setText(" "));
            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.receipt_double_line))
                    .setGravity(Gravity.CENTER));

            //Amex Instalment data

            //TERM
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_instalment_term))
                            .setFontSize(FONT_SMALL)
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(transData.getInstalmentTerms() + " MONTHS")
                            .setFontSize(FONT_SMALL)
                            .setGravity(Gravity.LEFT)
                            .setWeight(2.5f));

            //TOTAL DUE
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_instalment_total_due))
                            .setFontSize(FONT_SMALL)
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(CurrencyConverter.convert(Utils.parseLongSafe(transData.getAmount(), 0), transData.getCurrency()))
                            .setFontSize(FONT_SMALL)
                            .setGravity(Gravity.LEFT)
                            .setWeight(2.5f));

            //MONTHLY DUE
            temp = CurrencyConverter.convert(Utils.parseLongSafe(transData.getInstalmentMonthDue(), 0), transData.getCurrency());
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_instalment_monthly_due))
                            .setFontSize(FONT_SMALL)
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(temp)
                            .setFontSize(FONT_SMALL)
                            .setGravity(Gravity.LEFT)
                            .setWeight(2.5f));

            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.receipt_double_line))
                    .setGravity(Gravity.CENTER));

            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.receipt_instalment_msg_amex)));

            page.addLine().addUnit(page.createUnit().setText(" "));

            boolean isSignFree = transData.isSignFree();
            boolean issettleIncludeESignature = (transData.getSettlementErmReprintMode() == TransData.SettleErmReprintMode.HideReprint_ShowSignature
                    || transData.getSettlementErmReprintMode() == TransData.SettleErmReprintMode.ShowReprint_ShowSignature);

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
                if (isSignFree || (isTxnSmallAmt && receiptNo == 0)) {//For Small Amt transaction, present only in Merchant Copy
                    page.addLine().addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_sign_not_required))
                            .setGravity(Gravity.CENTER));
                } else {

                    if (!isTxnSmallAmt || issettleIncludeESignature) {
                        page.addLine().addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_sign))
                                .setFontSize(FONT_SMALL)
                                .setGravity(Gravity.CENTER));
                        Bitmap bitmap = loadSignature(transData);
                        if (bitmap != null) {
                            if (onErmMode) {
                                // Extra for ERM
                                page.addLine().addUnit(page.createUnit()
                                        .setText("[@][@SIGN_DATA]")
                                        .setGravity(Gravity.LEFT));
                            } else {
                                page.addLine().addUnit(page.createUnit()
                                        .setBitmap(bitmap)
                                        .setGravity(Gravity.CENTER));
                            }
                        }

                        if (bitmap == null) {
                            page.addLine().addUnit(page.createUnit().setText(" "));
                            //Skip Signature process, print space for cardholder manual Sign
                            page.addLine().addUnit(page.createUnit().setText("\n\n"));
                        }

                    }
//                page.addLine().addUnit(page.createUnit()
//                        .setText(Utils.getString(R.string.receipt_signx_line))
//                        .setFontSize(FONT_SMALL)
//                        .setGravity(Gravity.CENTER));
//                page.addLine().addUnit(page.createUnit().setText(" "));
                }
            }

        } else {
            //No sign copy, summary info
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
            stanno = (transStatus == TransData.ETransStatus.VOIDED) ? Component.getPaddedNumber(transData.getVoidStanNo(), 6) : stanno;
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
                            .setText(Utils.getString(R.string.receipt_amex_instalment_issuer))
                            .setFontSize(FONT_SMALL_18))
                    .addUnit(page.createUnit()
                            .setText(formattedTime)
                            .setFontSize(FONT_SMALL_18)
                            .setGravity(Gravity.END));


            //CARD NO
            if (isTxnSmallAmt && receiptNo == 0) {
                panMask = PanUtils.maskCardNo(transData.getPan(), transData.getIssuer().getPanMaskPattern());
            } else {
                panMask = PanUtils.maskCardNo(transData.getPan(), "(?<=\\d{0})\\d(?=\\d{4})");
            }

            EnterMode enterMode = transData.getEnterMode();
            String tmpEnterMode = enterMode.toString();
            if (tmpEnterMode.equals("I")) {
                tmpEnterMode = "C";
            } else if (enterMode == EnterMode.CLSS || enterMode == EnterMode.SP200) {
                tmpEnterMode = "CTLS";
            }
            temp = panMask + " ( " + tmpEnterMode + " )";

            //card NO
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(temp)
                            .setFontSize(FONT_BIG));

            page.addLine().addUnit(page.createUnit().setText(" "));

            //TransType //APP CODE
            if (transStatus.equals(TransData.ETransStatus.VOIDED) || transType == ETransType.VOID) {
                temp = Utils.getString(R.string.receipt_void_sale);
            } else {
                temp = Utils.getString(R.string.trans_sale).toUpperCase();
            }
            if (transData.getReferralStatus() != null && transData.getReferralStatus() != TransData.ReferralStatus.NORMAL) {
                temp += Utils.getString(R.string.receipt_amex_call_issuer);
            }

            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(temp.toUpperCase())
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.START))
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_app_code) + " : " + transData.getAuthCode())
                            .setFontSize(FONT_SMALL_18)
                            .setGravity(Gravity.END));

            if (isTxnSmallAmt && receiptNo == 0 && transData.getIssuer() != null
                    && !(transData.getEnterMode() == EnterMode.CLSS || transData.getEnterMode() == EnterMode.SP200)) {
                String txtSmallAmtbyIssuer = Component.getReceiptTxtSmallAmt(transData.getIssuer().getIssuerBrand());
                if (txtSmallAmtbyIssuer != null) {
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText(txtSmallAmtbyIssuer)
                                    .setFontSize(FONT_NORMAL)
                                    .setGravity(Gravity.START));
                }
            }

            // amount
            long amount = Utils.parseLongSafe(transData.getAmount(), 0);
            if (transType.isSymbolNegative() || transData.getTransState() == TransData.ETransStatus.VOIDED)
                amount = -amount;
            temp = CurrencyConverter.convert(amount, transData.getCurrency());
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText((receiptNo==1) ? "TOTAL" : "BASE")
                            .setFontSize(FONT_BIG)
                            .setWeight(4.0f))
                    .addUnit(page.createUnit()
                            .setText(temp)
                            .setFontSize(FONT_BIG)
                            .setGravity(Gravity.END)
                            .setWeight(9.0f));

            page.addLine().addUnit(page.createUnit().setText(" "));

            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.receipt_double_line))
                    .setGravity(Gravity.CENTER));

            //Amex Instalment data

            //TERM
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_instalment_term))
                            .setFontSize(FONT_SMALL)
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(transData.getInstalmentTerms() + " MONTHS")
                            .setFontSize(FONT_SMALL)
                            .setGravity(Gravity.LEFT)
                            .setWeight(2.5f));

            //TOTAL DUE
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_instalment_total_due))
                            .setFontSize(FONT_SMALL)
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(CurrencyConverter.convert(Utils.parseLongSafe(transData.getAmount(), 0), transData.getCurrency()))
                            .setFontSize(FONT_SMALL)
                            .setGravity(Gravity.LEFT)
                            .setWeight(2.5f));

            //MONTHLY DUE
            temp = CurrencyConverter.convert(Utils.parseLongSafe(transData.getInstalmentMonthDue(), 0), transData.getCurrency());
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_instalment_monthly_due))
                            .setFontSize(FONT_SMALL)
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(temp)
                            .setFontSize(FONT_SMALL)
                            .setGravity(Gravity.LEFT)
                            .setWeight(2.5f));

            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.receipt_double_line))
                    .setGravity(Gravity.CENTER));

            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.receipt_instalment_msg_amex)));

            page.addLine().addUnit(page.createUnit().setText(" "));
            if (isTxnSmallAmt && receiptNo == 0) {//present only in Merchant Copy
                page.addLine().addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_sign_not_required))
                        .setGravity(Gravity.CENTER));
            }
        }

        /*Footer*/
        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_sign_line))
                .setGravity(Gravity.CENTER));

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

        try {
            String Disclaimer[] = EReceiptUtils.stringSplitter(Utils.getString(R.string.receipt_verify), 34);
            for (int dcmrIndex = 0; dcmrIndex <= Disclaimer.length - 1; dcmrIndex++) {
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

        if (!onErmMode) {
            if (Component.isAllowSignatureUpload(transData) && transData.geteReceiptUploadStatus() != TransData.UploadStatus.NORMAL && receiptNo == 0) {
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.footer_ereceipt_upload_not_success))
                                .setFontSize(FONT_BIG)
                                .setGravity(Gravity.CENTER));
            }
        }

        /*if (acquirer.isEnableTle() && (offlineSendState == null || TransData.OfflineStatus.OFFLINE_SENT == offlineSendState)) {
            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.receipt_trusted_trans))
                    .setFontSize(FONT_SMALL)
                    .setGravity(Gravity.CENTER));
        }*/

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
//        }

        if (Component.isDemo()) {
            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.demo_mode))
                    .setGravity(Gravity.CENTER));
        }

        if (!isPrintPreview) {
            page.addLine().addUnit(page.createUnit().setText("\n\n\n\n"));
        }

        if (onErmMode) {
            PageToSlipFormat.isSettleMode = false;
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

    @Override
    public String generateString() {
        return null;
    }
}
