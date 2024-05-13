/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-9-6
 * Module Author: Kim.L
 * Description:
 *
 * ============================================================================
 */
package com.pax.edc.opensdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;

/**
 * Package-private, implementation of {@link ITransAPI}
 */
class TransAPIImpl implements ITransAPI {
    private Activity mActivity;

    TransAPIImpl(Activity activity) {
        mActivity = activity;
    }

    @Override
    public boolean doTrans(BaseRequest request) {
        if(!isEDCInstalled(mActivity)){
            return false;
        }

        if(!request.checkArgs()){
            Log.e("EDC API", "checkArgs fail");
            return false;
        }
        Uri uri = Uri.parse(Constants.URI);
        Intent intent = new Intent(Constants.ACTION);
        intent.setData(uri);
        intent.setPackage(Constants.PACKAGE_NAME);
        intent.putExtras(request.toBundle(new Bundle()));
        intent.putExtra(Constants.SDK_VERSION, Constants.SDK_VERSION_VALUE);
        intent.putExtra(Constants.APP_PACKAGE, mActivity.getPackageName());
        mActivity.startActivityForResult(intent, Constants.REQUEST_CODE);
        return true;
    }

    @Override
    public BaseResponse onResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.REQUEST_CODE && data != null) {
            return handleResponse(data);
        }
        return null;
    }

    private boolean isEDCInstalled(Context context){
        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(Constants.PACKAGE_NAME, 0);
            // TODO: 9/19/2017 signature check
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return packageInfo != null;
    }

    private BaseResponse handleResponse(@NonNull Intent data){
        int commandType =  data.getIntExtra(Constants.COMMAND_TYPE, 0);
        switch(commandType) {
            case Constants.PRE_AUTH:
                return new PreAuthMsg.Response(data.getExtras());
            case Constants.SALE:
                return new SaleMsg.Response(data.getExtras());
            case Constants.VOID:
                return new VoidMsg.Response(data.getExtras());
            case Constants.REFUND:
                return new RefundMsg.Response(data.getExtras());
            case Constants.SETTLE:
                return new SettleMsg.Response(data.getExtras());
            case Constants.REPRINT_TRANS:
                return new ReprintTransMsg.Response(data.getExtras());
            case Constants.REPRINT_TOTAL:
                return new ReprintTotalMsg.Response(data.getExtras());
            default:
                return null;
        }
    }
}
