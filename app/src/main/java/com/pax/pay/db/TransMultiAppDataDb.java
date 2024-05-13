package com.pax.pay.db;

import androidx.annotation.NonNull;

import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.pax.pay.base.Acquirer;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.model.TransMultiAppData;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import th.co.bkkps.utils.Log;

public class TransMultiAppDataDb {

    private static final String TAG = "TransMultiAppDataDb";
    private static TransMultiAppDataDb instance;
    private final BaseDbHelper dbHelper;
    private RuntimeExceptionDao<TransMultiAppData, Integer> transDao = null;

    private TransMultiAppDataDb() {
        dbHelper = BaseDbHelper.getInstance();
    }

    public static synchronized TransMultiAppDataDb getInstance() {
        if (instance == null) {
            instance = new TransMultiAppDataDb();
        }

        return instance;
    }

    /***************************************
     * Dao
     ******************************************/
    private RuntimeExceptionDao<TransMultiAppData, Integer> getTransDao() {
        if (transDao == null) {
            transDao = dbHelper.getRuntimeExceptionDao(TransMultiAppData.class);
        }
        return transDao;
    }

    /*--------------------Trans Multi App Data-----------------------------*/

    public boolean insertTransData(TransMultiAppData transData) {
        try {
            RuntimeExceptionDao<TransMultiAppData, Integer> dao = getTransDao();
            dao.create(transData);
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "", e);
        }

        return false;
    }

    public boolean deleteAllTransData() {
        try {
            RuntimeExceptionDao<TransMultiAppData, Integer> dao = getTransDao();
            dao.delete(findAllTransData());
            return true;
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }
        return false;
    }

    public boolean deleteAllTransData(Acquirer acquirer) {
        try {
            RuntimeExceptionDao<TransMultiAppData, Integer> dao = getTransDao();
            dao.delete(findAllTransData(acquirer));
            return true;
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }
        return false;
    }

    @NonNull
    private List<TransMultiAppData> findAllTransData() {
        try {
            RuntimeExceptionDao<TransMultiAppData, Integer> dao = getTransDao();
            return dao.queryForAll();
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    @NonNull
    private List<TransMultiAppData> findAllTransData(Acquirer acquirer) {
        try {
            RuntimeExceptionDao<TransMultiAppData, Integer> dao = getTransDao();
            QueryBuilder<TransMultiAppData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransMultiAppData, Integer> where = queryBuilder.where().eq(Acquirer.ID_FIELD_NAME, acquirer);
            return where.query();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    public TransMultiAppData findLastTransData() {
        try {
            List<TransMultiAppData> list = findAllTransData();
            if (!list.isEmpty()) {
                return list.get(list.size() - 1);
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    public TransMultiAppData findLastTransData(Acquirer acquirer) {
        try {
            List<TransMultiAppData> list = findAllTransData(acquirer);
            if (!list.isEmpty()) {
                return list.get(list.size() - 1);
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    public TransMultiAppData findTransDataByTraceNo(long traceNo) {
        try {
            RuntimeExceptionDao<TransMultiAppData, Integer> dao = getTransDao();
            QueryBuilder<TransMultiAppData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransMultiAppData, Integer> where = queryBuilder.where().eq(TransData.TRACENO_FIELD_NAME, traceNo);
            return where.queryForFirst();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return null;
    }

    public TransMultiAppData findTransDataByStanNo(long stanNo) {
        try {
            RuntimeExceptionDao<TransMultiAppData, Integer> dao = getTransDao();
            QueryBuilder<TransMultiAppData, Integer> queryBuilder = dao.queryBuilder();
            Where<TransMultiAppData, Integer> where = queryBuilder.where().eq(TransData.STANNO_FIELD_NAME, stanNo);
            return where.queryForFirst();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return null;
    }
}
