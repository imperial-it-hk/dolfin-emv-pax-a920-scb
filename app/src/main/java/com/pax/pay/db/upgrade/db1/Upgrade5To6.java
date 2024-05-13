package com.pax.pay.db.upgrade.db1;

import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.support.ConnectionSource;
import com.pax.pay.base.Acquirer;
import com.pax.pay.db.upgrade.DbUpgrader;
import com.pax.pay.trans.model.TransData;

import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by SORAYA S on 04-Feb-19.
 */

public class Upgrade5To6 extends DbUpgrader {

    private static final Map<String, Object> defTransData = new TreeMap<>();
    private static final Map<String, Object> defUpdateAcq = new TreeMap<>();
    private static final Map<String, Object> defUpdateAcqTle = new TreeMap<>();

    static {
        //Alter table TransData
        defTransData.put("OrigStanNo", null);
        defTransData.put("branchID", null);

        //Update table Acquirer (BBL_PROMPT/QRC_HOST)
        defUpdateAcq.put(Acquirer.BILLER_ID_PROMPTPAY, "010753600037401");
        defUpdateAcq.put("recvTimeout", 10);
        defUpdateAcq.put("promptQrTimeout", 40);
        defUpdateAcq.put("promptRetryTimeout", 15);

        //Update table Acquirer (KBANK/UNIONPAY) to set default TLE value
        defUpdateAcqTle.put(Acquirer.ENABLE_TLE, true);
    }

    @Override
    protected void upgrade(SQLiteDatabase db, ConnectionSource cs) throws SQLException {
        DbUpgrader.upgradeTable(db, cs, TransData.class, ADD, defTransData);
        DbUpgrader.upgradeDataTable(db, Acquirer.class, UPDATE, defUpdateAcq, Acquirer.NAME_FIELD_NAME + "='QR_PROMPT' OR " + Acquirer.NAME_FIELD_NAME + "='QRC_HOST'");
        DbUpgrader.upgradeDataTable(db, Acquirer.class, UPDATE, defUpdateAcqTle, Acquirer.NAME_FIELD_NAME + "='KBANK' OR " + Acquirer.NAME_FIELD_NAME + "='UNIONPAY'");
    }
}
