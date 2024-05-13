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

/**
 * Factory for create a Transaction instance
 */
public class TransAPIFactory {

    private TransAPIFactory() {
    }

    public static ITransAPI createTransAPI(Activity activity) {
        return new TransAPIImpl(activity);
    }

}
