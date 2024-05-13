/*
 *
 *   ============================================================================
 *   = COPYRIGHT
 *                 PAX TECHNOLOGY, Inc. PROPRIETARY INFORMATION
 *     This software is supplied under the terms of a license agreement or
 *     nondisclosure agreement with PAX  Technology, Inc. and may not be copied
 *     or disclosed except in accordance with the terms in that agreement.
 *        Copyright (C) 2019 -? PAX Technology, Inc. All rights reserved.
 *   Description: // Detail description about the function of this module,
 *               // interfaces with the other modules, and dependencies.
 *   Revision History:
 *   Date	                 Author	                        Action
 *   8/14/19 6:05 PM  	         XuShuang           	Create/Add/Modify/Delete
 *   ============================================================================
 *
 */

package com.pax.view.widget;

import android.content.Context;

/**
 * Created by minson
 */

public class ScannerDpUtils {

    /**
     * convert dp to px
     *
     * @param context
     * @param dpValue
     * @return
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * convert px to dp
     *
     * @param context
     * @param pxValue
     * @return
     */
    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }
}
