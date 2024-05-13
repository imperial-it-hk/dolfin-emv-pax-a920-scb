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
package com.pax.pay.db.upgrade;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import th.co.bkkps.utils.Log;
import androidx.annotation.IntDef;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTable;
import com.j256.ormlite.table.TableUtils;
import com.pax.pay.base.AcqIssuerRelation;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.Issuer;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;

/**
 * called by db helper
 */
public abstract class DbUpgrader {

    private static final String TAG = "DB Upgrader";

    @IntDef({ADD, DELETE, UPDATE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface OperationType {
    }

    public static final int ADD = 0;
    public static final int DELETE = 1;
    public static final int UPDATE = 2;

    public static void upgrade(SQLiteDatabase db, ConnectionSource cs, int oldVersion, int newVersion, String packagePath) {
        try {
            Class<?> c1 = Class.forName(packagePath + ".Upgrade" + oldVersion + "To" + newVersion);
            DbUpgrader upgrader = (DbUpgrader) c1.newInstance();
            Log.i("DbUpgrader", "upgrading from version(" + oldVersion +
                    ") to version(" + newVersion + ")");
            upgrader.upgrade(db, cs);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            Log.w(TAG, "", e);
            throw new IllegalArgumentException("No Upgrader for " + db.toString() +
                    " from version(" + oldVersion +
                    ") to version(" + newVersion + ")");
        } catch (SQLException e) {
            Log.w(TAG, "", e);
        }
    }

    private static void createTable(SQLiteDatabase db, ConnectionSource cs, Class clazz) throws SQLException {
        try {
            String sql = (String) TableUtils.getCreateTableStatements(cs, clazz).get(0);
            db.execSQL(sql);
        } catch (Exception e) {
            Log.w(TAG, e);
            TableUtils.createTable(cs, clazz);
        }
    }

    protected static void upgradeTable(SQLiteDatabase db, ConnectionSource cs, Class clazz, @OperationType int type, Map<String, Object> columnWithValue) throws SQLException {
        String tableName = extractTableName(clazz);

        db.beginTransaction();
        try {
            //Rename table
            String tempTableName = tableName + "_temp";
            String sql = "ALTER TABLE " + tableName + " RENAME TO " + tempTableName;
            db.execSQL(sql);

            //Create table
            createTable(db, cs, clazz);

            //Load data
            String columns;
            if (type == ADD) {
                columns = Arrays.toString(getColumnNames(db, tempTableName));
            } else if (type == DELETE) {
                columns = Arrays.toString(getColumnNames(db, tableName));
            } else {
                throw new IllegalArgumentException("OperationType error");
            }

            columns = columns.replace("[", "").replace("]", "");
            sql = "INSERT INTO " + tableName + "(" + columns + ") " + "SELECT " + columns + " FROM " + tempTableName;
            db.execSQL(sql);

            // update default value
            if (type == ADD && columnWithValue != null && columnWithValue.size() > 0) {
                String[] sets = columnWithValue.keySet().toArray((new String[columnWithValue.size()]));
                for (int i = 0; i < sets.length; ++i) {
//                    sets[i] += ("='" + columnWithValue.get(sets[i]).toString() + "'");
                    sets[i] += getColumnWithValue(columnWithValue.get(sets[i]), UPDATE);
                }
                String newColumn = Arrays.toString(sets).replace("[", "").replace("]", "");
                sql = "UPDATE " + tableName + " SET " + newColumn;
                db.execSQL(sql);
            }

            //Drop temp table
            sql = "DROP TABLE IF EXISTS " + tempTableName;
            db.execSQL(sql);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

    }

    protected static void upgradeDataTable(SQLiteDatabase db, Class clazz, @OperationType int type, Map<String, Object> columnWithValue, String where) throws SQLException {
        String tableName = extractTableName(clazz);
        db.beginTransaction();
        try {
            if (columnWithValue != null && !columnWithValue.isEmpty()) {
                String[] sets = columnWithValue.keySet().toArray((new String[columnWithValue.size()]));
                String[] colValAdd = new String[columnWithValue.size()];
                for (int i = 0; i < sets.length; ++i) {
                    if (type == ADD) {
                        colValAdd[i] = getColumnWithValue(columnWithValue.get(sets[i]), type);
                    } else if (type == UPDATE) {
                        sets[i] += getColumnWithValue(columnWithValue.get(sets[i]), type);
                    } else {
                        throw new IllegalArgumentException("OperationType error");
                    }
                }
                String sql;
                if (type == ADD) {
                    String columns = Arrays.toString(sets).replace("[", "").replace("]", "");
                    String values = Arrays.toString(colValAdd).replace("[", "").replace("]", "");
                    sql = "INSERT INTO " + tableName + "(" + columns + ") VALUES (" + values + ")";
                    db.execSQL(sql);
                } else if (type == UPDATE) {
                    String newColumn = Arrays.toString(sets).replace("[", "").replace("]", "");
                    sql = "UPDATE " + tableName + " SET " + newColumn + (where != null ? " WHERE " + where : "");
                    db.execSQL(sql);
                } else {
                    throw new IllegalArgumentException("OperationType error");
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * For add Acquirer and Issuer relation data.
     * Need to call this method after adding new data in Acquirer or Issuer
     * @param db
     * @param clazz
     * @param newData
     * @throws SQLException
     */
    public static void upgradeAcqIssuerRelation(SQLiteDatabase db, Class clazz, int newData) throws SQLException {
        String tableName = extractTableName(clazz);
        Cursor cursor = null;
        String sql, columns;
        db.beginTransaction();
        try {
            if (clazz != null) {
                cursor = db.rawQuery("SELECT count(*) from " + tableName, null);
                cursor.moveToFirst();
                int numRows = cursor.getInt(0);

                if (numRows > newData) {
                    columns = Acquirer.ID_FIELD_NAME + ", " + Issuer.ID_FIELD_NAME;
                    tableName = extractTableName(AcqIssuerRelation.class);

                    if (Acquirer.class == clazz) {
                        for (int i = numRows; i > (numRows - newData); i--) {
                            cursor = db.rawQuery("SELECT count(*) FROM " + extractTableName(Issuer.class), null);
                            cursor.moveToFirst();
                            int numIssuers = cursor.getInt(0);
                            for (int j = 1; j <= numIssuers; j++) {
                                cursor = db.rawQuery("SELECT count(*) FROM " + tableName + " WHERE " + Acquirer.ID_FIELD_NAME + "=" + i + " AND " + Issuer.ID_FIELD_NAME + "=" + j, null);
                                cursor.moveToFirst();
                                if (cursor.getInt(0) <= 0) {
                                    sql = "INSERT INTO " + tableName + "(" + columns + ") VALUES (" + i + ", " + j + ")";
                                    db.execSQL(sql);
                                }
                            }
                        }
                    }
                    else if (Issuer.class == clazz) {
                        for (int i = numRows; i > (numRows - newData); i--) {
                            cursor = db.rawQuery("SELECT count(*) FROM " + extractTableName(Acquirer.class), null);
                            cursor.moveToFirst();
                            int numAcq = cursor.getInt(0);
                            for (int j = 1; j <= numAcq; j++) {
                                cursor = db.rawQuery("SELECT count(*) FROM " + tableName + " WHERE " + Acquirer.ID_FIELD_NAME + "=" + j + " AND " + Issuer.ID_FIELD_NAME + "=" + i, null);
                                cursor.moveToFirst();
                                if (cursor.getInt(0) <= 0) {
                                    sql = "INSERT INTO " + tableName + "(" + columns + ") VALUES (" + j + ", " + i + ")";
                                    db.execSQL(sql);
                                }
                            }
                        }
                    }
                }
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.endTransaction();
        }
    }

    private static String[] getColumnNames(SQLiteDatabase db, String tableName) {
        String[] columnNames = null;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * from " + tableName, null);
            if (cursor != null) {
                columnNames = cursor.getColumnNames();
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return columnNames;
    }

    private static <T> String extractTableName(Class<T> clazz) {
        DatabaseTable databaseTable = clazz.getAnnotation(DatabaseTable.class);
        String name = null;
        if (databaseTable != null && databaseTable.tableName() != null && !databaseTable.tableName().isEmpty()) {
            name = databaseTable.tableName();
        }

        if (name == null) {
            name = clazz.getSimpleName().toLowerCase();
        }

        return name;
    }

    private static String getColumnWithValue(Object obj, @OperationType int type) {
        String value = "NULL";
        if (obj == null) {
            value = (type == UPDATE) ? "=" + value : value;
        } else {
            if (obj instanceof String) {
                value = (type == UPDATE) ? ("='" + obj.toString() + "'") : ("'" + obj.toString() + "'");
            } else if (obj instanceof Integer) {
                value = (type == UPDATE) ? ("=" + ((Integer) obj).intValue()) : ("" + ((Integer) obj).intValue());
            } else if (obj instanceof Boolean) {
                value = (type == UPDATE) ? ("=" + (((Boolean) obj).booleanValue() ? 1 : 0)) : ("" + (((Boolean) obj).booleanValue() ? 1 : 0));
            }
        }
        return value;
    }

    protected abstract void upgrade(SQLiteDatabase db, ConnectionSource cs) throws SQLException;
}
