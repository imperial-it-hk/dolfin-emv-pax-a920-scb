package com.pax.pay.trans.model;

import com.pax.eemv.utils.Tools;

import java.util.HashMap;

import th.co.bkkps.utils.Log;

public class ReservedFieldHandle {

    public enum FieldTables {
        SERVICE_CODE,
        PROD_CD,
        QTY,
        SALES_AMT,
        REDEEMED_AMT,
        NET_SALES_AMT,
        REDEEMED_PT,
        BAL_PT,
        POINT_RATE,
        BALANCE_RATE,
        PROD_NAME,
        SPACE_FILTER,

        PLAN_ID,
        PAYMENT_TERM,
        COMPUTE_METHOD,
        INTEREST_RATE,
        INTEREST_FREE,
        FIRST_PAY_AMOUNT,
        LAST_PAY_AMOUNT,
        MONTH_PAY_AMOUNT,
        TOTAL_PAY_AMOUNT,
        OUT_PRINCIPLE,
        OUT_INTEREST,
        HANDING_FEE,
        IPLAN_MODE,

        TABLE_CODE,
        SUPPLIER_NAME,
        PRODUCT_NAME,
        MODEL_NAME,
        CURRENCY,
        TRANS_ID,
        SUPPLIER_NO,
        PRODUCT_TYPE,
        MODEL_NO,
        SERIAL_NO,
        CHARGED_TIME,
        CARD_MASKING,
    }

    public static final DynamicFields[] redeemed_response = {
            new DynamicFields(FieldTables.SERVICE_CODE, true, 2),
            new DynamicFields(FieldTables.PROD_CD, true, 5),
            new DynamicFields(FieldTables.QTY, true, 2),
            new DynamicFields(FieldTables.SALES_AMT, true, 12),
            new DynamicFields(FieldTables.REDEEMED_AMT, true, 12),
            new DynamicFields(FieldTables.NET_SALES_AMT, true, 12),
            new DynamicFields(FieldTables.REDEEMED_PT, true, 9),
            new DynamicFields(FieldTables.BAL_PT, true, 9),
            new DynamicFields(FieldTables.POINT_RATE, true, 7),
            new DynamicFields(FieldTables.BALANCE_RATE, true, 9),
            new DynamicFields(FieldTables.PROD_NAME, true, 25),
            new DynamicFields(FieldTables.SPACE_FILTER, true, 1)
    };

    public static final DynamicFields[] smtp_f61_response = {
            new DynamicFields(FieldTables.PLAN_ID, true, 3),
            new DynamicFields(FieldTables.PAYMENT_TERM, true, 2),
            new DynamicFields(FieldTables.COMPUTE_METHOD, true, 1),
            new DynamicFields(FieldTables.INTEREST_RATE, true, 6),
            new DynamicFields(FieldTables.INTEREST_FREE, true, 2),
            new DynamicFields(FieldTables.FIRST_PAY_AMOUNT, true, 9),
            new DynamicFields(FieldTables.LAST_PAY_AMOUNT, true, 9),
            new DynamicFields(FieldTables.MONTH_PAY_AMOUNT, true, 9),
            new DynamicFields(FieldTables.TOTAL_PAY_AMOUNT, true, 9),
            new DynamicFields(FieldTables.OUT_PRINCIPLE, true, 9),
            new DynamicFields(FieldTables.OUT_INTEREST, true, 9),
            new DynamicFields(FieldTables.HANDING_FEE, true, 5),
            new DynamicFields(FieldTables.IPLAN_MODE, true, 2)
    };

    public static final DynamicFields[] smtp_f63_response = {
            new DynamicFields(FieldTables.TABLE_CODE, true, 2),
            new DynamicFields(FieldTables.SUPPLIER_NAME, true, 20),
            new DynamicFields(FieldTables.PRODUCT_NAME, true, 20),
            new DynamicFields(FieldTables.MODEL_NAME, true, 20)
    };

    public static final DynamicFields[] dolfinIpp_f63_response = {
            new DynamicFields(FieldTables.CURRENCY, true, 3),
            new DynamicFields(FieldTables.TRANS_ID, true, 50),
            new DynamicFields(FieldTables.PLAN_ID, true, 11),
            new DynamicFields(FieldTables.IPLAN_MODE, true, 10),
            new DynamicFields(FieldTables.SUPPLIER_NO, true, 10),
            new DynamicFields(FieldTables.PAYMENT_TERM, true, 11),
            new DynamicFields(FieldTables.PRODUCT_TYPE, true, 3),
            new DynamicFields(FieldTables.MODEL_NO, true, 6),
            new DynamicFields(FieldTables.SERIAL_NO , true, 18),
            new DynamicFields(FieldTables.COMPUTE_METHOD, true, 1),
            new DynamicFields(FieldTables.INTEREST_RATE, true, 6),
            new DynamicFields(FieldTables.INTEREST_FREE, true, 2),
            new DynamicFields(FieldTables.FIRST_PAY_AMOUNT, true, 9),
            new DynamicFields(FieldTables.LAST_PAY_AMOUNT, true, 9),
            new DynamicFields(FieldTables.MONTH_PAY_AMOUNT, true, 9),
            new DynamicFields(FieldTables.TOTAL_PAY_AMOUNT, true, 9),
            new DynamicFields(FieldTables.OUT_PRINCIPLE, true, 9),
            new DynamicFields(FieldTables.OUT_INTEREST, true, 9),
            new DynamicFields(FieldTables.HANDING_FEE, true, 5),
            new DynamicFields(FieldTables.SUPPLIER_NAME, true, 20),
            new DynamicFields(FieldTables.PRODUCT_NAME, true, 20),
            new DynamicFields(FieldTables.MODEL_NAME, true, 20),
            new DynamicFields(FieldTables.CHARGED_TIME, true, 17),
            new DynamicFields(FieldTables.CARD_MASKING, true, 16),
    };

    public static HashMap<FieldTables, byte[]> unpackReservedField(byte[] buff_data, DynamicFields[] fields, boolean isAllDataFixed) {
        HashMap<FieldTables, byte[]> result = null;
        if (buff_data != null) {
            Log.d("SMARTPAY-RESERVED-FIELD" + ((fields.equals(ReservedFieldHandle.smtp_f61_response)) ? "61" : "63"), Tools.bcd2Str(buff_data));
            int off = 0, len = buff_data.length;
            if (isAllDataFixed && len != getLength(fields)) {
                return null;
            }
            result = new HashMap<>();
            for (DynamicFields f : fields) {
                if (off < len && off+f.lenTag <= len) {
                    if (f.isFixedLength) {
                        byte[] value = new byte[f.lenTag];
                        System.arraycopy(buff_data, off, value, 0, f.lenTag);
                        result.put(f.field, value);
                    } else {
                        // do nothing
                    }
                    off += f.lenTag;
                }
            }
        }
        return result;
    }

    private static int getLength(DynamicFields[] fields) {
        int len = 0;
        for (DynamicFields f : fields) {
            len += f.lenTag;
        }
        return len;
    }

    public static class DynamicFields {

        private FieldTables field;
        private byte[] value;
        private boolean isFixedLength;
        private int lenTag;

        public DynamicFields(FieldTables field, boolean isFixedLength, int lenTag) {
            this.field = field;
            this.isFixedLength = isFixedLength;
            this.lenTag = lenTag;
        }

        public FieldTables getField() {
            return field;
        }

        public void setField(FieldTables field) {
            this.field = field;
        }

        public byte[] getValue() {
            return value;
        }

        public void setValue(byte[] value) {
            this.value = value;
        }

        public boolean isFixedLength() {
            return isFixedLength;
        }

        public void setFixedLength(boolean fixedLength) {
            isFixedLength = fixedLength;
        }

        public int getLenTag() {
            return lenTag;
        }

        public void setLenTag(int lenTag) {
            this.lenTag = lenTag;
        }
    }
}
