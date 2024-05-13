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

import java.sql.SQLException;

/**
 * Called by DbUpgrader by reflection
 */
public class Upgrade1To2 extends DbUpgrader {


   /* private static final Map<String, Object> def = new TreeMap<>();
    static {
        def.put(Acquirer.SSL_TYPE_FIELD_NAME, SysParam.Constant.CommSslType.NO_SSL);
    };*/

    @Override
    protected void upgrade(SQLiteDatabase db, ConnectionSource cs) throws SQLException {
        //DbUpgrader.upgradeTable(db, cs, Acquirer.class, OperationType.ADD, def);
    }
}
