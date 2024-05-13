package com.pax.pay.password;

        import com.pax.abl.utils.EncUtils;
        import com.pax.pay.app.FinancialApplication;
        import com.pax.settings.SysParam;

/**
 * Change adjust password
 */
public class ChangeTlePwdActivity extends BaseChangePwdActivity {

    @Override
    protected void savePwd() {
        FinancialApplication.getSysParam().set(SysParam.StringParam.SEC_TLE_PWD, EncUtils.sha1(pwd));
    }
}
