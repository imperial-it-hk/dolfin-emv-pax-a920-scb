/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-4-19
 * Module Author: Kim.L
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.db.upgrade.db1;

import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.support.ConnectionSource;
import com.pax.pay.db.upgrade.DbUpgrader;
import com.pax.pay.trans.model.TransData;

import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Called by DbUpgrader by reflection
 */
public class Upgrade2To3 extends DbUpgrader {


    private static final Map<String, Object> def = new TreeMap<>();

    static {
        def.put(TransData.SIGN_PATH, "");
    }

    @Override
    protected void upgrade(SQLiteDatabase db, ConnectionSource cs) throws SQLException {
        DbUpgrader.upgradeTable(db, cs, TransData.class, ADD, def);
    }
}
