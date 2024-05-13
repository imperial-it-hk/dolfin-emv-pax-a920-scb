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
package com.pax.pay.app.quickclick;

/**
 * a 500ms-quick-click-protection singleton
 */
public class QuickClickProtection extends AQuickClickProtection {
    private static QuickClickProtection quickClickProtection;

    private QuickClickProtection(long timeoutMs) {
        super(timeoutMs);
    }

    public static synchronized QuickClickProtection getInstance() {
        if (quickClickProtection == null) {
            quickClickProtection = new QuickClickProtection(500);
        }

        return quickClickProtection;
    }
}
