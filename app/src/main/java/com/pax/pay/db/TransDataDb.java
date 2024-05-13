/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-2-15
 * Module Author: Kim.L
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.db;

import androidx.annotation.NonNull;

import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.Issuer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.MerchantProfileManager;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import th.co.bkkps.utils.Log;

/**
 * DAO for transaction data table
 */
public class TransDataDb {

    private static final String TAG = "TransDataDb";
    private static final String COUNT_SUM_RAW = "COUNT(*), SUM(" + TransData.AMOUNT_FIELD_NAME + ")";
    private static final String COUNT_SUM_RAW_WALLET = "COUNT(*), SUM(" + TransData.AMOUNT_FIELD_NAME + "), " + TransData.WALLET_NAME;
    private static final String COUNT_SUM_REDEEM_KBANK = "COUNT(*), SUM(" + TransData.REDEEM_QTY_KBANK_FIELD_NAME + "), SUM(" + TransData.REDEEM_POINTS_KBANK_FIELD_NAME + ")," +
            " SUM(" + TransData.REDEEM_AMOUNT_KBANK_FIELD_NAME + "), SUM(" + TransData.REDEEM_CREDIT_KBANK_FIELD_NAME + ") , " +
            "SUM(" + TransData.REDEEM_TOTAL_KBANK_FIELD_NAME + ")";
    private static final String COUNT_SUM_REDEEM_KBANK_CREDITSALE = "COUNT(*), 0, 0, 0, SUM(" + TransData.REDEEM_CREDIT_KBANK_FIELD_NAME + "), 0";
    private static final String COUNT_SUM_DCC_KBANK = "COUNT(*), SUM(" + TransData.AMOUNT_FIELD_NAME + "), SUM(" + TransData.DCC_AMOUNT_KBANK_FIELD_NAME + ")";
    private static TransDataDb instance;
    private final BaseDbHelper dbHelper;
    private RuntimeExceptionDao<TransData, Integer> transDao = null;

    private TransDataDb() {
        dbHelper = BaseDbHelper.getInstance();
    }

    /**
     * get the Singleton of the DB Helper
     *
     * @return the Singleton of DB helper
     */
    public static synchronized TransDataDb getInstance() {
        if (instance == null) {
            instance = new TransDataDb();
        }

        return instance;
    }

    /***************************************
     * Dao
     ******************************************/
    private RuntimeExceptionDao<TransData, Integer> getTransDao() {
        if (transDao == null) {
            transDao = dbHelper.getRuntimeExceptionDao(TransData.class);
        }
        return transDao;
    }

    /*--------------------Trans Data-----------------------------*/

    /**
     * insert a transData
     */
    public boolean insertTransData(TransData transData) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            dao.create(transData);
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "", e);
        }

        return false;
    }

    /**
     * update a transData
     */
    public boolean updateTransData(TransData transData) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            dao.update(transData);
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "", e);
        }

        return false;
    }

    /**
     * find transData by id
     *
     * @param id transaction record id
     * @return transaction data object
     */
    public TransData findTransData(int id) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            return dao.queryForId(id);
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    /**
     * find transData by trace No
     *
     * @param traceNo trace no
     * @return transaction data object
     */
    public TransData findTransDataByTraceNo(long traceNo, boolean isOnlyCreditTrans) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.where()
                    .eq(TransData.TRACENO_FIELD_NAME, traceNo)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_INFO)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_KPLUS)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_ALIPAY)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_WECHAT)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_CREDIT)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.ADJUST);
            if (isOnlyCreditTrans) {
                List<Acquirer> acqs = new ArrayList<>();
                acqs.add(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_KBANK));
                acqs.add(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_KBANK_BDMS));
                acqs.add(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_AMEX));
                acqs.add(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_UP));
                acqs.add(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_DCC));

                where.and().in(Acquirer.ID_FIELD_NAME, acqs)
                        .and().eq(TransData.MERCHANT_NAME_FILED_NAME, MerchantProfileManager.INSTANCE.getCurrentMerchant());
            }


            return where.queryForFirst();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    /**
     * find transData by trace No
     *
     * @param traceNo trace no
     * @return transaction data object
     */
    public TransData findTransDataByTraceNo(long traceNo, boolean isOnlyCreditTrans, List<ETransType> excludeTrans) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.where()
                    .eq(TransData.TRACENO_FIELD_NAME, traceNo)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_INFO)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_KPLUS)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_ALIPAY)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_WECHAT)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_CREDIT)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.ADJUST)
                    .and().notIn(TransData.TYPE_FIELD_NAME, excludeTrans);
            if (isOnlyCreditTrans) {
                List<Acquirer> acqs = new ArrayList<>();
                acqs.add(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_KBANK));
                acqs.add(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_KBANK_BDMS));
                acqs.add(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_AMEX));
                acqs.add(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_UP));
                acqs.add(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_DCC));

                where.and().in(Acquirer.ID_FIELD_NAME, acqs);
            }


            return where.queryForFirst();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    /**
     * find transData by stan No
     *
     * @param stanNo stan no
     * @return transaction data object
     */
    public TransData findTransDataByStanNo(long stanNo, boolean isOnlyCreditTrans) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.where()
                    .eq(TransData.STANNO_FIELD_NAME, stanNo)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_INFO)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_KPLUS)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_ALIPAY)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_WECHAT)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_CREDIT)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.ADJUST);
            if (isOnlyCreditTrans) {
                List<Acquirer> acqs = new ArrayList<>();
                acqs.add(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_KBANK));
                acqs.add(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_KBANK_BDMS));
                acqs.add(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_AMEX));
                acqs.add(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_UP));
                acqs.add(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_QRC));

                where.and().in(Acquirer.ID_FIELD_NAME, acqs)
                        .and().eq(TransData.MERCHANT_NAME_FILED_NAME, MerchantProfileManager.INSTANCE.getCurrentMerchant());
            }


            return where.queryForFirst();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    /**
     * find transData by trace No and list of acquirers
     *
     * @param traceNo trace no
     * @return transaction data object
     */
    public TransData findTransDataByTraceNoAndAcqs(long traceNo, List<Acquirer> acquirers) {
        try {
            List<Acquirer> acquirerList = new ArrayList<>();
            for (int acq_index = 0; acq_index <= acquirers.size() - 1; acq_index++) {
                if (acquirers.get(acq_index) != null) {
                    acquirerList.add(acquirers.get(acq_index));
                }
            }

            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.where()
                    .eq(TransData.TRACENO_FIELD_NAME, traceNo)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_INFO)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_KPLUS)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_ALIPAY)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_WECHAT)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_CREDIT)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.ADJUST)
                    .and().in(Acquirer.ID_FIELD_NAME, acquirerList)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, MerchantProfileManager.INSTANCE.getCurrentMerchant());
            return where.queryForFirst();
        } catch (Exception e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    /**
     * find transData by trace No and list of acquirers
     *
     * @param traceNo trace no
     * @return transaction data object
     */
    public TransData findTransDataByTraceNoAndAcqs(long traceNo, List<Acquirer> acquirers, List<ETransType> excludeTrans) {
        try {
            List<Acquirer> acquirerList = new ArrayList<>();
            for (int acq_index = 0; acq_index <= acquirers.size() - 1; acq_index++) {
                if (acquirers.get(acq_index) != null) {
                    acquirerList.add(acquirers.get(acq_index));
                }
            }

            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.where()
                    .eq(TransData.TRACENO_FIELD_NAME, traceNo)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_INFO)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_KPLUS)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_ALIPAY)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_WECHAT)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_CREDIT)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.ADJUST)
                    .and().notIn(TransData.TYPE_FIELD_NAME, excludeTrans)
                    .and().in(Acquirer.ID_FIELD_NAME, acquirerList);
            return where.queryForFirst();
        } catch (Exception e) {
            Log.w(TAG, "", e);
        }

        return null;
    }


    /**
     * find transData by Stan No and list of acquirers
     *
     * @param stanNo trace no
     * @return transaction data object
     */
    public TransData findTransDataByStanNoAndAcqs(long stanNo, List<Acquirer> acquirers) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.where()
                    .eq(TransData.STANNO_FIELD_NAME, stanNo)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL)
                    .and().in(Acquirer.ID_FIELD_NAME, acquirers)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, MerchantProfileManager.INSTANCE.getCurrentMerchant())
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_INFO)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_KPLUS)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_ALIPAY)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_WECHAT)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_CREDIT)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.ADJUST);

            return where.queryForFirst();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }


    /**
     * find transData by Stan No and list of acquirers
     *
     * @param stanNo trace no
     * @return transaction data object
     */
    public TransData findTransDataByStanNoAndAcqs(long stanNo, List<Acquirer> acquirers, List<ETransType> excludeTrans) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.where()
                    .eq(TransData.STANNO_FIELD_NAME, stanNo)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL)
                    .and().in(Acquirer.ID_FIELD_NAME, acquirers)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_INFO)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_KPLUS)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_ALIPAY)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_WECHAT)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_CREDIT)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.ADJUST)
                    .and().notIn(TransData.TYPE_FIELD_NAME, excludeTrans);

            return where.queryForFirst();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    public TransData findAdjustedTransDataByTraceNo(long traceNo, Acquirer acquirer) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.where()
                    .eq(TransData.TRACENO_FIELD_NAME, traceNo)
                    .and().eq(TransData.TYPE_FIELD_NAME, ETransType.ADJUST)
                    .and().eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, MerchantProfileManager.INSTANCE.getCurrentMerchant());
            return where.queryForFirst();
        } catch (Exception e) {
            Log.w(TAG, "", e);
        }

        return null;
    }


    /**
     * find wallet transData by trace No
     *
     * @param traceNo trace no
     * @return wallet transaction data object
     */
    public TransData findWalletTransDataByTraceNo(long traceNo) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_WALLET);
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.where()
                    .eq(TransData.TRACENO_FIELD_NAME, traceNo)
                    .and().eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, MerchantProfileManager.INSTANCE.getCurrentMerchant())
//                    .and().eq(TransData.TYPE_FIELD_NAME, ETransType.QR_SALE_WALLET)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

            return where.queryForFirst();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    /**
     * find wallet transData by trace No
     *
     * @param traceNo trace no
     * @return wallet transaction data object
     */
    public TransData findPromptPayTransDataByTraceNo(long traceNo) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_QR_PROMPT);
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.where()
                    .eq(TransData.TRACENO_FIELD_NAME, traceNo)
                    .and().eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, MerchantProfileManager.INSTANCE.getCurrentMerchant())
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

            return where.queryForFirst();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    /**
     * find Kplus transData by trace No
     *
     * @param traceNo trace no
     * @return wallet transaction data object
     */
    public TransData findKplusTransDataByTraceNo(long traceNo) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_KPLUS);
            Acquirer acquirerBsC = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_ALIPAY_B_SCAN_C);
            List<Acquirer> acqList = new ArrayList<>();
            acqList.add(acquirer);
            acqList.add(acquirerBsC);
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.where()
                    .eq(TransData.TRACENO_FIELD_NAME, traceNo)
                    .and().in(Acquirer.ID_FIELD_NAME, acqList)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, MerchantProfileManager.INSTANCE.getCurrentMerchant())
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_KPLUS)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

            return where.queryForFirst();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    /**
     * find Alipay transData by trace No
     *
     * @param traceNo trace no
     * @return wallet transaction data object
     */
    public TransData findAlipayTransDataByTraceNo(long traceNo) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_ALIPAY);
            Acquirer acquirerBsC = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_ALIPAY_B_SCAN_C);
            List<Acquirer> acqList = new ArrayList<>();
            acqList.add(acquirer);
            acqList.add(acquirerBsC);
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.where()
                    .eq(TransData.TRACENO_FIELD_NAME, traceNo)
                    .and().in(Acquirer.ID_FIELD_NAME, acqList)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, MerchantProfileManager.INSTANCE.getCurrentMerchant())
//                    .and().eq(TransData.TYPE_FIELD_NAME, ETransType.QR_SALE_WALLET)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

            return where.queryForFirst();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }


    public TransData findAlipayTransDataByStanNo(long stanNo) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_ALIPAY);
            Acquirer acquirerBsC = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_ALIPAY_B_SCAN_C);
            List<Acquirer> acqList = new ArrayList<>();
            acqList.add(acquirer);
            acqList.add(acquirerBsC);
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.where()
                    .eq(TransData.STANNO_FIELD_NAME, stanNo)
                    .and().in(Acquirer.ID_FIELD_NAME, acqList)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, MerchantProfileManager.INSTANCE.getCurrentMerchant())
//                    .and().eq(TransData.TYPE_FIELD_NAME, ETransType.QR_SALE_WALLET)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

            return where.queryForFirst();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    /**
     * find wallet transData by trace No
     *
     * @param traceNo trace no
     * @return wallet transaction data object
     */
    public TransData findWechatTransDataByTraceNo(long traceNo) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_WECHAT);
            Acquirer acquirerBsC = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_WECHAT_B_SCAN_C);
            List<Acquirer> acqList = new ArrayList<>();
            acqList.add(acquirer);
            acqList.add(acquirerBsC);
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.where()
                    .eq(TransData.TRACENO_FIELD_NAME, traceNo)
                    .and().in(Acquirer.ID_FIELD_NAME, acqList)
//                    .and().eq(TransData.TYPE_FIELD_NAME, ETransType.QR_SALE_WALLET)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

            return where.queryForFirst();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    /**
     * find wallet transData by trace No
     *
     * @param traceNo trace no
     * @return wallet transaction data object
     */
    public TransData findWechatBscanCTransDataByTraceNo(long traceNo) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_WECHAT_B_SCAN_C);
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.where()
                    .eq(TransData.TRACENO_FIELD_NAME, traceNo)
                    .and().eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, MerchantProfileManager.INSTANCE.getCurrentMerchant())
//                    .and().eq(TransData.TYPE_FIELD_NAME, ETransType.QR_SALE_WALLET)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

            return where.queryForFirst();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    public TransData findWechatTransDataByStanNo(long stanNo) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_WECHAT);
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.where()
                    .eq(TransData.STANNO_FIELD_NAME, stanNo)
                    .and().eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, MerchantProfileManager.INSTANCE.getCurrentMerchant())
//                    .and().eq(TransData.TYPE_FIELD_NAME, ETransType.QR_SALE_WALLET)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

            return where.queryForFirst();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    public TransData findWechatBscanCTransDataByStanNo(long stanNo) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_WECHAT_B_SCAN_C);
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.where()
                    .eq(TransData.STANNO_FIELD_NAME, stanNo)
                    .and().eq(Acquirer.ID_FIELD_NAME, acquirer)
//                    .and().eq(TransData.TYPE_FIELD_NAME, ETransType.QR_SALE_WALLET)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

            return where.queryForFirst();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    /**
     * find wallet transData by trace No
     *
     * @param traceNo trace no
     * @return wallet transaction data object
     */
    public TransData findQRCreditTransDataByTraceNo(long traceNo) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_QR_CREDIT);
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.where()
                    .eq(TransData.TRACENO_FIELD_NAME, traceNo)
                    .and().eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, MerchantProfileManager.INSTANCE.getCurrentMerchant())
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

            return where.queryForFirst();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    public TransData findQRCreditTransDataByStanNo(long stanNo) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_QR_CREDIT);
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.where()
                    .eq(TransData.STANNO_FIELD_NAME, stanNo)
                    .and().eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, MerchantProfileManager.INSTANCE.getCurrentMerchant())
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

            return where.queryForFirst();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    /**
     * find transData by trace No
     *
     * @param traceNo    trace no
     * @param eTransType EtransType
     * @return wallet transaction data object
     */
    public TransData findTransData(long traceNo, ETransType eTransType) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.where()
                    .eq(TransData.TRACENO_FIELD_NAME, traceNo)
                    .and().eq(TransData.TYPE_FIELD_NAME, eTransType);
            return where.queryForFirst();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    /**
     * filter transData by types and statues
     *
     * @param types    {@link ETransType}
     * @param statuses {@link TransData.ETransStatus}
     * @return transaction data list
     */
    @NonNull
    public List<TransData> findTransData(List<ETransType> types, List<TransData.ETransStatus> statuses, boolean chkAuthCode) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            List<Acquirer> acqs = FinancialApplication.getAcqManager().findEnableAcquirers();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.orderBy(TransData.TRACENO_FIELD_NAME, true).where();

            where.ne(TransData.STATE_FIELD_NAME, TransData.ETransStatus.VOIDED);
            where.and().in(TransData.TYPE_FIELD_NAME, types)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

            if (chkAuthCode)
                where.and().isNotNull(TransData.APPR_CODE);

            //return where.query();
            //String x = where.prepare().getStatement();
            //Log.d("QUERY", x);
            return where.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }

        return new ArrayList<>(0);
    }

    /**
     * filter transData by types and statues
     *
     * @param types    {@link ETransType}
     * @param statuses {@link TransData.ETransStatus}
     * @return transaction data list
     */
    @NonNull
    public List<TransData> findTransDataExceptAcq(List<ETransType> types, List<TransData.ETransStatus> statuses, boolean chkAuthCode, String...exceptAcqName) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            List<Acquirer> acqs = FinancialApplication.getAcqManager().findEnableAcquirersExcept(Arrays.asList(exceptAcqName));
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.orderBy(TransData.TRACENO_FIELD_NAME,true).where();

            where.ne(TransData.STATE_FIELD_NAME, TransData.ETransStatus.VOIDED);
            where.and().in(TransData.TYPE_FIELD_NAME, types)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL)
                    .and().in(Acquirer.ID_FIELD_NAME, acqs)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, MerchantProfileManager.INSTANCE.getCurrentMerchant());

            if (chkAuthCode)
                where.and().isNotNull(TransData.APPR_CODE);

            //return where.query();
            //String x = where.prepare().getStatement();
            //Log.d("QUERY", x);
            return where.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }

        return new ArrayList<>(0);
    }

    /**
     * filter transData by offline status
     *
     * @param statuses {@link TransData.OfflineStatus}
     * @return transaction data list
     */
    @NonNull
    public List<TransData> findOfflineTransData(List<TransData.OfflineStatus> statuses) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            List<Acquirer> acqs = new ArrayList<>();
            queryBuilder.where().in(TransData.OFFLINE_STATE_FIELD_NAME, statuses)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL)
                    .and().notIn(Acquirer.ID_FIELD_NAME, acqs)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, MerchantProfileManager.INSTANCE.getCurrentMerchant());
            return queryBuilder.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }

        return new ArrayList<>(0);
    }

    /**
     * get all of offline transData by acquirers
     *
     * @param acquirers
     * @return
     */
    @NonNull
    public List<TransData> findAllOfflineTransData(List<Acquirer> acquirers, List<TransData.OfflineStatus> filters) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            queryBuilder.where().in(Acquirer.ID_FIELD_NAME, acquirers)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, MerchantProfileManager.INSTANCE.getCurrentMerchant())
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL)
                    .and().ne(TransData.STATE_FIELD_NAME, TransData.ETransStatus.VOIDED)
                    .and().isNotNull(TransData.OFFLINE_STATE_FIELD_NAME)
                    .and().notIn(TransData.OFFLINE_STATE_FIELD_NAME, filters);
            return queryBuilder.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }

        return new ArrayList<>(0);
    }

    /**
     * read the first offline record by acquirer
     *
     * @return transaction data object, if the record is not existed, return null
     */
    public TransData findOfflineTransData(Acquirer acquirer, List<TransData.OfflineStatus> filters) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            queryBuilder.where().eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, MerchantProfileManager.INSTANCE.getCurrentMerchant())
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL)
                    .and().ne(TransData.STATE_FIELD_NAME, TransData.ETransStatus.VOIDED)
                    .and().isNotNull(TransData.OFFLINE_STATE_FIELD_NAME)
                    .and().notIn(TransData.OFFLINE_STATE_FIELD_NAME, filters);
            return queryBuilder.queryForFirst();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return null;
    }

    /**
     * filter transData by types, statues and acquirer
     *
     * @param types    {@link ETransType}
     * @param statuses {@link TransData.ETransStatus}
     * @param acq      the specific acquirer
     * @return transaction data list
     */
    //AET-95
    @NonNull
    public List<TransData> findTransData(List<ETransType> types, List<TransData.ETransStatus> statuses, Acquirer acq) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.orderBy(TransData.TRACENO_FIELD_NAME, true)
                    .where().in(TransData.TYPE_FIELD_NAME, types)
                    .and().notIn(TransData.STATE_FIELD_NAME, statuses)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL)
                    .and().eq(Acquirer.ID_FIELD_NAME, acq)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName);
            if (acq.getName().equals(Constants.ACQ_QR_PROMPT) ||
                    acq.getName().equals(Constants.ACQ_QRC)) {
                where.and().isNotNull(TransData.APPR_CODE);
            }

            return queryBuilder.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }

        return new ArrayList<>(0);
    }

    /**
     * filter transData by types, statues and acquirer
     *
     * @param types    {@link ETransType}
     * @param statuses {@link TransData.ETransStatus}
     * @param acq      the specific acquirer
     * @param issuer   the specific issuer
     * @return transaction data list
     */
    //AET-95
    @NonNull
    public List<TransData> findTransData(List<ETransType> types, List<TransData.ETransStatus> statuses, Acquirer acq, Issuer issuer) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.orderBy(TransData.TRACENO_FIELD_NAME, true)
                    .where().in(TransData.TYPE_FIELD_NAME, types)
                    .and().notIn(TransData.STATE_FIELD_NAME, statuses)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL)
                    .and().eq(Acquirer.ID_FIELD_NAME, acq)
                    .and().eq(Issuer.ID_FIELD_NAME, issuer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName);
            if (acq.getName().equals(Constants.ACQ_QR_PROMPT) ||
                    acq.getName().equals(Constants.ACQ_QRC)) {
                where.and().isNotNull(TransData.APPR_CODE);
            }

            return queryBuilder.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }

        return new ArrayList<>(0);
    }

    /**
     * find the last transData
     *
     * @return transaction data object
     */
    public TransData findLastTransData() {
        try {
            List<TransData> list = findAllTransData();
            if (!list.isEmpty()) {
                return list.get(list.size() - 1);
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    /**
     * find the last transData by list of Acquirers
     *
     * @return transaction data object
     */
    public TransData findLastTransDataByAcqsAndMerchant(List<Acquirer> acquirers) {
        try {
            List<TransData> list = findAllTransDataByAcqsAndMerchant(false, acquirers);
            if (!list.isEmpty()) {
                return list.get(list.size() - 1);
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    public TransData findLastTransDataByAcqs(List<Acquirer> acquirers) {
        try {
            List<TransData> list = findAllTransDataByAcqs(false, acquirers);
            if (!list.isEmpty()) {
                return list.get(list.size() - 1);
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    /**
     * find the last transData for History
     *
     * @return transaction data object
     */
    public TransData findLastTransDataHistory() {
        try {
            List<TransData> list = findAllTransDataHistory();
            if (!list.isEmpty()) {
                return list.get(list.size() - 1);
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    /**
     * find the last transData for History
     *
     * @return transaction data object
     */
    public TransData findLastWalletTransData() {
        try {
            List<TransData> list = findAllWalletSaleTransData(false);
            if (!list.isEmpty()) {
                return list.get(list.size() - 1);
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    /**
     * find the last transData for History
     *
     * @return transaction data object
     */
    public TransData findLastQRVisaTransData(boolean includeReversal) {
        try {
            List<TransData> list = findAllQRVisaTransData(includeReversal);
            if (!list.isEmpty()) {
                return list.get(list.size() - 1);
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    /**
     * find the last PromptPay transData
     *
     * @param acquirer the specific acquirer
     * @return transaction data object
     */
    public TransData findLastTransPromptPayData(Acquirer acquirer, boolean isCheckStatus) {
        try {
            List<TransData> list = findQRSaleTransData(acquirer, false);
            if (!list.isEmpty()) {
                return list.get(list.size() - 1);
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    /**
     * find the last transData for History
     *
     * @return transaction data object
     */
    public TransData findLastKplusTransData() {
        try {
            List<TransData> list = findAllKplusSaleTransData(false);
            if (!list.isEmpty()) {
                return list.get(list.size() - 1);
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    /**
     * find the last transData for History
     *
     * @return transaction data object
     */
    public TransData findLastAlipayTransData() {
        try {
            List<TransData> list = findAllAlipaySaleTransData(false);
            if (!list.isEmpty()) {
                return list.get(list.size() - 1);
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    /**
     * find the last transData for History
     *
     * @return transaction data object
     */
    public TransData findLastWechatTransData() {
        try {
            List<TransData> list = findAllWechatSaleTransData(false);
            if (!list.isEmpty()) {
                return list.get(list.size() - 1);
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    /**
     * find the last transData for History
     *
     * @return transaction data object
     */
    public TransData findLastQRCreditTransData() {
        try {
            List<TransData> list = findAllQRCreditSaleTransData(false);
            if (!list.isEmpty()) {
                return list.get(list.size() - 1);
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    /**
     * find transData by trace No
     *
     * @return transaction data list
     */
    @NonNull
    public List<TransData> findAllTransDataHistory() {
        return findAllTransDataEnabled(false, true);
    }

    @NonNull
    public TransData findLatestSaleVoidTransData() {
        return findLatestTransDataByReversalStatusAndTransState(TransData.ReversalStatus.NORMAL, TransData.ETransStatus.NORMAL);
    }

    /**
     * find transData by trace No
     *
     * @return transaction data list
     */
    @NonNull
    public List<TransData> findAllTransData() {
        return findAllCreditTransData(false);
    }

    /**
     * find transData by acquirer name, does not include void transaction
     *
     * @return transaction data list
     */
    @NonNull
    public List<TransData> findAllTransData(Acquirer acq, boolean filterOffline) {
        return findAllTransData(acq, false, filterOffline, false);
    }

    /**
     * find transData by acquirer name
     *
     * @param includeVoid if includes void transaction
     * @return transaction data list
     */
    @NonNull
    public List<TransData> findAllTransData(Acquirer acq, boolean includeVoid, boolean filterOffline, boolean includeAdjustTrans) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.orderBy(TransData.TRACENO_FIELD_NAME, true)
                    .where().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

            if (filterOffline) {
                List<TransData.OfflineStatus> filterOut = new ArrayList<>();
                filterOut.add(TransData.OfflineStatus.OFFLINE_ERR_RESP);
                filterOut.add(TransData.OfflineStatus.OFFLINE_ERR_SEND);
                filterOut.add(TransData.OfflineStatus.OFFLINE_ERR_UNKNOWN);
                filterOut.add(TransData.OfflineStatus.OFFLINE_NOT_SENT);
                filterOut.add(TransData.OfflineStatus.OFFLINE_SENDING);
                filterOut.add(TransData.OfflineStatus.OFFLINE_VOIDED);

                where.or(
                        where.and().isNull(TransData.OFFLINE_STATE_FIELD_NAME),
                        where.and(
                                where.isNotNull(TransData.OFFLINE_STATE_FIELD_NAME),
                                where.notIn(TransData.OFFLINE_STATE_FIELD_NAME, filterOut)));
            }
            if (!includeVoid)
                where.and().ne(TransData.STATE_FIELD_NAME, TransData.ETransStatus.VOIDED);

            if (!includeAdjustTrans) {
                where.and().ne(TransData.TYPE_FIELD_NAME, ETransType.ADJUST);
            }

            where.and().eq(Acquirer.ID_FIELD_NAME, acq)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName);

            return where.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    /**
     * find transData except Pre-Auth by acquirer name
     *
     * @param includeVoid if includes void transaction
     * @return transaction data list
     */
    @NonNull
    public List<TransData> findAllTransDataExceptPreAuth(Acquirer acq, boolean includeVoid, boolean filterOffline) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.orderBy(TransData.TRACENO_FIELD_NAME, true)
                    .where().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

            if (filterOffline) {
                List<TransData.OfflineStatus> filterOut = new ArrayList<>();
                filterOut.add(TransData.OfflineStatus.OFFLINE_ERR_RESP);
                filterOut.add(TransData.OfflineStatus.OFFLINE_ERR_SEND);
                filterOut.add(TransData.OfflineStatus.OFFLINE_ERR_UNKNOWN);
                filterOut.add(TransData.OfflineStatus.OFFLINE_NOT_SENT);
                filterOut.add(TransData.OfflineStatus.OFFLINE_SENDING);
                filterOut.add(TransData.OfflineStatus.OFFLINE_VOIDED);

                where.or(
                        where.and().isNull(TransData.OFFLINE_STATE_FIELD_NAME),
                        where.and(
                                where.isNotNull(TransData.OFFLINE_STATE_FIELD_NAME),
                                where.notIn(TransData.OFFLINE_STATE_FIELD_NAME, filterOut)));
            }
            if (!includeVoid)
                where.and().ne(TransData.STATE_FIELD_NAME, TransData.ETransStatus.VOIDED);

            List<ETransType> preAuthTypes = Arrays.asList(ETransType.PREAUTH, ETransType.PREAUTHORIZATION,
                    ETransType.PREAUTHORIZATION_CANCELLATION, ETransType.SALE_COMPLETION);
            where.and().notIn(TransData.TYPE_FIELD_NAME, preAuthTypes); // filter-out these types to handle in another function

            where.and().eq(Acquirer.ID_FIELD_NAME, acq)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName);;

            return where.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    /**
     * find transData by trace No
     *
     * @return transaction data list
     */
    @NonNull
    public List<TransData> findAllTransData(boolean includeReversal) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            if (includeReversal)
                return dao.queryForAll();
            return dao.queryForEq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    /**
     * find transData by trace No
     *
     * @return transaction data list
     */
    @NonNull
    private List<TransData> findAllTransData(boolean includeReversal, Acquirer acquirer) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.where()
                    .eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName);
            if (includeReversal)
                where.and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);
            return where.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    /**
     * find all transData by list of acquirers
     *
     * @return transaction data list
     */
    @NonNull
    public List<TransData> findAllTransDataByAcqsAndMerchant(boolean includeReversal, List<Acquirer> acquirers) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();

            Where<TransData, Integer> where = queryBuilder.where()
                    .in(Acquirer.ID_FIELD_NAME, acquirers)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName)
                    .and().ne(TransData.TYPE_FIELD_NAME, ETransType.ADJUST);

            if (!includeReversal)
                where.and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

            return where.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    @NonNull
    public List<TransData> findAllTransDataByAcqs(boolean includeReversal, List<Acquirer> acquirers) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();

            Where<TransData, Integer> where = queryBuilder.where()
                    .in(Acquirer.ID_FIELD_NAME, acquirers)
                    .and().ne(TransData.TYPE_FIELD_NAME, ETransType.ADJUST);

            if (!includeReversal)
                where.and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

            return where.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    public List<TransData> findEntireTransDataRecords() {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            return queryBuilder.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    /**
     * find Credit transData
     *
     * @return transaction data list
     */
    @NonNull
    public List<TransData> findAllCreditTransData(boolean includeReversal) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            String[] creditAcqs = {Constants.ACQ_KBANK, Constants.ACQ_KBANK_BDMS, Constants.ACQ_AMEX, Constants.ACQ_UP};

            List<Acquirer> acqs = new ArrayList<>();
            /*acqs.add(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_KBANK));
            acqs.add(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_AMEX));
            acqs.add(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_UP));*/

            for (String tempAcqs : creditAcqs) {
                Acquirer acq = FinancialApplication.getAcqManager().findAcquirer(tempAcqs);
                if (acq != null) {
                    acqs.add(acq);
                }
            }

            Where<TransData, Integer> where = queryBuilder.where()
                    .in(Acquirer.ID_FIELD_NAME, acqs)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, MerchantProfileManager.INSTANCE.getCurrentMerchant())
                    .and().ne(TransData.TYPE_FIELD_NAME, ETransType.ADJUST);

            if (!includeReversal)
                where.and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

            return where.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    /**
     * find Wallet transData
     *
     * @return transaction data list
     */
    @NonNull
    public List<TransData> findAllWalletTransData(boolean includeVoid) {
        try {
            Acquirer acq = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_WALLET);
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.where()
                    // AET-258
                    .eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL)
                    .and().eq(Acquirer.ID_FIELD_NAME, acq)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_WALLET);
            if (!includeVoid)
                where.and().ne(TransData.STATE_FIELD_NAME, TransData.ETransStatus.VOIDED);
            return where.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    /**
     * find transData by acquirer name
     *
     * @param includeVoid if includes void transaction
     * @return transaction data list
     */
    @NonNull
    public List<TransData> findAllReferredTrans(Acquirer acq, boolean includeVoid) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();

            List<TransData.ReferralStatus> filter = new ArrayList<>();
            filter.add(TransData.ReferralStatus.NORMAL);
            filter.add(TransData.ReferralStatus.REFERRED_SUCC);

            Where<TransData, Integer> where = queryBuilder.where()
                    .notIn(TransData.REFERRAL_FIELD_NAME, filter).and()
                    .ne(TransData.TYPE_FIELD_NAME, ETransType.ADJUST).and()
                    .eq(Acquirer.ID_FIELD_NAME, acq)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName);
            if (!includeVoid)
                where.and().ne(TransData.STATE_FIELD_NAME, TransData.ETransStatus.VOIDED);
            return where.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    /**
     * find transData
     *
     * @return transaction data list
     */
    @NonNull
    private List<TransData> findAllTransData(boolean includeReversal, boolean isChkAuthCode) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            if (includeReversal) {
                return dao.queryForAll();
            }
            if (isChkAuthCode) {
                QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
                Where<TransData, Integer> where = queryBuilder.where()
                        .eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL).and()
                        .isNotNull(TransData.APPR_CODE).and();
                return where.query();
            }
            return dao.queryForEq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    /**
     * find transData
     *
     * @return transaction data list
     */
    @NonNull
    private List<TransData> findAllTransDataEnabled(boolean includeReversal, boolean isChkAuthCode) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            List<Acquirer> acqs = FinancialApplication.getAcqManager().findEnableAcquirers();
            if (includeReversal) {
                return dao.queryForAll();
            }
            if (isChkAuthCode) {
                QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
                Where<TransData, Integer> where = queryBuilder.where()
                        .eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL)
                        .and().isNotNull(TransData.APPR_CODE)
                        .and().in(Acquirer.ID_FIELD_NAME, acqs)
                        .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName)
						.and().ne(TransData.TYPE_FIELD_NAME, ETransType.ADJUST);
                return where.query();
            }
            return dao.queryForEq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    @NonNull
    private TransData findLatestTransDataByReversalStatusAndTransState(TransData.ReversalStatus revsStatus, TransData.ETransStatus transStatus) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder().orderBy(TransData.ID_FIELD_NAME, false);
            Where<TransData, Integer> where = queryBuilder.where().ne(TransData.TYPE_FIELD_NAME, ETransType.ADJUST);
            TransData tmpTransData = where.queryForFirst();
            if (tmpTransData != null) {
                if ((tmpTransData.getTransType() == ETransType.SALE
                        || tmpTransData.getTransType() == ETransType.QR_INQUIRY
                        || tmpTransData.getTransType() == ETransType.QR_INQUIRY_ALIPAY
                        || tmpTransData.getTransType() == ETransType.QR_INQUIRY_WECHAT
                        || tmpTransData.getTransType() == ETransType.QR_INQUIRY_CREDIT)
                            && tmpTransData.getReversalStatus() == revsStatus
                            && tmpTransData.getAuthCode() != null
                            && tmpTransData.getTransState() == transStatus) {
                    return tmpTransData;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "", e.getMessage());
        }

        return null;
    }


    /**
     * find transData by acquirer name
     *
     * @param includeVoid if includes void transaction
     * @param filter      transType
     * @return transaction data list
     */
    @NonNull
    public List<TransData> findAllTransData(Acquirer acq, boolean includeVoid, List<ETransType> filter, boolean isChkAuthCode) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            // AET-258
            Where<TransData, Integer> where = queryBuilder.where().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL)
                    .and().eq(Acquirer.ID_FIELD_NAME, acq)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName);

            if (filter != null && !filter.isEmpty())
                where.and().notIn(TransData.TYPE_FIELD_NAME, filter);

            if (!includeVoid) {
                where.and().ne(TransData.STATE_FIELD_NAME, TransData.ETransStatus.VOIDED);
            }

            if (isChkAuthCode) {
                where.and().isNotNull(TransData.APPR_CODE);
            }
            return where.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    /**
     * find transData
     *
     * @return transaction data list
     */
    @NonNull
    public List<TransData> findAllTransData(boolean isChkAuthCode, List<ETransType> filter) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.where()
                    .eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL).and()
                    .notIn(TransData.TYPE_FIELD_NAME, filter);
            if (isChkAuthCode) {
                where.and().isNotNull(TransData.APPR_CODE);
            }
            return where.query();
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    /**
     * find transData
     *
     * @return transaction data list
     */
    @NonNull
    public List<TransData> findAllTransDataByMerchant(boolean isChkAuthCode, List<ETransType> filter) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.where()
                    .eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL).and()
                    .eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName).and()
                    .notIn(TransData.TYPE_FIELD_NAME, filter);
            if (isChkAuthCode) {
                where.and().isNotNull(TransData.APPR_CODE);
            }
            return where.query();
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    /**
     * find transData by acquirer name
     *
     * @param isCheckStatus if true, get only success transaction
     * @return transaction data list
     */
    @NonNull
    public List<TransData> findQRSaleTransData(Acquirer acq, boolean isCheckStatus) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();

            List<ETransType> transTypes = new ArrayList<>();
            transTypes.add(ETransType.BPS_QR_SALE_INQUIRY);
            transTypes.add(ETransType.BPS_QR_INQUIRY_ID);

            Where<TransData, Integer> where = queryBuilder.where()
                    // AET-258
                    .eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL)
                    .and().eq(Acquirer.ID_FIELD_NAME, acq)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName)
                    .and().in(TransData.TYPE_FIELD_NAME, transTypes);
            if (isCheckStatus) {
                where.and().eq(TransData.QR_SALE_STATUS, TransData.QrSaleStatus.SUCCESS.toString());
            }
            return where.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    /**
     * find transData by acquirer name
     *
     * @param isCheckStatus       if true, get only success online transaction
     * @param isOnlyOffline       if true, get only offline transaction
     * @param isOfflineNotSuccess
     * @return transaction data list
     */
    @NonNull
    public List<TransData> findQRSaleTransData(Acquirer acq, boolean isCheckStatus, boolean isOnlyOffline, boolean isOfflineNotSuccess) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();

            List<ETransType> transTypes = new ArrayList<>();
            transTypes.add(ETransType.BPS_QR_SALE_INQUIRY);
            transTypes.add(ETransType.BPS_QR_INQUIRY_ID);

            Where<TransData, Integer> where = queryBuilder.where()
                    // AET-258
                    .eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL).and()
                    .eq(Acquirer.ID_FIELD_NAME, acq)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName)
                    .and().in(TransData.TYPE_FIELD_NAME, transTypes);
            if (isCheckStatus) {
                where.and().eq(TransData.QR_SALE_STATUS, TransData.QrSaleStatus.SUCCESS.toString());
                where.and().eq(TransData.QR_SALE_STATE, TransData.QrSaleState.QR_SEND_ONLINE);
            }
            if (isOnlyOffline) {
                if (isOfflineNotSuccess)
                    where.and().ne(TransData.QR_SALE_STATUS, TransData.QrSaleStatus.SUCCESS.toString());
                where.and().eq(TransData.QR_SALE_STATE, TransData.QrSaleState.QR_SEND_OFFLINE);
            }
            return where.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    /**
     * find wallet transData by trace No
     *
     * @return wallet transaction data list
     */
    @NonNull
    private List<TransData> findAllWalletSaleTransData(boolean includeReversal) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_WALLET);
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.where()
                    .eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName);
//                    .eq(TransData.TYPE_FIELD_NAME, ETransType.QR_SALE_WALLET);
            if (!includeReversal)
                where.and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);
            return where.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    /**
     * find sale qr visa trans
     *
     * @return wallet transaction data list
     */
    @NonNull
    private List<TransData> findAllQRVisaTransData(boolean includeReversal) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_QRC);
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.where()
                    .eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName);
            if (!includeReversal)
                where.and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

            return where.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    /**
     * find wallet transData by trace No
     *
     * @return wallet transaction data list
     */

    @NonNull
    private List<TransData> findAllKplusSaleTransData(boolean includeReversal) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_KPLUS);
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.where()
                    .eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName);
//                    .eq(TransData.TYPE_FIELD_NAME, ETransType.QR_SALE_WALLET);
            if (!includeReversal)
                where.and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);
            return where.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }


    /**
     * find wallet transData by trace No
     *
     * @return wallet transaction data list
     */
    @NonNull
    private List<TransData> findAllAlipaySaleTransData(boolean includeReversal) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_ALIPAY);
            Acquirer acquirerBsC = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_ALIPAY_B_SCAN_C);
            List<Acquirer> acqList = new ArrayList<>();
            acqList.add(acquirer);
            acqList.add(acquirerBsC);
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.where()
                    .eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName);
//                    .eq(TransData.TYPE_FIELD_NAME, ETransType.QR_SALE_WALLET);
            if (!includeReversal)
                where.and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);
            return where.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    /**
     * find wallet transData by trace No
     *
     * @return wallet transaction data list
     */
    @NonNull
    private List<TransData> findAllWechatSaleTransData(boolean includeReversal) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_WECHAT);
            Acquirer acquirerBsC = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_WECHAT_B_SCAN_C);
            List<Acquirer> acqList = new ArrayList<>();
            acqList.add(acquirer);
            acqList.add(acquirerBsC);
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.where()
                    .eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName);
//                    .eq(TransData.TYPE_FIELD_NAME, ETransType.QR_SALE_WALLET);
            if (!includeReversal)
                where.and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);
            return where.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    /**
     * find wallet transData by trace No
     *
     * @return wallet transaction data list
     */
    @NonNull
    private List<TransData> findAllQRCreditSaleTransData(boolean includeReversal) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_QR_CREDIT);
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.where()
                    .eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName);
            if (!includeReversal)
                where.and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);
            return where.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    /**
     * find All DCC currency of DCC Transactions
     */
    @NonNull
    public List<String[]> findAllDccCurrency() {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            Acquirer acq = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_DCC);
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder().selectColumns(TransData.DCC_CURRENCY_NAME_KBANK_FIELD_NAME);
            queryBuilder.where().eq(Acquirer.ID_FIELD_NAME, acq)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName)
                    .and().eq(TransData.DCC_REQUIRED_KBANK_FIELD_NAME, true)
                    .and().isNotNull(TransData.DCC_CURRENCY_NAME_KBANK_FIELD_NAME);
            queryBuilder.groupBy(TransData.DCC_CURRENCY_NAME_KBANK_FIELD_NAME);

            GenericRawResults<String[]> rawResults = queryBuilder.queryRaw();
            return rawResults.getResults();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    /**
     * find transData all Pre-Auth + Sale Comp by acquirer name
     *
     * @return transaction data list
     */
    @NonNull
    public List<TransData> findAllPreAuthSaleCompTransData(Acquirer acq) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.orderBy(TransData.TRACENO_FIELD_NAME, true)
                    .where().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

            List<ETransType> preAuthTypes = Arrays.asList(ETransType.PREAUTH, ETransType.PREAUTHORIZATION);
            List<ETransType> preAuthCancelSaleCompTypes = Arrays.asList(ETransType.PREAUTHORIZATION_CANCELLATION, ETransType.SALE_COMPLETION);

            where.or(
                    where.and(where.in(TransData.TYPE_FIELD_NAME, preAuthTypes),
                              where.eq(TransData.STATE_FIELD_NAME, TransData.ETransStatus.SALE_COMPLETED)),
                    where.and(
                            where.in(TransData.TYPE_FIELD_NAME, preAuthCancelSaleCompTypes),
                            where.eq(TransData.STATE_FIELD_NAME, TransData.ETransStatus.NORMAL)));

            where.and().eq(Acquirer.ID_FIELD_NAME, acq)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName);

            return where.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    /**
     * find transData all Pre-Auth (Normal State) by acquirer list
     *
     * @return transaction data list
     */
    @NonNull
    public List<TransData> findAllPreAuthTransDataWithNormalState(List<Acquirer> acquirers) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.orderBy(TransData.TRACENO_FIELD_NAME, true)
                    .where().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

            List<ETransType> preAuthTypes = Arrays.asList(ETransType.PREAUTH, ETransType.PREAUTHORIZATION);
            where.and().in(TransData.TYPE_FIELD_NAME, preAuthTypes)
                    .and().eq(TransData.STATE_FIELD_NAME, TransData.ETransStatus.NORMAL)
                    .and().in(Acquirer.ID_FIELD_NAME, acquirers)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName);

            return where.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    /**
     * find all pre-authorization transaction by acquirer
     *
     * @return pre-auth transaction data list
     */
    @NonNull
    public List<TransData> findAllPreAuthTransaction(boolean includeReversal) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder
                    .where().in(TransData.TYPE_FIELD_NAME, Arrays.asList(ETransType.PREAUTHORIZATION, ETransType.PREAUTHORIZATION_CANCELLATION))
                    .and().eq(TransData.STATE_FIELD_NAME, TransData.ETransStatus.NORMAL);
            if (!includeReversal)
                where.and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);
            return where.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    @NonNull
    public List<TransData> findAllPreAuthTransaction(boolean includeReversal, Acquirer acquirer) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder
                    .where().in(TransData.TYPE_FIELD_NAME, Arrays.asList(ETransType.PREAUTHORIZATION, ETransType.PREAUTHORIZATION_CANCELLATION))
                    .and().eq(TransData.STATE_FIELD_NAME, TransData.ETransStatus.NORMAL)
                    .and().eq(Acquirer.ID_FIELD_NAME, acquirer);
            if (!includeReversal)
                where.and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);
            return where.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    /**
     * find all pre-authorization cancellation and Sale Comp transaction
     *
     * @return pre-auth transaction data list
     */
    @NonNull
    public List<TransData> findAllPreAuthCancelAndSaleCompTransaction(boolean includeReversal, Acquirer acquirer) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder
                    .where().in(TransData.TYPE_FIELD_NAME, Arrays.asList(ETransType.PREAUTHORIZATION_CANCELLATION, ETransType.SALE_COMPLETION))
                    .and().in(TransData.STATE_FIELD_NAME, Arrays.asList(TransData.ETransStatus.NORMAL, TransData.ETransStatus.VOIDED, TransData.ETransStatus.ADJUSTED))
                    .and().eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName);
            if (!includeReversal)
                where.and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);
            return where.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    /**
     * find all pre-authorization transaction with Sale Comp and Void State
     *
     * @return pre-auth transaction data list
     */
    @NonNull
    public List<TransData> findAllPreAuthTransDataWithSaleCompVoidState(boolean includeReversal, Acquirer acquirer) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder
                    .where().in(TransData.TYPE_FIELD_NAME, Arrays.asList(ETransType.PREAUTH, ETransType.PREAUTHORIZATION))
                    .and().in(TransData.STATE_FIELD_NAME, Arrays.asList(TransData.ETransStatus.SALE_COMPLETED, TransData.ETransStatus.VOIDED))
                    .and().eq(Acquirer.ID_FIELD_NAME, acquirer);
            if (!includeReversal)
                where.and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);
            return where.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }


    /**
     * delete transData by id
     */
    public boolean deleteTransData(int id) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            dao.deleteById(id);
            return true;
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }
        return false;
    }

    /**
     * delete transData by trace no
     */
    public boolean deleteTransDataByTraceNo(long traceNo) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            DeleteBuilder<TransData, Integer> deleteBuilder = dao.deleteBuilder();
            deleteBuilder.where().eq(TransData.TRACENO_FIELD_NAME, traceNo);
            dao.delete(deleteBuilder.prepare());
            return true;
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return false;
    }

    /**
     * delete transData by trace no
     */
    public boolean deleteTransDataByBatchNo(Acquirer acquirer, long batchNo) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            DeleteBuilder<TransData, Integer> deleteBuilder = dao.deleteBuilder();
            deleteBuilder.where().eq(TransData.BATCHNO_FIELD_NAME, batchNo)
                    .and().eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName);
            dao.delete(deleteBuilder.prepare());
            return true;
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return false;
    }

    /**
     * delete transData by trace no
     */
    public boolean deleteAllTransData() {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            dao.delete(findAllTransData(true));
            return true;
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }
        return false;
    }

    /**
     * delete transData by acquirer name
     */
    public boolean deleteAllTransData(Acquirer acqname) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            dao.delete(findAllTransData(acqname, true, false, true));
            return true;
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }
        return false;
    }

    /**
     * delete transData by acquirer name
     */
    public boolean deleteAllTransData(Acquirer acquirer, boolean includeReversal) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            dao.delete(findAllTransData(includeReversal, acquirer));
            return true;
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }
        return false;
    }

    /**
     * delete transData except Pre-Auth by acquirer name
     */
    public boolean deleteAllTransDataExceptPreAuth(Acquirer acqname) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            dao.delete(findAllTransDataExceptPreAuth(acqname, true, false));
            return true;
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }
        return false;
    }

    /**
     * delete transData Pre-Auth Cancel and Sale Comp Type by acquirer name
     */
    public boolean deleteAllPreAuthCancelAndSaleCompTransData(Acquirer acq) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            dao.delete(findAllPreAuthCancelAndSaleCompTransaction(false, acq));
            return true;
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }
        return false;
    }

    /**
     * delete transData Sale Comp by acquirer name
     */
    public boolean deleteAllPreAuthTransDataWithSaleCompState(Acquirer acq) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            dao.delete(findAllPreAuthTransDataWithSaleCompVoidState(false, acq));
            return true;
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }
        return false;
    }

    /**
     * count of transaction data
     *
     * @return the count
     */
    public long countOf() {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            queryBuilder.where().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL)
                    .and().isNotNull(TransData.APPR_CODE);
            return queryBuilder.countOf();
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return 0;
    }

    /**
     * count of transaction data
     *
     * @return the count
     */
    public long countOfmerchant() {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            queryBuilder.where().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName)
                    .and().isNotNull(TransData.APPR_CODE);
            return queryBuilder.countOf();
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return 0;
    }

    /**
     * count of transaction data
     *
     * @return the count
     */
    public long countOfReversal() {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            queryBuilder.where().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.PENDING);
            return queryBuilder.countOf();
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return 0;
    }

    /**
     * count of transaction data by acquirer
     *
     * @return the count
     */
    public long countOf(Acquirer acquirer, boolean includeStateVoid) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.where()
                    .eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL).and()
                    .ne(TransData.TYPE_FIELD_NAME, ETransType.ADJUST).and()
                    .eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName);
            if (!includeStateVoid)
                where.and().ne(TransData.STATE_FIELD_NAME, TransData.ETransStatus.VOIDED);
            return queryBuilder.countOf();
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return 0;
    }

    /**
     * count of transaction data by acquirer
     *
     * @return the count
     */
    public long countOf(Acquirer acquirer, boolean includeStateVoid, List<ETransType> filter) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.where()
                    .eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL).and()
                    .eq(Acquirer.ID_FIELD_NAME, acquirer).and()
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName)
                    .notIn(TransData.TYPE_FIELD_NAME, filter);
            if (!includeStateVoid)
                where.and().ne(TransData.STATE_FIELD_NAME, TransData.ETransStatus.VOIDED);
            return queryBuilder.countOf();
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return 0;
    }

    /**
     * count of transaction data with filter
     *
     * @param acquirer acquirer
     * @param statuses {@link TransData.ETransStatus}
     * @param issuers  issuer
     * @return count total
     */
    public long countOf(Acquirer acquirer, List<TransData.ETransStatus> statuses, List<Issuer> issuers) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransData, Integer> where = queryBuilder.where().in(TransData.STATE_FIELD_NAME, statuses)
                    .and().eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName)
                    .and().in(Issuer.ID_FIELD_NAME, issuers)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_INFO)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.GET_QR_WALLET)
                    .and().notIn(TransData.TYPE_FIELD_NAME, ETransType.ADJUST)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

            if (acquirer.getName().equals(Constants.ACQ_QRC))
                where.and().isNotNull(TransData.APPR_CODE);

            return queryBuilder.countOf();
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return 0;
    }

    private long[] getRawResults(List<String[]> results) {
        long[] obj = new long[]{0, 0};
        if (results != null && !results.isEmpty()) {
            String[] value = results.get(0);
            obj[0] = value[0] == null ? 0 : Utils.parseLongSafe(value[0], 0);
            obj[1] = value[1] == null ? 0 : Utils.parseLongSafe(value[1], 0);
        }
        return obj;
    }

    private long[] getRawResultsRedeemKbank(List<String[]> results) {
        long[] obj = new long[]{0, 0, 0, 0, 0, 0};
        if (results != null && !results.isEmpty()) {
            String[] value = results.get(0);
            obj[0] = value[0] == null ? 0 : Utils.parseLongSafe(value[0], 0);
            obj[1] = value[1] == null ? 0 : Utils.parseLongSafe(value[1], 0);
            obj[2] = value[2] == null ? 0 : Utils.parseLongSafe(value[2], 0);
            obj[3] = value[3] == null ? 0 : Utils.parseLongSafe(value[3], 0);
            obj[4] = value[4] == null ? 0 : Utils.parseLongSafe(value[4], 0);
            obj[5] = value[5] == null ? 0 : Utils.parseLongSafe(value[5], 0);
        }
        return obj;
    }

    /**
     * count and sum of transaction data with filter
     *
     * @param type {@link ETransType}
     * @return [0]count, [1]sum
     */
    public long[] countSumOf(ETransType type) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder =
                    dao.queryBuilder().selectRaw(COUNT_SUM_RAW);
            queryBuilder.where().eq(TransData.TYPE_FIELD_NAME, type)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

            GenericRawResults<String[]> rawResults = dao.queryRaw(queryBuilder.prepare().getStatement());
            return getRawResults(rawResults.getResults());
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return new long[]{0, 0};
    }

    /**
     * count and sum of transaction data with filter
     *
     * @param type   {@link ETransType}
     * @param status {@link TransData.ETransStatus}
     * @return [0]count, [1]sum
     */
    public long[] countSumOf(ETransType type, TransData.ETransStatus status) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder =
                    dao.queryBuilder().selectRaw(COUNT_SUM_RAW);
            queryBuilder.where().eq(TransData.TYPE_FIELD_NAME, type)
                    .and().eq(TransData.STATE_FIELD_NAME, status)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

            GenericRawResults<String[]> rawResults = queryBuilder.queryRaw();
            return getRawResults(rawResults.getResults());
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return new long[]{0, 0};
    }

    /**
     * count and sum of transaction data with filter
     *
     * @param type     {@link ETransType}
     * @param statuses {@link TransData.ETransStatus}
     * @return [0]count, [1]sum
     */
    public long[] countSumOf(ETransType type, List<TransData.ETransStatus> statuses) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder =
                    dao.queryBuilder().selectRaw(COUNT_SUM_RAW);
            queryBuilder.where().eq(TransData.TYPE_FIELD_NAME, type)
                    .and().in(TransData.STATE_FIELD_NAME, statuses)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

            GenericRawResults<String[]> rawResults = dao.queryRaw(queryBuilder.prepare().getStatement());
            return getRawResults(rawResults.getResults());
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return new long[]{0, 0};

    }

    /**
     * count and sum of transaction data with filter
     *
     * @param acquirer acquirer
     * @param type     {@link ETransType}
     * @return [0]count, [1]sum
     */
    public long[] countSumOf(Acquirer acquirer, ETransType type) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder =
                    dao.queryBuilder().selectRaw(COUNT_SUM_RAW);
            queryBuilder.where().eq(TransData.TYPE_FIELD_NAME, type)
                    .and().eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

            GenericRawResults<String[]> rawResults = queryBuilder.queryRaw();
            return getRawResults(rawResults.getResults());
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return new long[]{0, 0};
    }


    /**
     * count and sum of transaction data with filter
     *
     * @param acquirer acquirer
     * @param type     {@link ETransType}
     * @param status   {@link TransData.ETransStatus}
     * @return [0]count, [1]sum
     */
    public long[] countSumOf(Acquirer acquirer, ETransType type, TransData.ETransStatus status) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder =
                    dao.queryBuilder().selectRaw(COUNT_SUM_RAW);
            queryBuilder.where().eq(TransData.TYPE_FIELD_NAME, type)
                    .and().eq(TransData.STATE_FIELD_NAME, status)
                    .and().eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

            GenericRawResults<String[]> rawResults = queryBuilder.queryRaw();
            return getRawResults(rawResults.getResults());
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return new long[]{0, 0};
    }

    /**
     * count and sum of transaction data with filter
     *
     * @param acquirer acquirer
     * @param type     {@link ETransType}
     * @param status   {@link TransData.ETransStatus}
     * @return [0]count, [1]sum
     */
    public long[] countSumOf(Acquirer acquirer, ETransType type, TransData.ETransStatus status, boolean isSmallAmt) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder =
                    dao.queryBuilder().selectRaw(COUNT_SUM_RAW);
            queryBuilder.where().eq(TransData.TYPE_FIELD_NAME, type)
                    .and().eq(TransData.STATE_FIELD_NAME, status)
                    .and().eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL)
                    .and().eq(TransData.TXN_SMALL_AMT, isSmallAmt);

            GenericRawResults<String[]> rawResults = queryBuilder.queryRaw();
            return getRawResults(rawResults.getResults());
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return new long[]{0, 0};
    }


    /**
     * count and sum of transaction data with filter
     *
     * @param acquirer acquirer
     * @param type     {@link ETransType}
     * @param status   {@link TransData.ETransStatus}
     * @return [0]count, [1]sum
     */
    public long[] countSumOf(Acquirer acquirer, Issuer issuer, ETransType type, TransData.ETransStatus status) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder =
                    dao.queryBuilder().selectRaw(COUNT_SUM_RAW);
            queryBuilder.where().eq(TransData.TYPE_FIELD_NAME, type)
                    .and().eq(TransData.STATE_FIELD_NAME, status)
                    .and().eq(Issuer.ID_FIELD_NAME, issuer)
                    .and().eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

            GenericRawResults<String[]> rawResults = queryBuilder.queryRaw();
            return getRawResults(rawResults.getResults());
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return new long[]{0, 0};
    }

    /**
     * count and sum of transaction data with filter
     *
     * @param acquirer acquirer
     * @param type     {@link ETransType}
     * @param status   {@link TransData.ETransStatus}
     * @return [0]count, [1]sum
     */
    public long[] countSumOf(Acquirer acquirer, Issuer issuer, ETransType type, TransData.ETransStatus status, boolean isSmallAmt) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder =
                    dao.queryBuilder().selectRaw(COUNT_SUM_RAW);
            queryBuilder.where().eq(TransData.TYPE_FIELD_NAME, type)
                    .and().eq(TransData.STATE_FIELD_NAME, status)
                    .and().eq(Issuer.ID_FIELD_NAME, issuer)
                    .and().eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL)
                    .and().eq(TransData.TXN_SMALL_AMT, isSmallAmt);

            GenericRawResults<String[]> rawResults = queryBuilder.queryRaw();
            return getRawResults(rawResults.getResults());
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return new long[]{0, 0};
    }

    /**
     * count and sum of transaction data with filter
     *
     * @param acquirer        acquirer
     * @param type            {@link ETransType}
     * @param statuses        {@link TransData.ETransStatus}
     * @param isQrSale        if true, includes PromptPay transaction (only online success trans.)
     * @param isOnlyQrOffline if true, get only offline trans. (PromptPay)
     * @return [0]count, [1]sum
     */
    public long[] countSumOf(Acquirer acquirer, ETransType type, List<TransData.ETransStatus> statuses, boolean isQrSale, boolean isOnlyQrOffline) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder =
                    dao.queryBuilder().selectRaw(COUNT_SUM_RAW);
            Where<TransData, Integer> where = queryBuilder.where().eq(TransData.TYPE_FIELD_NAME, type)
                    .and().in(TransData.STATE_FIELD_NAME, statuses)
                    .and().eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

            if (isQrSale) {
                where.and().eq(TransData.QR_SALE_STATUS, TransData.QrSaleStatus.SUCCESS.toString());
                where.and().eq(TransData.QR_SALE_STATE, TransData.QrSaleState.QR_SEND_ONLINE);
            }
            if (isOnlyQrOffline)
                where.and().eq(TransData.QR_SALE_STATE, TransData.QrSaleState.QR_SEND_OFFLINE);

            GenericRawResults<String[]> rawResults = queryBuilder.queryRaw();
            return getRawResults(rawResults.getResults());
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return new long[]{0, 0};
    }

    /**
     * count and sum of transaction data with filter
     *
     * @param acquirer        acquirer
     * @param type            {@link ETransType}
     * @param statuses        {@link TransData.ETransStatus}
     * @param isQrSale        if true, includes PromptPay transaction (only online success trans.)
     * @param isOnlyQrOffline if true, get only offline trans. (PromptPay)
     * @param isSmallAmt      if true, get only small amount trans.
     * @return [0]count, [1]sum
     */
    public long[] countSumOf(Acquirer acquirer, ETransType type, List<TransData.ETransStatus> statuses, boolean isQrSale, boolean isOnlyQrOffline, boolean isSmallAmt) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder =
                    dao.queryBuilder().selectRaw(COUNT_SUM_RAW);
            Where<TransData, Integer> where = queryBuilder.where().eq(TransData.TYPE_FIELD_NAME, type)
                    .and().in(TransData.STATE_FIELD_NAME, statuses)
                    .and().eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL)
                    .and().eq(TransData.TXN_SMALL_AMT, isSmallAmt);

            if (isQrSale) {
                where.and().eq(TransData.QR_SALE_STATUS, TransData.QrSaleStatus.SUCCESS.toString());
                where.and().eq(TransData.QR_SALE_STATE, TransData.QrSaleState.QR_SEND_ONLINE);
            }
            if (isOnlyQrOffline)
                where.and().eq(TransData.QR_SALE_STATE, TransData.QrSaleState.QR_SEND_OFFLINE);

            GenericRawResults<String[]> rawResults = queryBuilder.queryRaw();
            return getRawResults(rawResults.getResults());
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return new long[]{0, 0};
    }

    /**
     * count and sum of transaction data with filter
     *
     * @param acquirer acquirer
     * @param statuses {@link TransData.ETransStatus}
     * @param issuer   issuer
     * @return [0]count, [1]sum
     */
    public long[] countSumOf(Acquirer acquirer, List<TransData.ETransStatus> statuses, Issuer issuer) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder =
                    dao.queryBuilder().selectRaw(COUNT_SUM_RAW);
            queryBuilder.where().in(TransData.STATE_FIELD_NAME, statuses)
                    .and().eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName)
                    .and().eq(Issuer.ID_FIELD_NAME, issuer)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

            GenericRawResults<String[]> rawResults = queryBuilder.queryRaw();
            return getRawResults(rawResults.getResults());
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return new long[]{0, 0};
    }

    /**
     * count and sum of transaction data with filter
     *
     * @param acquirer        acquirer
     * @param type            {@link ETransType}
     * @param statuses        {@link TransData.ETransStatus}
     * @param issuer          issuer
     * @param isOnlyQrOffline
     * @return [0]count, [1]sum
     */
    public long[] countSumOf(Acquirer acquirer, ETransType type, List<TransData.ETransStatus> statuses, Issuer issuer, boolean isOnlyQrOffline) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder =
                    dao.queryBuilder().selectRaw(COUNT_SUM_RAW);
            Where<TransData, Integer> where = queryBuilder.where().eq(TransData.TYPE_FIELD_NAME, type)
                    .and().in(TransData.STATE_FIELD_NAME, statuses)
                    .and().eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName)
                    .and().eq(Issuer.ID_FIELD_NAME, issuer)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

            if (type == ETransType.BPS_QR_SALE_INQUIRY) {
                if (isOnlyQrOffline) {
                    where.and().eq(TransData.QR_SALE_STATE, TransData.QrSaleState.QR_SEND_OFFLINE);
                } else {
                    where.and().eq(TransData.QR_SALE_STATUS, TransData.QrSaleStatus.SUCCESS.toString());
                    where.and().eq(TransData.QR_SALE_STATE, TransData.QrSaleState.QR_SEND_ONLINE);
                }
            }

            GenericRawResults<String[]> rawResults = queryBuilder.queryRaw();
            return getRawResults(rawResults.getResults());
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return new long[]{0, 0};
    }

    /**
     * count and sum of transaction data with filter
     *
     * @param acquirer        acquirer
     * @param type            {@link ETransType}
     * @param statuses        {@link TransData.ETransStatus}
     * @param issuer          issuer
     * @param isOnlyQrOffline
     * @return [0]count, [1]sum
     */
    public long[] countSumOf(Acquirer acquirer, ETransType type, List<TransData.ETransStatus> statuses, Issuer issuer, boolean isOnlyQrOffline, boolean isSmallAmt) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder =
                    dao.queryBuilder().selectRaw(COUNT_SUM_RAW);
            Where<TransData, Integer> where = queryBuilder.where().eq(TransData.TYPE_FIELD_NAME, type)
                    .and().in(TransData.STATE_FIELD_NAME, statuses)
                    .and().eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(Issuer.ID_FIELD_NAME, issuer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL)
                    .and().eq(TransData.TXN_SMALL_AMT, isSmallAmt);

            if (type == ETransType.BPS_QR_SALE_INQUIRY) {
                if (isOnlyQrOffline) {
                    where.and().eq(TransData.QR_SALE_STATE, TransData.QrSaleState.QR_SEND_OFFLINE);
                } else {
                    where.and().eq(TransData.QR_SALE_STATUS, TransData.QrSaleStatus.SUCCESS.toString());
                    where.and().eq(TransData.QR_SALE_STATE, TransData.QrSaleState.QR_SEND_ONLINE);
                }
            }

            GenericRawResults<String[]> rawResults = queryBuilder.queryRaw();
            return getRawResults(rawResults.getResults());
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return new long[]{0, 0};
    }

    /**
     * count and sum of Wallet transaction data with filter
     *
     * @param acquirer acquirer
     * @param statuses {@link TransData.ETransStatus}
     * @param issuer   issuer
     * @return [0]count, [1]sum
     */
    public List<String[]> countSumOfWallet(Acquirer acquirer, List<TransData.ETransStatus> statuses, Issuer issuer) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder =
                    dao.queryBuilder().selectRaw(COUNT_SUM_RAW_WALLET);
            queryBuilder.where().in(TransData.STATE_FIELD_NAME, statuses)
                    .and().eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName)
//                    .and().eq(Issuer.ID_FIELD_NAME, issuer)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

            queryBuilder.groupBy(TransData.WALLET_NAME);

            GenericRawResults<String[]> rawResults = queryBuilder.queryRaw();
            return rawResults.getResults();
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    /**
     * count and sum of Wallet transaction data with filter
     *
     * @param acquirer acquirer
     * @param type     {@link ETransType}
     * @param statuses {@link TransData.ETransStatus}
     * @param issuer   issuer
     * @return [0]count, [1]sum
     */
    public long[] countSumOfWallet(Acquirer acquirer, ETransType type, List<TransData.ETransStatus> statuses, Issuer issuer, String walletName) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder =
                    dao.queryBuilder().selectRaw(COUNT_SUM_RAW);
            queryBuilder.where().eq(TransData.TYPE_FIELD_NAME, type)
                    .and().in(TransData.STATE_FIELD_NAME, statuses)
                    .and().eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName)
                    .and().eq(TransData.WALLET_NAME, walletName)
//                    .and().eq(Issuer.ID_FIELD_NAME, issuer)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

            queryBuilder.groupBy(TransData.WALLET_NAME);

            GenericRawResults<String[]> rawResults = queryBuilder.queryRaw();
            return getRawResults(rawResults.getResults());
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return new long[]{0, 0};
    }

    /**
     * count and sum of transaction data with filter
     *
     * @param type     {@link ETransType}
     * @param statuses {@link TransData.ETransStatus}
     * @return [0]count, [1]sumQty, [2]sumPoints, [3]sumAmt, [4]sumCredit
     */
    public long[] countSumOfRedeemKbankByIssuer(Acquirer acquirer, ETransType type, List<TransData.ETransStatus> statuses, List<Issuer> issuersIn, List<Issuer> issuersNotIn) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            if (issuersIn != null || issuersNotIn != null) {
                RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
                QueryBuilder<TransData, Integer> queryBuilder =
                        dao.queryBuilder().selectRaw(COUNT_SUM_REDEEM_KBANK);
                Where<TransData, Integer> where = queryBuilder.where().eq(TransData.TYPE_FIELD_NAME, type)
                        .and().in(TransData.STATE_FIELD_NAME, statuses)
                        .and().eq(Acquirer.ID_FIELD_NAME, acquirer)
                        .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName);

                if (issuersIn != null) {
                    where.and().in(Issuer.ID_FIELD_NAME, issuersIn);
                } else if (issuersNotIn != null) {
                    where.and().notIn(Issuer.ID_FIELD_NAME, issuersNotIn);
                }
                where.and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

                GenericRawResults<String[]> rawResults = queryBuilder.queryRaw();
                return getRawResultsRedeemKbank(rawResults.getResults());
            }
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return new long[]{0, 0, 0, 0, 0, 0};
    }

    public long[] countSumOfRedeemKbank(Acquirer acquirer, ETransType type, List<TransData.ETransStatus> statuses) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder =
                    dao.queryBuilder().selectRaw(COUNT_SUM_REDEEM_KBANK_CREDITSALE);
            Where<TransData, Integer> where = queryBuilder.where().eq(TransData.TYPE_FIELD_NAME, type)
                    .and().in(TransData.STATE_FIELD_NAME, statuses)
                    .and().eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);
            GenericRawResults<String[]> rawResults = queryBuilder.queryRaw();
            return getRawResultsRedeemKbank(rawResults.getResults());
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return new long[]{0, 0, 0, 0, 0, 0};
    }


    /**
     * count and sum of dcc transaction
     *
     * @param acquirer Acquirer
     * @param type Trans Type
     * @param statuses List of Trans Status
     * @param currency Currency in String
     * @return
     */
    public String[] countSumOfDccKbank(Acquirer acquirer, ETransType type, List<TransData.ETransStatus> statuses, String currency) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder().selectRaw(COUNT_SUM_DCC_KBANK);

            queryBuilder.where().eq(TransData.TYPE_FIELD_NAME, type)
                    .and().in(TransData.STATE_FIELD_NAME, statuses)
                    .and().eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL)
                    .and().eq(TransData.DCC_CURRENCY_NAME_KBANK_FIELD_NAME, currency);

            queryBuilder.groupBy(TransData.DCC_CURRENCY_NAME_KBANK_FIELD_NAME);

            return queryBuilder.queryRawFirst();
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return null;
    }

    /**
     * count and sum of dcc transaction
     *
     * @param acquirer Acquirer
     * @param issuer Issuer
     * @param type Trans Type
     * @param statuses List of Trans Status
     * @param currency Currency in String
     * @return
     */
    public String[] countSumOfDccKbank(Acquirer acquirer, Issuer issuer, ETransType type, List<TransData.ETransStatus> statuses, String currency) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder().selectRaw(COUNT_SUM_DCC_KBANK);

            queryBuilder.where().eq(TransData.TYPE_FIELD_NAME, type)
                    .and().in(TransData.STATE_FIELD_NAME, statuses)
                    .and().eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName)
                    .and().eq(Issuer.ID_FIELD_NAME, issuer)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL)
                    .and().eq(TransData.DCC_CURRENCY_NAME_KBANK_FIELD_NAME, currency);

            queryBuilder.groupBy(TransData.DCC_CURRENCY_NAME_KBANK_FIELD_NAME);

            return queryBuilder.queryRawFirst();
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return null;
    }

    /**
     * List of currency code in TransData
     *
     * @param acquirer
     * @return
     */
    public List<Object[]> groupCurrencyDccKbank(Acquirer acquirer) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder().selectColumns(TransData.DCC_CURRENCY_CODE_KBANK_FIELD_NAME, TransData.DCC_CURRENCY_NAME_KBANK_FIELD_NAME);

            queryBuilder.where().eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName)
					.and().ne(TransData.TYPE_FIELD_NAME, ETransType.ADJUST);

            queryBuilder.groupBy(TransData.DCC_CURRENCY_CODE_KBANK_FIELD_NAME);
            queryBuilder.orderBy(TransData.DCC_CURRENCY_NAME_KBANK_FIELD_NAME, true);

            GenericRawResults<Object[]> rawResults = dao.queryRaw(queryBuilder.prepare().getStatement(), new DataType[]{DataType.BYTE_ARRAY});

            return rawResults.getResults();
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    /**
     * List of currency code in TransData
     *
     * @param acquirer
     * @param issuer
     * @return
     */
    public List<Object[]> groupCurrencyDccKbank(Acquirer acquirer, Issuer issuer) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder().selectColumns(TransData.DCC_CURRENCY_CODE_KBANK_FIELD_NAME, TransData.DCC_CURRENCY_NAME_KBANK_FIELD_NAME);

            queryBuilder.where().eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName)
					.and().ne(TransData.TYPE_FIELD_NAME, ETransType.ADJUST)
                    .and().eq(Issuer.ID_FIELD_NAME, issuer);

            queryBuilder.groupBy(TransData.DCC_CURRENCY_CODE_KBANK_FIELD_NAME);
            queryBuilder.orderBy(TransData.DCC_CURRENCY_NAME_KBANK_FIELD_NAME, true);

            GenericRawResults<Object[]> rawResults = dao.queryRaw(queryBuilder.prepare().getStatement(), new DataType[]{DataType.BYTE_ARRAY});

            return rawResults.getResults();
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    /**
     * read the first reversal record
     *
     * @return transaction data object, if the record is not existed, return null
     */
    public TransData findFirstDupRecord() {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            List<Acquirer> acqs = new ArrayList<>();
            acqs.add(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_WALLET));
            queryBuilder.where().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.PENDING).and()
                    .notIn(Acquirer.ID_FIELD_NAME, acqs);
            return queryBuilder.queryForFirst();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return null;
    }

    /**
     * read the first reversal record by Acquirer
     *
     * @param acquirer
     * @return transaction data object, if the record is not existed, return null
     */
    public TransData findFirstDupRecord(Acquirer acquirer) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            queryBuilder.where().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.PENDING).and()
                    .eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName);
            return queryBuilder.queryForFirst();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return null;
    }

    /**
     * delete reversal record
     */
    public boolean deleteDupRecord() {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            DeleteBuilder<TransData, Integer> deleteBuilder = dao.deleteBuilder();
            List<Acquirer> acqs = new ArrayList<>();
            acqs.add(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_WALLET));
            deleteBuilder.where().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.PENDING).and()
                    .notIn(Acquirer.ID_FIELD_NAME, acqs);
            deleteBuilder.delete();
            return true;
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return false;
    }

    /**
     * delete reversal record all Acquirers
     */
    public boolean deleteAllDupRecord() {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            DeleteBuilder<TransData, Integer> deleteBuilder = dao.deleteBuilder();
            deleteBuilder.where().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.PENDING);
            deleteBuilder.delete();
            return true;
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return false;
    }

    /**
     * delete reversal record by Acquirer
     */
    public boolean deleteDupRecord(Acquirer acquirer) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            DeleteBuilder<TransData, Integer> deleteBuilder = dao.deleteBuilder();
            deleteBuilder.where().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.PENDING)
                    .and().eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName);
            deleteBuilder.delete();
            return true;
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return false;
    }

    /**
     * read the first reversal record of MyPrompt transaction
     *
     * @return transaction data object, if the record is not existed, return null
     */
    public TransData findFirstDupRecordMyPrompt() {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_MY_PROMPT);
            queryBuilder.where().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.PENDING).and()
                    .eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName);
            return queryBuilder.queryForFirst();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return null;
    }

    /**
     * read the first reversal record of Wallet transaction
     *
     * @return transaction data object, if the record is not existed, return null
     */
    public TransData findFirstDupRecordWallet() {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_WALLET);
            queryBuilder.where().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.PENDING).and()
                    .eq(Acquirer.ID_FIELD_NAME, acquirer);
            return queryBuilder.queryForFirst();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return null;
    }

    /**
     * read the first reversal record of Wallet transaction
     *
     * @return transaction data object, if the record is not existed, return null
     */
    public TransData findFirstDupRecordPromptPay() {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_QR_PROMPT);
            queryBuilder.where().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.PENDING).and()
                    .eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName);
            return queryBuilder.queryForFirst();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return null;
    }

    /**
     * read the first reversal record of QRSALE transaction
     *
     * @return transaction data object, if the record is not existed, return null
     */
    public TransData findFirstDupRecordQR(Acquirer acquirer) {
        try {
            String merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            queryBuilder.where().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.PENDING).and()
                    .eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, merchantName);
            return queryBuilder.queryForFirst();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return null;
    }

    public TransData findFirstDupRecordAlipay() {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_ALIPAY);
            queryBuilder.where().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.PENDING).and()
                    .eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, MerchantProfileManager.INSTANCE.getCurrentMerchant());
            return queryBuilder.queryForFirst();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return null;
    }

    public TransData findFirstDupRecordWechat() {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_WECHAT);
            queryBuilder.where().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.PENDING).and()
                    .eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, MerchantProfileManager.INSTANCE.getCurrentMerchant());
            return queryBuilder.queryForFirst();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return null;
    }

    /**
     * read the first advice record by acquirer
     *
     * @return transaction data object, if the record is not existed, return null
     */
    public TransData findAdviceRecord(Acquirer acquirer) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            queryBuilder.where().eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, MerchantProfileManager.INSTANCE.getCurrentMerchant())
                    .and().eq(TransData.ADVICE_FIELD_NAME, TransData.AdviceStatus.PENDING);
            return queryBuilder.queryForFirst();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return null;
    }

    /**
     * read list of advice records by acquirer
     *
     * @return list of transaction data object, if the record is not existed, return empty list
     */
    public List<TransData> findAllAdviceRecord(Acquirer acquirer) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            queryBuilder.where().eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, MerchantProfileManager.INSTANCE.getCurrentMerchant())
                    .and().eq(TransData.ADVICE_FIELD_NAME, TransData.AdviceStatus.PENDING);
            return queryBuilder.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    /**
     * delete reversal record of Wallet transaction
     */
    public boolean deleteDupRecordWallet() {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            DeleteBuilder<TransData, Integer> deleteBuilder = dao.deleteBuilder();
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_WALLET);
            deleteBuilder.where().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.PENDING).and()
                    .eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, MerchantProfileManager.INSTANCE.getCurrentMerchant());
            deleteBuilder.delete();
            return true;
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return false;
    }

    /**
     * delete reversal record of QR transaction
     */
    public boolean deleteDupRecordQr() {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            DeleteBuilder<TransData, Integer> deleteBuilder = dao.deleteBuilder();
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_QRC);
            deleteBuilder.where().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.PENDING).and()
                    .eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, MerchantProfileManager.INSTANCE.getCurrentMerchant());
            deleteBuilder.delete();
            return true;
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return false;
    }

    /**
     * find transTotal by batch No
     *
     * @param refNo reference no.
     * @return data object
     */
    public TransData findTransDataByRefNo(String refNo) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            queryBuilder.where().eq(TransData.REF_NO, refNo);
            return queryBuilder.queryForFirst();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return null;
    }

    public List<TransData> findAllTransDatatest() {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            return dao.queryForAll();
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    /**
     * read the first reversal record of QRSALE transaction
     *
     * @return transaction data object, if the record is not existed, return null
     */
    public TransData findFirstDupRecordQRSale() {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_QRC);
            queryBuilder.where().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.PENDING).and()
                    .eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, MerchantProfileManager.INSTANCE.getCurrentMerchant());
            return queryBuilder.queryForFirst();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return null;
    }

    /**
     * delete reversal record of QRSALE transaction
     */
    public boolean deleteDupRecordQRSale() {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            DeleteBuilder<TransData, Integer> deleteBuilder = dao.deleteBuilder();
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_QRC);
            deleteBuilder.where().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.PENDING).and()
                    .eq(Acquirer.ID_FIELD_NAME, acquirer)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, MerchantProfileManager.INSTANCE.getCurrentMerchant());
            deleteBuilder.delete();
            return true;
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return false;
    }

    public List<TransData> findAllEReceiptPending(List<Acquirer> acquirers, int retry) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();

            Where<TransData, Integer> where = queryBuilder.where()
                    .in(Acquirer.ID_FIELD_NAME, acquirers)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, MerchantProfileManager.INSTANCE.getCurrentMerchant())
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL)
                    .and().eq(TransData.ESLIP_UPLOAD_FIELD_NAME, TransData.UploadStatus.PENDING)
                    .and().lt(TransData.ESLIP_RETRY_FIELD_NAME, retry);

            return where.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    public long findAllEReceiptPending(List<Acquirer> acquirers) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();

            Where<TransData, Integer> where = queryBuilder.where()
                    .in(Acquirer.ID_FIELD_NAME, acquirers)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, MerchantProfileManager.INSTANCE.getCurrentMerchant())
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL)
                    .and().eq(TransData.ESLIP_UPLOAD_FIELD_NAME, TransData.UploadStatus.PENDING);

            return where.countOf();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }

        return 0;
    }

    public List<TransData> findAllEReceiptUploadFail(List<Acquirer> acquirers) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();

            Where<TransData, Integer> where = queryBuilder.where()
                    .eq(TransData.ESLIP_UPLOAD_FIELD_NAME, TransData.UploadStatus.UPLOAD_FAILED_MANUAL_PRINT)
                    .and().in(Acquirer.ID_FIELD_NAME, acquirers)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, MerchantProfileManager.INSTANCE.getCurrentMerchant())
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

            return where.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    public LinkedHashMap<String, String> countOfEReceiptStatus() {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        try {
            String groupColumn = TransData.ESLIP_UPLOAD_FIELD_NAME;
            String rawQuery = TransData.ESLIP_UPLOAD_FIELD_NAME + ", COUNT(" + TransData.ESLIP_UPLOAD_FIELD_NAME + ")";

            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder().selectRaw(rawQuery).groupBy(groupColumn);

            GenericRawResults<String[]> rawResults = dao.queryRaw(queryBuilder.prepare().getStatement());

            List<String[]> resultList = rawResults.getResults();
            if (resultList != null && !resultList.isEmpty()) {
                for (String[] data : resultList) {
                    result.put(data[0], data[1]);
                }
            }

            return result;
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }

        return result;
    }

    public int[] countEReceiptTxnByType() {
        try {
            String countSignOnPaper = "COUNT(CASE WHEN signData IS NULL THEN 1 ELSE NULL END)";
            String countTxnWithPin = "COUNT(CASE WHEN pinVerifyMsg = 1 THEN 1 ELSE NULL END)";
            String countTxnSmallAmt = "COUNT(CASE WHEN isTxnSmallAmt = 1 THEN 1 ELSE NULL END)";
            String countSignOnScreen = "COUNT(CASE WHEN signData IS NOT NULL THEN 1 ELSE NULL END)";

            String rawQuery = countSignOnPaper + ", " + countTxnWithPin + ", " + countTxnSmallAmt + ", " + countSignOnScreen;

            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder().selectRaw(rawQuery);

            queryBuilder.where().isNotNull(TransData.ESLIP_UPLOAD_FIELD_NAME);

            GenericRawResults<String[]> rawResults = dao.queryRaw(queryBuilder.prepare().getStatement());

            List<String[]> resultList = rawResults.getResults();
            if (resultList != null && !resultList.isEmpty()) {
                String[] sResult = resultList.get(0);
                return new int[]{Integer.parseInt(sResult[0]), Integer.parseInt(sResult[1]), Integer.parseInt(sResult[2]), Integer.parseInt(sResult[3])};
            }
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return new int[]{0, 0, 0, 0};
    }

    public List<TransData> findAllTransDataWithEReceiptUploadStatus(boolean onlyFailStatus) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder().orderBy(TransData.TRACENO_FIELD_NAME, true);

            Where<TransData, Integer> where = queryBuilder.where();

            if (onlyFailStatus) {
                where.eq(TransData.ESLIP_UPLOAD_FIELD_NAME, TransData.UploadStatus.UPLOAD_FAILED_MANUAL_PRINT);
            } else {
                where.isNotNull(TransData.ESLIP_UPLOAD_FIELD_NAME);
            }

            where.and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

            return where.query();
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    public List<TransData> findAllTransDataErmPendingUpload(boolean onlyNeverPrintBefore) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder().orderBy(TransData.TRACENO_FIELD_NAME, true);

            Where<TransData, Integer> where = queryBuilder.where();
            if (onlyNeverPrintBefore == true) {
                where.ne(TransData.ESLIP_UPLOAD_FIELD_NAME, TransData.UploadStatus.NORMAL);
                where.and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);
                where.and().eq(TransData.ERECEIPT_PRINT_COUNT, "0");
            } else if (onlyNeverPrintBefore == false) {
                where.ne(TransData.ESLIP_UPLOAD_FIELD_NAME, TransData.UploadStatus.NORMAL);
                where.and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);
            }
            return where.query();
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    public int findCountTransDataWithEReceiptUploadStatus(boolean onlyFailStatus) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder().orderBy(TransData.TRACENO_FIELD_NAME, true);

            Where<TransData, Integer> where = queryBuilder.where();

            if (onlyFailStatus) {
                where.ne(TransData.ESLIP_UPLOAD_FIELD_NAME, TransData.UploadStatus.NORMAL);
            } else {
                where.isNotNull(TransData.ESLIP_UPLOAD_FIELD_NAME);
            }

            where.and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

            return where.query().toArray().length;
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return 0;//new ArrayList<>(0);
    }

    public TransData findFistTransDataWithEReceiptPedingStatus() {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder().orderBy(TransData.STANNO_FIELD_NAME, true);

            Where<TransData, Integer> where = queryBuilder.where();
            where.ne(TransData.ESLIP_UPLOAD_FIELD_NAME, TransData.UploadStatus.NORMAL);
            //where.ne(TransData.ID_FIELD_NAME, ignorTransID);
            where.and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);
            if (where.query().toArray().length > 0) {
                return (TransData) where.query().toArray()[0];
            } else {
                return null;
            }
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return null; //new ArrayList<>(0);
    }

    public TransData findTransDataByStanNo(long stanNo) {
        try {
            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder();

            Where<TransData, Integer> where = queryBuilder.where()
                    .eq(TransData.STANNO_FIELD_NAME, stanNo)
                    .and().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL);

            return where.queryForFirst();
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return null;
    }

    public long[] countERMReport(boolean isERMUnsucessful, boolean isVoid) {
        try {
//            String countTotal = "COUNT(CASE WHEN " + TransData.STATE_FIELD_NAME + " == '" + TransData.ETransStatus.NORMAL + "' THEN 1 ELSE NULL END)";
//            String sumTotal = "SUM(CASE WHEN " + TransData.STATE_FIELD_NAME + " == '" + TransData.ETransStatus.NORMAL + "' THEN CAST("+ TransData.AMOUNT_FIELD_NAME +" AS LONG) ELSE 0 END)";
            String countTotal = "COUNT(*)";
            String sumTotal = "SUM(" + TransData.AMOUNT_FIELD_NAME + ")";

            String rawQuery = countTotal + ", " + sumTotal;

            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder().selectRaw(rawQuery);

            List<ETransType> excludeTrans = Arrays.asList(ETransType.ADJUST, ETransType.PREAUTH, ETransType.PREAUTHORIZATION, ETransType.PREAUTHORIZATION_CANCELLATION);

            Where<TransData, Integer> where = queryBuilder.where().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL)
                    .and().notIn(TransData.TYPE_FIELD_NAME, excludeTrans)
                    .and().isNotNull(TransData.ESLIP_UPLOAD_FIELD_NAME)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, MerchantProfileManager.INSTANCE.getCurrentMerchant());
            if (isERMUnsucessful) {
                where.and().ne(TransData.ESLIP_UPLOAD_FIELD_NAME, TransData.UploadStatus.NORMAL);
            } else {
                where.and().eq(TransData.ESLIP_UPLOAD_FIELD_NAME, TransData.UploadStatus.NORMAL);
            }

            ETransType[] types = {ETransType.VOID, ETransType.KBANK_REDEEM_VOID, ETransType.KBANK_SMART_PAY_VOID,
                    ETransType.QR_VOID_ALIPAY, ETransType.QR_VOID_WECHAT, ETransType.QR_VOID_KPLUS, ETransType.QR_VOID_CREDIT,
                    ETransType.QR_MYPROMPT_VOID, ETransType.DOLFIN_INSTALMENT_VOID};
            List<ETransType> filter = Arrays.asList(types);
            if (isVoid) {
                where.and().in(TransData.TYPE_FIELD_NAME, filter)
                     .and().isNull(TransData.ADJUST_AMOUNT_FIELD_NAME);
            } else {
                where.and().notIn(TransData.TYPE_FIELD_NAME, filter);
            }

            GenericRawResults<String[]> rawResults = dao.queryRaw(queryBuilder.prepare().getStatement());

            List<String[]> resultList = rawResults.getResults();
            if (resultList != null && !resultList.isEmpty()) {
                String[] sResult = resultList.get(0);
                return new long[]{Utils.parseLongSafe(sResult[0], 0), Utils.parseLongSafe(sResult[1], 0)};
            }
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return new long[]{0, 0};
    }

    public long countERMReportForAdjust(boolean isERMUnsucessful) {
        try {

            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder().selectRaw("COUNT(*)");

            Where<TransData, Integer> where = queryBuilder.where().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL)
                    .and().eq(TransData.TYPE_FIELD_NAME, ETransType.ADJUST)
                    .and().isNotNull(TransData.ESLIP_UPLOAD_FIELD_NAME)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, MerchantProfileManager.INSTANCE.getCurrentMerchant());
            if (isERMUnsucessful) {
                where.and().ne(TransData.ESLIP_UPLOAD_FIELD_NAME, TransData.UploadStatus.NORMAL);
            } else {
                where.and().eq(TransData.ESLIP_UPLOAD_FIELD_NAME, TransData.UploadStatus.NORMAL);
            }

            GenericRawResults<String[]> rawResults = dao.queryRaw(queryBuilder.prepare().getStatement());

            List<String[]> resultList = rawResults.getResults();
            if (resultList != null && !resultList.isEmpty()) {
                String[] sResult = resultList.get(0);
                return Utils.parseLongSafe(sResult[0], 0);
            }
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return 0;
    }

    public long[] countERMReportForVoidAdjust(boolean isERMUnsucessful) {
        try {
            String countTotal = "COUNT(*)";
            String sumTotal = "SUM(" + TransData.ADJUST_AMOUNT_FIELD_NAME + ")";

            String rawQuery = countTotal + ", " + sumTotal;

            RuntimeExceptionDao<TransData, Integer> dao = getTransDao();
            QueryBuilder<TransData, Integer> queryBuilder = dao.queryBuilder().selectRaw(rawQuery);

            List<ETransType> excludeTrans = Arrays.asList(ETransType.ADJUST, ETransType.PREAUTH, ETransType.PREAUTHORIZATION, ETransType.PREAUTHORIZATION_CANCELLATION);

            Where<TransData, Integer> where = queryBuilder.where().eq(TransData.REVERSAL_FIELD_NAME, TransData.ReversalStatus.NORMAL)
                    .and().notIn(TransData.TYPE_FIELD_NAME, excludeTrans)
                    .and().isNotNull(TransData.ESLIP_UPLOAD_FIELD_NAME)
                    .and().eq(TransData.MERCHANT_NAME_FILED_NAME, MerchantProfileManager.INSTANCE.getCurrentMerchant());
            if (isERMUnsucessful) {
                where.and().ne(TransData.ESLIP_UPLOAD_FIELD_NAME, TransData.UploadStatus.NORMAL);
            } else {
                where.and().eq(TransData.ESLIP_UPLOAD_FIELD_NAME, TransData.UploadStatus.NORMAL);
            }

            ETransType[] types = {ETransType.VOID, ETransType.KBANK_REDEEM_VOID, ETransType.KBANK_SMART_PAY_VOID,
                    ETransType.QR_VOID_ALIPAY, ETransType.QR_VOID_WECHAT, ETransType.QR_VOID_KPLUS, ETransType.QR_VOID_CREDIT,
                    ETransType.QR_MYPROMPT_VOID, ETransType.DOLFIN_INSTALMENT_VOID};
            List<ETransType> filter = Arrays.asList(types);
            where.and().in(TransData.TYPE_FIELD_NAME, filter)
                 .and().isNotNull(TransData.ADJUST_AMOUNT_FIELD_NAME);

            GenericRawResults<String[]> rawResults = dao.queryRaw(queryBuilder.prepare().getStatement());

            List<String[]> resultList = rawResults.getResults();
            if (resultList != null && !resultList.isEmpty()) {
                String[] sResult = resultList.get(0);
                return new long[]{Utils.parseLongSafe(sResult[0], 0), Utils.parseLongSafe(sResult[1], 0)};
            }
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return new long[]{0, 0};
    }

    public void deleteAllPreAuthTransactionExpired() {
        List<Acquirer> acquirers = new ArrayList<>();
        acquirers.add(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_KBANK));
        acquirers.add(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_KBANK_BDMS));
        acquirers.add(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_UP));

        List<TransData> dataList = findAllPreAuthTransDataWithNormalState(acquirers);

        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_PATTERN, Locale.US);

        int dayExpired = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_NUMBER_OF_DAY_KEEP_PREAUTH_TRANS);//default 30 days

        Calendar thirtyDaysAgo = Calendar.getInstance();
        thirtyDaysAgo.add(Calendar.DAY_OF_MONTH, -1 * dayExpired);

        Date thirtyDaysAgoDate = thirtyDaysAgo.getTime();//if today is 2022-05-31, thirtyDaysAgoDate is 2022-05-01

        for (TransData data : dataList) {
            String transDateTime = data.getDateTime();
            try {
                Date transDate = sdf.parse(transDateTime);
                if (transDate != null && (transDate.before(thirtyDaysAgoDate) || transDate.equals(thirtyDaysAgoDate))) {
                    deleteTransData(data.getId());
                }
            } catch (ParseException ignored) {
                // do nothing
            }
        }
    }

}
