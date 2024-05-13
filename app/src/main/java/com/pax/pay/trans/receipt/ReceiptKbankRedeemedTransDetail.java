package com.pax.pay.trans.receipt;

import android.graphics.Typeface;
import android.view.Gravity;

import com.pax.edc.R;
import com.pax.glwrapper.page.IPage;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.ReservedFieldHandle;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.Utils;

import java.util.HashMap;
import java.util.Locale;

public class ReceiptKbankRedeemedTransDetail {
    public static void generateDetail(IPage page, TransData transData, boolean isMerchantCopy, boolean isAudit, boolean isReprint) {
        generateDetail(page, transData, isMerchantCopy, isAudit, isReprint, -1);
    }
    public static void generateDetail(IPage page, TransData transData, boolean isMerchantCopy, boolean isAudit, boolean isReprint, int fontsize) {
        boolean isStateVoided = transData.getTransState() == TransData.ETransStatus.VOIDED;
        boolean isVoid = isStateVoided || transData.getTransType() == ETransType.KBANK_REDEEM_VOID;

        HashMap<ReservedFieldHandle.FieldTables, byte[]> f63 = ReservedFieldHandle.unpackReservedField(transData.getField63RecByte(), ReservedFieldHandle.redeemed_response, true);

        if (f63 != null) {
            String sale_amt = new String(f63.get(ReservedFieldHandle.FieldTables.SALES_AMT));
            String redeemPt = new String(f63.get(ReservedFieldHandle.FieldTables.REDEEMED_PT));
            String redeem_amt = new String(f63.get(ReservedFieldHandle.FieldTables.REDEEMED_AMT));
            String net_sale_amt = new String(f63.get(ReservedFieldHandle.FieldTables.NET_SALES_AMT));
            String bal_rate = new String(f63.get(ReservedFieldHandle.FieldTables.BALANCE_RATE));
            String prod_cd = new String(f63.get(ReservedFieldHandle.FieldTables.PROD_CD));
            String prod_name = new String(f63.get(ReservedFieldHandle.FieldTables.PROD_NAME));
            String discount_amt = CurrencyConverter.convert(Utils.parseLongSafe(redeem_amt, 0), transData.getCurrency());

            int prod_qty = isVoid ? -(Integer.parseInt(new String(f63.get(ReservedFieldHandle.FieldTables.QTY)))) : Integer.parseInt(new String(f63.get(ReservedFieldHandle.FieldTables.QTY)));
            int bal_pt = Integer.parseInt(new String(f63.get(ReservedFieldHandle.FieldTables.BAL_PT)));
            int redeem_pt = isVoid ? -(Integer.parseInt(redeemPt)) : Integer.parseInt(redeemPt);

            long amount = isVoid ? -(Utils.parseLongSafe(redeem_amt, 0)) : Utils.parseLongSafe(redeem_amt, 0);
            redeem_amt = CurrencyConverter.convert(amount, transData.getCurrency());

            amount = isVoid ? -(Utils.parseLongSafe(net_sale_amt, 0)) : Utils.parseLongSafe(net_sale_amt, 0);
            net_sale_amt = CurrencyConverter.convert(amount, transData.getCurrency());

            amount = isVoid ? -(Utils.parseLongSafe(sale_amt, 0)) : Utils.parseLongSafe(sale_amt, 0);
            sale_amt = CurrencyConverter.convert(amount, transData.getCurrency());

            bal_rate = String.format(Locale.getDefault(), "%.2f", Double.parseDouble(bal_rate)/100);

            int FONT_BIG = IReceiptGenerator.FONT_BIG;
            int FONT_NORMAL_26 = IReceiptGenerator.FONT_NORMAL_26;
            int FONT_NORMAL = IReceiptGenerator.FONT_NORMAL;
            int FONT_SMALL_18 = IReceiptGenerator.FONT_SMALL_18;

            ETransType transType = isVoid && !isStateVoided ? transData.getOrigTransType() : transData.getTransType();

            switch (transType) {
                case KBANK_REDEEM_VOUCHER:
                    add2Units(Utils.getString(R.string.receipt_product_code), prod_cd, getfontSize(isMerchantCopy , FONT_NORMAL , FONT_SMALL_18, fontsize), Typeface.NORMAL, page, isAudit);
                    add2Units(Utils.getString(R.string.receipt_redeem_point), String.format(Locale.getDefault(),"%,d", redeem_pt),
                            getfontSize(isMerchantCopy , FONT_NORMAL_26 , FONT_SMALL_18, fontsize), isMerchantCopy ? Typeface.BOLD : Typeface.NORMAL, page, isAudit);
                    add2Units(Utils.getString(R.string.receipt_redeem_amt), redeem_amt,
                            getfontSize(isMerchantCopy , FONT_NORMAL_26 , FONT_SMALL_18, fontsize), isMerchantCopy ? Typeface.BOLD : Typeface.NORMAL, page, isAudit);
                    if (isMerchantCopy) addSingleLine(page, isAudit);
                    addDoubleLine(page, isAudit);
                    if (!isAudit) add2Units(Utils.getString(R.string.receipt_bal_pt), String.format(Locale.getDefault(),"%,d", bal_pt), FONT_NORMAL, Typeface.BOLD, page, false);
                    break;
                case KBANK_REDEEM_VOUCHER_CREDIT:
                    add2Units(Utils.getString(R.string.receipt_product_code), prod_cd, getfontSize(isMerchantCopy , FONT_NORMAL , FONT_SMALL_18,fontsize), Typeface.NORMAL, page, isAudit);
                    add2Units(Utils.getString(R.string.receipt_redeem_point), String.format(Locale.getDefault(),"%,d", redeem_pt),
                            getfontSize(isMerchantCopy , FONT_NORMAL_26 , FONT_SMALL_18, fontsize), isMerchantCopy ? Typeface.BOLD : Typeface.NORMAL, page, isAudit);
                    add2Units(Utils.getString(R.string.receipt_redeem_amt), redeem_amt,
                            getfontSize(isMerchantCopy , FONT_NORMAL_26 , FONT_SMALL_18, fontsize), isMerchantCopy ? Typeface.BOLD : Typeface.NORMAL, page, isAudit);
                    add2Units(Utils.getString(R.string.receipt_credit_amt), net_sale_amt,
                            getfontSize(isMerchantCopy , FONT_NORMAL_26 , FONT_SMALL_18, fontsize), isMerchantCopy ? Typeface.BOLD : Typeface.NORMAL, page, isAudit);
                    addSingleLine(page, isAudit);
                    add2Units(Utils.getString(R.string.receipt_amount_total), sale_amt, getfontSize(isMerchantCopy , FONT_NORMAL_26 , FONT_SMALL_18,fontsize), Typeface.BOLD, page, isAudit);
                    addDoubleLine(page, isAudit);
                    if (!isAudit) add2Units(Utils.getString(R.string.receipt_bal_pt), String.format(Locale.getDefault(),"%,d", bal_pt), FONT_NORMAL, Typeface.BOLD, page, false);
                    break;
                case KBANK_REDEEM_DISCOUNT:
                    add2Units(Utils.getString(R.string.receipt_product_code), prod_cd, getfontSize(isMerchantCopy , FONT_NORMAL , FONT_SMALL_18,fontsize), Typeface.NORMAL, page, isAudit);
                    add1Unit(Utils.getString(R.string.receipt_product_name), getfontSize(isMerchantCopy , FONT_NORMAL_26 , FONT_SMALL_18,fontsize), Typeface.NORMAL, Gravity.START, page, isAudit);
                    add1Unit("  " + prod_name, getfontSize(isMerchantCopy , FONT_NORMAL_26 , FONT_SMALL_18,fontsize), isMerchantCopy ? Typeface.BOLD : Typeface.NORMAL, Gravity.START, page, isAudit);
                    add2Units(Utils.getString(R.string.receipt_redeem_point), String.format(Locale.getDefault(),"%,d", redeem_pt),
                            getfontSize(isMerchantCopy , FONT_NORMAL_26 , FONT_SMALL_18,fontsize), isMerchantCopy ? Typeface.BOLD : Typeface.NORMAL, page, isAudit);
                    add2Units(Utils.getString(R.string.receipt_credit_amt), net_sale_amt,
                            getfontSize(isMerchantCopy , FONT_NORMAL_26 , FONT_SMALL_18,fontsize), isMerchantCopy ? Typeface.BOLD : Typeface.NORMAL, page, isAudit);
                    add2Units(Utils.getString(R.string.receipt_discount_percent), bal_rate, getfontSize(isMerchantCopy , FONT_NORMAL_26 , FONT_SMALL_18,fontsize), Typeface.NORMAL, page, isAudit);
                    add2Units(Utils.getString(R.string.receipt_discount_amt), discount_amt, getfontSize(isMerchantCopy , FONT_NORMAL_26 , FONT_SMALL_18,fontsize), Typeface.NORMAL, page, isAudit);
                    addSingleLine(page, isAudit);
                    add2Units(Utils.getString(R.string.receipt_amount_total), sale_amt, getfontSize(isMerchantCopy , FONT_NORMAL_26 , FONT_NORMAL,fontsize), Typeface.BOLD, page, isAudit);
                    addDoubleLine(page, isAudit);
                    if (!isAudit) add2Units(Utils.getString(R.string.receipt_bal_pt), String.format(Locale.getDefault(),"%,d", bal_pt), FONT_NORMAL, Typeface.BOLD, page, false);
                    break;
                case KBANK_REDEEM_INQUIRY:
                    add2Units(Utils.getString(R.string.receipt_bal_pt), String.format(Locale.getDefault(),"%,d", bal_pt), FONT_BIG, Typeface.NORMAL, page, isAudit);
                    break;
                default: // REDEEM PRODUCT, PRODUCT+CREDIT
                    add2Units(Utils.getString(R.string.receipt_product_code), prod_cd, getfontSize(isMerchantCopy , FONT_NORMAL , FONT_SMALL_18,fontsize), Typeface.NORMAL, page, isAudit);
                    add1Unit(Utils.getString(R.string.receipt_product_name), getfontSize(isMerchantCopy , FONT_NORMAL , FONT_SMALL_18,fontsize), Typeface.NORMAL, Gravity.START, page, isAudit);
                    add1Unit("  " + prod_name, getfontSize(isMerchantCopy , FONT_NORMAL_26 , FONT_SMALL_18,fontsize), isMerchantCopy ? Typeface.BOLD : Typeface.NORMAL, Gravity.START, page, isAudit);
                    add2Units(Utils.getString(R.string.receipt_redeem_point), String.format(Locale.getDefault(),"%,d", redeem_pt),
                            getfontSize(isMerchantCopy , FONT_NORMAL_26 , FONT_SMALL_18,fontsize), isMerchantCopy ? Typeface.BOLD : Typeface.NORMAL, page, isAudit);
                    if (transData.getTransType() == ETransType.KBANK_REDEEM_PRODUCT_CREDIT) {
                        add2Units(Utils.getString(R.string.receipt_credit_amt), net_sale_amt,
                                getfontSize(isMerchantCopy , FONT_NORMAL_26 , FONT_SMALL_18,fontsize), isMerchantCopy ? Typeface.BOLD : Typeface.NORMAL, page, isAudit);
                    }
                    add2Units(Utils.getString(R.string.receipt_product_qty), String.valueOf(prod_qty),
                            getfontSize(isMerchantCopy , FONT_NORMAL , FONT_SMALL_18,fontsize), Typeface.NORMAL, page, isAudit);
                    if (isMerchantCopy) addSingleLine(page, isAudit);
                    addDoubleLine(page, isAudit);
                    if (!isAudit) add2Units(Utils.getString(R.string.receipt_bal_pt), String.format(Locale.getDefault(),"%,d", bal_pt), FONT_NORMAL, Typeface.BOLD, page, false);
                    break;
            }
        }
    }

    private static int getfontSize(boolean isMerchantCopy , int normalfontsize, int smallfontsize , int fixfontsize) {
        int NormalFontSize = normalfontsize ;
        int smallFontSize = smallfontsize ;
        if (fixfontsize != -1) {
            return fixfontsize;
        }
        else {
            return ((isMerchantCopy) ? NormalFontSize : smallFontSize) ;
        }
    }

    private static void add2Units(String title, String value, int size, int style, IPage page, boolean isAudit) {
        if (isAudit) {
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(title)
                            .setFontSize(size))
                    .addUnit(page.createUnit()
                            .setText(value)
                            .setFontSize(size)
                            .setGravity(Gravity.END));
        } else {
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
    }

    private static void add1Unit(String text, int size, int style, int gravity, IPage page, boolean isAudit) {
        if (isAudit) {
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(text)
                            .setGravity(gravity));
        } else {
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(text)
                            .setFontSize(size)
                            .setTextStyle(style)
                            .setGravity(gravity));
        }
    }

    private static void addSingleLine(IPage page, boolean isAudit) {
        if (!isAudit) {
            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.receipt_one_line))
                    .setGravity(Gravity.CENTER));
        }
    }

    private static void addDoubleLine(IPage page, boolean isAudit) {
        if (!isAudit) {
            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.receipt_double_line))
                    .setGravity(Gravity.CENTER));
        }
    }
}
