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
package com.pax.pay.trans.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.pax.pay.base.Acquirer;

import java.io.Serializable;

@DatabaseTable(tableName = "trans_redeem_kbank_total")
public class TransRedeemKbankTotal implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String ID_FIELD_NAME = "redeemKbankTotal_id";

    //PRODUCT
    public static final String PRODUCT_VISA = "product_visa";
    public static final String PRODUCT_MASTERCARD = "product_mastercard";
    public static final String PRODUCT_JCB = "product_jcb";
    public static final String PRODUCT_OTHER = "product_other";
    public static final String PRODUCT_ALL_CARD = "product_all_card";
    public static final String PRODUCT_ITEMS = "product_items";
    public static final String PRODUCT_POINTS = "product_points";
    public static final String PRODUCT_REDEEM = "product_redeem";
    public static final String PRODUCT_TOTAL = "product_total";

    //PRODUCT+CREDIT
    public static final String PRODUCT_CREDIT_VISA = "productCredit_visa";
    public static final String PRODUCT_CREDIT_MASTERCARD = "productCredit_mastercard";
    public static final String PRODUCT_CREDIT_JCB = "productCredit_jcb";
    public static final String PRODUCT_CREDIT_OTHER = "productCredit_other";
    public static final String PRODUCT_CREDIT_ALL_CARD = "productCredit_all_card";
    public static final String PRODUCT_CREDIT_ITEMS = "productCredit_items";
    public static final String PRODUCT_CREDIT_POINTS = "productCredit_points";
    public static final String PRODUCT_CREDIT_REDEEM = "productCredit_redeem";
    public static final String PRODUCT_CREDIT_CREDIT = "productCredit_credit";
    public static final String PRODUCT_CREDIT_TOTAL = "productCredit_total";
    public static final String PRODUCT_CREDIT_SALE_CREDIT_TOTAL = "productCredit_sale_credit_total";
    public static final String PRODUCT_CREDIT_SALE_CREDIT_TRANS_COUNT = "productCredit_sale_credit_trans_count";

    //VOUCHER
    public static final String VOUCHER_VISA = "voucher_visa";
    public static final String VOUCHER_MASTERCARD = "voucher_mastercard";
    public static final String VOUCHER_JCB = "voucher_jcb";
    public static final String VOUCHER_OTHER = "voucher_other";
    public static final String VOUCHER_ALL_CARD = "voucher_all_card";
    public static final String VOUCHER_ITEMS = "voucher_items";
    public static final String VOUCHER_POINTS = "voucher_points";
    public static final String VOUCHER_REDEEM = "voucher_redeem";
    public static final String VOUCHER_TOTAL = "voucher_total";

    //VOUCHER+CREDIT
    public static final String VOUCHER_CREDIT_VISA = "voucherCredit_visa";
    public static final String VOUCHER_CREDIT_MASTERCARD = "voucherCredit_mastercard";
    public static final String VOUCHER_CREDIT_JCB = "voucherCredit_jcb";
    public static final String VOUCHER_CREDIT_OTHER = "voucherCredit_other";
    public static final String VOUCHER_CREDIT_ALL_CARD = "voucherCredit_all_card";
    public static final String VOUCHER_CREDIT_ITEMS = "voucherCredit_items";
    public static final String VOUCHER_CREDIT_POINTS = "voucherCredit_points";
    public static final String VOUCHER_CREDIT_REDEEM = "voucherCredit_redeem";
    public static final String VOUCHER_CREDIT_CREDIT = "voucherCredit_credit";
    public static final String VOUCHER_CREDIT_TOTAL = "voucherCredit_total";
    public static final String VOUCHER_CREDIT_SALE_CREDIT_TOTAL = "voucherCredit_sale_credit_total";
    public static final String VOUCHER_CREDIT_SALE_CREDIT_TRANS_COUNT = "voucherCredit_sale_credit_trans_count";

    //DISCOUNT
    public static final String DISCOUNT_VISA = "discount_visa";
    public static final String DISCOUNT_MASTERCARD = "discount_mastercard";
    public static final String DISCOUNT_JCB = "discount_jcb";
    public static final String DISCOUNT_OTHER = "discount_other";
    public static final String DISCOUNT_ALL_CARD = "discount_all_card";
    public static final String DISCOUNT_ITEMS = "discount_items";
    public static final String DISCOUNT_POINTS = "discount_points";
    public static final String DISCOUNT_REDEEM = "discount_redeem";
    public static final String DISCOUNT_CREDIT = "discount_credit";
    public static final String DISCOUNT_TOTAL = "discount_total";
    public static final String DISCOUNT_SALE_CREDIT_TOTAL = "discount_sale_credit_total";
    public static final String DISCOUNT_SALE_CREDIT_TRANS_COUNT = "discount_sale_credit_trans_count";

    public static final String TOTAL_SALE_ALL_CARDS = "total_sale_all_cards";

    @DatabaseField(generatedId = true, columnName = ID_FIELD_NAME)
    protected int id;

    @DatabaseField(columnName = PRODUCT_VISA)
    private long productVisa;

    @DatabaseField(columnName = PRODUCT_MASTERCARD)
    private long productMastercard;

    @DatabaseField(columnName = PRODUCT_JCB)
    private long productJcb;

    @DatabaseField(columnName = PRODUCT_OTHER)
    private long productOther;

    @DatabaseField(columnName = PRODUCT_ALL_CARD)
    private long productAllCard;

    @DatabaseField(columnName = PRODUCT_ITEMS)
    private long productItems;

    @DatabaseField(columnName = PRODUCT_POINTS)
    private long productPoints;

    @DatabaseField(columnName = PRODUCT_REDEEM)
    private long productRedeem;

    @DatabaseField(columnName = PRODUCT_TOTAL)
    private long productTotal;

    @DatabaseField(columnName = PRODUCT_CREDIT_VISA)
    private long productCreditVisa;

    @DatabaseField(columnName = PRODUCT_CREDIT_MASTERCARD)
    private long productCreditMastercard;

    @DatabaseField(columnName = PRODUCT_CREDIT_JCB)
    private long productCreditJcb;

    @DatabaseField(columnName = PRODUCT_CREDIT_OTHER)
    private long productCreditOther;

    @DatabaseField(columnName = PRODUCT_CREDIT_ALL_CARD)
    private long productCreditAllCard;

    @DatabaseField(columnName = PRODUCT_CREDIT_ITEMS)
    private long productCreditItems;

    @DatabaseField(columnName = PRODUCT_CREDIT_POINTS)
    private long productCreditPoints;

    @DatabaseField(columnName = PRODUCT_CREDIT_REDEEM)
    private long productCreditRedeem;

    @DatabaseField(columnName = PRODUCT_CREDIT_CREDIT)
    private long productCreditCredit;

    @DatabaseField(columnName = PRODUCT_CREDIT_TOTAL)
    private long productCreditTotal;

    @DatabaseField(columnName = PRODUCT_CREDIT_SALE_CREDIT_TOTAL)
    private long productCreditSaleCreditTotal;

    @DatabaseField(columnName = PRODUCT_CREDIT_SALE_CREDIT_TRANS_COUNT)
    private int productCreditSaleCreditTransCount;

    @DatabaseField(columnName = VOUCHER_VISA)
    private long voucherVisa;

    @DatabaseField(columnName = VOUCHER_MASTERCARD)
    private long voucherMastercard;

    @DatabaseField(columnName = VOUCHER_JCB)
    private long voucherJcb;

    @DatabaseField(columnName = VOUCHER_OTHER)
    private long voucherOther;

    @DatabaseField(columnName = VOUCHER_ALL_CARD)
    private long voucherAllCard;

    @DatabaseField(columnName = VOUCHER_ITEMS)
    private long voucherItems;

    @DatabaseField(columnName = VOUCHER_POINTS)
    private long voucherPoints;

    @DatabaseField(columnName = VOUCHER_REDEEM)
    private long voucherRedeem;

    @DatabaseField(columnName = VOUCHER_TOTAL)
    private long voucherTotal;

    @DatabaseField(columnName = VOUCHER_CREDIT_VISA)
    private long voucherCreditVisa;

    @DatabaseField(columnName = VOUCHER_CREDIT_MASTERCARD)
    private long voucherCreditMastercard;

    @DatabaseField(columnName = VOUCHER_CREDIT_JCB)
    private long voucherCreditJcb;

    @DatabaseField(columnName = VOUCHER_CREDIT_OTHER)
    private long voucherCreditOther;

    @DatabaseField(columnName = VOUCHER_CREDIT_ALL_CARD)
    private long voucherCreditAllCard;

    @DatabaseField(columnName = VOUCHER_CREDIT_ITEMS)
    private long voucherCreditItems;

    @DatabaseField(columnName = VOUCHER_CREDIT_POINTS)
    private long voucherCreditPoints;

    @DatabaseField(columnName = VOUCHER_CREDIT_REDEEM)
    private long voucherCreditRedeem;

    @DatabaseField(columnName = VOUCHER_CREDIT_CREDIT)
    private long voucherCreditCredit;

    @DatabaseField(columnName = VOUCHER_CREDIT_TOTAL)
    private long voucherCreditTotal;

    @DatabaseField(columnName = VOUCHER_CREDIT_SALE_CREDIT_TOTAL)
    private long voucherCreditSaleCreditTotal;

    @DatabaseField(columnName = VOUCHER_CREDIT_SALE_CREDIT_TRANS_COUNT)
    private int voucherCreditSaleCreditTransCount;

    @DatabaseField(columnName = DISCOUNT_VISA)
    private long discountVisa;

    @DatabaseField(columnName = DISCOUNT_MASTERCARD)
    private long discountMastercard;

    @DatabaseField(columnName = DISCOUNT_JCB)
    private long discountJcb;

    @DatabaseField(columnName = DISCOUNT_OTHER)
    private long discountOther;

    @DatabaseField(columnName = DISCOUNT_ALL_CARD)
    private long discountAllCard;

    @DatabaseField(columnName = DISCOUNT_ITEMS)
    private long discountItems;

    @DatabaseField(columnName = DISCOUNT_POINTS)
    private long discountPoints;

    @DatabaseField(columnName = DISCOUNT_REDEEM)
    private long discountRedeem;

    @DatabaseField(columnName = DISCOUNT_CREDIT)
    private long discountCredit;

    @DatabaseField(columnName = DISCOUNT_TOTAL)
    private long discountTotal;

    @DatabaseField(columnName = DISCOUNT_SALE_CREDIT_TOTAL)
    private long discountSaleCreditTotal;

    @DatabaseField(columnName = DISCOUNT_SALE_CREDIT_TRANS_COUNT)
    private int discountSaleCreditTransCount;

    @DatabaseField(columnName = TOTAL_SALE_ALL_CARDS)
    private long totalSaleAllCards;

    public long redeemTotalSaleTransCount = 0 ;
    public long redeemTotalVoidTransCount = 0 ;

    public int getId() {return id;}

    public void setId(int id) { this.id = id;}

    public long getProductVisa() { return productVisa; }

    public void setProductVisa(long productVisa) {this.productVisa = productVisa;}

    public long getProductMastercard() { return productMastercard; }

    public void setProductMastercard(long productMastercard) { this.productMastercard = productMastercard;}

    public long getProductJcb() {return productJcb;}

    public void setProductJcb(long productJcb) {this.productJcb = productJcb;}

    public long getProductOther() {return productOther;}

    public void setProductOther(long productOther) {this.productOther = productOther; }

    public long getProductAllCard() {return productAllCard;}

    public void setProductAllCard(long productAllCard) {this.productAllCard = productAllCard;}

    public long getProductItems() {return productItems;}

    public void setProductItems(long productItems) {this.productItems = productItems;}

    public long getProductPoints() {return productPoints;}

    public void setProductPoints(long productPoints) {this.productPoints = productPoints;}

    public long getProductRedeem() {return productRedeem; }

    public void setProductRedeem(long productRedeem) {this.productRedeem = productRedeem;}

    public long getProductTotal() {return productTotal;}

    public void setProductTotal(long productTotal) {this.productTotal = productTotal;}

    public long getProductCreditVisa() {return productCreditVisa;}

    public void setProductCreditVisa(long productCreditVisa) {this.productCreditVisa = productCreditVisa;}

    public long getProductCreditMastercard() {return productCreditMastercard;}

    public void setProductCreditMastercard(long productCreditMastercard) {this.productCreditMastercard = productCreditMastercard;}

    public long getProductCreditJcb() {return productCreditJcb;}

    public void setProductCreditJcb(long productCreditJcb) {this.productCreditJcb = productCreditJcb;}

    public long getProductCreditOther() {return productCreditOther;}

    public void setProductCreditOther(long productCreditOther) {this.productCreditOther = productCreditOther;}

    public long getProductCreditAllCard() {return productCreditAllCard; }

    public void setProductCreditAllCard(long productCreditAllCard) { this.productCreditAllCard = productCreditAllCard; }

    public long getProductCreditItems() { return productCreditItems;}

    public void setProductCreditItems(long productCreditItems) { this.productCreditItems = productCreditItems; }

    public long getProductCreditPoints() { return productCreditPoints; }

    public void setProductCreditPoints(long productCreditPoints) { this.productCreditPoints = productCreditPoints; }

    public long getProductCreditRedeem() { return productCreditRedeem; }

    public void setProductCreditRedeem(long productCreditRedeem) { this.productCreditRedeem = productCreditRedeem; }

    public long getProductCreditCredit() { return productCreditCredit; }

    public void setProductCreditCredit(long productCreditCredit) { this.productCreditCredit = productCreditCredit; }

    public long getProductCreditTotal() { return productCreditTotal; }

    public void setProductCreditTotal(long productCreditTotal) { this.productCreditTotal = productCreditTotal; }

    public long getProductCreditSaleCreditTotal() { return productCreditSaleCreditTotal; }

    public void setProductCreditSaleCreditTotal(long productCreditSaleCreditTotal) { this.productCreditSaleCreditTotal = productCreditSaleCreditTotal; }

    public int getProductCreditSaleCreditTransCount() { return productCreditSaleCreditTransCount; }

    public void setProductCreditSaleCreditTransCount(int productCreditSaleCreditTransCount) { this.productCreditSaleCreditTransCount = productCreditSaleCreditTransCount; }

    public long getVoucherVisa() { return voucherVisa; }

    public void setVoucherVisa(long voucherVisa) { this.voucherVisa = voucherVisa; }

    public long getVoucherMastercard() { return voucherMastercard; }

    public void setVoucherMastercard(long voucherMastercard) { this.voucherMastercard = voucherMastercard; }

    public long getVoucherJcb() { return voucherJcb; }

    public void setVoucherJcb(long voucherJcb) { this.voucherJcb = voucherJcb; }

    public long getVoucherOther() { return voucherOther; }

    public void setVoucherOther(long voucherOther) { this.voucherOther = voucherOther; }

    public long getVoucherAllCard() { return voucherAllCard; }

    public void setVoucherAllCard(long voucherAllCard) { this.voucherAllCard = voucherAllCard; }

    public long getVoucherItems() { return voucherItems; }

    public void setVoucherItems(long voucherItems) { this.voucherItems = voucherItems; }

    public long getVoucherPoints() { return voucherPoints; }

    public void setVoucherPoints(long voucherPoints) { this.voucherPoints = voucherPoints; }

    public long getVoucherRedeem() { return voucherRedeem; }

    public void setVoucherRedeem(long voucherRedeem) { this.voucherRedeem = voucherRedeem; }

    public long getVoucherTotal() { return voucherTotal; }

    public void setVoucherTotal(long voucherTotal) { this.voucherTotal = voucherTotal; }

    public long getVoucherCreditVisa() { return voucherCreditVisa; }

    public void setVoucherCreditVisa(long voucherCreditVisa) { this.voucherCreditVisa = voucherCreditVisa; }

    public long getVoucherCreditMastercard() { return voucherCreditMastercard; }

    public void setVoucherCreditMastercard(long voucherCreditMastercard) { this.voucherCreditMastercard = voucherCreditMastercard; }

    public long getVoucherCreditJcb() { return voucherCreditJcb; }

    public void setVoucherCreditJcb(long voucherCreditJcb) { this.voucherCreditJcb = voucherCreditJcb; }

    public long getVoucherCreditOther() { return voucherCreditOther; }

    public void setVoucherCreditOther(long voucherCreditOther) { this.voucherCreditOther = voucherCreditOther; }

    public long getVoucherCreditAllCard() { return voucherCreditAllCard; }

    public void setVoucherCreditAllCard(long voucherCreditAllCard) { this.voucherCreditAllCard = voucherCreditAllCard; }

    public long getVoucherCreditItems() { return voucherCreditItems; }

    public void setVoucherCreditItems(long voucherCreditItems) { this.voucherCreditItems = voucherCreditItems; }

    public long getVoucherCreditPoints() { return voucherCreditPoints; }

    public void setVoucherCreditPoints(long voucherCreditPoints) { this.voucherCreditPoints = voucherCreditPoints; }

    public long getVoucherCreditRedeem() { return voucherCreditRedeem; }

    public void setVoucherCreditRedeem(long voucherCreditRedeem) { this.voucherCreditRedeem = voucherCreditRedeem; }

    public long getVoucherCreditCredit() { return voucherCreditCredit; }

    public void setVoucherCreditCredit(long voucherCreditCredit) { this.voucherCreditCredit = voucherCreditCredit; }

    public long getVoucherCreditTotal() { return voucherCreditTotal; }

    public void setVoucherCreditTotal(long voucherCreditTotal) { this.voucherCreditTotal = voucherCreditTotal; }

    public long getVoucherCreditSaleCreditTotal() { return voucherCreditSaleCreditTotal; }

    public void setVoucherCreditSaleCreditTotal(long voucherCreditSaleCreditTotal) { this.voucherCreditSaleCreditTotal = voucherCreditSaleCreditTotal; }

    public long getVoucherCreditSaleCreditTransCount() { return voucherCreditSaleCreditTransCount; }

    public void setVoucherCreditSaleCreditTransCount(int voucherCreditSaleCreditTransCount) { this.voucherCreditSaleCreditTransCount = voucherCreditSaleCreditTransCount; }

    public long getDiscountVisa() { return discountVisa; }

    public void setDiscountVisa(long discountVisa) { this.discountVisa = discountVisa; }

    public long getDiscountMastercard() { return discountMastercard; }

    public void setDiscountMastercard(long discountMastercard) { this.discountMastercard = discountMastercard; }

    public long getDiscountJcb() { return discountJcb; }

    public void setDiscountJcb(long discountJcb) { this.discountJcb = discountJcb; }

    public long getDiscountOther() { return discountOther; }

    public void setDiscountOther(long discountOther) { this.discountOther = discountOther; }

    public long getDiscountAllCard() { return discountAllCard; }

    public void setDiscountAllCard(long discountAllCard) { this.discountAllCard = discountAllCard; }

    public long getDiscountItems() { return discountItems; }

    public void setDiscountItems(long discountItems) { this.discountItems = discountItems; }

    public long getDiscountPoints() { return discountPoints; }

    public void setDiscountPoints(long discountPoints) { this.discountPoints = discountPoints; }

    public long getDiscountRedeem() { return discountRedeem; }

    public void setDiscountRedeem(long discountRedeem) { this.discountRedeem = discountRedeem; }

    public long getDiscountCredit() { return discountCredit; }

    public void setDiscountCredit(long discountCredit) { this.discountCredit = discountCredit; }

    public long getDiscountTotal() { return discountTotal; }

    public void setDiscountTotal(long discountTotal) { this.discountTotal = discountTotal; }

    public long getDiscountSaleCreditTotal() { return discountSaleCreditTotal; }

    public void setDiscountSaleCreditTotal(long discountSaleCreditTotal) { this.discountSaleCreditTotal = discountSaleCreditTotal; }

    public long getDiscountSaleCreditTransCount() { return discountSaleCreditTransCount; }

    public void setDiscountSaleCreditTransCount(int discountSaleCreditTransCount) { this.discountSaleCreditTransCount = discountSaleCreditTransCount; }

    public long getTotalSaleAllCards() { return totalSaleAllCards; }

    public void setTotalSaleAllCards(long totalSaleAllCards) { this.totalSaleAllCards = totalSaleAllCards; }

    public long getVisaSum(){
        return getProductVisa() + getProductCreditVisa() + getVoucherVisa() + getVoucherCreditVisa() + getDiscountVisa();
    }

    public long getMastercardSum(){
        return getProductMastercard() + getProductCreditMastercard() + getVoucherMastercard() + getVoucherCreditMastercard() + getDiscountMastercard();
    }

    public long getJcbSum(){
        return getProductJcb() + getProductCreditJcb() + getVoucherJcb() + getVoucherCreditJcb() + getDiscountJcb();
    }

    public long getOtherCardSum(){
        return getProductOther() + getProductCreditOther() + getVoucherOther() + getVoucherCreditOther() + getDiscountOther();
    }

    public long getAllCardsSum(){
        return getProductAllCard() + getProductCreditAllCard() + getVoucherAllCard() + getVoucherCreditAllCard() + getDiscountAllCard();
    }

    public long getItemSum(){
        return getProductItems() +getProductCreditItems() +
               getVoucherItems() +getVoucherCreditItems()+
               getDiscountItems();
    }

    public long getPointsSum() {
        return  getProductPoints() + getProductCreditPoints() +
                getVoucherPoints() + getVoucherCreditPoints()+
                getDiscountPoints();
    }

    public long getRedeemAmtSum () {
        return getProductRedeem() + getProductCreditRedeem() +
                getVoucherRedeem() + getVoucherCreditRedeem()+
                getDiscountRedeem();
    }

    public long getCreditSum () {
        return  getProductCreditCredit() + getVoucherCreditCredit()+
                getDiscountCredit();
    }

    public long getSaleCreditSum () {
        return  getProductCreditSaleCreditTotal() + getVoucherCreditSaleCreditTotal() + getDiscountSaleCreditTotal();
    }

    public long getSaleCreditTransCount () {
        return  getProductCreditSaleCreditTransCount() + getVoucherCreditSaleCreditTransCount() + getDiscountSaleCreditTransCount();
    }

    public long getTotalSum() {
        return   getProductTotal() + getProductCreditTotal() +
                    getVoucherTotal() + getVoucherCreditTotal()+
                    getDiscountTotal();
        }

    public  long getRedeemTotalSaleTransCount () { return redeemTotalSaleTransCount;}
    public void  setRedeemTotalSaleTransCount (long SaleTransCount) { redeemTotalSaleTransCount =  SaleTransCount;}
    public  long getRedeemTotalVoidTransCount () { return redeemTotalVoidTransCount;}
    public void  setRedeemTotalVoidTransCount (long VoidTransCount) { redeemTotalVoidTransCount =  VoidTransCount;}
}
