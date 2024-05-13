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

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ATransaction;
import com.pax.abl.core.ActionResult;
import com.pax.abl.utils.PanUtils;
import com.pax.edc.R;
import com.pax.edc.expandablerecyclerview.BaseViewHolder;
import com.pax.edc.expandablerecyclerview.ExpandItemAnimator;
import com.pax.edc.expandablerecyclerview.ExpandableRecyclerAdapter;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.utils.Tools;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.db.TransDataDb;
import com.pax.pay.trans.AdjustTrans;
import com.pax.pay.trans.SaleVoidTrans;
import com.pax.pay.trans.WalletQrVoidTrans;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.TimeConverter;
import com.pax.pay.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import th.co.bkkps.amexapi.action.ActionAmexReprint;
import th.co.bkkps.dofinAPI.tran.action.ActionDolfinPrintTran;
import th.co.bkkps.scbapi.trans.action.ActionScbIppReprint;
import th.co.bkkps.scbapi.util.ScbUtil;

public class TransDetailFragment extends Fragment {

    protected Context context;

    protected RecyclerView mRecyclerView;
    private ExpandableRecyclerAdapter<TransData> mAdapter;
    protected List<TransData> mListItems = new ArrayList<>();

    protected View noTransRecord;

    protected RecordAsyncTask mRecordAsyncTask;

    protected boolean supportDoTrans = true;
    protected String acquirerName = "";

    public TransDetailFragment() {
        //do nothing
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_trans_detail_layout, container, false);
        this.context = getContext();

        noTransRecord = view.findViewById(R.id.no_trans_record);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.trans_list);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this.getActivity()));
        mRecyclerView.setItemAnimator(new ExpandItemAnimator());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mRecordAsyncTask != null) {
            mRecordAsyncTask.cancel(true);
            //ActivityStack.getInstance().pop(); // why need this
        }
        syncRecord();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mRecordAsyncTask != null) {
            mRecordAsyncTask.cancel(true);
        }
        mRecordAsyncTask = null;
    }

    public String getAcquirerName() {
        return acquirerName;
    }

    public void setAcquirerName(String acquirerName) {
        this.acquirerName = acquirerName;
    }

    public boolean isSupportDoTrans() {
        return supportDoTrans;
    }

    public void setSupportDoTrans(boolean supportDoTrans) {
        this.supportDoTrans = supportDoTrans;
    }

    protected class RecordAsyncTask extends AsyncTask<Void, Void, List<TransData>> {

        @Override
        @NonNull
        protected List<TransData> doInBackground(Void... params) {
            mListItems.clear();
            if(Utils.getString(R.string.acq_all_acquirer).equals(acquirerName)){
                List<ETransType> filter = new ArrayList<>();
                filter.add(ETransType.QR_VOID_WALLET);
                filter.add(ETransType.VOID);
                filter.add(ETransType.PROMPTPAY_VOID);
                filter.add(ETransType.QR_VOID);
                filter.add(ETransType.QR_VOID_KPLUS);
                filter.add(ETransType.QR_VOID_ALIPAY);
                filter.add(ETransType.QR_VOID_WECHAT);
                filter.add(ETransType.QR_VOID_CREDIT);
                filter.add(ETransType.GET_QR_KPLUS);
                filter.add(ETransType.GET_QR_ALIPAY);
                filter.add(ETransType.GET_QR_WECHAT);
                filter.add(ETransType.GET_QR_CREDIT);
                filter.add(ETransType.KBANK_REDEEM_VOID);
                filter.add(ETransType.KBANK_SMART_PAY_VOID);
                filter.add(ETransType.ADJUST);
                filter.add(ETransType.DOLFIN_INSTALMENT);
                filter.add(ETransType.DOLFIN_INSTALMENT_VOID);
                filter.add(ETransType.PREAUTH);
                filter.add(ETransType.PREAUTHORIZATION);
                filter.add(ETransType.PREAUTHORIZATION_CANCELLATION);
                //List<TransData> list = FinancialApplication.getTransDataDbHelper().findAllTransData(true, filter);
                List<TransData> list = FinancialApplication.getTransDataDbHelper().findAllTransDataByMerchant(true, filter);
                List<TransData> transDataList = new ArrayList<>();
                for(TransData transData :list){
                    if(transData.getAcquirer().isEnable()){
                        transDataList.add(transData);
                    }
                }

                mListItems.addAll(transDataList);

                Collections.reverse(mListItems);
            } else {
                Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(acquirerName);
                if (acquirer != null) {

//                List<TransData> list = FinancialApplication.getTransDataDbHelper().findAllTransData(acquirer, true);
                /* Modified by Cz for support QR PromptPay */
                    TransDataDb transDataDbHelper = FinancialApplication.getTransDataDbHelper();
                    List<ETransType> filter = new ArrayList<>();
                    List<TransData> list = new ArrayList<>();
                    filter.add(ETransType.QR_VOID_KPLUS);
                    filter.add(ETransType.QR_VOID_ALIPAY);
                    filter.add(ETransType.QR_VOID_WECHAT);
                    filter.add(ETransType.QR_VOID_CREDIT);
                    filter.add(ETransType.GET_QR_KPLUS);
                    filter.add(ETransType.GET_QR_ALIPAY);
                    filter.add(ETransType.GET_QR_WECHAT);
                    filter.add(ETransType.GET_QR_CREDIT);

                    if (acquirer.getName().equals(Constants.ACQ_QR_PROMPT)) {
                        List<TransData> qrSaleTransList = transDataDbHelper.findQRSaleTransData(acquirer, true, false, false);
                        List<TransData> qrSaleOfflineList = transDataDbHelper.findQRSaleTransData(acquirer, false, true, false);
                        qrSaleTransList.addAll(qrSaleOfflineList);
                        list.addAll(qrSaleTransList);
                    } else {
                        filter.add(ETransType.PROMPTPAY_VOID);
                        filter.add(ETransType.QR_VOID_WALLET);
                        filter.add(ETransType.VOID);
                        filter.add(ETransType.QR_VOID);
                        filter.add(ETransType.GET_QR_INFO);
                        filter.add(ETransType.GET_QR_WALLET);
                        filter.add(ETransType.KBANK_REDEEM_VOID);
                        filter.add(ETransType.KBANK_SMART_PAY_VOID);
                        filter.add(ETransType.ADJUST);
                        filter.add(ETransType.DOLFIN_INSTALMENT_VOID);
                        filter.add(ETransType.PREAUTH);
                        filter.add(ETransType.PREAUTHORIZATION);
                        filter.add(ETransType.PREAUTHORIZATION_CANCELLATION);
                        list = transDataDbHelper.findAllTransData(acquirer, true, filter,true);
                    }
                    mListItems.addAll(list);
                    Collections.reverse(mListItems);// 把list倒序，使最新一条记录在最上
                }
            }
            return mListItems;
        }

        @Override
        protected void onPostExecute(List<TransData> result) {
            super.onPostExecute(result);
            if (mListItems == null || mListItems.isEmpty()) {
                mRecyclerView.setVisibility(View.GONE);
                noTransRecord.setVisibility(View.VISIBLE);
                return;
            }
            mRecyclerView.setVisibility(View.VISIBLE);
            noTransRecord.setVisibility(View.GONE);
            if (mAdapter == null) {
                mAdapter = new ExpandableRecyclerAdapter<>(getActivity(), R.layout.trans_item,
                        new ExpandableRecyclerAdapter.ItemViewListener<TransData>() {
                            @Override
                            public BaseViewHolder<TransData> generate(View view) {
                                return new RecordViewHolder(view);
                            }
                        })
                        .setDataBeanList(mListItems);

            } else {
                mAdapter.setDataBeanList(mListItems);
            }
            mRecyclerView.setAdapter(mAdapter);
        }
    }

    protected class RecordViewHolder extends BaseViewHolder<TransData> implements View.OnClickListener {
        protected TextView transTypeTv;
        protected TextView transAmountTv;
        protected TextView transIssuerTv;
        protected TextView transNoTv;
        protected TextView transDateTv;
        protected TextView dccAmountTv;
        private TextView historyDetailState;
        private TextView historyDetailCardNo;
        private TextView historyDetailAuthCode;
        private TextView historyDetailRefNo;
        private TextView historyDetailQrRef;
        private TextView historyDetailWalletName;
        private TextView historyTextCardNo;
        private TextView historyTextAuthCode;
        private TextView historyTextRefNo;
        private TextView historyDetailDccExRate;
        private TextView historyDetailRedeemAmt;
        private TextView historyDetailRedeemPts;
        private TextView historyDetailRedeemQty;
        private TextView historyDetailRedeemCode;
        private View historyStateLayout;
        private View historyTransActionLayout;
        private View historyQrRefLayout;
        private View historyCardNoLayout;
        private View historyWalletNameLayout;
        private View historyRefNoLayout;
        protected View dccAmountTvLayout;
        private View historyDccExRateLayout;
        private View historyRedeemAmtLayout;
        private View historyRedeemPtsLayout;
        private View historyRedeemQtyLayout;
        private View historyRedeemCodeLayout;
        protected TextView stanNoTv;
        private Button voidBtn;
        private Button adjustBtn;
        private Button reprintBtn;

        RecordViewHolder(View itemView) {
            super(itemView, R.id.trans_item_header, R.id.expandable);
        }

        @Override
        protected void initView() {
            transTypeTv = (TextView) itemView.findViewById(R.id.trans_type_tv);
            transAmountTv = (TextView) itemView.findViewById(R.id.trans_amount_tv);
            transIssuerTv = (TextView) itemView.findViewById(R.id.issuer_type_tv);
            transNoTv = (TextView) itemView.findViewById(R.id.trans_no_tv);
            stanNoTv = (TextView) itemView.findViewById(R.id.stan_no_tv);
            transDateTv = (TextView) itemView.findViewById(R.id.trans_date_tv);
            dccAmountTv = (TextView) itemView.findViewById(R.id.dcc_amount_tv);

            historyDetailState = (TextView) itemView.findViewById(R.id.history_detail_state);
            historyDetailCardNo = (TextView) itemView.findViewById(R.id.history_detail_card_no);
            historyDetailWalletName = (TextView) itemView.findViewById(R.id.history_detail_wallet_name);
            historyDetailAuthCode = (TextView) itemView.findViewById(R.id.history_detail_auth_code);
            historyDetailRefNo = (TextView) itemView.findViewById(R.id.history_detail_ref_no);
            historyDetailQrRef = (TextView) itemView.findViewById(R.id.history_detail_qr_ref);
            historyTextCardNo = (TextView) itemView.findViewById(R.id.history_text_card_no);
            historyTextAuthCode = (TextView) itemView.findViewById(R.id.history_text_auth_code);
            historyTextRefNo = (TextView) itemView.findViewById(R.id.history_text_ref_no);
            historyDetailDccExRate = (TextView) itemView.findViewById(R.id.history_detail_dcc_ex_rate);
            historyDetailRedeemAmt = itemView.findViewById(R.id.history_redeem_amt);
            historyDetailRedeemPts = itemView.findViewById(R.id.history_redeem_points);
            historyDetailRedeemQty = itemView.findViewById(R.id.history_redeem_items);
            historyDetailRedeemCode = itemView.findViewById(R.id.history_redeem_code);

            historyTransActionLayout = itemView.findViewById(R.id.history_trans_action);
            historyStateLayout = itemView.findViewById(R.id.linear_state);
            historyCardNoLayout = itemView.findViewById(R.id.linear_card_no);
            historyWalletNameLayout = itemView.findViewById(R.id.linear_wallet_name);
            historyQrRefLayout = itemView.findViewById(R.id.linear_qr_ref);
            historyRefNoLayout = itemView.findViewById(R.id.linear_ref_no);
            dccAmountTvLayout = itemView.findViewById(R.id.dcc_amount_layout);
            historyDccExRateLayout = itemView.findViewById(R.id.linear_dcc_ex_rate);
            historyRedeemAmtLayout = itemView.findViewById(R.id.layout_history_redeem_amt);
            historyRedeemPtsLayout = itemView.findViewById(R.id.layout_history_redeem_points);
            historyRedeemQtyLayout = itemView.findViewById(R.id.layout_history_redeem_items);
            historyRedeemCodeLayout = itemView.findViewById(R.id.layout_history_redeem_code);

            voidBtn = (Button) itemView.findViewById(R.id.history_trans_action_void);
            adjustBtn = (Button) itemView.findViewById(R.id.history_trans_action_adjust);
            reprintBtn = (Button) itemView.findViewById(R.id.history_trans_action_reprint);
        }

        @Override
        protected void setListener() {
            //voidBtn.setOnClickListener(this);
            //adjustBtn.setOnClickListener(this);
            reprintBtn.setOnClickListener(this);
        }

        @Override
        public void bindView(final TransData dataBean, final BaseViewHolder viewHolder, final int pos) {
            ETransType transType = dataBean.getTransType();
            String acqName = dataBean.getAcquirer().getName();

            String transTypeValue;
            switch (transType) {
                case KBANK_REDEEM_DISCOUNT:
                    transTypeValue = "89999".equals(dataBean.getRedeemedDiscountType()) ? getString(R.string.trans_kbank_redeem_discount_var) : getString(R.string.trans_kbank_redeem_discount_fix);
                    break;
                case KBANK_SMART_PAY:
                case DOLFIN_INSTALMENT:
                    transTypeValue = Component.getTransByIPlanMode(dataBean);
                    break;
                default:
                    transTypeValue = (dataBean.getReferralStatus() != null && dataBean.getReferralStatus() != TransData.ReferralStatus.NORMAL) ?
                            transType.getTransName() + Utils.getString(R.string.receipt_amex_call_issuer) : transType.getTransName();//Fix to support referral trans
                    break;
            }
            if (!(ETransType.QR_INQUIRY == transType || ETransType.QR_INQUIRY_ALIPAY == transType || ETransType.QR_INQUIRY_WECHAT == transType)) {
                transTypeValue += dataBean.getTransState() == TransData.ETransStatus.VOIDED ? " (" + Utils.getString(R.string.trans_void) + ")" : "";
            }
            boolean isOfflineTransSend = ETransType.OFFLINE_TRANS_SEND == transType || ETransType.OFFLINE_TRANS_SEND == dataBean.getOrigTransType();
            if (isOfflineTransSend) {
                transTypeValue = transType.getTransName();
            } else if (dataBean.getOfflineSendState() != null && dataBean.getTipAmount() == null) {
                transTypeValue = Utils.getString(R.string.trans_offline) + " " + transTypeValue;
            }

            if (dataBean.getTransState() == TransData.ETransStatus.ADJUSTED) {
                transTypeValue += " (" + Utils.getString(R.string.receipt_state_adjust) + ")";
            }
            transTypeTv.setText(transTypeValue);

            //AET-18
            String amount;

            if (Constants.ACQ_REDEEM.equals(dataBean.getAcquirer().getName()) || Constants.ACQ_REDEEM_BDMS.equals(dataBean.getAcquirer().getName())) {
                amount = getRedeemedAmount(dataBean);
            } else {
                if (!dataBean.getTransType().isSymbolNegative() && dataBean.getTransState() != TransData.ETransStatus.VOIDED) {
                    transAmountTv.setTextColor(context.getResources().getColor(R.color.accent_amount));
                    if (Utils.parseLongSafe(dataBean.getAdjustedAmount(), 0) > 0) {
                        amount = CurrencyConverter.convert(Utils.parseLongSafe(dataBean.getAdjustedAmount(), 0), dataBean.getCurrency());
                    } else {
                        amount = CurrencyConverter.convert(Utils.parseLongSafe(dataBean.getAmount(), 0), dataBean.getCurrency());
                    }
                } else {
                    transAmountTv.setTextColor(context.getResources().getColor(R.color.accent));
                    if (Utils.parseLongSafe(dataBean.getAdjustedAmount(), 0) > 0) {
                        amount = CurrencyConverter.convert(-Utils.parseLongSafe(dataBean.getAdjustedAmount(), 0), dataBean.getCurrency());
                    } else {
                        amount = CurrencyConverter.convert(-Utils.parseLongSafe(dataBean.getAmount(), 0), dataBean.getCurrency()); //AET-18
                    }
                }
            }
            transAmountTv.setText(amount);

            if(ETransType.BPS_QR_SALE_INQUIRY == transType){
                transIssuerTv.setText(dataBean.getQrSaleState() == TransData.QrSaleState.QR_SEND_OFFLINE ? "("+ context.getString(R.string.state_qr_offline) +")" : "("+ context.getString(R.string.state_qr_online) +")");
            } else if (ETransType.QR_SALE_WALLET == transType
                    || ETransType.SALE_WALLET == transType
                    || ETransType.REFUND_WALLET == transType
                    || ETransType.QR_INQUIRY == transType
                    || ETransType.QR_INQUIRY_ALIPAY == transType
                    || ETransType.QR_INQUIRY_WECHAT == transType) {
                transIssuerTv.setText(dataBean.getTransState() == TransData.ETransStatus.VOIDED ? "("+ Utils.getString(R.string.trans_void) +")" : "");
            } else if (ETransType.QR_INQUIRY_CREDIT == transType) {
                transIssuerTv.setText(dataBean.getMerchantInfo() != null ? dataBean.getMerchantInfo().trim() : "");
            } else if (ETransType.QR_SALE_ALL_IN_ONE == transType || ETransType.STATUS_INQUIRY_ALL_IN_ONE == transType) {
                String type = Component.findQRType(dataBean);
                if (dataBean.getTransState() == TransData.ETransStatus.VOIDED) {
                    type += "(VOID)";
                }
                transIssuerTv.setText(type);
            }else if(ETransType.AMEX_INSTALMENT == transType){
                transIssuerTv.setText(Utils.getString(R.string.receipt_amex_instalment_issuer));
            } else if (ETransType.QR_MYPROMPT_SALE == transType) {
                transIssuerTv.setText(dataBean.getChannel());
            } else {
                transIssuerTv.setText(dataBean.getIssuer().getName());
            }

            transNoTv.setText(Component.getPaddedNumber(dataBean.getTraceNo(), 6));
            long stanNo = dataBean.getTransState() == TransData.ETransStatus.VOIDED ? dataBean.getVoidStanNo() : dataBean.getStanNo();
            stanNoTv.setText(Component.getPaddedNumber(stanNo, 6));

            String formattedDate;

            formattedDate = TimeConverter.convert(dataBean.getDateTime(), Constants.TIME_PATTERN_TRANS,
                    Constants.TIME_PATTERN_DISPLAY2);

            transDateTv.setText(formattedDate);

            if (dataBean.isDccRequired()) {
                if (!dataBean.getTransType().isSymbolNegative() && dataBean.getTransState() != TransData.ETransStatus.VOIDED) {
                    dccAmountTv.setTextColor(context.getResources().getColor(R.color.accent_amount));
                    if (Utils.parseLongSafe(dataBean.getAdjustedDccAmount(), 0) > 0) {
                        amount = CurrencyConverter.convert(Utils.parseLongSafe(dataBean.getAdjustedDccAmount(), 0), Tools.bytes2String(dataBean.getDccCurrencyCode()));
                    } else {
                        amount = CurrencyConverter.convert(Utils.parseLongSafe(dataBean.getDccAmount(), 0), Tools.bytes2String(dataBean.getDccCurrencyCode()));
                    }
                } else {
                    dccAmountTv.setTextColor(context.getResources().getColor(R.color.accent));
                    if (Utils.parseLongSafe(dataBean.getAdjustedDccAmount(), 0) > 0) {
                        amount = CurrencyConverter.convert(-Utils.parseLongSafe(dataBean.getAdjustedDccAmount(), 0), Tools.bytes2String(dataBean.getDccCurrencyCode()));
                    } else {
                        amount = CurrencyConverter.convert(-Utils.parseLongSafe(dataBean.getDccAmount(), 0), Tools.bytes2String(dataBean.getDccCurrencyCode())); //AET-18
                    }
                }
                dccAmountTv.setText(amount);
                dccAmountTvLayout.setVisibility(View.VISIBLE);
            }

            if (viewHolder.getExpandView().getVisibility() == View.VISIBLE) {
                updateExpandableLayout(dataBean);
            }
        }

        void updateExpandableLayout(TransData transData) {
            ETransType transType = transData.getTransType();
            String state = getState(transData);
            String authCode = transData.getAuthCode() != null ? transData.getAuthCode() : "";
            String refNo = transData.getRefNo() != null ? transData.getRefNo() : "";
            String qrRef2 = transData.getQrRef2() != null ? transData.getQrRef2() : "";
            String qrBuyerCode = transData.getQrBuyerCode() != null ? transData.getQrBuyerCode() : "";
            String walletName = transData.getWalletName() != null ? transData.getWalletName() : "";
            String dccExRate = transData.getDccConversionRate() != null ? String.format(Locale.getDefault(), "%.4f", Double.parseDouble(transData.getDccConversionRate()) / 10000) : "";
            String transId = transType == ETransType.QR_INQUIRY || transType == ETransType.QR_INQUIRY_CREDIT ?
                    (transData.getTxnNo() != null ? transData.getTxnNo().trim() : "") :
                    (transData.getWalletPartnerID() != null ? transData.getWalletPartnerID().trim() : "");

            if (transType == ETransType.QR_INQUIRY_CREDIT) {
                if (transData.getTransState() == TransData.ETransStatus.VOIDED) {
                    TransData voidTxn = FinancialApplication.getTransDataDbHelper().findTransData(transData.getTraceNo(), ETransType.QR_VOID_CREDIT);
                    transId = voidTxn.getTxnNo() != null ? voidTxn.getTxnNo().trim() : "";
                }
            }

            if (transType == ETransType.BPS_QR_SALE_INQUIRY || transType == ETransType.BPS_QR_INQUIRY_ID) {
                historyStateLayout.setVisibility(View.VISIBLE);
                historyDetailState.setText(state);
                historyTextAuthCode.setText(context.getString(R.string.history_detail_appr_code));
                historyDetailAuthCode.setText(authCode);
                historyQrRefLayout.setVisibility(View.VISIBLE);
                historyDetailQrRef.setText(qrRef2);
                historyRefNoLayout.setVisibility(View.VISIBLE);
                historyTextRefNo.setText(context.getString(R.string.history_detail_trans_id));
                historyDetailRefNo.setText(refNo);
                historyWalletNameLayout.setVisibility(View.GONE);
                historyCardNoLayout.setVisibility(View.GONE);
                historyDccExRateLayout.setVisibility(View.GONE);
                historyRedeemAmtLayout.setVisibility(View.GONE);
                historyRedeemPtsLayout.setVisibility(View.GONE);
                historyRedeemQtyLayout.setVisibility(View.GONE);
                historyRedeemCodeLayout.setVisibility(View.GONE);
            } else if (Constants.ACQ_WALLET.equals(transData.getAcquirer().getName())) {
                String cardNum = PanUtils.maskCardNo(qrBuyerCode, transData.getIssuer().getPanMaskPattern());
                historyTextCardNo.setText(context.getString(R.string.history_detail_wallet_card));
                historyDetailCardNo.setText(cardNum);
                historyTextRefNo.setText(context.getString(R.string.history_detail_ref_no));
                historyDetailRefNo.setText(refNo);
                historyWalletNameLayout.setVisibility(View.VISIBLE);
                historyDetailWalletName.setText(walletName);
                historyTextAuthCode.setText(context.getString(R.string.history_detail_appr_code));
                historyDetailAuthCode.setText(authCode);
                historyStateLayout.setVisibility(View.GONE);
                historyQrRefLayout.setVisibility(View.GONE);
                historyRefNoLayout.setVisibility(View.GONE);
                historyDccExRateLayout.setVisibility(View.GONE);
                historyRedeemAmtLayout.setVisibility(View.GONE);
                historyRedeemPtsLayout.setVisibility(View.GONE);
                historyRedeemQtyLayout.setVisibility(View.GONE);
                historyRedeemCodeLayout.setVisibility(View.GONE);
                if(cardNum != ""){
                    historyCardNoLayout.setVisibility(View.VISIBLE);
                    historyRefNoLayout.setVisibility(View.GONE);
                }else {
                    historyCardNoLayout.setVisibility(View.GONE);
                    historyRefNoLayout.setVisibility(View.VISIBLE);
                }
            } else if(transType == ETransType.QR_INQUIRY
                    || transType == ETransType.QR_INQUIRY_ALIPAY
                    || transType == ETransType.QR_INQUIRY_WECHAT
                    || transType == ETransType.QR_INQUIRY_CREDIT
                    || transType == ETransType.QR_VERIFY_PAY_SLIP){
                historyTextAuthCode.setText(context.getString(R.string.history_detail_appr_code));
                historyDetailAuthCode.setText(authCode);
                historyQrRefLayout.setVisibility(View.GONE);
                historyDetailQrRef.setText(qrRef2);
                historyRefNoLayout.setVisibility(View.VISIBLE);
                historyTextRefNo.setText(context.getString(R.string.history_detail_trans_id));
                historyDetailRefNo.setText(transId);
                historyStateLayout.setVisibility(View.GONE);
                historyWalletNameLayout.setVisibility(View.GONE);
                historyCardNoLayout.setVisibility(View.GONE);
                historyDccExRateLayout.setVisibility(View.GONE);
                historyRedeemAmtLayout.setVisibility(View.GONE);
                historyRedeemPtsLayout.setVisibility(View.GONE);
                historyRedeemQtyLayout.setVisibility(View.GONE);
                historyRedeemCodeLayout.setVisibility(View.GONE);
            } else if (Constants.ACQ_QRC.equals(transData.getAcquirer().getName())) {
                historyStateLayout.setVisibility(View.VISIBLE);
                historyDetailState.setText(state);
                historyCardNoLayout.setVisibility(View.VISIBLE);
                historyTextAuthCode.setText(context.getString(R.string.history_detail_auth_code));
                historyDetailAuthCode.setText(authCode);
                historyRefNoLayout.setVisibility(View.VISIBLE);
                historyTextRefNo.setText(context.getString(R.string.history_detail_ref_no));
                historyDetailRefNo.setText(refNo);
                historyCardNoLayout.setVisibility(View.GONE);
                historyQrRefLayout.setVisibility(View.GONE);
                historyWalletNameLayout.setVisibility(View.GONE);
                historyDccExRateLayout.setVisibility(View.GONE);
                historyRedeemAmtLayout.setVisibility(View.GONE);
                historyRedeemPtsLayout.setVisibility(View.GONE);
                historyRedeemQtyLayout.setVisibility(View.GONE);
                historyRedeemCodeLayout.setVisibility(View.GONE);
            } else if (Constants.ACQ_SCB_REDEEM.equals(transData.getAcquirer().getName())) {
                String cardNo = getCardNo(transData);
                historyStateLayout.setVisibility(View.VISIBLE);
                historyDetailState.setText(state);
                historyDccExRateLayout.setVisibility(View.GONE);
                historyCardNoLayout.setVisibility(View.VISIBLE);
                historyTextCardNo.setText(context.getString(R.string.history_detail_card_no));
                historyDetailCardNo.setText(cardNo);
                historyTextAuthCode.setText(context.getString(R.string.history_detail_auth_code));
                historyDetailAuthCode.setText(authCode);
                historyRefNoLayout.setVisibility(View.VISIBLE);
                historyTextRefNo.setText(context.getString(R.string.history_detail_ref_no));
                historyDetailRefNo.setText(refNo);
                historyQrRefLayout.setVisibility(View.GONE);
                HashMap<String, String> detail = ScbUtil.INSTANCE.getRedeemTransDetail(transData);
                historyDetailRedeemAmt.setText(detail.get(ScbUtil.DETAIL_REDEEM_AMT));
                historyDetailRedeemPts.setText(detail.get(ScbUtil.DETAIL_REDEEM_PTS));
                historyDetailRedeemQty.setText(detail.get(ScbUtil.DETAIL_REDEEM_QTY));
                historyDetailRedeemCode.setText(detail.get(ScbUtil.DETAIL_REDEEM_CODE));
                historyWalletNameLayout.setVisibility(View.VISIBLE);
                historyRedeemAmtLayout.setVisibility(View.VISIBLE);
                historyRedeemPtsLayout.setVisibility(View.VISIBLE);
                historyRedeemQtyLayout.setVisibility(View.VISIBLE);
                historyRedeemCodeLayout.setVisibility(View.VISIBLE);
            } else {
                String cardNo = getCardNo(transData);
                historyStateLayout.setVisibility(View.VISIBLE);
                historyDetailState.setText(state);
                historyDccExRateLayout.setVisibility(View.GONE);
                if (transData.isDccRequired()) {
                    historyDccExRateLayout.setVisibility(View.VISIBLE);
                    historyDetailDccExRate.setText(dccExRate);
                }
                historyCardNoLayout.setVisibility(View.VISIBLE);
                historyTextCardNo.setText(context.getString(R.string.history_detail_card_no));
                historyDetailCardNo.setText(cardNo);
                historyTextAuthCode.setText(context.getString(R.string.history_detail_auth_code));
                historyDetailAuthCode.setText(authCode);
                historyRefNoLayout.setVisibility(View.VISIBLE);
                historyTextRefNo.setText(context.getString(R.string.history_detail_ref_no));
                historyDetailRefNo.setText(refNo);
                historyQrRefLayout.setVisibility(View.GONE);
                historyWalletNameLayout.setVisibility(View.GONE);
                historyRedeemAmtLayout.setVisibility(View.GONE);
                historyRedeemPtsLayout.setVisibility(View.GONE);
                historyRedeemQtyLayout.setVisibility(View.GONE);
                historyRedeemCodeLayout.setVisibility(View.GONE);
            }

            historyTransActionLayout.setEnabled(supportDoTrans);

            if (transData.getOfflineSendState() != null &&
                    transData.getOfflineSendState() == TransData.OfflineStatus.OFFLINE_SENT) {
                //voidBtn.setEnabled(!transData.getTransState().equals(TransData.ETransStatus.VOIDED));
                //adjustBtn.setEnabled(transData.getTransType().isAdjustAllowed()
                //        && !transData.getTransState().equals(TransData.ETransStatus.VOIDED));
            } else if (transData.getTransState().equals(TransData.ETransStatus.NORMAL) &&
                    transType != ETransType.REFUND_WALLET) {
                //voidBtn.setEnabled(transData.getTransType().isVoidAllowed());
                //adjustBtn.setEnabled(transData.getTransType().isAdjustAllowed());
            } else if (transData.getTransState().equals(TransData.ETransStatus.ADJUSTED) &&
                    transData.getOfflineSendState() != null) {
                //voidBtn.setEnabled(transData.getTransType().isVoidAllowed()
                //        && transData.getOfflineSendState() == TransData.OfflineStatus.OFFLINE_SENT);
                //adjustBtn.setEnabled(transData.getTransType().isAdjustAllowed());
            } else {
                //voidBtn.setEnabled(false);
                //adjustBtn.setEnabled(false);
            }

            //Hide void and adjust button cuz we won't use them for now
            voidBtn.setVisibility(View.INVISIBLE);
            adjustBtn.setVisibility(View.INVISIBLE);
            voidBtn.setEnabled(false);
            adjustBtn.setEnabled(false);

            reprintBtn.setEnabled(supportDoTrans);
        }

        private String getRedeemedAmount(TransData transData) {
            String amount;
            if (transData.getTransState() == TransData.ETransStatus.VOIDED) {
                transAmountTv.setTextColor(context.getResources().getColor(R.color.accent));
                amount = CurrencyConverter.convert(0 - Utils.parseLongSafe(transData.getRedeemedTotal(), 0), transData.getCurrency());
            } else {
                transAmountTv.setTextColor(context.getResources().getColor(R.color.accent_amount));
                amount = CurrencyConverter.convert(Utils.parseLongSafe(transData.getRedeemedTotal(), 0), transData.getCurrency());
            }
            return amount;
        }

        private String getState(TransData transData) {
            TransData.ETransStatus temp = transData.getTransState();
            String state = "";
            if (transData.isOnlineTrans()) {
                if (temp.equals(TransData.ETransStatus.NORMAL)) {
                    state = context.getString(R.string.state_normal);
                } else if (temp.equals(TransData.ETransStatus.VOIDED)) {
                    state = context.getString(R.string.state_voided);
                } else if (temp.equals(TransData.ETransStatus.ADJUSTED)) {
                    state = getAdjustState(temp, transData.getOfflineSendState());
                }
            } else {
                state = getAdjustState(temp, transData.getOfflineSendState());
            }
            return state;
        }

        private String getAdjustState(TransData.ETransStatus transStatus, TransData.OfflineStatus offlineStatus) {
            String state;
            if (offlineStatus == TransData.OfflineStatus.OFFLINE_SENT) {
                state = context.getString(R.string.state_uploaded);
            } else {
                state = context.getString(R.string.state_not_sent);
            }

            if (transStatus.equals(TransData.ETransStatus.ADJUSTED)) {
                state += "(" + context.getString(R.string.state_adjusted) + ")";
            }
            if (transStatus.equals(TransData.ETransStatus.VOIDED)) {
                state += "(" + context.getString(R.string.state_voided) + ")";
            }
            return state;
        }

        private String getCardNo(TransData transData) {
            String cardNo;
            if (transData.getTransType() == ETransType.PREAUTH) {
                cardNo = transData.getPan();
            } else {
                if (!transData.isOnlineTrans()) {
                    cardNo = transData.getPan();
                } else {
                    cardNo = PanUtils.maskCardNo(transData.getPan(), transData.getIssuer().getPanMaskPattern());
                }
            }
            if(cardNo != null && transData.getTransType() != ETransType.REFUND_WALLET){
                cardNo += " /" + transData.getEnterMode().toString();
            }
            return cardNo;
        }

        @Override
        public void onClick(View v) {
            final int position = getAdapterPosition();
            final TransData transData = mListItems.get(position);
            switch (v.getId()) {
                case R.id.history_trans_action_reprint:
                    switch (transData.getAcquirer().getName()) {
                        case Constants.ACQ_DOLFIN:
                            doReprintDolfin(transData);
                            break;
                        case Constants.ACQ_SCB_IPP:
                        case Constants.ACQ_SCB_REDEEM:
                            doReprintScbIpp(transData);
                            break;
                        case Constants.ACQ_AMEX:
                            doReprintAmex(transData);
                            break;
                        default:
                            doReprint(transData);
                            break;
                    }
                    break;
                case R.id.history_trans_action_void:
                    if (Constants.ACQ_WALLET.equals(transData.getAcquirer().getName())) {
                        doVoidWalletAction(transData);
                    } else {
                        doVoidAction(transData);
                    }
                    break;
                case R.id.history_trans_action_adjust:
                    doAdjustAction(transData, position);
                    break;
                default:
                    break;
            }
        }

        private void doReprint(final TransData transData) {
            FinancialApplication.getApp().runInBackground(new Runnable() {
                @Override
                public void run() {
                    if (transData.getTransState() == TransData.ETransStatus.VOIDED) {
                        ETransType type;
                        switch (transData.getAcquirer().getName()) {
                            case Constants.ACQ_ALIPAY:
                            case Constants.ACQ_ALIPAY_B_SCAN_C:
                                type = ETransType.QR_VOID_ALIPAY;
                                break;
                            case Constants.ACQ_WECHAT:
                            case Constants.ACQ_WECHAT_B_SCAN_C:
                                type = ETransType.QR_VOID_WECHAT;
                                break;
                            case Constants.ACQ_KPLUS:
                                type = ETransType.QR_VOID_KPLUS;
                                break;
                            case Constants.ACQ_QR_CREDIT:
                                type = ETransType.QR_VOID_CREDIT;
                                break;
                            case Constants.ACQ_SMRTPAY:
                            case Constants.ACQ_SMRTPAY_BDMS:
                                type = ETransType.KBANK_SMART_PAY_VOID;
                                break;
                            case Constants.ACQ_REDEEM:
                            case Constants.ACQ_REDEEM_BDMS:
                                type = ETransType.KBANK_REDEEM_VOID;
                                break;
                            case Constants.ACQ_DOLFIN_INSTALMENT:
                                type = ETransType.DOLFIN_INSTALMENT_VOID;
                                break;
                            default:
                                type = ETransType.VOID;
                                break;

                        }
                        TransData trans = FinancialApplication.getTransDataDbHelper().findTransData(transData.getTraceNo(), type);
                        if (trans != null) {
                            transData.setEcrProcess(trans.isEcrProcess());
                            transData.setRefNo(trans.getRefNo());
                            transData.seteReceiptUploadStatus(trans.geteReceiptUploadStatus());
                            transData.setTxnNo(trans.getTxnNo());
                        }
                    }

                    if (Utils.parseLongSafe(transData.getAdjustedAmount(), 0) > 0) {
                        transData.setAmount(transData.getAdjustedAmount()); // for reprint, not impact txn
                    }

                    if (Utils.parseLongSafe(transData.getAdjustedDccAmount(), 0) > 0) {
                        transData.setDccAmount(transData.getAdjustedDccAmount()); // for reprint, not impact txn
                    }

                    Printer.printTransAgain(getActivity(), transData);
                }
            });
        }

        private void doVoidAction(TransData transData) {
            new SaleVoidTrans(getActivity(), transData, null).execute();
        }

        private void doVoidWalletAction(TransData transData) {
            new WalletQrVoidTrans(getActivity(), transData, null).execute();
        }

        private void doAdjustAction(TransData transData, final int position) {
            new AdjustTrans(getActivity(), transData, new ATransaction.TransEndListener() {
                @Override
                public void onEnd(ActionResult result) {
                    if (result.getRet() == TransResult.SUCC) {
                        FinancialApplication.getApp().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mAdapter.notifyItemChanged(position);
                            }
                        });
                    }
                }
            }).execute();
        }
        private void doReprintDolfin(final TransData transData) {
            ActionDolfinPrintTran actionDolfinPrintTran = new ActionDolfinPrintTran(new AAction.ActionStartListener() {
                @Override
                public void onStart(AAction action) {
                    ((ActionDolfinPrintTran) action).setParam(context,String.valueOf(transData.getTraceNo()));
                }
            });
            actionDolfinPrintTran.execute();
        }
        private void doReprintScbIpp(final TransData transData) {
            ActionScbIppReprint actionScbIppReprint = new ActionScbIppReprint(
                    action -> ((ActionScbIppReprint) action).setParam(context, transData.getTraceNo())
            );
            actionScbIppReprint.execute();
        }
        private void doReprintAmex(final TransData transData) {
            ActionAmexReprint actionAmexReprint = new ActionAmexReprint(
                    action -> ((ActionAmexReprint) action).setParam(context, transData.getTraceNo(), transData.getTraceNo(), -1)
            );
            actionAmexReprint.execute();
        }
    }
    protected void syncRecord() {
        mRecordAsyncTask = new RecordAsyncTask();
        mRecordAsyncTask.execute();
    }
}


