package com.pax.pay.db;

import th.co.bkkps.utils.Log;

import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.j256.ormlite.stmt.Where;
import com.pax.pay.trans.model.TemplateLinePay;
import com.pax.pay.trans.model.TransData;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by WITSUTA A on 5/21/2018.
 */

public class TemplateLinePayDb {
    private static final String TAG = "TemplateLinePayDb";

    private RuntimeExceptionDao<TemplateLinePay, Integer> transDao = null;

    private final BaseDbHelper dbHelper;

    private static TemplateLinePayDb instance;

    private TemplateLinePayDb() {
        dbHelper = BaseDbHelper.getInstance();
    }

    /**
     * get the Singleton of the DB Helper
     *
     * @return the Singleton of DB helper
     */
    public static synchronized TemplateLinePayDb getInstance() {
        if (instance == null) {
            instance = new TemplateLinePayDb();
        }

        return instance;
    }

    /***************************************
     * Dao
     ******************************************/
    private RuntimeExceptionDao<TemplateLinePay, Integer> getTemplateDao() {
        if (transDao == null) {
            transDao = dbHelper.getRuntimeExceptionDao(TemplateLinePay.class);
        }
        return transDao;
    }


    /**
     * insert a template.
     */
    public boolean insertTemplateData(TemplateLinePay templateLinePay) {
        try {
            RuntimeExceptionDao<TemplateLinePay, Integer> dao = getTemplateDao();
            dao.create(templateLinePay);
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "", e);
        }

        return false;
    }

    /**
     * find template by id
     *
     * @param id template record id
     * @return template data object
     */
    public TemplateLinePay findTemplateData(int id) {
        try {
            RuntimeExceptionDao<TemplateLinePay, Integer> dao = getTemplateDao();
            QueryBuilder<TemplateLinePay, Integer> queryBuilder = dao.queryBuilder();
            Where<TemplateLinePay, Integer> where = queryBuilder.where().eq(TemplateLinePay.TEMPLATE_ID, id);

            return where.queryForFirst();
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    /**
     * delete template by id.
     */
    public boolean deleteTemplateData(int id) {
        try {
            RuntimeExceptionDao<TemplateLinePay, Integer> dao = getTemplateDao();
            DeleteBuilder<TemplateLinePay, Integer> deleteBuilder = dao.deleteBuilder();
            deleteBuilder.where().eq(TemplateLinePay.TEMPLATE_ID, id);
            dao.delete(deleteBuilder.prepare());
            return true;
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return false;
    }

    /**
     * delete all templates.
     */
    public boolean deleteAllTemplatessData() {
        try {
            RuntimeExceptionDao<TemplateLinePay, Integer> dao = getTemplateDao();
            DeleteBuilder<TemplateLinePay, Integer> deleteBuilder = dao.deleteBuilder();
            dao.delete(deleteBuilder.prepare());
            return true;
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return false;
    }

    /**
     * find all templates.
     */
    public List<TemplateLinePay> findAllTemplates() {
        try {
            RuntimeExceptionDao<TemplateLinePay, Integer> dao = getTemplateDao();
            return dao.queryForAll();
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    /**
     * Update template
     */
    public boolean updateTemplate(TemplateLinePay templateLinePay) {
        try {
            RuntimeExceptionDao<TemplateLinePay, Integer> dao = getTemplateDao();
            UpdateBuilder<TemplateLinePay, Integer> updateBuilder = dao.updateBuilder();
            updateBuilder.where().eq(TemplateLinePay.TEMPLATE_ID, templateLinePay.getTemplateId());
            updateBuilder.updateColumnValue(TemplateLinePay.LAST_USAGE_TIMESTAMP,templateLinePay.getLastUsageTimestmp());
            updateBuilder.update();
            return true;
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
        return false;
    }
}
