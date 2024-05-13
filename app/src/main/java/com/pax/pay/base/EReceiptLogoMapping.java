package com.pax.pay.base;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.pax.eemv.utils.Tools;

import java.io.Serializable;

@DatabaseTable(tableName = "e_receipt_logo_mapping")
public class EReceiptLogoMapping implements Serializable {
    public static final String ID_FIELD_NAME = "ercm_id";
    public static final String ERCM_HOST_INDEX = "ercm_host_index";
    public static final String ERCM_ACQUIRER_NII = "ercm_acquirer_nii";
    public static final String ERCM_ACQUIRER_NAME = "ercm_acquirer_name";
    public static final String ERCM_SESSIONKEY_ENCRYPTED = "ercm_ssk_encrypted";
    public static final String ERCM_SESSIONKEY_KCV = "ercm_ssk_kcv";
    public static final String ERCM_SESSIONKEY_TXT = "ercm_ssk_clear_text";

    @DatabaseField(generatedId = true, columnName = ID_FIELD_NAME)
    private int id;
    @DatabaseField(columnName = ERCM_HOST_INDEX)
    private String ERCM_INIT_HOST_INDEX;
    @DatabaseField(columnName = ERCM_ACQUIRER_NII)
    private String ERCM_INIT_ACQUIRER_NII ;
    @DatabaseField(columnName = ERCM_ACQUIRER_NAME)
    private String ERCM_INIT_ACQUIRER_NAME ;
    @DatabaseField
    private String ERCM_INIT_RAW_HEADER_LOGO_FILE ;
    @DatabaseField
    private String ERCM_INIT_RAW_FOOTER_LOGO_FILE ;
    @DatabaseField
    private String ERCM_INIT_SSK_INFOS_FILE ;
    @DatabaseField
    private String ERCM_SSKB_TLE_INDICATOR ;
    @DatabaseField
    private String ERCM_SSKB_ERC_VERSION ;
    @DatabaseField
    private String ERCM_BANK_CODE ;
    @DatabaseField
    private String ERCM_TSN ;
    @DatabaseField
    private String ERCM_SSKB_KEK_TYPE ;
    @DatabaseField
    private String ERCM_SSKB_KEK_VERSION ;
    @DatabaseField(columnName = ERCM_SESSIONKEY_TXT, dataType = DataType.BYTE_ARRAY)
    private byte[] ERCM_SSKB_SSK_TXT ;
    @DatabaseField(columnName = ERCM_SESSIONKEY_KCV, dataType = DataType.BYTE_ARRAY)
    private byte[] ERCM_SSKB_SSK_KCV ;
    @DatabaseField(columnName = ERCM_SESSIONKEY_ENCRYPTED, dataType = DataType.BYTE_ARRAY)
    private byte[] ERCM_SSKB_SSK_ENC ;
    @DatabaseField
    private String ERCM_LAST_UPDATE ;

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public void setSaveDateTime(String exDateTime) {this.ERCM_LAST_UPDATE = exDateTime;}
    public String getSaveDateTime() {return this.ERCM_LAST_UPDATE;}

    public void setSessionKeyEncrypted(byte[] exSessionKeyEncrypted) {this.ERCM_SSKB_SSK_ENC = exSessionKeyEncrypted;}
    public byte[] getSessionKeyEncrypted() {return this.ERCM_SSKB_SSK_ENC;}

    public void setSessionKeyKCV(byte[] exSessionKeyKCV) {this.ERCM_SSKB_SSK_KCV = exSessionKeyKCV;}
    public byte[] getSessionKeyKCV() {return this.ERCM_SSKB_SSK_KCV;}

    public void setSessionKeyClearText(byte[] exSessionKeyClearText) {this.ERCM_SSKB_SSK_TXT = exSessionKeyClearText;}
    public byte[] getSessionKeyClearText() {return this.ERCM_SSKB_SSK_TXT;}

    public void setKekVersion(String exKekVersion) {this.ERCM_SSKB_KEK_VERSION = exKekVersion;}
    public String getKekVersion() {return this.ERCM_SSKB_KEK_VERSION;}

    public void setKekType(String exKekType) {this.ERCM_SSKB_KEK_TYPE = exKekType;}
    public String getKekType() {return this.ERCM_SSKB_KEK_TYPE;}

    public void setTerminalSerialNumber(String exTSN) {this.ERCM_TSN = exTSN;}
    public String getTerminalSerialNumber() {return this.ERCM_TSN;}

    public void setBankCode(String exBankCode) {this.ERCM_BANK_CODE = exBankCode;}
    public String getBankCode() {return this.ERCM_BANK_CODE;}

    public void setErcVersion(String exErcVersion) {this.ERCM_SSKB_ERC_VERSION = exErcVersion;}
    public String getErcVersion() {return this.ERCM_SSKB_ERC_VERSION;}

    public void setTleIndicator(String exTleIndicator) {this.ERCM_SSKB_TLE_INDICATOR = exTleIndicator;}
    public String getTleIndicator() {return this.ERCM_SSKB_TLE_INDICATOR;}

    public void setHostIndex(String exHostIndex) {this.ERCM_INIT_HOST_INDEX = exHostIndex;}
    public String getHostIndex() {return this.ERCM_INIT_HOST_INDEX;}

    public void setAcquirerNii(String exAcquirerNII) {this.ERCM_INIT_ACQUIRER_NII = exAcquirerNII;}
    public String getAcquirerNii() {return this.ERCM_INIT_ACQUIRER_NII;}

    public void setAcquirerName(String exAcquirerName) {this.ERCM_INIT_ACQUIRER_NAME = exAcquirerName;}
    public String getAcquirerName() {return this.ERCM_INIT_ACQUIRER_NAME;}

    public void setRawFileHeaderLogoFile(String exHeaderLogoFileName) {this.ERCM_INIT_RAW_HEADER_LOGO_FILE = exHeaderLogoFileName;}
    public String getRawFileHeaderLogoFile() {return this.ERCM_INIT_RAW_HEADER_LOGO_FILE;}

    public void setRawFileFooterLogoFile(String exFooterLogoFileName) {this.ERCM_INIT_RAW_FOOTER_LOGO_FILE = exFooterLogoFileName;}
    public String getRawFileFooterLogoFile() {return this.ERCM_INIT_RAW_FOOTER_LOGO_FILE;}

    public void setSessionKeyInfosFile(String exSessionKeyDataFileName) {this.ERCM_INIT_SSK_INFOS_FILE = exSessionKeyDataFileName;}
    public String getSessionKeyInfosFile() {return this.ERCM_INIT_SSK_INFOS_FILE;}


}
