package com.pax.pay.ECR;

public enum HyperComMsg {
    instance;


    /**************************************************************************************************/
    /*                                 Header Field For HyperCom Protocol                                   */
    /**************************************************************************************************/
    byte formatVer;
    byte reqResIndicator;
    byte[] transactionCode = new byte[2];
    byte[] responseCode = new byte[2];
    byte moreIndicator;

    /**************************************************************************************************/
    /*                                 Data Field For HyperCom Protocol                                   */
    /**************************************************************************************************/

    public String data_field_40_amount;
    public String data_field_65_invoiceNo;
    public String data_field_HN_nii;
    public String data_field_D6_ref2;
    public String data_field_D7_ref1;
    public String data_field_D8_branchID;

    // Extra for LemonFarm
    public String data_field_A1_qr_type;
    public String data_field_R1_ref_saleID;

    // Extra for Lawson
    public String data_field_45_merchant_number;
    public String data_field_R0_ref_saleID;

    public void reset() {
        formatVer = 0;
        reqResIndicator = 0;
        transactionCode[0] = 0;
        transactionCode[1] = 0;
        responseCode[0] = 0;
        responseCode[1] = 0;
        moreIndicator = 0;
        data_field_40_amount = null;
        data_field_65_invoiceNo = null;
        data_field_HN_nii = null;
        data_field_D6_ref2 = null;
        data_field_D7_ref1 = null;
        data_field_D8_branchID = null;

        data_field_A1_qr_type = null;
        data_field_R1_ref_saleID = null;

        data_field_45_merchant_number = null;
        data_field_R0_ref_saleID = null;
    }

    public byte[] getTransactionCode() {
        return transactionCode;
    }

    public void setTransactionCode(byte[] transCode) {
        if (transCode != null) {
            transactionCode = transCode;
        }
    }
}
