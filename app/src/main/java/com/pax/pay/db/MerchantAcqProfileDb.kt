package com.pax.pay.db

import com.j256.ormlite.dao.RuntimeExceptionDao
import com.j256.ormlite.stmt.StatementBuilder
import com.j256.ormlite.table.TableUtils
import com.pax.pay.base.MerchantAcqProfile
import com.pax.pay.base.MerchantProfile
import th.co.bkkps.utils.Log
import java.sql.SQLException

object  MerchantAcqProfileDb {
    private val TAG = "MerchantAcqProfileDb"

    private var transDao: RuntimeExceptionDao<MerchantAcqProfile, Int>? = null
    private var dbHelper: BaseDbHelper? = null

    init {
        dbHelper = BaseDbHelper.getInstance()
    }
    /***************************************
     * Dao
     */
    private fun getDao(): RuntimeExceptionDao<MerchantAcqProfile, Int>? {
        if (transDao == null) {
            transDao = dbHelper!!.getRuntimeExceptionDao(
                MerchantAcqProfile::class.java
            )
        }
        return transDao
    }


    /**
     * insert
     */
    fun insertData(merchantAcqProfile: MerchantAcqProfile): Boolean {
        try {
            val dao = getDao()
            dao!!.create(merchantAcqProfile)
            return true
        } catch (e: RuntimeException) {
            Log.e(TAG, "", e)
        }
        return false
    }

    /**
     * find
     */
    fun findData(id: Int): MerchantAcqProfile? {
        try {
            val dao = getDao()
            val queryBuilder = dao!!.queryBuilder()
            val where = queryBuilder.where().eq(MerchantAcqProfile.ID, id)
            return where.queryForFirst()
        } catch (e: SQLException) {
            Log.w(TAG, "", e)
        }
        return null
    }

    /**
     * find
     */
    fun findAcqFromMerchant(merchantName: String, acquirerName: String?): List<MerchantAcqProfile>? {
        try {
            val dao = getDao()
            val queryBuilder = dao!!.queryBuilder()
            acquirerName?.let {
                val where = queryBuilder.where().eq(MerchantAcqProfile.MERCHANT_NAME, merchantName).and().eq(MerchantAcqProfile.ACQ_HOST_NAME, acquirerName)
                return where.query()
            }?: run {
                val where = queryBuilder.where().eq(MerchantAcqProfile.MERCHANT_NAME, merchantName)
                return where.query()
            }
        } catch (e: SQLException) {
            Log.w(TAG, "", e)
        }
        return null
    }

    /**
     * delete
     */
    fun deleteData(id: Int): Boolean {
        try {
            val dao = getDao()
            val deleteBuilder = dao!!.deleteBuilder()
            deleteBuilder.where().eq(MerchantAcqProfile.ID, id)
            dao.delete(deleteBuilder.prepare())
            return true
        } catch (e: SQLException) {
            Log.w(TAG, "", e)
        }
        return false
    }

    /**
     * delete all
     */
    fun deleteAllData(): Boolean {
        try {
            val dao = getDao()
            val deleteBuilder = dao!!.deleteBuilder()
            dao.delete(deleteBuilder.prepare())
            return true
        } catch (e: SQLException) {
            Log.w(TAG, "", e)
        }
        return false
    }

    /**
     * find all
     */
    fun findAllData(): List<MerchantAcqProfile>? {
        try {
            val dao = getDao()
            return dao!!.queryForAll()
        } catch (e: RuntimeException) {
            Log.w(TAG, "", e)
        }
        return ArrayList(0)
    }

    /**
     * Update
     */
    fun updateData(merchantAcqProfile: MerchantAcqProfile): Boolean {
        try {
            val dao = getDao()
            val updateBuilder = dao!!.updateBuilder()
            updateBuilder.where().eq(MerchantAcqProfile.ID, merchantAcqProfile.id)
            updateBuilder.updateColumnValue(
                MerchantAcqProfile.MERCHANT_NAME, merchantAcqProfile.merchantName)
            updateBuilder.update()
            return true
        } catch (e: SQLException) {
            Log.w(TAG, "", e)
        }
        return false
    }

    fun updateTerminalIDMerchantID(merchantAcqProfile: MerchantAcqProfile): Boolean {
        try {
            val dao = getDao()
            dao?.let {
                return (if (it.update(merchantAcqProfile) == 1)  true else false)
            }
        } catch (e: SQLException) {
            Log.w(TAG, "", e)
        }
        return false
    }

    fun clearTable(){
        try {
            TableUtils.clearTable(BaseDbHelper.getInstance().connectionSource,
                MerchantAcqProfile::class.java)
        } catch (e: SQLException) {
            Log.w(TAG, "", e)
        }
    }
}

private fun <T, ID> RuntimeExceptionDao<T, ID>.create(merchantProfile: MerchantProfile.Companion) {

}
