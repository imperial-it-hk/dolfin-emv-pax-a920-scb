/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-3-23
 * Module Author: Kim.L
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.db;

import androidx.annotation.NonNull;

import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.pax.eemv.utils.Tools;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.Issuer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.MerchantProfileManager;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.model.TransDccKbankTotal;
import com.pax.pay.trans.model.TransRedeemKbankTotal;
import com.pax.pay.trans.model.TransTotal;
import com.pax.pay.utils.Utils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import th.co.bkkps.utils.Log;

import static com.pax.pay.trans.model.TransData.ETransStatus.ADJUSTED;
import static com.pax.pay.trans.model.TransData.ETransStatus.NORMAL;
import static com.pax.pay.trans.model.TransData.ETransStatus.VOIDED;

/**
 * DAO for transaction total table
 */
public class TransTotalDb {

    private static final String TAG = "TransTotalDb";
    private static final List<TransData.ETransStatus> filter = new ArrayList<>();
    private static final List<TransData.ETransStatus> filter_Ex = new ArrayList<>();
    private static TransTotalDb instance;

    static {
        filter.add(NORMAL);
        filter.add(ADJUSTED);

        filter_Ex.add(NORMAL);
        filter_Ex.add(ADJUSTED);
    }

    private final BaseDbHelper dbHelper;
    private RuntimeExceptionDao<TransTotal, Integer> transDao = null;

    private TransTotalDb() {
        dbHelper = BaseDbHelper.getInstance();
    }

    /**
     * get the Singleton of the DB Helper
     *
     * @return the Singleton of DB helper
     */
    public static synchronized TransTotalDb getInstance() {
        if (instance == null) {
            instance = new TransTotalDb();
        }

        return instance;
    }

    /***************************************
     * Dao
     ******************************************/
    private RuntimeExceptionDao<TransTotal, Integer> getTotalDao() {
        if (transDao == null) {
            transDao = dbHelper.getRuntimeExceptionDao(TransTotal.class);
        }
        return transDao;
    }

    /*-----------------------------Trans Data------------------------*/

    /**
     * insert a transTotal
     */
    public boolean insertTransTotal(TransTotal transTotal) {
        try {
            RuntimeExceptionDao<TransTotal, Integer> dao = getTotalDao();
            dao.create(transTotal);
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "", e);
        }

        return false;
    }

    /**
     * update a transTotal
     */
    public boolean updateTransTotal(TransTotal transTotal) {
        try {
            RuntimeExceptionDao<TransTotal, Integer> dao = getTotalDao();
            dao.update(transTotal);
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "", e);
        }

        return false;
    }

    /**
     * find transTotal by id
     *
     * @param id id
     * @return data object
     */
    public TransTotal findTransTotal(int id) {
        try {
            RuntimeExceptionDao<TransTotal, Integer> dao = getTotalDao();
            return dao.queryForId(id);
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    /**
     * find transTotal by batch No
     *
     * @param acquirer the specific acquirer
     * @param batchNo  batch no
     * @return data object
     */
    public TransTotal findTransTotalByBatchNo(Acquirer acquirer, long batchNo) {
        try {
            RuntimeExceptionDao<TransTotal, Integer> dao = getTotalDao();
            QueryBuilder<TransTotal, Integer> queryBuilder = dao.queryBuilder();
            queryBuilder.where().eq(TransTotal.BATCHNO_FIELD_NAME, batchNo)
                    .and().eq(Acquirer.ID_FIELD_NAME, acquirer);
            return queryBuilder.queryForFirst();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }


    /**
     * find the last transTotal
     *
     * @return data object
     */
    public TransTotal findLastTransTotal(Acquirer acquirer, boolean isClosed) {
        try {
            List<TransTotal> list = findAllTransTotal(acquirer, isClosed);
            if (!list.isEmpty()) {
                return list.get(list.size() - 1);
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    /**
     * find transTotal by batch No
     *
     * @return transaction total list
     */
    @NonNull
    public List<TransTotal> findAllTransTotal(Acquirer acquirer, boolean isClosed) {
        try {
            RuntimeExceptionDao<TransTotal, Integer> dao = getTotalDao();
            List<Acquirer> acqs = FinancialApplication.getAcqManager().findEnableAcquirers();
            QueryBuilder<TransTotal, Integer> queryBuilder = dao.queryBuilder();
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            Where<TransTotal, Integer> where = queryBuilder.where().eq(TransTotal.IS_CLOSED_FIELD_NAME, isClosed);
            if (acquirer != null) {
                where.and().eq(Acquirer.ID_FIELD_NAME, acquirer);
            } else if (!acqs.isEmpty()) {
                where.and().in(Acquirer.ID_FIELD_NAME, acqs);
            }
            where.and().eq(TransTotal.MERCHANT_NAME_FILED_NAME, merchantName);

            return queryBuilder.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }

        return new ArrayList<>(0);
    }

    /**
     * delete TransTotal by id
     */
    public boolean deleteTransTotal(int id) {
        try {
            RuntimeExceptionDao<TransTotal, Integer> dao = getTotalDao();
            dao.deleteById(id);
            return true;
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }
        return false;
    }

    /**
     * delete transTotal by batch no
     */
    public boolean deleteTransTotalByBatchNo(Acquirer acquirer, long batchNo) {
        try {
            RuntimeExceptionDao<TransTotal, Integer> dao = getTotalDao();
            DeleteBuilder<TransTotal, Integer> deleteBuilder = dao.deleteBuilder();
            deleteBuilder.where().eq(TransTotal.BATCHNO_FIELD_NAME, batchNo)
                    .and().eq(Acquirer.ID_FIELD_NAME, acquirer);
            dao.delete(deleteBuilder.prepare());
            return true;
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return false;
    }

    /**
     * delete all transTotal
     */
    public boolean deleteAllTransTotal() {
        try {
            RuntimeExceptionDao<TransTotal, Integer> dao = getTotalDao();
            List<TransTotal> list = dao.queryForAll();
            dao.delete(list);
            return true;
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }
        return false;
    }

    /**
     * delete all transTotal by Acquirer
     */
    public boolean deleteAllTransTotal(Acquirer acquirer) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransTotal, Integer> dao = getTotalDao();
            DeleteBuilder<TransTotal, Integer> deleteBuilder = dao.deleteBuilder();
            deleteBuilder.where().eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransTotal.MERCHANT_NAME_FILED_NAME, merchantName);
            deleteBuilder.delete();
            return true;
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return false;
    }

    /**
     * calc total transactions of the acquirer
     *
     * @param acquirer the target acquirer
     * @return data object
     */
    public TransTotal calcTotal(Acquirer acquirer) {
        return calcTotal(acquirer, false);
    }

    public TransTotal calcTotal(Acquirer acquirer, boolean isOnRedeemSettle) {
        TransTotal total = new TransTotal();

        total.setBatchNo(acquirer.getCurrBatchNo()); //AET-208
        total.setMerchantName(MerchantProfileManager.INSTANCE.getCurrentMerchant());

        long[] obj;
        long[] obj1 = new long[2];
        long[] obj2 = new long[2];
        long[] obj3 = new long[2];
        obj1[0] = 0;
        obj1[1] = 0;
        obj2[0] = 0;
        obj2[1] = 0;
        obj3[0] = 0;
        obj3[1] = 0;
        if (Constants.ACQ_QR_PROMPT.equals(acquirer.getName())) {//Modified by Cz to support PromptPay transaction.
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_SALE_INQUIRY, filter, true, false);
            long[] tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_SALE_INQUIRY, filter, false, true);
            obj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_INQUIRY_ID, filter, true, false);
            long[] tempObj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_INQUIRY_ID, filter, false, true);
            total.setSaleTotalNum(obj[0] + tempObj[0] + obj1[0] + tempObj1[0]);
            total.setSaleTotalAmt(obj[1] + tempObj[1] + obj1[1] + tempObj1[1]);
        } else if (Constants.ACQ_QRC.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_SALE_ALL_IN_ONE, filter, true, false);
            long[] tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.STATUS_INQUIRY_ALL_IN_ONE, filter, true, false);
            total.setSaleTotalNum(obj[0] + tempObj[0]);
            total.setSaleTotalAmt(obj[1] + tempObj[1]);
        } else if (Constants.ACQ_WALLET.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_SALE_WALLET, filter, false, false);
            long[] tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.SALE_WALLET, filter, false, false);
            total.setSaleTotalNum(obj[0] + tempObj[0]);
            total.setSaleTotalAmt(obj[1] + tempObj[1]);
        } else if (Constants.ACQ_KPLUS.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY, filter, false, false);
            long[] tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_VERIFY_PAY_SLIP, filter, false, false);
            total.setSaleTotalNum(obj[0] + tempObj[0]);
            total.setSaleTotalAmt(obj[1] + tempObj[1]);
        } else if (Constants.ACQ_MY_PROMPT.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_MYPROMPT_SALE, filter, false, false);
            total.setSaleTotalNum(obj[0]);
            total.setSaleTotalAmt(obj[1]);
        } else if (Constants.ACQ_ALIPAY.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_ALIPAY, filter, false, false);
            total.setSaleTotalNum(obj[0]);
            total.setSaleTotalAmt(obj[1]);
        } else if (Constants.ACQ_ALIPAY_B_SCAN_C.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_ALIPAY_SCAN, filter, false, false);
            total.setSaleTotalNum(obj[0]);
            total.setSaleTotalAmt(obj[1]);
        } else if (Constants.ACQ_WECHAT.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_WECHAT, filter, false, false);
            total.setSaleTotalNum(obj[0]);
            total.setSaleTotalAmt(obj[1]);
        } else if (Constants.ACQ_WECHAT_B_SCAN_C.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_WECHAT_SCAN, filter, false, false);
            total.setSaleTotalNum(obj[0]);
            total.setSaleTotalAmt(obj[1]);
        } else if (Constants.ACQ_QR_CREDIT.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_CREDIT, filter, false, false);
            total.setSaleTotalNum(obj[0]);
            total.setSaleTotalAmt(obj[1]);
        } else if (Constants.ACQ_REDEEM.equals(acquirer.getName()) || Constants.ACQ_REDEEM_BDMS.equals(acquirer.getName())) {
            TransRedeemKbankTotal transRedeemKbankTotal = calTotalRedeemKbank(acquirer, isOnRedeemSettle);
            total.setTransRedeemKbankTotal(transRedeemKbankTotal);
            total.setSaleTotalNum(transRedeemKbankTotal.getSaleCreditTransCount());
            total.setSaleTotalAmt(transRedeemKbankTotal.getSaleCreditSum());
        } else if (Constants.ACQ_SMRTPAY.equals(acquirer.getName()) || Constants.ACQ_SMRTPAY_BDMS.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.KBANK_SMART_PAY, filter, false, false);
            total.setSaleTotalNum(obj[0]);
            total.setSaleTotalAmt(obj[1]);
        } else if (Constants.ACQ_AMEX_EPP.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.AMEX_INSTALMENT, filter, false, false);
            total.setSaleTotalNum(obj[0]);
            total.setSaleTotalAmt(obj[1]);
        } else if (Constants.ACQ_BAY_INSTALLMENT.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BAY_INSTALMENT, filter, false, false);
            total.setSaleTotalNum(obj[0]);
            total.setSaleTotalAmt(obj[1]);
        } else if (Constants.ACQ_DOLFIN_INSTALMENT.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.DOLFIN_INSTALMENT, filter, false, false);
            total.setSaleTotalNum(obj[0]);
            total.setSaleTotalAmt(obj[1]);
        } else {
            // 消费
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.SALE, filter, false, false);
            obj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.OFFLINE_TRANS_SEND, filter, false, false);
            obj2 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.DOLFIN_SALE, filter, false, false);
            obj3 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.SALE_COMPLETION, filter, false, false);

            total.setSaleTotalNum(obj[0] + obj1[0] + obj2[0] + obj3[0]);
            total.setSaleTotalAmt(obj[1] + obj1[1] + obj2[1] + obj3[1]);

            //small amount
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.SALE, filter, false, false, true);
            obj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.OFFLINE_TRANS_SEND, filter, false, false, true);
            obj2 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.DOLFIN_SALE, filter, false, true);
            obj3 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.SALE_COMPLETION, filter, false, false, true);
            total.setSaleTotalSmallAmtNum(obj[0] + obj1[0]  + obj2[0] + obj3[0]);
            total.setSaleTotalSmallAmt(obj[1] + obj1[1]  + obj2[1] + obj3[1]);

        }

        // 撤销
        obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.VOID, NORMAL);
        total.setVoidTotalNum(obj[0]);
        total.setVoidTotalAmt(obj[1]);


        if (Constants.ACQ_WALLET.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.REFUND_WALLET, filter, false, false);
        } else {
            // 退货
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.REFUND, NORMAL);
        }
        total.setRefundTotalNum(obj[0]);
        total.setRefundTotalAmt(obj[1]);

        obj1[0] = 0;
        obj1[1] = 0;
        obj2[0] = 0;
        obj2[1] = 0;
        //sale void total
        if (Constants.ACQ_WALLET.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_SALE_WALLET, VOIDED);
            obj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.SALE_WALLET, VOIDED);
        } else if (Constants.ACQ_QR_PROMPT.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_SALE_INQUIRY, VOIDED);
            obj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_INQUIRY_ID, VOIDED);
        } else if (Constants.ACQ_KPLUS.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY, VOIDED);
            obj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_VERIFY_PAY_SLIP, VOIDED);
        } else if (Constants.ACQ_ALIPAY.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_ALIPAY, VOIDED);
        }  else if (Constants.ACQ_ALIPAY_B_SCAN_C.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_ALIPAY_SCAN, VOIDED);
        } else if (Constants.ACQ_WECHAT.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_WECHAT, VOIDED);
        } else if (Constants.ACQ_WECHAT_B_SCAN_C.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_WECHAT_SCAN, VOIDED);
        } else if (Constants.ACQ_QR_CREDIT.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_CREDIT, VOIDED);
        } else if (Constants.ACQ_REDEEM.equals(acquirer.getName()) || Constants.ACQ_REDEEM_BDMS.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.KBANK_REDEEM_VOID, NORMAL);
            total.getTransRedeemKbankTotal().setRedeemTotalVoidTransCount(obj[0]);
        } else if (Constants.ACQ_SMRTPAY.equals(acquirer.getName()) || Constants.ACQ_SMRTPAY_BDMS.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.KBANK_SMART_PAY, VOIDED);
        } else if (Constants.ACQ_AMEX_EPP.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.AMEX_INSTALMENT, VOIDED);
        } else if (Constants.ACQ_BAY_INSTALLMENT.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BAY_INSTALMENT, VOIDED);
        } else if (Constants.ACQ_MY_PROMPT.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_MYPROMPT_SALE, VOIDED);
        } else if (Constants.ACQ_DOLFIN_INSTALMENT.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.DOLFIN_INSTALMENT, VOIDED);
        } else {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.SALE, VOIDED);
            obj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.OFFLINE_TRANS_SEND, VOIDED);
            obj2 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.DOLFIN_SALE, VOIDED);
            obj3 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.SALE_COMPLETION, VOIDED);

        }
        total.setSaleVoidTotalNum(obj[0] + obj1[0] + obj2[0] + obj3[0]);
        total.setSaleVoidTotalAmt(obj[1] + obj1[1] + obj2[1] + obj3[1]);

        obj1[0] = 0;
        obj1[1] = 0;
        obj2[0] = 0;
        obj2[1] = 0;
        obj3[0] = 0;
        obj3[1] = 0;
        //sale void small amount
        if (Constants.ACQ_WALLET.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_SALE_WALLET, VOIDED, true);
            obj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.SALE_WALLET, VOIDED, true);
        } else if (Constants.ACQ_QR_PROMPT.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_SALE_INQUIRY, VOIDED, true);
            obj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_INQUIRY_ID, VOIDED, true);
        } else if (Constants.ACQ_KPLUS.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY, VOIDED, true);
            obj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_VERIFY_PAY_SLIP, VOIDED, true);
        } else if (Constants.ACQ_ALIPAY.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_ALIPAY, VOIDED, true);
        }  else if (Constants.ACQ_ALIPAY_B_SCAN_C.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_ALIPAY_SCAN, VOIDED, true);
        }else if (Constants.ACQ_WECHAT.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_WECHAT, VOIDED, true);
        } else if (Constants.ACQ_WECHAT_B_SCAN_C.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_WECHAT_SCAN, VOIDED, true);
        } else if (Constants.ACQ_QR_CREDIT.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_CREDIT, VOIDED, true);
        } else if (Constants.ACQ_REDEEM.equals(acquirer.getName()) || Constants.ACQ_REDEEM_BDMS.equals(acquirer.getName())) {
            long[] tmpObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.KBANK_REDEEM_VOID, NORMAL, true);
            obj[0] += tmpObj[0];
            obj[1] += tmpObj[1];
            total.getTransRedeemKbankTotal().setRedeemTotalVoidTransCount(obj[0]);
        } else if (Constants.ACQ_SMRTPAY.equals(acquirer.getName()) || Constants.ACQ_SMRTPAY_BDMS.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.KBANK_SMART_PAY, VOIDED, true);
        } else if (Constants.ACQ_AMEX_EPP.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.AMEX_INSTALMENT, VOIDED, true);
        } else if (Constants.ACQ_DOLFIN_INSTALMENT.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.DOLFIN_INSTALMENT, VOIDED, true);
        } else {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.SALE, VOIDED, true);
            obj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.OFFLINE_TRANS_SEND, VOIDED, true);
            obj2 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.DOLFIN_SALE, VOIDED, true);
            obj3 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.SALE_COMPLETION, VOIDED, true);
        }
        total.setVoidTotalSmallAmtNum(obj[0] + obj1[0] + obj2[0] + obj3[0]);
        total.setVoidTotalSmallAmt(obj[1] + obj1[1] + obj2[1] + obj3[1]);

        if (Constants.ACQ_QRC.equals(acquirer.getName())) {
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_SALE_ALL_IN_ONE, VOIDED);
            long[] tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.STATUS_INQUIRY_ALL_IN_ONE, VOIDED);
            total.setSaleVoidTotalNum(obj[0] + tempObj[0]);
            total.setSaleVoidTotalAmt(obj[1] + tempObj[1]);
        }


        //refund void total
        obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.REFUND, VOIDED);
        total.setRefundVoidTotalNum(obj[0]);
        total.setRefundVoidTotalAmt(obj[1]);

        // 预授权
        obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.PREAUTHORIZATION, NORMAL);
        total.setAuthTotalNum(obj[0]);
        total.setAuthTotalAmt(obj[1]);

        // 脱机 AET-75
        obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.OFFLINE_TRANS_SEND, filter, false, false);
        obj3 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.SALE_COMPLETION, filter, false, false);
        total.setOfflineTotalNum(obj[0] + obj3[0]);
        total.setOfflineTotalAmt(obj[1] + obj3[1]);

        return total;
    }

    /**
     * calc total transactions of all acquirers
     *
     * @return data object
     */
    public TransTotal calcTotal(boolean isOnRedeemSettle) {
        TransTotal total = new TransTotal();
        total.setMerchantName(MerchantProfileManager.INSTANCE.getCurrentMerchant());
        List<Acquirer> allAcquirers = FinancialApplication.getAcqManager().findEnableAcquirers();

        long[] obj;
        long[] obj1 = new long[2];
        long[] obj2 = new long[2];
        long[] obj3 = new long[2];
        long saleTotalNum = 0, saleTotalAmt = 0;
        long voidTotalNum = 0, voidTotalAmt = 0;
        long refundTotalNum = 0, refundTotalAmt = 0;
        long saleVoidTotalNum = 0, saleVoidTotalAmt = 0;
        long refundVoidTotalNum = 0, refundVoidTotalAmt = 0;
        long authTotalNum = 0, authTotalAmt = 0;
        long offlineTotalNum = 0, offlineTotalAmt = 0;
        long topupTotalNum = 0, topupTotalAmt = 0;
        long topupVoidTotalNum = 0, topupVoidTotalAmt = 0;
        long saleSmallAmtTotalNum = 0, saleSmallAmtTotalAmt = 0;
        long voidSmallAmtTotalNum = 0, voidSmallAmtTotalAmt = 0;

        for (Acquirer acquirer : allAcquirers) {
            obj1[0] = 0;
            obj1[1] = 0;
            obj2[0] = 0;
            obj2[1] = 0;
            obj3[0] = 0;
            obj3[1] = 0;
            if(Constants.ACQ_DOLFIN.equalsIgnoreCase(acquirer.getName())){
                continue;
            }
            if (Constants.ACQ_QR_PROMPT.equals(acquirer.getName())) {//Modified by Cz to support PromptPay transaction.
                obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_SALE_INQUIRY, filter, true, false);
                long[] tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_SALE_INQUIRY, filter, false, true);
                obj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_INQUIRY_ID, filter, true, false);
                long[] tempObj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_INQUIRY_ID, filter, false, true);
                saleTotalNum += obj[0] + tempObj[0] + obj1[0] + tempObj1[0];
                saleTotalAmt += obj[1] + tempObj[1] + obj1[1] + tempObj1[1];
            } else if (Constants.ACQ_QRC.equals(acquirer.getName())) {
                obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_SALE_ALL_IN_ONE, filter, true, false);
                long[] tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.STATUS_INQUIRY_ALL_IN_ONE, filter, true, false);
                saleTotalNum += obj[0] + tempObj[0];
                saleTotalAmt += obj[1] + tempObj[1];
            } else if (Constants.ACQ_WALLET.equals(acquirer.getName())) {
                obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_SALE_WALLET, filter, false, false);
                long[] tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.SALE_WALLET, filter, false, false);
                saleTotalNum += obj[0] + tempObj[0];
                saleTotalAmt += obj[1] + tempObj[1];
            } else if (Constants.ACQ_KPLUS.equals(acquirer.getName())) {
                obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY, filter, false, false);
                long[] tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_VERIFY_PAY_SLIP, filter, false, false);
                saleTotalNum += obj[0] + tempObj[0];
                saleTotalAmt += obj[1] + tempObj[1];
            } else if (Constants.ACQ_ALIPAY.equals(acquirer.getName())) {
                obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_ALIPAY, filter, false, false);
                saleTotalNum += obj[0];
                saleTotalAmt += obj[1];
            } else if (Constants.ACQ_ALIPAY_B_SCAN_C.equals(acquirer.getName())) {
                obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_ALIPAY_SCAN, filter, false, false);
                saleTotalNum += obj[0];
                saleTotalAmt += obj[1];
            } else if (Constants.ACQ_WECHAT.equals(acquirer.getName())) {
                obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_WECHAT, filter, false, false);
                saleTotalNum += obj[0];
                saleTotalAmt += obj[1];
            }  else if (Constants.ACQ_WECHAT_B_SCAN_C.equals(acquirer.getName())) {
                obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_WECHAT_SCAN, filter, false, false);
                saleTotalNum += obj[0];
                saleTotalAmt += obj[1];
            } else if (Constants.ACQ_QR_CREDIT.equals(acquirer.getName())) {
                obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_CREDIT, filter, false, false);
                saleTotalNum += obj[0];
                saleTotalAmt += obj[1];
            } else if (Constants.ACQ_REDEEM.equals(acquirer.getName()) || Constants.ACQ_REDEEM_BDMS.equals(acquirer.getName())) {
//                TransRedeemKbankTotal transRedeemKbankTotal = calTotalRedeemKbank(acquirer);
//                total.setTransRedeemKbankTotal(transRedeemKbankTotal);
                TransRedeemKbankTotal transRedeemKbankTotal = calTotalRedeemKbank(acquirer, isOnRedeemSettle);
                total.setTransRedeemKbankTotal(transRedeemKbankTotal);
                total.setSaleTotalNum(transRedeemKbankTotal.getSaleCreditTransCount());
                total.setSaleTotalAmt(transRedeemKbankTotal.getSaleCreditSum());
            } else if (Constants.ACQ_SMRTPAY.equals(acquirer.getName()) || Constants.ACQ_SMRTPAY_BDMS.equals(acquirer.getName())) {
                obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.KBANK_SMART_PAY, filter, false, false);
                saleTotalNum += obj[0];
                saleTotalAmt += obj[1];
            } else if (Constants.ACQ_AMEX_EPP.equals(acquirer.getName())) {
                obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.AMEX_INSTALMENT, filter, false, false);
                saleTotalNum += obj[0];
                saleTotalAmt += obj[1];
            } else if (Constants.ACQ_BAY_INSTALLMENT.equals(acquirer.getName())) {
                obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BAY_INSTALMENT, filter, false, false);
                saleTotalNum += obj[0];
                saleTotalAmt += obj[1];
            } if (Constants.ACQ_DOLFIN_INSTALMENT.equals(acquirer.getName())) {
                obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.DOLFIN_INSTALMENT, filter, false, false);
                saleTotalNum += obj[0];
                saleTotalAmt += obj[1];
            } else {
                // 消费
                obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.SALE, filter, false, false);
                obj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.OFFLINE_TRANS_SEND, filter, false, false);
                obj2 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.DOLFIN_SALE, filter, false, false);
                obj3 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.SALE_COMPLETION, filter, false, false);
                saleTotalNum += obj[0] + obj1[0] + obj2[0] + obj3[0];
                saleTotalAmt += obj[1] + obj1[1] + obj2[1] + obj3[1];

                //small amount
                obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.SALE, filter, false, false, true);
                obj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.OFFLINE_TRANS_SEND, filter, false, false, true);
                obj2 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.DOLFIN_SALE, filter, false, false, true);
                obj3 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.SALE_COMPLETION, filter, false, false, true);
                saleSmallAmtTotalNum += obj[0] + obj1[0] + obj2[0] + obj3[0];
                saleSmallAmtTotalAmt += obj[1] + obj1[1] + obj2[1] + obj3[1];

            }

            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.VOID, NORMAL);
            voidTotalNum += obj[0];
            voidTotalAmt += obj[1];

            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.VOID, NORMAL, true);
            voidSmallAmtTotalNum += obj[0];
            voidSmallAmtTotalAmt += obj[1];

            if (Constants.ACQ_WALLET.equals(acquirer.getName())) {
                obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.REFUND_WALLET, filter, false, false);
            } else {
                // 退货
                obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.REFUND, NORMAL);
            }
            refundTotalNum += obj[0];
            refundTotalAmt += obj[1];

            obj1[0] = 0;
            obj1[1] = 0;
            obj2[0] = 0;
            obj2[1] = 0;
            obj3[0] = 0;
            obj3[1] = 0;
            //sale void total
            if (Constants.ACQ_WALLET.equals(acquirer.getName())) {
                obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_SALE_WALLET, VOIDED);
                obj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.SALE_WALLET, VOIDED);
            } else if (Constants.ACQ_QR_PROMPT.equals(acquirer.getName())) {
                obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_SALE_INQUIRY, VOIDED);
                obj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_INQUIRY_ID, VOIDED);
            } else if (Constants.ACQ_KPLUS.equals(acquirer.getName())) {
                obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY, VOIDED);
                obj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_VERIFY_PAY_SLIP, VOIDED);
            } else if (Constants.ACQ_ALIPAY.equals(acquirer.getName())) {
                obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_ALIPAY, VOIDED);
            } else if (Constants.ACQ_ALIPAY_B_SCAN_C.equals(acquirer.getName())) {
                obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_ALIPAY_SCAN, VOIDED);
            } else if (Constants.ACQ_WECHAT.equals(acquirer.getName())) {
                obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_WECHAT, VOIDED);
            } else if (Constants.ACQ_WECHAT_B_SCAN_C.equals(acquirer.getName())) {
                obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_WECHAT_SCAN, VOIDED);
            } else if (Constants.ACQ_QR_CREDIT.equals(acquirer.getName())) {
                obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_CREDIT, VOIDED);
            } else if (Constants.ACQ_REDEEM.equals(acquirer.getName()) || Constants.ACQ_REDEEM_BDMS.equals(acquirer.getName())) {
                obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.KBANK_REDEEM_VOID, NORMAL);
                total.getTransRedeemKbankTotal().setRedeemTotalVoidTransCount(obj[0]);

                long[] tmpObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.KBANK_REDEEM_VOID, NORMAL, true);
                obj[0] += tmpObj[0];
                obj[1] += tmpObj[1];
                total.getTransRedeemKbankTotal().setRedeemTotalVoidTransCount(obj[0]);
            } else if (Constants.ACQ_SMRTPAY.equals(acquirer.getName()) || Constants.ACQ_SMRTPAY_BDMS.equals(acquirer.getName())) {
                obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.KBANK_SMART_PAY, VOIDED);
            } else if (Constants.ACQ_AMEX_EPP.equals(acquirer.getName())) {
                obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.AMEX_INSTALMENT, VOIDED);
            } else if (Constants.ACQ_BAY_INSTALLMENT.equals(acquirer.getName())) {
                obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BAY_INSTALMENT, VOIDED);
            } else if (Constants.ACQ_DOLFIN_INSTALMENT.equals(acquirer.getName())) {
                obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.DOLFIN_INSTALMENT, VOIDED);
            } else {
                obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.SALE, VOIDED);
                obj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.OFFLINE_TRANS_SEND, VOIDED);
                obj2 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.DOLFIN_SALE, VOIDED);
                obj3 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.SALE_COMPLETION, VOIDED);
            }
            saleVoidTotalNum += obj[0] + obj1[0] + obj2[0] + obj3[0];
            saleVoidTotalAmt += obj[1] + obj1[1] + obj2[1] + obj3[1];

            if (Constants.ACQ_QRC.equals(acquirer.getName())) {
                obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_SALE_ALL_IN_ONE, VOIDED);
                long[] tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.STATUS_INQUIRY_ALL_IN_ONE, VOIDED);
                saleVoidTotalNum += obj[0] + tempObj[0];
                saleVoidTotalAmt += obj[1] + tempObj[1];
            }

            //refund void total
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.REFUND, VOIDED);
            refundVoidTotalNum += obj[0];
            refundVoidTotalAmt += obj[1];

            // 预授权
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.PREAUTHORIZATION, NORMAL);
            authTotalNum += obj[0];
            authTotalAmt += obj[1];

            // 脱机 AET-75
            obj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.OFFLINE_TRANS_SEND, filter, false, false);
            obj3 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.SALE_COMPLETION, filter, false, false);
            offlineTotalNum += obj[0] + obj3[0];
            offlineTotalAmt += obj[1] + obj3[1];


            topupTotalNum += obj[0];
            topupTotalAmt += obj[1];

        }

        long redeemCreditSaleTransCount = (total.getTransRedeemKbankTotal() == null) ? 0 : total.getTransRedeemKbankTotal().getItemSum();
        long redeemCreditSaleSum = (total.getTransRedeemKbankTotal() == null) ? 0 : total.getTransRedeemKbankTotal().getSaleCreditSum();

        total.setSaleTotalNum(saleTotalNum + redeemCreditSaleTransCount);
        total.setSaleTotalAmt(saleTotalAmt + redeemCreditSaleSum);
        total.setVoidTotalNum(voidTotalNum);
        total.setVoidTotalAmt(voidTotalAmt);
        total.setRefundTotalNum(refundTotalNum);
        total.setRefundTotalAmt(refundTotalAmt);
        total.setSaleVoidTotalNum(saleVoidTotalNum);
        total.setSaleVoidTotalAmt(saleVoidTotalAmt);
        total.setRefundVoidTotalNum(refundVoidTotalNum);
        total.setRefundVoidTotalAmt(refundVoidTotalAmt);
        total.setAuthTotalNum(authTotalNum);
        total.setAuthTotalAmt(authTotalAmt);
        total.setOfflineTotalNum(offlineTotalNum);
        total.setOfflineTotalAmt(offlineTotalAmt);
        total.setTopupTotalNum(topupTotalNum);
        total.setTopupTotalAmt(topupTotalAmt);
        total.setTopupVoidTotalNum(topupVoidTotalNum);
        total.setTopupVoidTotalAmt(topupVoidTotalAmt);

        return total;
    }


    public TransTotal sumTransTotal(List<TransTotal> total) {
        long saleTotalNum = 0, saleTotalAmt = 0;
        long voidTotalNum = 0, voidTotalAmt = 0;
        long refundTotalNum = 0, refundTotalAmt = 0;
        long saleVoidTotalNum = 0, saleVoidTotalAmt = 0;
        long refundVoidTotalNum = 0, refundVoidTotalAmt = 0;
        long authTotalNum = 0, authTotalAmt = 0;
        long offlineTotalNum = 0, offlineTotalAmt = 0;
        long topupTotalNum = 0, topupTotalAmt = 0;
        long topupVoidTotalNum = 0, topupVoidTotalAmt = 0;

        TransTotal grandTotal = null;
        if (total != null) {
            for (TransTotal other : total) {
                saleTotalNum = saleTotalNum + other.getSaleTotalNum();
                saleTotalAmt = saleTotalAmt + other.getSaleTotalAmt();
                voidTotalNum = voidTotalNum + other.getVoidTotalNum();
                voidTotalAmt = voidTotalAmt + other.getVoidTotalAmt();
                refundTotalNum = refundTotalNum + other.getRefundTotalNum();
                refundTotalAmt = refundTotalAmt + other.getRefundTotalAmt();
                saleVoidTotalNum = saleVoidTotalNum + other.getSaleVoidTotalNum();
                saleVoidTotalAmt = saleVoidTotalAmt + other.getSaleVoidTotalAmt();
                refundVoidTotalNum = refundVoidTotalNum + other.getRefundVoidTotalNum();
                refundVoidTotalAmt = refundVoidTotalAmt + other.getRefundVoidTotalAmt();
                authTotalNum = authTotalNum + other.getAuthTotalNum();
                authTotalAmt = authTotalAmt + other.getAuthTotalAmt();
                offlineTotalNum = offlineTotalNum + other.getOfflineTotalNum();
                offlineTotalAmt = offlineTotalAmt + other.getOfflineTotalAmt();
                topupTotalNum = topupTotalNum + other.getTopupTotalNum();
                topupTotalAmt = topupTotalAmt + other.getTopupTotalAmt();
                topupVoidTotalNum = topupVoidTotalNum + other.getTopupVoidTotalNum();
                topupVoidTotalAmt = topupVoidTotalAmt + other.getTopupVoidTotalAmt();
            }
            grandTotal = new TransTotal();
            grandTotal.setMerchantName(MerchantProfileManager.INSTANCE.getCurrentMerchant());
            grandTotal.setSaleTotalNum(saleTotalNum);
            grandTotal.setSaleTotalAmt(saleTotalAmt);
            grandTotal.setVoidTotalNum(voidTotalNum);
            grandTotal.setVoidTotalAmt(voidTotalAmt);
            grandTotal.setRefundTotalNum(refundTotalNum);
            grandTotal.setRefundTotalAmt(refundTotalAmt);
            grandTotal.setSaleVoidTotalNum(saleVoidTotalNum);
            grandTotal.setSaleVoidTotalAmt(saleVoidTotalAmt);
            grandTotal.setRefundVoidTotalNum(refundVoidTotalNum);
            grandTotal.setRefundVoidTotalAmt(refundVoidTotalAmt);
            grandTotal.setAuthTotalNum(authTotalNum);
            grandTotal.setAuthTotalAmt(authTotalAmt);
            grandTotal.setOfflineTotalNum(offlineTotalNum);
            grandTotal.setOfflineTotalAmt(offlineTotalAmt);
            grandTotal.setTopupTotalNum(topupTotalNum);
            grandTotal.setTopupTotalAmt(topupTotalAmt);
            grandTotal.setTopupVoidTotalNum(topupVoidTotalNum);
            grandTotal.setTopupVoidTotalAmt(topupVoidTotalAmt);
        }
        return grandTotal;
    }

    public TransRedeemKbankTotal calTotalRedeemKbank(Acquirer acquirer, boolean isSettlement) {
        TransRedeemKbankTotal transRedeemKbankTotal = new TransRedeemKbankTotal();
        long[] objRedeem = new long[]{0, 0, 0, 0, 0, 0};
        long[] objRedeemSaleTrans = new long[]{0, 0, 0, 0, 0, 0};
        //List<String> listIssuers = Arrays.asList(Constants.ISSUER_VISA,Constants.ISSUER_JCB,Constants.ISSUER_MASTER);
        long redeemItems = 0, redeemPoints = 0, redeemAmt = 0, redeemCredit = 0, redeemAllCards = 0, redeemTotal = 0, redeemTotalAllCards = 0, redeemSaleCredit = 0, redeemSaleCreditTrans = 0;
        List<Issuer> issuerList = new ArrayList<>();
        List<Issuer> issuerSaleTransList = new ArrayList<>();

        List<Issuer> issuerTmpList = FinancialApplication.getAcqManager().findIssuerByBrand(Constants.ISSUER_VISA);
        List<Issuer> issuersVISA = issuerTmpList != null && !issuerTmpList.isEmpty() ? issuerTmpList : null;
        issuerTmpList = FinancialApplication.getAcqManager().findIssuerByBrand(Constants.ISSUER_MASTER);
        List<Issuer> issuersMASTER = issuerTmpList != null && !issuerTmpList.isEmpty() ? issuerTmpList : null;
        issuerTmpList = FinancialApplication.getAcqManager().findIssuerByBrand(Constants.ISSUER_JCB);
        List<Issuer> issuersJCB = issuerTmpList != null && !issuerTmpList.isEmpty() ? issuerTmpList : null;
        issuerList.addAll(issuersVISA);
        issuerList.addAll(issuersMASTER);
        issuerList.addAll(issuersJCB);


        //PRODUCT - VISA
        objRedeem = FinancialApplication.getTransDataDbHelper().countSumOfRedeemKbankByIssuer(acquirer, ETransType.KBANK_REDEEM_PRODUCT, filter, issuersVISA, null);
        transRedeemKbankTotal.setProductVisa(objRedeem[0]);
        redeemItems = objRedeem[1];
        redeemPoints = objRedeem[2];
        redeemAmt = objRedeem[3];
        redeemTotal = objRedeem[5];
        redeemAllCards = objRedeem[0];


        //PRODUCT - MASTERCARD
        objRedeem = FinancialApplication.getTransDataDbHelper().countSumOfRedeemKbankByIssuer(acquirer, ETransType.KBANK_REDEEM_PRODUCT, filter, issuersMASTER, null);
        transRedeemKbankTotal.setProductMastercard(objRedeem[0]);
        redeemItems += objRedeem[1];
        redeemPoints += objRedeem[2];
        redeemAmt += objRedeem[3];
        redeemTotal += objRedeem[5];
        redeemAllCards += objRedeem[0];

        //PRODUCT - JCB
        objRedeem = FinancialApplication.getTransDataDbHelper().countSumOfRedeemKbankByIssuer(acquirer, ETransType.KBANK_REDEEM_PRODUCT, filter, issuersJCB, null);
        transRedeemKbankTotal.setProductJcb(objRedeem[0]);
        redeemItems += objRedeem[1];
        redeemPoints += objRedeem[2];
        redeemAmt += objRedeem[3];
        redeemTotal += objRedeem[5];
        redeemAllCards += objRedeem[0];

        //PRODUCT - OTHER
        objRedeem = FinancialApplication.getTransDataDbHelper().countSumOfRedeemKbankByIssuer(acquirer, ETransType.KBANK_REDEEM_PRODUCT, filter, null, issuerList);
        transRedeemKbankTotal.setProductOther(objRedeem[0]);
        redeemItems += objRedeem[1];
        redeemPoints += objRedeem[2];
        redeemAmt += objRedeem[3];
        redeemTotal += objRedeem[5];
        redeemAllCards += objRedeem[0];

        // EXTENDED VOIDED PRODUCT SALE
        objRedeemSaleTrans = FinancialApplication.getTransDataDbHelper().countSumOfRedeemKbank(acquirer, ETransType.KBANK_REDEEM_PRODUCT, filter_Ex);

        //transRedeemKbankTotal.setProductAllCard(redeemAllCards + objRedeemSaleTrans[0]);
        transRedeemKbankTotal.setProductAllCard(redeemAllCards);
        transRedeemKbankTotal.setProductItems(redeemItems);
        transRedeemKbankTotal.setProductPoints(redeemPoints);
        transRedeemKbankTotal.setProductRedeem(redeemAmt);
        transRedeemKbankTotal.setProductTotal(redeemTotal);
        redeemTotalAllCards = redeemAllCards + ((isSettlement) ? 0 : objRedeemSaleTrans[0]);


        //PRODUCT+CREDIT - VISA
        objRedeem = FinancialApplication.getTransDataDbHelper().countSumOfRedeemKbankByIssuer(acquirer, ETransType.KBANK_REDEEM_PRODUCT_CREDIT, filter, issuersVISA, null);
        transRedeemKbankTotal.setProductCreditVisa(objRedeem[0]);
        redeemItems = objRedeem[1];
        redeemPoints = objRedeem[2];
        redeemAmt = objRedeem[3];
        redeemCredit = objRedeem[4];
        redeemTotal = objRedeem[5];
        redeemAllCards = objRedeem[0];

        //PRODUCT+CREDIT - MASTERCARD
        objRedeem = FinancialApplication.getTransDataDbHelper().countSumOfRedeemKbankByIssuer(acquirer, ETransType.KBANK_REDEEM_PRODUCT_CREDIT, filter, issuersMASTER, null);
        transRedeemKbankTotal.setProductCreditMastercard(objRedeem[0]);
        redeemItems += objRedeem[1];
        redeemPoints += objRedeem[2];
        redeemAmt += objRedeem[3];
        redeemCredit += objRedeem[4];
        redeemTotal += objRedeem[5];
        redeemAllCards += objRedeem[0];

        //PRODUCT+CREDIT - JCB
        objRedeem = FinancialApplication.getTransDataDbHelper().countSumOfRedeemKbankByIssuer(acquirer, ETransType.KBANK_REDEEM_PRODUCT_CREDIT, filter, issuersJCB, null);
        transRedeemKbankTotal.setProductCreditJcb(objRedeem[0]);
        redeemItems += objRedeem[1];
        redeemPoints += objRedeem[2];
        redeemAmt += objRedeem[3];
        redeemCredit += objRedeem[4];
        redeemTotal += objRedeem[5];
        redeemAllCards += objRedeem[0];

        //PRODUCT+CREDIT - OTHER
        objRedeem = FinancialApplication.getTransDataDbHelper().countSumOfRedeemKbankByIssuer(acquirer, ETransType.KBANK_REDEEM_PRODUCT_CREDIT, filter, null, issuerList);
        transRedeemKbankTotal.setProductCreditOther(objRedeem[0]);
        redeemItems += objRedeem[1];
        redeemPoints += objRedeem[2];
        redeemAmt += objRedeem[3];
        redeemCredit += objRedeem[4];
        redeemTotal += objRedeem[5];
        redeemAllCards += objRedeem[0];

        // EXTENDED VOIDED PRODUCT+CREDIT SALE CREDIT
        objRedeemSaleTrans = FinancialApplication.getTransDataDbHelper().countSumOfRedeemKbank(acquirer, ETransType.KBANK_REDEEM_PRODUCT_CREDIT, filter_Ex);
        redeemSaleCreditTrans += objRedeemSaleTrans[0];
        redeemSaleCredit += objRedeemSaleTrans[4];
        transRedeemKbankTotal.setProductCreditSaleCreditTotal(objRedeemSaleTrans[4]);
        transRedeemKbankTotal.setProductCreditSaleCreditTransCount((int) objRedeemSaleTrans[0]);

        //transRedeemKbankTotal.setProductCreditAllCard(redeemAllCards + redeemSaleCreditTrans);
        transRedeemKbankTotal.setProductCreditAllCard(redeemAllCards);
        transRedeemKbankTotal.setProductCreditItems(redeemItems);
        transRedeemKbankTotal.setProductCreditPoints(redeemPoints);
        transRedeemKbankTotal.setProductCreditRedeem(redeemAmt);
        transRedeemKbankTotal.setProductCreditCredit(redeemCredit);
        transRedeemKbankTotal.setProductCreditTotal(redeemTotal);
        redeemTotalAllCards += redeemAllCards + ((isSettlement) ? 0 : redeemSaleCreditTrans);

        //VOUCHER - VISA
        objRedeem = FinancialApplication.getTransDataDbHelper().countSumOfRedeemKbankByIssuer(acquirer, ETransType.KBANK_REDEEM_VOUCHER, filter, issuersVISA, null);
        transRedeemKbankTotal.setVoucherVisa(objRedeem[0]);
        redeemItems = objRedeem[1];
        redeemPoints = objRedeem[2];
        redeemAmt = objRedeem[3];
        redeemTotal = objRedeem[5];
        redeemAllCards = objRedeem[0];

        //VOUCHER - MASTERCARD
        objRedeem = FinancialApplication.getTransDataDbHelper().countSumOfRedeemKbankByIssuer(acquirer, ETransType.KBANK_REDEEM_VOUCHER, filter, issuersMASTER, null);
        transRedeemKbankTotal.setVoucherMastercard(objRedeem[0]);
        redeemItems += objRedeem[1];
        redeemPoints += objRedeem[2];
        redeemAmt += objRedeem[3];
        redeemTotal += objRedeem[5];
        redeemAllCards += objRedeem[0];

        //VOUCHER - JCB
        objRedeem = FinancialApplication.getTransDataDbHelper().countSumOfRedeemKbankByIssuer(acquirer, ETransType.KBANK_REDEEM_VOUCHER, filter, issuersJCB, null);
        transRedeemKbankTotal.setVoucherJcb(objRedeem[0]);
        redeemItems += objRedeem[1];
        redeemPoints += objRedeem[2];
        redeemAmt += objRedeem[3];
        redeemTotal += objRedeem[5];
        redeemAllCards += objRedeem[0];

        //VOUCHER - OTHER
        objRedeem = FinancialApplication.getTransDataDbHelper().countSumOfRedeemKbankByIssuer(acquirer, ETransType.KBANK_REDEEM_VOUCHER, filter, null, issuerList);
        transRedeemKbankTotal.setVoucherOther(objRedeem[0]);
        redeemItems += objRedeem[1];
        redeemPoints += objRedeem[2];
        redeemAmt += objRedeem[3];
        redeemTotal += objRedeem[5];
        redeemAllCards += objRedeem[0];


        // EXTENDED VOIDED VOUCHER SALE
        objRedeemSaleTrans = FinancialApplication.getTransDataDbHelper().countSumOfRedeemKbank(acquirer, ETransType.KBANK_REDEEM_PRODUCT, filter_Ex);

        //transRedeemKbankTotal.setVoucherAllCard(redeemAllCards + objRedeemSaleTrans[0]);
        transRedeemKbankTotal.setVoucherAllCard(redeemAllCards);
        transRedeemKbankTotal.setVoucherItems(redeemItems);
        transRedeemKbankTotal.setVoucherPoints(redeemPoints);
        transRedeemKbankTotal.setVoucherRedeem(redeemAmt);
        transRedeemKbankTotal.setVoucherTotal(redeemTotal);
        redeemTotalAllCards += redeemAllCards + ((isSettlement) ? 0 : objRedeemSaleTrans[0]);

        //VOUCHER+CREDIT - VISA
        objRedeem = FinancialApplication.getTransDataDbHelper().countSumOfRedeemKbankByIssuer(acquirer, ETransType.KBANK_REDEEM_VOUCHER_CREDIT, filter, issuersVISA, null);
        transRedeemKbankTotal.setVoucherCreditVisa(objRedeem[0]);
        redeemItems = objRedeem[1];
        redeemPoints = objRedeem[2];
        redeemAmt = objRedeem[3];
        redeemCredit = objRedeem[4];
        redeemTotal = objRedeem[5];
        redeemAllCards = objRedeem[0];

        //VOUCHER+CREDIT - MASTERCARD
        objRedeem = FinancialApplication.getTransDataDbHelper().countSumOfRedeemKbankByIssuer(acquirer, ETransType.KBANK_REDEEM_VOUCHER_CREDIT, filter, issuersMASTER, null);
        transRedeemKbankTotal.setVoucherCreditMastercard(objRedeem[0]);
        redeemItems += objRedeem[1];
        redeemPoints += objRedeem[2];
        redeemAmt += objRedeem[3];
        redeemCredit += objRedeem[4];
        redeemTotal += objRedeem[5];
        redeemAllCards += objRedeem[0];

        //VOUCHER+CREDIT - JCB
        objRedeem = FinancialApplication.getTransDataDbHelper().countSumOfRedeemKbankByIssuer(acquirer, ETransType.KBANK_REDEEM_VOUCHER_CREDIT, filter, issuersJCB, null);
        transRedeemKbankTotal.setVoucherCreditJcb(objRedeem[0]);
        redeemItems += objRedeem[1];
        redeemPoints += objRedeem[2];
        redeemAmt += objRedeem[3];
        redeemCredit += objRedeem[4];
        redeemTotal += objRedeem[5];
        redeemAllCards += objRedeem[0];

        //VOUCHER+CREDIT - OTHER
        objRedeem = FinancialApplication.getTransDataDbHelper().countSumOfRedeemKbankByIssuer(acquirer, ETransType.KBANK_REDEEM_VOUCHER_CREDIT, filter, null, issuerList);
        transRedeemKbankTotal.setVoucherCreditOther(objRedeem[0]);
        redeemItems += objRedeem[1];
        redeemPoints += objRedeem[2];
        redeemAmt += objRedeem[3];
        redeemCredit += objRedeem[4];
        redeemTotal += objRedeem[5];
        redeemAllCards += objRedeem[0];

        // EXTENDED VOUCHER+CREDIT SALE CREDIT
        objRedeemSaleTrans = FinancialApplication.getTransDataDbHelper().countSumOfRedeemKbank(acquirer, ETransType.KBANK_REDEEM_VOUCHER_CREDIT, filter_Ex);
        redeemSaleCreditTrans += objRedeemSaleTrans[0];
        redeemSaleCredit += objRedeemSaleTrans[4];
        transRedeemKbankTotal.setVoucherCreditSaleCreditTotal(objRedeemSaleTrans[4]);
        transRedeemKbankTotal.setVoucherCreditSaleCreditTransCount((int) objRedeemSaleTrans[0]);

        //transRedeemKbankTotal.setVoucherCreditAllCard(redeemAllCards + redeemSaleCreditTrans);
        transRedeemKbankTotal.setVoucherCreditAllCard(redeemAllCards);
        transRedeemKbankTotal.setVoucherCreditItems(redeemItems);
        transRedeemKbankTotal.setVoucherCreditPoints(redeemPoints);
        transRedeemKbankTotal.setVoucherCreditRedeem(redeemAmt);
        transRedeemKbankTotal.setVoucherCreditCredit(redeemCredit);
        transRedeemKbankTotal.setVoucherCreditTotal(redeemTotal);
        redeemTotalAllCards += redeemAllCards + ((isSettlement) ? 0 : redeemSaleCreditTrans);

        //DISCOUNT - VISA
        objRedeem = FinancialApplication.getTransDataDbHelper().countSumOfRedeemKbankByIssuer(acquirer, ETransType.KBANK_REDEEM_DISCOUNT, filter, issuersVISA, null);
        transRedeemKbankTotal.setDiscountVisa(objRedeem[0]);
        redeemItems = objRedeem[1];
        redeemPoints = objRedeem[2];
        redeemAmt = objRedeem[3];
        redeemCredit = objRedeem[4];
        redeemTotal = objRedeem[5];
        redeemAllCards = objRedeem[0];

        //DISCOUNT - MASTERCARD
        objRedeem = FinancialApplication.getTransDataDbHelper().countSumOfRedeemKbankByIssuer(acquirer, ETransType.KBANK_REDEEM_DISCOUNT, filter, issuersMASTER, null);
        transRedeemKbankTotal.setDiscountMastercard(objRedeem[0]);
        redeemItems += objRedeem[1];
        redeemPoints += objRedeem[2];
        redeemAmt += objRedeem[3];
        redeemCredit += objRedeem[4];
        redeemTotal += objRedeem[5];
        redeemAllCards += objRedeem[0];

        //DISCOUNT - JCB
        objRedeem = FinancialApplication.getTransDataDbHelper().countSumOfRedeemKbankByIssuer(acquirer, ETransType.KBANK_REDEEM_DISCOUNT, filter, issuersJCB, null);
        transRedeemKbankTotal.setDiscountJcb(objRedeem[0]);
        redeemItems += objRedeem[1];
        redeemPoints += objRedeem[2];
        redeemAmt += objRedeem[3];
        redeemCredit += objRedeem[4];
        redeemTotal += objRedeem[5];
        redeemAllCards += objRedeem[0];

        //DISCOUNT - OTHER
        objRedeem = FinancialApplication.getTransDataDbHelper().countSumOfRedeemKbankByIssuer(acquirer, ETransType.KBANK_REDEEM_DISCOUNT, filter, null, issuerList);
        transRedeemKbankTotal.setDiscountOther(objRedeem[0]);
        redeemItems += objRedeem[1];
        redeemPoints += objRedeem[2];
        redeemAmt += objRedeem[3];
        redeemCredit += objRedeem[4];
        redeemTotal += objRedeem[5];
        redeemAllCards += objRedeem[0];

        // EXTENDED DISCOUNT SALE CREDIT (FIX DISCOUNT RATE)
        objRedeemSaleTrans = FinancialApplication.getTransDataDbHelper().countSumOfRedeemKbank(acquirer, ETransType.KBANK_REDEEM_DISCOUNT, filter_Ex);
        redeemSaleCreditTrans += objRedeemSaleTrans[0];
        redeemSaleCredit += objRedeemSaleTrans[4];
        transRedeemKbankTotal.setDiscountSaleCreditTotal(objRedeemSaleTrans[4]);
        transRedeemKbankTotal.setDiscountSaleCreditTransCount((int) objRedeemSaleTrans[0]);

        //transRedeemKbankTotal.setDiscountAllCard(redeemAllCards + redeemSaleCreditTrans);
        transRedeemKbankTotal.setDiscountAllCard(redeemAllCards);
        transRedeemKbankTotal.setDiscountItems(redeemItems);
        transRedeemKbankTotal.setDiscountPoints(redeemPoints);
        transRedeemKbankTotal.setDiscountRedeem(redeemAmt);
        transRedeemKbankTotal.setDiscountCredit(redeemCredit);
        transRedeemKbankTotal.setDiscountTotal(redeemTotal);
        redeemTotalAllCards += redeemAllCards + ((isSettlement) ? 0 : redeemSaleCreditTrans);

        transRedeemKbankTotal.setTotalSaleAllCards(redeemTotalAllCards);
        return transRedeemKbankTotal;
    }

    public List<TransDccKbankTotal> calTotalDccGroupByCurrency(Acquirer acquirer) {
        List<TransDccKbankTotal> list = new ArrayList<>();

        List<Object[]> currencies = FinancialApplication.getTransDataDbHelper().groupCurrencyDccKbank(acquirer);
        if (currencies != null && !currencies.isEmpty()) {
            for (Object[] c : currencies) {
                String currency = (String) c[1];
                String[] saleData = FinancialApplication.getTransDataDbHelper().countSumOfDccKbank(acquirer, ETransType.SALE, filter, currency);
                String[] voidData = FinancialApplication.getTransDataDbHelper().countSumOfDccKbank(acquirer, ETransType.VOID, filter, currency);

                if (saleData == null && voidData == null) {
                    continue;
                }

                long[] sValue = new long[]{0, 0, 0};
                long[] vValue = new long[]{0, 0, 0};

                if (saleData != null) {
                    Log.d(TAG, "saleData = {" + saleData[0] + ", " + saleData[1] + ", " + saleData[2] + "}");
                    sValue[0] = Utils.parseLongSafe(saleData[0], 0);//count
                    sValue[1] = Utils.parseLongSafe(saleData[1], 0);//amount
                    sValue[2] = Utils.parseLongSafe(saleData[2], 0);//dccAmount
                }
                if (voidData != null) {
                    Log.d(TAG, "voidData = {" + voidData[0] + ", " + voidData[1] + ", " + voidData[2] + "}");
                    vValue[0] = Utils.parseLongSafe(voidData[0], 0);//count
                    vValue[1] = Utils.parseLongSafe(voidData[1], 0);//amount
                    vValue[2] = Utils.parseLongSafe(voidData[2], 0);//dccAmount
                }

                TransDccKbankTotal total = new TransDccKbankTotal();
                total.setCurrencyCode(currency);
                total.setCurrencyNumericCode(Tools.bytes2String((byte[]) c[0]));
                total.setSaleTotalNum(sValue[0]);
                total.setSaleTotalAmt(sValue[1]);
                total.setSaleDccTotalAmt(sValue[2]);
                total.setSaleVoidTotalNum(vValue[0]);
                total.setSaleVoidTotalAmt(vValue[1]);
                total.setSaleDccVoidTotalAmt(vValue[2]);
                total.setRefundTotalNum(0);
                total.setRefundTotalAmt(0);
                total.setRefundVoidTotalNum(0);
                total.setRefundVoidTotalAmt(0);
                total.setSaleOfflineTotalNum(0);
                total.setSaleOfflineTotalAmt(0);
                list.add(total);
            }
        }

        return list;
    }

    public List<TransDccKbankTotal> calTotalDccGroupByCurrency(Acquirer acquirer, Issuer issuer) {
        List<TransDccKbankTotal> list = new ArrayList<>();

        List<Object[]> currencies = FinancialApplication.getTransDataDbHelper().groupCurrencyDccKbank(acquirer, issuer);
        if (currencies != null && !currencies.isEmpty()) {
            for (Object[] c : currencies) {
                String currency = (String) c[1];
                String[] saleData = FinancialApplication.getTransDataDbHelper().countSumOfDccKbank(acquirer, issuer, ETransType.SALE, filter, currency);
                String[] voidData = FinancialApplication.getTransDataDbHelper().countSumOfDccKbank(acquirer, issuer, ETransType.VOID, filter, currency);

                if (saleData == null && voidData == null) {
                    continue;
                }

                long[] sValue = new long[]{0, 0, 0};
                long[] vValue = new long[]{0, 0, 0};

                if (saleData != null) {
                    Log.d(TAG, "saleData = {" + saleData[0] + ", " + saleData[1] + ", " + saleData[2] + "}");
                    sValue[0] = Utils.parseLongSafe(saleData[0], 0);//count
                    sValue[1] = Utils.parseLongSafe(saleData[1], 0);//amount
                    sValue[2] = Utils.parseLongSafe(saleData[2], 0);//dccAmount
                }
                if (voidData != null) {
                    Log.d(TAG, "voidData = {" + voidData[0] + ", " + voidData[1] + ", " + voidData[2] + "}");
                    vValue[0] = Utils.parseLongSafe(voidData[0], 0);//count
                    vValue[1] = Utils.parseLongSafe(voidData[1], 0);//amount
                    vValue[2] = Utils.parseLongSafe(voidData[2], 0);//dccAmount
                }

                TransDccKbankTotal total = new TransDccKbankTotal();
                total.setCurrencyCode(currency);
                total.setCurrencyNumericCode(Tools.bytes2String((byte[]) c[0]));
                total.setSaleTotalNum(sValue[0]);
                total.setSaleTotalAmt(sValue[1]);
                total.setSaleDccTotalAmt(sValue[2]);
                total.setSaleVoidTotalNum(vValue[0]);
                total.setSaleVoidTotalAmt(vValue[1]);
                total.setSaleDccVoidTotalAmt(vValue[2]);
                total.setRefundTotalNum(0);
                total.setRefundTotalAmt(0);
                total.setRefundVoidTotalNum(0);
                total.setRefundVoidTotalAmt(0);
                total.setSaleOfflineTotalNum(0);
                total.setSaleOfflineTotalAmt(0);
                list.add(total);
            }
        }

        return list;
    }

    public long[] calPreAuthTotal(Acquirer acquirer) {
        long[] obj = new long[]{0, 0, 0, 0};

        long[] temp = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.PREAUTHORIZATION, NORMAL);
        obj[0] = temp[0];//PreAuthTotalNum
        obj[1] = temp[1];//PreAuthTotalAmt

        temp = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.PREAUTHORIZATION_CANCELLATION, NORMAL);
        obj[2] = temp[0];//PreAuthCancelTotalNum
        obj[3] = temp[1];//PreAuthCancelTotalAmt

        return obj;
    }
}
