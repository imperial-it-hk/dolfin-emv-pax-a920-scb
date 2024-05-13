package com.pax.pay.db

import com.j256.ormlite.dao.RuntimeExceptionDao
import com.j256.ormlite.table.TableUtils
import com.pax.pay.base.MerchantAcqProfile
import com.pax.pay.base.MerchantProfile
import com.pax.pay.base.MerchantProfileAcqRelation
import th.co.bkkps.utils.Log
import java.sql.SQLException

object  MerchantProfileDb {
    private val TAG = "MerchantProfileDb"

    private var dataDao: RuntimeExceptionDao<MerchantProfile, Int>? = null
    private var relationDao: RuntimeExceptionDao<MerchantProfileAcqRelation, Int>? = null

    private var dbHelper: BaseDbHelper? = null

    init {
        dbHelper = BaseDbHelper.getInstance()
    }
    /***************************************
     * Dao
     */
    private fun getDao(): RuntimeExceptionDao<MerchantProfile, Int>? {
        if (dataDao == null) {
            dataDao = dbHelper!!.getRuntimeExceptionDao(
                MerchantProfile::class.java
            )
        }
        return dataDao
    }

    private fun getRelationDao(): RuntimeExceptionDao<MerchantProfileAcqRelation, Int>? {
        if (relationDao == null) {
            relationDao = dbHelper!!.getRuntimeExceptionDao(
                MerchantProfileAcqRelation::class.java)
        }
        return relationDao
    }


    /**
     * insert
     */
    fun insertData(merchantProfile: MerchantProfile): Boolean {
        try {
            val dao = getDao()
            dao!!.create(merchantProfile)
            return true
        } catch (e: RuntimeException) {
            Log.e(TAG, "", e)
        }
        return false
    }

    /**
     * find
     */
    fun findData(id: Int): MerchantProfile? {
        try {
            val dao = getDao()
            val queryBuilder = dao!!.queryBuilder()
            val where = queryBuilder.where().eq(MerchantProfile.ID, id)
            return where.queryForFirst()
        } catch (e: SQLException) {
            Log.w(TAG, "", e)
        }
        return null
    }

    /**
     * find
     */
    fun findData(merchantName: String): MerchantProfile? {
        try {
            val dao = getDao()
            val queryBuilder = dao!!.queryBuilder()
            val where = queryBuilder.where().eq(MerchantProfile.MERCHANT_NAME, merchantName)
            return where.queryForFirst()
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
            deleteBuilder.where().eq(MerchantProfile.ID, id)
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
    fun findAllData(): List<MerchantProfile>? {
        return findAllData(false)
    }

    fun findAllData(ascSortMode : Boolean): List<MerchantProfile>? {
        try {
            val dao = getDao()
            if (ascSortMode) {
                val queryBuilder = dao!!.queryBuilder().orderBy(MerchantProfile.ID, true)
                return queryBuilder.query().toList()
            } else {
                return dao!!.queryForAll()
            }
        } catch (e: RuntimeException) {
            Log.w(TAG, "", e)
        }
        return ArrayList(0)
    }

    fun findFirstRecord(): MerchantProfile? {
        try {
            val dao = getDao()
            val queryBuilder = dao!!.queryBuilder().orderBy(MerchantProfile.ID, true)
            return queryBuilder.queryForFirst()
        } catch (e: RuntimeException) {
            Log.w(TAG, "", e)
        }
        return null
    }

    /**
     * Update
     */
    fun updateData(merchantProfile: MerchantProfile): Boolean {
        try {
            val dao = getDao()
            val updateBuilder = dao!!.updateBuilder()
            updateBuilder.where().eq(MerchantProfile.ID, merchantProfile.id)
            updateBuilder.updateColumnValue(
                MerchantProfile.MERCHANT_NAME, merchantProfile.merchantLabelName)
            updateBuilder.update()
            return true
        } catch (e: SQLException) {
            Log.w(TAG, "", e)
        }
        return false
    }

    /**
     * bind
     */
    fun bind(root: MerchantProfile?, merchantAcqProfile: MerchantAcqProfile?): Boolean {
        try {
            val dao = getRelationDao()
            val relation: MerchantProfileAcqRelation? = findRelation(root, merchantAcqProfile)
            if (null == relation) {
                dao!!.create(MerchantProfileAcqRelation(root, merchantAcqProfile)) //ignore the return value from create
            }
        } catch (e: java.lang.RuntimeException) {
            Log.w(TAG, "", e)
            return false
        }
        return true
    }

    fun clearTable(){
        try {
            TableUtils.clearTable(BaseDbHelper.getInstance().connectionSource,
                MerchantProfile::class.java)
        } catch (e: SQLException) {
            Log.w(TAG, "", e)
        }
    }



    /*------------------------------Relation---------------------------*/ /**
     * find the relation
     */
    private fun findRelation(merchantProfile: MerchantProfile?, merchantAcqProfile: MerchantAcqProfile?): MerchantProfileAcqRelation? {
        val dao = getRelationDao()
        val fieldsMap: MutableMap<String, Any> = HashMap()
        fieldsMap[MerchantProfile.ID] = merchantProfile!!
        fieldsMap[MerchantAcqProfile.ID] = merchantAcqProfile!!
        val relation = dao!!.queryForFieldValues(fieldsMap)
        return if (relation != null && relation.isNotEmpty()) relation[0] else null
    }
}

private fun <T, ID> RuntimeExceptionDao<T, ID>.create(merchantProfile: MerchantProfile.Companion) {

}
