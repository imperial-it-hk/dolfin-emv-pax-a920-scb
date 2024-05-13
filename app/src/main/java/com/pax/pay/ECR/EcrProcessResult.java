package com.pax.pay.ECR;

import android.util.SparseIntArray;

import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.utils.Utils;

public class EcrProcessResult {

     private EcrProcessResult() {
     }

     // Success status
    public static final int SUCC                                            = 0;

    // LINKPOS DISPLAY MESSAGE
    public static final int ECR_LINKPOS_TRANS_NOT_ALLOW                     = -100;
    public static final int ECR_PROCESS_FAILED                              = -101;
    public static final int ECR_FOUND_DOUBLE_TRANSACTION_BLOCKED            = -102;
    public static final int ECR_COMMAND_VALIDATION_FAILED                   = -103;
    public static final int ECR_VALIDATE_FAILED_TRANS_AMOUNT_FORMAT         = -104;
    public static final int ECR_VALIDATE_FAILED_TRANS_AMOUNT_LENGTH         = -105;
    public static final int ECR_VALIDATE_FAILED_MERC_NUMBER_FORMAT          = -106;
    public static final int ECR_VALIDATE_FAILED_MERC_NUMBER_LENGTH          = -107;
    public static final int ECR_VALIDATE_FAILED_TRACE_INVOICE_FORMAT        = -108;
    public static final int ECR_VALIDATE_FAILED_TRACE_INVOICE_LENGTH        = -109;
    public static final int ECR_VALIDATE_FAILED_QR_TYPE_FORMAT              = -110;
    public static final int ECR_VALIDATE_FAILED_QR_TYPE_LENGTH              = -111;
    public static final int ECR_VALIDATE_FAILED_HOST_NII_FORMAT             = -112;
    public static final int ECR_VALIDATE_FAILED_HOST_NII_LENGTH             = -113;
    public static final int ECR_VALIDATE_FAILED_SALE_REF_ID_R0_FORMAT       = -114;
    public static final int ECR_VALIDATE_FAILED_SALE_REF_ID_R0_LENGTH       = -115;
    public static final int ECR_VALIDATE_FAILED_SALE_REF_ID_R1_FORMAT       = -116;
    public static final int ECR_VALIDATE_FAILED_SALE_REF_ID_R1_LENGTH       = -117;
    public static final int ECR_FOUND_UNSUPPORTED_COMMAND                   = -118;
    public static final int ECR_SETTLEMENT_ZERO_RECORD                      = -119;
    public static final int ECR_PARAMETER_MISSING                           = -120;
    public static final int ECR_UNSUPPORTED_SPECIFIC_NII                    = -121;


    private static final SparseIntArray messageMap = new SparseIntArray();

    static {
        messageMap.put(ECR_LINKPOS_TRANS_NOT_ALLOW, R.string.err_ecr_transaction_not_allow);
        messageMap.put(ECR_FOUND_DOUBLE_TRANSACTION_BLOCKED, R.string.err_ecr_double_trans_blocked);
        messageMap.put(ECR_COMMAND_VALIDATION_FAILED, R.string.err_ecr_cmd_validation_failed);
        messageMap.put(ECR_VALIDATE_FAILED_TRANS_AMOUNT_FORMAT, R.string.err_ecr_cmd_field_trans_amt_format);
        messageMap.put(ECR_VALIDATE_FAILED_TRANS_AMOUNT_LENGTH, R.string.err_ecr_cmd_field_trans_amt_len);
        messageMap.put(ECR_VALIDATE_FAILED_MERC_NUMBER_FORMAT, R.string.err_ecr_cmd_field_merc_name_format);
        messageMap.put(ECR_VALIDATE_FAILED_MERC_NUMBER_LENGTH, R.string.err_ecr_cmd_field_merc_name_len);
        messageMap.put(ECR_VALIDATE_FAILED_TRACE_INVOICE_FORMAT, R.string.err_ecr_cmd_field_trace_invoice_format);
        messageMap.put(ECR_VALIDATE_FAILED_TRACE_INVOICE_LENGTH, R.string.err_ecr_cmd_field_trace_invoice_len);
        messageMap.put(ECR_VALIDATE_FAILED_QR_TYPE_FORMAT, R.string.err_ecr_cmd_field_qr_type_format);
        messageMap.put(ECR_VALIDATE_FAILED_QR_TYPE_LENGTH, R.string.err_ecr_cmd_field_qr_type_len);
        messageMap.put(ECR_VALIDATE_FAILED_HOST_NII_FORMAT, R.string.err_ecr_cmd_field_host_nii_format);
        messageMap.put(ECR_VALIDATE_FAILED_HOST_NII_LENGTH, R.string.err_ecr_cmd_field_host_nii_len);
        messageMap.put(ECR_VALIDATE_FAILED_SALE_REF_ID_R0_FORMAT, R.string.err_ecr_cmd_field_sale_ref_id_r0_format);
        messageMap.put(ECR_VALIDATE_FAILED_SALE_REF_ID_R0_LENGTH, R.string.err_ecr_cmd_field_sale_ref_id_r0_len);
        messageMap.put(ECR_VALIDATE_FAILED_SALE_REF_ID_R1_FORMAT, R.string.err_ecr_cmd_field_sale_ref_id_r1_format);
        messageMap.put(ECR_VALIDATE_FAILED_SALE_REF_ID_R1_LENGTH, R.string.err_ecr_cmd_field_sale_ref_id_r1_len);
        messageMap.put(ECR_FOUND_UNSUPPORTED_COMMAND, R.string.err_ecr_unsupported_command);
        messageMap.put(ECR_SETTLEMENT_ZERO_RECORD, R.string.err_ecr_settlement_zero_record);
        messageMap.put(ECR_PARAMETER_MISSING, R.string.err_ecr_settlement_zero_record);
        messageMap.put(ECR_UNSUPPORTED_SPECIFIC_NII, R.string.err_ecr_unsupported_specific_nii);
    }

    public static String getMessage(int ret) {
        String message;
        int resourceId = messageMap.get(ret, -1);
        if (resourceId == -1) {
            message = Utils.getString(R.string.err_undefine) + "[" + ret + "]";
        } else {
            message = Utils.getString(resourceId);
        }
        return message;
    }
}
