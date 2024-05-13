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

import android.content.Intent;

/**
 * Transaction interface for 3rd party app.
 *
 * @version 1.3 add {@link TransResponse#QR}
 * @version 1.2 add signature path to {@link TransResponse}
 * @version 1.1 add card type, cardholder signature to {@link TransResponse}
 * @version 1.0 init
 */
public interface ITransAPI {
    /**
     * @param request Class extends {@link BaseRequest}
     * @return true/false
     */
    boolean doTrans(BaseRequest request);

    /**
     * this method is required to be call in {@link android.app.Activity#onActivityResult(int, int, Intent)}
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode The integer result code returned by the child activity
     *                   through its setResult().
     * @param data An Intent, which can return result data to the caller
     *               (various data can be attached to Intent "extras").
     *
     * @return Class extends {@link BaseResponse}
     */
    BaseResponse onResult(int requestCode, int resultCode, Intent data);
}
