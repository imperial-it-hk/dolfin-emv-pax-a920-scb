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

import th.co.bkkps.utils.Log;

import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.pax.pay.emv.EmvAid;
import com.pax.pay.emv.EmvCapk;

import java.util.ArrayList;
import java.util.List;

/**
 * DAO for EMV param table
 */
public class EmvDb {

    private static final String TAG = "EmvDb";

    private RuntimeExceptionDao<EmvAid, Integer> aidDao = null;
    private RuntimeExceptionDao<EmvCapk, Integer> capkDao = null;
    private final BaseDbHelper dbHelper;

    private static EmvDb instance;

    private EmvDb() {
        dbHelper = BaseDbHelper.getInstance();
    }

    /**
     * get the Singleton of the DB Helper
     *
     * @return the Singleton of DB helper
     */
    public static synchronized EmvDb getInstance() {
        if (instance == null) {
            instance = new EmvDb();
        }

        return instance;
    }

    /***************************************
     * Dao
     ******************************************/
    private RuntimeExceptionDao<EmvAid, Integer> getAidDao() {
        if (aidDao == null) {
            aidDao = dbHelper.getRuntimeExceptionDao(EmvAid.class);
        }
        return aidDao;
    }

    private RuntimeExceptionDao<EmvCapk, Integer> getCapkDao() {
        if (capkDao == null) {
            capkDao = dbHelper.getRuntimeExceptionDao(EmvCapk.class);
        }
        return capkDao;
    }

    /*--------------------------AID------------------------------------*/

    /**
     * insert an aid
     */
    public boolean insertAID(EmvAid aid) {
        try {
            RuntimeExceptionDao<EmvAid, Integer> dao = getAidDao();
            dao.create(aid);
        } catch (RuntimeException e) {
            Log.e(TAG, "", e);
            return false;
        }

        return true;
    }

    /**
     * update an aid
     */
    public boolean updateAID(EmvAid aid) {
        try {
            RuntimeExceptionDao<EmvAid, Integer> dao = getAidDao();
            dao.update(aid);
        } catch (RuntimeException e) {
            Log.e(TAG, "", e);
            return false;
        }

        return true;
    }

    /**
     * find aid by aid string
     *
     * @param aid card application id
     * @return aid object
     */
    public EmvAid findAID(String aid) {
        if (aid == null || aid.isEmpty())
            return null;
        try {
            RuntimeExceptionDao<EmvAid, Integer> dao = getAidDao();
            List<EmvAid> list = dao.queryForEq(EmvAid.AID_FIELD_NAME, aid);

            if (list != null && !list.isEmpty())
                return list.get(0);
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    /**
     * find aid by Issuer brand
     *
     * @param issuerBrand Issuer brand
     * @return aid object
     */
    public List<EmvAid> findAIDByIssuerBrand(String issuerBrand) {
        if (issuerBrand == null || issuerBrand.isEmpty())
            return null;
        try {
            RuntimeExceptionDao<EmvAid, Integer> dao = getAidDao();
            List<EmvAid> list = dao.queryForEq("issuerBrand", issuerBrand);

            return list;
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }

        return new ArrayList<>(0);
    }

    /**
     * find all aid
     *
     * @return aid list
     */
    public List<EmvAid> findAllAID() {
        RuntimeExceptionDao<EmvAid, Integer> dao = getAidDao();
        return dao.queryForAll();
    }

    /**
     * delete an aid
     */
    public boolean deleteAID(int id) {
        try {
            RuntimeExceptionDao<EmvAid, Integer> dao = getAidDao();
            dao.deleteById(id);
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "", e);
        }
        return false;
    }

    /**
     * delete the EmvAid list
     */
    public boolean deleteAID(List<EmvAid> emvAids) {
        try {
            RuntimeExceptionDao<EmvAid, Integer> dao = getAidDao();
            dao.delete(emvAids);
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "", e);
        }
        return false;
    }


    /*-------------------------CAPK------------------------------------*/

    /**
     * insert a CAPK
     */
    public boolean insertCAPK(EmvCapk capk) {
        try {
            RuntimeExceptionDao<EmvCapk, Integer> dao = getCapkDao();
            dao.create(capk);
        } catch (RuntimeException e) {
            Log.e(TAG, "", e);
            return false;
        }

        return true;
    }

    /**
     * update a CAPK
     */
    public boolean updateCAPK(EmvCapk capk) {
        try {
            RuntimeExceptionDao<EmvCapk, Integer> dao = getCapkDao();
            dao.update(capk);
        } catch (RuntimeException e) {
            Log.e(TAG, "", e);
            return false;
        }

        return true;
    }

    /**
     * find CAPK by rid string
     *
     * @param rid rid
     * @return CAPK object
     */
    public EmvCapk findCAPK(String rid) {
        if (rid == null || rid.isEmpty())
            return null;
        try {
            RuntimeExceptionDao<EmvCapk, Integer> dao = getCapkDao();
            List<EmvCapk> list = dao.queryForEq(EmvCapk.RID_FIELD_NAME, rid);

            if (list != null && !list.isEmpty())
                return list.get(0);
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }

        return null;
    }

    /**
     * find all CAPK
     *
     * @return CAPK list
     */
    public List<EmvCapk> findAllCAPK() {
        RuntimeExceptionDao<EmvCapk, Integer> dao = getCapkDao();
        return dao.queryForAll();
    }

    /**
     * delete CAPK
     */
    public boolean deleteCAPK(int id) {
        try {
            RuntimeExceptionDao<EmvCapk, Integer> dao = getCapkDao();
            dao.deleteById(id);
            return true;
        } catch (RuntimeException e) {
            Log.w(TAG, "", e);
        }
        return false;
    }
}
