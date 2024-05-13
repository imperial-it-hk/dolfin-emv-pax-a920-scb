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
package com.pax.pay.base

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable

/**
 * Merchant profile and Merchant Acq relation
 */
@DatabaseTable(tableName = "merchant_profile_acq_relation")
class MerchantProfileAcqRelation() {

    constructor (merchantProfile: MerchantProfile?, merchantAcqProfile: MerchantAcqProfile?) : this() {
        this.merchantProfile = merchantProfile
        this.merchantAcqProfile = merchantAcqProfile
    }

    constructor (id: Int, merchantProfile: MerchantProfile?, merchantAcqProfile: MerchantAcqProfile?) : this() {
        this.id = id
        this.merchantProfile = merchantProfile
        this.merchantAcqProfile = merchantAcqProfile
    }

    @DatabaseField(generatedId = true)
    var id: Int = 0

    @DatabaseField(foreign = true, foreignAutoRefresh = true, columnName = MerchantProfile.ID)
    var merchantProfile: MerchantProfile? = null

    @DatabaseField(foreign = true, foreignAutoRefresh = true, columnName = MerchantAcqProfile.ID)
    var merchantAcqProfile: MerchantAcqProfile? = null
}
