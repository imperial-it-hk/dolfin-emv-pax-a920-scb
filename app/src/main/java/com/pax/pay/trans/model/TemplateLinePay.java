package com.pax.pay.trans.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;
import java.util.Date;


/**
 * Created by WITSUTA A on 5/21/2018.
 */

@DatabaseTable(tableName = "template_linepay")
public class TemplateLinePay implements Serializable {

    public static final String TEMPLATE_ID = "id";
    public static final String TEMPLATE_DATA = "template_data";
    public static final String DEFAULT_CHAR_MODE = "default_char_mode";
    public static final String LAST_USAGE_TIMESTAMP= "last_usage_timestamp";

    @DatabaseField(canBeNull = false, columnName = TEMPLATE_ID)
    protected int id;
    @DatabaseField(canBeNull = false, dataType = DataType.BYTE_ARRAY, columnName = TEMPLATE_DATA)
    protected byte[] templateData;
    @DatabaseField(canBeNull = false, columnName = DEFAULT_CHAR_MODE)
    protected String defaultCharMode;
    @DatabaseField(columnName = LAST_USAGE_TIMESTAMP)
    protected Date lastUsageTimestmp;
    @DatabaseField(canBeNull = false)
    protected Boolean isTemplateCorrect;
    @DatabaseField(canBeNull = false, dataType=DataType.SERIALIZABLE)
    protected String[] tailSlip;
    @DatabaseField(canBeNull = false, dataType= DataType.SERIALIZABLE)
    protected String[] templateInfo;


    public TemplateLinePay(){

    }

    //copy constructor, replace clone
    public TemplateLinePay(TemplateLinePay other) {
        this.id = other.id;
        this.templateData = other.templateData;
        this.defaultCharMode = other.defaultCharMode;
        this.lastUsageTimestmp = other.lastUsageTimestmp;
        this.isTemplateCorrect = other.isTemplateCorrect;
        this.tailSlip = other.tailSlip;
        this.templateInfo = other.templateInfo;
    }

    public int getTemplateId() {
        return id;
    }

    public void setTemplateId(int id) {
        this.id = id;
    }

    public byte[] getTemplateData() {
        return templateData;
    }

    public void setTemplateData(byte[] templateData) {
        this.templateData = templateData;
    }

    public String getDefaultCharMode() {
        return defaultCharMode;
    }

    public Date getLastUsageTimestmp() {
        return lastUsageTimestmp;
    }

    public void setLastUsageTimestmp(Date lastUsageTimestmp) {
        this.lastUsageTimestmp = lastUsageTimestmp;
    }

    public void setDefaultCharMode(String defaultCharMode) {
        this.defaultCharMode = defaultCharMode;
    }

    public boolean getTemplateCorrect() {
        return isTemplateCorrect;
    }

    public void setTemplateCorrect(Boolean templateCorrect) {
        isTemplateCorrect = templateCorrect;
    }

    public String[] getTailSlip() {
        return tailSlip;
    }

    public void setTailSlip(String[] tailSlip) {
        this.tailSlip = tailSlip;
    }

    public String[] getTemplateInfo() {
        return templateInfo;
    }

    public void setTemplateInfo(String[] templateInfo) {
        this.templateInfo = templateInfo;
    }
}
