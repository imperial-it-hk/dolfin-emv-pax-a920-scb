package com.pax.pay.trans.pack.qr;

import th.co.bkkps.utils.Log;

import com.pax.edc.R;
import com.pax.pay.ECR.HyperComMsg;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.QrFields;
import com.pax.settings.SysParam;

import java.util.Calendar;

/**
 * Created by SORAYA S on 06-Feb-18.
 */

public class PackQr {

    protected static final String TAG = "PackQr";

    private static final String AID_PROMPTPAY_DYNAMIC = "A000000677010112";
    private static final String AID_PROMPTPAY_STATIC = "A000000677010111";

    private String billerId;
    private String tid;
    private String mid;
    private String amt;
    private String datetime;
    private String traceNo;
    private int typeQR;

    public PackQr(int typeQR, String billerId, String tid, String mid, String amt, String datetime, String traceNo) {
        this.typeQR = typeQR;
        this.billerId = billerId;
        this.tid = tid;
        this.mid = mid;
        this.amt = amt;
        this.datetime = datetime;
        this.traceNo = traceNo;
    }

    public String packQr(){
        String qrStr = "";

        QrFields field1 = QrFields.PAYLOAD_VERSION;
        QrFields field2 = QrFields.POINT_OF_INIT_METHOD;
        QrFields field3_Static = QrFields.FIELD_3_STATIC;
        QrFields field3_Dynamic = QrFields.FIELD_3_DYNAMIC;
        QrFields field3_00 = QrFields.FIELD_3_MERCHANT_ID;
        QrFields field3_01 = QrFields.FIELD_3_BILLER_ID;
        QrFields field3_02 = QrFields.FIELD_3_REF1;
        QrFields field3_03 = QrFields.FIELD_3_REF2;
        QrFields field4 = QrFields.COUNTRY_CODE;
        QrFields field5 = QrFields.TRANS_AMT;
        QrFields field6 = QrFields.CURRENCY_CODE;
        QrFields field7 = QrFields.MERCHANT_NAME;
        QrFields field8 = QrFields.TERMINAL_ID;
        QrFields field9 = QrFields.CRC;

        String merchantName = FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_MERCHANT_NAME_EN);

        //set field 3 - #ID-30
        String merchantId = typeQR == R.string.trans_dynamic_qr ? AID_PROMPTPAY_DYNAMIC : AID_PROMPTPAY_STATIC;//dynamic-A000000677010112, static-A000000677010111
        String strField3_00 = field3_00.getId() + Component.getPaddedNumber(merchantId.length(), 2) + merchantId;
        billerId = typeQR == R.string.trans_dynamic_qr ? billerId : "0066891326668";//dynamic-010754400004305(billerId), static-mobile number
        String strField3_01 = field3_01.getId() + billerId.length() + billerId;

        String strField2;
        String strField3_head;
        String strField3_02 = null;
        String strField3_03 = null;
        String strField7 = null;
        String strField8 = null;
        Log.i(TAG,"R typeQR = " + typeQR);
        if(typeQR == R.string.trans_dynamic_qr){
            //for Dynamic
            strField3_02 = field3_02.getId() + Component.getPaddedNumber(mid.length(), 2) + mid;
            String ref2 = this.getQrRef2();
            strField3_03 = field3_03.getId() + Component.getPaddedNumber(ref2.length(), 2) + ref2;
            String field3 = strField3_00 + strField3_01 + strField3_02 + strField3_03;
            strField2 = "12";
            strField3_head = field3_Dynamic.getId() + Component.getPaddedNumber(field3.length(), 2);
            String merchantNameSize = "00";
            if(merchantName != null && !"".equals(merchantName)){
                merchantName = replaceSpecialCharacter(merchantName);
                int size = merchantName.length();
                merchantNameSize = Component.getPaddedNumber(size, 2);
            }
            strField7 = field7.getId() + merchantNameSize + merchantName;
            strField8 = field8.getIdAndLength() + "0708" + Component.getPaddedString(tid, 8, '0');
        }else{
            //for Static
            strField2 = "11";
            String field3  = strField3_00 + strField3_01;
            strField3_head = field3_Static.getId() + Component.getPaddedNumber(field3.length(), 2);
        }

        // field 1 - #ID-00
        qrStr += field1.getIdAndLength() + "01";//TODO

        // field 2 - #ID-01
        qrStr += field2.getIdAndLength() + strField2;//TODO

        // field 3 - #ID-00 for MerchantID
        qrStr += strField3_head + strField3_00;

        // field 3 - #ID-01 for BillerID
        qrStr += strField3_01;

        // field 3 - #ID-02 for Ref1
        if(strField3_02 != null){
            qrStr += strField3_02;
        }

        // field 3 - #ID-03 for Ref2
        if(strField3_03 != null){
            qrStr += strField3_03;
        }


        if(typeQR == R.string.trans_dynamic_qr){// dynamic QR (ID seq: 53, 54, 58)
            // field 4
            qrStr += field6.getIdAndLength() + "764";//TODO

            // field 5
            String transAmt = this.addPointandPaddingAmt(amt);
            qrStr += field5.getId() + Component.getPaddedNumber(transAmt.length(), 2) + transAmt;

            // field 6
            qrStr += field4.getIdAndLength() + "TH";//TODO
        }else{// static QR (ID seq: 58, 54, 53)
            // field 4
            qrStr += field4.getIdAndLength() + "TH";//TODO

            // field 5
            String transAmt = this.addPointandPaddingAmt(amt);
            qrStr += field5.getId() + Component.getPaddedNumber(transAmt.length(), 2) + transAmt;

            // field 6
            qrStr += field6.getIdAndLength() + "764";//TODO
        }

        // field 7 - #ID-59
        if(strField7 != null) {
            qrStr += strField7;
        }

        // field 8 - #ID-62
        if(strField8 != null) {
            qrStr += strField8;
        }

        // field 9 - #ID-63
        qrStr += field9.getIdAndLength();

        qrStr += this.getStringCRC16(qrStr);

        return qrStr;
    }

    public String getQrRef2(){
        String ref2 = Component.getPaddedString(tid, 8, '0')  + this.getMinOfYear() + traceNo;
        boolean isKerryAPI = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_KERRY_API);
        if (HyperComMsg.instance.data_field_D6_ref2 != null && isKerryAPI) {
            ref2 = updateQrFromKerryPos(ref2);
        }
        return ref2;
    }

    private String addPointandPaddingAmt(String amt){
        int size = amt.length();
        if(size <= 2){
            amt = Component.getPaddedString(amt, 3, '0');
        }
        amt = new StringBuilder(amt).insert(amt.length()-2, ".").toString();
        return amt;
    }

    private String getMinOfYear(){
        int year = Integer.parseInt(datetime.substring(0, 4));
        int month = Integer.parseInt(datetime.substring(4, 6));
        int date = Integer.parseInt(datetime.substring(6, 8));
        int hh = Integer.parseInt(datetime.substring(8, 10));
        int mm = Integer.parseInt(datetime.substring(10, 12));

        Calendar cal = Calendar.getInstance();
        cal.set(year, month-1, date);
        int dayOfYear = cal.get(Calendar.DAY_OF_YEAR);

        int minOfYear = ((dayOfYear - 1) * 1440) + (hh * 60) + mm;

        return Component.getPaddedString(String.valueOf(minOfYear), 6, '0');
    }

    private String getStringCRC16(String fields){
        int crc = 0xFFFF;          // initial value
        int polynomial = 0x1021;   // 0001 0000 0010 0001  (0, 5, 12)

        // byte[] testBytes = "123456789".getBytes("ASCII");

        byte[] bytes = fields.getBytes();

        for (byte b : bytes) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b   >> (7-i) & 1) == 1);
                boolean c15 = ((crc >> 15    & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit) crc ^= polynomial;
            }
        }

        crc &= 0xffff;
        return Integer.toHexString(crc).toUpperCase();
    }

    private String replaceSpecialCharacter(String string) {
        return string.replaceAll("[^A-Za-z0-9 ]"," ");// replace all special character to space.
    }

    private String updateQrFromKerryPos (String ref2) {
        String strPos = HyperComMsg.instance.data_field_D6_ref2.substring(0,6);
        ref2 = ref2.substring(0,ref2.length()-6);
        return ref2 + strPos;
    }
}
