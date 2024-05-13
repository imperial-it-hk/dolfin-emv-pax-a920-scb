/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-11-25
 * Module Author: Steven.W
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.trans;

import com.pax.abl.core.AAction;

import th.co.bkkps.utils.Log;

public class TransContext {
    private static TransContext transContext;

    private AAction currentAction;

    private TransContext() {

    }

    public static synchronized TransContext getInstance() {
        if (transContext == null) {
            Log.d("TransContext", "TransContext is null. Create new instance.");
            transContext = new TransContext();
        }
        return transContext;
    }

    public AAction getCurrentAction() {
        return currentAction;
    }

    public void setCurrentAction(AAction currentAction) {
        this.currentAction = currentAction;
    }

}
