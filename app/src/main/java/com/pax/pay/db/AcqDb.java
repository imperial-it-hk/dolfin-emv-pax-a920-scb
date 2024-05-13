/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-12-20
 * Module Author: Kim.L
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import th.co.bkkps.utils.Log;

import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.j256.ormlite.stmt.Where;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.base.AcqIssuerRelation;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.CardRange;
import com.pax.pay.base.Issuer;
import com.pax.pay.base.TransTypeMapping;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.model.AcqManager;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO for acquirer table
 */
public class AcqDb {

    private static final String TAG = "AcqDb";

    private PreparedQuery<Issuer> issuersForAcquirerQuery = null;
    private PreparedQuery<CardRange> cardRangeQuery = null;
    private PreparedQuery<CardRange> cardRangeByIssuerBrandQuery = null;

    private RuntimeExceptionDao<Acquirer, Integer> acquirerDao = null;
    private RuntimeExceptionDao<Issuer, Integer> issuerDao = null;
    private RuntimeExceptionDao<CardRange, Integer> cardRangeDao = null;
    private RuntimeExceptionDao<AcqIssuerRelation, Integer> relationDao = null;
    private RuntimeExceptionDao<TransTypeMapping, Integer> transTypeMappingsDao = null;

    private final BaseDbHelper dbHelper;

    private static AcqDb instance;

    private AcqDb() {
        dbHelper = BaseDbHelper.getInstance();
    }


    /**
     * get the Singleton of the DB Helper
     *
     * @return the Singleton of DB helper
     */
    public static synchronized AcqDb getInstance() {
        if (instance == null) {
            instance = new AcqDb();
        }

        return instance;
    }

    /***************************************
     * Dao
     ******************************************/
    private RuntimeExceptionDao<Acquirer, Integer> getAcquirerDao() {
        if (acquirerDao == null) {
            acquirerDao = dbHelper.getRuntimeExceptionDao(Acquirer.class);
        }
        return acquirerDao;
    }


    private RuntimeExceptionDao<Issuer, Integer> getIssuerDao() {
        if (issuerDao == null) {
            issuerDao = dbHelper.getRuntimeExceptionDao(Issuer.class);
        }
        return issuerDao;
    }

    private RuntimeExceptionDao<CardRange, Integer> getCardRangeDao() {
        if (cardRangeDao == null) {
            cardRangeDao = dbHelper.getRuntimeExceptionDao(CardRange.class);
        }
        return cardRangeDao;
    }

    private RuntimeExceptionDao<AcqIssuerRelation, Integer> getRelationDao() {
        if (relationDao == null) {
            relationDao = dbHelper.getRuntimeExceptionDao(AcqIssuerRelation.class);
        }
        return relationDao;
    }

    private RuntimeExceptionDao<TransTypeMapping, Integer> getTransTypeMappingDao() {
        if (transTypeMappingsDao == null) {
            transTypeMappingsDao = dbHelper.getRuntimeExceptionDao(TransTypeMapping.class);
        }
        return transTypeMappingsDao;
    }


    /*------------------------------Acquirer------------------------------------*/

    /**
     * insert an acquirer record
     *
     * @param acquirer the record
     */
    public boolean insertAcquirer(final Acquirer acquirer) {
        try {
            RuntimeExceptionDao<Acquirer, Integer> dao = getAcquirerDao();
            dao.create(acquirer); // ignore the return value from create
        } catch (RuntimeException e) {
            Log.e(TAG, "", e);
            return false;
        }

        return true;
    }

    /**
     * find the unique acquirer record by name
     *
     * @param acquirerName acquirer name
     * @return the matched {@link Acquirer} or null
     */
    public Acquirer findAcquirer(final String acquirerName) {
        try {
            RuntimeExceptionDao<Acquirer, Integer> dao = getAcquirerDao();
            List<Acquirer> acq = dao.queryForEq(Acquirer.NAME_FIELD_NAME, acquirerName);
            if (acq != null && !acq.isEmpty()) {
                return acq.get(0);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public int findCurrentMaxKeyId() {
        try {
            RuntimeExceptionDao<Acquirer, Integer> dao = getAcquirerDao();
            String resultCurrentMaxKeyID = acquirerDao.queryRaw("SELECT MAX(KEY_ID)+1 FROM ACQUIRER").getResults().get(0)[0];
            if (resultCurrentMaxKeyID!=null) {
                return Integer.parseInt(resultCurrentMaxKeyID);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }



    public Acquirer findActiveAcquirer(final String acquirerName) {
        try {
            RuntimeExceptionDao<Acquirer, Integer> dao = getAcquirerDao();
            QueryBuilder<Acquirer, Integer> queryBuilder = dao.queryBuilder();
            queryBuilder.where().eq(Acquirer.NAME_FIELD_NAME, acquirerName).and().eq(Acquirer.ENABLE, true);
            if (queryBuilder.countOf() > 0) {
                return queryBuilder.query().get(0);
            }
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return null;
    }


    public Acquirer findActiveAcquirerWithSpecificTleBankName(final String supportTleBankName) {
        try {
            RuntimeExceptionDao<Acquirer, Integer> dao = getAcquirerDao();
            QueryBuilder<Acquirer, Integer> queryBuilder = dao.queryBuilder();
            queryBuilder.where()
                    .eq(Acquirer.TLE_BANK_NAME, supportTleBankName)
                    .and().eq(Acquirer.ENABLE, true);
            if (queryBuilder.countOf() > 0) {
                return queryBuilder.query().get(0);
            }
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return null;
    }

    public List<Acquirer> findAcquirer(List<String> acquirerName) {
        try {
            RuntimeExceptionDao<Acquirer, Integer> dao = getAcquirerDao();
            QueryBuilder<Acquirer, Integer> queryBuilder = dao.queryBuilder().orderBy(Acquirer.ID_FIELD_NAME, true);
            queryBuilder.where().in(Acquirer.NAME_FIELD_NAME, acquirerName).and().eq(Acquirer.ENABLE, true);
            return queryBuilder.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    /**
     * find records of all Acquirers
     *
     * @return List of {@link Acquirer}
     */
    public List<Acquirer> findAllAcquirers() {
        RuntimeExceptionDao<Acquirer, Integer> dao = getAcquirerDao();
        return dao.queryForAll();
    }

    /**
     * find records of all ENABLED Acquirers
     *
     * @return List of {@link Acquirer} or null
     */
    public List<Acquirer> findEnableAcquirers() {
        try {
            RuntimeExceptionDao<Acquirer, Integer> dao = getAcquirerDao();
            List<Acquirer> acq = dao.queryForEq(Acquirer.ENABLE, true);
            if (acq != null && !acq.isEmpty()) {
                return acq;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Acquirer> findEnableAcquirersBySortMode(boolean ascMode) {
        RuntimeExceptionDao<Acquirer, Integer> dao = getAcquirerDao();
        QueryBuilder<Acquirer, Integer> queryBuilder = dao.queryBuilder().orderBy(Acquirer.ID_FIELD_NAME, ascMode);
        try {
            queryBuilder.where().eq(Acquirer.ENABLE, true)
                    .and().ne(Acquirer.NAME_FIELD_NAME, Constants.ACQ_ERCM_KEY_MANAGEMENT_SERVICE)
                    .and().ne(Acquirer.NAME_FIELD_NAME, Constants.ACQ_ERCM_RECEIPT_MANAGEMENT_SERVICE);
            List<Acquirer> acq = queryBuilder.query();
            if (acq != null && !acq.isEmpty()) {
                return acq;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public List<Acquirer> findEnableAcquirers(String exceptAcqName) {
        RuntimeExceptionDao<Acquirer, Integer> dao = getAcquirerDao();
        QueryBuilder<Acquirer, Integer> queryBuilder = dao.queryBuilder();
        try {
            Where<Acquirer, Integer> where = queryBuilder.where()
                    .eq(Acquirer.ENABLE, true)
                    .and().notIn(Acquirer.NAME_FIELD_NAME, exceptAcqName);
            return where.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    public List<Acquirer> findEnableAcquirers(List<String> exceptAcqName) {
        RuntimeExceptionDao<Acquirer, Integer> dao = getAcquirerDao();
        QueryBuilder<Acquirer, Integer> queryBuilder = dao.queryBuilder();
        try {
            Where<Acquirer, Integer> where = queryBuilder.where()
                    .eq(Acquirer.ENABLE, true)
                    .and().notIn(Acquirer.NAME_FIELD_NAME, exceptAcqName);
            return where.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    public List<Acquirer> findEnableAcquirersWithEnableERM() {
        RuntimeExceptionDao<Acquirer, Integer> dao = getAcquirerDao();
        QueryBuilder<Acquirer, Integer> queryBuilder = dao.queryBuilder();
        try {
            Where<Acquirer, Integer> where = queryBuilder.where()
                    .eq(Acquirer.ENABLE, true)
                    .and().eq(Acquirer.ENABLE_UPLOAD_ERM, true)
                    .and().ne(Acquirer.NAME_FIELD_NAME, Constants.ACQ_ERCM_KEY_MANAGEMENT_SERVICE)
                    .and().ne(Acquirer.NAME_FIELD_NAME, Constants.ACQ_ERCM_RECEIPT_MANAGEMENT_SERVICE);
            return where.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    public int findCountEnableAcquirersWithEnableERM() {
        try {
            RuntimeExceptionDao<Acquirer, Integer> dao = getAcquirerDao();
            QueryBuilder<Acquirer, Integer> queryBuilder = dao.queryBuilder().orderBy(Acquirer.ID_FIELD_NAME, true);
            queryBuilder.where().eq(Acquirer.ENABLE_UPLOAD_ERM, true)
                    .and().eq(Acquirer.ENABLE, true)
                    .and().ne(Acquirer.NAME_FIELD_NAME, Constants.ACQ_ERCM_KEY_MANAGEMENT_SERVICE)
                    .and().ne(Acquirer.NAME_FIELD_NAME, Constants.ACQ_ERCM_RECEIPT_MANAGEMENT_SERVICE);
            return queryBuilder.query().size();
        } catch (SQLException e) {
            Log.e(TAG, "", e);
            return 0;
        }
    }

    public List<Acquirer> findAcquirersWithERMStatus(boolean ERM_Upload_Flag) {
        try {
            RuntimeExceptionDao<Acquirer, Integer> dao = getAcquirerDao();
            QueryBuilder<Acquirer, Integer> queryBuilder = dao.queryBuilder().orderBy(Acquirer.ID_FIELD_NAME, true);
            queryBuilder.where().eq(Acquirer.ENABLE_UPLOAD_ERM, ERM_Upload_Flag)
                    .and().eq(Acquirer.ENABLE, true)
                    .and().ne(Acquirer.NAME_FIELD_NAME, Constants.ACQ_ERCM_KEY_MANAGEMENT_SERVICE)
                    .and().ne(Acquirer.NAME_FIELD_NAME, Constants.ACQ_ERCM_RECEIPT_MANAGEMENT_SERVICE);
            return queryBuilder.query();
        } catch (SQLException e) {
            Log.e(TAG, "", e);
            return new ArrayList<>(0);
        }
    }

    public int findAycapAcquirer(String HostName) {
        int ret;
        try {
            RuntimeExceptionDao<Acquirer, Integer> dao = getAcquirerDao();
            QueryBuilder<Acquirer, Integer> queryBuilder = dao.queryBuilder().orderBy(Acquirer.ID_FIELD_NAME, true);
            queryBuilder.where().eq(Acquirer.NAME_FIELD_NAME, HostName).and().eq("enable", true);
            List<Acquirer> listAcquirer = queryBuilder.query();
            if (listAcquirer.size() == 1) {
                ret = TransResult.SUCC;
            } else {
                ret = TransResult.ERR_HOST_NOT_FOUND;
            }
        } catch (SQLException e) {
            Log.e(TAG, "", e);
            ret = TransResult.ERR_PROCESS_FAILED;
        }
        return ret;
    }

    /**
     * update the acquirer
     *
     * @param acquirer the target acquirer
     */
    public boolean updateAcquirer(final Acquirer acquirer) {
        try {
            RuntimeExceptionDao<Acquirer, Integer> dao = getAcquirerDao();
            dao.update(acquirer);
        } catch (RuntimeException e) {
            Log.e(TAG, "", e);
            return false;
        }

        return true;
    }


    public boolean updateAcquirerCurrentHostId(final Acquirer acquirer, int CurrentHostId) {
        try {
            RuntimeExceptionDao<Acquirer, Integer> dao = getAcquirerDao();
            UpdateBuilder<Acquirer, Integer> updateBuilder = dao.updateBuilder();
            updateBuilder.updateColumnValue(Acquirer.CURRENT_HOST_ID, CurrentHostId).where().eq(Acquirer.NAME_FIELD_NAME, acquirer.getName());
            updateBuilder.update();
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "", e);
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public int getAcquirerCurrentHostId(final Acquirer acquirer) {
        try {
            RuntimeExceptionDao<Acquirer, Integer> dao = getAcquirerDao();
            QueryBuilder<Acquirer, Integer> queryBuilder = dao.queryBuilder().orderBy(Acquirer.ID_FIELD_NAME, true);
            queryBuilder.where().eq(Acquirer.NAME_FIELD_NAME, acquirer.getName());
            Acquirer[] acquirers = queryBuilder.query().toArray(new Acquirer[0]);

            return acquirers[0].getCurrentBackupAddressId();
        } catch (RuntimeException e) {
            Log.e(TAG, "", e);
            return -1;
        } catch (SQLException e) {
            Log.e(TAG, "", e);
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * delete the acquirer by id
     *
     * @param id acquire id
     */
    public boolean deleteAcquirer(int id) {
        try {
            RuntimeExceptionDao<Acquirer, Integer> dao = getAcquirerDao();
            dao.deleteById(id);
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
            return false;
        }

        return true;
    }


    public List<Acquirer> findEnableAcquirersWithKeyData() {
        try {
            RuntimeExceptionDao<Acquirer, Integer> dao = getAcquirerDao();
            QueryBuilder<Acquirer, Integer> queryBuilder = dao.queryBuilder().selectColumns(Acquirer.NAME_FIELD_NAME)
                    .selectColumns(Acquirer.TMK_FIELD_NAME)
                    .selectColumns(Acquirer.TWK_FIELD_NAME)
                    .selectColumns(Acquirer.UP_TMK_FIELD_NAME)
                    .selectColumns(Acquirer.UP_TWK_FIELD_NAME);

            if (existsColumnInTable(dbHelper.getReadableDatabase(), instance.acquirerDao.getTableName(), Acquirer.KEY_ID_FIELD_NAME)) {
                queryBuilder.selectColumns(Acquirer.KEY_ID_FIELD_NAME);
            } else {
                queryBuilder.selectColumns(Acquirer.ID_FIELD_NAME);
            }

            queryBuilder.where().eq(Acquirer.ENABLE, true);

            List<Acquirer> acquirers = queryBuilder.query();
            if (acquirers != null && !acquirers.isEmpty()) {
                return acquirers;
            }
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return null;
    }

    /**
     * check existence  of
     *
     * @param inDatabase    db
     * @param inTable       db
     * @param columnToCheck db
     */
    private boolean existsColumnInTable(SQLiteDatabase inDatabase, String inTable, String columnToCheck) {
        Cursor mCursor = null;
        try {
            // Query 1 row
            mCursor = inDatabase.rawQuery("SELECT * FROM " + inTable + " LIMIT 0", null);

            // getColumnIndex() gives us the index (0 to ...) of the column - otherwise we get a -1
            if (mCursor.getColumnIndex(columnToCheck) != -1)
                return true;
            else
                return false;

        } catch (Exception Exp) {
            // Something went wrong. Missing the database? The table?
            Log.e("menu", "When checking whether a column exists in the table, an error occurred: " + Exp.getMessage());
            return false;
        } finally {
            if (mCursor != null) mCursor.close();
        }
    }

    /*-----------------------------------Issuer--------------------------------------*/

    /**
     * insert an issuer record
     *
     * @param issuer the record
     */
    public boolean insertIssuer(final Issuer issuer) {
        try {
            RuntimeExceptionDao<Issuer, Integer> dao = getIssuerDao();
            dao.create(issuer); // ignore the return value from create
        } catch (RuntimeException e) {
            Log.e(TAG, "", e);
            return false;
        }

        return true;
    }

    /**
     * find the unique issuer record by name
     *
     * @param issuerName issuer name
     * @return the matched {@link Issuer} or null
     */
    public Issuer findIssuer(String issuerName) {
        RuntimeExceptionDao<Issuer, Integer> dao = getIssuerDao();
        List<Issuer> issuers = dao.queryForEq(Issuer.NAME_FIELD_NAME, issuerName);
        if (issuers != null && !issuers.isEmpty())
            return issuers.get(0);
        return null;
    }

    /**
     * find list of issuer by issuer brand
     *
     * @param issuerBrand
     * @return List of {@link Issuer} or null
     */
    public List<Issuer> findIssuerByBrand(String issuerBrand) {
        RuntimeExceptionDao<Issuer, Integer> dao = getIssuerDao();
        List<Issuer> issuers = dao.queryForEq(Issuer.ISSUER_BRAND_FIELD_NAME, issuerBrand);
        if (issuers == null || issuers.isEmpty()) {
            return null;
        }
        return issuers;
    }

    /**
     * find list of issuer by issuer name
     *
     * @param issuerName
     * @return List of {@link Issuer} or null
     */
    public List<Issuer> findIssuerByName(String issuerName) {
        RuntimeExceptionDao<Issuer, Integer> dao = getIssuerDao();
        List<Issuer> issuers = dao.queryForEq(Issuer.NAME_FIELD_NAME, issuerName);
        if (issuers == null || issuers.isEmpty()) {
            return null;
        }
        return issuers;
    }

    public List<Issuer> findIssuerByAcquirerName(String acquirerName) {
        RuntimeExceptionDao<Issuer, Integer> dao = getIssuerDao();
        List<Issuer> issuers = dao.queryForEq(Issuer.ACQUIRER_NAME_FIELD_NAME, acquirerName);
        if (issuers == null || issuers.isEmpty()) {
            return null;
        }
        return issuers;
    }

    /**
     * find records of all Issuers
     *
     * @return List of {@link Issuer}
     */
    public List<Issuer> findAllIssuers() {
        RuntimeExceptionDao<Issuer, Integer> dao = getIssuerDao();
        return dao.queryForAll();
    }

    /**
     * bind an acquirer with an issuer
     *
     * @param root   the acquirer
     * @param issuer the issuer
     */
    public boolean bind(final Acquirer root, final Issuer issuer) {
        try {
            RuntimeExceptionDao<AcqIssuerRelation, Integer> dao = getRelationDao();
            AcqIssuerRelation relation = findRelation(root, issuer);
            if (null == relation) {
                dao.create(new AcqIssuerRelation(root, issuer)); //ignore the return value from create
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
            return false;
        }

        return true;
    }

    /**
     * update the issuer
     *
     * @param issuer the target issuer
     */
    public boolean updateIssuer(final Issuer issuer) {
        try {
            RuntimeExceptionDao<Issuer, Integer> dao = getIssuerDao();
            dao.update(issuer);
        } catch (RuntimeException e) {
            Log.e(TAG, "", e);
            return false;
        }

        return true;
    }

    /**
     * delete the issuer by id
     *
     * @param id issuer id
     */
    public boolean deleteIssuer(int id) {
        try {
            RuntimeExceptionDao<Issuer, Integer> dao = getIssuerDao();
            dao.deleteById(id);
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
            return false;
        }

        return true;
    }

    /**
     * delete the issuer List
     *
     * @param issuers List
     */
    public boolean deleteIssuer(List<Issuer> issuers) {
        try {
            RuntimeExceptionDao<Issuer, Integer> dao = getIssuerDao();
            dao.delete(issuers);
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
            return false;
        }

        return true;
    }

    /*---------------------------------CardRange--------------------------------*/

    /**
     * insert a cardRange record
     *
     * @param cardRange the card range
     */
    public boolean insertCardRange(final CardRange cardRange) {
        try {
            RuntimeExceptionDao<CardRange, Integer> dao = getCardRangeDao();
            dao.create(cardRange); //ignore the return value from create
        } catch (RuntimeException e) {
            Log.e(TAG, "", e);
            return false;
        }
        return true;
    }

    /**
     * update record
     *
     * @param cardRange the record need to be updated
     */
    public boolean updateCardRange(final CardRange cardRange) {
        try {
            RuntimeExceptionDao<CardRange, Integer> dao = getCardRangeDao();
            dao.update(cardRange); //ignore the return value
        } catch (RuntimeException e) {
            Log.e(TAG, "", e);
            return false;
        }
        return true;
    }

    /**
     * find the unique issuer record
     *
     * @param low  the lower limit
     * @param high the higher limit
     * @return the matched {@link CardRange} or null
     */
    public CardRange findCardRange(final long low, final long high) {
        RuntimeExceptionDao<CardRange, Integer> dao = getCardRangeDao();
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put(CardRange.RANGE_LOW_FIELD_NAME, low);
        fields.put(CardRange.RANGE_HIGH_FIELD_NAME, high);

        List<CardRange> crs = dao.queryForFieldValues(fields);
        if (crs != null && !crs.isEmpty())
            return crs.get(0);
        return null;
    }

    /**
     * find the unique CardRange record
     *
     * @param pan the card no
     * @return the matched {@link CardRange} or null
     */
    public CardRange findCardRange(final String pan) {
        try {
            RuntimeExceptionDao<CardRange, Integer> dao = getCardRangeDao();
            if (cardRangeQuery == null) {
                cardRangeQuery = makePostsForCardRangeQuery();
            }
            String subPan = pan.substring(0, 10);
            cardRangeQuery.setArgumentHolderValue(0, subPan);
            cardRangeQuery.setArgumentHolderValue(1, subPan);
            cardRangeQuery.setArgumentHolderValue(2, pan.length());
            return dao.queryForFirst(cardRangeQuery);
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return null;
    }

    /**
     * find the all CardRange record
     *
     * @param pan the card no
     * @return the matched {@link CardRange} or null
     */
    public List<CardRange> findAllCardRange(final String pan) {
        try {
            RuntimeExceptionDao<CardRange, Integer> dao = getCardRangeDao();
            if (cardRangeQuery == null) {
                cardRangeQuery = makePostsForCardRangeQuery();
            }
            String subPan = pan.substring(0, 10);
            cardRangeQuery.setArgumentHolderValue(0, subPan);
            cardRangeQuery.setArgumentHolderValue(1, subPan);
            cardRangeQuery.setArgumentHolderValue(2, pan.length());
            return dao.query(cardRangeQuery);
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return null;
    }

    public List<CardRange> findAllCardRange(final String pan, final String issuerBrand) {
        try {
            RuntimeExceptionDao<CardRange, Integer> dao = getCardRangeDao();
            if (cardRangeByIssuerBrandQuery == null) {
                cardRangeByIssuerBrandQuery = makePostsForCardRangeQueryByIssuerBrand();
            }
            String subPan = pan.substring(0, 10);
            cardRangeByIssuerBrandQuery.setArgumentHolderValue(0, subPan);
            cardRangeByIssuerBrandQuery.setArgumentHolderValue(1, subPan);
            cardRangeByIssuerBrandQuery.setArgumentHolderValue(2, pan.length());
            cardRangeByIssuerBrandQuery.setArgumentHolderValue(3, issuerBrand);
            return dao.query(cardRangeByIssuerBrandQuery);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * find the card ranges of the issuer
     *
     * @param issuer the issuer
     * @return List of {@link CardRange}
     */
    public List<CardRange> findCardRange(final Issuer issuer) {
        RuntimeExceptionDao<CardRange, Integer> dao = getCardRangeDao();
        return dao.queryForEq(Issuer.ID_FIELD_NAME, issuer);
    }

    /**
     * find records of all Card Ranges
     *
     * @return List of {@link CardRange}
     */
    public List<CardRange> findAllCardRanges() {
        RuntimeExceptionDao<CardRange, Integer> dao = getCardRangeDao();
        return dao.queryForAll();
    }


    /**
     * delete the cardRange by id
     *
     * @param id cardRange id
     */
    public boolean deleteCardRange(int id) {
        try {
            RuntimeExceptionDao<CardRange, Integer> dao = getCardRangeDao();
            dao.deleteById(id);
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
            return false;
        }

        return true;
    }

    /**
     * delete the cardRange List
     *
     * @param cardRanges cardRange List
     */
    public boolean deleteCardRange(List<CardRange> cardRanges) {
        try {
            RuntimeExceptionDao<CardRange, Integer> dao = getCardRangeDao();
            dao.delete(cardRanges);
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
            return false;
        }

        return true;
    }

    /*------------------------------Relation---------------------------*/

    /**
     * find the relation of acquirer and issuer
     *
     * @param acquirer the acquirer
     * @param issuer   the issuer
     * @return the matched {@link AcqIssuerRelation} or null
     */
    private AcqIssuerRelation findRelation(final Acquirer acquirer, final Issuer issuer) {
        RuntimeExceptionDao<AcqIssuerRelation, Integer> dao = getRelationDao();
        Map<String, Object> fieldsMap = new HashMap<>();
        fieldsMap.put(Acquirer.ID_FIELD_NAME, acquirer);
        fieldsMap.put(Issuer.ID_FIELD_NAME, issuer);
        List<AcqIssuerRelation> relation = dao.queryForFieldValues(fieldsMap);
        if (relation != null && !relation.isEmpty())
            return relation.get(0);
        return null;
    }

    /**
     * find the relation of acquirer and issuer
     *
     * @param acquirer the acquirer
     * @return the matched {@link AcqIssuerRelation} or null
     */
    public List<AcqIssuerRelation> findRelation(final Acquirer acquirer) {
        RuntimeExceptionDao<AcqIssuerRelation, Integer> dao = getRelationDao();
        Map<String, Object> fieldsMap = new HashMap<>();
        fieldsMap.put(Acquirer.ID_FIELD_NAME, acquirer);
        List<AcqIssuerRelation> relation = dao.queryForFieldValues(fieldsMap);
        if (relation != null && !relation.isEmpty())
            return relation;
        return null;
    }

    /**
     * find records of all Relations
     *
     * @return List of {@link AcqIssuerRelation}
     */
    public List<AcqIssuerRelation> findAllRelations() {
        RuntimeExceptionDao<AcqIssuerRelation, Integer> dao = getRelationDao();
        return dao.queryForAll();
    }

    /**
     * find all issuers accepted by the acquirer
     *
     * @param acquirer the acquirer
     * @return List of {@link Issuer}
     * @throws SQLException
     */
    public List<Issuer> lookupIssuersForAcquirer(final Acquirer acquirer) throws SQLException {
        RuntimeExceptionDao<Issuer, Integer> dao = getIssuerDao();
        if (issuersForAcquirerQuery == null) {
            issuersForAcquirerQuery = makePostsForAcquirerQuery();
        }
        issuersForAcquirerQuery.setArgumentHolderValue(0, acquirer);
        return dao.query(issuersForAcquirerQuery);
    }

    /**
     * generate the sql for finding the acquirer
     */
    private PreparedQuery<Issuer> makePostsForAcquirerQuery() throws SQLException {
        RuntimeExceptionDao<AcqIssuerRelation, Integer> locRelationDao = getRelationDao();
        RuntimeExceptionDao<Issuer, Integer> locIssuerDao = getIssuerDao();
        //create a query for find the relation
        QueryBuilder<AcqIssuerRelation, Integer> relation = locRelationDao.queryBuilder();
        // sql: select issuer_id from acq_issuer_relation
        relation.selectColumns(Issuer.ID_FIELD_NAME);
        // sql: where acquirer_id=?
        relation.where().eq(Acquirer.ID_FIELD_NAME, new SelectArg());
        // create a foreign query
        QueryBuilder<Issuer, Integer> postQb = locIssuerDao.queryBuilder();
        // sql: where issuer_id in()
        postQb.where().in(Issuer.ID_FIELD_NAME, relation);
        /*
         * the sql is
         * "SELECT * FROM `issuer`
         * 		WHERE `issuer_id` IN (
         * 			SELECT `issuer_id` FROM `acq_issuer_relation` WHERE `acquirer_id` = ?
         * 		) "
         */
        return postQb.prepare();
    }

    /**
     * generate the sql for finding the card range
     *
     * @throws SQLException exception
     */
    private PreparedQuery<CardRange> makePostsForCardRangeQuery() throws SQLException {
        RuntimeExceptionDao<CardRange, Integer> locCardRangeDao = getCardRangeDao();
        QueryBuilder<CardRange, Integer> postQb = locCardRangeDao.queryBuilder();
        Where where = postQb.where();
        //WHERE (low <= ? AND high >= ?) @1
        where.le(CardRange.RANGE_LOW_FIELD_NAME, new SelectArg()).and().ge(CardRange.RANGE_HIGH_FIELD_NAME, new SelectArg());
        //WHERE (length = 0 OR ? = length) @2
        where.eq(CardRange.LENGTH_FIELD_NAME, 0).or().eq(CardRange.LENGTH_FIELD_NAME, new SelectArg());
        //WHERE @1 AND @2
        where.and(2);
        // order by (high - low)
        postQb.orderByRaw(CardRange.RANGE_HIGH_FIELD_NAME + "-" + CardRange.RANGE_LOW_FIELD_NAME);
        return postQb.prepare();
    }

    /**
     * generate the sql for finding the card range by Issuer
     *
     * @throws SQLException exception
     */
    private PreparedQuery<CardRange> makePostsForCardRangeQueryByIssuerBrand() throws SQLException {
        RuntimeExceptionDao<CardRange, Integer> locCardRangeDao = getCardRangeDao();
        QueryBuilder<CardRange, Integer> postQb = locCardRangeDao.queryBuilder();
        Where where = postQb.where();
        //WHERE (low <= ? AND high >= ?) @1
        where.le(CardRange.RANGE_LOW_FIELD_NAME, new SelectArg()).and().ge(CardRange.RANGE_HIGH_FIELD_NAME, new SelectArg());
        //WHERE (length = 0 OR ? = length) @2
        where.eq(CardRange.LENGTH_FIELD_NAME, 0).or().eq(CardRange.LENGTH_FIELD_NAME, new SelectArg());
        //WHERE @1 AND @2
        where.and(2);
        //WHERE (issuer_brand = ?)
        where.and().eq(CardRange.ISSUER_BRAND_FIELD_NAME, new SelectArg());
        // order by (high - low)
        postQb.orderByRaw(CardRange.RANGE_HIGH_FIELD_NAME + "-" + CardRange.RANGE_LOW_FIELD_NAME);
        return postQb.prepare();
    }

    /**
     * update the acqIssuerRelation
     *
     * @param acqIssuerRelation the relation object
     */
    public boolean updateRelation(final AcqIssuerRelation acqIssuerRelation) {
        try {
            RuntimeExceptionDao<AcqIssuerRelation, Integer> dao = getRelationDao();
            dao.update(acqIssuerRelation);
        } catch (RuntimeException e) {
            Log.e(TAG, "", e);
            return false;
        }

        return true;
    }

    /**
     * delete the acqIssuerRelation by id
     *
     * @param id acqIssuerRelation id
     */
    public boolean deleteAcqIssuerRelation(int id) {
        try {
            RuntimeExceptionDao<AcqIssuerRelation, Integer> dao = getRelationDao();
            dao.deleteById(id);
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
            return false;
        }

        return true;
    }

    /**
     * delete the acqIssuerRelation List
     *
     * @param relations acqIssuerRelation List
     */
    public boolean deleteAcqIssuerRelations(List<AcqIssuerRelation> relations) {
        try {
            RuntimeExceptionDao<AcqIssuerRelation, Integer> dao = getRelationDao();
            dao.delete(relations);
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
            return false;
        }

        return true;
    }

    public void clearInstance() {
        instance = null;
        acquirerDao = null;
        issuerDao = null;
        cardRangeDao = null;
        relationDao = null;
    }

    /**
     * find the transaction mapping with acquirer and issuer
     *
     * @param type
     * @param acquirer the acquirer
     * @param issuer   the issuer
     * @return the matched {@link TransTypeMapping} or null
     */
    private TransTypeMapping findTransMapping(final ETransType type, final Acquirer acquirer, final Issuer issuer) {
        RuntimeExceptionDao<TransTypeMapping, Integer> dao = getTransTypeMappingDao();
        Map<String, Object> fieldsMap = new HashMap<>();
        fieldsMap.put(TransTypeMapping.TYPE_FIELD_NAME, type);
        fieldsMap.put(Acquirer.ID_FIELD_NAME, acquirer);
        fieldsMap.put(Issuer.ID_FIELD_NAME, issuer);
        List<TransTypeMapping> mappings = dao.queryForFieldValues(fieldsMap);
        if (mappings != null && !mappings.isEmpty())
            return mappings.get(0);
        return null;
    }

    /**
     * insert transaction mapping with acquirer and issuer
     *
     * @param type
     * @param root   the acquirer
     * @param issuer the issuer
     */
    public boolean insertTransMapping(final ETransType type, final Acquirer root, final Issuer issuer, final int priority) {
        try {
            RuntimeExceptionDao<TransTypeMapping, Integer> dao = getTransTypeMappingDao();
            TransTypeMapping mapping = findTransMapping(type, root, issuer);
            if (null == mapping) {
                dao.create(new TransTypeMapping(type, root, issuer, priority)); //ignore the return value from create
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
            return false;
        }

        return true;
    }

    /**
     * find the transaction mapping by TransType and Issuer
     *
     * @param type
     * @param issuer the issuer
     * @return the matched {@link TransTypeMapping} or null
     */
    public TransTypeMapping findTransMapping(final ETransType type, final Issuer issuer, final int priority) {
        RuntimeExceptionDao<TransTypeMapping, Integer> dao = getTransTypeMappingDao();
        Map<String, Object> fieldsMap = new HashMap<>();
        fieldsMap.put(TransTypeMapping.TYPE_FIELD_NAME, type);
        fieldsMap.put(Issuer.ID_FIELD_NAME, issuer);
        fieldsMap.put(TransTypeMapping.PRIORITY_FIELD_NAME, priority);
        List<TransTypeMapping> mappings = dao.queryForFieldValues(fieldsMap);
        if (mappings != null && !mappings.isEmpty())
            return mappings.get(0);
        return null;
    }

    /**
     * find the transaction mapping
     *
     * @param acquirer Acquirer
     * @return the matched {@link TransTypeMapping} or null
     */
    public List<TransTypeMapping> findTransMapping(final Acquirer acquirer) {
        RuntimeExceptionDao<TransTypeMapping, Integer> dao = getTransTypeMappingDao();
        Map<String, Object> fieldsMap = new HashMap<>();
        fieldsMap.put(Acquirer.ID_FIELD_NAME, acquirer);
        return dao.queryForFieldValues(fieldsMap);
    }








    public List<String[]> getDatabaseAllTables() {
        try {
            RuntimeExceptionDao<Acquirer, Integer> dao = getAcquirerDao();
            List<String[]> tableNameList = acquirerDao.queryRaw("SELECT master.tbl_name, master.sql FROM sqlite_master as master WHERE type = 'table'").getResults();
            if (tableNameList!=null) {
                return tableNameList;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }
}
