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
package com.pax.pay.record;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.pax.edc.R;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.model.TransRedeemKbankTotal;
import com.pax.pay.trans.model.TransTotal;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.Utils;

public class TransTotalFragment extends Fragment {

    private TextView saleNumberTv;
    private TextView saleAmountTv;
    private TextView refundNumberTv;
    private TextView refundAmountTv;
//    private TextView topupNumberTv;
//    private TextView topupAmountTv;
    private TextView voidedSaleNumberTv;
    private TextView voidedSaleAmountTv;
    private TextView voidedRefundNumberTv;
    private TextView voidedRefundAmountTv;
//    private TextView voidedTopUpNumberTv;
//    private TextView voidedTopUpAmountTv;

    private String acquirerName = "";

    private LinearLayout transTotalLayout;
    private LinearLayout redeemKbankTotalLayout;

    private TextView redeemProductSum;
    private TextView redeemProductCreditSum;
    private TextView redeemVoucherSum;
    private TextView redeemVoucherCreditSum;
    private TextView redeemDiscountSum;
    private TextView redeemItemsSum;
    private TextView redeemPointsSum;
    private TextView redeemAmountSum;
    private TextView redeemCreditSum;
    private TextView redeemTotalSum;

    public TransTotalFragment() {
        //do nothing
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_trans_total_layout, container, false);

        saleNumberTv = (TextView) view.findViewById(R.id.sale_total_sum);
        saleAmountTv = (TextView) view.findViewById(R.id.sale_total_amount);

        refundNumberTv = (TextView) view.findViewById(R.id.refund_total_sum);
        refundAmountTv = (TextView) view.findViewById(R.id.refund_total_amount);

//        topupNumberTv = (TextView) view.findViewById(R.id.topup_total_sum);
//        topupAmountTv = (TextView) view.findViewById(R.id.topup_total_amount);

        voidedSaleNumberTv = (TextView) view.findViewById(R.id.void_sale_total_sum);
        voidedSaleAmountTv = (TextView) view.findViewById(R.id.void_sale_total_amount);

        voidedRefundNumberTv = (TextView) view.findViewById(R.id.void_refund_total_sum);
        voidedRefundAmountTv = (TextView) view.findViewById(R.id.void_refund_total_amount);

//        voidedTopUpNumberTv = (TextView) view.findViewById(R.id.void_topup_total_sum);
//        voidedTopUpAmountTv = (TextView) view.findViewById(R.id.void_topup_total_amount);

        transTotalLayout = (LinearLayout) view.findViewById(R.id.layout_trans_total);
        redeemKbankTotalLayout = (LinearLayout) view.findViewById(R.id.layout_redeem_kbank);

        redeemProductSum = (TextView) view.findViewById(R.id.redeem_product_total_sum);
        redeemProductCreditSum = (TextView) view.findViewById(R.id.redeem_product_credit_total_sum);
        redeemVoucherSum = (TextView) view.findViewById(R.id.redeem_voucher_total_sum);
        redeemVoucherCreditSum = (TextView) view.findViewById(R.id.redeem_voucher_credit_total_sum);
        redeemDiscountSum = (TextView) view.findViewById(R.id.redeem_discount_total_sum);
        redeemItemsSum = (TextView) view.findViewById(R.id.settle_redeem_items_total);
        redeemPointsSum = (TextView) view.findViewById(R.id.settle_redeem_points_total);
        redeemAmountSum = (TextView) view.findViewById(R.id.settle_redeem_amount_total);
        redeemCreditSum = (TextView) view.findViewById(R.id.settle_redeem_credit_total);
        redeemTotalSum = (TextView) view.findViewById(R.id.settle_redeem_total);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        initTables();
    }

    private void initTables() {
        TransTotal total;

        boolean isRedeemKbank = true;
        if(Utils.getString(R.string.acq_all_acquirer).equals(acquirerName)){
            total = FinancialApplication.getTransTotalDbHelper().calcTotal(false);
        } else {
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(acquirerName);
            if (acquirer == null)
                return;

            total = FinancialApplication.getTransTotalDbHelper().calcTotal(acquirer,false);
            isRedeemKbank = acquirer.getName().equals(Constants.ACQ_REDEEM) || acquirer.getName().equals(Constants.ACQ_REDEEM_BDMS);
        }

        if(total == null)
            return;

        TransRedeemKbankTotal redeemKbankTotal = total.getTransRedeemKbankTotal() != null ? total.getTransRedeemKbankTotal() : null;
        if(Utils.getString(R.string.acq_all_acquirer).equals(acquirerName)){
            if(redeemKbankTotal == null){
                redeemKbankTotalLayout.setVisibility(View.GONE);
            }else{
                redeemKbankTotalLayout.setVisibility(View.VISIBLE);
            }
        }else if(isRedeemKbank){
            redeemKbankTotalLayout.setVisibility(View.VISIBLE);
            transTotalLayout.setVisibility(View.GONE);
        }else{
            transTotalLayout.setVisibility(View.VISIBLE);
            redeemKbankTotalLayout.setVisibility(View.GONE);
        }



        String saleAmt = CurrencyConverter.convert(total.getSaleTotalAmt());
        String topupAmt = CurrencyConverter.convert(total.getTopupTotalAmt());
        //AET-18
        String refundAmt = CurrencyConverter.convert(0 - total.getRefundTotalAmt());
        String voidSaleAmt = CurrencyConverter.convert(0 - total.getSaleVoidTotalAmt());
        String voidRefundAmt = CurrencyConverter.convert(0 - total.getRefundVoidTotalAmt());
        String voidTopUpAmt = CurrencyConverter.convert(total.getTopupVoidTotalAmt());


        saleNumberTv.setText(String.valueOf(total.getSaleTotalNum()));
        saleAmountTv.setText(saleAmt);

        refundNumberTv.setText(String.valueOf(total.getRefundTotalNum()));
        refundAmountTv.setText(refundAmt);

//        topupNumberTv.setText(String.valueOf(total.getTopupTotalNum()));
//        topupAmountTv.setText(topupAmt);

        voidedSaleNumberTv.setText(String.valueOf(total.getSaleVoidTotalNum()));
        voidedSaleAmountTv.setText(voidSaleAmt);

        voidedRefundNumberTv.setText(String.valueOf(total.getRefundVoidTotalNum()));
        voidedRefundAmountTv.setText(voidRefundAmt);

//        voidedTopUpNumberTv.setText(String.valueOf(total.getRefundVoidTotalNum()));
//        voidedTopUpAmountTv.setText(voidTopUpAmt);

        if(redeemKbankTotal != null){
            redeemProductSum.setText(String.valueOf(redeemKbankTotal.getProductAllCard()));
            redeemProductCreditSum.setText(String.valueOf(redeemKbankTotal.getProductCreditAllCard()));
            redeemVoucherSum.setText(String.valueOf(redeemKbankTotal.getVoucherAllCard()));
            redeemVoucherCreditSum.setText(String.valueOf(redeemKbankTotal.getVoucherCreditAllCard()));
            redeemDiscountSum.setText(String.valueOf(redeemKbankTotal.getDiscountAllCard()));
            redeemItemsSum.setText(String.valueOf(redeemKbankTotal.getItemSum()));
            redeemPointsSum.setText(String.valueOf(redeemKbankTotal.getPointsSum()));
            redeemAmountSum.setText(CurrencyConverter.convert(redeemKbankTotal.getRedeemAmtSum()));
            redeemCreditSum.setText(CurrencyConverter.convert(redeemKbankTotal.getCreditSum()));
            redeemTotalSum.setText(CurrencyConverter.convert(redeemKbankTotal.getTotalSum()));
        }

    }

    public String getAcquirerName() {
        return acquirerName;
    }

    public void setAcquirerName(String acquirerName) {
        this.acquirerName = acquirerName;
    }
}
