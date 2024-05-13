package com.pax.pay.base;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.pax.pay.trans.model.ETransType;

import java.io.Serializable;

/**
 * transtype_mapping table
 */
@DatabaseTable(tableName = "transtype_mapping")
public class TransTypeMapping implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String ID_FIELD_NAME = "mapping_id";
    public static final String TYPE_FIELD_NAME = "trans_type";
    public static final String PRIORITY_FIELD_NAME = "priority";
    public static final int FIRST_PRIORITY = 1;
    public static final int SECOND_PRIORITY = 2;

    @DatabaseField(generatedId = true, columnName = ID_FIELD_NAME)
    private int id;

    @DatabaseField(canBeNull = false, columnName = TYPE_FIELD_NAME)
    protected ETransType transType;

    @DatabaseField(columnName = PRIORITY_FIELD_NAME)
    protected int priority = 1;

    @DatabaseField(foreign = true, foreignAutoRefresh = true, columnName = Acquirer.ID_FIELD_NAME)
    protected Acquirer acquirer;

    @DatabaseField(foreign = true, foreignAutoRefresh = true, columnName = Issuer.ID_FIELD_NAME)
    protected Issuer issuer;

    protected String acquirerName;
    protected String issuerName;

    public TransTypeMapping() {
    }

    public TransTypeMapping(ETransType transType, Acquirer acquirer, Issuer issuer, int priority) {
        this.transType = transType;
        this.acquirer = acquirer;
        this.issuer = issuer;
        this.priority = priority;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ETransType getTransType() {
        return transType;
    }

    public void setTransType(ETransType transType) {
        this.transType = transType;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Acquirer getAcquirer() {
        return acquirer;
    }

    public void setAcquirer(Acquirer acquirer) {
        this.acquirer = acquirer;
    }

    public Issuer getIssuer() {
        return issuer;
    }

    public void setIssuer(Issuer issuer) {
        this.issuer = issuer;
    }

    public String getAcquirerName() {
        return acquirerName;
    }

    public void setAcquirerName(String acquirerName) {
        this.acquirerName = acquirerName;
    }

    public String getIssuerName() {
        return issuerName;
    }

    public void setIssuerName(String issuerName) {
        this.issuerName = issuerName;
    }
}
