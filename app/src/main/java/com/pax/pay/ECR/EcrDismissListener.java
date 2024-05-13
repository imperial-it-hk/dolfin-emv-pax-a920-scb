package com.pax.pay.ECR;

import com.pax.edc.opensdk.TransResult;
public interface EcrDismissListener {
    void onDismiss(int result, boolean showDialog);
    void onDismiss(int result);
}
