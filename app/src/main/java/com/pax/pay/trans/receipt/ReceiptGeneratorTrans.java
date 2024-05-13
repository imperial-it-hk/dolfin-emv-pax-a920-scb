/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-11-25
 * Module Author: Steven.W
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.trans.receipt;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.view.Gravity;

import androidx.annotation.StringRes;

import com.pax.abl.utils.PanUtils;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.eemv.utils.Tools;
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
import com.pax.pay.utils.TimeConverter;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;

import th.co.bkkps.utils.Log;

/**
 * receipt generator
 *
 * @author Steven.W
 */
public class ReceiptGeneratorTrans implements IReceiptGenerator {

    private int receiptNo = 0;
    private TransData transData;
    private boolean isRePrint = false;
    private int receiptMax = 2;
    private boolean isPrintPreview = false;
    private boolean isShowQRBarcode = false;

    /**
     * @param transData        ：transData
     * @param currentReceiptNo : currentReceiptNo
     * @param receiptMax       ：generate which one, start from 0
     * @param isReprint        ：is reprint?
     */
    public ReceiptGeneratorTrans(TransData transData, int currentReceiptNo, int receiptMax, boolean isReprint) {
        this.transData = transData;
        this.receiptNo = currentReceiptNo;
        this.isRePrint = isReprint;
        this.receiptMax = receiptMax;
    }

    public ReceiptGeneratorTrans(TransData transData, int currentReceiptNo, int receiptMax, boolean isReprint, boolean isPrintPreview) {
        this.transData = transData;
        this.receiptNo = currentReceiptNo;
        this.isRePrint = isReprint;
        this.receiptMax = receiptMax;
        this.isPrintPreview = isPrintPreview;
    }

    @Override
    public Bitmap generateBitmap() {
        // transaction type
        ETransType transType = transData.getTransType();
        TransData.OfflineStatus offlineSendState = transData.getOfflineSendState();

        boolean isPrintSign = false;
        boolean isAmex = Constants.ACQ_AMEX.equals(transData.getAcquirer().getName());
        boolean isTxnSmallAmt = transData.isTxnSmallAmt();//EDCBBLAND-426 Support small amount
        boolean isPinVerifyMsg = transData.isPinVerifyMsg();
        boolean isOfflineTransSend = ETransType.OFFLINE_TRANS_SEND == transType || ETransType.OFFLINE_TRANS_SEND == transData.getOrigTransType();// offline via menu.
        boolean isVoidRefund = ETransType.REFUND == transData.getOrigTransType();

        // 生成第几张凭单不合法时， 默认0
        if (receiptNo > receiptMax) {
            receiptNo = 0;
        } else if (isTxnSmallAmt && receiptMax == 1) {
            receiptNo = receiptMax;
        }

        if (isAmex || ((!isTxnSmallAmt || isPinVerifyMsg) && (receiptNo == 0/* || ((receiptMax == 3) && receiptNo == 1)*/))) {
            isPrintSign = true;
        }// Transaction is small amount, no need to print sign, print only customer copy format layout. (Except AMEX)

        IPage page = Device.generatePage();
        TransData.ETransStatus transStatus = transData.getTransState();

        SysParam sysParam = FinancialApplication.getSysParam();
        Acquirer acquirer = transData.getAcquirer();
        String temp, panMask;

        /*Header*/
        // title
        page.addLine()
                .addUnit(page.createUnit()
                        .setBitmap(Component.getImageFromInternalFile(Constants.SLIP_LOGO_NAME, acquirer.getNii() + "_" + acquirer.getName()))
                        .setGravity(Gravity.CENTER));
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

        /*Body*/
        if (isPrintSign) {
            //Sign copy, full info
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
            stanno = (transType == ETransType.SALE && transStatus == TransData.ETransStatus.VOIDED) ? Component.getPaddedNumber(transData.getVoidStanNo(), 6) : stanno;
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


            // card NO
            if (transType == ETransType.PREAUTH) {
                panMask = isAmex ? PanUtils.maskCardNo(transData.getPan(), Constants.PAN_MASK_PATTERN4) : transData.getPan();
            } else {
                panMask = (isAmex && receiptNo > 0) ? PanUtils.maskCardNo(transData.getPan(), Constants.PAN_MASK_PATTERN4) :
                        PanUtils.maskCardNo(transData.getPan(), transData.getIssuer().getPanMaskPattern());
            }

            TransData.EnterMode enterMode = transData.getEnterMode();
            String tmpEnterMode = enterMode.toString();
            if (tmpEnterMode.equals("I")) {
                tmpEnterMode = "C";
            } else if (enterMode == EnterMode.CLSS || enterMode == EnterMode.SP200) {
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
                            .setText(transData.getIssuer().getName())
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_card_date_short) + temp1)
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

            if ((enterMode == EnterMode.INSERT || enterMode == EnterMode.CLSS) &&
                    (transType == ETransType.SALE || transType == ETransType.PREAUTH)) {

            }


            if ((enterMode == EnterMode.INSERT || enterMode == EnterMode.CLSS || enterMode == EnterMode.SP200) &&
                    (transType == ETransType.SALE || transType == ETransType.PREAUTH ||
                            transType == ETransType.VOID || transType == ETransType.OFFLINE_TRANS_SEND ||
                            transType == ETransType.REFUND)) {
                if (transData.getAid().contains(Constants.AMEX_AID_PREFIX)) {
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
                } else {
                    //APP /AID
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText("APP: " + transData.getEmvAppLabel())
                                    .setFontSize(FONT_SMALL_18)
                                    .setWeight(6))
                            .addUnit(page.createUnit()
                                    .setText("AID: " + transData.getAid())
                                    .setFontSize(FONT_SMALL_18)
                                    .setGravity(Gravity.END)
                                    .setWeight(7));
                }
                boolean isTvrPresent = transData.getTvr() != null && !transData.getTvr().isEmpty();
                boolean isTcPresent = transData.getTc() != null && !transData.getTc().isEmpty();
                if (isTvrPresent || isTcPresent) {
                    //TVR / TC
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText(isTvrPresent ? "TVR: " + transData.getTvr() : "")
                                    .setFontSize(FONT_SMALL_18)
                                    .setWeight(5))
                            .addUnit(page.createUnit()
                                    .setText(isTcPresent ? "TC: " + transData.getTc() : "")
                                    .setFontSize(FONT_SMALL_18)
                                    .setGravity(Gravity.END)
                                    .setWeight(7));
                }
            }

            printDccInfo(page, 1, true);

            page.addLine().addUnit(page.createUnit().setText(" "));

            temp = transStatus.equals(TransData.ETransStatus.NORMAL) ? "" : " (" + transStatus.toString() + ")";
            String transName;
            if (isAmex && transData.getReferralStatus() != null && transData.getReferralStatus() != TransData.ReferralStatus.NORMAL) {
                transName = transType + Utils.getString(R.string.receipt_amex_call_issuer) + temp;

            } else if (transStatus.equals(TransData.ETransStatus.VOIDED) || transType == ETransType.VOID) {
                transName = (!isOfflineTransSend) ? Utils.getString(R.string.receipt_void_sale) : Utils.getString(R.string.receipt_void_offline);
                transName = (!isVoidRefund) ? transName : Utils.getString(R.string.receipt_void_refund);
            } else {
                transName = (!isOfflineTransSend) ? (transType + temp) : transType.getTransName().toUpperCase();
            }

            transName += transData.isEcrProcess() ? Utils.getString(R.string.receipt_pos_tran) : "";
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(transName)
                            .setFontSize(FONT_BIG));

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

            printInstantDiscountInfo(page, acquirer);

            printDccInfo(page, 2, true);

            /*
            //Original Trans NO. for VOID
            if (transType == ETransType.VOID) {
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_orig_trans_no)
                                        + Component.getPaddedNumber(transData.getOrigTransNo(), 6)));
            }*/


            long pinFreeAmtLong = sysParam.get(SysParam.NumberParam.QUICK_PASS_TRANS_PIN_FREE_AMOUNT);
            long signFreeAmtLong = sysParam.get(SysParam.NumberParam.QUICK_PASS_TRANS_SIGN_FREE_AMOUNT);
            boolean isPinFree = transData.isPinFree();
            boolean isSignFree = transData.isSignFree();

//            if (isPinFree && isSignFree) {// sign free and pin free
//                if (enterMode == EnterMode.QR) {
//                    page.addLine().addUnit(page.createUnit()
//                            .setText(Utils.getString(R.string.receipt_qr_signature)));
//                } else {
//                    page.addLine().addUnit(page.createUnit()
//                            .setText(genFreePrompt(R.string.receipt_amount_prompt_start,
//                                    Math.min(pinFreeAmtLong, signFreeAmtLong), R.string.receipt_amount_prompt_end)));
//                }
//            } else if (isSignFree) {// only sign free
//                /*page.addLine().addUnit(page.createUnit()
//                        .setText(genFreePrompt(R.string.receipt_amount_prompt_start,
//                                signFreeAmtLong, R.string.receipt_amount_prompt_end_sign)));*/
//            } else if (isPinFree && !transData.isCDCVM()) {// pin free
//                page.addLine().addUnit(page.createUnit()
//                        .setText(genFreePrompt(R.string.receipt_amount_prompt_start,
//                                pinFreeAmtLong, R.string.receipt_amount_prompt_end_pin)));
//            }

            if (isPinVerifyMsg) {//PIN VERIFY
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
                }

                if (!isTxnSmallAmt && !isSignFree) {
                    page.addLine().addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_sign))
                            .setFontSize(FONT_SMALL)
                            .setGravity(Gravity.CENTER));
                    Bitmap bitmap = null;

                    bitmap = loadSignature(transData);
                    if (bitmap != null) {
                        page.addLine().addUnit(page.createUnit()
                                .setBitmap(loadSignature(transData))
                                .setGravity(Gravity.CENTER));
                    }


                    if (bitmap == null) {
                        page.addLine().addUnit(page.createUnit().setText(" "));
                        //Skip Signature process, print space for cardholder manual Sign
                        page.addLine().addUnit(page.createUnit().setText("\n\n"));
                    }
                }
            }

            printDccInfo(page, 3, true);
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


            //CARD NO
            if (transType == ETransType.PREAUTH) {
                panMask = transData.getPan();
            } else {
                if (isTxnSmallAmt && receiptNo == 0) {
                    panMask = PanUtils.maskCardNo(transData.getPan(), transData.getIssuer().getPanMaskPattern());
                } else {
                    panMask = PanUtils.maskCardNo(transData.getPan(), "(?<=\\d{0})\\d(?=\\d{4})");
                }
            }
            TransData.EnterMode enterMode = transData.getEnterMode();
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
                temp = (!isOfflineTransSend && !isVoidRefund) ? Utils.getString(R.string.receipt_void_sale) : "";
            } else {
                temp = transStatus.equals(TransData.ETransStatus.NORMAL) ? transType.getTransName() : " (" + transType.getTransName() + " " + transStatus.toString() + ")";
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

            printDccInfo(page, 1, false);

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
            //////
            page.addLine().addUnit(page.createUnit().setText(" "));

            printInstantDiscountInfo(page, acquirer);

            printDccInfo(page, 2, false);

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

        //CardHolder Name
        temp = transData.getTrack1();
        if (temp != null) {
            page.addLine().addUnit(page.createUnit()
                    .setText(temp.trim())
                    .setFontSize(FONT_BIG)
                    .setGravity(Gravity.CENTER));
        }

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_verify))
                .setFontSize(FONT_SMALL)
                .setGravity(Gravity.CENTER));

        Component.genAppVersiononSlip(page);

        page.addLine().adjustTopSpace(5)
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_no_refund))
                        .setFontSize(FONT_NORMAL_22)
                        .setGravity(Gravity.CENTER));

        if (acquirer.isEnableTle() && (offlineSendState == null || TransData.OfflineStatus.OFFLINE_SENT == offlineSendState)) {
            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.receipt_trusted_trans))
                    .setFontSize(FONT_SMALL)
                    .setGravity(Gravity.CENTER));
        }

        /* Generate QR code and Barcode */
        genQRBarCode(page, transData.getAmount(), panMask);
        /* End Generate QR code and Barcode */

//        if (receiptMax == 3) {
//            if (receiptNo == 0) {
//                page.addLine()
//                        .adjustTopSpace(5)
//                        .addUnit(page.createUnit()
//                                .setText(Utils.getString(R.string.receipt_stub_acquire))
//                                .setGravity(Gravity.CENTER));
//            } else if (receiptNo == 1) {
//                page.addLine()
//                        .adjustTopSpace(5)
//                        .addUnit(page.createUnit()
//                                .setText(Utils.getString(R.string.receipt_stub_merchant))
//                                .setGravity(Gravity.CENTER));
//            } else {
//                page.addLine()
//                        .adjustTopSpace(5)
//                        .addUnit(page.createUnit()
//                                .setText(Utils.getString(R.string.receipt_stub_cust))
//                                .setGravity(Gravity.CENTER));
//            }
//        } else {
        if (receiptNo == 0) {
            page.addLine()
                    .adjustTopSpace(5)
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_stub_merchant))
                            .setGravity(Gravity.CENTER));
            genImageOnEndReceipt(page, false);
        } else {
            page.addLine()
                    .adjustTopSpace(5)
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_stub_cust))
                            .setGravity(Gravity.CENTER));

            genImageOnEndReceipt(page, true);
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

        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }

    public Bitmap generatePromptPayReceiptBitmap() {
        boolean isPrintSign = false;
        // 生成第几张凭单不合法时， 默认0
        if (receiptNo > receiptMax) {
            receiptNo = 0;
        }
        // the first copy print signature, if three copies, the second copy should print signature too
        if (receiptNo == 0 || ((receiptMax == 3) && receiptNo == 1)) {
            isPrintSign = true;
        }

        IPage page = Device.generatePage();
        // transaction type
        ETransType transType = transData.getTransType();

        SysParam sysParam = FinancialApplication.getSysParam();
        Acquirer acquirer = transData.getAcquirer();

        String temp;

        /*Header*/
        // title
        page.addLine()
                .addUnit(page.createUnit()
                        .setBitmap(Component.getImageFromInternalFile(Constants.SLIP_LOGO_NAME))
                        .setGravity(Gravity.CENTER));
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

        /*Body*/
        //TID
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_terminal_code_short_sharp) + acquirer.getTerminalId())
                        .setFontSize(FONT_SMALL));
        //MER
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_merchant_code_short_sharp) + acquirer.getMerchantId())
                        .setFontSize(FONT_SMALL));
        String prompStan = Component.getPaddedNumber(transData.getStanNo(), 6);

        //STAN / TRACE
        String prompTrace = Component.getPaddedNumber(transData.getTraceNo(), 6);
        page.addLine()
                .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stan_code) + prompStan)
                                .setFontSize(FONT_SMALL)
                        //.setWeight(5)
                )
                .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_trans_no_short_sharp) + prompTrace)
                                .setFontSize(FONT_SMALL)
                                .setGravity(Gravity.END)
                        //.setWeight(7)
                );

        //BATCH / APP CODE
        page.addLine()
                .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_batch_num_short_sharp) + Component.getPaddedNumber(transData.getBatchNo(), 6))
                                .setFontSize(FONT_SMALL)
                        //.setWeight(5)
                )
                .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_appr_code) + transData.getAuthCode())
                                .setFontSize(FONT_SMALL)
                                .setGravity(Gravity.END)
                        //.setWeight(7)
                );


        // date/time
        String formattedDate = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.DATE_PATTERN_DISPLAY);

        String formattedTime = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.TIME_PATTERN_DISPLAY4);

        page.addLine()
                .addUnit(page.createUnit()
                        .setText(formattedDate)
                        .setFontSize(FONT_SMALL))
                .addUnit(page.createUnit()
                        .setText(formattedTime)
                        .setFontSize(FONT_SMALL)
                        .setGravity(Gravity.END));

        // issuer name = QR PROMPTPAY
        page.addLine()
                .adjustTopSpace(5)
                .addUnit(page.createUnit()
                        .setText(transData.getIssuer().getName())
                        .setFontSize(FONT_BIG));


        if (transData.getTransType() == ETransType.PROMPTPAY_VOID || transData.getTransState() == TransData.ETransStatus.VOIDED) {
            page.addLine()
                    .adjustTopSpace(10)
                    .addUnit(page.createUnit()
                            .setText("VOID SALE")
                            .setFontSize(FONT_BIG)
                            .setGravity(Gravity.CENTER));
        } else if (transData.getQrSaleState() == TransData.QrSaleState.QR_SEND_OFFLINE) {
            page.addLine()
                    .adjustTopSpace(10)
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_qr_offline))
                            .setFontSize(FONT_BIG)
                            .setGravity(Gravity.CENTER));
        } else if (transData.isTransInqID() || transData.getTransType() == ETransType.BPS_QR_INQUIRY_ID) {
            page.addLine()
                    .adjustTopSpace(10)
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_qr_inquiry))
                            .setFontSize(FONT_BIG)
                            .setGravity(Gravity.CENTER));
        } else {
            page.addLine()
                    .adjustTopSpace(10)
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_qr_sale))
                            .setFontSize(FONT_BIG)
                            .setGravity(Gravity.CENTER));
        }


        if (transData.getQrSaleState() == TransData.QrSaleState.QR_SEND_OFFLINE) {
            // ref#2
            page.addLine()
                    .adjustTopSpace(10)
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_ref2) + " " + transData.getQrRef2())
                            .setFontSize(FONT_NORMAL));
        } else {
            // trans id.
            page.addLine()
                    .adjustTopSpace(10)
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_trans_id) + " " + transData.getRefNo())
                            .setFontSize(FONT_NORMAL));
            // ref#2
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_ref2) + " " + transData.getQrRef2())
                            .setFontSize(FONT_NORMAL));
        }

        // amount
        long amount = Utils.parseLongSafe(transData.getAmount(), 0);
        if (transType.isSymbolNegative() || transData.getTransType() == ETransType.PROMPTPAY_VOID
                || transData.getTransState() == TransData.ETransStatus.VOIDED)
            amount = -amount;
        temp = CurrencyConverter.convert(amount, transData.getCurrency());
        page.addLine()
                .adjustTopSpace(10)
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_amount_short))
                        .setFontSize(FONT_BIG)
                        .setWeight(4.0f))
                .addUnit(page.createUnit()
                        .setText(temp)
                        .setFontSize(FONT_BIG)
                        .setGravity(Gravity.END)
                        .setWeight(9.0f));
        //////

        /*Footer*/
        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_sign_line))
                .setGravity(Gravity.CENTER));

        page.addLine()
                .adjustTopSpace(5)
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_verify))
                        .setFontSize(FONT_SMALL)
                        .setGravity(Gravity.CENTER));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_no_refund))
                .setFontSize(FONT_NORMAL_22)
                .setGravity(Gravity.CENTER));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_sign_line))
                .setGravity(Gravity.CENTER));

        /* Generate QR code and Barcode */
        genQRBarCode(page, transData.getAmount(), transData.getQrRef2());
        /* End Generate QR code and Barcode */

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
        /*page.addLine()
                .addUnit(page.createUnit()
                        .setText(getTerminalAndAppVersion())
                        .setFontSize(FONT_SMALL));*/

        if (Component.isDemo()) {
            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.demo_mode))
                    .setGravity(Gravity.CENTER));
        }

        if (!isPrintPreview) {
            page.addLine().addUnit(page.createUnit().setText("\n\n\n\n"));
        }

        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }

    public Bitmap generatePromptPayAllinOneReceiptBitmap() {
        boolean isPrintSign = false;
        // 生成第几张凭单不合法时， 默认0
        if (receiptNo > receiptMax) {
            receiptNo = 0;
        }
        // the first copy print signature, if three copies, the second copy should print signature too
        if (receiptNo == 0 || ((receiptMax == 3) && receiptNo == 1)) {
            isPrintSign = true;
        }

        IPage page = Device.generatePage();
        page.setTypefaceObj(Typeface.createFromAsset(FinancialApplication.getApp().getAssets(), Constants.FONT_NAME_MONO));
        // transaction type
        ETransType transType = transData.getTransType();

        SysParam sysParam = FinancialApplication.getSysParam();
        Acquirer acquirer = transData.getAcquirer();

        String temp;
        String issuerName = Component.findQRType(transData);

        String cardID = transData.getQrType();
        String refName = Utils.getString(R.string.receipt_qr_id);
        String ref = transData.getQrID();

        if (cardID.equals("01")) {
            refName = Utils.getString(R.string.receipt_ref2);
            ref = transData.getQrRef2();
        }

        /*Header*/
        // title
        page.addLine()
                .addUnit(page.createUnit()
                        .setBitmap(Component.getImageFromInternalFile(Constants.SLIP_LOGO_NAME))
                        .setGravity(Gravity.CENTER));
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
                .setText(Utils.getString(R.string.receipt_one_line_wallet))
                .setGravity(Gravity.CENTER));

        /*Body*/
        //TID
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_terminal_code_short_sharp) + acquirer.getTerminalId())
                        .setFontSize(FONT_SMALL));
        //MER
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_merchant_code_short_sharp) + acquirer.getMerchantId())
                        .setFontSize(FONT_SMALL));

        //STAN / TRACE
        String prompStan = Component.getPaddedNumber(transData.getStanNo(), 6);
        String prompTrace = Component.getPaddedNumber(transData.getTraceNo(), 6);
        page.addLine()
                .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stan_code) + prompStan)
                                .setFontSize(FONT_SMALL)
                        //.setWeight(5)
                )
                .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_trans_no_short_sharp) + prompTrace)
                                .setFontSize(FONT_SMALL)
                                .setGravity(Gravity.END)
                        //.setWeight(7)
                );

        //BATCH / APP CODE
        page.addLine()
                .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_batch_num_short_sharp) + Component.getPaddedNumber(transData.getBatchNo(), 6))
                                .setFontSize(FONT_SMALL)
                        //.setWeight(5)
                )
                .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_appr_code) + transData.getAuthCode())
                                .setFontSize(FONT_SMALL)
                                .setGravity(Gravity.END)
                        //.setWeight(7)
                );


        // date/time
        String formattedDate = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.DATE_PATTERN_DISPLAY);

        String formattedTime = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.TIME_PATTERN_DISPLAY4);

        page.addLine()
                .addUnit(page.createUnit()
                        .setText(formattedDate)
                        .setFontSize(FONT_SMALL))
                .addUnit(page.createUnit()
                        .setText(formattedTime)
                        .setFontSize(FONT_SMALL)
                        .setGravity(Gravity.END));

        // issuer name
        page.addLine()
                .adjustTopSpace(5)
                .addUnit(page.createUnit()
                        .setText(issuerName)
                        .setFontSize(FONT_BIG));

        temp = Utils.getString(R.string.receipt_qr_sale);
        if (transType == ETransType.QR_VOID || transData.getTransState() == TransData.ETransStatus.VOIDED) {
            temp = Utils.getString(R.string.receipt_qr_void);
        } else if (transType == ETransType.STATUS_INQUIRY_ALL_IN_ONE) {
            temp = Utils.getString(R.string.receipt_qr_inquiry);
        }

        page.addLine()
                .adjustTopSpace(15)
                .addUnit(page.createUnit()
                        .setText(temp)
                        .setFontSize(FONT_BIG)
                        .setGravity(Gravity.CENTER));

        temp = splitStr(transData.getRefNo(), 4, "-");
        page.addLine()
                .adjustTopSpace(15)
                .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_trans_id))
                                .setFontSize(FONT_SMALL)
                        //.setWeight(5)
                )
                .addUnit(page.createUnit()
                                .setText(temp)
                                .setFontSize(FONT_SMALL)
                                .setGravity(Gravity.END)
                        //.setWeight(7)
                );

        if (ref != null) {
            temp = splitStr(ref, 4, "-");
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(refName)
                            .setFontSize(FONT_SMALL)
                            .setWeight(0.3f)
                    )
                    .addUnit(page.createUnit()
                                    .setText(temp)
                                    .setFontSize(FONT_SMALL)
                                    .setGravity(Gravity.END)
                            //.setWeight(7)
                    );
        }


        // amount
        long amount = Utils.parseLongSafe(transData.getAmount(), 0);
        if (transType.isSymbolNegative() || transData.getTransState() == TransData.ETransStatus.VOIDED)
            amount = -amount;
        temp = CurrencyConverter.convert(amount, transData.getCurrency());
        page.addLine()
                .adjustTopSpace(15)
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_amount_short))
                        .setFontSize(FONT_BIG)
                        .setWeight(2.5f))
                .addUnit(page.createUnit()
                        .setText(temp)
                        .setFontSize(FONT_BIG)
                        .setGravity(Gravity.END)
                        .setWeight(10.0f));

        if (transData.getWalletSlipInfo() != null) {
            page.addLine()
                    .adjustTopSpace(15)
                    .addUnit(page.createUnit()
                            .setText(transData.getWalletSlipInfo())
                            .setFontSize(FONT_SMALL_19)
                    );
        }

        page.addLine()
                .adjustTopSpace(5)
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_one_line_wallet))
                        .setGravity(Gravity.CENTER));

        page.addLine()
                .adjustTopSpace(5)
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_verify))
                        .setFontSize(FONT_SMALL)
                        .setGravity(Gravity.CENTER));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_no_refund))
                .setFontSize(FONT_NORMAL_22)
                .setGravity(Gravity.CENTER));

        /* Generate QR code and Barcode */
        genQRBarCode(page, transData.getAmount(), ref);
        /* End Generate QR code and Barcode */

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

        if (Component.isDemo()) {
            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.demo_mode))
                    .setGravity(Gravity.CENTER));
        }

        if (!isPrintPreview) {
            page.addLine().addUnit(page.createUnit().setText("\n\n\n\n"));
        }

        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }

    public Bitmap generateWalletReceiptBitmap() {
        boolean isPrintSign = false;
        // 生成第几张凭单不合法时， 默认0
        if (receiptNo > receiptMax) {
            receiptNo = 0;
        }
        // the first copy print signature, if three copies, the second copy should print signature too
        if (receiptNo == 0 || ((receiptMax == 3) && receiptNo == 1)) {
            isPrintSign = true;
        }

        IPage page = Device.generatePage();
        page.setTypefaceObj(Typeface.createFromAsset(FinancialApplication.getApp().getAssets(), Constants.FONT_NAME_MONO));
        // transaction type
        ETransType transType = transData.getTransType();

        SysParam sysParam = FinancialApplication.getSysParam();
        Acquirer acquirer = transData.getAcquirer();

        String temp;

        /*Header*/
        // title
        page.addLine()
                .addUnit(page.createUnit()
                        .setBitmap(Component.getImageFromInternalFile(Constants.SLIP_LOGO_NAME))
                        .setGravity(Gravity.CENTER));
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
                .setText(Utils.getString(R.string.receipt_one_line_wallet))
                .setGravity(Gravity.CENTER));

        /*Body*/
        //TID /MERCHANT
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_terminal_code_short_sharp) + acquirer.getTerminalId())
                        .setFontSize(FONT_SMALL));

        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_merchant_code_short_sharp) + acquirer.getMerchantId())
                        .setFontSize(FONT_SMALL));


        //BATCH / HOST
        temp = Component.getPaddedNumber(transData.getStanNo(), 6);
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_batch_num_short_sharp) + Component.getPaddedNumber(transData.getBatchNo(), 6))
                        .setFontSize(FONT_SMALL)
                )
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_host) + transData.getIssuer().getName())
                        .setFontSize(FONT_SMALL)
                        .setGravity(Gravity.END)
                );

        //STAN / TRACE
        String walletStan;
        if ((transType == ETransType.QR_SALE_WALLET || transType == ETransType.SALE_WALLET) && transData.getTransState() == TransData.ETransStatus.VOIDED) {
            walletStan = Component.getPaddedNumber(transData.getVoidStanNo(), 6);
        } else {
            walletStan = Component.getPaddedNumber(transData.getStanNo(), 6);
        }
        String walletTrace = Component.getPaddedNumber(transData.getTraceNo(), 6);
        page.addLine()
                .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_trans_no_short_sharp) + walletTrace)
                                .setFontSize(FONT_SMALL)
                        //.setWeight(5)
                )
                .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stan_code) + walletStan)
                                .setFontSize(FONT_SMALL)
                                .setGravity(Gravity.END)
                        //.setWeight(7)
                );

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_double_line))
                .setGravity(Gravity.CENTER));

        // date/time
        String formattedDate = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.DATE_PATTERN_DISPLAY);

        String formattedTime = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.TIME_PATTERN_DISPLAY4);

        page.addLine()
                .addUnit(page.createUnit()
                        .setText(formattedDate)
                        .setFontSize(FONT_SMALL))
                .addUnit(page.createUnit()
                        .setText(formattedTime)
                        .setFontSize(FONT_SMALL)
                        .setGravity(Gravity.END));

        //APP CODE
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_appr_code))
                        .setFontSize(FONT_SMALL)
                )
                .addUnit(page.createUnit()
                        .setText(transData.getAuthCode())
                        .setFontSize(FONT_SMALL)
                        .setGravity(Gravity.END)
                );

        //REF NO.
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("REF. NO.")
                        .setFontSize(FONT_SMALL)
                )
                .addUnit(page.createUnit()
                        .setText(transData.getRefNo())
                        .setFontSize(FONT_SMALL)
                        .setGravity(Gravity.END)
                );

        //WALLET SALE / VOID
        TransData.ETransStatus transState = transData.getTransState();
        String typeWallet = transState == TransData.ETransStatus.VOIDED ? Utils.getString(R.string.trans_wallet_void).toUpperCase() : transData.getTransType().getTransName().toUpperCase();
        page.addLine()
                .adjustTopSpace(5)
                .addUnit(page.createUnit()
                        .setText(typeWallet)
                        .setFontSize(FONT_BIG)
                );


        // amount
        long amount = Utils.parseLongSafe(transData.getAmount(), 0);
        if (transType.isSymbolNegative())
            amount = -amount;
        if (transState == TransData.ETransStatus.VOIDED)
            amount = -amount;
        temp = CurrencyConverter.convert(amount, transData.getCurrency());
        //TOTAL //AMOUNT
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("TOTAL")
                        .setFontSize(FONT_BIG)
                )
                .addUnit(page.createUnit()
                        .setText(temp)
                        .setFontSize(FONT_BIG)
                        .setGravity(Gravity.END)
                );

        page.addLine().addUnit(page.createUnit().setText("\n"));

        page.addLine()
                .addUnit(page.createUnit()
                        .setText(transData.getWalletSlipInfo())
                        .setFontSize(FONT_SMALL_19)
                );

        page.addLine().addUnit(page.createUnit().setText("\n"));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_verify))
                .setFontSize(FONT_SMALL)
                .setGravity(Gravity.CENTER));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_no_refund))
                .setFontSize(FONT_NORMAL_22)
                .setGravity(Gravity.CENTER));

        /* Generate QR code and Barcode */
        genQRBarCode(page, transData.getAmount(), transData.getRefNo());
        /* End Generate QR code and Barcode */

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
                                .setText(Utils.getString(R.string.receipt_stub_user))
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
        /*page.addLine()
                .addUnit(page.createUnit()
                        .setText(getTerminalAndAppVersion())
                        .setFontSize(FONT_SMALL));*/

        if (Component.isDemo()) {
            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.demo_mode))
                    .setGravity(Gravity.CENTER));
        }

        if (!isPrintPreview) {
            page.addLine().addUnit(page.createUnit().setText("\n\n\n\n"));
        }

        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }

    public Bitmap generateKbankWalletReceiptBitmap() {
        boolean isPrintSign = false;
        // 生成第几张凭单不合法时， 默认0
        if (receiptNo > receiptMax) {
            receiptNo = 0;
        }
        // the first copy print signature, if three copies, the second copy should print signature too
        if (receiptNo == 0 || ((receiptMax == 3) && receiptNo == 1)) {
            isPrintSign = true;
        }

        IPage page = Device.generatePage();
        page.setTypefaceObj(Typeface.createFromAsset(FinancialApplication.getApp().getAssets(), Constants.FONT_NAME_MONO));
        // transaction type
        ETransType transType = transData.getTransType();

        SysParam sysParam = FinancialApplication.getSysParam();
        Acquirer acquirer = transData.getAcquirer();

        String temp;

        /*Header*/
        // title
        page.addLine()
                .addUnit(page.createUnit()
                        .setBitmap(Component.getImageFromInternalFile(Constants.SLIP_LOGO_NAME, acquirer.getNii() + "_" + acquirer.getName()))
                        .setGravity(Gravity.CENTER));
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
                .setText(Utils.getString(R.string.receipt_one_line_wallet))
                .setGravity(Gravity.CENTER));

        /*Body*/
        //TID /MERCHANT
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_terminal_code_short_sharp) + acquirer.getTerminalId())
                        .setFontSize(FONT_SMALL));

        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_merchant_code_short_sharp) + acquirer.getMerchantId())
                        .setFontSize(FONT_SMALL));


        //BATCH / HOST
        temp = Component.getPaddedNumber(transData.getStanNo(), 6);
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_batch_num_short_sharp) + Component.getPaddedNumber(transData.getBatchNo(), 6))
                        .setFontSize(FONT_SMALL)
                )
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_host) + transData.getIssuer().getName())
                        .setFontSize(FONT_SMALL)
                        .setGravity(Gravity.END)
                );

        //STAN / TRACE
        String walletStan;
        if ((transType == ETransType.QR_VOID_KPLUS && transData.getTransState() == TransData.ETransStatus.VOIDED)
                || (transType == ETransType.QR_VOID_ALIPAY && transData.getTransState() == TransData.ETransStatus.VOIDED)
                || (transType == ETransType.QR_VOID_WECHAT && transData.getTransState() == TransData.ETransStatus.VOIDED)) {
            walletStan = Component.getPaddedNumber(transData.getVoidStanNo(), 6);
        } else {
            walletStan = Component.getPaddedNumber(transData.getStanNo(), 6);
        }
        String walletTrace = Component.getPaddedNumber(transData.getTraceNo(), 6);
        page.addLine()
                .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_trans_no_short_sharp) + walletTrace)
                                .setFontSize(FONT_SMALL)
                        //.setWeight(5)
                )
                .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stan_code) + walletStan)
                                .setFontSize(FONT_SMALL)
                                .setGravity(Gravity.END)
                        //.setWeight(7)
                );

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_double_line))
                .setGravity(Gravity.CENTER));

        // date/time
        String formattedDate = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.DATE_PATTERN_DISPLAY);

        String formattedTime = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.TIME_PATTERN_DISPLAY4);

        page.addLine()
                .addUnit(page.createUnit()
                        .setText(formattedDate)
                        .setFontSize(FONT_SMALL))
                .addUnit(page.createUnit()
                        .setText(formattedTime)
                        .setFontSize(FONT_SMALL)
                        .setGravity(Gravity.END));

        //APP CODE
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_appr_code))
                        .setFontSize(FONT_SMALL)
                )
                .addUnit(page.createUnit()
                        .setText(transData.getAuthCode())
                        .setFontSize(FONT_SMALL)
                        .setGravity(Gravity.END)
                );

        TransData.ETransStatus transState = transData.getTransState();

        //WALLET SALE / VOID
//        String transName = transData.getTransType() != null ? transData.getTransType().getTransName() : "";
//        String transNameVoid = transName.contains(" ") ? (transName.substring(0, transName.indexOf(" ") + 1) + "VOID").toUpperCase() : transName.toUpperCase();
        String typeWallet = transState == TransData.ETransStatus.VOIDED ? Utils.getString(R.string.trans_void).toUpperCase() : Utils.getString(R.string.trans_sale).toUpperCase();
        typeWallet += transData.isEcrProcess() ? " " + Utils.getString(R.string.receipt_pos_tran) : "";
        page.addLine()
                .adjustTopSpace(5)
                .addUnit(page.createUnit()
                        .setText(typeWallet)
                        .setFontSize(FONT_NORMAL_26));


        String acquirerName = acquirer.getName();

        //TRAN ID
        if (Constants.ACQ_KPLUS.equals(acquirerName)) {
            page.addLine()
                    .adjustTopSpace(5)
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_tran_id_colon))
                            .setFontSize(FONT_NORMAL_22))
                    .addUnit(page.createUnit()
                            .setText(transData.getTxnNo() != null ? transData.getTxnNo().trim() : "")
                            .setFontSize(FONT_SMALL));
        } else {
            page.addLine()
                    .adjustTopSpace(5)
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_tran_id_colon))
                            .setFontSize(FONT_NORMAL_22));
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(transData.getWalletPartnerID() != null ? transData.getWalletPartnerID().trim() : "")
                            .setFontSize(FONT_SMALL));
        }

        //CHANNEL
        switch (acquirerName) {
            case Constants.ACQ_KPLUS:
                temp = Utils.getString(R.string.receipt_kplus);
                break;
            case Constants.ACQ_ALIPAY:
            case Constants.ACQ_ALIPAY_B_SCAN_C:
                temp = Utils.getString(R.string.receipt_alipay);
                break;
            case Constants.ACQ_WECHAT:
            case Constants.ACQ_WECHAT_B_SCAN_C:
                temp = Utils.getString(R.string.receipt_wechat);
                break;
        }
        page.addLine()
                .adjustTopSpace(5)
                .addUnit(page.createUnit()
                        .setText(typeWallet)
                        .setFontSize(FONT_BIG)
                );


        // amount
        long amount = Utils.parseLongSafe(transData.getAmount(), 0);
        if (transType.isSymbolNegative())
            amount = -amount;
        if (transState == TransData.ETransStatus.VOIDED)
            amount = -amount;
        temp = CurrencyConverter.convert(amount, transData.getCurrency());
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_amount_colon))
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(temp)
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));

        page.addLine().addUnit(page.createUnit().setText(" "));

        // NO.
        if (!Constants.ACQ_KPLUS.equals(acquirerName)) {
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_no))
                            .setFontSize(FONT_NORMAL_22));
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(transData.getWalletPartnerID())
                            .setFontSize(FONT_SMALL));
        }

        if (transData.getPayTime() != null && !Constants.ACQ_KPLUS.equals(acquirerName)) {
            temp = TimeConverter.convert(transData.getPayTime().trim(), Constants.TIME_PATTERN_TRANS, Constants.TIME_PATTERN_DISPLAY);
        } else {
            temp = transState == TransData.ETransStatus.VOIDED ?
                    TimeConverter.convert(transData.getOrigDateTime(), Constants.TIME_PATTERN_TRANS, Constants.TIME_PATTERN_DISPLAY) :
                    TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS, Constants.TIME_PATTERN_DISPLAY);
        }
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_pay_time_colon))
                        .setFontSize(FONT_NORMAL_22));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(temp)
                        .setFontSize(FONT_NORMAL_22)
                        .setGravity(Gravity.END));


        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_one_line_wallet)));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_no_sig_required))
                .setFontSize(FONT_NORMAL_22)
                .setGravity(Gravity.CENTER));

        page.addLine()
                .adjustTopSpace(5)
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_verify))
                        .setFontSize(FONT_SMALL)
                        .setGravity(Gravity.CENTER));

        Component.genAppVersiononSlip(page);

        page.addLine().adjustTopSpace(5)
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_no_refund))
                        .setFontSize(FONT_NORMAL_22)
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
                                .setText(Utils.getString(R.string.receipt_stub_user))
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

        if (Component.isDemo()) {
            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.demo_mode))
                    .setGravity(Gravity.CENTER));
        }

        if (!isPrintPreview) {
            page.addLine().addUnit(page.createUnit().setText("\n\n\n\n"));
        }

        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }

    public Bitmap generateKbankMyPromptReceiptBitmap() {
        boolean isPrintSign = false;
        // 生成第几张凭单不合法时， 默认0
        if (receiptNo > receiptMax) {
            receiptNo = 0;
        }
        // the first copy print signature, if three copies, the second copy should print signature too
        if (receiptNo == 0 || ((receiptMax == 3) && receiptNo == 1)) {
            isPrintSign = true;
        }

        IPage page = Device.generatePage();
        page.setTypefaceObj(Typeface.createFromAsset(FinancialApplication.getApp().getAssets(), Constants.FONT_NAME_MONO));
        // transaction type
        ETransType transType = transData.getTransType();

        SysParam sysParam = FinancialApplication.getSysParam();
        Acquirer acquirer = transData.getAcquirer();

        String temp;

        /*Header*/
        // title
        page.addLine()
                .addUnit(page.createUnit()
                        .setBitmap(Component.getImageFromInternalFile(Constants.SLIP_LOGO_NAME, acquirer.getNii() + "_" + acquirer.getName()))
                        .setGravity(Gravity.CENTER));
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
                .setText(Utils.getString(R.string.receipt_one_line_wallet))
                .setGravity(Gravity.CENTER));

        /*Body*/
        //TID /MERCHANT
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_terminal_code_short_sharp) + acquirer.getTerminalId())
                        .setFontSize(FONT_SMALL));

        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_merchant_code_short_sharp) + acquirer.getMerchantId())
                        .setFontSize(FONT_SMALL));


        //BATCH / HOST
        temp = Component.getPaddedNumber(transData.getStanNo(), 6);
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_batch_num_short_sharp) + Component.getPaddedNumber(transData.getBatchNo(), 6))
                        .setFontSize(FONT_SMALL)
                )
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_host) + transData.getIssuer().getName())
                        .setFontSize(FONT_SMALL)
                        .setGravity(Gravity.END)
                );

        //STAN / TRACE
        String walletStan;
        if (transType == ETransType.QR_MYPROMPT_VOID) {
            walletStan = Component.getPaddedNumber(transData.getVoidStanNo(), 6);
        } else {
            walletStan = Component.getPaddedNumber(transData.getStanNo(), 6);
        }
        String walletTrace = Component.getPaddedNumber(transData.getTraceNo(), 6);
        page.addLine()
                .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_trans_no_short_sharp) + walletTrace)
                                .setFontSize(FONT_SMALL)
                        //.setWeight(5)
                )
                .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stan_code) + walletStan)
                                .setFontSize(FONT_SMALL)
                                .setGravity(Gravity.END)
                        //.setWeight(7)
                );

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_double_line))
                .setGravity(Gravity.CENTER));

        // date/time
        String formattedDate = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.DATE_PATTERN_DISPLAY);

        String formattedTime = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.TIME_PATTERN_DISPLAY4);

        page.addLine()
                .addUnit(page.createUnit()
                        .setText(formattedDate)
                        .setFontSize(FONT_SMALL))
                .addUnit(page.createUnit()
                        .setText(formattedTime)
                        .setFontSize(FONT_SMALL)
                        .setGravity(Gravity.END));

        //APP CODE
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_appr_code))
                        .setFontSize(FONT_SMALL)
                )
                .addUnit(page.createUnit()
                        .setText(transData.getAuthCode())
                        .setFontSize(FONT_SMALL)
                        .setGravity(Gravity.END)
                );

        TransData.ETransStatus transState = transData.getTransState();

        String typeWallet = transType == ETransType.QR_MYPROMPT_VOID ? Utils.getString(R.string.trans_void).toUpperCase() : Utils.getString(R.string.trans_sale).toUpperCase();
        typeWallet += transData.isEcrProcess() ? " " + Utils.getString(R.string.receipt_pos_tran) : "";
        page.addLine()
                .adjustTopSpace(5)
                .addUnit(page.createUnit()
                        .setText(typeWallet)
                        .setFontSize(FONT_NORMAL_26));


        String acquirerName = acquirer.getName();

        //TRAN ID
        page.addLine()
                .adjustTopSpace(5)
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_tran_id_colon))
                        .setFontSize(FONT_NORMAL_22));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(transData.getWalletPartnerID() != null ? transData.getWalletPartnerID().trim() : "")
                        .setFontSize(FONT_SMALL));

        //CHANNEL
        switch (acquirerName) {
            case Constants.ACQ_KPLUS:
                temp = Utils.getString(R.string.receipt_kplus);
                break;
            case Constants.ACQ_ALIPAY:
            case Constants.ACQ_ALIPAY_B_SCAN_C:
                temp = Utils.getString(R.string.receipt_alipay);
                break;
            case Constants.ACQ_WECHAT:
            case Constants.ACQ_WECHAT_B_SCAN_C:
                temp = Utils.getString(R.string.receipt_wechat);
                break;
        }
        page.addLine()
                .adjustTopSpace(5)
                .addUnit(page.createUnit()
                        .setText(typeWallet)
                        .setFontSize(FONT_BIG)
                );


        // amount
        long amount = Utils.parseLongSafe(transData.getAmount(), 0);
        if (transType.isSymbolNegative())
            amount = -amount;
        if (transType == ETransType.QR_MYPROMPT_VOID)
            amount = -amount;
        temp = CurrencyConverter.convert(amount, transData.getCurrency());
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_amount_colon))
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(temp)
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));

        page.addLine().addUnit(page.createUnit().setText(" "));

        if (transData.getPayTime() != null && !Constants.ACQ_KPLUS.equals(acquirerName)) {
            temp = TimeConverter.convert(transData.getPayTime().trim(), Constants.TIME_PATTERN_TRANS, Constants.TIME_PATTERN_DISPLAY);
        } else {
            temp = transState == TransData.ETransStatus.VOIDED ?
                    TimeConverter.convert(transData.getOrigDateTime(), Constants.TIME_PATTERN_TRANS, Constants.TIME_PATTERN_DISPLAY) :
                    TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS, Constants.TIME_PATTERN_DISPLAY);
        }
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_pay_time_colon))
                        .setFontSize(FONT_NORMAL_22));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(temp)
                        .setFontSize(FONT_NORMAL_22)
                        .setGravity(Gravity.END));


        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_one_line_wallet)));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_no_sig_required))
                .setFontSize(FONT_NORMAL_22)
                .setGravity(Gravity.CENTER));

        page.addLine()
                .adjustTopSpace(5)
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_verify))
                        .setFontSize(FONT_SMALL)
                        .setGravity(Gravity.CENTER));

        Component.genAppVersiononSlip(page);

        page.addLine().adjustTopSpace(5)
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_no_refund))
                        .setFontSize(FONT_NORMAL_22)
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
                                .setText(Utils.getString(R.string.receipt_stub_user))
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

        if (Component.isDemo()) {
            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.demo_mode))
                    .setGravity(Gravity.CENTER));
        }

        if (!isPrintPreview) {
            page.addLine().addUnit(page.createUnit().setText("\n\n\n\n"));
        }

        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }

    private String genFreePrompt(@StringRes int amountPrompt, long amount, @StringRes int resultPrompt) {
        return Utils.getString(amountPrompt)
                + CurrencyConverter.convert(amount, transData.getCurrency())
                + ", "
                + Utils.getString(resultPrompt);
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

    private Bitmap loadSignature(TransData transData) {
        byte[] signData = transData.getSignData();
        if (signData == null) {
            return null;
        }
        return FinancialApplication.getGl().getImgProcessing().jbigToBitmap(signData);
    }

    /***
     * // generate bar code
     * private void generateBarCode(TransData transData, IPage page) {
     * <p>
     * ETransType transType = ETransType.valueOf(transData.getTransType());
     * if (transType == ETransType.AUTH || transType == ETransType.SALE) {
     * if (transData.getTransState().equals(ETransStatus.VOIDED)) {
     * return;
     * }
     * try {
     * JSONObject json = new JSONObject();
     * <p>
     * json.put("authCode", transData.getAuthCode());
     * json.put("date", transData.getDateTime().substring(4, 8));
     * json.put("transNo", transData.getStanNo());
     * json.put("refNo", transData.getRefNo());
     * <p>
     * JSONArray array = new JSONArray();
     * array.put(json);
     * <p>
     * page.addLine().addUnit(
     * FinancialApplication.gl.getImgProcessing().generateBarCode(array.toString(), 230, 230,
     * BarcodeFormat.QR_CODE), Gravity.CENTER);
     * } catch (JSONException e) {
     * e.printStackTrace();
     * }
     * <p>
     * }
     * }
     ***/

    private void printDccInfo(IPage page, int type, boolean isMerchantCopy) {
        if (transData.isDccRequired()) {
            boolean isMasterCard = Constants.ISSUER_MASTER.equals(transData.getIssuer().getName());
            switch (type) {
                case 1:
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText(Utils.getString(R.string.receipt_dcc_currency_code))
                                    .setFontSize(FONT_NORMAL))
                            .addUnit(page.createUnit()
                                    .setText(Tools.bytes2String(transData.getDccCurrencyCode()))
                                    .setFontSize(FONT_NORMAL)
                                    .setGravity(Gravity.END));
                    break;
                case 2:
                    double exRate = transData.getDccConversionRate() != null ? Double.parseDouble(transData.getDccConversionRate()) / 10000 : 0;
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText(Utils.getString(R.string.receipt_dcc_ex_rate))
                                    .setFontSize(FONT_SMALL_18))
                            .addUnit(page.createUnit()
                                    .setText(String.format(Locale.getDefault(), "%.4f", exRate))
                                    .setFontSize(FONT_SMALL_18)
                                    .setGravity(Gravity.END));
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText(Utils.getString(R.string.receipt_dcc_trans_currency))
                                    .setFontSize(FONT_SMALL_18));

                    // amount
                    long amount = Utils.parseLongSafe(transData.getDccAmount(), 0);
                    if (transData.getTransType().isSymbolNegative() || transData.getTransState() == TransData.ETransStatus.VOIDED)
                        amount = -amount;

                    String currencyNumeric = Tools.bytes2String(transData.getDccCurrencyCode());
                    String strAmt = CurrencyConverter.convert(amount, currencyNumeric);
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText(Utils.getString(R.string.receipt_amount_total))
                                    .setFontSize(FONT_BIG)
                                    .setWeight(4.0f))
                            .addUnit(page.createUnit()
                                    .setText(isMerchantCopy && !isMasterCard ? "[" + Utils.getString(R.string.receipt_check_mark) + "] " + strAmt : strAmt)
                                    .setFontSize(FONT_BIG)
                                    .setGravity(Gravity.END)
                                    .setWeight(9.0f));
                    page.addLine().addUnit(page.createUnit().setText(" "));

                    if (isMasterCard) {
                        page.addLine()
                                .addUnit(page.createUnit()
                                        .setText(Utils.getString(R.string.receipt_dcc_ack_mastercard))
                                        .setFontSize(FONT_SMALL_18));
                    } else {
                        page.addLine()
                                .addUnit(page.createUnit()
                                        .setText(Utils.getString(R.string.receipt_dcc_ack_visa,
                                                CurrencyConverter.getCurrencySymbol(CurrencyConverter.getDefCurrency(), true),
                                                CurrencyConverter.getCurrencySymbol(currencyNumeric, true), "%"))
                                        .setFontSize(FONT_SMALL_18));
                    }
                    page.addLine().addUnit(page.createUnit().setText(" "));
                    break;
                case 3:
                    page.addLine().addUnit(page.createUnit()
                            .setText(!isMasterCard ? "[" + Utils.getString(R.string.receipt_check_mark) + "] " + Utils.getString(R.string.receipt_sign_short) : " ")
                            .setFontSize(FONT_SMALL_18)
                            .setGravity(Gravity.START));
                    break;
            }
        }
    }

    private void printInstantDiscountInfo(IPage page, Acquirer acquirer) {
        //Only apply for Union Pay transaction
        if (Constants.ACQ_UP.equals(acquirer.getName()) && transData.getField63RecByte() != null && transData.getField63RecByte().length == 36) {
            byte[] tempBytes = Arrays.copyOfRange(transData.getField63RecByte(), 12, 24);

            //Instant Discount Amount
            String tempStr = Tools.bytes2String(tempBytes);
            tempStr = CurrencyConverter.convert(Utils.parseLongSafe(tempStr, 0), transData.getCurrency());
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_discount))
                            .setFontSize(FONT_NORMAL)
                            .setWeight(4.0f))
                    .addUnit(page.createUnit()
                            .setText(tempStr)
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.END)
                            .setWeight(9.0f));

            //Paid Amount
            tempBytes = Arrays.copyOfRange(transData.getField63RecByte(), 24, 36);
            tempStr = Tools.bytes2String(tempBytes);
            tempStr = CurrencyConverter.convert(Utils.parseLongSafe(tempStr, 0), transData.getCurrency());
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_paid))
                            .setFontSize(FONT_NORMAL)
                            .setWeight(4.0f))
                    .addUnit(page.createUnit()
                            .setText(tempStr)
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.END)
                            .setWeight(9.0f));
            page.addLine().addUnit(page.createUnit().setText(" "));
        }

    }

    @Override
    public String generateString() {
        return "Card No:" + transData.getPan() + "\nTrans Type:" + transData.getTransType().toString()
                + "\nAmount:" + CurrencyConverter.convert(Utils.parseLongSafe(transData.getAmount(), 0), transData.getCurrency())
                + "\nTip:" + CurrencyConverter.convert(Utils.parseLongSafe(transData.getTipAmount(), 0), transData.getCurrency())
                + "\nTransData:" + transData.getDateTime();
    }

    private String splitStr(String str, int len, String gap) {
        String temp = "";
        if (gap == null) {
            gap = " ";
        }
        for (int i = 0; i < str.length(); ) {
            temp += str.substring(i, i + len);
            i += len;
            if (i < str.length()) {
                temp += gap;
            }
        }
        return temp;
    }

    /**
     * Generate QR code and Barcode
     *
     * @param page
     * @param res1
     * @param res2
     */
    private void genQRBarCode(IPage page, Object res1, Object res2) {
        String sRes1 = (String) res1;
        String sRes2 = (String) res2;

        isShowQRBarcode = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_QR_BARCODE);
        if (isShowQRBarcode && ((receiptMax == 3 && receiptNo == 1) || (receiptMax != 3 && receiptNo == 0))) {// only merchant copy
            if (sRes1 != null && !sRes1.isEmpty()) {
                long amount = Utils.parseLongSafe(sRes1, 0);
                if (transData.getTransType().isSymbolNegative() || transData.getTransState() == TransData.ETransStatus.VOIDED)
                    amount = -amount;
                String temp = CurrencyConverter.convertWithoutCurrency(amount, transData.getCurrency());

                // result 1
                page.addLine()
                        .addUnit(page.createUnit()
                                .setBitmap(Utils.generateQrCode(temp, 200, 200))
                                .setGravity(Gravity.CENTER));
                page.addLine()
                        .addUnit(page.createUnit()
                                .setBitmap(Utils.generateBarCode(temp, 300, 80))
                                .setGravity(Gravity.CENTER));
                page.addLine().addUnit(page.createUnit()
                        .setText(temp)
                        .setFontSize(FONT_SMALL)
                        .setWeight(5.0f)
                        .setGravity(Gravity.CENTER));
            }

            if (sRes2 != null && !sRes2.isEmpty()) {
                // result 2
                page.addLine()
                        .addUnit(page.createUnit()
                                .setBitmap(Utils.generateQrCode(sRes2, 180, 180))
                                .setGravity(Gravity.CENTER));
                page.addLine()
                        .addUnit(page.createUnit()
                                .setBitmap(Utils.generateBarCode(sRes2, 370, 80))
                                .setGravity(Gravity.CENTER));
                page.addLine().addUnit(page.createUnit()
                        .setText(sRes2)
                        .setFontSize(FONT_SMALL)
                        .setWeight(5.0f)
                        .setGravity(Gravity.CENTER));
            }
        }
    }

    private void genImageOnEndReceipt(IPage page, Boolean isCustomerCopy) {
        Boolean isEnable = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_IMG_ON_END_RECEIPT);
        if (isEnable && isCustomerCopy) {
            //ADs
            page.addLine()
                    .addUnit(page.createUnit()
                            .setBitmap(Component.getImageFromInternalFile(Constants.DN_PARAM_IMG_ON_RECEIPT_FILE_PATH, Constants.DN_PARAM_IMG_ON_RECEIPT_FILE_NAME, "ads_on_slip_test4.jpg"))
                            .setGravity(Gravity.CENTER));
            page.addLine().addUnit(page.createUnit().setText(" "));
        }

    }

}
