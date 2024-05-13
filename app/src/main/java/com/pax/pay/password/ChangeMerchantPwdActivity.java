/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-3-21
 * Module Author: Kim.L
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.password;

import com.pax.abl.utils.EncUtils;
import com.pax.pay.app.FinancialApplication;
import com.pax.settings.SysParam;

/**
 * Change terminal password
 */
public class ChangeMerchantPwdActivity extends BaseChangePwdActivity {

    @Override
    protected void savePwd() {
        FinancialApplication.getSysParam().set(SysParam.StringParam.SEC_MERCHANT_PWD, EncUtils.sha1(pwd));
    }
}
