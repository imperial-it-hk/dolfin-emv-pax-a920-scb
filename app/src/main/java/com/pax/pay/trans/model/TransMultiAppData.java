package com.pax.pay.trans.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.pax.pay.base.Acquirer;

import java.io.Serializable;

@DatabaseTable(tableName = "trans_multi_app_data")
public class TransMultiAppData implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String ID_FIELD_NAME = "id";
    public static final String STANNO_FIELD_NAME = "stan_no";
    public static final String TRACENO_FIELD_NAME = "trace_no";
    public static final String BATCHNO_FIELD_NAME = "batch_no";

    @DatabaseField(generatedId = true, columnName = ID_FIELD_NAME)
    protected int id;

    @DatabaseField(canBeNull = false, columnName = STANNO_FIELD_NAME)
    protected long stanNo;

    @DatabaseField(canBeNull = false, columnName = TRACENO_FIELD_NAME)
    protected long traceNo;

    @DatabaseField(canBeNull = false, columnName = BATCHNO_FIELD_NAME)
    protected long batchNo;

    @DatabaseField(foreign = true, foreignAutoRefresh = true, columnName = Acquirer.ID_FIELD_NAME)
    protected Acquirer acquirer;

    public TransMultiAppData() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getStanNo() {
        return stanNo;
    }

    public void setStanNo(long stanNo) {
        this.stanNo = stanNo;
    }

    public long getTraceNo() {
        return traceNo;
    }

    public void setTraceNo(long traceNo) {
        this.traceNo = traceNo;
    }

    public long getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(long batchNo) {
        this.batchNo = batchNo;
    }

    public Acquirer getAcquirer() {
        return acquirer;
    }

    public void setAcquirer(Acquirer acquirer) {
        this.acquirer = acquirer;
    }
}
