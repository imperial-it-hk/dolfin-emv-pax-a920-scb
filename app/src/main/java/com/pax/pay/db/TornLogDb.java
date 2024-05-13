/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-3-7
 * Module Author: laiyi
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.db;

import th.co.bkkps.utils.Log;
import androidx.annotation.NonNull;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.pax.pay.emv.clss.ClssTornLog;

import java.util.ArrayList;
import java.util.List;

/**
 * DAO for torn log table
 */
public class TornLogDb {

    private static final String TAG = "TornLogDb";
    private RuntimeExceptionDao<ClssTornLog, Integer> tornLogDao = null;
    private final BaseDbHelper dbHelper;
    private static TornLogDb instance;

    private TornLogDb() {
        dbHelper = BaseDbHelper.getInstance();
    }

    /**
     * get the Singleton of the DB Helper
     *
     * @return the Singleton of DB helper
     */
    public static synchronized TornLogDb getInstance() {
        if (instance == null) {
            instance = new TornLogDb();
        }

        return instance;
    }

    /***************************************
     * Dao
     ******************************************/
    private RuntimeExceptionDao<ClssTornLog, Integer> getTornLogDao() {
        if (tornLogDao == null) {
            tornLogDao = dbHelper.getRuntimeExceptionDao(ClssTornLog.class);
        }
        return tornLogDao;
    }

    /*-------------------------Torn Data-------------------------------*/

    /**
     * insert a tornLog
     */
    public boolean insertTornLog(List<ClssTornLog> tornLog) {
        try {
            RuntimeExceptionDao<ClssTornLog, Integer> dao = getTornLogDao();
            dao.create(tornLog);
        } catch (RuntimeException e) {
            Log.e(TAG, "", e);
            return false;
        }

        return true;
    }

    /**
     * find tornLog
     */
    @NonNull
    public List<ClssTornLog> findAllTornLog() {
        try {
            RuntimeExceptionDao<ClssTornLog, Integer> dao = getTornLogDao();
            return dao.queryForAll();
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }
        return new ArrayList<>(0);
    }

    /**
     * delete clssTornLog
     */
    public boolean deleteAllTornLog() {
        try {
            RuntimeExceptionDao<ClssTornLog, Integer> dao = getTornLogDao();
            dao.delete(findAllTornLog());
            return true;
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }
        return false;
    }
}
