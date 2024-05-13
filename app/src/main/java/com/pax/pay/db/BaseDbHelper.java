/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-2-17
 * Module Author: Kim.L
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.db;

import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.pax.appstore.DownloadParamReceiver;
import com.pax.edc.BuildConfig;
import com.pax.pay.SplashActivity;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.AcqIssuerRelation;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.CardRange;
import com.pax.pay.base.EReceiptLogoMapping;
import com.pax.pay.base.Issuer;
import com.pax.pay.base.MerchantAcqProfile;
import com.pax.pay.base.MerchantProfileAcqRelation;
import com.pax.pay.base.TransTypeMapping;
import com.pax.pay.emv.CardBin;
import com.pax.pay.emv.CardBinBlack;
import com.pax.pay.emv.EmvAid;
import com.pax.pay.emv.EmvCapk;
import com.pax.pay.emv.clss.ClssTornLog;
import com.pax.pay.trans.model.Controller;
import com.pax.pay.base.MerchantProfile;
import com.pax.pay.trans.model.TemplateLinePay;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.model.TransMultiAppData;
import com.pax.pay.trans.model.TransRedeemKbankTotal;
import com.pax.pay.trans.model.TransTotal;
import com.pax.pay.utils.Utils;

import java.sql.SQLException;

import th.co.bkkps.utils.Log;

/**
 * DB helper
 */
class BaseDbHelper extends OrmLiteSqliteOpenHelper {
    protected static final String TAG = "DB";
    // DB Name
    private static final String DATABASE_NAME = "data.db";
    // DB version
    /**
     * 6: TransData: +OrigStanNo +branchID
     * 5: Acquirer: +enable, TransData: +qrType +qrID
     * 4: Acquirer: +QR
     * 3: TransData: +signPath
     * 2: test
     * 1: init
     */
    //private static final int DATABASE_VERSION = 6;
    private static final int DATABASE_VERSION = BuildConfig.VERSION_CODE;
    // DB Upgrader packagePath
    private static final String UPGRADER_PATH = "com.pax.pay.db.upgrade.db1";
    private static BaseDbHelper instance;

    private BaseDbHelper() {
        super(FinancialApplication.getApp(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqliteDatabase, ConnectionSource connectionSource) {
        SplashActivity.Companion.setInitialState(SplashActivity.InitialState.APP_FIRST_INITIAL);
        Log.d(SplashActivity.TAG,"----------------------------------> app_db_onCreate");
        try {
            TableUtils.createTableIfNotExists(connectionSource, AcqIssuerRelation.class);
            TableUtils.createTableIfNotExists(connectionSource, Acquirer.class);
            TableUtils.createTableIfNotExists(connectionSource, Issuer.class);
            TableUtils.createTableIfNotExists(connectionSource, CardRange.class);
            TableUtils.createTableIfNotExists(connectionSource, TransTypeMapping.class);
            TableUtils.createTableIfNotExists(connectionSource, CardBin.class);
            TableUtils.createTableIfNotExists(connectionSource, CardBinBlack.class);
            TableUtils.createTableIfNotExists(connectionSource, EmvAid.class);
            TableUtils.createTableIfNotExists(connectionSource, EmvCapk.class);
            TableUtils.createTableIfNotExists(connectionSource, TransData.class);
            TableUtils.createTableIfNotExists(connectionSource, TransTotal.class);
            TableUtils.createTableIfNotExists(connectionSource, ClssTornLog.class);
            TableUtils.createTableIfNotExists(connectionSource, TemplateLinePay.class);
            TableUtils.createTableIfNotExists(connectionSource, TransRedeemKbankTotal.class);
            TableUtils.createTableIfNotExists(connectionSource, EReceiptLogoMapping.class);

            TableUtils.createTableIfNotExists(connectionSource, MerchantProfile.class);
            TableUtils.createTableIfNotExists(connectionSource, MerchantAcqProfile.class);
            TableUtils.createTableIfNotExists(connectionSource, MerchantProfileAcqRelation.class);
            TableUtils.createTableIfNotExists(connectionSource, TransMultiAppData.class);
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqliteDatabase, ConnectionSource connectionSource, int oldVersion,
                          int newVersion) {
        try {
            Log.d(SplashActivity.TAG,"----------------------------------> app_upgrading");
            SplashActivity.Companion.setInitialState(SplashActivity.InitialState.APP_UPGRADING);
//            for (int i = oldVersion; i < newVersion; ++i) {
//                DbUpgrader.upgrade(sqliteDatabase, connectionSource, i, i + 1, UPGRADER_PATH);
//            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "", e);
        }
    }

    /**
     * get the Singleton of the DB Helper
     *
     * @return the Singleton of DB helper
     */
    public static synchronized BaseDbHelper getInstance() {
        if (instance == null) {
            if(Utils.isUpgrade(FinancialApplication.getApp())){
                // additional for app upgrading

                FinancialApplication.getApp().deleteDatabase(DATABASE_NAME);
                Log.d(SplashActivity.TAG,"\tDatabase was deleted successful");

                Controller.set(Controller.IS_FIRST_RUN, true);
                Log.d(SplashActivity.TAG,"\t[Controller.IS_FIRST_DOWNLOAD_PARAM_NEEDED] Flag = " + Controller.isRequireDownloadParam());

                SplashActivity.Companion.setInitialState(SplashActivity.InitialState.APP_UPGRADED);
                SplashActivity.Companion.enableBroadcastReceiver(false, DownloadParamReceiver.class);
                SplashActivity.Companion.setUpgradeStatus(true);
                Log.d(SplashActivity.TAG,"----------------------------------> app_upgrade");

                //KeyDataReadWriteJson.saveKeyDataToFile();
                //FinancialApplication.getApp().deleteDatabase(DATABASE_NAME);
                //Controller.set(Controller.IS_FIRST_RUN, true);
                //Controller.set(Controller.IS_SP200_NEED_UPDATE, false);
            }
            instance = new BaseDbHelper();
        }
        return instance;
    }
}
