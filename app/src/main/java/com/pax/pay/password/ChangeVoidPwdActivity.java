/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-12-21
 * Module Author: Rim.Z
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.password;

import com.pax.abl.utils.EncUtils;
import com.pax.pay.app.FinancialApplication;
import com.pax.settings.SysParam;

/**
 * Change Void password
 */
public class ChangeVoidPwdActivity extends BaseChangePwdActivity {

    @Override
    protected void savePwd() {
        FinancialApplication.getSysParam().set(SysParam.StringParam.SEC_VOID_PWD, EncUtils.sha1(pwd));
    }
}
