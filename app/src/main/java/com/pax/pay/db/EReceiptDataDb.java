package com.pax.pay.db;

import androidx.annotation.NonNull;

import th.co.bkkps.utils.Log;

import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.stmt.Where;
import com.pax.pay.base.AcqIssuerRelation;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.CardRange;
import com.pax.pay.base.EReceiptLogoMapping;
import com.pax.pay.base.Issuer;
import com.pax.pay.base.TransTypeMapping;
import com.pax.pay.utils.Convert;
import com.pax.pay.utils.EReceiptUtils;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

public class EReceiptDataDb {
    private static final String TAG = "ERECEIPT_DATA_DB";
    private final BaseDbHelper dbHelper;
    private static EReceiptDataDb instance;
    private EReceiptDataDb() {
        dbHelper = BaseDbHelper.getInstance();
    }
    private RuntimeExceptionDao<EReceiptLogoMapping, Integer> EReceiptLogoMappingsDao = null;

    /**
     * get the Singleton of the DB Helper
     *
     * @return the Singleton of DB helper
     */
    public static synchronized EReceiptDataDb getInstance() {
        if (instance == null) {
            instance = new EReceiptDataDb();
        }

        return instance;
    }

    /***************************************
     * Dao
     ******************************************/
    private RuntimeExceptionDao<EReceiptLogoMapping, Integer> getEReceiptLogoMappingsDao() {
        if (EReceiptLogoMappingsDao == null) {
            EReceiptLogoMappingsDao = dbHelper.getRuntimeExceptionDao(EReceiptLogoMapping.class);
        }
        return EReceiptLogoMappingsDao;
    }




    public boolean insertEReceiptLogoMapping(final EReceiptLogoMapping exERCM_LogoMapping) {
        try {
            RuntimeExceptionDao<EReceiptLogoMapping, Integer> dao = getEReceiptLogoMappingsDao();
            dao.create(exERCM_LogoMapping); //ignore the return value from create
        } catch (RuntimeException e) {
            Log.e(TAG, "", e);
            return false;
        }
        return true;
    }

    public boolean deleteErmSessionKey() {
        try {
            RuntimeExceptionDao<EReceiptLogoMapping, Integer> dao = getEReceiptLogoMappingsDao();
            dao.delete(FindAllRegisteredRecords()); //ignore the return value from create
        } catch (RuntimeException | SQLException e) {
            Log.e(TAG, "", e);
            return false;
        }
        return true;
    }

    public List<EReceiptLogoMapping> FindAllRegisteredRecords () throws SQLException {
        RuntimeExceptionDao<EReceiptLogoMapping, Integer> dao = getEReceiptLogoMappingsDao();
        QueryBuilder<EReceiptLogoMapping, Integer> findLogoQb = dao.queryBuilder();
        return findLogoQb.query();
    }

    public int findLogobyInfos (String exAcquirerNII,String exAcquirerName) throws SQLException {
        RuntimeExceptionDao<EReceiptLogoMapping, Integer> dao = getEReceiptLogoMappingsDao();
        QueryBuilder<EReceiptLogoMapping, Integer> findLogoQb = dao.queryBuilder();
        Where where = findLogoQb.where();
        where.eq(EReceiptLogoMapping.ERCM_ACQUIRER_NII,exAcquirerNII).and().eq(EReceiptLogoMapping.ERCM_ACQUIRER_NAME,exAcquirerName);

        EReceiptLogoMapping ERCMLogoMappingInfos = findLogoQb.queryForFirst();
        if (ERCMLogoMappingInfos != null) {
            return ERCMLogoMappingInfos.getId();
        } else {
            return -1;
        }
    }



    public EReceiptLogoMapping FindSessionKeyByAcquirerInfos (String exAcquirerNII,String exAcquirerName) throws SQLException {
        RuntimeExceptionDao<EReceiptLogoMapping, Integer> dao = getEReceiptLogoMappingsDao();
        QueryBuilder<EReceiptLogoMapping, Integer> findLogoQb = dao.queryBuilder();
        Where where = findLogoQb.where();
        where.eq(EReceiptLogoMapping.ERCM_ACQUIRER_NII,exAcquirerNII).and().eq(EReceiptLogoMapping.ERCM_ACQUIRER_NAME,exAcquirerName);

        return findLogoQb.queryForFirst();
    }

    public EReceiptLogoMapping FindSessionKeyByAcquirerIndex (String AcquirerIndex) throws SQLException {
        RuntimeExceptionDao<EReceiptLogoMapping, Integer> dao = getEReceiptLogoMappingsDao();
        QueryBuilder<EReceiptLogoMapping, Integer> findLogoQb = dao.queryBuilder();
        Where where = findLogoQb.where();
        where.eq(EReceiptLogoMapping.ERCM_HOST_INDEX, EReceiptUtils.StringPadding(AcquirerIndex,3,"0", Convert.EPaddingPosition.PADDING_LEFT));

        return findLogoQb.queryForFirst();
    }

    public int FindErmInitatedHostcount (String AcquirerIndex)  {
        try {
            RuntimeExceptionDao<EReceiptLogoMapping, Integer> dao = getEReceiptLogoMappingsDao();
            QueryBuilder<EReceiptLogoMapping, Integer> findLogoQb = dao.queryBuilder();
            findLogoQb.where().eq(EReceiptLogoMapping.ERCM_HOST_INDEX, EReceiptUtils.StringPadding(AcquirerIndex,3,"0", Convert.EPaddingPosition.PADDING_LEFT));

            return findLogoQb.query().size();
        } catch (Exception ex) {
            return 0;
        }
    }
    public int FindErmInitatedHostcount ()  {
        try {
            RuntimeExceptionDao<EReceiptLogoMapping, Integer> dao = getEReceiptLogoMappingsDao();
            QueryBuilder<EReceiptLogoMapping, Integer> findLogoQb = dao.queryBuilder();

            return findLogoQb.query().size();
        } catch (Exception ex) {
            return 0;
        }
    }

    public boolean updateEReceiptLogoMapping(@NonNull EReceiptLogoMapping exERCM_LogoMapping) {
        try {
            RuntimeExceptionDao<EReceiptLogoMapping, Integer> dao = getEReceiptLogoMappingsDao();
            dao.update(exERCM_LogoMapping); //ignore the return value
        } catch (RuntimeException e) {
            Log.e(TAG, "", e);
            return false;
        }
        return true;
    }
}
