package com.pax.pay.trans.model;

/**
 * Created by SORAYA S on 06-Feb-18.
 */

public enum QrFields {

    PAYLOAD_VERSION("00", "02"),
    POINT_OF_INIT_METHOD("01", "02"),
    FIELD_3_STATIC("29", ""),
    FIELD_3_DYNAMIC("30", ""),
    FIELD_3_MERCHANT_ID("00", ""),
    FIELD_3_BILLER_ID("01", ""),
    FIELD_3_REF1("02", ""),
    FIELD_3_REF2("03", ""),
    CURRENCY_CODE("53", "03"),
    TRANS_AMT("54", ""),
    COUNTRY_CODE("58", "02"),
    MERCHANT_NAME("59", ""),
    TERMINAL_ID("62", "12"),
    CRC("63", "04");

    private String id;
    private String length;

    QrFields(String id, String length) {
        this.id = id;
        this.length = length;
    }

    public String getId() {
        return id;
    }

    public String getLength() {
        return length;
    }

    public String getIdAndLength(){
        return id + length;
    }
}
