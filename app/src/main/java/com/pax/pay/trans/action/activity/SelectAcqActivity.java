/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-12-28
 * Module Author: caowb
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.trans.action.activity;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.expandablerecyclerview.BaseViewHolder;
import com.pax.edc.expandablerecyclerview.ExpandItemAnimator;
import com.pax.edc.expandablerecyclerview.ExpandableRecyclerAdapter;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.BaseActivityWithTickForAction;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.model.TransTotal;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.ToastUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import th.co.bkkps.utils.Log;

public class SelectAcqActivity extends BaseActivityWithTickForAction {

    private static final String ACQ_NAME = "acq_name";

    private CheckBox mCheck;
    private Button mSettle;
    private RecyclerView mRecyclerView;

    private ExpandableRecyclerAdapter<Map<String, String>> acquirerListAdapter;

    private List<String> checkedAcqs;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_selectacq_layout;
    }

    @Override
    protected void loadParam() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            checkedAcqs = bundle.getStringArrayList(Constants.ACQUIRER_NAME);
        }

        if (checkedAcqs == null)
            checkedAcqs = new ArrayList<>();

        List<Acquirer> acquirers = FinancialApplication.getAcqManager().findEnableAcquirersWithSortMode(true);
        ArrayList<Map<String, String>> myListArray = new ArrayList<>();

        for (Acquirer i : acquirers) {
            Map<String, String> map = new HashMap<>();
            map.put(ACQ_NAME, i.getName());
            myListArray.add(map);
        }
        acquirerListAdapter = new ExpandableRecyclerAdapter<>(SelectAcqActivity.this, R.layout.selectacq_item,
                new ExpandableRecyclerAdapter.ItemViewListener<Map<String, String>>() {
                    @Override
                    public BaseViewHolder<Map<String, String>> generate(View view) {
                        return new AcqSettleViewHolder(view);
                    }
                })
                .setDataBeanList(myListArray);
    }

    @Override
    protected String getTitleString() {
        return getString(R.string.settle_select_acquirer);
    }

    @Override
    protected void initViews() {
        mCheck = (CheckBox) findViewById(R.id.item_select_acq_check);
        mSettle = (Button) findViewById(R.id.select_acq_settle);
        mRecyclerView = (RecyclerView) findViewById(R.id.select_acq_list);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(acquirerListAdapter);
        mRecyclerView.setItemAnimator(new ExpandItemAnimator());

        confirmBtnChange();


    }

    @Override
    protected void setListeners() {
        mSettle.setOnClickListener(this);

        mRecyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                tickTimer.start();

                return false;
            }
        });

        mCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                tickTimer.start();

                //AET-39
                if (isChecked) {
                    List<Acquirer> acqList = FinancialApplication.getAcqManager().findEnableAcquirersWithSortMode(true);
                    for (Acquirer acquirer : acqList) {
                        if (!checkedAcqs.contains(acquirer.getName())) {
                            checkedAcqs.add(acquirer.getName());
                        }
                    }
                } else {
                    if (checkedAcqs.size() == FinancialApplication.getAcqManager().findEnableAcquirersWithSortMode(true).size()) {
                        checkedAcqs.clear();
                    }
                }
                confirmBtnChange();
                acquirerListAdapter.notifyDataSetChanged();
            }
        });

        //AET-39
        if (checkedAcqs != null && checkedAcqs.size() == FinancialApplication.getAcqManager().findEnableAcquirersWithSortMode(true).size()) {
            mCheck.setChecked(true);
        }
    }

    @Override
    protected boolean onKeyBackDown() {
        finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
        return true;
    }

    @Override
    public void onClickProtected(View v) {
        if (v.getId() == R.id.select_acq_settle) {
            finish2SettleAcq();
        }
    }

    private void finish2SettleAcq() {
        if (checkedAcqs.isEmpty()) {
            ToastUtils.showMessage(R.string.err_settle_select_acq);
            return;
        }
        finish(new ActionResult(TransResult.SUCC, checkedAcqs));
    }

    private class AcqSettleViewHolder extends BaseViewHolder<Map<String, String>> {

        private TextView textView;
        private CheckBox checkBox;
        private TextView acqName;
        private TextView merchantName;
        private TextView merchantId;
        private TextView terminalId;
        private TextView batchNo;
        private TextView saleSum;
        private TextView saleAmt;
        private TextView refundSum;
        private TextView refundAmt;
//        private TextView topupSum;
//        private TextView topupAmt;
        private TextView voidSaleSum;
        private TextView voidSaleAmt;
        private TextView voidRefundSum;
        private TextView voidRefundAmt;
//        private TextView voidTopUpSum;
//        private TextView voidTopUpAmt;
        private Button settleConfirm;

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

        public AcqSettleViewHolder(View itemView) {
            super(itemView, R.id.select_acq_item_header, R.id.expandable);
        }

        @Override
        protected void initView() {
            textView = (TextView) itemView.findViewById(R.id.expandable_toggle_button);
            checkBox = (CheckBox) itemView.findViewById(R.id.item_select_acq_check);

            acqName = (TextView) itemView.findViewById(R.id.settle_acquirer_name);
            merchantName = (TextView) itemView.findViewById(R.id.settle_merchant_name);
            merchantId = (TextView) itemView.findViewById(R.id.settle_merchant_id);
            terminalId = (TextView) itemView.findViewById(R.id.settle_terminal_id);
            batchNo = (TextView) itemView.findViewById(R.id.settle_batch_num);

            saleSum = (TextView) itemView.findViewById(R.id.sale_total_sum);
            saleAmt = (TextView) itemView.findViewById(R.id.sale_total_amount);
            refundSum = (TextView) itemView.findViewById(R.id.refund_total_sum);
            refundAmt = (TextView) itemView.findViewById(R.id.refund_total_amount);
//            topupSum = (TextView) itemView.findViewById(R.id.topup_total_sum);
//            topupAmt = (TextView) itemView.findViewById(R.id.topup_total_amount);

            voidSaleSum = (TextView) itemView.findViewById(R.id.void_sale_total_sum);
            voidSaleAmt = (TextView) itemView.findViewById(R.id.void_sale_total_amount);
            voidRefundSum = (TextView) itemView.findViewById(R.id.void_refund_total_sum);
            voidRefundAmt = (TextView) itemView.findViewById(R.id.void_refund_total_amount);
//            voidTopUpSum = (TextView) itemView.findViewById(R.id.void_topup_total_sum);
//            voidTopUpAmt = (TextView) itemView.findViewById(R.id.void_topup_total_amount);

            settleConfirm = (Button) itemView.findViewById(R.id.settle_confirm);

            transTotalLayout = (LinearLayout) itemView.findViewById(R.id.layout_trans_total);
            redeemKbankTotalLayout = (LinearLayout) itemView.findViewById(R.id.layout_redeem_kbank);

            redeemProductSum = (TextView) itemView.findViewById(R.id.redeem_product_total_sum);
            redeemProductCreditSum = (TextView) itemView.findViewById(R.id.redeem_product_credit_total_sum);
            redeemVoucherSum = (TextView) itemView.findViewById(R.id.redeem_voucher_total_sum);
            redeemVoucherCreditSum = (TextView) itemView.findViewById(R.id.redeem_voucher_credit_total_sum);
            redeemDiscountSum = (TextView) itemView.findViewById(R.id.redeem_discount_total_sum);
            redeemItemsSum = (TextView) itemView.findViewById(R.id.settle_redeem_items_total);
            redeemPointsSum = (TextView) itemView.findViewById(R.id.settle_redeem_points_total);
            redeemAmountSum = (TextView) itemView.findViewById(R.id.settle_redeem_amount_total);
            redeemCreditSum = (TextView) itemView.findViewById(R.id.settle_redeem_credit_total);
            redeemTotalSum = (TextView) itemView.findViewById(R.id.settle_redeem_total);
        }

        @Override
        protected void setListener() {
            settleConfirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkedAcqs.clear();
                    checkedAcqs.add(findAcquirer(getAdapterPosition()));
                    finish(new ActionResult(TransResult.SUCC, checkedAcqs));
                }
            });
        }

        @Override
        public void bindView(final Map<String, String> dataBean, final BaseViewHolder viewHolder, final int pos) {
            textView.setText(dataBean.get(ACQ_NAME));
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    tickTimer.start();
                    Log.d("SelectAcq", "onCheckedChanged  " + pos);
                    if (isChecked) {
                        if (!checkedAcqs.contains(dataBean.get(ACQ_NAME)))
                            checkedAcqs.add(dataBean.get(ACQ_NAME));
                    } else {
                        checkedAcqs.remove(dataBean.get(ACQ_NAME));
                    }
                    confirmBtnChange();
                    //AET-39
                    mCheck.setChecked(checkedAcqs.size() == FinancialApplication.getAcqManager().findEnableAcquirersWithSortMode(true).size());
                }
            });

            //AET-39
            checkBox.setChecked(checkedAcqs.contains(dataBean.get(ACQ_NAME)));

            if (viewHolder.getExpandView().getVisibility() == View.VISIBLE) {
                updateValueTable(pos);
            }
        }

        private void updateValueTable(final int position) {
            String acquirerName = findAcquirer(position);
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(acquirerName);
            TransTotal total = FinancialApplication.getTransTotalDbHelper().calcTotal(acquirer);

            acqName.setText(acquirer.getName());
            merchantName.setText(getString(R.string.settle_merchant_name));
            merchantId.setText(acquirer.getMerchantId());
            terminalId.setText(acquirer.getTerminalId());
            batchNo.setText(String.valueOf(acquirer.getCurrBatchNo()));

            if(acquirer.getName().equals(Constants.ACQ_REDEEM) || acquirer.getName().equals(Constants.ACQ_REDEEM_BDMS)){
                redeemKbankTotalLayout.setVisibility(View.VISIBLE);
                transTotalLayout.setVisibility(View.GONE);

                redeemProductSum.setText(String.valueOf(total.getTransRedeemKbankTotal().getProductAllCard()));
                redeemProductCreditSum.setText(String.valueOf(total.getTransRedeemKbankTotal().getProductCreditAllCard()));
                redeemVoucherSum.setText(String.valueOf(total.getTransRedeemKbankTotal().getVoucherAllCard()));
                redeemVoucherCreditSum.setText(String.valueOf(total.getTransRedeemKbankTotal().getVoucherCreditAllCard()));
                redeemDiscountSum.setText(String.valueOf(total.getTransRedeemKbankTotal().getDiscountAllCard()));
                redeemItemsSum.setText(String.valueOf(total.getTransRedeemKbankTotal().getItemSum()));
                redeemPointsSum.setText(String.valueOf(total.getTransRedeemKbankTotal().getPointsSum()));
                redeemAmountSum.setText(CurrencyConverter.convert(total.getTransRedeemKbankTotal().getRedeemAmtSum()));
                redeemCreditSum.setText(CurrencyConverter.convert(total.getTransRedeemKbankTotal().getCreditSum()));
                redeemTotalSum.setText(CurrencyConverter.convert(total.getTransRedeemKbankTotal().getTotalSum()));
            }else{
                transTotalLayout.setVisibility(View.VISIBLE);
                redeemKbankTotalLayout.setVisibility(View.GONE);

                String saleAmtStr = CurrencyConverter.convert(total.getSaleTotalAmt());
                //AET-18
                String refundAmtStr = CurrencyConverter.convert(0 - total.getRefundTotalAmt());
                String topupAmtStr = CurrencyConverter.convert(0 - total.getTopupTotalAmt());
                String voidSaleAmtStr = CurrencyConverter.convert(0 - total.getSaleVoidTotalAmt());
                String voidRefundAmtStr = CurrencyConverter.convert(0 - total.getRefundVoidTotalAmt());
                String voidTopUpAmtStr = CurrencyConverter.convert(total.getTopupVoidTotalAmt());

                saleSum.setText(String.valueOf(total.getSaleTotalNum()));
                saleAmt.setText(saleAmtStr);
                refundSum.setText(String.valueOf(total.getRefundTotalNum()));
                refundAmt.setText(refundAmtStr);
//                topupSum.setText(String.valueOf(total.getTopupTotalNum()));
//                topupAmt.setText(topupAmtStr);

                voidSaleSum.setText(String.valueOf(total.getSaleVoidTotalNum()));
                voidSaleAmt.setText(voidSaleAmtStr);
                voidRefundSum.setText(String.valueOf(total.getRefundVoidTotalNum()));
                voidRefundAmt.setText(voidRefundAmtStr);
//                voidTopUpSum.setText(String.valueOf(total.getTopupVoidTotalNum()));
//                voidTopUpAmt.setText(voidTopUpAmtStr);
            }
        }

        private String findAcquirer(int position) {
            return acquirerListAdapter.getDataBeanList().get(position).get(ACQ_NAME);
        }
    }

    // AET-114
    private void confirmBtnChange() {
        mSettle.setEnabled(!checkedAcqs.isEmpty());
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP){
            if(mSettle.isEnabled()){
                finish2SettleAcq();
            }
            return false;
        }else{
            return super.dispatchKeyEvent(event);
        }
    }
}
