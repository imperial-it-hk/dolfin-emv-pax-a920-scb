/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-9-18
 * Module Author: Kim.L
 * Description:
 *
 * ============================================================================
 */
package com.pax.edc.opensdk;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;

/**
 * For app module to call, hide from Javadoc
 */
public class MessageUtils {
    private MessageUtils(){
        //do nothing
    }

    public static BaseRequest generateRequest(Intent intent) {
        int commandType = intent.getIntExtra(Constants.COMMAND_TYPE, 0);
        switch(commandType) {
            case Constants.PRE_AUTH:
                return new PreAuthMsg.Request(intent.getExtras());
            case Constants.SALE:
                return new SaleMsg.Request(intent.getExtras());
            case Constants.VOID:
                return new VoidMsg.Request(intent.getExtras());
            case Constants.REFUND:
                return new RefundMsg.Request(intent.getExtras());
            case Constants.SETTLE:
                return new SettleMsg.Request(intent.getExtras());
            case Constants.REPRINT_TRANS:
                return new ReprintTransMsg.Request(intent.getExtras());
            case Constants.REPRINT_TOTAL:
                return new ReprintTotalMsg.Request(intent.getExtras());
            default:
                return null;
        }
    }

    public static int getType(@NonNull BaseRequest request){
        return request.getType();
    }

    @NonNull
    public static Bundle toBundle(@NonNull BaseRequest request, @NonNull Bundle bundle) {
        return request.toBundle(bundle);
    }

    public static void fromBundle(@NonNull BaseRequest request, Bundle bundle) {
        request.fromBundle(bundle);
    }

    public static boolean checkArgs(@NonNull BaseRequest request){
        return request.checkArgs();
    }

    public static int getType(@NonNull BaseResponse response){
        return response.getType();
    }

    @NonNull
    public static Bundle toBundle(@NonNull BaseResponse response, @NonNull Bundle bundle) {
        return response.toBundle(bundle);
    }

    public static void fromBundle(@NonNull BaseResponse response, Bundle bundle) {
        response.fromBundle(bundle);
    }

    public static boolean checkArgs(@NonNull BaseResponse response){
        return response.checkArgs();
    }
}
