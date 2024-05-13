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

import android.os.Bundle;
import androidx.annotation.NonNull;

import java.io.Serializable;

/**
 * Common Request
 */
public abstract class BaseRequest implements Serializable {
    private String appId; // TODO: 9/19/2017 keep it since it can be used to do function limit(using white list, black list)
    private String packageName;

    BaseRequest(){
        //do nothing
    }

    abstract int getType();

    @NonNull
    Bundle toBundle(@NonNull Bundle bundle) {
        bundle.putInt(Constants.COMMAND_TYPE, this.getType());
        bundle.putString(Constants.APP_ID, this.appId);
        bundle.putString(Constants.APP_PACKAGE, this.packageName);
        return bundle;
    }

    void fromBundle(Bundle bundle) {
        this.appId = IntentUtil.getStringExtra(bundle, Constants.APP_ID);
        this.packageName = IntentUtil.getStringExtra(bundle, Constants.APP_PACKAGE);
    }

    abstract boolean checkArgs();

    /**
     * @return app ID
     */
    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    @Override
    public String toString() {
        return appId;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
}
