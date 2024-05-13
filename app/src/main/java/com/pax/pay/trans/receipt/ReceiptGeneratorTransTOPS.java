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
//import android.graphics.fonts.FontStyle;
import android.view.Gravity;

import androidx.annotation.StringRes;

import com.pax.abl.utils.PanUtils;
import com.pax.appstore.DownloadManager;
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
import com.pax.pay.trans.model.TemplateLinePay;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.model.TransData.EnterMode;
import com.pax.pay.utils.Convert;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.EReceiptUtils;
import com.pax.pay.utils.TimeConverter;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import th.co.bkkps.utils.Log;
import th.co.bkkps.utils.PageToSlipFormat;

/**
 * receipt generator
 *
 * @author Steven.W
 */
public class ReceiptGeneratorTransTOPS implements IReceiptGenerator {

    float halfWidthSize = 0.6f;
    float fullSizeOneLine = 1.2f;
    float fullSizeDoubleLine = 0.8f;
    private int receiptNo = 0;
    private TransData transData;
    private boolean isRePrint = false;
    private int receiptMax = 2;
    private boolean isPrintPreview = false;
    private boolean isShowQRBarcode = false;
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
    public ReceiptGeneratorTransTOPS(TransData transData, int currentReceiptNo, int receiptMax, boolean isReprint) {
        this.transData = transData;
        this.receiptNo = currentReceiptNo;
        this.isRePrint = isReprint;
        this.receiptMax = receiptMax;
        this.onErmMode = false;
        PageToSlipFormat.getInstance().Reset();

    }

    public ReceiptGeneratorTransTOPS(TransData transData, int currentReceiptNo, int receiptMax, boolean isReprint, boolean isPrintPreview) {
        this.transData = transData;
        this.receiptNo = currentReceiptNo;
        this.isRePrint = isReprint;
        this.receiptMax = receiptMax;
        this.isPrintPreview = isPrintPreview;
        this.onErmMode = false;
        PageToSlipFormat.getInstance().Reset();
    }

    public ReceiptGeneratorTransTOPS(TransData transData, int currentReceiptNo, int receiptMax, boolean isReprint, boolean isPrintPreview, boolean onERMMode) {
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
        boolean isAmex = Constants.ACQ_AMEX.equals(transData.getAcquirer().getName());
        boolean isTxnSmallAmt = transData.isTxnSmallAmt();//EDCBBLAND-426 Support small amount
        boolean isPinVerifyMsg = transData.isPinVerifyMsg();
        boolean isOfflineTransSend = ETransType.OFFLINE_TRANS_SEND == transType || ETransType.OFFLINE_TRANS_SEND == transData.getOrigTransType();// offline via menu.
        boolean isRefund = ETransType.REFUND == transType || ETransType.REFUND == transData.getOrigTransType();

        // 生成第几张凭单不合法时， 默认0
        if (receiptNo > receiptMax) {
            receiptNo = 0;
        } else if (isTxnSmallAmt && receiptMax == 1) {
            receiptNo = receiptMax;
        }
        // the first copy print signature, if three copies, the second copy should print signature too
        /*if (receiptNo == 0 || ((receiptMax == 3) && receiptNo == 1)) {
            isPrintSign = true;
        }
        isPrintSign = isAmex ? true : isPrintSign;*/

//        if (isAmex || (/*(!isTxnSmallAmt || isPinVerifyMsg) && */(receiptNo == 0 || ((receiptMax == 3) && receiptNo == 1)))) {
//            isPrintSign = true;
//        }// Transaction is small amount, no need to print sign, print only customer copy format layout. (Except AMEX)
        if ((receiptNo == 0 || ((receiptMax == 3) && receiptNo == 1))) {
            isPrintSign = true;
        }

        IPage page = Device.generatePage(true);
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
                .setScaleX(fullSizeOneLine)
                .setGravity(Gravity.CENTER));

        /*Body*/
        if (isPrintSign) {
            //Sign copy, full info
            // host
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_host2))
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(": " + acquirer.getName())
                            .setGravity(Gravity.LEFT)
                            .setWeight(4.0f));
            // terminal ID/operator ID
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_terminal_code_short2))
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(": " + acquirer.getTerminalId())
                            .setWeight(4.0f)
                            .setGravity(Gravity.LEFT)
                    );
            // merchant ID
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_merchant_code_short2))
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(": " + acquirer.getMerchantId())
                            .setGravity(Gravity.LEFT)
                            .setWeight(4.0f));

            //STAN NO
            String stanno = Component.getPaddedNumber(transData.getStanNo(), 6);
            stanno = (transType == ETransType.SALE && transStatus == TransData.ETransStatus.VOIDED) ? Component.getPaddedNumber(transData.getVoidStanNo(), 6) : stanno;
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_stan_code2))
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(": " + stanno)
                            .setGravity(Gravity.LEFT)
                            .setWeight(4.0f));

            // batch NO
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_batch_num_short2))
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(": " + Component.getPaddedNumber(transData.getBatchNo(), 6))
                            .setGravity(Gravity.LEFT)
                            .setWeight(4.0f));

            //trace NO
            String traceno = Component.getPaddedNumber(transData.getTraceNo(), 6);
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_trans_no_short2))
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(": " + traceno)
                            .setGravity(Gravity.LEFT)
                            .setWeight(4.0f));

            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.receipt_one_line))
                    .setScaleX(fullSizeOneLine)
                    .setGravity(Gravity.CENTER));


            // card NO
            if (transType == ETransType.PREAUTH) {
                panMask = isAmex ? PanUtils.maskCardNo(transData.getPan(), Constants.PAN_MASK_PATTERN4) : transData.getPan();
            } else if (!transData.isOnlineTrans() && receiptNo == 0 && transType != ETransType.ADJUST) {
                panMask = PanUtils.maskCardNo(transData.getPan(), Constants.PAN_MASK_PATTERN3);
            } else {
                panMask = (isAmex && receiptNo > 0)
                        ? PanUtils.maskCardNo(transData.getPan(), Constants.PAN_MASK_PATTERN4)
                        : PanUtils.maskCardNo(transData.getPan(), transData.getIssuer().getPanMaskPattern());
            }

            EnterMode enterMode = transData.getEnterMode();
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
            //card type /expire date
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(transData.getIssuer().getName())
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_card_date_short) + temp1)
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.END));
            //card NO
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(temp)
                            .setFontSize(FONT_NORMAL_26));

            // date/time
            String formattedDate = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                    Constants.DATE_PATTERN_DISPLAY3);

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
            // REF. NO.
            if (!temp.isEmpty()) {
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_ref_no))
                                .setFontSize(FONT_NORMAL)
                                .setWeight(2.0f))
                        .addUnit(page.createUnit()
                                .setText(":")
                                .setFontSize(FONT_NORMAL)
                                .setWeight(0.5f))
                        .addUnit(page.createUnit()
                                .setText(temp)
                                .setFontSize(FONT_NORMAL)
                                .setGravity(Gravity.END)
                                .setWeight(3.0f));
            }

            //APP CODE
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_app_code))
                            .setFontSize(FONT_NORMAL)
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(":")
                            .setFontSize(FONT_NORMAL)
                            .setWeight(0.5f))
                    .addUnit(page.createUnit()
                            .setText(temp1)
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.END)
                            .setWeight(3.0f));


            if ((enterMode == EnterMode.INSERT || enterMode == EnterMode.CLSS || enterMode == EnterMode.SP200) &&
                    (transType == ETransType.SALE || transType == ETransType.PREAUTH ||
                            transType == ETransType.VOID || transType == ETransType.OFFLINE_TRANS_SEND ||
                            transType == ETransType.REFUND || transType == ETransType.ADJUST)) {
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
                page.addLine().addUnit(page.createUnit().setText(" "));
            }


            printDccInfo(page, 1, true);

            temp = transStatus.equals(TransData.ETransStatus.NORMAL) ? "" : " (" + transStatus.toString() + ")";
            String transName;
            if (isAmex && transData.getReferralStatus() != null && transData.getReferralStatus() != TransData.ReferralStatus.NORMAL) {
                transName = transType + Utils.getString(R.string.receipt_amex_call_issuer) + temp;

            } else if (transStatus.equals(TransData.ETransStatus.VOIDED) || transType == ETransType.VOID) {
                if (transType == ETransType.SALE_COMPLETION || transData.getOrigTransType() == ETransType.SALE_COMPLETION) {
                    transName = Utils.getString(R.string.receipt_void_sale_comp);
                } else if (transType == ETransType.PREAUTHORIZATION) {
                    transName = ETransType.PREAUTHORIZATION_CANCELLATION.getTransName();
                } else {
                    transName = (!isOfflineTransSend) ? Utils.getString(R.string.receipt_void_sale) : Utils.getString(R.string.receipt_void_offline);
                    transName = (!isRefund) ? transName : Utils.getString(R.string.receipt_void_refund);
                }

            } else if (transType == ETransType.ADJUST) {
                transName = transType.getTransName().toUpperCase();
            } else if (transType == ETransType.PREAUTHORIZATION || transType == ETransType.PREAUTHORIZATION_CANCELLATION) {
                transName = transType.getTransName();
            } else {
                transName = (!isOfflineTransSend && transStatus != TransData.ETransStatus.ADJUSTED) ? (transType + temp) : transType.getTransName().toUpperCase();
                if (transType == ETransType.SALE_COMPLETION) {
                    transName = (!isOfflineTransSend) ? (transType.getTransName() + temp) : transType.getTransName().toUpperCase();
                }
            }

            /*if (!isOfflineTransSend && transData.getOfflineSendState() != null) {
                if (transName.lastIndexOf("VOID") != -1) {
                    transName = transName.replace(" ", " " + Utils.getString(R.string.trans_offline).toUpperCase() + " ");
                } else {
                    transName = Utils.getString(R.string.trans_offline).toUpperCase() + " " + transName;
                }
            }*/

            transName += transData.isEcrProcess() ? " " + Utils.getString(R.string.receipt_pos_tran) : "";
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

            // amount
            long amount = Utils.parseLongSafe(transData.getAmount(), 0);
            if (Utils.parseLongSafe(transData.getAdjustedAmount(), 0) > 0) {
                amount = Utils.parseLongSafe(transData.getAdjustedAmount(), 0);
            }
            if (transType.isSymbolNegative() || transData.getTransState() == TransData.ETransStatus.VOIDED)
                amount = -amount;
            temp = CurrencyConverter.convert(amount, transData.getCurrency());

            if (transType == ETransType.ADJUST || transStatus == TransData.ETransStatus.ADJUSTED) {
                printTipAdjustInfo(page, amount);
            }

            if (temp.length() > 16) {
                page.addLine()
                        .adjustTopSpace(1)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_amount_total))
                                .setFontSize(FONT_BIG_28)
                                .setWeight(3.0f))
                        .addUnit(page.createUnit()
                                .setText(temp)
                                .setFontSize(FONT_BIG_28)
                                .setGravity(Gravity.END)
                                .setWeight(10.0f));
            } else {
                page.addLine()
                        .adjustTopSpace(1)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_amount_total))
                                .setFontSize(FONT_BIG)
                                .setWeight(4.0f))
                        .addUnit(page.createUnit()
                                .setText(temp)
                                .setFontSize(FONT_BIG)
                                .setGravity(Gravity.END)
                                .setWeight(9.0f));
            }
            page.addLine().addUnit(page.createUnit().setText(" "));

            printInstantDiscountInfo(page, acquirer);

            page.addLine().addUnit(page.createUnit()
                    .setText("==================")
                    .setScaleX(fullSizeDoubleLine)
                    .setGravity(Gravity.END));

            // REF 1 & 2
            printEdcRef1Ref2(page, transData);

            /*if ((enterMode == EnterMode.INSERT || enterMode == EnterMode.CLSS || enterMode == EnterMode.SP200) &&
                    (transType == ETransType.SALE || transType == ETransType.PREAUTH ||
                            transType == ETransType.VOID || transType == ETransType.OFFLINE_TRANS_SEND)) {
                //APP:AID / TC
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(transData.getEmvAppLabel() +":" + transData.getAid())
                                .setFontSize(FONT_SMALL)
                                .setScaleX(0.6f));
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText("TC: " + transData.getTc())
                                .setFontSize(FONT_SMALL)
                                .setScaleX(0.6f));

            }else{
                //APP:AID / TC
                if(transData.getTc() != null) {
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText("TC: " + transData.getTc())
                                    .setFontSize(FONT_SMALL)
                                    .setScaleX(0.6f)
                                    .setGravity(Gravity.END));
                }
            }*/


            page.addLine().addUnit(page.createUnit().setText(" "));


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
            boolean issettleIncludeESignature = ((transData.getSettlementErmReprintMode() == TransData.SettleErmReprintMode.HideReprint_ShowSignature
                    || transData.getSettlementErmReprintMode() == TransData.SettleErmReprintMode.ShowReprint_ShowSignature) ? true : false);

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
                } else {

                    if ((!isTxnSmallAmt) || issettleIncludeESignature) {
                        page.addLine().addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_sign))
                                .setFontSize(FONT_SMALL)
                                .setGravity(Gravity.CENTER));
                        Bitmap bitmap = null;

                        bitmap = loadSignature(transData);
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

            //STAN NO
            String stanno = Component.getPaddedNumber(transData.getStanNo(), 6);
            stanno = (transType == ETransType.SALE && transStatus == TransData.ETransStatus.VOIDED) ? Component.getPaddedNumber(transData.getVoidStanNo(), 6) : stanno;
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
                if (transData.getTransType() == ETransType.SALE_COMPLETION || transData.getOrigTransType() == ETransType.SALE_COMPLETION) {
                    temp = Utils.getString(R.string.receipt_void_sale_comp);
                } else if (transType == ETransType.PREAUTHORIZATION) {
                    temp = ETransType.PREAUTHORIZATION_CANCELLATION.getTransName();
                } else {
                    temp = (!isOfflineTransSend && !isRefund) ? Utils.getString(R.string.receipt_void_sale) : "";
                }
            } else {
                temp = transStatus == TransData.ETransStatus.NORMAL || transStatus == TransData.ETransStatus.ADJUSTED  ? transType.getTransName() : transType.getTransName() + " (" + transStatus.toString() + ")";
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
            if (Utils.parseLongSafe(transData.getAdjustedAmount(), 0) > 0) {
                amount = Utils.parseLongSafe(transData.getAdjustedAmount(), 0);
            }
            if (transType.isSymbolNegative() || transData.getTransState() == TransData.ETransStatus.VOIDED)
                amount = -amount;
            temp = CurrencyConverter.convert(amount, transData.getCurrency());

            if (transType == ETransType.ADJUST || transStatus == TransData.ETransStatus.ADJUSTED) {
                printTipAdjustInfo(page, amount);
            }

            if (temp.length() > 16) {
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText((receiptNo == 1) ? "TOTAL" : "BASE")
                                .setFontSize(FONT_BIG_28)
                                .setWeight(3.0f))
                        .addUnit(page.createUnit()
                                .setText(temp)
                                .setFontSize(FONT_BIG_28)
                                .setGravity(Gravity.END)
                                .setWeight(10.0f));
            } else {
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText((receiptNo == 1) ? "TOTAL" : "BASE")
                                .setFontSize(FONT_BIG)
                                .setWeight(4.0f))
                        .addUnit(page.createUnit()
                                .setText(temp)
                                .setFontSize(FONT_BIG)
                                .setGravity(Gravity.END)
                                .setWeight(9.0f));
            }

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

        /* CardHolder Name */
//        temp = transData.getTrack1();
//        if(temp != null){
//            page.addLine().addUnit(page.createUnit()
//                    .setText(temp.trim())
//                    .setFontSize(FONT_NORMAL_26)
//                    .setGravity(Gravity.CENTER));
//        }
        String[] CardHolderName;
        if (transData.getTrack1() != null) {
            try {
                CardHolderName = EReceiptUtils.stringSplitter(transData.getTrack1(), EReceiptUtils.MAX23_CHAR_PER_LINE);
                if (CardHolderName.length == 1 && CardHolderName[0] != null) {
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
                Log.d(TAG, e.getMessage() + " : Error during reformat [CardHolderName]");
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


        if (acquirer.isEnableTle() && (offlineSendState == null || TransData.OfflineStatus.OFFLINE_SENT == offlineSendState)) {
            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.receipt_trusted_trans))
                    .setFontSize(FONT_SMALL)
                    .setGravity(Gravity.CENTER));
        }


        /* Generate QR code and Barcode */
        genQRBarCode(page, transData.getAmount(), panMask);
        /* End Generate QR code and Barcode */


        if (receiptMax == 3) {
            if (receiptNo == 0) {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_acquire_hyphen))
                                .setGravity(Gravity.CENTER));
            } else if (receiptNo == 1) {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_merchant_hyphen))
                                .setGravity(Gravity.CENTER));
                //EDR data
                printEcrPOSInfo(page);
                /* Generate QR code COD */
                genQRcodeCOD(page, transData, panMask, false);
                /* Generate Marketing Image */
                genImageOnEndReceipt(page, false);
            } else {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_cust_hyphen))
                                .setGravity(Gravity.CENTER));
                //EDR data
                printEcrPOSInfo(page);
                genImageOnEndReceipt(page, true);
            }
        } else {
            if (receiptNo == 0) {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_merchant_hyphen))
                                .setGravity(Gravity.CENTER));
                //EDR data
                printEcrPOSInfo(page);
                /* Generate QR code COD */
                genQRcodeCOD(page, transData, panMask, false);
                /* Generate Marketing Image */
                genImageOnEndReceipt(page, false);
            } else {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_cust_hyphen))
                                .setGravity(Gravity.CENTER));
                //EDR data
                printEcrPOSInfo(page);
                genImageOnEndReceipt(page, true);
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

        if (onErmMode) {
            PageToSlipFormat.isSettleMode = false;
            PageToSlipFormat.getInstance().Append(page);
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

        //STAN NO
        String stanno = Component.getPaddedNumber(transData.getStanNo(), 6);
        stanno = (transType == ETransType.SALE && transData.getTransState() == TransData.ETransStatus.VOIDED) ? Component.getPaddedNumber(transData.getVoidStanNo(), 6) : stanno;
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_stan_code3) + stanno)
                        .setFontSize(FONT_SMALL_18)
                        .setGravity(Gravity.START)
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
                                .setText(Utils.getString(R.string.receipt_stub_acquire_hyphen))
                                .setGravity(Gravity.CENTER));
            } else if (receiptNo == 1) {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_merchant_hyphen))
                                .setGravity(Gravity.CENTER));
            } else {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_cust_hyphen))
                                .setGravity(Gravity.CENTER));
            }
        } else {
            if (receiptNo == 0) {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_merchant_hyphen))
                                .setGravity(Gravity.CENTER));
            } else {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_cust_hyphen))
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

        //STAN NO
        String stanno = Component.getPaddedNumber(transData.getStanNo(), 6);
        stanno = (transType == ETransType.SALE && transData.getTransState() == TransData.ETransStatus.VOIDED) ? Component.getPaddedNumber(transData.getVoidStanNo(), 6) : stanno;
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_stan_code3) + stanno)
                        .setFontSize(FONT_SMALL_18)
                        .setGravity(Gravity.START)
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
                                .setText(Utils.getString(R.string.receipt_stub_acquire_hyphen))
                                .setGravity(Gravity.CENTER));
            } else if (receiptNo == 1) {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_merchant_hyphen))
                                .setGravity(Gravity.CENTER));
            } else {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_cust_hyphen))
                                .setGravity(Gravity.CENTER));
            }
        } else {
            if (receiptNo == 0) {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_merchant_hyphen))
                                .setGravity(Gravity.CENTER));
            } else {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_cust_hyphen))
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
        /*
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
         */

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
                                .setText(Utils.getString(R.string.receipt_stub_acquire_hyphen))
                                .setGravity(Gravity.CENTER));
            } else if (receiptNo == 1) {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_merchant_hyphen))
                                .setGravity(Gravity.CENTER));
            } else {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_user_hyphen))
                                .setGravity(Gravity.CENTER));
            }
        } else {
            if (receiptNo == 0) {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_merchant_hyphen))
                                .setGravity(Gravity.CENTER));
            } else {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_cust_hyphen))
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

    private void ComposeTemplateLinePay(TransData transData, int numTailSlip, IPage page) {
        String[] slipInfo = transData.getInfoSlipLinePay();
        TemplateLinePay templateLinePay = FinancialApplication.getTemplateLinePayDbHelper().findTemplateData(transData.getTemplateId());

        if (templateLinePay != null && templateLinePay.getTemplateCorrect()) {

            // update date last usage.
            Date dateNow = new Date();
            templateLinePay.setLastUsageTimestmp(dateNow);
            FinancialApplication.getTemplateLinePayDbHelper().updateTemplate(templateLinePay);

            String[] template = templateLinePay.getTemplateInfo();
            String[] ArrTailSlip = templateLinePay.getTailSlip();
            String tailSlip = ArrTailSlip[numTailSlip];
            if (ArrTailSlip[numTailSlip] == null) {
                tailSlip = ArrTailSlip[0];
            }

            for (int i = 0; i < slipInfo.length; i++) {

                for (int ii = 0; ii < template.length; ii++) {
                    int index = template[ii].indexOf("{}");
                    int indexTail = template[ii].indexOf("TailSlip");
                    if (index != -1) {
                        template[ii] = template[ii].replaceFirst("\\{.*?\\}", slipInfo[i]);
                        break;
                    }
                    if (indexTail != -1) {
                        template[ii] = template[ii].replace("TailSlip", tailSlip);
                    }
                }
            }

            if (templateLinePay.getDefaultCharMode().equals("FE")) {
                for (int i = 0; i < template.length; i++) {
                    String line = template[i];
                    String strSize = line.substring(0, 2);
                    if (strSize.equals("FF")) {
                        page.addLine()
                                .addUnit(page.createUnit()
                                        .setText(line.substring(2))
                                        .setFontSize(FONT_NORMAL)
                                );
                    } else {
                        page.addLine()
                                .addUnit(page.createUnit()
                                        .setText(line)
                                        .setFontSize(FONT_SMALL)
                                );
                    }
                }
            } else if (templateLinePay.getDefaultCharMode().equals("FF")) {
                for (int i = 0; i < template.length; i++) {
                    String line = template[i];
                    String strSize = line.substring(0, 2);
                    if (strSize.equals("FE")) {
                        page.addLine()
                                .addUnit(page.createUnit()
                                        .setText(line.substring(2))
                                        .setFontSize(FONT_SMALL_18)
                                );
                    } else {
                        page.addLine()
                                .addUnit(page.createUnit()
                                        .setText(line)
                                        .setFontSize(FONT_SMALL)
                                );
                    }
                }
            }
        } else {
            // incorrect template or no template in data base
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_template_id) + transData.getTemplateId())
                            .setFontSize(FONT_SMALL)
                    );

            for (int i = 0; i < slipInfo.length; i++) {
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText("* " + slipInfo[i].trim())
                                .setFontSize(FONT_NORMAL)
                        );
            }
        }
    }

    public Bitmap generateWalletVerifyPaySlipReceiptBitmap() {
        if (receiptNo > receiptMax) {
            receiptNo = 0;
        }

        IPage page = Device.generatePage(true);
        ETransType transType = transData.getTransType();


        SysParam sysParam = FinancialApplication.getSysParam();
        Acquirer acquirer = transData.getAcquirer();

        String temp = null;

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
                .setScaleX(fullSizeOneLine)
                .setGravity(Gravity.CENTER));


        // terminal ID/operator ID
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_terminal_code_short2))
                        .setFontSize(FONT_SMALL))
                .addUnit(page.createUnit()
                        .setText(": " + acquirer.getTerminalId())
                        .setFontSize(FONT_SMALL)
                        .setWeight(4.0f)
                        .setGravity(Gravity.LEFT)
                );
        // merchant ID
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_merchant_code_short2))
                        .setFontSize(FONT_SMALL))
                .addUnit(page.createUnit()
                        .setText(": " + acquirer.getMerchantId())
                        .setFontSize(FONT_SMALL)
                        .setGravity(Gravity.LEFT)
                        .setWeight(4.0f));

        //trace NO
        String walletTrace = Component.getPaddedNumber(transData.getTraceNo(), 6);
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_trans_no_short2) + " : " + walletTrace)
                        .setFontSize(FONT_SMALL)
                        .setWeight(2.0f))
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_batch_num_short2) + " : " + Utils.getStringPadding(transData.getAcquirer().getCurrBatchNo(),6,"0", Convert.EPaddingPosition.PADDING_LEFT))
                        .setGravity(Gravity.END)
                        .setFontSize(FONT_SMALL)
                        .setWeight(2.0f));

        //Host name
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_host2) + " : " + acquirer.getName())
                        .setFontSize(FONT_SMALL)
                        .setWeight(2.5f))
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_host_nii) + " : " + acquirer.getNii())
                        .setGravity(Gravity.END)
                        .setFontSize(FONT_SMALL)
                        .setWeight(1.5f));

        String printDate = Device.getTime("dd/MM/yy");
        String printTime = Device.getTime("HH:mm:ss");
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_date_short) + ": " + printDate)
                        .setFontSize(FONT_SMALL)
                        .setWeight(2.0f))
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_time_short) + ": " + printTime)
                        .setGravity(Gravity.END)
                        .setFontSize(FONT_SMALL)
                        .setWeight(2.0f));


        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setScaleX(fullSizeOneLine)
                .setGravity(Gravity.CENTER));

        // TransType
        page.addLine()
                .adjustTopSpace(5)
                .addUnit(page.createUnit()
                        .setText(transType.getTransName())
                        .setFontSize(FONT_BIG_28)
                        //.setTextStyle(FontStyle.FONT_WEIGHT_BOLD)); // FontStyle class is only support API 29, using 700 instead first.
                        .setTextStyle(700));


        String payTimeSplitter[] = (transData.getPayTime()==null) ? null : transData.getPayTime().split(" ");
        String paytimeDate = (payTimeSplitter!=null) ? payTimeSplitter[0].replace("/","") : "";
        String paytimeTime = (payTimeSplitter!=null) ? payTimeSplitter[1].replace(":","") : "";
        String paytimeDateStr = (paytimeDate!=null) ? paytimeDate.substring(6,8) + "/" + paytimeDate.substring(4,6)+ "/" + paytimeDate.substring(0,4) : "";
        String paytimeTimeStr = (paytimeTime!=null) ?  paytimeTime.substring(0,2) + ":" + paytimeTime.substring(2,4) + ":" + paytimeTime.substring(4,6) : "";

        page.addLine()
                .adjustTopSpace(5)
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_payment_date) )
                        .setFontSize(FONT_SMALL)
                        .setWeight(2.0f))
                .addUnit(page.createUnit()
                        .setText(": " + paytimeDateStr + " " + paytimeTimeStr)
                        .setFontSize(FONT_SMALL)
                        .setWeight(4.0f));


        String bankCodeVal = (transData.getWalletBankCode() == null) ? "-" : transData.getWalletBankCode();
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_bank_code) )
                        .setFontSize(FONT_SMALL)
                        .setWeight(2.0f))
                .addUnit(page.createUnit()
                        .setText(": " + bankCodeVal)
                        .setFontSize(FONT_SMALL)
                        .setWeight(4.0f));


        String transNo = (transData.getTxnNo()==null) ? "-" : transData.getTxnNo().trim() ;
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_reference) + " :" )
                        .setFontSize(FONT_SMALL));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(transNo)
                        .setGravity(Gravity.END)
                        .setFontSize(FONT_SMALL));

        String transID = (transData.getTxnID()==null) ? "-" : transData.getTxnID().trim() ;
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_transaction_id) + ":" )
                        .setFontSize(FONT_SMALL));

        page.addLine()
                .addUnit(page.createUnit()
                        .setText(transID)
                        .setGravity(Gravity.END)
                        .setFontSize(FONT_SMALL));


        // amount
        long amount = Utils.parseLongSafe(transData.getAmount(), 0);
        if (transType.isSymbolNegative())
            amount = -amount;
        temp = CurrencyConverter.convert(amount, transData.getCurrency());
        page.addLine()
                .adjustTopSpace(5)
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_amount) + ":")
                        .setFontSize(FONT_BIG_28)
                        .setWeight(2.0f))
                .addUnit(page.createUnit()
                        .setText(temp)
                        .setGravity(Gravity.END)
                        .setFontSize(FONT_BIG_28)
                        .setWeight(4.0f));

        page.addLine()
                .adjustTopSpace(5)
                .addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.receipt_one_line))
                    .setScaleX(fullSizeOneLine)
                    .setGravity(Gravity.CENTER));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_please_recheck_the_transaction))
                .setFontSize(FONT_SMALL)
                .setScaleX(halfWidthSize)
                .setGravity(Gravity.CENTER));

        page.addLine().addUnit(page.createUnit()
                .setText(" ")
                .setGravity(Gravity.CENTER));


        try {
            String Disclaimer[] = EReceiptUtils.stringSplitter(Utils.getString(R.string.receipt_verify), 34);
            for (int dcmrIndex = 0; dcmrIndex <= Disclaimer.length - 1; dcmrIndex++) {
                page.addLine().addUnit(page.createUnit()
                        .setText(Disclaimer[dcmrIndex])
                        .setFontSize(FONT_SMALL)
                        .setGravity(Gravity.CENTER));
            }
        } catch (Exception ex) {
            ex.getMessage();
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


        if (receiptMax == 3) {
            if (receiptNo == 0) {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_acquire_hyphen))
                                .setGravity(Gravity.CENTER));
            } else if (receiptNo == 1) {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_merchant_hyphen))
                                .setGravity(Gravity.CENTER));
                genImageOnEndReceipt(page, false);
            } else {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_user_hyphen))
                                .setGravity(Gravity.CENTER));
                genImageOnEndReceipt(page, true);
            }
        } else {
            if (receiptNo == 0) {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_merchant_hyphen))
                                .setGravity(Gravity.CENTER));
                genImageOnEndReceipt(page, false);
            } else {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_cust_hyphen))
                                .setGravity(Gravity.CENTER));
                genImageOnEndReceipt(page, true);
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

        if (onErmMode) {
            PageToSlipFormat.isSettleMode = false;
            PageToSlipFormat.getInstance().Append(page);
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

        IPage page = Device.generatePage(true);
        //page.setTypefaceObj(Typeface.createFromAsset(FinancialApplication.getApp().getAssets(), Constants.FONT_NAME_MONO));
        // transaction type
        ETransType transType = transData.getTransType();

        SysParam sysParam = FinancialApplication.getSysParam();
        Acquirer acquirer = transData.getAcquirer();

        String temp = null;

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
                .setScaleX(fullSizeOneLine)
                .setGravity(Gravity.CENTER));


        /*Body*/
        // host
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_host2))
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(": " + acquirer.getName())
                        .setGravity(Gravity.LEFT)
                        .setWeight(4.0f));
        // terminal ID/operator ID
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_terminal_code_short2))
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(": " + acquirer.getTerminalId())
                        .setWeight(4.0f)
                        .setGravity(Gravity.LEFT)
                );
        // merchant ID
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_merchant_code_short2))
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(": " + acquirer.getMerchantId())
                        .setGravity(Gravity.LEFT)
                        .setWeight(4.0f));


        //STAN NO
        String walletStan;
        TransData.ETransStatus transState = transData.getTransState();
//        if((transType == ETransType.QR_VOID_KPLUS && transData.getTransState() == TransData.ETransStatus.VOIDED)
//                || (transType == ETransType.QR_VOID_ALIPAY && transData.getTransState() == TransData.ETransStatus.VOIDED)
//                || (transType == ETransType.QR_VOID_WECHAT && transData.getTransState() == TransData.ETransStatus.VOIDED)){
        if (transState == TransData.ETransStatus.VOIDED) {
            walletStan = Component.getPaddedNumber(transData.getVoidStanNo(), 6);
        } else {
            walletStan = Component.getPaddedNumber(transData.getStanNo(), 6);
        }
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_stan_code2))
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(": " + walletStan)
                        .setGravity(Gravity.LEFT)
                        .setWeight(4.0f));

        // batch NO
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_batch_num_short2))
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(": " + Component.getPaddedNumber(transData.getBatchNo(), 6))
                        .setGravity(Gravity.LEFT)
                        .setWeight(4.0f));

        //trace NO
        String walletTrace = Component.getPaddedNumber(transData.getTraceNo(), 6);
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_trans_no_short2))
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(": " + walletTrace)
                        .setGravity(Gravity.LEFT)
                        .setWeight(4.0f));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setScaleX(fullSizeOneLine)
                .setGravity(Gravity.CENTER));


        // date/time
        String formattedDate = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.DATE_PATTERN_DISPLAY);

        String formattedTime = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.TIME_PATTERN_DISPLAY4);

        page.addLine()
                .addUnit(page.createUnit()
                        .setText(formattedDate)
                        .setFontSize(FONT_NORMAL_22))
                .addUnit(page.createUnit()
                        .setText(formattedTime)
                        .setFontSize(FONT_NORMAL_22)
                        .setGravity(Gravity.END));

        // REF. NO.
        /*
        String refNo = transState == TransData.ETransStatus.VOIDED ? transData.getOrigRefNo() : transData.getRefNo();
        if (transData.isOnlineTrans()) {
            if (refNo != null && !refNo.isEmpty()) {
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_ref_no))
                                .setFontSize(FONT_NORMAL_22)
                                .setWeight(1.5f))
                        .addUnit(page.createUnit()
                                .setText(":")
                                .setFontSize(FONT_NORMAL_22)
                                .setWeight(0.5f))
                        .addUnit(page.createUnit()
                                .setText(refNo)
                                .setFontSize(FONT_NORMAL_22)
                                .setGravity(Gravity.END)
                                .setWeight(3.0f));
            }
        }

         */
        //APP CODE
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_app_code))
                        .setFontSize(FONT_NORMAL_22)
                        .setWeight(1.5f))
                .addUnit(page.createUnit()
                        .setText(":")
                        .setFontSize(FONT_NORMAL_22)
                        .setWeight(0.5f))
                .addUnit(page.createUnit()
                        .setText(transData.getAuthCode())
                        .setFontSize(FONT_NORMAL_22)
                        .setGravity(Gravity.END)
                        .setWeight(3.0f));


        //WALLET SALE / VOID
        //String transName = transData.getTransType() != null ? transData.getTransType().getTransName() : "";
        //String transNameVoid = transName.contains(" ") ? (transName.substring(0, transName.indexOf(" ") + 1) + "VOID").toUpperCase() : transName.toUpperCase();
//        String typeWallet = transState == TransData.ETransStatus.VOIDED ? Utils.getString(R.string.trans_void).toUpperCase() : Utils.getString(R.string.trans_sale).toUpperCase();
        String typeWallet = transState == TransData.ETransStatus.VOIDED || transType == ETransType.QR_VOID_KPLUS || transType == ETransType.QR_VOID_ALIPAY
                || transType == ETransType.QR_VOID_WECHAT || transType == ETransType.QR_VOID_CREDIT ?
                Utils.getString(R.string.trans_void).toUpperCase() : Utils.getString(R.string.trans_sale).toUpperCase();
        typeWallet += transData.isEcrProcess() ? " " + Utils.getString(R.string.receipt_pos_tran) : "";
        page.addLine()
                .adjustTopSpace(1)
                .addUnit(page.createUnit()
                        .setText(typeWallet)
                        .setFontSize(FONT_BIG)
                );

        String acquirerName = acquirer.getName();
        //TRAN ID
        if (Constants.ACQ_KPLUS.equals(acquirerName) || Constants.ACQ_QR_CREDIT.equals(acquirerName)) {
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_tran_id))
                            .setFontSize(FONT_NORMAL_22)
                            .setWeight(1.3f))
                    .addUnit(page.createUnit()
                            .setText(":")
                            .setFontSize(FONT_NORMAL_22)
                            .setWeight(0.2f))
                    .addUnit(page.createUnit()
                            .setText(transData.getTxnNo() != null ? transData.getTxnNo().trim() : "")
                            .setFontSize(FONT_NORMAL_22)
                            .setWeight(3.0f)
                            .setGravity(Gravity.END));
        } else {
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_tran_id))
                            .setFontSize(FONT_NORMAL_22)
                            .setWeight(1.5f))
                    .addUnit(page.createUnit()
                            .setText(":")
                            .setFontSize(FONT_NORMAL_22)
                            .setWeight(3.5f));
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(transData.getWalletPartnerID() != null ? transData.getWalletPartnerID().trim() : "")
                            .setFontSize(FONT_SMALL));
        }

        //CHANNEL
        switch (acquirerName) {
            case Constants.ACQ_KPLUS:
                // QRTAG31 implemented
                temp =  (transData.getQrSourceOfFund() != null) ? transData.getQrSourceOfFund() : "-" ;
                break;
            case Constants.ACQ_ALIPAY:
            case Constants.ACQ_ALIPAY_B_SCAN_C:
                temp = Utils.getString(R.string.receipt_alipay);
                break;
            case Constants.ACQ_WECHAT:
            case Constants.ACQ_WECHAT_B_SCAN_C:
                temp = Utils.getString(R.string.receipt_wechat);
                break;
            case Constants.ACQ_QR_CREDIT:
                temp = transData.getMerchantInfo() != null ? transData.getMerchantInfo().trim() : "";
                break;
        }


        if (Constants.ACQ_QR_CREDIT.equals(acquirerName)) {
            page.addLine()
                    .adjustTopSpace(5)
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_channel))
                            .setFontSize(FONT_NORMAL)
                            .setWeight(1.3f));
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("QR CREDIT")
                            .setFontSize(FONT_NORMAL)
                            .setWeight(3.5f))
                    .addUnit(page.createUnit()
                            .setText(temp)
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.END)
                            .setWeight(3.0f));
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_card_no))
                            .setFontSize(FONT_SMALL)
                            .setWeight(1.3f))
                    .addUnit(page.createUnit()
                            .setText(transData.getBuyerLoginID() != null ? transData.getBuyerLoginID().trim() : "")
                            .setFontSize(FONT_SMALL)
                            .setWeight(3.0f)
                            .setGravity(Gravity.END));
        } else {
            page.addLine()
                    .adjustTopSpace(5)
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_channel))
                            .setFontSize(FONT_NORMAL)
                            .setWeight(1.3f))
                    .addUnit(page.createUnit()
                            .setText(":")
                            .setFontSize(FONT_NORMAL_22)
                            .setWeight(0.2f))
                    .addUnit(page.createUnit()
                            .setText(temp)
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.END)
                            .setWeight(3.0f));
        }

        if (Constants.ACQ_ALIPAY.equals(acquirerName)||Constants.ACQ_ALIPAY_B_SCAN_C.equals(acquirerName)) {
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_account))
                            .setFontSize(FONT_NORMAL)
                            .setWeight(1.3f))
                    .addUnit(page.createUnit()
                            .setText(":")
                            .setFontSize(FONT_NORMAL)
                            .setWeight(0.2f))
                    .addUnit(page.createUnit()
                            .setText((transData.getBuyerLoginID() != null ? transData.getBuyerLoginID().trim() : ""))
                            .setFontSize(FONT_NORMAL)
                            .setWeight(3.0f));
        }


        // amount
        long amount = Utils.parseLongSafe(transData.getAmount(), 0);
        if (transType.isSymbolNegative())
            amount = -amount;
        if (transState == TransData.ETransStatus.VOIDED)
            amount = -amount;
        temp = CurrencyConverter.convert(amount, transData.getCurrency());
        //TOTAL //AMOUNT
        if (temp.length() > 16) {
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("BASE")
                            .setFontSize(FONT_BIG_28)
                            .setWeight(3.0f))
                    .addUnit(page.createUnit()
                            .setText(temp)
                            .setFontSize(FONT_BIG_28)
                            .setGravity(Gravity.END)
                            .setWeight(10.0f));
        } else {
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("BASE")
                            .setFontSize(FONT_BIG)
                    )
                    .addUnit(page.createUnit()
                            .setText(temp)
                            .setFontSize(FONT_BIG)
                            .setGravity(Gravity.END)
                    );
        }

        /*Generate QR on Receipt for AliPay and WeChat*/
        genQRBarCodeAlipayWallet(page, transData, acquirer);
        /*End Generate QR on Receipt for AliPay and WeChat*/

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

        //Pay Time
        /*if (transData.getPayTime() != null && !Constants.ACQ_KPLUS.equals(acquirerName)) {
            temp = TimeConverter.convert(transData.getPayTime().trim(), Constants.TIME_PATTERN_TRANS, Constants.TIME_PATTERN_DISPLAY);
        } else {
            temp = transState == TransData.ETransStatus.VOIDED ?
                    TimeConverter.convert(transData.getOrigDateTime(), Constants.TIME_PATTERN_TRANS, Constants.TIME_PATTERN_DISPLAY) :
                    TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS, Constants.TIME_PATTERN_DISPLAY);
        }*/
        if (transData.getPayTime() != null && !transData.getPayTime().isEmpty() && !Constants.ACQ_KPLUS.equals(acquirerName)) {
            temp = TimeConverter.convert(transData.getPayTime().trim(), Constants.TIME_PATTERN_TRANS, Constants.TIME_PATTERN_DISPLAY);
            if (temp.compareTo(transData.getPayTime().trim()) == 0) {
                temp = transState == TransData.ETransStatus.VOIDED || transType == ETransType.QR_VOID_KPLUS || transType == ETransType.QR_VOID_ALIPAY
                        || transType == ETransType.QR_VOID_WECHAT || transType == ETransType.QR_VOID_CREDIT ?
                        TimeConverter.convert(transData.getOrigDateTime(), Constants.TIME_PATTERN_TRANS, Constants.TIME_PATTERN_DISPLAY) :
                        TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS, Constants.TIME_PATTERN_DISPLAY);
            }
        } else {
            temp = transState == TransData.ETransStatus.VOIDED || transType == ETransType.QR_VOID_KPLUS || transType == ETransType.QR_VOID_ALIPAY
                    || transType == ETransType.QR_VOID_WECHAT || transType == ETransType.QR_VOID_CREDIT ?
                    TimeConverter.convert(transData.getOrigDateTime(), Constants.TIME_PATTERN_TRANS, Constants.TIME_PATTERN_DISPLAY) :
                    TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS, Constants.TIME_PATTERN_DISPLAY);
        }
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_pay_time))
                        .setFontSize(FONT_NORMAL_22)
                        .setWeight(1.3f))
                .addUnit(page.createUnit()
                        .setText(":")
                        .setFontSize(FONT_NORMAL_22)
                        .setWeight(0.2f))
                .addUnit(page.createUnit()
                        .setText(temp)
                        .setFontSize(FONT_NORMAL_22)
                        .setWeight(3.0f)
                        .setGravity(Gravity.END));

        page.addLine().addUnit(page.createUnit().setText(" "));
        // REF 1 & 2
        printEdcRef1Ref2(page, transData);

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setScaleX(fullSizeOneLine)
                .setGravity(Gravity.CENTER));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_no_sig_required))
                .setFontSize(FONT_NORMAL_22)
                .setGravity(Gravity.CENTER));

//        page.addLine()
//                .adjustTopSpace(5)
//                .addUnit(page.createUnit()
//                .setText(Utils.getString(R.string.receipt_verify))
//                .setFontSize(FONT_SMALL)
//                .setGravity(Gravity.CENTER));

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


        if (receiptMax == 3) {
            if (receiptNo == 0) {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_acquire_hyphen))
                                .setGravity(Gravity.CENTER));
            } else if (receiptNo == 1) {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_merchant_hyphen))
                                .setGravity(Gravity.CENTER));
                genImageOnEndReceipt(page, false);
            } else {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_user_hyphen))
                                .setGravity(Gravity.CENTER));
                genImageOnEndReceipt(page, true);
            }
        } else {
            if (receiptNo == 0) {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_merchant_hyphen))
                                .setGravity(Gravity.CENTER));
                genImageOnEndReceipt(page, false);
            } else {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_cust_hyphen))
                                .setGravity(Gravity.CENTER));
                genImageOnEndReceipt(page, true);
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

        if (onErmMode) {
            PageToSlipFormat.isSettleMode = false;
            PageToSlipFormat.getInstance().Append(page);
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

        IPage page = Device.generatePage(true);
        //page.setTypefaceObj(Typeface.createFromAsset(FinancialApplication.getApp().getAssets(), Constants.FONT_NAME_MONO));
        // transaction type
        ETransType transType = transData.getTransType();

        SysParam sysParam = FinancialApplication.getSysParam();
        Acquirer acquirer = transData.getAcquirer();

        String temp = null;

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
                .setScaleX(fullSizeOneLine)
                .setGravity(Gravity.CENTER));


        /*Body*/
        // host
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_host2))
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(": " + acquirer.getName())
                        .setGravity(Gravity.LEFT)
                        .setWeight(4.0f));
        // terminal ID/operator ID
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_terminal_code_short2))
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(": " + acquirer.getTerminalId())
                        .setWeight(4.0f)
                        .setGravity(Gravity.LEFT)
                );
        // merchant ID
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_merchant_code_short2))
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(": " + acquirer.getMerchantId())
                        .setGravity(Gravity.LEFT)
                        .setWeight(4.0f));


        //STAN NO
        String walletStan;
        TransData.ETransStatus transState = transData.getTransState();
        if (transState == TransData.ETransStatus.VOIDED) {
            walletStan = Component.getPaddedNumber(transData.getVoidStanNo(), 6);
        } else {
            walletStan = Component.getPaddedNumber(transData.getStanNo(), 6);
        }
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_stan_code2))
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(": " + walletStan)
                        .setGravity(Gravity.LEFT)
                        .setWeight(4.0f));

        // batch NO
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_batch_num_short2))
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(": " + Component.getPaddedNumber(transData.getBatchNo(), 6))
                        .setGravity(Gravity.LEFT)
                        .setWeight(4.0f));

        //trace NO
        String walletTrace = Component.getPaddedNumber(transData.getTraceNo(), 6);
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_trans_no_short2))
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(": " + walletTrace)
                        .setGravity(Gravity.LEFT)
                        .setWeight(4.0f));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setScaleX(fullSizeOneLine)
                .setGravity(Gravity.CENTER));


        // date/time
        String formattedDate = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.DATE_PATTERN_DISPLAY);

        String formattedTime = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.TIME_PATTERN_DISPLAY4);

        page.addLine()
                .addUnit(page.createUnit()
                        .setText(formattedDate)
                        .setFontSize(FONT_NORMAL_22))
                .addUnit(page.createUnit()
                        .setText(formattedTime)
                        .setFontSize(FONT_NORMAL_22)
                        .setGravity(Gravity.END));

        //APP CODE
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_app_code))
                        .setFontSize(FONT_NORMAL_22)
                        .setWeight(1.5f))
                .addUnit(page.createUnit()
                        .setText(":")
                        .setFontSize(FONT_NORMAL_22)
                        .setWeight(0.5f))
                .addUnit(page.createUnit()
                        .setText(transData.getAuthCode())
                        .setFontSize(FONT_NORMAL_22)
                        .setGravity(Gravity.END)
                        .setWeight(3.0f));


        String typeWallet = transType == ETransType.QR_MYPROMPT_VOID || transState == TransData.ETransStatus.VOIDED ? Utils.getString(R.string.trans_void).toUpperCase() : Utils.getString(R.string.trans_sale).toUpperCase();
        typeWallet += transData.isEcrProcess() ? " " + Utils.getString(R.string.receipt_pos_tran) : "";
        page.addLine()
                .adjustTopSpace(1)
                .addUnit(page.createUnit()
                        .setText(typeWallet)
                        .setFontSize(FONT_BIG)
                );

        String acquirerName = acquirer.getName();
        //TRAN ID
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_tran_id))
                        .setFontSize(FONT_NORMAL_22)
                        .setWeight(1.5f))
                .addUnit(page.createUnit()
                        .setText(":")
                        .setFontSize(FONT_NORMAL_22)
                        .setWeight(3.5f));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(transData.getWalletPartnerID() != null ? transData.getWalletPartnerID().trim() : "")
                        .setFontSize(FONT_SMALL));

        page.addLine()
                .adjustTopSpace(5)
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_channel))
                        .setFontSize(FONT_NORMAL)
                        .setWeight(1.3f))
                .addUnit(page.createUnit()
                        .setText(":")
                        .setFontSize(FONT_NORMAL_22)
                        .setWeight(0.2f))
                .addUnit(page.createUnit()
                        .setText(transData.getChannel())
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END)
                        .setWeight(3.0f));


        // amount
        long amount = Utils.parseLongSafe(transData.getAmount(), 0);
        if (transType.isSymbolNegative())
            amount = -amount;
        if (transType == ETransType.QR_MYPROMPT_VOID || transState == TransData.ETransStatus.VOIDED)
            amount = -amount;
        temp = CurrencyConverter.convert(amount, transData.getCurrency());
        //TOTAL //AMOUNT
        if (temp.length() > 16) {
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("BASE")
                            .setFontSize(FONT_BIG_28)
                            .setWeight(3.0f))
                    .addUnit(page.createUnit()
                            .setText(temp)
                            .setFontSize(FONT_BIG_28)
                            .setGravity(Gravity.END)
                            .setWeight(10.0f));
        } else {
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("BASE")
                            .setFontSize(FONT_BIG)
                    )
                    .addUnit(page.createUnit()
                            .setText(temp)
                            .setFontSize(FONT_BIG)
                            .setGravity(Gravity.END)
                    );
        }

        temp = transType == ETransType.QR_MYPROMPT_VOID || transState == TransData.ETransStatus.VOIDED?
                TimeConverter.convert(transData.getOrigDateTime(), Constants.TIME_PATTERN_TRANS, Constants.TIME_PATTERN_DISPLAY) :
                TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS, Constants.TIME_PATTERN_DISPLAY);

        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_pay_time))
                        .setFontSize(FONT_NORMAL_22)
                        .setWeight(1.3f))
                .addUnit(page.createUnit()
                        .setText(":")
                        .setFontSize(FONT_NORMAL_22)
                        .setWeight(0.2f))
                .addUnit(page.createUnit()
                        .setText(temp)
                        .setFontSize(FONT_NORMAL_22)
                        .setWeight(3.0f)
                        .setGravity(Gravity.END));

        page.addLine().addUnit(page.createUnit().setText(" "));
        // REF 1 & 2
        printEdcRef1Ref2(page, transData);

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setScaleX(fullSizeOneLine)
                .setGravity(Gravity.CENTER));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_no_sig_required))
                .setFontSize(FONT_NORMAL_22)
                .setGravity(Gravity.CENTER));


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


        if (receiptMax == 3) {
            if (receiptNo == 0) {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_acquire_hyphen))
                                .setGravity(Gravity.CENTER));
            } else if (receiptNo == 1) {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_merchant_hyphen))
                                .setGravity(Gravity.CENTER));
                genImageOnEndReceipt(page, false);
            } else {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_user_hyphen))
                                .setGravity(Gravity.CENTER));
                genImageOnEndReceipt(page, true);
            }
        } else {
            if (receiptNo == 0) {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_merchant_hyphen))
                                .setGravity(Gravity.CENTER));
                genImageOnEndReceipt(page, false);
            } else {
                page.addLine()
                        .adjustTopSpace(5)
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_stub_cust_hyphen))
                                .setGravity(Gravity.CENTER));
                genImageOnEndReceipt(page, true);
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

        if (onErmMode) {
            PageToSlipFormat.isSettleMode = false;
            PageToSlipFormat.getInstance().Append(page);
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
        byte[] signData = null;
        if (!(transData.getTransType() == ETransType.VOID)
                && !(transData.getTransType() == ETransType.KBANK_SMART_PAY_VOID)
                && !(transData.getTransType() == ETransType.KBANK_REDEEM_VOID)
                && !(transData.getTransType() == ETransType.QR_VOID_KPLUS)
                && !(transData.getTransType() == ETransType.QR_VOID_WECHAT)
                && !(transData.getTransType() == ETransType.QR_VOID_ALIPAY)
                && !(transData.getTransType() == ETransType.DOLFIN_INSTALMENT_VOID)
                && (!(isRePrint)
                || transData.getSettlementErmReprintMode() == TransData.SettleErmReprintMode.HideReprint_ShowSignature
                || transData.getSettlementErmReprintMode() == TransData.SettleErmReprintMode.ShowReprint_ShowSignature)) {
            signData = transData.getSignData();
        }

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

                    //DCC Margin
                    String markUp = Component.unpackField63Dcc(transData);
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText(Utils.getString(R.string.receipt_margin) + " = " + markUp + "%")
                                    .setFontSize(FONT_NORMAL)
                                    .setGravity(Gravity.END));

                    page.addLine().addUnit(page.createUnit().setText(" "));

                    try {
                        String disclaimerStr = null;
                        int fontsize;
                        String[] disclaimerArr = new String[0];
                        if (isMasterCard) {
                            fontsize = FONT_SMALL_16;
                            disclaimerStr = Utils.getString(R.string.receipt_dcc_ack_mastercard);
                            disclaimerArr = ReworkDisclaimer(disclaimerStr, EReceiptUtils.MAX42_CHAR_PER_LINE);
                        } else {
                            fontsize = FONT_SMALL_18;
                            disclaimerStr = Utils.getString(R.string.receipt_dcc_ack_visa_tops,
                                    CurrencyConverter.getCurrencySymbol(CurrencyConverter.getDefCurrency(), true),
                                    CurrencyConverter.getCurrencySymbol(currencyNumeric, true), markUp + "%");
                            disclaimerArr = ReworkDisclaimer(disclaimerStr, EReceiptUtils.MAX42_CHAR_PER_LINE);
                        }

                        Log.d("ERCM", "-------> Disclaimer Len =" + disclaimerArr.length + " line(s)");
                        for (int dcc_dclm_index = 0; dcc_dclm_index <= disclaimerArr.length; dcc_dclm_index++) {
                            Log.d("ERCM", "\t\t\tLine " + (dcc_dclm_index + 1) + " : " + disclaimerArr[dcc_dclm_index]);
                            page.addLine()
                                    .addUnit(page.createUnit()
                                            .setText(disclaimerArr[dcc_dclm_index].trim())
                                            .setFontSize(fontsize));
                        }
                    } catch (Exception ex) {

                    }
//                    if (isMasterCard) {
//                        page.addLine()
//                                .addUnit(page.createUnit()
//                                        .setText(Utils.getString(R.string.receipt_dcc_ack_mastercard))
//                                        .setFontSize(FONT_SMALL_18));
//                    } else {
//                        page.addLine()
//                                .addUnit(page.createUnit()
//                                        .setText(Utils.getString(R.string.receipt_dcc_ack_visa_tops,
//                                                CurrencyConverter.getCurrencySymbol(CurrencyConverter.getDefCurrency(), true),
//                                                CurrencyConverter.getCurrencySymbol(currencyNumeric, true), markUp + "%"))
//                                        .setFontSize(FONT_SMALL));
//                    }
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

    private String[] ReworkDisclaimer(String localStr, int MaxCharacterPerLine) {
        if (localStr == null) {
            return new String[0];
        }

        ArrayList<String> arrayList = new ArrayList<>();
        String[] tmpStr = null;
        boolean exitLoopFlag = false;
        String remainDisclaimer = localStr;
        do {
            try {
                tmpStr = EReceiptUtils.stringSplitter(remainDisclaimer, MaxCharacterPerLine);
                if (tmpStr.length == 2) {
                    if (tmpStr[1].length() > MaxCharacterPerLine) {
                        arrayList.add(tmpStr[0]);
                        remainDisclaimer = tmpStr[1];
                    } else {
                        arrayList.add(tmpStr[0]);
                        arrayList.add(tmpStr[1]);
                        exitLoopFlag = true;
                    }
                }
            } catch (Exception ex) {

            }
        } while (exitLoopFlag == false);

        String[] disclaimerArray = new String[arrayList.size()];
        for (int iStr = 0; iStr <= arrayList.size() - 1; iStr++) {
            disclaimerArray[iStr] = arrayList.get(iStr);
        }

        return disclaimerArray;
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

    public static void printEdcRef1Ref2(IPage page, TransData transData) {
        int iMode = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_SUPPORT_REF1_2_MODE);
        DownloadManager.EdcRef1Ref2Mode mode = DownloadManager.EdcRef1Ref2Mode.getByMode(iMode);
        if (mode == DownloadManager.EdcRef1Ref2Mode.REF_1_MODE) {
            if (transData.getSaleReference1() != null && !transData.getSaleReference1().isEmpty()) {
                String ref1 = FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_DISP_TEXT_REF1);
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(ref1 != null ? ref1 + " :" : "REFERENCE 1 :")
                                .setFontSize(FONT_NORMAL)
                                .setGravity(Gravity.START)
                                .setWeight(6.0f));
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(transData.getSaleReference1() != null ? transData.getSaleReference1() : "")
                                .setFontSize(FONT_NORMAL)
                                .setGravity(Gravity.END));
            }
        } else if (mode == DownloadManager.EdcRef1Ref2Mode.REF_1_2_MODE) {
            String ref1 = FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_DISP_TEXT_REF1);
            String ref2 = FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_DISP_TEXT_REF2);
            if (transData.getSaleReference1() != null && !transData.getSaleReference1().isEmpty()) {
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(ref1 != null ? ref1 + " :" : "REFERENCE 1 :")
                                .setFontSize(FONT_NORMAL)
                                .setGravity(Gravity.START)
                                .setWeight(6.0f));
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(transData.getSaleReference1() != null ? transData.getSaleReference1() : "")
                                .setFontSize(FONT_NORMAL)
                                .setGravity(Gravity.END));
            }
            if (transData.getSaleReference2() != null && !transData.getSaleReference2().isEmpty()) {
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(ref2 != null ? ref2 + " :" : "REFERENCE 2 :")
                                .setFontSize(FONT_NORMAL)
                                .setGravity(Gravity.START)
                                .setWeight(6.0f));
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(transData.getSaleReference2() != null ? transData.getSaleReference2() : "")
                                .setFontSize(FONT_NORMAL)
                                .setGravity(Gravity.END));
            }
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
        if (onErmMode) {
            return;
        }

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
        if (onErmMode) {
            return;
        }
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

    private void genQRBarCodeAlipayWallet(IPage page, TransData transData, Acquirer acquirer) {
        if (onErmMode) {
            return;
        }
        if (!transData.isEcrProcess()) {
            boolean isShowQRBarcodeAliWechat = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_QR_BARCODE_ALIPAY_WECHAT);
            if (isShowQRBarcodeAliWechat && ((receiptMax == 3 && receiptNo == 1) || (receiptMax != 3 && receiptNo == 0))) {// only merchant copy
                String qrData;
                if (Constants.ACQ_QR_CREDIT.equals(acquirer.getName())) {
                    String amount = Component.getPaddedNumber(Utils.parseLongSafe(transData.getAmount(), 0), 9);
                    String cardNo = transData.getBuyerLoginID() != null ? transData.getBuyerLoginID().trim() : "";
                    qrData = "07" + amount + cardNo;
                } else {
                    String amount = Component.getPaddedNumber(Utils.parseLongSafe(transData.getAmount(), 0), 8);
                    String tid = acquirer != null ? acquirer.getTerminalId() : null;
                    String appCode = transData.getAuthCode() != null ? transData.getAuthCode() : null;
                    String prefix = "";
                    switch (acquirer.getName()) {
                        case Constants.ACQ_ALIPAY:
                        case Constants.ACQ_ALIPAY_B_SCAN_C:
                            prefix = "01";
                            break;
                        case Constants.ACQ_WECHAT:
                        case Constants.ACQ_WECHAT_B_SCAN_C:
                            prefix = "02";
                            break;
                        case Constants.ACQ_KPLUS:
                            prefix = "05";
                            break;
                    }
                    //Constants.ACQ_ALIPAY.equalsIgnoreCase(acquirer.getName()) ? "01" : Constants.ACQ_ALIPAY.equalsIgnoreCase(acquirer.getName()) ? "02" : "05";//01=alipay, 02=wechat, 03= kplus
                    String formattedDate = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                            Constants.DATE_PATTERN_DISPLAY2);
                    qrData = prefix + amount + formattedDate + tid + appCode;
                }
                page.addLine()
                        .addUnit(page.createUnit()
                                .setBitmap(Utils.generateQrCode(qrData, 200, 200))
                                .setGravity(Gravity.CENTER));
                page.addLine().addUnit(page.createUnit()
                        .setText(qrData)
                        .setFontSize(FONT_SMALL_18)
                        .setGravity(Gravity.CENTER));
                page.addLine()
                        .addUnit(page.createUnit()
                                .setBitmap(Utils.generateBarCode(qrData, 300, 80))
                                .setGravity(Gravity.CENTER))
                        .adjustTopSpace(5);
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(qrData)
                                .setFontSize(FONT_SMALL_18)
                                .setGravity(Gravity.CENTER))
                        .adjustTopSpace(5);

                page.addLine().addUnit(page.createUnit().setText(" "));
            }
        }
    }

//    private void genQRcodeCOD(IPage page, TransData transData, String panMask) {
//        if (panMask == null){
//            genQRcodeCOD(page, transData, "", true);
//        } else {
//            genQRcodeCOD(page, transData, panMask, false);
//        }
//    }

    private void genQRcodeCOD(IPage page, TransData transData, String panMask, boolean isSaleQrMode) {
        if (onErmMode) {
            return;
        }
        if (!transData.isEcrProcess()) {
            boolean isShowQRcodeCOD = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_QR_BARCODE_COD);
            if (isShowQRcodeCOD && ((receiptMax == 3 && receiptNo == 1) || (receiptMax != 3 && receiptNo == 0))) {// only merchant copy
                String qrData = null;
                if (isSaleQrMode) {
                    // SALE QR-CODE (ALP, WCP, THQR, QRC)
                    qrData = genQRdataCODforQrPayment(transData);
                } else {
                    // SALE CREDIT/DEBIT CARD
                    qrData = genQRdataCODforCardPayment(transData, panMask);
                }

                page.addLine()
                        .addUnit(page.createUnit()
                                .setBitmap(Utils.generateQrCodeLevelM(qrData, 350, 350))
                                .setGravity(Gravity.CENTER));
                /*page.addLine().addUnit(page.createUnit()
                        .setText(qrData)
                        .setFontSize(FONT_SMALL_18)
                        .setGravity(Gravity.CENTER));*/

                page.addLine().addUnit(page.createUnit().setText(" "));
            }
        }
    }

    private String genQRdataCODforQrPayment(TransData transData) {
        String data;

        String transType = (transData.getTransType() != null) ? nonNullString(transData.getTransType().getTransName()).toUpperCase() : " ";

        TransData.ETransStatus transStatus = transData.getTransState();
        if (transStatus == TransData.ETransStatus.VOIDED) {
            transType = ETransType.VOID.getTransName().toUpperCase();
        }
        transType = (transType.toUpperCase().contains("SALE") || transType.toUpperCase().equals(ETransType.QR_INQUIRY_CREDIT.getTransName().toUpperCase()))
                       ? "SALE"
                       : (transType.toUpperCase().contains("VOID")) ? "VOID" : transType;

        long stanno = (transStatus == TransData.ETransStatus.VOIDED) ? transData.getVoidStanNo() : transData.getStanNo();
        String stan = nonNullString(Component.getPaddedNumber(stanno, 6));
        String batch = nonNullString(Component.getPaddedNumber(transData.getBatchNo(), 6));
        String trace = nonNullString(Component.getPaddedNumber(transData.getTraceNo(), 6));
        String appCode = nonNullString(transData.getAuthCode());
        String txnNo = nonNullString(transData.getTxnNo());

        String dateTime = transData.getDateTime() != null ? TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS, "yyMMddHHmmss") : " ";

        Acquirer acquirer = transData.getAcquirer();
        String cardName = null;
        switch (acquirer.getName()) {
            case Constants.ACQ_KPLUS : cardName = Constants.ISSUER_KPLUS_PROMPYPAY; break;
            default : cardName = acquirer.getName();
        }

        String tid = acquirer != null ? nonNullString(acquirer.getTerminalId()) : " ";
        String mid = acquirer != null ? nonNullString(acquirer.getMerchantId()) : " ";
        // amount
        long lAmt = Utils.parseLongSafe(transData.getAmount(), 0);
        String amount = ((transData.getTransType().isSymbolNegative() || transData.getTransState() == TransData.ETransStatus.VOIDED) ? "-" : "")
                + String.valueOf(Utils.parseLongSafe(transData.getAmount(), 0));
        amount = amount.substring(0, amount.length() - 2) + "." + amount.substring(amount.length() - 2);
        String cardHolderName = nonNullString(transData.getTrack1());

        String dblAmount = Utils.parseDoubleSafe(amount, 0.00).toString();


////        [Delimiter format]
////        ===========================================================================================================

        data = transType
                + "|" + stan
                + "|" + batch
                + "|" + trace
                + "|" + appCode
                + "|" + txnNo
                + "|" + ""
                + "|" + ""
                + "|" + ""
                + "|" + dateTime
                + "|" + tid
                + "|" + mid
                + "|" + dblAmount
                + "|" + cardName                // in case "KPLUS" > "PROMPT PAY" , another wallet type use acquirer name
                + "|";

////        [JSON format]
////        ===========================================================================================================
//        JsonObject jsObj = new JsonObject();
//        jsObj.addProperty("type", transType);
//        jsObj.addProperty("stan", stan);
//        jsObj.addProperty("batch", batch);
//        jsObj.addProperty("trace", trace);
//        jsObj.addProperty("appcode", appCode);
//        jsObj.addProperty("refNo", txnNo);                    // set > Kbank Wallet Trans No.
//        jsObj.addProperty("creditNo", "");              // set > empty value
//        jsObj.addProperty("expiry", "");                // set > empty value
//        jsObj.addProperty("cardLabel", "");             // set > empty value
//        jsObj.addProperty("dateTime", dateTime);
//        jsObj.addProperty("tid", tid);
//        jsObj.addProperty("mid", mid);
//        jsObj.addProperty("amount", dblAmount);
//        jsObj.addProperty("cardName", cardName);                // in case "KPLUS" > "PROMPT PAY" , another wallet type use acquirer name
//
        Log.i(TAG, "QR Data COD : " + data);
        return data;
    }

    private String genQRdataCODforCardPayment(TransData transData, String panMask) {
        String data;

        String transType = transData.getTransType() != null ? nonNullString(transData.getTransType().getTransName()).toUpperCase() : " ";

        TransData.ETransStatus transStatus = transData.getTransState();
        if (transStatus == TransData.ETransStatus.VOIDED) {
            transType = ETransType.VOID.getTransName().toUpperCase();
        }

        long stanno = (transStatus == TransData.ETransStatus.VOIDED) ? transData.getVoidStanNo() : transData.getStanNo();
        String stan = nonNullString(Component.getPaddedNumber(stanno, 6));
        String batch = nonNullString(Component.getPaddedNumber(transData.getBatchNo(), 6));
        String trace = nonNullString(Component.getPaddedNumber(transData.getTraceNo(), 6));
        String appCode = nonNullString(transData.getAuthCode());
        String refNo = nonNullString(transData.getRefNo());

        panMask = nonNullString(panMask).replace("*", "X");

        String cardExp = "XXXX";
        String cardIssuer = transData.getIssuer() != null ? nonNullString(transData.getIssuer().getIssuerName()) : " ";
        String dateTime = transData.getDateTime() != null ? TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS, "yyMMddHHmmss") : " ";

        Acquirer acquirer = transData.getAcquirer();
        String tid = acquirer != null ? nonNullString(acquirer.getTerminalId()) : " ";
        String mid = acquirer != null ? nonNullString(acquirer.getMerchantId()) : " ";
        // amount
        long lAmt = Utils.parseLongSafe(transData.getAmount(), 0);
        String amount = ((transData.getTransType().isSymbolNegative() || transData.getTransState() == TransData.ETransStatus.VOIDED) ? "-" : "")
                + String.valueOf(Utils.parseLongSafe(transData.getAmount(), 0));
        amount = amount.substring(0, amount.length() - 2) + "." + amount.substring(amount.length() - 2);
        String cardHolderName = nonNullString(transData.getTrack1());

//        [Plain Text with fix-length format]
//        ===========================================================================================================
//        data = transType + field2 + stan + batch + trace + appCode + refNo + pan + field9 + cardExp
//                + cardIssuer + dateTime + tid + mid + field15 + field16 + field17 + amount + cardHolderName;

//        [Delimiter format]
//        ===========================================================================================================
        data = transType
                + "|" + stan
                + "|" + batch
                + "|" + trace
                + "|" + appCode
                + "|" + refNo
                + "|" + panMask
                + "|" + cardExp
                + "|" + cardIssuer
                + "|" + dateTime
                + "|" + tid
                + "|" + mid
                + "|" + amount
                + "|" + cardHolderName
                + "|";

//        [JSON format]
//        ===========================================================================================================
//        Double dblAmount = Utils.parseDoubleSafe(amount, 0.00);
//        JsonObject jsObj = new JsonObject();
//        jsObj.addProperty("type", transType);
//        jsObj.addProperty("stan", stan);
//        jsObj.addProperty("batch", batch);
//        jsObj.addProperty("trace", trace);
//        jsObj.addProperty("appcode", appCode);
//        jsObj.addProperty("refNo", refNo);
//        jsObj.addProperty("creditNo", panMask);
//        jsObj.addProperty("expiry", cardExp);
//        jsObj.addProperty("cardLabel", cardIssuer);
//        jsObj.addProperty("dateTime", dateTime);
//        jsObj.addProperty("tid", tid);
//        jsObj.addProperty("mid", mid);
//        jsObj.addProperty("amount", dblAmount);
//        jsObj.addProperty("cardName", cardHolderName);

//        data =  jsObj.toString();

        Log.i(TAG, "QR Data COD : " + data);
        return data;
    }

    private String nonNullString(String input) {
        return input != null ? input.trim() : " ";
    }

    private void printEcrPOSInfo(IPage page) {
        //EDR data
        //ticket no.
        if (transData.isEcrProcess()) {
            if (Utils.isStringNotNullOrEmpty(transData.getPosNo_ReceiptNo())) {
                //ticket no.
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText("Ticket No.")
                                .setFontSize(FONT_NORMAL)
                                .setWeight(2.0f))
                        .addUnit(page.createUnit()
                                .setText(": " + transData.getPosNo_ReceiptNo())
                                .setGravity(Gravity.LEFT)
                                .setWeight(2.5f));
            }

            if (Utils.isStringNotNullOrEmpty(transData.getCashierName())) {
                //User name
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText("USER Name")
                                .setFontSize(FONT_NORMAL)
                                .setWeight(2.0f))
                        .addUnit(page.createUnit()
                                .setText(": " + transData.getCashierName())
                                .setGravity(Gravity.LEFT)
                                .setWeight(2.5f));
            }

//            if (Utils.isStringNotNullOrEmpty(transData.getReferenceSaleID())) {
//                // SALE Reference ID
//                page.addLine()
//                        .addUnit(page.createUnit()
//                                .setText("Ref.Sale ID : ")
//                                .setFontSize(FONT_NORMAL)
//                                .setWeight(2.0f))
//                        .addUnit(page.createUnit()
//                                .setText(transData.getReferenceSaleID())
//                                .setGravity(Gravity.LEFT)
//                                .setWeight(2.5f));
//            }

            //End EDR data
        }
    }

    private void printTipAdjustInfo(IPage page, long newTotalAmt) {
        long lTipAmount = Utils.parseLongSafe(transData.getTipAmount(), 0);
        long lBaseAmount = newTotalAmt - lTipAmount;
        String baseAmount = CurrencyConverter.convert(lBaseAmount, transData.getCurrency());
        page.addLine()
                .adjustTopSpace(1)
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_amount_short))
                        .setFontSize(FONT_NORMAL)
                        .setWeight(3.0f))
                .addUnit(page.createUnit()
                        .setText(baseAmount)
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END)
                        .setWeight(10.0f));

        String tipAmount = CurrencyConverter.convert(lTipAmount, transData.getCurrency());
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_amount_tip))
                        .setFontSize(FONT_NORMAL)
                        .setWeight(3.0f))
                .addUnit(page.createUnit()
                        .setText(tipAmount)
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END)
                        .setWeight(10.0f));
    }

}
