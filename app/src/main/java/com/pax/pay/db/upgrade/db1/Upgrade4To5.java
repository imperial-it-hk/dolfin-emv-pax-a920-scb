package com.pax.pay.db.upgrade.db1;

import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.support.ConnectionSource;
import com.pax.edc.R;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.CardRange;
import com.pax.pay.base.Issuer;
import com.pax.pay.db.upgrade.DbUpgrader;
import com.pax.pay.emv.EmvAid;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by SORAYA S on 11-Jan-19.
 */

public class Upgrade4To5 extends DbUpgrader {

    private static final Map<String, Object> defAcq = new TreeMap<>();
    private static final Map<String, Object> defTransData = new TreeMap<>();
    private static final Map<String, Object> defUpdateAcq = new TreeMap<>();
    private static final Map<String, Object> defUpdateFloorLmt = new TreeMap<>();
    private static final Map<String, Object> defInsertAcq = new TreeMap<>();
    private static final Map<String, Object> defInsertIssuer = new TreeMap<>();
    private static final Map<String, Object> defUpdateCardRange81 = new TreeMap<>();
    private static final Map<String, Object> defInsertCardRange81_1 = new TreeMap<>();
    private static final Map<String, Object> defInsertCardRange81_2 = new TreeMap<>();
    private static final Map<String, Object> defInsertCardRange81_3 = new TreeMap<>();
    private static final Map<String, Object> defInsertCardRange81_4 = new TreeMap<>();
    private static final Map<String, Object> defInsertCardRange81_5 = new TreeMap<>();

    static {
        //Alter table Acquirer
        defAcq.put(Acquirer.ENABLE, true);

        //Alter table TransData
        defTransData.put("qrType", null);
        defTransData.put("qrID", null);

        //Update table Acquirer (BBL_PROMPT)
        defUpdateAcq.put("terminalId", "80001014");
        defUpdateAcq.put("merchantId", "000002200554822");

        //Update table Issuer,EmvAid (JCB-CARD floorLimit)
        defUpdateFloorLmt.put("floorLimit", 0);

        //Insert QRC_HOST into Acquirer
        defInsertAcq.put(Acquirer.NAME_FIELD_NAME, "QRC_HOST");
        defInsertAcq.put("nii", "021");
        defInsertAcq.put("terminalId", "80001014");
        defInsertAcq.put("merchantId", "000002200554822");
        defInsertAcq.put("currBatchNo", 1);
        defInsertAcq.put("ip", "171.99.133.30");
        defInsertAcq.put("port", 27749);
        defInsertAcq.put(Acquirer.ENABLE_KEYIN, false);
        defInsertAcq.put(Acquirer.ENABLE_QR, true);
        defInsertAcq.put("isEmvTcAdvice", false);
        defInsertAcq.put(Acquirer.BILLER_ID_PROMPTPAY, "010552904093300");
        defInsertAcq.put("billerServiceCode", "BBL");
        defInsertAcq.put("recvTimeout", 60);
        defInsertAcq.put("promptQrTimeout", 60);
        defInsertAcq.put("promptRetryTimeout", 45);
        defInsertAcq.put(Acquirer.ENABLE, true);
        defInsertAcq.put("isDisableTrickFeed", false);
        defInsertAcq.put("isEnableSmallAmt", false);
        defInsertAcq.put(Acquirer.ENABLE_TLE, false);
        defInsertAcq.put(Acquirer.ENABLE_UPI, false);
        defInsertAcq.put(Acquirer.TEST_MODE, false);
        defInsertAcq.put("portBak1", 0);
        defInsertAcq.put("portBak2", 0);
        defInsertAcq.put(Acquirer.SSL_TYPE_FIELD_NAME, "NO_SSL");
        defInsertAcq.put("tcpTimeOut", 0);
        defInsertAcq.put("wirelessTimeOut", 0);

        //Insert QRSALE into Issuer
        defInsertIssuer.put(Issuer.NAME_FIELD_NAME, "QRSALE");
        defInsertIssuer.put(Issuer.ECR_NAME_FIELD_NAME, "QRSALE");
        defInsertIssuer.put(Issuer.ECR_ID_FIELD_NAME, 3);
        defInsertIssuer.put("floorLimit", 0);
        defInsertIssuer.put("adjustPercent", 10);
        defInsertIssuer.put("isAllowManualPan", true);
        defInsertIssuer.put("isReferral", false);
        defInsertIssuer.put("isAutoReversal", false);
        defInsertIssuer.put("panMaskPattern", "(?<=\\\\d{6})\\\\d(?=\\\\d{4})");
        defInsertIssuer.put("acqHostName", "QRC_HOST");
        defInsertIssuer.put("isAllowCheckExpiry", false);
        defInsertIssuer.put("isAllowCheckPanMod10", true);
        defInsertIssuer.put("isAllowExpiry", true);
        defInsertIssuer.put("isAllowPrint", true);
        defInsertIssuer.put("isEnableAdjust", true);
        defInsertIssuer.put("isEnableOffline", true);
        defInsertIssuer.put("isEnableSmallAmt", false);
        defInsertIssuer.put("isRequireMaskExpiry", true);
        defInsertIssuer.put("isRequirePIN", true);
        defInsertIssuer.put("numberOfReceipt", 0);
        defInsertIssuer.put("smallAmount", 0);

        //Update CardRange UnionPay_81_series
        defUpdateCardRange81.put(CardRange.RANGE_HIGH_FIELD_NAME, "8171999999");

        //Insert UP(81 series) panLength 16,17,18,19,20 into CardRange
        defInsertCardRange81_1.put(CardRange.NAME_FIELD_NAME, "UnionPay_8_01");
        defInsertCardRange81_1.put(CardRange.ISSUER_NAME_FIELD_NAME, "UnionPay");
        defInsertCardRange81_1.put(CardRange.RANGE_LOW_FIELD_NAME, "8100000000");
        defInsertCardRange81_1.put(CardRange.RANGE_HIGH_FIELD_NAME, "8171999999");
        defInsertCardRange81_1.put(CardRange.LENGTH_FIELD_NAME, 16);
        defInsertCardRange81_1.put("issuer_id", 4);

        defInsertCardRange81_2.put(CardRange.NAME_FIELD_NAME, "UnionPay_8_02");
        defInsertCardRange81_2.put(CardRange.ISSUER_NAME_FIELD_NAME, "UnionPay");
        defInsertCardRange81_2.put(CardRange.RANGE_LOW_FIELD_NAME, "8100000000");
        defInsertCardRange81_2.put(CardRange.RANGE_HIGH_FIELD_NAME, "8171999999");
        defInsertCardRange81_2.put(CardRange.LENGTH_FIELD_NAME, 17);
        defInsertCardRange81_2.put("issuer_id", 4);

        defInsertCardRange81_3.put(CardRange.NAME_FIELD_NAME, "UnionPay_8_03");
        defInsertCardRange81_3.put(CardRange.ISSUER_NAME_FIELD_NAME, "UnionPay");
        defInsertCardRange81_3.put(CardRange.RANGE_LOW_FIELD_NAME, "8100000000");
        defInsertCardRange81_3.put(CardRange.RANGE_HIGH_FIELD_NAME, "8171999999");
        defInsertCardRange81_3.put(CardRange.LENGTH_FIELD_NAME, 18);
        defInsertCardRange81_3.put("issuer_id", 4);

        defInsertCardRange81_4.put(CardRange.NAME_FIELD_NAME, "UnionPay_8_04");
        defInsertCardRange81_4.put(CardRange.ISSUER_NAME_FIELD_NAME, "UnionPay");
        defInsertCardRange81_4.put(CardRange.RANGE_LOW_FIELD_NAME, "8100000000");
        defInsertCardRange81_4.put(CardRange.RANGE_HIGH_FIELD_NAME, "8171999999");
        defInsertCardRange81_4.put(CardRange.LENGTH_FIELD_NAME, 19);
        defInsertCardRange81_4.put("issuer_id", 4);

        defInsertCardRange81_5.put(CardRange.NAME_FIELD_NAME, "UnionPay_8_05");
        defInsertCardRange81_5.put(CardRange.ISSUER_NAME_FIELD_NAME, "UnionPay");
        defInsertCardRange81_5.put(CardRange.RANGE_LOW_FIELD_NAME, "8100000000");
        defInsertCardRange81_5.put(CardRange.RANGE_HIGH_FIELD_NAME, "8171999999");
        defInsertCardRange81_5.put(CardRange.LENGTH_FIELD_NAME, 20);
        defInsertCardRange81_5.put("issuer_id", 4);
    }

    @Override
    protected void upgrade(SQLiteDatabase db, ConnectionSource cs) throws SQLException {
        DbUpgrader.upgradeTable(db, cs, Acquirer.class, ADD, defAcq);
        DbUpgrader.upgradeTable(db, cs, TransData.class, ADD, defTransData);
        DbUpgrader.upgradeDataTable(db, Acquirer.class, UPDATE, defUpdateAcq, Acquirer.NAME_FIELD_NAME + "='QR_PROMPT'");
        DbUpgrader.upgradeDataTable(db, Issuer.class, UPDATE, defUpdateFloorLmt, Issuer.NAME_FIELD_NAME + "='JCB-CARD'");
        DbUpgrader.upgradeDataTable(db, EmvAid.class, UPDATE, defUpdateFloorLmt, EmvAid.AID_FIELD_NAME + "='A0000000651010'");
        DbUpgrader.upgradeDataTable(db, CardRange.class, UPDATE, defUpdateCardRange81, CardRange.NAME_FIELD_NAME + "='UnionPay_81_series'");
        DbUpgrader.upgradeDataTable(db, CardRange.class, ADD, defInsertCardRange81_1, null);
        DbUpgrader.upgradeDataTable(db, CardRange.class, ADD, defInsertCardRange81_2, null);
        DbUpgrader.upgradeDataTable(db, CardRange.class, ADD, defInsertCardRange81_3, null);
        DbUpgrader.upgradeDataTable(db, CardRange.class, ADD, defInsertCardRange81_4, null);
        DbUpgrader.upgradeDataTable(db, CardRange.class, ADD, defInsertCardRange81_5, null);
        DbUpgrader.upgradeDataTable(db, Acquirer.class, ADD, defInsertAcq, null);
        DbUpgrader.upgradeDataTable(db, Issuer.class, ADD, defInsertIssuer, null);
        DbUpgrader.upgradeAcqIssuerRelation(db, Acquirer.class, 1);//Need to call this method after adding new data in Acquirer or Issuer
        DbUpgrader.upgradeAcqIssuerRelation(db, Issuer.class, 1);//Need to call this method after adding new data in Acquirer or Issuer
    }
}
